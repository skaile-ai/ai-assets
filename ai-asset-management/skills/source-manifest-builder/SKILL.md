---
name: source-manifest-builder
description: "Use when an AI-asset repo author wants to ship a deterministic, author-curated asset inventory in `.skaile-source.yaml`. Walks the repo, builds the `assets:` block via `skaile source build-manifest`, reviews entries with the author (publisher mapping, requires-graph, file filter), computes hashes, and writes the result back. Trigger when the user says 'create a source manifest', 'lock down my asset versions', 'pre-pack my repo for the Catalog', or asks how to publish a repo to skaile.store without admin curation."
metadata:
  version: 1.0.0
  source: MERGED
  tags: [asset-store, manifest, source, discovery, repo-author, catalog]
  prerequisites:
    files:
      - path: .skaile-source.yaml
        gate: soft
        description: "Existing source config; created if absent"
    inputs_optional:
      - id: repo_path
        label: "Path to the asset repo"
        type: text
        default: "."
        hint: "Repo root - the directory containing .skaile-source.yaml"
      - id: write
        label: "Write directly vs preview"
        type: boolean
        default: false
        hint: "If false, prints the proposed block; if true, edits the file"
    reads:
      - path: agent-framework/discovery/CLAUDE.md
      - path: agent-framework/discovery/docs/author-manifest.md
      - path: _devlog/plans/2026-05-11-author-shipped-inventory-manifest.md
    writes:
      - path: .skaile-source.yaml
keywords: [asset-store, manifest, source, discovery, repo-author]
---

# Source Manifest Builder

Help the author of an AI-asset repo lock down a deterministic, author-curated
inventory in `.skaile-source.yaml`. The `assets:` block is the contract
between the repo and any Catalog ingestion path: SHA256 hashes are pinned,
file filters are explicit, publisher mapping is per-entry, and AI enrichment
is skipped.

You drive a five-step workflow: confirm context, run the build, review with
the author, write + verify, suggest commit.

## Step 0: Confirm context

1. Resolve `repo_path` (default `.`).
2. Check whether `<repo_path>/.skaile-source.yaml` exists.
   - If absent: offer to scaffold a minimal v2 stub:
     ```yaml
     version: 2
     publisher_default: "@<domain>"
     default_ref: main
     ```
     Ask the author to confirm the publisher template (often `@<domain>` to
     derive from the first-level directory name, sometimes a fixed
     `@skaile`-style namespace).
3. If the file exists, read it and parse the current version.
   - `version: 1`: warn that running `--write` will bump to `version: 2`.
   - `version: 2` with no `assets:` block yet: normal upgrade path.
   - `version: 2` with `assets:` present: the author is refreshing an
     existing inventory; you'll show a diff in step 4 before writing.

## Step 1: Run the build (preview)

Run the build in preview mode and capture the proposed YAML:

```bash
bun run cli source build-manifest --path <repo_path>
```

This prints the proposed `.skaile-source.yaml` (with the new `assets:` block)
to stdout. Hash computation and discovery already happened. Errors and
non-fatal warnings appear on stderr.

Common warnings to surface to the author:

- `Unknown asset kind "..."`: a custom kind that isn't registered in the
  built-in or extension registries. Ask whether they need a plugin or
  meant a different kind name.
- Manifest parse errors: a `.md` / `.yaml` / `.json` file that looks like
  an asset manifest but doesn't parse cleanly. Ask the author to fix
  before re-running.

## Step 2: Review with the author

Walk the proposed entries with the author. For each entry ask:

| Field | Question |
|---|---|
| `publisher` | Is `@<derived-domain>` correct, or should it be a different publisher namespace? Suggest the canonical publisher convention used elsewhere in the repo. |
| `name` | Confirm the asset name. The default comes from the manifest's `name:` field or the parent directory. |
| `version` | Is the declared `version:` the one to publish? Authors sometimes forget to bump pre-publish. |
| `files` | Walk through `files[]`. Are there same-directory files NOT included that should be? Are there files included that shouldn't ship (e.g. tests, scratch notes)? |
| `requires` | List each `requires[]` ref. Confirm whether it was author-declared in the manifest or auto-extracted (composes, skills, items[].ref). Flag entries where the version range looks weak (`*`, `>=0`). |
| `metadata` | Surface the `description` / `keywords` / `tags` passthrough — does the author want to change wording before pinning? |

If anything needs to change, the answers go back into the manifest file
(`SKILL.md` frontmatter, `agent.yaml`, etc.), not into the `.skaile-source.yaml`.
Then re-run step 1 to refresh the proposal.

## Step 3: Write + verify

When the author is happy with the proposal:

```bash
# If the file already had an `assets:` block, show the drift first.
bun run cli source build-manifest --path <repo_path> --diff

# Write the new manifest.
bun run cli source build-manifest --path <repo_path> --write

# Strict verification.
bun run cli source verify-manifest --path <repo_path> --strict
```

`verify-manifest --strict` exits non-zero on:
- Any `ManifestHashMismatch` (someone touched a file after the hash was pinned)
- Any `ManifestFileMissing` (a `files[]` entry that no longer exists)
- Missing `sha256` on any entry (build-manifest --write always pins, so this
  shouldn't happen unless the file was hand-edited)
- Files on disk that aren't referenced by any `assets[].files[]`

If strict fails, decide with the author:
- Genuine drift after a code change → re-run `build-manifest --write` to
  re-pin
- Orphan files that should be inventoried → add a new entry or extend an
  existing `files[]` list

## Step 4: Suggest commit

Surface the diff to the author:

```bash
git diff -- .skaile-source.yaml
```

Propose a commit message tracking what was pinned:

```
chore(source-manifest): pin <N> assets at version <v> [sha pinned]

Bumps .skaile-source.yaml to version 2 and locks the inventory. Each
entry now carries its deterministic SHA256 hash; downstream Catalog
ingestion will use these as the contract instead of running glob
discovery + admin curation.
```

## Step 5: Hand off

If the author plans to publish to skaile.store:

- Phase 2 admin curation will skip AI enrichment when `assets:` is present
- The recorded `sha256` is wire-compatible with the server-computed hash
  (same algorithm: files sorted, `path + \0 + content + \0` framing)
- See `_devlog/specs/2026-05-03-ai-asset-store-v2-design.md` for the full
  Catalog model and the Phase 2.4 ingestion flow

If the author plans to keep the repo private:

- The inventory still works for `skaile source sync` against a local
  Library — discovery uses the manifest as authoritative
- Hash pinning gives the author a tamper-detection signal: re-running
  `verify-manifest` after any file edit catches drift
