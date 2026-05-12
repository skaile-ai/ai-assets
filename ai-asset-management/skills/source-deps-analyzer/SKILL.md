---
name: source-deps-analyzer
description: "Use when a sidecar manifest under ~/.skaile/sources/ needs its `requires:` graph filled in or refined. Scans asset bodies, manifest references, and code imports across the source; runs an LLM batch over the corpus to propose cross-asset requires-edges; writes proposals to a draft commit on a `deps-analysis-<timestamp>` branch for review. Trigger when the user says 'analyze deps for sidecar X', 'fill in the requires-graph', 'find missing dependencies in my sidecar', or asks how to extract cross-asset references automatically."
metadata:
  version: 1.0.0
  source: MERGED
  tags: [asset-store, manifest, sidecar, dependencies, ai-analysis, requires-graph]
  prerequisites:
    files:
      - path: ~/.skaile/sources
        gate: hard
    inputs_required:
      - id: slug
        label: "Sidecar slug"
        type: text
  reads:
    - path: agent-framework/bridge/CLAUDE.md
    - path: _devlog/specs/2026-05-12-source-sidecar-manifests.md
  writes:
    - path: ~/.skaile/sources/<slug>/.skaile-source.yaml
keywords: [asset-store, manifest, sidecar, dependencies, ai-analysis, requires-graph]
---

# Source Deps Analyzer

Enrich a sidecar manifest's `requires:` graph by reading every asset body in
the source and asking an LLM to infer cross-references. Output lands on a
**topic branch** in `~/.skaile/sources/` so the user reviews + merges
explicitly — no silent edits to `main`.

This skill is the heavy-lift counterpart to `source-sidecar-curator`, which
covers manual review. Run the curator first to lock down publisher mapping
and version pins; then run this skill to fill in the requires-graph.

You drive a nine-step workflow.

## Step 0: Locate the sidecar

1. Validate the input `slug`:
   ```bash
   skaile source sidecar list
   ```
2. Read the current manifest into context:
   ```bash
   skaile source sidecar show <slug>
   ```

Bail out early if `assets:` is empty — the user needs to rebuild first:
`skaile source sidecar rebuild <slug>`.

## Step 1: Locate the upstream

Each sidecar's `.skaile-source.lock.json` records the `upstream_path:` field.
That's where the asset bodies live. Confirm the path exists on disk:

```bash
cat ~/.skaile/sources/<slug>/.skaile-source.lock.json | jq .upstream_path
```

If the upstream path is missing on disk, abort with a clear message — the
deps-analyzer can't read bodies from a path that doesn't exist.

## Step 2: Pre-scan each asset (static)

For each entry under `assets:`:

1. Resolve `<upstream_path>/<entry.path>/<entry.files[0]>` (the manifest file
   itself, typically `SKILL.md` / `agent.yaml` / etc.)
2. Read the body + frontmatter
3. Extract candidate cross-references via static patterns:

| Pattern | Example | Likely edge |
|---|---|---|
| Markdown link to a sibling skill | `[overview](../overview/SKILL.md)` | `@<dom>/overview` |
| `@publisher/name` mention in prose | "Use the `@skaile/git` skill" | `@skaile/git` |
| Existing `requires:` block (frontmatter) | `requires: ["@skaile/git@^1"]` | preserve / validate |
| TypeScript/JavaScript imports | `import { foo } from '@skaile/agent-core'` | `@skaile/agent-core` |
| Python imports | `from skaile_agent_core import foo` | `@skaile/agent-core` (slug-resolved) |
| Flow node refs | `kind: skill\n  ref: @x/y@1` | `@x/y` |

Build a structured pre-scan report:

```yaml
asset: @dom/alpha@1.0.0
candidates:
  - target: '@dom/beta'
    source: 'markdown-link'
    snippet: '[beta](../beta/SKILL.md)'
  - target: '@skaile/git'
    source: 'prose-mention'
    snippet: 'Use the @skaile/git skill to commit.'
```

## Step 3: Build the LLM batch prompt

Bundle all assets + their pre-scan reports into a single prompt. The model
goes through the same bridge driver the runner is configured with —
`agent-framework/bridge/CLAUDE.md` documents the abstraction. Do not
introduce a new provider config; reuse whatever the user already set via
`skaile config` / `skaile setup`.

Prompt skeleton (the actual wording is the skill author's; this is the
contract):

```
You are analyzing N assets from a single source repo. For each asset, you
see:
  - The asset's identity (ref, kind)
  - The asset's body (truncated to ~2K tokens per asset)
  - Pre-scanned candidate references

Your job: produce a JSON array of proposed `requires:` edges, one per (from, to)
pair, with a short justification snippet.

Output schema:
[
  {
    "from": "<publisher>/<name>@<version>",
    "to": "<publisher>/<name>[@<range>]",
    "justification": "<short why — quote or paraphrase>"
  },
  ...
]

Rules:
- Only propose edges the body or frontmatter clearly evidences.
- Don't invent edges based on naming heuristics alone.
- Prefer the pinned version when the body references it; fall back to
  `@<publisher>/<name>` (no range) otherwise.
- Skip edges already declared in the entry's `requires:`.
```

## Step 4: Parse the LLM response

Response is a JSON array. Validate:
- Every `from` matches an existing entry's `<publisher>/<name>@<version>`
- Every `to` parses as `@<publisher>/<name>[@<range>]`
- No duplicates against the asset's existing `requires:`

Group edges by `from` so the next step can update each entry once.

## Step 5: Create a topic branch

```bash
skaile source sidecar git -- checkout -b deps-analysis-$(date +%Y%m%d-%H%M%S)
```

The branch name carries a timestamp so multiple runs don't collide.

## Step 6: Apply edits

For each entry that has new edges:

1. Append the proposed `to` refs to the entry's `requires:` array (de-dup
   against existing entries)
2. Optionally add a `metadata.requires_justification:` map (one snippet per
   new edge) so reviewers see the reasoning without scrolling to the diff

Write the updated `.skaile-source.yaml` to disk.

## Step 7: Validate

Run the strict verifier — it will complain if any new requires-edge has
syntactically wrong form:

```bash
skaile source sidecar verify <slug> --strict
```

If validation fails, dump the LLM response + the validator output and ask
the user how to proceed (most often: ignore the offending edge and re-run).

## Step 8: Commit on the topic branch

```bash
skaile source sidecar git -- commit -am "feat(<slug>): deps-analyzer proposals (N edges)"
```

## Step 9: Show the diff + hand off

Print the diff for the user to review:

```bash
skaile source sidecar git -- diff main..deps-analysis-<timestamp>
```

Suggest next steps:

```bash
# Accept all
skaile source sidecar git -- checkout main
skaile source sidecar git -- merge --no-ff deps-analysis-<timestamp>

# Reject all
skaile source sidecar git -- branch -D deps-analysis-<timestamp>

# Cherry-pick specific edges
skaile source sidecar git -- diff main..deps-analysis-<timestamp> -- <slug>/.skaile-source.yaml
# ...then edit by hand and commit on main
```

## Notes

- The skill **never** auto-merges the branch. The user decides explicitly.
- LLM calls are billed against the user's configured driver. For large
  sources (50+ assets) consider running the pre-scan first to see how many
  candidate edges fell out of static analysis before invoking the model.
- The skill is idempotent on the branch: re-running on the same sidecar
  creates a fresh `deps-analysis-<timestamp>` branch (timestamp differs).
  The user prunes stale branches themselves.
- For deterministic CI use, prefer
  `source-sidecar-curator` + hand-written `requires:` blocks. The deps
  analyzer is for first-pass enrichment of a fresh sidecar.
