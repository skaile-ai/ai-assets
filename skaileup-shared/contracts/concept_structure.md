# _concept/ Directory Structure

All skills read and write to a `_concept/` folder inside the target project.
This is the canonical structure. Skills must use these exact paths.

```
_concept/
├── _grounding/                          ← research, reference material & user inputs (read by ALL skills)
│   ├── onboarding-info.md               ← wizard hints: route, profile, research_depth, tech stack preferences
│   ├── overview/                        ← overview step research + user_input.json
│   ├── features/                        ← features step research + user_input.json
│   ├── behaviors/                       ← behaviors step research
│   ├── brand_visual/                    ← brand-visual step research + user_input.json
│   ├── brand_behavioral/                ← brand-behavioral step research
│   ├── techstack/                       ← techstack step research + user_input.json
│   ├── architecture/                    ← architecture step research
│   ├── datamodel/                       ← datamodel step research
│   ├── screens/                         ← screens step research
│   ├── components/                      ← components step research
│   ├── general/                         ← cross-cutting research (domain, competitors, audiences, etc.)
│   │   ├── domain.md                    ← industry terminology, trends, compliance
│   │   ├── competitors.md               ← competitor analysis, feature comparisons
│   │   ├── audiences.md                 ← target personas, pain points, workflows
│   │   ├── design_inspiration.md        ← layout patterns, visual references
│   │   ├── patterns.md                  ← architectural and UX patterns for the domain
│   │   ├── colors_fonts.md              ← color palette research, typography trends
│   │   └── behavioral_patterns.md       ← state machines, workflow patterns from competitors
│   └── findings/                        ← raw screenshots, saved pages, research notes
│
├── _standards/                          ← discovered codebase standards (read by ALL skills)
│   ├── index.yml                        ← standards index for fast matching
│   ├── api/
│   ├── database/
│   ├── ui/
│   ├── naming/
│   ├── testing/
│   └── architecture/
│
├── 1_discovery/
│   ├── 1_overview/
│   │   ├── brief.md                     ← elevator pitch, audience, problem, hero flow
│   │   ├── goals.md                     ← success criteria, constraints, deadlines
│   │   └── comparable.md                ← reference apps, what to borrow/avoid
│   │
│   └── 2_brand/
│       ├── identity.md                  ← colors, fonts, tone — human-readable
│       ├── tokens.json                  ← machine-readable design tokens
│       └── references/                  ← screenshots from reference URLs
│
├── 2_experience/
│   ├── 1_journeys/                      ← optional: user journeys
│   │   └── stories.json                 ← personas, story maps (hero/vital/hygiene/backlog), EARS criteria
│   │
│   ├── 2_features/
│   │   ├── 01_<group_name>/             ← numbered feature groups
│   │   │   └── <feature>.md             ← one file per feature (includes ## Permissions section)
│   │   └── ...
│   │
│   ├── 3_screens/
│   │   ├── 00_layout/
│   │   │   └── shell.md                 ← app chrome: nav, sidebar, header
│   │   ├── 01_<group_name>/             ← numbered, matching 2_features/ groups
│   │   │   └── <screen>.md
│   │   └── components/                  ← reusable component specs (optional)
│   │
│   ├── 4_behaviors/                     ← optional: behavioral specs
│   │   └── <group_name>.allium          ← one spec per feature group
│   │
│   └── 5_storybook/                     ← optional: living Storybook prototype
│       ├── .storybook/                  ← config (main.ts, preview.ts, theme.ts)
│       ├── src/
│       │   ├── styles/brand.css         ← brand tokens as CSS custom properties
│       │   ├── @types/                  ← TypeScript interfaces
│       │   ├── data/seed.ts             ← typed seed data per scenario
│       │   ├── components/              ← custom components not in the project's UI library
│       │   ├── pages/<Group>/           ← full page compositions from screen specs
│       │   └── stories/
│       │       ├── Components/          ← Layer 1: custom component stories
│       │       ├── Pages/<NN Group>/    ← Layer 2: screen composition stories
│       │       └── Journeys/            ← Layer 3: clickable user journey flows
│       │           ├── Hero/
│       │           ├── Vital/
│       │           └── Hygiene/
│       ├── package.json
│       ├── vite.config.ts
│       └── tsconfig.json
│
└── 3_blueprint/
    ├── 1_techstack/
    │   └── stack.md                     ← chosen technologies + reasoning
    │
    ├── 2_architecture/                  ← optional
    │   └── architecture.md              ← system architecture, modules, data flow, protocols
    │
    └── 3_datamodel/                     ← schema format chosen by agent from stack (see below)
        ├── model.dbml                   ← canonical semantic model (generic/unknown stack)
        ├── model.json                   ← editor state for generic stack (drag-and-drop canvas)
        ├── schema.prisma                ← Prisma stack output (translated from semantic model)
        ├── postxl-schema.json           ← PostXL/NestJS stack output
        ├── seed.json                    ← realistic sample data organized by scenario
        └── feature_map.json             ← maps models to source features (cross-reference)
```

