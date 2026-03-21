---
name: conceptualization-contract
description: "Shared contract for all dev-conceptualization skills. Describes _concept/ folder layout, skill naming conventions, pipeline phase structure, and file ownership rules. REQUIRED reading for any cf_* or saxe_* conceptualization skill."
metadata:
  type: system
  do_not_invoke: true
  stage: alpha
  requires: []
disable-model-invocation: true
---

# Conceptualization Domain — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `dev-conceptualization` skills read this before operating.

## Scope

This contract covers artifacts and conventions that are **conceptualization-specific**.
Structural conventions shared with `dev-implementation` live in `dev-shared/contracts/`.

## _concept/ Folder Layout

```
_concept/
├── 1_discovery/
│   ├── 1_overview/         ← brief.md, goals.md, comparable.md
│   ├── 2_research/         ← _grounding/ findings per step
│   └── 3_brand/            ← identity.md, tokens.json, behavioral.md
├── 2_experience/
│   ├── 1_journeys/         ← user journey maps (optional)
│   ├── 2_features/         ← 01_group/feature.md (numbered groups)
│   ├── 3_screens/          ← 00_layout/shell.md, 01_group/screen.md
│   └── 4_components/       ← component inventory (optional)
├── 3_blueprint/
│   ├── 1_techstack/        ← stack.md
│   ├── 2_architecture/     ← architecture.md
│   └── 3_datamodel/        ← model.dbml, model.json, model.schema.json
└── PLANS.md                ← pipeline progress (concept phase)
```

## Skill Naming Conventions

Skills live in numbered group directories under `skills/`:

| Prefix | Lineage | Example path |
|--------|---------|--------------|
| `cf_`  | Concept Forge | `skills/10_discovery/cf_overview/` |
| `saxe_` | Saxe platform | `skills/10_discovery/saxe_overview/` |
| `cf/` subdir | CF variant (pre-merge) | `skills/00_orchestrator/cf/` |
| `saxe/` subdir | Saxe variant (pre-merge) | `skills/00_orchestrator/saxe/` |

Once merged, a skill uses `source: MERGED` and lives in a flat named dir (no `cf/`/`saxe/` subdirs).

## Numbered Group Alignment

Feature groups and screen groups share the same numbering:

```
2_experience/2_features/01_user_auth/   ↔   2_experience/3_screens/01_user_auth/
```

Skills must preserve this alignment when creating or renaming groups.

## Pipeline Phase Structure

| Phase | Group | Skills involved |
|-------|-------|-----------------|
| Discovery | `10_discovery` | overview, research, brand-visual, brand-behavioral |
| Experience | `20_experience` | features, behaviors, screens, components, mock, journeys, storybook |
| Blueprint | `30_blueprint` | techstack, architecture, datamodel |
| Add feature | `40_add-feature` | add-feature (post-concept increments) |
| Reverse-engineer | `80_reverse-engineer` | reverse-engineer (existing repo entry) |
| Review | `90_review` | review (gardening + audit) |

## Cross-References

Two-way links are maintained:
- `features[].screens[]` ↔ `screens[].implements[]`
- `datamodel entities` → `features` (via YAML frontmatter `cross_refs`)

## Reads / Writes Protocol

Skills **read from lower-numbered groups** and **write to their own group only**.
No skill writes to a lower-numbered group's files.

## PLANS.md (Concept Phase)

Concept progress is tracked in `_concept/PLANS.md`:

```markdown
# Plans

## Concept Plan: <App Name>
### Settings
- Profile: <name>
- Research depth: skip | light | moderate | deep

### Progress
- [ ] overview — not started
- [x] features — completed YYYY-MM-DD
...

### Decisions
### Open Questions
### Blockers
```
