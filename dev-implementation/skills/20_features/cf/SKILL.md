---
name: feature
description: "Implements a single feature end-to-end using TDD. Use when the implementation plan exists and you want to build one feature at a time. Reads feature spec, screen spec, data model, and tech stack. Searches for prog-expert-* skills for patterns and recipes. Updates feature status to 'implemented' on success."
keywords: implement, feature, tdd, code, build, test, development, engineering
subagent: true
user_inputs:
  dialog:
  - id: feature_id
    label: "Which feature should I implement? (e.g., 01_user_auth/login)"
    type: text
    required: true
  files: []
metadata:
  stage: alpha
  requires:
  - implementation-contract
---

# App Implement Feature — Single Feature Builder

## Overview

Implements a single feature end-to-end following TDD: write tests first,
implement to pass tests, then verify. Reads the implementation plan from
PLANS.md, the feature spec, screen spec(s), data model, and tech stack.
Searches for `prog-expert-*` skills for framework-specific patterns and recipes.

Updates the feature's frontmatter status to `implemented` on success.

## When to Use

- An implementation plan exists in PLANS.md (from `cf_implement`)
- Database migrations are in place (from `cf_implement_migrate`)
- You want to build one feature at a time, in dependency order
- You want TDD: test-first, then implement, then verify

## When NOT to Use

- No implementation plan exists — run `cf_implement` first
- No migrations exist — run `cf_implement_migrate` first
- You want to plan all features at once (use `cf_implement`)
- You want to audit existing code (use `cf_quality_audit`)
- Concept phases are incomplete — finish the pipeline first

## Prerequisites

| Artifact | Path | Missing? Run | Gate |
|----------|------|-------------|------|
| Implementation plan | `PLANS.md` (implementation section) | `cf_implement` | <HARD-GATE> |
| Feature spec | `_concept/03_features/<group>/<feature>.md` | `cf_concept_functionality_features` | <HARD-GATE> |
| Screen spec(s) | `_concept/07_screens/<group>/<screen>.md` | `cf_concept_ui_screens` | <HARD-GATE> |
| Data model | `_concept/06_datamodel/model.json` | `cf_concept_datamodel` | <HARD-GATE> |
| Tech stack | `_concept/05_techstack/stack.md` | `cf_concept_techstack` | <HARD-GATE> |
| Migrations | migration files for required entities | `cf_implement_migrate` | <HARD-GATE> |
| Seed data | `_concept/06_datamodel/seed.json` | `cf_concept_datamodel` | recommended |
| Brand tokens | `_concept/04_brand/tokens.json` | `cf_concept_brand_visual` | recommended |
| Behavioral specs | `_concept/03b_behavior/*.allium` | `cf_concept_functionality_behaviors` | optional |
| Component inventory | `_concept/07_screens/components/*.md` | `cf_concept_ui_components` | optional |
| Brand behavioral | `_concept/04_brand/behavioral.md` | `cf_concept_brand_behavioral` | optional |
| Copy guidelines | `_concept/04_brand/copy_guidelines.md` | `cf_concept_brand_behavioral` | optional |

If any <HARD-GATE> artifact is missing, stop immediately and name the prerequisite skill.

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** Implementation plan (`PLANS.md`), feature spec (`03_features/<group>/<feature>.md`), screen specs (`07_screens/<group>/<screen>.md`), data model (`06_datamodel/model.json`), tech stack (`05_techstack/stack.md`), migrations
**If gates fail:** Run `cf_implement`, `cf_concept_functionality_features`, `cf_concept_ui_screens`, `cf_concept_datamodel`, `cf_concept_techstack`, or `cf_implement_migrate` as needed
**On completion:** Present summary, then suggest next steps.

## Context Budget

| Source | Priority | Token estimate |
|--------|----------|---------------|
| `PLANS.md` (task for this feature) | must read | ~800 |
| `03_features/<group>/<feature>.md` | must read | ~500 |
| `07_screens/<group>/<screen>.md` | must read (all screens for this feature) | ~1500 |
| `06_datamodel/model.json` | must read (relevant entities only) | ~1000 |
| `05_techstack/stack.md` | must read | ~800 |
| `06_datamodel/seed.json` | read relevant scenarios | ~800 |
| `04_brand/tokens.json` | read if exists | ~500 |
| `04_brand/behavioral.md` | read if exists (for UI text) | ~500 |
| `04_brand/copy_guidelines.md` | read if exists (for error/empty messages) | ~500 |
| `07_screens/components/*.md` | read referenced components | ~800 |
| `03b_behavior/*.allium` | read if relevant group exists | ~800 |

**Total budget:** ~8500 tokens input. Code output varies by feature complexity.

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid paths
- `cf__shared/frontmatter.md` — status lifecycle (draft → implemented)
- `cf__shared/semantic_types.md` — if dealing with data layer
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Workflow

### Step 1: Identify the Feature

Read the `feature_id` input (e.g., `01_user_auth/login`).
Find the matching task in `PLANS.md` implementation section.
Read the feature spec at `_concept/03_features/<feature_id>.md`.

If the feature has unmet data dependencies (another feature's entities
are not yet migrated), stop:

> "Feature `<feature_id>` depends on entities from `<other_feature>`.
> Implement `<other_feature>` first, or run `cf_implement_migrate` for the missing entities."

### Step 2: Load Feature Context

Read in order:
1. **Feature spec** — requirements, success criteria, error states
2. **Screen spec(s)** — from feature's `screens:` array, or find screens
   where `implements:` includes this feature
3. **Data model** — entities listed in feature's `data_entities:` array
4. **Component specs** — any shared components referenced by the screens
5. **Behavioral spec** — if `03b_behavior/<group>.allium` exists for this
   feature group, read state machines and transition rules
