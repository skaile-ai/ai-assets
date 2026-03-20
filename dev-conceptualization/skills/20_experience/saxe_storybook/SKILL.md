---
name: concept-2-experience-4-storybook
description: "Storybook visualization with 3 layers: building-block components, screen compositions, and clickable user journey flows. Reads screen specs, user journeys, brand tokens, and optional seed data to generate a standalone Storybook project. Run after concept-2-experience-3-screens to visualize the entire UX."
---

ROLE  Storybook Orchestrator — delegates to 4 sub-skills that each handle one phase of Storybook generation.

READS
  _concept/2_experience/3_screens/**/*.md                — component inventories, states, data reqs, layout constraints
  _concept/2_experience/3_screens/00_layout/shell.md     — app shell structure, navigation, responsive breakpoints
  _concept/2_experience/1_journeys/stories.json          — user journeys with personas and story maps
  _concept/1_discovery/3_brand/tokens.json              — color palette, fonts, radius, spacing, shadows, mode
  _concept/1_discovery/3_brand/identity.md              — design philosophy, atmosphere
  ? _concept/2_experience/2_features/**/*.md             — feature context for story organization

WRITES
  _concept/2_experience/4_storybook/                     — standalone Storybook project

REFERENCES
  shared/contracts/concept_structure.md               — valid paths
  shared/contracts/frontmatter.md                     — screen frontmatter fields

REQUIRES
  hard: pnpm, node
  state: _concept/2_experience/3_screens/ exists with at least one screen spec
  state: _concept/1_discovery/3_brand/tokens.json exists
  state: _concept/2_experience/1_journeys/stories.json exists

# --- Sub-skills ---
#
# This skill delegates to 4 sub-skills, run in sequence:
#
#   1. concept-2-experience-4-storybook-1-setup
#      Scaffold project, install deps, apply brand tokens, create src/@types/ directory
#
#   2. concept-2-experience-4-storybook-2-components
#      Identify custom components NOT in @postxl/ui-components, build them + stories
#
#   3. concept-2-experience-4-storybook-3-pages
#      Build AppShell + full-page compositions from screen specs + stories
#
#   4. concept-2-experience-4-storybook-4-journeys
#      Build clickable multi-screen user journey flows from stories.json

# --- Workflow ---

EMIT [concept-2-experience-4-storybook] started run_id=<uuid>

STEP 1: Setup
  - Run sub-skill `concept-2-experience-4-storybook-1-setup` completely
  - Verify _concept/2_experience/4_storybook/ exists with passing build

STEP 2: Custom Components
  - Run sub-skill `concept-2-experience-4-storybook-2-components` completely
  - Verify src/stories/Components/ has stories for all custom components

STEP 3: Pages
  - Run sub-skill `concept-2-experience-4-storybook-3-pages` completely
  - Verify src/stories/Pages/ has stories for every screen spec

STEP 4: Journeys
  - Run sub-skill `concept-2-experience-4-storybook-4-journeys` completely
  - Verify src/stories/Journeys/ has stories for all non-backlog journeys

STEP 5: Final verification
  $ cd _concept/2_experience/4_storybook && pnpm run build
  - Count story files per layer:
    $ echo "Components:" && ls src/stories/Components/*.stories.tsx 2>/dev/null | wc -l
    $ echo "Pages:" && find src/stories/Pages -name '*.stories.tsx' 2>/dev/null | wc -l
    $ echo "Journeys:" && find src/stories/Journeys -name '*.stories.tsx' 2>/dev/null | wc -l
  - IF any layer has 0 stories (except Components which may be empty if all are library) -> fix
  - Report counts to user

  IF agent-browser is available
    $ pnpm run storybook dev --port 6006 --no-open
    - Verify sidebar shows 3 top-level groups: Components, Pages, Journeys
    - Click through a few stories to verify rendering
    - Stop Storybook server when done

  EMIT [concept-2-experience-4-storybook] completed run_id=<uuid> components=<N> pages=<N> journeys=<N>

CHECKPOINT storybook_review
  > "Your app concept is now fully visualized in Storybook with 3 layers:
  >
  > **1. Building Blocks:** <N> custom components with brand tokens applied
  > **2. Screens:** <N> page compositions with all states
  > **3. User Journeys:** <N> clickable flows (<hero_count> hero, <vital_count> vital, <hygiene_count> hygiene)
  >
  > Run `cd _concept/2_experience/4_storybook && pnpm run storybook dev` to explore.
  >
  > Review and tell me what to adjust."

MUST  run all 4 sub-skills in sequence — each depends on the previous
MUST  verify build passes after all sub-skills complete
MUST  report story counts per layer
NEVER skip any sub-skill

CHECKLIST
  - [ ] Sub-skill 1 (setup) completed — project scaffolded, brand applied, src/@types/ created
  - [ ] Sub-skill 2 (components) completed — custom components built with stories
  - [ ] Sub-skill 3 (pages) completed — all screen specs rendered as page stories
  - [ ] Sub-skill 4 (journeys) completed — all non-backlog journeys as click-dummies
  - [ ] Final build passes
  - [ ] All 3 layers visible in sidebar (verified)
