---
name: verify
description: "Use when implementation is complete and you need to verify the running app matches the concept specs. Navigates every screen with agent-browser and checks each feature's acceptance criteria against the live UI."
keywords: [verify, validation, acceptance, spec-check, browser, implementation, qa]
subagent: true
user_inputs:
  dialog: []
  files: []
metadata:
  stage: alpha
  requires:
  - implementation-contract
---

# App Verify — Implementation vs. Concept Validation

## Overview

Verifies that the running application matches the concept specifications. Uses
agent-browser to navigate every screen and checks each feature's acceptance
criteria against the live UI. Produces a feature x acceptance-criteria matrix
showing pass/fail status for every requirement.

## When to Use

- After `cf_test_e2e` passes — to confirm the app matches the *concept*, not just that it runs
- After implementing a batch of features — to verify fidelity to specs
- Before a release — as a final concept-compliance gate
- When the user says "verify", "does it match the spec", or "check implementation"

## When NOT to Use

- Before implementation exists — run `cf_quality_ready` first to check completeness
- For static code analysis without a running app — use `cf_quality_audit`
- For E2E testing of user journeys — use `cf_test_e2e` (tests behavior, not spec fidelity)
- For concept structure audits — use `cf_quality_review`

## Prerequisites

<HARD-GATE> Source code must exist for at least one feature. Check for `package.json`, `nuxt.config.ts`, or equivalent project file. If missing: "No application source found. Run implementation first."

<HARD-GATE> Feature specs must exist in `_concept/03_features/`. If missing: "No feature specs found. Run `cf_concept_functionality_features` first."

<HARD-GATE> Screen specs must exist in `_concept/07_screens/`. If missing: "No screen specs found. Run `cf_concept_ui_screens` first."

<HARD-GATE> Platform must be Linux or macOS (`uname -s`). agent-browser requires it.

<HARD-GATE> agent-browser must be installed:
```bash
agent-browser --version || (npm install -g agent-browser && agent-browser install --with-deps)
```

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid _concept/ paths
- `cf__shared/frontmatter.md` — status lifecycle, required fields
- `cf__shared/feedback_loop.md` — cross-reference protocol
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
| Source code (routes, components) | ~3000 | Required |
| `_concept/04_brand/tokens.json` | ~500 | Optional |

## Standalone Mode

This skill can be invoked directly without the orchestrator.
**Gate check:** Source code must exist, `_concept/03_features/` must exist, `_concept/07_screens/` must exist, platform must be Linux or macOS, agent-browser must be installed
**If gates fail:** Run cf_implement_feature (for source code), cf_concept_functionality_features (for features), cf_concept_ui_screens (for screens)
**On completion:** Present summary, then suggest next steps (fix failing items or proceed to release).

## Workflow

### Phase 1: Build Verification Matrix

Launch two sub-agents simultaneously:

#### Sub-agent 1: Concept Inventory

Read all `_concept/` artifacts and build the verification matrix:

1. Read `_concept/03_features/**/*.md` — extract every requirement checkbox and success criterion
2. Read `_concept/07_screens/**/*.md` — extract routes, component inventories, states, template data
3. Read `_concept/06_datamodel/model.json` — entity definitions for data validation
4. Read `_concept/06_datamodel/seed.json` — scenario data for testing different states
5. Cross-reference features to screens via `screens:` and `implements:` fields

Produce a matrix:

```
| Feature | Requirement | Screen | Route | Check Method |
|---------|------------|--------|-------|-------------|
| Login | Email + password form | login.md | /login | Visual + interaction |
| Login | Error on invalid creds | login.md | /login | Interaction + message |
| Dashboard | Shows user stats | overview.md | /dashboard | Visual + data |
```

#### Sub-agent 2: Application Inventory

Read the source code to understand:
1. Available routes and pages
2. Component structure
3. API endpoints
4. Dev server startup command and port
5. Auth setup (`.env.example`, auth feature docs)

Return: startup guide, route map, component tree.

### Phase 2: Start Application

1. Install dependencies
2. Start dev server in background
3. Wait for ready
4. `agent-browser open <url>` and confirm initial load
5. Screenshot: `verify-screenshots/00-initial-load.png`

