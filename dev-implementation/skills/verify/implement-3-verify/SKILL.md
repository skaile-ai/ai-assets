---
name: implement-3-verify
description: "Full-stack verification gate. This skill should be used when the user asks to 'verify the app', 'run full test suite', 'check everything works', 'pre-deployment check', 'run verification', or 'is the app ready'. Runs the complete E2E test suite, visual regression checks, lint, type checking, and produces a comprehensive verification report."
metadata:
  stage: alpha
  requires:
  - implementation-contract
---

ROLE  Verification agent — runs Level 3 full-stack verification and produces a pass/fail report.

READS
  _implementation/progress.json                — feature status (all must be approved)
  _implementation/PLANS.md                     — implementation plan to update
  _concept/2_experience/2_features/**/*.md                 — feature list for per-feature breakdown
  _concept/2_experience/3_screens/**/*.md                  — screen specs for browser walkthrough

WRITES
  _implementation/verification/reports/full-verification.json  — structured report
  _implementation/verification/screenshots/full-walkthrough/   — browser walkthrough evidence

REFERENCES
  shared/contracts/prerequisites.md              — tool prerequisite checks
  shared/contracts/verification.md               — Level 3 full verification protocol
  shared/contracts/implementation_structure.md   — where to write reports
  references/report_templates.md        — JSON template, console format, walkthrough procedure

REQUIRES
  hard: pnpm
  soft: docker (Keycloak auth flow test), agent-browser (browser walkthrough Step 5)
  state: _implementation/progress.json exists with all features approved
  state: build passing (Level 1)

# ── Workflow ──────────────────────────────────────────────────

STEP 1: Pre-flight
  - Read _implementation/progress.json — all features must be `approved`
  - Verify no uncommitted changes in working tree
  - Verify on `implement/<app-slug>` branch (not a feature branch)
  - Verify Docker services running (database, auth)
  IF any features are not approved
    - List unapproved features and stop
  EMIT [implement-3-verify] started run_id=<uuid>

STEP 2: Run complete E2E suite
  - $ cd e2e && pnpm run e2e
  - Capture: total tests, passed, failed, skipped
  - Capture per-feature breakdown
  - Capture failure details (test name, assertion, screenshot)
  IF tests fail
    - Triage failures — see references/report_templates.md § E2E failure triage
    - Fix regressions, re-run failing tests
    - If still failing: document as blocking issue
  UNTIL all E2E tests pass or failures documented as blocking

STEP 3: Visual regression check
  - $ cd e2e && pnpm run e2e:update-snapshots  # only if intentional changes
  - Review snapshot diffs — see references/report_templates.md § Visual regression triage
  - Categorize each diff as expected (update baseline) or unexpected (blocking)

STEP 4: Code quality
  - $ pnpm run build
  - $ pnpm run lint
  - $ pnpm run test:types
  - $ cd frontend && pnpm run test:storybook
  - $ cd backend && pnpm run test:jest
  - Record results for each check (pass/fail, counts, warnings)

STEP 5: Browser walkthrough
  IF agent-browser is available
    - DO browser_walkthrough
  ELSE
    - Set browser_check.passed = null, note "agent-browser not available — skipped"
    - EMIT [implement-3-verify] audit_warn check=browser reason="agent-browser not available"

STEP 6: Generate verification report
  - Write full-verification.json — see references/report_templates.md § full-verification.json
  - Determine verdict: pass (0 blocking), fail (any blocking), needs_review (judgment needed)
  OUTPUT _implementation/verification/reports/full-verification.json
    { "level": 3, "verdict": "<pass|fail|needs_review>", ... }

STEP 7: Present results
  - Print console summary — see references/report_templates.md § Console summary format
  - Update _implementation/PLANS.md with verification results
  - $ git add -A && git commit -m "chore: add full verification report"
  EMIT [implement-3-verify] completed verdict=<verdict> e2e_passed=<n> blocking=<n>

STEP 8: Final gate
  IF verdict is pass
    CHECKPOINT verification_complete
      > "Your app is fully tested and ready!
      > Everything works: [list key user journeys that passed in plain language]
      >
      > Technical details (if interested):
      >   E2E: N/N passing, Build: pass, Visual: pass, Storybook: pass
      >
      > Approve for completion? (approve / request changes / run again)"
  ELSE
    - Present blocking issues and recommendations
    - Do not request approval until issues are resolved

# ── Procedures ────────────────────────────────────────────────

PROCEDURE browser_walkthrough
  - Auth flow: navigate to login → authenticate → reach dashboard
  - Navigation: click every sidebar item → verify pages load
  - Feature check: for each feature group navigate to primary screen, verify core functionality, check responsive behavior
  - Theme check: toggle dark mode → verify rendering
  - Error handling: trigger a known error state → verify error UI
  - Take screenshots at each step → _implementation/verification/screenshots/full-walkthrough/

# ── Constraints ───────────────────────────────────────────────

MUST  have all features individually approved before running
MUST  run every check in Level 3 verification (see shared/contracts/verification.md)
MUST  include agent-browser walkthrough when available (not just automated tests)
MUST  save all evidence (reports, screenshots)
MUST  present blocking issues clearly before requesting approval

NEVER  approve with failing E2E tests
NEVER  skip the agent-browser walkthrough without flagging it
NEVER  mark verification as passed when blocking issues exist
NEVER  delete or modify test files to make tests pass

CHECKLIST
  - [ ] All E2E tests pass (or failures documented as blocking)
  - [ ] Visual regression diffs reviewed
  - [ ] Build, lint, and type checks pass
  - [ ] Storybook stories render without errors
  - [ ] Backend unit tests pass
  - [ ] Browser walkthrough completed (or skipped with warning)
  - [ ] full-verification.json written
  - [ ] PLANS.md updated with results
  - [ ] Verification report committed
