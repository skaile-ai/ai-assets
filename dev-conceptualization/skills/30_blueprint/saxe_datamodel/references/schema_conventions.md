# PostXL Schema Conventions

Detailed conventions for writing `postxl-schema.json` and associated files.

## Schema Structure Template

```json
{
  "name": "app-name",
  "slug": "app-name",
  "description": "From brief.md",
  "projectType": "standalone",
  "version": "0.1.0",
  "auth": {
    "provider": "keycloak",
    "roleClaimPath": "realm_access.roles",
    "defaultDeny": true
  },
  "schemas": [{"name": "PXL"}, {"name": "Data"}],
  "defaultSchema": "Data",
  "systemUser": {"name": "System", "email": "system@app.io"},
  "standardModels": ["User", "Action", "ActionOperation", "File", "TableView", "Comment"],
  "models": {
    "Task": {
      "description": "A task in the system.",
      "schema": "Data",
      "standardFields": ["id", "createdAt", "updatedAt"],
      "labelField": "title",
      "fields": {
        "title": {
          "type": "String",
          "label": "Title",
          "description": "The name of the task."
        },
        "status": {
          "type": {
            "Todo": "Not yet started.",
            "InProgress": "Currently being worked on.",
            "Done": "Completed."
          },
          "label": "Status"
        },
        "assignedToId": {
          "type": "User?",
          "label": "Assigned To",
          "description": "The user responsible for this task."
        }
      },
      "actions": {
        "complete": "Mark the task as done."
      }
    }
  }
}
```

## Schemas and Role-Based Access

Every PostXL schema must define database schemas that group models by domain concern.
Each schema carries auth rules controlling who can read, write, and see admin UI.

```json
"schemas": [
  {
    "name": "PXL",
    "auth": {
      "read":    { "anyRole": ["<all roles>"] },
      "write":   { "anyRole": ["<admin roles only>"] },
      "adminUi": { "visibleFor": ["<admin roles only>"] }
    }
  },
  {
    "name": "Config",
    "auth": {
      "read":    { "anyRole": ["<all roles>"] },
      "write":   { "anyRole": ["<admin roles only>"] },
      "adminUi": { "visibleFor": ["<admin roles only>"] }
    }
  },
  {
    "name": "<DomainData>",
    "auth": {
      "read":    { "anyRole": ["<all roles>"] },
      "write":   { "anyRole": ["<all roles or subset>"] },
      "adminUi": { "visibleFor": ["<admin roles>"] }
    }
  }
]
```

**Schema design guidelines:**
- **PXL** — always present; covers PostXL system internals (standard models). Admin-only write.
- **Config** — org setup, templates, infrastructure settings. Admin-only write, all can read.
- **Domain schemas** — group by access pattern and domain boundary:
  - User-facing work data (projects, conversations) → all roles can write
  - Runtime/infrastructure (deployments, containers) → admin-only write
- Set `defaultSchema` to the most commonly used domain schema.
- Every model must have a `"schema"` property assigning it to one of the defined schemas.

## Naming Conventions

| Convention | Rule |
|------------|------|
| Model names | PascalCase singular (`Task`, `Project`, `Invoice`) |
| Field names | camelCase (`firstName`, `assignedToId`, `dueDate`) |
| Relations | Field name ends with `Id`, type = target model name (`User`, `Project?`) |
| Optional relations | Append `?` to the type (`User?` means nullable) |
| Inline enums | Type is an object mapping values to descriptions: `{"Todo": "Not yet started."}` |
| Standard fields | Use `standardFields` array — never manually define `id`, `createdAt`, `updatedAt` |
| Label field | Set `labelField` to the field used as display name in lists and dropdowns |
| Actions | Map of action name to description for custom business operations |
| Schema | Assign each model to a schema (`Data` for domain, `PXL` for system) |

## Standard Models

PostXL provides these out of the box. Include only those the app needs
in the `standardModels` array. Do not define fields — PostXL generates them.

| Model | Purpose |
|-------|---------|
| `User` | Authentication and user profiles |
| `Action` | Audit log of user actions |
| `ActionOperation` | Individual operations within an action |
| `Comment` | Comments on any model |
| `File` | File uploads and attachments |
| `TableView` | Saved table/list view configurations |
| `Config` | Application configuration key-value pairs |

## labelField Validation

After writing the schema, verify that every model's `labelField` points to a
field whose type is `String`. A `labelField` backed by an inline enum or any
non-string type causes display issues in generated UI (dropdowns, breadcrumbs,
search results). Fix by:
- Changing `labelField` to a different String-type field, or
- Adding a new String field (e.g., `name`, `title`) and using that

## Architecture Context

When reading `_concept/3_blueprint/2_architecture/architecture.md`, look for:
- Custom NestJS modules beyond standard PostXL — may need their own entities
- Communication protocols (WebSocket/SSE) — may need session/message/event models
- External integrations — may need connection/credential models
- Additional apps — inter-app entities (agent sessions, job queues)

## Journey-Derived State Machines

When `_concept/2_experience/1_journeys/stories.json` exists, use EARS acceptance criteria
to derive entity state machines:
- Event-driven criteria (`WHEN ... THE SYSTEM SHALL ...`) → state transitions
- State-driven criteria (`IF ... THE SYSTEM SHALL ...`) → guard conditions
- Story downstream links → model relationships and enum values

## Seed Data Format

The PostXL backend's bulk import expects a specific format that differs from
the schema's naming conventions. Generate `seed.json` in backend-compatible
format directly:

- **Model keys:** camelCase plural (e.g., `organizations`, `workspaces`,
  `orgMembers`, `appVersions`) — NOT PascalCase singular
- **Field names:** snake_case (e.g., `cloud_provider`, `started_at`,
  `organization_id`) — NOT camelCase
- **Enum values:** PascalCase matching inline enum definitions (e.g., `"Done"`)
- **Mapping rule for model keys:** PascalCase singular → camelCase plural:
  `Organization` → `organizations`, `OrgMember` → `orgMembers`,
  `BuildSession` → `buildSessions`, `ApiKey` → `apiKeys`
- **Mapping rule for field names:** camelCase → snake_case:
  `cloudProvider` → `cloud_provider`, `organizationId` → `organization_id`,
  `startedAt` → `started_at`

This allows `implement-1-setup-1-scaffold` to copy seed data directly into
`backend/test-data.json` without transformation.

### User Records Are Mandatory

Always generate `User` records (key: `users`) for every user ID referenced
anywhere in the data. Include:
- `id`, `sub`, `name`, `email`, `profile_picture_url` fields
- A **dev user** entry matching the mock auth identity (`sub: "test"`) with
  appropriate org/workspace memberships
- Without User records, FK-dependent records silently fail or create orphaned references

### Data Quality Rules

- Use names from different locales, vary string lengths, include special characters
- IDs must be consistent across models (relations should resolve)
- Add a `permissions` scenario if the app has role-based features
- Every seed.json must include at minimum: `empty`, `single_user`, `populated`, `edge_cases`

## feature_map.json Format

Cross-reference mapping each model to the feature files that require it:

```json
{
  "Task": ["2_experience/2_features/02_tasks/create_task.md", "2_experience/2_features/02_tasks/assign_task.md"],
  "User": ["2_experience/2_features/01_user_auth/login.md"],
  "Project": ["2_experience/2_features/03_projects/create_project.md"]
}
```

Every model must trace back to at least one feature. If a model has no
feature source, question whether it belongs in the schema.
