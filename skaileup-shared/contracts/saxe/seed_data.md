# Seed Data Convention

`_concept/3_blueprint/3_datamodel/seed.json` provides realistic sample data for mockups,
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
        "users": [],
        "tasks": [],
        "projects": []
      }
    },
    "single_user": {
      "description": "One user, minimal data — tests first-use experience",
      "data": {
        "users": [
          {
            "id": "u1",
            "sub": "test",
            "email": "maria.schmidt@example.com",
            "name": "Maria Schmidt",
            "profile_picture_url": null,
            "created_at": "2026-03-01T10:00:00Z",
            "updated_at": "2026-03-01T10:00:00Z"
          }
        ],
        "tasks": [],
        "projects": []
      }
    },
    "populated": {
      "description": "Realistic usage — multiple users, varied data, mixed statuses",
      "data": {
        "users": [
          {
            "id": "u1",
            "sub": "test",
            "email": "maria.schmidt@example.com",
            "name": "Maria Schmidt",
            "profile_picture_url": null,
            "created_at": "2026-03-01T10:00:00Z",
            "updated_at": "2026-03-10T08:00:00Z"
          },
          {
            "id": "u2",
            "sub": "user-jean-pierre",
            "email": "jean-pierre.dubois@example.com",
            "name": "Jean-Pierre Dubois",
            "profile_picture_url": null,
            "created_at": "2026-03-02T14:00:00Z",
            "updated_at": "2026-03-09T16:00:00Z"
          },
          {
            "id": "u3",
            "sub": "user-yuki",
            "email": "yuki.tanaka@example.com",
            "name": "Yuki Tanaka",
            "profile_picture_url": null,
            "created_at": "2026-03-03T09:00:00Z",
            "updated_at": "2026-03-11T11:00:00Z"
          }
        ],
        "tasks": [
          {
            "id": "t1",
            "title": "Design landing page",
            "status": "Done",
            "assigned_to_id": "u1",
            "created_at": "2026-03-01T10:30:00Z",
            "updated_at": "2026-03-07T15:00:00Z"
          },
          {
            "id": "t2",
            "title": "Set up CI pipeline",
            "status": "InProgress",
            "assigned_to_id": "u2",
            "created_at": "2026-03-03T11:00:00Z",
            "updated_at": "2026-03-10T09:00:00Z"
          },
          {
            "id": "t3",
            "title": "Write user documentation",
            "status": "Todo",
            "assigned_to_id": null,
            "created_at": "2026-03-05T14:00:00Z",
            "updated_at": "2026-03-05T14:00:00Z"
          }
        ],
        "projects": [
          {
            "id": "p1",
            "name": "Website Redesign",
            "status": "Active",
            "created_at": "2026-02-28T09:00:00Z",
            "updated_at": "2026-03-11T12:00:00Z"
          }
        ]
      }
    },
    "edge_cases": {
      "description": "Stress-tests for layout, validation, and i18n",
      "data": {
        "users": [
          {
            "id": "u10",
            "sub": "user-maria-jose",
            "email": "maría-josé.fernández-garcía-de-la-cruz@subdomain.example.co.uk",
            "name": "María José Fernández-García de la Cruz",
            "profile_picture_url": null
          },
          {
            "id": "u11",
            "sub": "user-x",
            "email": "a@b.co",
            "name": "X",
            "profile_picture_url": null
          },
          {
            "id": "u12",
            "sub": "user-hans",
            "email": "müller+test@grüße.de",
            "name": "Hans-Jürgen Müller-Lüdenscheid",
            "profile_picture_url": null
          }
        ],
        "tasks": [
          {
            "id": "t10",
            "title": "A",
            "status": "Todo",
            "assigned_to_id": "u11"
          },
          {
            "id": "t11",
            "title": "Implement the extremely long feature requirement that was discussed in the meeting with the stakeholders from the marketing and product teams on Friday afternoon including all edge cases and accessibility concerns that were raised during the review",
            "status": "InProgress",
            "assigned_to_id": "u10"
          }
        ],
        "projects": []
      }
    },
    "permissions": {
      "description": "Tests role-based visibility — admin sees everything, member sees own",
      "data": {
        "users": [
          {
            "id": "u20",
            "sub": "test",
            "email": "admin@example.com",
            "name": "Admin User",
            "profile_picture_url": null
          },
          {
            "id": "u21",
            "sub": "user-member",
            "email": "member@example.com",
            "name": "Regular Member",
            "profile_picture_url": null
          }
        ],
        "tasks": [
          {
            "id": "t20",
            "title": "Admin-only task",
            "status": "Todo",
            "assigned_to_id": "u20",
            "visibility": "Admin"
          },
          {
            "id": "t21",
            "title": "Shared task",
            "status": "Todo",
            "assigned_to_id": "u21",
            "visibility": "All"
          }
        ]
      }
    }
  }
}
```

**Dev user requirement.** Every scenario (except `empty`) MUST include a User
record with `"sub": "test"` — this matches the mock auth identity used in
development. Without it, auth-scoped tRPC queries return empty results and the
developer sees a blank app. The dev user must also have appropriate
membership records (e.g., `orgMembers`, `workspaceMembers`) to access data.

## Standard Scenarios

Every seed.json MUST include at least these four:

| Scenario      | Purpose                                               | Used by                                       |
| ------------- | ----------------------------------------------------- | --------------------------------------------- |
| `empty`       | No data — empty states, onboarding flows              | Mockups (empty state), E2E (first-use)        |
| `single_user` | Minimal — first-use experience                        | Mockups (getting started), E2E (setup flow)   |
| `populated`   | Realistic — the "happy path" view                     | Mockups (default view), E2E (core journeys)   |
| `edge_cases`  | Stress — long names, special chars, missing optionals | Mockups (layout robustness), E2E (validation) |

Additional scenarios (like `permissions`) are optional based on feature needs.

## Data Quality Rules

**Backend-compatible format.** Seed data is generated in the PostXL backend's
expected import format so it can be copied directly into `backend/test-data.json`:

- Model keys use **camelCase plural** (e.g., `users`, `organizations`, `buildSessions`)
  — NOT PascalCase singular. Mapping: `Organization` → `organizations`,
  `OrgMember` → `orgMembers`, `AppVersion` → `appVersions`, etc.
- Field names use **snake_case** (e.g., `cloud_provider`, `organization_id`,
  `started_at`) — NOT camelCase
- Enum values use PascalCase matching inline enum definitions (e.g. `"Done"`, not `"done"`)
- Every record should include `createdAt` and `updatedAt` timestamps in `populated` and `single_user` scenarios
- Use names from different locales (German, French, Japanese, Spanish)
- Vary string lengths: very short ("X"), medium, very long (100+ chars)
- Include special characters: umlauts, accents, hyphens, plus signs
- Include null/empty values for optional fields
- Use realistic email formats including subdomains and TLDs
- Task/item statuses should cover all enum values
- At least 2 entries per model in `populated`, 3+ preferred
- IDs must be consistent across models (relations should resolve)
- Relation fields (ending in `Id`) must reference valid IDs from the related model

## How Skills Use Scenarios

### app-design (mockups)

Render each screen in multiple scenarios:

- `populated` as the default/hero view
- `empty` for empty state design
- `edge_cases` to verify layout doesn't break

### concept-2-experience-3-screens (screen specs)

Reference `populated` scenario in the `## Template Data` section.
Mention `empty` and `edge_cases` in the `## States` section.

### app-e2e (testing)

- Use `empty` scenario to test onboarding/first-use flows
- Use `populated` scenario for core journey tests
- Use `edge_cases` scenario for validation and layout stress tests
- Use `permissions` scenario for role-based access tests

### concept-3-blueprint-3-datamodel (creation)

The datamodel skill generates seed.json with all standard scenarios
after the schema is approved. Model keys use camelCase plural and field
names use snake_case to match the backend's expected import format.
Values are generated to be realistic and varied. Every scenario (except
`empty`) includes a dev user (`sub: "test"`) with appropriate memberships.

### implement-1-setup-1-scaffold (seed integration)

The scaffold skill copies `seed.json`'s `populated` scenario into
`backend/test-data.json` and configures the seed migration to use it.
Because the seed data is already in the backend's expected format
(camelCase plural keys, snake_case fields), no transformation is needed.
