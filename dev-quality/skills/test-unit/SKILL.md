---
name: test-unit
description: "Use when you need unit test files generated from feature specs. Reads existing source code patterns and feature requirements, then produces one test file per feature with test cases mapped to requirements."
keywords: [testing, unit-tests, vitest, jest, code-generation, tdd]
subagent: true
user_inputs:
  dialog: []
  files: []
---

# App Test Unit — Feature-Driven Unit Test Generation

## Overview

Generates unit test files from feature specifications. Reads the existing source
code to understand test framework, patterns, and conventions, then produces one
test file per feature where each requirement becomes a test case. Tests cover
individual functions, composables, utilities, and component logic in isolation.

## When to Use

- After implementation exists — to add test coverage for implemented features
- When the user says "generate unit tests", "write tests", or "test coverage"
- After `cf_test_plan` — to generate executable tests from the plan
- As part of a TDD workflow — to generate test stubs before implementation

## When NOT to Use

- For API endpoint or database tests — use `cf_test_integration`
- For browser-based testing — use `cf_test_e2e`
- For spec-fidelity checks — use `cf_quality_verify`
- For generating a test plan (not code) — use `cf_test_plan`
- When no source code exists at all — run implementation first

## Prerequisites

<HARD-GATE> Source code must exist. Check for `package.json`, `nuxt.config.ts`, `pyproject.toml`, or equivalent. If missing: "No application source found. Implementation must exist before generating unit tests."

<HARD-GATE> Feature specs must exist in `_concept/03_features/`. If missing: "No feature specs found. Run `cf_concept_functionality_features` first."

<HARD-GATE> Tech stack must be known. Check `_concept/05_techstack/stack.md` or infer from `package.json`. If neither exists: "Cannot determine test framework. Run `cf_concept_techstack` or ensure package.json exists."

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** Source code (package.json or equivalent), feature specs (`03_features/`), tech stack (`05_techstack/stack.md` or package.json)
**If gates fail:** Run `cf_concept_functionality_features` or `cf_concept_techstack` as needed
**On completion:** Present summary, then suggest next steps.

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid _concept/ paths
- `cf__shared/frontmatter.md` — feature frontmatter fields
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

| Source | Token estimate | Priority |
|--------|---------------|----------|
| `_concept/03_features/**/*.md` | ~3000 | Required |
| `_concept/05_techstack/stack.md` | ~500 | Required |
| `package.json` (deps + scripts) | ~500 | Required |
| Existing test files (pattern discovery) | ~2000 | Required |
| Source code for features under test | ~4000 | Required |
| `_concept/08_testing/test_plan.md` | ~2000 | Optional |

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

For each feature in `_concept/03_features/`:

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

```bash
# Run the generated tests to verify they at least parse and execute
npm run test -- --run <test-file-pattern>
```

If tests fail due to missing mocks or imports, fix them.
If tests fail due to actual bugs found — report them, do not change the test.

### Phase 4: Present Report

```
## Unit Test Generation Report

### Tests Generated
| Feature | File | Tests | Requirements Covered |
|---------|------|-------|---------------------|
| Login | tests/auth/login.test.ts | 8 | 4/4 |
| Dashboard | tests/dashboard/overview.test.ts | 12 | 6/6 |
| Settings | tests/settings/preferences.test.ts | 5 | 3/3 |

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

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

> "Generated N test files with N test cases covering N requirements. N tests
> passing, N failing (potential bugs). Next: run `cf_test_integration` for API/data flow
> tests, or `cf_test_e2e` for browser-based testing."

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Ignoring existing test patterns | Writing tests in a different style | Read 2-3 existing tests first and match conventions exactly |
| Testing implementation details | Coupling to internal structure | Test behavior described in the requirement, not internal methods |
| Generating tests that need a database | Blurring unit vs. integration | Mock all external dependencies; leave DB tests for integration |
| Writing snapshot tests for everything | Lazy coverage | Only snapshot when the output shape matters to the requirement |
| Skipping error state tests | Only testing happy paths | Every error state in the feature spec needs a test |
| Creating tests in the wrong location | Not checking project structure | Match the existing test file layout exactly |

## Integration

- **Upstream:** Implementation (code must exist), `cf_concept_functionality_features` (specs), `cf_concept_techstack` (framework)
- **Called by:** orchestrator or standalone
- **Downstream:** CI pipeline, `cf_quality_verify` (test results feed verification)
- **Parallel with:** `cf_test_integration` (different test scope, can run simultaneously)
- **Consumes:** `_concept/08_testing/test_plan.md` (if exists, use its scenarios)
- **Events:**
  ```
  [cf_test_unit] started
    run_id: <uuid>
  [cf_test_unit] checkpoint feature=login tests=8 passing=7 failing=1
  [cf_test_unit] completed
    run_id: <uuid>
    features: N
    test_files: N
    tests_total: N
    tests_passing: N
    tests_failing: N
  ```
