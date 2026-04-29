---
name: "test-integration"
description: "Set up and generate integration tests for any skaile-dev package. Covers API routes, DB operations, and cross-module flows. Knows the storage backends: SQLite + drizzle for forge apps, PostgreSQL + Prisma for platform/backend, temp-dir state for agent-framework. Scaffolds test DB isolation (in-memory SQLite / ephemeral test DB / temp workspaces), auth helpers, seed fixtures, then generates tests. Verifies tests run."
metadata:
  version: "1.1.0"
  tags:
    - "testing"
    - "integration-tests"
    - "api"
    - "database"
    - "drizzle"
    - "prisma"
    - "setup"
    - "generation"
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
        hint: "e.g. forge/L4-project, platform/backend, agent-framework/runner"
    inputs_optional:
      - id: mode
        label: "Mode"
        type: select
        options:
          - "setup"
          - "generate"
          - "both"
        default: "both"
      - id: storage
        label: "Storage backend override"
        type: select
        options:
          - "auto"
          - "sqlite-memory"
          - "sqlite-tempfile"
          - "postgres-testdb"
          - "temp-dir"
          - "none"
        default: "auto"
        hint: "auto = infer from package.json dependencies"
    reads:
      - path: "<target>/CLAUDE.md"
      - path: "<target>/TEST_PLAN.md"
      - path: "<target>/package.json"
      - path: "<target>/server"
      - path: "<target>/src"
      - path: "<target>/drizzle.config.ts or <target>/prisma/schema.prisma"
      - path: ".env.example"
      - path: "ai-assets/skaile-development/references/test_stack_map.md"
    produces:
      - path: "<target>/tests/integration/**/*.test.ts"
      - path: "<target>/tests/integration/setup.ts"
      - path: "<target>/tests/fixtures/**/*"
      - path: "<target>/tests/fixtures/fake-*.mjs (for subprocess-driver packages)"
      - path: "<target>/tests/integration/auth.ts"
      - path: "<target>/vitest.config.ts (if missing or needs happy-dom)"
      - path: "<target>/package.json (adds @skaile/test-utils devDep if missing)"
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
      - id: "storage"
        label: "Storage backend"
        type: "select"
        options: ["auto", "sqlite-memory", "sqlite-tempfile", "postgres-testdb", "temp-dir", "none"]
        required: false
        default: "auto"
    files: []
---

# Test Integration — Integration Test Setup & Generation

## Overview

Sets up integration-test infrastructure and generates tests that exercise the **real stack boundary** of a package: API request → handler → business logic → storage → response. Unlike unit tests, integration tests use real implementations (real DB, real filesystem, real subprocess) — but they stay inside one package; cross-service flows are reserved for `skaile-dev-test-e2e`.

This skill targets **Layer 3** of the monorepo test concept (cross-module, temp-dir, subprocess, Docker). Read these canonical documents before scaffolding anything:

- **Concept / design spec:** `_devlog/specs/2026-04-22-test-concept-design.md` — layer taxonomy, shared infrastructure, coverage policy.
- **Implementation plan:** `_devlog/plans/2026-04-22-test-gap-fill.md` — phase-by-phase gap-fill for the monorepo.
- **Starlight reference:** `docs/src/content/docs/testing.md` — user-facing testing overview.
- **Shared helpers:** `agent-framework/test-utils/CLAUDE.md` + `agent-framework/test-utils/src/index.ts` — `makeTempDir`, `makeInMemoryTransport`, `makeMockDriver`, `loadFixtureWorkspace`, `withTempProject`.
- **Root overview:** root `CLAUDE.md` § "Testing Strategy".

## Integration Patterns

Integration tests at Layer 3 come in four flavours. Pick the one that matches the package's boundary:

