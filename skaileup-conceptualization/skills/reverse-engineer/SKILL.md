---
name: "reverse-engineer"
description: "Use when the user has an existing project repository and wants to generate or bootstrap a _concept/ folder from it. Triggered by: 'reverse engineer this project', 'generate concept from existing code', 'I have a codebase, extract the concept', 'document this existing app', 'build concept from repo'."
metadata:
  version: "1.0.0"
  tags:
    - "reverse"
    - "existing"
    - "codebase"
    - "repo"
    - "extract"
    - "import"
    - "bootstrap"
    - "existing-project"
    - "infer"
    - "scan"
  source: "MIGRATED"
  prerequisites:
    inputs_required:
      - id: repo_path
        label: "Repository Path"
        type: text
        hint: "Absolute or relative path to the existing project repository root"
    inputs_optional:
      - id: concept_path
        label: "Concept Output Path"
        type: text
        hint: "Where to write _concept/ (default: <repo_path>/_concept/). Override if the concept lives elsewhere."
      - id: extraction_scope
        label: "What to Extract"
        type: multiselect
        options:
          - overview
          - techstack
          - features
          - datamodel
          - brand
          - screens
        default:
          - overview
          - techstack
          - features
          - datamodel
          - brand
          - screens
        hint: "Which concept artifacts to generate. Omit items you already have or want to write manually."
      - id: overwrite_mode
        label: "If _concept/ Files Already Exist"
        type: select
        options:
          - skip
          - diff_and_confirm
          - overwrite
        default: skip
        hint: "skip = never overwrite, diff_and_confirm = show diff and ask, overwrite = always replace"
      - id: app_description_hint
        label: "What does this app do? (optional hint)"
        type: text
        hint: "Brief description to help the agent when README is sparse or missing."
    produces:
      - path: "_concept"
        description: "Full _concept/ structure extracted and inferred from the existing repository"
  user_inputs:
    dialog:
      - id: "repo_path"
        label: "Repository Path"
        type: "text"
        required: true
        hint: "Absolute or relative path to the existing project repository root"
      - id: "concept_path"
        label: "Concept Output Path"
        type: "text"
        required: false
        hint: "Where to write _concept/ (default: <repo_path>/_concept/). Override if the concept lives elsewhere."
      - id: "extraction_scope"
        label: "What to Extract"
        type: "multiselect"
        required: false
        options:
          - "overview"
          - "techstack"
          - "features"
          - "datamodel"
          - "brand"
          - "screens"
        default:
          - "overview"
          - "techstack"
          - "features"
          - "datamodel"
          - "brand"
          - "screens"
        hint: "Which concept artifacts to generate. Omit items you already have or want to write manually."
      - id: "overwrite_mode"
        label: "If _concept/ Files Already Exist"
        type: "select"
        required: false
        options:
          - "skip"
          - "diff_and_confirm"
          - "overwrite"
        default: "skip"
        hint: "skip = never overwrite, diff_and_confirm = show diff and ask, overwrite = always replace"
      - id: "app_description_hint"
        label: "What does this app do? (optional hint)"
        type: "text"
        required: false
        hint: "Brief description to help the agent when README is sparse or missing."
    files: []
---

# Reverse Engineer — Concept from Existing Repository

## Overview

The **reverse-engineer** skill analyzes an existing project repository and
produces a complete `_concept/` directory from it. It is an alternative entry
point to the pipeline: instead of building a concept from user dialog, it reads
source code, configuration, schemas, and documentation to infer what was built,
why, and how.

**Writes to:** `_concept/` (all applicable folders)

Every generated artifact is tagged with a confidence level:
- `extracted` — read directly from code or config (high confidence)
- `inferred` — reasoned from context or structure (medium confidence)
- `needs_review` — could not be determined reliably (must be validated)

## When to Use

- User says "I have an existing codebase", "reverse engineer this", "document this app"
- User wants to bring an existing project into the pipeline
- `_concept/` does not exist yet but source code does
- User wants to use skills (design, testing, refactoring) on a pre-existing project

## When NOT to Use

- User is starting from scratch with no existing code — use `overview` instead
- `_concept/` already exists and is fully populated — run `review` instead
- User wants to update a specific artifact — run the individual skill instead

## Prerequisites

No standard pipeline prerequisites. This skill is an entry point.

Before starting:
- `repo_path` must point to a readable directory
- At minimum, the repository must have at least one of: source files, a README, or a package manifest

## Shared Contracts

