---
name: migrate-skaile-manifest
description: "Use when you need to migrate a legacy skaile.yaml (schema 4.x and earlier, using `repositories:` or `ai_resources:`) into the new canonical-identity shape (`sources:` + `stores:` + canonical dep refs `kind:@<publisher>/name[#pin]`). Triggers: 'migrate skaile.yaml', 'convert manifest', 'schema 4.x migration', 'update to canonical-identity', 'fix repositories: error', 'fix ai_resources: error', '@skaile to @skaile-ai rebrand'."
license: MIT
metadata:
  author: skaile-ai
  tags:
    - manifest
    - migration
    - skaile.yaml
    - canonical-identity
    - sources
    - stores
    - publisher
  stage: alpha
  source: NEW
  user_inputs:
    dialog:
      - id: target_path
        label: "Path to the skaile.yaml to migrate"
        type: text
        required: true
        default: "skaile.yaml"
      - id: write_in_place
        label: "Write changes back to disk (otherwise dry-run)"
        type: boolean
        required: false
        default: true
---

# Migrate skaile.yaml to canonical-identity

## Purpose

The manifest schema changed in `@skaile/workspaces` 4.x. The canonical source of
truth is the spec at `_devlog/specs/2026-05-31-manifest-canonical-identity.md` in
the workspaces repo. The old schema declared dependencies via per-project
`repositories:` maps or `ai_resources:` lists; the new schema moves *identity*
into a `(publisher, kind, name, version)` tuple, with sources declared under
`sources:`, optional catalog fallbacks under `stores:`, and dependencies written
as canonical refs `kind:@<publisher>/name[#pin]`.

Your job is to rewrite the user's `skaile.yaml` into the new shape. The
`metadata.version` strip in source SKILL.md files is a **separate concern** owned
by source authors, not by consumers — do not touch SKILL.md frontmatter when
migrating a *consumer* manifest.

## Mapping rules

```
── Sources ───────────────────────────────────────────────
legacy `repositories: { <name>: { url, branch? } }`
  → sources: [{ url, pin: <branch>? }]
    • drop <name> (was a project-local slug)
    • move `branch:` to `pin:` (canonical name)

legacy `ai_resources: [{ name, path, branch?, dependencies? }]`
  → sources: [{ url: <path>, pin: <branch>? }]
  → top-level dependencies: [...lifted from each entry...]
    • each lifted dep is scoped to the publisher inferred from `path`

legacy `ai_resources: { sources: [...], requires: [...] }` (legacy object shape)
  → same as the array form; `requires:` becomes top-level `dependencies:`
    scoped to the inferred publisher of the single source.

── Dep refs ──────────────────────────────────────────────
bare `skill:foo`
  → `skill:@<publisher>/foo` where <publisher> = inferred GitHub <org-or-user>
    of the source URL. If multiple sources come from different GitHub orgs,
    ASK the user which publisher owns this dep.

`kind:name@skaile` or `kind:@skaile/name`  (legacy curated namespace)
  → `kind:@skaile-ai/name`  (brand rebrand)

legacy `kind:name@<publisher>` (the pre-scoped form)
  → `kind:@<publisher>/name`; the `#pin` suffix is kept.

`bundle:<name>@<publisher>[#pin]`  (the bundle kind no longer exists)
  → `preset:@<publisher>/<name>`; DROP the `#pin` and warn: presets are
    versioned on their own (starting at 1.0.0), so a bundle-era range such as
    `#^2.0` can never match.

pre-existing canonical refs already in the new shape pass through unchanged.

── Publication half (only if this repo IS also a source) ─
write `skaile.manifest.yaml` (the publisher manifest; a consuming project's own
file stays `skaile.yaml`):
  publisher: <inferred-from-URL-or-explicit>   # no leading @
  # version: omitted — git tag waterfall
  # no `assets:` inventory — it would switch discovery off the tree walk

── SKILL.md frontmatter ──────────────────────────────────
strip `metadata.version:` from every SKILL.md in this repo.
(Other metadata.* keys stay.)
```

## Interactive disambiguation rules (MUST)

Three cases you CANNOT silently guess:

- **(a) Bare dep, multiple GitHub orgs in `sources:`.** Stop. Print the dep, list
  the candidate publishers (each source's inferred GH org), ask the user to pick.
  Do **not** pick the first source by default.
- **(b) Source URL is not on `github.com`.** Print the URL, ask the user for the
  canonical publisher string (must be GitHub-shaped: ≤39 chars, alphanumeric +
  hyphens). If they refuse, refuse the migration with a clear error.
- **(c) SKILL.md `name:` disagrees with its parent directory name.** Directory
  name wins. Print both, ask the user to confirm before editing the SKILL.md
  `name:` field.

## Workflow

1. Read the target `skaile.yaml`. Identify which legacy keys are present
   (`repositories:`, `ai_resources:` array form, `ai_resources:` object form).
2. Plan the rewrite: list every change you will make. Ask the user to approve
   (when `write_in_place: true`).
3. For each ambiguity → follow the disambiguation rules above.
4. Invoke `scripts/migrate.py --in <path> --out <path>` for the mechanical
   rewrite. The script handles YAML parsing, comment preservation via
   `ruamel.yaml`, and deterministic rule application. Pass resolved ambiguities
   via `--publisher-map <json>` (a JSON object mapping each source URL to its
   canonical publisher, plus an optional `_bare_default` key for bare deps).
5. Print a diff of the result.
6. If `write_in_place: true`, write the new YAML and (when applicable, for a
   *source* repo) the SKILL.md frontmatter edits.

## Reference

See [`references/mapping-rules.md`](references/mapping-rules.md) for the full
table, including pre-release SemVer edge cases and worked examples for each of
the three ambiguity cases.

## Auto-improve

When the user hits a pain point the rules don't cover, capture it in
`resources/improvement_ideas.md` for next-iteration triage (standard
`skill-builder` pattern).

## Smoke Test

Verify the script end-to-end:

```bash
cd .../migrate-skaile-manifest
uv run scripts/migrate.py \
  --in scripts/smoke_fixture/skaile.legacy.yaml \
  --out /tmp/migrated.yaml \
  --publisher-map '{"_bare_default": "skaile-ai"}'
diff -u scripts/smoke_fixture/expected.yaml /tmp/migrated.yaml
```

Empty diff = green. `npm test` runs the same check
(`scripts/migrate-skaile-manifest.test.sh` at the repo root; needs `uv`).
Update `expected.yaml` only when the spec's mapping
rules change; never to paper over a regression.
