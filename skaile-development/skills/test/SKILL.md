---
name: "test"
description: "Test construction and execution for the skaile-dev monorepo. Two modes: 'run' executes the test suite for one or more packages and reports results; 'construct' generates new tests for recently implemented code. Knows the full test stack: Vitest 3.2.4 (agent-framework + forge/L4-project + forge/L4-assistant + _scripts), Vitest 4.1 (forge/L5-concept), Jest (platform backend), Vitest (platform frontend), Playwright (E2E), and how to run each via the Bun workspace. Coverage is collected under Bun with @vitest/coverage-istanbul (not v8) and ratcheted against the committed baseline via _scripts/check-coverage-ratchet.ts."
metadata:
  version: "1.2.0"
  tags:
    - "testing"
    - "vitest"
    - "jest"
    - "playwright"
    - "bun"
    - "monorepo"
    - "skaile-development"
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
        hint: "e.g. 'forge/L4-project', 'platform/backend', 'agent-framework/cli'"
      - id: "filter"
        label: "Test name filter (for 'run' mode — runs only matching tests)"
        type: "text"
        required: false
      - id: "level"
        label: "Level (for 'construct' mode)"
        type: "select"
        options:
          - "unit"
          - "integration"
          - "e2e"
          - "auto"
        required: false
        default: "auto"
        hint: "auto = infer from changed files; else delegates to test-unit / test-integration / test-e2e"
    files: []
---

# Run Tests — Test Construction and Execution

## Canonical References (read first)

The skaile-dev monorepo has a committed layered test strategy. Before writing or
changing tests, read (at minimum) the concept spec and the phase plan — they own
the terminology (L0–L5 layers), coverage policy, and CI lanes that this skill enforces.

- **Concept / design spec:** `_devlog/specs/2026-04-22-test-concept-design.md` — layer
  taxonomy, shared infrastructure, coverage policy.
- **Phase plan:** `_devlog/plans/2026-04-22-test-gap-fill.md` — phase-by-phase gap-fill
  with per-package targets.
- **User-facing docs:** `/testing/` (Starlight, source at `docs/src/content/docs/testing.md`) —
  CI lanes table, ratchet description, local commands.
- **Coverage baseline:** `_devlog/reports/coverage-baseline-2026-04-22/summary.json` —
  ratchet compares this against the CI run's `coverage-summary.json`.
- **Shared helpers:** `agent-framework/test-utils/` (see its `CLAUDE.md`) — always use
  these instead of reinventing temp-dir cleanup, mock drivers, in-memory transports,
  or WebSocket doubles.
- **CI lanes:** `.github/workflows/test-fast.yml`, `.github/workflows/test-full.yml`,
  `.github/workflows/test-e2e.yml` — three workflows that run on every PR.

## Overview

Manages the test lifecycle for the skaile-dev monorepo. Works in two modes:

| Mode | What It Does |
|------|-------------|
| `run` | Execute the test suite for specified packages, report results, triage failures |
| `construct` | Generate new tests for recently implemented code — thin wrapper that delegates to `test-unit`, `test-integration`, or `test-e2e` based on the `level` input |

For any non-trivial test authoring (setting up infra from scratch, generating a full plan,
adding a new Playwright suite), prefer the dedicated skills:

- **`test-plan`** — generate a per-package `TEST_PLAN.md` from CLAUDE.md + source
- **`test-unit`** — scaffold unit infra + generate unit tests
- **`test-integration`** — scaffold integration infra (DB / temp-dir) + generate tests
- **`test-e2e`** — scaffold Playwright (web) or CLI harness + generate journeys

`test construct` is retained for quick, already-configured packages where the user just
wants a few tests for recently changed files.

## Test Stack by Package

The root `vitest.config.ts` aggregates every agent-framework package plus `_scripts/`.
A single `bun x --bun vitest run` from the repo root runs them all. Packages that need
a different environment (happy-dom for Vue composables, Nitro shims for forge routes,
vitest 4.1 for forge/L5-concept) keep their own `vitest.config.ts` and their own
`bun run test` script; those are scoped runs.

