# Verification Report Templates

## full-verification.json

```json
{
  "level": 3,
  "timestamp": "ISO-8601",
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
    "backend_tests": {
      "passed": true,
      "total": 0,
      "passed_count": 0,
      "failed_count": 0
    },
    "browser_check": {
      "passed": true,
      "screenshots": [],
      "notes": ""
    },
    "spec_fidelity": {
      "features_checked": 0,
      "requirements_total": 0,
      "requirements_passed": 0,
      "requirements_partial": 0,
      "requirements_failed": 0,
      "deviations": []
    }
  },
  "verdict": "pass | fail | needs_review",
  "blocking_issues": [],
  "warnings": []
}
```

When browser skill is not available:
```json
"browser_check": { "passed": null, "notes": "browser skill not available — skipped" }
```

## Console Summary Format

```
=== Full Verification Report ===

Build:              PASS
Lint:               PASS (N warnings)
Types:              PASS
E2E Tests:          N/N passing
Backend Tests:      N/N passing (or 0 — no custom backend logic)
Storybook:          N stories, all rendering
Browser:            PASS

Spec Fidelity:      M/M requirements passing
  Features:         N/N fully verified
  Partial:          P features with minor deviations
  Deviations:       see report

Blocking issues: 0
Warnings: N

Verdict: PASS / FAIL / NEEDS REVIEW

[PASS]: The implementation is verified and ready for deployment.
[FAIL]: N blocking issues must be resolved before deployment.
[NEEDS REVIEW]: N items require human judgment.
```

## Spec Fidelity Matrix

```
### Feature × Acceptance Criteria Matrix

| Feature | Requirement | Screen | Status | Evidence |
|---------|------------|--------|--------|----------|
| Login | Email + password form | login.md | PASS | screenshot |
| Login | Error on invalid creds | login.md | FAIL | No error message shown |
| Dashboard | Shows user stats | overview.md | PARTIAL | Stats present but count wrong |

### Deviations
| Screen | Spec Says | App Shows | Severity |
|--------|-----------|-----------|----------|
| /dashboard | 4 stat cards | 3 stat cards | MEDIUM |

### Missing Components
| Screen | Component | Spec Reference |
|--------|-----------|---------------|
| /settings | Theme toggle | 2_experience/3_screens/settings/preferences.md |
```

## Browser Walkthrough Procedure

1. **Auth flow:** Navigate to login, authenticate, reach dashboard
2. **Navigation:** Click every sidebar item, verify pages load
3. **Feature check:** For each feature group:
   - Navigate to primary screen
   - Verify core functionality (data displays, forms submit)
   - Check responsive behavior (resize viewport)
4. **Theme check:** Toggle dark mode, verify all pages render correctly
5. **Error handling:** Trigger a known error state, verify error UI appears

Save screenshots to `_implementation/verification/screenshots/full-walkthrough/`.

## E2E Failure Triage

1. Analyze failures — regressions from feature merges or new issues?
2. For regressions: create fix commit on `implement/<app-slug>`
3. Re-run failing tests
4. If still failing: document as blocking issue in `blocking_issues`

## Visual Regression Triage

- **Expected:** intentional changes from feature implementations → update baseline
- **Unexpected:** regressions → flag as blocking issue
