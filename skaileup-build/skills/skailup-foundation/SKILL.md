---
name: "skailup-foundation"
description: "Applies the three foundational layers every app needs before feature work: brand tokens → CSS variables, authentication setup, and app shell layout. Also seeds initial data and configures Storybook with brand theme if present. Reads the tech stack profile for stack-specific recipes. Run after scaffold, before implement-feature."
metadata:
  version: "1.0.0"
  tags:
    - "foundation"
    - "theming"
    - "brand"
    - "css-vars"
    - "auth"
    - "app-shell"
    - "layout"
    - "navigation"
    - "seed"
    - "storybook"
  source: "MERGED"
  prerequisites:
    files:
      - path: "package.json"
        gate: hard
        description: "Project must be scaffolded before applying foundation layers"
      - path: "_concept/1_discovery/2_brand/tokens.json"
        gate: hard
        description: "Brand tokens required for CSS variable generation"
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        gate: hard
        description: "Tech stack required to select foundation recipes (auth, layout, theme)"
    reads:
      - path: "_concept/1_discovery/2_brand/identity.md"
        description: "Brand identity for theme customization"
      - path: "_concept/2_experience/3_screens/00_layout/shell.md"
        description: "App shell structure for navigation and layout implementation"
      - path: "_concept/3_blueprint/2_architecture/architecture.md"
        description: "Architecture for auth provider and session configuration"
      - path: "_concept/3_blueprint/3_datamodel/seed.json"
        description: "Seed data for initial database population"
    produces:
      - path: "_implementation/progress.json"
        description: "Foundation layer completion status"
---

# Foundation — Brand, Auth, App Shell, and Seed

## Overview

Applies the foundational layers every app needs before feature work begins:

1. **Brand tokens → CSS variables** — translate `tokens.json` into stack-appropriate
   CSS custom properties so every component inherits brand colors and fonts
2. **Authentication setup** — configure auth plugins, middleware, and config files
   using the tech-stack recipe
3. **App shell** — create the base layout, navigation, and sidebar/header components
   wired to the screen list
4. **Seed data** — wire up `seed.json` scenarios for development and testing
5. **Storybook brand config** — configure Storybook theme decorator with brand tokens
   (only if `2_experience/4_storybook/` exists)

**Framework-agnostic.** All file locations, variable naming, auth plugin names,
and layout patterns come from `skaileup-standards/profiles/<tech_stack_skill>/SKILL.md`.

## When to Use

- Project has been scaffolded (`package.json` exists in project root)
- User says "apply branding", "set up auth", "create app shell", "add navigation"
- After `scaffold`, before `implement-feature`

## When NOT to Use

- Project not scaffolded yet — run `scaffold` first
- `_concept/1_discovery/2_brand/tokens.json` missing — run `brand-visual` first
- `_concept/3_blueprint/1_techstack/stack.md` missing — run `techstack` first
- App shell already exists and only a feature is needed — use `implement-feature`

## Prerequisites

