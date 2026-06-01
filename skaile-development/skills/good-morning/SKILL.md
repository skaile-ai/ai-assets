---
name: "good-morning"
description: "The morning startup ritual for skaile-dev — opens with a cheeky \"what
  did Peter do last night\" investigation of the repo state, syncs the shell repo
  + submodules (reattaching any that landed detached at origin/main), reinstalls workspace
  deps, and builds the platform backend so frontend + backend are ready to start.
  Does NOT launch dev servers — Peter starts those himself. Use when Peter says \"\
  good morning\", \"/good-morning\", \"let's start the day\", \"fire up the platform\"\
  , \"boot me up\", or any other morning kick-off phrase."
metadata:
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
      description: "Platform backend must exist — Step 5 builds it; the sign-off points
        Peter at `bun run dev`."
    - path: "platform/frontend/package.json"
      gate: hard
      description: "Platform frontend must exist — the sign-off points Peter at its
        `bun run dev`."
---

# good-morning — The Morning Startup Routine

A semi-serious morning ritual: peek at what Peter changed overnight, sync the monorepo, refresh submodules (reattaching any that landed detached at origin/main), reinstall the workspace, and build the platform backend so Peter can drop straight into `bun run dev` himself. The skill does **not** launch the dev servers — that's a Peter action.

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
# Resolve the shell-repo absolute path once — used in Step 5 (backend build).
REPO_ROOT="$(git rev-parse --show-toplevel)"

# Shell repo: current branch + last 3 commits + dirty status
git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD
git -C "$REPO_ROOT" log --oneline -3
git -C "$REPO_ROOT" status --short

# Capture the user's git identity for the Step 6 sign-off (first word, fallback to Peter)
USER_NAME="$(git -C "$REPO_ROOT" config user.name | awk '{print $1}')"
USER_NAME="${USER_NAME:-Peter}"
echo "USER_NAME=$USER_NAME"

# Truncate the pull-summary log — Steps 2 & 3 append to it, Step 3.5 reads it
: > /tmp/good-morning-pulls.log

# Submodules: which ones moved, which ones are dirty
git -C "$REPO_ROOT" submodule status
```

Capture three values for later steps:
- `<repo-root>` — absolute path to the shell repo (used in Step 5)
- `<branch>` — current shell-repo branch (used in Step 2)
- `<user-name>` — first word of `git config user.name`, fallback `Peter` (used in Step 6 sign-off only)

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

### Step 3 — Realign submodules, pull each submodule's branch, reattach detached HEADs

This step has **three phases**:

1. Move every submodule's working tree to the SHA the shell repo records (`git submodule update --init --recursive`). This always lands in **detached-HEAD** state.
2. For submodules already on a branch matching the shell repo's branch, fast-forward that branch from origin.
3. For submodules still detached *but pointing at origin/main*, reattach them to `main` so subsequent commits don't land on a detached HEAD. This catches the very common case where the shell repo's pull bumped a submodule pointer to a new SHA on origin/main — phase 1 leaves the submodule detached at that SHA, and without this phase it stays detached.

#### Phase 1 — Realign submodule pointers

First, pull submodule pointers to match the now-current shell repo. Try `--recursive` first; if a nested submodule reference is broken (not uncommon in `forge/`), fall back to a non-recursive update so the top-level pointers still align:

```bash
git -C "$REPO_ROOT" submodule update --init --recursive 2>/tmp/submodule-update.err \
  || { echo "  ⚠ recursive update failed — falling back to top-level only"; \
       cat /tmp/submodule-update.err; \
       git -C "$REPO_ROOT" submodule update --init; }
