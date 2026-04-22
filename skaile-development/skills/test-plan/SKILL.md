---
name: "test-plan"
description: "Generates a per-package test plan for any skaile-dev package. Reads CLAUDE.md (architecture, public API), the source tree, and existing tests; produces <package>/TEST_PLAN.md listing untested units, coverage gaps, integration seams, and recommended scenarios per layer (unit/integration/e2e). Used as the input for test-unit, test-integration, and test-e2e."
metadata:
  version: "1.1.0"
  tags:
    - "testing"
    - "test-plan"
    - "coverage"
    - "scenarios"
    - "scaffolding"
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
      - id: layers
        label: "Layers to plan (comma-separated)"
        type: text
        default: "unit,integration,e2e"
        hint: "Choose subset when some layers don't apply (libraries have no e2e)"
    reads:
      - path: "<target>/CLAUDE.md"
        description: "Package architecture + public API source of truth"
      - path: "<target>/package.json"
        description: "Scripts, entry points, exported bins"
      - path: "<target>/src"
        description: "Source tree for testable unit discovery"
      - path: "ai-assets/skaile-development/references/test_stack_map.md"
        description: "Framework + run commands for the package"
      - path: "_devlog/specs/2026-04-22-test-concept-design.md"
        description: "Canonical layer taxonomy (L0-L5), shared infrastructure, coverage policy"
      - path: "_devlog/plans/2026-04-22-test-gap-fill.md"
        description: "Phase plan with per-package targets"
      - path: "_devlog/reports/coverage-baseline-2026-04-22/summary.json"
        description: "Per-package baseline line/branch/function pct used to measure gap-to-target"
      - path: "_devlog/reports/coverage-ci/coverage-summary.json"
        description: "Optional: latest CI coverage-summary used to diff against baseline"
      - path: "docs/src/content/docs/testing.md"
        description: "User-facing Starlight overview of the layered test strategy"
    produces:
      - path: "<target>/TEST_PLAN.md"
        description: "Structured per-layer test plan, consumed by test-unit/-integration/-e2e"
  user_inputs:
    dialog:
      - id: "target"
        label: "Package path"
        type: "text"
        required: true
      - id: "layers"
        label: "Layers (comma-separated)"
        type: "text"
        required: false
        default: "unit,integration,e2e"
    files: []
---

# Test Plan — Per-Package Coverage Plan

## Overview

Produces a structured test plan that enumerates *what* should be tested at each layer for one package. The plan is the input for the three setup/generation skills (`test-unit`, `test-integration`, `test-e2e`) and for `audit` to measure coverage gaps.

### Canonical documents (read these first)

The 2026-04-22 test gap-fill initiative established the layered test strategy for the monorepo. Every plan must be aligned to these artifacts:

- `_devlog/specs/2026-04-22-test-concept-design.md` — layer taxonomy (L0–L5), shared infrastructure, coverage policy.
- `_devlog/plans/2026-04-22-test-gap-fill.md` — phase-by-phase work, per-package targets.
- `_devlog/reports/coverage-baseline-2026-04-22/summary.json` — committed baseline with per-package line/branch/function pct.
- `docs/src/content/docs/testing.md` — user-facing Starlight overview.
- Root `CLAUDE.md` § "Testing Strategy" — quick layer summary.
- `agent-framework/test-utils/CLAUDE.md` — shared helpers (`makeTempDir`, `makeInMemoryTransport`, `MockWebSocket` + `installMockWebSocket`, `makeMockDriver`, `loadFixtureWorkspace`, `withTempProject`, `registerCleanup`).

### Layer targets

| Layer | Description | Line coverage target |
|---|---|---|
| L0 | Pure types (`agent-framework/types`) | `tsc --noEmit` only |
| L1 | Pure functions, no I/O | ≥ 90% lines / ≥ 80% branches |
| L2 | I/O-bound units with in-process mocks | ≥ 75% lines |
| L3 | Cross-module integration (subprocess/Docker/temp-dir) | ≥ 60% lines |
| L4 | CLI/SDK entry points via spawn harness | ≥ 1 happy + 1 error path per command |
| L5 | Forge apps — Unit + Integration + E2E pyramid | unit ≥ 70%, integration ≥ 50%, E2E critical journeys |

The plan is package-local (not app-wide) and is derived from:
- The package's **CLAUDE.md** — the source of truth for Architecture + public API
- The **package.json** — entry points, exported binaries, build targets
- The **source tree** — exported symbols, server routes, pages, components
- **Existing tests** — what's already covered (do not re-list)
- The **baseline coverage** in `_devlog/reports/coverage-baseline-2026-04-22/summary.json` — to measure gap-to-target per layer

