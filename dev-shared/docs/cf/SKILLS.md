# Skill Reference

Per-skill reference: what each skill reads, writes, and requires. For the pipeline overview and shared contracts, see `CLAUDE.md`.

---

## Orchestration

### `cf_orchestrator`
Drives the full pipeline. Handles user communication, onboarding, PLANS.md, subagent dispatch, and completion checks. Does not write to `_concept/` directly — it coordinates skills that do.

- **Reads:** `pipeline.json`, PLANS.md (if resuming)
- **Writes:** PLANS.md
- **Hard gates:** none (entry point)
- **Key behaviors:** profile resolution, research depth selection, parallel group execution, auto-review between phases, next-step suggestion after standalone skill completion

---

## Conceptualization Skills

### `cf_concept_overview`
Produces the project brief from a conversation with the user.

- **Reads:** nothing (entry point)
- **Writes:** `01_project/brief.md`, `01_project/goals.md`, `01_project/comparable.md`
- **Hard gates:** none
- **Unblocks:** `cf_concept_functionality_features`, `cf_concept_brand_visual`, `cf_concept_techstack`

---

### `cf_concept_reverse_engineer`
Alternative entry point for existing codebases. Reads source code and produces a full `_concept/` folder.

- **Reads:** existing project source code
- **Writes:** all `_concept/` folders
- **Hard gates:** none (alt entry point)

---

### `cf_research` _(parallel mode)_
Runs alongside any pipeline step to ground decisions in real-world data. Writes to `_grounding/` which every other skill can read.

- **Reads:** step context (brief, features, etc.)
- **Writes:** `_grounding/general/` (domain, competitors, audiences, design patterns, colors/fonts), `_grounding/{step}/` (step-specific), `_grounding/{step}/user_input.json` (persisted dialog values)
- **Hard gates:** none (mode, not a sequential step)

---

### `cf_concept_functionality_features`
Defines what the app does as a set of numbered feature groups with one markdown file per feature.

- **Reads:** `01_project/`
- **Writes:** `03_features/A_01_<group>/`, `03_features/B_02_<group>/`, …
- **Hard gates:** `01_project/brief.md` must exist
- **Unblocks:** `cf_concept_functionality_behaviors`, `cf_concept_architecture`, `cf_concept_datamodel`

---

### `cf_concept_functionality_behaviors` _(optional)_
Formalizes entity state machines as `.allium` surface files. Enriches data model and screen specs with explicit state transitions and data exposure rules.

- **Reads:** `01_project/`, `03_features/`
- **Writes:** `03b_behavior/<group>.allium`
- **Hard gates:** `03_features/` must exist

---

### `cf_concept_brand_visual`
Defines the visual identity: colors, typography, spacing, and design tokens.

- **Reads:** `01_project/`
- **Writes:** `04_brand/identity.md`, `04_brand/tokens.json`, `04_brand/references/`
- **Hard gates:** `01_project/brief.md` must exist
- **Unblocks:** `cf_concept_ui_screens`

---

### `cf_concept_brand_behavioral` _(optional)_
Defines communication tone, copy guidelines, and naming conventions.

- **Reads:** `01_project/`, `03_features/`, `04_brand/identity.md`
- **Writes:** `04_brand/behavioral.md`, `04_brand/copy_guidelines.md`
- **Hard gates:** `04_brand/identity.md` must exist

---

### `cf_concept_techstack`
Selects the technology stack with documented trade-offs.

- **Reads:** `01_project/`
- **Writes:** `05_techstack/stack.md`
- **Hard gates:** `01_project/brief.md` must exist
- **Unblocks:** `cf_concept_architecture`, `cf_concept_datamodel`

---

### `cf_concept_architecture`
Documents system architecture: services, data flows, protocols, integration points.

- **Reads:** `03_features/`, `05_techstack/stack.md`
- **Writes:** `05b_architecture/architecture.md`
- **Hard gates:** `03_features/` and `05_techstack/stack.md` must exist

---

### `cf_concept_datamodel`
Designs the data model using stack-independent semantic types. Produces DBML (human-readable), JSON (editor-native), and seed data.

