---
name: add-feature
description: "Use when adding a new feature or modifying an existing feature in a live concept. Surgically adds the feature spec and cascades changes through downstream artifacts (data model, screens, architecture, tech stack). Can also trigger implementation if the app is already built."
keywords: [add-feature, modify, extend, feature, change, update, cascade, iteration]
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

# Add Feature — Surgical Concept Modification

## Role

You are a Feature Addition agent. You add or modify a single feature in an existing
concept, then cascade changes through all downstream artifacts that already exist.
You never touch artifacts that haven't been created yet.

## Reads

- `_concept/01_project/brief.md` ← required
- `_concept/03_features/` ← all existing features (required)
- `_concept/02_journeys/stories.json` ← optional (if journeys were run)
- `_concept/04_brand/tokens.json` ← optional
- `_concept/05_techstack/stack.md` ← optional
- `_concept/05b_architecture/architecture.md` ← optional
- `_concept/06_datamodel/model.json` ← optional
- `_concept/06_datamodel/model.dbml` ← optional
- `_concept/06_datamodel/seed.json` ← optional
- `_concept/07_screens/` ← all existing screens (optional)
- `_concept/_grounding/` ← any existing research (optional)

## Writes

Writes only to artifacts that already exist:
- New feature spec: `_concept/03_features/{group}/{feature}.md`
- Modified feature spec: updates in-place with change log
- Cascaded artifacts: `05b_architecture/`, `06_datamodel/`, `07_screens/`, `02_journeys/`

## References

- `references/cascade_rules.md` — which artifacts to cascade and how
- `references/feature_spec_template.md` — feature frontmatter + section format
- `shared/contracts/concept_structure.md` — _concept/ paths and naming rules
- `shared/contracts/feedback_loop.md` — cross-reference protocol
- `shared/contracts/golden_principles.md` — entity naming, numbering rules

## Hard Gates

- `_concept/03_features/` must exist (at least one feature group)
- `_concept/01_project/brief.md` must exist

## Workflow

### STEP 1 — Read Existing Concept

READ ALL files in `_concept/03_features/` (all groups, all feature specs)
? READ `_concept/02_journeys/stories.json`
? READ `_concept/05_techstack/stack.md`
? READ `_concept/05b_architecture/architecture.md`
? READ `_concept/06_datamodel/model.json`
? READ `_concept/07_screens/` (all screens)

Build a mental map of:
- Existing feature groups (numbering, names, what they cover)
- Existing entities + relations (from model.json/model.dbml)
- Existing screens (which features they serve)
- Any open cross-references or feedback loop entries

### STEP 2 — Understand the Request

Ask the user:
- Is this a **new feature** or **modification to an existing feature**?
- For new: which group does it belong to (or is a new group needed)?
- For modification: which feature file(s) are affected?
- What is the user's outcome / why is this feature needed?
- What entities, screens, or behaviors are involved?
- Is this MVP scope or future scope?

### STEP 3 — Impact Assessment

Before writing anything, produce an impact assessment:

```
## Impact Assessment: <Feature Name>

**Type:** New feature | Modification
**Group:** <existing group> | New group: <name>
**Priority:** must-have / should-have / could-have

**Downstream impact:**
- Journeys: [new story? | update downstream links on stories: X, Y]
- Data model: [new entities: X | new fields on: Y | new relation: X → Y]
- Architecture: [new module | new external integration | no change]
- Tech stack: [new dependency: X | no change]
- Screens: [new screen: X | update existing: Y | no change]
- Seed data: [new scenario | update populated scenario | no change]

**Files to create:** <list>
**Files to modify:** <list>
```

CHECKPOINT
> "Here is the impact assessment for [feature name]. Does this scope look right?
> Approve to proceed with writing the feature spec."

### STEP 4 — Write Feature Spec

Create or update the feature spec file following the template:

**Path:** `_concept/03_features/<group>/<feature>.md`

```markdown
---
last_updated: YYYY-MM-DD
status: draft
priority: must-have | should-have | could-have
roles: [role1, role2]
screens: []
data_entities: []
stories: []
---

# <Feature Name>

## Description
<What this feature does>

## User Benefit
<Why a user needs this feature, linked to a story outcome if applicable>

## Requirements
- <specific requirement>
- <specific requirement>

## Success Criteria
- <measurable success criterion>
- <measurable success criterion>

## Error States
- <error condition>: <expected system behavior>
```

If a `stories.json` exists, link the feature to relevant story IDs in `stories: []`.

MUST follow `shared/contracts/golden_principles.md` for naming and numbering.
MUST follow `shared/contracts/feedback_loop.md` for cross-reference fields.

CHECKPOINT
> "Feature spec written: `_concept/03_features/<group>/<feature>.md`
> Ready to cascade changes to [list of affected artifacts].
> Approve to proceed."

### STEP 5 — Cascade Changes

For each artifact that already exists AND is affected (see `references/cascade_rules.md`):

**5a — Journeys** (if `02_journeys/stories.json` exists and new flow introduced)
- Add new story(ies) to the appropriate stage
- Write EARS criteria for each new story
- Update `downstream.candidate_features` on relevant existing stories

**5b — Tech Stack** (if `05_techstack/stack.md` exists and new dependency needed)
- Add new integration/library to stack.md
- Document rationale

**5c — Architecture** (if `05b_architecture/architecture.md` exists and structural change)
- Update affected modules, data flows, or infrastructure sections
- Add new module if required by the feature

**5d — Data Model** (if `06_datamodel/` exists and new entities/fields needed)
- Update `model.dbml` — add tables, fields, relations
- Update `model.json` — keep in sync with DBML
- Update `seed.json` — add example data for new entities
- Back-link: update feature spec `data_entities: []` field

**5e — Screens** (if `07_screens/` exists and new or modified screens needed)
- Write new screen specs following `07_screens/<group>/<screen>.md` format
- Update `shell.md` navigation if new top-level route
- Update affected existing screen specs
- Back-link: update feature spec `screens: []` field
- Forward-link: update screen spec `implements:` field

After each cascade step, emit a checkpoint update:
```
[add-feature] checkpoint
  cascade_step: <name>
  files_modified: [list]
```

### STEP 6 — Quality Gate

Run self-check:
- [ ] Feature spec has required frontmatter fields
- [ ] All new entities follow golden_principles.md naming
- [ ] All cross-references are bidirectional (feature↔screen, feature↔model)
- [ ] No orphaned references (screens that no longer exist, etc.)
- [ ] seed.json updated if model changed
- [ ] No files created that weren't in the impact assessment

EMIT audit_pass | audit_fail
  checks: <count>
  issues: [list if any]

### STEP 7 — Optional Implementation (If App Already Built)

IF `_concept/PLANS.md` exists with implementation progress:
1. Ask: "Do you want to implement this feature now, or save for the next implementation run?"
2. If yes: hand off to `implement/feature` skill with the new feature spec as context
3. If no: update PLANS.md to add the feature to the implementation backlog

EMIT completed
  skill: add-feature
  run_id: <uuid>
  feature: <feature name>
  group: <group name>
  cascaded: [list of modified artifacts]

## Constraints

MUST read ALL existing features before writing anything.
MUST present impact assessment and get approval before writing feature spec.
MUST get approval before cascading to downstream artifacts.
MUST use bidirectional cross-references (feedback_loop.md).
NEVER create pipeline steps that don't exist yet (only cascade to existing artifacts).
NEVER renumber existing feature groups.
NEVER overwrite an existing feature spec without showing the diff first.
NEVER invent entities or screens that aren't needed by the feature.

EMIT started
  skill: add-feature
  run_id: <uuid>
