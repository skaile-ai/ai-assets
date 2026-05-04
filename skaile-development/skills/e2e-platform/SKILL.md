---
name: 'e2e-platform'
description: 'Run or extend the Skaile platform e2e suite. Two modes: `run` executes the existing suite with pre-flight checks (stale-service guard for post-pull stale bundles, stale-deps guard for lockfile drift, seed sync, proxy slot health), failure-mode diagnosis, and auto-recovery (kill stale skaile-serve subprocesses, sync seed data, restart stuck proxy slots); `add` analyzes a git diff, proposes a test plan, waits for explicit user approval, lives-inspects the UI via chrome-devtools MCP, then writes new specs against the existing `platform/e2e/` harness. Use when the user says "run e2e tests", "fix e2e failures", "add e2e coverage for <feature>", "write e2e tests for my PR", or similar.'
metadata:
  version: '1.1.0'
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
        description: 'Existing platform e2e harness must be in place (this skill extends it, does not scaffold it — see `test-e2e` for initial scaffolding).'
      - path: 'platform/e2e/CLAUDE.md'
        gate: soft
        description: 'Recommended knowledge source. The skill references its failure-mode table and scaffolding rules.'
  user_inputs:
    dialog:
      - id: 'mode'
        label: 'Mode'
        type: 'select'
        options: ['run', 'add']
        required: true
        hint: "'run' executes the suite with auto-recovery; 'add' analyzes a diff and proposes new specs with a user approval gate"
      - id: 'scope'
        label: 'Scope'
        type: 'text'
        required: false
        hint: "'run' mode: spec file path or test name filter (default: full suite). 'add' mode: feature area to focus analysis on (default: git diff main…HEAD)"
      - id: 'session_mode_hint'
        label: 'Session mode'
        type: 'select'
        options: ['auto', 'mock', 'local']
        required: false
        default: 'auto'
        hint: "'add' mode only. 'auto' picks based on change kind (local for session-lifecycle or flow tests, mock otherwise)"
      - id: 'live_inspect'
        label: 'Live-inspect UI before writing'
        type: 'select'
        options: ['true', 'false']
        required: false
        default: 'true'
        hint: "'add' mode only. Uses chrome-devtools MCP to verify selectors against the actual DOM before committing them to a spec. Disable only if the MCP is unavailable."
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
- "fix the failing e2e tests" — `mode=run` (auto-recovery handles infra-level failures)
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

WRITES
  (add mode)
    platform/e2e/specs/**/<new-or-edited>.spec.ts

