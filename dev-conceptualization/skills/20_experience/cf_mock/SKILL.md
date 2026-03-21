---
name: mock
description: "Use when screen specs are approved and user wants interactive HTML mockups. Also when user says 'mockup', 'prototype', 'show me what it looks like'. Generates a linked multi-page prototype. Supports 3 stacks: Alpine.js+Shoelace, Vue 3+PrimeVue, Preact+HTM. Auto-selects stack template from tech-stack skill if stack.md exists."
keywords: [ui, ux, design, frontend, mockups, prototype, linked, alpine, shoelace, primevue, vue3, preact, htm]
user_inputs:
  dialog: []
  files: []
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

# Concept Mock — Linked Multi-Page Prototype

## Overview

The **concept mock** skill generates a **linked, navigable prototype** — not isolated
screen mockups. Every page shares a common shell layout, brand styling, and
working navigation. The result is a mini-website the user can click through
in a browser to validate flows end-to-end.

**Phase:** tools / mockup
**Writes to:** `_concept/05_mockups/`

## Technology Stacks

Three stacks available, each CDN-only with zero build step:

| Stack | ID | Best For |
|-------|----|----------|
| Alpine.js + Shoelace | `alpine_shoelace` | Lightweight prototypes, web-component UI primitives |
| Vue 3 + PrimeVue | `vue_primevue` | Data-heavy apps, rich DataTable/Dialog/Form components |
| Preact + HTM | `preact_htm` | Modern ES modules, signal-based reactivity, minimal overhead |

All stacks are **CDN-only, zero build**. Every HTML file opens directly in a browser.

### Auto-Selection from Tech Stack

If `_concept/05_techstack/stack.md` exists and contains `tech_stack_skill:`, the mock
skill reads that tech-stack SKILL.md and uses its `mock_template:` field to
automatically select the template. This avoids the user having to choose manually.

**Stack → Default Mock Template:**

| tech_stack_skill | mock_template | Notes |
|-----------------|---------------|-------|
| `tech-stack/nuxt-primevue` | `vue_primevue` | Direct match |
| `tech-stack/nuxt-ui` | `vue_primevue` | Closest CDN match for Reka UI |
| `tech-stack/nextjs-radix` | `preact_htm` | React ecosystem → Preact |
| `tech-stack/nextjs-shadcn` | `preact_htm` | React ecosystem → Preact |
| `tech-stack/nuxt-minimal` | `alpine_shoelace` | Minimal stack → Alpine |
| `tech-stack/postxl` | `preact_htm` | PostXL uses React conventions |
| custom / unknown | — | Ask user to choose |

If no `tech_stack_skill:` is set (standalone mode without techstack, or custom stack),
present the three options and ask the user to choose before proceeding.

## When to Use

- Screen specs are approved and the user wants interactive HTML mockups
- The user says "mockup", "prototype", "show me what it looks like"
- The orchestrator dispatches this after screens are complete
- Route is `prototype` — this skill produces the primary deliverable

## When NOT to Use

- No screen specs exist yet — run **cf_concept_ui_screens** first
- No brand tokens exist — run **cf_concept_brand_visual** first
- The user wants to implement production code — use **implement** instead

## Prerequisites

### HARD-GATE

All three must be satisfied:
1. `_concept/07_screens/` has at least one screen file
2. `_concept/04_brand/tokens.json` exists
3. `_concept/06_datamodel/seed.json` exists

If any are missing, stop and name what is needed.

### Optional: Tech Stack for Template Auto-Selection

If `_concept/05_techstack/stack.md` exists:
- Read it and extract `tech_stack_skill:`
- Read `tech-stack/<tech_stack_skill>/SKILL.md` and extract `mock_template:`
- Use the mapped template from the "Stack → Default Mock Template" table above
- Skip the manual template selection step

If `05_techstack/stack.md` does not exist, ask the user which template to use.

### Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid paths
- `cf__shared/frontmatter.md` — screen frontmatter fields
- `cf__shared/seed_data.md` — scenario convention
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/04_brand/tokens.json` | Yes |
| **Must read** | `_concept/07_screens/**/*.md` | Yes |
| **Must read** | `_concept/07_screens/00_layout/shell.md` | Yes |
| **Must read** | `_concept/06_datamodel/seed.json` | Yes |
| **Optional** | `_concept/05_techstack/stack.md` | No (for template auto-selection) |
| **Optional** | `tech-stack/<tech_stack_skill>/SKILL.md` | No (read if stack.md has tech_stack_skill) |
| **Optional** | `_concept/07_screens/components/*.md` | No |
| **Optional** | `_concept/_research/general/design_inspiration.md` | No |
| **Never load** | `_concept/01_project/`, `_concept/03_features/`, source code | — |

## Standalone Mode

This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/07_screens/` must have at least one screen, `_concept/04_brand/tokens.json` must exist, `_concept/06_datamodel/seed.json` must exist
**If gates fail:** Run cf_concept_ui_screens (for screens), cf_concept_brand_visual (for tokens), cf_concept_datamodel (for seed data)
**On completion:** Present summary, then suggest next steps (user review, cf_test_e2e for visual verification).

## Output Structure

```
_concept/05_mockups/
├── index.html                    ← entry point / landing or first screen
├── cf__shared/
│   ├── styles.css                ← brand CSS variables + stack-specific overrides
│   ├── layout.js / app-shell.js  ← shared shell component (stack-specific)
│   ├── seed.js                   ← seed.json exported as JS object
│   ├── router.js                 ← lightweight page-switching (optional)
│   └── primevue-setup.js         ← PrimeVue only: plugin registration
├── screens/
│   ├── dashboard.html            ← one HTML per screen (uses shared shell)
│   ├── login.html
│   ├── settings.html
│   └── ...
└── README.md                     ← how to open, what to test
```

## Workflow

### Phase 0: Resolve Template and Read Stack Templates

**Before generating any code**, determine which mock template to use:

1. **Auto-select (preferred):** If `_concept/05_techstack/stack.md` exists:
   - Read it, extract `tech_stack_skill:`
   - Read `tech-stack/<tech_stack_skill>/SKILL.md`, extract `mock_template:`
   - Use the template from the "Stack → Default Mock Template" table
   - No user prompt needed
2. **Manual select (fallback):** If no `tech_stack_skill:` is resolvable:
   - Present the three options (`alpine_shoelace`, `vue_primevue`, `preact_htm`)
   - Ask user to choose before proceeding

Once the template ID (`mockup_style`) is resolved:

3. Read `templates/{mockup_style}/*.md` — all template files for the chosen stack
4. Read `templates/layouts/*.md` — layout templates (dashboard, landing page)

These templates define the exact CDN resources, component patterns, and boilerplate
to use. Follow them precisely.

### Phase 1: Brand Foundation (`cf__shared/styles.css`)

From `tokens.json`, generate CSS custom properties following the pattern in
`templates/{mockup_style}/styles.md`:

- Brand color variables (`--brand-*`)
- Stack-specific overrides (Shoelace `--sl-*`, PrimeVue `--p-*`, or plain CSS)
- Typography variables (`--font-heading`, `--font-body`)
- Tailwind config extending with brand variables
- Spacing on 8pt grid

**Do NOT invent colors, fonts, or spacing.** Everything from brand tokens.

### Phase 2: Shared Shell Layout

Read `07_screens/00_layout/shell.md` and build the shell component following
`templates/{mockup_style}/shared-shell.md`:

- **Sidebar/nav** with links to every screen (icons from screen specs)
- **Header** with app name, breadcrumb, user avatar placeholder
- **Main content area** where screen content renders
- **Mobile responsive**: sidebar collapses at `lg:` breakpoint

### Phase 3: Seed Data (`cf__shared/seed.js`)

Convert `seed.json` scenarios to JS following `templates/{mockup_style}/seed-setup.md`:

- Export `populated`, `empty`, `edge_cases` scenarios
- Add a scenario switcher (floating UI element) so reviewers can toggle views
- Use stack-appropriate reactivity (Alpine events, Vue refs, Preact signals)

### Phase 4: Screen Pages

For each screen in `07_screens/`, generate an HTML file following
`templates/{mockup_style}/page-boilerplate.md`:

Each screen page:
- **Includes the shared shell** (sidebar, header) with current page highlighted
- **Links to other screens** via real `<a href>` tags (working navigation!)
- **Renders seed data** with stack-appropriate binding
- **Handles states**: empty, populated, edge_cases (toggle via scenario switcher)
- **Interactive elements**: modals, tabs, dropdowns using stack components
- **Responsive**: mobile-first with Tailwind breakpoints

### Phase 5: Index & Navigation

`index.html` serves as the entry point:
- Redirects to the first meaningful screen (e.g., dashboard or login)
- OR acts as a screen selector showing all available screens

### Phase 6: Polish & Verification

- **Cross-page navigation**: Click every nav link — all must resolve
- **Brand consistency**: No default framework colors leaking — everything uses `brand-*`
- **Responsive**: Check at 375px, 768px, 1280px
- **Empty states**: Toggle to empty scenario — verify graceful messaging
- **Contrast**: All text passes WCAG AA (4.5:1 ratio)
- **Interactions**: Hover, focus-visible, disabled states on all interactive elements
- **Micro-animations**: `transition-all duration-200 ease-in-out` on state changes

### Emit Events

```
[cf_concept_mock] started
  run_id: <uuid>
  reads: 04_brand/tokens.json, 07_screens/, 06_datamodel/seed.json
  mode: linked_prototype
  stack: <mockup_style>

[cf_concept_mock] checkpoint phase=shell_complete
  screens: N
  shared_files: [list of cf__shared/ files]

[cf_concept_mock] completed
  run_id: <uuid>
  mockups_generated: N
  brand_tokens_source: _concept/04_brand/tokens.json
  navigation_links: N (cross-page links verified)
  stack: <mockup_style>
```

## Outputs

| File | Description |
|------|-------------|
| `_concept/05_mockups/index.html` | Entry point / screen selector |
| `_concept/05_mockups/cf__shared/styles.css` | Brand CSS variables + stack overrides |
| `_concept/05_mockups/cf__shared/layout.js` or `app-shell.js` | Shared shell component |
| `_concept/05_mockups/cf__shared/seed.js` | Seed data as JS for all screens |
| `_concept/05_mockups/screens/*.html` | One HTML per screen, all linked |
| `_concept/05_mockups/README.md` | How to open, what to test |

## Completion Summary

Present to user: files produced (index.html, shared styles/layout/seed, screen HTML files), key decisions made (stack choice, shell layout, component mapping, scenario rendering), suggested next steps (which skills are now unblocked — e.g., cf_test_e2e for visual verification, or user review and iteration on mockups).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Isolated HTML files with no navigation | Agent generates one file per screen with no links | Every page includes the shared shell with working `<a href>` links to all other pages. |
| Colors not from tokens.json | Agent picks "nice" colors | Every color must trace back to tokens.json via CSS variables. |
| Inlining everything per page | Agent copies styles/scripts into each HTML | Use shared `cf__shared/` files — single source of truth for styles, layout, seed data. |
| Static mockups without interactivity | Agent generates plain HTML | Use stack-appropriate reactivity for all dynamic elements. |
| Missing mobile layout | Agent only designs for desktop | Mobile-first. Sidebar collapses at `lg:` breakpoint. |
| No empty state design | Agent only shows populated view | Render empty scenario from seed.js with helpful onboarding messages. |
| Broken cross-page links | Agent uses wrong relative paths | Verify every link resolves. All screen pages are in `screens/`, shared files in `cf__shared/`. |
| Not reading templates first | Agent writes code without consulting stack templates | Phase 0 is mandatory — read all templates before generating any code. |
| Default framework styling visible | Agent forgets to override CSS variables | Override ALL framework CSS variables to match brand tokens. |

## Integration

- **Called by:** orchestrator or standalone
- **Primary deliverable for:** `prototype` route
- **Reads from:** `04_brand/tokens.json`, `07_screens/`, `06_datamodel/seed.json`
- **Feeds into:** user review, `cf_test_e2e` (visual verification)
- **Feedback loops:** Updates screen spec status to `mockup_ready`

## Strict Constraints

- **FORBIDDEN:** Colors not from `tokens.json` — use CSS variables only
- **FORBIDDEN:** Isolated pages without shared navigation
- **FORBIDDEN:** Static mockups — all interactive elements need stack-appropriate reactivity
- **FORBIDDEN:** Ignoring mobile viewports
- **FORBIDDEN:** Magic numbers in CSS — use Tailwind utilities + 8pt grid
- **FORBIDDEN:** Default framework styling without brand override
- **FORBIDDEN:** Generating code without reading stack templates first
- **REQUIRED:** Shared shell layout across all pages
- **REQUIRED:** Working `<a href>` links between all screens
- **REQUIRED:** Brand fonts from Google Fonts via CDN
- **REQUIRED:** Iconify for all icons
- **REQUIRED:** Scenario switcher for populated/empty/edge_cases
- **REQUIRED:** CDN-only dependencies — no build step, no npm
