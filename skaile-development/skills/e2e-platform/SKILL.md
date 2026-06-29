---
name: 'e2e-platform'
description: 'Run, fix, or extend the Skaile platform e2e suite. Three modes: `run`
  executes the existing suite with pre-flight checks (stale-service guard for post-pull
  stale bundles, stale-deps guard for lockfile drift, seed sync, proxy slot health),
  failure-mode diagnosis, and auto-recovery (kill stale skaile-serve subprocesses,
  sync seed data, restart stuck proxy slots); `fix` triages each failing test as a
  stale test (the product changed on purpose) vs a real regression (the product broke),
  then repairs stale tests (rewrite to the new shape / delete a removed feature / skip
  with a reason) while STOPPING and escalating regressions to the human — never
  weakening an assertion to force green; `add` analyzes a git diff, proposes a test
  plan, waits for explicit user approval, lives-inspects the UI via chrome-devtools
  MCP, then writes new specs against the existing `platform/e2e/` harness. Use when
  the user says "run e2e tests", "fix the failing e2e tests", "the e2e suite is red",
  "add e2e coverage for <feature>", "write e2e tests for my PR", or similar.'
metadata:
  version: '1.3.0'
  tags:
  - 'testing'
  - 'e2e'
  - 'playwright'
  - 'platform'
  - 'skaile-development'
  source: 'MERGED'
  stage: 'beta'
  prerequisites:
    files:
    - path: 'platform/e2e/playwright.config.ts'
      gate: hard
      description: 'Existing platform e2e harness must be in place (this skill extends
        it, does not scaffold it — see `test-e2e` for initial scaffolding).'
    - path: 'platform/e2e/CLAUDE.md'
      gate: soft
      description: 'Recommended knowledge source. The skill references its failure-mode
        table and scaffolding rules.'
  user_inputs:
    dialog:
    - id: 'mode'
      label: 'Mode'
      type: 'select'
      options: ['run', 'fix', 'add']
      required: true
      hint: "'run' executes the suite with auto-recovery; 'fix' triages failing tests
        stale-vs-regression and repairs stale ones while escalating regressions; 'add'
        analyzes a diff and proposes new specs with a user approval gate"
    - id: 'scope'
      label: 'Scope'
      type: 'text'
      required: false
      hint: "'run' mode: spec file path or test name filter (default: full suite).
        'fix' mode: failing spec file or test name filter (default: full failing suite).
        'add' mode: feature area / merged PRs to focus on (default: git diff main…HEAD;
        falls back to gap-discovery over recently-merged features when the diff is empty)"
    - id: 'session_mode_hint'
      label: 'Session mode'
      type: 'select'
      options: ['auto', 'mock', 'local']
      required: false
      default: 'auto'
      hint: "'add' mode only. 'auto' picks based on change kind (local for session-lifecycle
        or flow tests, mock otherwise)"
    - id: 'live_inspect'
      label: 'Live-inspect UI before writing'
      type: 'select'
      options: ['true', 'false']
      required: false
      default: 'true'
      hint: "'add' mode only. Uses chrome-devtools MCP to verify selectors against
        the actual DOM before committing them to a spec. Disable only if the MCP is
        unavailable."
    files: []
---

# E2E Platform — Run + Extend the Platform E2E Suite

Covers the workflows missing from the sibling skills:

- `test-e2e` scaffolds Playwright for forge apps + agent-framework CLI, but explicitly delegates `platform/frontend` to `platform/e2e/` and stops there — this skill picks up from that delegation.
- `test` (run mode) executes Jest/Vitest/Playwright generically but lacks Skaile-specific pre-flight + recovery.
- `verify-ui` does visual checks against a running UI without writing specs.

## When to use

Invoke when the user says:

- "run the e2e tests" / "run e2e" — `mode=run`
- "fix the failing e2e tests" / "the e2e suite is red" / "these tests have been failing on Jenkins" — `mode=fix` (triage stale-vs-regression, then repair stale / escalate regressions). Use `mode=run` only when the failure is purely infra/flaky (recovery, no spec edits).
- "add e2e tests for <feature>" — `mode=add`
- "write e2e tests for my PR" — `mode=add`
- "propose e2e coverage for these changes" — `mode=add`

## When NOT to use

- For scaffolding a brand-new Playwright harness (forge app, CLI package) → use `test-e2e`
- For visual smoke of the platform UI without writing specs → use `verify-ui`
- For unit tests or integration tests → use `test-unit` / `test-integration`
- For **stateful-mode** e2e (Prisma + DB migrations). Out of scope for now — current suite is all `e2e:stateless`. Reach out to the maintainers if stateful e2e becomes a blocker.

---

ROLE  Skaile platform e2e runner + spec writer. Knows the platform's
test-harness particularities (session modes, impersonation,
org-scoped slug URLs, coverage fixture, per-spec backend isolation).

READS
  ! platform/e2e/CLAUDE.md                   — failure-mode table + scaffolding rules
  ! platform/e2e/README.md                   — env vars, session modes
  ? platform/e2e/E2E-GUIDE.md                — seed data tables, keyword triggers
  ! platform/e2e/specs/**                    — existing specs as templates + coverage map
  ! platform/e2e/fixtures/{test-fixtures,handle-backend}.ts — auto-fixtures + startBackend/getTestHeaders
  git diff main...HEAD                        — for `add` mode analysis
  (add mode) frontend routes + tRPC routes   — to derive URL patterns + selectors
  (fix mode) test-results/**/error-context.md — the failing run's assertion + stack
  (fix mode) the PR/commit that last changed the asserted behavior (git log -S / blame)
  (fix mode) source + live UI (chrome-devtools MCP) — to prove stale-vs-regression

WRITES
  (add + fix modes)
    platform/e2e/specs/**/<new-or-edited>.spec.ts

