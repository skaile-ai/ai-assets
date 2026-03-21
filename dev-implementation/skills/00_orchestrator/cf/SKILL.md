---
name: implement
description: "Generates an implementation plan from the complete concept. Use when all conceptualization is done (screens, datamodel, techstack) and you need a structured coding task list. Reads entire _concept/ and produces an implementation section in PLANS.md. Searches for prog-expert-* skills matching the tech stack."
keywords: implement, plan, coding, tasks, development, scaffold, features, tdd, engineering
subagent: true
user_inputs:
  dialog: []
  files: []
metadata:
  stage: alpha
  requires:
  - implementation-contract
---

# App Implement — Implementation Plan Generator

## Overview

Reads the entire `_concept/` folder and generates a structured implementation
plan. Each task maps to a feature, references the relevant data entities and
screen specs, and includes acceptance criteria derived from feature requirements.
Tasks are ordered by data dependency so they can be executed sequentially.

This skill does NOT write code. It produces the plan that `cf_implement_feature`
follows to build each feature.

## When to Use

- All concept phases are complete (features, datamodel, screens, techstack)
- You are ready to transition from concept design to code implementation
- You want a structured, dependency-ordered task list before writing any code
- The project has been bootstrapped (`cf_implement_bootstrap`) or you want to plan before bootstrapping

## When NOT to Use

- Concept phases are incomplete — finish the pipeline first
- You want to implement a single feature (use `cf_implement_feature` instead)
- You want to audit existing code (use `cf_quality_audit` instead)
- You only need mockups (use `cf_concept_mock`)

## Prerequisites

| Artifact | Path | Missing? Run | Gate |
|----------|------|-------------|------|
| Scaffolded project | project directory exists | `cf_implement_bootstrap` | recommended |
| Project brief | `_concept/01_project/brief.md` | `cf_concept_overview` | <HARD-GATE> |
| Features | `_concept/03_features/**/*.md` | `cf_concept_functionality_features` | <HARD-GATE> |
| Tech stack | `_concept/05_techstack/stack.md` | `cf_concept_techstack` | <HARD-GATE> |
| Data model | `_concept/06_datamodel/model.json` | `cf_concept_datamodel` | <HARD-GATE> |
| Data model (DBML) | `_concept/06_datamodel/model.dbml` | `cf_concept_datamodel` | <HARD-GATE> |
| Screens | `_concept/07_screens/**/*.md` | `cf_concept_ui_screens` | <HARD-GATE> |
| Brand tokens | `_concept/04_brand/tokens.json` | `cf_concept_brand_visual` | recommended |
| Seed data | `_concept/06_datamodel/seed.json` | `cf_concept_datamodel` | recommended |
| Behavioral specs | `_concept/03b_behavior/*.allium` | `cf_concept_functionality_behaviors` | optional |
| Component inventory | `_concept/07_screens/components/*.md` | `cf_concept_ui_components` | optional |

If any <HARD-GATE> artifact is missing, stop immediately and name the prerequisite skill.

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** Project brief (`01_project/brief.md`), features (`03_features/**/*.md`), tech stack (`05_techstack/stack.md`), data model (`06_datamodel/model.json`, `06_datamodel/model.dbml`), screens (`07_screens/**/*.md`)
**If gates fail:** Run `cf_concept_overview`, `cf_concept_functionality_features`, `cf_concept_techstack`, `cf_concept_datamodel`, or `cf_concept_ui_screens` as needed
**On completion:** Present summary, then suggest next steps.

## Context Budget

| Source | Priority | Token estimate |
|--------|----------|---------------|
| `01_project/brief.md` | must read | ~500 |
| `03_features/**/*.md` | must read (all) | ~3000 |
| `05_techstack/stack.md` | must read | ~800 |
| `06_datamodel/model.json` | must read | ~2000 |
| `06_datamodel/model.dbml` | must read | ~1500 |
| `07_screens/**/*.md` | must read (all) | ~4000 |
| `04_brand/tokens.json` | read if exists | ~500 |
| `06_datamodel/seed.json` | skim | ~1000 |
| `03b_behavior/*.allium` | read if exists | ~2000 |
| `07_screens/components/*.md` | read if exists | ~1500 |

**Total budget:** ~17000 tokens input. Plan output ~3000-5000 tokens.

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid paths and dependency flow
- `cf__shared/plans.md` — PLANS.md format (implementation plan section)
- `cf__shared/semantic_types.md` — type translation table (needed for migration tasks)
- `cf__shared/frontmatter.md` — status lifecycle
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Workflow

### Step 1: Read Entire _concept/

Read all artifacts in dependency order:
1. `01_project/brief.md` — understand scope
2. `05_techstack/stack.md` — understand target stack
3. `03_features/**/*.md` — enumerate all features and requirements
4. `03b_behavior/*.allium` — if present, understand state machines
5. `06_datamodel/model.json` + `model.dbml` — understand entities and relationships
6. `06_datamodel/seed.json` — understand test scenarios
7. `04_brand/tokens.json` — if present, note theme setup needs
8. `07_screens/**/*.md` — understand every screen and its components
9. `07_screens/components/*.md` — if present, note shared component specs

### Step 2: Search for Expert Skills

Search for `prog-expert-*` skills that match the tech stack. For example:
- Nuxt stack → look for `prog-expert-nuxt`, `prog-expert-vue`
- Next.js stack → look for `prog-expert-nextjs`, `prog-expert-react`
- Directus backend → look for `prog-expert-directus`
- Prisma ORM → look for `prog-expert-prisma`
- PrimeVue UI → look for `prog-expert-primevue`

