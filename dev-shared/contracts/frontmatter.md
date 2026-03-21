# Frontmatter Schema

All markdown files in `_concept/` use YAML frontmatter.
Skills must use these field names exactly.

## Universal Fields

Every markdown file in `_concept/`:

```yaml
---
last_updated: YYYY-MM-DD    # ISO date, updated on every write
---
```

---

## 1_discovery/1_overview/brief.md

```yaml
---
elevator_pitch: "One sentence"
audience: "Who it's for"
problem: "What it solves"
hero_flow: "The most important user action"
comparable_products: [app1, app2]
last_updated: YYYY-MM-DD
---
```

---

## _grounding/general/competitors.md

```yaml
---
products_analyzed: 5
last_updated: YYYY-MM-DD
---
```

## _grounding/general/audiences.md

```yaml
---
personas_defined: 3
last_updated: YYYY-MM-DD
---
```

## _grounding/general/domain.md

```yaml
---
last_updated: YYYY-MM-DD
---
```

## _grounding/general/design_inspiration.md

```yaml
---
references_collected: 8
last_updated: YYYY-MM-DD
---
```

---

## 1_discovery/2_brand/identity.md

```yaml
---
mood: "calm | bold | professional | playful | ..."
mode: light | dark | both
last_updated: YYYY-MM-DD
---
```

---

## 2_experience/1_journeys/stories.json

JSON file — no frontmatter. Structure:

```json
{
  "version": "1.0",
  "last_updated": "YYYY-MM-DD",
  "personas": [
    {
      "id": "persona_id",
      "name": "Persona Name",
      "role": "role_name",
      "goals": ["..."]
    }
  ],
  "journeys": [
    {
      "id": "journey_id",
      "persona": "persona_id",
      "title": "Journey Title",
      "steps": [
        {
          "action": "What the user does",
          "system_response": "What the system does",
          "acceptance": "EARS-format acceptance criterion"
        }
      ],
      "candidate_features": ["feature_slug"],
      "candidate_entities": ["EntityName"]
    }
  ]
}
```

---

## 2_experience/2_features/\<group\>/\<feature\>.md

```yaml
---
priority: must-have | nice-to-have
roles: [all_users]              # or [admin, member, guest]
permissions:                    # role → allowed actions
  admin: [view, create, edit, delete]
  member: [view, create]
  guest: [view]
story_refs: []                  # journey IDs from stories.json that motivated this feature
agent_notes: |
  Free-form notes from the agent about this feature.
  Used for context across sessions.
screens: []                     # populated by screens skill
data_entities: []               # populated by datamodel skill
last_updated: YYYY-MM-DD
---
```

### screens[] format (populated by downstream skill)

```yaml
screens:
  - path: 2_experience/3_screens/01_user_auth/login.md
```

### data_entities[] format (populated by downstream skill)

```yaml
data_entities: [User, Session]
```

---

## 2_experience/3_screens/\<group\>/\<screen\>.md

```yaml
---
implements:
  - 2_experience/2_features/01_user_auth/login.md
  - 2_experience/2_features/01_user_auth/registration.md
data_entities: [User]
layout: 2_experience/3_screens/00_layout/shell.md
last_updated: YYYY-MM-DD
---
```

---

## 3_blueprint/1_techstack/stack.md

```yaml
---
platform: web | mobile | desktop | api
framework: ""                   # e.g. PostXL, Laravel, Rails, custom
frontend: ""                    # e.g. Vite + React 19
ui_library: ""                  # e.g. @postxl/ui-components, shadcn/ui
backend: ""                     # e.g. NestJS + Fastify + tRPC
orm: ""                         # e.g. Prisma, TypeORM, Drizzle, Ecto
database: ""                    # e.g. PostgreSQL, SQLite
auth: ""                        # e.g. Keycloak, Auth.js, custom
package_manager: ""             # e.g. pnpm, bun, npm
last_updated: YYYY-MM-DD
---
```

---

## 3_blueprint/2_architecture/architecture.md

```yaml
---
apps: []                        # deployable units, e.g. [api, web, worker]
custom_services: []             # runtime services outside the main app
custom_modules: []              # code-level module boundaries within the app
protocols: []                   # e.g. [http, trpc, websocket]
external_integrations: []       # third-party APIs, services
last_updated: YYYY-MM-DD
---
```

---

## 3_blueprint/3_datamodel/feature_map.json

JSON file — no frontmatter. Maps each model to its source feature files:

```json
{
  "last_updated": "YYYY-MM-DD",
  "models": {
    "User": {
      "source_features": [
        "2_experience/2_features/01_user_auth/login.md",
        "2_experience/2_features/01_user_auth/registration.md"
      ]
    }
  }
}
```
