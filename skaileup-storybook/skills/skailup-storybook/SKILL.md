---
name: "skailup-storybook"
description: "Use after screens are approved to generate a 3-layer Storybook project: custom building-block components, full-page screen compositions, and clickable user journey flows. Framework-agnostic — resolves addon and story format from skaileup-standards/profiles/. Delegates to 4 sub-skills in sequence."
metadata:
  version: "1.0.0"
  tags:
    - "storybook"
    - "components"
    - "stories"
    - "visualization"
    - "ui"
    - "design-system"
    - "journeys"
    - "screens"
  source: "MERGED"
  parameters:
    depth:
      type: enum
      values: [none, light, medium, max]
      default: medium
  artifacts:
    requires:
      - id: screens
        gate: hard
      - id: features
        gate: hard
    produces:
      - id: prototype
        description: "Full Storybook prototype"
        variant: storybook
    consumes:
      - id: brand-tokens
        gate: hard
      - id: techstack
        gate: soft
      - id: journeys
        gate: soft
      - id: datamodel
        gate: soft
  prerequisites:
    files:
      - path: "_concept/experience/screens"
        gate: hard
        description: "Screen specs required — storybook generates components and pages from screen specs"
        min_entries: 1
      - path: "_concept/discovery/brand/tokens.json"
        gate: hard
        description: "Design tokens required for brand-accurate component styling"
      - path: "_concept/blueprint/techstack.md"
        gate: hard
        description: "Tech stack determines storybook addon, story format, and component library"
    reads:
      - path: "_concept/experience/journeys/stories.json"
        description: "User journeys for journey flow stories (4th storybook layer)"
      - path: "_concept/blueprint/datamodel/seed.json"
        description: "Seed data for realistic story args"
    produces:
      - path: "_concept/experience/4_storybook"
        description: "Complete storybook project: setup, components, pages, journey stories"
---

# Storybook — 3-Layer Component & Journey Visualization

## Overview

The **storybook** skill generates a complete Storybook project with three layers:
building-block custom components, screen compositions with real data states, and
clickable journey flows mapped to user journey stages.

It is **stack-agnostic**. The Storybook addon, story file format (`.vue`/`.tsx`/`.svelte`),
component library, and icon library are all resolved at runtime from the tech stack profile
(`skaileup-standards/profiles/<tech_stack_skill>/SKILL.md`).

It delegates to 4 sub-skills that run in sequence:

| Sub-skill | Role |
|---|---|
| `storybook-setup` | Scaffold project, install deps, apply brand tokens |
| `storybook-components` | Identify and build custom components with stories |
| `storybook-pages` | Build AppShell + full-page screen compositions |
| `storybook-journeys` | Build clickable multi-screen user journey flows |

## When to Use

- Screen specs are approved and the team wants a living component library
- User says "storybook", "component library", "design system", "story"
- After `experience/screens/` is complete and before or alongside implementation

## When NOT to Use

- No screen specs yet — run `screens` first
- No brand tokens — run `brand-visual` first
- No tech stack chosen — run `techstack` first
- User wants HTML mockups (zero-build) — use `mock` instead

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md` before proceeding.

**Hard gates:**
1. `_concept/experience/screens/` has at least one screen spec
2. `_concept/discovery/brand/tokens.json` exists
3. `_concept/blueprint/techstack.md` exists (required for stack resolution)

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/blueprint/techstack.md` | Yes |
| Must read | `skaileup-standards/profiles/<tech_stack_skill>/SKILL.md` | Yes |
| Must read | `_concept/discovery/brand/tokens.json` | Yes |
| Must read | `_concept/experience/screens/**/*.md` | Yes |
| Check if present | `_concept/experience/journeys/stories.json` | No (Layer 3) |
| Check if present | `_concept/blueprint/datamodel/seed.json` | No (story data) |

## Standalone Mode

**Gate check:** screens, tokens.json, stack.md must exist.
**On completion:** Present summary with story counts and run instructions.

---

ROLE  Storybook Orchestrator — resolves tech stack, then delegates to 4 sub-skills in sequence.

READS
  _concept/blueprint/techstack.md              — tech_stack_skill, package_manager
  skaileup-standards/profiles/<tech_stack_skill>/SKILL.md     — storybook_addon, story_format, component_library, icon_library
  _concept/experience/screens/**/*.md                — screen specs (UI elements, states, data requirements)
  _concept/experience/screens/00_layout/shell.md     — app shell structure, navigation
  _concept/discovery/brand/tokens.json               — color palette, fonts, radius, spacing, shadows, mode
  _concept/discovery/brand/identity.md               — design philosophy, atmosphere
  ? _concept/experience/journeys/stories.json        — user journeys (for Layer 3)
  ? _concept/experience/features/**/*.md             — feature context for story organization
  ? _concept/blueprint/datamodel/seed.json           — seed data for realistic story content

WRITES
  _concept/experience/4_storybook/                     — complete standalone Storybook project

