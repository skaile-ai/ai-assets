# Golden Principles

Mechanical rules enforced across all `_concept/` artifacts.
`concept-review` checks these automatically. Every violation
is a finding in the audit report.

## Entity Rules

- Every model MUST declare `standardFields` including at least `["id", "createdAt", "updatedAt"]`
- Model names are PascalCase singular: `User`, `Task`, `ProjectMember`
- Field names are camelCase: `displayName`, `createdAt`, `assignedToId`
- No two models may share the same name

## Enum Rules

- Enum values are PascalCase: `Draft`, `InProgress`, `Done` (not snake_case)
- Inline enums use object format: `{"Draft": "description", "InProgress": "description"}`
- Inline enums auto-name as `{ModelName}{PascalCase(fieldName)}`

## Relation Rules

- Relation field names MUST end with `Id` suffix: `ownerId`, `projectId`
- Relation field type = referenced model name: `User`, `Project?`

## Feature Rules

- Every feature file MUST have at least one requirement checkbox (`- [ ]`)
- Every feature file MUST have a `## Description` section with content
- `priority` must be `must-have` or `nice-to-have` — no other values
- `roles` must be a non-empty array
- `status` must follow the lifecycle: draft → approved → implemented → tested

## Feature Group Rules

- Group folders use two-digit numbered prefixes: `01_`, `02_`, `03_`
- Numbers are sequential with no gaps (no `01_`, `03_` without `02_`)
- Group names are lowercase_snake_case: `01_user_auth`, not `01_UserAuth`

## Screen Rules

- Every screen MUST have `implements:` with at least one feature path
- Every screen MUST have a `## Component Inventory` section with at least one item
- Every screen MUST have a `## States` section (at minimum: default, loading, error)
- Screen groups mirror feature group numbers exactly

## Cross-Reference Rules

- Every path in frontmatter is relative to `_concept/`
- Every path in frontmatter MUST resolve to an existing file
- If a screen lists a feature in `implements:`, that feature must list the screen in `screens:`
- If a feature has `data_entities:`, each entity must exist in `postxl-schema.json`

## Frontmatter Rules

- Every `.md` file in `_concept/` MUST have YAML frontmatter
- Every frontmatter MUST include `status` and `last_updated`
- `last_updated` must be a valid ISO date (YYYY-MM-DD)
- `status` must be one of the defined lifecycle values (see `frontmatter.md`)

## Naming Rules

- All filenames are lowercase_snake_case with `.md` extension
- No spaces in any path
- No special characters beyond underscore and hyphen
- Model and field names in `postxl-schema.json` follow PascalCase (models) and camelCase (fields)

## Seed Data Rules

- `seed.json` must contain at least 2 entries per model in the `populated` scenario
- `seed.json` must include at minimum: `empty`, `single_user`, `populated`, `edge_cases` scenarios
- Model keys use PascalCase matching `postxl-schema.json` model names
- Field names use camelCase matching `postxl-schema.json` field names
- Enum values use PascalCase matching inline enum definitions
- Data must be realistic (real names, varied lengths, different locales)
- Edge cases included: long names, special characters, empty optional fields
- Relation fields (`*Id`) must reference valid IDs from the related model