| Package | Framework | Run Command (from skaile-dev root) |
|---------|-----------|-----------------------------------|
| `agent-framework/*` (all packages under this tree) | Vitest 3.2.4 | `bun x --bun vitest run` |
| `_scripts/` (check-coverage-ratchet etc.) | Vitest 3.2.4 | `bun x --bun vitest run` |
| `forge/L4-project` | Vitest 3.2.4 + `happy-dom` | `bun run --filter @skaile/forge-project test` |
| `forge/L4-assistant` | Vitest 3.2.4 + `happy-dom` | `bun run --filter @skaile/forge-assistant test` |
| `forge/L5-concept` | Vitest 4.1 | `bun run --filter @skaile/forge-concept test` |
| `forge/common-backend` | Vitest 3.2.4 | root `bun x --bun vitest run` (included via root config) |
| `forge/common-ui` | Vitest 3.2.4 (unit) + Playwright CT (e2e) | `bun x --bun vitest run` (unit); `cd forge/common-ui && bun run test:e2e` (CT) |
| `agent-framework/tui` | Vitest 3.2.4 | `bun run --filter @skaile/agent-tui test` |
| `platform/backend` | Jest | `bun run --filter ./platform/backend test` |
| `platform/frontend` | Vitest | `bun run --filter ./platform/frontend test` |
| `platform/e2e` | Playwright | `bun run --filter ./platform/e2e test:e2e` |
| `forge/L4-project/tests/e2e/` | Playwright | `cd forge/L4-project && bun run test:e2e` |

**Never** run `vitest` from inside a submodule/package with `bun` — always invoke from
the skaile-dev root so the workspace overrides resolve every `@skaile/*` dependency
locally. The forge Nuxt-app scoped runs are the only exception because they use their
own vitest config with setup files.

**Forge/concept** uses vitest 4.1 which differs from the root's 3.2.4; it is NOT
wired into the shared root config or the coverage ratchet. Run it only with its
own scoped command.

## When to Use

- After any code change — verify nothing broke (`mode=run`)
- After implementing a new feature — add test coverage (`mode=construct`)
- Triaging CI failures — investigate specific failing tests (`mode=run, filter=<name>`)
- Before opening a PR — full suite pass is required

## When NOT to Use

- For E2E test setup or generation — use `test-e2e`
- For integration test setup or generation — use `test-integration`
- For unit test setup or generation — use `test-unit`
- For generating a per-package test plan — use `test-plan`
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

EMIT [test] started mode=<mode> scope=<packages>

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
      - $ <run command for package> [--reporter=verbose] [--testNamePattern=<filter>] 2>&1 | tail -80
      - Capture: total, passed, failed, skipped, duration
      EMIT [test] package_result package=<name> total=<n> passed=<n> failed=<n>

    COMPACT CHECKPOINT (every 3 packages or when scope=all):
      After completing each group of 3 packages, pause and call /compact before
      continuing. This prevents context from growing unboundedly across many test
      iterations and is the single most impactful token-saving action in this skill.

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
    | forge/L4-project | 42 | 42 | 0 | 0 | 1.2s |
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

  EMIT [test] run_complete total=<n> passed=<n> failed=<n>

  IF any REGRESSION failures
    - Report: "These tests were passing before. Fix before proceeding."
  IF any BAD_TEST failures
    - Report: "These tests appear to have incorrect assertions. Review before fixing source."
  IF any INFRA_ISSUE failures
    - Report: "Infrastructure issues detected — resolve environment before re-running."

# ── Mode: construct ───────────────────────────────────────────────

