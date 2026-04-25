---
name: "skailup-add-feature"
description: "Use when adding a new feature or modifying an existing feature in a live concept. Surgically adds the feature spec, cascades changes through downstream artifacts (journeys, tech stack, architecture, data model, screens), and optionally triggers implementation if the app is already built."
metadata:
  version: "1.0.0"
  tags:
    - "add-feature"
    - "modify"
    - "extend"
    - "feature"
    - "change"
    - "update"
    - "cascade"
    - "iteration"
    - "incremental"
  source: "MERGED"
  parameters:
    depth:
      type: enum
      values: [none, light, medium, max]
      default: medium
  prerequisites:
    files:
      - path: "_concept/1_discovery/1_overview/brief.md"
        gate: hard
        description: "Project brief must exist — add-feature requires an existing concept"
      - path: "_concept/2_experience/2_features"
        gate: hard
        description: "At least one feature group must exist to add to"
        min_entries: 1
    inputs_required:
      - id: feature_description
        label: "What feature do you want to add or change?"
        type: text
        hint: "Describe it in plain language — what should the user be able to do?"
      - id: feature_mode
        label: "New feature or modification?"
        type: select
        options:
          - new
          - modification
    reads:
      - path: "_concept/2_experience/1_journeys/stories.json"
        description: "Existing journeys for cascade impact assessment"
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        description: "Tech stack for implementation cascade planning"
      - path: "_concept/3_blueprint/2_architecture/architecture.md"
        description: "Architecture for service boundary impact"
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        description: "Data model for entity cascade"
      - path: "_concept/2_experience/3_screens"
        description: "Existing screens to identify cascade targets"
    produces:
      - path: "_concept/2_experience/2_features"
        description: "New or modified feature spec file"
  user_inputs:
    dialog:
      - id: "feature_description"
        label: "What feature do you want to add or change?"
        type: "text"
        required: true
        hint: "Describe it in plain language — what should the user be able to do?"
      - id: "feature_mode"
        label: "New feature or modification?"
        type: "select"
        options:
          - "new"
          - "modification"
        required: true
    files: []
---

# Add Feature — Surgical Concept Modification

## Overview

The **add-feature** skill adds or modifies a single feature in an existing concept,
then cascades changes through all downstream artifacts that already exist.
It never touches artifacts that haven't been created yet.

## When to Use

- User says "add a feature", "add X to the concept", "I want a new feature for Y"
- User says "modify the login feature", "change the requirements for X"
- User wants to extend an existing concept with new or changed functionality

## When NOT to Use

- No concept exists yet — run `overview` and `features` first
- Adding many features at once — run `features` instead
- Wanting a full concept review — run `review`

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md`,
`skaileup-shared/contracts/feedback_loop.md`, and `skaileup-shared/contracts/golden_principles.md` before proceeding.

**Hard gates:**
- `_concept/1_discovery/1_overview/brief.md` must exist
- `_concept/2_experience/2_features/` must have at least one feature group

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/1_discovery/1_overview/brief.md` | Yes |
| Must read | `_concept/2_experience/2_features/**/*.md` | Yes |
| Check if present | `_concept/2_experience/1_journeys/stories.json` | No |
| Check if present | `_concept/1_discovery/2_brand/tokens.json` | No |
| Check if present | `_concept/3_blueprint/1_techstack/stack.md` | No |
| Check if present | `_concept/3_blueprint/2_architecture/architecture.md` | No |
| Check if present | `_concept/3_blueprint/3_datamodel/model.json` | No |
| Check if present | `_concept/3_blueprint/3_datamodel/model.dbml` | No |
| Check if present | `_concept/3_blueprint/3_datamodel/seed.json` | No |
| Check if present | `_concept/3_blueprint/3_datamodel/feature_map.json` | No |
| Check if present | `_concept/2_experience/3_screens/**/*.md` | No |
| Check if present | `_implementation/progress.json` | No (determines if Phase 4 runs) |

## Standalone Mode

**Gate check:** brief.md and at least one feature group must exist.
**On completion:** Present cascade summary and suggest next steps.

---

ROLE  Feature Addition agent — surgically adds or modifies a single feature and cascades
      changes through all existing downstream artifacts.

