---
name: foundation
description: "Use after scaffold to apply brand tokens as CSS variables, configure authentication, and create the app shell layout. Reads the tech stack skill for stack-specific implementation recipes. Run before feature implementation."
keywords: [foundation, theming, brand, css-vars, auth, app-shell, layout, navigation]
user_inputs:
  dialog: []
  files: []
---

# Foundation — Brand, Auth, and App Shell

## Overview

The **foundation** skill applies the three foundational layers every app needs before
feature work begins:

1. **Brand tokens → CSS variables** — translate `tokens.json` into stack-appropriate
   CSS custom properties so every component inherits brand colors and fonts
2. **Authentication setup** — configure auth plugins, middleware, and config files
   using the tech-stack recipe
3. **App shell** — create the base layout, navigation, and sidebar/header components
   wired to the screen list

It is **framework-agnostic**. Implementation specifics (file locations, plugin names,
layout patterns) come from reading `tech-stack/<tech_stack_skill>/SKILL.md` at runtime.

**Phase:** implement / foundation
**Writes to:** project source code (stack-specific paths from tech-stack skill)

## When to Use

- The project has been scaffolded and `package.json` exists in the project root
- The user says "apply branding", "set up auth", "create app shell", "add navigation"
- After `implement/scaffold`, before `implement/feature`
- The orchestrator dispatches this as the second implementation phase

## When NOT to Use

- The project has not been scaffolded yet — run **scaffold** first
- `_concept/04_brand/tokens.json` does not exist — run **brand-visual** first
- `_concept/05_techstack/stack.md` does not exist — run **techstack** first
- The app shell already exists and only a feature is needed — use **feature** instead

## Prerequisites

### HARD-GATE

All three must be satisfied:

1. `package.json` exists in the project root (project is scaffolded)
2. `_concept/04_brand/tokens.json` exists
3. `_concept/05_techstack/stack.md` exists

If any are missing:

> "Foundation cannot run. Missing: [list]. Run [skill] first."

### Shared Contracts

