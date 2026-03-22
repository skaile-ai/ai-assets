---
name: "mock"
description: "Use when screen specs are approved and user wants interactive HTML mockups. Also when user says 'mockup', 'prototype', 'show me what it looks like'. Generates a linked multi-page prototype. Supports 3 stacks: Alpine.js+Shoelace, Vue 3+PrimeVue, Preact+HTM. Auto-selects stack template from tech-stack profile if stack.md exists."
metadata:
  version: "1.0.0"
  tags:
    - "ui"
    - "ux"
    - "design"
    - "frontend"
    - "mockups"
    - "prototype"
    - "linked"
    - "alpine"
    - "shoelace"
    - "primevue"
    - "vue3"
    - "preact"
    - "htm"
  source: "MIGRATED"
  prerequisites:
    files:
      - path: "_concept/2_experience/3_screens"
        gate: hard
        description: "Screen specs must exist — mockup renders screen specs as HTML"
        min_entries: 1
      - path: "_concept/1_discovery/2_brand/tokens.json"
        gate: hard
        description: "Design tokens required for brand-accurate styling"
      - path: "_concept/3_blueprint/3_datamodel/seed.json"
        gate: hard
        description: "Seed data required for realistic mock content"
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        gate: soft
        description: "Tech stack used to auto-select mockup stack template"
    reads:
      - path: "_concept/1_discovery/1_overview/brief.md"
        description: "App name and navigation structure"
      - path: "_concept/2_experience/2_features"
        description: "Feature list for page inventory"
    produces:
      - path: "_concept/mockups"
        description: "Linked multi-page HTML prototype files"
---

# Concept Mock — Linked Multi-Page Prototype

## Overview

The **mock** skill generates a **linked, navigable prototype** — not isolated
screen mockups. Every page shares a common shell layout, brand styling, and
working navigation. The result is a mini-website the user can click through
in a browser to validate flows end-to-end.

**Writes to:** `_concept/mockups/`

## Technology Stacks

Three stacks available, each CDN-only with zero build step:

| Stack | ID | Best For |
|-------|----|----------|
| Alpine.js + Shoelace | `alpine_shoelace` | Lightweight prototypes, web-component UI primitives |
| Vue 3 + PrimeVue | `vue_primevue` | Data-heavy apps, rich DataTable/Dialog/Form components |
| Preact + HTM | `preact_htm` | Modern ES modules, signal-based reactivity, minimal overhead |

All stacks are **CDN-only, zero build**. Every HTML file opens directly in a browser.

### Auto-Selection from Tech Stack

If `_concept/3_blueprint/1_techstack/stack.md` exists and contains `tech_stack_skill:`,
the mock skill reads that profile SKILL.md and uses its `mock_template:` field to
automatically select the template. This avoids the user having to choose manually.

**Stack → Default Mock Template:**

| tech_stack_skill | mock_template | Notes |
|-----------------|---------------|-------|
| `nuxt-primevue` | `vue_primevue` | Direct match |
| `nuxt-ui` | `vue_primevue` | Closest CDN match for Reka UI |
| `nextjs-radix` | `preact_htm` | React ecosystem → Preact |
| `nextjs-shadcn` | `preact_htm` | React ecosystem → Preact |
| `nuxt-minimal` | `alpine_shoelace` | Minimal stack → Alpine |
| `postxl` | `preact_htm` | PostXL uses React conventions |
| custom / unknown | — | Ask user to choose |

If no `tech_stack_skill:` is set, present the three options and ask the user to choose
before proceeding.

## When to Use

- Screen specs are approved and the user wants interactive HTML mockups
- The user says "mockup", "prototype", "show me what it looks like"
- The orchestrator dispatches this after screens are complete

## When NOT to Use

- No screen specs exist yet — run `screens` first
- No brand tokens exist — run `brand-visual` first
- The user wants to implement production code — use `implement-feature` instead

## Prerequisites

**Hard gates:**
1. `_concept/2_experience/3_screens/` has at least one screen file
2. `_concept/1_discovery/2_brand/tokens.json` exists
3. `_concept/3_blueprint/3_datamodel/seed.json` exists

If any are missing, stop and name what is needed.

**Optional: Tech Stack for Template Auto-Selection**

If `_concept/3_blueprint/1_techstack/stack.md` exists:
- Read it and extract `tech_stack_skill:`
- Read `dev-standards/profiles/<tech_stack_skill>/SKILL.md` and extract `mock_template:`
- Use the mapped template from the table above
- Skip the manual template selection step

## Shared Contracts

Before starting, read:
- `dev-shared/contracts/concept_structure.md` — valid paths
- `dev-shared/contracts/frontmatter.md` — screen frontmatter fields
- `dev-shared/contracts/seed_data.md` — scenario convention
- `dev-shared/contracts/iron_laws.md` — non-negotiable constraints
- `dev-shared/contracts/agent_patterns.md` — communication style, standalone mode

