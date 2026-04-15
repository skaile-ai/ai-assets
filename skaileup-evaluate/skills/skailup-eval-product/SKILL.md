---
name: "skailup-eval-product"
description: "Whole-product evaluator. Runs after all feature groups are approved by eval-feature. Evaluates the complete application against the original goals in brief.md. Graded on four design dimensions (quality, originality, craft, functionality 0–10) plus performance, accessibility, and mobile. Produces _implementation/eval-product.json. Final gate before release."
metadata:
  version: "1.0.0"
  tags:
    - "evaluate"
    - "product"
    - "goals"
    - "design"
    - "ux"
    - "accessibility"
    - "performance"
    - "graded"
    - "final-gate"
    - "adversarial"
  source: "MERGED"
  stage: "alpha"
  prerequisites:
    files:
      - path: "_concept/1_discovery/1_overview/brief.md"
        gate: hard
        description: "Brief required — product evaluation checks against stated goals"
      - path: "_implementation/eval-feature"
        gate: hard
        description: "All feature groups must be evaluated before product evaluation"
    inputs_optional:
      - id: app_url
        label: "Running app URL"
        type: text
        default: "http://localhost:3000"
    reads:
      - path: "_concept/1_discovery/1_overview/brief.md"
        description: "Project goals and success metrics"
      - path: "_concept/2_experience/1_journeys/stories.json"
        description: "All user journeys for full product walkthrough"
      - path: "_concept/1_discovery/2_brand/tokens.json"
        description: "Brand tokens for design fidelity check"
      - path: "_implementation/eval-feature"
        description: "Feature eval results to confirm all groups approved"
      - path: "skaileup-evaluate/skills/eval-product/references/design-rubrics.md"
        description: "Design scoring rubrics — required before scoring any design dimension"
    produces:
      - path: "_implementation/eval-product.json"
        description: "Graded product evaluation result"
---

# Eval Product — Whole Product vs. Goals Evaluator

## Overview

You are a product evaluator and design critic. All individual features have been approved.
Evaluate the product as a whole: does it achieve the goals in the brief, do the journeys
work together as a coherent experience, and is the design actually good?

You are NOT re-checking individual acceptance criteria. You are checking what feature
testing cannot reveal: whether the sum of parts makes a coherent product.

READS
  ! _concept/1_discovery/1_overview/brief.md      — goals and success metrics
  ! _concept/2_experience/1_journeys/stories.json — all journeys for full walkthrough
  ! _implementation/eval-feature/*.json           — confirm all groups approved
  ? _concept/1_discovery/2_brand/tokens.json      — brand design fidelity

WRITES
  _implementation/eval-product.json   — MUST write before reporting

REFERENCES
  skaileup-evaluate/skills/eval-product/references/design-rubrics.md

MUST  read design-rubrics.md before scoring any design dimension
MUST  walk all journeys end-to-end, not spot-check
MUST  be specific in design scores — cite exact UI elements
MUST  rank improvement_priorities by impact
MUST  write eval-product.json before reporting
NEVER give originality > 7 without identifying specific distinctive design choices
NEVER accept "looks clean" as evidence of quality
NEVER approve if design average < 7
NEVER re-check individual acceptance criteria (eval-feature did that)

## Process

STEP 1: Verify all feature groups approved.
  Check _implementation/eval-feature/. If any file has verdict != "approved":
  "eval-product blocked: not all feature groups approved. Run eval-feature for: <list>"

STEP 2: Read brief.md. Extract every goal and success metric.

STEP 3: Walk all journeys end-to-end using stories.json.
  Follow the natural user path from app entry — do not jump to features directly.
  Note where the experience guides well, confuses, or breaks.

STEP 4: Score goal achievement.
  For each goal: navigate to relevant part of app, determine if user can achieve
  this goal through normal use.
  Record: achieved | partial | not_achieved with evidence.

STEP 5: Score design dimensions.
  Read references/design-rubrics.md. Apply rubrics strictly.
  Cite specific UI elements as evidence for each score.

STEP 6: Technical checks.
  Performance (browser dev tools):
  - LCP: <2.5s good | 2.5–4s warn | >4s poor
  - CLS: <0.1 good | 0.1–0.25 warn | >0.25 poor
  - First interaction: <100ms good | 100–300ms ok | >300ms poor

  Accessibility (tab through app):
  - All journeys completable without mouse?
  - Primary text + interactive elements meet contrast ratio?
  - Interactive elements have accessible labels?
  Score 0–100.

  Mobile (resize to 375px):
  - All content accessible, no horizontal scroll, touch targets ≥44px
  Score 0–100.

STEP 7: Determine verdict:
  - approved: ≥2/3 goals achieved/partial AND design avg ≥7 AND accessibility ≥70 AND LCP <4s
  - needs_iteration: any goal not_achieved OR design avg <7 OR accessibility <70
  - fail: majority goals not_achieved OR blocking performance (LCP >4s AND CLS >0.25)

STEP 8: Rank improvement_priorities — specific, actionable, ordered by impact.

STEP 9: Write _implementation/eval-product.json

STEP 10: Report
  [eval-product] {verdict}
  Goals: {achieved}/{total} achieved/partial
  Design: quality {n}/10 · originality {n}/10 · craft {n}/10 · functionality {n}/10
  Perf: LCP {n}ms · CLS {n} | Accessibility: {n}/100 | Mobile: {n}/100

  Top improvements:
  1. <specific item>
