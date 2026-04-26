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
├── discovery/
│   ├── brief.md                         ← elevator pitch, audience, problem, hero flow
│   ├── goals.md                         ← success criteria, constraints, deadlines
│   ├── comparable.md                    ← reference apps, what to borrow/avoid
│   │
│   └── brand/
│       ├── identity.md                  ← colors, fonts, tone — human-readable
│       ├── tokens.json                  ← machine-readable design tokens
│       └── references/                  ← screenshots from reference URLs
│
├── experience/
│   ├── journeys/                        ← optional: user journeys
│   │   └── stories.json                 ← personas, story maps (hero/vital/hygiene/backlog), EARS criteria
│   │
│   ├── features/
│   │   ├── 01_<group_name>/             ← numbered feature groups
│   │   │   └── <feature>.md             ← one file per feature (includes ## Permissions section)
│   │   └── ...
│   │
│   ├── screens/
│   │   ├── 00_layout/
│   │   │   └── shell.md                 ← app chrome: nav, sidebar, header
│   │   ├── 01_<group_name>/             ← numbered, matching features/ groups
│   │   │   └── <screen>.md
│   │   └── components/                  ← reusable component specs (optional)
│   │
│   └── behaviors/                       ← optional: behavioral specs
│       └── <group_name>.allium          ← one spec per feature group
│
├── prototype/
│   └── storybook/                       ← optional: living Storybook prototype
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
└── blueprint/
    ├── techstack.md                     ← chosen technologies + reasoning
    │
    ├── architecture.md                  ← optional: system architecture, modules, data flow, protocols
    │
    └── datamodel/                       ← schema format chosen by agent from stack (see below)
        ├── model.dbml                   ← canonical semantic model (generic/unknown stack)
        ├── model.json                   ← editor state for generic stack (drag-and-drop canvas)
        ├── schema.prisma                ← Prisma stack output (translated from semantic model)
        ├── postxl-schema.json           ← PostXL/NestJS stack output
        ├── seed.json                    ← realistic sample data organized by scenario
        └── feature_map.json             ← maps models to source features (cross-reference)
```

## _grounding/ — Research, Reference & User Input Layer

`_grounding/` is a **special, unnumbered folder** outside the pipeline sequence.
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

| Skill | `_grounding/step/` subfolder |
|---|---|
| `skailup-overview` | `step/overview/` |
| `skailup-features` | `step/features/` |
| `skailup-behaviors` | `step/behaviors/` |
| `skailup-brand-visual` | `step/brand-visual/` |
| `skailup-brand-behavioral` | `step/brand-behavioral/` |
| `skailup-techstack` | `step/techstack/` |
| `skailup-architecture` | `step/architecture/` |
| `skailup-datamodel` | `step/datamodel/` |
| `skailup-screens` | `step/screens/` |
| `skailup-components` | `step/components/` |

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

## blueprint/datamodel/ — Schema Format

The agent selects the schema format from the project's tech stack
(read from `blueprint/techstack.md` or `_grounding/onboarding/decisions.yaml`):

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

- Phase folders use plain names with no numeric prefix: `discovery/`, `experience/`, `blueprint/`, `prototype/`
- Discovery artifacts live directly in `discovery/` (brief.md, goals.md, comparable.md) or in `discovery/brand/`
- Feature groups and screen groups: `01_<group_name>/` (two-digit prefix, matching across both)
- Screen groups mirror feature group numbers exactly
- Special folders: leading underscore (`_grounding/`, `_seeds/`, `_standards/`) — not sequential steps
- File names: lowercase, hyphen-separated or underscore-separated (`password_reset.md`)
- No spaces in paths

## Feature Files — Permissions Section

Each feature file in `experience/features/` includes:

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

This is consumed by `blueprint/datamodel/` (auth rules) and implementation scaffolding
(authorization policy).

## Read Direction

Skills read from **earlier pipeline phases** and write to **their own** folder only.

| Skill writing to | May read from |
|---|---|
| `discovery/` (brief, goals, comparable) | `_grounding/` |
| `discovery/brand/` | `_grounding/`, `discovery/brief.md` |
| `experience/journeys/` | `_grounding/`, `discovery/` |
| `experience/features/` | `_grounding/`, `discovery/`, `experience/journeys/` |
| `experience/screens/` | `_grounding/`, `discovery/`, `experience/journeys/`, `experience/features/`, optionally `blueprint/` |
| `experience/behaviors/` | `_grounding/`, `discovery/`, `experience/features/` |
| `prototype/storybook/` | `_grounding/`, `discovery/brand/`, `experience/journeys/`, `experience/features/`, `experience/screens/`, optionally `blueprint/datamodel/` |
| `blueprint/techstack.md` | `_grounding/`, `discovery/`, `experience/` |
| `blueprint/architecture.md` | `_grounding/`, `discovery/`, `experience/`, `blueprint/techstack.md` |
| `blueprint/datamodel/` | `_grounding/`, `discovery/`, `experience/`, `blueprint/techstack.md`, `blueprint/architecture.md` |

`_grounding/` and `_standards/` are always readable by every skill regardless of phase.

`experience/behaviors/` is optional. Skills that consume it (`blueprint/architecture.md`, `blueprint/datamodel/`,
`experience/screens/`) check for its existence before reading.

`blueprint/architecture.md` is optional. Skills writing to `blueprint/datamodel/` and `experience/screens/`
read it when present to understand service boundaries, data flows, and protocols.

## Dependency Flow

```
          ┌──────────────────────────────────┐
          │           _grounding/             │
          │   (research mode, parallel)       │
          └─────────────┬────────────────────┘
                        │ read by all skills
                        ▼
          ┌─────── discovery ────────────────┐
          │  brief/goals/comparable   brand/ │
          └──────┬──────────────┬────────────┘
                 │              │
                 ▼              │
       experience/journeys/     │
                 │              │
                 ▼              │
       experience/features/     │
              │      │          │
              ▼      └──────────┼──► blueprint/techstack.md
    experience/behaviors/       │              │
              │                 │              ▼
              └──────────►  blueprint/architecture.md
                                │              │
                                ▼              ▼
                         blueprint/datamodel/
                                │
                    ┌───────────┤
                    ▼           ▼
          experience/screens/  prototype/storybook/
```

**Parallel tracks:** Brand, Journeys→Features, and Techstack run in parallel after the
overview. Architecture depends on Features + Techstack. Datamodel depends on
Features + Techstack + Architecture. Screens and Storybook depend on everything above.

`_grounding/` feeds every step. The research skill can run alongside any pipeline phase,
continuously enriching the knowledge base. Per-skill dialog values are stored in
`_grounding/onboarding/inputs/{step}.json` and loaded on resume.

## Legacy Path Compatibility

Projects **not yet migrated** via `_scripts/migrate-concept-v2.sh` may have v1 numbered paths:

| v1 path (legacy) | v2 path (current) |
|---|---|
| `1_discovery/1_overview/brief.md` | `discovery/brief.md` |
| `1_discovery/1_overview/goals.md` | `discovery/goals.md` |
| `1_discovery/1_overview/comparable.md` | `discovery/comparable.md` |
| `1_discovery/2_brand/` | `discovery/brand/` |
| `2_experience/1_journeys/` | `experience/journeys/` |
| `2_experience/2_features/` | `experience/features/` |
| `2_experience/3_screens/` | `experience/screens/` |
| `2_experience/4_behaviors/` | `experience/behaviors/` |
| `2_experience/5_storybook/` | `prototype/storybook/` |
| `3_blueprint/1_techstack/stack.md` | `blueprint/techstack.md` |
| `3_blueprint/2_architecture/architecture.md` | `blueprint/architecture.md` |
| `3_blueprint/3_datamodel/` | `blueprint/datamodel/` |

Additional legacy paths from older CF tooling:
- `01_project/`, `03_features/`, `04_brand/` etc. (flat numbered structure) — read both old and new paths, prefer new
- `_research/` or `02_research/` — treat as `_grounding/research/` content
- `A_01_<group>/` feature group prefix (letter+number) — treat same as `01_<group>/`
- `_grounding/general/` — treat as `_grounding/research/` content (rename on next write)
- `_grounding/{step}/user_input.json` (e.g. `_grounding/overview/user_input.json`) — treat as `_grounding/onboarding/inputs/{step}.json`
- `_grounding/onboarding-info.md` — treat as legacy equivalent of `_grounding/onboarding/profile.yaml` + `_grounding/onboarding/decisions.yaml`; extract and split on next write

Skills should detect legacy structure from file existence and migrate output to the new paths.
Run `_scripts/migrate-concept-v2.sh <project-dir>` to migrate a project in bulk.
