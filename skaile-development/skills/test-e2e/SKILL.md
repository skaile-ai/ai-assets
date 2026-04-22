---
name: "test-e2e"
description: "Set up and generate end-to-end tests for forge apps, platform/frontend, and the agent-framework CLI. Scaffolds Playwright for web apps (forge + platform) including config, global setup/teardown, sandbox, and fixtures. For CLI packages, generates shell-based end-to-end tests that invoke the bin and assert stdout/artifacts. Verifies tests run against a real dev server or real process."
metadata:
  version: "1.1.0"
  tags:
    - "testing"
    - "e2e"
    - "playwright"
    - "browser"
    - "cli"
    - "setup"
    - "generation"
    - "journey"
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
        hint: "forge app, platform/frontend, or agent-framework/cli"
    inputs_optional:
      - id: mode
        label: "Mode"
        type: select
        options:
          - "setup"
          - "generate"
          - "both"
        default: "both"
      - id: kind
        label: "E2E kind"
        type: select
        options:
          - "auto"
          - "web"
          - "cli"
        default: "auto"
        hint: "auto = infer from package type | web = Playwright | cli = shell"
    reads:
      - path: "<target>/CLAUDE.md"
      - path: "<target>/TEST_PLAN.md"
      - path: "<target>/package.json"
      - path: "<target>/app/pages"
      - path: "<target>/server/api"
      - path: "<target>/server/utils/auth.ts"
      - path: "<target>/src/commands"
      - path: "ai-assets/skaile-development/references/test_stack_map.md"
      - path: "_devlog/specs/2026-04-22-test-concept-design.md"
      - path: "_devlog/plans/2026-04-22-test-gap-fill.md"
      - path: "docs/src/content/docs/testing.md"
      - path: "agent-framework/test-utils/src/index.ts"
    produces:
      - path: "<target>/playwright.config.ts"
      - path: "<target>/tests/e2e/*.spec.ts (forge/concept uses test/e2e/)"
      - path: "<target>/tests/e2e/fixtures.ts"
      - path: "<target>/tests/e2e/sandbox.ts"
      - path: "<target>/tests/e2e/global-setup.ts"
      - path: "<target>/tests/e2e/global-teardown.ts"
      - path: "<target>/tests/cli-e2e/setup.ts (L4 CLI)"
      - path: "<target>/tests/cli-e2e/<group>.test.ts (L4 CLI)"
      - path: "<target>/tests/_setup/h3-event.ts (L5 Nitro integration)"
      - path: "<target>/tests/_setup/nitro-globals.ts (L5 Nitro integration)"
      - path: "<target>/tests/api-<route>.test.ts (L5 Nitro integration)"
      - path: "<target>/tests/_setup/spawn-server.ts (L5 spawn-harness, optional)"
      - path: "<target>/tests/api-server-harness.test.ts (L5 spawn-harness, gated)"
      - path: "<target>/vitest.config.ts (adds happy-dom + setupFiles if missing)"
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
      - id: "kind"
        label: "Kind"
        type: "select"
        options: ["auto", "web", "cli"]
        required: false
        default: "auto"
    files: []
---

# Test E2E — End-to-End Test Setup & Generation

## Overview

Bootstraps and generates end-to-end tests for the top of the layer stack. Two flavours, cleanly distinguished:

| Layer | Kind | Applies to | Framework | Pattern | Test suffix |
|---|---|---|---|---|---|
| **L4** | CLI spawn-harness | `agent-framework/cli` (and `sdk` acceptance) | Vitest | `runCli(args, { cwd })` spawns the real binary, asserts stdout/exit | `.test.ts` |
| **L5** | Nitro integration (cheap cousin) | `forge/project`, `forge/assistant`, `forge/concept` | Vitest + synthetic h3 event | In-process route handler invoked with `makeEvent({ cookies })` | `.test.ts` (prefix `api-`) |
| **L5** | Spawned-server harness | `forge/project`, `forge/assistant` | Vitest + child Nitro process | Real Nuxt dev server spawned, gated behind `FORGE_SERVER_TESTS=1` | `.test.ts` (prefix `api-server-`) |
| **L5** | Web E2E | `forge/*`, `platform/e2e` | Playwright | Browser journeys against a running dev server | `.spec.ts` |

