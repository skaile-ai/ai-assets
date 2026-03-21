# TODO PESTER — Remaining Cleanup Issues

Last updated: 2026-03-22

## Frontmatter Consistency

~80 skills are missing `source` and/or `version` fields in their SKILL.md frontmatter.
Every SKILL.md should have:

```yaml
source: CF | SAXE | MERGED | MIGRATED
version: 1.0.0
```

**Action:** Audit all SKILL.md files and add missing fields. Domains affected:
- `dev-implementation-experts-js/` — most expert skills lack `source`/`version`
- `dev-implementation-experts-python/` — same
- `dev-implementation-experts-typst/` — same
- `ai-resource-management/` — same
- `dev-architecture/` — same
- `knowledge-research/` — same
- `knowledge-writing/` — same
- `use/` — same

## Experimental / Unclear Skills

### `screens-technical`
- Path: `dev-conceptualization/skills/screens-technical/`
- Status: Experimental variant of `screens`, not integrated into any flow or pipeline
- **Action:** Decide whether to integrate, merge into `screens`, or remove

### `storybook-types`
- Path: `dev-conceptualization/skills/storybook-types/`
- Status: PostXL-specific (generates typed Storybook bindings for PostXL components)
- **Action:** Move to a PostXL profile or expert skill, or generalize for other stacks

## DOMAIN.md Updates

Test skills (`test-plan`, `test-unit`, `test-integration`) were moved from `dev-quality` to `dev-implementation`.

- `dev-quality/DOMAIN.md` — remove test skill entries
- `dev-implementation/DOMAIN.md` — add test skill entries

## Legacy Directories

### `skills/00_orchestrator/`
- `dev-conceptualization/skills/00_orchestrator/` — old CF/Saxe orchestrator variants
- `dev-implementation/skills/00_orchestrator/` — old CF/Saxe orchestrator variants
- **Action:** Both are superseded by MERGED orchestrators. Verify no other files reference them, then delete.

### Old contracts directories
- `dev-conceptualization/contracts/` — check if still referenced or can be removed (dev-shared/contracts/ is canonical)
- `dev-implementation/contracts/` — same
- `dev-quality/contracts/` — same
- `dev-standards/contracts/` — same

## Contract References

Some skills may still reference old contract paths:
- `cf__shared/` → should be `dev-shared/contracts/`
- `_research/` → should be `_grounding/`
- Old flat `_concept/` paths (e.g., `03_features/`) → should use hierarchical paths (e.g., `2_experience/2_features/`)

**Action:** Grep for old paths across all SKILL.md files and update.

## Documentation References

After renaming `package.yaml` → `<name>.skillpack.yaml` and `<name>.json` → `<name>.flow.json`, update references in:
- `docs/concepts/domains.md` — references `package.yaml`
- `docs/developer-guide/index.md` — references `package.yaml`
- `CLAUDE.md` (skaile-dev root) — may reference old naming conventions
- CLI code (`skaile-agent-cli/`) — must handle `.skillpack.yaml` and `.flow.json` extensions

## Flow Schema

- `dev-conceptualization/flows/flow.schema.json` exists but may need updating after flow file renames to `*.flow.json`
- Verify CLI (`skaile-agent-cli/`) handles the `.flow.json` extension

## Nested Package

- `knowledge-research/skills/knowledge-writing/package.yaml` — `knowledge-writing` package is nested inside `knowledge-research`. Should it be a top-level domain instead?
