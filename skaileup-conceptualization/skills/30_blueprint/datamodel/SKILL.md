---
name: "datamodel"
description: "Use when features are approved but _concept/3_blueprint/3_datamodel/ is empty. Produces model.dbml (DBML), model.json (editor canvas), seed.json (test scenarios), and feature_map.json (model-to-feature cross-reference)."
metadata:
  version: "1.0.0"
  tags:
    - "data"
    - "schema"
    - "database"
    - "entities"
    - "relationships"
    - "dbml"
    - "model"
    - "seed"
    - "feature-map"
  source: "MERGED"
  prerequisites:
    files:
      - path: "_concept/2_experience/2_features"
        gate: hard
        description: "Features drive data model entities — features must exist first"
        min_entries: 1
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        gate: hard
        description: "Tech stack determines storage type and semantic type mapping"
    reads:
      - path: "_concept/_grounding/overview/user_input.json"
        description: "Complexity tier for model depth"
      - path: "_concept/2_experience/1_journeys/stories.json"
        description: "User journeys for data flow context"
      - path: "_concept/3_blueprint/2_architecture/architecture.md"
        description: "Architecture for service boundaries and data ownership"
      - path: "_concept/2_experience/4_behaviors"
        description: "Behavioral specs for entity state machine fields"
      - path: "_concept/_grounding/general/patterns.md"
        description: "Domain patterns for semantic type selection"
    produces:
      - path: "_concept/3_blueprint/3_datamodel/model.dbml"
        description: "Human-readable DBML entity definitions"
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        description: "Editor canvas state for visual data model"
      - path: "_concept/3_blueprint/3_datamodel/seed.json"
        description: "Scenario-based test data for all downstream testing"
      - path: "_concept/3_blueprint/3_datamodel/feature_map.json"
        description: "Model-to-feature cross-reference for readiness checks"
---

# Data Model

## Overview

The **datamodel** skill is the Data Model agent. It analyzes approved features and
produces a stack-independent data model using semantic types. It outputs:

- `model.dbml` — human-readable entity definitions (DBML syntax)
- `model.json` — editor-native format (visual canvas state)
- `seed.json` — scenario-based test data (empty, single_user, populated, edge_cases)
- `feature_map.json` — model-to-feature cross-reference

The model uses **semantic types** (from `skaileup-shared/contracts/semantic_types.md`), not
SQL types. Stack translation (Prisma, Directus schema, SQL DDL) is a separate on-request step.

## When to Use

- Features are approved but `_concept/3_blueprint/3_datamodel/` is empty
- User asks about entities, tables, relationships, database schema
- User says "data model", "what data do we need", "database design"

## When NOT to Use

- Features have not been written or approved yet — run **features** first
- User wants to change specific existing model fields — edit directly
- User wants a stack-specific schema output only — use stack translation as a follow-up

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md`,
`skaileup-shared/contracts/semantic_types.md`, `skaileup-shared/contracts/golden_principles.md`,
`skaileup-shared/contracts/feedback_loop.md`, and `skaileup-shared/contracts/seed_data.md`.

**Hard gates:**
- `_concept/2_experience/2_features/` must contain at least one feature file
- `_concept/3_blueprint/1_techstack/stack.md` must exist

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/1_discovery/1_overview/brief.md` | Yes |
| Must read | `_concept/2_experience/2_features/**/*.md` | Yes |
| Must read | `_concept/3_blueprint/1_techstack/stack.md` | Yes |
| Check if present | `_concept/_grounding/overview/user_input.json` | No (complexity) |
| Check if present | `_concept/2_experience/1_journeys/stories.json` | No |
| Check if present | `_concept/3_blueprint/2_architecture/architecture.md` | No |
| Check if present | `_concept/2_experience/4_behaviors/*.allium` | No |
| Check if present | `_concept/_grounding/general/patterns.md` | No |
| Never load | `_concept/2_experience/3_screens/`, `_concept/1_discovery/2_brand/` | — |

## Standalone Mode

**Gate check:** At least one feature file + stack.md must exist.
**On completion:** Present model summary and suggest next steps (screens, architecture).

---

ROLE  Data Model agent — derives entities, relationships, and enums from features;
      produces model.dbml, model.json, seed.json, and feature_map.json.

READS
  _concept/1_discovery/1_overview/brief.md                  — app name, audience
  _concept/2_experience/2_features/**/*.md                   — feature requirements
  _concept/3_blueprint/1_techstack/stack.md                 — stack constraints (for translation hints)
  ? _concept/_grounding/overview/user_input.json            — complexity field (drives involvement level)
  ? _concept/2_experience/1_journeys/stories.json            — EARS criteria → state machine derivation
  ? _concept/3_blueprint/2_architecture/architecture.md    — custom modules → additional entities
  ? _concept/2_experience/4_behaviors/*.allium              — entity state machines, enum values
  ? _concept/_grounding/general/patterns.md                — domain-specific data patterns

WRITES
  _concept/3_blueprint/3_datamodel/model.dbml       — human-readable DBML entity definitions
  _concept/3_blueprint/3_datamodel/model.json       — editor-native canvas format
  _concept/3_blueprint/3_datamodel/seed.json        — scenario-based test data
  _concept/3_blueprint/3_datamodel/feature_map.json — model → feature cross-reference
  _concept/2_experience/2_features/**/*.md           — feedback loop: data_entities[] populated

