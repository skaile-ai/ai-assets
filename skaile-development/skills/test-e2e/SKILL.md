---
name: "test-e2e"
description: "Set up and generate end-to-end tests for forge apps, platform/frontend, and the agent-framework CLI. Scaffolds Playwright for web apps (forge + platform) including config, global setup/teardown, sandbox, and fixtures. For CLI packages, generates shell-based end-to-end tests that invoke the bin and assert stdout/artifacts. Verifies tests run against a real dev server or real process."
metadata:
  version: "1.0.0"
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
      - path: "<target>/src/commands"
      - path: "ai-assets/skaile-development/references/test_stack_map.md"
    produces:
      - path: "<target>/playwright.config.ts"
      - path: "<target>/test/e2e/*.spec.ts or tests/e2e/*.spec.ts"
      - path: "<target>/test/e2e/fixtures.ts"
      - path: "<target>/test/e2e/sandbox.ts"
      - path: "<target>/test/e2e/global-setup.ts"
      - path: "<target>/test/e2e/global-teardown.ts"
      - path: "<target>/tests/cli-e2e/*.test.ts (for CLIs)"
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

Bootstraps and generates end-to-end tests for:

| Kind | Applies to | Framework | Pattern |
|---|---|---|---|
| **web** | `forge/project`, `forge/concept`, other forge apps, `platform/frontend` (via `platform/e2e`) | Playwright | User journeys via browser against a running dev server |
| **cli** | `agent-framework/cli` | Shell invocation through vitest | Spawn the bin, assert exit code, stdout, artifacts |

`platform/e2e/` already has a Playwright suite — for the platform, this skill *extends* it rather than scaffolding a new config. For forge apps, `forge/concept` already has a working Playwright setup that is the reference template; other forge apps inherit that shape.

The agent-framework itself has no dedicated e2e layer per package — most runner / bridge / session packages are covered by integration tests. However, `agent-framework/cli` gets a dedicated CLI-e2e suite that invokes the compiled bin.

## When to Use

- Adding Playwright coverage to a forge app that doesn't have it yet (`forge/project` has e2e but other forge apps may not)
- Adding journeys after a new page/route is implemented
- Adding CLI e2e tests for a new skaile command in `agent-framework/cli`
- After `test-plan` flags an e2e gap

## When NOT to Use

- For pure-logic tests — use `test-unit`
- For API + DB tests — use `test-integration`
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
  Web:
    <target>/playwright.config.ts                   — if missing
    <target>/test/e2e/global-setup.ts               — if missing
    <target>/test/e2e/global-teardown.ts            — if missing
    <target>/test/e2e/fixtures.ts                   — shared test fixtures
    <target>/test/e2e/sandbox.ts                    — isolated per-test workspace/data
    <target>/test/e2e/<journey>.spec.ts             — per-journey tests
    <target>/test/e2e/screenshots/                  — captured on failure
  CLI:
    <target>/tests/cli-e2e/setup.ts                 — bin locator, temp workspace helper
    <target>/tests/cli-e2e/<command>.test.ts        — per-command tests

MUST  read CLAUDE.md before scaffolding anything
MUST  reuse existing Playwright config (forge/concept, forge/project) as the reference template
MUST  scaffold isolated per-test sandbox — no leaking state between runs
MUST  start the dev server / build the CLI before tests run (via globalSetup or test fixtures)
MUST  screenshot every failing journey step automatically
MUST  verify generated tests execute before reporting
MUST  pick a dedicated port per forge app to avoid conflicts (forge/concept uses 3344; assign unique ports)
NEVER run against a production URL
NEVER hardcode absolute paths — always use the package's temp/sandbox helper
NEVER leave dev servers running — ensure globalTeardown kills all child processes

EMIT [test-e2e] started target=<pkg> mode=<mode> kind=<kind>

# ── Phase 1: Classify ────────────────────────────────────────────

STEP 1: Determine kind
  IF kind = auto:
    IF <target> is platform/frontend → delegate to platform/e2e (do NOT create a parallel config) — inform user
    IF <target>/nuxt.config.ts exists OR <target>/vite.config.* with React → web
    IF <target> is agent-framework/cli OR package.json has `bin` entry → cli
    ELSE → report "this package is library-grade; use test-integration"

STEP 2: Load context
  - Read CLAUDE.md
  - Read TEST_PLAN.md if present — pick e2e scenarios
  - Read package.json: scripts (dev, start, build), bin
  - Identify dev server port (from nuxt.config.ts / vite.config.ts / env)

# ── Phase 2: Web E2E Setup ───────────────────────────────────────

