# \_concept/ Directory Structure

All skills read and write to a `_concept/` folder inside the target project.
This is the canonical structure. Skills must use these exact paths.

```
_concept/
├── 1_discovery/1_overview/                          Phase 1: Discovery
│   ├── brief.md                  ← elevator pitch, audience, problem, hero flow
│   ├── goals.md                  ← success criteria, constraints, deadlines
│   └── comparable.md             ← reference apps, what to borrow/avoid
│
├── 1_discovery/2_research/                         Phase 1: Discovery (optional)
│   ├── domain.md                 ← market/domain knowledge
│   ├── competitors.md            ← competitor analysis
│   ├── audiences.md              ← user personas, needs, pain points
│   ├── design_inspiration.md     ← layout patterns, color approaches, brand references
│   └── findings/                 ← raw material (saved pages, screenshots, notes)
│
├── 1_discovery/3_brand/                            Phase 1: Discovery (optional)
│   ├── identity.md               ← colors, fonts, tone — human-readable
│   ├── tokens.json               ← machine-readable design tokens
│   └── references/               ← screenshots from reference URLs
│
├── 2_experience/1_journeys/                         Phase 2: Experience
│   └── stories.json              ← structured user journeys with personas and EARS criteria
│
├── 2_experience/2_features/                         Phase 2: Experience
│   ├── 01_<group_name>/          ← numbered feature groups
│   │   ├── <feature>.md          ← one file per feature (includes ## Permissions section)
│   │   └── ...
│   └── 02_<group_name>/
│       └── ...
│
├── 2_experience/3_screens/                          Phase 2: Experience
│   ├── 00_layout/
│   │   └── shell.md              ← app chrome: nav, sidebar, header
│   ├── 01_<group_name>/          ← numbered, matching 2_experience/2_features/ groups
│   │   ├── <screen>.md
│   │   └── ...
│   └── ...
│
├── 2_experience/4_storybook/                        Phase 2: Experience (optional)
│   ├── .storybook/               ← Storybook configuration (main.ts, preview.ts, theme.ts)
│   ├── src/
│   │   ├── styles/brand.css      ← brand tokens as CSS custom properties
│   │   ├── @types/               ← TypeScript interfaces (mocked initially, replaced by pxl types after step 10)
│   │   ├── data/seed.ts          ← typed seed data per scenario
│   │   ├── components/           ← custom components NOT in @postxl/ui-components
│   │   ├── pages/<Group>/        ← full page compositions from screen specs
│   │   └── stories/
│   │       ├── Components/       ← Layer 1: custom component stories
│   │       ├── Pages/<NN Group>/ ← Layer 2: screen composition stories
│   │       └── Journeys/         ← Layer 3: clickable user journey flows
│   │           ├── Hero/
│   │           ├── Vital/
│   │           └── Hygiene/
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
├── 3_blueprint/1_techstack/                        Phase 3: Blueprint
│   └── stack.md                  ← chosen technologies + reasoning
│
├── 3_blueprint/2_architecture/                     Phase 3: Blueprint (optional)
│   └── architecture.md           ← system architecture, modules, data flow, protocols
│
└── 3_blueprint/3_datamodel/                        Phase 3: Blueprint
    ├── postxl-schema.json        ← PostXL schema (models, fields, relations, auth)
    ├── seed.json                 ← realistic sample data organized by scenario
    └── feature_map.json          ← maps models to source features (cross-reference)

# After step 9 (data model), if Storybook exists, step 10 runs `pxl types`
# to replace mocked types in 2_experience/4_storybook/src/@types/ with
# schema-generated types and verifies compilation.
# This does NOT create a new folder — it updates 2_experience/4_storybook/ in-place.
```

## Phases

The concept pipeline is organized into three phases:

1. **Discovery** (01–03) — Understand the problem, research the domain, establish brand identity.
2. **Experience** (04–06b) — Map user journeys, define features from those journeys, specify screens, and optionally visualize in Storybook.
3. **Blueprint** (07–10) — Choose technologies, design architecture, model the data, and integrate schema types into Storybook.

## Naming Rules

- Top-level folders are numbered: `01_` through `09_` (sequential, no letter suffixes)
- Feature groups use numbered prefixes: `01_user_auth/`, `02_dashboard/`
- Screen groups mirror feature group numbers exactly
- File names are lowercase, underscore-separated: `password_reset.md`
- No spaces in paths

## Read Direction

Skills always read from **lower-numbered** folders and write to **their own** folder.
A skill writing to `3_blueprint/3_datamodel/` may read from `1_discovery/1_overview/`, `2_experience/2_features/`,
`3_blueprint/1_techstack/`, and `3_blueprint/2_architecture/`, but never from `2_experience/3_screens/`.

`2_experience/1_journeys/` provides structured user journey maps with personas and EARS
acceptance criteria. It is the primary input for feature planning — the
`concept-2-experience-2-features` skill reads journeys to derive and organize features.

`2_experience/3_screens/` comes before `3_blueprint/1_techstack/` in folder numbering but depends on
`2_experience/2_features/` and `1_discovery/3_brand/`. Screen specs may optionally read from
`3_blueprint/1_techstack/`, `3_blueprint/2_architecture/`, and `3_blueprint/3_datamodel/` when those exist, so
screens can be written in any order relative to the Blueprint phase.

Each feature file in `2_experience/2_features/` includes a `## Permissions` section with a
role-action matrix and a `permissions` frontmatter field mapping roles to allowed
actions. This is consumed by `concept-3-blueprint-3-datamodel` (auth rules) and `implement-1-setup-1-scaffold`
(authorization policy).

`2_experience/4_storybook/` is a standalone Storybook project with 3 layers: custom
components, screen compositions, and clickable user journey flows. It reads from
`2_experience/3_screens/`, `2_experience/1_journeys/`, `1_discovery/3_brand/`, and
optionally from `2_experience/2_features/` and `3_blueprint/3_datamodel/`.

`postxl-schema.json` defines the data model. Seed data is maintained separately
in `seed.json` using named scenarios (populated, empty, edge_cases, etc.) for
mockups, screen specs, and E2E testing.