## _grounding/ — Research, Reference & User Input Layer

`_grounding/` is a **special, unnumbered folder** outside the numbered pipeline sequence.
It is the primary destination for all research output and persisted user inputs.

**Key rules:**
- **Written by:** the research mode (runs in parallel with any pipeline step) and skills saving user inputs
- **Read by:** ALL skills — always available as input regardless of which folder a skill owns
- **Not numbered:** leading underscore signals infrastructure, not a sequential step

**Structure:**
- **`onboarding-info.md`** — written by the UI wizard at project start. Contains the user's selected
  route, profile, research_depth, and tech stack preferences. Skills that make tech stack or
  architecture decisions read this file first and skip questions already answered here.
- **Step subfolders** (`overview/`, `features/`, `brand_visual/`, etc.) — each step stores its own
  research files and a `user_input.json` containing saved dialog field values (JSON, keyed by field ID)
- **`general/`** — cross-cutting research (domain, competitors, audiences, design inspiration,
  patterns, colors/fonts, behavioral patterns)
- **`findings/`** — raw material (screenshots, saved pages, research notes)

Step subfolder names map to the final segment of the skill path (shortened where applicable):
- `1_discovery/1_overview` → `overview/`
- `2_experience/2_features` → `features/`
- `2_experience/4_behaviors` → `behaviors/`
- `1_discovery/2_brand` → `brand_visual/` (visual) and `brand_behavioral/` (behavioral)
- `3_blueprint/1_techstack` → `techstack/`
- `3_blueprint/2_architecture` → `architecture/`
- `3_blueprint/3_datamodel` → `datamodel/`
- `2_experience/3_screens` → `screens/`
- `2_experience/3_screens/components` → `components/`

## _standards/ — Discovered Codebase Standards

`_standards/` is a **special, unnumbered folder** (like `_grounding/`) that stores
conventions discovered from an existing codebase.

**Key rules:**
- **Written by:** `support/standards-discover` (mode-based, runs in parallel like research)
- **Read by:** ALL skills — always available regardless of which folder a skill owns
- **Index file:** `index.yml` provides fast matching of standards to skills by `applies_to` and `keywords`

When `_standards/` exists, skills check for applicable standards before making decisions
(see Standards Injection pattern in `agent_patterns.md`).

## 3_blueprint/3_datamodel/ — Schema Format

The agent selects the schema format from the project's tech stack
(read from `3_blueprint/1_techstack/stack.md` or `_grounding/onboarding-info.md`):

| Stack signal | Schema file(s) |
|---|---|
| Generic / unknown | `model.dbml` + `model.json` (human-readable + editor state) |
| Prisma detected | `schema.prisma` |
| PostXL / NestJS | `postxl-schema.json` |
| Multiple outputs needed | Produce canonical `model.dbml` first, then emit the stack-specific format |

Regardless of schema format, `seed.json` and `feature_map.json` are always produced.

`seed.json` uses named scenarios (`empty`, `single_user`, `populated`, `edge_cases`).
`feature_map.json` maps each model to its source feature files (used by implementation skills
and quality audit for cross-reference validation).

## Naming Rules

- Phase folders: `1_discovery/`, `2_experience/`, `3_blueprint/` (single digit, no padding)
- Subfolders within phases: `1_overview/`, `2_features/`, `3_screens/` (single digit)
- Feature groups and screen groups: `01_<group_name>/` (two-digit, matching across both)
- Screen groups mirror feature group numbers exactly
- Special folders: leading underscore (`_grounding/`, `_standards/`) — not sequential steps
- File names: lowercase, underscore-separated (`password_reset.md`)
- No spaces in paths

