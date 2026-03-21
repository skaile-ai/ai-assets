---
name: test-plan
description: "Use when you need a comprehensive test plan derived from concept specs. Generates test scenarios per feature covering happy paths, error states, edge cases, and permissions, mapped to seed data scenarios."
keywords: [testing, test-plan, qa, scenarios, coverage, acceptance]
user_inputs:
  dialog:
  - id: test_scope
    label: "Test Scope"
    type: select
    options: ["must-have-only", "all-features"]
    required: true
    default: "all-features"
    hint: "Whether to generate test scenarios for must-have features only or all features"
  files: []
metadata:
  stage: alpha
  requires:
  - quality-contract
---

# App Test Plan — Concept-Driven Test Generation

## Overview

Generates a structured test plan from the concept specifications. Reads features,
screens, data model, and seed data to produce test scenarios for every feature,
covering happy paths, error states, edge cases, and permission boundaries. Output
is a single plan file at `_concept/08_testing/test_plan.md`.

## When to Use

- After features, screens, and data model are approved — to define what needs testing
- Before implementation begins — to establish acceptance criteria upfront
- Before `cf_test_e2e` — to have a structured test script to follow
- When the user says "test plan", "what should we test", or "generate test scenarios"

## When NOT to Use

- For running actual tests — use `cf_test_e2e` (browser) or `cf_quality_verify` (spec fidelity)
- For generating test code — use `cf_test_unit` or `cf_test_integration`
- For auditing existing test coverage — use `cf_quality_audit`
- When features are still in draft — wait for approval first

## Prerequisites

<HARD-GATE> Feature specs must exist in `_concept/03_features/`. If missing: "No feature specs found. Run `cf_concept_functionality_features` first."

<HARD-GATE> Screen specs must exist in `_concept/07_screens/`. If missing: "No screen specs found. Run `cf_concept_ui_screens` first."

<HARD-GATE> Data model must exist at `_concept/06_datamodel/model.json`. If missing: "No data model found. Run `cf_concept_datamodel` first."

Recommended but not blocking:
- `_concept/06_datamodel/seed.json` — enhances scenario mapping
- `_concept/03b_behavior/*.allium` — enriches permission and state-based scenarios

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** Feature specs (`03_features/`), screen specs (`07_screens/`), data model (`06_datamodel/model.json`)
**If gates fail:** Run `cf_concept_functionality_features`, `cf_concept_ui_screens`, or `cf_concept_datamodel` as needed
**On completion:** Present summary, then suggest next steps.

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid _concept/ paths
- `cf__shared/frontmatter.md` — status lifecycle, priority field
- `cf__shared/feedback_loop.md` — cross-reference protocol
- `cf__shared/seed_data.md` — scenario-based seed data convention
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

| Source | Token estimate | Priority |
|--------|---------------|----------|
| `_concept/01_project/brief.md` | ~500 | Required |
| `_concept/03_features/**/*.md` | ~3000 | Required |
| `_concept/06_datamodel/model.json` | ~2000 | Required |
| `_concept/06_datamodel/seed.json` | ~1500 | Required |
| `_concept/07_screens/**/*.md` | ~4000 | Required |
| `_concept/03b_behavior/*.allium` | ~2000 | Optional |

## Workflow

### Step 1: Read Context and Apply Scope Filter

1. Read `_concept/01_project/brief.md` — app overview
2. Read `_concept/03_features/**/*.md` — all feature specs
3. Read `_concept/07_screens/**/*.md` — all screen specs
4. Read `_concept/06_datamodel/model.json` — entities, relationships, field validations
5. Read `_concept/06_datamodel/seed.json` — named scenarios (if exists)
6. Read `_concept/03b_behavior/*.allium` — behavioral rules (if exists)

Apply `test_scope` filter:
- `must-have-only`: include only features where `priority: must-have`
- `all-features`: include all features regardless of priority

### Step 2: Generate Scenarios Per Feature

For each in-scope feature, produce four scenario categories:

#### Happy Path
What happens when everything works correctly.
- One scenario per requirement checkbox in the feature spec
- Map to `populated` seed data scenario
- Include expected UI state from screen spec

#### Error States
What happens when things go wrong.
- One scenario per error state documented in the feature spec
- Include form validation failures (derive from model.json field constraints)
- Include API error responses (network, auth, server)
- Map to `edge_cases` seed data scenario where applicable

#### Edge Cases
Boundary conditions and unusual inputs.
- Empty states (map to `empty` seed data scenario)
- Maximum length inputs (derive from model.json field types)
- Special characters in text fields
- Concurrent operations (if relevant)
- First-time user vs. returning user