MUST   read platform/e2e/CLAUDE.md before executing or writing anything
MUST   run pre-flight before every invocation (see Step 0)
MUST   use the shared `page` fixture from test-fixtures.ts (NOT `browser.newContext()`)
MUST   use org-scoped slug-based URLs (`/acme/projects/<slug>/main/...`)
MUST   use `page.addInitScript` for impersonation (pxl-mock-user + optional pxl-mock-roles)
MUST   (add mode) gate writing behind explicit user approval of the proposed plan
MUST   (add mode) live-inspect UI via chrome-devtools MCP before committing selectors (when live_inspect=true)
MUST   (fix mode) classify EVERY failure as stale-test or regression WITH EVIDENCE (the commit/PR that changed behavior, the source, and a live-UI inspection for selector drift) BEFORE editing any spec
MUST   (fix mode) confirm the FEATURE still works before calling a test "stale" — a passing sibling test, the control actually performing its action, the API returning data when called correctly. Burden of proof is on "stale"; if unproven, treat as a suspected regression
MUST   (fix mode) STOP and report a confirmed/suspected regression to the human — never edit, weaken, or skip the test to pass over it
MUST   (fix mode) gate any spec DELETION or `.skip` behind explicit user approval; prefer rewriting to preserve coverage
MUST   delegate execution to `bunx playwright test` / the existing npm scripts — do not re-invent runners
MUST   follow the stale-service / stale-deps recovery recipes verbatim and in order — even if an earlier check reported "clean", later steps may still be needed (e.g. workspace topology changes evade the lockfile-mtime check). Do NOT skip steps based on side checks.
NEVER  write to `platform/e2e/playwright.config.ts` or global fixtures — those belong to `test-e2e`
NEVER  make a test lie: weaken/loosen an assertion, add a blind wait, or broaden a selector just to force green. In `fix` mode, triage stale-vs-regression first (see Mode: fix); a real regression is reported to the human, never silenced by editing the test.
NEVER  use `page.waitForLoadState('networkidle')` (SSE subscriptions never idle)
NEVER  hardcode project IDs in URLs — always slugs
NEVER  (add mode) bypass the user approval gate

EMIT   [e2e-platform] started mode=<mode> scope=<scope>

# ── Step 0: Pre-flight (both modes) ──────────────────────────────

1. Read `platform/e2e/CLAUDE.md` if not already in context.

