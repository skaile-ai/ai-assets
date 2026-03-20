---
name: storybook
description: "Use after screens are approved to generate a 3-layer Storybook project. Reads the tech stack to use the correct Storybook addon. Generates building-block components, screen compositions, and clickable journey flow stories."
keywords: [storybook, components, stories, visualization, ui, design-system, journeys]
user_inputs:
  dialog: []
  files: []
---

# Storybook — 3-Layer Component & Journey Visualization

## Overview

The **storybook** skill generates a complete Storybook project with three layers of
stories: atomic building-block components, screen compositions with real data states,
and clickable journey flows mapped to user journey stages.

It is **framework-agnostic**. The correct Storybook addon, story format, and component
import paths are determined at runtime by reading `05_techstack/stack.md` and the
referenced `tech-stack/<tech_stack_skill>/SKILL.md`.

**Phase:** experience / storybook
**Writes to:** `_concept/08_storybook/`

## 3-Layer Architecture

### Layer 1: Building-Block Components

Atoms and molecules extracted from the component inventory.

- Source: `07_screens/components/*.md` (preferred) or component lists embedded in
  per-screen specs
- One story file per component: `src/components/<ComponentName>/<ComponentName>.stories.ts`
- Companion component file: `<ComponentName>.vue` (or `.tsx` for React stacks)
- Stories cover: Default, variants (size, color, state), disabled, loading
- Props come from the component spec; data from `seed.json` where applicable

### Layer 2: Screen Compositions

Each screen rendered as a Storybook story using the real component library and
realistic data states from `seed.json`.

- Source: `07_screens/**/*.md` (one story file per screen)
- Location: `src/screens/<ScreenName>/<ScreenName>.stories.ts`
- Required story variants per screen:
  - `Populated` — fully loaded state (from `seed.json` populated scenario)
  - `Empty` — zero-data onboarding state (from `seed.json` empty scenario)
  - `Error` — error/failure state (from `seed.json` edge_cases scenario)
  - `Loading` — skeleton/spinner state

### Layer 3: Journey Flows

Clickable story sequences that map to the hero journey stages in `02_journeys/stories.json`.

- Source: `02_journeys/stories.json` (optional — skip Layer 3 gracefully if absent)
- Location: `src/journeys/<JourneyName>.stories.ts`
- Each journey stage becomes an ordered story in a sequence
- Stories link to each other using Storybook's `play` function or navigation args
- The sequence mirrors the user's path through the app

## When to Use

- Screen specs are approved and the team wants a living component library
- The user says "storybook", "component library", "design system", "story"
- After `07_screens/` is complete and before or alongside implementation
- The orchestrator dispatches this after screens are approved

## When NOT to Use

- No screen specs exist yet — run **screens** first
- No brand tokens exist — run **brand-visual** first
- No tech stack chosen — run **techstack** first
- The user wants HTML mockups (CDN, zero-build) — use **mock** instead

## Prerequisites

### HARD-GATE

All three must be satisfied:

1. `_concept/07_screens/` has at least one screen file
2. `_concept/04_brand/tokens.json` exists
3. `_concept/05_techstack/stack.md` exists (required for addon resolution)

If any are missing:

> "Cannot generate Storybook. Missing: [list]. Run [skill] first."

### Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid paths
- `cf__shared/frontmatter.md` — screen frontmatter fields
- `cf__shared/seed_data.md` — scenario convention
- `cf__shared/iron_laws.md` — non-negotiable constraints
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/05_techstack/stack.md` | Yes |
| **Must read** | `tech-stack/<tech_stack_skill>/SKILL.md` | Yes (resolved from stack.md) |
| **Must read** | `_concept/04_brand/tokens.json` | Yes |
| **Must read** | `_concept/07_screens/**/*.md` | Yes |
| **Optional** | `_concept/07_screens/components/*.md` | No (for Layer 1 component list) |
| **Optional** | `_concept/02_journeys/stories.json` | No (for Layer 3 journey flows) |
| **Optional** | `_concept/06_datamodel/seed.json` | No (for realistic story data) |
| **Never load** | `_concept/01_project/`, `_concept/03_features/`, source code | — |

## Tech Stack Resolution

Read `_concept/05_techstack/stack.md` and extract `tech_stack_skill:`.

Then read `tech-stack/<tech_stack_skill>/SKILL.md` and extract:

| Field | Example | Purpose |
|-------|---------|---------|
| `storybook_addon:` | `@storybook/nuxt` | npm package for Storybook integration |
| `story_format:` | `Vue SFC` or `CSF3` | How story component files are written |
| `component_import:` | `~/components/` | Import path prefix for components |

If `tech-stack/<tech_stack_skill>/SKILL.md` does not contain `storybook_addon:`,
ask the user which Storybook addon to use before proceeding.

## Output Structure

```
_concept/08_storybook/
├── .storybook/
│   ├── main.ts            ← addon config from tech-stack (storybook_addon value)
│   ├── preview.ts         ← brand token CSS vars + global decorators
│   └── theme.ts           ← brand colors/fonts as Storybook theme object
├── src/
│   ├── components/        ← Layer 1: building-block component stories
│   │   └── <ComponentName>/
│   │       ├── <ComponentName>.stories.ts
│   │       └── <ComponentName>.vue  (or .tsx for React stacks)
│   ├── screens/           ← Layer 2: screen composition stories
│   │   └── <ScreenName>/
│   │       └── <ScreenName>.stories.ts
│   └── journeys/          ← Layer 3: journey flow stories
│       └── <JourneyName>.stories.ts
├── package.json           ← storybook + addon deps (versions pinned)
└── README.md              ← how to run, what the three layers mean
```

## Workflow

### Step 1: Read Stack and Resolve Addon

1. Read `_concept/05_techstack/stack.md`, extract `tech_stack_skill:`
2. Read `tech-stack/<tech_stack_skill>/SKILL.md`
3. Extract `storybook_addon:`, `story_format:`, `component_import:`
4. If any are missing, ask user to confirm before continuing

### Step 2: Set Up `.storybook/` Config

Write `main.ts` using the resolved addon:

```ts
// main.ts (example for nuxt-primevue)
import type { StorybookConfig } from '<storybook_addon>/types'

const config: StorybookConfig = {
  stories: ['../src/**/*.stories.@(ts|tsx|js|jsx)'],
  addons: ['<storybook_addon>', '@storybook/addon-essentials'],
  framework: { name: '<storybook_addon>', options: {} },
}
export default config
```

Write `theme.ts` using brand colors and fonts from `tokens.json`.
Write `preview.ts` applying all CSS custom properties from `tokens.json` as
global decorators so every story inherits brand styling.

### Step 3: Layer 1 — Building-Block Components

1. Collect component list from `07_screens/components/*.md` (or from per-screen
   component inventory sections if no dedicated components folder)
2. For each component:
   - Write `src/components/<ComponentName>/<ComponentName>.vue` (or `.tsx`)
     using `component_import:` paths from tech-stack skill
   - Write `src/components/<ComponentName>/<ComponentName>.stories.ts` with:
     - `Default` story
     - Variant stories (states, sizes, colors from component spec)
     - Props documented with JSDoc / argTypes

### Step 4: Layer 2 — Screen Compositions

1. For each screen spec in `07_screens/`:
   - Derive screen name from folder/file name
   - Write `src/screens/<ScreenName>/<ScreenName>.stories.ts` with stories:
     - `Populated` — uses `seed.json` populated scenario (if seed.json exists)
     - `Empty` — uses `seed.json` empty scenario
     - `Error` — uses `seed.json` edge_cases scenario
     - `Loading` — skeleton state
   - Import components from Layer 1 where they are referenced in the screen spec

### Step 5: Layer 3 — Journey Flows

1. Check if `_concept/02_journeys/stories.json` exists
   - If absent: skip Layer 3 gracefully, note it in README.md
2. For each journey/stage group in `stories.json`:
   - Write `src/journeys/<JourneyName>.stories.ts`
   - Each stage becomes an ordered story
   - Stories reference the corresponding screen story via `parameters.storyLink`
   - The sequence is annotated in story titles so the order is clear in Storybook UI

### Step 6: Write `package.json`

Use the resolved `storybook_addon` and tech-stack package manager to write
a `package.json` with correct dev deps:

```json
{
  "name": "storybook",
  "private": true,
  "scripts": {
    "storybook": "storybook dev -p 6006",
    "build-storybook": "storybook build"
  },
  "devDependencies": {
    "storybook": "^8.0.0",
    "<storybook_addon>": "^8.0.0",
    "@storybook/addon-essentials": "^8.0.0"
  }
}
```

Package manager comes from `stack.md` (`package_manager:` field).

### Emit Events

```
[storybook] started
  run_id: <uuid>
  reads: 05_techstack/stack.md, 04_brand/tokens.json, 07_screens/
  tech_stack_skill: <resolved value>
  storybook_addon: <resolved value>

[storybook] checkpoint layer=1
  components_found: N
  stories_written: N

[storybook] checkpoint layer=2
  screens_found: N
  stories_written: N

[storybook] checkpoint layer=3
  journeys_found: N (or "skipped: 02_journeys/stories.json absent")
  stories_written: N

[storybook] completed
  run_id: <uuid>
  output: _concept/08_storybook/
  total_stories: N
  layers: [1, 2, 3]  (or [1, 2] if layer 3 skipped)
```

## Outputs

| File | Description |
|------|-------------|
| `_concept/08_storybook/.storybook/main.ts` | Storybook config with resolved addon |
| `_concept/08_storybook/.storybook/preview.ts` | Brand CSS vars + global decorators |
| `_concept/08_storybook/.storybook/theme.ts` | Brand colors/fonts as Storybook theme |
| `_concept/08_storybook/src/components/**` | Layer 1: building-block stories + components |
| `_concept/08_storybook/src/screens/**` | Layer 2: screen composition stories |
| `_concept/08_storybook/src/journeys/**` | Layer 3: journey flow stories (if journeys exist) |
| `_concept/08_storybook/package.json` | Storybook + addon deps |
| `_concept/08_storybook/README.md` | How to run, what the three layers mean |

## Completion Summary

Present to user: files produced, addon used, number of stories per layer, how to
run the Storybook (`cd _concept/08_storybook && <package_manager> install && <package_manager> run storybook`),
suggested next steps (implementation skills for the actual project).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|--------------------|
| Hardcoding `@storybook/react` for a Vue stack | Agent skips stack.md read | Always resolve `storybook_addon:` from `tech-stack/<tech_stack_skill>/SKILL.md` first |
| Writing `.tsx` files for a Vue project | Agent defaults to React conventions | Check `story_format:` from tech-stack skill before writing any component file |
| Skipping brand tokens in preview.ts | Agent forgets to wire tokens | CSS custom properties from `tokens.json` must be applied in `preview.ts` global decorator |
| Creating Layer 3 when stories.json is absent | Agent hallucinates journey data | Check for `02_journeys/stories.json` first; skip gracefully if absent |
| Using hardcoded component paths | Agent guesses import paths | Use `component_import:` from tech-stack skill for all import statements |
| Writing stories without seed data | Agent uses static mock data | Import from `seed.json` scenarios when the file exists |
| Generating all layers in one pass without checkpoints | Agent loses track of progress | Emit a `checkpoint` event after each layer is complete |

## Integration

- **Called by:** orchestrator or standalone
- **Reads from:** `05_techstack/stack.md`, `tech-stack/<id>/SKILL.md`, `04_brand/tokens.json`, `07_screens/`, optionally `02_journeys/stories.json`, `06_datamodel/seed.json`
- **Writes to:** `_concept/08_storybook/`
- **Feeds into:** design review, component documentation, implementation reference
- **Feedback loops:** None inbound. Outbound: Storybook is a living reference for `implement/feature` skill.

## Standalone Mode

This skill can be invoked directly without the orchestrator.
**Gate check:** `07_screens/` exists, `04_brand/tokens.json` exists, `05_techstack/stack.md` exists
**If gates fail:** Run `screens`, `brand-visual`, or `techstack` as needed.
**On completion:** Present summary, then suggest running implementation skills.
