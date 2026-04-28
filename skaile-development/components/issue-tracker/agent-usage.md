# Issue Tracker — Agent Usage Reference

How to render and manage the issue tracker component from within an agent session.

## Prerequisites

The component must be registered before rendering. Emit a `component_register`
event once at session start (or before the first render):

```json
{
  "type": "component_register",
  "components": {
    "issue-tracker": {
      "schema": {
        "type": "object",
        "properties": {
          "issues": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "id": { "type": "string" },
                "title": { "type": "string" },
                "status": { "type": "string", "enum": ["open", "in-progress", "done"] },
                "priority": { "type": "string", "enum": ["high", "medium", "low"] },
                "assignee": { "type": "string" },
                "createdBy": { "type": "string" },
                "createdAt": { "type": "string" },
                "sourceMessageId": { "type": "string" },
                "notes": { "type": "string" }
              }
            }
          },
          "sessionId": { "type": "string" }
        }
      },
      "interactions": ["create", "update", "select", "export"],
      "targets": ["preview"],
      "source": {
        "kind": "web-component",
        "url": "asset://skaile-development/components/issue-tracker/issue-tracker.js",
        "tagName": "skaile-issue-tracker"
      },
      "fallback": "## Issues\n{{#each issues}}\n- [{{status}}] **{{title}}** ({{priority}}) - assigned to {{assignee}}\n{{/each}}"
    }
  }
}
```

## Rendering

Emit a `ui_render` event to display the issue tracker in the preview pane:

```json
{
  "type": "ui_render",
  "id": "<unique-render-id>",
  "target": "preview",
  "persistState": true,
  "component": {
    "kind": "catalog",
    "name": "issue-tracker",
    "props": {
      "issues": [
        {
          "id": "issue-1",
          "title": "Set up authentication module",
          "status": "open",
          "priority": "high",
          "assignee": "agent",
          "createdBy": "user",
          "createdAt": "2026-04-28T10:00:00Z",
          "sourceMessageId": "msg-42",
          "notes": "Keycloak OIDC integration"
        }
      ],
      "sessionId": "<current-session-id>"
    }
  }
}
```

Key fields:

| Field | Value | Why |
|---|---|---|
| `target` | `"preview"` | Renders in the right-side preview pane, not inline in chat |
| `persistState` | `true` | Component state survives session hibernation and resume |
| `component.kind` | `"catalog"` | Looks up the component definition from the registry |
| `component.name` | `"issue-tracker"` | Matches the key in `component_register` |

## Updating Issues

To update the issue list (add, modify, or remove issues), emit a `ui_render_update`:

```json
{
  "type": "ui_render_update",
  "id": "<same-render-id>",
  "patch": {
    "issues": [ /* full updated array */ ]
  }
}
```

The patch is shallow-merged into the existing props, so you only need to include
the fields you are changing. For `issues`, always send the full array (the
component replaces the list, it does not merge individual items).

## Handling User Interactions

When the user interacts with the issue tracker, the platform sends a
`ui_interaction` command. The runner converts this to a structured prompt:

```
[UI Interaction]
render_id: <render-id>
action: create
value: {"title":"New issue from user","priority":"medium"}
timestamp: 2026-04-28T10:05:00Z
```

Supported interaction actions:

| Action | Value Shape | Agent Should |
|---|---|---|
| `create` | `{ title, priority?, assignee?, notes? }` | Add the issue to the tracker, emit `ui_render_update` with the new list |
| `update` | `{ id, status?, priority?, assignee?, notes? }` | Apply the change, emit `ui_render_update` |
| `select` | `{ id, sourceMessageId? }` | Acknowledge; optionally provide context about the selected issue |
| `export` | `{ format: "markdown" }` | Generate a summary of all issues in the requested format |

## Clearing

To remove the issue tracker from the preview pane:

```json
{
  "type": "ui_clear",
  "render_id": "<same-render-id>"
}
```

## State Persistence

When `persistState: true` is set, the component's `state_update` events (emitted
via the `component:<render-id>` shared state store) are captured during session
hibernation and restored on wake. The agent does not need to re-render the
component after a session resume — the platform handles restoration automatically.
