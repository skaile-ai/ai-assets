# Feature Auto-Approval Criteria

Rules for auto-approving individual features without human checkpoint.

## Auto-Approval Checks

All checks must pass for auto-approval. Any failure escalates to human review.

| Check | Requirement |
|-------|------------|
| E2E tests | 100% pass rate (0 failures, 0 skipped) |
| Storybook | All stories render without errors |
| Browser check | agent-browser reports no blocking issues and no deviations from screen spec |
| Build | Level 1 verification passes (build + lint + types) |
| No spec deviations | No deviations from concept specs logged in implementation notes |

## On Auto-Approve

1. Log: `[implement] feature_auto_approved feature=<group>/<feature>`
2. Record `approval_method: "auto"` in `progress.json`
3. Squash-merge the feature branch
4. Continue to the next feature without pausing

## Escalation to Human Review

Escalate when any of these are true:

- Any E2E test fails or is skipped
- Storybook reports rendering errors
- agent-browser flags deviations from the screen spec
- Build, lint, or type checks fail
- The feature tracking file contains notes about spec deviations
- The feature required changes to generated code or schema

When escalating, present the standard checkpoint prompt with failing checks
highlighted.

## Auto-Approvable Phases

| Phase | Auto-approvable? | Condition |
|-------|-----------------|-----------|
| 0 — Plan | No | User must approve scope |
| 1 — Scaffold | Yes | Build passes, no errors |
| 2 — Startup | Yes | App accessible, no console errors |
| 3 — Foundation | No | User must see visual result |
| 3.5 — Infrastructure | Yes | Build passes, all processes start, connectivity verified |
| 4 — Features (individual) | Yes | All criteria above met |
| 4 — Features (group) | No | User must approve each group |
| 5 — Re-generation | Yes | No conflicts, build passes |
| 5.5 — UAT | No | User must personally test and approve |
| 6 — Verification | No | User must approve final state |

## User Override

Disable feature auto-approval by setting `auto_approve_features: false` in
PLANS.md scope section. Default is `true`.
