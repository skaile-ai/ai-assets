# PostXL for Agents

A condensed reference about the PostXL (PXL) framework, written for AI agents that need to evaluate whether a proposed app idea fits PostXL, and reason about how to build it.

Companion docs in this folder:

- `skaile-agent-framework.md` -- runtime stack for AI agents (drivers, sessions, mounts, connectors, flows)
- `skaile-ecosystem.md` -- the whole skaile-dev monorepo (forge apps, ai-assets, platform)
- `skaile-platform.md` -- the Skaile enterprise platform (a PostXL-generated app extended with the agent framework)

The Skaile platform is the canonical real-world PostXL output. When evaluating "should this app be built with PostXL?", `skaile-platform.md` shows how far a PostXL-generated codebase can be pushed.

---

## 1. What PostXL Is

PostXL is a **schema-driven code generator** that emits a complete, opinionated full-stack TypeScript application from a single declarative `postxl-schema.json` file (plus optional split `schema/*.model.json` and `schema/*.enum.json` files).

It is **not** a runtime framework or library you import at runtime. It is a build-time generator: you describe data models, enums, actions, and authorization in JSON; PostXL writes the NestJS backend, the React frontend, the Prisma schema, the Playwright E2E tests, and the DevOps scaffolding for you.

The generated app is a normal TypeScript monorepo - you can run, deploy, and extend it without PostXL on the runtime path.

### Mental model

- Input: `postxl-schema.json` (+ optional split files) and a `generate.ts` that selects which generators to run.
- Output: `backend/`, `frontend/`, `e2e/`, `migrations/`, Docker/CI files, env examples.
- Re-running the generator is idempotent. Files are tracked in `postxl-lock.json`. Manual edits inside `// @custom-start` / `// @custom-end` markers are preserved across regeneration; edits outside markers eject the file (you own it from then on).

---

## 2. Generated Stack (Fixed Choices)

These are not configurable - they are baked into the generators.

| Layer            | Technology                                                                 |
| ---------------- | -------------------------------------------------------------------------- |
| Language         | TypeScript (Node.js 24+)                                                   |
| Backend runtime  | NestJS on Fastify                                                          |
| API style        | tRPC (primary), optional REST                                              |
| ORM / DB         | Prisma, PostgreSQL (multi-schema supported, e.g. `PXL`, `Data`)            |
| Auth             | Keycloak (OIDC), JWT roles via configurable claim path                     |
| Frontend         | Vite + React 19                                                            |
| FE routing       | TanStack Router (file-based)                                               |
| FE data          | TanStack Query via tRPC client, TanStack Form, TanStack Table              |
| UI components    | Shared `@postxl/ui-components` package (Radix + Tailwind v4); ~60 prims    |
| Styling          | Tailwind CSS v4                                                            |
| Validation       | Zod (decoders) generated per model / enum / DU                             |
| Testing          | Jest (BE unit), Vitest (FE unit), Playwright (E2E), Storybook              |
| File storage     | S3 (optional)                                                              |
| Mail             | Pluggable adapters: `log`, `smtp`, `ses`; renderers: `simple`, `emailmd`   |
| AI               | OpenAI provider, agent + tools + cache services (optional)                 |
| Package manager  | pnpm 10.30+ (manages Node version too)                                     |
| Monorepo         | Turborepo (with optional remote caching)                                   |
| DevOps           | Docker Compose, Bitbucket Pipelines / Jenkinsfile, Dockerfiles             |

If the user wants a different ORM, framework, or UI library, PostXL is not a fit.

---

## 3. Schema: What You Can Express

Schema is parsed by `@postxl/schema` (Zod-based). Key elements:

### Project-level

- `name`, `slug`, `description`, `version`, `projectType` (`standalone` | `workspace`).
- `databaseSchemas` - separate Prisma schemas (default `PXL` for system tables, `Data` for domain). Models / enums opt into a schema.
- `defaultRoles` - default `['superadmin', 'admin', 'editor', 'viewer']`.
- `auth` (project-level) - provider (`keycloak`), `roleClaimPath`, `defaultDeny`, named action `scopes`.
- `schemaAuth` - per-schema `read` / `write` / `create` / `update` / `delete` / `actions` / `adminUi` rules using `anyRole: [...]`.
- `systemUser` - identity used for seed/system writes.
- `standardModels` - opt-in built-ins: `Action`, `ActionOperation`, `User`, `File`, `TableView`, `Comment`, `Config`. These provide audit, file uploads, saved table views, comments, KV config out of the box.
- `standardEnums` - `ActionStatus`, `MutationStatus`, `MutationKind`.

