# Maintaining a PostXL App

Portable reference — applies to any existing PostXL project.

Command examples use `pnpm`, the default for a generated project. Substitute the project's
own package manager (skaile-dev uses bun — see the skill body).

## Mental model

```
Inputs (you edit these)            Outputs (PostXL generates these)
┌─────────────────────────┐       ┌──────────────────────────────────┐
│ postxl-schema.json      │       │ backend/libs/*  (NestJS + tRPC)  │
│ schema/*.model.json     │  ──▶  │ frontend/src/*  (React + Vite)   │
│ schema/*.enum.json      │       │ e2e/specs/*     (Playwright)     │
│ generate.ts             │       │ Prisma schema, Docker, CI files  │
└─────────────────────────┘       └──────────────────────────────────┘
                                  Tracked in postxl-lock.json
```

Regeneration is idempotent. The lockfile holds each generated file's checksum, which is how
the generator detects manual edits.

## The three modes

Decide which mode applies **before** touching anything.

### Mode 1 — Schema change (prefer this)

For "add a model", "rename a field", "change validation", "tweak auth", "add a faker rule".

1. Edit `postxl-schema.json` (or the matching `schema/<name>.model.json`). Grammar:
   `schema-grammar.md`.
2. `pxl validate` — fails fast, sub-second.
3. `pnpm run generate` from the project root.
4. Run the verify loop below.
5. If model fields changed: `pnpm exec prisma migrate dev`, then **read the generated SQL**
   to confirm the constraint you intended actually exists.

### Mode 2 — Custom logic inside a generated file

```typescript
import { Injectable } from '@nestjs/common'

// @custom-start:additionalImports
import { CustomLogger } from './logger'
// @custom-end:additionalImports

@Injectable()
export class UserService {
  // ...generated methods...

  // @custom-start:auditHook
  private logAudit(event: string) {
    this.logger.log(event)
  }
  // @custom-end:auditHook
}
```

- Marker styles: `// @custom-start[:name]` … `// @custom-end[:name]`, or the `/* */` block form.
- Names are optional but recommended — they let the generator reposition a block when
  surrounding code shifts.
- **Anchor each block after an identifiable line** (a method signature, an import group). With
  no anchor above it, the block is appended at the end with a warning on the next regen.
- Named blocks must be unique within a file and have matching start/end.

**A custom block inside a region the generator rewrites will conflict**, and the merge keeps
your local copy. Resolving means removing the block, regenerating, and re-adding it.

### Mode 3 — New code outside generated paths

- Frontend: `frontend/src/components/`, `frontend/src/routes/<feature>/`, `frontend/src/lib/`
- Backend: a new module outside `backend/libs/`, e.g. `backend/apps/api/src/<feature>/`
- E2E: new spec files in `e2e/specs/`

The generator never touches files outside its known output paths.

## Ejection and drift

Editing a generated file outside a `@custom-*` block **ejects** it: the lockfile records
`"ejected"` instead of a checksum and the generator stops re-emitting it. The file is then
yours forever and upstream generator fixes never reach it. Eject deliberately, never as a
workaround.

```bash
pxl status     # eject/drift state of every generated file
pxl doctor     # full preflight: schema + drift + custom blocks + env + migrations
```

`pxl doctor` is the single best command when something feels off.

To un-eject: force-regenerate the file (overwriting your edits) or delete its lockfile entry
and regenerate. Confirm with `pxl status`.

## Generator flags are not what they look like

Verified against `@postxl/cli` 1.10.3. **Check the version you actually resolved** before
trusting any of this — the wrong version does not announce itself, it produces a
plausible-looking mess.

| Flag | What it actually does |
|---|---|
| `--dry-run` | Preview without writing or touching the lockfile. **Start here, always.** |
| `-m <Model…>` | Scope generation to models. Safe, and the right scoping flag. |
| `-d` | Show the diff between the on-disk and generated file — the only way to see what a merge would discard. |
| `-i` | **`--ignore-errors`**, *not* "skip ejected". It swallows schema-verification and formatting errors so a broken run still exits 0. It does **not** suppress the conflict report. |
| `-f` | `--force`, and it is **repo-wide** — *not* narrowed by `-m` or `-p`. A single `-f` can rewrite hundreds of unrelated files and create stray template files. |
| `-p '<glob>'` | Filters files but **crashes** the run partway, leaving no base reconstruction. Avoid. |
| `--no-three-way` | Disables base reconstruction explicitly. |