6. **Brand behavioral** — if `04_brand/behavioral.md` exists, read tone
   and copy guidelines for all user-facing strings
7. **Seed data** — relevant entity scenarios for test fixtures

### Step 3: Search for Expert Skills

Search for `prog-expert-*` skills matching the tech stack:

```
# Example searches based on stack.md
prog-expert-nuxt          # Nuxt patterns, composables, server routes
prog-expert-vue           # Vue 3 composition API, reactivity
prog-expert-primevue      # PrimeVue component usage, theming
prog-expert-directus      # Directus SDK, permissions, flows
prog-expert-prisma        # Prisma client, queries, relations
prog-expert-drizzle       # Drizzle ORM patterns
```

Load relevant expert skills for framework-specific patterns, recipes,
and best practices. Follow their guidance for file structure, naming
conventions, and idiomatic code.

### Step 4: Write Tests First (TDD)

Before writing any implementation code, write tests that define the
feature's expected behavior. Derive test cases from:

- Feature **requirements** → one test per requirement
- Feature **success criteria** → positive path tests
- Feature **error states** → negative path tests
- Screen **states** → UI state tests (loading, empty, error, populated)
- Seed **edge_cases** scenario → boundary condition tests

```
# Test structure
tests/
├── <feature_group>/
│   ├── <feature>.test.ts       # unit/integration tests
│   └── <feature>.e2e.test.ts   # E2E tests (if cf_test_e2e patterns exist)
```

All tests should FAIL at this point (red phase).

### Step 5: Implement the Feature

Write implementation code to make all tests pass. Follow the file list
from the PLANS.md task. Typical files per feature:

**Data layer:**
- API endpoints / server routes for CRUD operations
- Type definitions derived from `model.json` entities

**UI layer:**
- Page component(s) matching screen spec routes
- Shared components from component inventory
- Form validation matching feature requirements

**Wiring:**
- Navigation entries (update layout shell if needed)
- Route definitions
- State management (if applicable)

**Copy and UX:**
- Use brand behavioral guidelines for all user-facing strings
- Use copy guidelines templates for errors, empty states, confirmations
- Reference brand tokens for colors, fonts, spacing

### Step 6: Verify (Green Phase)

Run the test suite. All tests written in Step 4 must pass.

If tests fail:
1. Read the failure output
2. Fix the implementation (not the tests, unless the test was wrong)
3. Re-run until green

### Step 7: Update Feature Status

Once all tests pass, update the feature frontmatter:

```yaml
# In _concept/03_features/<group>/<feature>.md
---
impl_status: implemented
last_updated: YYYY-MM-DD
---
```

Update the matching screen spec(s):

```yaml
# In _concept/07_screens/<group>/<screen>.md
---
impl_status: implemented
last_updated: YYYY-MM-DD
---
```

Check off the task in `PLANS.md`:

```markdown
- [x] <feature_group>/<feature_name> — implemented YYYY-MM-DD
```

### Step 8: Emit Events

```
[cf_implement_feature] started
  run_id: <uuid>
  feature: <feature_id>
  entities: [entity1, entity2]
  screens: [screen1, screen2]

[cf_implement_feature] checkpoint phase=tests_written
  test_files: N
  test_cases: M

[cf_implement_feature] checkpoint phase=implementation_complete
  files_created: N
  files_modified: M

[cf_implement_feature] checkpoint phase=tests_passing
  passed: N
  failed: 0

[cf_implement_feature] feedback_loop updated 03_features/<group>/<feature>.md
  impl_status: implemented

[cf_implement_feature] completed
  run_id: <uuid>
  feature: <feature_id>
  tests: N passed
  files: M created/modified
```

## Outputs

| Output | Location |
|--------|----------|
| Test files | `tests/<feature_group>/<feature>.test.ts` |
| Implementation files | per PLANS.md task file list |
| Updated feature status | `_concept/03_features/<group>/<feature>.md` |
| Updated screen status | `_concept/07_screens/<group>/<screen>.md` |
| Updated PLANS.md | task checked off |

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

> "Feature `<feature_id>` implemented. Summary:
> - Tests: N passing, 0 failing
> - Files created: [list]
> - Files modified: [list]
> - Feature impl_status: implemented
>
> Next: implement the next feature in dependency order, or run `cf_test_unit` / `cf_test_integration` for additional test coverage."

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Writing code before tests | Urgency to see results | Always write failing tests first — this is non-negotiable TDD |
| Implementing multiple features at once | Trying to be efficient | One feature at a time, in dependency order from PLANS.md |
| Ignoring expert skills | Not searching for patterns | Always search `prog-expert-*` for the tech stack — they have recipes |
| Hardcoding UI text | Not reading brand behavioral | All user-facing strings come from copy guidelines or are written in the established tone |
| Skipping error/empty states | Only implementing the happy path | Screen specs define all states — implement every one |
| Not updating feature status | Forgetting the feedback loop | Always update frontmatter to `implemented` and check off PLANS.md |
| Implementing without migrations | Data layer not ready | Verify migrations exist for all required entities before starting |
| Inventing API patterns | Not following stack conventions | Expert skills define idiomatic patterns — follow them |

## Integration

- **Upstream:** reads from `PLANS.md`, `_concept/` (feature, screens, model, brand, stack)
- **Downstream:** updates feature/screen status, PLANS.md progress
- **Phase:** implementation (code writing)
- **Pipeline position:** after `cf_implement` plan, after `cf_implement_migrate`
- **Subagent:** yes — searches for and invokes `prog-expert-*` skills
- **Called by:** orchestrator or standalone
- **Feedback loop:** updates feature status to `implemented`, checks off PLANS.md task
