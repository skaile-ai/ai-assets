---
name: concept-2-experience-4-storybook-1-setup
description: "Sub-skill 1/4: Scaffold a standalone Storybook project, install dependencies from a pinned package.json, and apply brand tokens as CSS custom properties. Types and seed data are built incrementally by later sub-skills. Run by the storybook orchestrator."
---

ROLE Storybook Setup agent — scaffolds the project, installs deps, and applies brand tokens.

READS
  _concept/1_discovery/3_brand/tokens.json              — color palette, fonts, radius, spacing, shadows, mode
  _concept/1_discovery/3_brand/identity.md              — design philosophy, atmosphere
  _concept/1_discovery/1_overview/brief.md              — app name for Storybook branding
  _concept/2_experience/3_screens/00_layout/shell.md    — responsive breakpoints for viewport presets

WRITES
  _concept/2_experience/4_storybook/                    — Storybook project scaffold (config, styles, deps)

REFERENCES
  (none — all config is provided as ready-to-use templates in templates/)

REQUIRES
  hard: pnpm, node
  state: _concept/1_discovery/3_brand/tokens.json exists
  state: _concept/1_discovery/1_overview/brief.md exists

# --- Templates ---
#
# This skill ships ready-to-use templates in templates/:
#
#   templates/
#   ├── .storybook/
#   │   ├── main.ts          — copy verbatim
#   │   ├── preview.ts       — copy verbatim
#   │   └── theme.ts         — replace {{PLACEHOLDER}} values from tokens.json + brief.md
#   ├── src/styles/
#   │   └── brand.css        — replace {{PLACEHOLDER}} values from tokens.json
#   ├── vite.config.ts       — copy verbatim
#   └── tsconfig.json        — copy verbatim
#
# Files marked "copy verbatim" require no modification.
# Files with {{PLACEHOLDER}} markers need token values substituted.

# --- Workflow ---

STEP 1: Read context
  - Read tokens.json for colors, fonts, radius, spacing, shadows, mode
  - Read identity.md for atmosphere and design philosophy
  - Read brief.md for app name (used in Storybook branding)
  - Read shell.md for responsive breakpoints
  EMIT [concept-2-experience-4-storybook-1-setup] started run_id=<uuid>

STEP 2: Scaffold project and install dependencies
  $ mkdir -p _concept/2_experience/4_storybook
  - Copy templates/package.json to _concept/2_experience/4_storybook/package.json
  $ cd _concept/2_experience/4_storybook && pnpm install
  EMIT [concept-2-experience-4-storybook-1-setup] deps_installed

STEP 3: Copy static templates verbatim
  - Copy templates/.storybook/main.ts    -> .storybook/main.ts
  - Copy templates/.storybook/preview.ts -> .storybook/preview.ts
  - Copy templates/vite.config.ts        -> vite.config.ts
  - Copy templates/tsconfig.json         -> tsconfig.json
  These files are ready to use — no modifications needed.

STEP 4: Apply brand tokens to templated files
  - Copy templates/.storybook/theme.ts -> .storybook/theme.ts
    Replace {{PLACEHOLDER}} values:
      {{MODE}}               <- tokens.json mode ("light" or "dark")
      {{APP_NAME}}           <- brief.md app name
      {{colors.*}}           <- tokens.json color values
      {{fonts.*}}            <- tokens.json font families

  - Copy templates/src/styles/brand.css -> src/styles/brand.css
    Replace {{PLACEHOLDER}} values:
      {{fonts.*}}            <- tokens.json font families (Google Fonts URL + CSS vars)
      {{colors.*}}           <- tokens.json color values (light mode :root block)
      {{dark.colors.*}}      <- tokens.json dark mode overrides (.dark block)
      {{radius}}, {{shadows.*}}, {{spacing_base}} <- tokens.json
    IF tokens.json.mode is "light" only (no dark mode)
      - Remove the entire .dark { ... } block
    The static section below the "copy verbatim" marker needs NO changes.

STEP 5: Create empty src/@types directory
  $ mkdir -p _concept/2_experience/4_storybook/src/@types
  - Write src/@types/README.md:
    ```
    # Types
    Minimal TypeScript interfaces, built incrementally by each sub-skill.
    Only properties actually needed for rendering are defined here.
    After the data model is finalized (step 9), `concept-3-blueprint-4-storybook-types`
    runs `pxl types` to replace these mocked types with schema-generated types.
    ```
  - Types are NOT generated here — each sub-skill (components, pages, journeys)
    adds only the interfaces it needs to src/@types/ as it builds its layer.

STEP 6: Verify setup
  $ cd _concept/2_experience/4_storybook && pnpm run build
  IF build fails
    - Fix errors and retry
  EMIT [concept-2-experience-4-storybook-1-setup] completed run_id=<uuid>

MUST use the pinned package.json from templates/ — never manually install individual packages
MUST use templates as the starting point — never write config files from scratch
MUST replace ALL {{PLACEHOLDER}} values in templated files — none should remain
NEVER invent colors, fonts, or spacing — everything comes from tokens.json
NEVER modify the static CSS section in brand.css (below the "copy verbatim" marker)

CHECKLIST
  - [ ] package.json copied and pnpm install succeeded
  - [ ] Static templates copied verbatim (main.ts, preview.ts, vite.config.ts, tsconfig.json)
  - [ ] theme.ts has all {{PLACEHOLDER}} values replaced with brand tokens
  - [ ] brand.css has all {{PLACEHOLDER}} values replaced with brand tokens
  - [ ] brand.css .dark block removed if light-only mode
  - [ ] No {{PLACEHOLDER}} markers remain in any output file
  - [ ] src/@types/ directory created
  - [ ] Storybook builds without errors