A plain generate run reports `N files with merge conflicts` and writes the **merge result**
into each — neither the generated content nor your file unchanged. Always read that list. The
separate "Generation aborted: unresolved merge conflicts" *is* a no-op: markers left on disk
by an earlier run abort the whole run before anything is written.

The merge base is **reconstructed, not stored** — re-derived from the previous schema using
the *currently installed* generators. Where the generator itself changed, base and incoming
already agree, so the change cannot surface. 1.10.2+ verifies the reconstruction against the
per-file lock checksum and falls back to a 2-way merge on mismatch, listing those files
separately. On a generator upgrade a **rising** conflict count is the tool working, not a
regression.

## The verify loop

PostXL generates both halves. **Type-checking one half misses real failures** — the frontend
imports backend-derived types.

```bash
pxl validate                      # 1. schema, sub-second
pnpm run generate                 # 2. regenerate
pnpm run -r test:types            # 3. typecheck BOTH backend and frontend
pnpm exec prisma migrate dev      # 4. only if model fields changed
```

For faster iteration on one model, `pxl generate -m Country City` — but re-run the project's
full generate script once before committing, since scoped runs skip the chained steps.

**`pnpm run generate` may not reach `prisma generate`.** Generate scripts commonly chain with
`&&`; if an earlier link fails, the Prisma client is silently left stale against the new
schema. After any schema change, run `pnpm exec prisma generate` yourself and confirm.

## Command reference

| Task | Command |
|---|---|
| Validate schema | `pxl validate` |
| Project orientation | `pxl info` |
| Composite preflight | `pxl doctor` |
| Eject/drift status | `pxl status` |
| List/validate custom blocks | `pxl custom-block` |
| Regenerate everything | `pnpm run generate` |
| Regenerate one model | `pxl generate -m ModelName` |
| Preview a regen | `pxl generate --dry-run` |
| See what a merge would discard | `pxl generate -d --dry-run -m ModelName` |
| Apply DB migration | `pnpm exec prisma migrate dev` |
| Reset DB | `pnpm exec prisma migrate reset` |
| Create a new project | `pxl create-project` (see `create-app.md`) |

## Operational modes

The generated backend ships several runtime modes — same code, different repository wiring:

| Script | Repositories | Auth |
|---|---|---|
| `dev` | in-memory | none (mock roles) |
| `dev:auth` | in-memory | Keycloak |
| `dev:stateful` | Prisma + Postgres | none |
| `dev:stateful:auth` | Prisma + Postgres | Keycloak |
| `e2e:stateless` / `e2e:stateful` | either | none |

`dev` is the fastest loop and needs no database.

## UI components

The frontend ships `@postxl/ui-components` (~60 primitives: Button, Input, Dialog, DataGrid,
form components). Read `node_modules/@postxl/ui-components/CLAUDE.md` before creating any UI
element, and never hand-roll a generic primitive. For tables use `DataGrid` — it carries
sorting, filtering, inline edit, and saved views via the `TableView` standard model.

## Common pitfalls

| Mistake | What to do instead |
|---|---|
| Editing under `backend/libs/` or `frontend/src/components/admin/` directly | Check `postxl-lock.json` — if listed, it is generated. Edit the schema or use a custom block. |
| Forgetting `prisma migrate dev` after a schema change | Backend boots; DB queries fail at runtime with missing-table/column errors. |
| Typechecking only the backend | The frontend imports BE-derived types and breaks silently until built. |
| Running generate from `backend/` or `frontend/` | Must run from the project root — the directory holding `postxl-schema.json`. |
| Custom block with no anchor line | Fragile; lands at the end with a warning after a future regen. |
| Putting business logic in `generate.ts` | That file only configures *which generators run*. |
| Trusting `pxl validate` to prove a constraint exists | It validates shape only. Read the migration SQL. |
| Reaching for `-f` or `-p` to fix a conflict | `-f` is repo-wide; `-p` crashes. Use `-d --dry-run -m <Model>` to inspect, then resolve the custom block. |