REFERENCES
  skaileup-shared/contracts/concept_structure.md   — valid _concept/ paths and naming rules
  skaileup-shared/contracts/semantic_types.md      — stack-independent field type catalog
  skaileup-shared/contracts/golden_principles.md   — naming rules: snake_case fields, PascalCase enums, _id suffix
  skaileup-shared/contracts/feedback_loop.md       — cross-reference protocol (features ↔ datamodel)
  skaileup-shared/contracts/seed_data.md           — scenario format and data quality rules
  references/model_conventions.md             — DBML + model.json template, naming rules, feature_map format

MUST  use semantic types from skaileup-shared/contracts/semantic_types.md — not SQL types
MUST  follow naming rules from golden_principles.md (snake_case fields, PascalCase enum values, _id relation suffix)
MUST  produce all four outputs: model.dbml, model.json, seed.json, feature_map.json
MUST  trace every entity back to at least one feature in feature_map.json
MUST  include all four seed scenarios (empty, single_user, populated, edge_cases)
MUST  update feature data_entities[] via feedback loop
NEVER  use SQL types (VARCHAR, INT, BOOLEAN) — only semantic types (string, integer, boolean)
NEVER  pre-populate screens[] or data_entities[] in features before this step
NEVER  load downstream artifacts (screens, brand)

EMIT  [datamodel] started run_id=<uuid>

