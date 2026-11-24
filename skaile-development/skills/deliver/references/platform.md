# Deliver: platform profile (`skaile-ai/platform`)

Facts that apply only when the target repo is platform. `SKILL.md` stays repo-agnostic
and points here. Every item below already cost a real run. When this file and the
platform repo's own `AGENTS.md` disagree, the repo wins: fix this file.

## Queue labels (step 3)

| Label | Meaning |
|---|---|
| `skaile-queued` | Enqueued in the auto-ship run group |
| `ship:local` | Queued for the local ship loop |

Never remove one. `skaile-queued` is also the filter of the reconcile cron: every 30
minutes it enqueues each open issue that lacks the label, so a removal queues the issue
again. Signs that another runner already works on the issue: an open PR for it, the
label `ship:local-claimed` or `status:shipping`, or another worktree for it.

## A fresh worktree (step 4)

Do this before the first test, not only before the boot:

```bash
cp .env.example .env
cp backend/apps/api/.env.example backend/apps/api/.env
cp frontend/.env.example frontend/.env
bun install
```

Without the `.env` files a test fails with a config ZodError that looks like a real
failure. Set `API_PORT` in `backend/apps/api/.env` before a boot (see below).

## Changesets (step 6)

Every PR that touches `backend/` or `frontend/` needs a changeset. Use the package name
from the `package.json` of the half you changed:

| You changed | Changeset line |
|---|---|
| anything under `backend/` (any lib, any app) | `'@postxl/skaile-platform-api': patch` |
| anything under `frontend/` | `'@postxl/skaile-platform-frontend': patch` |
| docs / CI only | an empty changeset (`bun run changeset --empty`) |

Backend libs have no `package.json` of their own. Names like `@postxl/session` are path
aliases, not packages: a changeset that names one fails `changeset-check`. The
`whats-new*` markers are described in `.changeset/README.md` of the platform repo.

## Docs impact (step 8, before the commit)

Docs are part of the change. Decide `docs_impact` before the commit:

- **user-visible** when the changeset carries a `whats-new*` marker other than
  `whats-new-fix:`, or the diff adds, renames, removes or changes the documented
  behaviour of a `platform.*` capability, a connector, a provider, a UI surface, a role
  or a permission. When unsure, it is user-visible.
- **none** for internal refactors, tests, CI, infrastructure, and fixes that restore
  behaviour the docs already describe.

For a user-visible change, in the same PR:

1. `features/SKAILE-PLATFORM-CAPABILITIES.md` (business voice, bump `updated:`) and the
   matching doc under `features/<NN-section>/`.
2. The platform guide: the matching file under `features/platform-guide/`, then
   `bun run guide:embed`, and stage the regenerated
   `backend/libs/library/src/seed/platform-guide-skill-content.ts`. Never hand-edit the
   embed. Real UI labels in bold; never list live `platform.*` capabilities from memory.
3. A user-visible change carries a `whats-new:`, `whats-new-highlight:` or
   `whats-new-agent:` marker in its changeset. Add the one that fits.

A doc you decline is a marker in that same changeset, which CI reads:

```
<!-- capabilities-doc: n/a — <reason, at least 10 characters> -->
<!-- guide: n/a — <reason, at least 10 characters> -->
```

## Pre-push checks (step 8)

Lint is Oxlint and the formatter is Oxfmt (no Prettier, no ESLint, no Biome). Oxfmt
covers `.ts` and `.tsx` files only. For the changed files run what the pre-push hook
runs:

```bash
node scripts/pre-push-lint.mjs --files <paths>   # oxfmt --check + oxlint, by slice
```

It runs oxlint slice by slice because the type-aware rules need too much memory for a
whole workspace; do not replace it with one direct `oxlint` call.

In addition to lint and the affected tests:

```bash
(cd backend  && bun run test:invariants)   # after any backend change
(cd frontend && bun run test:invariants)   # after any frontend change
```

Backend jest does not type-check. For changed test files run
`node scripts/pre-push-typecheck.mjs --files <paths>`.

Obey the machine's rules for heavy jobs (jest, vitest, tsc, oxlint over a tree, a
build) when its instructions define a wrapper or a lock.

## Boot verification (step 8, any backend change)

`build`, `test:types` and `test:jest` all pass on broken NestJS DI. Booting the app is
the only local check that resolves the whole graph. Parallel runs on one machine share
ports and process names, so isolate each boot:

- **Give each run its own `API_PORT`.** `main.ts` checks the port before
  `NestFactory.create` and exits 1 with `Port <n> is already in use!`. A port collision
  looks exactly like a DI break. Read the log for that line before you blame the diff.
