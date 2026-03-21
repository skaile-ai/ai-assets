---
name: screens
description: "Use when features, brand, techstack, and datamodel are all approved but _concept/07_screens/ is empty. Also when user says 'design the screens', 'UI specs', 'what pages do we need'."
keywords: [screens, pages, ui, layout, user-experience, navigation]
user_inputs:
  dialog: []
  files:
  - "03_features/**/*.md"
  - "04_brand/tokens.json"
  - "05_techstack/stack.md"
  - "06_datamodel/model.json"
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

# Screens

## Overview

The screens skill reads all approved upstream artifacts (features, brand, tech stack,
data model) and produces per-screen descriptions from the user's perspective. Each
screen spec answers: what is this screen for, what can the user see and do here,
and which business entities are involved. These descriptions are written in plain
language — no component names, no CSS tokens, no technical implementation details.

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

- Surface `exposes` blocks -> screen **Information Displayed** (what the user sees)
- Surface `provides` blocks -> screen **Actions** (what the user can do)
- Surface `when` guards -> screen **Situations** (how the screen changes based on context)
- Surface `facing` clauses -> confirm which role sees which screen
- Surface `related` links -> inform navigation between screens

When allium surfaces exist, they are authoritative for what data is exposed and
what actions are available. The screen spec should match the surface contract.

### Step 2: Derive Screens from Features

For each feature, identify the screens required. For each screen:

1. Name it clearly (matching the feature group: `01_user_auth/login.md`)
2. The 3-second test: what does the user understand immediately?
3. What is the screen's purpose in the user's journey?
4. Which entities from `model.json` are relevant to the user here?

Confirm the screen list with the user before writing:

> "I've identified these screens: [list]. Add, remove, or rename?"

### Step 3: Write Screen Specifications

```bash
mkdir -p _concept/07_screens/00_layout
mkdir -p _concept/07_screens/01_user_auth
```

**First, write the layout shell:**

`_concept/07_screens/00_layout/shell.md` — the overall app structure the user
experiences: where navigation lives, how the app is organized, how it adapts
to different devices.

**Then, for each screen:**

`_concept/07_screens/<NN_group>/<screen>.md`

Write each screen spec from the user's point of view using this format:

```yaml
---
implements:
  - 03_features/01_user_auth/login.md
data_entities: [user]
layout: 07_screens/00_layout/shell.md
last_updated: YYYY-MM-DD
---

# Screen: Login

## Purpose
Let users sign in to access their personal and team workspace.

## What the User Sees
- The app logo and name
- A sign-in form asking for email and password
- A "remember me" option
- Links to reset a forgotten password or create a new account

## Information Displayed
- **User entity:** email (entered by user)

## Actions
- **Sign in:** enter email and password, then submit to access the dashboard
- **Forgot password:** navigate to password recovery
- **Create account:** navigate to registration

## Situations
- **First visit:** empty form, ready to fill in
- **Signing in:** form is temporarily disabled while checking credentials
- **Wrong credentials:** a message explains what went wrong, form is ready to retry
- **Success:** user is taken to the dashboard

## Entities Involved
- **User** — the person signing in (email, password for authentication)
- **Session** — created upon successful sign-in
```

### Key Principles for Screen Specs

- **Write for a non-technical reader.** A product owner, designer, or stakeholder
  should understand every screen spec without technical knowledge.
- **Describe what the user sees and does**, not how it's built. Say "a list of
  their tasks" not "a DataTable component with pagination".
- **Name real things.** Say "email" not "input field". Say "their profile picture"
  not "Avatar component".
- **Explain the purpose first.** Every screen starts with why it exists from the
  user's perspective.
- **List information by entity.** Under "Information Displayed", group by data model
  entity so the connection to the data model is clear.
- **Describe situations, not states.** Say "when the user has no tasks yet" not
  "empty state". Say "while loading" not "loading state".
- **No brand tokens, no CSS, no component names.** The design skills handle all
  visual and technical translation downstream.

### Step 4: Register Screens in Features (Feedback Loop)

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
| `_concept/07_screens/00_layout/shell.md` | App structure: navigation, areas, device adaptation |
| `_concept/07_screens/<NN_group>/<screen>.md` | Per-screen spec: purpose, information, actions, situations |

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
| "I'll describe the components and their technical props" | Screen specs describe what the user sees and does, not how it's built. Save technical details for design skills. |
| "I'll just list the screens without describing information and actions" | Information displayed and user actions are the primary deliverable. Design skills need to know what goes on each screen. |
| "The data model doesn't matter for screen specs" | Every screen must specify which entities and fields the user interacts with. This traces back to `model.json`. |
| "I can skip the layout shell" | The shell describes how the user navigates the app. Every screen exists within it. Write it first. |
| "I can skip describing different situations" | Users encounter empty states, loading, errors, and success. Missing situations mean incomplete understanding of the user experience. |
| "I should include CSS values, component library names, or brand hex codes" | Never. Describe things the user sees in plain language. Design skills translate to visual and technical specs. |

## Research Mode

Research UI patterns and design inspiration. Check
`_concept/_research/general/design_inspiration.md` and `_concept/_research/general/patterns.md`
for layout and interaction patterns discovered during the research phase. These
inform navigation structure and screen organization.

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