```

#### Phase 2 — Fast-forward submodules that are on a matching branch

For every submodule whose checked-out branch is the **same name** as the shell repo's branch, fast-forward it. Detached-HEAD submodules and submodules on a different branch are skipped here — phase 3 picks up the detached ones.

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

#### Phase 3 — Reattach detached HEADs that already match origin/main

When the shell repo's pull bumped a submodule pointer to a new SHA on `origin/main`, phase 1 leaves that submodule **detached** at the new SHA and phase 2 skips it (because its branch is `HEAD`, not `main`). The submodule is fully pulled — it just isn't on the `main` branch ref. Reattach it so a subsequent commit doesn't go to a detached HEAD.

Reattach rules:
- Only act on submodules in detached-HEAD state.
- Fetch `origin/main` first (phase 2 didn't fetch for detached submodules).
- Only reattach if `HEAD == origin/main`. If detached at any other SHA, the user deliberately pinned it (a tag, an older commit, a feature SHA) — leave alone.
- If a local `main` ref already exists, only reattach when local `main` has **zero commits ahead of HEAD** (no unpushed local-only work to lose). If it has unpushed commits, leave detached and flag.
- If no local `main` ref exists, create it at HEAD.

```bash
for sub in agent-framework agent-plugin ai-assets ai-assets-skaileup forge infra marketing platform store; do
  if [ ! -e "$REPO_ROOT/$sub/.git" ]; then
    continue
  fi
  cur=$(git -C "$REPO_ROOT/$sub" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
  # Only act on detached HEADs; phase 2 already handled on-branch submodules.
  [ "$cur" != "HEAD" ] && continue

  # Phase 2 didn't fetch this one — refresh remote refs before comparing.
  git -C "$REPO_ROOT/$sub" fetch origin --prune >/dev/null 2>&1

  cur_head=$(git -C "$REPO_ROOT/$sub" rev-parse HEAD)
  origin_main=$(git -C "$REPO_ROOT/$sub" rev-parse origin/main 2>/dev/null || echo "")

  if [ -z "$origin_main" ]; then
    echo "  • $sub: detached, no origin/main reachable — leaving detached"
    continue
  fi

  if [ "$cur_head" != "$origin_main" ]; then
    # Detached at some non-main SHA (tag, older commit, feature pin) — leave alone.
    echo "  • $sub: detached at ${cur_head:0:8} (≠ origin/main, leaving detached)"
    continue
  fi

  # Detached at origin/main tip — safe to reattach if no local-only commits would be lost.
  local_main=$(git -C "$REPO_ROOT/$sub" rev-parse main 2>/dev/null || echo "")
  if [ -z "$local_main" ]; then
    git -C "$REPO_ROOT/$sub" checkout -b main >/dev/null 2>&1 \
      && echo "  ↪ $sub: created local main at ${cur_head:0:8}"
    continue
  fi
  ahead=$(git -C "$REPO_ROOT/$sub" rev-list --count "$cur_head..$local_main" 2>/dev/null || echo "?")
  if [ "$ahead" = "0" ]; then
    git -C "$REPO_ROOT/$sub" checkout -B main HEAD >/dev/null 2>&1 \
      && echo "  ↪ $sub: reattached to main (was detached at origin/main tip)"
  else
    echo "  ⚠ $sub: detached at origin/main but local main has $ahead unpushed commits — leaving detached"
  fi
done
```

Continue past warnings — the shell-repo pointers are already aligned, and any "leaving detached" line is informational (the submodule is at exactly the SHA the shell repo records).

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

If the build fails: **STOP**. Show the first compile error block. Do not sign off as "ready to start" — Peter would just hit the same build error the moment he ran `bun run dev`.

### Step 6 — Sign off (services are ready to start)

The skill does **not** launch the dev servers — Peter starts them himself, typically in two terminal panes so he can see the logs.

Before signing off, do a courtesy check that ports 3000 and 3001 aren't already bound by stale processes from a previous session — if they are, mention it so Peter knows he'll need to run `kill-backend` or kill the vite zombie before `bun run dev`.

```bash
# Courtesy port check — informational only, do NOT kill anything from inside this skill.
lsof -i :3001 -sTCP:LISTEN -t 2>/dev/null \
  && echo "  ⚠ port 3001 already bound — run \`kill-backend\` before \`bun run dev\` in platform/backend" \
  || echo "  ✓ port 3001 free"
lsof -i :3000 -sTCP:LISTEN -t 2>/dev/null \
  && echo "  ⚠ port 3000 already bound — kill the stale process before \`bun run dev\` in platform/frontend" \
  || echo "  ✓ port 3000 free"
```

Then wrap up with the sign-off. Substitute `<user-name>` with the value captured in Step 1 (the first word of `git config user.name`, fallback `Peter`):

> ☕ Good morning, `<user-name>`. Workspace synced, deps installed, backend built clean. Backend and frontend are ready to start when you are:
>
> - `cd platform/backend && bun run dev` — NestJS API (port 3001)
> - `cd platform/frontend && bun run dev` — Vite dev server (port 3000)
>
> Go build something.

Concrete examples:
- git user is "Henk Blankenberg" → "☕ Good morning, Henk."
- git user.name unset → "☕ Good morning, Peter."

If any step earlier flagged a warning (skipped submodule, uncommitted changes, port already bound, detached submodule left alone, etc.), include a one-line "Heads-up" before the sign-off.

**Note on naming:** Only the Step 6 sign-off uses `<user-name>`. The opening line in Step 1 (`First let me check, what Peter did last night...`) and any in-skill "Peter" references stay hardcoded — that's the personality of the skill regardless of who's running it.

## Failure Handling

| Step | Failure mode                                                                | Action                                                                                                            |
| ---- | --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| 2    | `pull --ff-only` rejects                                                    | STOP — branch has diverged from origin                                                                            |
| 3.1  | recursive submodule update fails (broken nested ref, e.g. `forge/`)         | fall back to `submodule update --init` (top-level only); continue and flag in Step 6 heads-up                     |
| 3.2  | a submodule cannot ff                                                       | continue, flag in Step 6 heads-up                                                                                 |
| 3    | a submodule directory is missing on disk                                    | continue, flag in Step 6 heads-up (do not attempt to clone)                                                       |
| 3.3  | submodule detached at non-main SHA (tag, older commit, feature pin)         | leave detached — that's a deliberate pin; do not reattach                                                         |
| 3.3  | submodule detached at origin/main but local `main` has unpushed commits     | leave detached, flag in Step 6 heads-up so Peter can decide whether to merge/push                                 |
| 3.5  | `/tmp/good-morning-pulls.log` empty                                         | print "Nothing new pulled — already up to date." and skip the table                                               |
| 3.5  | `git log <old>..<cur>` errors (e.g. shallow clone)                          | skip that row, continue with the others — informational step, never stops the skill                               |
| 4    | `bun i` fails                                                               | STOP — likely lockfile / registry issue                                                                           |
| 4    | `bun install:global` fails                                                  | continue if `bun i` succeeded; flag for follow-up                                                                 |
| 5    | backend build fails                                                         | STOP — do not sign off as "ready to start"; Peter needs to fix the build first                                    |
| 6    | port 3001 or 3000 already bound                                             | flag in heads-up so Peter runs `kill-backend` or kills the stale vite before `bun run dev` — do NOT kill from here |

## What This Skill Never Does

- Never starts `bun run dev` for backend or frontend — Peter starts those himself so he controls the terminals and sees the logs directly
- Never kills processes — even when ports 3000/3001 are already bound, the skill only reports it and lets Peter run `kill-backend` himself
- Never switches branches in the shell repo — Peter stays on whatever branch he ended yesterday on
- Never reattaches a submodule that is detached at a non-`origin/main` SHA — that's treated as a deliberate pin (tag, older commit, feature SHA) and left alone
- Never reattaches a submodule when local `main` has unpushed commits ahead of the detached HEAD — those commits would be silently lost; the skill flags instead
- Never force-pulls, rebases, or stashes — divergence and dirty trees are stop signals
- Never commits, pushes, or opens PRs
- Never seeds, migrates, or wipes the database — that's a separate concern
- Never installs Biome on `platform/` or runs Biome there — platform uses Prettier + ESLint
- Never skips the opening line — the personality is part of the contract
