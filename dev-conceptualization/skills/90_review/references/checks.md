# Audit Checks Reference

Detailed check tables and severity classifications for the review skill.

## Pipeline Structure Checks (Step 1)

Uses `dev-shared/contracts/concept_structure.md` for expected phase-grouped paths.

| Check | How | Severity if failing |
|-------|-----|---------------------|
| Folder exists | `_concept/<phase_folder>/` present | HIGH |
| Has content | At least one expected output file exists | HIGH |
| Frontmatter valid | All `.md` files have `---` delimiters + required fields | MEDIUM |

### Expected Folders

| Phase folder | Required output | Optional |
|---|---|---|
| `1_discovery/1_overview/` | `brief.md`, `goals.md` | `comparable.md` |
| `1_discovery/2_brand/` | `identity.md`, `tokens.json` | `behavioral.md`, `copy_guidelines.md`, `brandbook.html` |
| `2_experience/1_journeys/` | `stories.json` | `journey_stages.md` |
| `2_experience/2_features/` | At least one feature group `NN_*/` | — |
| `2_experience/3_screens/` | At least one screen group `NN_*/` | `components/` |
| `3_blueprint/1_techstack/` | `stack.md` | — |
| `3_blueprint/2_architecture/` | `architecture.md` | — |
| `3_blueprint/3_datamodel/` | `model.json`, `model.dbml` | `feature_map.json`, `seed.json` |

---

## Frontmatter Compliance (Step 2)

For every `.md` file in `_concept/`, verify against `dev-shared/contracts/frontmatter.md`:

| Check | Field | Severity |
|-------|-------|----------|
| YAML delimiters present | `---` open and close | HIGH |
| Date field present | `last_updated` | MEDIUM |
| Date format valid | `YYYY-MM-DD` | LOW |
| **Status field ABSENT** | No `status:` field — globally removed | MEDIUM |

### File-type-specific fields

| File type | Required fields | Severity if missing |
|---|---|---|
| Feature files | `priority`, `story_refs`, `roles`, `screens`, `data_entities`, `last_updated` | HIGH |
| Screen files | `implements`, `data_entities`, `layout`, `last_updated` | HIGH |
| Architecture | `apps`, `custom_modules`, `protocols`, `external_integrations`, `last_updated` | MEDIUM |
| Stack | `platform`, `frontend`, `backend`, `database`, `tech_stack_skill`, `last_updated` | MEDIUM |
| Identity | `mood`, `mode`, `last_updated` | MEDIUM |

> **Note:** `story_refs` (not `stories`) is the canonical field name for feature files.

---

## Golden Principles (Step 3)

For every applicable rule in `dev-shared/contracts/golden_principles.md`:

| Rule | How to check | Severity |
|------|-------------|----------|
| Entity IDs: `snake_case` | Check entity names in `model.json` | HIGH |
| Field names: `snake_case` | Check field names in `model.json` | HIGH |
| Enum values: `PascalCase` | Check enum definitions in `model.json` | MEDIUM |
| Relation fields: `_id` suffix | Check foreign-key fields in `model.json` | MEDIUM |
| Feature groups: sequential | Scan `2_experience/2_features/` group numbers — no gaps | HIGH |
| Screen groups mirror features | Screen group numbers match feature group numbers | HIGH |
| Every feature has ≥1 requirement | Feature body has at least one `- [ ]` checkbox | MEDIUM |
| All frontmatter paths resolve | Every path in `screens:`, `implements:`, `data_entities:` exists | HIGH |
| Seed keys: singular `snake_case` | Check `seed.json` top-level scenario entity keys | MEDIUM |

---

## Cross-Reference Integrity (Step 4)

### Features ↔ Screens

For every feature with `screens:` entries:
- Does each referenced screen file exist?
- Does that screen's `implements:` list this feature?

For every screen with `implements:` entries:
- Does each referenced feature file exist?
- Does that feature's `screens:` list this screen?

| Result | Severity |
|--------|----------|
| Screen listed in feature but file missing | HIGH |
| Screen not listing feature in `implements:` | HIGH |
| Feature listed in screen but file missing | HIGH |
| Feature not listing screen in `screens:` | HIGH |

### Data Model ↔ Feature Map

If `feature_map.json` exists:
- For each entity mapped, do the referenced feature files exist?

If `model.json` exists:
- For each entity, is there a corresponding entry in `feature_map.json`?

| Result | Severity |
|--------|----------|
| Feature path in `feature_map.json` missing | HIGH |
| `model.json` entity with no `feature_map.json` entry | MEDIUM |

---

## Entropy Indicators (Step 5)

| Indicator | Condition | Label | Severity |
|-----------|-----------|-------|----------|
| Stale file | `last_updated` > 30 days old | STALE | LOW |
| Missing link | Feature with empty `screens: []` when `2_experience/3_screens/` exists | MISSING LINK | MEDIUM |
| Orphaned entity | `model.json` entity with no `feature_map.json` entry | ORPHANED ENTITY | MEDIUM |
| Unexpected file | File outside expected `_concept/` structure | UNEXPECTED FILE | LOW |
| Group mismatch | Feature group without matching screen group | GROUP MISMATCH | HIGH |
| Plan drift | `PLANS.md` progress out of sync with actual `_concept/` state | PLAN DRIFT | MEDIUM |
| Stale status field | Any file has `status:` field present — globally removed | STALE STATUS FIELD | MEDIUM |

> **Stale Status Field**: The `status` field was globally removed from all concept frontmatter. Its presence in any file is now an issue to clean up, not a missing field to add.

---

## Severity Classification

| Severity | Definition | Blocks pipeline? |
|----------|------------|-----------------|
| CRITICAL | Missing required output file; broken data model; corrupt JSON | Yes — immediately |
| HIGH | Broken cross-references; missing required frontmatter; group gaps | Yes — score < 70 |
| MEDIUM | Missing optional fields; stale status fields; orphaned entities | No — but degrade score |
| LOW | Stale dates; unexpected files; formatting issues | No |
