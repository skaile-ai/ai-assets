---
name: "skailup-screens"
description: "Use after features are approved to write screen specifications. Reads features, optional brand/techstack/datamodel/journeys artifacts, and produces per-screen docs organized in numbered groups. Registers screens back into feature frontmatter via feedback loop."
metadata:
  version: "1.0.0"
  tags:
    - "screens"
    - "pages"
    - "ui"
    - "layout"
    - "navigation"
    - "ux"
    - "experience"
    - "user-perspective"
  source: "MERGED"
  parameters:
    depth:
      type: enum
      values: [none, light, medium, max]
      default: medium
  artifacts:
    requires:
      - id: features
        gate: hard
    produces:
      - id: screens
        description: "Screen design specifications"
    consumes:
      - id: brand-identity
        gate: soft
      - id: brand-tokens
        gate: soft
      - id: datamodel
        gate: soft
      - id: architecture
        gate: soft
  prerequisites:
    files:
      - path: "_concept/2_experience/2_features"
        gate: hard
        description: "At least one feature must exist — screens implement features"
        min_entries: 1
    reads:
      - path: "_concept/1_discovery/1_overview/brief.md"
        description: "App name, purpose, hero flow for navigation design"
      - path: "_concept/2_experience/1_journeys/stories.json"
        description: "User journeys to ensure screens cover all story flows"
      - path: "_concept/1_discovery/2_brand/tokens.json"
        description: "Design tokens for brand-aware component suggestions"
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        description: "Tech stack for routing patterns and component conventions"
      - path: "_concept/3_blueprint/2_architecture/architecture.md"
        description: "Architecture for API surface awareness"
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        description: "Data model for data display and form field mapping"
      - path: "_concept/3_blueprint/3_datamodel/seed.json"
        description: "Seed data for realistic screen template data"
      - path: "_concept/_grounding/general/design_inspiration.md"
        description: "Design references for layout inspiration"
    produces:
      - path: "_concept/2_experience/3_screens"
        description: "Per-screen specification files organized in numbered groups"
---

# Screens — Screen Specifications

## Overview

The **screens** skill is the Screen Specification agent. It reads all available
upstream artifacts and produces per-screen descriptions organized under
`_concept/2_experience/3_screens/`. It also registers each screen back into the
feature files it implements via the feedback loop.

Screen specs are written from the **user's perspective** in plain language — what
the user sees and does, not how it is built. No component library names, no CSS
tokens, no implementation details.

## When to Use

- Features exist and are approved in `_concept/2_experience/2_features/`
- User asks about screens, pages, UI layout, navigation, routes
- User says "design the screens", "what pages do we need", "UI specs"

## When NOT to Use

- Features do not exist yet — run **features** first
- User wants visual mockups — use **mock** skill after screens
- User wants to edit an existing screen spec directly

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md`,
`skaileup-shared/contracts/frontmatter.md`, and `skaileup-shared/contracts/feedback_loop.md`
before proceeding.

**Hard gate:** `_concept/2_experience/2_features/` must exist with at least one feature file.

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/1_discovery/1_overview/brief.md` | Yes |
| Must read | `_concept/2_experience/2_features/**/*.md` | Yes |
| Check if present | `_concept/2_experience/1_journeys/stories.json` | No |
| Check if present | `_concept/1_discovery/2_brand/tokens.json` | No |
| Check if present | `_concept/3_blueprint/1_techstack/stack.md` | No |
| Check if present | `_concept/3_blueprint/3_datamodel/model.json` | No |
| Check if present | `_concept/3_blueprint/2_architecture/architecture.md` | No |
| Check if present | `_concept/3_blueprint/3_datamodel/seed.json` | No |
| Check if present | `_concept/_grounding/general/design_inspiration.md` | No |
| Never load | source code, build artifacts, node_modules | — |

## Standalone Mode

**Gate check:** `_concept/2_experience/2_features/` must exist with at least one file.
**On completion:** Show screens summary and present next steps.

---

