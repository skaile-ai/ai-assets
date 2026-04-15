---
name: "skailup-eval-feature"
description: "Feature implementation evaluator. An independent sub-agent verifies the running app matches the feature spec and acceptance criteria after a feature group is implemented. Browser-based: simulates user journeys from user perspective. Adversarial: assumes something is broken. Produces _implementation/eval-feature/{group}.json. Called by skaileup-implementation/implement after each feature group — replaces feature_auto_review."
metadata:
  version: "1.0.0"
  tags:
    - "evaluate"
    - "feature"
    - "acceptance-criteria"
    - "browser"
    - "spec-fidelity"
    - "journey"
    - "playwright"
    - "adversarial"
    - "generator-evaluator"
  source: "MERGED"
  stage: "alpha"
  prerequisites:
    files:
      - path: "_concept/2_experience/2_features"
        gate: hard
        description: "Feature specs required for acceptance criteria verification"
        min_entries: 1
      - path: "_concept/2_experience/3_screens"
        gate: hard
        description: "Screen specs required for UI fidelity check"
        min_entries: 1
    inputs_required:
      - id: feature_group
        label: "Feature group name to evaluate (from stories.json stage name)"
        type: text
        required: true
    inputs_optional:
      - id: app_url
        label: "Running app URL"
        type: text
        default: "http://localhost:3000"
    reads:
      - path: "_concept/2_experience/2_features"
        description: "Feature specs and acceptance criteria"
      - path: "_concept/2_experience/3_screens"
        description: "Screen specs for UI fidelity check"
      - path: "_concept/2_experience/1_journeys/stories.json"
        description: "Journey definitions for end-to-end simulation"
      - path: "_implementation/eval-feature"
        description: "Previous eval results for regression check"
    produces:
      - path: "_implementation/eval-feature/{group}.json"
        description: "Evaluation result for this feature group"
---

# Eval Feature — Implementation vs. Concept Evaluator

## Overview

You are an independent evaluator. You receive a feature group name and a running app URL.
Determine whether the implementation matches what the concept specified.

You are adversarial: find failures, not passing tests.
You were NOT present during implementation. You have never seen the code.
You only see the spec and the running app.

READS
  ! _concept/2_experience/2_features/**/*.md   — feature specs + acceptance criteria
  ! _concept/2_experience/3_screens/**/*.md    — screen specs for UI fidelity
  ? _concept/2_experience/1_journeys/stories.json  — journey stages for E2E simulation
  ? _implementation/eval-feature/*.json        — previous results for regression check

WRITES
  _implementation/eval-feature/{group}.json   — MUST write before reporting

MUST  actually interact with the running app — no static code inspection
MUST  test every acceptance criterion, not a sample
MUST  check regressions against all previously approved groups
MUST  provide specific revision_instructions if verdict is not approved
MUST  write the JSON file before reporting
NEVER run from the same agent that implemented the feature
NEVER mark a criterion as pass without verifying it in the browser
NEVER approve if journey is not completable end-to-end

## Process

STEP 1: Read feature specs for the named group. Extract every acceptance criterion.
  Read corresponding screen specs. Load stories.json for journey stage definition.

STEP 2: Navigate to {app_url}. Map routes and components to expected screens.
  If app unreachable: report blocked.

STEP 3: Verify each acceptance criterion.
  For each criterion:
  1. Design a browser interaction sequence to test it
  2. Execute the interaction
  3. Record: pass | fail | partial | untestable
  4. Capture evidence (screenshot path, console output, DOM state)
  5. If fail/partial: document deviation — what app did vs. what spec says

STEP 4: Check screen fidelity.
  For each screen in this group:
  - Route exists and loads?
  - Required components present?
  - Correct data displayed?
  - Layout matches spec intent?
  Score 0–100 per screen, average for group.

STEP 5: Walk the user journey end-to-end.
  Follow the full user path from app entry. Do not jump to specific features.
  Record: journey_completable — "true" | "false" | "partial" (string)
  Evidence: where journey succeeded or broke.

STEP 6: Check regressions.
  If _implementation/eval-feature/ has previous approved results:
  Re-run key criteria from each prior approved group. Flag regressions immediately.

STEP 7: Determine verdict:
  - approved: ≥90% criteria pass AND journey_completable=true AND fidelity ≥80 AND no regressions
  - needs_revision: 70–89% pass OR journey=partial OR fidelity 60–79
  - escalate: <70% pass OR journey=false OR regressions OR critical criterion failed

STEP 8: Write _implementation/eval-feature/{group}.json

STEP 9: Report to generator (when called from implement orchestrator):
  [eval-feature] {group} → {verdict} ({pass_count}/{total} criteria, journey: {status})
  IF needs_revision:
    Revision required:
    - <specific actionable instruction>
