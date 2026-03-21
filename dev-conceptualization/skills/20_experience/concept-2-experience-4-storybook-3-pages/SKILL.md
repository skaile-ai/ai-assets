---
name: concept-2-experience-4-storybook-3-pages
description: "Sub-skill 3/4: Build full-page compositions from screen specs and their Storybook stories. Each page uses @postxl/ui-components directly for library components and custom components from the previous step. Includes state variants, seed data scenarios, and responsive viewports. Run by the storybook orchestrator."
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

ROLE  Page Builder — creates full-page React compositions from screen specs and their stories.

READS
  _concept/2_experience/3_screens/**/*.md               — all screen specs (purpose, route, components, states, data, layout)
  _concept/2_experience/3_screens/00_layout/shell.md    — app shell structure, navigation, responsive breakpoints
  _concept/1_discovery/3_brand/tokens.json              — brand tokens

WRITES
  _concept/2_experience/4_storybook/src/components/AppShell.tsx          — shared app shell wrapper
  _concept/2_experience/4_storybook/src/pages/<Group>/<PageName>.tsx     — page compositions
  _concept/2_experience/4_storybook/src/stories/Pages/<NN Group>/<PageName>.stories.tsx
  _concept/2_experience/4_storybook/src/@types/<entity>.ts              — minimal interfaces for page data
  _concept/2_experience/4_storybook/src/pages/manifest.json            — screen-to-page mapping for journeys sub-skill

REFERENCES
  (none — imports are resolved from the installed packages and src/components/index.ts barrel)

REQUIRES
  state: _concept/2_experience/4_storybook/ exists with passing build
  state: _concept/2_experience/4_storybook/src/components/ has custom components (from step 2)
  state: _concept/2_experience/3_screens/ exists with screen specs

# --- Workflow ---

STEP 1: Read all screen specs
  - Read 00_layout/shell.md for app shell navigation, sidebar items, header, footer, breakpoints
  - Read ALL screen specs in _concept/2_experience/3_screens/**/*.md (excluding 00_layout/)
  - Build complete inventory:
    | Group | Screen | Route | Components Used | States | Data Entities |
  EMIT [concept-2-experience-4-storybook-3-pages] started run_id=<uuid> screens=<N>

STEP 2: Build AppShell component
  - Write src/components/AppShell.tsx:
    - Render full app shell from 00_layout/shell.md
    - Include sidebar with navigation items (derive from shell spec, not hardcoded)
    - Include top bar with logo/app name, search, user avatar
    - Use @postxl/ui-components: Sidebar, SidebarProvider, Avatar, Button, etc.
    - Accept children prop for page content area
    - Support collapsed/expanded sidebar states
    - Responsive: mobile uses Sheet overlay, desktop uses fixed sidebar

STEP 3: Build AppShell story
  - Write src/stories/Pages/00 Layout/AppShell.stories.tsx:
    - title: 'Pages/00 Layout/AppShell'
    - layout: 'fullscreen'
    - Variants: DesktopExpanded, DesktopCollapsed, Tablet, Mobile

STEP 4: Add types needed for pages
  - For each page, identify data entities from screen spec Data Requirements
  - Add MINIMAL TypeScript interfaces to src/@types/<entity>.ts:
    - Only include properties the page actually renders (labels, counts, dates, statuses)
    - Reuse and extend types already created by the components sub-skill — don't duplicate
    - Add comment: `// Minimal type for Storybook — will be replaced by generated types`
    - Re-export from src/@types/index.ts barrel
  - Create inline seed data objects in story files using these types
    - Realistic values derived from screen spec Template Data section
    - Never use "Lorem ipsum" — use domain-appropriate placeholder content