MUST   read platform/e2e/CLAUDE.md before executing or writing anything
MUST   run pre-flight before every invocation (see Step 0)
MUST   use the shared `page` fixture from test-fixtures.ts (NOT `browser.newContext()`)
MUST   use org-scoped slug-based URLs (`/acme/projects/<slug>/main/...`)
MUST   use `page.addInitScript` for impersonation (pxl-mock-user + optional pxl-mock-roles)
MUST   (add mode) gate writing behind explicit user approval of the proposed plan
MUST   (add mode) live-inspect UI via chrome-devtools MCP before committing selectors (when live_inspect=true)
MUST   delegate execution to `bunx playwright test` / the existing npm scripts — do not re-invent runners
MUST   follow the stale-service / stale-deps recovery recipes verbatim and in order — even if an earlier check reported "clean", later steps may still be needed (e.g. workspace topology changes evade the lockfile-mtime check). Do NOT skip steps based on side checks.
NEVER  write to `platform/e2e/playwright.config.ts` or global fixtures — those belong to `test-e2e`
NEVER  auto-fix assertions to make a test pass — report the drift for user review
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
     Run the bundled service helper. It is idempotent — only starts what's missing (backend in `e2e:stateless` mode, frontend in `dev:noAuth` mode), reuses already-running services, detaches via `nohup` so services survive after the script exits, and writes logs to `platform/.e2e-backend.log` / `platform/.e2e-frontend.log`. It also runs `bun run build` in `platform/backend` first when the e2e entry-point trampoline (`dist/apps/api/apps/api/src/e2e.js`) is missing **or stale relative to `tsconfig.json`** — `bun run e2e:stateless` does not generate or regenerate it on its own (only `bun run build` does), so without this step the backend either crashes with `Cannot find module .../dist/apps/api/apps/api/src/e2e.js` (missing) or with an `ERR_MODULE_NOT_FOUND` for a `@skaile/*` workspace package whose path alias was added after the trampoline was last built (stale — the runtime falls through to the workspace's TS source via `package.json` `exports` and dies on a `.js`-extension relative import that has no compiled sibling):
     ```bash
     cd platform && ./scripts/e2e.sh
     ```
     The script blocks until both ports listen (timeout 60s per service) and prints one line per service. After it returns, re-verify with the same `lsof` check.
     IF a port is still down after the script ran: read the last ~30 lines of the relevant log, report the startup error verbatim to the user, STOP. Do not retry blindly — startup failures usually mean missing migrations, port conflicts with another app, or a code bug, and looping won't fix any of those.
   ELSE: both up — continue.

   NOTE: This is *auto-start* (spawn what's missing). It is **not** *auto-fix stale* (kill+restart already-running services to clear out-of-date code) — that's Step 3 below, more destructive (drops in-flight state, may break other dev work), and still requires explicit user authorization.

   NOTE: If you ever need to bring up the backend manually (bypassing `./scripts/e2e.sh`), run `bun run build` in `platform/backend` first whenever the trampoline is absent **or older than `tsconfig.json`** — `bun run e2e:stateless` itself never produces or refreshes it. After a pull that adds `@skaile/*` path aliases, the trampoline file still exists but is stale, and the existence check is not enough.

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
       >   4. cd platform/backend && bun run build && bun run e2e:stateless    ← `bun run build` regenerates the trampoline; `nest start --watch` does not
       >   5. cd platform/frontend && bun run build && bun run preview --port 3000
       > Then re-invoke this skill."
     STOP. Auto-start of *missing* services (Step 2) is fine; killing and restarting *already-running* services to clear staleness is destructive (drops in-flight requests, may interrupt parallel dev work) and requires explicit user authorization.

4. **Stale-trampoline check (post-pull guard #2).** A trampoline file regenerated by `bun run build` registers the runtime tsconfig-paths registry for `@skaile/*` workspace aliases. `nest start --watch` re-emits the per-file `.js` outputs but does **not** rebuild the trampoline, so when a pull adds new path aliases (typical for `@skaile/agent-connectors/*` Phase 7 work) the trampoline silently goes stale: the file exists, the existence check passes, but the in-memory alias map at process start is missing entries. The runtime falls through to the workspace symlink → `package.json` `exports` pointing at TS source → `ERR_MODULE_NOT_FOUND` on a `.js`-extension relative import inside that source (e.g. `Cannot find module .../agent-framework/connectors/src/port-pool.js`).

   Compare the trampoline file's mtime against `tsconfig.json`:
   ```bash
   TRAMP=platform/backend/dist/apps/api/apps/api/src/e2e.js
   TSCONF=platform/backend/tsconfig.json
   [ -f "$TRAMP" ] && [ "$TSCONF" -nt "$TRAMP" ] && echo "STALE_TRAMPOLINE"
   [ -f "$TRAMP" ] || echo "MISSING_TRAMPOLINE"
   ```
   IF `STALE_TRAMPOLINE` or `MISSING_TRAMPOLINE`:
     This is non-destructive — `bun run build` only writes to `dist/`, no service kill or in-flight state loss. Run it without prompting for authorization, then proceed:
     ```bash
     cd platform/backend && bun run build
     ```
     If services were already running with the stale trampoline (the dangerous case the stale-service check at Step 3 misses when the process started recently but loaded a stale registry), the running backend still has the stale alias map in memory — fixing the file isn't enough. After `bun run build`, also restart the backend (treat as a stale-service event: route through the recovery recipe in Step 3 with explicit user authorization, since restarting drops in-flight state).

5. **Stale-deps check.** Two independent failure modes — both produce the same opaque module-resolution errors at runtime, so check both:

   (a) Lockfile newer than `node_modules` (typical post-pull symptom):
   ```bash
   LOCK=skaile-dev/bun.lockb
   MARK=skaile-dev/node_modules/.modules.yaml
   [ -f "$LOCK" ] && [ -f "$MARK" ] && [ "$LOCK" -nt "$MARK" ] && echo STALE_DEPS
   ```

   (b) Critical workspace symlinks missing — the mtime check above cannot detect the case where a package was relocated within the workspace tree (e.g. `theme/` → `agent-framework/theme/`). Neither file's mtime moves, but `node_modules/@skaile/*` is missing entirely. Verify directly:
   ```bash
   # If the @skaile namespace is gone, the workspace is catastrophically out of sync.
   [ -d skaile-dev/node_modules/@skaile ] || echo MISSING_SKAILE_NS
   # Spot-check a few critical packages that platform/frontend imports.
   for pkg in @skaile/theme @skaile/agent-core @skaile/agent-types; do
     [ -e "skaile-dev/node_modules/$pkg" ] || echo "MISSING_$pkg"
   done
   ```
   (Path-adjust if running from a subfolder.)

   IF `STALE_DEPS` OR any `MISSING_*`:
     Report:
       > "Dependencies out of sync — run `bun install` from the skaile-dev root before tests, or this run will surface as opaque module-resolution errors (e.g. Vite '[Internal server error] Can't resolve @skaile/<pkg>'). The mtime check (a) and the symlink-existence check (b) catch different failure modes — either firing requires `bun install`."
     STOP unless the user has already authorized auto-install elsewhere. Do not run `bun install` without explicit permission.

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
    | UI drift: `heading "Foo" not found` | No | Report — spec needs updating; suggest `mode=add` to re-propose |
    | Persistence race / ambiguous selector / `toHaveCount(0)` false-green | No | Report with diagnosis category; suggest `mode=add` or manual fix |
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
    | 1 | <spec.ts:line> | <test name> | UI drift | Run `e2e-platform mode=add` to re-propose selectors |
    | 2 | <spec.ts:line> | <test name> | Persistence race | Add `waitForTimeout(500)` after confirming event, before reload |

    ### Recoveries applied
    - Killed N stale `skaile serve` subprocesses
    - Ran `sync-seed` (test-data.json was 2 mins newer than dist)
    - Restarted e2e proxy (had 2 stuck "stopping" slots)

    ### Next steps
    - <concrete suggestions>
    ```

  EMIT   [e2e-platform] run_complete passing=<N> failing=<N>

# ── Mode: add ────────────────────────────────────────────────────

IF mode = add

  STEP A1: Analyze changes
    ```bash
    git diff main...HEAD --name-only        # full branch diff
    git diff --name-only                    # unstaged
    git diff --staged --name-only           # staged
    ```
    IF `scope` input was provided: focus analysis on that area (filter the diff, or read named files directly).

    Categorize each changed file:

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

# ── End modes ────────────────────────────────────────────────────

CHECKLIST
  - [ ] Pre-flight ran (both modes)
  - [ ] (add) Plan was presented BEFORE any file was written
  - [ ] (add) User explicitly approved the plan
  - [ ] (add) Live UI inspection caught selector drift before write (when live_inspect=true)
  - [ ] (add) New specs use shared `page` fixture + org-scoped slug URLs + scaffolding rules
  - [ ] (both) Failures categorized, infra auto-recovered, spec-design failures reported not auto-fixed
  - [ ] Report includes follow-ups + coverage notes

---

## Integration

- **Called by:** human or `implement` skill (after a user-facing feature lands)
- **Calls:** `platform/scripts/e2e.sh` (auto-start FE+BE when missing, idempotent), chrome-devtools MCP (via `mcp__chrome-devtools__*` tools, for live inspection), `sync-seed-data.js` via `bun run sync-seed`
- **Delegates to:** `verify-ui` (when user wants visual coverage alongside specs), `test-e2e` (when user asks about scaffolding a fresh e2e setup for a different package)
- **Reads:** `platform/e2e/CLAUDE.md`, `platform/e2e/README.md`, `platform/e2e/E2E-GUIDE.md`, existing specs under `platform/e2e/specs/**`

## Known limitations

- **Stateful mode is out of scope.** Current suite is all `e2e:stateless`. If a PR needs stateful coverage, note it as a follow-up in the report; don't attempt to scaffold a parallel stateful flow.
- **Flow-execution specs stay skipped.** `specs/flow-execution.spec.ts` requires docker + a real LLM — see the spec's own comment for the prerequisite checklist. `mode=run` will not try to un-skip them.
- **Does not scaffold new e2e harnesses.** For a brand-new Playwright setup (new forge app, new CLI package), use `test-e2e` instead.

## Mistakes to avoid

| Mistake | Instead |
|---|---|
| Running `bun run e2e:stateless` directly when the trampoline is missing | Backend crashes at boot with `Cannot find module .../dist/apps/api/apps/api/src/e2e.js`. Use `./scripts/e2e.sh` (it runs `bun run build` once when the trampoline is absent), or run `cd platform/backend && bun run build` first. |
| Writing specs without live-verifying selectors | Use chrome-devtools MCP first; UI drifts faster than specs |
| `browser.newContext()` for impersonation | `page.addInitScript` on the shared `page` fixture (coverage depends on it) |
| Asserting on `getByText('Dashboard')` etc. | Use `getByRole('heading', { name: ... })` or enabled interactive elements — sidebar links match text before page body loads |
| Hardcoding `proj-1` / `proj-2` / `proj-3` in URLs | Use slugs: `inventory-tracker`, `hr-onboarding`, `expense-reports` |
| Assuming `PlatformAdmin` sees all projects | Default mock user (`test`) owns no seed projects. Use `user-bu-1` / `user-bu-2` to see content. |
| Skipping the approval gate in `add` mode | The gate is the entire point — it prevents spec sprawl |
| Auto-fixing a failing assertion to make it pass | Report the drift; the test is telling you something real |
