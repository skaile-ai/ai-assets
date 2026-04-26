---
name: "skailup-components"
description: "Reusable component inventory. Use when screen specs exist and you need to identify shared UI patterns (data tables, forms, cards, navigation) across screens. Maps components to the tech stack's component library. Outputs to _concept/experience/screens/components/."
metadata:
  version: "1.0.0"
  tags:
    - "components"
    - "patterns"
    - "reusable"
    - "inventory"
    - "ui"
    - "datatable"
    - "forms"
    - "cards"
    - "navigation"
  source: "MIGRATED"
  parameters:
    depth:
      type: enum
      values: [none, light, medium, max]
      default: medium
  artifacts:
    requires:
      - id: screens
        gate: hard
    produces:
      - id: components
        description: "Reusable UI component catalog"
    consumes:
      - id: brand-tokens
        gate: soft
  prerequisites:
    files:
      - path: "_concept/experience/screens"
        gate: hard
        description: "Screen specs required to identify shared UI patterns across screens"
        min_entries: 1
      - path: "_concept/discovery/brand/tokens.json"
        gate: hard
        description: "Design tokens required for component styling context"
      - path: "_concept/blueprint/techstack.md"
        gate: hard
        description: "Tech stack required to map abstract components to concrete library components"
    produces:
      - path: "_concept/experience/screens/components"
        description: "Component inventory files with props, variants, states, and accessibility notes"
---

# App Components — Reusable Component Inventory

## Overview

Analyzes all screen specifications to extract repeating UI patterns and
produces a reusable component inventory. Each component spec defines props,
variants, states, and accessibility notes. Component specs reference the
tech stack to map abstract components to concrete library components
(e.g., "data table" maps to PrimeVue DataTable or shadcn/ui DataTable).

## When to Use

- Screen specs exist and contain component inventories with repeated patterns
- You want a single source of truth for shared UI elements before implementation
- Multiple screens use similar patterns (data tables, forms, card grids, navigation)
- You need to map abstract components to the chosen component library

## When NOT to Use

- No screen specs yet — run `screens` first
- The app has only 1-2 simple screens with no shared patterns
- You only need visual mockups — use `mock`
- You are looking for brand/styling guidance — use `brand-visual`

## Prerequisites

| Artifact | Path | Missing? Run | Gate |
|----------|------|-------------|------|
| Screen specs | `_concept/experience/screens/**/*.md` | `screens` | HARD |
| Brand tokens | `_concept/discovery/brand/tokens.json` | `brand-visual` | HARD |
| Tech stack | `_concept/blueprint/techstack.md` | `techstack` | HARD |
| Layout shell | `_concept/experience/screens/00_layout/shell.md` | `screens` | recommended |
| Data model | `_concept/blueprint/datamodel/model.json` | `datamodel` | recommended |
| Features | `_concept/experience/features/**/*.md` | `features` | recommended |

If any HARD gate artifact is missing, stop immediately and name the prerequisite skill.

## Shared Contracts

Before starting, read:
- `skaileup-shared/contracts/concept_structure.md` — valid paths
- `skaileup-shared/contracts/frontmatter.md` — screen frontmatter fields
- `skaileup-shared/contracts/iron_laws.md` — non-negotiable constraints
- `skaileup-shared/contracts/agent_patterns.md` — communication style, standalone mode
- `skaileup-shared/contracts/wireframe_conventions.md` — ASCII wireframe symbol vocabulary

## Context Budget

| Source | Priority |
|--------|----------|
| `_concept/experience/screens/**/*.md` | Required (all) |
| `_concept/blueprint/techstack.md` | Required |
| `_concept/discovery/brand/tokens.json` | Required |
| `_concept/experience/screens/00_layout/shell.md` | Required |
| `_concept/blueprint/datamodel/model.json` | Skim for entity shapes |
| `_concept/experience/features/**/*.md` | Skim for data patterns |

## Workflow

### Step 1: Read All Screen Specs

Read every file in `_concept/experience/screens/` (including `00_layout/shell.md`).
For each screen, extract the **Component Inventory** section. Build a
frequency map of component types mentioned across screens.

### Step 2: Read Tech Stack

Read `_concept/blueprint/techstack.md`. Extract:
- UI library (PrimeVue, Radix UI, shadcn/ui, Tailwind-only, etc.)
- CSS framework
- Any component-specific notes

This determines the **library mapping** for each abstract component.

### Step 3: Identify Shared Patterns

Group components by pattern type. A component is "shared" if it appears
in 2+ screens or has complex enough behavior to warrant a spec.

Common pattern categories:

| Pattern | Signals | Example library mapping |
|---------|---------|------------------------|
| Data table | Lists with sorting, filtering, pagination | PrimeVue DataTable, shadcn DataTable |
| Form | Input fields, validation, submit | PrimeVue InputText + form layout, shadcn Form |
| Card | Repeated content blocks with image/title/body | Custom card component |
| Navigation | Sidebar, breadcrumbs, tabs | PrimeVue TabView, Menubar |
| Dialog/Modal | Confirmation, creation, editing overlays | PrimeVue Dialog, shadcn Dialog |
| Empty state | No-data placeholders | Custom (references brand behavioral copy) |
| Status badge | Enum-based colored indicators | PrimeVue Tag, shadcn Badge |
| File upload | Image/document upload with preview | PrimeVue FileUpload |
| Search | Global or contextual search with results | PrimeVue AutoComplete |
| Action bar | Bulk actions, toolbar buttons | PrimeVue Toolbar |

