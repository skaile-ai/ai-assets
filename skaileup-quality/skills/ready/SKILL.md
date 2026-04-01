---
name: "ready"
description: "Pre-flight readiness check before E2E testing. Verifies that each feature has a concept doc, screen spec, data model entry, brand tokens, and tech stack. Surfaces gaps with remediation commands. Blocks testing of incomplete features."
metadata:
  version: "1.0.0"
  tags:
    - "readiness"
    - "preflight"
    - "checklist"
    - "testing"
    - "validation"
    - "gate"
    - "implementation"
  source: "MERGED"
  prerequisites:
    files:
      - path: "_concept/2_experience/2_features"
        gate: hard
        description: "At least one feature must exist to check readiness"
        min_entries: 1
    reads:
      - path: "_concept/2_experience/3_screens"
        description: "Screen specs for feature-to-screen coverage check"
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        description: "Data model for feature-to-model coverage check"
      - path: "_concept/3_blueprint/3_datamodel/feature_map.json"
        description: "Feature map for model-to-feature cross-reference"
      - path: "_concept/1_discovery/2_brand/tokens.json"
        description: "Brand tokens existence check (global prerequisite)"
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        description: "Tech stack existence check (global prerequisite)"
      - path: "_concept/2_experience/4_storybook/src/pages"
        description: "Storybook pages for soft mockup/composition check"
---

# Ready — Pre-flight Readiness Gate

## Overview

The **ready** skill is the Readiness Gate. It verifies that a feature or the
whole app is ready for implementation or end-to-end testing. It surfaces exactly
what is missing so the user can fix gaps efficiently.

## When to Use

- Checking if features are ready for implementation or E2E testing
- The user says "is it ready?", "pre-flight check", "readiness"
- Before running `e2e` to avoid wasting time on incomplete features
- The orchestrator dispatches this as a gate before E2E

## When NOT to Use

- You want to audit concept structure health — use **review** instead
- You want to audit source code — use **audit** instead
- No features exist yet — run **features** first

## Prerequisites

**Hard gate:** `_concept/2_experience/2_features/` must exist with at least one feature file.

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/2_experience/2_features/**/*.md` | Yes |
| **Must read** | `_concept/2_experience/3_screens/**/*.md` | Yes |
| **Must read** | `_concept/3_blueprint/3_datamodel/model.json` | Yes |
| **Must read** | `_concept/3_blueprint/3_datamodel/feature_map.json` | Yes |
| **Must read** | `_concept/1_discovery/2_brand/tokens.json` | Yes (existence check) |
| **Must read** | `_concept/3_blueprint/1_techstack/stack.md` | Yes (existence check) |
| **Optional** | `_concept/2_experience/4_storybook/` | No (mockup/composition check) |
| **Never load** | Source code, `_concept/_grounding/` | — |

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Checking only some features | Check every feature in every group — never sample. |
| Reporting "ready" when any check fails | A feature is "ready" only when ALL required checks pass. |
| Not naming the specific skill to run | Always name the exact skill that resolves each gap. |
| Blocking on optional checks | Storybook/mockup is a soft check. Do not block when absent. |
| Running fixes automatically | Only report. Let the user decide what to fix and when. |

---

ROLE  Readiness Gate — verifies features are ready for E2E testing, surfaces gaps.

READS
  _concept/2_experience/2_features/**/*.md             — feature list and frontmatter
  _concept/2_experience/3_screens/**/*.md              — screen specs (implements: field)
  _concept/3_blueprint/3_datamodel/model.json          — model definitions
  _concept/3_blueprint/3_datamodel/feature_map.json    — model-to-feature mapping
  _concept/1_discovery/2_brand/tokens.json             — brand tokens existence
  _concept/3_blueprint/1_techstack/stack.md            — tech stack existence
  ? _concept/2_experience/4_storybook/                 — storybook compositions (soft check)

WRITES
  (none — read-only audit skill, output is the report shown to user)

REFERENCES
  skaileup-shared/contracts/concept_structure.md          — expected _concept/ paths
  references/report_templates.md                     — readiness table, fix templates, check details

STEP 1: Discover features
  - Read all files in _concept/2_experience/2_features/**/*.md
  - Build feature list from discovered files (feature name + group)
  IF no feature files found
    - Stop with: "No features found in `_concept/2_experience/2_features/`. Run `features` first."

STEP 2: Check global prerequisites
  - Verify _concept/1_discovery/2_brand/tokens.json exists
  - Verify _concept/3_blueprint/1_techstack/stack.md exists
  - Record global status (brand tokens ✓/✗, tech stack ✓/✗)

STEP 3: Check each feature
  For each feature, verify all of:
  - Concept doc exists: _concept/2_experience/2_features/<group>/<feature>.md
  - Screen spec: at least one .md in _concept/2_experience/3_screens/ has this feature in `implements:`
  - Data model: feature listed in feature_map.json for at least one model
  - Mockup/composition: soft check — storybook page at _concept/2_experience/4_storybook/src/pages/ OR html file in _concept/05_mockups/ (either counts; absence is a warning, not a blocker)
  A feature is "ready" when concept doc + screen spec + data model all pass.

STEP 4: Print readiness report
  - Print readiness table (see references/report_templates.md)
  - Print global prerequisite status line
  - For each NOT-ready feature, list missing items with remediation command
  - Print verdict: "X of Y features are ready for E2E testing"

STEP 5: Verdict message
  IF all features ready
    > "All features ready. Run `e2e` with confidence."
  ELSE IF some features ready
    > "Partial readiness. Run `e2e` only for ready features, or fix gaps first."
  ELSE
    > "No features ready for E2E testing. Fix gaps above first."

MUST  check every feature — never skip or sample
MUST  show remediation command for each missing required item
NEVER  modify any _concept/ files — this is a read-only audit
NEVER  report a feature as ready when any required check fails

EMIT  [ready] started run_id=<uuid>
EMIT  [ready] completed run_id=<uuid> ready=<N> total=<M>
