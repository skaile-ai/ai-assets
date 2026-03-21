# app-audit CLI

## Trigger

Invoke with: `/audit` or "Audit the codebase" or "Find bugs before testing"

## When to Use

- Before the first `app-e2e` run on a new feature
- After significant refactoring
- As a standalone code-quality check
- In CI before deploying

## Output

- Prioritized report printed in conversation
- Optional: `audit-report.md`

## Relationship to app-e2e

`app-audit` is the static analysis phase that was previously embedded inside `app-e2e` as Sub-agent 3. Running it separately:
- Makes `app-e2e` faster (no redundant static analysis)
- Lets you fix code issues before spending time on browser testing
- Can be re-run independently without running the full E2E suite