#### Permissions (if roles exist)
Role-based access control scenarios.
- One scenario per role defined in feature's `roles:` field
- What each role CAN do (positive test)
- What each role CANNOT do (negative test)
- Map to `permissions` seed data scenario (if present in seed.json)
- If `.allium` files exist, derive from `facing` clauses

### Step 3: Map Scenarios to Seed Data

For each scenario, specify which seed data scenario and entities to use:

```markdown
| Scenario | Seed Scenario | Entities | Setup Notes |
|----------|--------------|----------|-------------|
| Login with valid creds | populated | user (admin@example.com) | Pre-seeded user |
| Login with invalid creds | populated | — | Use wrong password |
| Dashboard empty state | empty | — | No data seeded |
| Task list overflow | edge_cases | tasks (100 items) | Pagination test |
```

### Step 4: Calculate Coverage Summary

```
## Coverage Summary

| Feature | Happy | Error | Edge | Permissions | Total |
|---------|-------|-------|------|-------------|-------|
| Login | 3 | 4 | 2 | 2 | 11 |
| Dashboard | 5 | 2 | 3 | 1 | 11 |
| Settings | 2 | 1 | 2 | 0 | 5 |
| **Total** | **10** | **7** | **7** | **3** | **27** |
```

### Step 5: Write Test Plan

```bash
mkdir -p _concept/08_testing
```

Write `_concept/08_testing/test_plan.md`:

```yaml
---
last_updated: YYYY-MM-DD
scope: all-features  # or must-have-only
feature_count: N
scenario_count: N
seed_data_mapped: true  # or false
---
```

Structure the file:

```markdown
# Test Plan

## Scope
[must-have-only | all-features] — N features, N scenarios

## Coverage Summary
[table from Step 4]

## Feature: <Name>
### Happy Path
- [ ] **Scenario name** — Description. Route: /path. Seed: populated.
  Expected: [outcome]. Evidence: [what to check].

### Error States
- [ ] **Scenario name** — Description. Trigger: [how to cause error].
  Expected: [error message/behavior].

### Edge Cases
- [ ] **Scenario name** — Description. Seed: edge_cases.
  Expected: [outcome].

### Permissions
- [ ] **Scenario name** — Role: admin. Expected: [access granted/denied].

## Seed Data Requirements
[table from Step 3]
```

### Step 6: Emit Events

```
[cf_test_plan] started scope=all-features
  run_id: <uuid>
[cf_test_plan] checkpoint phase=scenarios_generated
  features: 7, scenarios: 42
[cf_test_plan] completed
  run_id: <uuid>
  output: _concept/08_testing/test_plan.md
```

### Step 7: Present Summary

Show the coverage summary table and ask:

> "Test plan generated with N scenarios across N features. Review the plan at
> `_concept/08_testing/test_plan.md`. Would you like to adjust scope, add
> custom scenarios, or approve?"

## Outputs

- `_concept/08_testing/test_plan.md` — the complete test plan

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

> "Test plan written to `_concept/08_testing/test_plan.md`. Next steps:
> - Run `cf_test_unit` to generate unit test code from these scenarios
> - Run `cf_test_integration` to generate integration test code
> - Run `cf_test_e2e` to execute browser-based tests following this plan"

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Writing test code instead of a plan | Confusing test-plan with test-unit | This skill produces a markdown plan, not executable tests |
| Ignoring seed.json scenarios | Not reading seed data convention | Map every scenario to a named seed data scenario |
| Only testing happy paths | Optimism bias | Require at least 1 error + 1 edge case per feature |
| Inventing requirements not in specs | Over-testing | Every scenario must trace to a feature requirement or screen spec |
| Skipping permissions when roles exist | Not checking feature frontmatter | If `roles:` has multiple entries, generate permission scenarios |
| Testing nice-to-have when scope is must-have-only | Ignoring the filter | Respect the `test_scope` user input |

## Integration

- **Upstream:** `cf_concept_functionality_features` (feature specs), `cf_concept_ui_screens` (screen specs), `cf_concept_datamodel` (model + seed)
- **Called by:** orchestrator or standalone
- **Downstream:** `cf_test_unit`, `cf_test_integration`, `cf_test_e2e` (all consume the plan)
- **Parallel with:** `cf_quality_verify` (verify checks spec fidelity, test-plan defines test scenarios)
- **Events:**
  ```
  [cf_test_plan] started scope=<scope>
    run_id: <uuid>
  [cf_test_plan] completed
    run_id: <uuid>
    features: N
    scenarios: N
  ```
