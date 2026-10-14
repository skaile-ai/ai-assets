# Ship — platform profile (`skaile-ai/platform`)

Facts that apply ONLY when the target repo is platform. `SKILL.md` stays repo-agnostic;
it points here from STEP 0b. Every item below already cost a real ship run. When this
file and the platform repo's own `CLAUDE.md` disagree, the repo wins — fix this file.

## Changesets (STEP 11)

Every PR that touches `backend/` or `frontend/` needs a changeset. Use the package name
from the `package.json` of the half you changed:

| You changed | Changeset line |
|---|---|
| anything under `backend/` (any lib, any app) | `'@postxl/skaile-platform-api': patch` |
| anything under `frontend/` | `'@postxl/skaile-platform-frontend': patch` |
| docs / CI only | an empty changeset (`bun run changeset --empty`) |

Backend libs have NO `package.json` of their own. Names like `@postxl/session` or
`@postxl/router-trpc` are path aliases, not packages — a changeset that names one fails
`changeset-check`. The `whats-new*` markers that a user-visible change needs are in
`.changeset/README.md` of the platform repo.

## Pre-push checks (STEP 11)

In addition to lint and the affected tests, run the invariant suites. They are
source scans that catch rule breaks the affected tests do not reach:

```bash
cd backend  && bun run test:jest -- --testPathPatterns invariant
cd frontend && bun run test:unit -- invariant
```

## Boot verification (STEP 11, any backend change)

`build`, `test:types` and `test:jest` all pass on broken NestJS DI. Booting the app is the
only local check that resolves the whole graph. Each boot attempt in a ship run has to
be isolated, because parallel ship runs on one machine share ports and process names.

- **Give each run its own `API_PORT`.** `main.ts` checks the port BEFORE
  `NestFactory.create` and exits 1 with `Port <n> is already in use!`. A port collision
  therefore looks exactly like a DI break (an early exit, no `API is running`). Read
  the log for that line before you blame the diff.
- **A fresh worktree has no `.env` files.** Copy `backend/apps/api/.env.example` to
  `backend/apps/api/.env` (and the root `.env.example` to `.env`), then set `API_PORT`
  in the copy. `PORT` is ignored.
- **Boot:** `cd backend && bun run dev`, run in the background with its output in a log
  file. Success = the log contains `API is running`. Alternative without the watcher:
  `bun run build` and then `backend/scripts/boot-smoke.sh` (what CI runs).
- **macOS has no `timeout` binary.** Poll the log in a bounded loop (for example 120
  iterations of `sleep 1`) instead of `timeout 120 …`.
- **Kill by PID, never by pattern.** Get the PIDs from your own port only:
  `lsof -ti :<API_PORT> | xargs kill`. NEVER `pkill -f "nest start.*--watch"` or any
  other pattern kill — one run's pattern kill stopped the backend of every sibling run.
- **Clean up the process chain.** `bun run dev` starts `dotenvx → nest start --watch`.
  Killing the listener can leave that pair alive, and every attempt leaves one more. Keep
  the PID of the `bun run dev` you started (`$!`) and kill its whole tree by parent PID,
  never by pattern:

  ```bash
  killtree() { for c in $(pgrep -P "$1"); do killtree "$c"; done; kill "$1" 2>/dev/null; }
  killtree "$DEV_PID"
  lsof -ti :"$API_PORT"   # must print nothing
  ```

## Known flaky CI signatures (Phase 12 classification)

These are documented in the platform `CLAUDE.md` (Troubleshooting). Classify them as
UNRELATED flakes. Do NOT chase the suite the job names, do NOT re-run it alone to
"confirm", and do NOT add a retry to the code.

| Signature | What it is |
|---|---|
| jest `signal=SIGSEGV`, or `N failed` suites with ZERO failed tests | a jest worker died in V8 GC; the named suite is only the one the worker held |
| durable-Postgres lane: `1 failed` suite, zero failed tests, `terminating connection due to administrator command` at `Client.idleListener` | a migration test's teardown races `pool.end()`; the victim suite changes between runs |
| `tsc` / `eslint` exit 134, `FATAL ERROR: … heap out of memory`, no diagnostics | a V8 out-of-memory, not a type or lint error (frontend `tsc` is the usual one) |

Discriminate before you call something a flake. It is a flake only when BOTH hold:

1. The failing file is NOT in this PR's diff.
2. A sibling PR on the same base commit passed the same job (`gh pr list --state all`
   plus `gh pr checks <n>`, or the same job on `main` at that commit).

If either fails, treat it as RELATED and fix it.

## Merged is not deployed (STEP 3b, STEP 16)

Nothing deploys on a plain merge to `main`. The release and the deploy are one event:

- **Platform change:** merging the changesets Version PR (`chore: version platform`,
  branch `changeset-release/main`) changes the version, and `version.yml` then
  fast-forwards `deploy`. Jenkins watches `deploy`. Never merge `main` into `deploy`
  by hand — it races that job.
- **`@skaile/workspaces` change** takes three hops before it can reach production:
  (1) merge the workspaces PR and then its Version Packages PR, which publishes to npm;
  (2) merge a platform PR that bumps the pin in `backend/package.json` and
  `frontend/package.json` (and rebuilds the agent image if the runner changed);
  (3) merge the platform Version PR.

So the final report says `Deployed: no — waits for the next platform Version PR` (or the
workspaces hop it waits on), never just `merged`.