2. Ensure services are up. Both modes need backend (:3001) + frontend (:3000) listening.
   ```bash
   lsof -i :3000 -sTCP:LISTEN >/dev/null 2>&1 || echo "FE_DOWN"
   lsof -i :3001 -sTCP:LISTEN >/dev/null 2>&1 || echo "BE_DOWN"
   ```
   IF either is down:
     **Record which services were down at preflight** — the cleanup steps at end-of-mode (R5 / A8) only tear down services we auto-started here. Services already up belong to whatever process started them (parallel dev session, another tool) and must be left alone. Track per-service: `WE_STARTED_BACKEND = BE_DOWN`, `WE_STARTED_FRONTEND = FE_DOWN`.
     EMIT   [e2e-platform] auto_starting backend=<true|false> frontend=<true|false>

     Run the bundled service helper. It is idempotent — only starts what's missing (backend in `e2e:stateless` mode, frontend in `dev:noAuth` mode), reuses already-running services, detaches via `nohup` so services survive after the script exits, and writes logs to `platform/.e2e-backend.log` / `platform/.e2e-frontend.log`. It also runs a **clean** backend rebuild (`rm -rf dist && bun run build` in `platform/backend`) whenever `dist/` is stale — detected by any of: the e2e entry-point trampoline (`dist/apps/api/apps/api/src/e2e.js`) is missing; the trampoline is older than `tsconfig.json`; or any backend source file is newer than the trampoline (the post-pull case). The clean build is required because `nest build` is incremental and never deletes orphaned `.js` files — after a pull that renames a workspace package (e.g. `@skaile/agent-sdk` → `@skaile/workspaces`) or restructures directories, stale output survives every incremental rebuild and the backend crashes at boot with `Cannot find module '@skaile/agent-sdk/...'`. `bun run e2e:stateless` never rebuilds anything on its own:
     ```bash
     cd platform && ./scripts/e2e.sh
     ```
     The script blocks until both ports listen (timeout 60s per service) and prints one line per service. After it returns, re-verify with the same `lsof` check.
     IF a port is still down after the script ran: read the last ~30 lines of the relevant log, report the startup error verbatim to the user, STOP. Do not retry blindly — startup failures usually mean missing migrations, port conflicts with another app, or a code bug, and looping won't fix any of those.
   ELSE: both up — set `WE_STARTED_BACKEND = false`, `WE_STARTED_FRONTEND = false`. Cleanup at end-of-mode (R5 / A8) will skip — services were running before this invocation, leave them.
     EMIT   [e2e-platform] services_pre_existing  (Step R5/A8 will skip cleanup)

   NOTE: This is *auto-start* (spawn what's missing). It is **not** *auto-fix stale* (kill+restart already-running services to clear out-of-date code) — that's Step 3 below, more destructive (drops in-flight state, may break other dev work), and still requires explicit user authorization.

   NOTE: If you ever need to bring up the backend manually (bypassing `./scripts/e2e.sh`), run a **clean** `rm -rf dist && bun run build` in `platform/backend` first whenever `dist/` may be stale — `bun run e2e:stateless` never rebuilds. A plain incremental `bun run build` is not enough: `nest build` leaves orphaned `.js` files behind, so after a package rename or directory restructure the backend still crashes on the stale output. The existence/mtime checks alone are not enough to detect this.

3. **Stale-service check (post-pull guard).** A backend or frontend started before the latest commit on `platform/main` is serving pre-pull code; the suite will mass-fail with UI-drift-looking errors that are actually stale-bundle errors. Compare each service's process start time against the platform submodule HEAD commit time:
   ```bash
   PLATFORM_HEAD_TS=$(git -C platform log -1 --format=%ct HEAD)
   for svc in backend:3001 frontend:3000; do
     name=${svc%:*}; port=${svc#*:}
     pid=$(lsof -i :$port -sTCP:LISTEN -t 2>/dev/null | head -1)
     [ -z "$pid" ] && continue
     start=$(date -j -f "%a %b %e %H:%M:%S %Y" "$(ps -o lstart= -p $pid | tail -1 | xargs)" "+%s" 2>/dev/null)
     [ -n "$start" ] && [ "$start" -lt "$PLATFORM_HEAD_TS" ] \
       && echo "STALE_$name (started $((PLATFORM_HEAD_TS - start))s before HEAD commit)"
   done
   ```
   IF anything reports `STALE_*`:
     Report to user — do NOT run tests (they will mass-fail and waste 30+ seconds):
       > "Stale services detected after a recent code update. The suite will mass-fail. Run ALL steps below in order — do NOT skip step 1 even if Step 4 (stale-deps) reported clean. The lockfile-mtime check cannot detect missing workspace symlinks after a package relocation. Recipe:
       >   1. cd skaile-dev && bun install                                  ← always run, even if deps look clean
       >   2. invoke /kill-backend skill (kills the whole backend chain cleanly)
       >   3. pkill -f 'vite preview'
       >   4. cd platform/backend && rm -rf dist && bun run build && bun run e2e:stateless    ← clean rebuild; incremental `bun run build` leaves orphaned `.js` files, `nest start --watch` rebuilds nothing
       >   5. cd platform/frontend && bun run build && bun run preview --port 3000
       > Then re-invoke this skill."
     STOP. Auto-start of *missing* services (Step 2) is fine; killing and restarting *already-running* services to clear staleness is destructive (drops in-flight requests, may interrupt parallel dev work) and requires explicit user authorization.

4. **Stale-dist check (post-pull guard #2).** The backend serves compiled output from `dist/`. `nest build` is **incremental** — it re-emits changed files but **never deletes orphaned output**. Three distinct staleness modes, all crashing the backend at boot:

   - **Missing trampoline.** `nest start --watch` expects `dist/apps/api/apps/api/src/e2e.js`, generated by `postbuild-trampoline.js` (runs only inside `bun run build`). Crash: `Cannot find module .../e2e.js`.
   - **Stale trampoline.** The tsconfig-paths registry compiled into the trampoline misses path aliases added after it was last built. Crash: `ERR_MODULE_NOT_FOUND` on a `.js`-extension relative import (e.g. `.../agent-framework/connectors/src/port-pool.js`).
   - **Orphaned output.** After a pull that renames a workspace package (e.g. `@skaile/agent-sdk` → `@skaile/workspaces`) or restructures directories, old `.js` files survive every incremental rebuild. Crash: `Cannot find module '@skaile/agent-sdk/session'`. The mtime-vs-`tsconfig` check **cannot** detect this — a package rename does not touch `tsconfig.json`.

   Detect all three by comparing the trampoline against `tsconfig.json` **and** against backend source:
   ```bash
   TRAMP=platform/backend/dist/apps/api/apps/api/src/e2e.js
   TSCONF=platform/backend/tsconfig.json
   [ -f "$TRAMP" ] || echo "MISSING_DIST"
   [ -f "$TRAMP" ] && [ "$TSCONF" -nt "$TRAMP" ] && echo "STALE_DIST (tsconfig)"
   [ -f "$TRAMP" ] && [ -n "$(find platform/backend/apps platform/backend/libs -name '*.ts' -newer "$TRAMP" -print -quit)" ] && echo "STALE_DIST (source)"
   ```
   IF any `MISSING_DIST` / `STALE_DIST`:
     This is non-destructive (only `dist/` is touched). Run a **clean** rebuild without prompting — a plain incremental `bun run build` does not prune orphaned files and will not fix the rename case:
     ```bash
     cd platform/backend && rm -rf dist && bun run build
     ```
     If services were already running against the stale `dist/`, the running backend still has the stale code/alias map in memory — fixing `dist/` isn't enough. After the rebuild, also restart the backend (treat as a stale-service event: route through the recovery recipe in Step 3 with explicit user authorization, since restarting drops in-flight state).

5. **Stale-deps check.** Two independent failure modes — both produce the same opaque module-resolution errors at runtime, so check both:

   (a) Lockfile newer than `node_modules` (typical post-pull symptom):
   ```bash
   LOCK=skaile-dev/bun.lockb
   MARK=skaile-dev/node_modules/.modules.yaml
   [ -f "$LOCK" ] && [ -f "$MARK" ] && [ "$LOCK" -nt "$MARK" ] && echo STALE_DEPS
   ```

   (b) Critical workspace symlinks missing — the mtime check above cannot detect the case where a package was relocated within the workspace tree (e.g. `theme/` → `agent-framework/theme/`). Neither file's mtime moves, but the consumer-package `node_modules/@skaile/*` symlinks are missing entirely.

   **Important:** this Bun workspace does NOT hoist `@skaile/*` to the repo-root `node_modules/`. Workspace symlinks live in *per-package* `node_modules/` (e.g. `platform/frontend/node_modules/@skaile/theme`, `platform/backend/node_modules/@skaile/agent-sdk`). Checking `skaile-dev/node_modules/@skaile/` always reports missing on this layout — it's a false positive. Spot-check the consumer packages directly:
   ```bash
   # Spot-check critical workspace symlinks where they actually live.
   # Adjust the prefix if running from a subfolder.
   for entry in \
     platform/frontend/node_modules/@skaile/theme \
     platform/frontend/node_modules/@skaile/agent-types \
     platform/backend/node_modules/@skaile/agent-sdk \
   ; do
     [ -e "$entry" ] || echo "MISSING_$entry"
   done
   ```

   IF `STALE_DEPS` OR any `MISSING_*` from (b):
     Report:
       > "Dependencies out of sync — run `bun install` from the skaile-dev root before tests, or this run will surface as opaque module-resolution errors (e.g. Vite '[Internal server error] Can't resolve @skaile/<pkg>'). The mtime check (a) and the per-package symlink check (b) catch different failure modes — either firing requires `bun install`."
     STOP unless the user has already authorized auto-install elsewhere. Do not run `bun install` without explicit permission.

     IF the user authorizes `bun install`: run it from the skaile-dev root, then re-run BOTH (a) and (b). If `bun install` reports "no changes" AND (b) is still red on the same paths, treat that as catastrophic (the workspace topology has changed in a way `bun install` won't fix on its own — escalate to the user with the failing paths verbatim, do NOT loop).

6. Kill stale `skaile serve` subprocesses (cheap hygiene):
   ```bash
   pkill -f 'skaile serve' 2>/dev/null || true
   ```

7. Check seed staleness:
   ```bash
   SRC=platform/backend/test-data.json
   DST=platform/backend/dist/backend/test-data.json
   [ -f "$DST" ] && [ "$SRC" -nt "$DST" ] && echo STALE
   ```
   IF STALE: run `cd platform/backend && bun run sync-seed`.

8. Detect proxy-stuck "stopping" slots:
   ```bash
   tail -50 /tmp/skaile-platform-logs/backend-e2e.log 2>/dev/null | grep -c '(stopping)'
   ```
   IF count > 0 (for >30 seconds):
     > "The e2e proxy has N stuck 'stopping' slots from a prior run. Restart? (yes/no)"
     IF yes:
       ```bash
       pkill -f 'node.*e2e.js'; pkill -f 'nest start'
       cd platform/backend && bun run e2e:stateless &
       ```
       Wait for `:3001` to accept connections again before continuing.

EMIT   [e2e-platform] preflight_ok

# ── Mode: run ────────────────────────────────────────────────────

IF mode = run

  STEP R1: Execute
    ```bash
    cd platform/e2e
    BASE_URL='http://localhost:3000' API_URL='http://localhost:3001' \
      bunx playwright test <scope> --retries=0 --workers=1 --reporter=dot --max-failures=3 \
      2>&1 | tail -60
    ```
    - `<scope>` is the user-provided scope, if any. Omit for full suite.
    - `--retries=0` is load-bearing — surfaces the real failure mode instead of retry-masked flakiness.
    - `--workers=1` is the current default (per playwright.config); bumping it is out-of-scope for `run`.
    - `--reporter=dot` keeps agent output tiny: one char per test (`.` pass, `F` fail). Failure detail still prints in full at the end. Pass `--reporter=list` only when triaging a single failing spec and you need the per-test running narrative.
    - `--max-failures=3` bails after 3 failures so a cascade-fail doesn't dump 30+ failures into the agent's context. Drop it when investigating broad regressions.
    - `| tail -60` is enough for the dot summary + a couple of failure traces. For triage, re-run the failing spec alone with `--reporter=list` and no tail to see full traces.

  STEP R2: Classify failures (if any)
    For each failed test, match against `platform/e2e/CLAUDE.md`'s failure-mode table:

    | Cause category | Auto-recover? | Action |
    |---|---|---|
    | Infra: `beforeAll timeout` / `ERR_CONNECTION_REFUSED` / `provision_secrets timed out` | Yes | Apply recipe from table, retry once |
    | Seed drift: `teamAdmin.list returns []` / similar empty-list surprise | Yes | `sync-seed`, retry once |
    | Coverage warning: `MCR: must be Array(V8)` | No | Report — test design issue (see scaffolding rules) |
    | UI / API drift: `heading "Foo" not found`, `expected object, received null` | No | Report — spec asserts the old shape; suggest `mode=fix` to triage stale-vs-regression and repair |
    | Persistence race / ambiguous selector / `toHaveCount(0)` false-green | No | Report with diagnosis category; suggest `mode=fix` |
    | Unknown (not in table) | No | Report verbatim — do not guess |

  STEP R3: Apply recoveries and retry once
    Only for auto-recoverable causes. Apply the recipe, re-run the failing specs only (not the full suite). Record whether the retry passed.

  STEP R4: Report
    ```
    ## E2E Run — <timestamp>

    ### Summary
    - <N> passed / <N> failed / <N> skipped / <N> flaky
    - Wall time: <Xs>

    ### Failures (after auto-recovery)
    | # | Spec | Test | Category | Suggested action |
    | 1 | <spec.ts:line> | <test name> | UI/API drift | Run `e2e-platform mode=fix` to triage stale-vs-regression + repair |
    | 2 | <spec.ts:line> | <test name> | Persistence race | Run `e2e-platform mode=fix` (or add a wait after the confirming event, before reload) |

    ### Recoveries applied
    - Killed N stale `skaile serve` subprocesses
    - Ran `sync-seed` (test-data.json was 2 mins newer than dist)
    - Restarted e2e proxy (had 2 stuck "stopping" slots)

    ### Next steps
    - <concrete suggestions>
    ```

  EMIT   [e2e-platform] run_complete passing=<N> failing=<N>

  STEP R5: Cleanup services we auto-started (if any)
    Reference the WE_STARTED_BACKEND / WE_STARTED_FRONTEND flags recorded at Step 0 §2 (also visible as the `auto_starting` EMIT line). Dispatch:

    | WE_STARTED_BACKEND | WE_STARTED_FRONTEND | Action |
    |---|---|---|
    | true  | true  | `cd platform && ./scripts/e2e.sh --stop` |
    | true  | false | `cd platform && ./scripts/e2e.sh --stop-backend` |
    | false | true  | `cd platform && ./scripts/e2e.sh --stop-frontend` |
    | false | false | Skip — services were already up at preflight; leave them. |

    Print the script's output verbatim into the report so the user sees what was torn down. If the script reports a port "still up" after stopping, do NOT escalate (broader pkill could kill the user's parallel processes); just include the warning in the report's "Next steps" so the user can decide.
    EMIT   [e2e-platform] cleanup_complete

# ── Mode: fix ────────────────────────────────────────────────────

IF mode = fix

  A failing e2e test is a question, not a problem to silence. The job is to
  restore the suite's truth: update the tests the product intentionally
  outgrew, and raise the alarm on the tests that caught a real bug. The fix is
  OPPOSITE for the two cases, so the triage in F2 is the whole skill — never
  skip it, never make a test lie to get green.

  STEP F1: Reproduce on FRESH services, no retries
    - Pre-flight (Step 0) MUST have reported fresh, non-stale backend +
      frontend. A stale bundle fakes BOTH failures and passes — refuse to
      triage against it (re-run Step 0's stale-service guard if unsure).
    - Run the failing scope with `--retries=0 --workers=1` (the real failure
      mode, not retry-masked). Read the exact error and the per-test
      `test-results/**/error-context.md` (it carries the assertion + stack).
    - If a failure does NOT reproduce locally on fresh services, it is flaky /
      infra, not a spec bug — handle via `mode=run` recovery and do NOT rewrite
      a spec that actually passes (that would be making a test lie in reverse).

  STEP F2: Classify each failure — STALE vs REGRESSION (with evidence)
    For EVERY failing test, decide which it is and PROVE it:

    | Verdict | Means | Evidence to gather |
    |---|---|---|
    | STALE | The product changed on purpose; the test still asserts the old shape (moved selector, renamed label, changed route, new required input) | The PR/commit that changed it (`git log -S "<symbol>"`, blame), a design spec/CLAUDE.md note, the current source, and — for selector/UI drift — a live chrome-devtools MCP inspection |
    | REGRESSION | The product broke; the red test is correct and is doing its job | The feature genuinely fails for a real user — not merely that a selector moved |

    Burden of proof is on STALE. Before declaring a test stale, CONFIRM THE
    FEATURE STILL WORKS — e.g. a sibling test still passes, the control still
    performs its action, the API still returns data when called correctly. If
    you cannot prove the feature works, treat it as a SUSPECTED REGRESSION (F4),
    not stale. A plausible-looking selector change is the most common way a real
    bug gets silently rewritten away.

  STEP F3: Fix STALE tests — preserve coverage, never loosen
    Pick the narrowest correct repair:
    - Behaviour still exists, shape moved → REWRITE to the new selector / URL /
      flow. Keep — or TIGHTEN — the assertion; never weaken it to pass.
    - Feature permanently removed → DELETE the spec (git keeps the history).
      Note the resulting coverage gap in the report. Requires approval (F5).
    - Behaviour temporarily unverifiable but returning → `.skip` WITH a reason +
      a tracking note. Requires approval (F5). NEVER `.skip` as a cop-out for a
      selector you could not get working — delete-and-note the gap, or fix it.
    Re-run the rewritten test on fresh services with `--retries=0` until green;
    the passing run is the proof, not the reasoning.

  STEP F4: Handle a REGRESSION — STOP; do not touch the test
    A confirmed (or strongly suspected) regression is NOT a test problem.
    - Do NOT edit, weaken, or `.skip` the test to make CI green.
    - Report it to the human with: the failing test, the user-visible symptom,
      the suspected commit/PR, and the evidence. Recommend a PRODUCT fix.
    - Leave the test red (it is doing its job) unless the user directs otherwise.

  STEP F5: Approval gate — deletions, skips, regressions
    Before DELETING a spec, adding `.skip`, or concluding "regression — needs a
    product fix", present the verdict + evidence and get EXPLICIT user approval.
    Plain rewrites that preserve coverage do not need the gate, but their green
    re-run (F6) is mandatory.

  STEP F6: Verify + lint
    - Re-run every TOUCHED spec on fresh services, `--retries=0 --workers=1`,
      until green. Re-run the whole affected spec FILE too, to catch sibling
      breakage your edit may have introduced.
    - `bunx tsc --noEmit -p tsconfig.json` in `platform/e2e`; then prettier +
      eslint the changed files. Platform uses Prettier + ESLint — NEVER Biome.
    - e2e-only changes need no changeset (only `backend/`+`frontend/` source do).

  STEP F7: Report
    ```
    ## E2E Fix — <timestamp>

    ### Triage
    | # | Spec:line | Test | Verdict | Root cause (PR/commit) | Action |
    | 1 | <spec.ts:line> | <name> | STALE | #<pr> relabelled X→Y | rewrote selector |
    | 2 | <spec.ts:line> | <name> | REGRESSION | suspected #<pr> | REPORTED — needs product fix |

    ### Regressions found (NOT fixed here — left red on purpose)
    - <test> — <user-visible symptom> — suspected <commit/PR> — recommend <product fix>

    ### Verification
    - <N> rewritten specs green on fresh services (--retries=0); affected files green

    ### Coverage / follow-ups
    - <deleted specs + the gap left>, <unrelated flakes left alone>, <skips + reason>
    ```

  EMIT   [e2e-platform] fix_complete stale=<N> regressions=<N> rewritten=<N> deleted=<N>

  STEP F8: Cleanup services we auto-started (if any)
    Same dispatch as run mode's STEP R5 — refer to that step's table. Print the
    script's output verbatim; do NOT escalate beyond the script's pkill patterns.
    EMIT   [e2e-platform] cleanup_complete

# ── Mode: add ────────────────────────────────────────────────────

IF mode = add

  STEP A1: Analyze changes — TWO sources; pick by what's actually present

    (a) **Branch-diff mode** (default — "add tests for my unmerged PR"):
    ```bash
    git diff main...HEAD --name-only        # full branch diff
    git diff --name-only                    # unstaged
    git diff --staged --name-only           # staged
    ```

    (b) **Gap-discovery mode** — use when the branch diff above is EMPTY (you're on
    `main`, or the features already merged) OR the user asks "what recent features
    need e2e?" / names merged PRs. **Do NOT conclude "nothing to add" just because
    `main...HEAD` is empty** — an empty branch diff means there's no *unmerged* work,
    NOT that coverage is complete. Instead, discover recently-merged features and
    cross-reference them against existing specs to find gaps:
    ```bash
    # 1. enumerate recently-merged feature commits (tune the window / scope; if the
    #    window is dry, widen it — `--since="6 weeks ago"` — or report "no recent
    #    feat commits found" rather than silently concluding there are no gaps)
    git -C platform log origin/main --since="2 weeks ago" --pretty='%h %ad %s' --date=short \
      | grep -iE 'feat|^[0-9a-f]+ [0-9-]+ (Add|Support)'
    # 2. read each candidate feature's surface (selectors, routes, testids, PR #)
    git -C platform show <sha>
    # 3. inventory current specs + the specs the team recently ADDED
    git ls-tree -r --name-only origin/main -- e2e/specs | grep '\.spec\.ts$'
    git -C platform log origin/main --since="2 weeks ago" --diff-filter=A --name-only --pretty=format: -- e2e/specs
    ```
    Then grep `platform/e2e/specs/**` for each feature's keywords / PR number — the
    features with NO matching spec are the gaps to propose. (The team often adds
    specs alongside a feature wave, so many recent features are already covered —
    surface only the genuine gaps.)

    IF a `scope`/feature input was provided: focus on that area in either mode
    (filter the diff, or `git show` the named PR commits directly).

    Categorize each changed file (or each merged feature's touched files):

    | Category | Path pattern | Test shape to consider |
    |---|---|---|
    | Frontend route | `platform/frontend/src/routes/` | Navigation, content assertion, impersonation gate |
    | Frontend page | `platform/frontend/src/pages/` | Interaction flow, form fill, assertion on API response |
    | Frontend component | `platform/frontend/src/components/` | Typically covered by Storybook — only add e2e if the component changes an existing journey |
    | tRPC route | `platform/backend/libs/router-trpc/src/routes/` | End-to-end via the UI — don't test routes in isolation (that's `test-integration`) |
    | Backend view | `platform/backend/libs/view/src/` | Assert the UI displays view output correctly |
    | Seed data | `platform/backend/test-data.json` | May require updating existing specs; add new data only if needed for a new test |
    | Schema | `platform/backend/postxl-schema.json`, `schema.prisma` | Wide-reaching — consult user on what to cover |

    Read each file to extract:
    - URL patterns (`/{orgSlug}/...`)
    - testIds (`data-test-id`)
    - Interactive elements (buttons by `name`, placeholders, headings)
    - Keywords that trigger mock events (for workspace-chat changes)

    Read every existing spec under `platform/e2e/specs/` and build a coverage map: what's currently covered, where overlap would be, which specs to edit vs. create.

  STEP A2: Build proposal
    Present a table — each row is one proposed test (or edit):

    ```
    ## Proposed e2e plan

    | # | What it verifies | Target spec | Pattern | Selectors to live-verify | Session mode |
    | 1 | /acme/billing page loads with monthly total | NEW billing.spec.ts | navigation + content | heading "Billing", text /\$\d+\.\d{2}/ | mock |
    | 2 | Invite email field accepts + rejects formats | EDIT invite-lifecycle.spec.ts | form interaction | input[type=email], "Send Invite" btn | mock |
    | 3 | Deleting a session fires confirmation modal | NEW session-delete.spec.ts | confirmation modal | "Delete session" btn, dialog "Are you sure" | local |

    ### Skipped (already covered)
    - Teams CRUD → covered by `specs/multi-user/teams.spec.ts` (#1)
    - Role filtering → covered by `specs/multi-user/sharing.spec.ts` (#3)

    ### Duplication warnings
    - Item 2 overlaps with `invite-lifecycle.spec.ts` test #2 — recommend EDIT rather than a new spec.

    ### Session mode reasoning
    - Item 3 uses `local` because it exercises session lifecycle; others use `mock` for speed.

    ### Impersonation plan
    - Item 1: user-bu-1 (project owner, sees billing)
    - Item 2: user-it-1 (ItAdmin, has Invite button)
    - Item 3: user-bu-1 + pxl-mock-roles=User (non-admin path)
    ```

    Keep it TIGHT. Default to fewer tests, not more. For each proposed test, ask:
      - Does an existing spec already cover this flow? → SKIP, note in "Skipped" section.
      - Is this a component-level concern better tested by Storybook? → SKIP, note why.
      - Is this a backend concern better tested by `test-integration`? → SKIP, note why.

    **Feasibility gate — DROP (don't force) anything in these categories; note in
    the proposal that it's unit-tested instead.** The stateless harness + Playwright
    make several feature classes infeasible or irreducibly flaky:
      - **Agent / LLM-driven** (needs a live assistant/agent turn → container
        cold-wake + a model response): @mention routing, chat replies, assistant
        panels, onboarding *conversation*, anything asserting an echo round-trip.
        Cold-wake is the #1 flakiness source. Assert **routing / URL / shell render
        that settles BEFORE the wake**, never the reply.
      - **Unreachable hydrated state** (a *stateless-seed* limitation, not a
        categorical impossibility): seed sessions are Closed/Hibernated with no live
        writable mount, so resource-tree files, file-viewer edit, HTML preview, and
        "list the uploaded file" can't be reached deterministically here. (A future
        stateful-mode harness could cover these.)
      - **Browser-intercepted input**: `Ctrl+Shift+W`, `Ctrl+P` (Chromium swallows
        them); HTML5 native drag-and-drop (Playwright can't drive it reliably).
      - **Visual-only / external**: avatars, spinners, OG cards, native Web Share
        sheet, SharePoint/Office embeds.
    For a feature that's mostly in these classes, propose only its deterministic
    UI slice (e.g. the route renders, a control is present) and say what's deferred.

    **Determinism guards — keep these in mind here, apply them when writing in STEP A5:**
      - After a mutation whose `waitForResponse` returns 200, a follow-up reload may
        still not see the write (SSE broadcast + commit run in parallel) — add
        `await page.waitForTimeout(500)` before the reload (per E2E-GUIDE).
      - Prefer `locator(...).filter({ has })` row scoping over `xpath=..` parent hops.
      - Live-verify selectors via chrome-devtools — but it hits the *shared* `:4000`
        backend (state may be polluted), NOT a clean per-spec backend.

    **Onboarding/assistant gotcha:** e2e users default `onboardingCompleted: true`
    (`NODE_ENV === 'test'`), so the first-run welcome-session is suppressed. To test
    it, flip a user unboarded via the PlatformAdmin `userAdmin.setOnboardingCompleted`
    procedure (or `resetAssistantOnboarding`) in `beforeEach`. Avoid non-Latin-1
    glyphs (e.g. `✕`) in test titles — they break the `x-test-test-title` HTTP header.

  STEP A3: User approval gate
    > "Review this plan. Reply with one of:
    >   approve            — proceed with all items
    >   approve N,M        — proceed with only these items (drop the rest)
    >   narrow: <text>     — narrow the diff scope and re-analyze
    >   skip N: <reason>   — drop item N from the plan
    >   edit N: <desc>     — change what item N covers (re-propose)
    >   cancel             — abort"

    Loop until `approve` / `approve N,M` / `cancel`.

    DO NOT write any file until explicit approval.

  STEP A4: Live UI inspection (skip if live_inspect=false)
    For each approved item:
      - Use chrome-devtools MCP (`mcp__chrome-devtools__*` tools) to navigate the relevant page with the impersonation set via `evaluate_script`.
      - Take a snapshot (`take_snapshot`).
      - Verify each proposed selector actually exists in the snapshot.
      - Record the actual accessible names / roles.

    IF any selector doesn't resolve:
      Report the drift to the user with alternatives from the snapshot:
        > "Item N: expected `heading 'Billing'`, but current DOM has `heading 'Billing Overview'` (level=1).
        >  Use the new wording? (yes/no/cancel item N)"

    The goal is to avoid the stale-heading flakiness pattern — if the UI moved, we catch it before writing a spec that would immediately fail.

  STEP A5: Write specs
    For each approved item:

    1. **Pick a template** from existing specs that most closely matches the pattern:
       - Navigation + content → `specs/dashboard.spec.ts`
       - Workspace/chat → `specs/message-flow.spec.ts`
       - Admin tRPC + UI mix → `specs/multi-user/invite-lifecycle.spec.ts`
       - Session lifecycle → `specs/session-lifecycle.spec.ts`
       - Simple API-only → `specs/multi-user/teams.spec.ts`

    2. **Apply scaffolding rules** (from platform/e2e/CLAUDE.md):
       - Shared `page` fixture. Never `browser.newContext()`.
       - `startBackend('file', testInfo)` + `stopBackend` iff the spec mutates state, creates sessions, or needs isolation. Skip for read-only specs.
       - Impersonation via `page.addInitScript` setting `pxl-mock-user` (and `pxl-mock-roles` if non-admin).
       - Org-scoped slug URLs.
       - `getByRole` over `getByText`. Disambiguate with `.first()` / `{ exact: true }` when needed.
       - Use the selectors from Step A4 (the live-verified ones), not the ones from the initial proposal.

    3. **Guard against known flakiness patterns** (from CLAUDE.md):
       - Ambiguous selector → anchor on page-body heading, not sidebar link
       - Persistence race → small wait after confirming event, before reload
       - Cache hydration race → wait for a known list member first, then assert absence of target

    4. **Raw fetches**: if the spec uses raw `fetch()` for test-only endpoints AND `startBackend('file')`, pass `getTestHeaders(testInfo)` so the fetch hits the same per-spec backend.

  STEP A6: Verify the new specs
    Run ONLY the newly-written / edited specs (not the full suite) for faster feedback:
    ```bash
    cd platform/e2e
    bunx playwright test <new-spec-paths> --retries=0 --workers=1 --reporter=dot --max-failures=3 \
      2>&1 | tail -60
    ```
    Same reasoning as Step R1 for the flags. If a new spec fails and the dot output is too terse to diagnose, re-run THAT one spec with `--reporter=list` and no tail.

    IF failures:
      Apply the same classification as `run` mode's Step R2. Auto-recover infra failures and retry. For spec-design failures, report verbatim — do not modify assertions to make them pass.

  STEP A7: Report
    ```
    ## E2E Add — <timestamp>

    ### Analysis
    Diff scope: <main…HEAD | user scope>
    Files changed: <N>, categorized:
    - frontend routes: <list>
    - frontend pages: <list>
    - tRPC routes: <list>
    - ...

    ### Plan (approved)
    | # | What | Spec | Pattern |
    | 1 | ... | NEW billing.spec.ts | navigation + content |

    ### Live UI verification
    | # | Selector proposed | Actual | Action |
    | 1 | heading "Billing" | heading "Billing Overview" | Updated selector |

    ### Written
    - NEW platform/e2e/specs/billing.spec.ts (3 tests)
    - EDIT platform/e2e/specs/multi-user/invite-lifecycle.spec.ts (2 tests added)

    ### Results
    - 5 new tests → 5 passed / 0 failed

    ### Coverage notes
    - What's NOT covered (user's decision): <items skipped in approval step>
    - Follow-ups: <stateful-mode needed for X, Storybook story missing for Y>
    ```

  EMIT   [e2e-platform] add_complete specs_written=<N> tests_added=<N>

  STEP A8: Cleanup services we auto-started (if any)
    Same dispatch as run mode's STEP R5 — refer to that step's table. Print the script's output verbatim, do NOT escalate beyond the script's pkill patterns.
    EMIT   [e2e-platform] cleanup_complete

# ── End modes ────────────────────────────────────────────────────

CHECKLIST
  - [ ] Pre-flight ran (all modes) — services confirmed FRESH (stale guards green)
  - [ ] (add) Plan was presented BEFORE any file was written
  - [ ] (add) User explicitly approved the plan
  - [ ] (add) Live UI inspection caught selector drift before write (when live_inspect=true)
  - [ ] (add) New specs use shared `page` fixture + org-scoped slug URLs + scaffolding rules
  - [ ] (fix) EVERY failure classified stale-vs-regression WITH evidence before any edit
  - [ ] (fix) "Stale" verdicts proved the feature still works (not just a moved selector)
  - [ ] (fix) Regressions reported to the human + left red — never edited/skipped to pass
  - [ ] (fix) Deletions / `.skip` got explicit user approval; rewrites preserved/tightened assertions
  - [ ] (fix) Touched specs AND their full files re-run green on fresh services (--retries=0)
  - [ ] (run) Failures categorized, infra auto-recovered, spec-design failures reported not auto-fixed
  - [ ] Report includes follow-ups + coverage notes
  - [ ] (all) Cleanup ran ONLY for services we auto-started — pre-existing services left untouched (Step R5 / F8 / A8 dispatch table)

---

## Integration

- **Called by:** human or `implement` skill (after a user-facing feature lands)
- **Calls:** `platform/scripts/e2e.sh` (auto-start FE+BE when missing; selective `--stop-backend` / `--stop-frontend` / `--stop` for end-of-mode cleanup of only services this skill put up), chrome-devtools MCP (via `mcp__chrome-devtools__*` tools, for live inspection), `sync-seed-data.js` via `bun run sync-seed`
- **Delegates to:** `verify-ui` (when user wants visual coverage alongside specs), `test-e2e` (when user asks about scaffolding a fresh e2e setup for a different package), **the human** (`fix` mode escalates a confirmed/suspected regression instead of editing the test to pass)
- **Reads:** `platform/e2e/CLAUDE.md`, `platform/e2e/README.md`, `platform/e2e/E2E-GUIDE.md`, existing specs under `platform/e2e/specs/**`

## Known limitations

- **Stateful mode is out of scope.** Current suite is all `e2e:stateless`. If a PR needs stateful coverage, note it as a follow-up in the report; don't attempt to scaffold a parallel stateful flow.
- **Flow-execution specs stay skipped.** `specs/flow-execution.spec.ts` requires docker + a real LLM — see the spec's own comment for the prerequisite checklist. `mode=run` will not try to un-skip them.
- **Does not scaffold new e2e harnesses.** For a brand-new Playwright setup (new forge app, new CLI package), use `test-e2e` instead.

## Mistakes to avoid

| Mistake | Instead |
|---|---|
| Running `bun run e2e:stateless` against a stale `dist/` | Backend crashes at boot — `Cannot find module .../e2e.js` (missing trampoline) or `Cannot find module '@skaile/...'` (orphaned output from a package rename). Use `./scripts/e2e.sh` (it clean-rebuilds when `dist/` is stale), or run `cd platform/backend && rm -rf dist && bun run build` first. A plain incremental `bun run build` does not prune orphaned files. |
| Writing specs without live-verifying selectors | Use chrome-devtools MCP first; UI drifts faster than specs |
| `browser.newContext()` for impersonation | `page.addInitScript` on the shared `page` fixture (coverage depends on it) |
| Asserting on `getByText('Dashboard')` etc. | Use `getByRole('heading', { name: ... })` or enabled interactive elements — sidebar links match text before page body loads |
| Hardcoding `proj-1` / `proj-2` / `proj-3` in URLs | Use slugs: `inventory-tracker`, `hr-onboarding`, `expense-reports` |
| Assuming `PlatformAdmin` sees all projects | Default mock user (`test`) owns no seed projects. Use `user-bu-1` / `user-bu-2` to see content. |
| Skipping the approval gate in `add` mode | The gate is the entire point — it prevents spec sprawl |
| Rewriting a failing spec to green without triage | `mode=fix` STEP F2: classify stale-vs-regression with evidence FIRST; a moved selector and a real bug look identical until you check the feature works |
| Weakening an assertion / adding a blind wait to pass | Never make a test lie. Rewrite to the new shape (keep/tighten the assertion), or report the regression |
| `.skip`-ing a test you couldn't get working | That's a cop-out. Delete-and-note the gap (feature gone) or fix it; `.skip` is only for temporarily-unverifiable-but-returning behaviour, with a reason |
| Triaging against a stale backend/frontend | Re-run Step 0's stale guards; a stale bundle fakes both failures AND passes |
