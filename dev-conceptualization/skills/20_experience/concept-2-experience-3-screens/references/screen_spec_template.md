# Screen Spec Template

Every screen file follows this structure. The frontmatter and sections are
mandatory; content varies per screen.

## Frontmatter

```yaml
---
status: draft
implements:
  - 2_experience/2_features/<NN_group>/<feature>.md
data_entities: [<Model>, ...]
layout: 2_experience/3_screens/00_layout/shell.md
last_updated: YYYY-MM-DD
---
```

## Required Sections

### Purpose (3-second test)
One sentence: what does the user immediately understand when this screen loads?

### Route
The URL path (e.g., `/login`, `/dashboard`, `/tasks/:id`).

### Component Inventory (top to bottom)
Numbered list of `@postxl/ui-components` used on this screen.
For DataGrid, specify cell variants per column.
See `references/ui_components.md` for valid names.

### Data Requirements
Which entities from `postxl-schema.json` are displayed or mutated.
If architecture specifies custom protocols (WebSocket, SSE), note them here.

### User Actions
Bullet list of what the user can do, with outcomes.

### States
Named states the screen can be in:
- **Default** — initial view
- **Loading** — async operations in progress
- **Error** — validation or server errors
- **Empty** — no data to display
- **Success** — operation completed

### Template Data
Reference scenarios from `_concept/3_blueprint/3_datamodel/seed.json`:
- `populated` — primary display scenario
- `empty` — empty state
- `edge_cases` — boundary conditions

## Enrichment from User Journeys

When `_concept/2_experience/1_journeys/stories.json` exists, use it to inform screen design:

| Journey element             | Maps to screen section |
|-----------------------------|----------------------|
| Story outcomes              | User Actions         |
| EARS event-driven criteria  | States (conditional UI: disabled buttons, show/hide) |
| Story personas              | Role-based screen visibility |
| Story map sequence          | Navigation flow between screens |
| Downstream candidate_screens | Screen list validation |

## Enrichment from Architecture

When `_concept/3_blueprint/2_architecture/architecture.md` exists:

- Custom protocols (WebSocket, SSE) — note in Data Requirements for real-time screens
- Additional apps (e.g., VM agents) — document communication flow
- Custom modules — reference module-specific API routes beyond standard CRUD