Before starting, read:
- `cf__shared/iron_laws.md` — non-negotiable constraints (no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode
- `cf__shared/concept_structure.md` — valid _concept/ paths

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/05_techstack/stack.md` | Yes |
| **Must read** | `tech-stack/<tech_stack_skill>/SKILL.md` | Yes (resolved from stack.md) |
| **Must read** | `_concept/04_brand/tokens.json` | Yes |
| **Optional** | `_concept/05b_architecture/architecture.md` | No (for custom route/module structure) |
| **Optional** | `_concept/07_screens/**/*.md` | No (for navigation wiring) |
| **Never load** | `_concept/03_features/`, `_concept/06_datamodel/`, test files | — |

## Tech Stack Resolution

Read `_concept/05_techstack/stack.md` and extract `tech_stack_skill:`.

Then read `tech-stack/<tech_stack_skill>/SKILL.md` and extract:

| Section | Purpose |
|---------|---------|
| `css_vars_mapping:` | How `tokens.json` fields map to CSS custom properties for this stack (file location, variable naming, any framework-specific overrides like PrimeVue `--p-*` or Shoelace `--sl-*`) |
| `auth_setup:` | Config files to create, plugin names, middleware names, package deps for auth |
| `app_shell:` | Layout file location, navigation component patterns, sidebar/header conventions |

If any section is missing from the tech-stack SKILL.md, ask the user for guidance
before proceeding with that phase.

## 3-Phase Workflow

### Phase 1: Brand Tokens → CSS Variables

**Goal:** Create a single CSS/theme file that translates every `tokens.json` entry
into CSS custom properties so the rest of the app never has a hardcoded color or font.

1. Read `_concept/04_brand/tokens.json`
2. Read `css_vars_mapping:` from tech-stack SKILL.md for file location and
   variable naming convention
3. Write the theme file to the stack-specified location
4. If the stack uses a component library with its own CSS variable system
   (e.g., PrimeVue `--p-*`, Shoelace `--sl-*`), also write those overrides
   in the same theme file

**Rule:** ALL colors and fonts in the project must trace back to `tokens.json`.
No hardcoded hex values. No fallback brand colors invented by the agent.

**Emit:**
```
[foundation] checkpoint phase=brand
  theme_file: <path>
  tokens_applied: N
```

### Phase 2: Authentication Setup

**Goal:** Configure the stack's auth system using the recipe from `auth_setup:`.

1. Read `auth_setup:` from tech-stack SKILL.md
2. Create auth config file(s) at the stack-specified location
3. Create auth plugin/middleware files following the stack recipe
4. If the concept includes auth details (`_concept/03_features/` contains an auth
   feature spec), wire up the specific flows (login, logout, role checking)
5. If no auth details are available in the concept, create placeholder auth
   logic with clear `// TODO:` comments indicating what needs to be filled in

**Rule:** Never hardcode credentials. Never invent auth flows not present in
the concept. Use the tech-stack auth recipe as the structure; leave logic
placeholders if the concept does not specify.

**Emit:**
```
[foundation] checkpoint phase=auth
  auth_files: [list of created files]
  auth_method: <value from tech-stack skill>
```

### Phase 3: App Shell

**Goal:** Create the base layout, navigation component, and sidebar/header
wired to the screen list from `07_screens/`.

1. Read `app_shell:` from tech-stack SKILL.md for layout file conventions
2. Read `_concept/07_screens/**/*.md` (screen list and navigation metadata)
3. Read `_concept/05b_architecture/architecture.md` if it exists (custom
   routing or module structure may override defaults)
4. Create the base layout file at the stack-specified location
5. Create navigation component(s) with links to every screen in `07_screens/`
6. Wire navigation: active state, icons (from screen specs), mobile collapse

**Rule:** Use only layout patterns native to the chosen tech stack (from
`app_shell:` in the tech-stack skill). Do not create non-standard layout
structures or invent custom routing patterns.

**Emit:**
```
[foundation] checkpoint phase=shell
  layout_file: <path>
  nav_component: <path>
  screens_wired: N
```

### Emit Started/Completed

```
[foundation] started
  run_id: <uuid>
  reads: 05_techstack/stack.md, 04_brand/tokens.json
  tech_stack_skill: <resolved value>

[foundation] completed
  run_id: <uuid>
  phases: [brand, auth, shell]
  theme_file: <path>
  auth_files: [list]
  layout_file: <path>
```

## Outputs

All output paths are determined by the tech-stack SKILL.md, not hardcoded here.

| Output | Source of path |
|--------|---------------|
| Theme / CSS variables file | `css_vars_mapping:` in tech-stack SKILL.md |
| Auth config file(s) | `auth_setup:` in tech-stack SKILL.md |
| Auth plugin/middleware file(s) | `auth_setup:` in tech-stack SKILL.md |
| Base layout component | `app_shell:` in tech-stack SKILL.md |
| Navigation component | `app_shell:` in tech-stack SKILL.md |

## Completion Summary

Present to user: files produced (theme file, auth files, layout/nav files), key
decisions made (CSS var naming scheme, auth method, layout pattern), suggested next
steps (run `implement/feature` for each feature, or `implement/design` for high-fidelity
mockups before feature work).

## FORBIDDEN

- **Inventing CSS values** not present in `tokens.json` — every color and font must
  trace to brand tokens
- **Hardcoding auth credentials** of any kind
- **Creating non-standard layout patterns** for the stack — follow the tech-stack skill's
  `app_shell:` recipe exactly
- **Overwriting existing files** without showing a diff and getting user approval
- **Starting without reading tech-stack SKILL.md** — the implementation recipes live there

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|--------------------|
| Using hardcoded hex colors in CSS | Agent writes colors from memory | Read `tokens.json` first; map every variable using `css_vars_mapping:` from tech-stack skill |
| Inventing PrimeVue CSS var names | Agent guesses `--p-*` variable names | Read them from `css_vars_mapping:` in tech-stack nuxt-primevue SKILL.md |
| Creating non-standard auth patterns | Agent uses a generic auth pattern | Read `auth_setup:` from tech-stack skill; follow the recipe for this specific stack |
| Hardcoding nav items | Agent lists screens from memory | Read `07_screens/` directory structure for the actual screen list |
| Skipping placeholder TODOs in auth | Agent leaves auth empty or invents logic | Add clear `// TODO:` comments where concept details are missing |
| Running before scaffold | Agent jumps to foundation without a project | Check for `package.json` in project root first |

## Integration

- **Called by:** orchestrator or standalone (second implementation phase)
- **Reads from:** `_concept/05_techstack/stack.md`, `tech-stack/<id>/SKILL.md`, `_concept/04_brand/tokens.json`, optionally `_concept/07_screens/` and `_concept/05b_architecture/`
- **Feeds into:** `implement/feature` (features are built on top of the app shell), `implement/design` (design mockups use the real theme)
- **Feedback loops:** None inbound. Outbound: theme file and layout are consumed by all feature implementations.

## Standalone Mode

This skill can be invoked directly without the orchestrator.
**Gate check:** `package.json` in project root, `04_brand/tokens.json`, `05_techstack/stack.md`
**If gates fail:** Run `scaffold`, `brand-visual`, or `techstack` as needed.
**On completion:** Present summary, then suggest running `implement/feature`.
