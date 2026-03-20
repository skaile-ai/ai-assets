---
name: implement-2-features-1-page
description: 'Page-level feature implementation with TDD Guard. Implements all features within one page using outside-in TDD: writes failing page tests, then for each feature writes failing feature tests and implements until green. Uses storybook page compositions as UI starting point. TDD Guard enforces red-green discipline at the feature level.'
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

ROLE Page implementer — implements all features within one page using TDD Guard-enforced red-green cycles.

READS
\_concept/2_experience/3_screens/<group>/<screen>.md — page spec: component inventory, states, routes, data requirements
\_concept/2_experience/2_features/<group>/<feature>.md — feature specs for features on this page
\_concept/2_experience/4_storybook/src/pages/ — storybook page composition (UI reference)
\_concept/3_blueprint/3_datamodel/postxl-schema.json — data model (relevant models)
\_concept/3_blueprint/3_datamodel/seed.json — seed data scenarios
\_concept/1_discovery/3_brand/tokens.json — brand tokens (never invent colors)

WRITES
e2e/specs/pages/<group>/<page-slug>.spec.ts — page-level e2e tests
e2e/specs/features/<group>/<feature-slug>.spec.ts — feature-level e2e tests
frontend/src/\*_/_.tsx — page and feature UI components
backend/libs/update/src/\*.update.service.ts — custom action implementations
postxl-schema.json — custom action definitions (then regenerate)

REFERENCES
references/tdd_guard.md — TDD Guard state machine and CLI
references/tdd_workflow.md — Playwright patterns, seed data, pitfalls
references/backend_patterns.md — custom backend decision tree
references/component_gotchas.md — PostXL component pitfalls
references/custom_actions.md — custom action schema and generation pattern

REQUIRES
hard: pnpm, git
state: dev stack running (frontend: 3000, API: 3001)
state: screen spec exists for the target page
state: at least one feature spec references this page

# ── Step 0: Preflight — verify dev stack is still running ─────────

# TDD Guard hooks are automatically active (defined in this skill's frontmatter).

# All Edit/Write calls are blocked unless the RED→GREEN cycle is followed.

STEP 0: Preflight

- Verify backend is running:
  $ curl -sf http://localhost:3001 > /dev/null && echo "Backend OK" || echo "Backend NOT running"
  IF backend not running → STOP. Backend must be running before any work.
- Verify frontend is running:
  $ curl -sf http://localhost:3000 > /dev/null && echo "Frontend OK" || echo "Frontend NOT running"
  IF frontend not running
  - STOP. Instruct: cd frontend && pnpm run dev
  - Do NOT proceed until frontend responds
- Run existing e2e tests to establish clean baseline:
  $ cd e2e && pnpm e2e
  IF any tests fail
  - STOP. Fix failing tests before starting new work
  - Do NOT proceed with a broken baseline
- Record baseline test count for regression checking

MUST have passing test baseline before writing any new tests
MUST have both backend and frontend running before any test work

# ── Step 1: Write page tests ─────────────────────────────────────

STEP 1: Write page-level e2e tests

- Read screen spec: component inventory, states, route, data requirements
- Read storybook page composition for UI reference
- Create e2e/specs/pages/<group>/<page-slug>.spec.ts
- Use isolated backend: startBackend('file', testInfo) in beforeAll
- Reset to populated scenario by default
- Write tests for:
  - Page loads and renders core layout (from screen spec component inventory)
  - Each screen state works (default, empty, loading, error)
  - Navigation to/from this page works
  - Data displays correctly from seed data
- Use test IDs (data-testid) for element addressing
- Keep tests minimal: assert user-visible outcomes
- All page tests MUST FAIL at this point
  $ git commit -m "test: write page e2e tests for <page>"

PATTERNS
import { startBackend, stopBackend } from 'fixtures/handle-backend'
import { test, expect } from '../../fixtures/test-fixtures'

    test.describe('<Page Name>', () => {
      test.describe.configure({ mode: 'serial' })
      test.beforeAll(async ({}, testInfo) => { await startBackend('file', testInfo) })
      test.afterAll(async ({}, testInfo) => { await stopBackend('file', testInfo) })

      test('page renders with populated data', async ({ page }) => {
        await page.goto('<route>')
        await expect(page.getByTestId('page-<slug>')).toBeVisible()
        // Assert core layout elements from screen spec
      })

      test('empty state shows placeholder', async ({ page }) => {
        // Reset to empty scenario if needed
        await page.goto('<route>')
        await expect(page.getByTestId('empty-state')).toBeVisible()
      })
    })

# ── Step 2-4: Feature TDD loop ───────────────────────────────────

STEP 2: Start feature TDD cycle

- For each feature on this page (from feature specs where screens[] includes this page):

STEP 2a: TDD RED — write feature tests - Read feature spec: requirements, success criteria, error states - Create e2e/specs/features/<group>/<feature-slug>.spec.ts - Write minimal e2e tests covering: - Happy path (primary success scenario) - Key error states from feature spec - Guard conditions / validation rules - Use test IDs for element addressing - Tests run against shared dev backend (not isolated — features share page state) - All feature tests MUST FAIL (no implementation yet)
$ git commit -m "test: write feature e2e tests for <feature>"

    PATTERNS
      import { test, expect } from '../../fixtures/test-fixtures'

      test.describe('<Feature Name>', () => {
        test('<happy path>', async ({ page }) => {
          await page.goto('<route>')
          await page.getByTestId('<action-element>').click()
          await expect(page.getByTestId('<result-element>')).toContainText('<expected>')
        })

        test('<error state>', async ({ page }) => {
          await page.goto('<route>')
          // Trigger error condition
          await expect(page.getByTestId('error-message')).toBeVisible()
        })
      })

