---
name: "verify"
description: "Full-stack verification gate. Runs the complete E2E test suite, visual regression checks, lint, type checking, Storybook, and browser walkthrough. Also verifies the running app matches concept specs (feature × acceptance criteria matrix). Produces full-verification.json. Run after all features are implemented."
metadata:
  version: "1.0.0"
  tags:
    - "verify"
    - "validation"
    - "e2e"
    - "acceptance"
    - "spec-check"
    - "browser"
    - "implementation"
    - "qa"
    - "build"
    - "lint"
  source: "MERGED"
  prerequisites:
    files:
      - path: "_implementation/progress.json"
        gate: hard
        description: "All features must be approved in progress.json before full verification"
      - path: "_concept/2_experience/2_features"
        gate: hard
        description: "Feature specs required for acceptance criteria matrix"
        min_entries: 1
      - path: "_concept/2_experience/3_screens"
        gate: hard
        description: "Screen specs required for spec-fidelity checks"
        min_entries: 1
    reads:
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        description: "Data model for data validation checks"
      - path: "_concept/3_blueprint/3_datamodel/seed.json"
        description: "Seed data for test fixture population"
      - path: "_concept/1_discovery/2_brand/tokens.json"
        description: "Brand tokens for visual regression baseline"
    produces:
      - path: "_implementation/verification/reports/full-verification.json"
        description: "Complete verification report with feature × acceptance criteria matrix"
---

# Verify — Full-Stack Verification Gate

## Overview

Two verification scopes combined:

1. **Full-stack verification** (from Saxe): E2E tests, visual regression, build,
   lint, types, Storybook, and browser walkthrough
2. **Concept-spec-fidelity** (from CF): verifies the running app matches the
   concept specs — feature × acceptance criteria matrix showing pass/fail per requirement

Produces `_implementation/verification/reports/full-verification.json` and a
human-readable verification report.

## When to Use

- All features implemented and individually approved
- After `implement-feature` completes all journeys
- Before final deployment
- User says "verify", "does it match the spec", "run full verification"

## When NOT to Use

- Before implementation exists — run `implement` first
- For concept structure audits — use `review`
- For static code analysis only — use `audit`

## Prerequisites

**Hard gates:**
- `_implementation/progress.json` exists (all features approved)
- Build is currently passing (Level 1 verification)
- `_concept/2_experience/2_features/` exists
- `_concept/2_experience/3_screens/` exists

---

ROLE  Verification agent — full-stack verification gate plus concept-spec-fidelity check.

READS
  _implementation/progress.json                          — feature status (all must be approved)
  _implementation/PLANS.md                               — plan to update with results
  _concept/2_experience/2_features/**/*.md               — feature requirements and acceptance criteria
  _concept/2_experience/3_screens/**/*.md                — screen specs, routes, component inventory
  _concept/3_blueprint/3_datamodel/model.json            — entity definitions for data validation
  _concept/3_blueprint/3_datamodel/seed.json             — seed scenarios for state testing
  ? _concept/1_discovery/2_brand/tokens.json             — brand tokens for compliance check

WRITES
  _implementation/verification/reports/full-verification.json  — structured verification report
  _implementation/verification/screenshots/full-walkthrough/   — browser walkthrough evidence

REFERENCES
  dev-shared/contracts/concept_structure.md   — canonical _concept/ paths
  references/report_templates.md     — JSON template, console format, walkthrough procedure

MUST  have all features approved before running
MUST  run every check in Level 3 verification
MUST  include browser walkthrough when available (not just automated tests)
MUST  save all evidence (reports, screenshots)
MUST  present blocking issues clearly before requesting approval
NEVER approve with failing E2E tests
NEVER mark verification as passed when blocking issues exist
NEVER delete or modify test files to make tests pass

EMIT [verify] started run_id=<uuid>

# ── Workflow ──────────────────────────────────────────────────────

STEP 1: Pre-flight
  - Read progress.json — all features must be approved
  - Verify no uncommitted changes in working tree
  - Verify on `implement/<app-slug>` branch (not a feature branch)
  IF any features are not approved
    - List unapproved features and stop

# ── Part 1: Full-Stack Verification ──────────────────────────────

STEP 2: Run complete E2E suite
  - $ <stack's e2e run command>
  - Capture: total tests, passed, failed, skipped
  - Capture per-feature breakdown and failure details
  IF tests fail
    - Triage failures (regressions vs. new issues)
    - Fix regressions, re-run failing tests
    - Document remaining failures as blocking if still failing