### Models

Each model declares:

- `schema`, `databaseName`, `excelName`, `description`, `isReadonly`.
- `fields` - typed:
  - **Id** - always present, configurable generation.
  - **Scalar** - `String`, `Int`, `Float`, `Boolean`, `DateTime` (with `?` for optional). Validations: `maxLength`, `min`, `max`, `int`/`float`. Auto-fields: `isCreatedAt`, `isUpdatedAt`. `isUnique`, `hasIndex`, `defaultValue`, `placeholder`.
  - **Json** - opaque `unknown`-typed JSON blob.
  - **Relation** - typed by referencing another model name (e.g. `"type": "Post"` or `"Post?"`).
  - **Enum** - typed by referencing an enum name.
  - **DiscriminatedUnion** - inline tagged union with `commonFields` and an array of `members` (each `type` is the discriminator). DU sub-fields get prefixed `excelName` / `databaseName` for flat I/O.
- `standardFields` - opt into common fields like `id`, `createdAt`, `updatedAt`.
- `labelField`, `keyField`, `defaultSort`, `indexes`.
- `actions` - named custom actions (e.g. `publish`, `unpublish`) on top of standard CRUD.
- `auth` - per-model `read`/`write`/`create`/`update`/`delete`/per-action role rules and `adminUi.visibleFor`.
- `repository.type` - `DatabaseDirect`, `DatabaseCached`, `InMemory`, or `NoRepository`.
- `seed` - inline seed rows.
- `faker` - `seed` + `items` count for generated mock data; per-field `faker` expressions like `lorem.slug`, `internet.email`.

### Enums

Named enum with values, optional schema, `excelName`. Decoders are generated for type-safe parsing across BE, FE, and Excel I/O.

---

## 4. What Gets Generated

### Backend (`backend/libs/*`)

- `repositories/` - one repository per model. CRUD (create, update, upsert, delete, clone, plus `*Many`), get/getAll/getAllAsArray, filter, findFirst, count, relation lookups (`getIdsFor{Rel}`, `getItemsFor{Rel}`), `init`, `reset`. Repository variant chosen per model: in-memory map, Prisma direct, or Prisma + cache.
- `view/` - per-model `ViewService` with filter/sort/pagination and an `authorize(...)` choke point (for read-side permissions, including row-level if you customize).
- `actions/` - dispatcher pattern. Every mutation goes through a central `Dispatcher` which calls `AuthorizationService.authorizeAction(...)` + `AuthorizationPolicyService` before executing the action. Provides standard CRUD actions and any custom actions declared in the schema.
- `update/` - per-model update services that decode payloads (Zod) and call repositories.
- `router-trpc/` - per-model tRPC routes: `getFilteredPaginated`, `get`, CRUD mutations, custom action endpoints. Auth middleware injects `viewer` and `user`.
- `restApi/` (optional) - REST mirror of tRPC routes.
- `decoders/` - Zod codecs per model / enum / DU, with `flattened` and `excel` variants for tabular I/O.
- `types/` - shared types (DTOs, ViewModels) used by BE and exported to FE.
- `authentication/` - Keycloak integration, JWT decoding, role extraction, NestJS guards.
- `seed/` + `seedData/` - applies inline seed rows + generated mock data on stateful boot.
- `mock-data/` - faker-driven dataset.
- `import/` + `excel-io/` + `xlport/` - Excel template generation, upload, parse, validate, dry-run review, and bulk import. One workbook per model and a combined data-model workbook.
- `data-management/` - data wipe / reseed endpoints for dev environments.
- `s3/` + `upload/` - file upload pipeline (when S3 is enabled).
- `mail/` - configurable adapter (log/smtp/ses) and renderer (simple/emailmd).
- `ai/` - optional. Generates `AiModule`, `AiAgentService`, `AiToolsService`, `AiCacheService`, OpenAI provider; can be wired to model actions.

### Frontend (`frontend/src/*`)

