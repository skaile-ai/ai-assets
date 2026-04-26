---
name: "skailup-implement-feature"
description: "Journey-first feature orchestrator with TDD Guard. Builds features by walking user journeys outside-in (hero → vital → hygiene from stories.json), writing failing journey e2e tests first, then delegating page-by-page implementation to the implement-feature-page sub-skill. Can also implement a single feature standalone using the feature_id input."
metadata:
  version: "1.0.0"
  tags:
    - "implement"
    - "feature"
    - "tdd"
    - "journey"
    - "e2e"
    - "test-first"
    - "code"
    - "build"
    - "engineering"
  source: "MERGED"
  prerequisites:
    files:
      - path: "_concept/experience/features"
        gate: hard
        description: "Feature specs required for implementation targets"
        min_entries: 1
      - path: "_concept/experience/screens"
        gate: hard
        description: "Screen specs required for page implementation"
        min_entries: 1
      - path: "_concept/blueprint/datamodel/model.json"
        gate: hard
        description: "Data model required for entity and migration implementation"
      - path: "package.json"
        gate: hard
        description: "Dev stack must be running (project scaffolded and foundation applied)"
    inputs_optional:
      - id: feature_id
        label: "Single feature to implement? (e.g., 01_user_auth/login) — leave blank for journey-first mode"
        type: text
    reads:
      - path: "_concept/experience/journeys/stories.json"
        description: "Journey stages for hero → vital → hygiene implementation order"
      - path: "_concept/experience/4_storybook/src/pages"
        description: "Storybook page compositions as UI starting point for each page"
    produces:
      - path: "_implementation/progress.json"
        description: "Updated feature implementation status after each page completes. Each page entry tracks status: pending | in_progress | complete for resume support."
  user_inputs:
    dialog:
      - id: "feature_id"
        label: "Single feature to implement? (e.g., 01_user_auth/login) — leave blank for journey-first mode"
        type: "text"
        required: false
      - id: "auto_approve_pages"
        label: "Auto-approve per-page checkpoints? (for unattended pipeline runs)"
        type: "boolean"
        required: false
        default: false
        note: "Flow context value takes precedence if set by parent implement skill. Test-green and regression gates are never bypassed regardless of this setting."
    files: []
---

# Implement Feature — Journey-First Feature Orchestrator

## Overview

Implements features using outside-in TDD at three levels:
1. **Journey level** — write a failing capstone E2E test for the full multi-page flow
   (written before pages start as a contract; verified after all pages are individually green)
2. **Page level** — each page is a vertical slice: write failing tests → implement → green gate
   → regression gate → checkpoint → commit. Never proceed to the next page until tests pass.
3. **Feature level** — each page sub-skill implements individual features with TDD Guard

**Journey-first strategy:** features are built in user-journey order (hero →
vital → hygiene from `stories.json`). This delivers a working end-to-end flow
early and surfaces integration issues before they compound.

**TDD Guard** enforces the Red→Green cycle at the feature level:
- `initial` → only Red declarations permitted
- `writing_tests` → test files editable only
- `red` → test failed; Green declarations permitted
- `making_tests_pass` → only declared prod files editable

See `references/tdd_guard.md` for the full TDD Guard state machine.

**Standalone mode:** call with `feature_id` to implement a single feature
without journey context (useful for `add-feature` follow-through).

## When to Use

- Foundation is complete (app shell exists, auth configured)
- `stories.json` exists (journey-first) OR a specific feature is requested
- User says "implement features", "build the app features", "implement next journey"
- The `implement` orchestrator dispatches this as Phase 4

## When NOT to Use

- Foundation not complete — run `foundation` first
- No implementation plan exists — run `implement` orchestrator first
- Auditing existing code — use `audit`

## Prerequisites

**Hard gates:**
- Project is scaffolded and `foundation` phase is complete
- `_concept/experience/features/` has at least one feature group
- `_concept/experience/screens/` has screen specs
- `_concept/blueprint/datamodel/model.json` exists
- Dev stack is running (frontend + backend accessible)