## When to Use

- Before running `test-unit` / `test-integration` / `test-e2e` on a package that has no plan yet
- After a major refactor — regenerate the plan to capture new coverage gaps
- When onboarding a package to structured testing (most agent-framework libs)
- As prep for a release (feeds into `ready` as evidence of coverage)

## When NOT to Use

- For writing tests directly — use `test-unit`, `test-integration`, or `test-e2e`
- For running tests — use `test`
- For a multi-package coverage dashboard — use `audit scope=full`

---

ROLE  Test planner — reads a single package's CLAUDE.md + source tree and produces a layered test plan.

READS
  ! <target>/CLAUDE.md                 — Architecture + public API sections
  ! <target>/package.json              — scripts + entry points
  ! <target>/src/**                    — exported symbols, routes, components
  ? <target>/tests/** or test/**       — existing coverage to exclude
  ! ai-assets/skaile-development/references/test_stack_map.md
  ! _devlog/specs/2026-04-22-test-concept-design.md         — canonical taxonomy
  ! _devlog/plans/2026-04-22-test-gap-fill.md               — phase plan
  ! _devlog/reports/coverage-baseline-2026-04-22/summary.json — per-package baseline
  ? _devlog/reports/coverage-ci/coverage-summary.json       — latest CI run (if present)

WRITES
  <target>/TEST_PLAN.md                — the plan (overwrites prior, backup goes to TEST_PLAN.prev.md)

MUST  read the target CLAUDE.md before listing any unit
MUST  read the coverage baseline entry for the target and report gap-to-target per layer
MUST  exclude units that already have test coverage (scan existing test files)
MUST  classify every unit into exactly one layer (unit / integration / e2e) using the L0-L5 taxonomy
MUST  provide a seed-data or fixture strategy for every integration and e2e scenario
MUST  cite shared helpers from @skaile/test-utils wherever applicable (makeTempDir / makeInMemoryTransport / MockWebSocket / installMockWebSocket / makeMockDriver / loadFixtureWorkspace / withTempProject)
MUST  respect the `layers` input — skip sections for layers not requested
NEVER invent public API that isn't in CLAUDE.md or exported from src/
NEVER list units in the wrong layer (DB-touching code is integration, browser-dependent is e2e)
NEVER propose ad-hoc mkdtemp/afterEach boilerplate — every temp dir must use makeTempDir from @skaile/test-utils

EMIT [test-plan] started target=<pkg>

# ── Phase 1: Context Loading ──────────────────────────────────────

STEP 1: Validate target
  - Verify <target>/package.json exists
  - Read <target>/CLAUDE.md (required: What This Is, Architecture, Tech Stack sections)
  - Read <target>/package.json (name, main, bin, scripts)

STEP 2: Classify package category and test layer
  Using the Package Categories table from references/readiness_criteria.md:
  - app | library | internal-library | cli | docs-site | ai-domain | theme
  - This determines which layers apply (libraries have no e2e, internal types have only unit, etc.)

  Also assign the **test layer (L0-L5)** from the concept spec:
  - L0 — types-only (tsc --noEmit)
  - L1 — pure functions (core, resolver, flow-engine, bridge/pure, forge/common-ui)
  - L2 — I/O-bound with in-process mocks (transport, client, session, store, asset-manager, sdk, forge/common-backend, forge/tui)
  - L3 — cross-module integration (connectors, runner, bridge/drivers, lab, workspace-plugin)
  - L4 — entry points (cli, sdk acceptance)
  - L5 — reference apps (forge/*)

STEP 3: Read coverage baseline for the target
  - Read `_devlog/reports/coverage-baseline-2026-04-22/summary.json`
  - Extract packages.<target>.{lines.pct, branches.pct, functions.pct, notes}
  - Compare against the layer target:
    * L1: ≥ 90% lines / ≥ 80% branches
    * L2: ≥ 75% lines
    * L3: ≥ 60% lines
    * L4: coverage is command-surface, not %
    * L5: unit ≥ 70%, integration ≥ 50%
  - Compute **gap-to-target** = max(0, target_pct - current_pct).
  - If `_devlog/reports/coverage-ci/coverage-summary.json` exists, also compute the diff from CI
    to detect regressions since baseline.

STEP 4: Discover existing coverage
  - Glob <target>/{tests,test}/**/*.{test,spec}.{ts,tsx} AND <target>/src/**/*.test.ts (colocated)
  - For each existing test file, extract what it covers (describe blocks, file-under-test imports)
  - Build a set of already-covered source paths
  - Cross-check with the CI coverage-summary (if present): flag `src/` files at 0% lines as
    fully uncovered — they are the priority targets.