READS
  _concept/1_discovery/1_overview/brief.md              — app purpose, audience
  _concept/2_experience/2_features/**/*.md              — all existing features (names, groups, priorities)
  ? _concept/2_experience/1_journeys/stories.json       — user journey context
  ? _concept/1_discovery/2_brand/tokens.json            — brand tokens (for screen cascade)
  ? _concept/3_blueprint/1_techstack/stack.md           — tech stack
  ? _concept/3_blueprint/2_architecture/architecture.md — architecture
  ? _concept/3_blueprint/3_datamodel/model.json         — data model (canonical cross-ref)
  ? _concept/3_blueprint/3_datamodel/model.dbml         — data model (human-readable)
  ? _concept/3_blueprint/3_datamodel/seed.json          — seed data
  ? _concept/3_blueprint/3_datamodel/feature_map.json   — model-to-feature mapping
  ? _concept/2_experience/3_screens/**/*.md             — screen specs
  ? _implementation/progress.json                       — implementation status (determines if Phase 4 runs)

WRITES
  _concept/2_experience/2_features/<NN_group>/<feature>.md   — new or updated feature spec
  Cascades (only to artifacts that already exist):
    _concept/2_experience/1_journeys/stories.json
    _concept/3_blueprint/1_techstack/stack.md
    _concept/3_blueprint/2_architecture/architecture.md
    _concept/3_blueprint/3_datamodel/model.json + model.dbml + seed.json + feature_map.json
    _concept/2_experience/3_screens/<NN_group>/<screen>.md

REFERENCES
  skaileup-shared/contracts/concept_structure.md      — valid paths, naming rules
  skaileup-shared/contracts/frontmatter.md            — required YAML fields per file type
  skaileup-shared/contracts/feedback_loop.md          — cross-reference protocol
  skaileup-shared/contracts/golden_principles.md      — entity naming, numbering rules
  skaileup-shared/contracts/semantic_types.md         — stack-independent field types
  references/cascade_rules.md                    — per-artifact cascade details and cascade order
  references/feature_spec_template.md            — frontmatter template, discovery questions, impact template

MUST  read the full existing concept before making any changes
MUST  present impact assessment and get approval before writing feature spec
MUST  get approval before cascading to downstream artifacts
MUST  use bidirectional cross-references (feedback_loop.md)
NEVER cascade to artifacts that don't already exist
NEVER renumber existing feature groups
NEVER overwrite existing feature spec without showing the diff first
NEVER invent entities or screens that aren't needed by the feature
NEVER invent colors or fonts — consume from 1_discovery/2_brand/tokens.json

EMIT  [add-feature] started run_id=<uuid> mode=add|modify feature=<name>

STEP 1: Read existing concept
  - Read brief.md for app context
  - Read ALL files in _concept/2_experience/2_features/ (all groups, all specs)
  - Read each optional artifact that exists (journeys, techstack, architecture, model, screens)
  - Check if _implementation/progress.json exists (determines if Phase 4 runs)
  - Build a mental map of:
    - Existing feature groups (numbering, names, coverage)
    - Existing entities + relations (from model.json/model.dbml)
    - Existing screens (which features they serve)
    - Open cross-references and feedback loop state

STEP 2: Understand the request
  - Ask discovery questions (see references/feature_spec_template.md):
    - Is this a NEW feature or a MODIFICATION?
    - For new: which group? new group needed?
    - For modification: which feature file(s)?
    - What is the user's outcome / why is this needed?
    - What entities, screens, or behaviors are involved?
    - MVP scope or future scope?

STEP 3: Impact assessment
  - Analyze which downstream artifacts will be affected
  - Present impact assessment (use template in references/feature_spec_template.md):
    - Feature, group, priority
    - Journeys: new story / update downstream links
    - Tech stack: new dependency or no change
    - Architecture: new module / new integration / no change
    - Data model: new entities / new fields / new relation / no change
    - Screens: new screen / update existing / no change
    - Files to create / files to modify
  CHECKPOINT impact_assessment
    > "Review the impact assessment for [feature name].
    > Does this scope look right? Approve to proceed with writing the feature spec."
  EMIT  [add-feature] checkpoint phase=impact_assessed

