# Verification Protocol

What constitutes "verified" at each stage of the implementation pipeline.

## Verification Levels

### Level 1: Build Verification (every phase)

Run after every significant change. Must pass before any checkpoint.

- [ ] `pnpm run build` exits 0 (backend + frontend)
- [ ] `pnpm run lint` exits 0
- [ ] TypeScript: `pnpm run test:types` exits 0
- [ ] No unresolved merge conflict markers in source files

### Level 2: Feature Verification (per feature)

Run after implementing a feature, before requesting approval.

- [ ] All Level 1 checks pass
- [ ] All acceptance criteria E2E tests pass (`pnpm run e2e -- --grep "<feature>"`)
- [ ] Backend unit tests pass (`cd backend && pnpm run test:jest`), if custom backend logic exists
- [ ] Storybook stories render without errors (`pnpm run test:storybook`)
- [ ] agent-browser visual check: navigate to feature, verify against screen spec
- [ ] No console errors visible in browser DevTools during agent-browser check
- [ ] Screenshot saved to `_implementation/verification/screenshots/<feature>/`

### Level 3: Full Verification (pre-deployment gate)

Run after all features are implemented, before final approval.

- [ ] All Level 1 checks pass
- [ ] Complete E2E suite passes (`pnpm run e2e`)
- [ ] All backend unit tests pass (`cd backend && pnpm run test:jest`)
- [ ] Visual regression: no unexpected snapshot changes
- [ ] All Storybook stories render without errors
- [ ] agent-browser walkthrough of full user flow (happy path)
- [ ] Database seed data loads correctly for all scenarios
- [ ] Auth flow works end-to-end (login → navigate → logout)
- [ ] Report saved to `_implementation/verification/reports/full-verification.json`

### Level 4: User Acceptance Testing (UAT)

Run after re-generation, before final verification. The business user tests
the app against their original requirements.

- [ ] Key user journeys identified from feature specs (hero flow + must-have features)
- [ ] Each journey walked through with agent-browser, demonstrated to the user
- [ ] User has tested and approved each journey (pass / fail / needs changes)
- [ ] Any failed journeys fixed and re-tested
- [ ] UAT report saved to `_implementation/verification/reports/uat-report.json`
- [ ] User has approved overall UAT results

## Verification Report Format

```json
{
  "level": 1 | 2 | 3,
  "timestamp": "ISO date",
  "feature": "feature_name or null for full",
  "results": {
    "build": { "passed": true, "duration_ms": 12000 },
    "lint": { "passed": true, "warnings": 3 },
    "types": { "passed": true, "errors": 0 },
    "e2e": {
      "passed": true,
      "total": 15,
      "passed_count": 15,
      "failed_count": 0,
      "skipped_count": 0
    },
    "backend_unit_tests": {
      "passed": true,
      "total": 8,
      "passed_count": 8,
      "failed_count": 0,
      "skipped_count": 0
    },
    "storybook": { "passed": true, "stories": 8 },
    "browser_check": {
      "passed": true,
      "screenshots": ["path/to/screenshot.png"],
      "notes": "All components render correctly"
    }
  },
  "verdict": "pass | fail | needs_review",
  "blocking_issues": [],
  "warnings": []
}
```

## E2E Test Strategy

### Assertion Tests (primary method)

Specific, deterministic assertions that verify exact behavior:

```typescript
// Good: specific assertion
await expect(page.getByText('Invalid email or password')).toBeVisible()
await expect(page).toHaveURL('/dashboard')
await expect(page.getByRole('button', { name: 'Deploy' })).toBeEnabled()

// Bad: vague assertion
await expect(page).toHaveScreenshot() // only for high-level flow
```

### Snapshot Tests (secondary, sparingly)

Visual regression snapshots for high-level user flows only:

- One snapshot test per feature (the final AC)
- Captures the happy-path end state
- Uses `populated` seed scenario for consistency
- Snapshot baseline committed to git

### Seed Data Usage

E2E tests consume seed data from `_concept/3_blueprint/3_datamodel/seed.json`:

| Scenario      | Test usage                                |
| ------------- | ----------------------------------------- |
| `empty`       | First-use, onboarding flows               |
| `single_user` | Minimal happy path                        |
| `populated`   | Full feature testing, data grid rendering |
| `edge_cases`  | Error handling, boundary conditions       |

## agent-browser Visual Checks

Use the `agent-browser` skill for visual verification:

1. Start the development server if not running
2. Navigate to the feature's route (from screen spec)
3. Verify each screen state matches the spec:
   - Component inventory present
   - Layout matches spec description
   - Interactive elements respond correctly
4. Take screenshot for evidence
5. Report any deviations from spec

Visual checks complement E2E tests — they catch rendering issues,
layout problems, and visual regressions that assertion tests miss.

## Test Environment

Features use the shared development stack by default. The PostXL backend runs
in stateless mode (in-memory repositories) which provides fast startup, no
database setup, and automatic isolation per E2E spec file (each spec gets its
own backend instance).

In-memory mode supports full CRUD operations — mutations persist within a test
run and are discarded when the backend instance shuts down. This is sufficient
for testing both frontend and custom backend logic.

### When to use the shared dev stack (default)

All features use the shared dev stack (`pnpm run dev`) on default ports:

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:3001`

### Exception: isolated environment

Only spin up a separate docker-compose instance when:

- The feature requires conflicting auth configurations
- The feature requires persistent database state across test runs
- Multiple features are being developed in parallel and need different seed data

This is rare and should be the exception.

## Auto-Review Thresholds

For auto-approval during orchestrated runs:

| Metric             | Threshold                                       |
| ------------------ | ----------------------------------------------- |
| Build              | Must pass (0 errors)                            |
| Lint               | Must pass (warnings OK)                         |
| Types              | Must pass (0 errors)                            |
| E2E tests          | 100% pass rate                                  |
| Backend unit tests | 100% pass rate (if custom backend logic exists) |
| Storybook          | All stories render                              |
| Browser check      | No blocking issues                              |

If all thresholds are met, the orchestrator may auto-approve.
If any threshold fails, escalate to human review.

For feature auto-approval (see `implement` Auto-Review Mode), all thresholds
must be met simultaneously, the verification report's `verdict` must be `"pass"`,
and the feature must have no logged spec deviations in its tracking file.
