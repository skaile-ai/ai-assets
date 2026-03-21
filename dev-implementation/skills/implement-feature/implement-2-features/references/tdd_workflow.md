# TDD Workflow for Feature Implementation

Detailed guide for the test-driven development cycle used by `implement-2-features`.

## The Core Principle

Write down what "done" looks like before the agent starts building.
Acceptance criteria force thinking through edge cases before implementation.
The agent builds it. Something else checks it.

> "You can't trust what an agent produces unless you told it what 'done'
> looks like before it started." — Claude Code Camp

## The Four-Stage Verification Pipeline

Inspired by the harness engineering approach:

### 1. Pre-flight (no LLM)

Before any implementation work, verify mechanically:

- Does the feature spec exist in `_concept/2_experience/2_features/`?
- Does at least one screen spec exist in `_concept/2_experience/3_screens/`?
- Is the data model generated (relevant models in `postxl-schema.json`)?
- Is the development server running?
- Is the database seeded?

Fail fast before spending effort.

### 2. Planning (read concept → write ACs)

One pass to read all concept artifacts and produce structured acceptance criteria.
The planner reads:

- Feature spec: requirements, success criteria, error states
- Screen spec: component inventory, states, user actions
- Behavioral spec: Allium rules, guard conditions
- Relevant code: existing generated files, component selectors

Output: `_implementation/acceptance_criteria/<group>/<feature>.ac.md`

### 3. Test Writing (ACs → Playwright tests)

One test per AC, each independently runnable. Key patterns:

**Important: Scope text locators to `main`.** PostXL generates an admin sidebar
with links for every model (e.g., `AppConcept`, `CloudConnection`, `AppVersion`).
It also includes an AI Assistant sidebar with a "Send" button. These cause
Playwright strict mode violations when text/role locators match multiple elements.
Always scope to `page.locator('main')`:

```typescript
// BAD — matches admin sidebar link "AppConcept"
await expect(page.getByText(/concept/i)).toBeVisible()

// GOOD — scoped to main content
const main = page.locator('main')
await expect(main.getByText(/concept/i)).toBeVisible()
```

**Navigation and routing:**
```typescript
test('AC-1: successful login redirects to dashboard', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Email').fill('test@example.com')
  await page.getByLabel('Password').fill('validPassword123')
  await page.getByRole('button', { name: 'Sign In' }).click()
  await expect(page).toHaveURL('/dashboard')
  await expect(page.locator('main').getByText('Welcome')).toBeVisible()
})
```

**Error states:**
```typescript
test('AC-2: wrong password shows error message', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Email').fill('test@example.com')
  await page.getByLabel('Password').fill('wrongPassword')
  await page.getByRole('button', { name: 'Sign In' }).click()
  await expect(page.locator('main').getByText('Invalid email or password')).toBeVisible()
  await expect(page).toHaveURL('/login')
})
```

**Form validation:**
```typescript
test('AC-3: empty fields prevent submission', async ({ page }) => {
  await page.goto('/login')
  const main = page.locator('main')
  const submitButton = main.getByRole('button', { name: 'Sign In' })
  await expect(submitButton).toBeDisabled()
  // Or: clicking submit shows inline error
  await submitButton.click()
  await expect(main.getByText('Email is required')).toBeVisible()
})
```

**Data display:**
```typescript
test('AC-4: app list shows all user apps', async ({ page }) => {
  // Using 'populated' seed scenario
  await page.goto('/dashboard/apps')
  const main = page.locator('main')
  const rows = main.getByRole('row')
  await expect(rows).toHaveCount(5) // from seed data
  await expect(main.getByText('Client Engagement Tracker')).toBeVisible()
})
```

**State transitions (from behavioral specs):**
```typescript
test('AC-5: deploying app transitions to Running', async ({ page }) => {
  await page.goto('/apps/1/deploy')
  const main = page.locator('main')
  await main.getByRole('button', { name: 'Deploy' }).click()
  await expect(main.getByText('Provisioning')).toBeVisible()
  // Wait for transition
  await expect(main.getByText('Running')).toBeVisible({ timeout: 30000 })
})
```

**Radix UI component roles:**
```typescript
// ToggleGroupItem uses role="radio", not role="button"
await page.getByRole('radio', { name: /agent/i }).click()
await expect(page.getByRole('radio', { name: /agent/i })).toBeChecked()
```

**The single snapshot test (always last):**
```typescript
test('AC-N: complete login flow', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Email').fill('test@example.com')
  await page.getByLabel('Password').fill('validPassword123')
  await page.getByRole('button', { name: 'Sign In' }).click()
  await expect(page).toHaveURL('/dashboard')
  await expect(page).toHaveScreenshot('login-complete-flow.png')
})
```

### 4. Implementation (build until green)

Write code until all tests pass. Run tests frequently:

```bash
# Run just this feature's tests
pnpm run e2e -- --grep "AC-"

# Run a specific test
pnpm run e2e -- --grep "AC-1"
```

## Seed Data Integration

### Loading seed scenarios in tests

```typescript
import { seedScenarios } from '../fixtures/seed-data'

test.describe('App List', () => {
  test.beforeEach(async ({ request }) => {
    // Reset database to specific scenario
    await request.post('/api/test/reset', {
      data: { scenario: 'populated' }
    })
  })

  test('shows apps from populated scenario', async ({ page }) => {
    await page.goto('/apps')
    // Assert against known seed data values
    await expect(page.getByText('Kundenportal')).toBeVisible()
  })
})
```

### Scenario selection per AC

| AC tests | Recommended scenario |
|----------|---------------------|
| First-use, onboarding | `empty` |
| Basic happy path | `single_user` |
| Data display, lists, grids | `populated` |
| Error handling, boundaries | `edge_cases` |

## Common Pitfalls

### Tests that test implementation, not behavior

**Bad:** Testing CSS class names, internal state, component props
**Good:** Testing what the user sees and does

### Overly brittle selectors

**Bad:** `page.locator('.css-1a2b3c > div:nth-child(3)')`
**Good:** `page.getByRole('button', { name: 'Deploy' })`

### Unscoped text locators hitting PostXL admin sidebar

**Bad:** `page.getByText(/concept/i)` — matches sidebar link "AppConcept"
**Good:** `page.locator('main').getByText(/concept/i)` — scoped to page content

PostXL's admin sidebar contains links for every data model. Any regex that
partially matches a model name (e.g., `/connect/i` matches "CloudConnection",
`/version/i` matches "AppVersion") will cause Playwright strict mode failures.
The AI Assistant sidebar also has duplicate buttons (e.g., "Send"). Always scope
text and button locators to `page.locator('main')`.

### Seed data accumulation in stateless mode

Tests that create data (e.g., form submissions, wizard flows) persist within a
test run's backend instance. On repeated runs, the sidebar app list grows. Use
`.first()` on locators that may match stale entries from prior runs:
```typescript
await expect(page.locator('main').getByText('My App').first()).toBeVisible()
```

### Missing error path coverage

If the concept spec lists 3 error states, there should be 3 error-path ACs.
Don't just test the happy path.

### Snapshot overuse

Only ONE snapshot test per feature. Everything else uses specific assertions.
Snapshots are for catching unexpected visual regressions, not for verifying behavior.

## Debugging Failing Tests

When tests fail during implementation:

1. **Read the error message** — Playwright provides clear failure messages
2. **Check the screenshot** — Playwright auto-captures on failure
3. **Check the selector** — Is the element actually on the page?
4. **Check the route** — Is the page navigated correctly?
5. **Check the seed data** — Is the expected data loaded?
6. **Check the backend** — Is the API returning data? Check network tab.
