# Frontmatter Schema

All markdown files in `_concept/` use YAML frontmatter. These are the
standard fields. Skills must use these field names exactly.

## Universal Fields

Every file:

```yaml
---
status: draft | approved | implemented | tested
last_updated: YYYY-MM-DD # ISO date, updated on every write
---
```

## 1_discovery/1_overview/brief.md

```yaml
---
status: draft | approved
complexity_tier: small | standard | complex # determined during overview, controls pipeline behavior
elevator_pitch: 'One sentence'
audience: "Who it's for"
problem: 'What it solves'
hero_flow: 'The most important user action'
comparable_products: [app1, app2]
last_updated: YYYY-MM-DD
---
```

## 1_discovery/2_research/competitors.md

```yaml
---
status: draft | approved
products_analyzed: 5
last_updated: YYYY-MM-DD
---
```

## 1_discovery/2_research/audiences.md

```yaml
---
status: draft | approved
personas_defined: 3
last_updated: YYYY-MM-DD
---
```

## 1_discovery/2_research/domain.md

```yaml
---
status: draft | approved
last_updated: YYYY-MM-DD
---
```

## 1_discovery/2_research/design_inspiration.md

```yaml
---
status: draft | approved
references_collected: 8
last_updated: YYYY-MM-DD
---
```

## 1_discovery/3_brand/identity.md

```yaml
---
status: draft | approved
mood: 'calm | bold | professional | playful | ...'
mode: light | dark | both
last_updated: YYYY-MM-DD
---
```

## 2_experience/1_journeys/stories.json

This file uses JSON, not markdown with frontmatter. The schema is defined in
the `concept-2-experience-1-journeys` skill. Key structure:

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

## 2_experience/2_features/<group>/<feature>.md

```yaml
---
status: draft | approved | implemented | tested
priority: must-have | nice-to-have
roles: [all_users] # or [admin, member, guest]
permissions: # role → [actions] mapping, populated during feature planning
  admin: [view, create, edit, delete]
  member: [view, create]
story_refs: [] # journey IDs from 2_experience/1_journeys/stories.json that motivated this feature
agent_notes: |
  Free-form notes from the agent about this feature.
  Used for context across sessions.
screens: [] # populated by concept-2-experience-3-screens
data_entities: [] # populated by concept-3-blueprint-3-datamodel
last_updated: YYYY-MM-DD
---
```

### screens[] format (populated by downstream skill)

```yaml
screens:
  - path: 2_experience/3_screens/01_user_auth/login.md
    status: draft
```

### data_entities[] format (populated by downstream skill)

```yaml
data_entities: [User, Session]
```

## 3_blueprint/1_techstack/stack.md

```yaml
---
status: draft | approved
platform: web
framework: PostXL
frontend: 'Vite + React 19'
ui_library: '@postxl/ui-components (Radix + Tailwind v4)'
backend: 'NestJS + Fastify + tRPC'
orm: Prisma
database: PostgreSQL
auth: Keycloak
package_manager: pnpm
last_updated: YYYY-MM-DD
---
```

## 3_blueprint/2_architecture/architecture.md

```yaml
---
status: draft | approved
apps: [api, web]
custom_modules: []
protocols: [http, trpc]
external_integrations: []
last_updated: YYYY-MM-DD
---
```

## 2_experience/3_screens/<group>/<screen>.md

```yaml
---
status: draft | approved | mockup_ready | implemented | tested
implements:
  - 2_experience/2_features/01_user_auth/login.md
  - 2_experience/2_features/01_user_auth/registration.md
data_entities: [User]
layout: 2_experience/3_screens/00_layout/shell.md
last_updated: YYYY-MM-DD
---
```

## Status Lifecycle

```
draft → approved → implemented → tested
                       ↑
               mockup_ready (screens only)
```

- `draft` — initial creation, not yet reviewed by user
- `approved` — user has reviewed and confirmed
- `mockup_ready` — visual mockup exists (screens only)
- `implemented` — code exists for this feature/screen
- `tested` — E2E tests pass for this feature/screen
