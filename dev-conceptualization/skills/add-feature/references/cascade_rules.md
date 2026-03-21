# Cascade Rules — Add Feature

When adding or modifying a feature, cascade changes only to artifacts that already exist.
Never create a pipeline step that hasn't been run yet.

## Rule: Only cascade to existing artifacts

Check for each artifact before cascading:

| Artifact | Existence check | What to update |
|---|---|---|
| Journeys | `_concept/2_experience/1_journeys/stories.json` exists | Add stories, update downstream links |
| Tech Stack | `_concept/3_blueprint/1_techstack/stack.md` exists | Add integrations to Additional Integrations section |
| Architecture | `_concept/3_blueprint/2_architecture/architecture.md` exists | Update modules, protocols, external_integrations, infrastructure |
| Data Model | `_concept/3_blueprint/3_datamodel/model.json` exists | Add entities, fields, relations; sync model.dbml; update seed.json + feature_map.json |
| Screens | `_concept/2_experience/3_screens/` has any .md files | Add/modify screen specs; update shell.md if new route |

## Cascade Order

Always cascade in this order to maintain dependency integrity:

1. **Journeys** (loosest dependency — story stages inform feature priority)
2. **Tech Stack** (affects architecture choices)
3. **Architecture** (affects data model)
4. **Data Model** (affects screens)
5. **Screens** (reads from everything above)

Cascading out of order risks writing screens that don't match the data model, or an architecture that doesn't match the tech stack.

---

## 1: Journeys (`2_experience/1_journeys/stories.json`)

Update when: new user flow is introduced that isn't covered by existing stories.

1. Determine if the feature introduces a new user flow
2. If yes, add stories to the appropriate story map (stage: vital for new MVP features usually)
3. Write at least one EARS acceptance criterion per new story
4. Update `downstream.candidate_features` to link back to the new feature
5. Update `downstream.candidate_entities` for new entities
6. Update `downstream.candidate_screens` for new screens introduced

**Do NOT** create a new story_map unless the feature represents a wholly new journey.
In most cases, add a story to an existing story_map.

---

## 2: Tech Stack (`3_blueprint/1_techstack/stack.md`)

Update when: the feature requires a new library, service, or integration not already in the stack.

1. Read `_concept/3_blueprint/1_techstack/stack.md`
2. Add the new integration to the "Additional Integrations" section
3. Include: service name, what it does, why this feature needs it
4. Update `last_updated`

Do not change existing technology choices — that requires a separate architecture decision.

Examples of integrations that trigger this: payment gateway (Stripe), email service (Resend/SendGrid), search engine (Meilisearch), SMS (Twilio), AI/ML API, maps API.

---

## 3: Architecture (`3_blueprint/2_architecture/architecture.md`)

Update when: the feature introduces a new system component, protocol, or external dependency.

1. Read `_concept/3_blueprint/2_architecture/architecture.md`
2. Update the relevant sections:
   - Custom Modules (new service module or handler)
   - Communication Protocols (new WebSocket/SSE endpoint, new event)
   - External Integrations (new API adapter)
   - Infrastructure (new service, new env var)
3. Update frontmatter arrays: `custom_modules`, `protocols`, `external_integrations`
4. Update `last_updated`

**Do NOT** refactor existing architecture for the sake of adding one feature.

---

## 4: Data Model (`3_blueprint/3_datamodel/`)

Update when: the feature needs new entities, fields, or relations.

**model.dbml** (add tables, fields, relations using semantic types):
```
Table new_entity {
  id uuid [pk]
  field_name semantic_type [note: "description"]
  relation_id uuid [ref: > existing_entity.id]
  created_at datetime
  updated_at datetime
}
```

**model.json**: Keep in sync with DBML. Add new entity nodes and relation edges.

**seed.json**: Add example records for new entities.
- Use singular snake_case entity keys (e.g., `"task"`, not `"Tasks"`)
- Update the `populated` scenario at minimum
- Update other scenarios as appropriate
- Use PascalCase for enum values (per golden_principles.md)
- Follow seed_data.md conventions (realistic data, no Lorem ipsum)

**feature_map.json**: Add mapping: `"EntityDisplayName": ["feature path(s) that use this entity"]`

**Feedback loop**: Update the feature spec's `data_entities: []` to list new entity names.

Follow `dev-shared/contracts/golden_principles.md`:
- Entity IDs: `snake_case` (e.g., `task_item`)
- Field names: `snake_case`
- Relation fields: `_id` suffix (e.g., `assigned_to_id`)
- Enum values: `PascalCase` (e.g., `InProgress`)

---

## 5: Screens (`2_experience/3_screens/`)

Update when: the feature needs a new screen or modifies an existing screen.

**New screen:**
```markdown
---
implements:
  - 2_experience/2_features/<NN_group>/<feature>.md
data_entities: [<ModelName>, ...]
layout: 2_experience/3_screens/00_layout/shell.md
last_updated: YYYY-MM-DD
---

# Screen Name

## Purpose
<What this screen enables the user to do>

## Route
`/path/to/screen`

## What User Sees
<Plain-language description — no component library names>

## Information Displayed
<List of data shown>

## Actions
<List of user actions>

## Situations
<Default | Empty | Error | Loading state descriptions>

## UI Elements
<Plain-language list of UI elements — buttons, forms, lists, etc.>
```

**Existing screen update**: Modify relevant sections. Note what changed.

**Navigation update**: If the new screen is a top-level route, update
`_concept/2_experience/3_screens/00_layout/shell.md` to add the nav item.

**Feedback loop (forward)**: Update feature spec `screens:` to include the new screen path.
**Feedback loop (reverse)**: Update each screen's `implements:` to include the feature path.

**Important**: Screen specs use plain user-perspective language. No component library names, no CSS tokens, no hex colors. See merged `screens` skill for the full spec format.
