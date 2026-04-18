---
name: UI Rendering
description: Emit typed UI components into the chat stream using the Agent UI Protocol
source: MIGRATED
version: 1.0.0
keywords: [ui, render, catalog, component, form, select, map, table]
reads_from: [UI_MANIFEST system context]
writes_to: []
---

# UI Rendering Skill

You can render rich interactive components directly in the chat. The client has
told you exactly what it supports via the `<system-context name="UI_MANIFEST">`
block injected into your first prompt. Always check that block before using any
component.

## When to Use UI Rendering

Use a catalog component when:
- You need the user to choose one or more options (use `multi-select` or `single-select`)
- You need structured input (use `form`)
- You want to confirm an irreversible action (use `confirm`)
- You are showing tabular data the user may want to select from (use `data-table`)
- You want to display a map with locations (use `map`)
- You are showing a file or set of files from the workspace (use `file-link`, `file-list`, `file-preview`)
- You want to render formatted content (use `markdown`, `code`, `image`)

Use plain text when the response is a simple explanation or answer.

## Emitting a ui_render Event

Output a JSON object with this shape to render a component. The bridge will
parse it and broadcast it to the client as a `UIRenderEvent`.

```json
{
  "type": "ui_render",
  "id": "<uuid-v4>",
  "manifest_version": "1.0",
  "component": {
    "kind": "catalog",
    "name": "<component-name>",
    "props": { ... }
  },
  "on_interact": {
    "action": "reply",
    "template": "User selected: {{value}}"
  }
}
```

- `id` must be a UUID v4 (generate a unique string like `550e8400-e29b-41d4-a716-446655440000`)
- `manifest_version` must match the version in `<system-context name="UI_MANIFEST">`
- `name` must be a key from `manifest.components` — check the manifest first
- `on_interact.action`:
  - `"reply"` — when the user interacts, the interaction result is automatically sent back to you as a new user message using the `template` string
  - `"none"` — interaction is logged but does not trigger a new agent turn

## Component Prop Reference

### `multi-select`
```json
{
  "question": "Which topics interest you?",
  "options": [
    { "label": "Research", "description": "Deep literature review" },
    { "label": "Writing", "description": "Draft and edit documents" }
  ],
  "minSelect": 1,
  "maxSelect": 3
}
```

### `single-select`
```json
{
  "question": "How would you like to proceed?",
  "options": [
    { "label": "Start fresh" },
    { "label": "Continue from last session" }
  ]
}
```

### `confirm`
```json
{
  "message": "This will delete all files in the workspace. Are you sure?",
  "variant": "danger",
  "confirmLabel": "Delete everything",
  "cancelLabel": "Cancel"
}
```

### `form`
```json
{
  "title": "New project settings",
  "fields": [
    { "key": "name", "label": "Project name", "type": "text", "required": true },
    { "key": "budget", "label": "Budget (€)", "type": "number" },
    { "key": "deadline", "label": "Deadline", "type": "date" },
    {
      "key": "priority",
      "label": "Priority",
      "type": "select",
      "options": ["Low", "Medium", "High"]
    }
  ]
}
```

### `data-table`
```json
{
  "columns": [
    { "key": "name", "label": "Name" },
    { "key": "status", "label": "Status" },
    { "key": "date", "label": "Due date" }
  ],
  "rows": [
    { "name": "Task A", "status": "open", "date": "2026-05-01" }
  ],
  "selectable": true
}
```

### `markdown`
```json
{
  "content": "## Analysis\n\nHere is the **summary**...",
  "slug": "<project-slug>"
}
```

### `code`
```json
{
  "code": "console.log('hello')",
  "language": "javascript",
  "filename": "index.js"
}
```

### `image`
```json
{
  "url": "workspace://images/chart.png",
  "alt": "Revenue chart Q1 2026",
  "slug": "<project-slug>"
}
```

### `file-link`
```json
{
  "path": "workspace://reports/analysis.pdf",
  "label": "Q1 Analysis Report",
  "action": "auto",
  "slug": "<project-slug>"
}
```

### `file-list`
```json
{
  "title": "Project files",
  "files": [
    { "path": "workspace://data/survey.csv", "label": "Survey data" },
    { "path": "workspace://output/report.pdf", "label": "Final report" }
  ],
  "slug": "<project-slug>"
}
```

### `location`
```json
{
  "name": "Stadtbibliothek Stuttgart",
  "address": "Mailänder Platz 1, 70173 Stuttgart",
  "coordinates": { "lat": 48.7893, "lng": 9.1839 },
  "actions": ["open-map", "navigate", "copy-address"]
}
```

### `map`
```json
{
  "geojson": "workspace://data/locations.geojson",
  "height": 450,
  "provider": "auto",
  "featureClick": true,
  "slug": "<project-slug>"
}
```

## Handling ui_interaction Commands

When a user interacts with a component (clicks a button, submits a form, selects a row),
you receive a `ui_interaction` command formatted as a user message:

```
[UI Interaction] render_id=<id> action=<action> value=<JSON>
```

Parse the `value` based on the component:
- `multi-select`: `{ "selected": ["Option A", "Option B"] }`
- `single-select`: `{ "selected": "Option A" }`
- `confirm`: action is `"confirm"` or `"cancel"`, value is `null`
- `form`: `{ "values": { "name": "...", "budget": 5000 } }`
- `data-table`: `{ "selectedIndices": [0, 2], "selectedRows": [...] }`
- `feature-click` (map): `{ "featureId": "...", "properties": {...} }`

## Rules

- Always check `<system-context name="UI_MANIFEST">` before rendering a component
- Only use components listed in `manifest.components` — others will silently fail
- Always include `on_interact` if you expect to receive the result
- Use `"action": "none"` for display-only components (markdown, image, code, file-link)
- Generate a new UUID for each `id` — never reuse IDs within a session
- For `slug` props: use the project's slug (it is in your context / system prompt)
