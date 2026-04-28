# Agent Component Rendering Reference

How to render and interact with dynamic components from within an agent session.
This reference is for agents (LLMs), not human developers.

---

## How Components Become Available

At session start, the runtime discovers all registered components and sends them
to you as `AVAILABLE_COMPONENTS` in your system context. Each entry contains:

- **name** - identifier used in `ui_render`
- **targets** - where it can render: `chat`, `preview`, `modal`
- **interactions** - action names the component can emit back to you
- **schema** - JSON Schema describing the props you pass
- **messageType** (optional) - for chat renderers that handle custom message types

You do NOT need to emit `component_register` yourself - the runtime handles
registration. You only need to emit render/update/clear events.

---

## Rendering a Component

Emit a `ui_render` event to display a component:

```json
{
  "type": "ui_render",
  "id": "render-001",
  "target": "preview",
  "persistState": true,
  "component": {
    "kind": "catalog",
    "name": "issue-tracker",
    "props": {
      "issues": [
        {
          "id": "issue-1",
          "title": "Set up authentication",
          "status": "open",
          "priority": "high",
          "assignee": "agent",
          "createdBy": "user",
          "createdAt": "2026-04-28T10:00:00Z"
        }
      ],
      "sessionId": "session-abc"
    }
  }
}
```

Key fields:

| Field | Description |
|-------|-------------|
| `id` | Unique identifier. Reuse it for updates and clearing. |
| `target` | `"chat"` (inline), `"preview"` (sidebar), `"modal"` (overlay). |
| `persistState` | `true` if the component should survive session hibernation. Use for preview components. |
| `component.kind` | Always `"catalog"` for registered components. |
| `component.name` | Must match a name from AVAILABLE_COMPONENTS. |
| `component.props` | Data to pass. Must conform to the component's schema. |

### Target Selection

- **`preview`** - right-side panel. Use for persistent tools (issue tracker, file browser, dashboards). Only one preview component active at a time.
- **`chat`** - inline in the conversation stream. Use for data displays, results, cards.
- **`modal`** - overlay dialog. Use for wizards, confirmations, large forms.

---

## Updating a Component

Emit `ui_render_update` to patch props on a live component without re-rendering
from scratch:

```json
{
  "type": "ui_render_update",
  "id": "render-001",
  "patch": {
    "issues": [
      { "id": "issue-1", "title": "Set up authentication", "status": "done", "priority": "high", "assignee": "agent", "createdBy": "user", "createdAt": "2026-04-28T10:00:00Z" },
      { "id": "issue-2", "title": "Add unit tests", "status": "open", "priority": "medium", "assignee": "agent", "createdBy": "agent", "createdAt": "2026-04-28T11:00:00Z" }
    ]
  }
}
```

The `patch` is shallow-merged into existing props. Only include fields you are
changing. For array props (like `issues`), always send the full array - the
component replaces the entire list, it does not merge individual items.

---

## Reading Component State

Components write UI state (filters, sort order, selections) to shared state
under the key `component:<render-id>`. You can read this from the store snapshot
to understand what the user has configured:

```
component:render-001 = { "filter": "open", "sortBy": "priority" }
```

Use this to:
- Know which filter the user has applied before updating data
- Restore user preferences when re-rendering
- Understand user intent from their UI interactions

---

## Responding to Interactions

When a user interacts with a component, you receive a structured prompt:

```
[UI Interaction]
render_id: render-001
action: create
value: {"title":"Fix login bug","priority":"high","assignee":"agent"}
timestamp: 2026-04-28T10:05:00Z
```

How to respond:

1. **Acknowledge** the interaction in your response (e.g., "I'll add that issue.")
2. **Process** the action (add the item, apply the update, generate the export)
3. **Update** the component if needed via `ui_render_update`

Common interaction patterns:

| Action | What It Means | Your Response |
|--------|---------------|---------------|
| `create` | User created something via the component UI | Add it to your data, emit `ui_render_update` with the new list |
| `update` | User changed a field (status, priority, etc.) | Apply the change, emit `ui_render_update` |
| `select` | User clicked/selected an item | Acknowledge; provide context about the selected item |
| `export` | User requested an export | Generate the requested format and include it in your response |

---

## Clearing a Component

Emit `ui_clear` to remove a component from its render target:

```json
{
  "type": "ui_clear",
  "render_id": "render-001"
}
```

Clear when:
- The task that needed the component is complete
- The user asks to close/dismiss the component
- You are replacing it with a different component in the same target

---

## When to Render Components

**Do render when:**
- The user explicitly asks ("show the issue tracker", "open the dashboard")
- A workflow step produces structured output that a component can display better than text
- You are managing items (issues, tasks) that benefit from an interactive UI

**Do NOT render when:**
- The information fits naturally in a text response
- The user has not indicated they want a visual component
- You are uncertain whether a component exists for this purpose (check AVAILABLE_COMPONENTS first)
- A preview component is already showing something the user is actively using

**Single-active rule for preview:** Only one component renders in the preview target
at a time. Rendering a new preview component replaces the current one.

---

## Custom Message Types

Some components handle custom message types in the chat stream (e.g., GIFs).
These are rendered automatically - you emit a `finished` event with `customType`
and `customData`:

```json
{
  "type": "finished",
  "summary": "[gif: funny cat]",
  "customType": "gif",
  "customData": {
    "url": "https://media.example.com/funny-cat.gif",
    "alt": "funny cat",
    "width": 480,
    "height": 270
  }
}
```

The client looks up the registered chat-renderer for this `customType` and renders
the component automatically. Clients without the renderer fall back to displaying
`summary` as plain text.
