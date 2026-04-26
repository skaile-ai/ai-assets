# Screen Spec Template

Every screen file follows this structure. The frontmatter and core sections are
mandatory; content varies per screen.

## Frontmatter

```yaml
---
implements:
  - experience/features/<NN_group>/<feature>.md
data_entities: [<Model>, ...]
layout: experience/screens/00_layout/shell.md
last_updated: YYYY-MM-DD
---
```

**Notes:**
- `implements[]` — paths to feature files this screen covers. At least one required.
- `data_entities[]` — entity names from model.json that appear on this screen.
- `layout` — always points to `00_layout/shell.md`.

## Required Sections

### Purpose (3-second test)
One sentence: what does the user immediately understand when this screen loads?

### Route
The URL path (e.g., `/login`, `/dashboard`, `/tasks/:id`).

### What the User Sees
A plain-language description of the initial view — what is visible on screen load.
Write as if describing it to a non-technical stakeholder.

### Wireframe

ASCII wireframe showing the spatial layout of this screen within the app shell.
Follows `skaileup-shared/contracts/wireframe_conventions.md`.

- **Required** at depth `medium` and `max`
- **Skipped** at depth `light` and `none`
- At depth `max`, annotate zones with `feat:<feature-name>` when the screen
  implements multiple features (derived from `implements[]` frontmatter)

```text
┌─────────────────────────────────────────────┐
│ [=] App Name              [search] [avatar] │
├────────────┬────────────────────────────────┤
│ Nav Item 1 │  Page Title                    │
│ Nav Item 2 │ ┌────────────────────────────┐ │
│ Nav Item 3 │ │  Main content area         │ │
│            │ │                            │ │
│            │ └────────────────────────────┘ │
└────────────┴────────────────────────────────┘
```

### Information Displayed
Group displayed information by data entity. Reference field names from model.json.

```
User: name, email, avatar
Task: title, status, assignee, due date
```

### Actions
Bullet list of what the user can do, with outcomes.

```
- Sign in: submit email and password → navigate to dashboard
- Forgot password: click link → navigate to password recovery
- Create account: click link → navigate to registration
```

### Situations
Named situations the screen can be in. Use user-perspective language.

- **Default** — what the user sees normally
- **Loading** — while waiting for data
- **Empty** — when there is nothing to show yet
- **Error** — when something goes wrong
- **Success** — after a successful action

### UI Elements
List the functional UI elements on this screen in plain language (top to bottom).
Do NOT use component library names. If the project has a defined tech stack, the
implementation skill will map these to actual components.

```
- App logo and name (top left)
- Email input field
- Password input field with show/hide toggle
- "Remember me" checkbox
- Sign in button (primary action)
- "Forgot password?" link
- "Create account" link
```

### Template Data *(optional — include when seed.json exists)*
Reference scenarios from `_concept/blueprint/datamodel/seed.json`:

- `populated` — what the screen shows with realistic data
- `empty` — what the screen shows when there is no data yet
- `edge_cases` — boundary conditions (very long names, many items, etc.)

## Enrichment from User Journeys

When `_concept/experience/journeys/stories.json` exists, use it to inform screen design:

| Journey element              | Maps to screen section  |
|------------------------------|-------------------------|
| Story outcomes               | Actions                 |
| EARS event-driven criteria   | Situations (conditional behavior) |
| Story personas               | Role-based screen visibility |
| Story map sequence           | Navigation flow between screens |
| Downstream candidate_screens | Screen list validation  |

## Enrichment from Architecture

When `_concept/blueprint/architecture.md` exists:

- Custom protocols (WebSocket, SSE) — note in Information Displayed for real-time data
- Additional apps or services — document communication flow
- Custom module API routes — reference in Actions for non-standard operations

## Writing Principles

1. **Write for a non-technical reader.** A product owner or designer should understand every spec.
2. **Describe what the user sees and does**, not how it is built.
3. **Name real things.** Say "their profile picture" not "Avatar". Say "email" not "input field".
4. **Explain purpose first.** Every screen starts with why it exists from the user's perspective.
5. **Describe situations in user terms.** Say "when the user has no tasks yet" not "empty state component".
6. **Never include brand tokens, CSS, or component names.** Design and implementation skills handle translation.
