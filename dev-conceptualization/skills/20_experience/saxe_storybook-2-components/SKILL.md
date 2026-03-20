---
name: concept-2-experience-4-storybook-2-components
description: "Sub-skill 2/4: Identify custom building-block components NOT available in @postxl/ui-components, build them, and create their Storybook stories. Components compose from @postxl/ui-components or Radix primitives. Run by the storybook orchestrator."
---

ROLE  Component Builder — identifies and builds custom components that are not available in @postxl/ui-components.

READS
  _concept/2_experience/3_screens/**/*.md               — component inventories from all screen specs
  _concept/1_discovery/3_brand/tokens.json              — brand tokens for styling

WRITES
  _concept/2_experience/4_storybook/src/components/<ComponentName>.tsx
  _concept/2_experience/4_storybook/src/components/index.ts              — barrel export of all custom components
  _concept/2_experience/4_storybook/src/stories/Components/<ComponentName>.stories.tsx
  _concept/2_experience/4_storybook/src/@types/<entity>.ts               — minimal interfaces for component props

REFERENCES
  (none — the installed @postxl/ui-components package is the source of truth)

REQUIRES
  state: _concept/2_experience/4_storybook/ exists with passing build (setup skill completed)
  state: _concept/2_experience/3_screens/ exists with at least one screen spec

# --- Workflow ---

STEP 1: Inventory all components across screen specs
  - Read the @postxl/ui-components barrel export to discover available library components:
    $ cat _concept/2_experience/4_storybook/node_modules/@postxl/ui-components/dist/index.d.ts
    (or index.js — whichever exposes the export list)
  - Read ALL screen specs in _concept/2_experience/3_screens/**/*.md
  - Extract a DEDUPLICATED list of every component referenced in Component Inventory sections
  - Cross-reference each component against the discovered library exports
  - Split into two lists:
    a) LIBRARY components — exported by @postxl/ui-components -> used directly in pages, NO story needed here
    b) CUSTOM components — NOT exported by @postxl/ui-components -> these need to be built
  - Present the two lists to confirm scope:
    > "Library components (used directly, no build needed): [list]
    > Custom components to build: [list]
    > Does this look right?"
  EMIT [concept-2-experience-4-storybook-2-components] started run_id=<uuid> library=<N> custom=<N>

STEP 2: Build types needed for components
  - For each custom component, identify the data entities its props reference
    (from screen spec Data Requirements and Component Inventory)
  - Write MINIMAL TypeScript interfaces to src/@types/<entity>.ts:
    - Only include properties the component actually renders
    - Use simple types (string, number, boolean, Date) — no Prisma imports
    - Add comment: `// Minimal type for Storybook — will be replaced by generated types`
    - Export from src/@types/index.ts barrel
  - If a type file already exists in src/@types/ (from setup), extend it — don't overwrite

STEP 3: Build each custom component
  For EACH custom component:

  a) Write src/components/<ComponentName>.tsx:
    - Import types from src/@types/
    - Compose from @postxl/ui-components primitives where possible
      (e.g., StatCard composes Card + Badge, ChatMessage composes Card + Avatar + Button)
    - If no suitable @postxl/ui-components primitive exists, use Radix primitives
    - Use lucide-react for all icons (never emojis or icon fonts)
    - Add comment: `// Custom component — not available in @postxl/ui-components`
    - Document props via JSDoc for Storybook autodocs
    - Apply brand tokens via CSS custom properties (--color-*, --font-*, --radius)

  b) Write src/stories/Components/<ComponentName>.stories.tsx:
    - title: 'Components/<ComponentName>'
    - tags: ['autodocs']
    - Include variant stories as applicable:
      - Default — standard rendering
      - AllVariants — all variants/sizes side by side
      - WithData — populated seed data
      - Empty — empty state (if component supports it)
      - Loading — skeleton/spinner state (if applicable)
      - Interactive — key interactions (hover, click, toggle)
    IF component appears in multiple screens with different configurations
      - Add a story variant per unique configuration

STEP 4: Write barrel export
  - Write src/components/index.ts:
    - Re-export all custom components built in Step 3
    - e.g., `export { StatCard } from './StatCard'`
    - This is the contract for the pages sub-skill — it imports custom components from here
  IF no custom components were built:
    - Write src/components/index.ts as empty file with comment: `// No custom components — all from @postxl/ui-components`
    - Create src/stories/Components/README.md explaining all components come from the library
    - Layer 1 in the sidebar may be empty — this is acceptable

STEP 5: Verify
  - Count component story files:
    $ ls _concept/2_experience/4_storybook/src/stories/Components/*.stories.tsx 2>/dev/null | wc -l
    -> Should equal the number of custom components from Step 1
  $ cd _concept/2_experience/4_storybook && pnpm run build
  IF build fails
    - Fix and retry

  IF agent-browser is available
    $ cd _concept/2_experience/4_storybook && pnpm run storybook dev --port 6006 --no-open &
    - Open http://localhost:6006
    - Navigate to Components/ in sidebar — verify each custom component renders correctly
    - Check brand tokens are applied (colors, fonts, radius match tokens.json)
    - Stop Storybook server
    IF rendering issues found
      - Fix and re-verify

  EMIT [concept-2-experience-4-storybook-2-components] completed run_id=<uuid> components=<N>

  Expected sidebar structure:
  ```
  Components/
  ├── StatCard          <- custom: composed from Card + Badge
  ├── ChatMessage       <- custom: composed from Card + Avatar
  ├── TimelineEntry     <- custom: not in library
  └── ... (only custom components)
  ```

MUST  use @postxl/ui-components as the first choice for composition
MUST  fall back to Radix primitives only when no @postxl/ui-components match exists
MUST  use lucide-react for all icons — never emojis or icon fonts
MUST  use 'Components/<Name>' as the story title prefix
MUST  include 'autodocs' tag on all component stories
MUST  pass WCAG AA contrast (4.5:1) for all text in component stories
MUST  use realistic domain-appropriate data in stories — never use "Lorem ipsum"
NEVER create stories for components that exist in @postxl/ui-components
NEVER invent colors or fonts — use CSS custom properties from brand tokens

MUST  write types to src/@types/ — only properties needed for rendering, nothing more
MUST  add `// Minimal type for Storybook` comment to all type files
MUST  write src/components/index.ts barrel — this is the contract for the pages sub-skill

CHECKLIST
  - [ ] All screen spec components inventoried and categorized (library vs custom)
  - [ ] Minimal types written to src/@types/ for component props
  - [ ] Every custom component has a .tsx file in src/components/
  - [ ] Every custom component has a .stories.tsx file in src/stories/Components/
  - [ ] src/components/index.ts barrel exports all custom components
  - [ ] Components compose from @postxl/ui-components where possible
  - [ ] All icons use lucide-react
  - [ ] Story count matches custom component count
  - [ ] Build passes
