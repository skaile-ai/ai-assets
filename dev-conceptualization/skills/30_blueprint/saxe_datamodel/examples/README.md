# concept-3-blueprint-3-datamodel Example Fixture

## Scenario

A simple task manager app with two models (User, Task) and one relationship.

## Input

- `input/1_discovery/1_overview/brief.md` — approved project brief for "TaskFlow"
- `input/2_experience/2_features/01_user_auth/login.md` — login feature
- `input/2_experience/2_features/01_user_auth/registration.md` — registration feature
- `input/3_blueprint/1_techstack/stack.md` — PostXL stack

## Expected Output

- `expected/3_blueprint/3_datamodel/postxl-schema.json` — PostXL schema with User + Task models, inline enums
- `expected/3_blueprint/3_datamodel/seed.json` — scenario-based seed data (empty, single_user, populated, edge_cases)
- `expected/3_blueprint/3_datamodel/feature_map.json` — model-to-feature cross-reference

## Validation

Run checks from `expected/_validation.json`:
- postxl-schema.json is valid JSON with at least 2 models
- Each model has standardFields, labelField, and fields
- seed.json has all 4 required scenarios with PascalCase model keys
- Feature frontmatter has data_entities populated (feedback loop)