STEP 2b: TDD GREEN — implement feature - Use the storybook page composition as UI reference - Implementation order: 1. Add route (if new page, register in router) 2. Build page component (copy structure from storybook, wire to real data) 3. Add test IDs to components (data-testid attributes) 4. Wire data fetching (generated tRPC client for standard CRUD) 5. Implement forms and interactions 6. Implement state management - For custom backend logic beyond CRUD: 1. Define customActions in postxl-schema.json on the closest model 2. $ pnpm run generate (regenerate to get routes + placeholders) 3. Implement business logic in the model's update service (@custom-start blocks) 4. All external services use mock implementations (already available from infrastructure) - Use @postxl/ui-components for all UI primitives - Apply brand tokens via CSS custom properties (never hardcode) - Run feature tests after each significant change:
$ pnpm run e2e -- --grep "<feature>"
UNTIL feature tests pass

    - Then run ALL tests to catch regressions:
    $ pnpm run e2e
    UNTIL all prior tests still pass

    $ git commit -m "feat: implement <feature>"

STEP 2c: Repeat for remaining features - Continue STEP 2a-2b for each feature on this page
UNTIL all features on this page are implemented

# ── Step 3: Verify page ──────────────────────────────────────────

STEP 3: Fix until page tests pass

- Run page tests specifically:
  $ pnpm run e2e -- --grep "<page-slug>"
- If page tests fail:
  - Usually integration between features on the same page
  - Fix layout, data flow, or state management issues
  - Re-run ALL tests to ensure no regressions
    UNTIL page tests pass AND all prior tests still pass
    $ git commit -m "feat: complete page <page-name>"

STEP 4: Verify build
$ cd backend && pnpm run test:types
$ cd backend && pnpm run lint
$ cd frontend && pnpm run build
$ cd frontend && pnpm run lint
$ bash scripts/check-frontend-paths.sh
IF any step fails - Fix issues - Re-run all tests
UNTIL all verification passes

STEP 5: Report completion
EMIT [implement-2-features-1-page] completed page=<page> features=<N> tests=<N>

# ── Custom Actions Pattern ────────────────────────────────────────

PROCEDURE define_custom_action
When a feature needs domain logic beyond standard CRUD:

1. Identify the closest model (the one most affected by the action)
2. Add to postxl-schema.json:
   ```json
   "models": {
     "Project": {
       "actions": {
         "deploy": "Deploy the project to the target environment",
         "archive": "Archive the project and all its artifacts"
       }
     }
   }
   ```
3. $ pnpm run generate
   - Generates: action type, payload type, tRPC mutation, update service placeholder
4. Implement business logic in backend/libs/update/src/project.update.service.ts
   - Use @custom-start/@custom-end markers
   - Inject existing providers from infrastructure phase
   - Use mock/in-memory implementations for external services
5. Call from frontend via generated tRPC mutation hook

# ── Seed Data ─────────────────────────────────────────────────────

PROCEDURE configure_seed_scenario
By default, tests use the populated scenario from test-data.json.
When a test needs different data:

1. Add a new scenario to test-data.json (e.g., "with_active_deployment")
2. In the test's beforeAll:
   ```typescript
   test.beforeAll(async ({ request }, testInfo) => {
     await startBackend('file', testInfo);
     await request.put('/api/test/reset-scenario', {
       data: { key: 'test-data', scenario: 'with_active_deployment' },
     });
   });
   ```
3. Commit the new scenario with the test

# ── Constraints ───────────────────────────────────────────────────

MUST write failing page tests BEFORE implementing any features
MUST write failing feature tests BEFORE implementing each feature (TDD red-green)
MUST use storybook page compositions as UI starting point
MUST add data-testid attributes to all interactive and assertable elements
MUST use test IDs and value assertions in tests (not screenshots)
MUST run ALL tests after each feature to catch regressions
MUST use @postxl/ui-components for all UI primitives
MUST consume brand tokens — never invent visual decisions
MUST use customActions for domain logic (not raw CRUD combinations)
MUST use mock/in-memory implementations for external services in tests
MUST use @custom-start/@custom-end markers in generated files

NEVER implement before tests exist
NEVER use screenshot assertions
NEVER skip regression testing after a feature
NEVER hardcode colors, fonts, or spacing
NEVER create duplicate adapters when infrastructure providers exist
NEVER modify \_concept/ files
NEVER use `any` to work around path resolution — fix tsconfig paths instead

CHECKLIST

- [ ] Page e2e tests written and initially failing
- [ ] All features on this page have e2e tests
- [ ] All feature tests pass
- [ ] Page tests pass
- [ ] All prior tests still pass (no regressions)
- [ ] Build passes (backend + frontend type check + lint)
- [ ] Frontend path aliases in sync (check-frontend-paths.sh)
- [ ] Custom actions defined in schema and regenerated (if any)
- [ ] Test IDs added to all interactive elements
