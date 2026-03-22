---
name: "storybook-journeys"
description: "Sub-skill 4/4: Build clickable multi-screen user journey stories (click-dummies). Each journey flows through real page components inside AppShell. Covers hero, vital, and hygiene flows from stories.json. Called by the storybook orchestrator."
metadata:
  version: "1.0.0"
  tags:
    - "storybook"
    - "journeys"
    - "click-dummy"
    - "user-flows"
    - "interactive"
    - "hero"
    - "vital"
    - "hygiene"
  source: "MERGED"
---

# Storybook Journeys (Sub-skill 4/4)

ROLE  Journey Builder — creates clickable multi-screen user flow stories (click-dummies)
      that let users walk through complete journeys using real UI elements.

READS
  _concept/2_experience/1_journeys/stories.json          — user journeys: story maps, personas, stages
  _concept/2_experience/4_storybook/src/pages/manifest.json — screen-to-page mapping (from pages sub-skill)
  _concept/2_experience/3_screens/**/*.md                — screen specs (fallback for mapping)
  [passed by orchestrator]: story_extension, package_manager

WRITES
  _concept/2_experience/4_storybook/src/stories/Journeys/Hero/<FlowName>.stories.<ext>
  _concept/2_experience/4_storybook/src/stories/Journeys/Vital/<FlowName>.stories.<ext>
  _concept/2_experience/4_storybook/src/stories/Journeys/Hygiene/<FlowName>.stories.<ext>

REQUIRES
  state: _concept/2_experience/4_storybook/src/pages/ has page components (sub-skill 3 completed)
  state: _concept/2_experience/4_storybook/src/components/AppShell exists (sub-skill 3 completed)
  state: _concept/2_experience/4_storybook/src/pages/manifest.json exists
  state: _concept/2_experience/1_journeys/stories.json exists

EMIT  [storybook-journeys] started run_id=<uuid>

STEP 1: Map journeys to pages
  - Read stories.json — collect ALL story maps where stage is hero, vital, OR hygiene
    (skip backlog stage)
  - Count: hero=<N> (must be exactly 1), vital=<N>, hygiene=<N>
  - Read src/pages/manifest.json for the screen-to-page mapping
  - For each story map, resolve candidate_screens to page component imports:
    - Use manifest.json as primary source
    - Fall back to matching by route/purpose in screen specs if not in manifest
  - Present the journey-to-page mapping:
    > "Journeys to build:
    > - Hero: <name> (N steps → [page1, page2, ...])
    > - Vital: <name1> (N steps), <name2> (N steps)
    > - Hygiene: <name1> (N steps), ..."
  EMIT  [storybook-journeys] checkpoint hero=1 vital=<N> hygiene=<N>

STEP 2: Build journey stories
  For EACH story map (hero, vital, AND hygiene):

  OUTPUT src/stories/Journeys/<Stage>/<JourneyName>.stories.<story_extension>

    title: 'Journeys/<Stage>/<JourneyLabel>'
    layout: 'fullscreen'

    ONLY import existing page components from src/pages/ and AppShell.
    The journey is a single Interactive story — a TRUE CLICK-DUMMY:

    Implementation pattern:
      - Render AppShell with sidebar, navigation at all times
      - Use framework-appropriate reactive state to track the current step (screen index)
      - Render the matching page component as AppShell content based on current step
      - Wire click/event handlers on REAL UI elements to advance steps:
          - Sidebar/top bar nav items → navigate to corresponding screen
          - Action buttons ("Submit", "Create", "Save") → advance to next step
          - Links and menu items → navigate to target screen
      - Highlight the active navigation item matching the current step
      - Show a subtle step indicator banner:
          persona name | Step N of M | step description
      - CLICK-HINT HIGHLIGHTING:
          When user interacts with an element that does NOT advance the step,
          apply .click-hint CSS class to all elements that DO advance.
          Auto-hide after 3 seconds.
          (Track showHints state + timeout, apply class to advancing elements)
      - Data should reflect journey progression:
          e.g., after "create project", next screen shows the new project in the list
      - Reuse types from src/@types/ — do NOT create new type files
      - Create per-step data objects inline in the story file

    For the hero flow: mark it as the default story when Storybook opens.

STEP 3: Verify
  $ find _concept/2_experience/4_storybook/src/stories/Journeys -name '*.stories.*' | wc -l
  → Must equal total count of hero + vital + hygiene story maps

  $ cd _concept/2_experience/4_storybook && <package_manager> run build
  IF build fails
    - Fix and retry

  EMIT  [storybook-journeys] completed run_id=<uuid> journeys=<N>

  Expected sidebar structure:
  ```
  Journeys/
  ├── Hero/
  │   └── <HeroFlowName>          ← exactly 1 hero flow (default story)
  ├── Vital/
  │   ├── <VitalFlow1>
  │   └── <VitalFlow2>
  └── Hygiene/
      ├── <HygieneFlow1>
      └── <HygieneFlow2>
  ```

MUST  produce a journey story for EVERY non-backlog story map
MUST  use ONLY existing page components and AppShell — no journey-specific components
MUST  navigate through REAL UI elements — no "Next Step" / "Previous Step" buttons
MUST  include click-hint highlighting (.click-hint class on elements that advance)
MUST  show persona name and step indicator as a subtle banner
MUST  render full AppShell with active navigation at all times
MUST  mark hero flow as the default Storybook story
NEVER create journey-specific components, layouts, or wrappers
NEVER add explicit "Next Step" / "Previous Step" navigation
NEVER skip vital or hygiene flows — all non-backlog journeys are required

CHECKLIST
  - [ ] Hero flow has a journey story in Journeys/Hero/
  - [ ] ALL vital flows have journey stories in Journeys/Vital/
  - [ ] ALL hygiene flows have journey stories in Journeys/Hygiene/
  - [ ] Journey story count matches hero + vital + hygiene story map count
  - [ ] Journeys use ONLY existing pages and AppShell (no new components)
  - [ ] Navigation via real UI elements only — no prev/next buttons
  - [ ] Click-hint highlighting implemented (.click-hint class)
  - [ ] Persona + step indicator shown as subtle banner
  - [ ] Hero flow is marked as default story
  - [ ] Build passes
