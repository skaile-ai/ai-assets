# Cascade Rules

When a feature is added or modified, update all downstream artifacts that
**already exist** in the concept. Never create pipeline steps that haven't
been run yet.

## 2a: Journeys (if `2_experience/1_journeys/stories.json` exists)

1. Determine if this feature introduces new user flows
2. If yes, add stories to the appropriate story map stage (vital/hygiene)
3. Write EARS acceptance criteria for new stories
4. Update downstream links (candidate_features, candidate_entities, candidate_screens)

## 2b: Tech Stack (if `3_blueprint/1_techstack/stack.md` exists)

If the new feature requires integrations not in the current tech stack:

1. Read `_concept/3_blueprint/1_techstack/stack.md`
2. Add the new integration to the "Additional Integrations" section
3. Update `last_updated`
4. Inform the user: "Added [integration] to tech stack."

Examples: payment gateway (Stripe), email service (Resend), search (Meilisearch),
maps API, SMS, AI/ML API.

## 2c: Architecture (if `3_blueprint/2_architecture/architecture.md` exists)

If the feature needs new backend modules, protocols, or infrastructure:

1. Read `_concept/3_blueprint/2_architecture/architecture.md`
2. Update the relevant sections:
   - Custom Modules table (new NestJS module)
   - Communication Protocols (new WebSocket/SSE endpoint)
   - External Integrations (new API adapter)
   - Infrastructure (new Docker service, env vars)
3. Update frontmatter arrays (`custom_modules`, `protocols`, `external_integrations`)
4. Update `last_updated`

## 2d: Data Model (if `3_blueprint/3_datamodel/postxl-schema.json` exists)

This is often the most significant cascade. Follow concept-3-blueprint-3-datamodel conventions:

1. Read the existing `postxl-schema.json`
2. Determine what changes are needed:
   - **New models** — add to `models` object (PascalCase singular, camelCase fields,
     relations end with `Id`)
   - **New fields on existing models** — add to existing model's `fields`
   - **New enums** — add inline enum objects with PascalCase values
   - **New relations** — add `<model>Id` fields with type = target model
   - **Modified fields** — update type, label, or description
3. Ensure every model has `standardFields: ["id", "createdAt", "updatedAt"]`
4. Ensure every model has a valid `labelField` pointing to a String-type field
5. Validate: `pxl validate _concept/3_blueprint/3_datamodel/postxl-schema.json`
6. Update `feature_map.json` — add mappings for new/modified models
7. Update `seed.json` — add seed data for new models in all scenarios
   (empty, single_user, populated, edge_cases). Use backend format:
   camelCase plural keys, snake_case field names.
8. **Feedback loop:** Update the feature's `data_entities:` array

## 2e: Screens (if `2_experience/3_screens/` exists)

1. Determine what screens the feature needs:
   - **New feature:** Identify screens from requirements + user actions
   - **Modified feature:** Check if existing screens cover new requirements

2. For **new screens:**
   - Write screen spec following `concept-2-experience-3-screens` conventions
   - Include: frontmatter with `implements:`, component inventory using
     `@postxl/ui-components`, data requirements, user actions, states
   - Reference brand tokens from `1_discovery/3_brand/tokens.json` — never invent colors
   - Place in `2_experience/3_screens/<NN_group>/<screen>.md`

3. For **modified screens:**
   - Read the current screen spec
   - Add new components, data requirements, user actions, or states
   - Update `last_updated`

4. Check if `2_experience/3_screens/00_layout/shell.md` needs a navigation update
   (new sidebar item, new tab, new route)

5. **Feedback loop:** Update the feature's `screens:` array:
   ```yaml
   screens:
     - path: 2_experience/3_screens/<group>/<screen>.md
       status: draft
   ```

6. **Feedback loop (reverse):** Update each screen's `implements:` array
   to include the feature path.