ROLE  Screen Specification agent — produces _concept/2_experience/3_screens/ from
      all upstream artifacts; updates feature frontmatter screens[] via feedback loop.

READS
  _concept/1_discovery/1_overview/brief.md                   — app name, audience, hero flow
  _concept/2_experience/2_features/**/*.md                    — requirements per feature
  ? _concept/2_experience/1_journeys/stories.json             — journey context for screen flow design
  ? _concept/1_discovery/2_brand/tokens.json                 — brand color/typography references
  ? _concept/3_blueprint/1_techstack/stack.md                — framework and component library
  ? _concept/3_blueprint/3_datamodel/model.json              — entities and fields
  ? _concept/3_blueprint/2_architecture/architecture.md     — custom protocols, additional apps
  ? _concept/3_blueprint/3_datamodel/seed.json               — scenario-based seed data
  ? _concept/_grounding/general/design_inspiration.md       — layout and interaction patterns

WRITES
  _concept/2_experience/3_screens/00_layout/shell.md          — app shell: nav, header, layout areas
  _concept/2_experience/3_screens/<NN_group>/<screen>.md       — per-screen spec (user perspective)
  _concept/2_experience/2_features/**/*.md                     — feedback loop: screens[] populated

REFERENCES
  skaileup-shared/contracts/concept_structure.md      — valid _concept/ paths and naming rules
  skaileup-shared/contracts/frontmatter.md            — required YAML fields
  skaileup-shared/contracts/feedback_loop.md          — cross-reference protocol (screens ↔ features)
  references/screen_spec_template.md             — screen spec structure with all sections

MUST  write 00_layout/shell.md before any individual screen specs
MUST  register every screen back into its parent feature's screens[] frontmatter (feedback loop)
MUST  write screen specs from the user's perspective in plain language
MUST  identify data entities from model.json (or infer if model.json is absent)
NEVER  include component library names, CSS tokens, or implementation details in screen specs
NEVER  write screens for features that have no feature spec
NEVER  invent brand colors or fonts — reference tokens.json if it exists; omit if absent

EMIT  [screens] started run_id=<uuid>

STEP 1: Read prerequisites
  - Read brief.md for app name, audience, hero flow
  - Read all _concept/2_experience/2_features/**/*.md
  - Stop if features directory is empty or missing:
    > "No feature specs found. Run `features` first."
  IF _concept/2_experience/1_journeys/stories.json exists
    - Read stories.json for journey context (screen flow design, navigation order)
  IF _concept/3_blueprint/3_datamodel/model.json exists
    - Read model.json for entity names and fields (enriches data requirements)
  ELSE
    - Infer data entities from feature requirements and journey acceptance criteria
  IF _concept/1_discovery/2_brand/tokens.json exists
    - Note brand color palette, typography, spacing scale for reference
  IF _concept/3_blueprint/2_architecture/architecture.md exists
    - Note custom protocols (WebSocket, SSE) for real-time screens
    - Note additional apps and their communication flows
  IF _concept/3_blueprint/3_datamodel/seed.json exists
    - Load scenarios for Template Data sections

STEP 2: Derive screen list from features
  - For each feature, identify required screens
  - For each screen: name, route/URL, purpose in one sentence, data entities involved
  - Group screens by feature group (matching NN_group numbering from features/)
  IF _concept/2_experience/1_journeys/stories.json exists
    - Use journey flows to inform screen navigation and sequence
    - Map story stages to screen priority: hero stories → primary nav, vital → secondary nav
    - Validate derived screens against downstream.candidate_screens hints in stories.json

  CHECKPOINT screen_list
    Present the screen list to the user:
    > "I've identified these screens: [list with one-line purpose each]. Add, remove, or rename?"

  UNTIL user approves the screen list

STEP 3: Write layout shell
  - $ mkdir -p _concept/2_experience/3_screens/00_layout

  OUTPUT _concept/2_experience/3_screens/00_layout/shell.md
    ---
    implements: []
    data_entities: []
    layout: ""
    last_updated: <YYYY-MM-DD>
    ---
    # Shell: App Layout
    ## Purpose
    ## Navigation
    ## Layout Areas
    ## Responsive Behaviour
    (Reference brand tokens if available — do not invent values)