IF mode = construct

  STEP 0: Route to the right quality skill (preferred path)

    FIRST CHECK: does the target package have `tests/TEST_PLAN.md`?
      IF NO → delegate to `test-plan` with target=<scope> first. A TEST_PLAN
              ties the per-package work to the layer taxonomy from the
              concept spec and enumerates the cases each test file must cover.
              After test-plan returns, continue with the level-appropriate
              test-* skill below.
      IF YES → proceed to level routing.

    Then, by level:
      IF level = unit or level = auto + changed files look like pure logic (no I/O imports):
        → Delegate to `test-unit` with target=<scope> and STOP
      IF level = integration or level = auto + changed files include API routes / DB handlers / subprocess drivers:
        → Delegate to `test-integration` with target=<scope> and STOP
      IF level = e2e or level = auto + changed files include pages/ or CLI bins:
        → Delegate to `test-e2e` with target=<scope> and STOP

    Only fall through to the legacy in-place construction below when the user
    explicitly wants a quick one-file addition in an already-configured package.

    DISPATCH HYGIENE (construct mode):
      When scope covers multiple packages, do NOT dispatch one agent per package
      sequentially. Group packages by L-layer (see CLAUDE.md layer table) and
      dispatch one agent per layer group in parallel. Apply MVC prompts: each
      agent receives only its package's CLAUDE.md excerpt and the relevant source
      files — not the full parent context. See references/sub-agent-dispatch.md.

  STEP 0.5: Patterns new tests MUST follow

    - **Temp directories:** always import `makeTempDir` from `@skaile/test-utils`
      (not raw `mkdtempSync` + `afterEach(rmSync)`). The helper registers cleanup
      via Vitest's `onTestFinished` when available and falls back to `afterEach`
      otherwise — it has been adopted across 23 agent-framework test files.
    - **Mock drivers:** use `makeMockDriver` from `@skaile/test-utils`.
    - **In-memory transport (A.2 / A.3 pattern):** use `makeInMemoryTransport`
      for transport/client round-trip tests.
    - **WebSocket client doubles:** use `MockWebSocket` + `installMockWebSocket`
      from `@skaile/test-utils/mock-websocket`. Install it on `globalThis.WebSocket`
      in `beforeEach` and tear down in `afterEach`.
    - **Fixture workspaces:** use `loadFixtureWorkspace` from `@skaile/test-utils`;
      fixture files live under `tests/fixtures/` (never `__fixtures__` or `test/fixtures`).
    - **Vue composables in forge apps** (L5 unit tier): the forge Nuxt app's
      `vitest.config.ts` must set `environment: "happy-dom"` and list
      `happy-dom` as a devDependency. `forge/L4-project/vitest.config.ts` is the
      reference.
    - **Nitro route integration tests** (forge L5 integration tier): synthesize an
      h3 event via `tests/_setup/h3-event.ts` + install Nitro globals via
      `tests/_setup/nitro-globals.ts` as a `setupFiles` entry. See
      `forge/L4-project/tests/api-*.test.ts` for the canonical pattern (import the
      route handler dynamically, mock `@skaile/forge-common-backend` at the package
      boundary, call the handler with a synthetic event).
    - **Bridge / subprocess drivers** (L3 integration tier): use the fake-binary
      harness pattern in `agent-framework/bridge/tests/omp-driver.test.ts`. The
      driver is redirected at a fake Node script via the `OMP_BRIDGE_BIN` /
      `OMP_BRIDGE_PREARGS` env vars; the fake controls ready/exit/stderr via
      `FAKE_OMP_*` env vars. Guarded by `SKAILE_SPAWN_TESTS=1` in the full lane.
    - **Docker / lab integration** (L3): guarded by `SKAILE_DOCKER_TESTS=1`.
    - **E2E spawn-harness** (L4 CLI): spawn the compiled `skaile` binary via
      `bun x` and assert on stdout / exit code per subcommand. Files live under
      `agent-framework/cli/tests/e2e/<subcommand>.test.ts`.
    - **Playwright** (L5 E2E): `.spec.ts` suffix under `tests/e2e/`. Each forge
      app owns its own `playwright.config.ts` and runs via `bun run test:e2e`.
    - **Playwright Component Testing (CT)** (composable libraries): when a Vue composable
      uses TipTap, ProseMirror, or any other library that requires real browser DOM beyond
      what happy-dom provides, use `@playwright/experimental-ct-vue`. Config at
      `playwright-ct.config.ts`, fixtures in `tests/e2e/fixtures/`, spec files as `*.spec.ts`
      under `tests/e2e/`. Requires the `resolveVueCompilerDom()` Vite plugin (see
      `forge/common-ui/playwright-ct.config.ts`). Run with `bun run test:e2e`.
      Recording: `bun run test:e2e:record -- http://localhost:<ctPort>` — opens `playwright codegen`
      against the CT dev server, records interactions as spec code.

  STEP 0.6: Mocking gotchas (project-specific — do not fight these)

    - `better-sqlite3` is not yet Bun-compatible. Forge route handlers that call
      `getDb()` via `@skaile/forge-common-backend` must mock `createDb` (and
      any other boundary functions the route pulls in) at the package boundary:
      ```ts
      vi.mock("@skaile/forge-common-backend", async () => {
        const actual = await vi.importActual<Record<string, unknown>>(
          "@skaile/forge-common-backend",
        );
        return { ...actual, createDb: mockCreateDb, getSessionUser: mockGetSessionUser };
      });
      ```
      For forge/L5-concept routes, the mock must return a chainable stub
      (`.select().from().get() → { count: 1 }`) because `getDb()` runs a seed
      check on first call and will otherwise execute the seed path.
    - Under Bun + Vitest, `vi.mock` of a same-package relative util sometimes
      misses. Register three specifier forms (two relatives + the absolute path
      via `new URL(... , import.meta.url).pathname`). This is documented in the
      D.2 forge/L4-assistant update on 2026-04-22.
    - Two test files are `describe.skip` under Bun because of `vi.mock` + dynamic
      `import()` limitations: `agent-framework/bridge/tests/codex-driver.test.ts`
      and `agent-framework/runner/tests/session-builder.test.ts`. They pass under
      plain `bun x vitest run` (Node). **Do not try to "fix" them**; the skip is
      intentional and documented.

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
    | src/composables/useWorkspace.test.ts | forge/L4-project | 8 | 2 |

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

  EMIT [test] construct_complete files_created=<n> tests_generated=<n> passing=<n>

