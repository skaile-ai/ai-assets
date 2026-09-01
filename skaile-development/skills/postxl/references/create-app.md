# Creating a New PostXL App

Portable reference — this is the greenfield path, for a repo that has **no**
`postxl-schema.json` yet. For an existing generated app, use `maintain.md` instead.

## Prerequisites

| Requirement | Why | Check |
|---|---|---|
| **Node** | `pxl` is a Node CLI | `node --version` |
| **pnpm** | `create-project` shells out to `pnpm install` / `pnpm exec` / `pnpm run`. It is not optional and there is no bun fallback. | `pnpm --version` |
| Network to `registry.npmjs.org` | all `@postxl/*` packages are published public — no token, no `.npmrc` | `npm view @postxl/cli version` |

**If `pnpm` is missing, the run does not fail — it succeeds and lies.** Every subprocess
step (`installDependencies`, `runGenerate`, `generateTanStackRouter`, `generatePrismaClient`,
`initializeGitRepository`) is `try` / `catch` → log and continue. The run reaches
`✓ Project "X" created successfully`, prints "Next steps", and **exits 0**, leaving an
unusable tree with errors scrolled off above.

So the exit code is worth nothing here. **The verification is the backend boot line**, not a
successful scaffold.

### Installing pnpm in a sandboxed container

Agent containers routinely break the obvious install, and each breaks it differently. Check
before you start, not mid-scaffold — the failure above means you will not be told.

- **Verify node and pnpm separately.** An image may ship one without the other: a bun-only
  image has neither, and a node image usually has no pnpm.
- **A read-only runtime store blocks the global install.** Where the toolchain is mounted
  read-only — a nix store, an immutable image layer — both `npm i -g pnpm` and
  `corepack enable` fail, because they write into the node prefix inside it. Use a user
  prefix (`npm config set prefix "$HOME/.local" && npm i -g pnpm`), or
  `corepack pnpm@<version> …`, which downloads into a writable cache and honours the
  `packageManager` field the generator writes into the app.
- **No node at all needs the standalone build.** A normal pnpm is a Node script with a
  `#!/usr/bin/env node` shebang, so it cannot run without node. The standalone distribution
  (`@pnpm/exe`, or the `get.pnpm.io` script) bundles its own.
- **Install where the workspace persists.** If `$HOME` is container-local rather than
  bind-mounted, a pnpm installed there vanishes when the container is replaced — including
  on a hibernate/wake cycle. Put it under the persisted workspace instead.
- **Assume non-root.** Agent containers typically run as an unprivileged user, so `apt` /
  `apk` is not available as a fallback.

Which of these applies is a property of the host, not of PostXL — check the host's own
runtime documentation for what its image ships and which paths persist.

## The command

```bash
pnpm dlx @postxl/cli create-project "My App" \
  --slug my-app \
  --project-path ./my-app \
  --schema ./my-schema.json
```

`new` is an alias for `create-project`.

| Flag | Meaning |
|---|---|
| `-s, --slug` | project slug (defaults to a slugified name) |
| `-p, --project-path` | where to write (defaults to `../<slug>`) |
| `-S, --schema` | an existing `postxl-schema.json` to use instead of the built-in simple schema |
| `--skip-git` | do not `git init` the generated project |
| `--skip-generate` | write files without generating; with `--schema`, the custom schema is written directly |
| `-l, --link-postxl` | link to a local PostXL monorepo (framework development only) |

Interactive on a TTY (prompts for anything missing). **When piped, every required value
must be passed as an argument or option** — so in an agent session, always pass them
explicitly.

## What the run does

Nine steps, and it is slow — two full dependency installs and two full generations:

1. Write initial files (schema, `generate.ts`, `tsconfig`, `package.json`, `.env`) — using
   the simple schema for the first pass **even when `--schema` is given**