Read these canonical documents before scaffolding anything:

- **Concept / design spec:** `_devlog/specs/2026-04-22-test-concept-design.md` — layer taxonomy, the L4/L5 distinction, the three-layer pyramid for forge apps.
- **Implementation plan:** `_devlog/plans/2026-04-22-test-gap-fill.md` — Phase C (CLI E2E) and Phase D (forge pyramid).
- **Starlight reference:** `docs/src/content/docs/testing.md` — user-facing testing overview, CI lane split.
- **Root overview:** root `CLAUDE.md` § "Testing Strategy".
- **Shared helpers:** `agent-framework/test-utils/CLAUDE.md` + `agent-framework/test-utils/src/index.ts` — used by the CLI E2E harness (`makeTempDir`).

Platform E2E already lives in `platform/e2e/` with its own config — for the platform this skill *extends* it rather than scaffolding a new one. `forge/concept` (historical `test/e2e/` layout), `forge/project` (`tests/e2e/`), `forge/assistant` (`tests/e2e/`), and `platform/e2e/` are the four reference Playwright suites.

The agent-framework itself has no Playwright layer — runner / bridge / session packages are covered by integration tests. `agent-framework/cli` owns the dedicated L4 CLI-e2e suite that invokes the compiled bin via `bun`.

## When to Use

- Adding Playwright coverage to a forge app (`forge/project`, `forge/assistant`, `forge/concept`) or the platform suite
- Adding journeys after a new page/route is implemented
- Adding L4 CLI E2E tests for a new skaile subcommand in `agent-framework/cli`
- Adding L5 Nitro integration tests (synthetic h3 event) for a new forge API route
- Adding an L5 spawned-server case when `vi.mock` can't reach a route's deep transitive graph (documented pattern, gated behind `FORGE_SERVER_TESTS=1`)
- After `test-plan` flags an e2e gap

## When NOT to Use

- For pure-logic tests — use `test-unit`
- For in-package API + DB tests that don't need a real browser or real binary — use `test-integration`
- For agent-framework runner / session / bridge libraries — use `test-integration` (in-package integration covers them)
- For visual review of a single page — use `verify-ui`

---

ROLE  E2E test setup + generator. Scaffolds Playwright (web) or CLI harness, generates journey tests, and verifies they pass.

