---
name: "good-morning"
description: "The morning startup ritual for skaile-dev — opens with a cheeky \"what did Peter do last night\" investigation of the repo state, then syncs the shell repo + submodules, pulls the current branch, reinstalls workspace deps, builds the platform backend, and launches platform frontend + backend in dev mode. Use when Peter says \"good morning\", \"/good-morning\", \"let's start the day\", \"fire up the platform\", \"boot me up\", or any other morning kick-off phrase."
metadata:
  version: "1.0.0"
  tags:
    - "morning"
    - "startup"
    - "sync"
    - "dev"
    - "bootstrap"
    - "daily"
    - "routine"
    - "kickoff"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
      - path: "package.json"
        gate: hard
        description: "Must run from the skaile-dev shell repo root (Bun workspace)."
      - path: "platform/backend/package.json"
        gate: hard
        description: "Platform backend must exist — Step 5 builds it and Step 6 launches it."
      - path: "platform/frontend/package.json"
        gate: hard
        description: "Platform frontend must exist — Step 6 launches its dev server."
---

# good-morning — The Morning Startup Routine

A semi-serious morning ritual: peek at what Peter changed overnight, sync the monorepo, refresh submodules, reinstall the workspace, build the platform backend, and launch the platform frontend + backend so everyone can drop straight back into work.

## When to Use

Invoke when a User signals the start of the day, including phrases like:

- "good morning"
- "/good-morning"
- "let's start the day"
- "fire up the platform"
- "boot me up"
- "what did Peter do last night"

## Required Opening Line

When this skill runs, the **very first sentence** the assistant emits (before any tool calls, before any other prose) MUST be exactly:

> First let me check, what Peter did last night...

This is non-negotiable — it's the personality of the skill. Then immediately move into Step 1.

## Procedure

Run these steps in order. Stop and report on any failure unless explicitly noted as "continue but flag".

### Step 1 — Investigate what Peter did last night

Establish what Peter touched recently across the shell repo and submodules. This is the "investigation" tone — keep it short and slightly amused.

```bash
# Resolve the shell-repo absolute path once — every later step (especially the
# background launches in Step 6) MUST use this. Background bash invocations do
# NOT inherit the harness's persisted CWD, so any relative `cd ./platform/...`
# pattern is silently wrong and falls through to the wrong package's scripts.
REPO_ROOT="$(git rev-parse --show-toplevel)"

# Shell repo: current branch + last 3 commits + dirty status
git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD
git -C "$REPO_ROOT" log --oneline -3
git -C "$REPO_ROOT" status --short

# Capture the user's git identity for the Step 7 sign-off (first word, fallback to Peter)
USER_NAME="$(git -C "$REPO_ROOT" config user.name | awk '{print $1}')"
USER_NAME="${USER_NAME:-Peter}"
echo "USER_NAME=$USER_NAME"

# Truncate the pull-summary log — Steps 2 & 3 append to it, Step 3.5 reads it
: > /tmp/good-morning-pulls.log

# Submodules: which ones moved, which ones are dirty
git -C "$REPO_ROOT" submodule status
```

Capture three values for later steps:
- `<repo-root>` — absolute path to the shell repo (use it in **every** background command)
- `<branch>` — current shell-repo branch (used in Step 2)
- `<user-name>` — first word of `git config user.name`, fallback `Peter` (used in Step 7 sign-off only)

Summarise findings to Peter in 2–3 lines, e.g.:

> Last night Peter was on `feat/platform-mobile-phase-1`, last touched `platform/` (3 commits ahead of origin), agent-framework is clean. Let's get him going.

### Step 2 — Sync the shell repo (current branch)

Capture the pre-pull SHA before fetching, so Step 3.5 can compute exactly which commits arrived. Only log the entry **after** `pull --ff-only` succeeds (a no-op pull leaves the SHA equal to HEAD; Step 3.5 filters those out).

```bash
SHELL_OLD_SHA="$(git -C "$REPO_ROOT" rev-parse HEAD)"
git -C "$REPO_ROOT" fetch origin --prune
git -C "$REPO_ROOT" pull --ff-only origin <branch> \
  && echo "skaile-dev (shell)|$REPO_ROOT|$SHELL_OLD_SHA" >> /tmp/good-morning-pulls.log
```

