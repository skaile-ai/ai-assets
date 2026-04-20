---
name: "test-unit"
description: "Set up and generate unit tests for any skaile-dev package (forge apps, agent-framework libraries, platform frontend/backend). Reads TEST_PLAN.md when present, otherwise discovers testable units from source. Scaffolds missing test infra (vitest.config.ts, test harness), then generates one test file per module with test cases mapped to CLAUDE.md Architecture. Verifies tests run before completing."
metadata:
  version: "1.0.0"
  tags:
    - "testing"
    - "unit-tests"
    - "vitest"
    - "jest"
    - "setup"
    - "generation"
    - "tdd"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
      - path: "package.json"
        gate: hard
        description: "Monorepo root package.json required"
    inputs_required:
      - id: target
        label: "Package path"
        type: text
        hint: "e.g. forge/project, agent-framework/runner, platform/backend"
    inputs_optional:
      - id: mode
        label: "Mode"
        type: select
        options:
          - "setup"
          - "generate"
          - "both"
        default: "both"
        hint: "setup = test infra only | generate = tests only | both = setup then generate"
      - id: scope
        label: "Scope within package"
        type: text
        hint: "Glob of source paths to cover (defaults to entire src/)"
    reads:
      - path: "<target>/CLAUDE.md"
        description: "Architecture + public API — source of truth for what to test"
      - path: "<target>/TEST_PLAN.md"
        description: "Optional plan from test-plan skill"
      - path: "<target>/src"
        description: "Source to analyze for testable units"
      - path: "ai-assets/skaile-development/references/test_stack_map.md"
        description: "Package → framework + run command"
    produces:
      - path: "<target>/vitest.config.ts"
        description: "Test config (scaffolded if missing)"
      - path: "<target>/tests/**/*.test.ts"
        description: "Generated unit test files"
  user_inputs:
    dialog:
      - id: "target"
        label: "Package path"
        type: "text"
        required: true
      - id: "mode"
        label: "Mode"
        type: "select"
        options: ["setup", "generate", "both"]
        required: false
        default: "both"
      - id: "scope"
        label: "Source glob (optional)"
        type: "text"
        required: false
    files: []
---

# Test Unit — Unit Test Setup & Generation

## Overview

Sets up unit testing infrastructure and generates unit tests for any package in the skaile-dev monorepo.

| Mode | What it does |
|---|---|
| `setup` | Scaffolds missing test infrastructure: `vitest.config.ts` (or ensures Jest config for platform/backend), test directory, baseline setup file |
| `generate` | Generates one test file per testable unit. Requires infrastructure to exist. |
| `both` (default) | Runs setup first, then generate |

Unit tests cover isolated pure-logic modules: exported functions, composables, utilities, type guards, validators, store actions. They never touch filesystem, DB, network, or browser — those belong to `test-integration` / `test-e2e`.

## When to Use

- Starting unit test coverage for an agent-framework library (several have zero coverage today)
- Adding tests after implementing new composables / utilities in a forge app
- After a refactor that invalidates existing tests
- As the first layer of `test-plan` → `test-unit` → `test-integration` → `test-e2e`

## When NOT to Use

- For API endpoint tests — use `test-integration`
- For browser journeys — use `test-e2e`
- For running existing tests — use `test`

---

ROLE  Unit test setup + generator for a single package. Scaffolds missing test infra, then produces one test file per testable unit.

READS
  ! <target>/CLAUDE.md                       — Architecture + public API
  ? <target>/TEST_PLAN.md                    — preferred input when present
  ! <target>/package.json                    — detect existing framework
  ! <target>/src/**                          — testable source
  ? <target>/{tests,test}/**                 — existing patterns to match
  ! ai-assets/skaile-development/references/test_stack_map.md

WRITES
  <target>/vitest.config.ts (or jest.config.*)   — only if missing
  <target>/tests/setup.ts                        — shared test setup (only if missing)
  <target>/tests/**/*.test.ts                    — generated per-module test files