| Pattern | When to use | Helper | Canonical reference |
|---|---|---|---|
| **Subprocess via fake binary** | CLI-driver wrappers (omp, claude-sdk, codex) that spawn an external binary | Fake-binary fixture + env-var override | `agent-framework/bridge/tests/omp-driver.test.ts` + `tests/fixtures/fake-omp.mjs` (redirected via `OMP_BRIDGE_BIN`) |
| **In-memory transport** | Cross-module protocol tests where both halves of a client/server live in one process | `makeInMemoryTransport()` from `@skaile/test-utils` | `agent-framework/client/tests/agent-client.test.ts`, `agent-framework/transport/tests/in-memory-transport.test.ts` |
| **Real filesystem with volume/connector managers** | Connectors, runners, anything that touches YAML/FS; zero mocks | `makeTempDir("pkg-test")` from `@skaile/test-utils` | `agent-framework/connectors/tests/e2e/scaffold-resources.test.ts` |
| **Docker-gated** | Container or compose-stack integration (lab, docker connectors) | `const runIfDocker = process.env.SKAILE_DOCKER_TESTS === "1" ? describe : describe.skip` | `agent-framework/lab/tests/docker/integration.test.ts` |

## Storage Matrix (when the package owns persistent state)

| Package | Storage | Isolation strategy |
|---|---|---|
| `forge/L4-project`, `forge/L5-concept` (+ other forge apps) | SQLite + drizzle-orm | `:memory:` DB **or** per-test tempfile via `makeTempDir`; drizzle migrations in `beforeAll` |
| `platform/backend` | PostgreSQL + Prisma | Separate test database (`DATABASE_URL_TEST`); `prisma migrate deploy` in `beforeAll`; truncate tables in `afterEach` |
| `agent-framework/runner`, `session`, `bridge`, `workspace-plugin`, `lab` | Temp dirs on disk | `makeTempDir("<pkg>-test")` per test; cleanup is auto-scheduled by the helper via `onTestFinished`; no DB |
| `agent-framework/connectors` | Per-connector (postgres, redis, sqlite, ...) | `makeTempDir` + in-memory variants; Docker for container-backed adapters (gate behind `SKAILE_DOCKER_TESTS=1`) |
| `agent-framework/flow-engine`, `resolver`, `core`, `types` | Pure logic | Usually no integration layer; belongs in `skaile-dev-test-unit` |
| `agent-framework/transport`, `client` | Network protocol | `makeInMemoryTransport()` — both halves in one call; no real sockets |
| `platform/frontend` | No direct storage | Mock API via MSW or stub fetch; render via TanStack providers |

## When to Use

- Adding coverage for API routes in forge apps
- Adding Jest test suite for a new NestJS controller in platform/backend
- Verifying agent-framework runner / session / bridge end-to-end (in-package)
- Exercising drizzle queries against a real SQLite schema
- Wrapping a subprocess driver (omp-style) with a fake-binary harness
- Exercising a client/server protocol pair end-to-end in one process
- After `skaile-dev-test-plan` flags integration gaps

## When NOT to Use

- For pure-logic coverage — use `skaile-dev-test-unit`
- For cross-package browser journeys or CLI spawn-harnesses — use `skaile-dev-test-e2e`
- For unit-testable pure functions that happen to touch the filesystem trivially — refactor to pure + use `skaile-dev-test-unit`

## Scaffolding required

Every package that gains an integration suite needs:

1. **`@skaile/test-utils` as a devDependency** (`workspace:*`) — provides `makeTempDir`, `makeInMemoryTransport`, `makeMockDriver`, `loadFixtureWorkspace`, `withTempProject`. Never reinvent these helpers in-package.
2. **`vitest.config.ts` with `environment: "happy-dom"`** if the package contains Vue/DOM-touching code; otherwise omit the field (Node is the default).
3. **Test file location:** `tests/integration/<feature>.test.ts` — this is the Layer 3 convention from the concept spec. Do not colocate integration tests next to source (`.test.ts` colocation is reserved for Layer 1–2 unit tests).
4. **Fixtures:** `tests/fixtures/` at the package root for YAML/JSON inputs. No `__fixtures__`, no `test/fixtures`.

