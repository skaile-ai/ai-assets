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

## Drift and ejection — two different things

These are constantly conflated, and the recovery for one is a no-op for the other.

**Drift** — you edited a generated file on disk. The lockfile still holds the *generated*
checksum; divergence is found by comparing. The generator will merge or overwrite it, and
`-f` wins.

**Permanent ejection** — a human hand-replaced that file's checksum in `postxl-lock.json`
with the literal string `"ejected"`. **The generator never writes this sentinel** — it only
preserves it (`EJECTED_SENTINEL` in `utils/lockfile.js`; the docstring says outright, "Users
mark a file as ejected by manually replacing its checksum"). Sync then short-circuits for
that path: never written, never deleted, never checked for conflict markers.

**`force: true` respects the sentinel.** From `utils/sync.js`: *"Permanently ejected files are
fully owned by the developer — we must not … force overwrite."* So:

| To recover | Drift | Permanent eject |
|---|---|---|
| `-f` (force) | works | **no-op** |
| Delete the lockfile entry | works | **the only route** — next run treats it as `L:empty` and regenerates |

Editing a generated file outside a `@custom-*` block therefore does *not* silently eject it —
it drifts, and the next generate will try to merge. Ejection is a deliberate human act. What
both share: once a file is genuinely ejected, upstream generator fixes never reach it again.

```bash
pxl status     # drift/eject state of every generated file
pxl doctor     # full preflight: schema + drift + custom blocks + env + migrations
```

`pxl doctor` is the single best command when something feels off.

### `@custom-override:<memberName>`

Beyond `@custom-start`/`@custom-end`, the three-way merge supports a member-level keep-mine
marker: `@custom-override:<memberName>` pins your version of one class member (method or
property with a body) against regeneration, without ejecting the whole file. Reach for it
before ejecting — ejection is all-or-nothing and permanent.

## Generator flags

Verified against `@postxl/cli` 1.10.3. **Check which version you actually resolved** before
trusting flag behaviour — a stale resolution does not announce itself, and several
long-circulating warnings about these flags are stale-CLI symptoms rather than real semantics.

| Flag | What it does |
|---|---|
| `--dry-run` | Preview without writing. Skips the lockfile *and* the base snapshot. **Start here.** |
| `-m <Model…>` | Scope generation to models. Aggregate files always rebuild regardless, so a `-m` run still touches them. |
| `-d` | Show the diff between the on-disk and generated file — the only way to see what a merge would discard. |
| `-p '<glob>'` | Scope to a file glob. Genuinely scoped: the considered set is the pattern-filtered VFS plus lock entries passing `matchesPattern`, and non-matching lock entries are preserved. |
| `-f` | Force. Overwrites drift — but **not** permanent ejects. Combine with `-p` to narrow it. |
| `-i` | **`--ignore-errors`**, *not* "skip ejected". It swallows schema-verification and formatting errors so a broken run still exits 0. It does **not** suppress the conflict report. (The framework's own docs get this one wrong.) |
| `--no-three-way` | Disables base reconstruction; ejected files fall back to a 2-way merge. |

**`-f -p '<glob>'` is the documented remedy for conflict markers**, not something to avoid.
It is also the *only* way past the "Generation aborted: unresolved merge conflicts" abort —
`sync()` bails on pre-existing markers unless `force` is set.

If a scoped run blows up far outside its glob, suspect the resolved CLI version before you
conclude the flag is broken.

## Exit codes — the one that will bite a pipeline

`pxl generate` exits **1** when the run left something a human must act on: an aborted sync
(unresolved markers already on disk), an unverified merge ancestor, or an unparseable merge.

**Conflict markers on their own still exit 0.** That is deliberate and documented in the
source — flipping it would break pipelines that tolerate conflicts. The consequence:

```bash
pxl generate && pnpm build     # sails straight through a conflicted file
```

So "read the conflict list" is not advice, it is the only signal. Do not treat a zero exit as
evidence that a generate run is clean.

## Base reconstruction and `.postxl/base-snapshot.json`

The three-way merge base is **reconstructed, not stored** — re-derived from the previous
*schema* using the currently installed generators. The schema snapshot that makes this
possible lives at `.postxl/base-snapshot.json`.

**Commit that file.** It is explicitly intended to be in git so CI and fresh clones can
reconstruct the base too. Gitignored — an easy mistake if you do not know what it is — every
fresh clone and every CI run silently degrades to a 2-way merge, which conflicts far more
eagerly. The generator prints a note when it merged without a base; that note is the tell.

Reconstruction is **skipped entirely when nothing on disk is edited** — a clean tree pays
nothing. This is why `-d` output can look inconsistent between runs for no visible reason.

Where the generator itself changed, base and incoming already agree, so the change cannot
surface. 1.10.2+ verifies the reconstruction against the per-file lock checksum and falls back
to a 2-way merge on mismatch, listing those files separately. On a generator upgrade a
**rising** conflict count is the tool working, not a regression.

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
| Treating `pxl generate` exit 0 as "clean" | Conflict markers alone exit 0. Read the conflict list every run. |
| Using `-f` to recover a permanently ejected file | Force respects the `"ejected"` sentinel. Delete the lockfile entry instead. |
| Gitignoring `.postxl/base-snapshot.json` | Every fresh clone and CI run silently drops to a 2-way merge. |