If `pull --ff-only` rejects (branch has diverged), **STOP**:

> Peter, your `<branch>` has diverged from origin. I won't auto-merge — handle it manually then call `/good-morning` again.

### Step 3 — Realign submodule pointers, then update each submodule's branch

First, pull submodule pointers to match the now-current shell repo. Try `--recursive` first; if a nested submodule reference is broken (not uncommon in `forge/`), fall back to a non-recursive update so the top-level pointers still align:

```bash
git -C "$REPO_ROOT" submodule update --init --recursive 2>/tmp/submodule-update.err \
  || { echo "  ⚠ recursive update failed — falling back to top-level only"; \
       cat /tmp/submodule-update.err; \
       git -C "$REPO_ROOT" submodule update --init; }
```

Then, for every submodule whose checked-out branch is the **same name** as the shell repo's branch, fast-forward it. Detached-HEAD submodules and submodules on a different branch are skipped (and reported).

```bash
BRANCH=$(git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD)

for sub in agent-framework agent-plugin ai-assets ai-assets-skaileup forge infra marketing platform store; do
  if [ ! -e "$REPO_ROOT/$sub/.git" ]; then
    echo "  • $sub: not present on disk — skip"
    continue
  fi
  cur=$(git -C "$REPO_ROOT/$sub" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
  case "$cur" in
    HEAD|"")
      echo "  • $sub: detached HEAD or missing — pinned by shell repo (skip)"
      ;;
    "$BRANCH")
      sub_old_sha="$(git -C "$REPO_ROOT/$sub" rev-parse HEAD 2>/dev/null || echo "")"
      if git -C "$REPO_ROOT/$sub" fetch origin --prune \
        && git -C "$REPO_ROOT/$sub" pull --ff-only origin "$cur"; then
        echo "  ✓ $sub: pulled $cur"
        echo "$sub|$REPO_ROOT/$sub|$sub_old_sha" >> /tmp/good-morning-pulls.log
      else
        echo "  ⚠ $sub: cannot fast-forward $cur (continuing)"
      fi
      ;;
    *)
      echo "  • $sub: on $cur (skip — not the shell-repo branch)"
      ;;
  esac
done
```

Continue past warnings — the shell-repo pointers are already aligned.

### Step 3.5 — Pulled-commits summary

Now that all pulls are done, render a compact summary table of what arrived. Reads `/tmp/good-morning-pulls.log` populated by Steps 2 and 3 (each line: `<label>|<dir>|<old-sha>`). Categorisation is by conventional-commits prefix on the commit subject:

- **Feat** — `feat(...)` / `feat!` / `feat:` — features added
- **Fix** — `fix(...)` / `fix!` / `fix:` — bugs fixed
- **Revert** — `revert(...)` / `revert:` — features removed / rolled back
- **Other** — chore, docs, test, refactor, perf, build, style, ci
- **Breaking** — any commit whose prefix has `!` (e.g. `feat!:`) or whose body contains `BREAKING CHANGE`

If `/tmp/good-morning-pulls.log` is empty, print "Nothing new pulled — already up to date." and skip to Step 4.

