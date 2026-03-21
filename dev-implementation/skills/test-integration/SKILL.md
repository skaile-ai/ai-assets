---
name: test-integration
description: "Use when you need integration tests that verify API endpoints, data flow, and cross-feature interactions against a real database. Reads feature specs and data model to generate tests covering the full request-response cycle."
keywords: [testing, integration, api, database, endpoints, data-flow]
source: MIGRATED
version: 1.0.0
subagent: true
user_inputs:
  dialog: []
  files: []
---

# Test Integration — API & Data Flow Testing

## Overview

Generates integration test files that verify API endpoints, database operations,
and cross-feature data flows using a real database. Reads feature specs, screen
specs, and the data model to produce tests covering the full request-response
cycle — from HTTP request through business logic to database mutation and response.

## When to Use

- After implementation and database migration are complete
- When the user says "integration tests", "API tests", or "test data flow"
- After `test-unit` — to add the next layer of test coverage
- When testing cross-feature interactions (e.g., creating a task updates the dashboard count)

## When NOT to Use

- For testing isolated functions/composables — use `test-unit`
- For browser-based testing — use `e2e`
- For spec-fidelity checks — use `verify`
- For generating a test plan (not code) — use `test-plan`
- When no database or API exists yet — implementation and migration must come first

## Prerequisites

**Hard gates:**
1. Source code must exist with API endpoints (`server/api/`, `routes/`, or equivalent)
2. Feature specs must exist in `_concept/2_experience/2_features/`
3. Data model must exist at `_concept/3_blueprint/3_datamodel/model.json`
4. Database must be accessible (check `.env` or `.env.example` for connection details)

## Shared Contracts

Before starting, read:
- `dev-shared/contracts/concept_structure.md` — valid _concept/ paths
- `dev-shared/contracts/frontmatter.md` — feature frontmatter fields
- `dev-shared/contracts/seed_data.md` — scenario-based seed data convention
- `dev-shared/contracts/iron_laws.md` — non-negotiable constraints

## Context Budget

| Source | Priority |
|--------|----------|
| `_concept/2_experience/2_features/**/*.md` | Required |
| `_concept/3_blueprint/3_datamodel/model.json` | Required |
| `_concept/3_blueprint/3_datamodel/seed.json` | Required |
| `_concept/3_blueprint/1_techstack/stack.md` | Required |
| API route source files | Required |
| Existing test files (patterns) | Required |
| `.env.example` | Required |
| `_concept/testing/test_plan.md` | Optional |

## Workflow

### Phase 1: Discover Integration Environment

#### Sub-agent 1: API & Database Inventory

1. Read `_concept/3_blueprint/3_datamodel/model.json` — all entities, relationships, field constraints
2. Read `_concept/3_blueprint/3_datamodel/seed.json` — test data scenarios
3. Read API route files — map endpoints to entities
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
  // Seed with base data from seed.json populated scenario
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
    expect(after.body.task_count).toBe(before.body.task_count + 1)
  })
})
```

#### Data Integrity Tests

Verify database constraints from model.json:

```typescript
describe('Data integrity: task entity', () => {
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
| `single_user` | Basic operations with minimal data |
| `populated` | CRUD operations on existing data |
| `edge_cases` | Boundary values, max-length fields, special characters |

### Phase 4: Run Tests

Run integration tests. If tests fail:
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

### Cross-Feature Tests
| Test | Features Involved | Entities |
|------|------------------|----------|
| Task creation updates dashboard | Tasks + Dashboard | task, dashboard_stats |

### Test Results
- Total: N tests
- Passing: N
- Failing: N

### Issues Found
| Test | Issue | Severity | File |
|------|-------|----------|------|
| Task cascade delete | Comments not deleted | HIGH | server/api/tasks/[id].delete.ts |

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

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Using mocks instead of real database | Integration tests use real database — that's the point |
| Not resetting state between tests | Truncate or rollback after each test |
| Hardcoding test data | Use seed.json scenarios for consistent, meaningful test data |
| Testing only happy paths | Test field constraints, auth requirements, and error responses |
| Ignoring cross-feature flows | model.json relationships reveal cross-feature data dependencies |
| Using production database | Always use a separate test database or transaction rollback |

EMIT  [test-integration] started run_id=<uuid>
EMIT  [test-integration] checkpoint feature=<name> tests=<N> passing=<N> failing=<N>
EMIT  [test-integration] completed run_id=<uuid> features=<N> test_files=<N> tests_total=<N> endpoints_covered=<N> cross_feature_tests=<N>