STEP 3: Visual regression check
  - Review any snapshot/screenshot diffs
  - Categorize: expected (update baseline) or unexpected (blocking)

STEP 4: Code quality
  - $ <build command>
  - $ <lint command>
  - $ <type check command>
  IF storybook is installed
    - $ <storybook test command>
  IF backend unit tests exist
    - $ <backend test command>
  - Record results for each check

STEP 5: Browser walkthrough
  IF browser skill is available
    - Auth flow: navigate to login → authenticate → reach dashboard
    - Navigation: click every sidebar item → verify pages load
    - Feature check: for each feature group, navigate to primary screen,
      verify core functionality, check responsive behavior
    - Theme check: toggle dark mode → verify rendering
    - Error handling: trigger a known error state → verify error UI
    - Screenshots → _implementation/verification/screenshots/full-walkthrough/
  ELSE
    - Set browser_check.passed = null, note "browser skill not available — skipped"
    EMIT [verify] audit_warn check=browser reason="browser skill not available"

# ── Part 2: Concept-Spec-Fidelity Check ───────────────────────────

STEP 6: Build verification matrix
  Launch two parallel checks:

  Check A — Concept inventory:
    - Read all feature specs: extract every requirement checkbox, acceptance criteria
    - Read all screen specs: routes, component inventory, states
    - Cross-reference features → screens via screens[] and implements[]
    - Build matrix: Feature | Requirement | Screen | Route | Check method

  Check B — Application inventory:
    - Read source code: available routes, page components, API endpoints
    - Return: route map, component tree, data layer patterns

STEP 7: Verify each feature
  FOR EACH feature in the verification matrix:
    - Navigate to the feature's primary screen route
    - Check component inventory: PRESENT / MISSING / DEVIATED vs screen spec
    - Test screen states (populated, empty, edge_cases from seed scenarios)
    - For each requirement checkbox: perform interaction, check outcome, mark PASS/FAIL/PARTIAL
    IF tokens.json exists
      - Verify brand compliance: primary colors, fonts, spacing

STEP 8: Generate verification report
  - Write full-verification.json (see references/report_templates.md)
  - Determine verdict: pass (0 blocking), fail (any blocking), needs_review (judgment needed)
  OUTPUT _implementation/verification/reports/full-verification.json

STEP 9: Present results + update tracking
  - Print console summary (see references/report_templates.md)
  - Update _implementation/PLANS.md with verification results
  - Update last_updated in feature files where ALL requirements pass
  - $ git add -A && git commit -m "chore: add full verification report"
  EMIT [verify] completed verdict=<verdict> e2e_passed=<n> requirements_passed=<m> blocking=<n>

STEP 10: Final gate
  IF verdict is pass
    CHECKPOINT verification_complete
      > "Your app is fully tested and ready!
      > Everything works: [list key user journeys in plain language]
      >
      > Technical details (if interested):
      >   E2E: N/N passing, Build: pass, Storybook: N stories, Browser: pass
      >   Requirements: M/M passing
      >
      > Approve for completion?"
  ELSE
    - Present blocking issues with severity and recommended fixes
    - Do not request approval until issues are resolved

CHECKLIST
  - [ ] All E2E tests pass (or failures documented as blocking)
  - [ ] Visual regression diffs reviewed
  - [ ] Build, lint, and type checks pass
  - [ ] Storybook stories render without errors (if installed)
  - [ ] Backend unit tests pass (if any)
  - [ ] Browser walkthrough completed (or skipped with warning)
  - [ ] Concept-spec-fidelity matrix complete (all features verified)
  - [ ] full-verification.json written
  - [ ] PLANS.md updated with results
  - [ ] Verification report committed

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Only running E2E, skipping spec-fidelity | Always run both — automated tests check behavior, spec-fidelity checks intent |
| Skipping empty/edge states | Use all seed scenarios per screen spec |
| Marking PARTIAL as PASS | PARTIAL means spec is not fully met — document the gap |
| Modifying specs to match implementation | Report deviations and let user decide |
| Approving with any blocking issue | Never mark pass when blocking issues exist |

## Integration

- **Called by:** `implement` orchestrator or standalone
- **Upstream:** all features individually approved, E2E suite exists
- **Outputs:** `_implementation/verification/reports/`, feature `last_updated` updated