Before starting, read:
- `skaileup-shared/contracts/concept_structure.md` — canonical `_concept/` paths and naming rules
- `skaileup-shared/contracts/frontmatter.md` — required YAML fields
- `skaileup-shared/contracts/semantic_types.md` — stack-independent types (data model output uses these)
- `skaileup-shared/contracts/iron_laws.md` — non-negotiable constraints
- `skaileup-shared/contracts/agent_patterns.md` — communication style, standalone mode

## Context Budget

| Source | Priority |
|--------|----------|
| Shared contracts (above) | Required |
| `<repo_path>/README.md`, root package manifests, router files, schema/model files, config files | Required (from repo) |
| Compiled output (`dist/`, `build/`, `.next/`, `node_modules/`), binary assets, lockfiles | Never load |

Keep context targeted. Do not load entire source trees. Scan file lists first, then
read selectively based on file purpose.

## Workflow

### Step 1: Validate Input

Confirm `repo_path` is readable. If not, stop immediately and report.

If `concept_path` is not provided, default to `<repo_path>/_concept/`.

Check `overwrite_mode`. If `_concept/` already contains files and `overwrite_mode` is `skip`,
note which output artifacts already exist — skip them at generation time. If `diff_and_confirm`,
collect diffs at the end of each phase before writing.

EMIT  [reverse-engineer] started run_id=<uuid> repo_path=<path> concept_path=<path> scope=[...]

### Step 2: Repository Discovery

**Goal:** Build a map of the repository before reading any file deeply.

2a. **File tree scan** — List the top 2 levels of `repo_path`. Identify:
- Root manifest files (package.json, pyproject.toml, Cargo.toml, go.mod, pom.xml, composer.json, Gemfile)
- README, CHANGELOG, docs/
- Source root (`src/`, `app/`, `lib/`, pages, routes)
- Config files (vite.config, nuxt.config, next.config, tailwind.config, tsconfig)
- Database/model directories (prisma/, migrations/, models/, schemas/, db/)
- Test directories (tests/, __tests__/, spec/, e2e/, cypress/, playwright/)
- CI/CD configs (.github/workflows/, .gitlab-ci.yml, Dockerfile)

2b. **Project type detection** — From manifests, determine:
- Language(s): TypeScript, JavaScript, Python, Rust, Go, Ruby, Java, PHP, other
- App type: web app, API-only, CLI, library, monorepo, full-stack

2c. **Depth check** — For monorepos, identify which sub-package is the primary app.
Ask the user if ambiguous (more than one candidate app package).

EMIT  [reverse-engineer] checkpoint phase=discovery project_type=<type> languages=[...]

### Step 3: Overview Extraction (scope: overview)

**Goal:** Produce `1_discovery/1_overview/brief.md`, `goals.md`, `comparable.md`.

Read in order:
1. `README.md` (or docs/README.md, docs/index.md)
2. Package manifest fields: `name`, `description`, `homepage`, `keywords`, `author`
3. `CHANGELOG.md` — for release history and feature evolution hints
4. Any `docs/` or `wiki/` markdown

Extract:
- **App name** — from manifest `name` field, then README title
- **Elevator pitch** — from manifest `description`, README tagline, or first paragraph
- **Target audience** — from README "Who is this for?", "Getting Started" section
- **Problem statement** — from README motivation, background, or "Why?" sections
- **Hero flow** — from README quickstart, primary usage example, or most prominent route
- **Comparable products** — from README "Similar to", "Inspired by", "Alternatives" sections
- **Success criteria / goals** — from README features list, roadmap, or milestone markers

Confidence tagging rules:
- `extracted` — text verbatim from README or manifest
- `inferred` — synthesized from code structure or indirect clues
- `needs_review` — field left empty because no signal found

Write:

**`_concept/1_discovery/1_overview/brief.md`**
```yaml
---
elevator_pitch: "<extracted or inferred>"
audience: "<extracted or inferred>"
problem: "<extracted or inferred>"
hero_flow: "<extracted or inferred>"
comparable_products: []
last_updated: YYYY-MM-DD
extraction_confidence:
  elevator_pitch: extracted | inferred | needs_review
  audience: extracted | inferred | needs_review
  problem: extracted | inferred | needs_review
  hero_flow: extracted | inferred | needs_review
---
```

Body: narrative description synthesizing what was found.

**`_concept/1_discovery/1_overview/goals.md`** — success criteria from README, milestones, known constraints.

**`_concept/1_discovery/1_overview/comparable.md`** — products mentioned in the README. If none found,
note that comparables were not documented. Never fabricate comparables.

### Step 4: Tech Stack Detection (scope: techstack)