STEP 5: Build page components
  For EACH screen spec (excluding 00_layout/):
    - Derive numbered group prefix from folder name (e.g., 01_auth_tenancy -> "01")
    - Derive human-readable group name (e.g., 01_auth_tenancy -> "Auth Tenancy")

    a) Write src/pages/<Group>/<PageName>.tsx:
      - Import types from src/@types/
      - Import library components DIRECTLY from "@postxl/ui-components"
      - Import custom components from src/components/index.ts barrel (built in step 2)
      - Use lucide-react for all icons
      - Compose all components listed in the screen spec's Component Inventory
      - Apply layout constraints (max-width, scroll, fixed elements, responsive rules)
      - Wire up data via props (typed with src/@types/ interfaces)
      - Implement interactive states with React useState
      - Apply responsive breakpoints

    b) Write src/stories/Pages/<NN Group>/<PageName>.stories.tsx:
      - title: 'Pages/<NN> <GroupName>/<PageName>'
      - layout: 'fullscreen'
      - For EACH state in screen spec's ## States section:
        - Create a named story variant (Default, Loading, Empty, Error, etc.)
        - Use appropriate seed data scenario
      - Add responsive variants: Mobile, Tablet

STEP 6: Verify
  - Count page story files:
    $ find _concept/2_experience/4_storybook/src/stories/Pages -name '*.stories.tsx' | wc -l
    -> Should cover every screen spec + AppShell
  $ cd _concept/2_experience/4_storybook && pnpm run build
  IF build fails
    - Fix and retry

  Write src/pages/manifest.json — screen-to-page mapping for the journeys sub-skill:
    {
      "<screen-spec-path>": {
        "component": "<Group>/<PageName>",
        "import": "./src/pages/<Group>/<PageName>",
        "route": "<route from screen spec>"
      }
    }

  IF agent-browser is available
    $ cd _concept/2_experience/4_storybook && pnpm run storybook dev --port 6006 --no-open &
    - Open http://localhost:6006
    - Navigate to Pages/ — verify AppShell renders with sidebar and header
    - Spot-check 2-3 page stories — verify layout, data binding, responsive variants
    - Check WCAG AA contrast on text elements
    - Stop Storybook server
    IF rendering issues found
      - Fix and re-verify

  EMIT [concept-2-experience-4-storybook-3-pages] completed run_id=<uuid> pages=<N>

  Expected sidebar structure:
  ```
  Pages/
  ├── 00 Layout/
  │   └── AppShell
  ├── 01 Auth Tenancy/
  │   ├── Login
  │   └── UserManagement
  ├── 02 Projects/
  │   └── ProjectDashboard
  └── ... (every screen spec)
  ```

MUST  import library components directly from "@postxl/ui-components" — not via wrappers
MUST  import custom components from src/components/index.ts barrel
MUST  use lucide-react for all icons
MUST  create state variants for every state listed in screen specs
MUST  include responsive variants (Mobile, Tablet) for each page
MUST  build AppShell first — pages render inside it
MUST  write types to src/@types/ — only properties needed for rendering, nothing more
MUST  pass WCAG AA contrast (4.5:1) for all text elements
MUST  use realistic domain-appropriate data — never use "Lorem ipsum"
NEVER hardcode navigation items — derive from shell spec
NEVER skip screen states documented in screen specs
NEVER invent colors or fonts — use CSS custom properties

CHECKLIST
  - [ ] AppShell component built from shell.md with sidebar + header
  - [ ] AppShell story has responsive variants (expanded, collapsed, tablet, mobile)
  - [ ] Every screen spec has a page component in src/pages/
  - [ ] Every screen spec has a page story in src/stories/Pages/
  - [ ] Minimal types in src/@types/ for page data entities
  - [ ] Page stories have state variants matching screen spec ## States
  - [ ] Story data uses realistic domain-appropriate content (no Lorem ipsum)
  - [ ] Responsive variants included (Mobile, Tablet)
  - [ ] src/pages/manifest.json maps every screen spec to its page component
  - [ ] WCAG AA contrast (4.5:1) verified for text elements
  - [ ] Build passes
