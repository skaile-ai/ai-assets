# _concept/ Directory Structure

All skills read and write to a `_concept/` folder inside the target project.
This is the canonical structure. Skills must use these exact paths.

```
_concept/
├── _standards/                          ← discovered codebase standards (read by ALL skills)
│   ├── index.yml                        ← standards index for fast matching
│   ├── api/                             ← API conventions
│   ├── database/                        ← database patterns
│   ├── ui/                              ← UI conventions
│   ├── naming/                          ← naming conventions
│   ├── testing/                         ← testing patterns
│   └── architecture/                    ← architectural patterns
│
├── _grounding/                          ← research, reference material & user inputs (read by ALL skills)
│   ├── onboarding-info.md               ← wizard hints: route, profile, research_depth, tech stack preferences
│   ├── overview/                        ← concept/discovery/overview step research + user_input.json
│   ├── features/                        ← concept/experience/features step research + user_input.json
│   ├── behaviors/                       ← concept/experience/behaviors step research
│   ├── brand_visual/                    ← concept/discovery/brand-visual step research + user_input.json
│   ├── brand_behavioral/               ← concept/discovery/brand-behavioral step research
│   ├── techstack/                       ← concept/blueprint/techstack step research + user_input.json
│   ├── architecture/                    ← concept/blueprint/architecture step research
│   ├── datamodel/                       ← concept/blueprint/datamodel step research
│   ├── screens/                         ← concept/experience/screens step research
│   ├── components/                      ← concept/experience/components step research
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
├── 01_project/
│   ├── brief.md                         ← elevator pitch, audience, problem, hero flow
│   ├── goals.md                         ← success criteria, constraints, deadlines
│   └── comparable.md                    ← reference apps, what to borrow/avoid
│
├── 02_journeys/                         ← optional: user journeys (produced by journeys skill)
│   └── stories.json                     ← personas, story maps (hero/vital/hygiene/backlog), EARS criteria
│
├── 02_research/                         ← DEPRECATED — use _grounding/ for new projects
│   ├── domain.md
│   ├── competitors.md
│   ├── audiences.md
│   ├── design_inspiration.md
│   └── findings/
│
├── 03_features/
│   ├── A_01_<group_name>/               ← letter+number prefixed feature groups
│   │   ├── <feature>.md                 ← one file per feature
│   │   └── ...
│   └── B_02_<group_name>/
│       └── ...
│
├── 03b_behavior/                        ← optional: behavioral specs (.allium)
│   ├── <group_name>.allium              ← one spec per feature group
│   └── ...
│
├── 04_brand/
│   ├── identity.md                      ← colors, fonts, tone — human-readable
│   ├── tokens.json                      ← machine-readable design tokens
│   └── references/                      ← screenshots from reference URLs
│
├── 05_techstack/
│   └── stack.md                         ← chosen technologies + reasoning
│
├── 05b_architecture/
│   └── architecture.md              ← system architecture, data flow, protocols
│
├── 06_datamodel/
│   ├── model.dbml                       ← human-readable entity definitions
│   ├── model.json                       ← editor state (drag-and-drop editable)
│   ├── model.schema.json                ← JSON Schema (from TypeBox definition)
│   └── seed.json                        ← realistic sample data for screens
│
├── 07_screens/
│   ├── 00_layout/
│   │   └── shell.md                     ← app chrome: nav, sidebar, header
│   ├── A_01_<group_name>/               ← letter+number prefixed, matching 03_features/ groups
│   │   ├── <screen>.md
│   │   └── ...
│   ├── components/                      ← reusable component specs
│   │   ├── navigation.md
│   │   ├── data_table.md
│   │   ├── form_controls.md
│   │   └── ...
│   └── ...
│
└── 08_testing/
    └── test_plan.md                     ← test scenarios per feature, mapped to seed data
```

## _grounding/ — Research, Reference & User Input Layer

`_grounding/` is a **special, unnumbered folder** that sits outside the numbered
pipeline sequence. It is the primary destination for all research output and persisted
user inputs.

**Key rules:**
- **Written by:** the research mode (runs in parallel with any pipeline step) and
  skills saving user inputs
- **Read by:** ALL skills — `_grounding/` is always available as input regardless
  of which numbered folder a skill owns
- **Not numbered:** it uses a leading underscore to signal it is infrastructure,
  not a sequential pipeline step

**Structure:**
- **`onboarding-info.md`** — written by the UI wizard at project start. Contains
  the user's selected route, profile, research_depth, and any tech stack preferences
  (frontend framework, component library, backend, architecture notes). Skills that
  make tech stack or architecture decisions should read this file first and skip
  asking questions that are already answered here.
- **Step subfolders** (`overview/`, `features/`, `brand_visual/`, etc.) — each step
  can store its own research files and a `user_input.json` file containing saved
  dialog field values from the UI (JSON object with field IDs as keys)
- **`general/`** — cross-cutting research that applies to multiple steps (domain,
  competitors, audiences, design inspiration, patterns, colors/fonts, behavioral patterns)
- **`findings/`** — raw material (screenshots, saved pages, research notes)

Step subfolder names map to the final segment of the skill path:
using shorter names where applicable:
- `concept/discovery/overview` -> `overview/`
- `concept/experience/features` -> `features/`
- `concept/experience/behaviors` -> `behaviors/`
- `concept/discovery/brand-visual` -> `brand_visual/`
- `concept/discovery/brand-behavioral` -> `brand_behavioral/`
- `concept/blueprint/techstack` -> `techstack/`
- `concept/blueprint/architecture` -> `architecture/`
- `concept/blueprint/datamodel` -> `datamodel/`
- `concept/experience/screens` -> `screens/`
- `concept/experience/components` -> `components/`

