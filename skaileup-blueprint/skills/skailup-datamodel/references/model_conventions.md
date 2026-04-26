# Data Model Conventions

Naming rules and format specifications for `model.dbml`, `model.json`,
`seed.json`, and `feature_map.json`.

## Naming Rules

All naming follows `skaileup-shared/contracts/golden_principles.md`.

| Element | Convention | Example |
|---------|-----------|---------|
| Entity name | PascalCase singular | `Task`, `ProjectMember`, `InvoiceLine` |
| Field name | snake_case | `display_name`, `due_date`, `assigned_to_id` |
| Relation field | snake_case + `_id` suffix | `assigned_to_id`, `project_id`, `created_by_id` |
| Enum ID | snake_case | `task_status`, `user_role` |
| Enum values | PascalCase | `Todo`, `InProgress`, `Done`, `Admin`, `Member` |
| Entity ID in model.json | snake_case | `"id": "task"`, `"id": "project_member"` |
| Field ID in model.json | `<entity>_<field>` | `"id": "task_title"`, `"id": "task_status"` |
| Relation ID | `rel_<from>_<to>` | `"id": "rel_task_user"` |

## model.dbml Format

```dbml
Table user {
  id uuid [pk]
  email email [unique, not null]
  display_name string [not null]
  role user_role [default: 'Member']
  avatar image
  created_at datetime [not null]
}

Table task {
  id uuid [pk]
  title string [not null]
  description richtext
  status task_status [default: 'Todo']
  assigned_to_id uuid [ref: > user.id]
  due_date date
}

Enum user_role {
  Admin
  Member
}

Enum task_status {
  Todo
  InProgress
  Done
}
```

Use semantic types from `skaileup-shared/contracts/semantic_types.md`. Never use SQL types.

## model.json Format

```json
{
  "version": "1.0",
  "editor": {
    "viewport": { "x": 0, "y": 0, "zoom": 1 }
  },
  "entities": [
    {
      "id": "user",
      "display_name": "User",
      "icon": "user",
      "color": "#6366f1",
      "position": { "x": 100, "y": 100 },
      "from_features": ["experience/features/01_user_auth/login.md"],
      "fields": [
        { "id": "user_id", "name": "id", "type": "uuid", "primary": true },
        { "id": "user_email", "name": "email", "type": "email", "required": true, "unique": true },
        { "id": "user_display_name", "name": "display_name", "type": "string", "required": true },
        { "id": "user_role", "name": "role", "type": "enum", "enum_id": "user_role", "default": "Member" }
      ]
    },
    {
      "id": "task",
      "display_name": "Task",
      "icon": "check-square",
      "color": "#f59e0b",
      "position": { "x": 400, "y": 100 },
      "from_features": ["experience/features/02_tasks/create_task.md"],
      "fields": [
        { "id": "task_id", "name": "id", "type": "uuid", "primary": true },
        { "id": "task_title", "name": "title", "type": "string", "required": true },
        { "id": "task_status", "name": "status", "type": "enum", "enum_id": "task_status", "default": "Todo" },
        { "id": "task_assigned_to_id", "name": "assigned_to_id", "type": "relation" }
      ]
    }
  ],
  "relationships": [
    {
      "id": "rel_task_user",
      "from": "task",
      "from_field": "task_assigned_to_id",
      "to": "user",
      "type": "m2o",
      "label": "assigned to",
      "inverse_label": "assigned tasks",
      "required": false,
      "on_delete": "set_null"
    }
  ],
  "enums": [
    {
      "id": "user_role",
      "values": [
        { "value": "Admin", "label": "Administrator" },
        { "value": "Member", "label": "Member" }
      ]
    },
    {
      "id": "task_status",
      "values": [
        { "value": "Todo", "label": "To Do" },
        { "value": "InProgress", "label": "In Progress" },
        { "value": "Done", "label": "Done" }
      ]
    }
  ]
}
```

## Relation Types

| Type | Meaning | On Delete default |
|------|---------|------------------|
| `m2o` | Many-to-one (FK on this entity) | `set_null` |
| `o2m` | One-to-many (FK on the other entity) | `cascade` |
| `m2m` | Many-to-many (requires junction entity) | n/a |

For many-to-many, always create an explicit junction entity (e.g., `project_member`
for User ↔ Project). Do not use implicit join tables.

## seed.json Format

See `skaileup-shared/contracts/seed_data.md` for the authoritative format.

Key rules:
- Entity keys: singular snake_case (`"user"`, `"task"`, `"project_member"`)
- Field names: snake_case (`"display_name"`, `"assigned_to_id"`)
- Enum values: PascalCase matching model definition (`"Todo"`, `"InProgress"`)
- IDs: short strings consistent across entities (`"u1"`, `"t1"`)
- Scenarios: `empty`, `single_user`, `populated`, `edge_cases` (minimum)
- Dev/test user: include one user clearly designated for development testing

```json
{
  "version": "1.0",
  "scenarios": {
    "empty": {
      "description": "No data — tests empty states and onboarding",
      "data": { "user": [], "task": [] }
    },
    "single_user": {
      "description": "One user, no content — first-use experience",
      "data": {
        "user": [
          { "id": "u1", "email": "dev@example.com", "display_name": "Dev User", "role": "Admin" }
        ],
        "task": []
      }
    },
    "populated": {
      "description": "Realistic usage — multiple users, mixed statuses",
      "data": {
        "user": [
          { "id": "u1", "email": "maria.schmidt@example.com", "display_name": "Maria Schmidt", "role": "Admin" },
          { "id": "u2", "email": "jean-pierre.dubois@example.com", "display_name": "Jean-Pierre Dubois", "role": "Member" }
        ],
        "task": [
          { "id": "t1", "title": "Design landing page", "status": "Done", "assigned_to_id": "u1" },
          { "id": "t2", "title": "Set up CI pipeline", "status": "InProgress", "assigned_to_id": "u2" }
        ]
      }
    },
    "edge_cases": {
      "description": "Stress-tests: long strings, special characters, nulls",
      "data": {
        "user": [
          { "id": "u10", "email": "maria-jose.fernandez-garcia@subdomain.example.co.uk", "display_name": "Maria José Fernández-García de la Cruz", "role": "Member" }
        ],
        "task": [
          { "id": "t10", "title": "A", "status": "Todo", "assigned_to_id": null },
          { "id": "t11", "title": "Implement the extremely long feature requirement discussed in the stakeholder meeting including all edge cases and accessibility concerns", "status": "InProgress", "assigned_to_id": "u10" }
        ]
      }
    }
  }
}
```

## feature_map.json Format

Cross-reference mapping every entity to the feature(s) that require it.

```json
{
  "Task": ["experience/features/02_tasks/create_task.md", "experience/features/02_tasks/assign_task.md"],
  "User": ["experience/features/01_user_auth/login.md"],
  "Comment": ["experience/features/03_collaboration/comments.md"]
}
```

Every entity must map to at least one feature. If an entity has no feature source,
question whether it belongs in the model.

## Journey-Derived State Machines

When `_concept/experience/journeys/stories.json` exists, use EARS acceptance criteria
to derive entity state machines:

- **Event-driven** (`WHEN user submits ... THE SYSTEM SHALL transition status to Done`)
  → `Done` is a valid status value; event is a state transition
- **State-driven** (`IF status is InProgress ... THE SYSTEM SHALL show progress bar`)
  → `InProgress` is a guard state; it must exist as an enum value
- **Story `downstream.candidate_entities`** hints → validate against derived entity list

EARS criteria are authoritative for determining enum values and state transitions.
If an EARS criterion mentions a status value, that value must appear in the enum.