- **Set `API_PORT`** in `backend/apps/api/.env` (copied in step 4). `PORT` is ignored.
- **Boot:** `cd backend && bun run dev` in the background with its output in a log
  file. Success = the log contains `API is running`. Without the watcher:
  `bun run build`, then `backend/scripts/boot-smoke.sh` (what CI runs).
- **macOS has no `timeout` binary.** Poll the log in a bounded loop.
- **Kill by PID, never by pattern.** `lsof -ti :<API_PORT> | xargs kill`. Never
  `pkill -f`, and never the `kill-backend` skill during a run: both stop the backend of
  every sibling run.
- **Clean up the process chain.** `bun run dev` starts `dotenvx → nest start --watch`.
  Keep the PID of the `bun run dev` you started and kill its tree by parent PID:

  ```bash
  killtree() { for c in $(pgrep -P "$1"); do killtree "$c"; done; kill "$1" 2>/dev/null; }
  killtree "$DEV_PID"
  lsof -ti :"$API_PORT"   # must print nothing
  ```

## PR body (step 8)

The PR template starts with an `Owner:` line and has a `Verified` section that wants
proof of a run: the test output, the `API is running` line for a backend change, a
screenshot for a UI change.

## Known flaky CI signatures (step 9)

Documented in the platform `docs/troubleshooting.md`. Classify them as unrelated. Do
not chase the suite the job names, do not re-run it alone to confirm, do not add a
retry to the code.

| Signature | What it is |
|---|---|
| jest `signal=SIGSEGV`, or `N failed` suites with zero failed tests | a jest worker died in V8 GC; the named suite is only the one the worker held |
| durable-Postgres lane: `1 failed` suite, zero failed tests, `terminating connection due to administrator command` at `Client.idleListener` | a migration test's teardown races `pool.end()`; the victim suite changes between runs |
| `tsc` / `oxlint` exit 134, `FATAL ERROR: … heap out of memory`, no diagnostics | a V8 out-of-memory, not a type or lint error |

It is a flake only when both hold:

1. The failing file is not in this PR's diff.
2. A sibling PR on the same base commit passed the same job, or the same job passed on
   `main` at that commit.

Otherwise treat it as related and fix it.

## Before the merge (step 12)

Two green PRs that are each behind `main` have broken `main` together. Before every
`gh pr merge`, from a checkout that has the script (the run's worktree, before it is
removed):

```bash
sha=$(scripts/pre-merge-fresh-check.sh <pr>) &&   # can run longer than 10 minutes
gh pr merge <pr> --squash --match-head-commit "$sha" ...
```

Merge only on exit 0, and at once: stdout is the full head SHA it tested. The platform
`AGENTS.md` ("Commits and PRs") is the authority for the exit codes; when it and this
table disagree, it wins.

| Exit | Meaning | Do |
|---|---|---|
| 0 | green | merge now, with the printed SHA |
| 1 | red; it names the failed jobs | fix the branch; do not merge |
| 3 | timed out; the run it dispatched still goes | do not start a second run. Follow the hint in the message: `gh run watch <id>`, and merge on that run's green only while `main` is still at the `<base>` the message names (`git ls-remote origin refs/heads/main`); else run the check again. Merge with the PR head: the last SHA inside the parentheses of the `testing <base>..<head> (...)` line. The `<head>` before the parentheses is the merge commit, not the PR head |
| 4 | no result (a closed or merged PR, a conflict, a PR from a forked repository, a dispatch or a read that failed) | do not merge; read the message. A PR from a forked repository is a person's call |
| 6 | green, but `main` moved past the tested tip | run the check again |

Everything except the SHA goes to stderr, so the messages stay visible through the
command substitution. A shell tool that ends the script early (a time limit, exit 129,
130 or 143) leaves the run going, as exit 3 does: run the script in the background
when the tool has a limit, and treat an early end like exit 3.

## Merged is not deployed (step 3, step 13)

Nothing deploys on a plain merge to `main`. The release and the deploy are one event:

- **Platform change:** merging the changesets Version PR (`chore: version platform`,
  branch `changeset-release/main`) makes `version.yml` merge the release commit into
  `deploy-candidate`. The fleet Jenkins polls that branch, then builds, dry-runs and
  deploys with no person in between. Nobody pushes `deploy` or `deploy-candidate` by
  hand, and `deploy` no longer moves. See the platform `docs/versioning-and-releases.md`.
- **`@skaile/workspaces` change** takes three hops before it can reach production:
  (1) merge the workspaces PR and then its Version Packages PR, which publishes to npm;
  (2) merge a platform PR that bumps the pin in `backend/package.json` and
  `frontend/package.json` (and rebuilds the agent image if the runner changed);
  (3) merge the platform Version PR.

So the final report says `Deployed: no - waits for the next platform Version PR` (or
the workspaces hop it waits on), never just `merged`.
