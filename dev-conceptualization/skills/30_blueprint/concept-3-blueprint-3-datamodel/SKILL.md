---
name: concept-3-blueprint-3-datamodel
description: "Step 9: PostXL data model. Reads features and produces postxl-schema.json (schema) + seed.json (test data). The schema is consumed by PostXL code generators. Seed data uses named scenarios for mockups and testing."
keywords: data, schema, database, entities, relationships, postxl, prisma
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

ROLE  Data Model agent — produces postxl-schema.json, seed.json, and feature_map.json from features and architecture.

READS
  _concept/1_discovery/1_overview/brief.md                    — app name, slug, audience
  _concept/2_experience/2_features/**/*.md                    — requirements per feature
  _concept/3_blueprint/2_architecture/architecture.md       — custom modules, protocols, integrations

WRITES
  _concept/3_blueprint/3_datamodel/postxl-schema.json  — PostXL schema (models, fields, relations, enums, auth, actions)
  _concept/3_blueprint/3_datamodel/seed.json           — scenario-based test data (backend-compatible format)
  _concept/3_blueprint/3_datamodel/feature_map.json    — model-to-feature cross-reference mapping

REFERENCES
  shared/contracts/concept_structure.md          — valid _concept/ paths and naming rules
  shared/contracts/frontmatter.md                — feature frontmatter fields
  shared/contracts/semantic_types.md             — PostXL field type catalog (Prisma-based)
  shared/contracts/feedback_loop.md              — cross-reference protocol (features ↔ datamodel)
  shared/contracts/seed_data.md                  — scenario format and data quality rules
  references/schema_conventions.md      — schema template, naming rules, seed format, allium mapping
  concept/concept/references/complexity_tiers.md      — tier definitions and phase behavior
  concept/concept/references/technical_involvement.md — involvement choice protocol

REQUIRES
  hard: pxl (schema validation)
  state: _concept/1_discovery/1_overview/brief.md exists
  state: _concept/2_experience/2_features/ contains at least one .md file

MUST  validate postxl-schema.json with pxl before proceeding
MUST  verify every labelField points to a String-type field
MUST  include User records in seed.json for every referenced user ID
MUST  include a dev user (sub: "test") in seed.json
MUST  trace every model back to at least one feature in feature_map.json
MUST  define schemas with role-based auth (read/write/adminUi) and assign every model to a schema
MUST  include a PXL schema for system internals
NEVER  manually define id, createdAt, or updatedAt — use standardFields array
NEVER  define fields for standard models — PostXL generates them automatically
NEVER  use camelCase model keys or PascalCase field names in seed.json

EMIT  [concept-3-blueprint-3-datamodel] started run_id=<uuid>

STEP 1: Read context
  - Read brief.md for app name, slug, audience, complexity_tier (default: standard)
  - Read all _concept/2_experience/2_features/**/*.md for requirements
  - Read _concept/3_blueprint/2_architecture/architecture.md for custom modules, protocols, integrations
  - $ mkdir -p _concept/3_blueprint/3_datamodel
  - Derive enums and state machines from feature requirements and journey acceptance criteria

STEP 1b: Determine involvement level
  IF complexity_tier is small
    - > "I'll design the data model automatically based on your features. I'll show you a summary when done. Want to review the details instead?"
    - Default to automatic mode
  IF complexity_tier is complex
    - > "The data model shapes how your app stores and connects information. I recommend we design it together. Or I can propose something and you review?"
    - Default to involved mode
  ELSE (standard)
    - > "Would you like to be involved in data model design, or should I handle it based on your features?"

  IF automatic mode
    - Derive entities, relationships, and enums from features without asking the 3 questions
    - Skip to STEP 3 (determine standard models)
  ELSE (involved mode)
    - Continue to STEP 2

STEP 2: Analyze features (involved mode)
  - For each feature identify: data created/read/updated/deleted, entities, relationships, enums
  - Ask the user to confirm or clarify:
    1. For each feature: what information does it need to keep track of?
    2. How are things connected? (e.g., "a user can attend many events", "each session belongs to one event")
    3. Who should be able to see or change what? (e.g., "only admins can delete events")

STEP 3: Determine standard models
  - Identify which standard models the app needs: User, Action, ActionOperation, Comment, File, TableView, Config
  - List required ones in the standardModels array
  - Do NOT define fields for standard models

