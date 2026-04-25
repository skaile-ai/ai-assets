# _concept/ Directory Structure

All skills read and write to a `_concept/` folder inside the target project.
This is the canonical structure. Skills must use these exact paths.

## concept.yaml — Project Manifest

Every `_concept/` directory contains a `concept.yaml` at its root. This manifest tracks:
- Project type and profile (web-app, cli-tool, etc.)
- Per-domain tier settings
- Artifact status (which artifacts exist, their source, last_updated)
- Grounding artifact status (research, onboarding)
- Seed mapping (which seeds map to which artifacts)

```yaml
type: web-app
profile: web-app  # references skaileup-shared/contracts/profiles/web-app.yaml

artifacts:
  brief:
    status: approved
    source: generated
    produced_by: skailup-overview
    last_updated: 2026-04-25
  features:
    status: draft
    source: seed-partial
    seed_file: "_seeds/feature-list.md"
    produced_by: skailup-features
    last_updated: 2026-04-25

grounding:
  competitors:
    status: draft
    source: generated
    produced_by: skailup-research
    last_updated: 2026-04-25
    products_analyzed: 5

onboarding:
  profile:
    status: approved
    source: onboarding
    last_updated: 2026-04-25
  decisions:
    status: approved
    source: onboarding
    last_updated: 2026-04-25
```

```
_concept/
├── concept.yaml                         ← project manifest (type, profile, artifact status, seed mapping)
│
├── _grounding/                          ← research, reference material & user inputs (read by ALL skills)
│   ├── onboarding/                      ← structured onboarding output
│   │   ├── profile.yaml                 ← project profile (name, type, tier, audience, problem)
│   │   ├── decisions.yaml               ← collected decisions (tech-stack, research depth, etc.)
│   │   └── inputs/                      ← per-skill dialog answers (preserved for resume)
│   │       ├── overview.json
│   │       ├── features.json
│   │       └── brand-visual.json
│   │
│   ├── research/                        ← structured research artifacts
│   │   ├── domain.md                    ← industry terminology, regulations, trends
│   │   ├── competitors.md               ← per-product analysis (features, gaps, positioning)
│   │   ├── audiences.md                 ← target personas with design implications
│   │   ├── design-inspiration.md        ← layout patterns, color, typography, components
│   │   ├── patterns.md                  ← UX/architectural patterns for this domain
│   │   ├── colors-fonts.md              ← color palette and typography research
│   │   └── behavioral-patterns.md       ← state machines, lifecycle patterns from competitors
│   │
│   ├── findings/                        ← raw material (screenshots, saved pages, excerpts)
│   │   ├── index.md                     ← catalog of all raw findings with source + date
│   │   └── *.png / *.md                 ← screenshots, page saves, raw notes
│   │
│   └── step/                            ← step-specific research (dispatched alongside a skill)
│       ├── features/                    ← research triggered during features skill
│       ├── screens/                     ← research triggered during screens skill
│       └── datamodel/                   ← research triggered during datamodel skill
│
├── _seeds/                              ← user-provided material (input layer)
│   └── (any files dropped by the user — auto-classified by skailup-ingest-seeds)
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
- **`onboarding/`** — written by the UI wizard at project start.
  - `profile.yaml` — project profile: name, type, tier, audience, problem statement
  - `decisions.yaml` — collected decisions: tech-stack preferences, research depth, route
  - `inputs/` — per-skill dialog field values preserved as JSON for resume (e.g. `overview.json`, `features.json`)
  - Skills that make tech stack or architecture decisions read `profile.yaml` and `decisions.yaml` first
    and skip questions already answered here.
- **`research/`** — cross-cutting research artifacts: domain, competitors, audiences, design
  inspiration, patterns, colors/fonts, behavioral patterns. Written by the research skill; read
  by all pipeline skills.
- **`findings/`** — raw material (screenshots, saved pages, excerpts). `index.md` catalogs all
  entries with source and date.
- **`step/`** — step-specific research dispatched alongside a pipeline skill, organized by skill
  name. Each subfolder mirrors the convention below.

Step subfolder names under `_grounding/step/` map to the final segment of the skill path:

| Skill path | `_grounding/step/` subfolder |
|---|---|
| `1_discovery/1_overview` | `step/overview/` |
| `2_experience/2_features` | `step/features/` |
| `2_experience/4_behaviors` | `step/behaviors/` |
| `1_discovery/2_brand` (visual) | `step/brand-visual/` |
| `1_discovery/2_brand` (behavioral) | `step/brand-behavioral/` |
| `3_blueprint/1_techstack` | `step/techstack/` |
| `3_blueprint/2_architecture` | `step/architecture/` |
| `3_blueprint/3_datamodel` | `step/datamodel/` |
| `2_experience/3_screens` | `step/screens/` |
| `2_experience/3_screens/components` | `step/components/` |

## _seeds/ — User-Provided Input Layer

`_seeds/` is an **unnumbered input layer** where users place existing material before or during a
concept pipeline run.

**Key rules:**
- **Written by:** the user (manual drop) — not generated by any skill
- **Read by:** `skailup-ingest-seeds` — classifies files by content analysis and maps each to an artifact slot
- **Seed states:** `seed` (complete, ready to use), `seed-partial` (usable but incomplete), `seed-reference` (inspiration only)
- **Tracking:** once ingested, each seed is recorded in `concept.yaml` under the `artifacts` or `grounding` section with its mapped artifact, seed state, and source file path

Skills do not read `_seeds/` directly. They read the canonical artifact paths and check `concept.yaml`
to learn whether an artifact was produced from a seed (and at what completeness level).

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
(read from `3_blueprint/1_techstack/stack.md` or `_grounding/onboarding/decisions.yaml`):

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
- Special folders: leading underscore (`_grounding/`, `_seeds/`, `_standards/`) — not sequential steps
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
continuously enriching the knowledge base. Per-skill dialog values are stored in
`_grounding/onboarding/inputs/{step}.json` and loaded on resume.

## Legacy Path Compatibility

Projects created with older CF tooling may have:
- `01_project/`, `03_features/`, `04_brand/` etc. (flat numbered structure) — read both old and new paths, prefer new
- `_research/` or `02_research/` — treat as `_grounding/research/` content
- `A_01_<group>/` feature group prefix (letter+number) — treat same as `01_<group>/`
- `_grounding/general/` — treat as `_grounding/research/` content (rename on next write)
- `_grounding/{step}/user_input.json` (e.g. `_grounding/overview/user_input.json`) — treat as `_grounding/onboarding/inputs/{step}.json`
- `_grounding/onboarding-info.md` — treat as legacy equivalent of `_grounding/onboarding/profile.yaml` + `_grounding/onboarding/decisions.yaml`; extract and split on next write

Skills should detect legacy structure from file existence and migrate output to the new paths.
