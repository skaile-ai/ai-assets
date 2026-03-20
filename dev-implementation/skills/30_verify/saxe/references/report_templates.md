# Verification Report Templates

## full-verification.json

```json
{
  "level": 3,
  "timestamp": "ISO date",
  "feature": null,
  "results": {
    "build": { "passed": true, "duration_ms": 0 },
    "lint": { "passed": true, "warnings": 0 },
    "types": { "passed": true, "errors": 0 },
    "e2e": {
      "passed": true,
      "total": 0,
      "passed_count": 0,
      "failed_count": 0,
      "skipped_count": 0,
      "per_feature": {}
    },
    "storybook": { "passed": true, "stories": 0 },
    "backend_unit_tests": {
      "passed": true,
      "total": 0,
      "passed_count": 0,
      "failed_count": 0
    },
    "browser_check": {
      "passed": true,
      "screenshots": [],
      "notes": ""
    }
  },
  "verdict": "pass | fail | needs_review",
  "blocking_issues": [],
  "warnings": []
}
```

When `agent-browser` is not available, set:
```json
"browser_check": { "passed": null, "notes": "agent-browser not available — skipped" }
```

## Console summary format

```
=== Full Verification Report ===

Build:              PASS
Lint:               PASS (N warnings)
Types:              PASS
E2E Tests:          N/N passing
Backend Unit Tests: N/N passing (or 0 — no custom backend logic)
Storybook:          N stories, all rendering
Browser:            PASS

Blocking issues: 0
Warnings: N

Verdict: PASS / FAIL / NEEDS REVIEW

[If PASS]: The implementation is verified and ready for deployment.
[If FAIL]: N blocking issues must be resolved before deployment.
[If NEEDS REVIEW]: N items require human judgment.
```

## Browser walkthrough procedure

Use the `agent-browser` skill to perform a full user flow walkthrough:

1. **Auth flow:** Navigate to login, authenticate, reach dashboard
2. **Navigation:** Click through every sidebar item, verify pages load
3. **Feature check:** For each implemented feature group:
   - Navigate to the primary screen
   - Verify core functionality works (data displays, forms submit, etc.)
   - Check responsive behavior (resize viewport)
4. **Theme check:** Toggle dark mode, verify all pages render correctly
5. **Error handling:** Trigger a known error state, verify error UI appears

Take screenshots at each step to `_implementation/verification/screenshots/full-walkthrough/`.

## Backend unit test coverage

Backend unit tests verify custom logic added during `implement-2-features` Steps 6c-6d:
- Custom action handlers (validation, state transitions, side effects)
- External service adapters (in-memory for test, real for production)
- Custom ViewService transforms (computed fields, aggregations)
- Custom authorization rules

If no custom backend tests exist (all features used generated CRUD only),
the step still runs but reports 0 tests -- that is acceptable.

## E2E failure triage

When E2E tests fail:
1. Analyze failures -- are they regressions from feature merges or new issues?
2. For regressions: create a fix commit on `implement/<app-slug>`
3. Re-run failing tests
4. If still failing: document as blocking issue

## Visual regression triage

When snapshot diffs appear, categorize as:
- **Expected:** intentional changes from feature implementations -- update baseline
- **Unexpected:** regressions -- flag as blocking issue
