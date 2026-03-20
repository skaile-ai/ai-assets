# Frontmatter Schema

All markdown files in `_concept/` use YAML frontmatter. These are the
standard fields. Skills must use these field names exactly.

## Universal Fields

Every file:

```yaml
---
last_updated: YYYY-MM-DD          # ISO date, updated on every write
---
```

## Optional Implementation Tracking

For features and screens that enter the implementation phase:

```yaml
impl_status: pending | implemented | tested
```

This is optional and only relevant during implementation/testing phases.

## 01_project/brief.md

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

## 02_research/competitors.md

```yaml
---
products_analyzed: 5
last_updated: YYYY-MM-DD
---
```

## 02_research/audiences.md

```yaml
---
personas_defined: 3
last_updated: YYYY-MM-DD
---
```

## 02_research/domain.md

```yaml
---
last_updated: YYYY-MM-DD
---
```

## 02_research/design_inspiration.md

```yaml
---
references_collected: 8
last_updated: YYYY-MM-DD
---
```

## 03_features/<group>/<feature>.md

```yaml
---
priority: must-have | nice-to-have
roles: [all_users]                # or [admin, member, guest]
agent_notes: |
  Free-form notes from the agent about this feature.
  Used for context across sessions.
screens: []                       # populated by cf_concept_ui_screens
data_entities: []                 # populated by cf_concept_datamodel
last_updated: YYYY-MM-DD
---
```

### screens[] format (populated by downstream skill)

```yaml
screens:
  - path: 07_screens/01_user_auth/login.md
```

### data_entities[] format (populated by downstream skill)

```yaml
data_entities: [user, session]
```

## 04_brand/identity.md

```yaml
---
mood: "calm | bold | professional | playful | ..."
mode: light | dark | both
last_updated: YYYY-MM-DD
---
```

## 05_techstack/stack.md

```yaml
---
platform: web | mobile | desktop | api
frontend: ""
ui_library: ""
backend: ""
database: ""
auth: ""
hosting: ""
package_manager: ""
last_updated: YYYY-MM-DD
---
```

## 05b_architecture/architecture.md

```yaml
---
apps: []
custom_services: []
protocols: []
external_integrations: []
last_updated: YYYY-MM-DD
---
```

## 07_screens/<group>/<screen>.md

```yaml
---
implements:
  - 03_features/01_user_auth/login.md
  - 03_features/01_user_auth/registration.md
data_entities: [user]
layout: 07_screens/00_layout/shell.md
last_updated: YYYY-MM-DD
---
```