STEP 1: Read context
  - Read brief.md for app name and domain
  - Read all feature files for functional requirements
  - Read stack.md for backend constraints (informs translation hints only)
  - Check _grounding/overview/user_input.json for complexity field
  IF _concept/2_experience/1_journeys/stories.json exists
    - Read stories.json; derive state machines from EARS acceptance criteria:
      - Event-driven criteria (WHEN ... THE SYSTEM SHALL ...) → state transitions
      - State-driven criteria (IF status is X ...) → guard conditions
      - Story downstream.candidate_entities hints → model candidates
  IF _concept/3_blueprint/2_architecture/architecture.md exists
    - Note custom modules → may need their own entities or configuration models
    - Note communication protocols → may need session, message, or event models
    - Note external integrations → may need connection or credential models
  IF _concept/2_experience/4_behaviors/*.allium exists
    - Read all .allium files for formal entity definitions, state enums, transition rules
    - Allium states → enum values; Allium relationships → model relations
  IF _concept/_grounding/general/patterns.md exists
    - Read domain-specific data patterns for entity structure and relationship guidance

STEP 1b: Determine involvement level
  - Read complexity from _grounding/overview/user_input.json (default: standard)
  IF complexity is "small"
    - > "I'll design the data model automatically based on your features. I'll show you a summary when done. Want to review the details instead?"
    - Proceed in automatic mode → skip to STEP 3
  IF complexity is "complex"
    - > "The data model shapes how your app stores information. I recommend we design it together — or I can propose something and you review?"
    - Default to involved mode → continue to STEP 2
  ELSE (standard)
    - > "Would you like to be involved in data model design, or should I handle it based on your features?"

STEP 2: Analyze features (involved mode)
  - For each feature identify: data created/read/updated/deleted, entities, relationships, enums
  - Ask the user:
    1. For each feature: what information does it need to store?
    2. How are things connected? (e.g., "a user has many tasks")
    3. Who should be able to see or change what? (roles/permissions)
  - If user is uncertain, analyze features and propose what is needed

STEP 3: Write DBML
  - $ mkdir -p _concept/3_blueprint/3_datamodel
  - Write model.dbml with semantic types (see references/model_conventions.md)

  OUTPUT _concept/3_blueprint/3_datamodel/model.dbml
    Use DBML syntax with semantic types. Example:
    Table user {
      id uuid [pk]
      email email [unique, not null]
      display_name string [not null]
      role user_role [default: 'Member']
      avatar image
      created_at datetime [not null]
    }
    Table task {
      id uuid [pk]
      title string [not null]
      status task_status [default: 'Todo']
      assigned_to_id uuid [ref: > user.id]
      due_date date
    }
    Enum user_role { Admin Member }
    Enum task_status { Todo InProgress Done }

STEP 4: Write model.json
  - Write model.json as the editor-native format (see references/model_conventions.md)

  OUTPUT _concept/3_blueprint/3_datamodel/model.json
    {
      "version": "1.0",
      "editor": { "viewport": { "x": 0, "y": 0, "zoom": 1 } },
      "entities": [{
        "id": "<snake_case_name>",
        "display_name": "<PascalCase>",
        "icon": "<icon>",
        "color": "<hex>",
        "position": { "x": N, "y": N },
        "from_features": ["2_experience/2_features/<group>/<feature>.md"],
        "fields": [
          { "id": "<entity>_<field>", "name": "<snake_case>", "type": "<semantic_type>", ... }
        ]
      }],
      "relationships": [{
        "id": "rel_<from>_<to>",
        "from": "<entity_id>", "from_field": "<field_id>",
        "to": "<entity_id>", "type": "m2o|o2m|m2m",
        "label": "...", "inverse_label": "...",
        "required": false, "on_delete": "set_null|cascade|restrict"
      }],
      "enums": [{
        "id": "<enum_id>",
        "values": [{ "value": "<PascalCase>", "label": "<human label>" }]
      }]
    }

STEP 5: Write seed data
  - Read skaileup-shared/contracts/seed_data.md for scenario format and quality rules
  - Write seed.json using canonical format (singular snake_case entity keys, PascalCase enum values)
  - Must include all four scenarios: empty, single_user, populated, edge_cases
  - Add "permissions" scenario if the app has role-based features
  - Include a dev/test user in single_user and populated scenarios
  - Use names from different locales, vary string lengths, include special characters
  - IDs must be consistent across entities (relations must resolve)

  OUTPUT _concept/3_blueprint/3_datamodel/seed.json
    See skaileup-shared/contracts/seed_data.md for full format and examples.

STEP 6: Write feature_map.json
  - Cross-reference every entity back to the feature(s) that require it

  OUTPUT _concept/3_blueprint/3_datamodel/feature_map.json
    {
      "<PascalCaseModel>": ["2_experience/2_features/<group>/<feature>.md", ...]
    }
  - Every entity must map to at least one feature; question any entity without a feature source

STEP 7: Update feature frontmatter (feedback loop)
  - For each feature, update its data_entities[] with the entity names that serve it:
    data_entities: [user, task]
  EMIT  [datamodel] feedback_loop updated <feature-path> set data_entities: [<entities>]

EMIT  [datamodel] checkpoint phase=datamodel_written entities=<N> enums=<N> features_updated=<N>

STEP 8: Human approval
  CHECKPOINT datamodel_approved
    Show business summary:
    > "Your app will keep track of [primary entities in plain language].
    > [Describe key relationships naturally]: Each [thing] can have multiple [related things].
    >
    > Technical summary:
    >   Entities: N, Relationships: N, Enums: N, Features updated: N
    >
    > Approve, or tell me what to change."

  UNTIL user explicitly approves

STEP 9: Stack translation (on request only)
  IF user asks "generate Prisma schema", "export to Directus", "SQL DDL", etc.:
    - Read stack.md to confirm the target
    - Use semantic_types.md translation table
    - Write stack-specific output (separate file, not a replacement for model.json)
    - Directus: collection snapshot JSON
    - Prisma: schema.prisma
    - Supabase: SQL migration
    - Raw SQL: PostgreSQL DDL

STEP 10: Hand off
  > "Data model approved. Next steps:
  > - Run `screens` to spec the UI (now data entities are defined)
  > - Run `concept-orchestrator` to continue the full pipeline"

EMIT  [datamodel] completed run_id=<uuid> entities=<N> relationships=<N> features_updated=<N>

CHECKLIST
  - [ ] _concept/3_blueprint/3_datamodel/model.dbml exists with semantic types
  - [ ] _concept/3_blueprint/3_datamodel/model.json exists with all entities and relationships
  - [ ] _concept/3_blueprint/3_datamodel/seed.json has all four scenarios (empty, single_user, populated, edge_cases)
  - [ ] _concept/3_blueprint/3_datamodel/feature_map.json exists — every entity traces to a feature
  - [ ] All field names are snake_case (golden_principles)
  - [ ] All enum values are PascalCase (golden_principles)
  - [ ] All relation fields use _id suffix (golden_principles)
  - [ ] Feature files updated with data_entities[] (feedback loop complete)
  - [ ] User has explicitly approved the data model

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Using SQL types (VARCHAR, INT) | Use semantic types only: string, integer, boolean, etc. Stack translation is a separate step. |
| Feature list = entity list | Features and entities are not 1:1. Multiple features share entities, and infrastructure entities (audit logs, sessions) serve no single feature. |
| Skipping behavioral specs | .allium files are authoritative for state machines. Enum values from allium must match exactly. |
| Empty seed.json | All four scenarios are required. Screens and E2E tests use them immediately. |
| Omitting feature_map.json | Every entity must trace to a feature. Entities without a source are likely unnecessary. |
| PascalCase or camelCase field names | Fields must be snake_case in the semantic layer (golden_principles). |

## Research Mode

Check before writing the model:
- `_concept/_grounding/general/patterns.md` — domain-specific data patterns, entity structure conventions

## Integration

- **Called by:** `concept-orchestrator` or standalone (after features)
- **Requires:** features + techstack
- **Feedback loop:**
  - Feature `data_entities[]` populated (forward link from datamodel)
- **Feeds into:** `screens` (entities inform Information Displayed), `seed` (uses seed.json), `scaffold` (uses model.json for entity generation)