CHECKLIST
  - [ ] Existing test patterns read before constructing new tests
  - [ ] Tests match package naming and placement conventions
  - [ ] Tests cover observable behavior, not implementation details
  - [ ] All generated tests run without infrastructure errors
  - [ ] Failures triaged (regression vs. new vs. infra vs. bad test)
  - [ ] Source bugs reported (tests not modified to hide them)

---

## Token Hygiene (read before running)

Test runs are the largest single driver of context growth. Follow these rules to
keep sessions from ballooning:

**Truncate output.** Every test run appends to context. Always pipe to `| tail -80`
unless you need to see full output for triage. The summary line at the end is what
matters — the rest is noise once tests pass.

**Compact between packages.** When running scope=all or more than 3 packages, call
`/compact` in the conversation after each package group finishes. Do not run the
entire monorepo suite in one uninterrupted loop without compaction.

**Split wide scopes into separate sessions.** For test gap-fill or construct work
spanning many packages, prefer one session per L-layer group:
- Session A: L0–L2 (types, core, resolver, flow-engine, bridge/pure, common-ui)
- Session B: L3 (transport, client, session, store, asset-manager, sdk, tui)
- Session C: L4+ (connectors, runner, bridge/drivers, lab, cli, forge apps)

**Do not re-read CLAUDE.md or spec files on every iteration.** Read once, keep in
context. If you've already loaded a package's CLAUDE.md this session, don't read
it again.

## Bun Workspace Test Commands (Quick Reference)

```bash
# Run all agent-framework + _scripts tests — truncate output to summary
bun x --bun vitest run 2>&1 | tail -80

# Run with watch mode (development)
bun x --bun vitest

# Run with test name filter
bun x --bun vitest run -t "workspace rename" 2>&1 | tail -40

# Run a single test file (full output is fine for a single file)
bun x --bun vitest run agent-framework/core/src/manifest.test.ts

# Run a forge Nuxt app's suite (their own vitest config — happy-dom + Nitro shims)
bun run --filter @skaile/forge-project test 2>&1 | tail -60
bun run --filter @skaile/forge-assistant test 2>&1 | tail -60
bun run --filter @skaile/forge-concept test 2>&1 | tail -60    # vitest@4.1, scoped only

# Run platform backend (Jest)
bun run --filter ./platform/backend test 2>&1 | tail -80

# Run platform / forge-project E2E (Playwright)
bun run --filter ./platform/e2e test:e2e 2>&1 | tail -60
(cd forge/L4-project && bun run test:e2e 2>&1 | tail -60)

# Run forge/common-ui Playwright CT (TipTap/ProseMirror browser tests)
(cd forge/common-ui && bun run test:e2e 2>&1 | tail -20)

# Record new CT tests (codegen)
# Terminal 1: cd forge/common-ui && bun run test:e2e:ui
# Terminal 2: cd forge/common-ui && bun run test:e2e:record -- http://localhost:3101

# Run with coverage (istanbul under Bun — mirrors test-full.yml CI lane)
bun x --bun vitest run \
  --coverage.enabled \
  --coverage.provider=istanbul \
  --coverage.reporter=text-summary \
  --coverage.reporter=json-summary \
  --coverage.reportsDirectory=_devlog/reports/coverage-ci \
  2>&1 | tail -40

# Check the ratchet against the committed baseline
bun _scripts/check-coverage-ratchet.ts
# Options: --ci <path> --baseline <path> --tolerance <pct>
# Exit codes: 0 pass, 1 regression, 2 invalid input
```