- **Reads:** `03_features/`, `05_techstack/stack.md`, `05b_architecture/` _(optional)_, `03b_behavior/` _(optional)_
- **Writes:** `06_datamodel/model.dbml`, `06_datamodel/model.json`, `06_datamodel/model.schema.json`, `06_datamodel/seed.json`
- **Hard gates:** `03_features/` and `05_techstack/stack.md` must exist
- **Unblocks:** `cf_concept_ui_screens`, `cf_implement_migrate`, `cf_implement_seed`

---

### `cf_concept_ui_screens`
Produces per-screen specifications with component inventories. Every screen references brand tokens, data model entities, and feature specs.

- **Reads:** `01_project/`, `03_features/`, `04_brand/`, `05_techstack/stack.md`, `06_datamodel/model.json`, `03b_behavior/` _(optional)_, `05b_architecture/` _(optional)_
- **Writes:** `07_screens/00_layout/shell.md`, `07_screens/<NN_group>/<screen>.md`
- **Updates upstream:** `03_features/` — populates `screens[]` in feature frontmatter (feedback loop)
- **Hard gates:** `03_features/`, `04_brand/tokens.json`, `05_techstack/stack.md`, `06_datamodel/model.json` must all exist
- **Unblocks:** `cf_concept_ui_components`, `cf_concept_mock`, `cf_implement`, `cf_test_plan`

---

### `cf_concept_ui_screens_technical` _(future testing)_
Variant of `cf_concept_ui_screens` reserved for experimentation. **Not registered in `pipeline.json`** — not dispatched by the orchestrator.

---

### `cf_concept_ui_components` _(optional)_
Produces reusable component specs shared across screens.

- **Reads:** `07_screens/`, `04_brand/`, `05_techstack/stack.md`
- **Writes:** `07_screens/components/navigation.md`, `07_screens/components/data_table.md`, …
- **Hard gates:** `07_screens/` must exist

---

### `cf_concept_mock`
Generates HTML mockups from screen specs and brand tokens.

- **Reads:** `07_screens/`, `04_brand/`, `06_datamodel/`
- **Writes:** `05_mockups/<screen>.html`
- **Hard gates:** `07_screens/` and `04_brand/tokens.json` must exist

---

## Implementation Skills

### `cf_implement`
Reads the complete `_concept/` and produces a structured implementation plan in `PLANS.md`.

- **Reads:** all `_concept/` folders
- **Writes:** PLANS.md (implementation section)
- **Hard gates:** `cf_quality_ready` must pass (or user overrides)

---

### `cf_implement_bootstrap`
Scaffolds a new project from the chosen tech stack.

- **Reads:** `05_techstack/stack.md`
- **Writes:** new project scaffolding on disk
- **Hard gates:** `05_techstack/stack.md` must exist

---

### `cf_implement_feature`
Implements one feature end-to-end using TDD. Searches for `prog-expert-*` skills matching the tech stack.

- **Reads:** feature spec, screen spec, `06_datamodel/`, `05_techstack/stack.md`, `prog-expert-*` skills
- **Writes:** source code files for the feature
- **Hard gates:** PLANS.md implementation section, feature spec, screen spec must exist

---

### `cf_implement_migrate`
Generates database migrations from the data model.

- **Reads:** `06_datamodel/model.dbml`, `06_datamodel/model.json`, `05_techstack/stack.md`
- **Writes:** migration files (format depends on stack: Prisma, Directus, Drizzle, or raw SQL)
- **Hard gates:** `06_datamodel/model.dbml` and `05_techstack/stack.md` must exist

---

### `cf_implement_seed`
Generates seed scripts from the scenario-based `seed.json`.

- **Reads:** `06_datamodel/seed.json`, `05_techstack/stack.md`
- **Writes:** seed scripts for each scenario (empty, single_user, populated, edge_cases)
- **Hard gates:** `06_datamodel/seed.json` must exist

---

## Testing Skills

### `cf_test_plan`
Produces a test plan mapping feature scenarios to seed data scenarios.

- **Reads:** `03_features/`, `07_screens/`, `06_datamodel/seed.json`
- **Writes:** `08_testing/test_plan.md`
- **Hard gates:** `03_features/` and `06_datamodel/seed.json` must exist

---

### `cf_test_unit`
Generates unit test files from feature specs and existing source code patterns.

- **Reads:** source code, `03_features/`
- **Writes:** unit test files (one per feature)
- **Hard gates:** source code and `03_features/` must exist