# ── Phase 2: Unit Discovery ───────────────────────────────────────

STEP 5: Enumerate testable units per layer

  L1 — Unit (pure logic, no I/O):
    - Exported functions in src/ that don't import fs, DB, network
    - Composables (Nuxt / Vue) — test return values + reactivity
    - Utility modules, validators, pure stores
    - Type guards, parsers, formatters
    - Reference pattern: `agent-framework/flow-engine/tests/snapshots.test.ts`
    - Reference pattern: `agent-framework/client/tests/agent-client.test.ts`

  L2 — Unit with in-process I/O:
    - Filesystem work against temp dirs (use `makeTempDir` from @skaile/test-utils)
    - WebSocket/stream code with in-memory transport pairs (use `makeInMemoryTransport`)
    - WebSocket globals replaced by `MockWebSocket` + `installMockWebSocket`
    - Auth primitives (PBKDF2, cookie signing) with deterministic salts
    - Reference pattern: `agent-framework/core/tests/repo-manager.test.ts` (temp-dir fixture)
    - Reference pattern: `agent-framework/client/tests/agent-client.test.ts` (in-memory transport)
    - Reference pattern: `agent-framework/transport/tests/ws-client.test.ts` (MockWebSocket + fake timers)

  L3 — Integration (cross-module, single package):
    - API route handlers (server/api/*.ts, Nuxt server, NestJS controllers)
    - Database repositories / services (drizzle queries, Prisma services)
    - Subprocess-driven drivers — real binary stubbed with a fixture script
      Reference: `agent-framework/bridge/tests/omp-driver.test.ts` (fake-omp fixture + `OMP_BRIDGE_BIN`)
    - Docker-backed tests — guarded by env var `SKAILE_DOCKER_TESTS=1`
    - Subprocess-spawning tests — guarded by env var `SKAILE_SPAWN_TESTS=1`
    - Volume/connector adapters driven from YAML fixtures
      Reference: `agent-framework/connectors/tests/e2e/scaffold-resources.test.ts`

  L4 — E2E spawn-harness:
    - CLI invocations via `runCli(args, { cwd })` — real compiled binary via `bun x`
    - stdout/exit-code assertions for each top-level subcommand group
    - One happy-path + one error-path per subcommand (missing flag, bad argument)

  L5 — Reference apps (forge) have three shapes:
    - Unit (composables, server utilities, auth middleware):
      Reference: `forge/project/tests/use-color-mode.test.ts` (happy-dom for Vue composables)
    - Integration (Nitro route handlers with synthetic h3 events + mocked @skaile/forge-common-backend):
      Reference: `forge/project/tests/_setup/h3-event.ts`, `_setup/nitro-globals.ts`,
      `api-auth-logout.test.ts`, `api-auth-me.test.ts`
    - E2E (Playwright) — critical user journeys only.

  Skip: generated files, `dist/`, `node_modules`, test files themselves, `.config.ts` files.

# ── Phase 3: Scenario Generation ──────────────────────────────────

STEP 6: For each unit, specify scenarios

  L1 unit scenarios (drive to ≥ 90% lines):
    - Happy path — correct inputs → expected output
    - Edge inputs — empty, max, min, special chars, zero-length arrays, unicode
    - Error inputs — invalid types, bad shapes → throw/reject

  L2 unit scenarios (drive to ≥ 75% lines):
    - Round-trip through in-memory transport (request → response preserves id, order, payload)
    - Filesystem I/O: temp dir created, files written and read, cleanup verified
    - Subscriber cleanup: dispose() stops events; no leak in subscriber list
    - Timeouts + fake timers for async code with deadlines

  L3 integration scenarios (drive to ≥ 60% lines):
    - Request → response for every handler, with auth variations
    - DB writes → verify row state + constraints (SQLite temp file for forge, test Postgres for platform)
    - DB reads → empty state, populated state, filters, pagination
    - Cascade / transaction behavior
    - Subprocess lifecycle: spawn, stdout capture, stderr preserved, SIGTERM → exit < 2s, kill-on-dispose
    - Driver reconnect after subprocess crash

  L4 spawn-harness scenarios:
    - Every top-level CLI subcommand: happy path (zero exit, expected stdout fragment) + error path (nonzero exit, useful message)

  L5 scenarios:
    - Unit: composable return values, reactivity, pure component render
    - Integration: each Nitro route with valid + invalid + unauthenticated inputs
    - E2E: one journey per user-facing feature (from README / CLAUDE.md Architecture)
      - Happy path + one error path per journey
      - Responsive breakpoints for frontend pages (375, 768, 1440)

# ── Phase 4: Fixture & Seed Strategy ──────────────────────────────

STEP 7: Specify fixture/seed per scenario

  All temp dirs: `makeTempDir(prefix)` from `@skaile/test-utils` — never hand-roll
  `mkdtempSync + afterEach(rmSync)`.

  Forge (SQLite + drizzle): temp-file DB via `makeTempDir`; seed JSON loaded in beforeAll
  Platform backend (PostgreSQL + Prisma): separate test DB, truncate in afterEach
  Agent framework L2: `makeInMemoryTransport` for WS pairs, `MockWebSocket` + `installMockWebSocket` for globals
  Agent framework L3 drivers: fake-binary fixture under `tests/fixtures/fake-omp/` + env override (e.g. `OMP_BRIDGE_BIN`)
  Agent framework L3 lab/docker: gated by `SKAILE_DOCKER_TESTS=1`; skip block otherwise
  Platform frontend: mock API via MSW (or fetch stub); render with TanStack providers
  Forge Nitro integration: synthetic h3 event via `tests/_setup/h3-event.ts` + Nitro globals via `tests/_setup/nitro-globals.ts`
  YAML workspaces: `loadFixtureWorkspace(name)` reads from `tests/fixtures/workspaces/<name>.yaml`

# ── Phase 5: Write Plan ───────────────────────────────────────────

STEP 8: Write <target>/TEST_PLAN.md

  OUTPUT <target>/TEST_PLAN.md
  ---
  last_updated: <YYYY-MM-DD>
  package: <target>
  category: <category>
  test_layer: L1 | L2 | L3 | L4 | L5   # primary layer from the concept spec
  layers: [unit, integration, e2e]     # only included layers
  unit_count: N
  integration_count: N
  e2e_count: N
  existing_coverage: <N files / N scenarios>
  baseline:
    lines_pct: <number>
    branches_pct: <number>
    functions_pct: <number>
  target:
    lines_pct: <90|75|60>
  gap_to_target:
    lines_pct: <number>
  ---

  # Test Plan — <target>

  ## Coverage Context

  - **Layer:** L<N>  (target ≥ <X>% lines)
  - **Baseline** (from `_devlog/reports/coverage-baseline-2026-04-22/summary.json`):
    lines <current_pct>%, branches <current_pct>%, functions <current_pct>%
  - **Gap to target:** +<N> percentage points of lines to reach the layer floor.
  - **Current CI** (from `_devlog/reports/coverage-ci/coverage-summary.json`, if present):
    lines <current_pct>% (<delta_since_baseline>)

  ## Coverage Gap

  Files in `src/` that are uncovered or below the layer threshold — these are the priority
  targets for test-unit / test-integration / test-e2e.

  | File | Lines % | Functions % | Priority |
  |---|---|---|---|
  | src/agent.ts | 0 | 0 | HIGH — integration scope, needs fake-binary fixture |
  | src/serve.ts | 0 | 0 | HIGH — WebSocket server, needs installMockWebSocket |
  | src/helpers.ts | 42.1 | 50.0 | MEDIUM — close to 60% floor, missing edge cases |

  Include a brief rationale per file: what kind of test is needed (pure unit? temp-dir?
  fake-binary? mocked boundary?).

  ## Coverage Summary

  | Layer | Scenarios | Already Covered | To Add |
  |---|---|---|---|
  | Unit | 42 | 18 | 24 |
  | Integration | 12 | 0 | 12 |
  | E2E | 5 | 0 | 5 |

  ## Proposed Scenarios

  Scenarios are bucketed by layer. Each lists the shared helper it should use (from
  `@skaile/test-utils`) so `test-unit` / `test-integration` generates consistent code.

  ### Unit Layer (L1/L2)

  #### Module: `src/lib/skill-manifest.ts`
  Helpers: pure functions, no helpers needed.
  - [ ] **parseManifest(yaml: string)** — parses SKILL.md frontmatter
    - Happy: valid frontmatter → typed result
    - Edge: missing optional fields → defaults applied
    - Error: invalid YAML → throws ParseError

  #### Module: `src/workspace/repo-manager.ts`
  Helpers: `makeTempDir` (temp dir), `loadFixtureWorkspace` (optional).
  - [ ] **RepoManager.clone(dest)** — clones into temp dir
    - Happy: clones into `makeTempDir("repo-clone")`, returns repo path

  [repeat for each module]

  ### Integration Layer (L3)

  #### Handler: `server/api/sessions/start.post.ts`
  Fixture: temp SQLite DB via `makeTempDir`, drizzle migrations applied, seed `empty`.
  - [ ] POST with valid body → 200, session row created in DB
  - [ ] POST with missing workspaceId → 400
  - [ ] POST without auth → 401
  - [ ] Concurrent POSTs from same user → second returns existing session id

  #### Driver: `src/drivers/omp-driver.ts`
  Fixture: `tests/fixtures/fake-omp/` script + `OMP_BRIDGE_BIN=<path>` env.
  Gated by `SKAILE_SPAWN_TESTS=1`.
  - [ ] spawn → captures stdout
  - [ ] SIGTERM → exit within 2s
  - [ ] dispose() → kills orphan process (verified via `ps`)

  ### E2E Layer (L4/L5)

  #### Journey: Start a workspace session (L5)
  Route: /workspace/:id
  Seed: populated (`test/e2e/fixtures.ts` — default user + one workspace)
  - [ ] User opens /workspace/w1 → workspace loads, session starts
  - [ ] 375px viewport — layout renders without overflow

  #### CLI: `skaile session start` (L4)
  Harness: `runCli(["session", "start", ...], { cwd: makeTempDir() })`.
  - [ ] Happy: exit 0, stdout contains "session id:"
  - [ ] Error: no workspace → exit 1, stderr mentions "workspace"

  [repeat for each journey]

  ## Fixtures

  | Name | Used By | Seed | Loader |
  |---|---|---|---|
  | empty | unit + integration | no seed | inline |
  | populated | integration + e2e | 1 user, 2 workspaces, 3 sessions | `loadFixtureWorkspace("populated")` |
  | fake-omp | L3 drivers | exec script at tests/fixtures/fake-omp/omp | env `OMP_BRIDGE_BIN` |

  ## Out of Scope

  - src/generated/**/* — schema-generated, not worth testing in isolation
  - Deprecated exports (see CLAUDE.md "Deprecated")
  - Bun-incompatible paths (e.g. `better-sqlite3` under Bun) — mock at the boundary

STEP 9: Backup previous plan
  IF <target>/TEST_PLAN.md existed → rename to TEST_PLAN.prev.md before overwriting

STEP 10: Report
  [test-plan] target=<pkg> layer=<L> category=<cat> → N unit + N integration + N e2e scenarios
  Baseline: <current_pct>% lines | Target: <target_pct>% | Gap: +<delta> pts
  Existing: N covered | To add: N
  Plan written to <target>/TEST_PLAN.md
  Next: run `test-unit target=<pkg>` to generate unit tests from the plan.

EMIT [test-plan] completed target=<pkg> layer=<L> unit=<N> integration=<N> e2e=<N> gap=<pct>

CHECKLIST
  - [ ] CLAUDE.md read before unit discovery
  - [ ] Package category + test layer classified (L0-L5)
  - [ ] Baseline coverage read from `_devlog/reports/coverage-baseline-2026-04-22/summary.json`
  - [ ] Gap-to-target computed for the primary layer
  - [ ] Fully uncovered src/ files flagged in `Coverage Gap` section
  - [ ] Existing coverage scanned and excluded
  - [ ] Every unit classified into exactly one layer
  - [ ] Every scenario cites a helper from @skaile/test-utils where applicable
  - [ ] Every integration/e2e scenario has a fixture strategy + env-var gating if needed
  - [ ] Prior TEST_PLAN.md backed up if it existed
  - [ ] Layers input respected (skipped layers have no section)

---

## Integration

- **Called by:** `test-unit`, `test-integration`, `test-e2e`, `ready` (as coverage evidence), `quality`
- **Reads:** `<target>/CLAUDE.md`, `<target>/src/**`, `references/test_stack_map.md`,
  `_devlog/specs/2026-04-22-test-concept-design.md`, `_devlog/plans/2026-04-22-test-gap-fill.md`,
  `_devlog/reports/coverage-baseline-2026-04-22/summary.json`,
  `_devlog/reports/coverage-ci/coverage-summary.json` (optional)
- **Writes:** `<target>/TEST_PLAN.md`
- **Related:** after the plan lands, `test-unit` / `test-integration` / `test-e2e` generate the
  actual test files; `audit scope=package target=<pkg>` re-runs coverage against the plan; the
  coverage ratchet at `_scripts/check-coverage-ratchet.ts` gates regressions in CI.