---

ROLE  Integration test setup + generator for a single package. Bootstraps real-storage test harness, then generates tests covering API + DB + cross-module flows.

READS
  ! <target>/CLAUDE.md
  ? <target>/TEST_PLAN.md
  ! <target>/package.json
  ! <target>/(server|src)/**                 — route handlers, services
  ? <target>/drizzle.config.ts               — drizzle schema location
  ? <target>/prisma/schema.prisma            — Prisma schema
  ? .env.example                             — connection string conventions
  ! ai-assets/skaile-development/references/test_stack_map.md
  ! _devlog/specs/2026-04-22-test-concept-design.md  — layer taxonomy
  ! _devlog/plans/2026-04-22-test-gap-fill.md        — phase plan
  ! agent-framework/test-utils/src/index.ts  — shared helpers exports
  ? docs/src/content/docs/testing.md         — user-facing testing overview

WRITES
  <target>/tests/integration/setup.ts                — per-test DB setup / teardown (uses makeTempDir from @skaile/test-utils)
  <target>/tests/integration/auth.ts                 — auth helper (if applicable)
  <target>/tests/integration/<feature>.test.ts       — test files
  <target>/tests/fixtures/**                         — YAML/JSON fixtures (package root, NOT under integration/)
  <target>/tests/fixtures/fake-<cmd>.mjs             — fake-binary fixture for subprocess-driver packages
  <target>/vitest.config.ts                          — adds environment: "happy-dom" + test.include patterns if missing
  <target>/package.json                              — adds @skaile/test-utils devDep (workspace:*) if missing

MUST  read package CLAUDE.md before generating
MUST  read the concept spec (`_devlog/specs/2026-04-22-test-concept-design.md`) + plan (`_devlog/plans/2026-04-22-test-gap-fill.md`) before picking a pattern
MUST  detect integration pattern from package.json + source (subprocess vs. in-memory vs. real-FS vs. docker)
MUST  use `@skaile/test-utils` helpers (`makeTempDir`, `makeInMemoryTransport`, etc.) — never reinvent them
MUST  scaffold isolation before any test is generated (makeTempDir, :memory: DB, test DB, or transport pair)
MUST  reset state in afterEach (truncate / cleanup scheduled by makeTempDir) — never leak between tests
MUST  generate a seed/fixture for every scenario that needs data
MUST  include both success (2xx) and auth/validation (4xx) tests for every endpoint
MUST  gate Docker tests behind `SKAILE_DOCKER_TESTS=1` via `describe.skip`
MUST  gate long subprocess tests behind `SKAILE_SPAWN_TESTS=1`
MUST  verify tests run and pass before reporting
NEVER use the production database
NEVER hardcode test data inline — use fixture builders
NEVER mock the DB — use real storage (that's the whole point)
NEVER open a real network socket in an integration test — use `makeInMemoryTransport()` instead
NEVER leave test DB running after suite completion

EMIT [test-integration] started target=<pkg> mode=<mode>

# ── Phase 1: Classify & Plan ─────────────────────────────────────

STEP 1: Load context
  - Read <target>/CLAUDE.md
  - Read <target>/TEST_PLAN.md if exists
  - Read <target>/package.json

STEP 2: Determine integration pattern
  IF storage = auto:
    IF package wraps a CLI binary (deps include spawn-based driver; src exposes an env-var override like OMP_BRIDGE_BIN) → subprocess-fake-binary
    IF package exposes a client/server protocol pair (agent-framework/client|transport|session) → in-memory-transport
    IF deps include 'drizzle-orm' + 'better-sqlite3' or 'libsql' → sqlite-memory (preferred) or sqlite-tempfile via makeTempDir
    ELSE IF deps include '@prisma/client' + 'pg' → postgres-testdb
    ELSE IF path matches agent-framework/(runner|connectors|workspace-plugin|bridge|lab) → temp-dir via makeTempDir
    ELSE IF package needs Docker (lab, containerised connectors) → docker-gated (SKAILE_DOCKER_TESTS=1)
    ELSE IF package only has pure logic → none (use skaile-dev-test-unit instead — abort)

STEP 3: Detect existing integration infra
  - integration_dir exists? (tests/integration/)
  - `@skaile/test-utils` already a devDependency?
  - fixtures folder exists at `tests/fixtures/`?
  - fake-binary fixtures exist at `tests/fixtures/fake-*.mjs`?

# ── Phase 2: Setup ───────────────────────────────────────────────

STEP 4 (skip if mode=generate): Scaffold integration infrastructure

  Common:
    - Create <target>/tests/integration/ if missing
    - Ensure vitest.config.ts includes 'tests/integration/**/*.test.ts' in `test.include`
    - If package has DOM/Vue code: set `test.environment: "happy-dom"` in vitest.config.ts
    - Set a longer timeout for integration (30000ms) via `test.integrationTimeout` or matching pattern
    - Ensure `@skaile/test-utils` is in devDependencies (workspace:*)

  Subprocess via fake binary (CLI-driver wrappers — omp, claude-sdk, codex style):
    Reference: `agent-framework/bridge/tests/omp-driver.test.ts` + `agent-framework/bridge/tests/fixtures/fake-omp.mjs` + `agent-framework/bridge/src/drivers/omp.ts`.
    Steps:
      1. In the *source* driver, expose an env-var override for the spawn binary (e.g. `OMP_BRIDGE_BIN`, `OMP_BRIDGE_PREARGS`) as an explicit test-only extension point. Document it in the package's CLAUDE.md Environment Variables table.
      2. Write a fake-binary fixture at `tests/fixtures/fake-<cmd>.mjs` that speaks the driver's protocol (JSON-RPC, newline-delimited JSON, whatever the real binary emits).
      3. In the test, point the driver at the fake via the env vars; run the spawn → ready → prompt → event → kill lifecycle end-to-end.
      4. Gate long-running subprocess cases behind `SKAILE_SPAWN_TESTS=1` if they exceed ~10s.

  In-memory transport (client/server protocol tests):
    Reference: `agent-framework/client/tests/agent-client.test.ts`, `agent-framework/transport/tests/in-memory-transport.test.ts`.
    ```typescript
    import { makeInMemoryTransport } from "@skaile/test-utils";

    const { client, server } = makeInMemoryTransport();
    // wire client + server and assert the round-trip preserves message IDs, ordering, etc.
    ```
    Never use real WebSocket/TCP sockets here — that belongs in a higher layer.

  Real filesystem (connectors, runners, workspace-plugin):
    Reference: `agent-framework/connectors/tests/e2e/scaffold-resources.test.ts`.
    ```typescript
    import { makeTempDir } from "@skaile/test-utils";

    test("scaffolds a workspace from a manifest", async () => {
      const dir = makeTempDir("connectors-test");
      // real I/O, zero mocks — cleanup is scheduled by makeTempDir via onTestFinished
    });
    ```
    Do not reinvent `mkdtempSync`/`rmSync` boilerplate. `makeTempDir` registers cleanup automatically.

  SQLite (forge apps):
    WRITE <target>/tests/integration/setup.ts
    ```typescript
    import { makeTempDir } from "@skaile/test-utils";
    import { join } from "node:path";
    import Database from "better-sqlite3";
    import { drizzle } from "drizzle-orm/better-sqlite3";
    import { migrate } from "drizzle-orm/better-sqlite3/migrator";

    export async function createTestDb() {
      const dir = makeTempDir("forge-test");
      const sqlite = new Database(join(dir, "test.sqlite"));
      const db = drizzle(sqlite);
      await migrate(db, { migrationsFolder: "./drizzle" });
      return { db, close: () => sqlite.close() };
    }
    ```
    (No manual directory cleanup — `makeTempDir` handles it.)

  Postgres (platform/backend):
    WRITE <target>/tests/integration/setup.ts (if none):
    - reads DATABASE_URL_TEST
    - runs `prisma migrate deploy` in beforeAll
    - per-test: `await prisma.$transaction(...)` with rollback OR truncate in afterEach
    - closes prisma client in afterAll

  Docker-gated (lab, container-backed connectors):
    Reference: `agent-framework/lab/tests/docker/integration.test.ts`.
    ```typescript
    import { describe } from "vitest";

    const runIfDocker = process.env.SKAILE_DOCKER_TESTS === "1" ? describe : describe.skip;

    runIfDocker("Docker integration — <feature>", () => {
      // spin up compose stack, assert, tear down
    });
    ```
    The full CI lane flips `SKAILE_DOCKER_TESTS=1` only when lab is known-stable. Local runs opt in manually.

  Auth helpers (if package has auth middleware):
    WRITE <target>/tests/integration/auth.ts — helper to mint a test session token / bearer for authenticated requests.

  Fixtures:
    Create <target>/tests/fixtures/ (package root — not under tests/integration/) with builder functions per entity. YAML fixtures under `tests/fixtures/workspaces/*.yaml`, JSON fixtures under `tests/fixtures/<domain>/*.json`. Load via `loadFixtureWorkspace("<name>")` from `@skaile/test-utils` where applicable.

