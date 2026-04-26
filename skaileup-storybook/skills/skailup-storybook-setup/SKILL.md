---
name: "skailup-storybook-setup"
description: "Sub-skill 1/4: Scaffold a standalone Storybook project, install dependencies, and apply brand tokens as CSS custom properties. Called by the storybook orchestrator."
metadata:
  version: "1.0.0"
  tags:
    - "storybook"
    - "setup"
    - "scaffold"
    - "brand-tokens"
    - "config"
  source: "MERGED"
  parameters:
    depth:
      type: enum
      values: [none, light, medium, max]
      default: medium
  artifacts:
    requires:
      - id: prototype
        gate: hard
        variant: storybook
    produces:
      - id: prototype
        description: "Storybook project scaffold with brand token integration"
        variant: storybook
  prerequisites:
    files:
      - path: "_concept/discovery/brand/tokens.json"
        gate: hard
        description: "Brand tokens required for CSS custom properties and Storybook theming"
      - path: "_concept/discovery/brief.md"
        gate: hard
        description: "App name required for Storybook project branding"
    reads:
      - path: "_concept/discovery/brand/identity.md"
        description: "Design philosophy and atmosphere for Storybook customization"
      - path: "_concept/experience/screens/00_layout/shell.md"
        description: "Responsive breakpoints for Storybook viewport presets"
    produces:
      - path: "_concept/experience/4_storybook"
        description: "Storybook project scaffold with brand token integration"
---

# Storybook Setup (Sub-skill 1/4)

ROLE  Storybook Setup — scaffolds the Storybook project, installs dependencies, and applies brand tokens.

READS
  _concept/discovery/brand/tokens.json              — color palette, fonts, radius, spacing, shadows, mode
  _concept/discovery/brand/identity.md              — design philosophy, atmosphere
  _concept/discovery/brief.md              — app name for Storybook branding
  _concept/experience/screens/00_layout/shell.md    — responsive breakpoints for viewport presets
  [passed by orchestrator]: storybook_addon, story_format, story_extension, component_library, package_manager

WRITES
  _concept/experience/4_storybook/                    — Storybook project scaffold

REQUIRES
  state: _concept/discovery/brand/tokens.json exists
  state: _concept/discovery/brief.md exists
  provided: storybook_addon, story_extension, component_library, package_manager (from orchestrator)

EMIT  [storybook-setup] started run_id=<uuid>

STEP 1: Read context
  - Read tokens.json for colors, fonts, radius, spacing, shadows, mode
  - Read identity.md for atmosphere and design philosophy
  - Read brief.md for app name
  - Read shell.md for responsive breakpoints (used in viewport presets)

STEP 2: Scaffold project
  $ mkdir -p _concept/experience/4_storybook/src/stories
  $ mkdir -p _concept/experience/4_storybook/.storybook

STEP 3: Write package.json
  OUTPUT _concept/experience/4_storybook/package.json
    {
      "name": "storybook",
      "private": true,
      "scripts": {
        "storybook": "storybook dev -p 6006",
        "build-storybook": "storybook build",
        "build": "storybook build"
      },
      "devDependencies": {
        "storybook": "^8.0.0",
        "<storybook_addon>": "^8.0.0",
        "@storybook/addon-essentials": "^8.0.0",
        "typescript": "^5.0.0"
      }
    }
  - Use the storybook_addon resolved by the orchestrator
  - Add any additional dependencies required by the tech stack (e.g., vite for React stacks)
  $ cd _concept/experience/4_storybook && <package_manager> install
  EMIT  [storybook-setup] checkpoint phase=deps_installed

STEP 4: Write .storybook/main config
  OUTPUT _concept/experience/4_storybook/.storybook/main.<ts|js>
    - framework: storybook_addon
    - stories: ['../src/**/*.stories.*']
    - addons: [storybook_addon, '@storybook/addon-essentials']
    - Viewport presets derived from shell.md breakpoints

STEP 5: Write .storybook/theme
  OUTPUT _concept/experience/4_storybook/.storybook/theme.<ts|js>
    - Storybook theme object using brand colors and fonts from tokens.json
    - base: tokens.json mode ('light' or 'dark')
    - appBg: tokens.json colors.background
    - fontBase: tokens.json fonts.body
    - fontCode: tokens.json fonts.mono
    - brandTitle: app name from brief.md

STEP 6: Write .storybook/preview (brand tokens as CSS custom properties)
  OUTPUT _concept/experience/4_storybook/.storybook/preview.<ts|js>
    - Import brand.css (global styles)
    - Apply CSS custom properties from tokens.json as decorators:
      - --color-primary, --color-secondary, --color-accent, --color-background,
        --color-surface, --color-foreground, --color-muted, --color-border,
        --color-destructive, --color-success, --color-warning, --radius,
        font variables from tokens.json fonts.*
    - Viewport presets from shell.md breakpoints

STEP 7: Write src/styles/brand.css
  OUTPUT _concept/experience/4_storybook/src/styles/brand.css
    :root {
      /* Font loading — Google Fonts import for heading and body fonts from tokens.json */
      /* CSS custom properties — all values from tokens.json */
      --color-primary: <tokens.colors.primary>;
      /* ... all 11 color tokens ... */
      --font-heading: '<tokens.fonts.heading>', sans-serif;
      --font-body: '<tokens.fonts.body>', sans-serif;
      --font-mono: '<tokens.fonts.mono>', monospace;
      --radius: <tokens.radius>;
      --spacing-base: <tokens.spacing_base>;
      /* shadows from tokens.shadows.sm/md/lg */
    }
    IF tokens.mode is "both"
      .dark {
        /* dark mode overrides */
      }
  NEVER invent colors, fonts, or spacing — everything comes from tokens.json

STEP 8: Create src/@types directory
  $ mkdir -p _concept/experience/4_storybook/src/@types
  - Write src/@types/README.md:
    "Types are built incrementally by each sub-skill.
     Only properties needed for rendering are defined here."
  - Types are NOT generated here — each subsequent sub-skill adds only the interfaces it needs.

STEP 9: Verify setup
  $ cd _concept/experience/4_storybook && <package_manager> run build
  IF build fails
    - Fix errors and retry
  EMIT  [storybook-setup] completed run_id=<uuid>

MUST  use storybook_addon provided by orchestrator — never hardcode @storybook/react or @storybook/vue
MUST  use package_manager provided by orchestrator — never hardcode pnpm
MUST  apply ALL CSS custom properties from tokens.json — no tokens may be invented
MUST  derive viewport presets from shell.md — never hardcode breakpoints
NEVER leave any placeholder values unreplaced
NEVER modify the brand.css .dark block if mode is light-only

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip this skill entirely |
| `light` | Minimal setup — hero flow stories only |
| `medium` | Standard setup — hero + vital flow stories (default) |
| `max` | Full setup — all flows, edge cases, responsive variants, dark mode |

CHECKLIST
  - [ ] package.json installed successfully
  - [ ] .storybook/main uses correct storybook_addon (not hardcoded)
  - [ ] .storybook/theme uses brand colors and fonts from tokens.json
  - [ ] .storybook/preview applies all CSS custom properties from tokens.json
  - [ ] brand.css has all color and font token values
  - [ ] brand.css .dark block removed if light-only mode
  - [ ] src/@types/ directory created
  - [ ] Storybook builds without errors