STEP 3b: Define schemas and role-based access
  - Define database schemas that group models by domain concern
  - Every schema MUST include PXL (system internals) plus domain-specific schemas
  - Each schema defines auth rules: read (who can query), write (who can mutate), adminUi (who sees admin panels)
  - Assign every model to a schema via its "schema" property
  - Schema design guidelines:
    - Group models by access pattern and domain boundary, not just by count
    - Configuration/setup entities (org settings, templates, infrastructure) → admin-only write
    - User-facing work data (projects, conversations, user content) → all roles write
    - System/runtime infrastructure (deployments, containers, jobs) → admin-only write
  - Example schema structure:
    ```json
    "schemas": [
      {
        "name": "PXL",
        "auth": {
          "read":    { "anyRole": ["<all roles>"] },
          "write":   { "anyRole": ["<admin roles>"] },
          "adminUi": { "visibleFor": ["<admin roles>"] }
        }
      },
      {
        "name": "<DomainSchema>",
        "auth": { ... }
      }
    ],
    "defaultSchema": "<most common schema>"
    ```
  - Also define a systemUser for automated operations:
    ```json
    "systemUser": { "name": "System", "email": "system@<app-domain>", "sub": null }
    ```

STEP 4: Write postxl-schema.json
  - See references/schema_conventions.md for full template and naming rules
  OUTPUT _concept/3_blueprint/3_datamodel/postxl-schema.json
    {
      "name": "<app-slug>",
      "slug": "<app-slug>",
      "description": "<from brief.md>",
      "projectType": "standalone",
      "version": "0.1.0",
      "schemas": [<defined schemas with auth>],
      "defaultSchema": "<default>",
      "systemUser": { "name": "System", "email": "system@<domain>", "sub": null },
      "standardModels": [<selected standard models>],
      "models": { "<Model>": { "schema": "<SchemaName>", "fields": { ... }, "labelField": "<string-field>" } }
    }

STEP 5: Validate schema
  RUN  pxl validate _concept/3_blueprint/3_datamodel/postxl-schema.json
  UNTIL pxl validate passes
    - Fix reported issues: missing required fields, invalid types, broken relations, duplicates
    - Re-run validation
  - After pxl passes, verify every labelField references a String-type field
  - If labelField points to enum or non-string: change labelField or add a new String field

STEP 6: Write seed data
  - Read shared/contracts/seed_data.md for scenario format and quality rules
  - Generate seed.json in backend-compatible format (see references/schema_conventions.md)
  OUTPUT _concept/3_blueprint/3_datamodel/seed.json
    {
      "empty": {},
      "single_user": { "users": [...] },
      "populated": { "users": [...], "<entities>": [...] },
      "edge_cases": { ... }
    }

STEP 7: Write feature_map.json
  OUTPUT _concept/3_blueprint/3_datamodel/feature_map.json
    {
      "<Model>": ["2_experience/2_features/<group>/<feature>.md", ...]
    }

STEP 8: Update feature frontmatter (feedback loop)
  - For each feature, update data_entities array with PascalCase model names
  EMIT  [concept-3-blueprint-3-datamodel] feedback_loop updated <feature-path> set data_entities: [<models>]

EMIT  [concept-3-blueprint-3-datamodel] checkpoint phase=datamodel_written files=3_blueprint/3_datamodel/postxl-schema.json,3_blueprint/3_datamodel/seed.json,3_blueprint/3_datamodel/feature_map.json

STEP 9: Human approval
  CHECKPOINT datamodel_approval
    Show business summary first:
      > "Your app will keep track of [primary entities in plain language].
      > [Describe key relationships naturally]: Each [thing] can have multiple [related things].
      > [User-facing implication]: Users will see [what] on their [where].
      >
      > Technical details (if interested):
      >   Models: N, Standard Models: N, Inline Enums: N, Features updated: N
      >
      > Approve, or tell me what to change."

  UNTIL user explicitly approves
    - Apply requested changes
    - Re-validate with pxl
    - Show updated summary

EMIT  [concept-3-blueprint-3-datamodel] completed run_id=<uuid> models=N standard_models=N features_updated=N

CHECKLIST
  - [ ] postxl-schema.json exists and passes pxl validate
  - [ ] schemas array defines PXL + domain-specific schemas with read/write/adminUi auth per role
  - [ ] Every model has a "schema" property assigning it to a defined schema
  - [ ] Every labelField references a String-type field
  - [ ] seed.json has all four scenarios (empty, single_user, populated, edge_cases)
  - [ ] seed.json uses backend-compatible format (camelCase plural keys, snake_case fields)
  - [ ] seed.json includes User records for all referenced user IDs + dev user
  - [ ] feature_map.json maps every model to at least one feature
  - [ ] Feature files updated with data_entities arrays
  - [ ] User has explicitly approved the data model