EMIT [test-integration] setup_done target=<pkg> pattern=<pattern>

# ── Phase 3: Generation ──────────────────────────────────────────

STEP 5 (skip if mode=setup): Enumerate integration targets

  From TEST_PLAN.md integration layer, OR from discovery:

  - API routes: glob `<target>/server/api/**/*.ts` (forge) or controllers (platform/backend) or `src/routes/**` (agent CLI)
  - DB repositories / services: classes with @Injectable + prisma, or drizzle-query modules
  - Agent framework: runner public methods, session lifecycle, flow-engine + bridge end-to-end in-package
  - Frontend (platform): skip — this layer is mostly unit + e2e for frontend

STEP 6: Generate test files

  Test file layout: one file per resource/feature, named `<resource>.test.ts`.

  Template (forge API route, vitest + drizzle):
  ```typescript
  import { describe, it, expect, beforeEach, afterEach } from 'vitest'
  import { createTestDb } from './setup'
  import { createUser } from './fixtures/users'
  import { POST as startSession } from '~/server/api/sessions/start.post'

  describe('POST /api/sessions/start', () => {
    let ctx: Awaited<ReturnType<typeof createTestDb>>
    beforeEach(async () => { ctx = await createTestDb() })
    afterEach(() => ctx.cleanup())

    it('creates a new session for authenticated user', async () => {
      const user = await createUser(ctx.db)
      const event = mockEvent({ body: { workspaceId: 'w1' }, user })
      const res = await startSession(event)
      expect(res.id).toBeDefined()
      const rows = await ctx.db.select().from(sessions).where(eq(sessions.userId, user.id))
      expect(rows).toHaveLength(1)
    })

    it('returns 400 when workspaceId is missing', async () => {
      ...
    })

    it('returns 401 without auth', async () => {
      ...
    })
  })
  ```

  Template (NestJS controller, Jest + Prisma):
  ```typescript
  import { Test } from '@nestjs/testing'
  import { INestApplication } from '@nestjs/common'
  import * as request from 'supertest'

  describe('SessionsController (integration)', () => {
    let app: INestApplication
    let prisma: PrismaService

    beforeAll(async () => {
      const mod = await Test.createTestingModule({ imports: [AppModule] }).compile()
      app = mod.createNestApplication()
      prisma = app.get(PrismaService)
      await app.init()
    })
    afterAll(async () => { await prisma.$disconnect(); await app.close() })
    afterEach(async () => { await prisma.session.deleteMany() })

    it('POST /sessions creates a session', async () => {
      const res = await request(app.getHttpServer())
        .post('/sessions')
        .set('Authorization', await testBearer())
        .send({ workspaceId: 'w1' })
      expect(res.status).toBe(201)
      const row = await prisma.session.findFirst()
      expect(row?.workspaceId).toBe('w1')
    })
    ...
  })
  ```

  Template (agent-framework/runner lifecycle, vitest + makeTempDir):
  ```typescript
  import { describe, it, expect } from 'vitest'
  import { makeTempDir } from '@skaile/test-utils'
  import { Runner } from '../src/runner'

  describe('Runner.startSession', () => {
    it('initializes session dir and writes manifest', async () => {
      const dir = makeTempDir('runner-test')
      const runner = new Runner({ workspace: dir })
      const session = await runner.startSession({ skill: 'greet' })
      expect(fs.existsSync(join(dir, 'sessions', session.id))).toBe(true)
    })
    ...
  })
  ```

  Template (subprocess fake-binary, vitest + fake-omp pattern):
  ```typescript
  import { fileURLToPath } from 'node:url'
  import { afterEach, beforeEach, describe, expect, it } from 'vitest'
  import { OmpDriver } from '../src/drivers/omp.ts'

  const fakeBinPath = fileURLToPath(new URL('./fixtures/fake-omp.mjs', import.meta.url))

  function useFakeBinary(mode = 'normal') {
    process.env.OMP_BRIDGE_BIN = process.execPath
    process.env.OMP_BRIDGE_PREARGS = fakeBinPath
    process.env.FAKE_OMP_MODE = mode
  }

  describe('OmpDriver subprocess harness', () => {
    beforeEach(() => useFakeBinary())
    afterEach(() => {
      delete process.env.OMP_BRIDGE_BIN
      delete process.env.OMP_BRIDGE_PREARGS
      delete process.env.FAKE_OMP_MODE
    })

    it('spawns, emits ready, handles a prompt, and exits cleanly on dispose', async () => {
      const driver = new OmpDriver({ cwd: process.cwd() })
      await driver.start()
      // ...assert events, then:
      await driver.kill()
    })
  })
  ```

  Template (in-memory transport, vitest + makeInMemoryTransport):
  ```typescript
  import { describe, it, expect } from 'vitest'
  import { makeInMemoryTransport } from '@skaile/test-utils'
  import { AgentClient } from '../src/client'

  describe('AgentClient round-trip', () => {
    it('pairs request and response via correlation id', async () => {
      const { client: clientTransport, server: serverTransport } = makeInMemoryTransport()
      // wire a minimal server on serverTransport, then drive the client:
      const client = new AgentClient(clientTransport)
      await expect(client.prompt('hi')).resolves.toMatchObject({ ok: true })
    })
  })
  ```

