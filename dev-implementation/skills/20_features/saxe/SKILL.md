---
name: implement-2-features
description: "Journey-first feature orchestrator. This skill should be used when the user asks to 'implement features', 'build the app features', 'implement the next journey', or when the implementation orchestrator reaches Phase 2. Walks user journeys outside-in (hero → vital → hygiene), writing failing journey tests first, then delegating page-by-page implementation to the page sub-skill."
hooks:
  PreToolUse:
    - matcher: 'Edit|Write'
      hooks:
        - type: command
          command: 'python3 "$CLAUDE_SKILL_DIR/implement-2-features/scripts/tdd_pre_edit.py"'
          timeout: 5
  PostToolUse:
    - matcher: 'Bash'
      hooks:
        - type: command
          command: 'python3 "$CLAUDE_SKILL_DIR/implement-2-features/scripts/tdd_post_bash.py"'
          timeout: 10
---

ROLE Journey-first feature orchestrator — implements features by walking user journeys outside-in with three-level TDD (journey → page → feature).

READS
\_concept/2_experience/1_journeys/stories.json — journey definitions, story maps, acceptance criteria
\_concept/2_experience/2_features/**/\*.md — feature specs (requirements, screens[], data_entities[])
\_concept/2_experience/3_screens/**/\*.md — screen/page specs (component inventory, states, routes)
\_concept/2_experience/4_storybook/src/pages/ — storybook page compositions (UI starting point)
\_concept/3_blueprint/3_datamodel/postxl-schema.json — data model
\_concept/3_blueprint/3_datamodel/seed.json — seed data scenarios
? \_implementation/progress.json — resume state (if exists)

WRITES
e2e/specs/journeys/<stage>-<journey-slug>.spec.ts — journey-level e2e tests
\_implementation/progress.json — journey/page/feature status
\_implementation/PLANS.md — checked-off journeys
\_implementation/decisions.md — implementation decisions

REFERENCES
shared/contracts/prerequisites.md — tool prerequisite checks
shared/contracts/stories_schema.json — stories.json schema
shared/contracts/verification.md — verification levels
shared/contracts/git_workflow.md — branch conventions
shared/contracts/implementation_structure.md — tracking files
references/tdd_workflow.md — Playwright patterns, seed data, pitfalls
references/tdd_guard.md — TDD Guard state machine (feature-level enforcement)
references/component_gotchas.md — PostXL component pitfalls

REQUIRES
hard: pnpm, git
state: \_concept/2_experience/1_journeys/stories.json exists
state: \_concept/2_experience/2_features/ has at least one feature group
state: \_concept/2_experience/3_screens/ has screen specs
state: \_concept/3_blueprint/3_datamodel/postxl-schema.json exists
state: dev stack running (frontend: 3000, API: 3001)

# ── Step 0: Verify dev stack and test baseline ────────────────────

# TDD Guard hooks are automatically active (defined in this skill's frontmatter).

# All Edit/Write calls are blocked unless the RED→GREEN cycle is followed.

STEP 0: Verify dev stack and test baseline

- Verify backend is running:
  $ curl -sf http://localhost:3001 > /dev/null && echo "Backend OK" || echo "Backend NOT running"
  IF backend not running
  - STOP. Instruct: cd saxe-platform/backend && pnpm run e2e:stateless
  - Do NOT proceed until backend responds
- Verify frontend is running:
  $ curl -sf http://localhost:3000 > /dev/null && echo "Frontend OK" || echo "Frontend NOT running"
  IF frontend not running
  - STOP. Instruct: cd saxe-platform/frontend && pnpm run dev
  - Do NOT proceed until frontend responds
- Run existing e2e tests to establish clean baseline:
  $ cd saxe-platform/e2e && pnpm e2e
  IF any tests fail
  - STOP. Fix failing tests before starting new work
  - Do NOT proceed with a broken baseline
    MUST have passing test baseline before any implementation

# ── Step 1: Derive journey → page → feature map ──────────────────

STEP 1: Build implementation map

- Read stories.json
- Collect story_maps, order by stage: hero first, then vital, then hygiene
- Skip backlog stage
- For each story_map (journey):
  - Collect ALL downstream.candidate_screens from its stories
  - Resolve each candidate_screen to a screen spec file in \_concept/2_experience/3_screens/
  - For each screen spec, find features whose frontmatter screens[] includes that screen
  - Deduplicate: a page or feature appears only in the FIRST journey that references it
- Build map structure:
  ```
  journey: { id, label, stage, stories[] }
    pages: [
      { screen_spec, route, features: [
        { feature_spec, data_entities[] }
      ]}
    ]
  ```
- Track which pages and features are already implemented (from progress.json if resuming)

STEP 2: Present plan

- Show journey order with page and feature counts
- For each journey: list pages and features in business terms
- Note any pages shared across journeys (will be implemented once, verified in each)

CHECKPOINT journey_plan > "Here's how I'll build your app, journey by journey: > > 1. **[Hero]** <journey label> — N pages, M features > Pages: <list page names> > 2. **[Vital]** <journey label> — N pages, M features > Pages: <list page names> > ... > > Each journey gets its own end-to-end tests that verify the full user flow. > Approve to start with the hero journey."

# ── Step 3-6: Journey loop ────────────────────────────────────────

STEP 3: Start journey

