# Cascade Rules — Add Feature

When adding or modifying a feature, cascade changes only to artifacts that already exist.
Never create a pipeline step that hasn't been run yet.

## Rule: Only cascade to existing artifacts

Check for each artifact before cascading:

| Artifact | Existence check | What to update |
|----------|----------------|----------------|
| Journeys | `_concept/02_journeys/stories.json` exists | Add stories, update downstream links |
| Tech Stack | `_concept/05_techstack/stack.md` exists | Add integrations/dependencies |
| Architecture | `_concept/05b_architecture/architecture.md` exists | Update modules, protocols, infra |
| Data Model | `_concept/06_datamodel/model.json` exists | Add entities, fields, relations, seed |
| Screens | `_concept/07_screens/` has any .md files | Add/modify screen specs |

## Cascade Details

### 2a — Journeys (`02_journeys/stories.json`)

Update when: new user flow is introduced that isn't covered by existing stories.

- Add story to the appropriate story map (choose stage: vital usually for new MVP features)
- Write at least one EARS acceptance criterion
- Set `downstream.candidate_features` to link back to the new feature
- Set `downstream.entities` for new entities this story requires
- Update `downstream.screens` for new screens this story introduces

**Do NOT:** Create a new story_map unless the feature represents a wholly new journey.
In most cases, add a story to an existing story_map.

### 2b — Tech Stack (`05_techstack/stack.md`)

Update when: the feature requires a new library, service, or integration.

- Add to the relevant section (frontend, backend, database, external services)
- Include: package name/version, what it does, why this feature needs it
- Do not change existing technology choices (that requires a separate architecture decision)

### 2c — Architecture (`05b_architecture/architecture.md`)

Update when: the feature introduces a new system component, protocol, or external dependency.

- New backend module: add to module diagram
- New external service: add to integration diagram
- New data flow: update sequence or flow diagrams
- New infrastructure: update deployment topology

**Do NOT:** Refactor existing architecture for the sake of adding one feature.

### 2d — Data Model (`06_datamodel/model.json` + `model.dbml` + `seed.json`)

Update when: the feature needs new entities, fields, or relations.

**model.dbml:**
```
Table new_entity {
  id integer [pk, increment]
  field_name type [note: "description"]
  created_at timestamp
  updated_at timestamp
}

Ref: new_entity.relation_id > existing_entity.id
```

**model.json:** Keep in sync with DBML. Update the JSON canvas state to include
new entity nodes and relation edges.

**seed.json:** Add example records for new entities in the `populated` scenario.
If the feature adds a new scenario (e.g. "pro plan"), add it as a new scenario key.

Follow `shared/contracts/golden_principles.md`:
- Entity names: PascalCase, singular
- Field names: snake_case
- Every entity needs `id`, `created_at`, `updated_at`

**Back-link:** Update the feature spec's `data_entities: []` to list new entity names.

### 2e — Screens (`07_screens/`)

Update when: the feature needs a new screen or modifies an existing screen's layout/components.

**New screen:**
```markdown
---
last_updated: YYYY-MM-DD
implements: [feature-group/feature-name]
route: /path/to/screen
---

# Screen Name

## Layout
<describe layout structure>

## Components
- <ComponentName> — <what it does>
- <ComponentName> — <what it does>

## States
- default: <description>
- empty: <description>
- error: <description>
```

**Existing screen update:** Modify the component list, layout, or states sections.
Show what changed in a comment or note at the top.

**Navigation:** If the new screen is a top-level route, update `07_screens/00_layout/shell.md`
to add the nav item.

**Cross-references:**
- Feature spec `screens: []` → list new screen paths
- Screen spec `implements: []` → list feature identifiers

## Cascade Order

Always cascade in this order to maintain dependency integrity:

1. Journeys (loosest dependency)
2. Tech Stack (affects architecture choices)
3. Architecture (affects data model)
4. Data Model (affects screens)
5. Screens (reads from everything above)

If you cascade out of order, you may write screens that don't match
the data model, or an architecture that doesn't match the tech stack.