STEP 7: Generate scenarios
  Cover per endpoint / service method:
  - Happy path (2xx)
  - Validation failure (400)
  - Auth required / forbidden (401/403)
  - Not found (404)
  - Conflict / constraint violation (409)
  - Boundary: empty DB, populated DB
  - Concurrency where relevant

  Subprocess-specific scenarios (where applicable):
  - Ready signal: spawn → driver emits "ready" within timeout
  - Prompt round-trip: send → receive event stream → assert contents
  - stderr capture: fake binary writes to stderr → driver surfaces it, does not drop
  - Crash handling: fake binary exits nonzero → driver emits typed error + cleans up
  - Kill on dispose: `driver.kill()`/`dispose()` terminates the child within ~2s

## Constraints

- **Integration runtime budget:** Integration tests run in the CI **full lane** (`.github/workflows/test-full.yml`). Keep each test under ~10s to stay inside the lane's overall budget. Longer cases go behind a gate.
- **Docker:** Any test that needs Docker MUST gate behind `SKAILE_DOCKER_TESTS=1` (see the `runIfDocker` pattern). CI flips this in the full lane only when lab is stable.
- **Subprocess:** Long-running subprocess tests (>10s cumulative) MUST gate behind `SKAILE_SPAWN_TESTS=1`. Short spawn-then-kill cycles are fine unguarded.
- **No real network:** protocol tests use `makeInMemoryTransport()`; never open a real WebSocket/TCP port in an integration test.
- **No publishing:** `@skaile/test-utils` is dev-only. Import it from `devDependencies`, never `dependencies`.

