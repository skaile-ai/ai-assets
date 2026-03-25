---
name: "skaildev-run-tests"
description: "Test construction and execution for the skaile-dev monorepo. Two modes: 'run' executes the test suite for one or more packages and reports results; 'construct' generates new tests for recently implemented code. Knows the full test stack: vitest (forge apps), Jest (platform backend), Vitest (platform frontend), Playwright (E2E), and how to run each via the Bun workspace."
metadata:
  version: "1.0.0"
  tags:
    - "testing"
    - "vitest"
    - "jest"
    - "playwright"
    - "bun"
    - "monorepo"
    - "skaile-dev"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
      - path: "package.json"
        gate: hard
        description: "Monorepo root package.json required"
  user_inputs:
    dialog:
      - id: "mode"
        label: "Mode: 'run' (execute tests) or 'construct' (generate new tests)"
        type: "select"
        options:
          - "run"
          - "construct"
        required: true
        default: "run"
      - id: "scope"
        label: "Package(s) to test (comma-separated, or 'all' for full suite)"
        type: "text"
        required: false
        default: "all"
        hint: "e.g. 'forge/project', 'platform/backend', 'agent-framework/cli,arm'"
      - id: "filter"
        label: "Test name filter (for 'run' mode — runs only matching tests)"
        type: "text"
        required: false
    files: []
---

# Run Tests — Test Construction and Execution

## Overview

Manages the test lifecycle for the skaile-dev monorepo. Works in two modes:

| Mode | What It Does |
|------|-------------|
| `run` | Execute the test suite for specified packages, report results, triage failures |
| `construct` | Generate new tests for recently implemented code based on source analysis |

## Test Stack by Package

| Package | Framework | Run Command |
|---------|-----------|-------------|
| `forge/project` | Vitest | `bun x --bun vitest run --project forge-project` |
| `forge/concept` | Vitest | `bun x --bun vitest run --project forge-concept` |
| `forge/common-backend` | Vitest | `bun x --bun vitest run --project forge-common-be` |
| `forge/common-ui` | Vitest | `bun x --bun vitest run --project forge-common-ui` |
| `platform/backend` | Jest | `bun run test --filter ./platform/backend` |
| `platform/frontend` | Vitest | `bun run test --filter ./platform/frontend` |
| `platform/e2e` | Playwright | `bun run e2e --filter ./platform/e2e` |
| `agent-framework/cli` | Vitest | `bun x --bun vitest run --project agent-cli` |
| `agent-framework/runner` | Vitest | `bun x --bun vitest run --project agent-runner` |
| `agent-framework/bridge` | Vitest | `bun x --bun vitest run --project agent-bridge` |
| `agent-framework/flow-engine` | Vitest | `bun x --bun vitest run --project agent-flow-engine` |
| `agent-framework/resolver` | Vitest | `bun x --bun vitest run --project agent-resolver` |
| `ai-resource-manager` | Pytest | `cd ai-resource-manager && uv run pytest` |
| All (JS/TS) | Vitest/Jest | `bun x --bun vitest run` |

## When to Use

- After any code change — verify nothing broke (`mode=run`)
- After implementing a new feature — add test coverage (`mode=construct`)
- Triaging CI failures — investigate specific failing tests (`mode=run, filter=<name>`)
- Before opening a PR — full suite pass is required

## When NOT to Use

- For E2E tests in the full skaile pipeline — use `dev-quality/e2e`
- For acceptance-criteria verification against `_concept/` specs — use `verify`

---

ROLE  Test executor and constructor for the skaile-dev monorepo — runs tests, triages failures, generates new tests from source analysis.

READS
  package.json (root + per-package)           — workspace config, test scripts
  <package>/CLAUDE.md                         — package conventions for test patterns
  <package>/vitest.config.ts or jest.config.* — test configuration
  existing test files                          — patterns to follow when constructing
  recently changed source files (construct mode) — what to test

MUST  read existing test files before constructing new ones — match conventions exactly
MUST  run tests before reporting results — never report without running
MUST  triage failures: distinguish regressions from new failures vs. infrastructure issues
MUST  distinguish test failures (bugs) from test construction errors (bad test code)
NEVER modify a test to make it pass — fix the source code instead
NEVER skip failing tests by commenting them out
NEVER report "all passing" without actually running the suite

EMIT [skaildev-run-tests] started mode=<mode> scope=<packages>

# ── Mode: run ─────────────────────────────────────────────────────