MUST  read CLAUDE.md before generating any test
MUST  detect and respect the framework already in use (vitest for most; jest for platform/backend)
MUST  read 2–3 existing test files to match conventions (import style, mock patterns, naming)
MUST  prefer TEST_PLAN.md as input when it exists
MUST  verify generated tests execute before completing
MUST  place test files per package convention (tests/, test/unit/, __tests__/, or colocated)
NEVER modify existing test files — only add new ones, or refuse and ask
NEVER test implementation details (private methods, internal state)
NEVER use mocks for pure logic — if it needs mocks, it's integration
NEVER hide a real bug by weakening an assertion

EMIT [test-unit] started target=<pkg> mode=<mode>

# ── Phase 1: Validate & Classify ──────────────────────────────────

STEP 1: Load context
  - Validate <target>/package.json exists
  - Read <target>/CLAUDE.md — note Architecture + Tech Stack
  - Read <target>/TEST_PLAN.md if present
  - Look up framework + run command from test_stack_map.md

STEP 2: Detect current state
  - config_exists = <target>/(vitest.config.ts|jest.config.*) present?
  - tests_dir = tests/ | test/ | test/unit/ | __tests__/ | colocated
  - setup_file = tests/setup.ts or equivalent present?
  - example_tests = 2–3 existing *.test.ts to learn patterns

# ── Phase 2: Setup ────────────────────────────────────────────────

STEP 3 (skip if mode=generate): Scaffold missing infrastructure

  IF framework not configured:
    FOR forge apps / agent-framework / platform/frontend:
      - Add devDependency: vitest if not present
      - Write <target>/vitest.config.ts with:
        ```typescript
        import { defineConfig } from 'vitest/config'
        export default defineConfig({
          test: {
            include: ['tests/**/*.test.ts', 'src/**/*.test.ts'],
            exclude: ['**/node_modules/**', '**/dist/**', 'tests/e2e/**'],
            testTimeout: 15000,
            globals: false,   // prefer explicit imports
          },
        })
        ```
    FOR platform/backend:
      - Jest is already wired via NestJS defaults — do not scaffold new config
      - Ensure <target>/test/ exists
  IF tests dir is missing:
    - Create <target>/tests/
  IF setup file is missing and framework needs one (e.g. Nuxt test utils):
    - Write <target>/tests/setup.ts with minimal globals

  Also ensure the package's workspace entry in root `vitest.config.ts` defines a project name matching test_stack_map.md (e.g. forge-project, agent-runner). Add the project if missing.

EMIT [test-unit] setup_done target=<pkg> scaffolded=<list>

# ── Phase 3: Discovery ────────────────────────────────────────────

STEP 4 (skip if mode=setup): Select units to cover

  IF TEST_PLAN.md exists:
    - Extract unit-layer units from the plan
  ELSE:
    - Glob <target>/src/**/*.ts
    - Exclude: *.test.ts, *.spec.ts, *.d.ts, generated, dist
    - For each file, extract exported functions/classes/composables
    - Skip files importing: fs, node:fs, pg, prisma, drizzle-orm, @nuxt/schema server, 'axios', 'undici', 'playwright'
      (those belong to integration/e2e)

  Build: list of (file, exports[], existing_test?).

STEP 5: Match conventions
  - Read 2–3 existing test files in the package
  - Note: import style, assertion style (expect / assert), mocking (vi.mock vs jest.mock), file naming
  - Note test-file placement (colocated or tests/ mirroring src/)

# ── Phase 4: Generation ───────────────────────────────────────────

