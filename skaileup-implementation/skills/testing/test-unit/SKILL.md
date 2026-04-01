---
name: "test-unit"
description: "Use when you need unit test files generated from feature specs. Reads existing source code patterns and feature requirements, then produces one test file per feature with test cases mapped to requirements."
metadata:
  version: "1.0.0"
  tags:
    - "testing"
    - "unit-tests"
    - "vitest"
    - "jest"
    - "code-generation"
    - "tdd"
  source: "MIGRATED"
  subagent: true
  prerequisites:
    files:
      - path: "package.json"
        gate: hard
        description: "Source code must exist — unit tests require implemented functions to test"
      - path: "_concept/2_experience/2_features"
        gate: hard
        description: "Feature specs required — each requirement becomes a test case"
        min_entries: 1
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        gate: hard
        description: "Tech stack required to select correct test framework (vitest/jest)"
    reads:
      - path: "_concept/testing/test_plan.md"
        description: "Test plan for structured test scenario coverage"
    produces:
      - path: "src"
        description: "One test file per feature (in stack-appropriate test directory)"
---

# Test Unit — Feature-Driven Unit Test Generation

## Overview

Generates unit test files from feature specifications. Reads the existing source
code to understand test framework, patterns, and conventions, then produces one
test file per feature where each requirement becomes a test case. Tests cover
individual functions, composables, utilities, and component logic in isolation.

## When to Use

- After implementation exists — to add test coverage for implemented features
- When the user says "generate unit tests", "write tests", or "test coverage"
- After `test-plan` — to generate executable tests from the plan
- As part of a TDD workflow — to generate test stubs before implementation

## When NOT to Use

- For API endpoint or database tests — use `test-integration`
- For browser-based testing — use `e2e`
- For spec-fidelity checks — use `verify`
- For generating a test plan (not code) — use `test-plan`
- When no source code exists at all — run implementation first

## Prerequisites

**Hard gates:**
1. Source code must exist (`package.json`, `pyproject.toml`, or equivalent)
2. Feature specs must exist in `_concept/2_experience/2_features/`
3. Tech stack must be known — `_concept/3_blueprint/1_techstack/stack.md` or infer from `package.json`

## Shared Contracts

Before starting, read:
- `skaileup-shared/contracts/concept_structure.md` — valid _concept/ paths
- `skaileup-shared/contracts/frontmatter.md` — feature frontmatter fields
- `skaileup-shared/contracts/iron_laws.md` — non-negotiable constraints

## Context Budget

| Source | Priority |
|--------|----------|
| `_concept/2_experience/2_features/**/*.md` | Required |
| `_concept/3_blueprint/1_techstack/stack.md` | Required |
| `package.json` (deps + scripts) | Required |
| Existing test files (pattern discovery) | Required |
| Source code for features under test | Required |
| `_concept/testing/test_plan.md` | Optional |

## Workflow

### Phase 1: Discover Test Environment

#### Sub-agent 1: Test Framework Detection

Read project configuration to determine:

1. **Test runner:** vitest, jest, pytest, go test, etc.
2. **Test file convention:** `*.test.ts`, `*.spec.ts`, `__tests__/`, etc.
3. **Assertion library:** expect (vitest/jest), assert, chai, etc.
4. **Mocking approach:** vi.mock, jest.mock, unittest.mock, etc.
5. **Existing test patterns:** read 2-3 existing test files to learn conventions
6. **Test config:** `vitest.config.ts`, `jest.config.js`, `pytest.ini`, etc.

If no test framework is configured:
> "No test framework detected. Recommend adding vitest (for Nuxt/Vue) or jest. Install it first?"

#### Sub-agent 2: Feature-to-Source Mapping

For each feature in `_concept/2_experience/2_features/`:

1. Read the feature spec — extract requirements and success criteria
2. Find the corresponding source files (pages, components, composables, API routes)
3. Identify testable units: exported functions, composables, utility methods, API handlers
4. Map each requirement to one or more testable units

Return: feature → source file → testable units → requirements mapping.

### Phase 2: Generate Test Files

For each feature, create one test file following the project's conventions.

#### Test File Structure

```typescript
// Example for vitest + Vue/Nuxt
import { describe, it, expect, vi } from 'vitest'

describe('Feature: <feature_name>', () => {
  describe('<Requirement 1 from spec>', () => {
    it('should <expected behavior>', () => {
      // Arrange — setup using patterns found in codebase
      // Act — call the function/composable
      // Assert — verify against requirement
    })

    it('should handle <error state from spec>', () => {
      // Error state test
    })
  })

  describe('<Requirement 2 from spec>', () => {
    // ...
  })
})
```

#### Conventions to Follow

- **File placement:** match existing test file locations (colocated or `__tests__/`)
- **Naming:** match existing pattern (`*.test.ts` or `*.spec.ts`)
- **Imports:** use the same import style as existing tests
- **Mocking:** mock external dependencies the same way existing tests do
- **One file per feature:** group all tests for a feature together
- **Describe blocks map to requirements:** each requirement checkbox = one `describe`
- **Test names reference spec:** include the requirement text in test description

#### What to Test

| Source Type | What to Test |
|-------------|-------------|
| Composables | Return values, reactivity, error handling |
| Utility functions | Input/output, edge cases, type handling |
| API handlers | Request parsing, response shape, error responses |
| Store/state | Mutations, getters, actions, initial state |
| Validators | Valid inputs, invalid inputs, boundary values |

#### What NOT to Test (leave for integration/E2E)

- Database queries (integration)
- Full HTTP request/response cycles (integration)
- Visual rendering (E2E)
- Cross-feature interactions (integration)
- Browser behavior (E2E)

### Phase 3: Verify Tests Run

Run the generated tests to verify they at least parse and execute. If tests fail due
to missing mocks or imports, fix them. If tests fail due to actual bugs found —
report them, do not change the test.

### Phase 4: Present Report

```
## Unit Test Generation Report

### Tests Generated
| Feature | File | Tests | Requirements Covered |
|---------|------|-------|---------------------|
| Login | tests/auth/login.test.ts | 8 | 4/4 |
| Dashboard | tests/dashboard/overview.test.ts | 12 | 6/6 |

### Test Results
- Total: N tests
- Passing: N
- Failing: N (may indicate bugs in implementation)

### Uncoverable Requirements
| Feature | Requirement | Reason |
|---------|------------|--------|
| Login | OAuth redirect | Requires browser (E2E) |
| Dashboard | Real-time updates | Requires WebSocket (integration) |

### Potential Bugs Found
| Test | Expected | Actual | File |
|------|----------|--------|------|
| Login error message | "Invalid credentials" | Returns 500 | server/api/auth/login.ts:42 |
```

## Outputs

- One test file per feature, placed according to project conventions
- Test generation report (displayed to user)

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Ignoring existing test patterns | Read 2-3 existing tests first and match conventions exactly |
| Testing implementation details | Test behavior described in the requirement, not internal methods |
| Generating tests that need a database | Mock all external dependencies; leave DB tests for integration |
| Writing snapshot tests for everything | Only snapshot when the output shape matters to the requirement |
| Skipping error state tests | Every error state in the feature spec needs a test |
| Creating tests in the wrong location | Match the existing test file layout exactly |

EMIT  [test-unit] started run_id=<uuid>
EMIT  [test-unit] checkpoint feature=<name> tests=<N> passing=<N> failing=<N>
EMIT  [test-unit] completed run_id=<uuid> features=<N> test_files=<N> tests_total=<N> tests_passing=<N> tests_failing=<N>
