---
name: datamodel
description: "Use when features and techstack are approved but _concept/06_datamodel/ is empty. Also when user says 'data model', 'database schema', 'entities', 'what tables do we need'."
keywords: [data, schema, database, entities, relationships, dbml, typebox, model, seed]
user_inputs:
  dialog: []
  files:
  - "03_features/**/*.md"
  - "05_techstack/stack.md"
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

# Data Model

## Overview

The data model skill analyzes approved features and produces a stack-independent
data model using semantic types. It outputs two synchronized formats: DBML for
human readability and JSON for the visual editor. It does NOT output
Directus-specific, Prisma-specific, or any other stack-locked schema unless
the user explicitly requests a stack translation.

## When to Use
- Features and techstack are approved but `_concept/06_datamodel/` is empty
- User asks about entities, tables, relationships, database schema
- User says "data model", "what data do we need", "database design"

## When NOT to Use
- Features have not been written or approved yet (run features first)
- User wants to change existing model fields (edit files directly)
- User wants stack-specific output only (use stack translation as a follow-up)

## Prerequisites

**HARD-GATE (from pipeline.json):**
```json
"hard_gates": [
  { "type": "file_exists", "path": "03_features/" },
  { "type": "file_exists", "path": "05_techstack/stack.md" }
]
```
- `03_features/` must exist with at least one feature file
- `05_techstack/stack.md` must exist