### Step 4: Confirm Component List

Present the identified components to the user:

> "I've identified these shared components across your screens:
>
> | Component | Used in | Library mapping |
> |-----------|---------|----------------|
> | DataTable | dashboard, task list, user admin | PrimeVue DataTable |
> | TaskForm | create task, edit task | PrimeVue form controls |
> | StatusBadge | task list, task detail, dashboard | PrimeVue Tag |
> | EmptyState | all list screens | Custom |
> | ConfirmDialog | delete actions across 4 screens | PrimeVue ConfirmDialog |
>
> Add, remove, or rename?"

### Step 5: Write Component Specs

**Output per component: `_concept/experience/screens/components/<component_name>.md`**

```yaml
---
pattern: data_table
library_component: PrimeVue DataTable
used_in:
  - experience/screens/02_dashboard/overview.md
  - experience/screens/03_tasks/task_list.md
  - experience/screens/04_admin/user_list.md
data_entities: [task, user]
last_updated: YYYY-MM-DD
---

# Component: Data Table

## Purpose
Sortable, filterable, paginated table for displaying entity collections.

## Props
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| columns | Column[] | required | Column definitions (field, header, sortable, filterable) |
| data | any[] | required | Row data array |
| paginator | boolean | true | Show pagination controls |
| rows | number | 20 | Rows per page |
| selectable | boolean | false | Enable row selection |
| loading | boolean | false | Show loading skeleton |
| empty_message | string | from copy_guidelines | Message when no data |

## Variants
- **Default:** full table with pagination, sorting, filtering
- **Compact:** reduced padding, smaller font, no row hover
- **Selectable:** checkbox column, bulk action bar appears on selection

## States
- **Loading:** skeleton rows matching column count
- **Empty:** empty state component with message from brand behavioral guidelines
- **Error:** error banner above table with retry action
- **Populated:** normal data display

## Accessibility
- Sortable columns announce sort direction to screen readers
- Pagination controls are keyboard-navigable
- Row selection uses standard checkbox semantics

## Library Mapping
PrimeVue DataTable with:
- `sortMode="multiple"`
- `paginator` + `rows` + `rowsPerPageOptions`
- `filterDisplay="menu"` for column filters
- Custom `#empty` template slot for empty state

## Anatomy

~~~text
┌─────────────────────────────────────────┐
│ [Bulk Actions]            [search] [v]  │  <- toolbar zone
├───────┬──────┬────────┬─────────────────┤
│ [ ] ^ │ Name │ Status │ Actions         │  <- header (sortable)
├───────┼──────┼────────┼─────────────────┤
│ [ ]   │ ...  │  * Ok  │ [Edit] [Delete] │  <- row
│ [ ]   │ ...  │  o New │ [Edit] [Delete] │
├───────┴──────┴────────┴─────────────────┤
│              < 1 2 3 >       20/page [v]│  <- pagination
└─────────────────────────────────────────┘
~~~
```

Component file names are lowercase, underscore-separated: `data_table.md`,
`status_badge.md`, `empty_state.md`, `confirm_dialog.md`.

### Step 6: Update Screen Specs (Cross-References)

For each screen that uses a shared component, add a `components:` array
to the screen frontmatter if not already present:

```yaml
# In experience/screens/02_dashboard/overview.md
components:
  - experience/screens/components/data_table.md
  - experience/screens/components/status_badge.md
```

## Outputs

| File | Purpose |
|------|---------|
| `_concept/experience/screens/components/<name>.md` | One spec per shared component |

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip this skill entirely |
| `light` | Core items only — list names and one-line descriptions, skip edge cases |
| `medium` | Standard coverage — full specs for core items, brief for secondary (default) |
| `max` | Exhaustive coverage — every feature/screen/component with full detail, edge cases, error states |

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Creating a component for every UI element | Only spec components that appear in 2+ screens or have complex behavior |
| Ignoring the tech stack | Always map to the chosen component library from `stack.md` |
| Copy-pasting library docs | Spec should describe THIS app's usage, not generic library docs |
| Missing empty/loading/error states | Every component needs at least: loading, empty, error, populated states |
| Not cross-referencing screens | Every component must list `used_in` screens; screens should reference components |
| Inventing components not in screens | Only spec components actually referenced in existing screen specs |

MUST  include a ## Anatomy wireframe for components with 3+ internal zones
MUST  read skaileup-shared/contracts/wireframe_conventions.md before generating anatomy wireframes

Anatomy wireframe depth behavior:
  - depth none/light: skip anatomy wireframes
  - depth medium: anatomy for complex components (3+ zones) only
  - depth max: anatomy for all components + variant wireframes showing different states

A zone is a visually distinct functional region within the component — e.g.,
toolbar, header row, body, footer, action bar. Interactive sub-elements within
a zone (individual buttons, inputs) do not count as separate zones.
Examples: data table has 4 zones (toolbar, header, body, pagination); badge has 1 zone.

EMIT  [components] started run_id=<uuid>
EMIT  [components] checkpoint phase=patterns_identified components=<N> screens_analyzed=<N>
EMIT  [components] completed run_id=<uuid> components_written=<N> screens_updated=<N>
