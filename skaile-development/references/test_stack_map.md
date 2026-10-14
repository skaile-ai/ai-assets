# Test Stack Map

Quick map of each skaile repo to its test framework, test-file location convention, and command to run.

Every repo is standalone (the `skaile-dev` shell is archived). Run each command from **that repo's
root** unless the row names a subdirectory. The target repo's own `CLAUDE.md` is the authority:
when this map and that file disagree, the repo's `CLAUDE.md` wins — fix this map.

Used by: `test`, `test-plan`, `test-unit`, `test-integration`, `test-e2e`, `audit`, `ready`, `quality`.

---

## Forge Apps (Nuxt 4 + drizzle-orm + SQLite)

All forge packages except `forge/skaile-forge` and `forge/forge-common` share a single root `vitest.config.ts` at `forge/` that picks up
`**/tests/**/*.test.ts`. Run the whole suite with `bun x --bun vitest run` from `forge/`.
`forge/skaile-forge` is a standalone repo (`skaile-ai/skaile-forge`) with its own `vitest.config.ts`
and `playwright.config.ts`; run `bun run test` / `bun run test:e2e` inside `forge/skaile-forge`. Its Vitest
config picks up both `test/unit/**/*.test.ts` (unit-level, incl. Nitro route tests with a synthetic h3 event)
and `tests/integration/**/*.test.ts` (cross-module integration, e.g. `framework-contract.test.ts`).
`forge/forge-common` is a standalone repo (`skaile-ai/forge-common`) with its own `vitest.config.ts`;
run `cd forge/forge-common && bun x --bun vitest run`.

| Package | Framework | Test Dir | Pattern | Status |
|---|---|---|---|---|
| `forge/skaile-forge` | Vitest (unit + integration) + Playwright (e2e) | unit `test/unit/` + integration `tests/integration/` + e2e `tests/e2e/` | `*.test.ts`, `*.spec.ts` | unit ✓, integration ✓, e2e ✓ |
| `forge/forge-common` | Vitest | `tests/` + `tests/ui/` (happy-dom composables) | `*.test.ts` | unit ✓ |
| `forge/L5-concept` | Vitest (unit) + Playwright (e2e) | `test/unit/` + `test/e2e/` | `*.test.ts`, `*.spec.ts` | unit ✓, e2e ✓ |
| `forge/L4-assistant` | Playwright (e2e only) | `tests/e2e/` | `*.spec.ts` | e2e ✓ |
| `forge/L1-chat` | Vitest | `tests/` | `*.test.ts` | unit ✓ |
| `forge/L3-mattermost` | Vitest | `tests/` | `*.test.ts` | unit ✓, integration ✓ |
| `forge/L2-tui` | Vitest | `tests/` | `*.test.ts` | unit ✓ |
| `forge/common-backend` | Vitest | `tests/` | `*.test.ts` | unit ✓ |
| `forge/common-ui` | Vitest + Playwright CT | `tests/` + `tests/e2e/` | `*.test.ts`, `*.spec.ts` | unit ✓, e2e ✓ (Playwright CT) |
| `forge/common-tui` | Vitest | `tests/` | `*.test.ts` | unit ✓ |

Forge E2E specs live in `tests/e2e/` (except `forge/L5-concept`: `test/e2e/`). In `forge/skaile-forge`, `tests/e2e/_setup/global-setup.ts` builds the app once and every Playwright worker boots the built output with its own `FORGE_HOME`. Use `bun run test:e2e` (or `bun x playwright test`) inside the package.

For `forge/common-ui`, E2E uses Playwright CT (component testing) instead of a full Nuxt dev server — see the Playwright Component Testing section below.

## Workspaces (`skaile-ai/workspaces` — TypeScript + Bun, Biome)

One Bun workspace; `packages/*` are the members. The runtime stack is the single package
`packages/workspaces/`, with one folder per former package (`runner/`, `bridge/`, `cli/`, `core/`, …).

| Target | Framework | Test Dir | Pattern | Run Command (from the repo root) |
|---|---|---|---|---|
| every package | Vitest | `tests/` per folder (some colocated in `src/`) | `*.test.ts` | `bun run test` |
| `packages/workspaces` only | Vitest | `<folder>/tests/` | `*.test.ts` | `cd packages/workspaces && bun run test` |
| one folder or file | Vitest | — | — | `cd packages/workspaces && bun x --bun vitest run runner/tests/<file>.test.ts` |

Format and lint with Biome: `bun run format`, `bun run lint`. Typecheck: `bun run typecheck`.
CI fails when a package collects fewer tests than its floor in `scripts/test-count-floor.json`.

## Platform (`skaile-ai/platform` — PostXL: NestJS + React, Prettier + ESLint)

Install with `bun install` at the repo root. Never run Biome in this repo.

| Package | Framework | Test Dir | Pattern | Run Command (from the repo root) |
|---|---|---|---|---|
| `backend` | Jest | colocated in `libs/*/src`, `apps/*/src`, `test/` | `*.test.ts` | `cd backend && bun run test:jest -- --testPathPatterns <pattern>` |
| `frontend` | Vitest | colocated in `src/` | `*.test.ts`, `*.test.tsx` | `cd frontend && bun run test:unit` |
| `e2e` | Playwright | `specs/` | `*.spec.ts` | `cd e2e && bun run e2e:local` (see the `e2e-platform` skill) |

Typecheck both packages: `bun run typecheck` (root). Lint: `bun run lint` (root) or inside
`backend/` / `frontend/`. A backend change is verified only when the app boots
(`cd backend && bun run dev` reaches `API is running`) — see the platform `CLAUDE.md`.

## Other repos

| Repo | Formatter / linter | Test command (from the repo root) |
|---|---|---|
| `skaile-ai/ai-assets` | — (prose + shell scripts) | `bash scripts/run-tests.sh` |
| `skaile-ai/store` | Prettier + ESLint (PostXL) | `bun run test`; lint `bun run lint` |
| `skaile-ai/brand` | — | `bun run test`; typecheck `bun run typecheck` |

## Test Infrastructure Conventions

| Layer | Forge apps | Workspaces | Platform backend | Platform frontend |
|---|---|---|---|---|
| Unit | Vitest, `test/unit/` or `tests/` | Vitest, `tests/` | Jest, colocated `*.test.ts` | Vitest, colocated `*.test.ts(x)` |
| Integration | Vitest w/ real SQLite temp DB | Vitest w/ in-memory fixtures | Jest w/ PostgreSQL test container | — |
| E2E | Playwright, `tests/e2e/` (L5-concept: `test/e2e/`) | — (N/A — library) | — (delegated to `e2e/`) | Playwright, via the platform repo's `e2e/` |

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
- **Workspaces**: No DB. State lives in temp dirs (`fs.mkdtempSync(os.tmpdir())`), cleaned in `afterEach`.
