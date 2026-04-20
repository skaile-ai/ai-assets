# Test Stack Map

Authoritative map of every package in the skaile-dev monorepo to its test framework, test-file location convention, and command to run.

Used by: `test`, `test-plan`, `test-unit`, `test-integration`, `test-e2e`, `audit`, `ready`, `quality`.

---

## Forge Apps (Nuxt 4 + drizzle-orm + SQLite)

| Package | Framework | Test Dir | Pattern | Run Command |
|---|---|---|---|---|
| `forge/project` | Vitest (unit) + Playwright (e2e) | `tests/` + `tests/e2e/` | `*.test.ts`, `*.spec.ts` | `bun x --bun vitest run --project forge-project` |
| `forge/concept` | Vitest (unit) + Playwright (e2e) | `test/unit/` + `test/e2e/` | `*.test.ts`, `*.spec.ts` | `bun x --bun vitest run --project forge-concept` |
| `forge/assistant` | Vitest | `test/` or `tests/` | `*.test.ts` | `bun x --bun vitest run --project forge-assistant` |
| `forge/chat` | Vitest | `test/` or `tests/` | `*.test.ts` | `bun x --bun vitest run --project forge-chat` |
| `forge/mattermost` | Vitest | `test/` or `tests/` | `*.test.ts` | `bun x --bun vitest run --project forge-mattermost` |
| `forge/tui` | Vitest | `test/` or `tests/` | `*.test.ts` | `bun x --bun vitest run --project forge-tui` |
| `forge/common-backend` | Vitest | `test/` | `*.test.ts` | `bun x --bun vitest run --project forge-common-be` |
| `forge/common-ui` | Vitest | `test/` | `*.test.ts` | `bun x --bun vitest run --project forge-common-ui` |
| `forge/common-tui` | Vitest | `test/` | `*.test.ts` | `bun x --bun vitest run --project forge-common-tui` |

Forge E2E runs Playwright against `bun run dev` on a dedicated port. Fixtures in `test/e2e/fixtures.ts`, sandbox in `test/e2e/sandbox.ts`. Use `bun x playwright test` inside the package.

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
| `agent-framework/workspace` | Vitest | `tests/` | `*.test.ts` | `bun x --bun vitest run --project agent-workspace` |
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

## Database Isolation

- **Forge**: drizzle-orm + SQLite. Use `:memory:` or a per-test temp file via `fs.mkdtempSync()`. Migrations run via drizzle-kit in `beforeAll`.
- **Platform backend**: PostgreSQL. Use a separate test DB (`DATABASE_URL_TEST`) with `prisma migrate deploy` in `beforeAll`. Truncate tables in `afterEach`.
- **Agent framework**: No DB. State lives in temp dirs (`fs.mkdtempSync(os.tmpdir())`), cleaned in `afterEach`.