```bash
if [ ! -s /tmp/good-morning-pulls.log ]; then
  echo "Nothing new pulled — already up to date."
else
  printf "\n## What got pulled\n\n"
  printf "| %-22s | %4s | %3s | %6s | %5s | %8s |\n" "Repo" "Feat" "Fix" "Revert" "Other" "Breaking"
  printf "|------------------------|------|-----|--------|-------|----------|\n"

  while IFS='|' read -r label dir old_sha; do
    [ -z "$label" ] && continue
    cur_sha="$(git -C "$dir" rev-parse HEAD 2>/dev/null || echo "")"
    [ -z "$cur_sha" ] || [ "$old_sha" = "$cur_sha" ] && continue

    log="$(git -C "$dir" log --pretty=format:'%s' "$old_sha..$cur_sha" 2>/dev/null)"
    [ -z "$log" ] && continue

    feat=$(printf '%s\n' "$log"  | grep -cE '^feat(\(|!|:)'   || true)
    fix=$(printf '%s\n' "$log"   | grep -cE '^fix(\(|!|:)'    || true)
    revert=$(printf '%s\n' "$log" | grep -cE '^revert(\(|!|:)' || true)
    total=$(printf '%s\n' "$log" | grep -cE '.')
    other=$(( total - feat - fix - revert ))
    [ $other -lt 0 ] && other=0

    # Breaking detection: (a) bang in the prefix, (b) "BREAKING CHANGE" anywhere in commit body,
    # (c) bare "BREAKING:" title prefix (skaile-dev's pre-conventional format, still in use)
    bang=$(printf '%s\n' "$log" | grep -cE '^[a-z]+(\([^)]*\))?!:' || true)
    body_break=$(git -C "$dir" log --pretty=format:'%B' "$old_sha..$cur_sha" 2>/dev/null \
                  | grep -cE 'BREAKING CHANGE' || true)
    bare_break=$(printf '%s\n' "$log" | grep -cE '^BREAKING(\(|:)' || true)
    breaking=$(( bang + body_break + bare_break ))

    printf "| %-22s | %4d | %3d | %6d | %5d | %8d |\n" \
      "$label" "$feat" "$fix" "$revert" "$other" "$breaking"
  done < /tmp/good-morning-pulls.log

  # Highlights — show up to 8 feat/fix/revert/breaking titles, prefixed with their repo
  printf "\nHighlights:\n"
  {
    while IFS='|' read -r label dir old_sha; do
      [ -z "$label" ] && continue
      cur_sha="$(git -C "$dir" rev-parse HEAD 2>/dev/null || echo "")"
      [ -z "$cur_sha" ] || [ "$old_sha" = "$cur_sha" ] && continue
      git -C "$dir" log --pretty=format:"  [$label] %h %s" "$old_sha..$cur_sha" 2>/dev/null \
        | grep -E '^\s*\[[^]]+\] [a-f0-9]+ ((feat|fix|revert)(\(|!|:)|BREAKING(\(|:)|[a-z]+(\([^)]*\))?!:)' || true
    done < /tmp/good-morning-pulls.log
  } | head -8

  rm -f /tmp/good-morning-pulls.log
fi
```

If the table has zero rows (everything was a no-op pull), say "Nothing new pulled" and move on. Do not stop — this is informational only.

**Expected output shape:**

```text
## What got pulled

| Repo                   | Feat |  Fix | Revert | Other | Breaking |
|------------------------|------|------|--------|-------|----------|
| agent-framework        |    8 |    5 |      0 |     7 |        1 |
| platform               |    2 |    1 |      0 |     4 |        0 |
| ai-assets              |    1 |    0 |      0 |     2 |        0 |

Highlights:
  [agent-framework] 5491d1e feat(bridge,runner,...): unified credential mediation rollout
  [agent-framework] 4e5d743 fix(discovery): import parseFrontmatter from subpath
  [agent-framework] f3706d9 BREAKING: factor lab out into skaile-ai/forge-lab
  [platform] b764f73 feat(users): bulk-add users + credential mediation
  [ai-assets] 5ffaa89 feat(skill): add good-morning skill
```

### Step 4 — Reinstall workspace deps + register the global CLI

```bash
bun i && bun install:global
```

If `bun i` fails: **STOP**. Show the first error and let Peter resolve (likely a lockfile drift or registry issue).

### Step 5 — Build the platform backend

Always use the absolute path so this works regardless of the harness's persisted CWD:

```bash
cd "$REPO_ROOT/platform/backend" && bun run build
```

If the build fails: **STOP**. Show the first compile error block. Do not proceed to launching dev servers — they'll only fail again.

### Step 6 — Launch platform backend + frontend in dev mode

Both run as background processes so Peter regains the prompt immediately and can keep working.

> **Critical — absolute paths only.** Background bash invocations start with a fresh CWD; they do **not** inherit `cd` from earlier synchronous commands. Running `bun run dev` from anywhere except `platform/backend` will silently invoke the **wrong** script: a prod-built backend with no `--watch` loop, leaving an orphan `node main.js` process bound to port 3001 that has to be hunted down with the `kill-backend` skill. Always interpolate the absolute path you captured in Step 1.

