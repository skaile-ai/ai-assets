# cf_quality_audit CLI

## Trigger

Invoke with: `/audit` or "Audit the codebase" or "Find bugs before testing"

## When to Use

- Before the first `cf_test_e2e` run on a new feature
- After significant refactoring
- As a standalone code-quality check
- In CI before deploying

## Output

- Prioritized report printed in conversation
- Optional: `audit-report.md`

## Relationship to cf_test_e2e

`cf_quality_audit` is the static analysis phase that was previously embedded inside `cf_test_e2e` as Sub-agent 3. Running it separately:
- Makes `cf_test_e2e` faster (no redundant static analysis)
- Lets you fix code issues before spending time on browser testing
- Can be re-run independently without running the full E2E suite