**Goal:** Produce `3_blueprint/1_techstack/stack.md`.

Read:
- Root manifest and lock file
- `package.json` `dependencies` + `devDependencies`
- Framework config files (nuxt.config.ts, next.config.js, vite.config.ts, etc.)
- Dockerfile / docker-compose.yml
- CI/CD workflow files
- `.env.example` or `.env.sample`

Detect each dimension:

| Dimension | Detection Strategy |
|-----------|-------------------|
| `platform` | Dockerfile base image, CI runner, deployment config |
| `frontend` | `nuxt`, `next`, `remix`, `astro`, `sveltekit`, `vite`+`react`/`vue` in deps |
| `ui_library` | `@nuxt/ui`, `shadcn-ui`, `@radix-ui`, `primevue`, `vuetify`, `mantine`, `chakra-ui`, `tailwindcss` |
| `backend` | `express`, `fastify`, `hono`, `koa`, `django`, `fastapi`, `flask`, `rails`, `laravel` |
| `database` | `pg`, `mysql2`, `@prisma/client`, `mongoose`, `drizzle-orm`, `sqlalchemy`, `sequelize` |
| `auth` | `next-auth`, `lucia`, `passport`, `@auth0/nextjs-auth0`, `better-auth`, `supabase` |
| `hosting` | vercel.json, netlify.toml, railway.json, fly.toml, deploy CI step |
| `package_manager` | `bun.lockb` → bun, `pnpm-lock.yaml` → pnpm, `yarn.lock` → yarn, else npm |

Write:

**`_concept/3_blueprint/1_techstack/stack.md`**
```yaml
---
platform: "<detected>"
frontend: "<detected>"
ui_library: "<detected>"
backend: "<detected>"
database: "<detected>"
auth: "<detected>"
hosting: "<detected>"
package_manager: "<detected>"
last_updated: YYYY-MM-DD
extraction_confidence:
  # extracted | inferred | needs_review per field
---
```

Body: notes on version constraints, known tradeoffs, or unusual combinations observed.

### Step 5: Feature Extraction (scope: features)

**Goal:** Produce `2_experience/2_features/<NN_group>/<feature>.md` files.

Feature extraction is a two-pass process: route discovery → behavioral inference.

**Pass 1: Route/Endpoint Discovery**

Scan for route definitions in this order of priority:
1. **Router files** — `src/router/`, `app/router.ts`, Next.js `app/` or `pages/`, Nuxt `pages/`, Rails `routes.rb`, Django `urls.py`, FastAPI router registrations
2. **API route handlers** — `server/api/`, `api/`, `routes/`, `controllers/`, Express `app.get(...)` calls
3. **Navigation components** — sidebar, navbar, breadcrumb components that reference route names

**Pass 2: Behavioral Inference**

For each route/page component, read the file and infer:
- What the user can do on this screen / at this endpoint
- What data it reads and writes
- What roles/guards protect it (auth middleware, route guards, permission checks)

> **IMPORTANT — Feature files describe user-facing behavior, not backend implementation.**
> Routes and endpoints discovered in Pass 1 are evidence for inferring features, not
> content to copy into feature files. Do NOT include API route paths, server-side
> class/method names, file system paths, internal event types, or framework-specific
> implementation details in feature file content or requirements.
> Express requirements as what the user can do, not how the server does it.

**Feature Grouping**

Group routes into feature groups using URL prefix or domain:
- `/auth/*`, `/login`, `/signup` → `01_user_auth/`
- `/dashboard`, `/home`, `/` → `02_dashboard/`
- `/tasks/*`, `/todos/*` → `03_tasks/`

Assign sequential two-digit prefixes: `01_`, `02_`, `03_`…

Write one `.md` per logical feature within each group:

**`_concept/2_experience/2_features/<NN_group>/<feature>.md`**
```yaml
---
priority: must-have
roles: []
screens: []
data_entities: []
last_updated: YYYY-MM-DD
extraction_confidence: extracted | inferred | needs_review
source_files:
  - "<relative path to route/component that evidences this feature>"
---
```

Body: what the feature does, what the user can accomplish, notable behaviors observed
in the source.

EMIT  [reverse-engineer] checkpoint phase=features_extracted groups=<N> features=<N> needs_review=<N>

### Step 6: Data Model Extraction (scope: datamodel)

**Goal:** Produce `3_blueprint/3_datamodel/model.dbml`, `model.json`, `seed.json`.

