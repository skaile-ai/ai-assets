---
name: "eval-concept"
description: "Concept completeness and clarity gate. An independent evaluator reviews _concept/ artifacts adversarially — assumes gaps exist and proves completeness. Checks every feature has traceable acceptance criteria, screen specs, data model coverage, and an unambiguous brief. Produces _concept/eval-concept.json. Run at end of skaileup-conceptualization before implementation begins."
metadata:
  version: "1.0.0"
  tags:
    - "evaluate"
    - "concept"
    - "completeness"
    - "clarity"
    - "gate"
    - "acceptance-criteria"
    - "traceability"
    - "adversarial"
  source: "MERGED"
  stage: "alpha"
  prerequisites:
    files:
      - path: "_concept/1_discovery/1_overview/brief.md"
        gate: hard
        description: "Project brief required"
      - path: "_concept/2_experience/2_features"
        gate: hard
        description: "At least one feature must exist"
        min_entries: 1
      - path: "_concept/2_experience/3_screens"
        gate: hard
        description: "At least one screen spec must exist"
        min_entries: 1
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        gate: hard
        description: "Data model required for entity coverage check"
    reads:
      - path: "_concept/1_discovery/1_overview/brief.md"
        description: "Project goals, problem statement, target user, success metrics"
      - path: "_concept/2_experience/1_journeys/stories.json"
        description: "User journeys for end-to-end traceability check"
      - path: "_concept/2_experience/2_features"
        description: "Feature specs with acceptance criteria"
      - path: "_concept/2_experience/3_screens"
        description: "Screen specs for feature-to-screen cross-reference check"
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        description: "Data model for entity coverage check"
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        description: "Tech stack for implementation feasibility check"
    produces:
      - path: "_concept/eval-concept.json"
        description: "Evaluation result with scores, verdict, and resolution flags"
---

# Eval Concept — Concept Completeness and Clarity Gate

## Overview

You are an independent evaluator. You were NOT present during conceptualization.
You only see the artifacts. Determine whether `_concept/` is complete and clear
enough for an implementation team to build from without ambiguity.

Approach adversarially: assume gaps exist and prove completeness.
Never infer intent. If something is not explicitly stated, it is missing.

READS
  ! _concept/1_discovery/1_overview/brief.md         — goals, target user, success metrics
  ! _concept/2_experience/2_features/**/*.md          — feature specs + acceptance criteria
  ! _concept/2_experience/3_screens/**/*.md           — screen specs
  ! _concept/3_blueprint/3_datamodel/model.json       — data model
  ? _concept/2_experience/1_journeys/stories.json     — journey traceability
  ? _concept/3_blueprint/1_techstack/stack.md         — feasibility check

WRITES
  _concept/eval-concept.json     — MUST write before reporting

MUST  read all artifacts silently before scoring
MUST  quote the exact problematic text in every flag description
MUST  provide a specific actionable resolution for every flag
MUST  write eval-concept.json before reporting to user
MUST  apply all scoring deductions listed in each dimension
NEVER infer intent — unstated means missing
NEVER approve (verdict: pass) with any blocking flags
NEVER run from the same agent that ran the conceptualization pipeline

## Evaluation Dimensions

### 1. Completeness (start at 100, apply deductions)

Required:
- brief.md has: problem statement, target user, success metrics, ≥3 goals
- Every feature has: title, description, ≥1 acceptance criterion
- Every journey in stories.json has features assigned
- Every feature appears in ≥1 screen spec
- Every screen references ≥1 feature
- model.json has entities for every feature that creates or reads persistent data
- stack.md specifies: framework, database, auth method, deployment target

Deductions:
- Missing brief section: −20 each
- Feature with no acceptance criteria: −10 each
- Orphaned feature (no screen): −5 each
- Orphaned screen (no feature): −5 each
- Missing data entity for data-creating feature: −10 each

### 2. Clarity (start at 100, apply deductions)

Requirements:
- Each acceptance criterion must be independently verifiable
- No vague criteria: "should work", "functions correctly", "looks good", "is fast"
- No undefined terms in feature descriptions or acceptance criteria
- No contradictions between features
- Brief goals must be measurable or observable

Deductions:
- Non-verifiable acceptance criterion: −5 each
- Undefined term (used without definition): −3 each
- Contradiction between features: −15 each
- Unmeasurable goal in brief: −5 each

### 3. Traceability (start at 100, apply deductions)

Every user journey must trace: brief goal → journey → features → screens → data entities.

Deductions:
- Broken link in journey chain: −15 each
- Feature unreachable from any journey: −10 each
- Screen with no traceable data source: −5 each

## Process

STEP 1: Read all artifacts silently. Do not produce output.

STEP 2: Apply scoring deductions. For each deduction, create a flag:
  ```json
  {
    "type": "missing|ambiguous|contradiction|orphan|untraceable",
    "severity": "blocking|warning",
    "location": "<exact path>",
    "description": "<quote the problematic text>",
    "resolution": "<specific action to fix>"
  }
  ```
  Blocking: missing artifacts, non-verifiable criteria, contradictions, score < 70
  Warning: minor gaps, score 70-79

STEP 3: overall_score = completeness × 0.4 + clarity × 0.35 + traceability × 0.25

STEP 4: Determine verdict:
  - pass: all three scores ≥ 80 AND no blocking flags
  - needs_resolution: any score 60–79 OR blocking flags
  - fail: any score < 60

STEP 5: Write _concept/eval-concept.json

STEP 6: Report

  IF pass:
    ✓ Concept evaluation passed (overall: <score>/100)
      Completeness: <score> · Clarity: <score> · Traceability: <score>
      Ready for implementation.

  IF needs_resolution or fail:
    ✗ Concept evaluation: <verdict> (overall: <score>/100)
      Completeness: <score> · Clarity: <score> · Traceability: <score>

      Blocking issues (<n>):
      1. [<type>] <location>
         "<quoted text>"
         → <resolution>

      Re-run eval-concept after resolving blocking issues.
