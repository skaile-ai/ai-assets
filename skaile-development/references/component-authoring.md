# Component Authoring Guide

Build dynamic web components for the Agent App Protocol. This guide covers
the full lifecycle: directory structure, COMPONENT.md manifest, web component
contract, styling, agent integration, testing, and pre-flight checklist.

---

## Quick Start

A component lives in `ai-assets/skaile-development/components/<name>/`:

```
my-component/
  COMPONENT.md         # Manifest (YAML frontmatter + description)
  my-component.js      # Web component ES module
  agent-usage.md       # Agent rendering reference (optional but recommended)
  test-harness.html    # Standalone browser test page (optional)
```

Minimal working example - a status badge component:

**COMPONENT.md:**

```yaml
---
name: status-badge
description: Displays a colored status indicator
version: 1.0.0
keywords: [status, indicator, badge]

provides:
  - type: component
    component: { kind: web-component, url: ./status-badge.js, tagName: skaile-status-badge }
    schema:
      type: object
      properties:
        label: { type: string }
        status: { type: string, enum: [success, warning, error, info] }
    interactions: []
    targets: [chat]
    fallback: "**{{label}}**: {{status}}"
---

# Status Badge

Renders a colored status indicator inline in chat.
```

**status-badge.js:**

```javascript
class SkaileStatusBadge extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({ mode: "open" });
    this._label = "";
    this._status = "info";
  }

  set label(v) { this._label = v || ""; this._render(); }
  get label() { return this._label; }
  set status(v) { this._status = v || "info"; this._render(); }
  get status() { return this._status; }

  connectedCallback() { this._render(); }

  _render() {
    const colors = { success: "#22c55e", warning: "#f59e0b", error: "#ef4444", info: "#3b82f6" };
    this.shadowRoot.innerHTML = `
      <style>
        :host { display: inline-flex; align-items: center; gap: 6px;
          font-family: var(--font-sans, system-ui, sans-serif);
          color: var(--foreground, #1a1a1a); font-size: 13px; }
        .dot { width: 8px; height: 8px; border-radius: 50%; }
      </style>
      <span class="dot" style="background:${colors[this._status] || colors.info}"></span>
      <span>${this._esc(this._label)}</span>
    `;
  }

  _esc(s) {
    return String(s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;")
      .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }
}

if (!customElements.get("skaile-status-badge")) {
  customElements.define("skaile-status-badge", SkaileStatusBadge);
}
export { SkaileStatusBadge };
```

---

## COMPONENT.md Reference

The manifest uses YAML frontmatter followed by a markdown description body.
Every field is documented below.

### Top-Level Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Lowercase, hyphenated identifier. Must match directory name. |
| `description` | string | Yes | One-line summary (used in catalogs and tooltips). |
| `version` | string | Yes | SemVer string (e.g. `1.0.0`). |
| `keywords` | string[] | No | Discovery tags for search and categorization. |
| `config` | object | No | Per-instance configuration options. See Config section. |
| `provides` | array | Yes | What this component asset provides. Each entry is a `provides` block. |

### Config Section

Config entries define settings that can be overridden per-instance (e.g., API keys,
default values). Each key is a config option:

```yaml
config:
  api_key:
    description: "API key for the search provider"
    required: false
  provider:
    description: "Which API to use"
    default: klipy
    enum: [klipy, giphy, tenor]
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `description` | string | Yes | What this config option controls. |
| `required` | boolean | No | Whether the component fails without it. Default: false. |
| `default` | any | No | Default value when not configured. |
| `enum` | array | No | Allowed values. |

### Provides Blocks

Each `provides` entry declares a capability. Three types exist:

#### Type: `component` (generic component)

The most common type. Renders in chat, preview, or modal via `ui_render`.

```yaml
provides:
  - type: component
    component: { kind: web-component, url: ./my-component.js, tagName: skaile-my-component }
    schema:
      type: object
      properties:
        items: { type: array, items: { type: object, properties: { ... } } }
        title: { type: string }
    interactions: [create, update, select, export]
    targets: [preview]
    fallback: |
      ## {{title}}
      {{#each items}}
      - {{name}}: {{value}}
      {{/each}}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | `"component"` | Yes | Marks this as a generic component. |
| `component` | object | Yes | Where the implementation lives. See Component Source. |
| `schema` | object | Yes | JSON Schema for the component's props. |
| `interactions` | string[] | Yes | Interaction action names the component emits. Empty array `[]` if none. |
| `targets` | string[] | Yes | Render targets: `chat`, `preview`, `modal`. |
| `fallback` | string | No | Handlebars template for clients that cannot load the component. |

#### Type: `input-extension` (augments text input)

Triggered by a prefix in the user's text input. Produces content (text or a
custom message type).

```yaml
provides:
  - type: input-extension
    trigger: { type: prefix, value: "/gif" }
    component: { kind: web-component, url: ./gif-picker.js, tagName: skaile-gif-picker }
    produces: { type: message, messageType: gif }
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | `"input-extension"` | Yes | Marks this as an input extension. |
| `trigger` | object | Yes | How the extension activates. Currently only `{ type: prefix, value: "..." }`. |
| `component` | object | Yes | Component to render when triggered. |
| `produces` | object | Yes | What happens on selection. `{ type: "text" }` inserts text. `{ type: "message", messageType: "..." }` creates a custom-typed message. |

#### Type: `chat-renderer` (renders a custom message type)

Renders messages of a specific custom type in the chat stream.

```yaml
provides:
  - type: chat-renderer
    messageType: gif
    component: { kind: web-component, url: ./gif-renderer.js, tagName: skaile-gif-display }
    schema:
      type: object
      properties:
        url: { type: string }
        alt: { type: string }
    interactions: []
    targets: [chat]
    fallback: "![{{alt}}]({{url}})"
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | `"chat-renderer"` | Yes | Marks this as a chat renderer. |
| `messageType` | string | Yes | Which custom message type to render (matches `customType` on messages). |
| `component` | object | Yes | Component to render the message. |
| `schema` | object | Yes | JSON Schema for the message's `customData`. |
| `interactions` | string[] | Yes | Interaction actions (usually `[]` for renderers). |
| `targets` | string[] | Yes | Usually `[chat]`. |
| `fallback` | string | No | Handlebars template for unsupported clients. |

### Component Source

```yaml
component:
  kind: web-component       # "web-component" or "iframe"
  url: ./my-component.js    # Relative path or asset:// URL
  tagName: skaile-my-component  # Custom element tag name (must include hyphen)
```

| Field | Type | Description |
|-------|------|-------------|
| `kind` | string | `"web-component"` (ES module, custom element) or `"iframe"` (sandboxed). |
| `url` | string | Path to the JS file. Relative (`./`) in COMPONENT.md, resolved to `asset://` at runtime. |
| `tagName` | string | HTML custom element name. Must start with `skaile-` and contain a hyphen. |

### Markdown Body

After the YAML frontmatter, include a markdown description with:

- **H1 title** matching the component name
- **Usage** section explaining how users interact with it
- **Interactions table** (if the component has interactions) showing action, trigger, and effect

See the issue-tracker COMPONENT.md for a complete example.

---

## Web Component Contract

All components are vanilla web components (custom elements with shadow DOM).
No framework dependencies - the JS file must be self-contained.

### Shadow DOM (required)

Always use shadow DOM for style encapsulation:

```javascript
constructor() {
  super();
  this.attachShadow({ mode: "open" });
}
```

### Properties

The host application (React 19, Vue, or vanilla JS) sets props as **JavaScript
property setters**, not HTML attributes. Use attribute observation only for simple
string/number values that might be set via HTML.

```javascript
// Property setter for complex types (arrays, objects)
set items(val) {
  this._items = Array.isArray(val) ? val : [];
  this._render();
}
get items() { return this._items; }

// observedAttributes for simple string/number props
static get observedAttributes() { return ["filter", "sort-by"]; }

attributeChangedCallback(name, _old, val) {
  if (name === "filter") { this._filter = val || "all"; this._render(); }
  else if (name === "sort-by") { this._sortBy = val || "priority"; this._render(); }
}
```

Rules:
- Complex types (arrays, objects) must use property setters - never attribute serialization.
- Property setters must trigger `_render()` to update the DOM.
- Attribute names use kebab-case (`sort-by`), property names use camelCase (`sortBy`).
- Always provide a getter for every setter.

### Events

Components emit three standard event types. All events must use
`bubbles: true, composed: true` to cross the shadow DOM boundary.

#### `select` - primary selection action

Emitted when the user selects an item. Detail contains the selected data:

```javascript
this.dispatchEvent(new CustomEvent("select", {
  bubbles: true,
  composed: true,
  detail: { url: gif.url, alt: gif.alt, width: gif.width, height: gif.height }
}));
```

Use for: pickers, search results, list item selection.

#### `interaction` - typed action with payload

Emitted when the user performs a named action. Detail is `{ action, value }`:

```javascript
// Create action
this._emit("interaction", {
  action: "create",
  value: { id: this._genId(), title: "New item", priority: "medium" }
});

// Update action
this._emit("interaction", {
  action: "update",
  value: { id: "item-1", field: "status", value: "done" }
});

// Export action
this._emit("interaction", {
  action: "export",
  value: { format: "markdown" }
});
```

Use for: CRUD operations, exports, bulk actions. Declare action names in `interactions[]`.

#### `state_update` - persist component UI state

Emitted when the user changes UI state that should persist (filters, sort order,
scroll position). Detail is `{ store, state }`:

```javascript
this._emit("state_update", {
  store: this._storeKey(),  // "component:<id>"
  state: { filter: this._filter, sortBy: this._sortBy }
});
```

The store key format is `component:<element-id>`, where `element-id` comes from
the `id` attribute set by the host. This maps to shared state storage.

### Event Helper Pattern

Both existing components use a common `_emit` helper:

```javascript
_emit(name, detail) {
  this.dispatchEvent(new CustomEvent(name, {
    detail,
    bubbles: true,
    composed: true,
  }));
}
```

### Rendering Pattern

Components rebuild the full shadow DOM innerHTML on each render:

```javascript
_render() {
  if (!this.shadowRoot) return;
  // Build HTML string
  let h = "";
  h += '<div class="header">...</div>';
  h += '<div class="body">...</div>';

  this.shadowRoot.innerHTML = "<style>" + MyComponent._styles + "</style>" + h;
  this._bind();
}
```

Rules:
- Store CSS in a static `_styles` property (string).
- Rebuild innerHTML completely on each `_render()` call.
- Call `_bind()` after innerHTML assignment to attach event listeners.
- Guard with `if (!this.shadowRoot) return;`.

### Event Binding Pattern

After rendering, attach event listeners to dynamically created elements:

```javascript
_bind() {
  const root = this.shadowRoot;

  // Event delegation via data attributes
  for (const btn of root.querySelectorAll("[data-action]")) {
    const action = btn.getAttribute("data-action");
    btn.addEventListener("click", () => this._handleAction(action));
  }

  // Form handling
  const form = root.querySelector("form");
  if (form) {
    form.addEventListener("submit", (ev) => {
      ev.preventDefault();
      // ...process form...
    });
  }
}
```

### HTML Escaping (required)

Always escape user-provided content before inserting into innerHTML:

```javascript
_esc(s) {
  return String(s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;")
    .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
```

### Lifecycle

```javascript
connectedCallback() {
  this._render();  // Initial render when element enters DOM
}

disconnectedCallback() {
  // Cleanup: clear timers, abort fetch controllers, remove global listeners
  if (this._debounceTimer) clearTimeout(this._debounceTimer);
}
```

### Custom Element Registration

Guard against double-registration:

```javascript
if (!customElements.get("skaile-my-component")) {
  customElements.define("skaile-my-component", SkaileMyComponent);
}
export { SkaileMyComponent };
```

---

## Styling Reference

Components use CSS custom properties (design tokens) provided by the host
application. Always use tokens with fallback values so the component works
standalone.

### Available Theme Tokens

These are the tokens used by existing components. Use these for consistency:

| Token | Fallback | Usage |
|-------|----------|-------|
| `--font-sans` | `system-ui, sans-serif` | Base font family |
| `--foreground` | `#1a1a1a` | Primary text color |
| `--background` | `#fff` | Page/container background |
| `--surface` | `#fff` | Card/panel background (use for elevated surfaces) |
| `--border` | `#e5e5e5` | Border color for dividers, inputs, cards |
| `--primary` | `#2563eb` | Primary action color (buttons, links, focus rings) |
| `--muted` | `#f5f5f5` | Muted background (empty states, placeholders) |
| `--muted-foreground` | `#666` | Secondary/muted text color |
| `--accent` | `#f0f0f0` | Accent background (hover states, badges, chips) |
| `--destructive` | `#ef4444` | Error/danger color |
| `--radius` | `8px` | Border radius for cards, buttons, inputs |

### Token Usage Examples

```css
:host {
  display: block;
  font-family: var(--font-sans, system-ui, sans-serif);
  color: var(--foreground, #1a1a1a);
  background: var(--background, #fff);
  border: 1px solid var(--border, #e5e5e5);
  border-radius: var(--radius, 8px);
  font-size: 13px;
}

/* Buttons */
.btn-primary {
  background: var(--primary, #2563eb);
  color: #fff;
  border-radius: var(--radius, 8px);
}

/* Focus ring */
.btn:focus-visible {
  outline: 2px solid var(--primary, #2563eb);
  outline-offset: 1px;
}

/* Muted text */
.meta {
  color: var(--muted-foreground, #666);
  font-size: 12px;
}

/* Hover state */
.item:hover {
  background: var(--accent, #f0f0f0);
}
```

### Dark Mode

Theme tokens switch automatically when the host app enters dark mode -
you do not need `@media (prefers-color-scheme: dark)` inside the component.
The host sets different token values for dark mode.

For standalone test harnesses, simulate dark mode with a CSS class:

```css
.dark {
  --foreground: #f3f4f6;
  --background: #1f2937;
  --surface: #1f2937;
  --border: #374151;
  --primary: #60a5fa;
  --muted: #374151;
  --muted-foreground: #9ca3af;
  --accent: #374151;
}
```

### Shadow DOM CSS Patterns

```css
/* :host selector for component root */
:host {
  display: block;
  overflow: hidden;
}

/* :host with conditions */
:host([hidden]) { display: none; }

/* Internal layout */
.container { padding: 8px 10px; }
```

Do NOT use:
- `:host-context()` - limited browser support
- `::slotted()` - components are self-contained, no slots
- CSS `@import` - adds network requests inside shadow DOM
- Hardcoded colors - always use tokens with fallbacks

### Responsive Design

Components must work at 280px (sidebar/panel) to 800px (full preview pane).

```css
/* Responsive grid */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 4px;
}

/* Responsive header */
.header {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
```

---

## Agent Integration

### agent-usage.md

Create `agent-usage.md` in the component directory. This file teaches agents
how to render and interact with your component. It is NOT for human developers -
write it for an LLM that emits JSON events.

Required sections:

1. **Prerequisites** - `component_register` event with full schema
2. **Rendering** - `ui_render` event with example props
3. **Updating** - `ui_render_update` with patch examples (if applicable)
4. **Handling Interactions** - how `ui_interaction` is converted to prompts, with a table of actions
5. **Clearing** - `ui_clear` event
6. **State Persistence** - behavior when `persistState: true`

See `components/issue-tracker/agent-usage.md` for the reference implementation.

### How the Agent Sees Components

At session start, registered components appear in the agent's context as
`AVAILABLE_COMPONENTS`. The agent can then decide to render them.

### Protocol Events

The agent uses three events to manage components:

**`ui_render`** - display a component:

```json
{
  "type": "ui_render",
  "id": "<unique-render-id>",
  "target": "preview",
  "persistState": true,
  "component": {
    "kind": "catalog",
    "name": "issue-tracker",
    "props": { "issues": [...], "sessionId": "..." }
  }
}
```

**`ui_render_update`** - patch props on a live component:

```json
{
  "type": "ui_render_update",
  "id": "<same-render-id>",
  "patch": { "issues": [...updated...] }
}
```

**`ui_clear`** - remove a component:

```json
{
  "type": "ui_clear",
  "render_id": "<same-render-id>"
}
```

### Interaction Flow

1. User interacts with the component (click, form submit, etc.)
2. Component emits `interaction` event with `{ action, value }`
3. Platform converts to `ui_interaction` command
4. Runner formats as structured prompt for the agent
5. Agent processes and responds (may emit `ui_render_update`)

---

## Testing

### Test Harness (standalone HTML)

Create `test-harness.html` in the component directory. See the template at
`references/component-test-harness.html`. The harness should:

- Load the component JS as an ES module
- Inject sample data via property setters
- Log all events (`select`, `interaction`, `state_update`)
- Toggle dark mode
- Test at different widths (resize the browser)

### Manual Verification Checklist

- [ ] Component renders with sample data
- [ ] Component renders with empty data (empty state)
- [ ] Component renders at 280px width
- [ ] Component renders at 800px width
- [ ] All interactive elements are keyboard-accessible (Tab + Enter)
- [ ] Focus is visible on interactive elements
- [ ] Events fire with correct detail shape
- [ ] Dark mode renders correctly
- [ ] HTML content is escaped (test with `<script>` in data)

---

## Patterns

### Filter/Sort Component (like issue-tracker)

A list with controls that filter and sort items.

Key elements:
- Header with filter/sort dropdowns
- Item list that re-renders on filter/sort change
- `state_update` event when filter/sort changes (for persistence)
- `interaction` event for CRUD actions
- `select` event for item selection

```javascript
// State
this._items = [];
this._filter = "all";
this._sortBy = "priority";

// Filtered + sorted items
_filteredItems() {
  let result = this._items.slice();
  if (this._filter === "open") result = result.filter(i => i.status !== "done");
  if (this._sortBy === "priority") result.sort((a, b) => P_ORD[a.priority] - P_ORD[b.priority]);
  return result;
}

// Filter change emits state_update
_onFilterChange(filter) {
  this._filter = filter;
  this._emit("state_update", {
    store: this._storeKey(),
    state: { filter: this._filter, sortBy: this._sortBy }
  });
  this._render();
}
```

Interactions: `[create, update, select, export]`
Targets: `[preview]` (sidebar)

### Search Picker (like gif-picker)

A search box with results grid. User selects one item.

Key elements:
- Search input with debounced API calls
- Results grid with lazy-loaded images
- `select` event with the chosen item's data
- Loading/error/empty states
- No `interaction` or `state_update` (picker is transient)

```javascript
// Debounced search
_debounceSearch() {
  if (this._timer) clearTimeout(this._timer);
  this._timer = setTimeout(() => this._search(), 300);
}

// Selection emits select event
_handleSelect(item) {
  this.dispatchEvent(new CustomEvent("select", {
    bubbles: true, composed: true,
    detail: { url: item.url, alt: item.alt, width: item.width, height: item.height }
  }));
}
```

Interactions: `[]` (selection via `select` event, not `interaction`)
Targets: input-extension uses `produces: { type: message, messageType: ... }`

### Data Display with Export

A read-only data view (table, chart, summary card) with an export button.

Key elements:
- Data visualization (table rows, chart, stats grid)
- Export button that emits an `interaction` with `action: "export"`
- Optional filter controls
- `state_update` for view preferences

```javascript
// Export interaction
_handleExport(format) {
  this._emit("interaction", {
    action: "export",
    value: { format, data: this._items }
  });
}
```

Interactions: `[export]` (optionally `[select, export]`)
Targets: `[preview]` or `[chat, preview]`

### Multi-Step Wizard

A form with sequential steps (onboarding, configuration, multi-part input).

Key elements:
- Step indicator showing current/total progress
- Next/Back navigation
- Per-step validation before advancing
- `interaction` event on completion with full collected data
- `state_update` to persist partial progress

```javascript
// State
this._step = 0;
this._steps = ["basic", "details", "confirm"];
this._data = {};

// Step navigation
_next() {
  if (!this._validate()) return;
  if (this._step < this._steps.length - 1) {
    this._step++;
    this._emitStateUpdate();
    this._render();
  } else {
    this._emit("interaction", { action: "submit", value: this._data });
  }
}

_back() {
  if (this._step > 0) { this._step--; this._render(); }
}
```

Interactions: `[submit]` (or `[submit, cancel]`)
Targets: `[modal]` or `[preview]`

---

## Accessibility

- All interactive elements must be keyboard-reachable (Tab order)
- Buttons and clickable elements must use `<button>` elements, not `<div onclick>`
- Focus must be visible: use `focus-visible` with `outline: 2px solid var(--primary)`
- Provide `title` or `aria-label` on icon-only buttons
- Use semantic HTML where possible (`<form>`, `<select>`, `<label>`)

---

## Size Budget

| Category | Target | Hard Limit |
|----------|--------|------------|
| Simple component (badge, indicator) | <5 KB | 15 KB |
| Standard component (picker, list) | <15 KB | 30 KB |
| Complex component (tracker, editor) | <30 KB | 50 KB |

Measured as uncompressed JS file size. Components must be self-contained -
no external dependencies or CDN imports.

---

## Pre-Flight Checklist

Before submitting a component:

- [ ] COMPONENT.md has valid YAML frontmatter with all required fields
- [ ] `name` matches directory name and uses lowercase-hyphenated format
- [ ] `tagName` starts with `skaile-` and contains a hyphen
- [ ] Web component uses `attachShadow({ mode: "open" })`
- [ ] Complex props use JS property setters (not attributes)
- [ ] All events use `bubbles: true, composed: true`
- [ ] Events use standard names: `select`, `interaction`, `state_update`
- [ ] `interaction` event detail shape is `{ action: string, value: unknown }`
- [ ] `state_update` event detail shape is `{ store: string, state: object }`
- [ ] CSS uses theme tokens with fallback values (no hardcoded colors)
- [ ] Renders correctly at 280px width (sidebar)
- [ ] Keyboard navigation works for all interactive elements
- [ ] Focus is visible on interactive elements
- [ ] User content is HTML-escaped via `_esc()` helper
- [ ] Custom element registration is guarded: `if (!customElements.get(...))`
- [ ] `agent-usage.md` documents all props, interactions, and rendering events
- [ ] `fallback` template produces readable markdown
- [ ] JS file is self-contained and within size budget
- [ ] test-harness.html loads and exercises the component