## Feature Files — Permissions Section

Each feature file in `2_experience/2_features/` includes:

```markdown
---
permissions:
  admin: [create, read, update, delete]
  member: [read, update]
  guest: [read]
---

## Permissions

| Role | Actions |
|------|---------|
| admin | create, read, update, delete |
| member | read, update |
| guest | read |
```

This is consumed by `3_blueprint/3_datamodel` (auth rules) and `implement/1_setup/scaffold`
(authorization policy).

## Read Direction

Skills read from **lower-numbered** folders and write to **their own** folder only.

| Skill writing to | May read from |
|---|---|
| `1_discovery/1_overview` | `_grounding/` |
| `1_discovery/2_brand` | `_grounding/`, `1_overview/` |
| `2_experience/1_journeys` | `_grounding/`, `1_discovery/` |
| `2_experience/2_features` | `_grounding/`, `1_discovery/`, `2_experience/1_journeys/` |
| `2_experience/3_screens` | `_grounding/`, `1_discovery/`, `2_experience/1_journeys/`, `2_experience/2_features/`, optionally `3_blueprint/` |
| `2_experience/4_behaviors` | `_grounding/`, `1_discovery/`, `2_experience/2_features/` |
| `2_experience/5_storybook` | `_grounding/`, `1_discovery/2_brand/`, `2_experience/1_journeys/`, `2_experience/2_features/`, `2_experience/3_screens/`, optionally `3_blueprint/3_datamodel/` |
| `3_blueprint/1_techstack` | `_grounding/`, `1_discovery/`, `2_experience/` |
| `3_blueprint/2_architecture` | `_grounding/`, `1_discovery/`, `2_experience/`, `3_blueprint/1_techstack/` |
| `3_blueprint/3_datamodel` | `_grounding/`, `1_discovery/`, `2_experience/`, `3_blueprint/1_techstack/`, `3_blueprint/2_architecture/` |

`_grounding/` and `_standards/` are always readable by every skill regardless of phase.

`2_experience/4_behaviors/` is optional. Skills that consume it (`2_architecture/`, `3_datamodel/`,
`3_screens/`) check for its existence before reading.

`3_blueprint/2_architecture/` is optional. Skills writing to `3_datamodel/` and `3_screens/`
read it when present to understand service boundaries, data flows, and protocols.

## Dependency Flow

```
          ┌──────────────────────────────────┐
          │           _grounding/             │
          │   (research mode, parallel)       │
          └─────────────┬────────────────────┘
                        │ read by all skills
                        ▼
          ┌─────── 1_discovery ──────────────┐
          │  1_overview     2_brand          │
          └──────┬──────────────┬────────────┘
                 │              │
                 ▼              │
       2_experience/1_journeys  │
                 │              │
                 ▼              │
       2_experience/2_features  │
              │      │          │
              ▼      └──────────┼──► 3_blueprint/1_techstack
    2_exp/4_behaviors           │              │
              │                 │              ▼
              └──────────►  3_blueprint/2_architecture
                                │              │
                                ▼              ▼
                         3_blueprint/3_datamodel
                                │
                    ┌───────────┤
                    ▼           ▼
          2_exp/3_screens   2_exp/5_storybook
```

**Parallel tracks:** Brand, Journeys→Features, and Techstack run in parallel after the
overview. Architecture depends on Features + Techstack. Datamodel depends on
Features + Techstack + Architecture. Screens and Storybook depend on everything above.

`_grounding/` feeds every step. The research skill can run alongside any pipeline phase,
continuously enriching the knowledge base. Each step subfolder may also hold a
`user_input.json` with pre-collected dialog values from the UI.

## Legacy Path Compatibility

Projects created with older CF tooling may have:
- `01_project/`, `03_features/`, `04_brand/` etc. (flat numbered structure) — read both old and new paths, prefer new
- `_research/` or `02_research/` — treat as `_grounding/general/` content
- `A_01_<group>/` feature group prefix (letter+number) — treat same as `01_<group>/`

Skills should detect legacy structure from file existence and migrate output to the new paths.
