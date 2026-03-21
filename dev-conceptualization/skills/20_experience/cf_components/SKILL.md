---
name: components
description: "Reusable component inventory. Use when screen specs exist and you need to identify shared UI patterns (data tables, forms, cards, navigation) across screens. Maps components to the tech stack's component library. Outputs to _concept/07_screens/components/."
keywords: components, patterns, reusable, inventory, ui, datatable, forms, cards, navigation, primevue, radix
user_inputs:
  dialog: []
  files: []
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
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

- No screen specs yet — run `cf_concept_ui_screens` first
- The app has only 1-2 simple screens with no shared patterns
- You only need visual mockups (that is `cf_concept_mock`)
- You are looking for brand/styling guidance (that is `cf_concept_brand_visual`)

## Prerequisites

| Artifact | Path | Missing? Run | Gate |
|----------|------|-------------|------|
| Screen specs | `_concept/07_screens/**/*.md` | `cf_concept_ui_screens` | <HARD-GATE> |
| Brand tokens | `_concept/04_brand/tokens.json` | `cf_concept_brand_visual` | <HARD-GATE> |
| Tech stack | `_concept/05_techstack/stack.md` | `cf_concept_techstack` | <HARD-GATE> |
| Layout shell | `_concept/07_screens/00_layout/shell.md` | `cf_concept_ui_screens` | recommended |
| Data model | `_concept/06_datamodel/model.json` | `cf_concept_datamodel` | recommended |
| Features | `_concept/03_features/**/*.md` | `cf_concept_functionality_features` | recommended |

If any <HARD-GATE> artifact is missing, stop immediately and name the prerequisite skill.

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** Screen specs (`07_screens/**/*.md`), brand tokens (`04_brand/tokens.json`), tech stack (`05_techstack/stack.md`)
**If gates fail:** Run `cf_concept_ui_screens`, `cf_concept_brand_visual`, or `cf_concept_techstack` as needed
**On completion:** Present summary, then suggest next steps.

## Context Budget

| Source | Priority | Token estimate |
|--------|----------|---------------|
| `07_screens/**/*.md` | must read (all) | ~4000 |
| `05_techstack/stack.md` | must read | ~800 |
| `04_brand/tokens.json` | must read | ~500 |
| `07_screens/00_layout/shell.md` | must read | ~600 |
| `06_datamodel/model.json` | skim for entity shapes | ~1500 |
| `03_features/**/*.md` | skim for data patterns | ~1500 |

**Total budget:** ~9000 tokens input. Keep each component spec under 1500 tokens.

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid paths (`07_screens/components/`)
- `cf__shared/frontmatter.md` — screen frontmatter fields
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Workflow

### Step 1: Read All Screen Specs

Read every file in `_concept/07_screens/` (including `00_layout/shell.md`).
For each screen, extract the **Component Inventory** section. Build a
frequency map of component types mentioned across screens.

### Step 2: Read Tech Stack

Read `_concept/05_techstack/stack.md`. Extract:
- UI library (PrimeVue, Radix UI, shadcn/ui, Tailwind-only, etc.)
- CSS framework
- Any component-specific notes

This determines the **library mapping** for each abstract component.

### Step 3: Identify Shared Patterns

Group components by pattern type. A component is "shared" if it appears
in 2+ screens or has complex enough behavior to warrant a spec.

Common pattern categories:

| Pattern | Signals | Example library mapping |
|---------|---------|----------------------|
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

Present the identified components directly to the user:

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

```bash
mkdir -p _concept/07_screens/components
```

**Output per component: `_concept/07_screens/components/<component_name>.md`**

```yaml
---
pattern: data_table
library_component: PrimeVue DataTable
used_in:
  - 07_screens/02_dashboard/overview.md
  - 07_screens/03_tasks/task_list.md
  - 07_screens/04_admin/user_list.md
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
```

### Step 6: Update Screen Specs (Cross-References)

For each screen that uses a shared component, add a `components:` array
to the screen frontmatter if not already present:

```yaml
# In 07_screens/02_dashboard/overview.md
components:
  - 07_screens/components/data_table.md
  - 07_screens/components/status_badge.md
```

### Step 7: Emit Events

```
[cf_concept_ui_components] started
  run_id: <uuid>
  reads: 07_screens/, 05_techstack/stack.md, 04_brand/tokens.json

[cf_concept_ui_components] checkpoint phase=patterns_identified
  components: 8
  screens_analyzed: 12

[cf_concept_ui_components] completed
  run_id: <uuid>
  components_written: 8
  screens_updated: 12
```

## Outputs

| File | Purpose |
|------|---------|
| `_concept/07_screens/components/<name>.md` | One spec per shared component |

Component file names are lowercase, underscore-separated: `data_table.md`,
`status_badge.md`, `empty_state.md`, `confirm_dialog.md`.

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

> "Component inventory written to `_concept/07_screens/components/`. These specs will be consumed by:
> - `cf_concept_mock` — use library components in mockups
> - `cf_implement_feature` — developers reference these for component usage
> - `cf_quality_review` — validates component cross-references
>
> Next: generate mockups or proceed to implementation."

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Creating a component for every UI element | Over-decomposition | Only spec components that appear in 2+ screens or have complex behavior |
| Ignoring the tech stack | Writing abstract specs with no library mapping | Always map to the chosen component library from `stack.md` |
| Copy-pasting PrimeVue docs | Lazy mapping without context | Spec should describe THIS app's usage, not generic library docs |
| Missing empty/loading/error states | Only thinking about the happy path | Every component needs at least: loading, empty, error, populated states |
| Not cross-referencing screens | Components exist in isolation | Every component must list `used_in` screens; screens should reference components |
| Inventing components not in screens | Speculating about future needs | Only spec components that are actually referenced in existing screen specs |

## Integration

- **Upstream:** reads from `07_screens/`, `05_techstack/`, `04_brand/`
- **Downstream:** consumed by `cf_concept_mock`, `cf_implement_feature`
- **Called by:** orchestrator or standalone
- **Phase:** ui-planning (optional, runs after screens)
- **Pipeline position:** after `cf_concept_ui_screens`, before `cf_concept_mock`
- **Feedback loop:** updates screen frontmatter with `components:` references