### Phase 3: Verify Each Feature

For each feature in the verification matrix:

#### 3a: Navigate to Screen
Use agent-browser to navigate to the route specified in the screen spec.
Screenshot: `verify-screenshots/<feature_group>/<screen>-loaded.png`

#### 3b: Check Component Inventory
Compare visible UI elements against the screen spec's component inventory.
Mark each component as PRESENT / MISSING / DEVIATED.

#### 3c: Check Screen States
Use seed.json scenarios to test each state documented in the screen spec:
- `populated` — does the screen render correctly with data?
- `empty` — does the empty state match the spec?
- `edge_cases` — do overflow/boundary values display correctly?

#### 3d: Verify Acceptance Criteria
For each requirement checkbox in the feature spec:
- Perform the interaction described
- Check the expected outcome
- Screenshot the result
- Mark PASS / FAIL / PARTIAL

#### 3e: Check Brand Compliance (if tokens.json exists)
- Verify primary colors are used correctly
- Check font usage
- Verify spacing and border-radius conventions

### Phase 4: Cleanup

Stop dev server, close browser session.

### Phase 5: Generate Verification Report

```
## Verification Report

### Summary
Features Verified: N/N
Requirements Passed: N/N (NN%)
Components Present: N/N (NN%)

### Feature × Acceptance Criteria Matrix

| Feature | Requirement | Status | Evidence |
|---------|------------|--------|----------|
| Login | Email + password form | PASS | verify-screenshots/01_user_auth/login-form.png |
| Login | Error on invalid creds | FAIL | No error message shown |
| Dashboard | Shows user stats | PARTIAL | Stats present but count wrong |

### Deviations
| Screen | Spec Says | App Shows | Severity |
|--------|-----------|-----------|----------|
| /dashboard | 4 stat cards | 3 stat cards | MEDIUM |

### Missing Components
| Screen | Component | Spec Reference |
|--------|-----------|---------------|
| /settings | Theme toggle | 07_screens/03_settings/preferences.md |

Screenshots: verify-screenshots/
```

## Outputs

- `verify-screenshots/` — organized by feature group
- Verification report (displayed to user)
- Optional export to `verify-report.md`

## Completion Summary

Present to user: files produced (verify-screenshots/, verification report, optionally verify-report.md), key decisions made (pass/fail/partial per feature requirement, deviations detected), suggested next steps (which skills are now unblocked — fix failing items, update concept specs for spec drift, or proceed to release if all pass).

### Update Feature Status (Feedback Loop)

For every feature where ALL requirements pass, update frontmatter:

```
[cf_quality_verify] feedback_loop updated 03_features/01_user_auth/login.md
  set impl_status: tested
```

Features with any FAIL remain at their current impl_status.

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Testing behavior instead of spec fidelity | Confusing verify with E2E | Check each requirement against the feature doc, not general functionality |
| Skipping empty/edge states | Only testing populated scenario | Use all seed.json scenarios per screen spec |
| Marking PARTIAL as PASS | Being lenient on deviations | PARTIAL means the spec is not fully met — document the gap |
| Modifying specs to match implementation | Hiding deviations | Report deviations and let the user decide direction |
| Running without screen specs | Feature specs alone are not enough | Require 07_screens/ — that's where routes and components live |

## Integration

- **Upstream:** `cf_test_e2e` (behavioral tests pass), `cf_concept_ui_screens` (specs exist), implementation (code exists)
- **Downstream:** Feature impl_status updates to `tested`, feeds into release readiness
- **Parallel with:** `cf_quality_audit` (static analysis complements runtime verification)
- **Events:**
  ```
  [cf_quality_verify] started
    run_id: <uuid>
  [cf_quality_verify] checkpoint feature=login status=pass requirements=5/5
  [cf_quality_verify] checkpoint feature=dashboard status=partial requirements=3/4
  [cf_quality_verify] feedback_loop updated 03_features/01_user_auth/login.md set impl_status: tested
  [cf_quality_verify] completed
    run_id: <uuid>
    features_verified: 7
    pass: 5, partial: 1, fail: 1
  ```
