# Wireframe Conventions

Shared reference for all skills that produce ASCII wireframes. Defines a consistent
symbol vocabulary so wireframes from different skills look like they belong to the
same system.

## Box Drawing Characters

```
Borders:    ┌ ─ ┐ │ └ ┘ ├ ┤ ┬ ┴ ┼
Corners:    ┌───┐  (top-left, top-right)
            └───┘  (bottom-left, bottom-right)
Junctions:  ├───┤  (left/right T)
            ┬   ┴  (top/bottom T)
            ┼      (cross)
```

## Standard Icon Vocabulary

Icons represent functional elements, not decorative ones. Use sparingly.

| Icon | Meaning | Use in |
|---|---|---|
| `[Button Label]` | Clickable button | Actions, toolbar |
| `[+ Label]` | Create/add action | Toolbar, empty states |
| `(o) / (*)` | Radio button (off/on) | Forms |
| `[ ] / [x]` | Checkbox (off/on) | Forms, selection |
| `[___________]` | Text input field | Forms |
| `[v]` or `[dropdown v]` | Dropdown/select | Forms, filters |
| `[...more]` | Overflow/truncated content | Lists, tables |
| `< 1 2 3 >` | Pagination | Tables, lists |
| `--- / ===` | Divider (light/heavy) | Section separation |
| `>` | Breadcrumb separator | Navigation |
| `[X]` | Close button (uppercase X) | Modals, drawers (top-right corner) |
| `[=]` | Menu toggle / hamburger | Mobile header, collapsed sidebar |
| `:::` | Drag handle | Reorderable lists |
| `(i)` | Info tooltip | Help text |

**Symbol disambiguation:** `[x]` (lowercase) is always a checked checkbox.
`[X]` (uppercase) is always a close/dismiss button, placed in the top-right
corner of its container (modal, drawer, toast).

## Layout Zones

Every screen wireframe should show clearly delimited zones. The standard zones are:

```
┌─────────────────────────────────────────────┐
│                  HEADER                      │
├────────────┬────────────────────────────────┤
│            │         TOOLBAR                 │
│  SIDEBAR   │────────────────────────────────│
│            │                                 │
│            │         CONTENT                 │
│            │                                 │
│            │────────────────────────────────│
│            │         FOOTER / PAGINATION     │
└────────────┴────────────────────────────────┘
```

Optional overlay zones (drawn as floating boxes):

```
                    ┌──────────────┐
                    │    MODAL     │
                    │              │
                    └──────────────┘

                              ┌──────────┐
                              │  DRAWER  │
                              │ (right)  │
                              │          │
                              └──────────┘
```

## Responsive Notation

Show breakpoint variants when layout changes significantly:

```
Desktop (lg+):                    Mobile (< lg):
┌──────┬───────────────┐         ┌──────────────────┐
│ Side │   Content     │         │ [=] Header       │
│ bar  │               │         ├──────────────────┤
└──────┴───────────────┘         │    Content       │
                                 └──────────────────┘
```

## State Annotation

When a wireframe shows a specific state, annotate it below:

```
┌─────────────────────────────┐
│  No projects yet.           │
│                             │
│  [+ Create your first one]  │
│                             │
└─────────────────────────────┘
State: EMPTY
```

## Sizing and Proportion

- Standard wireframe width: 50-70 characters
- Use relative column proportions (sidebar ~20%, content ~80%)
- Content within zones uses indentation to show nesting
- Tables show 2-3 example rows, then `...` for repetition

## Rules

1. **No brand colors, fonts, or styling** — wireframes are structural only
2. **No component library names** — say "data table" not "PrimeVue DataTable"
3. **Label zones with functional names** — "Sidebar", "Content", not "div.left"
4. **Show real content hints** — use placeholder text that suggests the data type
   (e.g., "Project Alpha" not "Lorem ipsum")
5. **One wireframe per state** — if empty and populated layouts differ, show both
6. **Always show the shell context** — even if documenting a sub-region, show
   enough of the surrounding shell to orient the reader

## Component Anatomy Zones

A zone is a visually distinct functional region within a component — e.g.,
toolbar, header row, body, footer, action bar. Interactive sub-elements within
a zone (individual buttons, inputs) do not count as separate zones.

Examples:
- Data table: 4 zones (toolbar, header, body, pagination)
- Form with sections: 3+ zones (header, field groups, action bar)
- Simple badge: 1 zone (not eligible for anatomy wireframe)

Components with 3+ zones require a ## Anatomy wireframe.