**Hard gates (all must exist):**
1. `package.json` in the project root (project is scaffolded)
2. `_concept/1_discovery/2_brand/tokens.json`
3. `_concept/3_blueprint/1_techstack/stack.md`

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/3_blueprint/1_techstack/stack.md` | Yes |
| Must read | `skaileup-standards/profiles/<tech_stack_skill>/SKILL.md` | Yes |
| Must read | `_concept/1_discovery/2_brand/tokens.json` | Yes |
| Read if exists | `_concept/1_discovery/2_brand/identity.md` | Recommended |
| Read if exists | `_concept/2_experience/3_screens/00_layout/shell.md` | Recommended |
| Read if exists | `_concept/3_blueprint/2_architecture/architecture.md` | Optional |
| Read if exists | `_concept/3_blueprint/3_datamodel/seed.json` | Optional |
| Never load | Features, model.json, test files | — |

---

ROLE  Foundation agent — applies brand tokens, configures auth, wires app shell, seeds data, and configures Storybook.

READS
  _concept/3_blueprint/1_techstack/stack.md           — tech_stack_skill, auth provider
  skaileup-standards/profiles/<tech_stack_skill>/SKILL.md  — css_vars_mapping, auth_setup, app_shell recipes
  _concept/1_discovery/2_brand/tokens.json            — color tokens, typography, spacing, shadows
  ? _concept/1_discovery/2_brand/identity.md          — design philosophy, atmosphere
  ? _concept/2_experience/3_screens/00_layout/shell.md — navigation structure, sidebar items
  ? _concept/3_blueprint/2_architecture/architecture.md — custom routing or module structure
  ? _concept/3_blueprint/3_datamodel/seed.json        — seed data scenarios

WRITES
  <stack-specific theme file>                          — brand tokens as CSS custom properties
  <stack-specific auth files>                          — auth configuration
  <stack-specific layout file>                         — app shell component
  <stack-specific nav component>                       — navigation component
  <stack-specific seed file>                           — seed data configured from seed.json
  _implementation/progress.json                        — foundation phase status

REFERENCES
  skaileup-shared/contracts/concept_structure.md  — canonical _concept/ paths
  skaileup-shared/contracts/iron_laws.md          — non-negotiable constraints

MUST  read tech stack profile before any implementation
MUST  translate ALL colors and fonts from tokens.json — never invent values
MUST  implement both light and dark modes if tokens specify both
MUST  derive navigation items from shell.md or 3_screens/ — never hardcode
MUST  commit once per sub-phase (tokens, auth, shell, seed, storybook)
NEVER invent CSS values not present in tokens.json
NEVER hardcode auth credentials
NEVER create non-standard layout patterns — follow the tech-stack profile
NEVER overwrite existing files without showing a diff and getting approval
NEVER modify _concept/ files

EMIT [foundation] started run_id=<uuid> tech_stack_skill=<value>

# ── Phase 1: Brand Tokens → CSS Variables ──────────────────────────

STEP 1: Apply brand tokens
  - Read tokens.json: all color tokens (light + dark), font families, spacing, border radii, shadows
  - Read identity.md (if exists): atmosphere, design philosophy
  - Read css_vars_mapping from tech stack profile:
    - Theme file location
    - CSS variable naming convention
    - Component library overrides (e.g., PrimeVue --p-*, Shoelace --sl-*, etc.)
  - Write the theme file at the profile-specified location
  - Apply light + dark mode blocks if tokens specify both
  - Set up font imports (Google Fonts or other provider)
  - Apply atmosphere effects (background gradients, glow) if specified in identity.md
    $ <build command>
    EMIT [foundation] checkpoint phase=brand_tokens theme_file=<path>
    Commit: `foundation: apply brand tokens as CSS custom properties`

  RULE: ALL colors and fonts must trace to tokens.json. No hardcoded hex values.

# ── Phase 2: Authentication Setup ────────────────────────────────

STEP 2: Configure authentication
  - Read auth_setup from tech stack profile:
    - Config files to create
    - Plugin/middleware names
    - Package dependencies
    - Role mapping conventions
  - Create auth config file(s) at the profile-specified location
  - Create auth plugin/middleware files following the profile recipe
  - Read features/ for auth feature spec (if exists) to wire specific flows
  - If no auth spec: create placeholder auth logic with clear `// TODO:` comments
    IF concept specifies SSO, social login, or custom roles
    - Configure per the profile's auth extension points
    Commit: `foundation: configure authentication` (if changes needed)

  RULE: Never hardcode credentials. Never invent auth flows not in the concept.

# ── Phase 3: App Shell ────────────────────────────────────────────

