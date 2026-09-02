---
name: "postxl"
description: "[skaile-development] Use when creating, building, modifying, or extending a
  PostXL-generated app — including scaffolding a brand-new one into an empty repo with
  `pxl create-project`. Activates when you see `postxl-schema.json`, `postxl-lock.json`,
  `generate.ts`, `skaile.preview.json`, or `// @custom-start` markers; when the user mentions
  PostXL, PXL, `pxl`, regenerate, eject, custom block, or schema drift; when a user asks to
  build a new full-stack TypeScript app on the PostXL stack (NestJS + Prisma + tRPC + React 19
  + Vite); or when a regeneration produced a confusing diff or merge conflict. Also covers the
  skaile-dev projects `platform/backend`, `platform/frontend`, and `store/backend`."
version: 1.0.0
metadata:
  tags:
  - "postxl"
  - "pxl"
  - "code-generation"
  - "scaffolding"
  - "create-project"
  - "schema"
  - "nestjs"
  - "react"
  - "prisma"
  - "trpc"
  - "typescript"
  - "platform"
  - "store"
  - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
    - path: "postxl-schema.json"
      gate: soft
      description: "Project-root schema. Present → maintain mode. Absent → create mode."
    - path: "postxl-lock.json"
      gate: soft
      description: "Per-file checksums and ejection status for generated outputs."
    - path: "schema/"
      gate: soft
      description: "Optional split-schema directory with per-model / per-enum JSON files."
    - path: "platform/CLAUDE.md"
      gate: soft
      description: "Read first when working inside skaile-dev's platform/."
    - path: "store/CLAUDE.md"
      gate: soft
      description: "Read first when working inside skaile-dev's store/."
  produces: []
  reads:
  - path: "postxl-schema.json"
  - path: "schema/*.model.json"
  - path: "schema/*.enum.json"
  - path: "postxl-lock.json"
  - path: "generate.ts"
  - path: "ai-assets/skaile-development/skills/postxl/references/create-app.md"
  - path: "ai-assets/skaile-development/skills/postxl/references/schema-grammar.md"
  - path: "ai-assets/skaile-development/skills/postxl/references/maintain.md"
  - path: "ai-assets/skaile-development/skills/postxl/references/skaile-dev-patterns.md"
  - path: "platform/CLAUDE.md"
  - path: "store/CLAUDE.md"
  - path: "node_modules/@postxl/ui-components/CLAUDE.md"
---

# PostXL App Development

**PostXL** is a schema-driven full-stack TypeScript framework. A PostXL app is **not** a
hand-written codebase: most of `backend/`, `frontend/` and `e2e/` is regenerated from
`postxl-schema.json`, and every generated file is tracked in `postxl-lock.json`.

This skill routes. The detail lives in `references/` — load only the one you need.

## Pick your mode first

| Situation | Read |
|---|---|
| Empty repo, no `postxl-schema.json` — scaffold a new app | `references/create-app.md` |
| Existing app — add/change a model, field, enum, auth rule | `references/schema-grammar.md` |
| Existing app — change behavior, regenerate, resolve drift or an eject | `references/maintain.md` |
| Working inside skaile-dev (`platform/`, `store/`) | `references/skaile-dev-patterns.md` **and** that project's `CLAUDE.md` |

A generated project ships **its own `CLAUDE.md`**. It is regenerated with the project and is
more current than this skill — read it before changing anything in that app.

## Three things that define this framework

**Generated files are not yours.** Editing one outside a `// @custom-*` block makes it *drift*,
and the next generate will try to merge it. *Ejection* is different and is a deliberate human
act — hand-writing `"ejected"` as that file's lockfile checksum. The generator never writes
that sentinel, and `-f` does not override it. Full table in `references/maintain.md`.

**Strictness is split, and it fails in opposite directions.** Field-level keys are `.strict()`,
so a typo is a hard `unrecognized_keys` error. Model-level keys are stripped by a plain
`z.object()` and preserved on `model.source`, which is how real keys like `repository` and
`actions` work — generators re-parse them off `source`. So a model-level key does something
**iff some generator reads it**; an invented one parses clean and emits nothing.

**A zero exit is not a clean run.** `pxl generate` exits 1 on an aborted sync, an unverified
ancestor, or an unparseable merge — but **conflict markers alone exit 0**, deliberately. So
`pxl generate && build` sails through conflicts. Read the conflict list; and for anything that
should produce a database constraint, read the migration SQL, because `pxl validate` proves
shape, not effect.

## Package manager

A **generated standalone project is a pnpm workspace**, and `pxl create-project` shells out to
pnpm. **skaile-dev is bun across the board.** Use the one that matches the tree you are in —
this is the single most common cross-context mistake with this skill.

## skaile-dev context

Three projects in the monorepo are PostXL-generated:

| Project | Path | State |
|---|---|---|
| Skaile platform backend | `platform/backend/` | Live — NestJS + Fastify + Prisma + tRPC |
| Skaile platform frontend | `platform/frontend/` | Live — Vite + React 19 + TanStack Router |
| Public catalog | `store/` | Live — schema, `generate.ts` and lockfile at the `store/` root; generates into `store/backend/` + `store/frontend/` |

In `platform/`, `bun run generate` chains `pxl generate` + `tsr generate`. Bypassing it with a
bare `pxl generate` leaves the frontend route tree stale.

---

ROLE  PostXL guidance — routes to create / schema / maintain, keeps the agent out of generated
files, and drives the regen + dual-typecheck verify loop.