STEP 3a (web, skip if mode=generate): Scaffold Playwright

  Reference: forge/concept/playwright.config.ts

  IF <target>/playwright.config.ts missing:
    WRITE <target>/playwright.config.ts
    ```typescript
    import { defineConfig } from '@playwright/test'

    const PORT = <pick unused port based on package, e.g. 3400 for forge/project, 3344 for forge/concept, etc.>

    export default defineConfig({
      testDir: './test/e2e',
      globalSetup: './test/e2e/global-setup.ts',
      globalTeardown: './test/e2e/global-teardown.ts',
      timeout: 60_000,
      retries: 0,
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

  IF <target>/test/e2e/global-setup.ts missing:
    WRITE it — builds DB, seeds `populated` fixture, writes a storage state file used for auth

  IF <target>/test/e2e/global-teardown.ts missing:
    WRITE it — stops any spawned side-processes

  IF <target>/test/e2e/sandbox.ts missing:
    WRITE it — `makeSandbox()` creates per-test temp data dir (SQLite file + uploads) and cleans up

  IF <target>/test/e2e/fixtures.ts missing:
    WRITE it — extends Playwright `test` with a `sandbox` fixture, typed users, and helper actions

  Add to package.json scripts if missing:
    "test:e2e": "playwright test"
    "test:e2e:ui": "playwright test --ui"

  Ensure `@playwright/test` is in devDependencies. If missing → add and run `bun install`.

  Add `.gitignore` entries if missing: `playwright-report/`, `test-results/`, `test/e2e/screenshots/`

# ── Phase 3: CLI E2E Setup ───────────────────────────────────────

STEP 3b (cli, skip if mode=generate): Scaffold CLI harness

  IF <target>/tests/cli-e2e/ missing:
    CREATE the dir
    WRITE <target>/tests/cli-e2e/setup.ts
    ```typescript
    import { execFileSync, spawnSync } from 'node:child_process'
    import { mkdtempSync, rmSync } from 'node:fs'
    import { tmpdir } from 'node:os'
    import { join, resolve } from 'node:path'

    const BIN = resolve(__dirname, '../../dist/bin.js')

    export function runCli(args: string[], opts: { cwd?: string; env?: Record<string, string> } = {}) {
      return spawnSync('bun', [BIN, ...args], {
        cwd: opts.cwd ?? process.cwd(),
        env: { ...process.env, ...opts.env },
        encoding: 'utf8',
      })
    }

    export function makeTempProject() {
      const dir = mkdtempSync(join(tmpdir(), 'skaile-cli-e2e-'))
      return { dir, cleanup: () => rmSync(dir, { recursive: true, force: true }) }
    }
    ```

  Ensure the package's build script produces the bin before tests run; add a pre-hook in the package's "test:e2e" script: `"test:e2e": "bun run build && bun x --bun vitest run tests/cli-e2e"`.

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

STEP 4b (cli, skip if mode=setup): Generate command tests

  Per public command (from CLAUDE.md Commands section or <target>/src/commands/):

  Template:
  ```typescript
  import { describe, it, expect, afterEach } from 'vitest'
  import { runCli, makeTempProject } from './setup'

  describe('skaile init', () => {
    let proj: ReturnType<typeof makeTempProject>
    afterEach(() => proj?.cleanup())

    it('creates a new project with expected files', () => {
      proj = makeTempProject()
      const res = runCli(['init', 'demo'], { cwd: proj.dir })
      expect(res.status).toBe(0)
      expect(res.stdout).toContain('Created')
      expect(fs.existsSync(join(proj.dir, 'demo/skaile.yaml'))).toBe(true)
    })

    it('fails with clear message when name missing', () => {
      proj = makeTempProject()
      const res = runCli(['init'], { cwd: proj.dir })
      expect(res.status).not.toBe(0)
      expect(res.stderr).toContain('name')
    })
  })
  ```

  Scenarios per command:
  - Happy path invocation
  - Missing required arg / bad flag
  - Exit-code check on success + failure
  - Artifact presence check (file created, log entry written)

# ── Phase 6: Verify ──────────────────────────────────────────────

STEP 5: Run the e2e suite
  Web: `bun x playwright test` inside <target>
  CLI: `bun run test:e2e` inside <target>

  Capture journey pass/fail + screenshots.
  IF dev server fails to start → report error (port conflict / missing env var)
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
  | Workspace create + session | test/e2e/workspace.spec.ts | 4 | ✓ |

  ### Results
  - Journeys: N, passing: N, failing: N
  - Screenshots: test/e2e/screenshots/ + playwright-report/

  ### Issues Found
  | Journey | Issue | Fix |

EMIT [test-e2e] completed target=<pkg> kind=<kind> journeys=<N> passing=<N>

CHECKLIST
  - [ ] Kind correctly determined
  - [ ] Port unique per forge app
  - [ ] globalSetup + globalTeardown in place
  - [ ] Per-test sandbox isolates DB / workspace
  - [ ] Dev server or built bin ready before tests run
  - [ ] Every journey has an error path + responsive check (web) / bad-args path (cli)
  - [ ] Screenshots configured on failure
  - [ ] Generated tests pass or source bugs reported

---

## Integration

- **Called by:** `test-plan` (next step), `implement` (after new user-facing feature), `quality`
- **Reads:** `<target>/CLAUDE.md`, `<target>/TEST_PLAN.md`, page/route source, `test_stack_map.md`
- **Writes:** `<target>/playwright.config.ts`, `<target>/test/e2e/**`, `<target>/tests/cli-e2e/**`

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