STEP 3: Create app shell
  - Read app_shell from tech stack profile: layout file location, nav patterns
  - Read shell.md (if exists): navigation structure, sidebar items, header components
  - If no shell.md: derive navigation from `2_experience/3_screens/` directory structure
  - Read architecture.md (if exists): custom routing or module structure
  - Create base layout file at profile-specified location
  - Create navigation component with links to every screen in 3_screens/
  - Wire: active state, icons (from screen specs), mobile collapse behavior
    EMIT [foundation] checkpoint phase=shell layout_file=<path> screens_wired=<N>
    Commit: `foundation: create app shell with navigation`

  RULE: Navigation must list every screen from screen specs. No hardcoded nav items.

# ── Phase 4: Seed Data ────────────────────────────────────────────

STEP 4: Configure seed data
  IF seed.json exists
    - Extract the `populated` scenario
    - Transform to stack-specific seed format (per profile seed_format if defined)
    - Map concept field names to stack conventions (snake_case → camelCase if needed)
    - Assign explicit short IDs (e.g., `usr-admin`, `proj-1`) for deterministic seeds
    - Resolve cross-references (foreign keys) using the assigned IDs
    - Write to the stack-appropriate seed file location
    - Run seed to verify it loads correctly
    Commit: `foundation: configure seed data from concept populated scenario`

# ── Phase 5: Storybook Brand Config ──────────────────────────────

STEP 5: Configure Storybook (if exists)
  IF _concept/2_experience/4_storybook/ exists AND Storybook is installed
    - Configure Storybook theme with brand tokens (background, fonts, colors)
    - Create theme decorator wrapping all stories with brand CSS variables
    - Set up viewport presets from shell spec's responsive breakpoints
    Commit: `foundation: configure Storybook with brand theme`

# ── Phase 6: Verify and Complete ─────────────────────────────────

STEP 6: Verify and checkpoint
  - Run build command (from profile)
  IF browser skill is available
    - Navigate to app
    - Verify: login page loads, app shell renders, nav links work, theme applied
    - Take screenshots → _implementation/verification/screenshots/foundation/
  - Update _implementation/progress.json: foundation phase → approved
  - Update _implementation/PLANS.md: check off foundation tasks
  EMIT [foundation] completed run_id=<uuid> phases=[brand,auth,shell,seed,storybook]

CHECKPOINT foundation_approval
  > "Your app now has your brand's look and feel — colors, fonts, and layout are applied.
  > [Show screenshots if available]
  >
  > Approve, or tell me what to change."

CHECKLIST
  - [ ] Brand tokens applied (CSS variables, light + dark if applicable)
  - [ ] Fonts imported and referenced in CSS custom properties
  - [ ] Auth configuration complete (or placeholder with TODOs)
  - [ ] Navigation wired from shell spec / screen directory
  - [ ] Seed data loaded from populated scenario (if seed.json exists)
  - [ ] Storybook brand theme configured (if storybook installed)
  - [ ] Build passes
  - [ ] Visual verification screenshots saved (if browser available)

---

## Tech Stack Resolution

Read `stack.md` → extract `tech_stack_skill`. Then read
`skaileup-standards/profiles/<tech_stack_skill>/SKILL.md` and extract:

| Section | Purpose |
|---------|---------|
| `css_vars_mapping` | How to translate tokens.json fields to CSS custom properties |
| `auth_setup` | Config files, plugin names, middleware, package deps for auth |
| `app_shell` | Layout file location, nav component patterns |
| `seed_format` | How to write seed data for this stack |

If any section is missing from the profile, ask the user for guidance.

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Hardcoded hex colors | Read tokens.json; use css_vars_mapping from profile |
| Guessing CSS var names | Read css_vars_mapping from the tech-stack profile |
| Non-standard auth patterns | Follow auth_setup from the profile exactly |
| Hardcoded nav items | Derive from shell.md or 3_screens/ directory structure |
| Skipping seed data | seed.json populated scenario sets up realistic dev data |

## Integration

- **Called by:** `implement` orchestrator or standalone
- **Reads:** `_concept/3_blueprint/1_techstack/`, `_concept/1_discovery/2_brand/`, `_concept/2_experience/3_screens/`
- **Feeds into:** `implement-feature` (features build on top of the app shell)
