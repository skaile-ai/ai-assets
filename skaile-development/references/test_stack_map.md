# Test Stack Map

Authoritative map of every package in the skaile-dev monorepo to its test framework, test-file location convention, and command to run.

Used by: `skaile-dev-test`, `skaile-dev-test-plan`, `skaile-dev-test-unit`, `skaile-dev-test-integration`, `skaile-dev-test-e2e`, `skaile-dev-code-audit`, `skaile-dev-release-check`, `skaile-dev-quality-gate`.

---

## Forge Apps (Nuxt 4 + drizzle-orm + SQLite)

All forge packages share a single root `vitest.config.ts` at `forge/` that picks up
`**/tests/**/*.test.ts`. Run the whole suite with `bun x --bun vitest run` from `forge/`.

| Package | Framework | Test Dir | Pattern | Status |
|---|---|---|---|---|
| `forge/L4-project` | Vitest (unit) + Playwright (e2e) | `tests/` + `tests/e2e/` | `*.test.ts`, `*.spec.ts` | unit ✓, e2e ✓ |
| `forge/L5-concept` | Vitest (unit) + Playwright (e2e) | `test/unit/` + `test/e2e/` | `*.test.ts`, `*.spec.ts` | unit ✓, e2e ✓ |
| `forge/L4-assistant` | Playwright (e2e only) | `tests/e2e/` | `*.spec.ts` | e2e ✓ |
| `forge/L1-chat` | Vitest | `tests/` | `*.test.ts` | unit ✓ |
| `forge/L3-mattermost` | Vitest | `tests/` | `*.test.ts` | unit ✓, integration ✓ |
| `forge/L2-tui` | Vitest | `tests/` | `*.test.ts` | unit ✓ |
| `forge/common-backend` | Vitest | `tests/` | `*.test.ts` | unit ✓ |
| `forge/common-ui` | Vitest + Playwright CT | `tests/` + `tests/e2e/` | `*.test.ts`, `*.spec.ts` | unit ✓, e2e ✓ (Playwright CT) |
| `forge/common-tui` | Vitest | `tests/` | `*.test.ts` | unit ✓ |

Forge E2E runs Playwright against `bun run dev` on a dedicated port. Fixtures in `test/e2e/fixtures.ts`, sandbox in `test/e2e/sandbox.ts`. Use `bun x playwright test` inside the package.

For `forge/common-ui`, E2E uses Playwright CT (component testing) instead of a full Nuxt dev server — see the Playwright Component Testing section below.

## Agent Framework (TypeScript + Bun + OMP)

| Package | Framework | Test Dir | Pattern | Run Command |
|---|---|---|---|---|
| `agent-framework/cli` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-cli` |
| `agent-framework/runner` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-runner` |
| `agent-framework/bridge` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-bridge` |
| `agent-framework/flow-engine` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-flow-engine` |
| `agent-framework/resolver` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-resolver` |
| `agent-framework/connectors` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-connectors` |
| `agent-framework/session` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-session` |
| `agent-framework/store` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-store` |
| `agent-framework/asset-manager` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-asset-manager` |
| `agent-framework/core` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-core` |
| `agent-framework/types` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-types` |
| `agent-framework/transport` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-transport` |
| `agent-framework/workspace-plugin` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-workspace-plugin` |
| `agent-framework/lab` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-lab` |
| `agent-framework/sdk` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-sdk` |
| `agent-framework/client` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-client` |

## Platform

| Package | Framework | Test Dir | Pattern | Run Command |
|---|---|---|---|---|
| `platform/backend` | Jest | `libs/*/test`, `apps/*/test` | `*.spec.ts` | `bun run test --filter ./platform/backend` |
| `platform/frontend` | Vitest | `src/**/__tests__` | `*.test.tsx` | `bun run test --filter ./platform/frontend` |
| `platform/e2e` | Playwright | `tests/` | `*.spec.ts` | `bun run e2e --filter ./platform/e2e` |

## Whole Monorepo

| Target | Command |
|---|---|
| All Vitest projects | `bun x --bun vitest run` |
| Every package (incl. platform Jest) | `bun run test` (root script) |
| Coverage | `bun x --bun vitest run --coverage` |

## Test Infrastructure Conventions

| Layer | Forge apps | Agent framework | Platform backend | Platform frontend |
|---|---|---|---|---|
| Unit | Vitest, `test/unit/` or `tests/` | Vitest, `tests/` | Jest, colocated `*.spec.ts` | Vitest, `__tests__/` |
| Integration | Vitest w/ real SQLite temp DB | Vitest w/ in-memory fixtures | Jest w/ PostgreSQL test container | — |
| E2E | Playwright, `test/e2e/` | — (N/A — library) | — (delegated to platform/e2e) | Playwright, via `platform/e2e/` |

## Playwright Component Testing (forge/common-ui)

`forge/common-ui` uses `@playwright/experimental-ct-vue` for composables that
require a real browser DOM (TipTap/ProseMirror). CT mode spins up a Vite server,
mounts a `.vue` fixture component in Chromium, and tears down cleanly — no Nuxt
app needed.

Config: `forge/common-ui/playwright-ct.config.ts` (port 3101)
Run:    `cd forge/common-ui && bun run test:e2e`
UI:     `cd forge/common-ui && bun run test:e2e:ui`
Record: `cd forge/common-ui && bun run test:e2e:record -- http://localhost:3101`

The config includes a `resolveVueCompilerDom()` Vite plugin that forces Rollup
to inline `@vue/compiler-dom`. This is required under Bun workspace hoisting
because the CT framework's virtual-module context cannot resolve bare specifiers
via normal node_modules traversal.

Collaboration tests (Hocuspocus) are excluded — they require a live WebSocket
server. Document with skip comments in `tests/e2e/useSkaileEditor.spec.ts`.

## Database Isolation

- **Forge**: drizzle-orm + SQLite. Use `:memory:` or a per-test temp file via `fs.mkdtempSync()`. Migrations run via drizzle-kit in `beforeAll`.
- **Platform backend**: PostgreSQL. Use a separate test DB (`DATABASE_URL_TEST`) with `prisma migrate deploy` in `beforeAll`. Truncate tables in `afterEach`.
- **Agent framework**: No DB. State lives in temp dirs (`fs.mkdtempSync(os.tmpdir())`), cleaned in `afterEach`.