- `routes/` - TanStack Router file-based routes, including auth guards.
- `pages/admin/` - one admin page per model, plus `data-management`, `excel-io`, `import-review`. Sidebar grouped by `databaseSchema`. Visibility honors `auth.adminUi.visibleFor`.
- `pages/dashboard/` - placeholder dashboard.
- `pages/login/`, `pages/unauthorized/`, `pages/error/` - auth UX.
- `components/`, `hooks/` - per-model tRPC hooks (`useX`, `useXs`, mutations) and admin tables / forms.
- Tables built on `DataGrid` (sorting, filtering, inline edit, cell selection, saved views via the `TableView` standard model).
- Forms built on TanStack Form, automatically generated for each model and DU.
- `ui/` is **deprecated** - all primitives come from `@postxl/ui-components`.
- Optional integrations: `charts`, `spreadsheet`, `applicationHeader`.

### E2E (`e2e/`)

- Playwright tests generated per model (CRUD happy paths, auth flows, import flows). Two run modes: `stateless` (in-memory backend) and `stateful` (real DB).

### Infra / DevOps

- `docker-compose.yml` (Postgres, Keycloak, optional S3/minio).
- Per-app Dockerfiles + build scripts.
- Bitbucket Pipelines or Jenkinsfile.
- `.env.example` files (the generator never writes real `.env` to avoid clobbering secrets).

---

## 5. Authorization Model

PostXL has standardized role-based authorization with hooks at every choke point:

- Roles default to `superadmin | admin | editor | viewer` (overridable via `defaultRoles`).
- Project-level `auth.scopes` for non-model action scopes (e.g. `import`).
- Schema-level defaults via `schemaAuth` (e.g. all `Data` models readable by `editor`+).
- Per-model overrides for read/write/create/update/delete/actions.
- Per-action role rules (e.g. `publish` requires `admin`+).
- `adminUi.visibleFor` controls Admin UI visibility per schema/model.
- `defaultDeny: true` denies access when no rule matches.

Backend enforces via `ViewService.authorize(...)` (read) and `AuthorizationService.authorizeAction(...)` / `AuthorizationPolicyService` (write). Frontend uses the same role data for menu/route visibility but BE remains the source of truth. Row-level rules require custom code (extend the generated authorize hooks via `// @custom-*` blocks).

---

## 6. Customization & Extensibility

- **Custom blocks** - `// @custom-start[:name]` / `// @custom-end[:name]` blocks survive regeneration and are repositioned around their anchor lines.
- **Eject** - editing a generated file outside custom blocks marks it ejected; you maintain it manually. Use `pnpm run generate -i` to skip ejected files.
- **Schema split** - large schemas can be split into `schema/<modelName>.model.json` and `schema/<enumName>.enum.json`.
- **Selective regen** - `pnpm run generate -m Country City` (per-model), `pnpm run generate -f -p 'glob'` (force regen by path).
- **Custom generators** - `generate.ts` selects which generators run; you can write your own `GeneratorInterface` and add it to the array. Generators consume the parsed schema + a context object built up by other generators.
- **Project-specific code** - lives outside generated paths (`src/components/`, custom routes, custom NestJS modules) and is left alone.

---

## 7. Operational Modes

The generated backend supports multiple modes via env flags / scripts:

- `dev` - stateless (in-memory repositories), no auth. Fastest startup. Good for FE/UI iteration.
- `dev:auth` - stateless + Keycloak.
- `dev:stateful` - Prisma + Postgres, no auth.
- `dev:stateful:auth` - full prod-like.
- `e2e:stateless` / `e2e:stateful` - dedicated modes for Playwright runs.

This dual stateless/stateful posture is a core PostXL idea: same code, swap the repository layer.

---

## 8. Strengths (When PostXL Fits Well)

- **Internal/admin-heavy CRUD apps** with many models, complex relations, role-based access, and a need for an admin UI, audit trail, file uploads, comments, and Excel import/export. PostXL gives you 80%+ of this for free from a JSON schema.
- **B2B SaaS backoffice / data-management tools** (think CMS, configuration consoles, ops dashboards, portfolio managers, program-management tools).
- **Projects with evolving schemas** - changing a model means editing JSON and regenerating, not refactoring twenty files.
- **Teams that want a consistent stack across multiple projects** - all projects share the same NestJS/tRPC/React/Tailwind/Keycloak conventions; one engineer can work across them.
- **Excel/CSV-heavy workflows** - the import/export pipeline (templates, dry-run review, error rows, type-safe decoders) is non-trivial and hard to build from scratch.
- **Multi-schema PG setups** - cleanly separates system (`PXL`) and domain (`Data`) tables.
- **Apps that need both UI and API** - tRPC + REST come for free.

