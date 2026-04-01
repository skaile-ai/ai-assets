# Seed Data Convention

`_concept/06_datamodel/seed.json` provides realistic sample data for mockups,
screen specs, and E2E testing. Data is organized as named **scenarios** so
each consumer can pick the state it needs.

## Format

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
          { "id": "u1", "email": "maria.schmidt@example.com", "display_name": "Maria Schmidt", "role": "admin", "created_at": "2026-03-01T10:00:00Z", "updated_at": "2026-03-01T10:00:00Z" }
        ],
        "task": [],
        "project": []
      }
    },
    "populated": {
      "description": "Realistic usage — multiple users, varied data, mixed statuses",
      "data": {
        "user": [
          { "id": "u1", "email": "maria.schmidt@example.com", "display_name": "Maria Schmidt", "role": "admin", "created_at": "2026-03-01T10:00:00Z", "updated_at": "2026-03-10T08:00:00Z" },
          { "id": "u2", "email": "jean-pierre.dubois@example.com", "display_name": "Jean-Pierre Dubois", "role": "member", "created_at": "2026-03-02T14:00:00Z", "updated_at": "2026-03-09T16:00:00Z" },
          { "id": "u3", "email": "yuki.tanaka@example.com", "display_name": "Yuki Tanaka", "role": "member", "created_at": "2026-03-03T09:00:00Z", "updated_at": "2026-03-11T11:00:00Z" }
        ],
        "task": [
          { "id": "t1", "title": "Design landing page", "status": "done", "assigned_to": "u1", "created_at": "2026-03-01T10:30:00Z", "updated_at": "2026-03-07T15:00:00Z" },
          { "id": "t2", "title": "Set up CI pipeline", "status": "in_progress", "assigned_to": "u2", "created_at": "2026-03-03T11:00:00Z", "updated_at": "2026-03-10T09:00:00Z" },
          { "id": "t3", "title": "Write user documentation", "status": "todo", "assigned_to": null, "created_at": "2026-03-05T14:00:00Z", "updated_at": "2026-03-05T14:00:00Z" }
        ],
        "project": [
          { "id": "p1", "name": "Website Redesign", "status": "active", "created_at": "2026-02-28T09:00:00Z", "updated_at": "2026-03-11T12:00:00Z" }
        ]
      }
    },
    "edge_cases": {
      "description": "Stress-tests for layout, validation, and i18n",
      "data": {
        "user": [
          { "id": "u10", "email": "maría-josé.fernández-garcía-de-la-cruz@subdomain.example.co.uk", "display_name": "María José Fernández-García de la Cruz", "role": "member" },
          { "id": "u11", "email": "a@b.co", "display_name": "X", "role": "admin" },
          { "id": "u12", "email": "müller+test@grüße.de", "display_name": "Hans-Jürgen Müller-Lüdenscheid", "role": "member" }
        ],
        "task": [
          { "id": "t10", "title": "A", "status": "todo", "assigned_to": "u11" },
          { "id": "t11", "title": "Implement the extremely long feature requirement that was discussed in the meeting with the stakeholders from the marketing and product teams on Friday afternoon including all edge cases and accessibility concerns that were raised during the review", "status": "in_progress", "assigned_to": "u10" }
        ],
        "project": []
      }
    },
    "permissions": {
      "description": "Tests role-based visibility — admin sees everything, member sees own",
      "data": {
        "user": [
          { "id": "u20", "email": "admin@example.com", "display_name": "Admin User", "role": "admin" },
          { "id": "u21", "email": "member@example.com", "display_name": "Regular Member", "role": "member" }
        ],
        "task": [
          { "id": "t20", "title": "Admin-only task", "status": "todo", "assigned_to": "u20", "visibility": "admin" },
          { "id": "t21", "title": "Shared task", "status": "todo", "assigned_to": "u21", "visibility": "all" }
        ]
      }
    }
  }
}
```

## Standard Scenarios

Every seed.json MUST include at least these four:

| Scenario | Purpose | Used by |
|----------|---------|---------|
| `empty` | No data — empty states, onboarding flows | Mockups (empty state), E2E (first-use) |
| `single_user` | Minimal — first-use experience | Mockups (getting started), E2E (setup flow) |
| `populated` | Realistic — the "happy path" view | Mockups (default view), E2E (core journeys) |
| `edge_cases` | Stress — long names, special chars, missing optionals | Mockups (layout robustness), E2E (validation) |

Additional scenarios (like `permissions`) are optional based on feature needs.

## Data Quality Rules

- Every record must include `created_at` and `updated_at` (datetime) timestamps
  matching the golden principle that all entities have these fields
- Use names from different locales (German, French, Japanese, Spanish)
- Vary string lengths: very short ("X"), medium, very long (100+ chars)
- Include special characters: umlauts, accents, hyphens, plus signs
- Include null/empty values for optional fields
- Use realistic email formats including subdomains and TLDs
- Task/item statuses should cover all enum values
- At least 2 entries per entity in `populated`, 3+ preferred
- IDs must be consistent across entities (relations should resolve)

## How Skills Use Scenarios

### cf_concept_mock (mockups)

Render each screen in multiple scenarios:
- `populated` as the default/hero view
- `empty` for empty state design
- `edge_cases` to verify layout doesn't break

### cf_concept_ui_screens (screen specs)

Reference `populated` scenario in the `## Template Data` section.
Mention `empty` and `edge_cases` in the `## States` section.

### cf_test_e2e (testing)

- Use `empty` scenario to test onboarding/first-use flows
- Use `populated` scenario for core journey tests
- Use `edge_cases` scenario for validation and layout stress tests
- Use `permissions` scenario for role-based access tests

### cf_concept_datamodel (creation)

The datamodel skill generates seed.json with all standard scenarios
after the model is approved. Entity fields come from model.json,
values are generated to be realistic and varied.