READS
  ! <project-root>/postxl-schema.json                — primary schema
  ? <project-root>/schema/*.{model,enum}.json        — split schemas
  ! <project-root>/postxl-lock.json                  — checksums + eject markers
  ? <project-root>/generate.ts                       — generator pipeline configuration
  ? <project-root>/CLAUDE.md                         — the generated app's own conventions
  ! references/create-app.md                         — greenfield scaffold
  ! references/schema-grammar.md                     — schema authoring
  ! references/maintain.md                           — modes, regen, drift, verify loop
  ! references/skaile-dev-patterns.md                — when inside platform/ or store/
  ! platform/CLAUDE.md                               — when project = platform/
  ? store/CLAUDE.md                                  — when project = store/
  ? node_modules/@postxl/ui-components/CLAUDE.md     — UI primitive catalog

WRITES
  <project-root>/postxl-schema.json                  — schema edits (Mode 1)
  <project-root>/schema/*.{model,enum}.json          — split-schema edits
  <project-root>/backend/libs/**, frontend/src/**    — only inside `// @custom-*` blocks (Mode 2)
  <project-root>/backend/apps/<feature>/**, frontend/src/{routes,components,lib}/** — new modules (Mode 3)

MUST  read the target project's own `CLAUDE.md` before any edit — generated app, `platform/`, or `store/`
MUST  prefer Mode 1 (schema edit) when the change is expressible as a schema field, validation, auth rule, or faker rule
MUST  use `// @custom-start[:name]` … `// @custom-end[:name]` for in-file custom logic (Mode 2)
MUST  use the package manager the tree actually uses — pnpm in a generated standalone app, bun in skaile-dev
MUST  run generate from the **project root** (the directory holding `postxl-schema.json`)
MUST  run the dual verify loop (backend typecheck + frontend typecheck) after any schema change or regen
MUST  apply the DB migration after any schema change that altered model fields, and read the generated SQL to confirm the constraint exists
MUST  use `@postxl/ui-components` primitives for any new UI element — never recreate buttons, inputs, dialogs, or tables
MUST  in skaile-dev, register every new user-facing `platform/frontend/` action as a command palette action (Frontend Action Pattern)
NEVER edit a file listed in `postxl-lock.json` outside a `// @custom-*` block — it drifts, and the next generate merges over it
NEVER treat `pxl generate` exit 0 as a clean run — conflict markers alone exit 0; read the conflict list
NEVER use `-f` to recover a permanently ejected file — force respects the `"ejected"` sentinel; delete the lockfile entry instead
NEVER reach for `-f` to clear conflict markers or a "Generation aborted" — force takes the non-merge path, so it silently destroys every `@custom-*` block in the files it rewrites; resolve the on-disk markers by hand and re-run
NEVER trust `pxl validate` as evidence that a constraint was emitted — it validates shape only
NEVER use `compositeUnique` — it is not a PostXL schema key; it parses silently and emits nothing. Use `indexes`
NEVER gitignore `.postxl/base-snapshot.json` — without it every fresh clone and CI run silently drops to a 2-way merge
NEVER run `bunx pxl generate` directly in `platform/` — use `bun run generate` so `tsr generate` also runs
NEVER run Biome inside `platform/` — it is Prettier + ESLint there
NEVER create a new barrel `index.ts` in `platform/backend/libs/` — import via subpath alias
NEVER use the `@Optional()` NestJS decorator — it silently swallows DI resolution failures
NEVER access the database directly (raw Prisma client, raw SQL) in custom blocks or new modules — go through `modelViewService` / `modelUpdateService`

CHECKLIST
  - [ ] Read the target project's `CLAUDE.md` (generated app's own, or `platform/` / `store/`)
  - [ ] Picked the right mode: create, schema (Mode 1), custom block (Mode 2), or new module (Mode 3)
  - [ ] Used the tree's own package manager (pnpm in a generated app, bun in skaile-dev)
  - [ ] `pxl validate` passed before generating
  - [ ] In `platform/`: used `bun run generate`, not bare `bunx pxl generate`
  - [ ] Read the generate run's conflict list — exit 0 does not mean clean
  - [ ] Custom blocks anchored, uniquely named, with matching `@custom-start` / `@custom-end`
  - [ ] Did NOT edit a file listed in `postxl-lock.json` outside a custom block
  - [ ] Migration applied and the generated SQL confirms the intended constraint
  - [ ] Both backend and frontend typechecks pass
  - [ ] `pxl status` shows no unintended drift / ejection
  - [ ] Lint clean — in `platform/`, ESLint only, Biome never invoked
  - [ ] No new `index.ts` barrel under `platform/backend/libs/`
  - [ ] No `@Optional()` on any NestJS injectable
  - [ ] Custom action handlers in `<Model>UpdateService` inject no `DispatcherService`-dependent service
  - [ ] Custom blocks and new modules read/write via `modelViewService` / `modelUpdateService`, not raw Prisma
  - [ ] New user-facing `platform/frontend/` UI registered as a command palette action
  - [ ] Used `@postxl/ui-components` primitives for new UI
  - [ ] Submodule pointer bumped in the shell repo after committing inside `platform/` or `store/`

---

## Integration

- **Called by:** `implement` (target = `platform/backend`, `platform/frontend`, `store/backend`);
  the user directly when creating or working on any PostXL project.
- **Calls:** `git` (commits + submodule pointer bump), `test`, `audit` (scope=diff), `doc`,
  `verify-ui` / `e2e-platform` (when UI changed).
- **Reference layout:** `references/{create-app,schema-grammar,maintain}.md` are portable to any
  PostXL project; `references/skaile-dev-patterns.md` is monorepo-specific and is the file to
  drop if this skill is ever published to the public catalog.
