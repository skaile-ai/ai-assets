---
name: test-integration
description: "Use when you need integration tests that verify API endpoints, data flow, and cross-feature interactions against a real database. Reads feature specs and data model to generate tests covering the full request-response cycle."
keywords: [testing, integration, api, database, endpoints, data-flow]
subagent: true
user_inputs:
  dialog: []
  files: []
metadata:
  stage: alpha
  requires:
  - quality-contract
---

# App Test Integration — API & Data Flow Testing

## Overview

Generates integration test files that verify API endpoints, database operations,
and cross-feature data flows using a real database. Reads feature specs, screen
specs, and the data model to produce tests covering the full request-response
cycle — from HTTP request through business logic to database mutation and response.

## When to Use

- After implementation and database migration are complete
- When the user says "integration tests", "API tests", or "test data flow"
- After `cf_test_unit` — to add the next layer of test coverage
- When testing cross-feature interactions (e.g., creating a task updates the dashboard count)

## When NOT to Use

- For testing isolated functions/composables — use `cf_test_unit`
- For browser-based testing — use `cf_test_e2e`
- For spec-fidelity checks — use `cf_quality_verify`
- For generating a test plan (not code) — use `cf_test_plan`
- When no database or API exists yet — implementation and migration must come first

## Prerequisites

<HARD-GATE> Source code must exist with API endpoints. Check for `server/api/`, `routes/`, or equivalent. If missing: "No API endpoints found. Implementation must exist before generating integration tests."

<HARD-GATE> Feature specs must exist in `_concept/03_features/`. If missing: "No feature specs found. Run `cf_concept_functionality_features` first."

<HARD-GATE> Data model must exist at `_concept/06_datamodel/model.json`. If missing: "No data model found. Run `cf_concept_datamodel` first."

<HARD-GATE> Database must be accessible. Check `.env` or `.env.example` for connection details. If no database configuration found: "No database connection configured. Run migration first."

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** Source code with API endpoints, feature specs (`03_features/`), data model (`06_datamodel/model.json`), database accessible
**If gates fail:** Run `cf_implement_feature`, `cf_concept_functionality_features`, `cf_concept_datamodel`, or `cf_implement_migrate` as needed
**On completion:** Present summary, then suggest next steps.

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid _concept/ paths
- `cf__shared/frontmatter.md` — feature frontmatter fields
- `cf__shared/seed_data.md` — scenario-based seed data convention
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

| Source | Token estimate | Priority |
|--------|---------------|----------|
| `_concept/03_features/**/*.md` | ~3000 | Required |
| `_concept/06_datamodel/model.json` | ~2000 | Required |
| `_concept/06_datamodel/seed.json` | ~1500 | Required |
| `_concept/05_techstack/stack.md` | ~500 | Required |
| API route source files | ~4000 | Required |
| Existing test files (patterns) | ~2000 | Required |
| `_concept/08_testing/test_plan.md` | ~2000 | Optional |
| `.env.example` | ~200 | Required |

## Workflow

### Phase 1: Discover Integration Environment

#### Sub-agent 1: API & Database Inventory

1. Read `_concept/06_datamodel/model.json` — all entities, relationships, field constraints
2. Read `_concept/06_datamodel/seed.json` — test data scenarios
3. Read API route files (`server/api/`, `routes/`, etc.) — map endpoints to entities
4. Read `.env.example` — database type, connection pattern
5. Read migration files — understand actual schema

Produce an endpoint inventory:

```
| Endpoint | Method | Entity | Feature | Auth Required |
|----------|--------|--------|---------|--------------|
| /api/auth/login | POST | user | Login | No |
| /api/tasks | GET | task | Task List | Yes |
| /api/tasks | POST | task | Create Task | Yes |
| /api/tasks/:id | PUT | task | Edit Task | Yes |
```

#### Sub-agent 2: Test Infrastructure

1. Detect test framework and configuration
2. Read existing integration tests (if any) for conventions
3. Determine test database strategy:
   - Separate test database (preferred)
   - Transaction rollback per test
   - Database seeding approach
4. Identify auth mechanism for protected endpoints

### Phase 2: Generate Test Infrastructure

If no integration test setup exists, generate:

#### Test Database Setup

```typescript
// Example: test setup file
beforeAll(async () => {
  // Connect to test database
  // Run migrations
  // Seed with base data from seed.json
})

afterEach(async () => {
  // Reset database to clean state (truncate or rollback)
})

afterAll(async () => {
  // Disconnect
})
```

#### Auth Helpers

```typescript
// Helper to get authenticated request context
async function asUser(role: string) {
  // Login with seed.json user for the given role
  // Return auth token/cookie
}
```

### Phase 3: Generate Integration Tests

For each feature, generate tests covering:

#### API Endpoint Tests

For each endpoint in the inventory:

