---
name: "skailup-screens-technical"
description: "Technical variant of the screens skill with component inventories. Use when screen specs exist and require precise component-level breakdowns. NOT in pipeline — experimental variant for testing purposes."
metadata:
  version: "1.0.0"
  tags:
    - "screens"
    - "pages"
    - "ui"
    - "layout"
    - "components"
    - "states"
    - "routes"
    - "navigation"
  source: "MIGRATED"
  parameters:
    depth:
      type: enum
      values: [none, light, medium, max]
      default: medium
---

> **NOTE:** This skill is a variant of `screens` reserved for future testing and
> experimentation. It is **not registered in any flow** and will not be dispatched
> by the orchestrator. Do not use it in production pipelines.

# Screens (Technical Variant)

## Overview

The screens skill reads all approved upstream artifacts (features, brand, tech stack,
data model) and produces per-screen specifications with component inventories. These
are the blueprints that design skills use to generate mockups. Every screen references
brand tokens for colors and fonts, data model entities for content, and features for
behavior.

## When to Use

- All upstream artifacts are approved and `_concept/2_experience/3_screens/` is empty
- User asks about screens, pages, UI layout, navigation, routes
- User says "design the screens", "what pages do we need", "UI specs"

## When NOT to Use

- Features, brand, techstack, or datamodel are not yet approved (run those first)
- User wants visual mockups (use `mock` after screens)
- User wants to edit an existing screen spec (edit the file directly)

## Prerequisites

**Hard gates:**
- `_concept/2_experience/2_features/` must exist with at least one feature file
- `_concept/1_discovery/2_brand/tokens.json` must exist (unless brand was explicitly skipped)
- `_concept/3_blueprint/1_techstack/stack.md` must exist
- `_concept/3_blueprint/3_datamodel/model.json` must exist

If any gate fails, stop immediately and name the missing prerequisite skill.

## Shared Contracts

Before starting, read:
- `skaileup-shared/contracts/concept_structure.md` — valid _concept/ paths and naming rules
- `skaileup-shared/contracts/frontmatter.md` — required YAML fields
- `skaileup-shared/contracts/feedback_loop.md` — cross-reference protocol
- `skaileup-shared/contracts/iron_laws.md` — non-negotiable constraints
- `skaileup-shared/contracts/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

| Source | Priority |
|--------|----------|
| `_concept/1_discovery/1_overview/brief.md` | Required |
| `_concept/2_experience/2_features/**/*.md` | Required |
| `_concept/1_discovery/2_brand/identity.md` | Required |
| `_concept/1_discovery/2_brand/tokens.json` | Required |
| `_concept/3_blueprint/1_techstack/stack.md` | Required |
| `_concept/3_blueprint/3_datamodel/model.json` | Required |
| `_concept/2_experience/4_behaviors/*.allium` | Optional — surface definitions |
| `_concept/3_blueprint/2_architecture/architecture.md` | Optional — protocols and custom services |
| `_grounding/general/design_inspiration.md` | Optional |
| `_grounding/general/patterns.md` | Optional |

**Never load:** Source code, build artifacts, node_modules.

## Workflow

### Step 1: Read Prerequisites

Read all must-read files. If any hard-gate prerequisite is missing, stop and name
the prerequisite skill.

| Artifact | Path | Missing? Run |
|----------|------|-------------|
| Project brief | `_concept/1_discovery/1_overview/brief.md` | `overview` |
| Features | `_concept/2_experience/2_features/**/*.md` | `features` |
| Brand tokens | `_concept/1_discovery/2_brand/tokens.json` | `brand-visual` |
| Tech stack | `_concept/3_blueprint/1_techstack/stack.md` | `techstack` |
| Data model | `_concept/3_blueprint/3_datamodel/model.json` | `datamodel` |

**Optional: Behavioral specs.** Check if `_concept/2_experience/4_behaviors/*.allium` exists.
If present, read all `.allium` files. Use Allium surfaces to enrich screen specs:

- Surface `exposes` blocks → screen **Data Requirements** (which fields to show)
- Surface `provides` blocks → screen **User Actions** (which actions are available)
- Surface `when` guards → screen **States** (state-dependent UI)
- Surface `facing` clauses → confirm which role sees which screen

When allium surfaces exist, they are authoritative for what data is exposed and
what actions are available. The screen spec should match the surface contract.

### Step 2: Read Brand Tokens

Load `_concept/1_discovery/2_brand/tokens.json`. Use these values for:
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

```
_concept/2_experience/3_screens/00_layout/
_concept/2_experience/3_screens/01_user_auth/
```

**First, write the layout shell:**

`_concept/2_experience/3_screens/00_layout/shell.md` — navigation, sidebar, header,
footer, responsive breakpoints.

**Then, for each screen:**

`_concept/2_experience/3_screens/<NN_group>/<screen>.md`

```yaml
---
implements:
  - 2_experience/2_features/01_user_auth/login.md
data_entities: [user]
layout: 2_experience/3_screens/00_layout/shell.md
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
- Fill email and password → submit → redirect to dashboard
- Click "Forgot password?" → navigate to password reset
- Click "Register" → navigate to registration

## States
- **Default:** empty form
- **Loading:** submit button shows spinner
- **Error:** inline validation messages, toast for auth failure
- **Success:** redirect to dashboard

## Template Data
{
  "user": {
    "email": "maria.schmidt@example.com",
    "password": "********"
  }
}
```

### Step 5: Register Screens in Features (Feedback Loop)

For each screen written, update the feature files it implements:

```yaml
# In 2_experience/2_features/01_user_auth/login.md — add to screens[]
screens:
  - path: 2_experience/3_screens/01_user_auth/login.md
```

## Outputs

| File | Purpose |
|------|---------|
| `_concept/2_experience/3_screens/00_layout/shell.md` | App shell: navigation, sidebar, header, footer, breakpoints |
| `_concept/2_experience/3_screens/<NN_group>/<screen>.md` | Per-screen spec with component inventory, data, states |

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip this skill entirely |
| `light` | Core items only — list names and one-line descriptions, skip edge cases |
| `medium` | Standard coverage — full specs for core items, brief for secondary (default) |
| `max` | Exhaustive coverage — every feature/screen/component with full detail, edge cases, error states |

## Common Mistakes

| Rationalization | Reality |
|----------------|---------|
| "I know what screens look like" | You must consume brand tokens. Never invent colors, fonts, or spacing. Every visual reference must trace back to `tokens.json`. |
| "I'll just list the screens without component inventories" | Component inventories are the primary deliverable of this variant. Design skills cannot produce mockups without knowing what goes on each screen, top to bottom. |
| "The data model doesn't matter for screen specs" | Every screen must specify which entities and fields it displays. Template data comes from `model.json` entities and `seed.json` scenarios. |
| "I can skip the layout shell" | The shell (navigation, sidebar, header) is the most reused component. Every screen references it. Write it first. |
| "States are optional" | Every screen needs at minimum: default, loading, error, empty. Users encounter all of these. Missing states mean broken mockups and untested flows. |

EMIT  [screens-technical] started run_id=<uuid>
EMIT  [screens-technical] completed run_id=<uuid> screens_written=<N> features_updated=<N>