## 9. Weaknesses (When PostXL Is the Wrong Tool)

- **Stack lock-in** - NestJS/Fastify/Prisma/Postgres/React/Tailwind/Keycloak. Want Next.js, Remix, Hono, Drizzle, MySQL, MongoDB, Auth0, Material UI, Vue, mobile? Not without rewriting generators.
- **Consumer / public-facing apps** - the generated UI is admin-shaped (sidebars, tables, forms). Marketing sites, social apps, content-heavy public pages need a different starting point.
- **Real-time / streaming workloads** - no first-class WebSocket, SSE, or pub/sub generators. You can add them as custom modules but you lose generator leverage.
- **Highly custom UX** - if every screen is bespoke, you fight the generated admin layout more than you benefit.
- **Non-relational data** - generators assume Prisma + relational modeling. Document, graph, or time-series stores need custom repositories.
- **Tiny apps** - the generator + monorepo overhead is heavy for a single-page tool or microservice.
- **Mature codebases that need to *adopt* PostXL** - it is a green-field generator, not a refactor tool. Eject patterns help but adoption is one-way (you can't easily "un-PostXL" a project later if you've heavily customized).
- **Frontend perf-critical apps** - Vite + React 19 is fine, but the generated admin assumes desktop, query-heavy UIs.
- **Compliance-heavy auth** - role-based via Keycloak is the assumption. ABAC, fine-grained per-row policies, multi-tenant isolation across orgs need significant custom code on top of the existing hooks.

## 10. Decision Rubric

Score the user's idea against these. The more "yes"es, the better the fit.

1. Is it primarily CRUD over a relational data model with >5 entities and relationships?
2. Does it need an admin UI for staff/internal users (as opposed to or in addition to an end-user UI)?
3. Does it need role-based access control with a small set of roles?
4. Does it need Excel import/export, audit trail, file uploads, or comments?
5. Is the team OK with NestJS + tRPC + React + Tailwind + Postgres + Keycloak?
6. Will the schema evolve and benefit from regeneration?
7. Is the deployment self-hosted or on infra that can run Docker + Postgres + Keycloak?

If most answers are yes -> PostXL is likely a strong starting point.
If most are no -> recommend a different stack (Next.js + Drizzle + better-auth, or a SaaS like Retool, depending on the gap).

---

## 11. How to Build a New App with PostXL (Workflow)

1. **Define the schema** - draft `postxl-schema.json`: models, enums, relations, roles, schema split (PXL vs Data), standard models (always include `User`, `Action`, `ActionOperation`).
2. **Configure `generate.ts`** - pick which generators to run. Defaults from `projects/demo/generate.ts` are a good starting point. Tweak: `configureBackendMailGenerator({ adapters, renderers })`, `configureDevopsGenerator({ useDatabase, keycloakComponents, useS3, useBitbucket })`, optional `backendAiGenerator`, optional REST API.
3. **Generate** - `pnpm run generate`.
4. **Bring up infra** - `docker compose up -d db keycloak`, `pnpm prisma migrate dev`.
5. **Iterate** - run BE + FE in dev mode (stateless first, then stateful), use Storybook for component work, Playwright for E2E.
6. **Customize** - add domain logic via `// @custom-*` blocks in generated services, eject FE pages when needed, write custom NestJS modules outside `libs/` for project-specific work.
7. **Backport** - if a customization is reusable, backport it to a generator under `packages/generators/src/*` (see `docs/generators.md`).

---

## 12. Important Caveats for Agents

- The generator is opinionated. When a user asks "can PostXL do X?", first check if X aligns with the fixed stack (section 2) and the generated layers (section 4). If X requires a different runtime tech, the answer is "no, not without writing a custom generator or replacing the stack".
- Custom logic *can* live in any generated file via custom blocks, but the user must be willing to maintain those blocks (anchors can shift if the generator output changes).
- Do not promise features that PostXL does not have. Notable absences: real-time, GraphQL, mobile, server-side rendering, microservices/event bus, search engines (no Elastic/Meilisearch generator), background workers/queues (no built-in BullMQ), feature flags, A/B testing, payments, i18n.
- Treat `projects/demo` as the canonical reference for what a generated project looks like. If unsure, check what is actually emitted there before claiming a capability exists.