IF mode = run

  STEP 1: Resolve scope
    IF scope = all
      - Run full monorepo test suite
    ELSE
      - Parse scope into package list
      - Derive correct run command per package from the test stack table above

  STEP 2: Execute
    FOR EACH package in scope:
      - $ <run command for package> [--reporter=verbose] [--testNamePattern=<filter>]
      - Capture: total, passed, failed, skipped, duration
      EMIT [skaildev-run-tests] package_result package=<name> total=<n> passed=<n> failed=<n>

  STEP 3: Triage failures
    FOR EACH failing test:
      - Read the test file and the test case
      - Read the source file(s) it tests
      - Classify failure:
        - REGRESSION: previously passing test now fails due to code change
        - NEW_FAILURE: test was already failing before this change (check git blame)
        - INFRA_ISSUE: database not available, port conflict, missing env var
        - BAD_TEST: test itself is incorrectly written (assertion is wrong, not the source)
      - Report classification with evidence

  STEP 4: Present results report

    ```
    ## Test Results — <date>

    ### Summary
    | Package | Total | Passed | Failed | Skipped | Duration |
    |---------|-------|--------|--------|---------|----------|
    | forge/project | 42 | 42 | 0 | 0 | 1.2s |
    | platform/backend | 156 | 154 | 2 | 0 | 8.4s |
    ...
    | **Total** | **N** | **N** | **N** | **N** | **Ns** |

    ### Failures (if any)
    | Test | Package | File | Classification | Summary |
    |------|---------|------|----------------|---------|
    | "creates session token" | platform/backend | ... | REGRESSION | token field renamed in schema |

    ### Recommended Actions
    - Fix: <file>:<line> — <what to change>
    - Investigate: <infra issue description>
    ```

  EMIT [skaildev-run-tests] run_complete total=<n> passed=<n> failed=<n>

  IF any REGRESSION failures
    - Report: "These tests were passing before. Fix before proceeding."
  IF any BAD_TEST failures
    - Report: "These tests appear to have incorrect assertions. Review before fixing source."
  IF any INFRA_ISSUE failures
    - Report: "Infrastructure issues detected — resolve environment before re-running."

# ── Mode: construct ───────────────────────────────────────────────

IF mode = construct

  STEP 1: Identify what needs tests
    IF scope is provided → look for untested code in those packages
    ELSE
      - Use `git diff --name-only HEAD~1..HEAD` to find recently changed source files
      - Filter to testable files: source files (not tests, not configs)

  STEP 2: Discover test environment per package
    - Read existing test files for the package
    - Read vitest.config.ts / jest.config.* for configuration
    - Note: test file naming convention (`*.test.ts` or `*.spec.ts`)
    - Note: test directory convention (colocated or `__tests__/`)
    - Note: import style, mock patterns, assertion library
    - Read 2–3 existing tests to internalize the style

  STEP 3: Identify testable units
    FOR EACH changed source file:
      - Identify exported functions, composables, classes, API handlers, store actions
      - For each: determine what behavior is observable from outside (not implementation)
      - Classify as: unit-testable, integration-testable, or E2E-only
      - Map to existing tests: is there already a test file for this unit?

  STEP 4: Generate test files
    FOR EACH unit without test coverage:
      - Create test file following the package's naming and placement convention
      - Structure: describe blocks map to exported units; it blocks map to behaviors
      - Test names reference observable behavior, not internal method names
      - Use the same mocking patterns as existing tests
      - Prioritize: happy path, error path, boundary values
      - Do NOT test internal implementation details

    Test structure template (adapt to framework):
    ```typescript
    describe('<ExportedUnit>', () => {
      describe('<behavior group from docs/spec>', () => {
        it('should <observable outcome>', () => {
          // Arrange
          // Act
          // Assert — test the observable contract, not internals
        })

        it('should handle <error case>', () => {
          // ...
        })
      })
    })
    ```

  STEP 5: Run generated tests
    - $ <run command for package>
    IF tests fail due to missing setup (mocks, providers) → fix setup
    IF tests fail because the source has a bug → report it; do NOT fix the test
    IF tests fail because the test is wrong → fix the test

  STEP 6: Present construction report

    ```
    ## Test Construction Report

    ### Generated
    | File | Package | Tests | Units Covered |
    |------|---------|-------|---------------|
    | src/composables/useWorkspace.test.ts | forge/project | 8 | 2 |

    ### Coverage Added
    | Unit | File | Was Tested | Now Tested |
    |------|------|-----------|-----------|
    | useWorkspace | src/composables/useWorkspace.ts | No | Yes |

    ### Skipped (E2E-only)
    | Unit | Reason |
    |------|--------|
    | WorkspacePage.vue | Full browser interaction — needs Playwright |

    ### Results
    - New tests: N
    - Passing: N
    - Failing (source bugs): N (see below)
    ```

  EMIT [skaildev-run-tests] construct_complete files_created=<n> tests_generated=<n> passing=<n>

CHECKLIST
  - [ ] Existing test patterns read before constructing new tests
  - [ ] Tests match package naming and placement conventions
  - [ ] Tests cover observable behavior, not implementation details
  - [ ] All generated tests run without infrastructure errors
  - [ ] Failures triaged (regression vs. new vs. infra vs. bad test)
  - [ ] Source bugs reported (tests not modified to hide them)

---

## Bun Workspace Test Commands (Quick Reference)

```bash
# Run all JS/TS tests (monorepo-wide)
bun x --bun vitest run

# Run with watch mode (development)
bun x --bun vitest

# Run a specific package
bun x --bun vitest run --project forge-project

# Run with test name filter
bun x --bun vitest run -t "workspace rename"

# Run platform backend (Jest)
bun run test --filter ./platform/backend

# Run platform E2E (Playwright)
bun run e2e --filter ./platform/e2e

# Run Python CLI tests
cd ai-resource-manager && uv run pytest -v

# Run with coverage
bun x --bun vitest run --coverage
```

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Modifying tests to make them pass | Fix the source code — tests are the spec |
| Running only the new tests | Run the full package suite to catch regressions |
| Constructing tests without reading existing ones | Always read 2–3 existing tests first — conventions vary |
| Testing implementation details | Test observable behavior only |
| Skipping infra failure investigation | INFRA failures mask real bugs — fix environment first |

## Integration

- **Called by:** `implement-skaile` (after each task and before finish)
- **Calls:** none
- **Escalates to:** `dev-quality/e2e` for full E2E suite; `dev-quality/test-unit` for spec-driven test generation
