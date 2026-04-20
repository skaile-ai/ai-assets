---
name: "test-integration"
description: "Set up and generate integration tests for any skaile-dev package. Covers API routes, DB operations, and cross-module flows. Knows the storage backends: SQLite + drizzle for forge apps, PostgreSQL + Prisma for platform/backend, temp-dir state for agent-framework. Scaffolds test DB isolation (in-memory SQLite / ephemeral test DB / temp workspaces), auth helpers, seed fixtures, then generates tests. Verifies tests run."
metadata:
  version: "1.0.0"
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
        hint: "e.g. forge/project, platform/backend, agent-framework/runner"
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
      - path: "<target>/tests/integration/fixtures/*.ts"
      - path: "<target>/tests/integration/auth.ts"
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

Sets up integration-test infrastructure and generates tests that exercise the **real stack boundary** of a package: API request → handler → business logic → storage → response. Unlike unit tests, integration tests use real implementations (real DB, real filesystem) — but they stay inside one package; cross-service flows are reserved for `test-e2e`.

## Storage Matrix

| Package | Storage | Isolation strategy |
|---|---|---|
| `forge/project`, `forge/concept` (+ other forge apps) | SQLite + drizzle-orm | `:memory:` DB **or** per-test tempfile via `fs.mkdtempSync`; drizzle migrations in `beforeAll` |
| `platform/backend` | PostgreSQL + Prisma | Separate test database (`DATABASE_URL_TEST`); `prisma migrate deploy` in `beforeAll`; truncate tables in `afterEach` |
| `agent-framework/runner`, `session`, `bridge`, `workspace`, `lab` | Temp dirs on disk | `fs.mkdtempSync(os.tmpdir())` per test; cleanup in `afterEach`; no DB |
| `agent-framework/connectors` | Per-connector (postgres, redis, sqlite, ...) | Testcontainers or `:memory:` variants; connector-specific |
| `agent-framework/flow-engine`, `resolver`, `core`, `types`, `transport` | Pure logic | Usually no integration layer; only if they expose network ports |
| `platform/frontend` | No direct storage | Mock API via MSW or stub fetch; render via TanStack providers |

## When to Use

- Adding coverage for API routes in forge apps
- Adding Jest test suite for a new NestJS controller in platform/backend
- Verifying agent-framework runner / session / bridge end-to-end (in-package)
- Exercising drizzle queries against a real SQLite schema
- After `test-plan` flags integration gaps

## When NOT to Use

- For pure-logic coverage — use `test-unit`
- For cross-package browser journeys — use `test-e2e`
- For unit-testable pure functions that happen to touch the filesystem trivially — refactor to pure + use `test-unit`

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

