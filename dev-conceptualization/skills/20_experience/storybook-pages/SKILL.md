---
name: "storybook-pages"
description: "Sub-skill 3/4: Build AppShell and full-page screen compositions from screen specs. Each page includes all state variants and responsive viewports. Writes manifest.json for the journeys sub-skill. Called by the storybook orchestrator."
metadata:
  version: "1.0.0"
  tags:
    - "storybook"
    - "pages"
    - "screens"
    - "compositions"
    - "app-shell"
    - "states"
    - "responsive"
  source: "MERGED"
---

# Storybook Pages (Sub-skill 3/4)

ROLE  Page Builder — creates full-page screen compositions from screen specs, including AppShell.

READS
  _concept/2_experience/3_screens/**/*.md               — screen specs (purpose, route, UI elements, states, data)
  _concept/2_experience/3_screens/00_layout/shell.md    — app shell structure, navigation, breakpoints
  _concept/1_discovery/2_brand/tokens.json              — brand tokens
  _concept/2_experience/4_storybook/src/components/     — custom components from sub-skill 2
  [passed by orchestrator]: component_library, story_extension, package_manager

WRITES
  _concept/2_experience/4_storybook/src/components/AppShell.<ext>       — shared app shell wrapper
  _concept/2_experience/4_storybook/src/pages/<Group>/<PageName>.<ext>  — page compositions
  _concept/2_experience/4_storybook/src/stories/Pages/<NN Group>/<Page>.stories.<ext>
  _concept/2_experience/4_storybook/src/@types/<entity>.<ts|js>         — minimal interfaces for page data
  _concept/2_experience/4_storybook/src/pages/manifest.json             — screen-to-page mapping for journeys

REQUIRES
  state: _concept/2_experience/4_storybook/ exists with passing build
  state: _concept/2_experience/4_storybook/src/components/ has barrel export (from sub-skill 2)
  state: _concept/2_experience/3_screens/ has screen specs
  provided: component_library, story_extension, package_manager (from orchestrator)

EMIT  [storybook-pages] started run_id=<uuid> screens=<N>

STEP 1: Read all screen specs
  - Read 00_layout/shell.md for navigation structure, sidebar items, header, footer, breakpoints
  - Read ALL screen specs in _concept/2_experience/3_screens/**/*.md (excluding 00_layout/)
  - Build complete inventory:
    | Group | Screen | Route | UI Elements | States | Data Requirements |

STEP 2: Build AppShell component
  OUTPUT _concept/2_experience/4_storybook/src/components/AppShell.<ext>
    - Render full app shell from shell.md: sidebar, header/top bar, content area
    - Navigation items derived from shell spec — never hardcoded
    - Accept children prop/slot for page content area
    - Support collapsed/expanded sidebar states
    - Responsive: mobile overlay, desktop fixed sidebar (use breakpoints from shell.md)
    - Use component_library components for shell elements (sidebar, avatar, nav, button, etc.)
    - Apply brand tokens throughout

STEP 3: Build AppShell story
  OUTPUT _concept/2_experience/4_storybook/src/stories/Pages/00 Layout/AppShell.stories.<ext>
    - title: 'Pages/00 Layout/AppShell'
    - layout: 'fullscreen'
    - Variants: DesktopExpanded, DesktopCollapsed, Mobile (at minimum)

STEP 4: Add types for pages
  - For each page, identify data entities from screen spec Data Requirements
  - Add MINIMAL interfaces to src/@types/<entity>.<ts|js>
    - Only include properties the page renders
    - Reuse + extend types from sub-skill 2 — don't duplicate
    - Add comment: `// Minimal type for Storybook`
    - Re-export from src/@types/index barrel
  - Create inline seed data in story files:
    - Realistic domain-appropriate values from screen spec
    - Never use "Lorem ipsum"

STEP 5: Build page components
  For EACH screen spec (excluding 00_layout/):
    - Derive numbered group prefix from folder name (e.g., 01_user_auth → "01")
    - Derive human-readable group name (e.g., 01_user_auth → "User Auth")

    a) OUTPUT src/pages/<Group>/<PageName>.<story_extension>
      - Import types from src/@types/
      - Import library components directly from component_library
      - Import custom components from src/components/index barrel
      - Compose all UI elements from screen spec
      - Apply layout constraints from screen spec (max-width, scroll, fixed elements)
      - Wire data via props (typed with src/@types/ interfaces)
      - Handle interactive states with framework-appropriate state management
      - Apply responsive breakpoints

    b) OUTPUT src/stories/Pages/<NN Group>/<PageName>.stories.<story_extension>
      - title: 'Pages/<NN> <GroupName>/<PageName>'
      - layout: 'fullscreen'
      - For EACH state listed in screen spec:
        - Create named story variant: Default, Populated, Empty, Loading, Error, etc.
        - Use appropriate seed data scenario
      - Add responsive variants: Mobile, Tablet

STEP 6: Write manifest and verify
  OUTPUT _concept/2_experience/4_storybook/src/pages/manifest.json
    {
      "<screen-spec-relative-path>": {
        "component": "<Group>/<PageName>",
        "import": "./src/pages/<Group>/<PageName>",
        "route": "<route from screen spec>"
      }
    }

  $ find _concept/2_experience/4_storybook/src/stories/Pages -name '*.stories.*' | wc -l
  → Should cover every screen spec + AppShell

  $ cd _concept/2_experience/4_storybook && <package_manager> run build
  IF build fails
    - Fix and retry

  EMIT  [storybook-pages] completed run_id=<uuid> pages=<N>

MUST  build AppShell first — pages render inside it
MUST  import library components directly from component_library — not via wrappers
MUST  import custom components from src/components/index barrel
MUST  use icon_library for all icons
MUST  create state variants for every state listed in screen specs
MUST  include responsive variants (Mobile, Tablet) for each page
MUST  use realistic domain-appropriate data — never "Lorem ipsum"
MUST  write src/pages/manifest.json — required by storybook-journeys
NEVER hardcode navigation items — derive from shell spec
NEVER skip screen states documented in screen specs
NEVER invent colors or fonts — use CSS custom properties

CHECKLIST
  - [ ] AppShell component built from shell.md
  - [ ] AppShell story has responsive variants
  - [ ] Every screen spec has a page component in src/pages/
  - [ ] Every screen spec has a page story in src/stories/Pages/
  - [ ] Minimal types in src/@types/ for page data
  - [ ] Page stories have state variants matching screen spec
  - [ ] Responsive variants included (Mobile, Tablet)
  - [ ] src/pages/manifest.json maps every screen spec to its component
  - [ ] Build passes