2. `pnpm install`
3. First generation, no formatting (prettier/eslint not installed yet)
4. `pnpm install` again — for devDependencies the generated `package.json` added
5. Swap in the custom schema, if one was given
6. Second generation, with full formatting
7. `tsr generate` — TanStack Router route tree
8. `prisma generate`
9. `git init` unless `--skip-git`

Expect a multi-minute run and roughly **1.3 GB** on disk once `node_modules` exists. Budget
session disk accordingly.

## What you get

```
my-app/
├── backend/            NestJS + Fastify + Prisma + tRPC
├── frontend/           Vite + React 19 + TanStack Router
├── e2e/                Playwright
├── CLAUDE.md           ← the app's own agent guidance, generated
├── skaile.preview.json ← Skaile multi-app preview config, generated
├── docker-compose.yml
├── postxl-schema.json · postxl-lock.json · generate.ts
├── pnpm-workspace.yaml
└── Jenkinsfile · turbo.json · prettier.config.js
```

**The generated project is a pnpm workspace.** Its root scripts are pnpm-only. Do not
substitute bun inside a generated app unless the project has been deliberately converted.

**Read the generated `CLAUDE.md` before making changes.** It is the app's own conventions
document and it is regenerated with the project — it is more current than any skill.

## Running it — no database required

The generated backend's default `dev` script is stateless and unauthenticated:

```
dotenvx run -f ./apps/api/.env --env STATEFUL=false --env AUTH=false \
  --env AUTH_MOCK_ROLES=superadmin -- nest start --debug --watch
```

`STATEFUL=false` swaps the generated repositories to in-memory implementations, so **a
fresh app boots with no PostgreSQL and no Keycloak**. That is the fastest path to something
demonstrable.

```bash
./scripts/setup.sh          # pnpm i + copy .env.example files
cd backend  && pnpm run dev # → "<App> API is running statelessly …"
cd frontend && pnpm run dev
```

The backend boot marker is `<App Name> API is running statelessly|statefully …`. Treat that
line as the verification that the app works — not a successful build, and not a successful
generation.

Switch to `dev:stateful` only when you actually need persistence; that mode requires
Postgres and `prisma migrate dev`.

## Previewing it in Skaile

`create-project` emits `skaile.preview.json` already:

```json
{
  "version": 1,
  "apps": [
    { "id": "frontend", "label": "Frontend", "path": "frontend", "role": "frontend", "protocol": true },
    { "id": "backend",  "label": "Backend",  "path": "backend",  "role": "backend" }
  ],
  "defaultAppId": "frontend"
}
```

That is the file verbatim — `label` and `path` are required per app, not decoration.

Two things the generator does **not** yet handle — fix both before relying on a preview:

- **No base path.** The generated `frontend/vite.config.ts` sets no `base`, and its
  `server.allowedHosts` lists only `app.postxl.com` and `host.docker.internal`. Under a
  preview proxy that serves the app from a sub-path, the preview can report ready while
  every module request 404s. Set `base` from the host's preview-base env var and widen
  `allowedHosts`.
- **`protocol: true` pins an old SDK.** The generated frontend depends on
  `@skaile/workspaces: ^1.11.0`, far behind current. If the app-channel handshake misbehaves,
  set `"protocol": false` — the app then previews as a plain SPA and everything else works.

## After the scaffold

You now have a normal PostXL project. Every subsequent change goes through the three modes
in `maintain.md`; schema questions go to `schema-grammar.md`.

## Gotchas

| Symptom | Cause |
|---|---|
| "✓ created successfully" but nothing runs | `pnpm` (or node) missing. Every step is catch-and-continue, so the run exits 0 over a failed install. Scroll up for the errors; re-run into a clean path |
| Hangs with no output | Ran without a TTY and without all required options — it is waiting on a prompt that will never be answered |
| Custom schema seems ignored | Step 1 always uses the simple schema; the custom one is swapped in at step 5. Only a failure *between* those leaves the simple schema in place |
| Backend won't boot, DB errors | Something set `STATEFUL=true`; the default `dev` script is stateless |
| Preview green but blank | The vite base-path issue above |