```typescript
describe('POST /api/tasks', () => {
  it('creates a task with valid data', async () => {
    // Use seed.json "populated" scenario data
    const response = await request.post('/api/tasks')
      .set('Authorization', authToken)
      .send({ title: 'Test task', ... })

    expect(response.status).toBe(201)
    expect(response.body).toMatchObject({ title: 'Test task' })

    // Verify database record
    const record = await db.query('SELECT * FROM tasks WHERE id = ?', [response.body.id])
    expect(record).toBeDefined()
  })

  it('rejects invalid data', async () => {
    // Test field constraints from model.json
  })

  it('requires authentication', async () => {
    // Test without auth token
    const response = await request.post('/api/tasks').send({ ... })
    expect(response.status).toBe(401)
  })
})
```

#### Data Flow Tests

Test cross-feature interactions derived from entity relationships in model.json:

```typescript
describe('Cross-feature: Task → Dashboard', () => {
  it('creating a task updates the dashboard count', async () => {
    const before = await request.get('/api/dashboard/stats').set(...)
    await request.post('/api/tasks').set(...).send({ ... })
    const after = await request.get('/api/dashboard/stats').set(...)
    expect(after.body.taskCount).toBe(before.body.taskCount + 1)
  })
})
```

#### Data Integrity Tests

Verify database constraints from model.json:

```typescript
describe('Data integrity: Task entity', () => {
  it('has id, created_at, updated_at on creation', async () => { ... })
  it('updates updated_at on modification', async () => { ... })
  it('cascades delete to related comments', async () => { ... })
  it('enforces unique constraint on <field>', async () => { ... })
})
```

#### Seed Data Scenario Tests

Use each seed.json scenario:

| Scenario | Test Purpose |
|----------|-------------|
| `empty` | API returns empty arrays, correct default states |
| `populated` | CRUD operations on existing data |
| `edge_cases` | Boundary values, max-length fields, special characters |
| `permissions` | Role-based access (if present) |

### Phase 4: Run Tests

```bash
# Run integration tests
npm run test:integration -- --run
```

If tests fail:
- **Missing tables/columns:** report migration gap
- **Auth errors:** report auth setup issue
- **Constraint violations:** likely a bug — report it
- **Connection errors:** report database configuration issue

### Phase 5: Present Report

```
## Integration Test Generation Report

### Tests Generated
| Feature | File | Tests | Endpoints Covered |
|---------|------|-------|-------------------|
| Auth | tests/integration/auth.test.ts | 12 | 3 |
| Tasks | tests/integration/tasks.test.ts | 18 | 4 |
| Dashboard | tests/integration/dashboard.test.ts | 6 | 2 |

### Cross-Feature Tests
| Test | Features Involved | Entities |
|------|------------------|----------|
| Task creation updates dashboard | Tasks + Dashboard | task, dashboard_stats |
| User deletion cascades to tasks | Auth + Tasks | user, task |

### Test Results
- Total: N tests
- Passing: N
- Failing: N

### Issues Found
| Test | Issue | Severity | File |
|------|-------|----------|------|
| Task cascade delete | Comments not deleted | HIGH | server/api/tasks/[id].delete.ts |
| Auth rate limit | No rate limiting on login | MEDIUM | server/api/auth/login.post.ts |

### Database Coverage
| Entity | Create | Read | Update | Delete | Constraints |
|--------|--------|------|--------|--------|-------------|
| user | Yes | Yes | Yes | Yes | Yes |
| task | Yes | Yes | Yes | Yes | Partial |
```

## Outputs

- Integration test files placed according to project conventions
- Test infrastructure (setup, helpers) if not already present
- Test generation report (displayed to user)

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

> "Generated N integration test files with N test cases covering N API endpoints.
> N tests passing, N failing. Next: run `cf_test_e2e` for browser-based testing,
> or `cf_quality_verify` to verify implementation against the concept."

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Using mocks instead of real database | Habit from unit tests | Integration tests use real database — that's the point |
| Not resetting state between tests | Tests depend on order | Truncate or rollback after each test |
| Hardcoding test data | Not using seed.json | Use seed.json scenarios for consistent, meaningful test data |
| Testing only happy paths | Skipping constraint validation | Test field constraints, auth requirements, and error responses |
| Ignoring cross-feature flows | Testing endpoints in isolation | model.json relationships reveal cross-feature data dependencies |
| Using production database | Dangerous default | Always use a separate test database or transaction rollback |

## Integration

- **Upstream:** Implementation + migration (code and DB must exist), `cf_concept_functionality_features`, `cf_concept_datamodel`
- **Called by:** orchestrator or standalone
- **Downstream:** CI pipeline, `cf_quality_verify` (test results feed verification)
- **Parallel with:** `cf_test_unit` (different test scope, can run simultaneously)
- **Consumes:** `_concept/08_testing/test_plan.md` (if exists, use its scenarios)
- **Events:**
  ```
  [cf_test_integration] started
    run_id: <uuid>
  [cf_test_integration] checkpoint feature=auth tests=12 passing=11 failing=1
  [cf_test_integration] completed
    run_id: <uuid>
    features: N
    test_files: N
    tests_total: N
    endpoints_covered: N
    cross_feature_tests: N
  ```
