---
name: "test-unit"
description: "Set up and generate unit tests for any skaile-dev package (forge apps, agent-framework libraries, platform frontend/backend). Reads TEST_PLAN.md when present, otherwise discovers testable units from source. Scaffolds missing test infra (vitest.config.ts, test harness), then generates one test file per module with test cases mapped to CLAUDE.md Architecture. Verifies tests run before completing."
metadata:
  version: "1.1.0"
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
      - path: "_devlog/specs/2026-04-22-test-concept-design.md"
        description: "Canonical layer taxonomy (L0-L5) + shared infrastructure"
      - path: "_devlog/plans/2026-04-22-test-gap-fill.md"
        description: "Phase plan with per-package targets"
      - path: "agent-framework/test-utils/CLAUDE.md"
        description: "Shared helpers (makeTempDir, makeInMemoryTransport, MockWebSocket, ...)"
      - path: "docs/src/content/docs/testing.md"
        description: "User-facing Starlight overview"
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

### Canonical documents (read these first)

The 2026-04-22 test gap-fill initiative established the layered test strategy and the shared
helper package. Stay aligned with:

- `_devlog/specs/2026-04-22-test-concept-design.md` — layer taxonomy (L0–L5), shared infrastructure, coverage policy.
- `_devlog/plans/2026-04-22-test-gap-fill.md` — phase-by-phase work, per-package targets.
- `_devlog/reports/coverage-baseline-2026-04-22/summary.json` — per-package baseline.
- `docs/src/content/docs/testing.md` — user-facing Starlight overview.
- `agent-framework/test-utils/CLAUDE.md` + `src/index.ts` — shared helpers: `makeTempDir`, `cleanupTempDir`, `registerCleanup`, `makeMockDriver`, `makeInMemoryTransport`, `MockWebSocket`, `installMockWebSocket`, `loadFixtureWorkspace`, `withTempProject`.
- Root `CLAUDE.md` § "Testing Strategy".

### Modes

| Mode | What it does |
|---|---|
| `setup` | Scaffolds missing test infrastructure: `vitest.config.ts` (or ensures Jest config for platform/backend), test directory, baseline setup file, registers `@skaile/test-utils` where needed |
| `generate` | Generates one test file per testable unit. Requires infrastructure to exist. |
| `both` (default) | Runs setup first, then generate |

Unit tests cover L1 pure-logic modules (no I/O) and L2 I/O-bound units isolated via in-process
mocks (`makeTempDir` for filesystem, `makeInMemoryTransport` for WS, `MockWebSocket` +
`installMockWebSocket` for global replacement). They never touch real DBs, real network, or
browsers — those belong to `test-integration` / `test-e2e`.

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
  ! _devlog/specs/2026-04-22-test-concept-design.md
  ! _devlog/plans/2026-04-22-test-gap-fill.md
  ! agent-framework/test-utils/src/index.ts   — the surface of shared helpers to import

WRITES
  <target>/vitest.config.ts (or jest.config.*)   — only if missing
  <target>/tests/setup.ts                        — shared test setup (only if missing)
  <target>/tests/**/*.test.ts                    — generated per-module test files
  <target>/package.json                          — adds `"test": "vitest run"` script + @skaile/test-utils devDep if needed