READS
  ! <target>/CLAUDE.md
  ? <target>/TEST_PLAN.md
  ! <target>/package.json
  Web: <target>/app/pages/**, <target>/server/api/**, <target>/public/index.html
  CLI: <target>/src/commands/**, <target>/src/bin.ts, <target>/bin
  ! ai-assets/skaile-development/references/test_stack_map.md

WRITES
  L5 Playwright (forge apps, platform/e2e extension):
    <target>/playwright.config.ts                   — if missing
    <target>/tests/e2e/global-setup.ts              — if missing (forge/concept uses test/e2e/)
    <target>/tests/e2e/global-teardown.ts           — if missing
    <target>/tests/e2e/fixtures.ts                  — shared test fixtures
    <target>/tests/e2e/sandbox.ts                   — isolated per-test workspace/data
    <target>/tests/e2e/<flow>.spec.ts               — per-flow tests (.spec.ts suffix)
    <target>/tests/e2e/screenshots/                 — captured on failure
  L5 Nitro integration (forge apps):
    <target>/tests/_setup/h3-event.ts               — synthetic h3 event
    <target>/tests/_setup/nitro-globals.ts          — Nitro global stubs for Vitest
    <target>/tests/api-<route>.test.ts              — per-route integration tests
    <target>/vitest.config.ts                       — sets environment: "happy-dom" + setupFiles
  L5 Spawned-server harness (forge/project, forge/assistant; gated):
    <target>/tests/_setup/spawn-server.ts           — spawns Nuxt dev server, returns { url, stop }
    <target>/tests/api-server-harness.test.ts       — canonical gated test (FORGE_SERVER_TESTS=1)
  L4 CLI spawn-harness (agent-framework/cli + package.json with `bin`):
    <target>/tests/cli-e2e/setup.ts                 — runCli harness, makeTempProject via @skaile/test-utils
    <target>/tests/cli-e2e/<group>.test.ts          — per-subcommand-group tests (3-6 tests each)

MUST  read CLAUDE.md before scaffolding anything
MUST  read the concept spec (`_devlog/specs/2026-04-22-test-concept-design.md`) + plan (`_devlog/plans/2026-04-22-test-gap-fill.md`) before choosing between L4 CLI, L5 Nitro integration, L5 spawned-server, or L5 Playwright
MUST  reuse existing reference configs: `agent-framework/cli/tests/cli-e2e/` for L4, `forge/project/tests/` for L5 Nitro integration, `forge/project/playwright.config.ts` + `forge/assistant/playwright.config.ts` + `forge/concept/playwright.config.ts` + `platform/e2e/playwright.config.ts` for L5 Playwright
MUST  scaffold isolated per-test sandbox — no leaking state between runs (use `makeTempDir` / `makeTempProject` from `@skaile/test-utils` for L4)
MUST  start the dev server (L5 Playwright) via `webServer` in playwright.config.ts; the L4 harness invokes `src/index.ts` via `bun` directly (no build step)
MUST  screenshot every failing L5 Playwright journey step automatically (`screenshot: 'only-on-failure'`)
MUST  verify generated tests execute before reporting
MUST  pick a dedicated port per forge app to avoid conflicts (forge/concept uses 3344; assign unique ports — see the port allocation table at the bottom of this file)
MUST  use `.test.ts` for L4/Nitro integration/spawn-server; `.spec.ts` for L5 Playwright
MUST  gate L5 spawned-server tests behind `FORGE_SERVER_TESTS=1`
NEVER run Playwright in the fast or full CI lanes — E2E lane only (`.github/workflows/test-e2e.yml`)
NEVER run against a production URL
NEVER hardcode absolute paths — always use the package's temp/sandbox helper
NEVER leave dev servers running — ensure globalTeardown kills all child processes

EMIT [test-e2e] started target=<pkg> mode=<mode> kind=<kind>

# ── Phase 1: Classify ────────────────────────────────────────────

STEP 1: Determine kind
  IF kind = auto:
    IF <target> is platform/frontend OR platform/e2e → STOP and hand off to `e2e-platform` skill. That skill already knows the existing platform/e2e harness (session modes, impersonation, org-scoped URLs, failure-mode table) and has two modes: `run` executes the suite with auto-recovery; `add` analyzes a diff and proposes new specs with a user approval gate. Do NOT create a parallel Playwright config.
    IF <target>/nuxt.config.ts exists OR <target>/vite.config.* with React → web (Playwright, L5)
      Also consider: does this forge app need L5 Nitro integration or the L5 spawned-server harness? Both live alongside Playwright and may be generated together.
    IF <target> is agent-framework/cli OR package.json has `bin` entry → cli (L4 spawn-harness)
    ELSE → report "this package is library-grade; use test-integration"

STEP 2: Load context
  - Read CLAUDE.md
  - Read TEST_PLAN.md if present — pick e2e scenarios
  - Read package.json: scripts (dev, start, build), bin
  - Identify dev server port (from nuxt.config.ts / vite.config.ts / env)

# ── Phase 2: Web E2E Setup ───────────────────────────────────────

STEP 3a (web, skip if mode=generate): Scaffold Playwright (L5)

  Each forge app owns its own `playwright.config.ts`. Reference configs:
  - `forge/project/playwright.config.ts` — tests at `tests/e2e/*.spec.ts`
  - `forge/assistant/playwright.config.ts` — tests at `tests/e2e/*.spec.ts`
  - `forge/concept/playwright.config.ts` — tests at `test/e2e/*.spec.ts` (historical; keep the `test/` vs `tests/` quirk for this app only — do not rename)
  - `platform/e2e/playwright.config.ts` — platform suite; do NOT scaffold a parallel config when target is platform/frontend, extend this one instead

  Test-file suffix is `.spec.ts` (Playwright default) — this distinguishes L5 browser specs from L4 Vitest `.test.ts`. The entry path is `tests/e2e/` for new forge apps; only `forge/concept` uses `test/e2e/`.

  Chromium only (matches existing CI config in `.github/workflows/test-e2e.yml`).

  IF <target>/playwright.config.ts missing:
    WRITE <target>/playwright.config.ts
    ```typescript
    import { defineConfig } from '@playwright/test'

    const PORT = <pick unused port based on package, e.g. 3400 for forge/project, 3344 for forge/concept, etc.>

    export default defineConfig({
      testDir: './tests/e2e',
      globalSetup: './tests/e2e/global-setup.ts',
      globalTeardown: './tests/e2e/global-teardown.ts',
      timeout: 60_000,
      retries: process.env.CI ? 2 : 0,
      workers: process.env.CI ? 1 : undefined,
      reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
      use: {
        baseURL: `http://localhost:${PORT}`,
        headless: true,
        screenshot: 'only-on-failure',
        trace: 'retain-on-failure',
        video: 'retain-on-failure',
      },
      projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
      webServer: {
        command: `bun run dev --port ${PORT}`,
        url: `http://localhost:${PORT}`,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
    })
    ```

  IF <target>/tests/e2e/global-setup.ts missing:
    WRITE it — builds DB, seeds `populated` fixture, writes a storage state file used for auth

  IF <target>/tests/e2e/global-teardown.ts missing:
    WRITE it — stops any spawned side-processes

  IF <target>/tests/e2e/sandbox.ts missing (forge apps with SQLite auth):
    WRITE it — `makeSandbox()` creates per-test temp data dir (SQLite file + uploads) and cleans up

  IF <target>/tests/e2e/fixtures.ts missing:
    WRITE it — extends Playwright `test` with a `sandbox` fixture, typed users, and helper actions

  Add to package.json scripts if missing:
    "test:e2e": "playwright test"
    "test:e2e:ui": "playwright test --ui"

  Ensure `@playwright/test` is in devDependencies. If missing → add and run `bun install`.

  Playwright browser install (documented for CI + first-run):
    `bun x --bun playwright install chromium --with-deps`
    CI caches the browser binaries keyed on `bun.lock` + `package.json` hashes — no extra setup needed per app. Local dev: run once after adding `@playwright/test`.

  Add `.gitignore` entries if missing: `playwright-report/`, `test-results/`, `tests/e2e/screenshots/` (or `test/e2e/screenshots/` for forge/concept).

# ── Phase 3: CLI E2E Setup ───────────────────────────────────────

STEP 3b (cli, skip if mode=generate): Scaffold L4 CLI harness

  Reference: `agent-framework/cli/tests/cli-e2e/setup.ts`, `agent-framework/cli/tests/cli-e2e/help.test.ts`, plus `config.test.ts` / `flow.test.ts` / `session.test.ts` for subcommand-group patterns.

  Tests live at `<target>/tests/cli-e2e/<group>.test.ts` (NOT `tests/e2e/` — that suffix is reserved for L5 Playwright `.spec.ts` files). The filename prefix is the top-level subcommand group.

  IF <target>/tests/cli-e2e/ missing:
    CREATE the dir
    WRITE <target>/tests/cli-e2e/setup.ts (mirrors `agent-framework/cli/tests/cli-e2e/setup.ts`):
    ```typescript
    import { resolve } from "node:path";
    import { spawnSync, type SpawnSyncOptions } from "node:child_process";
    import { makeTempDir } from "@skaile/test-utils";

    // Invoke src/index.ts directly via `bun` so the e2e suite is independent
    // of the tsup build step.
    export const BIN = resolve(__dirname, "../../src/index.ts");

    export interface CliResult {
      status: number | null;
      stdout: string;
      stderr: string;
    }

    export function runCli(
      args: string[],
      opts: Pick<SpawnSyncOptions, "cwd" | "env" | "input" | "timeout"> = {},
    ): CliResult {
      const res = spawnSync("bun", [BIN, ...args], {
        cwd: opts.cwd ?? process.cwd(),
        env: { ...process.env, ...(opts.env ?? {}) },
        input: opts.input,
        encoding: "utf8",
        timeout: opts.timeout ?? 30_000,
      });
      return { status: res.status, stdout: res.stdout ?? "", stderr: res.stderr ?? "" };
    }

    export function makeTempProject(prefix = "skaile-cli-e2e") {
      const dir = makeTempDir(prefix);
      return { dir, cleanup: () => { /* no-op; makeTempDir schedules cleanup */ } };
    }
    ```

  Ensure `@skaile/test-utils` is in devDependencies (workspace:*). Add the test script to package.json if missing: `"test:e2e": "bun x --bun vitest run tests/cli-e2e"`. Do NOT require a `bun run build` pre-step — the canonical pattern invokes `src/index.ts` directly via `bun`.

EMIT [test-e2e] setup_done target=<pkg> kind=<kind>

# ── Phase 4: Web Generation ──────────────────────────────────────

STEP 4a (web, skip if mode=setup): Generate journey specs

  Per journey (from TEST_PLAN.md or from discovery of pages):

  Template:
  ```typescript
  import { test, expect } from './fixtures'

  test.describe('Workspace journey', () => {
    test('user creates a workspace and starts a session', async ({ page, sandbox, user }) => {
      await page.goto('/')
      await page.getByRole('button', { name: 'New workspace' }).click()
      await page.getByLabel('Name').fill('Alpha')
      await page.getByRole('button', { name: 'Create' }).click()
      await expect(page.getByText('Alpha')).toBeVisible()

      await page.getByRole('button', { name: 'Start session' }).click()
      await expect(page).toHaveURL(/\/workspace\/.+/)

      // verify DB state via sandbox
      const workspaces = await sandbox.db.select().from(sandbox.schema.workspaces)
      expect(workspaces.find(w => w.name === 'Alpha')).toBeDefined()
    })

    test('responsive — 375px viewport', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 812 })
      await page.goto('/')
      await expect(page).toHaveScreenshot('home-mobile.png')
    })
  })
  ```

  Scenarios per page/feature:
  - Happy path journey end-to-end
  - One error path (network failure / validation)
  - Responsive check for the landing view

# ── Phase 5: CLI Generation ──────────────────────────────────────

STEP 4b (cli, skip if mode=setup): Generate L4 command tests

  One file per top-level subcommand group (not per leaf command). Canonical groups in agent-framework/cli: `help`, `config`, `flow`, `session`, `complete`, `path`, `validate`. Each file exports 3-6 tests: `--help`, a happy path, an error path, and 0-3 targeted edge cases.

  Template (mirrors `agent-framework/cli/tests/cli-e2e/config.test.ts`):
  ```typescript
  import { describe, it, expect } from 'vitest'
  import { runCli, makeTempProject } from './setup'

  describe('skaile config', () => {
    it('--help prints usage', () => {
      const res = runCli(['config', '--help'])
      expect(res.status).toBe(0)
      expect(res.stdout).toContain('config')
    })

    it('config show reports the effective settings in an empty project', () => {
      const { dir } = makeTempProject()
      const res = runCli(['config', 'show'], { cwd: dir })
      expect(res.status).toBe(0)
      expect(res.stdout.length).toBeGreaterThan(0)
    })

    it('rejects setting a project key via config set', () => {
      const { dir } = makeTempProject()
      const res = runCli(['config', 'set', 'framework', 'nuxt'], { cwd: dir })
      expect(res.status).not.toBe(0)
      expect(res.stderr + res.stdout).toMatch(/skaile\.yaml/)
    })
  })
  ```

  Scenarios per subcommand group:
  - `--help` exits 0 and mentions the group name (sanity)
  - Happy path: valid args → zero exit, expected stdout fragment present
  - Error path: missing required arg / bad flag → nonzero exit, useful error message
  - Optional: artifact check (file created, log entry written) when the command has a side effect

  Keep tests fast — every `runCli` call spawns a subprocess, so 30+ tests per file adds up. Target ~3-6 tests per group.

# ── Phase 5.5: L5 Nitro Integration (forge apps) ─────────────────

STEP 4c (web, forge apps only): Scaffold L5 Nitro integration tests

  L5 Nitro integration is the cheap cousin of Playwright: it invokes a Nuxt route handler directly with a synthetic h3 event. No browser, no dev server, just Vitest + `vi.mock` at the boundary. Use it to cover every API route's happy + auth-failure + validation-failure paths cheaply; reserve Playwright for real user journeys.

  Reference files:
  - `forge/project/tests/_setup/h3-event.ts` — `makeEvent({ cookies, body, query, context })` synthetic event
  - `forge/project/tests/_setup/nitro-globals.ts` — setup file that stubs Nitro globals (`defineEventHandler`, `readBody`, `getCookie`, `setCookie`, …)
  - `forge/project/tests/api-auth-me.test.ts` — canonical mock-at-the-boundary example
  - `forge/project/vitest.config.ts` — `setupFiles: ["tests/_setup/nitro-globals.ts"]` + `environment: "happy-dom"`

  Three-file scaffolding when missing:
    1. WRITE `<target>/tests/_setup/h3-event.ts` (copy from `forge/project/tests/_setup/h3-event.ts`)
    2. WRITE `<target>/tests/_setup/nitro-globals.ts` (copy from the same ref)
    3. WRITE/UPDATE `<target>/vitest.config.ts` — set `test.environment: "happy-dom"` and `test.setupFiles: ["tests/_setup/nitro-globals.ts"]`

  Tests live at `<target>/tests/api-<route>.test.ts` (flat under tests/, prefix `api-` — note this is NOT under `tests/integration/`; this is the forge-app convention).

  Cookie-name convention (confirm per app in `server/utils/auth.ts`):

    | Forge app | Cookie name |
    |---|---|
    | `forge/project` | `forge_project_auth` |
    | `forge/assistant` | `forge_assistant_auth` |
    | `forge/concept` | `auth_session` (different pattern — historical, confirm by reading `server/utils/auth.ts`) |

  Mock boundary: any route that reaches `getDb()` or auth primitives MUST mock `@skaile/forge-common-backend`'s `createDb` + `getSessionUser`/`deleteSession` at the module boundary (not deeper). See `forge/project/tests/api-auth-me.test.ts` for the canonical `vi.hoisted` + `vi.mock` shape.

  Template:
  ```typescript
  import { beforeEach, describe, expect, test, vi } from 'vitest'
  import { makeEvent } from './_setup/h3-event'

  const { mockCreateDb, mockGetSessionUser } = vi.hoisted(() => ({
    mockCreateDb: vi.fn(() => ({ _fake: 'db' })),
    mockGetSessionUser: vi.fn(),
  }))

  vi.mock('@skaile/forge-common-backend', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@skaile/forge-common-backend')
    return { ...actual, createDb: mockCreateDb, getSessionUser: mockGetSessionUser }
  })

  describe('GET /api/auth/me', () => {
    beforeEach(() => {
      mockCreateDb.mockClear()
      mockGetSessionUser.mockReset()
    })

    test('returns the authenticated user when the cookie is valid', async () => {
      mockGetSessionUser.mockResolvedValue({ id: 1, username: 'admin', role: 'admin' })
      const handler = (await import('../server/api/auth/me.get')).default as (e: unknown) => Promise<unknown>
      const event = makeEvent({ cookies: { forge_project_auth: 'valid-token' } })
      const result = await handler(event)
      expect(result).toMatchObject({ username: 'admin' })
    })

    test('rejects with a 401 when the cookie is missing', async () => {
      // ...
    })
  })
  ```

# ── Phase 5.6: L5 Spawned-Server Harness (forge apps) ────────────

STEP 4d (web, forge apps only, when vi.mock cannot reach the route's transitive deps): Document the spawned-server escape hatch

  Some routes transitively import `agent-manager.ts` (→ `consola` → `@skaile/agent-bridge` → the full agent-framework chain) and `vi.mock` under Bun+Vitest cannot reliably intercept those deep imports. The fallback is to spawn the real Nuxt dev server as a child process.

  Reference files:
  - `forge/project/tests/_setup/spawn-server.ts` — `{ url, stop }` handle; polls the port; parses "Listening on" from stdout
  - `forge/project/tests/api-server-harness.test.ts` — canonical gate + usage
  - `_devlog/entries/2026-04-22-phase-d2-nitro-integration.md` — documents the vi.mock failure mode + why `birpc` packaging currently blocks flipping the flag in CI

  Tests MUST self-gate behind `FORGE_SERVER_TESTS=1`:
  ```typescript
  const runIfServer = process.env.FORGE_SERVER_TESTS === '1' ? describe : describe.skip
  runIfServer('spawn-server', () => { /* ... */ })
  ```

  Currently disabled in CI — a `birpc` packaging bug blocks enabling it. When that clears, flipping `FORGE_SERVER_TESTS=1` in the e2e lane activates the harness for forge/project + forge/assistant.

  Do not scaffold spawn-server for a forge app that doesn't have it yet unless the user explicitly requests it; Nitro integration (STEP 4c) should be the default.

## Constraints

- **CI lane placement:** Playwright runs ONLY in the E2E lane (`.github/workflows/test-e2e.yml`), never in the fast lane (`test-fast.yml`) or full lane (`test-full.yml`). The L4 CLI spawn-harness runs in the full lane. L5 Nitro integration runs in the full lane. L5 spawned-server runs in the E2E lane once its gate is flipped.
- **Playwright matrix:** one GitHub Actions matrix cell per app — `forge/project`, `forge/assistant`, `forge/concept`, `platform/e2e`. Each cell owns its browser cache (keyed on `bun.lock` + `package.json`). Test results + HTML reports upload on failure only, 14-day retention.
- **Browser install:** `bun x --bun playwright install chromium --with-deps` (CI runs this once per cache key).
- **Chromium only:** matches existing CI. Do not add firefox/webkit projects without explicit approval — they triple the lane runtime.
- **Subprocess spawn cost (L4):** every `runCli` invocation is a bun subprocess. Target 3-6 tests per subcommand-group file; don't explode the count per group. Long-running cases belong behind `SKAILE_SPAWN_TESTS=1` in the integration suite, not the L4 E2E suite.
- **Spawned-server harness (L5):** gated behind `FORGE_SERVER_TESTS=1`. Currently blocked by an unrelated `birpc` packaging bug (see `_devlog/entries/2026-04-22-phase-d2-nitro-integration.md`). Flipping the flag in CI happens when that clears — at which point forge/project + forge/assistant harnesses light up automatically.
- **No `@skaile/test-utils` in Playwright specs:** the Playwright runner does not use the shared vitest helpers. Use Playwright fixtures and the app's own sandbox helpers instead. L4 and Nitro-integration tests (both Vitest-based) DO use `@skaile/test-utils`.

# ── Phase 6: Verify ──────────────────────────────────────────────

STEP 5: Run the e2e suite
  L5 Playwright: `bun x playwright test` inside <target>
  L4 CLI: `bun x --bun vitest run tests/cli-e2e` inside <target>
  L5 Nitro integration: `bun x --bun vitest run tests/api-*.test.ts` inside <target>
  L5 Spawn-server: `FORGE_SERVER_TESTS=1 bun x --bun vitest run tests/api-server-harness.test.ts`

  Capture journey pass/fail + screenshots.
  IF dev server fails to start → report error (port conflict / missing env var)
  IF Playwright browser not installed → run `bun x --bun playwright install chromium --with-deps` and retry
  IF L4 CLI test hangs → check the subcommand's stdin expectations; the harness sets a 30s default timeout
  IF vi.mock under a Nitro integration test can't intercept the real module → escalate to the spawned-server harness (STEP 4d)
  IF assertion fails due to app bug → report it; do NOT modify assertion
  IF flaky test → mark with `test.fixme` and report

# ── Phase 7: Report ──────────────────────────────────────────────

STEP 6: Present
  ## E2E Test Setup — <target>

  ### Kind
  <web|cli>

  ### Infrastructure
  - [x] playwright.config.ts (port <port>)
  - [x] test/e2e/global-setup.ts + teardown
  - [x] test/e2e/sandbox.ts (isolated per-test DB)
  - [x] test/e2e/fixtures.ts (sandbox + user fixtures)

  ### Generated Journeys
  | Journey | File | Steps | Status |
  |---|---|---|---|
  | Workspace create + session | test/e2e/workspace.spec.ts | 4 | pass |

  ### Results
  - Journeys: N, passing: N, failing: N
  - Screenshots: test/e2e/screenshots/ + playwright-report/

  ### Issues Found
  | Journey | Issue | Fix |

EMIT [test-e2e] completed target=<pkg> kind=<kind> journeys=<N> passing=<N>

CHECKLIST
  - [ ] Kind + layer (L4 CLI / L5 Nitro / L5 spawn-server / L5 Playwright) correctly determined
  - [ ] Port unique per forge app (see allocation table below)
  - [ ] L5 Playwright: globalSetup + globalTeardown in place
  - [ ] L5 Playwright: per-test sandbox isolates DB / workspace
  - [ ] L5 Playwright: webServer configured in playwright.config.ts
  - [ ] L4 CLI: tests at `tests/cli-e2e/<group>.test.ts`, 3-6 tests per group (help + happy + error)
  - [ ] L5 Nitro integration: `tests/_setup/{h3-event.ts,nitro-globals.ts}` scaffolded; correct cookie-name per app
  - [ ] L5 spawn-server: gated behind `FORGE_SERVER_TESTS=1`
  - [ ] Test file suffix: `.test.ts` for Vitest-based (L4/Nitro/spawn); `.spec.ts` for Playwright
  - [ ] Screenshots configured on failure (Playwright only)
  - [ ] `@skaile/test-utils` in devDependencies if the package gained L4 or Nitro integration tests
  - [ ] Generated tests pass or source bugs reported

---

## Integration

- **Called by:** `test-plan` (next step), `implement` (after new user-facing feature), `quality`
- **Reads:** `<target>/CLAUDE.md`, `<target>/TEST_PLAN.md`, page/route source, `test_stack_map.md`, `_devlog/specs/2026-04-22-test-concept-design.md`, `_devlog/plans/2026-04-22-test-gap-fill.md`, `agent-framework/test-utils/src/index.ts`
- **Writes:**
  - L4 CLI: `<target>/tests/cli-e2e/setup.ts`, `<target>/tests/cli-e2e/<group>.test.ts`, `<target>/package.json` (devDep on `@skaile/test-utils`)
  - L5 Nitro integration: `<target>/tests/_setup/h3-event.ts`, `<target>/tests/_setup/nitro-globals.ts`, `<target>/tests/api-<route>.test.ts`, `<target>/vitest.config.ts`
  - L5 spawn-server: `<target>/tests/_setup/spawn-server.ts`, `<target>/tests/api-server-harness.test.ts`
  - L5 Playwright: `<target>/playwright.config.ts`, `<target>/tests/e2e/**` (or `test/e2e/**` for forge/concept)

## Canonical References

| Layer | Pattern | File |
|---|---|---|
| L4 | CLI spawn-harness | `agent-framework/cli/tests/cli-e2e/setup.ts`, `agent-framework/cli/tests/cli-e2e/help.test.ts`, `config.test.ts`, `flow.test.ts` |
| L5 | Nitro integration (synthetic h3 event) | `forge/project/tests/_setup/h3-event.ts`, `forge/project/tests/api-auth-me.test.ts` |
| L5 | Spawned-server harness | `forge/project/tests/_setup/spawn-server.ts`, `forge/project/tests/api-server-harness.test.ts` |
| L5 | Playwright web E2E | `forge/project/tests/e2e/*.spec.ts`, `forge/assistant/tests/e2e/*.spec.ts`, `forge/concept/test/e2e/*.spec.ts`, `platform/e2e/` |

## Port Allocation for Forge Apps

Pick a stable port per forge app to avoid conflicts during parallel e2e runs:

| Package | Port |
|---|---|
| `forge/concept` | 3344 |
| `forge/project` | 3400 |
| `forge/assistant` | 3410 |
| `forge/chat` | 3420 |
| `forge/mattermost` | 3430 |
| `forge/tui` | 3440 |

When adding a new forge app, append to the table here and use the next free port.
