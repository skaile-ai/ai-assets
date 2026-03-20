---
name: ready
description: "Use when checking if a feature or the whole app is ready for implementation or E2E testing. Also when user says 'is it ready?', 'pre-flight check', 'readiness'."
keywords: [readiness, preflight, checklist, testing, validation, gate, implementation]
user_inputs:
  dialog: []
  files: []
---

# Ready — Pre-flight Readiness Gate

## Overview

The **ready** skill is the Readiness Gate. It verifies that a feature or the
whole app is ready for implementation or end-to-end testing. It surfaces exactly
what is missing so the user can fix gaps efficiently.

**Phase:** quality / gate
**Writes to:** readiness report (presented to user)

## When to Use

- Checking if features are ready for implementation or E2E testing
- The user says "is it ready?", "pre-flight check", "readiness"
- Before running `cf_test_e2e` to avoid wasting time on incomplete features
- The orchestrator dispatches this as a gate before implementation

## When NOT to Use

- You want to audit concept structure health — use **review** instead
- You want to audit source code — use **audit** instead
- No features exist yet — run **features** first

## Prerequisites

### HARD-GATE

`_concept/03_features/` must exist with at least one feature file. If not:

> "No features found in `_concept/03_features/`. Run the **features** skill first."

### Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — expected paths
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/03_features/**/*.md` | Yes |
| **Must read** | `_concept/07_screens/**/*.md` | Yes (to check coverage) |
| **Must read** | `_concept/06_datamodel/model.json` | Yes (to check entity coverage) |
| **Must read** | `_concept/04_brand/tokens.json` | Yes (existence check) |
| **Must read** | `_concept/05_techstack/stack.md` | Yes (existence check) |
| **Optional** | `_concept/05_mockups/` | No (mockup existence check) |
| **Never load** | Source code, `_concept/_research/` | — |

## Standalone Mode

This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/03_features/` must exist with at least one feature file
**If gates fail:** Run cf_concept_functionality_features first
**On completion:** Present summary, then suggest next steps (cf_test_e2e for ready features, or fix gaps first).

## Workflow

### Step 1: Discover Features

Read all files in `_concept/03_features/**/*.md`. Build a list from the files.

If no feature files found, stop:
> "No features found in `_concept/03_features/`. Run the **features** skill first."

### Step 2: Check Each Feature

For each feature, check:

| Check | How to verify |
|-------|---------------|
| Concept doc | `_concept/03_features/<group>/<feature>.md` exists |
| Screen spec | At least one `.md` in `_concept/07_screens/` with this feature in `implements:` |
| Data model | Feature listed in `from_features` of at least one entity in `_concept/06_datamodel/model.json` |
| Brand tokens | `_concept/04_brand/tokens.json` exists |
| Tech stack | `_concept/05_techstack/stack.md` exists |
| Mockup | At least one `.html` in `_concept/05_mockups/` linked from the feature or screen |
| Implementation | `impl_status: implemented` in feature frontmatter (if in implementation phase) |

### Step 3: Print Readiness Table

```
## Readiness Report

| Feature | Group | Screen | Data Model | Mockup | Ready? |
|---------|-------|--------|------------|--------|--------|
| login | 01_user_auth | yes | yes | yes | Yes |
| dashboard | 02_dashboard | yes | no | no | No |
| profile | 03_settings | no | no | no | No |

Global: Brand tokens yes | Tech stack yes
```

### Step 4: Recommend Fixes

```
## What to Do

### dashboard (02_dashboard)
- Data model missing — run `cf_concept_datamodel`
- Mockup missing — run `cf_concept_mock`

### profile (03_settings)
- Screen spec missing — run `cf_concept_ui_screens`
- Data model missing — run `cf_concept_datamodel`
- Mockup missing — run `cf_concept_mock`
```

### Step 5: Verdict

```
X of Y features are ready for E2E testing.
Ready: [list]
Not ready: [list]
```

If ALL ready:
> "All features ready. Run `cf_test_e2e` with confidence."

If SOME ready:
> "Partial readiness. Run `cf_test_e2e` only for ready features, or fix gaps first."

If NONE ready:
> "No features ready for E2E testing. Fix gaps above first."

### Emit Events

```
[cf_quality_ready] started
  run_id: <uuid>

[cf_quality_ready] completed
  run_id: <uuid>
  total_features: N
  ready_features: N
  not_ready_features: N
```

## Outputs

| Output | Description |
|--------|-------------|
| Readiness report | Table showing per-feature readiness with specific gaps |
| Recommended actions | Specific skills to run to fix each gap |

## Completion Summary

Present to user: files produced (readiness report), key decisions made (per-feature readiness assessment), suggested next steps (which skills are now unblocked — e.g., cf_test_e2e for ready features, or specific skills to fix gaps for non-ready features).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Checking only some features | The agent stops after the first few | Check every feature in every group. |
| Reporting "ready" when mockups are missing | The agent considers mockups optional | Mockups are part of the readiness checklist. Report as gap. |
| Not naming the specific skill to run | The agent says "fix this" without guidance | Always name the exact skill that resolves each gap. |
| Blocking on optional checks | The agent requires behavioral specs | Behavioral specs (03b_behavior) are optional. Do not block on them. |
| Running fixes automatically | The agent starts fixing gaps | Only report. Let the user decide what to fix and when. |

## Integration

- **Called by:** orchestrator or standalone
- **Reads from:** `_concept/` (all numbered folders)
- **Feeds into:** decision to run `cf_test_e2e`, `cf_implement`, or fix gaps
- **Feedback loops:** None. This is a read-only check.