The research skill (`concept/discovery/research`) writes cross-cutting research to `_grounding/general/`
and step-specific research to `_grounding/{step}/`. Skills should check
`_grounding/{step}/user_input.json` for pre-collected user inputs before asking the user.

**Backward compatibility:** When a legacy `_research/` or `02_research/` folder exists,
skills should read from both locations, preferring `_grounding/` when the same topic
appears in multiple locations.

## _standards/ — Discovered Codebase Standards

`_standards/` is a **special, unnumbered folder** (like `_grounding/`) that stores
conventions discovered from an existing codebase.

**Key rules:**
- **Written by:** `support/standards-discover` (mode-based, runs in parallel like research)
- **Read by:** ALL skills — `_standards/` is always available as input regardless
  of which numbered folder a skill owns
- **Not numbered:** it uses a leading underscore to signal it is infrastructure
- **Index file:** `index.yml` provides fast matching of standards to skills
  by `applies_to` and `keywords` fields

When `_standards/` exists, skills should check for applicable standards before
making decisions (see Standards Injection pattern in `agent_patterns.md`).

## _research/ — DEPRECATED

> **Deprecation notice:** `_research/` is retained for backward compatibility
> with existing projects. New projects should use `_grounding/` exclusively.
> Skills should check both `_grounding/` and `_research/` and prefer `_grounding/`.
>
> Migration: move topic files from `_research/{step}/` into the matching `_grounding/{step}/`
> subfolder, and cross-cutting files from `_research/general/` into `_grounding/general/`.
> Move raw material from `_research/findings/` to `_grounding/findings/`.

## 02_research/ — DEPRECATED

> **Deprecation notice:** `02_research/` is retained for backward compatibility
> with existing projects. New projects should use `_grounding/` exclusively.
>
> Migration: move files from `02_research/` into `_grounding/general/`
> (e.g., `domain.md` -> `_grounding/general/domain.md`, `competitors.md` ->
> `_grounding/general/competitors.md`).

## 08_testing/ — Test Plans

`08_testing/` holds test plans that map feature scenarios to seed data.
`test_plan.md` references features from `03_features/` and seed scenarios from
`06_datamodel/seed.json`. Skills writing to `08_testing/` may read from all
numbered folders plus `_grounding/`.

## 07_screens/components/ — Component Inventory

The `components/` subfolder inside `07_screens/` captures reusable UI component
specifications that are shared across multiple screens. Each component file
describes props, variants, states, and accessibility notes. These are referenced
by individual screen specs via cross-links.

## Naming Rules

- Top-level numbered folders: `01_` through `08_` (plus `03b_behavior/` and `05b_architecture/` as lettered sub-steps)
- `_grounding/` uses a leading underscore (unnumbered, infrastructure)
- Feature groups use letter+number prefixes: `A_01_user_auth/`, `B_02_dashboard/`
- Screen groups mirror feature group numbers exactly
- Component files in `07_screens/components/` are lowercase, underscore-separated
- File names are lowercase, underscore-separated: `password_reset.md`
- No spaces in paths

## Read Direction

Skills always read from **lower-numbered** folders and write to **their own** folder.
A skill writing to `06_datamodel/` may read from `01_project/`, `03_features/`,
`03b_behavior/`, `05_techstack/`, and `05b_architecture/`, but never from `07_screens/`.
A skill writing to `07_screens/` may optionally read `05b_architecture/` for protocol
and service context affecting screen data flows.

**Exception — `_grounding/`:** Because `_grounding/` is unnumbered infrastructure,
**every skill may read from it** regardless of the skill's own folder number.
The research mode writes cross-cutting research to `_grounding/general/` and
step-specific research to `_grounding/{step}/`. Skills also write `user_input.json`
to their step subfolder when persisting user dialog inputs.

`03b_behavior/` is optional. Skills that can consume it (architecture, datamodel, screens)
check for its existence and use it if present. The pipeline works without it.

`05b_architecture/` documents system architecture. Skills writing to `06_datamodel/`
and `07_screens/` should read it when present to understand custom services, data flows,
and protocols.

## Dependency Flow

```
                    ┌─────────────────────────────────┐
                    │         _grounding/              │
                    │   (research mode, runs parallel) │
                    └────────────┬────────────────────┘
                                 │ read by all skills
                                 ▼
               ┌─────────── 01_project ───────────┐
               │                │                  │
               ▼                ▼                  ▼
          04_brand        03_features         05_techstack
               │           │        │              │
               │           ▼        └──────┬───────┘
               │     03b_behavior*         │
               │           │               ▼
               │           └──────► 05b_architecture
               │                          │
               │                          ▼
               │        06_datamodel ◄────┘
               │              │
               ▼              ▼
            07_screens ◄──────┘
               │
               ▼
          08_testing
```

**Parallel tracks:** Features, Brand, and Tech Stack run in parallel after the
project brief. Architecture depends on Features + Tech Stack. Data Model depends
on Features + Tech Stack + Architecture. Screens depend on everything. Testing
depends on Screens + Data Model + Features.

`_grounding/` feeds into every step — the research mode can run alongside any
pipeline phase, continuously enriching the knowledge base. Each step subfolder
can also hold a `user_input.json` with pre-collected dialog values from the UI.