STEP 6: For each uncovered unit, generate a test file

  File location: mirror src/ into tests/ (or match existing package layout)
  File name: `<module>.test.ts`

  Template (vitest):
  ```typescript
  import { describe, it, expect } from 'vitest'
  import { <exports> } from '<relative path to module>'

  describe('<module>', () => {
    describe('<exportedSymbol>', () => {
      it('returns <expected behavior> for <happy-path input>', () => {
        // Arrange
        const input = ...
        // Act
        const result = <exportedSymbol>(input)
        // Assert
        expect(result).toEqual(...)
      })

      it('handles <edge-case input>', () => {
        // ...
      })

      it('throws for <invalid input>', () => {
        expect(() => <exportedSymbol>(<bad>)).toThrow(<ErrorType>)
      })
    })
  })
  ```

  Template (jest / platform-backend):
  ```typescript
  import { <exports> } from '<path>'

  describe('<module>', () => {
    describe('<exportedSymbol>', () => {
      it('returns <expected> for <input>', () => {
        expect(<exportedSymbol>(<input>)).toEqual(<output>)
      })
    })
  })
  ```

  Rules:
  - One describe per exported symbol; nested describes for sub-behaviors
  - Each it-block tests one observable outcome
  - Cover: happy + 1 edge + 1 error path per export
  - Use types from the source module directly — no `as any`
  - No mocks beyond `vi.fn()` for simple callbacks
  - Match existing file's import ordering and quote style

STEP 7: Frontend-specific generation (platform/frontend, forge/common-ui)
  - For hooks/composables: use `renderHook` (React) or `mount` via vue-test-utils; skip DOM-heavy tests (those are e2e)
  - For pure components (stateless): render-and-assert DOM once
  - Complex component logic → move to integration; do not test here

STEP 8: Agent-framework-specific generation
  - For manifest parsing, flow-engine transitions, resolver logic: heavily unit-testable — generate thorough coverage
  - For runner.ts / session.ts / bridge drivers: limit unit tests to pure helpers; real tests go to integration

# ── Phase 5: Verify ───────────────────────────────────────────────

STEP 9: Run the generated tests
  - Execute the package's test command from test_stack_map.md
  - Capture pass/fail/error

  IF infrastructure errors (imports, mocks, setup):
    - Fix them — this is test scaffolding work, not production bug
  IF assertions fail because the source is wrong:
    - REPORT the bug to the user; do NOT weaken the test
  IF assertion fails because the test has a wrong expectation:
    - Fix the test to match the real (correct) behavior

# ── Phase 6: Report ───────────────────────────────────────────────

STEP 10: Present report
  ## Unit Test Generation — <target>

  ### Infrastructure
  - [x] vitest.config.ts present
  - [x] tests/setup.ts present
  - [x] Workspace project registered in root vitest.config.ts

  ### Generated
  | Module | File | Tests | Status |
  |---|---|---|---|
  | src/lib/skill-manifest.ts | tests/lib/skill-manifest.test.ts | 6 | ✓ pass |
  | src/resolver/parse.ts | tests/resolver/parse.test.ts | 8 | ✓ pass |

  ### Results
  - Total new tests: N
  - Passing: N
  - Failing (likely source bugs): N
    - <test name> — expected X but got Y in <file:line>

  ### Deferred
  | Unit | Reason |
  |---|---|
  | src/runner.ts:startSession | Requires filesystem — move to test-integration |
  | src/ui/chat-panel.tsx | Browser-dependent — move to test-e2e |

EMIT [test-unit] completed target=<pkg> files=<N> tests=<N> passing=<N> failing=<N>

CHECKLIST
  - [ ] CLAUDE.md read before generation
  - [ ] Framework detected and respected
  - [ ] Existing test patterns matched
  - [ ] Infrastructure scaffolded if missing
  - [ ] One test file per testable module
  - [ ] Generated tests executed and passing (or source bugs clearly reported)
  - [ ] Deferred units flagged with reason

---

## Integration

- **Called by:** `test-plan` (suggests test-unit as next step), `implement` (when adding new pure logic), `quality`
- **Reads:** `<target>/CLAUDE.md`, `<target>/TEST_PLAN.md`, `<target>/src/**`, `test_stack_map.md`
- **Writes:** `<target>/vitest.config.ts` (if missing), `<target>/tests/**/*.test.ts`
