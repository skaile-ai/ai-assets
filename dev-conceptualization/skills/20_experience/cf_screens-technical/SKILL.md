---
name: screens-technical
description: "Variant of cf_concept_ui_screens for future testing purposes. Not active in the pipeline."
keywords: [screens, pages, ui, layout, components, states, routes, navigation]
user_inputs:
  dialog: []
  files:
    - "03_features/**/*.md"
    - "04_brand/tokens.json"
    - "05_techstack/stack.md"
    - "06_datamodel/model.json"
---

> **NOTE:** This skill is a variant of `cf_concept_ui_screens` reserved for future
> testing and experimentation. It is **not registered in `pipeline.json`** and will
> not be dispatched by the orchestrator. Do not use it in production pipelines.

# Screens (Technical Variant)

## Overview

The screens skill reads all approved upstream artifacts (features, brand, tech stack,
data model) and produces per-screen specifications with component inventories. These
are the blueprints that design skills use to generate mockups. Every screen references
brand tokens for colors and fonts, data model entities for content, and features for
behavior.

## When to Use
- All upstream artifacts are approved and `_concept/07_screens/` is empty
- User asks about screens, pages, UI layout, navigation, routes
- User says "design the screens", "what pages do we need", "UI specs"

## When NOT to Use
- Features, brand, techstack, or datamodel are not yet approved (run those first)
- User wants visual mockups (use cf_concept_mock after screens)
- User wants to edit an existing screen spec (edit the file directly)

## Prerequisites

**HARD-GATE (from pipeline.json):**
```json
"hard_gates": [
  { "type": "file_exists", "path": "03_features/" },
  { "type": "file_exists", "path": "04_brand/tokens.json" },
  { "type": "file_exists", "path": "05_techstack/stack.md" },
  { "type": "file_exists", "path": "06_datamodel/model.json" }
]
```
- `03_features/` must exist with at least one feature file
- `04_brand/tokens.json` must exist (unless brand was explicitly skipped)
- `05_techstack/stack.md` must exist
- `06_datamodel/model.json` must exist