STEP 4: Write screen specs
  - $ mkdir -p _concept/2_experience/3_screens/<NN_group>  (for each feature group)
  - For each screen, write a spec following references/screen_spec_template.md

  OUTPUT _concept/2_experience/3_screens/<NN_group>/<screen>.md
    ---
    implements:
      - 2_experience/2_features/<NN_group>/<feature>.md
    data_entities: [<Model>, ...]
    layout: 2_experience/3_screens/00_layout/shell.md
    last_updated: <YYYY-MM-DD>
    ---
    # Screen: <Name>
    ## Purpose
    ## Route
    ## What the User Sees
    ## Information Displayed
    ## Actions
    ## Situations
    ## UI Elements
    ## Template Data (if seed.json exists)

STEP 5: Register screens in features (feedback loop)
  - For each screen written, update the parent feature's frontmatter:
    screens:
      - path: 2_experience/3_screens/<NN_group>/<screen>.md
  - EMIT  [screens] feedback_loop updated 2_experience/2_features/<NN_group>/<feature>.md screens[]=<screen path>

EMIT  [screens] checkpoint phase=screens_written screens=<N> features_updated=<N>

STEP 6: Human approval
  CHECKPOINT screens_approved
    Show business summary:
    > "Here are the screens your users will see:
    > [List key screens in user terms: 'A login page', 'A dashboard showing...', 'A settings page where...']
    > Total: N screens across N feature groups.
    >
    > Do these cover all the things your users need to do? Add, remove, or adjust?"

  UNTIL user explicitly approves

STEP 7: Hand off
  > "Screen specs approved. Next steps:
  > - Run `mock` to generate HTML mockups from these specs
  > - Run `storybook` to create interactive component stories
  > - Run `concept-orchestrator` to continue the full pipeline"

EMIT  [screens] completed run_id=<uuid> screens_written=<N> features_updated=<N>

CHECKLIST
  - [ ] _concept/2_experience/3_screens/00_layout/shell.md exists
  - [ ] Every feature group has at least one screen spec
  - [ ] All screen specs have required frontmatter (implements, data_entities, layout, last_updated)
  - [ ] Screen specs are in plain language — no component library names or CSS
  - [ ] Data entities referenced from model.json (or inferred if model absent)
  - [ ] Feature files updated with screens[] in frontmatter (feedback loop complete)
  - [ ] User has explicitly approved the screen specs

---

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip this skill entirely |
| `light` | Core items only — list names and one-line descriptions, skip edge cases |
| `medium` | Standard coverage — full specs for core items, brief for secondary (default) |
| `max` | Exhaustive coverage — every feature/screen/component with full detail, edge cases, error states |

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Using component library names ("DataTable", "Avatar") | Describe what the user sees: "a list of tasks", "their profile picture". |
| Including CSS tokens or hex colors | Reference brand tokens by name (e.g., "primary color") or omit if tokens.json is absent. |
| Skipping the layout shell | Write shell.md first — every screen exists within it. |
| Skipping the Situations section | Users encounter empty states, errors, and loading. Missing situations = incomplete UX. |
| Pre-populating screens based on guesswork | Derive screens from feature requirements only. |
| Writing screens before the screen list is approved | Always confirm the screen list with the user before writing specs. |

## Research Mode

If research data exists, check before writing screens:

- `_concept/_grounding/general/design_inspiration.md` — layout and interaction patterns
- `_concept/_grounding/general/patterns.md` — proven UI patterns for this domain

Use these to inform navigation structure and screen organization.

## Integration

- **Called by:** `concept-orchestrator` or standalone (after features)
- **Requires:** `_concept/2_experience/2_features/` with at least one feature
- **Feedback loop:**
  - Screen registers itself into `features/**/*.md` `screens[]` (forward link)
  - Feature's `screens[]` lists which screens implement it (back-link, used by mock/storybook)
- **Feeds into:** `mock`, `storybook`, `e2e` — all need screen specs