**Backend** (NestJS via `dotenvx + nest start --watch`, port 3001):

Substitute the literal absolute path for `<repo-root>` (it is the value you captured into `REPO_ROOT` in Step 1, e.g. `/Users/henk/Work/Projects/PostXL/skaile-dev`). Run via Bash with `run_in_background=true`:

```bash
cd <repo-root>/platform/backend && bun run dev
```

**Frontend** (Vite):

```bash
cd <repo-root>/platform/frontend && bun run dev
```

After launching, wait ~5 seconds and tail each background log to confirm:
- Backend log contains `Skaile Platform API is running` AND the parent process tree shows `nest start --watch` (proves the dev/watch loop, not a prod build).
- Frontend log contains `VITE v… ready` and a `Local: http://localhost:3000/` line.

If the backend log is missing `nest start --watch` in the process tree (`ps -ef | grep nest`), the wrong script ran — kill the chain via `kill-backend` and retry with the correct absolute path.

Capture both background-shell IDs and report them to Peter in Step 7.

### Step 7 — Sign off

Wrap up with a single status line. Substitute `<user-name>` with the value captured in Step 1 (the first word of `git config user.name`, fallback `Peter`):

> ☕ Good morning, `<user-name>`. Backend is booting (bg `<id>`, port 3001). Frontend is starting (bg `<id>`). You're up — go build something.

Concrete examples:
- git user is "Henk Blankenberg" → "☕ Good morning, Henk."
- git user.name unset → "☕ Good morning, Peter."

If any step earlier flagged a warning (skipped submodule, uncommitted changes, etc.), include a one-line "Heads-up" before the sign-off.

**Note on naming:** Only the Step 7 sign-off uses `<user-name>`. The opening line in Step 1 (`First let me check, what Peter did last night...`) and any in-skill "Peter" references stay hardcoded — that's the personality of the skill regardless of who's running it.

## Failure Handling

| Step | Failure mode                                                                | Action                                                                                                            |
| ---- | --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| 2    | `pull --ff-only` rejects                                                    | STOP — branch has diverged from origin                                                                            |
| 3    | recursive submodule update fails (broken nested ref, e.g. `forge/`)         | fall back to `submodule update --init` (top-level only); continue and flag in Step 7 heads-up                     |
| 3    | a submodule cannot ff                                                       | continue, flag in Step 7 heads-up                                                                                 |
| 3    | a submodule directory is missing on disk                                    | continue, flag in Step 7 heads-up (do not attempt to clone)                                                       |
| 3.5  | `/tmp/good-morning-pulls.log` empty                                         | print "Nothing new pulled — already up to date." and skip the table                                               |
| 3.5  | `git log <old>..<cur>` errors (e.g. shallow clone)                          | skip that row, continue with the others — informational step, never stops the skill                               |
| 4    | `bun i` fails                                                               | STOP — likely lockfile / registry issue                                                                           |
| 4    | `bun install:global` fails                                                  | continue if `bun i` succeeded; flag for follow-up                                                                 |
| 5    | backend build fails                                                         | STOP — do not start dev servers                                                                                   |
| 6    | port 3001 already bound                                                     | run the `kill-backend` skill, then retry Step 6                                                                   |
| 6    | backend log shows `running … in prod mode` and process tree lacks `nest`    | the wrong script ran (relative `cd` failed) — `kill-backend`, then retry Step 6 with the **absolute** repo path   |
| 6    | background command fails immediately with `cd: no such file or directory`   | a relative `cd` was used; replace with `cd <repo-root>/platform/...` and re-launch                                |

## What This Skill Never Does

- Never switches branches — Peter stays on whatever branch he ended yesterday on
- Never force-pulls, rebases, or stashes — divergence and dirty trees are stop signals
- Never commits, pushes, or opens PRs
- Never seeds, migrates, or wipes the database — that's a separate concern
- Never installs Biome on `platform/` or runs Biome there — platform uses Prettier + ESLint
- Never skips the opening line — the personality is part of the contract
