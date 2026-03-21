# audit CLI

## Trigger

Invoke with: `/audit`, "audit the codebase", "security check", "find bugs", "find bugs before testing"

## When to Use

- Before the first `e2e` run on a new feature
- After significant refactoring
- As a standalone code-quality check
- In CI before deploying

## Output

- Prioritized report printed in conversation
- Optional: `audit-report.md`

## Relationship to e2e

`audit` is the static analysis phase. Running it separately from `e2e`:
- Makes `e2e` faster (no redundant static analysis)
- Lets you fix code issues before spending time on browser testing
- Can be re-run independently without running the full E2E suite

## Recommended Workflow Position

```
implement → audit → ready → e2e → verify
               ↑
          Run this here
```
