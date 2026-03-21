---
name: concept-2-experience-4-storybook-4-journeys
description: "Sub-skill 4/4: Build clickable user journey stories that let users walk through multi-screen flows using real UI elements. Each journey composes existing pages inside the AppShell. Covers hero, vital, and hygiene flows from stories.json. Run by the storybook orchestrator."
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

ROLE  Journey Builder — creates clickable multi-screen user flow stories from journey definitions.

READS
  _concept/2_experience/1_journeys/stories.json          — user journeys: story maps, personas, stages
  _concept/2_experience/4_storybook/src/pages/manifest.json — screen-to-page mapping (from pages sub-skill)
  _concept/2_experience/3_screens/**/*.md                — screen specs (fallback for mapping)

WRITES
  _concept/2_experience/4_storybook/src/stories/Journeys/Hero/<FlowName>.stories.tsx
  _concept/2_experience/4_storybook/src/stories/Journeys/Vital/<FlowName>.stories.tsx
  _concept/2_experience/4_storybook/src/stories/Journeys/Hygiene/<FlowName>.stories.tsx

REQUIRES
  state: _concept/2_experience/4_storybook/src/pages/ has page components (from step 3)
  state: _concept/2_experience/4_storybook/src/components/AppShell.tsx exists (from step 3)
  state: _concept/2_experience/1_journeys/stories.json exists

# --- Workflow ---

STEP 1: Map journeys to pages
  - Read stories.json — collect ALL story maps where stage is hero, vital, OR hygiene
  - Count: hero=<N> (must be exactly 1), vital=<N>, hygiene=<N>
  - Skip backlog stage
  - Read src/pages/manifest.json for the screen-to-page mapping
  - For each story map, identify which pages each story step maps to:
    - Use manifest.json to resolve candidate_screens to page component imports
    - If a screen is not in the manifest, fall back to matching by route/purpose in screen specs
  - Present the journey-to-page mapping:
    > "Journeys to build:
    > - Hero: <name> (N steps -> [page1, page2, ...])
    > - Vital: <name1> (N steps), <name2> (N steps)
    > - Hygiene: <name1> (N steps), ..."
  EMIT [concept-2-experience-4-storybook-4-journeys] started run_id=<uuid> hero=1 vital=<N> hygiene=<N>

STEP 2: Build journey stories
  For EACH story map (hero, vital, AND hygiene):

  Write src/stories/Journeys/<StageName>/<JourneyName>.stories.tsx:
    - title: 'Journeys/<Stage>/<JourneyLabel>'
    - layout: 'fullscreen'
    - ONLY import existing page components from src/pages/ and AppShell
    - The journey is a single Interactive story — a TRUE CLICK-DUMMY:

    Implementation pattern:
      - Render AppShell with sidebar, top bar, and navigation at all times
      - Use React useState to track the current step (screen index)
      - Render the matching page component as AppShell children
      - Wire onClick handlers on REAL UI elements to advance:
        - Sidebar/top bar nav items -> navigate to corresponding screen
        - Action buttons ("Submit", "Create", "Save") -> advance to next step
        - Links and menu items -> navigate to target screen
      - Highlight the active navigation item matching the current step
      - Show a subtle step indicator banner: persona name, step N of M, step description
      - CLICK-HINT HIGHLIGHTING:
        When user clicks somewhere that does NOT advance the step,
        apply .click-hint CSS class to all elements that DO advance.
        Auto-hide after 3 seconds.
        Implementation: track showHints state + timeout ref, apply class to advancing elements
      - Data should reflect journey progression (e.g., after "create project",
        the next screen shows the new project in the list)
      - Reuse types from src/@types/ — do NOT create new type files
      - Create per-step data objects inline in the story file

    For the hero flow: mark it as the default story when Storybook opens.

STEP 3: Verify
  - Count journey story files:
    $ find _concept/2_experience/4_storybook/src/stories/Journeys -name '*.stories.tsx' | wc -l
    -> Must equal total count of hero + vital + hygiene story maps
  $ cd _concept/2_experience/4_storybook && pnpm run build
  IF build fails
    - Fix and retry

  IF agent-browser is available
    $ cd _concept/2_experience/4_storybook && pnpm run storybook dev --port 6006 --no-open &
    - Open http://localhost:6006
    - Navigate to Journeys/Hero/ — click through the hero flow end-to-end
    - Verify: screens swap inside AppShell, navigation highlights update, click-hints pulse
    - Spot-check 1 vital journey
    - Stop Storybook server
    IF rendering issues found
      - Fix and re-verify

  EMIT [concept-2-experience-4-storybook-4-journeys] completed run_id=<uuid> journeys=<N>

  Expected sidebar structure:
  ```
  Journeys/
  ├── Hero/
  │   └── <HeroFlowName>              <- exactly 1 hero flow
  ├── Vital/
  │   ├── <VitalFlow1>                <- ALL vital flows
  │   └── <VitalFlow2>
  └── Hygiene/
      ├── <HygieneFlow1>             <- ALL hygiene flows
      └── <HygieneFlow2>
  ```

MUST  produce a journey story for EVERY non-backlog story map
MUST  use ONLY existing page components and AppShell — no journey-specific components
MUST  navigate through real UI elements — no "Next"/"Previous" buttons
MUST  include click-hint highlighting (pulsing .click-hint class)
MUST  show persona and step indicator as subtle banner
MUST  render full AppShell with active navigation at all times
NEVER create journey-specific components, layouts, or wrappers
NEVER add "Next Step" / "Previous Step" navigation buttons
NEVER skip vital or hygiene flows — all non-backlog journeys are required

CHECKLIST
  - [ ] Hero flow has a journey story in Journeys/Hero/
  - [ ] ALL vital flows have journey stories in Journeys/Vital/
  - [ ] ALL hygiene flows have journey stories in Journeys/Hygiene/
  - [ ] Journey story count matches hero + vital + hygiene story map count
  - [ ] Journeys use ONLY existing pages and AppShell
  - [ ] Navigation via real UI elements, no prev/next buttons
  - [ ] Click-hint highlighting works (pulse on wrong click)
  - [ ] Persona + step indicator shown
  - [ ] Build passes