Note which expert skills are available. These will be referenced in
individual task instructions so `cf_implement_feature` can invoke them.

### Step 3: Build Dependency Graph

Analyze entity relationships from `model.json` to determine implementation order:

1. **Foundation entities** — no foreign keys to other app entities (e.g., user, role)
2. **Dependent entities** — reference foundation entities (e.g., task → user)
3. **Junction entities** — many-to-many bridges (e.g., task_tag)

Features are ordered so that a feature's data dependencies are always
implemented before the feature itself.

### Step 4: Generate Task List

For each feature (in dependency order), create a task:

```markdown
### Task: <feature_group>/<feature_name>

**Feature:** `03_features/<NN_group>/<feature>.md`
**Screen(s):** `07_screens/<NN_group>/<screen>.md` [, ...]
**Data entities:** [entity1, entity2]
**Expert skills:** [prog-expert-nuxt, prog-expert-primevue]
**Priority:** must-have | nice-to-have

#### Files to create/modify
- `server/api/<entity>/index.get.ts` — list endpoint
- `server/api/<entity>/[id].get.ts` — detail endpoint
- `server/api/<entity>/index.post.ts` — create endpoint
- `app/pages/<route>.vue` — screen implementation
- `app/components/<Component>.vue` — shared components used
- `tests/<feature>.test.ts` — feature tests

#### Acceptance criteria
(Derived from feature requirements and screen states)
- [ ] User can <action> from <screen>
- [ ] <entity> is persisted to database
- [ ] Error states render correctly
- [ ] Empty state shows correct message
- [ ] Loading state shows skeleton/spinner

#### Data dependencies
- Requires: [entity1 migration, entity2 migration]
- Seed scenario: populated (for development), edge_cases (for testing)
```

### Step 5: Add Infrastructure Tasks

Prepend infrastructure tasks before feature tasks:

1. **Database setup + migrations** — create migration files from `model.dbml`
   (references `cf_implement_migrate` skill)
2. **Seed data** — generate seed scripts from `seed.json`
   (references `cf_implement_seed` skill)
3. **Auth setup** — if auth features exist, configure auth provider from `stack.md`
4. **Theme/brand setup** — if `tokens.json` exists, generate CSS variables / theme config
5. **Layout shell** — implement `00_layout/shell.md` (navigation, sidebar, header)

### Step 6: Write Implementation Plan

Append to (or create) `PLANS.md` following the format in `cf__shared/plans.md`.

The implementation plan section includes:
- Scope (what is built, what is out of scope)
- Source artifacts (paths to all concept files)
- Progress checklist (infrastructure + feature tasks, unchecked)
- Available expert skills
- Known technical debt (if any)
- Verification checklist

### Step 7: Present Plan

Show the full task list with dependency order:

```
## Implementation Plan: <App Name>

Infrastructure:
1. [ ] Database migrations (N entities, M relationships)
2. [ ] Seed data (4 scenarios)
3. [ ] Auth configuration
4. [ ] Theme setup from brand tokens
5. [ ] Layout shell (navigation, sidebar)

Features (dependency order):
6. [ ] 01_user_auth/login — User, Session entities
7. [ ] 01_user_auth/registration — User entity
8. [ ] 02_dashboard/overview — Task, User entities (depends on #6)
9. [ ] 03_tasks/task_list — Task entity (depends on #8)
...

Expert skills available: prog-expert-nuxt, prog-expert-primevue
```

### Step 8: Emit Events

```
[cf_implement] started
  run_id: <uuid>
  reads: _concept/ (all)

[cf_implement] checkpoint phase=dependency_graph
  entities: N
  features: N (M must-have)
  dependency_levels: K

[cf_implement] checkpoint phase=plan_written
  tasks: N (infrastructure: I, features: F)
  expert_skills: [list]

[cf_implement] completed
  run_id: <uuid>
  artifact: PLANS.md (implementation section)
  tasks: N
```

## Outputs

| File | Purpose |
|------|---------|
| `PLANS.md` | Implementation plan section appended to project plans |

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

> "Implementation plan written to `PLANS.md`. Before writing code:
> - Run `cf_implement_bootstrap` first if the project hasn't been scaffolded yet — it reads `05_techstack/stack.md` and creates the project directory with the correct framework, config, and dependencies
>
> Then, to start building:
> - Run `cf_implement_migrate` to generate database migrations
> - Run `cf_implement_seed` to generate seed scripts
> - Run `cf_implement_feature --feature <feature_id>` for each feature in order
>
> Each feature task references the expert skills and concept artifacts it needs."

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Implementing features before migrations | Ignoring data dependencies | Always order: migrations → seed → auth → features |
| One giant task for the whole app | Not decomposing by feature | One task per feature, with explicit file list and acceptance criteria |
| Ignoring nice-to-have features | Only planning must-haves | Include nice-to-haves as separate tasks, clearly marked, at the end |
| Not searching for expert skills | Forgetting the skill ecosystem | Always search for `prog-expert-*` matching the tech stack |
| Acceptance criteria that don't match features | Writing generic criteria | Derive criteria directly from feature requirements and screen states |
| Missing error/empty/loading states | Only planning happy paths | Every screen task must include all states from the screen spec |

## Integration

- **Upstream:** reads entire `_concept/` (all phases must be complete)
- **Downstream:** consumed by `cf_implement_feature`, `cf_implement_migrate`, `cf_implement_seed`
- **Phase:** implementation planning (bridge between concept and code)
- **Pipeline position:** after all concept phases, before any code writing
- **Subagent:** yes — may search for and reference `prog-expert-*` skills
- **Called by:** orchestrator or standalone
- **Feedback loop:** none (does not modify concept files)