If any gate fails, stop immediately and name the missing prerequisite skill.

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/03_features/` must exist with at least one feature file, `_concept/05_techstack/stack.md` must exist
**If gates fail:** Run `cf_concept_functionality_features` or `cf_concept_techstack` as needed.
**On completion:** Present summary, then orchestrator suggests next steps.

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid _concept/ paths and naming rules
- `cf__shared/semantic_types.md` — stack-independent data types
- `cf__shared/feedback_loop.md` — cross-reference protocol
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

**Must read:**
- `_concept/01_project/brief.md`
- `_concept/03_features/` (all feature files)
- `_concept/05_techstack/stack.md`
- `cf__shared/semantic_types.md`
- `cf__shared/feedback_loop.md`

**Optional:**
- `_concept/03b_behavior/*.allium` — formal entity definitions, state enums, transition rules _(fallback: empty_default)_
- `_concept/05b_architecture/architecture.md` — custom services, protocols, extra entities _(fallback: skip_if_absent)_
- `_concept/_research/general/patterns.md` — domain-specific data patterns from research

**Never load:**
- `_concept/07_screens/`
- `_concept/04_brand/`

## Workflow

### Step 1: Read Context

Read all must-read files. Stop if any hard-gate prerequisite is missing.

| Artifact | Path | Missing? Run |
|----------|------|-------------|
| Project brief | `_concept/01_project/brief.md` | `cf_concept_overview` |
| Features | `_concept/03_features/**/*.md` | `cf_concept_functionality_features` |
| Tech stack | `_concept/05_techstack/stack.md` | `cf_concept_techstack` |
| Architecture | `_concept/05b_architecture/architecture.md` | `cf_concept_architecture` _(optional read — skip_if_absent)_ |

Tech stack is needed to understand backend constraints but does NOT
change the data model format — only the future stack translation.

**Architecture context.** Read `_concept/05b_architecture/architecture.md` if it exists
(optional read with `skip_if_absent` fallback — omit this section if the file is not present).
This tells you:

- Which custom services or extensions exist beyond the stack defaults — these may
  need their own entities or configuration models
- Which communication protocols are used — entities involved in WebSocket/SSE flows
  may need session, message, or event models
- Which external integrations exist — these may need connection/credential models
- Which additional apps exist — inter-service entities (e.g., agent sessions, job queues)
  need to be reflected in the data model

**Optional: Behavioral specs.** Check if `_concept/03b_behavior/*.allium` exists.
If present, read all `.allium` files. These contain formal entity definitions,
state enums, transition rules, and relationships that should inform your model:

- Allium entity fields -> model entity fields (translate Allium types to semantic types)
- Allium inline enums (e.g., `status: active | locked`) -> model enums
- Allium relationships (`with` clauses) -> model relationships
- Allium config values -> inform field defaults and constraints
- Allium rules -> help identify which entities need status/state fields

The behavioral specs are authoritative for state machines and transitions. If an
allium spec defines `status: todo | in_progress | done` on a Task entity, use
exactly those enum values in the model.

**Optional: Research patterns.** Check `_concept/_research/general/patterns.md` for
domain-specific data patterns discovered during research. These inform entity
structure and relationship design.

### Step 2: Analyze Features

For each feature, identify:
- What data gets created, read, updated, deleted?
- What entities are needed?
- What relationships exist between entities?
- What enums are needed?

Ask the user to confirm or clarify:

| # | Question |
|---|----------|
| 1 | For each feature: what data does it need? |
| 2 | How do things relate? (a user has many tasks...) |
| 3 | Who can see or change what? (roles/permissions) |

### Step 3: Write DBML

```bash
mkdir -p _concept/06_datamodel
```

**Output: `_concept/06_datamodel/model.dbml`**

Human-readable entity definitions using DBML syntax:

```dbml
Table user {
  id uuid [pk]
  email email [unique, not null]
  display_name string [not null]
  role user_role [default: 'member']
  avatar image
  created_at datetime [not null]
}

Table task {
  id uuid [pk]
  title string [not null]
  description richtext
  status task_status [default: 'todo']
  assigned_to uuid [ref: > user.id]
  due_date date
}

Enum user_role {
  admin
  member
}

Enum task_status {
  todo
  in_progress
  done
}
```

Use semantic types (from `cf__shared/semantic_types.md`), not SQL types.

### Step 4: Write model.json

**Output: `_concept/06_datamodel/model.json`**

The editor-native format. Every item needs a stable `id` for the visual editor.

```json
{
  "version": "1.0",
  "editor": {
    "viewport": { "x": 0, "y": 0, "zoom": 1 }
  },
  "entities": [
    {
      "id": "user",
      "display_name": "User",
      "icon": "user",
      "color": "#6366f1",
      "position": { "x": 100, "y": 100 },
      "from_features": ["03_features/01_user_auth/login.md"],
      "fields": [
        { "id": "user_id", "name": "id", "type": "uuid", "primary": true },
        { "id": "user_email", "name": "email", "type": "email", "required": true, "unique": true },
        { "id": "user_name", "name": "display_name", "type": "string", "required": true },
        { "id": "user_role", "name": "role", "type": "enum", "enum_id": "user_role", "default": "member" }
      ]
    }
  ],
  "relationships": [
    {
      "id": "rel_task_user",
      "from": "task",
      "from_field": "assigned_to",
      "to": "user",
      "type": "m2o",
      "label": "assigned to",
      "inverse_label": "assigned tasks",
      "required": false,
      "on_delete": "set_null"
    }
  ],
  "enums": [
    {
      "id": "user_role",
      "values": [
        { "value": "admin", "label": "Administrator" },
        { "value": "member", "label": "Member" }
      ]
    }
  ]
}
```

### Step 5: Write Seed Data

Read `cf__shared/seed_data.md` for the scenario format and data quality rules.

**Output: `_concept/06_datamodel/seed.json`**

Organized as named scenarios. Every seed.json must include at minimum:
`empty`, `single_user`, `populated`, `edge_cases`.

```json
{
  "version": "1.0",
  "scenarios": {
    "empty": {
      "description": "No data — tests empty states and onboarding",
      "data": {
        "user": [],
        "task": []
      }
    },
    "single_user": {
      "description": "One user, no content — first-use experience",
      "data": {
        "user": [
          { "id": "u1", "email": "maria.schmidt@example.com", "display_name": "Maria Schmidt", "role": "admin" }
        ],
        "task": []
      }
    },
    "populated": {
      "description": "Realistic usage — multiple users, mixed statuses",
      "data": {
        "user": [
          { "id": "u1", "email": "maria.schmidt@example.com", "display_name": "Maria Schmidt", "role": "admin" },
          { "id": "u2", "email": "jean-pierre.dubois@example.com", "display_name": "Jean-Pierre Dubois", "role": "member" },
          { "id": "u3", "email": "yuki.tanaka@example.com", "display_name": "Yuki Tanaka", "role": "member" }
        ],
        "task": [
          { "id": "t1", "title": "Design landing page", "status": "done", "assigned_to": "u1" },
          { "id": "t2", "title": "Set up CI pipeline", "status": "in_progress", "assigned_to": "u2" },
          { "id": "t3", "title": "Write documentation", "status": "todo", "assigned_to": null }
        ]
      }
    },
    "edge_cases": {
      "description": "Stress-tests for layout, validation, i18n",
      "data": {
        "user": [
          { "id": "u10", "email": "maria-jose.fernandez-garcia@subdomain.example.co.uk", "display_name": "Maria Jose Fernandez-Garcia de la Cruz", "role": "member" },
          { "id": "u11", "email": "a@b.co", "display_name": "X", "role": "admin" }
        ],
        "task": [
          { "id": "t10", "title": "A", "status": "todo", "assigned_to": "u11" },
          { "id": "t11", "title": "Implement the extremely long feature requirement discussed in the stakeholder meeting including all edge cases and accessibility concerns raised during review", "status": "in_progress", "assigned_to": "u10" }
        ]
      }
    }
  }
}
```

Add a `permissions` scenario if the app has role-based features.
Use names from different locales, vary string lengths, include special characters.
IDs must be consistent across entities (relations should resolve).

### Step 6: Update Feature Frontmatter (Feedback Loop)

For each feature, determine which entities serve it. Update the feature's
`data_entities` array:

```yaml
# In 03_features/01_user_auth/login.md
data_entities: [user, session]
```

Emit feedback loop events:

```
[cf_concept_datamodel] feedback_loop updated 03_features/01_user_auth/login.md
  set data_entities: [user, session]
```

### Step 7: Stack Translation (On Request Only)

If the user asks "generate Directus schema" or "export to Prisma":

1. Read `_concept/05_techstack/stack.md` to confirm the target
2. Use the translation table in `cf__shared/semantic_types.md`
3. Output the stack-specific format

**Directus:** collection snapshot JSON
**Prisma:** `schema.prisma` file
**Supabase:** SQL migration
**Raw SQL:** PostgreSQL DDL

Stack translations are separate files, not replacements for model.json.

## Outputs

| File | Purpose |
|------|---------|
| `_concept/06_datamodel/model.dbml` | Human-readable entity definitions (DBML syntax) |
| `_concept/06_datamodel/model.json` | Editor-native format (visual canvas state) |
| `_concept/06_datamodel/seed.json` | Scenario-based seed data (empty, single_user, populated, edge_cases) |

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

Show summary:

```
Entities: N (user, task, project, ...)
Relationships: N
Enums: N
Features updated: N (data_entities populated)
```

## Common Mistakes

| Rationalization | Reality |
|----------------|---------|
| "The model is obvious from the features" | Features describe user intent, not data structure. Many implicit entities (audit logs, sessions, join tables) only emerge during analysis. Always do the full entity extraction. |
| "I don't need to read the behavioral specs" | Allium specs are authoritative for state machines and enums. Skipping them means inventing states that conflict with formalized behavior. Always check `03b_behavior/`. |
| "I'll use SQL types directly" | The model is stack-independent. Use semantic types from `cf__shared/semantic_types.md`. Stack translation is a separate, on-request step. |
| "Seed data can wait" | Seed data is consumed by mockups and E2E tests immediately. Empty seed.json means broken downstream. Write it now with all four required scenarios. |
| "I'll just copy the feature list as entities" | Features and entities are not 1:1. Multiple features share entities, and some entities (timestamps, audit trails) serve infrastructure, not a single feature. |

## Research Mode

Research data patterns and schema designs from competitors. Check
`_concept/_research/general/patterns.md` for domain-specific data patterns discovered
during the research phase. These patterns inform entity structure, relationship
cardinality, and enum design.

## Integration

- **Called by:** orchestrator or standalone
- **Reads from:** 01_project/, 03_features/, 05_techstack/, 03b_behavior/ (optional), _research/general/patterns.md (optional)
- **Writes to:** 06_datamodel/
- **Updates upstream:** 03_features/ (populates `data_entities` in feature frontmatter)
- **Consumed by:** screens, cf_concept_mock, bootstrap, cf_test_e2e

```
[cf_concept_datamodel] completed
  run_id: <uuid>
  entities: N
  relationships: N
  features_updated: N
```