MUST  read CLAUDE.md before generating any test
MUST  detect and respect the framework already in use (vitest for most; jest for platform/backend)
MUST  read 2–3 existing test files to match conventions (import style, mock patterns, naming)
MUST  prefer TEST_PLAN.md as input when it exists
MUST  verify generated tests execute before completing (bun x --bun vitest run <package>)
MUST  place test files per package convention (tests/, test/unit/, __tests__/, or colocated)
MUST  use @skaile/test-utils helpers instead of hand-rolled mkdtemp/afterEach, mock transports, or mock WebSockets
NEVER modify existing test files — only add new ones, or refuse and ask
NEVER test implementation details (private methods, internal state)
NEVER use mocks for pure logic — if it needs mocks, it's integration
NEVER hand-roll mkdtempSync + afterEach(rmSync) boilerplate — use makeTempDir
NEVER hand-roll an in-memory transport — use makeInMemoryTransport
NEVER hand-roll a WebSocket stub — use MockWebSocket + installMockWebSocket
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

  Scaffolding checklist — verify each item, create if missing:

  **Config file:**
  - For forge apps / agent-framework / platform/frontend: `<target>/vitest.config.ts` must exist,
    OR the package must be registered as a project in the root `vitest.config.ts`.
    Agent-framework packages are included via the root vitest config; forge apps typically
    own a per-package config.
  - For platform/backend: Jest is already wired via NestJS defaults — do not scaffold new config.
    Ensure `<target>/test/` exists.

  **`package.json` scripts:**
  - `"test": "vitest run"` — required for `bun run --filter '<pkg>' test` to work.
  - `"test:watch": "vitest"` — optional.

  **Dev-dependency on @skaile/test-utils:**
  - If any generated test will use `makeTempDir`, `makeInMemoryTransport`, `MockWebSocket`,
    `installMockWebSocket`, `makeMockDriver`, `loadFixtureWorkspace`, or `withTempProject`:
    ```json
    "devDependencies": {
      "@skaile/test-utils": "workspace:*"
    }
    ```
  - Run `bun install` from the monorepo root afterwards.

  **Vue composable tests (forge apps):**
  - Add `environment: "happy-dom"` to the package's `vitest.config.ts`.
  - Already configured on forge/project, forge/assistant, forge/concept.
  - Add `happy-dom` to `devDependencies` if missing.

  **Example vitest.config.ts for a new forge package:**
  ```typescript
  import { defineConfig } from 'vitest/config';
  export default defineConfig({
    test: {
      environment: 'happy-dom',   // only if the package has Vue composables
      include: ['tests/**/*.test.ts', 'src/**/*.test.ts'],
      exclude: ['**/node_modules/**', '**/dist/**', 'tests/e2e/**'],
      testTimeout: 15000,
      globals: false,
    },
  });
  ```

  **Tests directory + setup file:**
  - Create `<target>/tests/` if missing.
  - If the package has Nitro routes that need test-time globals, add
    `<target>/tests/_setup/nitro-globals.ts` + `<target>/tests/_setup/h3-event.ts`
    — mirror `forge/project/tests/_setup/`.

  **Root workspace registration:**
  - Ensure the package's workspace entry in root `vitest.config.ts` defines a project name
    matching test_stack_map.md (e.g. forge-project, agent-runner). Add the project if missing.

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

  Pick the right pattern from the reference library below, then generate.

## Pattern Library (cite the reference in a comment at the top of each generated file)

### Pattern 1 — Pure unit (no I/O, no helpers)

  Reference: `agent-framework/flow-engine/tests/snapshots.test.ts`

  ```typescript
  import { describe, it, expect } from 'vitest';
  import { <exports> } from '../src/<module>';

  describe('<module>', () => {
    describe('<exportedSymbol>', () => {
      it('returns <expected> for <happy input>', () => {
        expect(<exportedSymbol>(<input>)).toEqual(<output>);
      });

      it('handles <edge case>', () => {
        expect(<exportedSymbol>(<edge>)).toEqual(<result>);
      });

      it('throws for <invalid input>', () => {
        expect(() => <exportedSymbol>(<bad>)).toThrow(<ErrorType>);
      });
    });
  });
  ```

### Pattern 2 — Temp-dir fixture (L2 filesystem code)

  Reference: `agent-framework/core/tests/repo-manager.test.ts`

  ```typescript
  import { describe, it, expect } from 'vitest';
  import { makeTempDir } from '@skaile/test-utils';
  import { writeFileSync } from 'node:fs';
  import { join } from 'node:path';
  import { RepoManager } from '../src/repo-manager';

  describe('RepoManager', () => {
    it('clones into the given directory', () => {
      const dir = makeTempDir('repo-manager');   // auto-cleanup on test-finished
      writeFileSync(join(dir, 'seed.yaml'), 'name: x');
      const rm = new RepoManager(dir);
      expect(rm.path).toEqual(dir);
    });
  });
  ```

  Note: `makeTempDir` prefers Vitest's `onTestFinished` for test-scoped cleanup and falls back
  to `afterEach`. Never roll your own `mkdtempSync + afterEach(rmSync)` boilerplate.