**Recommended (journey mode):**
- `_concept/experience/journeys/stories.json` exists
- `_concept/experience/4_storybook/src/pages/` exists (UI reference)

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/experience/journeys/stories.json` | Journey mode |
| Must read | `_concept/experience/features/**/*.md` | Yes |
| Must read | `_concept/experience/screens/**/*.md` | Yes |
| Must read | `_concept/blueprint/datamodel/model.json` | Yes |
| Must read | `_concept/blueprint/techstack.md` | Yes |
| Read if exists | `_concept/experience/4_storybook/src/pages/` | Recommended (UI reference) |
| Read if exists | `_concept/blueprint/datamodel/seed.json` | Recommended |
| Read if exists | `_implementation/progress.json` | If resuming |

---

ROLE  Journey-first feature orchestrator — implements features by walking user journeys outside-in with three-level TDD.

READS
  _concept/experience/journeys/stories.json            — journey definitions, story maps
  _concept/experience/features/**/*.md                  — feature specs
  _concept/experience/screens/**/*.md                   — screen/page specs
  ? _concept/experience/4_storybook/src/pages/            — storybook page compositions (UI reference)
  _concept/blueprint/datamodel/model.json               — data model
  _concept/blueprint/datamodel/seed.json                — seed data scenarios
  ? _implementation/progress.json                           — resume state

WRITES
  e2e/specs/journeys/<stage>-<journey-slug>.spec.<ext>      — journey-level e2e tests
  _implementation/progress.json                             — journey/page/feature status
  _implementation/PLANS.md                                  — checked-off journeys

REFERENCES
  skaileup-shared/contracts/concept_structure.md  — canonical _concept/ paths
  references/tdd_guard.md           — TDD Guard state machine and CLI
  references/tdd_workflow.md        — E2E patterns, seed data, pitfalls

MUST  implement journeys in stage order: hero → vital → hygiene
MUST  write failing journey tests BEFORE implementing any pages
MUST  deduplicate pages across journeys — implement once, verify in each
MUST  verify page tests are GREEN (5c) before running regression tests
MUST  verify ALL tests pass (5d) before proceeding to the next page
MUST  commit after each completed page (5f)
MUST  resume from last completed page in progress.json on interrupted runs
MUST  run spec-compliance review before code-quality review after each journey
MUST  use populated scenario seed data by default
MUST  use test IDs (data-testid) for element addressing in tests
MUST  use one git branch per journey, squash-merged after approval
NEVER implement pages before journey tests exist
NEVER use screenshot assertions — use test IDs and value assertions
NEVER skip regression testing (all prior tests must still pass)
NEVER skip spec-compliance review to go straight to quality review
NEVER modify _concept/ files

# ── Standalone Mode (single feature) ──────────────────────────────

IF feature_id is provided
  - Read the specific feature spec
  - Find the matching screen spec(s)
  - Search for expert skills matching the tech stack
  - Write failing tests first (TDD RED)
  - Implement the feature (TDD GREEN)
  - Verify all tests pass
  - Update PLANS.md: check off the feature
  - Update feature's last_updated in frontmatter
  - EMIT [implement-feature] completed feature=<feature_id> tests=<N>

# ── Journey Mode (default) ────────────────────────────────────────

# ── Step 0: Verify dev stack ──────────────────────────────────────

STEP 0: Preflight
  - Verify backend is accessible (health check)
  - Verify frontend is accessible (health check)
  IF either is not running
    - STOP. Provide exact command to start the missing service.
    - Do NOT proceed until both are running.
  - Run existing E2E tests to establish clean baseline
  IF any tests fail
    - STOP. Fix failing tests before starting new work.
    MUST have passing test baseline before any implementation

# ── Step 1: Build journey → page → feature map ────────────────────

STEP 1: Build implementation map
  - Read stories.json
  - Collect story_maps, ordered by stage: hero first, then vital, then hygiene
  - Skip backlog stage
  - For each story_map (journey):
    - Collect candidate_screens from its stories
    - Resolve each to a screen spec file in _concept/experience/screens/
    - For each screen spec, find features whose frontmatter screens[] includes it
    - Deduplicate: a page/feature appears only in the FIRST journey that references it
  - Track already-implemented pages/features from progress.json (if resuming)

STEP 2: Present plan
  - Show journey order with page and feature counts
  - Note pages shared across journeys
  CHECKPOINT journey_plan
    > "Here's how I'll build your app, journey by journey:
    >
    > 1. [Hero] <journey label> — N pages, M features
    >    Pages: <list>
    > 2. [Vital] <journey label> — N pages, M features
    > ...
    >
    > Each journey gets its own end-to-end tests verifying the full flow.
    > Approve to start with the hero journey."

# ── Step 3–8: Journey loop ─────────────────────────────────────────

STEP 3: Start journey
  - Pick next unfinished journey (hero → vital → hygiene)
  $ git checkout implement/<app-slug>
  $ git checkout -b feat/<journey-slug>
  EMIT [implement-feature] journey_start journey=<id> stage=<stage> pages=<N>

STEP 4: Write capstone journey E2E test
  - Create e2e/specs/journeys/<stage>-<journey-slug>.spec.<ext>
  - Walk the FULL multi-page flow from the story map (contract-first: declares acceptance before any pages exist)
  - Use isolated backend / test fixture for data setup
  - Reset to populated scenario by default
  - One test per story in the journey (serial mode)
  - Each test: navigate, act, assert per story acceptance_criteria
  - Use test IDs (data-testid) for element addressing
  - All journey tests MUST FAIL at this point — that is expected and correct
  $ git commit -m "test: write journey capstone e2e for <journey-label>"
  NOTE: This test is verified at Step 6, NOT here. It declares intent; green gate is after all pages pass.

PATTERNS (adapt to your test framework / language):
  describe('<Journey Label>', () => {
    beforeAll(async () => { /* setup: reset DB to populated scenario */ })
    afterAll(async () => { /* teardown */ })

    test('<story title>', async () => {
      // Navigate, act, assert per story acceptance_criteria
      // await page.goto('<route>')
      // await expect(page.getByTestId('<element>')).toBeVisible()
    })
  })

STEP 5: Implement pages — vertical slice loop
  - Read progress.json to find last completed page (resume support)
  - For each page in this journey (skip if progress.json marks it complete):

    STEP 5a: Write failing tests for THIS page only
      - Write unit and/or integration tests targeting this page's features
      - If the page is view-only, static, or redirect-only (no meaningful unit tests):
          add a render/smoke test: it('renders without error', ...) and document
          the reason as a comment: // no unit tests: <reason>
      - Run tests — they MUST FAIL at this point
      $ git commit -m "test: write failing tests for page <page-name>"

    STEP 5b: Implement this page
      - RUN implement-feature-page sub-skill with:
          screen_spec path, feature specs for this page, journey context (seed data scenario)

    STEP 5c: Page green gate
      - Run page tests specifically
      MUST be GREEN before proceeding to 5d
      IF any page test is still red: diagnose and fix before continuing

    STEP 5d: Regression gate
      - Run ALL tests (all prior pages + current page)
      MUST all pass before proceeding to 5e
      IF any prior test regressed: diagnose and fix before continuing
      NEVER proceed to the next page with a failing regression

    STEP 5e: Per-page checkpoint
      IF auto_approve_pages is false (default):
        CHECKPOINT page_complete
          > "Page '<page-name>' complete.
          > Page tests: N passing. All prior tests: N passing.
          > Approve to continue to the next page."
      IF auto_approve_pages is true (flow context or dialog):
        - Skip human approval; log auto-approval in progress.json
        NOTE: 5c and 5d gates still apply regardless of auto_approve_pages

    STEP 5f: Commit and update progress
      - Update progress.json: mark page status = "complete"
      $ git commit -m "feat: implement page <page-name>"
      EMIT [implement-feature] page_complete journey=<id> page=<page-name> tests=<N>

  - Repeat for each page

STEP 6: Verify capstone journey E2E test
  - Run the journey capstone E2E test written in Step 4
  - All pages are individually green at this point — this step catches multi-page integration issues
  - Diagnose cross-page issues (navigation, shared state, data flow between pages)
  - Fix integration issues
  - Re-run ALL tests to ensure no regressions
    UNTIL journey capstone passes AND all prior tests still pass
    $ git commit -m "feat: complete journey <journey-label>"
  EMIT [implement-feature] all_tests journey=<id> total=N passed=P failed=F

STEP 6a: Spec compliance review (REQUIRED before quality review)
  - Read each feature spec in this journey against the actual code produced, line by line
  - Assume the implementer "finished suspiciously quickly" — do not trust passing tests alone
  - Verify: every requirement in the spec is present in the code, not just implied
  - Verify: acceptance criteria (EARS) are all addressable by a test
  - If any requirement is missing or misimplemented:
    - Fix it now. Do NOT proceed to quality review with an incomplete spec.
    - Re-run tests after the fix.
  - Record result: COMPLIANT | NON_COMPLIANT (with list of gaps)
  EMIT [implement-feature] spec_review journey=<id> result=<COMPLIANT|NON_COMPLIANT> gaps=<N>

STEP 6b: Code quality review (only runs after spec compliance passes)
  IF spec compliance result is NON_COMPLIANT → fix gaps, repeat STEP 6a
  ELSE
    - Check test coverage: all feature code paths have at least one test
    - Check file boundaries: no feature bleeds into another's files
    - Check naming: matches golden_principles conventions
    - Check no debugging artifacts (console.log, TODO, commented-out blocks) remain
    - If issues found: fix and re-run tests
  EMIT [implement-feature] quality_review journey=<id> result=<PASS|FAIL> issues=<N>

STEP 7: Journey checkpoint
  CHECKPOINT journey_complete
    > "Journey '<journey-label>' is complete. Users can now:
    > [describe what this journey enables in plain language]
    >
    > Tests passing: journey (N), pages (N), features (N).
    >
    > Approve to continue to the next journey."

STEP 8: Merge journey branch
  $ git checkout implement/<app-slug>
  $ git merge --squash feat/<journey-slug>
  $ git commit -m "feat: implement <stage> journey — <journey-label>"
  $ git branch -d feat/<journey-slug>
  - Update progress.json: journey → complete
  - Update PLANS.md: check off journey
  EMIT [implement-feature] journey_complete journey=<id> stage=<stage>

STEP 9: Repeat for remaining journeys
  UNTIL all journeys (hero + vital + hygiene) are complete

STEP 10: Final check
  - Run ALL E2E tests one final time
  - Run full build + lint for backend and frontend
  EMIT [implement-feature] completed journeys=<N> pages=<N> features=<N> tests=<N>

CHECKLIST
  - [ ] Journey → page → feature map derived from stories.json
  - [ ] All journeys processed in stage order (hero → vital → hygiene)
  - [ ] Journey e2e tests pass for each completed journey
  - [ ] All page and feature tests pass (no regressions)
  - [ ] Build passes (backend + frontend + lint)
  - [ ] progress.json and PLANS.md updated

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Implementing pages before journey tests | Always write failing journey tests first |
| Numeric feature-group order instead of journey order | Use stories.json stage order: hero → vital → hygiene |
| Not running regression tests after each page | Run ALL tests after each page, not just the current journey |
| Skipping spec compliance and going straight to quality | Spec compliance must pass before quality review — both are required |
| Running quality review on a misbuilt feature | Fix spec gaps first; quality review on wrong code is wasted work |
| Screenshot assertions in tests | Use data-testid + value assertions only |
| Modifying `_concept/` files | concept is read-only — update last_updated only via feedback loop |

## Integration

- **Called by:** `implement` orchestrator or standalone
- **Dispatches to:** `implement-feature-page` (per page)
- **Reads:** `_concept/experience/` (journeys, features, screens, storybook)
- **Writes:** E2E test files, `_implementation/progress.json`