---

### `cf_test_integration`
Generates integration tests verifying API endpoints and data flow against a real database.

- **Reads:** source code, `06_datamodel/`
- **Writes:** integration test files
- **Hard gates:** source code and `06_datamodel/` must exist

---

### `cf_test_e2e`
Runs end-to-end browser tests using `cf_tool_browser`.

- **Reads:** `_concept/`, source code, running app
- **Writes:** screenshots, test report
- **Hard gates:** app must be running; `08_testing/test_plan.md` must exist

---

## Quality Skills

### `cf_quality_review`
Scores concept health (0-100) across 6 categories and optionally auto-fixes safe issues.

- **Reads:** all `_concept/` folders
- **Writes:** `quality.json`, optional auto-fixes
- **Checks:** cross-reference integrity, stale files, orphaned entities, frontmatter compliance, numbered group alignment
- **Modes:** audit (default), gardening (`--garden`)

---

### `cf_quality_audit`
Static analysis of source code plus lightweight structure checks.

- **Reads:** source code, `_concept/`
- **Writes:** audit report

---

### `cf_quality_sync`
Repairs broken bidirectional cross-references. Shows a diff before applying any fix.

- **Reads:** all `_concept/` folders
- **Writes:** fixed cross-references in-place

---

### `cf_quality_ready`
Pre-flight readiness gate. Blocks implementation from starting until all concept artifacts are complete.

- **Reads:** all `_concept/` folders
- **Writes:** readiness checklist (no _concept/ changes)

---

### `cf_quality_verify`
Navigates the running app and checks each feature's acceptance criteria against the live UI.

- **Reads:** `_concept/`, source code, running app (via `cf_tool_browser`)
- **Writes:** verification report

---

## Standards Skills

### `cf_discover_standards` _(parallel mode)_
Analyzes an existing codebase and extracts conventions into `_standards/`.

- **Reads:** existing project source code
- **Writes:** `_standards/index.yml`, `_standards/api/`, `_standards/database/`, `_standards/ui/`, etc.
- **Hard gates:** source code must exist

---

### `cf_standards_inject`
Matches `_standards/` entries to the requesting skill by `applies_to` + keyword overlap. Called internally by skills before executing their workflow.

- **Reads:** `_standards/index.yml`, matched standard files
- **Writes:** nothing (returns matched context)

---

### `cf_standards_sync`
Bidirectional sync between project standards and reusable profiles.

- **Reads:** `_standards/`, `cf__shared/profiles.json`
- **Writes:** updated standards or profile entries

---

## Tool Skills

### `cf_tool_browser`
Browser automation CLI for agents. Used by `cf_test_e2e` and `cf_quality_verify`.

- **Capabilities:** navigate, click, fill forms, screenshot, visual regression
- **Called by:** `cf_test_e2e`, `cf_quality_verify`, or directly

---

## Shared Contracts (`cf__shared/`)

All skills must read relevant contracts before operating:

| Contract | Purpose |
|----------|---------|
| `concept_structure.md` | Canonical `_concept/` paths, naming rules, read direction |
| `pipeline.json` | Dependency graph, phases, hard_gates, user_inputs, complexity presets, routes |
| `frontmatter.md` | Required YAML fields per file type |
| `semantic_types.md` | 18 stack-independent types + translation table (Directus, Prisma, Supabase, SQL) |
| `feedback_loop.md` | Two-way cross-reference protocol: screens → features, datamodel → features |
| `golden_principles.md` | Mechanical rules: entities need id+timestamps, sequential numbering, snake_case |
| `iron_laws.md` | Non-negotiable constraints (no data model without features, no screens without brand tokens, etc.) |
| `agent_patterns.md` | Reusable patterns: read-context-first, self-collect inputs, research mode, standalone mode, subagent dispatch, expert discovery |
| `plans.md` | PLANS.md format: concept plan + implementation plan + decisions log |
| `profiles.json` | Reusable config presets (default, rapid_prototype, enterprise) with inheritance |
| `seed_data.md` | Scenario-based seed data convention: empty, single_user, populated, edge_cases |
| `skill_template.md` | Canonical SKILL.md template for creating new skills |
| `skill_testing.md` | Example fixtures + `_validation.json` format for skill self-testing |