### Pattern 3 — Shared beforeEach + temp dir

  Reference: `agent-framework/connectors/tests/e2e/scaffold-resources.test.ts`,
  `agent-framework/runner/tests/runner.test.ts`

  Use when many tests in one describe share the same fixture setup. Still use `makeTempDir`
  inside the beforeEach.

### Pattern 4 — In-memory transport pair (L2 WebSocket-layer code)

  Reference: `agent-framework/client/tests/agent-client.test.ts`

  ```typescript
  import { describe, it, expect } from 'vitest';
  import { makeInMemoryTransport } from '@skaile/test-utils';
  import { AgentClient } from '../src/client';

  describe('AgentClient', () => {
    it('round-trips a command through the transport', async () => {
      const { clientSide, serverSide } = makeInMemoryTransport();
      const client = new AgentClient(clientSide);
      const received: unknown[] = [];
      serverSide.onMessage((m) => received.push(m));
      await client.send({ type: 'ping' });
      expect(received).toEqual([{ type: 'ping' }]);
    });
  });
  ```

### Pattern 5 — Mock WebSocket global + fake timers

  Reference: `agent-framework/transport/tests/ws-client.test.ts`

  ```typescript
  import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
  import { MockWebSocket, installMockWebSocket } from '@skaile/test-utils';

  describe('WsClient', () => {
    let restore: () => void;
    beforeEach(() => {
      restore = installMockWebSocket();
      vi.useFakeTimers();
    });
    afterEach(() => {
      restore();
      vi.useRealTimers();
    });

    it('reconnects after server close with exponential backoff', () => {
      // ... drive MockWebSocket lifecycle explicitly ...
    });
  });
  ```

### Pattern 6 — Boundary mock for common-backend (forge app unit tests)

  Reference: `forge/project/tests/api-auth-me.test.ts`, `forge/project/tests/api-auth-logout.test.ts`

  Forge apps can't import `@skaile/forge-common-backend`'s `createDb` at runtime in a unit test
  (better-sqlite3 is not Bun-compatible). Mock at the package boundary with `vi.mock`.

### Pattern 7 — Nitro route integration (synthetic h3 event)

  Reference: `forge/project/tests/_setup/h3-event.ts`, `_setup/nitro-globals.ts`,
  `api-auth-logout.test.ts`

  Use for Nuxt Nitro route unit-integration tests. The `_setup/` helpers install the minimum
  Nitro globals and build a synthetic h3 event; the test calls the route handler directly.

### Pattern 8 — Happy-dom for Vue composables

  Reference: `forge/project/tests/use-color-mode.test.ts`

  Add `environment: "happy-dom"` to the package's `vitest.config.ts`. Then composables that
  touch `document` / `window` work without a full browser.

### Pattern 9 — Subprocess harness (L3 drivers — integration, not unit)

  Reference: `agent-framework/bridge/tests/omp-driver.test.ts` (fake-omp fixture + `OMP_BRIDGE_BIN`)

  Unit generation should skip these — they belong to `test-integration`. Defer with a reason.

### Pattern 10 — Platform backend (Jest)

  ```typescript
  import { <exports> } from '<path>';

  describe('<module>', () => {
    describe('<exportedSymbol>', () => {
      it('returns <expected> for <input>', () => {
        expect(<exportedSymbol>(<input>)).toEqual(<output>);
      });
    });
  });
  ```

  Keep the NestJS default Jest config — do not scaffold a new one.

## Generation rules

  - One describe per exported symbol; nested describes for sub-behaviors
  - Each it-block tests one observable outcome
  - Cover: happy + 1 edge + 1 error path per export
  - Use types from the source module directly — no `as any`
  - No mocks beyond `vi.fn()` for simple callbacks at L1; `@skaile/test-utils` at L2
  - Match existing file's import ordering and quote style

