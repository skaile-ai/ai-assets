# skaile-dev PostXL Patterns

**Internal reference — NOT portable.** Everything here is specific to the skaile-dev
monorepo. If this skill is ever published to the public catalog, this file is the one that
does not travel; the other three references do.

## The generate chain does not reach `prisma generate`

In `platform/`, the scripts chain with `&&`:

```
generate → generate:project (generate:project:pxl && generate:tsr) && generate:prisma
```

`generate:tsr` runs a filtered workspace command that **fails when invoked from `platform/`**,
so `&&` short-circuits and `generate:prisma` never runs — leaving the committed Prisma client
silently stale against the new schema. It looks like a harmless tail error after a successful
codegen run.

After any schema change, run the tail steps yourself:

```bash
bunx prisma generate                  # from platform/
cd frontend && bunx tsr generate      # only if routes changed
```

CI does not catch this: the generate-clean check deliberately runs only `pxl generate`.

## Resolve the right `@postxl/cli`

`platform` declares its own pin, but resolution can fall through to a stale copy in an
ancestor directory. A one-model schema change on the wrong version rewrote ~113 unrelated
repository files and left conflict markers in 13 of them; on the pinned version the same
change touched 32 and none conflicted. It also makes `--dry-run` *write files*.

```bash
ls -d node_modules/@postxl/cli ../node_modules/@postxl/cli 2>/dev/null | \
  while read d; do echo "$d $(grep -m1 '\"version\"' "$d/package.json")"; done
```

If a conflict report names files with no relation to your change, suspect the version before
you start resolving anything.

## Custom action handlers — service injection rules

PostXL generates one `<Model>UpdateService` per model in `backend/libs/update/`. Each handler
is dispatched through `DispatcherService` (in `ActionsModule`), which already depends on
`UpdateService` (forwardRef'd), which depends on every `<Model>UpdateService` (forwardRef'd).

**Any service that itself depends on `DispatcherService` cannot be injected into a
`<Model>UpdateService` constructor** — it closes a cycle that JavaScript module-load cannot
resolve. `forwardRef`, `@Optional()` and `ModuleRef` all happen *after* module load, so none
of them break it.

Common offenders:

- `SkaileConfigService` — dispatches `skaileConfig.update`
- `SessionLifecycleService` — dispatches session field updates
- any custom service calling `this.dispatcher.dispatch(...)` internally

The right pattern when a handler needs data from such a service:

1. **In the tRPC route**, pre-resolve it via `ctx.<service>` — the route layer is outside the
   cycle because `TrpcPlugin` injects these services directly.
2. **Add the resolved value to the action payload** as a new field on the action's Zod decoder.
3. **In the handler**, consume `data.<field>`. The handler stays pure — no cross-module class
   injection, no runtime `import` of the cycle-causing service. If you need the *type*, use
   `import type { … }` (erased at compile time).

Preconditions that require reading from such a service belong in the route too — throw
`TRPCError({ code: 'BAD_REQUEST', … })` before dispatching.

Worked example (`Session.discard` in `platform/backend/`):

```typescript
// session-actions.route.ts — route layer, outside the cycle
discard: procedure
  .use(authMiddleware)
  .input(z.object({ sessionId: zSessionId }))
  .mutation(async ({ input, ctx }) => {
    const config = await ctx.skaileConfigService.getEffectiveConfig(input.sessionId)
    if (mountHasAutosync(config.mounts[0])) {
      throw new TRPCError({ code: 'BAD_REQUEST', message: '…' })
    }
    return ctx.dispatch({ scope: 'session', type: 'discard', payload: { sessionId: input.sessionId } })
  }),

// session.update.service.ts — handler, no SkaileConfigService import
import type { SkaileConfigData } from '@session/skaile-config.service'  // type-only, no cycle
```

**Symptom of getting this wrong:** backend boot fails with `UndefinedDependencyException`
pointing at an *unrelated* service. Nest gave up on the cycle and injected `undefined` into
the first consumer of the deadlocked provider it tried to construct.

## Diagnosing `UndefinedDependencyException` at boot

The reported service usually is not the cause.

1. Identify the unresolved parameter (the `?` in the constructor signature in the error).
   That is the provider that could not resolve — not the reported service.
2. Grep for both `@Inject(<ProviderClass>)` and **value-level** `import { <ProviderClass> }`
   (not `import type`).
3. Walk the import graph: does any file that runtime-imports the provider get re-imported,
   directly or transitively, by the provider's own dependency chain? That is the cycle.
4. If yes, the cycle is at JavaScript module-load level. Neither `forwardRef`, `@Optional()`
   nor `ModuleRef.get(…)` fixes it — all happen later than the failing module load. Remove
   the runtime import (switch to `import type`) or move the call site to a layer outside the
   cycle.
5. If no cycle is visible, check decorator parameter union types: `private readonly foo: Foo | null`
   emits `Object` as `design:type` metadata, and Nest cannot resolve it without an explicit
   `@Inject(Foo)`.

Related: **never `import type` a class you constructor-inject** without an explicit `@Inject()`
token — the erased import leaves Nest with a generic `Function` in `design:paramtypes`.

## Barrel files

`platform/backend/libs/` forbids new `index.ts` re-export barrels — they break NestJS DI. The
only exception is the PostXL-generated barrels tracked in `postxl-lock.json`. If a custom
block or Mode-3 module creates a barrel-shaped import, restructure it into direct subpath
imports (`@credential/credential.service`, not `@credential`).

The deeper reason: barrel imports — and even direct subpath imports of services that are
themselves transitively re-imported by the consumer — create circular file-level imports that
JavaScript resolves to `undefined` at module-load time.

## Frontend Action Pattern

Every new user-facing action in `platform/frontend/` — including ones added in custom blocks
or new `frontend/src/routes/<feature>/` modules — must register a command palette action via
`useCommandPaletteActions()`. This is mandatory: Cmd+K and the in-app agent are how features
get discovered. Convention is `<feature>.actions.tsx`, co-located, exporting
`use<Feature>Actions()`. Actions **delegate, never own mutations** — `run` calls callbacks
from the host component.

## Lint stack

Prettier + ESLint. **Never Biome** anywhere inside `platform/` — do not run `bun run format`.
Use each package's `bun run lint`.

## Data access

Custom blocks and new modules must never call the Prisma client or raw SQL directly. Reads go
through the relevant `modelViewService` methods or its `.data` property; writes through
`modelUpdateService` methods or a dispatched mutation action. Direct access bypasses caching,
authorization checks, and the event hooks PostXL generates.

## `@Optional()` is forbidden

Never annotate a NestJS constructor parameter with `@Optional()` — it silently drops DI
resolution failures, so broken wiring becomes invisible at runtime. When a dependency is
unavailable in a test context, provide a mock or stub in the test module instead.

## Submodule commit workflow

`platform/` and `store/` are git submodules of skaile-dev:

1. Commit inside the submodule.
2. Bump the submodule pointer with a `chore(submodules): bump …` commit in the shell repo.

The `git` skill handles both — use it rather than rolling your own.

## Two more sources of truth

- `platform/CLAUDE.md` — platform conventions on top of PostXL.
- `platform/backend/libs/<lib>/CLAUDE.md` — per-lib conventions. Read the one for the lib you
  are touching.