Read in priority order:
1. **Prisma schema** — `prisma/schema.prisma`
2. **Drizzle schema** — `db/schema.ts`, `src/db/*.ts`
3. **TypeORM entities** — `src/entities/`, `src/models/` decorated classes
4. **Mongoose models** — `models/*.ts`, `src/models/*.ts` with `new Schema({...})`
5. **SQL migrations** — `migrations/`, `db/migrations/` — most recent migration per table
6. **SQLAlchemy models** — `models.py`, `app/models/*.py`
7. **TypeScript interfaces** — `src/types/`, `shared/types/`, if no ORM is found
8. **GraphQL schema** — `schema.graphql`, `src/schema.ts`

For each entity/model found:
- Extract field names and types
- Map framework types → semantic types (see `skaileup-shared/contracts/semantic_types.md`):
  - `String` → `string`, `Int`/`Float` → `number`, `Boolean` → `boolean`
  - `DateTime` → `datetime`, `Json`/`jsonb` → `json`, `@id` fields → `uuid`
  - `@relation` → `relation`, `String @db.Text` → `richtext`, file fields → `image`/`file`
  - Enums → `enum` with extracted values
- Extract relationships (foreign keys, `@relation`, `hasMany`, `belongsTo`)
- Note unique constraints, nullable status, default values

Write:
- **`_concept/3_blueprint/3_datamodel/model.dbml`** — using DBML syntax with semantic types
- **`_concept/3_blueprint/3_datamodel/model.json`** — using the editor-native format
- **`_concept/3_blueprint/3_datamodel/seed.json`** — four standard scenarios (`empty`,
  `single_user`, `populated`, `edge_cases`). If the repo has fixture/seed/factory files,
  use their data as the `populated` scenario.

Update feature frontmatter `data_entities[]` via the feedback loop pattern for any
features already written in Step 5.

EMIT  [reverse-engineer] checkpoint phase=datamodel_extracted entities=<N> relationships=<N> enums=<N> source=<orm_type>

### Step 7: Brand / Visual Extraction (scope: brand)

**Goal:** Produce `1_discovery/2_brand/identity.md` and `1_discovery/2_brand/tokens.json`.

Read:
- `tailwind.config.ts` / `tailwind.config.js` — `theme.extend.colors`, fonts
- CSS custom property files (`tokens.css`, `variables.css`, `globals.css`)
- Design token files (`tokens.json`, `design-tokens.json`, `style-dictionary/`)
- Theme provider files (`ThemeProvider.tsx`, `theme.ts`)
- `nuxt.config.ts` → `@nuxt/ui` color config
- `app.vue` / `_app.tsx` / root layout for global styles

Extract:
- **Color palette** — primary, secondary, accent, neutral, semantic (success/error/warning/info)
- **Typography** — font families (heading, body, mono), scale if defined
- **Border radius** — design language (sharp, moderate, rounded, pill)
- **Dark mode support** — `darkMode` config present and strategy

Write:

**`_concept/1_discovery/2_brand/identity.md`**
```yaml
---
mood: []
mode: light | dark | both
last_updated: YYYY-MM-DD
extraction_confidence: extracted | inferred | needs_review
---
```

Body: describe the visual character inferred from the color palette, typography, and
component style.

**`_concept/1_discovery/2_brand/tokens.json`**
```json
{
  "colors": {
    "primary": "<hex>",
    "secondary": "<hex>",
    "accent": "<hex>",
    "background": "<hex>",
    "surface": "<hex>",
    "text": "<hex>",
    "border": "<hex>",
    "success": "<hex>",
    "warning": "<hex>",
    "error": "<hex>"
  },
  "typography": {
    "font_heading": "<family>",
    "font_body": "<family>",
    "font_mono": "<family>"
  },
  "radii": {
    "default": "<value>"
  },
  "_extraction": {
    "confidence": "extracted | inferred | needs_review",
    "source": "<file that was read>"
  }
}
```

If no design tokens are found, write a `needs_review` skeleton with empty values.
Never invent a color palette from scratch — mark it `needs_review`.

### Step 8: Screen Extraction (scope: screens)

**Goal:** Produce `2_experience/3_screens/<NN_group>/<screen>.md` files.

Use the same route groups established in Step 5. For each route, read the
corresponding page/view component:

- **Nuxt:** `pages/**/*.vue`, `layouts/`
- **Next.js:** `app/**/page.tsx`, `pages/**/*.tsx`
- **Vue Router:** `views/`, `src/pages/`
- **React Router:** `src/routes/`, `src/pages/`
- **Django:** template files (`templates/**/*.html`)
- **Rails:** `app/views/**/*.erb`

