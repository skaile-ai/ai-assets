---
name: "skailup-storybook-components"
description: "Sub-skill 2/4: Identify custom building-block components from screen specs, build them using the project's component library, and create their Storybook stories. Called by the storybook orchestrator."
metadata:
  version: "1.0.0"
  tags:
    - "storybook"
    - "components"
    - "stories"
    - "building-blocks"
    - "custom-components"
  source: "MERGED"
  parameters:
    depth:
      type: enum
      values: [none, light, medium, max]
      default: medium
  prerequisites:
    files:
      - path: "_concept/2_experience/4_storybook"
        gate: hard
        description: "Storybook project must be scaffolded first (run storybook-setup)"
      - path: "_concept/2_experience/3_screens"
        gate: hard
        description: "Screen specs required to identify custom building-block components"
        min_entries: 1
    reads:
      - path: "_concept/1_discovery/2_brand/tokens.json"
        description: "Brand tokens for component styling"
    produces:
      - path: "_concept/2_experience/4_storybook/src/components"
        description: "Custom building-block component files and barrel export"
---

# Storybook Components (Sub-skill 2/4)

ROLE  Component Builder — identifies custom components not available in the project's component
      library, builds them, and creates their Storybook stories.

READS
  _concept/2_experience/3_screens/**/*.md               — UI elements from all screen specs
  _concept/1_discovery/2_brand/tokens.json              — brand tokens for styling
  [passed by orchestrator]: component_library, story_extension, icon_library, component_import

WRITES
  _concept/2_experience/4_storybook/src/components/<ComponentName>.<ext>
  _concept/2_experience/4_storybook/src/components/index.<ts|js>         — barrel export
  _concept/2_experience/4_storybook/src/stories/Components/<Name>.stories.<ext>
  _concept/2_experience/4_storybook/src/@types/<entity>.<ts|js>          — minimal interfaces

REQUIRES
  state: _concept/2_experience/4_storybook/ exists with passing build (setup sub-skill completed)
  state: _concept/2_experience/3_screens/ has at least one screen spec
  provided: component_library, story_extension, icon_library (from orchestrator)

EMIT  [storybook-components] started run_id=<uuid>

STEP 1: Inventory components across all screen specs
  - Read ALL screen specs in _concept/2_experience/3_screens/**/*.md
  - Extract a DEDUPLICATED list of every UI element mentioned in "UI Elements" sections
  - For each UI element, determine:
    a) LIBRARY — available in the project's component_library → use directly in pages, no story needed
    b) CUSTOM — not in library → must be built
  - Inspect the installed component_library package to discover its exports:
    $ cat _concept/2_experience/4_storybook/node_modules/<component_library>/dist/index.d.ts
    (or index.js — whichever exposes the exports)
  - Present the two lists to confirm scope:
    > "Component library (<component_library>) provides: [list]
    > Custom components to build: [list]
    > Does this look right?"
  EMIT  [storybook-components] checkpoint library=<N> custom=<N>

STEP 2: Build types for custom components
  - For each custom component, identify data entities its props will reference
  - Write MINIMAL interfaces to src/@types/<entity>.<ts|js>:
    - Only include properties the component actually renders
    - Use simple types (string, number, boolean, Date) — no ORM/framework imports
    - Add comment: `// Minimal type for Storybook — replace with generated types after datamodel`
    - Export from src/@types/index.<ts|js> barrel
  - If a type file already exists, extend it — don't overwrite

STEP 3: Build each custom component
  For EACH custom component:

  a) Write src/components/<ComponentName>.<story_extension>
    - Import types from src/@types/
    - Compose from component_library primitives where possible
    - If no suitable library primitive exists, use plain HTML/CSS with brand tokens
    - Use icon_library for all icons — never emojis or icon fonts
    - Apply brand tokens via CSS custom properties (--color-*, --font-*, --radius)
    - Document props via JSDoc/comments for Storybook autodocs

  b) Write src/stories/Components/<ComponentName>.stories.<story_extension>
    - title: 'Components/<ComponentName>'
    - Include variant stories as applicable:
      - Default — standard rendering
      - AllVariants — all variants/sizes side by side
      - WithData — populated with realistic domain data
      - Empty — empty state (if component supports it)
      - Loading — skeleton/spinner state (if applicable)
    IF component appears in multiple screens with different configurations
      - Add a story variant per unique configuration
  MUST use realistic domain-appropriate data — never "Lorem ipsum"

STEP 4: Write barrel export
  OUTPUT _concept/2_experience/4_storybook/src/components/index.<ts|js>
    - Re-export all custom components built in Step 3
  IF no custom components were built
    - Write empty barrel with comment: `// No custom components — all from <component_library>`
    - Create src/stories/Components/README.md explaining this

STEP 5: Verify
  - Count component story files:
    $ ls _concept/2_experience/4_storybook/src/stories/Components/*.stories.* 2>/dev/null | wc -l
    -> Should equal the number of custom components
  $ cd _concept/2_experience/4_storybook && <package_manager> run build
  IF build fails
    - Fix and retry
  EMIT  [storybook-components] completed run_id=<uuid> components=<N>

MUST  derive components from screen spec "UI Elements" sections — never invent component names
MUST  check component_library exports before deciding something is "custom"
MUST  use icon_library provided by orchestrator — never hardcode lucide-react or heroicons
MUST  use 'Components/<Name>' as the story title prefix
MUST  include autodocs tag on all component stories
MUST  write src/components/index barrel — the pages sub-skill imports from here
MUST  use realistic domain-appropriate data in stories
NEVER create stories for components already available in the component_library
NEVER invent colors or fonts — use CSS custom properties from brand tokens

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip this skill entirely |
| `light` | Minimal setup — hero flow stories only |
| `medium` | Standard setup — hero + vital flow stories (default) |
| `max` | Full setup — all flows, edge cases, responsive variants, dark mode |

CHECKLIST
  - [ ] All screen spec UI elements inventoried and categorized (library vs custom)
  - [ ] Minimal types written to src/@types/ for component props
  - [ ] Every custom component has a component file in src/components/
  - [ ] Every custom component has a stories file in src/stories/Components/
  - [ ] src/components/index barrel exports all custom components
  - [ ] Icons use the project's icon_library (not hardcoded)
  - [ ] Build passes