**Do NOT use `@vitest/coverage-v8` under Node** for this repo. The v8 provider needs
Node's inspector, and under Node the connectors package breaks because
`connector-registry.ts` uses `require('./adapters/memory.js')` at runtime — which
Bun polyfills but Node's ESM loader does not. Istanbul instruments source at load
time and works identically under Bun and Node; we pick Bun because the fast-lane
tests already pass under Bun and we want one consistent runtime story. See
`.github/workflows/test-full.yml` for the full CI rationale.

## Known Constraints

These are the current hard constraints of the skaile-dev test framework. Do NOT
try to route around them — they are documented here so the skill won't flail.

| Constraint | Where | Workaround |
|---|---|---|
| `better-sqlite3` has no Bun build | forge/L4-project, forge/L5-concept, forge/L4-assistant | Mock `createDb` at the `@skaile/forge-common-backend` boundary. For forge/L5-concept, also return a chainable stub so the seed-check short-circuits. |
| `vi.mock` misses same-package relatives under Bun+Vitest | forge Nitro route tests | Register three specifier forms: `./foo`, `../foo`, and the absolute path via `new URL("../foo.ts", import.meta.url).pathname`. |
| `vi.mock` + dynamic `import()` unstable under Bun | `bridge/tests/codex-driver.test.ts`, `runner/tests/session-builder.test.ts` | These files are `describe.skip` under Bun and pass under plain `bun x vitest run` (Node). Leave them skipped — do not "fix" them. |
| `@vitest/coverage-v8` requires Node's inspector | whole monorepo | Use `@vitest/coverage-istanbul` under Bun. See test-full.yml. |
| Connectors package `require('./adapters/memory.js')` at runtime | `agent-framework/connectors` | Works under Bun; breaks under Node ESM. Run under `bun x --bun vitest run`. |
| forge/L5-concept uses vitest 4.1 (root is 3.2.4) | `forge/L5-concept` | Run scoped via `bun run --filter @skaile/forge-concept test`; do not include in the root v8/istanbul coverage run. |
| Bun.serve request handler body | `transport/src/server.ts` | Not reachable under Node. Validated by the 4 Bun-only integration tests in `ws-server.test.ts` (which are `describe.skip` unless running under `--bun`). |

## Post-Test Actions (construct or run mode)

After finishing test work — especially construct mode — recommend:

1. **Check the ratchet.** Regenerate coverage and compare against the baseline:
   ```bash
   bun x --bun vitest run \
     --coverage.enabled --coverage.provider=istanbul \
     --coverage.reporter=json-summary \
     --coverage.reportsDirectory=_devlog/reports/coverage-ci
   bun _scripts/check-coverage-ratchet.ts
   ```
   If the ratchet reports `baseline-improved` for a package and the gain is
   intentional, update that package's entry in
   `_devlog/reports/coverage-baseline-2026-04-22/summary.json` in the same PR.
2. **Record the change.** Call the `devlog` skill so `_devlog/DEVLOG.md` captures
   the testing work (new test files added, coverage moved, known-skip list changes).
3. **Before opening a PR**, run the local equivalent of test-full.yml
   (via the `quality` skill in `mode=full`) — see that skill for the canonical
   pre-PR sequence.

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Modifying tests to make them pass | Fix the source code — tests are the spec |
| Running only the new tests | Run the full package suite to catch regressions |
| Constructing tests without reading existing ones | Always read 2–3 existing tests first — conventions vary |
| Testing implementation details | Test observable behavior only |
| Skipping infra failure investigation | INFRA failures mask real bugs — fix environment first |
| Using `mkdtempSync` + manual cleanup | Use `makeTempDir()` from `@skaile/test-utils` |
| Using `@vitest/coverage-v8` under Node | Use istanbul under Bun (see Bun Workspace Test Commands) |
| "Fixing" the two skipped Bun tests | They're skipped on purpose; pass under Node. Leave them alone. |

## Integration

- **Called by:** `implement` (after each task and before finish), `audit` (as a pre-analysis gate), `quality`
- **Calls:** `test-plan`, `test-unit`, `test-integration`, `test-e2e` (from construct mode — test-plan first when no TEST_PLAN.md exists, then the level-specific skill)
- **Delegates to:** `test-plan` → `test-unit` / `test-integration` / `test-e2e` — all in the skaileup-evaluate domain, no external dependencies
- **Followed by:** `_scripts/check-coverage-ratchet.ts` (ratchet), then `devlog` (record)