For each screen, extract:
- **Layout** — which layout wrapper/template it uses
- **Data bindings** — what entities the component fetches
- **Interactions** — user actions visible in the component (buttons, forms, links)
- **States** — loading, empty, error, populated (look for conditional rendering)
- **Cross-references** — which features this screen implements

Write:

**`_concept/2_experience/3_screens/<NN_group>/<screen>.md`**
```yaml
---
implements: []
data_entities: []
layout: default | shell | auth | blank
last_updated: YYYY-MM-DD
extraction_confidence: extracted | inferred | needs_review
source_files:
  - "<relative path to component file>"
---
```

Body: description of screen purpose, key UI sections, primary interactions, and visible states.

After writing all screens, run the feedback loop:
- For each screen, add its path to the `screens[]` array in the matching feature files

### Step 9: Confidence Report

Present a summary table to the user:

```
## Reverse Engineering Report

Repository: <repo_path>
Run ID: <uuid>

### Artifacts Generated

| Artifact                         | Files | Extracted | Inferred | Needs Review |
|----------------------------------|-------|-----------|----------|--------------|
| 1_discovery/1_overview/          | 3     | N         | N        | N            |
| 3_blueprint/1_techstack/stack.md | 1     | N         | N        | N            |
| 2_experience/2_features/         | N     | N         | N        | N            |
| 3_blueprint/3_datamodel/         | 3     | N         | N        | N            |
| 1_discovery/2_brand/             | 2     | N         | N        | N            |
| 2_experience/3_screens/          | N     | N         | N        | N            |

### Fields Needing Human Review
List any `needs_review` fields here with the path and what information is missing.

### Confidence Notes
Note any unusual or ambiguous findings (e.g., multiple ORMs detected, no README found,
routes inferred from components rather than a router file).
```

EMIT  [reverse-engineer] completed run_id=<uuid> artifacts_written=<N> needs_review_fields=<N>

## Outputs

| File | Description |
|------|-------------|
| `_concept/1_discovery/1_overview/brief.md` | Project vision, audience, problem, hero flow |
| `_concept/1_discovery/1_overview/goals.md` | Goals, constraints, deadlines inferred from docs |
| `_concept/1_discovery/1_overview/comparable.md` | Products mentioned in README / docs |
| `_concept/3_blueprint/1_techstack/stack.md` | Tech stack detected from manifests and config |
| `_concept/2_experience/2_features/<NN>/<feature>.md` | Features inferred from routes and components |
| `_concept/3_blueprint/3_datamodel/model.dbml` | Data model extracted from ORM/schema files |
| `_concept/3_blueprint/3_datamodel/model.json` | Editor-native data model format |
| `_concept/3_blueprint/3_datamodel/seed.json` | Scenario seed data (4 standard scenarios) |
| `_concept/1_discovery/2_brand/identity.md` | Brand character extracted from CSS/theme |
| `_concept/1_discovery/2_brand/tokens.json` | Design tokens extracted from config |
| `_concept/2_experience/3_screens/<NN>/<screen>.md` | Screen specs extracted from page components |

## Common Mistakes

| Rationalization | Reality |
|----------------|---------|
| "The README is sparse, I'll infer a rich brief from the code" | Only infer what the code directly implies. Mark the rest `needs_review`. Never fabricate motivation, audience, or purpose. |
| "I'll skip brand if there's no design system" | Always write `1_discovery/2_brand/tokens.json` — even if all fields are `needs_review`. Downstream skills require it to exist. |
| "I'll generate seed data from the test fixtures" | Yes — use fixture data for the `populated` scenario. But also generate the other three required scenarios (`empty`, `single_user`, `edge_cases`). |
| "The entities in the ORM map 1:1 to features" | Features and entities are not 1:1. Group entities by the user-facing feature they serve. Infrastructure entities (sessions, audit logs) belong to their closest functional feature. |
| "I can skip screens — the features already cover the routes" | Screens are separate artifacts. Features describe intent; screens describe layout, data binding, and states. Both are required for downstream design and testing skills. |
| "I'll use the ORM's native types in model.dbml" | Translate to semantic types from `skaileup-shared/contracts/semantic_types.md`. Stack-specific types belong in stack translations, not the core model. |
| "There are no comparables mentioned, I'll suggest some" | Never fabricate comparables. Write "No comparables documented in repository." and mark as `needs_review`. |
| "I found all these API routes so I'll list them in the feature requirements" | Routes are evidence, not output. Feature requirements must describe user-facing behavior only. |
| "I'll include the SSE event types / EventEmitter details since they were in the code" | Strip all backend implementation details from feature files. Document what the user experiences, not how it is implemented. |