WRITES
  <target>/tests/integration/setup.ts                — per-test DB setup / teardown
  <target>/tests/integration/auth.ts                 — auth helper (if applicable)
  <target>/tests/integration/fixtures/*.ts           — seed builders
  <target>/tests/integration/<feature>.test.ts       — test files

MUST  read package CLAUDE.md before generating
MUST  detect storage backend from package.json + config files
MUST  scaffold DB isolation before any test is generated (in-memory or tempfile or test DB)
MUST  reset state in afterEach (truncate / tempdir cleanup) — never leak between tests
MUST  generate a seed/fixture for every scenario that needs data
MUST  include both success (2xx) and auth/validation (4xx) tests for every endpoint
MUST  verify tests run and pass before reporting
NEVER use the production database
NEVER hardcode test data inline — use fixture builders
NEVER mock the DB — use real storage (that's the whole point)
NEVER leave test DB running after suite completion

EMIT [test-integration] started target=<pkg> mode=<mode>

# ── Phase 1: Classify & Plan ─────────────────────────────────────

STEP 1: Load context
  - Read <target>/CLAUDE.md
  - Read <target>/TEST_PLAN.md if exists
  - Read <target>/package.json

STEP 2: Determine storage backend
  IF storage = auto:
    IF deps include 'drizzle-orm' + 'better-sqlite3' or 'libsql' → sqlite-memory (preferred) or sqlite-tempfile
    ELSE IF deps include '@prisma/client' + 'pg' → postgres-testdb
    ELSE IF path matches agent-framework/(runner|session|workspace|bridge|lab) → temp-dir
    ELSE IF package only has pure logic → none (use test-unit instead — abort)

STEP 3: Detect existing integration infra
  - integration_dir exists? (tests/integration/ or equivalent)
  - setup.ts exists?
  - fixtures folder exists?

# ── Phase 2: Setup ───────────────────────────────────────────────

STEP 4 (skip if mode=generate): Scaffold integration infrastructure

  Common:
    - Create <target>/tests/integration/ if missing
    - Ensure vitest.config.ts includes 'tests/integration/**/*.test.ts' in `test.include`
    - Set a longer timeout for integration (30000ms) via `test.integrationTimeout` or matching pattern

  SQLite (forge apps):
    WRITE <target>/tests/integration/setup.ts
    ```typescript
    import { mkdtempSync, rmSync } from 'node:fs'
    import { tmpdir } from 'node:os'
    import { join } from 'node:path'
    import { drizzle } from 'drizzle-orm/better-sqlite3'
    import Database from 'better-sqlite3'
    import { migrate } from 'drizzle-orm/better-sqlite3/migrator'

    export async function createTestDb() {
      const dir = mkdtempSync(join(tmpdir(), 'skaile-test-'))
      const sqlite = new Database(join(dir, 'test.sqlite'))
      const db = drizzle(sqlite)
      await migrate(db, { migrationsFolder: './drizzle' })
      return {
        db,
        cleanup: () => {
          sqlite.close()
          rmSync(dir, { recursive: true, force: true })
        },
      }
    }
    ```

  Postgres (platform/backend):
    WRITE <target>/tests/integration/setup.ts (if none):
    - reads DATABASE_URL_TEST
    - runs `prisma migrate deploy` in beforeAll
    - per-test: `await prisma.$transaction(...)` with rollback OR truncate in afterEach
    - closes prisma client in afterAll

  Agent-framework (temp-dir):
    WRITE <target>/tests/integration/setup.ts
    ```typescript
    import { mkdtempSync, rmSync } from 'node:fs'
    import { tmpdir } from 'node:os'
    import { join } from 'node:path'

    export function makeTempWorkspace(prefix = 'skaile-') {
      const dir = mkdtempSync(join(tmpdir(), prefix))
      return {
        dir,
        cleanup: () => rmSync(dir, { recursive: true, force: true }),
      }
    }
    ```

  Auth helpers (if package has auth middleware):
    WRITE <target>/tests/integration/auth.ts — helper to mint a test session token / bearer for authenticated requests

  Fixtures:
    Create <target>/tests/integration/fixtures/ with builder functions per entity.
    Example:
    ```typescript
    export async function createUser(db: DrizzleDb, overrides: Partial<NewUser> = {}) {
      const [row] = await db.insert(users).values({
        id: crypto.randomUUID(),
        email: 'test@example.com',
        createdAt: new Date(),
        ...overrides,
      }).returning()
      return row
    }
    ```

EMIT [test-integration] setup_done target=<pkg> backend=<backend>

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

  Template (agent-framework/runner lifecycle, vitest + temp dir):
  ```typescript
  import { describe, it, expect, beforeEach, afterEach } from 'vitest'
  import { makeTempWorkspace } from './setup'
  import { Runner } from '~/src/runner'

  describe('Runner.startSession', () => {
    let ws: ReturnType<typeof makeTempWorkspace>
    beforeEach(() => { ws = makeTempWorkspace() })
    afterEach(() => ws.cleanup())

    it('initializes session dir and writes manifest', async () => {
      const runner = new Runner({ workspace: ws.dir })
      const session = await runner.startSession({ skill: 'greet' })
      expect(fs.existsSync(join(ws.dir, 'sessions', session.id))).toBe(true)
    })
    ...
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

# ── Phase 4: Verify ──────────────────────────────────────────────

STEP 8: Run the generated integration tests
  - Execute the package's integration run command (e.g. `bun x --bun vitest run --project <name> tests/integration`)
  - Capture pass/fail/errors

  IF DB connection errors → report env var / container issue to the user
  IF migration errors → report schema drift
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
  | sessions | tests/integration/sessions.test.ts | 6 | ✓ |
  | workspaces | tests/integration/workspaces.test.ts | 4 | ✓ |

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

- **Called by:** `test-plan` (next step), `implement` (after adding API routes or services), `quality`
- **Reads:** `<target>/CLAUDE.md`, `<target>/TEST_PLAN.md`, drizzle/prisma configs, `test_stack_map.md`
- **Writes:** `<target>/tests/integration/**`