REFERENCES
  skaileup-shared/contracts/concept_structure.md      — valid _concept/ paths
  skaileup-shared/contracts/frontmatter.md            — screen frontmatter fields
  skaileup-shared/contracts/seed_data.md              — scenario convention

MUST  resolve storybook_addon, story_format, and component_library from tech stack profile before delegating
MUST  run all 4 sub-skills in sequence — each depends on the previous
MUST  verify build passes after all sub-skills complete
MUST  report story counts per layer
NEVER skip any sub-skill
NEVER hardcode React, Vue, or any specific framework — derive from tech stack profile

EMIT  [storybook] started run_id=<uuid>

STEP 1: Resolve tech stack
  - Read stack.md → extract tech_stack_skill, package_manager
  - Read skaileup-standards/profiles/<tech_stack_skill>/SKILL.md → extract:
    | Field | Purpose |
    |---|---|
    | storybook_addon | e.g. @storybook/nuxt, @storybook/react-vite |
    | story_format | e.g. Vue SFC, CSF3 |
    | story_extension | e.g. .vue, .tsx, .svelte |
    | component_library | e.g. @nuxt/ui, shadcn/ui, @postxl/ui-components, primevue |
    | component_import | e.g. ~/components/, @/components/ |
    | icon_library | e.g. lucide-vue-next, lucide-react, heroicons |
  IF any required field is missing from the profile
    - Ask user to confirm values before continuing
  EMIT  [storybook] checkpoint phase=stack_resolved addon=<addon> format=<format>

STEP 2: Run storybook-setup
  - Delegate to `storybook-setup` sub-skill
  - Pass: addon, story_format, story_extension, component_library, package_manager, tokens.json, brief.md, shell.md
  - Verify `_concept/experience/4_storybook/` exists with passing build before continuing
  EMIT  [storybook] checkpoint phase=setup_complete

STEP 3: Run storybook-components
  - Delegate to `storybook-components` sub-skill
  - Pass: screen specs, component_library, story_extension, icon_library, tokens.json
  - Verify `src/components/` has barrel export before continuing
  EMIT  [storybook] checkpoint phase=components_complete components=<N>

STEP 4: Run storybook-pages
  - Delegate to `storybook-pages` sub-skill
  - Pass: screen specs, shell.md, component_library, story_extension, package_manager
  - Verify `src/pages/manifest.json` exists before continuing
  EMIT  [storybook] checkpoint phase=pages_complete pages=<N>

STEP 5: Run storybook-journeys
  IF _concept/experience/journeys/stories.json exists
    - Delegate to `storybook-journeys` sub-skill
    - Pass: stories.json, manifest.json, story_extension, package_manager
    EMIT  [storybook] checkpoint phase=journeys_complete journeys=<N>
  ELSE
    - Note in README.md: "Layer 3 (Journeys) skipped — stories.json not found. Run `journeys` then `storybook-journeys` to add."
    EMIT  [storybook] checkpoint phase=journeys_skipped reason=stories.json_absent

STEP 6: Final verification
  $ cd _concept/experience/4_storybook && <package_manager> run build
  IF build fails
    - Fix errors, retry
  - Count story files per layer and report
  EMIT  [storybook] completed run_id=<uuid> components=<N> pages=<N> journeys=<N>

CHECKPOINT storybook_review
  > "Your app concept is now fully visualized in Storybook with 3 layers:
  >
  > **1. Building Blocks:** <N> custom components with brand tokens applied
  > **2. Screens:** <N> page compositions with all states (Populated, Empty, Error, Loading)
  > **3. User Journeys:** <N> clickable flows (hero + vital + hygiene)
  >
  > Run `cd _concept/experience/4_storybook && <package_manager> run storybook dev` to explore.
  >
  > Review and tell me what to adjust."

CHECKLIST
  - [ ] Tech stack resolved (addon, format, extension, component_library, icon_library)
  - [ ] Setup complete — project scaffolds, brand tokens applied, build passes
  - [ ] Components complete — custom components built with stories
  - [ ] Pages complete — all screen specs as page stories
  - [ ] Journeys complete (or skipped if stories.json absent)
  - [ ] Final build passes
  - [ ] Story counts reported per layer

---

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip this skill entirely |
| `light` | Minimal setup — hero flow stories only |
| `medium` | Standard setup — hero + vital flow stories (default) |
| `max` | Full setup — all flows, edge cases, responsive variants, dark mode |

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Hardcoding React or Vue | Always derive story format and extension from tech stack profile |
| Skipping stack profile read | storybook_addon and component_library are required for every downstream sub-skill |
| Skipping Layer 3 without noting it | Note in README.md when journeys are skipped and why |
| Running sub-skills in parallel | Each sub-skill depends on the previous — always run in sequence |
| Using `pnpm` when stack.md says `bun` | Always read `package_manager` from stack.md |

## Integration

- **Called by:** `concept-orchestrator` or standalone
- **Requires:** `screens/`, `discovery/brand/tokens.json`, `blueprint/techstack.md`
- **Feeds into:** design review, component documentation, implementation reference