If any gate fails, stop immediately and name the missing prerequisite skill.

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/03_features/`, `_concept/04_brand/tokens.json`, `_concept/05_techstack/stack.md`, `_concept/06_datamodel/model.json` must all exist
**If gates fail:** Run `cf_concept_functionality_features`, `cf_concept_brand_visual`, `cf_concept_techstack`, or `cf_concept_datamodel` as needed.
**On completion:** Present summary, then orchestrator suggests next steps.

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid _concept/ paths and naming rules
- `cf__shared/frontmatter.md` — required YAML fields
- `cf__shared/feedback_loop.md` — cross-reference protocol
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

**Must read:**
- `_concept/01_project/brief.md`
- `_concept/03_features/` (all feature files)
- `_concept/04_brand/identity.md` and `_concept/04_brand/tokens.json`
- `_concept/05_techstack/stack.md`
- `_concept/06_datamodel/model.json`
- `cf__shared/concept_structure.md`
- `cf__shared/frontmatter.md`
- `cf__shared/feedback_loop.md`

**Optional:**
- `_concept/03b_behavior/*.allium` — surface definitions for data exposure and actions _(fallback: empty_default)_
- `_concept/05b_architecture/architecture.md` — protocols and custom services affecting screen data flows _(fallback: skip_if_absent)_
- `_concept/_research/general/design_inspiration.md` — layout and interaction patterns from research
- `_concept/_research/general/patterns.md` — proven UI patterns for this domain

**Never load:**
- Source code
- Build artifacts or node_modules

## Workflow

### Step 1: Read Prerequisites

Read all must-read files. If any hard-gate prerequisite is missing, stop and name
the prerequisite skill.

| Artifact | Path | Missing? Run |
|----------|------|-------------|
| Project brief | `_concept/01_project/brief.md` | `cf_concept_overview` |
| Features | `_concept/03_features/**/*.md` | `cf_concept_functionality_features` |
| Brand tokens | `_concept/04_brand/tokens.json` | `cf_concept_brand_visual` |
| Tech stack | `_concept/05_techstack/stack.md` | `cf_concept_techstack` |
| Data model | `_concept/06_datamodel/model.json` | `cf_concept_datamodel` |

**Optional: Behavioral specs.** Check if `_concept/03b_behavior/*.allium` exists.
If present, read all `.allium` files. Use Allium surfaces to enrich screen specs:

- Surface `exposes` blocks -> screen **Data Requirements** (which fields to show)
- Surface `provides` blocks -> screen **User Actions** (which actions are available)
- Surface `when` guards -> screen **States** (state-dependent UI: disable buttons,
  show/hide elements based on entity state)
- Surface `facing` clauses -> confirm which role sees which screen
- Surface `related` links -> inform navigation between screens

When allium surfaces exist, they are authoritative for what data is exposed and
what actions are available. The screen spec should match the surface contract.

### Step 2: Read Brand Tokens

If `_concept/04_brand/tokens.json` exists, load it. Use these values for:
- Color references in component descriptions
- Font family names
- Border radius, spacing conventions
- Light/dark mode indication

**Never invent colors or fonts. Always reference the brand tokens.**

### Step 3: Derive Screens from Features

For each feature, identify the screens required. For each screen:

1. Name it clearly (matching the feature group: `01_user_auth/login.md`)
2. Determine its route/URL
3. The 3-second test: what does the user understand immediately?
4. What data from `model.json` entities is displayed?

Confirm the screen list with the user before writing:

> "I've identified these screens: [list]. Add, remove, or rename?"

### Step 4: Write Screen Specifications

```bash
mkdir -p _concept/07_screens/00_layout
mkdir -p _concept/07_screens/01_user_auth
```

**First, write the layout shell:**

`_concept/07_screens/00_layout/shell.md` — navigation, sidebar, header, footer,
responsive breakpoints.

**Then, for each screen:**

`_concept/07_screens/<NN_group>/<screen>.md`

```yaml
---
implements:
  - 03_features/01_user_auth/login.md
data_entities: [user]
layout: 07_screens/00_layout/shell.md
last_updated: YYYY-MM-DD
---

# Screen: Login

## Purpose (3-second test)
User immediately sees a login form and can sign in.

## Route
/login

## Component Inventory (top to bottom)
1. Logo — brand mark from tokens.json
2. Login form — email + password fields
3. Submit button — primary color from brand tokens
4. "Forgot password?" link
5. "Register" link

## Data Requirements
- User entity: email, password (for validation)
- Session entity: created on successful login

## User Actions
- Fill email and password -> submit -> redirect to dashboard
- Click "Forgot password?" -> navigate to password reset
- Click "Register" -> navigate to registration

## States
- **Default:** empty form
- **Loading:** submit button shows spinner
- **Error:** inline validation messages, toast for auth failure
- **Success:** redirect to dashboard

## Template Data
```json
{
  "user": {
    "email": "maria.schmidt@example.com",
    "password": "********"
  }
}
```
```

### Step 5: Register Screens in Features (Feedback Loop)

For each screen written, update the feature files it implements:

```yaml
# In 03_features/01_user_auth/login.md — add to screens[]
screens:
  - path: 07_screens/01_user_auth/login.md
```

Emit feedback loop events:

```
[cf_concept_ui_screens] feedback_loop updated 03_features/01_user_auth/login.md
  added screen: 07_screens/01_user_auth/login.md
```

## Outputs

| File | Purpose |
|------|---------|
| `_concept/07_screens/00_layout/shell.md` | App shell: navigation, sidebar, header, footer, breakpoints |
| `_concept/07_screens/<NN_group>/<screen>.md` | Per-screen spec with component inventory, data, states |

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

Show summary:

```
Screens written: N
  07_screens/00_layout/shell.md
  07_screens/01_user_auth/login.md
  07_screens/01_user_auth/registration.md
  ...

Features updated: N (screens[] populated)
```

## Common Mistakes

| Rationalization | Reality |
|----------------|---------|
| "I know what screens look like" | You must consume brand tokens. Never invent colors, fonts, or spacing. Every visual reference must trace back to `tokens.json`. |
| "I'll just list the screens without component inventories" | Component inventories are the primary deliverable. Design skills cannot produce mockups without knowing what goes on each screen, top to bottom. |
| "The data model doesn't matter for screen specs" | Every screen must specify which entities and fields it displays. Template data comes from `model.json` entities and `seed.json` scenarios. |
| "I can skip the layout shell" | The shell (navigation, sidebar, header) is the most reused component. Every screen references it. Write it first. |
| "States are optional" | Every screen needs at minimum: default, loading, error, empty. Users encounter all of these. Missing states mean broken mockups and untested flows. |

## Research Mode

Research UI patterns and design inspiration. Check
`_concept/_research/general/design_inspiration.md` and `_concept/_research/general/patterns.md`
for layout and interaction patterns discovered during the research phase. These
inform navigation structure, component choices, and responsive breakpoints.

## Integration

- **Called by:** orchestrator or standalone
- **Reads from:** 01_project/, 03_features/, 04_brand/, 05_techstack/, 06_datamodel/, 03b_behavior/ (optional), _research/general/design_inspiration.md (optional)
- **Writes to:** 07_screens/
- **Updates upstream:** 03_features/ (populates `screens[]` in feature frontmatter)
- **Consumed by:** cf_concept_mock, cf_test_e2e, ready

```
[cf_concept_ui_screens] completed
  run_id: <uuid>
  screens_written: N
  features_updated: N
```
