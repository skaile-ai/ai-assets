---
name: manifest-compliance
description: "Use when an AI-asset repo has manifests or skaile.yaml refs that fail canonical-name rules — the symptom is a runtime hard error like `publisher required in asset ref \"flow:@skaile-ai/CLI Concept\"` or `invalid asset name \"…\"` thrown by parseAssetRef at install/apply time. Scans a repo with `skaile validate`, then fixes each non-compliant asset identity (skill/agent `name`, flow `id`) to lowercase kebab-case and rewrites every reference (skaile.yaml dependencies/overrides, flow node skill refs, directory names). Trigger when the user says 'fix asset names', 'my manifest is invalid', 'publisher required error', 'invalid asset name', 'make my assets compliant', or 'migrate legacy manifest names'."
metadata:
  version: 1.0.0
  source: AUTHORED
  stage: beta
  tags: [asset-store, manifest, compliance, validation, naming, migration, repo-author]
  prerequisites:
    inputs_optional:
      - id: repo_path
        label: "Path to the asset repo / library to check"
        type: text
        default: "."
        hint: "Repo root — the tree containing SKILL.md / *.flow.yaml / skaile.yaml files"
      - id: write
        label: "Apply fixes vs report only"
        type: boolean
        default: false
        hint: "If false, only report violations; if true, rename + rewrite refs"
    reads:
      - path: packages/workspaces/core/CLAUDE.md
        description: "Canonical-identity schema + asset-ref grammar"
keywords: [manifest, compliance, asset-name, kebab-case, parseAssetRef, validate]
---

# Manifest Compliance

Detect and fix asset manifests that violate the **canonical asset-name rule**,
then rewrite every reference so the repo installs and applies without the
`parseAssetRef` hard error.

## The rule

A canonical asset **name** (and a flow's **id**) is **lowercase kebab-case**:

- allowed: `a-z`, `0-9`, single hyphens between segments
- forbidden: uppercase, spaces, underscores, dots, leading/trailing or double hyphens

A full reference is `kind:@<publisher>/name[#version]` — e.g.
`flow:@skaile-ai/cli-concept`. A name like `"CLI Concept"` (space + uppercase)
breaks the grammar; because the publisher is present, older builds reported the
*misleading* `publisher required …` error — the real fault is the name.

> A flow's `name:` frontmatter is a human-readable **display title** and may
> contain spaces/uppercase. The asset identity is the flow's `id:` field —
> that is what must be kebab-case. Fix `id`, leave `name` alone.

## Step 0: Confirm context

1. Resolve `repo_path` (default `.`).
2. Confirm it's a repo/library tree (has `SKILL.md`, `*.flow.yaml`, or
   `skaile.yaml` files somewhere under it).

## Step 1: Detect (always run first)

Run the repo-wide compliance check:

```bash
bun run cli validate <repo_path>
```

`skaile validate [path]` walks the tree and reports, per file:

- `invalid asset name "…"` / `invalid asset id "…"` — a manifest identity that
  isn't kebab-case (skill/agent `name`, flow `id`).
- `[dependencies] / [overrides.ref] — …` — a `skaile.yaml` ref that fails
  `parseAssetRef` (the exact path that hard-errors at runtime).
- `[assets] — invalid asset name "…"` — a published `assets[]` entry identity.

Exit code is `1` when any violation is found. Collect the full list before
fixing — there are usually several, and references are interlinked.

If validate is clean, report "compliant" and stop.

## Step 2: Plan the renames

For each violation, derive the compliant name and present the mapping to the
user **before** writing:

| Bad identity | Proposed kebab-case |
|---|---|
| `CLI Concept` | `cli-concept` |
| `Test Echo Flow` | `test-echo-flow` |
| `my_audit` | `my-audit` |

Kebab-case derivation: lowercase, replace runs of non-`[a-z0-9]` with a single
hyphen, trim leading/trailing hyphens. Flag collisions (two assets mapping to
the same name) for the user to disambiguate — never auto-merge.

## Step 3: Apply fixes (`write = true`)

For each renamed asset, apply **all** of these so no dangling reference remains:

1. **Manifest identity** — edit the `name:` (or, for flows, `id:`) field in the
   asset's own manifest (`SKILL.md` frontmatter, `agent.yaml`, `*.flow.yaml`,
   `MCP.md`, `CONNECTOR.md`, `*.preset.yaml`, etc.).
2. **Directory name** — if the asset's folder is named after the old identity,
   rename the directory to match (use `git mv` so history is preserved).
3. **Inbound references** — grep the whole tree for the old identity and update
   every occurrence:
   - `skaile.yaml` → `dependencies:`, `overrides[].ref`, `assets[].name`
   - flow nodes → `data.skill` / node refs that name the asset
   - any `kind:@publisher/<old-name>` ref string anywhere

   ```bash
   grep -rn "<old-name>" <repo_path> --include="*.yaml" --include="*.md" --include="*.json"
   ```

   Review each hit — only rewrite genuine asset-ref occurrences, not prose.

## Step 4: Verify

Re-run the detector and confirm a clean tree:

```bash
bun run cli validate <repo_path>
```

Expect `All manifests valid` and exit `0`. If violations remain, return to
Step 3 (a missed inbound reference, or a second-order collision).

## Step 5: Suggest commit

Summarize the renames applied (old → new) and the files touched, and suggest a
commit message, e.g.:

```
fix(assets): make asset identities canonical kebab-case

- flow "CLI Concept" → cli-concept (+ refs)
```

## CHECKLIST

- [ ] Ran `skaile validate <repo_path>` and collected every violation
- [ ] Proposed kebab-case mapping reviewed with the user (collisions flagged)
- [ ] Fixed flow identity in `id:`, not `name:` (display title left intact)
- [ ] Updated manifest identity, directory name, AND all inbound references
- [ ] Re-ran `skaile validate` to a clean (exit 0) tree before committing
