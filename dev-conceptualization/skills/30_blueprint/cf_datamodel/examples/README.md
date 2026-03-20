# cf_concept_datamodel Example Fixture

## Scenario

A simple task manager app with two entities (user, task) and one relationship.

## Input

- `input/01_project/brief.md` — approved project brief for "TaskFlow"
- `input/03_features/01_user_auth/login.md` — login feature
- `input/03_features/01_user_auth/registration.md` — registration feature
- `input/05_techstack/stack.md` — Nuxt + PrimeVue + Directus stack

## Expected Output

- `expected/06_datamodel/model.dbml` — DBML with user + task tables, 2 enums
- `expected/06_datamodel/model.json` — editor-native JSON with semantic types
- `expected/06_datamodel/seed.json` — scenario-based seed data (empty, single_user, populated, edge_cases)

## Validation

Run checks from `expected/_validation.json`:
- model.json is valid JSON with version 1.0
- At least 2 entities, 1 relationship, 1 enum
- seed.json has all 4 required scenarios
- Feature frontmatter has data_entities populated (feedback loop)