STEP 7: Frontend-specific generation (platform/frontend, forge/*)
  - For Vue composables (forge): use the happy-dom environment (Pattern 8).
  - For React hooks (platform/frontend): use `renderHook` from `@testing-library/react`.
  - For pure components (stateless): render-and-assert DOM once.
  - Complex component logic → move to integration; do not test here.

STEP 8: Agent-framework-specific generation
  - L1 (flow-engine, resolver, core manifest, bridge pure parts): heavy coverage via Pattern 1.
  - L2 (transport, client, session, store, asset-manager, sdk): use Patterns 2, 4, 5.
  - L3 (runner, bridge drivers, connectors, lab): limit unit tests to pure helpers; defer the
    integration-heavy modules to `test-integration`.

## Constraints (must-know gotchas from the 2026-04-22 landing)

The following rules were learned during the Phase A–E gap-fill. Violating them produces
flaky tests, Bun incompatibilities, or silent regressions.

1. **Always use `@skaile/test-utils` helpers**
   - `makeTempDir(prefix)` — never `mkdtempSync + afterEach(rmSync)`. The helper prefers
     Vitest's `onTestFinished` for test-scoped cleanup and falls back to `afterEach`.
   - `makeInMemoryTransport()` — never a custom event-emitter pair.
   - `MockWebSocket` + `installMockWebSocket()` — never a hand-rolled WebSocket stub.
   - `registerCleanup(fn)` — the primitive behind `makeTempDir`; available for custom cleanup.
   - Migrated across 23 test files in Phase E.1 — stay consistent.

2. **`better-sqlite3` is not Bun-compatible yet**
   - Forge app route tests that pull in `@skaile/forge-common-backend`'s `createDb` will
     fail under Bun+Vitest. Mock the boundary:
     ```ts
     vi.mock('@skaile/forge-common-backend', async () => {
       const actual = await vi.importActual('@skaile/forge-common-backend');
       return { ...actual, createDb: vi.fn(() => <chainable stub>) };
     });
     ```
   - For forge/concept: the `getDb()` call runs a seed-check at first call. The mock must
     return a chainable stub with `.select().from().get() → { count: 1 }` to short-circuit
     seeding.

3. **Same-package relative-util mocks need three specifier forms under Bun+Vitest**
   - Surfaced in Phase D.2 (forge/assistant subagent 2). When mocking a util inside the same
     package, register all three:
     ```ts
     const UTIL_ABS = new URL('../server/utils/foo.ts', import.meta.url).pathname;
     vi.mock('../server/utils/foo', () => ({ foo: vi.fn() }));
     vi.mock('../../server/utils/foo', () => ({ foo: vi.fn() }));
     vi.mock(UTIL_ABS, () => ({ foo: vi.fn() }));
     ```
   - Otherwise Bun's module resolver may bypass the mock on one of the import paths.

4. **Two tests are intentionally Bun-skipped**
   - `codex-driver.test.ts` and `session-builder.test.ts` rely on `vi.mock` of a dynamic
     import which Bun cannot hook. Run them under Node only:
     ```ts
     const runIfNode = typeof Bun === 'undefined' ? describe : describe.skip;
     runIfNode('<suite>', () => { /* ... */ });
     ```
   - Don't force new tests to use `vi.mock(...)` on dynamic imports — pick a different pattern.

5. **Test-file placement**
   - L1–L2 unit tests: colocated `src/foo.test.ts` OR mirrored under `tests/` — match the
     package's existing layout.
   - L3 integration: `tests/integration/<feature>.test.ts`.
   - L4 spawn-harness: `tests/e2e/<subcommand>.test.ts`.
   - Playwright (L5): keep `.spec.ts` suffix.

6. **No `vi.mock` of pure-logic siblings**
   - If the module under test imports a pure helper from its own package, test the real
     behaviour end-to-end. Mocking pure helpers hides bugs.

# ── Phase 5: Verify ───────────────────────────────────────────────

STEP 9: Run the generated tests AND the rest of the suite

  - Execute: `bun x --bun vitest run <package>` (or the package's `test` script).
  - Capture pass/fail/error per file.
  - Also run the **full agent-framework suite** quickly to confirm no regression:
    `bun x --bun vitest run agent-framework/...` — new tests should be additive.

  IF infrastructure errors (imports, mocks, setup):
    - Fix them — this is test scaffolding work, not production bug
  IF assertions fail because the source is wrong:
    - REPORT the bug to the user; do NOT weaken the test
  IF assertion fails because the test has a wrong expectation:
    - Fix the test to match the real (correct) behavior

STEP 10: Re-run coverage + ratchet (when coverage is wired in)

  ```bash
  # Regenerate CI-shape coverage
  bun x --bun vitest run \
    --coverage.enabled \
    --coverage.provider=istanbul \
    --coverage.reporter=json-summary \
    --coverage.reportsDirectory=_devlog/reports/coverage-ci

  # Assert no regression against baseline
  bun _scripts/check-coverage-ratchet.ts
  ```

  - Exit 0 → pass. Exit 1 → regression (fail).
  - If the ratchet reports `baseline-improved`, mention it in the report; updating
    `_devlog/reports/coverage-baseline-2026-04-22/summary.json` to lock in the gain is a
    manual follow-up.

# ── Phase 6: Report ───────────────────────────────────────────────

STEP 11: Present report
  ## Unit Test Generation — <target>

  ### Infrastructure
  - [x] vitest.config.ts present
  - [x] @skaile/test-utils registered in devDependencies
  - [x] package.json has `"test": "vitest run"`
  - [x] happy-dom environment configured (if Vue composables)
  - [x] Workspace project registered in root vitest.config.ts

  ### Generated
  | Module | File | Pattern | Tests | Status |
  |---|---|---|---|---|
  | src/lib/skill-manifest.ts | tests/lib/skill-manifest.test.ts | Pattern 1 (pure) | 6 | pass |
  | src/lib/repo-manager.ts | tests/lib/repo-manager.test.ts | Pattern 2 (temp-dir) | 5 | pass |
  | src/transport/ws-client.ts | tests/transport/ws-client.test.ts | Pattern 5 (MockWebSocket) | 8 | pass |

  ### Results
  - Total new tests: N
  - Passing: N
  - Failing (likely source bugs): N
    - <test name> — expected X but got Y in <file:line>

  ### Coverage delta
  - Before: lines <pct>%, branches <pct>%, functions <pct>%
  - After:  lines <pct>%, branches <pct>%, functions <pct>%
  - Ratchet: pass | baseline-improved (lock manually) | regressed (FAIL)

  ### Deferred
  | Unit | Reason |
  |---|---|
  | src/runner.ts:startSession | Cross-module integration — move to test-integration |
  | src/drivers/omp-driver.ts | Subprocess harness — move to test-integration |
  | src/ui/chat-panel.tsx | Browser-dependent — move to test-e2e |

STEP 12: Recommend follow-ups

  After generation:
  - Run the `devlog` skill to record the test addition in `_devlog/DEVLOG.md`.
  - If coverage improved meaningfully, edit `_devlog/reports/coverage-baseline-2026-04-22/summary.json`
    in the same PR to lock in the gain.
  - If L3-layer deferrals exist, recommend the user invoke `test-integration target=<pkg>`.

EMIT [test-unit] completed target=<pkg> files=<N> tests=<N> passing=<N> failing=<N> ratchet=<pass|improved|regressed>

CHECKLIST
  - [ ] CLAUDE.md + TEST_PLAN.md (if present) read before generation
  - [ ] Canonical documents consulted (_devlog/specs/2026-04-22-test-concept-design.md)
  - [ ] Framework detected and respected (Vitest / Jest)
  - [ ] Scaffolding checklist complete: config + test script + @skaile/test-utils devDep + happy-dom (if needed)
  - [ ] Existing test patterns matched from the Pattern Library
  - [ ] @skaile/test-utils helpers used (no hand-rolled mkdtemp / transport / WebSocket)
  - [ ] One test file per testable module
  - [ ] `bun x --bun vitest run <package>` passing — zero failures, zero regressions in sibling packages
  - [ ] Coverage ratchet re-run — no regression
  - [ ] Deferred units flagged with reason
  - [ ] `devlog` skill recommended as post-step

---

## Integration

- **Called by:** `test-plan` (suggests test-unit as next step), `implement` (when adding new pure logic), `quality`
- **Reads:** `<target>/CLAUDE.md`, `<target>/TEST_PLAN.md`, `<target>/src/**`, `test_stack_map.md`,
  `_devlog/specs/2026-04-22-test-concept-design.md`,
  `_devlog/plans/2026-04-22-test-gap-fill.md`,
  `agent-framework/test-utils/src/index.ts`
- **Writes:** `<target>/vitest.config.ts` (if missing), `<target>/tests/**/*.test.ts`,
  `<target>/package.json` (adds `test` script + `@skaile/test-utils` devDep when needed)
- **Triggers:** `devlog` (as a follow-up), `_scripts/check-coverage-ratchet.ts` (verification)