# ── Phase 4: Verify ──────────────────────────────────────────────

STEP 8: Run the generated integration tests
  - Execute the package's integration run command (e.g. `bun x --bun vitest run --project <name> tests/integration`)
  - Capture pass/fail/errors
  - Then run the monorepo full-suite regression: `bun x --bun vitest run` from the repo root (skip this if the monorepo suite is known broken for unrelated reasons — note it in the report instead).

  IF DB connection errors → report env var / container issue to the user
  IF migration errors → report schema drift
  IF subprocess tests hang → check the fake-binary fixture emits the ready signal; verify the env-var override is wired in the driver
  IF Docker tests all skip → confirm `SKAILE_DOCKER_TESTS=1` was set (usually intentional locally)
  IF assertion fails because source has a bug → report the bug; do not modify the assertion

# ── Phase 5: Report ──────────────────────────────────────────────

STEP 9: Report
  ## Integration Test Setup — <target>

  ### Storage
  Backend: <sqlite-tempfile|postgres-testdb|temp-dir>
  Isolation: <per-test / per-suite>

  ### Generated Infrastructure
  - [x] tests/integration/setup.ts
  - [x] tests/integration/auth.ts (if applicable)
  - [x] tests/integration/fixtures/*

  ### Generated Tests
  | Resource | File | Scenarios | Status |
  |---|---|---|---|
  | sessions | tests/integration/sessions.test.ts | 6 | pass |
  | workspaces | tests/integration/workspaces.test.ts | 4 | pass |

  ### Results
  - Total: N, passing: N, failing: N

  ### Issues Found
  | Test | Issue | Fix |

EMIT [test-integration] completed target=<pkg> files=<N> tests=<N> passing=<N>

CHECKLIST
  - [ ] Storage backend correctly detected
  - [ ] Test DB / workspace isolation scaffolded
  - [ ] State reset between tests
  - [ ] Fixtures modular and reusable
  - [ ] Auth helper present where needed
  - [ ] Each endpoint: happy + 400 + auth cases at minimum
  - [ ] All tests run (pass or clear source-bug report)

---

## Integration

- **Called by:** `skaile-dev-test-plan` (next step), `skaile-dev-implement` (after adding API routes or services), `skaile-dev-quality-gate`
- **Reads:** `<target>/CLAUDE.md`, `<target>/TEST_PLAN.md`, drizzle/prisma configs, `test_stack_map.md`, `_devlog/specs/2026-04-22-test-concept-design.md`, `_devlog/plans/2026-04-22-test-gap-fill.md`, `agent-framework/test-utils/src/index.ts`
- **Writes:** `<target>/tests/integration/**`, `<target>/tests/fixtures/**` (including `fake-<cmd>.mjs` fixtures for subprocess drivers), `<target>/vitest.config.ts` (adds `environment: "happy-dom"` + `test.include` patterns if missing), `<target>/package.json` (adds `@skaile/test-utils` devDependency if missing)

## Canonical References

Cite these when explaining a pattern to the user or when producing the generated tests:

| Pattern | File |
|---|---|
| Subprocess via fake binary | `agent-framework/bridge/tests/omp-driver.test.ts`, `agent-framework/bridge/tests/fixtures/fake-omp.mjs`, `agent-framework/bridge/src/drivers/omp.ts` (the `OMP_BRIDGE_BIN` override) |
| In-memory transport | `agent-framework/client/tests/agent-client.test.ts`, `agent-framework/transport/tests/in-memory-transport.test.ts` |
| Real-FS volume/connector | `agent-framework/connectors/tests/e2e/scaffold-resources.test.ts` |
| Docker-gated | `agent-framework/lab/tests/docker/integration.test.ts` |
