# Seed Data Convention

`_concept/blueprint/datamodel/seed.json` provides realistic sample data for mockups,
screen specs, and E2E testing. Data is organized as named **scenarios** so
each consumer can pick the state it needs.

## Format

Entity keys are singular snake_case matching the entity names in `model.json`.
Relation fields use the `_id` suffix. Enum values are PascalCase.

```json
{
  "version": "1.0",
  "scenarios": {
    "empty": {
      "description": "App with no user data yet — tests empty states",
      "data": {
        "user": [],
        "task": [],
        "project": []
      }
    },
    "single_user": {
      "description": "One user, minimal data — tests first-use experience",
      "data": {
        "user": [
          { "id": "u1", "email": "maria.schmidt@example.com", "display_name": "Maria Schmidt", "role": "Admin", "created_at": "2026-03-01T10:00:00Z", "updated_at": "2026-03-01T10:00:00Z" }
        ],
        "task": [],
        "project": []
      }
    },
    "populated": {
      "description": "Realistic usage — multiple users, varied data, mixed statuses",
      "data": {
        "user": [
          { "id": "u1", "email": "maria.schmidt@example.com", "display_name": "Maria Schmidt", "role": "Admin", "created_at": "2026-03-01T10:00:00Z", "updated_at": "2026-03-10T08:00:00Z" },
          { "id": "u2", "email": "jean-pierre.dubois@example.com", "display_name": "Jean-Pierre Dubois", "role": "Member", "created_at": "2026-03-02T14:00:00Z", "updated_at": "2026-03-09T16:00:00Z" },
          { "id": "u3", "email": "yuki.tanaka@example.com", "display_name": "Yuki Tanaka", "role": "Member", "created_at": "2026-03-03T09:00:00Z", "updated_at": "2026-03-11T11:00:00Z" }
        ],
        "task": [
          { "id": "t1", "title": "Design landing page", "status": "Done", "assigned_to_id": "u1", "created_at": "2026-03-01T10:30:00Z", "updated_at": "2026-03-07T15:00:00Z" },
          { "id": "t2", "title": "Set up CI pipeline", "status": "InProgress", "assigned_to_id": "u2", "created_at": "2026-03-03T11:00:00Z", "updated_at": "2026-03-10T09:00:00Z" },
          { "id": "t3", "title": "Write user documentation", "status": "Todo", "assigned_to_id": null, "created_at": "2026-03-05T14:00:00Z", "updated_at": "2026-03-05T14:00:00Z" }
        ],
        "project": [
          { "id": "p1", "name": "Website Redesign", "status": "Active", "created_at": "2026-02-28T09:00:00Z", "updated_at": "2026-03-11T12:00:00Z" }
        ]
      }
    },
    "edge_cases": {
      "description": "Stress-tests for layout, validation, and i18n",
      "data": {
        "user": [
          { "id": "u10", "email": "maría-josé.fernández-garcía-de-la-cruz@subdomain.example.co.uk", "display_name": "María José Fernández-García de la Cruz", "role": "Member" },
          { "id": "u11", "email": "a@b.co", "display_name": "X", "role": "Admin" },
          { "id": "u12", "email": "müller+test@grüße.de", "display_name": "Hans-Jürgen Müller-Lüdenscheid", "role": "Member" }
        ],
        "task": [
          { "id": "t10", "title": "A", "status": "Todo", "assigned_to_id": "u11" },
          { "id": "t11", "title": "Implement the extremely long feature requirement that was discussed in the meeting with the stakeholders from the marketing and product teams on Friday afternoon including all edge cases and accessibility concerns that were raised during the review", "status": "InProgress", "assigned_to_id": "u10" }
        ],
        "project": []
      }
    },
    "permissions": {
      "description": "Tests role-based visibility — admin sees everything, member sees own",
      "data": {
        "user": [
          { "id": "u20", "email": "admin@example.com", "display_name": "Admin User", "role": "Admin" },
          { "id": "u21", "email": "member@example.com", "display_name": "Regular Member", "role": "Member" }
        ],
        "task": [
          { "id": "t20", "title": "Admin-only task", "status": "Todo", "assigned_to_id": "u20", "visibility": "Admin" },
          { "id": "t21", "title": "Shared task", "status": "Todo", "assigned_to_id": "u21", "visibility": "All" }
        ]
      }
    }
  }
}
```

---

## Standard Scenarios

Every `seed.json` MUST include at least these four:

| Scenario | Purpose | Used by |
|---|---|---|
| `empty` | No data — empty states, onboarding flows | Mockups (empty state), E2E (first-use) |
| `single_user` | Minimal — first-use experience | Mockups (getting started), E2E (setup flow) |
| `populated` | Realistic — the "happy path" view | Mockups (default view), E2E (core journeys) |
| `edge_cases` | Stress — long names, special chars, missing optionals | Mockups (layout robustness), E2E (validation) |

Additional scenarios (like `permissions`) are optional based on feature needs.

---

## Data Quality Rules

- Entity keys match entity names in `model.json` — singular snake_case (`user`, `task`)
- Field names are snake_case matching the semantic model (`display_name`, `assigned_to_id`)
- Enum values are PascalCase matching inline enum definitions (`Done`, `InProgress`, `Active`)
- Relation fields end with `_id` and reference valid IDs from the related entity in the same scenario
- Every record in `populated` and `single_user` MUST include `created_at` and `updated_at` timestamps
- Statuses in `populated` should cover all enum values across the records
- At least 2 entries per entity in `populated`; 3+ preferred
- Use names from different locales (German, French, Japanese, Spanish)
- Vary string lengths: very short (`"X"`), medium, very long (100+ chars)
- Include special characters: umlauts, accents, hyphens, plus signs
- Include null values for optional fields
- Use realistic email formats including subdomains and international TLDs

> **Stack-specific dev user:** Some stacks require a fixed dev identity for auth-scoped
> queries to return data during development (e.g., a user matching the mock JWT subject).
> If the target stack requires this, every scenario except `empty` should include such a
> record. The stack translator or scaffold skill is responsible for documenting this
> requirement.

---

## How Skills Use Scenarios

### `mock` (mockups)

Render each screen in multiple scenarios:
- `populated` as the default/hero view
- `empty` for empty state design
- `edge_cases` to verify layout doesn't break

### `screens` (screen specs)

Reference `populated` scenario in the `## Template Data` section.
Mention `empty` and `edge_cases` in the `## States` section.

### `e2e` (testing)

- Use `empty` scenario to test onboarding/first-use flows
- Use `populated` scenario for core journey tests
- Use `edge_cases` scenario for validation and layout stress tests
- Use `permissions` scenario for role-based access tests

### `datamodel` (creation)

The datamodel skill generates `seed.json` with all standard scenarios after the
model is approved. Entity keys and field names follow the semantic layer conventions.
Values are generated to be realistic and varied.

### `scaffold` (seed integration)

The scaffold skill wires `seed.json` into the project's seed migration or test
data loader. The stack translator determines the exact format expected by the
backend — seed.json is the source of truth; any format transformation happens
at scaffold time.