## Context Budget

| Source | Priority |
|--------|----------|
| `_concept/1_discovery/2_brand/tokens.json` | Required |
| `_concept/2_experience/3_screens/**/*.md` | Required |
| `_concept/2_experience/3_screens/00_layout/shell.md` | Required |
| `_concept/3_blueprint/3_datamodel/seed.json` | Required |
| `_concept/3_blueprint/1_techstack/stack.md` | Optional (template auto-selection) |
| `dev-standards/profiles/<tech_stack_skill>/SKILL.md` | Optional (read if stack.md has tech_stack_skill) |
| `_concept/2_experience/3_screens/components/*.md` | Optional |
| `_grounding/general/design_inspiration.md` | Optional |

**Never load:** `_concept/1_discovery/`, `_concept/2_experience/2_features/`, source code.

## Output Structure

```
_concept/mockups/
├── index.html                    ← entry point / landing or first screen
├── shared/
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

1. **Auto-select (preferred):** If `_concept/3_blueprint/1_techstack/stack.md` exists:
   - Read it, extract `tech_stack_skill:`
   - Read `dev-standards/profiles/<tech_stack_skill>/SKILL.md`, extract `mock_template:`
   - Use the template from the mapping table above
   - No user prompt needed
2. **Manual select (fallback):** If no `tech_stack_skill:` is resolvable:
   - Present the three options (`alpine_shoelace`, `vue_primevue`, `preact_htm`)
   - Ask user to choose before proceeding

Once the template ID is resolved:

3. Read `templates/{mockup_style}/*.md` — all template files for the chosen stack
4. Read `templates/layouts/*.md` — layout templates (dashboard, landing page)

These templates define the exact CDN resources, component patterns, and boilerplate
to use. Follow them precisely.

### Phase 1: Brand Foundation (`shared/styles.css`)

From `tokens.json`, generate CSS custom properties following the pattern in
`templates/{mockup_style}/styles.md`:

- Brand color variables (`--brand-*`)
- Stack-specific overrides (Shoelace `--sl-*`, PrimeVue `--p-*`, or plain CSS)
- Typography variables (`--font-heading`, `--font-body`)
- Spacing on 8pt grid

**Do NOT invent colors, fonts, or spacing.** Everything from brand tokens.

### Phase 2: Shared Shell Layout

Read `2_experience/3_screens/00_layout/shell.md` and build the shell component following
`templates/{mockup_style}/shared-shell.md`:

- **Sidebar/nav** with links to every screen (icons from screen specs)
- **Header** with app name, breadcrumb, user avatar placeholder
- **Main content area** where screen content renders
- **Mobile responsive**: sidebar collapses at `lg:` breakpoint

### Phase 3: Seed Data (`shared/seed.js`)

Convert `seed.json` scenarios to JS:

- Export `populated`, `empty`, `edge_cases` scenarios
- Add a scenario switcher (floating UI element) so reviewers can toggle views
- Use stack-appropriate reactivity (Alpine events, Vue refs, Preact signals)

### Phase 4: Screen Pages

For each screen in `2_experience/3_screens/`, generate an HTML file. Each screen page:
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

## Outputs

| File | Description |
|------|-------------|
| `_concept/mockups/index.html` | Entry point / screen selector |
| `_concept/mockups/shared/styles.css` | Brand CSS variables + stack overrides |
| `_concept/mockups/shared/layout.js` | Shared shell component |
| `_concept/mockups/shared/seed.js` | Seed data as JS for all screens |
| `_concept/mockups/screens/*.html` | One HTML per screen, all linked |
| `_concept/mockups/README.md` | How to open, what to test |

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Isolated HTML files with no navigation | Every page includes the shared shell with working `<a href>` links to all other pages |
| Colors not from tokens.json | Every color must trace back to tokens.json via CSS variables |
| Inlining everything per page | Use shared `shared/` files — single source of truth for styles, layout, seed data |
| Static mockups without interactivity | Use stack-appropriate reactivity for all dynamic elements |
| Missing mobile layout | Mobile-first. Sidebar collapses at `lg:` breakpoint |
| No empty state design | Render empty scenario from seed.js with helpful onboarding messages |
| Broken cross-page links | Verify every link resolves. All screen pages are in `screens/`, shared files in `shared/` |
| Not reading templates first | Phase 0 is mandatory — read all templates before generating any code |
| Default framework styling visible | Override ALL framework CSS variables to match brand tokens |

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

EMIT  [mock] started run_id=<uuid> stack=<mockup_style>
EMIT  [mock] checkpoint phase=shell_complete screens=<N>
EMIT  [mock] completed run_id=<uuid> mockups_generated=<N> stack=<mockup_style> navigation_links=<N>