STEP 4: Write feature spec
  IF new feature
    - If fits existing group, add file to that group folder
    - If new group needed, create next sequential numbered folder (NN_group_name)
    - Write feature file per references/feature_spec_template.md
    - Cross-check against existing features for overlap or conflicts
  ELSE modification
    - Read current feature file
    - Apply requested changes (requirements, roles, priority, scope)
    - Preserve existing screens: and data_entities: arrays
    - Update last_updated to today
  CHECKPOINT feature_spec
    > "Here's the feature spec: `_concept/2_experience/2_features/<group>/<feature>.md`
    > Approve to proceed with cascade updates, or request changes."
  EMIT  [add-feature] checkpoint phase=feature_spec mode=add|modify feature=<name> group=<NN_group>

STEP 5: Cascade changes (only to existing artifacts)
  - Follow cascade order from references/cascade_rules.md:
    1. Journeys → 2. Tech Stack → 3. Architecture → 4. Data Model → 5. Screens
  IF stories.json exists AND feature introduces new user flows
    - Add stories to appropriate stage, write EARS criteria, update downstream links
  IF stack.md exists AND feature needs new dependency/integration
    - Add to Additional Integrations section, update last_updated
  IF architecture.md exists AND feature needs new modules or protocols
    - Update affected sections + frontmatter arrays (custom_modules, protocols, external_integrations)
  IF model.json/model.dbml exists AND feature needs new entities or fields
    - Update model.dbml (add tables, fields, relations using semantic types)
    - Update model.json (keep in sync with DBML)
    - Update seed.json (add example data for new entities in affected scenarios)
    - Update feature_map.json (add mappings for new models → feature paths)
    - Feedback loop: update feature spec data_entities: array
  IF screens/ exists AND feature needs new or modified screens
    - Write new screen specs per merged screens skill conventions (plain language, no component names)
    - Update shell.md navigation if new top-level route
    - Update affected existing screen specs
    - Feedback loop: update feature spec screens: array + screen implements: arrays
  CHECKPOINT cascade
    > "Review the cascade changes. Approve to continue, or request adjustments."
  EMIT  [add-feature] checkpoint phase=cascade cascade_updates=<N>

STEP 6: Quality gate
  - Verify all cross-references are bidirectional and valid
  - Verify all new entities follow golden_principles.md naming (snake_case ids, semantic types)
  - Verify seed.json uses singular snake_case keys and PascalCase enum values
  - Verify no files created outside the impact assessment scope
  - Report quality status; fix any issues found

  Present cascade summary (see references/feature_spec_template.md):
    Feature | mode | cascade step counts | feedback loop status

  EMIT  [add-feature] checkpoint phase=quality cross_refs=valid|N_broken

STEP 7: Optional implementation (if app already built)
  IF _implementation/progress.json exists
    > "The app has an existing implementation. Do you want to implement this feature now,
    > or save it for the next implementation run?"
    IF yes → hand off to `implement-feature` skill with the feature spec as context
    IF no → note the feature in PLANS.md as implementation backlog

EMIT  [add-feature] completed run_id=<uuid> feature=<group>/<feature> cascade_updates=<N> implemented=true|false

CHECKLIST
  - [ ] Full existing concept read before making changes
  - [ ] Impact assessment presented and approved
  - [ ] Feature spec written and approved
  - [ ] Cascade applied in correct order (Journeys → Techstack → Architecture → Datamodel → Screens)
  - [ ] All cross-references bidirectional and valid
  - [ ] seed.json updated if data model changed (singular snake_case keys, PascalCase enum values)
  - [ ] feature_map.json updated if new entities added
  - [ ] User has explicitly approved cascade

---

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip this skill entirely |
| `light` | Quick scan — high-level issues only |
| `medium` | Standard review — all sections checked, fixes suggested (default) |
| `max` | Deep audit — cross-reference validation, consistency checks, improvement suggestions |

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Cascading to artifacts that don't exist yet | Only cascade to existing files — check existence first |
| Renumbering existing feature groups | Never change existing group numbers — add new groups at end |
| Overwriting feature spec without showing diff | Always show what's changing before writing |
| Using PascalCase seed entity keys | Use singular snake_case keys per golden_principles.md |
| Adding `status` field to feature frontmatter | `status` is globally removed from all markdown frontmatter |
| Inventing colors or fonts in screen specs | Read tokens.json — never invent brand values |
| Cascading all 5 steps even when not needed | Only cascade to affected artifacts; skip if no change needed |

## Integration

- **Called by:** user directly or `concept-orchestrator` during iteration
- **Requires:** `brief.md`, at least one feature group
- **Cascades into:** all existing downstream concept artifacts
- **Feeds into:** `implement-feature` (optional Phase 4)
