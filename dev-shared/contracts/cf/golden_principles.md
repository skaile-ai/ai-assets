# Golden Principles

Mechanical rules enforced across all `_concept/` artifacts.
`cf_quality_review` checks these automatically. Every violation
is a finding in the audit report.

## Entity Rules

- Every entity MUST have a field named `id` with type `uuid` and `primary: true`
- Every entity MUST have `created_at` (datetime) and `updated_at` (datetime) fields
- Entity IDs are lowercase_snake_case: `user`, `task`, `project_member`
- No two entities may share the same `id`

## Enum Rules

- Enum values are lowercase_snake_case: `in_progress`, not `InProgress` or `IN_PROGRESS`
- Every enum must be referenced by at least one field (`enum_id`)
- Enum IDs are lowercase_snake_case matching their purpose: `task_status`, `user_role`

## Feature Rules

- Every feature file MUST have at least one requirement checkbox (`- [ ]`)
- Every feature file MUST have a `## Description` section with content
- `priority` must be `must-have` or `nice-to-have` — no other values
- `roles` must be a non-empty array

## Feature Group Rules

- Group folders use a letter prefix followed by a two-digit number: `A_01_`, `B_02_`, `C_03_`
- Letters and numbers are sequential with no gaps (no `A_01_`, `C_03_` without `B_02_`)
- Group names are lowercase_snake_case: `A_01_user_auth`, not `A_01_UserAuth`

## Screen Rules

- Every screen MUST have `implements:` with at least one feature path
- Every screen MUST have a `## Component Inventory` section with at least one item
- Every screen MUST have a `## States` section (at minimum: default, loading, error)
- Screen groups mirror feature group numbers exactly

## Cross-Reference Rules

- Every path in frontmatter is relative to `_concept/`
- Every path in frontmatter MUST resolve to an existing file
- If a screen lists a feature in `implements:`, that feature must list the screen in `screens:`
- If a feature has `data_entities:`, each entity must exist in `model.json`

## Frontmatter Rules

- Every `.md` file in `_concept/` MUST have YAML frontmatter
- Every frontmatter MUST include `last_updated`
- `last_updated` must be a valid ISO date (YYYY-MM-DD)

## Naming Rules

- All filenames are lowercase_snake_case with `.md` extension
- No spaces in any path
- No special characters beyond underscore and hyphen

## Seed Data Rules

- `seed.json` must contain at least 2 entries per entity
- Data must be realistic (real names, varied lengths, different locales)
- Edge cases included: long names, special characters, empty optional fields
