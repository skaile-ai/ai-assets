---
name: concept-2-experience-3-screens
description: "Step 6: Screen specifications. Reads all approved artifacts (features, journeys, brand, tech stack, data model) and writes screen specs with PostXL ui-component inventories. Registers screens back into feature frontmatter."
keywords: screens, components, ux, mockups, planning, layout
---

ROLE  Screen Specification agent — produces _concept/2_experience/3_screens/ from all upstream artifacts including user journeys.

READS
  _concept/1_discovery/1_overview/brief.md                   — app name, audience, hero flow
  _concept/2_experience/2_features/**/*.md                    — requirements per feature
  ? _concept/2_experience/1_journeys/stories.json             — journey context for screen flow design
  ? _concept/3_blueprint/3_datamodel/postxl-schema.json      — models and fields (enriches data requirements if available)
  ? _concept/1_discovery/3_brand/tokens.json                 — colors, fonts, spacing, light/dark mode
  ? _concept/3_blueprint/1_techstack/stack.md                — framework and tech choices
  ? _concept/3_blueprint/2_architecture/architecture.md     — custom protocols, modules, extra apps
  ? _concept/3_blueprint/3_datamodel/seed.json               — scenario-based template data

WRITES
  _concept/2_experience/3_screens/00_layout/shell.md          — app shell: nav, sidebar, header, footer, breakpoints
  _concept/2_experience/3_screens/<NN_group>/<screen>.md       — per-screen spec with component inventory
  _concept/2_experience/2_features/**/*.md                     — feedback loop: add screens[] to feature frontmatter

REFERENCES
  shared/contracts/concept_structure.md            — valid _concept/ paths and naming rules
  shared/contracts/frontmatter.md                  — screen frontmatter fields and status lifecycle
  shared/contracts/feedback_loop.md                — cross-reference protocol (screens → features)
  references/ui_components.md             — PostXL ui-component catalog with exact names
  references/screen_spec_template.md      — full screen spec template with all sections

MUST  use exact component names from references/ui_components.md
MUST  specify DataGrid cell variants per column (checkbox, date, number, short-text, etc.)
MUST  reference seed.json scenarios (populated, empty, edge_cases) in Template Data section
MUST  register every screen back into its parent feature's screens[] frontmatter
MUST  write 00_layout/shell.md before any individual screen specs
NEVER  invent colors or fonts — always reference brand tokens from tokens.json
NEVER  write screens for features that have no approved feature spec

EMIT  [concept-2-experience-3-screens] started run_id=<uuid>

STEP 1: Read prerequisites
  - Read brief.md for app name, audience, hero flow
  - Read all _concept/2_experience/2_features/**/*.md for feature requirements
  - If any required read is missing, stop and name the prerequisite skill:
    brief.md → concept-1-discovery-1-overview, features → concept-2-experience-2-features

IF _concept/3_blueprint/3_datamodel/postxl-schema.json exists
  - Read postxl-schema.json for entity names and fields (enriches data requirements)
ELSE
  - Infer data entities from feature requirements and journey acceptance criteria

IF _concept/1_discovery/3_brand/tokens.json exists
  - Load brand tokens for color references, fonts, spacing, border radius, light/dark mode
ELSE
  - Proceed with defaults if user approves

IF _concept/3_blueprint/2_architecture/architecture.md exists
  - Note custom protocols (WebSocket, SSE) for real-time screens
  - Note additional apps and their communication flows
  - Note custom module API routes beyond standard CRUD

STEP 2: Derive screen list from features
  - For each feature, identify required screens
  - For each screen: name, route/URL, 3-second test, data entities from postxl-schema.json
  - Group screens by feature group (matching NN_group numbering)
  IF _concept/2_experience/1_journeys/stories.json exists
    - Use journey flows to inform screen navigation and user flow
    - Map story stages to screen sequences (hero → primary nav, vital → secondary nav)
    - Ensure screen transitions align with journey step ordering

  CHECKPOINT screen_list
    Present the screen list to the user.
    > "I've identified these screens: [list]. Add, remove, or rename?"

  UNTIL user approves the screen list

STEP 3: Write layout shell
  - $ mkdir -p _concept/2_experience/3_screens/00_layout

  OUTPUT _concept/2_experience/3_screens/00_layout/shell.md
    ---
    status: draft
    last_updated: <YYYY-MM-DD>
    ---
    Navigation, sidebar, header, footer, responsive breakpoints.
    Reference brand tokens for theming. Use Sidebar, NavigationMenu, Breadcrumb components.

STEP 4: Write screen specs
  - $ mkdir -p _concept/2_experience/3_screens/<NN_group>  (for each feature group)
  - For each screen, write a spec following references/screen_spec_template.md

  OUTPUT _concept/2_experience/3_screens/<NN_group>/<screen>.md
    ---
    status: draft
    implements:
      - 2_experience/2_features/<NN_group>/<feature>.md
    data_entities: [<Model>, ...]
    layout: 2_experience/3_screens/00_layout/shell.md
    last_updated: <YYYY-MM-DD>
    ---
    Sections: Purpose, Route, Component Inventory, Data Requirements,
    User Actions, States, Template Data.

STEP 5: Register screens in features (feedback loop)
  - For each screen written, update the parent feature's frontmatter:
    screens:
      - path: 2_experience/3_screens/<NN_group>/<screen>.md
        status: draft
  - EMIT  [concept-2-experience-3-screens] feedback_loop updated 2_experience/2_features/<NN_group>/<feature>.md added screen: 2_experience/3_screens/<NN_group>/<screen>.md

EMIT  [concept-2-experience-3-screens] checkpoint phase=screens_written

STEP 6: Human approval
  CHECKPOINT screens_approval
    Show business summary first:
      > "Here are the screens your users will see:
      > [List key screens in user terms: 'A login page', 'A dashboard showing...', 'A settings page where...']
      > Total: N screens across N feature groups.
      >
      > Do these cover all the things your users need to do? Add, remove, or adjust?"

  UNTIL user explicitly approves

STEP 7: Hand off
  - Tell the user:
    > "Screen specs approved. Next steps:
    > - Run `concept-2-experience-4-storybook` to generate visual mockups from these specs
    > - Run `app-design` to generate high-fidelity mockups
    > - Run `app-ready` to check E2E readiness"

EMIT  [concept-2-experience-3-screens] completed run_id=<uuid> screens_written=N features_updated=N

CHECKLIST
  - [ ] _concept/2_experience/3_screens/00_layout/shell.md exists
  - [ ] Every feature has at least one screen spec
  - [ ] All screen specs have required frontmatter (status, implements, data_entities, layout)
  - [ ] Component Inventory uses exact names from references/ui_components.md
  - [ ] DataGrid columns specify cell variants
  - [ ] Data Requirements reference postxl-schema.json entities
  - [ ] Template Data references seed.json scenarios
  - [ ] Feature files updated with screens[] in frontmatter (feedback loop)
  - [ ] User has explicitly approved the screen specs
