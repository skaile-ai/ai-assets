---
name: source-sidecar-curator
description: "Use when a user wants to refine a sidecar manifest under ~/.skaile/sources/
  for a registered asset source. Walks the sidecar's auto-generated `assets:` block,
  interviews the user on publisher mapping, version, file filters, requires-graph,
  then verifies and commits. Trigger when the user says 'refine my sidecar', 'curate
  the manifest for source X', 'review sidecar assets', or asks how to clean up an
  auto-generated source manifest."
metadata:
  source: MERGED
  tags: [asset-store, manifest, sidecar, source, discovery, curator]
  prerequisites:
    files:
    - path: ~/.skaile/sources
      gate: hard
      description: "Sidecar root must exist (created by `skaile source add --sidecar`)"
    inputs_required:
    - id: slug
      label: "Sidecar slug (subdirectory under ~/.skaile/sources/)"
      type: text
    reads:
    - path: agent-framework/discovery/CLAUDE.md
    - path: agent-framework/discovery/docs/author-manifest.md
    - path: _devlog/specs/2026-05-12-source-sidecar-manifests.md
    writes:
    - path: ~/.skaile/sources/<slug>/.skaile-source.yaml
keywords: [asset-store, manifest, sidecar, source, discovery, curator]
---

# Source Sidecar Curator

Help the user refine a **sidecar manifest** under `~/.skaile/sources/<slug>/`.
A sidecar is a local-overlay `.skaile-source.yaml` that takes precedence over
the upstream repo's source config at discovery time. The auto-generated
inventory from `skaile source add --sidecar` is a starting point — this skill
walks the user through review and refinement.

You drive a six-step workflow: locate, read, interview, edit, verify, commit.
The companion skill `source-deps-analyzer` covers requires-graph enrichment
via LLM analysis — suggest it at the end if the requires-graph looks sparse.

## Step 0: Locate the sidecar

1. Validate that the input `slug` matches a registered sidecar:
   ```bash
   skaile source sidecar list
   ```
   If the slug isn't in the list, ask the user to pick one from the printed
   table. If the table is empty, point them at:
   ```bash
   skaile source add <path> --sidecar
   ```
2. The sidecar lives at `~/.skaile/sources/<slug>/.skaile-source.yaml`.
   The companion lock file (`.skaile-source.lock.json`) and provenance
   (`README.md`) are next to it but are informational only — never hand-edit
   the lock file; it's rewritten on every rebuild.

## Step 1: Read current state

Pull the manifest into context:

```bash
skaile source sidecar show <slug>
```

Note what's already pinned:

- Top-level fields: `publisher_default`, `default_ref`, `publisher_overrides`, `sync`
- `assets:` count (should be > 0; if 0, the sidecar was created against an empty source — run `skaile source sidecar rebuild <slug>` first)
- Per-entry `sha256` presence (auto-generated sidecars always have them)
- Per-entry `requires:` — empty arrays are common; the deps-analyzer skill fills these in

## Step 2: Interview the user per entry

For each entry under `assets:`, walk through:

| Field | Question | Action if changed |
|---|---|---|
| `publisher` | Is `@<derived-domain>` correct, or should it map to a different publisher? Look at the project's naming convention. | Set the right publisher per entry, or set `publisher_default` + `publisher_overrides` at the top of the manifest. |
| `name` | Confirm the asset name. Defaults come from the asset's own manifest (frontmatter `name:` or parent dir). | Edit the entry's `name` field directly. |
| `version` | Is the declared `version:` the one to publish? Authors sometimes forget to bump pre-publish. | Fix the upstream asset's manifest first, then rebuild the sidecar (`skaile source sidecar rebuild <slug>`) — or override here for sidecar-only purposes. |
| `files` | Walk through `files[]`. Are there same-directory files NOT included that should be (e.g. a `validator.py` next to a SKILL.md)? Files that shouldn't ship (scratch notes, .DS_Store)? | Edit `files[]` directly — the sidecar is explicit (no wildcards). |
| `requires` | Are the declared cross-asset edges valid? Are there obviously-missing ones (e.g. a skill that loads `@x/y` but doesn't list it)? | Add by hand, or defer to `source-deps-analyzer` (recommended for non-trivial graphs). |
| `metadata` | Surface `description` / `keywords` / `tags` — does the user want to refine the human-facing copy before pinning? | Edit `metadata:` on the entry. |

Take notes (or write a draft replacement YAML) as you go.

## Step 3: Apply edits

Two options:

**Quick path: in-place edit.** Spawn `$EDITOR` and let the user finish the
edit by hand. The CLI validates + commits on save:

```bash
skaile source sidecar edit <slug>
```

`sidecar edit` validates the YAML on exit. If validation fails, the file is
not committed — the user can re-run `edit` to fix.

**Programmatic path: file write.** If you have a fully-resolved edit plan,
write `~/.skaile/sources/<slug>/.skaile-source.yaml` directly. Don't forget
to keep `version: 2` and the `assets:` block as the source of truth.

## Step 4: Verify

Run the strict verifier:

```bash
skaile source sidecar verify <slug> --strict
```

`--strict` exits non-zero on:
- `ManifestHashMismatch` (someone touched a referenced file after the hash was pinned)
- `ManifestFileMissing` (a `files[]` entry that no longer exists on disk)
- `ManifestSchemaError` (manifest file no longer parses)
- `Missing sha256` (entries without pinned hashes)

If hashes are stale because the user edited the upstream files since the
last rebuild, that's expected — run:

```bash
skaile source sidecar rebuild <slug> --merge
```

`--merge` preserves manually-edited `publisher_overrides`, `default_ref`, and
per-entry `metadata` while re-running glob discovery.

## Step 5: Commit + diff

If the user said `--no-commit` during `edit`, surface the diff and let them
decide:

```bash
skaile source sidecar git -- diff -- <slug>/.skaile-source.yaml
```

Then commit by hand:

```bash
skaile source sidecar git -- commit -m "chore(<slug>): curated by source-sidecar-curator"
```

Otherwise, the auto-commit (`chore(<slug>): edit via $EDITOR`) is already in
place. Confirm with:

```bash
skaile source sidecar git -- log --oneline -5
```

## Step 6: Hand off to deps-analyzer (optional)

If the requires-graph still looks sparse (most entries have empty
`requires: []` and the source clearly has cross-references in skill bodies),
suggest running:

```bash
# Conceptually:
#   - Reads each asset body + frontmatter
#   - Static pre-scan: markdown links to same-source skills, existing
#     requires:, `@publisher/name` mentions, code imports
#   - Single LLM prompt: "Given these N asset bodies, infer cross-refs"
#   - Writes proposals to a `deps-analysis-<timestamp>` branch on the
#     sidecar repo for review
```

The `source-deps-analyzer` skill drives that flow. After running it, the
user reviews the branch diff and merges or discards.

## Notes

- The sidecar repo at `~/.skaile/sources/` is shared across all slugs.
  Commits stay local until the user runs
  `skaile source sidecar git -- push`. Auto-commits never push.
- All sidecar-mutating commands accept `--no-commit` to batch multiple
  edits into one user-authored commit.
- Removing a sidecar (`skaile source sidecar remove <slug>`) clears the DB
  pointer; `--keep-files` leaves the directory in git history.