- Pick next unfinished journey (hero → vital → hygiene order)
  $ git checkout implement/<app-slug>
  $ git checkout -b feat/<journey-slug>
  EMIT [implement-2-features] journey_start journey=<id> stage=<stage> pages=<N> features=<M>

STEP 4: Write journey e2e tests

- Create e2e/specs/journeys/<stage>-<journey-slug>.spec.ts
- The journey test walks the FULL multi-page flow from the story map
- Use isolated backend: startBackend('file', testInfo) in beforeAll
- Reset to populated scenario (or custom scenario if journey needs specific data)
- One test per story in the journey, in sequence (test.describe.configure({ mode: 'serial' }))
- Each test navigates, performs actions, asserts outcomes per the story's acceptance_criteria
- Use test IDs (data-testid) for element addressing — add these freely to components during implementation
- Keep tests minimal: assert user-visible outcomes, not implementation details
- All journey tests MUST FAIL at this point (pages don't exist yet)
  $ git commit -m "test: write journey e2e tests for <journey-label>"

PATTERNS
import { startBackend, stopBackend } from 'fixtures/handle-backend'
import { test, expect } from '../fixtures/test-fixtures'

    test.describe('<Journey Label>', () => {
      test.describe.configure({ mode: 'serial' })
      test.beforeAll(async ({}, testInfo) => { await startBackend('file', testInfo) })
      test.afterAll(async ({}, testInfo) => { await stopBackend('file', testInfo) })

      test('<story title>', async ({ page }) => {
        // Navigate, act, assert per story acceptance criteria
        await page.goto('<route>')
        await expect(page.getByTestId('<element>')).toBeVisible()
      })
    })

STEP 5: Implement pages

- For each page in this journey (skip if already implemented in a prior journey):
  - RUN implement-2-features-1-page with:
    - screen_spec path
    - feature specs for this page
    - journey context (for seed data scenario)
  - On completion: page tests + all feature tests pass
  - Update progress.json: mark page and its features as implemented
- After all pages: run ALL e2e tests (not just this journey) to catch regressions
  $ pnpm run e2e
  EMIT [implement-2-features] all_tests journey=<id> total=N passed=P failed=F

STEP 6: Fix until journey tests pass

- Run journey tests specifically:
  $ pnpm run e2e -- --grep "<journey-slug>"
- If journey tests fail:
  - Diagnose: usually integration issues between pages (navigation, shared state, data flow)
  - Fix the integration issues
  - Re-run ALL tests to ensure no regressions
    UNTIL journey tests pass AND all prior tests still pass
    $ git commit -m "feat: complete journey <journey-label>"

STEP 7: Journey checkpoint
CHECKPOINT journey_complete > "Journey '<journey-label>' is complete. Users can now: > [describe what the journey enables in plain language] > > All tests passing: journey (N), pages (N), features (N). > > Approve to continue to the next journey."
DO log_learnings

STEP 8: Merge journey branch
$ git checkout implement/<app-slug>
$ git merge --squash feat/<journey-slug>
$ git commit -m "feat: implement <stage> journey — <journey-label>"
$ git branch -d feat/<journey-slug>

- Update progress.json: mark journey as complete
- Update PLANS.md: check off journey
  EMIT [implement-2-features] journey_complete journey=<id> stage=<stage>

STEP 9: Repeat for remaining journeys

- Continue from STEP 3 for next journey
  UNTIL all journeys (hero + vital + hygiene) are complete

STEP 10: Final verification

- Run ALL e2e tests one final time
  $ pnpm run e2e
- Run full build + lint for both backend and frontend
  $ cd backend && pnpm run test:types
  $ cd backend && pnpm run lint
  $ cd frontend && pnpm run build
  $ cd frontend && pnpm run lint
  $ bash scripts/check-frontend-paths.sh
- Report summary: total journeys, pages, features, test count
  EMIT [implement-2-features] completed journeys=<N> pages=<N> features=<N> tests=<N>

# ── Procedures ────────────────────────────────────────────────────

PROCEDURE log_learnings

- Append to LEARNINGS.md under Skills & Subskills
- Note what worked and what didn't in this journey

# ── Constraints ───────────────────────────────────────────────────

MUST implement journeys in stage order: hero → vital → hygiene
MUST write failing journey tests BEFORE implementing any pages
MUST deduplicate pages across journeys — implement once, verify in each
MUST run ALL tests (not just current journey) after each page to catch regressions
MUST use isolated e2e backend for journey tests (startBackend/stopBackend)
MUST use populated scenario seed data by default
MUST use test IDs (data-testid) for element addressing in tests
MUST keep tests minimal — assert outcomes, not implementation details
MUST use one git branch per journey, squash-merged after approval

NEVER implement pages or features before journey tests exist
NEVER use screenshot assertions — use test IDs and value assertions
NEVER skip regression testing (all prior tests must still pass)
NEVER modify \_concept/ files
NEVER merge a journey with failing tests
NEVER use `any` to work around path resolution — fix tsconfig paths instead

CHECKLIST

- [ ] Journey → page → feature map derived from stories.json
- [ ] All journeys processed in stage order (hero → vital → hygiene)
- [ ] Journey e2e tests pass for each completed journey
- [ ] All page and feature tests pass (no regressions)
- [ ] Build passes (backend type check + frontend build + both lint)
- [ ] Frontend path aliases in sync (check-frontend-paths.sh)
- [ ] progress.json and PLANS.md updated
