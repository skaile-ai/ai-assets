---
name: "git"
description: "Git operations for the skaile-dev monorepo. Seven modes: commit (structured
  commit messages), branch (create/switch feature branches), worktree (parallel work
  in isolated checkouts), pr (open a pull request), finish (merge/PR/keep/discard
  branch), sync (two-way sync: pull + push shell repo and all submodules, print per-repo
  summary of commits pulled and pushed), deploy (merge main into deploy branch and
  push)."
metadata:
  tags:
  - "git"
  - "commit"
  - "branch"
  - "worktree"
  - "pull-request"
  - "submodule"
  - "monorepo"
  - "skaile-development"
  - "deploy"
  source: "MERGED"
  stage: "beta"
  user_inputs:
    dialog:
    - id: "mode"
      label: "Git operation mode"
      type: "select"
      options:
      - "commit"
      - "branch"
      - "worktree"
      - "pr"
      - "finish"
      - "sync"
      - "deploy"
      required: true
      hint: "commit = structured commit message | branch = create/switch feature branch
        | worktree = isolated checkout | pr = open pull request | finish = merge/PR/keep/discard
        | sync = update submodules | deploy = merge main into deploy branch and push"
    - id: "description"
      label: "Change description (plain language) — used to derive branch name and
        commit message"
      type: "text"
      required: false
    - id: "branch_type"
      label: "Branch type (for branch/worktree mode)"
      type: "select"
      options:
      - "feature"
      - "fix"
      - "refactor"
      - "docs"
      - "skill"
      - "chore"
      required: false
      default: "feature"
    - id: "target_packages"
      label: "Target package(s)"
      type: "text"
      required: false
    files: []
---

# Git — Monorepo Git Operations

## Overview

Handles all git operations for the skaile-dev monorepo. Works in six modes:

| Mode | What It Does | When to Use |
|------|-------------|-------------|
| `commit` | Generate a structured commit message with agent-readable metadata | After staging changes, before committing |
| `branch` | Create or switch to a feature branch | Before starting any code change |
| `worktree` | Add an isolated git worktree for parallel work | Multiple unrelated changes simultaneously |
| `pr` | Open a pull request with structured description | When change needs review before merging |
| `finish` | Close a branch: merge, PR, keep, or discard | After implementation is verified |
| `sync` | Fetch and update all submodules | After pulling, or to align submodule pointers |
| `deploy` | Merge main into the `deploy` branch and push | When ready to ship current main to the deploy target |

## When to Use

- Committing changes: `mode=commit` to generate structured messages
- Before any code change: `mode=branch` to get on a feature branch
- Parallel feature work: `mode=worktree` to work in isolation
- Closing out an implementation: `mode=finish`
- Team review: `mode=pr`
- Submodule alignment: `mode=sync`
- Shipping to deploy target: `mode=deploy`

## When NOT to Use

- Large multi-package implementations — use `implement` which calls this skill internally
- Resolving merge conflicts that are conceptual design decisions — escalate to user

---

## Model Dispatch

This skill runs in a subagent to keep the main conversation context lean.

DISPATCH  Spawn a subagent using the Agent tool to execute the selected mode:
  - Standard execution: `model: "sonnet"`
  - On error (tool failure, STOP condition, or unexpected git state): re-dispatch with `model: "opus"`

```
Agent({
  model: "sonnet",           // default
  prompt: "<full skill context + mode + inputs>"
})
```

IF the sonnet subagent returns a STOP, error, or unresolved conflict:
  Re-dispatch with `model: "opus"` and include the error output in the prompt so opus has full context.
  Do NOT retry with sonnet — escalate directly.

NEVER execute git mode steps inline when this skill is loaded via the Skill tool.
Always use the Agent tool for dispatch.

---

ROLE  Git operations manager for the skaile-dev monorepo — enforces structured commit messages, branch naming, worktree conventions, PR templates, safe commit practices, and submodule synchronization.

READS
  references/commit-spec.md           — structured commit message format
  references/branch_naming.md         — branch naming rules per change type
  references/worktree_patterns.md     — worktree setup and teardown patterns
  git status / git log / git diff     — current repository state
  ? _implementation/git-state.json    — existing git state if present

WRITES
  .git/                               — commits, branches, worktrees
  _implementation/git-state.json      — records active branch, mode, worktree path

MUST  follow the structured commit message format from references/commit-spec.md
MUST  derive branch name from description using references/branch_naming.md rules
MUST  check for dirty working tree before creating branches or worktrees
MUST  require typed confirmation for destructive operations (force-delete, discard)
MUST  write git-state.json when branch or worktree is created
MUST  run full test suite before any merge to main
MUST  verify platform/backend starts before committing any structural backend change - see STEP 1b in commit mode
MUST  run formatting and linting on all affected packages before committing — see STEP 1c in commit mode
MUST  push submodule commits to their remotes before removing a worktree or opening a PR — submodule commits only exist locally and are lost when the worktree is cleaned up
NEVER force-push or rewrite published history
NEVER run Biome inside platform/ — platform uses Prettier + ESLint exclusively
NEVER merge to main without tests passing
NEVER delete a branch without typed confirmation
NEVER create a worktree for sequential (non-parallel) work — use branch mode instead
NEVER write commit messages without reading the diff first
NEVER omit the scope or type fields in a commit message title
NEVER remove a worktree that contains unpushed submodule commits - they will be permanently lost
NEVER allow format/lint to silently mutate workspace-root files (`bun.lock`, root `package.json`, `.skaile/lock` files). After STEP 1c, run `git status` and revert any side-effect mutations that weren't in the original staged set — they belong in separate, deliberate commits
NEVER stage a submodule pointer bump without first running `git -C <submodule> fetch origin && git -C <submodule> log HEAD..origin/main --oneline` — if that output is non-empty, the bump would silently regress upstream commits

EMIT [git] started mode=<mode>

# ── Mode: commit ─────────────────────────────────────────────────

IF mode = commit

  You are writing a structured commit message for the skaile-dev monorepo.
  The message must follow the format defined in `references/commit-spec.md`.

  STEP 1: Gather Context
    1. Run `git diff --cached` (for staged commits) or `git diff main...HEAD` (for squash-merge prep) to see all changes.
    2. Identify the packages modified by mapping changed file paths to their package roots (e.g. `agent-framework/session/src/foo.ts` -> `agent-framework/session`).
    3. For each modified package, read its `CLAUDE.md` to understand architecture and conventions.
    4. Identify downstream packages that import from or depend on the modified packages — these are candidates for the `affects` field.

  STEP 1b: Backend start verification (conditional)
    Scan the diff for structural platform/backend changes — any of:
    - New or modified `@Injectable()` / `@Module()` class
    - Constructor parameter added, removed, or retyped in a service
    - `providers:`, `imports:`, or `exports:` arrays changed in a `*.module.ts`
    - Import paths changed in a service or module file

    IF none of the above → skip this step.

    IF structural change detected:
      > "Structural backend change detected — verifying the backend starts."
      RUN in background (15 s timeout): cd platform/backend && bun run dev
      Watch stdout for either a startup success banner ("Nest application successfully started")
      or an exception / unresolved dependency error.

      IF port already in use (EADDRINUSE / port 3001 blocked):
        ASK:
          > "Port 3001 is in use. Choose:
          >   1. Use kill-backend skill to free it, then retry
          >   2. Kill it manually — confirm when done
          >   3. Skip verification and commit anyway"
        HANDLE response:
          - option 1: RUN kill-backend skill, then retry the dev start
          - option 2: wait for user confirmation, then retry
          - option 3: proceed to STEP 2 without verification

      IF NestJS DI error or exception before startup banner:
        Terminate dev process.
        STOP: "Backend failed to start: <error summary>. Fix the DI error before committing."

      IF startup banner appears:
        Terminate dev process.
        > "Backend starts cleanly. Proceeding."
        Continue to STEP 1c.

  STEP 1c: Format and lint affected packages
    From the packages identified in STEP 1, determine which formatters to run.

    | Path prefix | Tool | Command (run from monorepo root) |
    |---|---|---|
    | `agent-framework/` | Biome | `cd agent-framework && bun run format && bun run lint` |
    | `forge/` | Biome | `cd forge && bun run format && bun run lint` |
    | `docs/`, `theme/` | Biome | `biome format --write docs/ theme/` |
    | `platform/backend/` | Prettier + ESLint | `cd platform/backend && bun run lint` |
    | `platform/frontend/` | Prettier + ESLint | `cd platform/frontend && bun run lint` |

    NEVER run Biome on `platform/` — it uses Prettier + ESLint exclusively.
    Only run the commands for areas that have staged changes.

    Before running format/lint, snapshot files that lint chains can mutate as
    a side effect (typecheck → bun install → re-resolve):
      $ cp bun.lock /tmp/bun.lock.before    (only if it exists)

    IF formatting/linting modified any staged files:
      $ git add <modified files>
      > "Formatted <N> files in <areas> — re-staged."
    IF linting reports unfixable errors:
      STOP: "Lint errors found. Fix them before committing."

    Lint side-effect guard — check for unintended workspace mutations:
      $ git status --short    (compare against the original dirty set)
      IF bun.lock changed AND was NOT in the original staged/dirty set:
        > "Lint side effect: bun.lock was rewritten by typecheck → bun install.
        >  Reverting — this belongs in a separate, deliberate commit."
        $ diff /tmp/bun.lock.before bun.lock || git checkout -- bun.lock
        Note in the commit report which side-effect files were reverted.
      IF any package.json was modified that was NOT in the original staged set:
        $ git checkout -- <those package.json files>
        Note the revert in the commit report.

    This guard exists because lint chains in a Bun workspace silently rewrite
    the root `bun.lock` (a single workspace-wide lockfile) when run from inside
    a submodule. The submodule's commit comes out clean while the parent gains
    an unrelated lockfile delta — observed concretely with a zod 4.4.3 → 4.3.6
    re-resolution during a one-line walkDir fix.

  STEP 2: Write the Message
    Follow this exact structure:

    ```
    <type>(<scope>): <title>

    <human-description>
    ```

    Rules:
    - **Title**: max 72 chars, imperative mood, lowercase, no period
    - **scope**: comma-separated package paths relative to repo root
    - **type**: `feat|fix|refactor|docs|test|chore|perf|build`
    - **Human description**: 1-3 sentences. What and why, not how.

    Output the complete commit message ready to be used with `git commit -m`.
    Do not wrap it in a code block or add commentary.

  STEP 3: Optional Review (before committing)
    After presenting the commit message, ask:
    > "Run a quick review before committing? (y/n)"

    IF user says yes (or equivalent):
      Run the `review` skill with target=staged (or target=branch for squash-merge prep).
      Wait for review output.
      IF review finds Important issues:
        > "Review found <N> important issue(s). Fix before committing?"
        STOP — do not commit. Let the user address the findings.
      IF review finds only Nits or no issues:
        > "Review passed. Proceeding with commit."
        Continue to commit.

    IF user says no (or equivalent):
      Proceed to commit without review.

    IF committing to main:
      Default to running the review (still skippable with explicit "no").

  EMIT [git] commit_ready

# ── Mode: branch ─────────────────────────────────────────────────

IF mode = branch

  STEP 1: Determine branch name
    - If description provided: derive slug from it (lowercase, hyphens, max 40 chars)
    - If target_packages provided: include package abbreviation in slug
    - Apply naming rule from references/branch_naming.md:
      feature/<pkg>/<slug>    e.g. feature/forge-project/workspace-rename
      fix/<pkg>/<slug>         e.g. fix/platform-backend/session-leak
      refactor/<pkg>/<slug>    e.g. refactor/cli/command-structure
      docs/<pkg>/<slug>        e.g. docs/cli/add-resource-types
      skill/<domain>/<slug>    e.g. skill/dev-workspace/add-devlog-skill
      chore/<slug>             e.g. chore/bump-bun-version

  STEP 2: Check git state
    $ git status --short
    IF dirty working tree
      - STOP: "Working tree has uncommitted changes. Commit or stash before branching."
      - List changed files

  STEP 3: Create or switch branch
    IF branch already exists
      $ git checkout <branch-name>
      > "Switched to existing branch: <branch-name>"
    ELSE
      $ git checkout -b <branch-name>
      > "Created and switched to: <branch-name>"

  STEP 4: Write git-state.json
    - Write _implementation/git-state.json:
      { "branch": "<name>", "mode": "branch", "worktree_path": null, "created_at": "<ISO>", "status": "active" }

  EMIT [git] branch_ready name=<branch-name>

# ── Mode: worktree ───────────────────────────────────────────────

IF mode = worktree

  STEP 1: Determine branch name (same as branch mode)

  STEP 2: Assess if worktree is appropriate
    - Worktrees are for genuinely parallel, file-independent work
    - If both changes touch the same files -> recommend branch mode instead
    > "Worktree mode is best for parallel changes that don't share files.
    > Confirm? (yes / use branch mode instead)"

  STEP 3: Create branch + worktree
    $ git checkout -b <branch-name>
    $ git checkout -   (back to previous branch)
    $ mkdir -p .worktrees
    $ git worktree add .worktrees/<slug> <branch-name>
    - Add .worktrees/ to .gitignore if not already present

  STEP 4: Write git-state.json
    { "branch": "<name>", "mode": "worktree", "worktree_path": ".worktrees/<slug>", "created_at": "<ISO>", "status": "active" }
    > "Worktree created at .worktrees/<slug>
    > Work in that directory. Main checkout is unaffected."

  EMIT [git] worktree_ready path=.worktrees/<slug>

# ── Mode: pr ─────────────────────────────────────────────────────

IF mode = pr

  STEP 1: Pre-flight
    $ git status   — must be clean
    - Run full test suite: `bun x --bun vitest run` (or appropriate command for package)
    IF tests fail
      - STOP: "Tests must pass before opening a PR. Fix failing tests first."

  STEP 2: Build PR title and body
    - Title: same format as Conventional Commits message for the branch's primary change
    - Body template:

    ```
    ## What

    <1-3 sentences: what was changed>

    ## Why

    <1-3 sentences: motivation, problem solved, or feature requested>

    ## Changes

    <bullet list of meaningful changes — package by package>
    - `<package>`: <what changed>

    ## Testing

    - [ ] Tests pass locally (`bun x --bun vitest run`)
    - [ ] Affected package manually tested
    - [ ] No regressions in other packages

    ## Documentation

    - [ ] README.md updated (if public API changed)
    - [ ] CLAUDE.md updated (if architecture/conventions changed)
    - [ ] Starlight docs updated (if commands/API changed)
    - [ ] Devlog entry written

    ## Notes

    <optional: breaking changes, follow-up tasks, known limitations>
    ```

  STEP 2a: Push submodule commits (if any)
    Before pushing the shell repo, ensure submodule commits are on their remotes.
    Without this, the PR's submodule pointer references commits that don't exist
    on the submodule remote — the PR cannot be checked out by reviewers.

    $ git config --file .gitmodules --get-regexp 'submodule\..*\.path' | awk '{print $2}'
    FOR EACH submodule path:
      $ git -C <path> log --oneline @{upstream}..HEAD 2>/dev/null
      IF output is non-empty:
        $ git -C <path> rev-parse --abbrev-ref HEAD -> sub_branch
        IF sub_branch = HEAD (detached):
          $ git -C <path> checkout -b <branch-name>
          SET sub_branch = <branch-name>
        $ git -C <path> push -u origin <sub_branch>
        > "Pushed <N> commits in <path> to origin/<sub_branch>"

    IF any push fails:
      STOP: "Cannot open PR — submodule push failed for <path>. Fix before retrying."

  STEP 3: Open PR
    $ git push origin <branch-name>
    $ gh pr create \
        --title "<title>" \
        --body "<body>" \
        --base main \
        --head <branch-name>
    > "Pull request opened: <URL>"

  EMIT [git] pr_created url=<url>

# ── Mode: finish ─────────────────────────────────────────────────

IF mode = finish

  STEP 1: Pre-flight
    - Read git-state.json: branch, mode, worktree_path
    - Run full test suite
    IF tests fail -> STOP

  STEP 1a: Submodule safeguard (CRITICAL for worktree branches)
    skaile-dev is a shell repo with git submodules. When work happens in a worktree,
    commits inside submodules (e.g. platform/, agent-framework/, forge/) exist ONLY
    in the worktree's local copy. Removing the worktree destroys those commits
    permanently unless they have been pushed to the submodule's own remote.

    This step MUST run before presenting options — even "keep" removes the worktree.

    $ git config --file .gitmodules --get-regexp 'submodule\..*\.path' | awk '{print $2}'
    FOR EACH submodule path:
      # Check if the submodule has commits not on the remote
      $ git -C <path> log --oneline @{upstream}..HEAD 2>/dev/null
      IF output is non-empty (unpushed commits exist):
        # Determine current branch
        $ git -C <path> rev-parse --abbrev-ref HEAD
        IF branch = HEAD (detached):
          # Create a branch from the detached commits so they can be pushed
          $ git -C <path> checkout -b <shell-branch-name>
        # Push submodule commits to the submodule's remote
        $ git -C <path> push -u origin <submodule-branch>
        Record: pushed_submodules += "<path> (<N> commits pushed to origin/<submodule-branch>)"

    IF pushed_submodules is non-empty:
      > "Submodule commits pushed to their remotes before cleanup:
      > <list each pushed submodule and commit count>"

    IF any push fails:
      STOP: "Failed to push submodule commits in <path>. Fix the push error before
             finishing — removing the worktree without pushing will permanently lose
             those commits."

  STEP 2: Present options
    > "Implementation complete. What would you like to do with <branch-name>?
    >
    > 1. merge — squash-merge to main and delete branch
    > 2. pull-request — open PR for review (branch stays open)
    > 3. keep — leave branch as-is; come back later
    > 4. discard — delete branch (requires typed 'discard' confirmation)"

  STEP 3: Execute

    CASE merge:
      - Ask: "Type 'merge' to confirm:"
      IF not confirmed -> STOP
      $ git checkout main
      $ git merge --squash <branch-name>
      - Generate commit message using mode=commit
      $ git branch -d <branch-name>
      IF worktree_path exists -> $ git worktree remove <worktree_path>
      - Update git-state.json: status=merged

    CASE pull-request:
      - RUN mode=pr
      IF worktree_path exists -> $ git worktree remove <worktree_path>
      - Update git-state.json: status=pr_open

    CASE keep:
      IF worktree_path exists -> $ git worktree remove <worktree_path>
      - Update git-state.json: status=kept

    CASE discard:
      - Ask: "Type 'discard' to permanently delete <branch-name>:"
      IF not confirmed -> STOP
      IF worktree_path exists -> $ git worktree remove <worktree_path> --force
      $ git checkout main
      $ git branch -D <branch-name>
      - Update git-state.json: status=discarded

  EMIT [git] finish_complete action=<merge|pr|keep|discard>

# ── Mode: sync ───────────────────────────────────────────────────

IF mode = sync

  You are performing a two-way synchronisation of the skaile-dev shell repo and all
  its submodules: pull all remote changes, reinstall dependencies if needed, push all
  local unpushed commits, then print a summary of what moved in each direction.

  STEP 0: Pre-flight + submodule discovery
    $ git rev-parse --abbrev-ref HEAD
    IF result ≠ main
      STOP: "✗ sync only runs on main — currently on <branch>. Switch to main first."
    Discover all submodule paths (not names) from .gitmodules:
    $ git config --file .gitmodules --get-regexp 'submodule\..*\.path' | awk '{print $2}'
    Use these paths for all git -C calls and display labels.
    Process submodules in the order they appear in .gitmodules.

  STEP 1: Shell repo — fetch and snapshot
    $ git fetch origin
    Record shell_pre_sha: $ git rev-parse HEAD
    unpushed_shell ← $ git log origin/main..HEAD --oneline
    incoming_shell ← $ git log HEAD..origin/main --oneline

  STEP 2: Shell repo — pull
    $ git pull --rebase origin main
    IF conflict
      STOP: "✗ conflict in shell repo" + list conflicting files
            + "Resolve conflicts and re-run sync."

  STEP 3: Per-submodule — fetch, snapshot, pull
    FOR EACH submodule path (in .gitmodules order):
      a. Resolve branch and ensure on main:
         $ git -C <path> rev-parse --abbrev-ref HEAD → branch
         IF branch = HEAD (detached):
           > "<path> is in detached HEAD — checking out main."
           $ git -C <path> checkout main
           IF fails (no local main branch):
             $ git -C <path> checkout -b main origin/main
           SET branch = main

      b. Fetch:
         $ git -C <path> fetch origin

      c. Record old SHA:
         $ git -C <path> rev-parse HEAD → old_sha[path]

      d. Snapshot unpushed:
         $ git -C <path> log origin/<branch>..<branch> --oneline → unpushed[path]

      e. Pull this submodule:
         First try: $ git submodule update --init --remote --merge -- <path>
         The --init flag handles newly added submodules that haven't been cloned yet.
         IF fails:
           STOP: "✗ <path>: update failed — <error>"
                 List which submodules updated vs. which did not.
                 "Re-run sync after resolving the issue."

         After update, the submodule may be in detached HEAD again (git submodule update
         detaches by default). Re-attach to main:
         $ git -C <path> rev-parse --abbrev-ref HEAD → post_branch
         IF post_branch = HEAD (detached):
           $ git -C <path> checkout main
           $ git -C <path> merge HEAD@{1} --ff-only
           (This moves the local main branch forward to the SHA that submodule update
           checked out, keeping us on the named branch.)

      f. Compute incoming:
         $ git -C <path> log <old_sha[path]>..HEAD --oneline → incoming[path]

  STEP 4: Post-pull dependency install
    Collect changed files:
      shell_changed   ← $ git diff <shell_pre_sha>..HEAD --name-only
      FOR EACH submodule path:
        sub_changed[path] ← $ git -C <path> diff <old_sha[path]>..HEAD --name-only

    IF any package.json appears in shell_changed or any sub_changed[path]:
      $ bun install   (run from monorepo root)
      IF fails: STOP: "✗ bun install failed — <error>"
      SET ran_bun_install = true

    IF cli/package.json appears in sub_changed[agent-framework]:
      $ bun link   (run from monorepo root — re-links the CLI globally)
      IF fails: STOP: "✗ bun link failed — <error>"
      SET ran_bun_link = true

  STEP 5: Push submodules
    FOR EACH submodule path where unpushed[path] is non-empty:
      $ git -C <path> push origin <branch>
      IF rejected:
        STOP: "✗ push rejected: <path> — <error>"
              Do NOT push remaining submodules.
              Do NOT run step 6.

  STEP 6: Push shell repo
    $ git push origin main
    IF rejected:
      STOP: "✗ push rejected: shell repo — <error>. Re-run sync."

  STEP 7: Print sync summary

    Print the following format (fill in actual values):

    ── Sync Summary ──────────────────────────────────────────────

    Shell repo (skaile-dev)
      [if incoming_shell OR unpushed_shell non-empty: expand to two direction lines]
      ↓ pulled   N commits
        [if N > 0: list as • <message> (<short-sha>) per commit]
        [if N = 0: inline] ✓ up to date
      ↑ pushed   N commits
        [if N > 0: list as • <message> (<short-sha>) per commit]
        [if N = 0: inline] ✓ nothing to push
      [if both directions zero: single collapsed line]
      Shell repo (skaile-dev)   ✓ up to date, nothing to push

    [FOR EACH submodule path in .gitmodules order]
      [if incoming[path] OR unpushed[path] non-empty: expand]
      <path>
        ↓ pulled   N commits [list if N > 0]
        ↑ pushed   N commits [list if N > 0]
      [if both zero: single collapsed line]
      <path>          ✓ up to date, nothing to push

    [if ran_bun_install OR ran_bun_link: print footer block]
      [blank line]
      [if ran_bun_install]      bun install        ✓ workspace deps reinstalled
      [if ran_bun_link]         bun link           ✓ CLI re-linked (agent-framework/cli changed)

    ──────────────────────────────────────────────────────────────

  EMIT [git] sync_complete

# ── Mode: deploy ─────────────────────────────────────────────────

IF mode = deploy

  You are merging main into the `deploy` branch and pushing it.
  This is the standard way to ship the current state of main to the deploy target.

  STEP 1: Pre-flight — verify current branch
    $ git rev-parse --abbrev-ref HEAD → current_branch

    IF current_branch ≠ main
      ASK:
        > "You are on '<current_branch>', not main. What would you like to do?
        >
        > 1. merge-to-main — finish this branch and merge it to main first (runs mode=finish)
        > 2. cancel — abort the deploy"

      IF user chooses merge-to-main:
        RUN mode=finish → let user complete the merge flow
        THEN continue from STEP 2 (now on main)
      IF user chooses cancel:
        STOP: "Deploy cancelled."

  STEP 2: Ensure all submodules and shell repo are pushed
    CI checks out the deploy branch and recursively fetches submodules at the
    committed SHAs. If any submodule or the shell repo has unpushed commits,
    CI will fail because it cannot resolve those SHAs.

    Discover submodule paths:
    $ git config --file .gitmodules --get-regexp 'submodule\..*\.path' | awk '{print $2}'

    FOR EACH submodule path:
      $ git -C <path> rev-parse --abbrev-ref HEAD → branch
      IF branch = HEAD (detached): skip (submodule points at a fixed SHA)
      $ git -C <path> log origin/<branch>..<branch> --oneline → unpushed
      IF unpushed is non-empty:
        $ git -C <path> push origin <branch>
        IF push fails:
          STOP: "Cannot deploy: failed to push <path> -- <error>. Fix and retry."

    Check shell repo:
    $ git log origin/main..main --oneline → unpushed_shell
    IF unpushed_shell is non-empty:
      $ git push origin main
      IF push fails:
        STOP: "Cannot deploy: failed to push shell repo -- <error>. Fix and retry."

  STEP 3: Stash working tree changes if needed
    $ git status --short → status
    IF any tracked files are modified (M lines):
      $ git stash push -m "pre-deploy stash"
      SET stashed = true
    ELSE
      SET stashed = false

  STEP 4: Checkout and update deploy branch
    $ git checkout deploy
    $ git pull origin deploy
    IF pull fails:
      IF stashed: $ git stash pop
      $ git checkout main
      STOP: "✗ Failed to pull deploy branch — <error>. Returned to main."

  STEP 5: Merge main into deploy
    $ git merge main --no-edit
    IF merge conflict:
      $ git merge --abort
      $ git checkout main
      IF stashed: $ git stash pop
      STOP: "✗ Merge conflict between main and deploy. Resolve on deploy branch manually."

  STEP 5b: Normalize submodule URLs to HTTPS (CI cannot use SSH)
    The Jenkins executors authenticate to GitHub over HTTPS via a GIT_ASKPASS
    credential and have NO SSH deploy key. `main` tracks submodules with SSH URLs
    (git@github.com:...), so any submodule newly merged into deploy arrives as SSH
    and its checkout fails with "Permission denied (publickey)" — observed when the
    `brand` submodule was first added. The deploy branch therefore keeps every
    submodule URL on HTTPS. Re-apply that invariant after every merge so newly-added
    submodules are converted automatically, not just the ones fixed by hand once.

    Rewrite all SSH GitHub remotes in .gitmodules to HTTPS:
      $ sed -i '' -E 's#git@github\.com:#https://github.com/#' .gitmodules    # macOS
      # Linux/CI shells: $ sed -i -E 's#git@github\.com:#https://github.com/#' .gitmodules

    Verify none remain (this must succeed):
      $ ! grep -q 'git@github.com:' .gitmodules

    IF .gitmodules changed (git diff --quiet .gitmodules returns non-zero):
      $ git add .gitmodules
      $ git commit -m "fix(gitmodules): normalize submodule URLs to HTTPS for CI checkout"
      > "Converted <N> submodule URL(s) to HTTPS on deploy: <names>."
    ELSE:
      > "All submodule URLs already HTTPS — no normalization needed."

    NOTE: this commit lives only on the deploy branch. `main` intentionally keeps
    SSH URLs for local developer clones; never merge this normalization back to main.

  STEP 6: Push deploy
    $ git push origin deploy
    IF push fails:
      $ git checkout main
      IF stashed: $ git stash pop
      STOP: "✗ Push to deploy failed — <error>. Returned to main."

  STEP 7: Return to main
    $ git checkout main
    IF stashed: $ git stash pop

  EMIT [git] deploy_complete
  REPORT:
    > "Deployed: main merged into deploy and pushed.
    > deploy is now at: <git rev-parse --short deploy>"

# ── End Modes ────────────────────────────────────────────────────

CHECKLIST
  - [ ] Commit messages follow references/commit-spec.md format
  - [ ] Branch name follows naming convention from references/branch_naming.md
  - [ ] Working tree was clean before branch/worktree creation
  - [ ] Tests pass before merge or PR
  - [ ] Typed confirmation received for merge and discard
  - [ ] Worktree cleaned up after finish (merge, keep, discard)
  - [ ] git-state.json written/updated
  - [ ] deploy mode only runs from main (or after explicit merge-to-main)
  - [ ] all submodules and shell repo pushed before deploy merge
  - [ ] submodule URLs normalized to HTTPS on deploy after merge (no `git@github.com:` remains in .gitmodules)
  - [ ] stash popped after deploy (even on failure)
  - [ ] Backend start verified (or explicitly skipped) when structural platform/backend changes are present
  - [ ] Formatting and linting run on all affected packages before commit (Biome for agent-framework/forge/docs/theme; Prettier+ESLint for platform)

---

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Committing directly to main | Always create a feature branch first |
| Branch name too generic (`fix/stuff`) | Include package + specific description |
| Using worktrees for sequential tasks | Worktrees add overhead; use branch mode for sequential work |
| Merging without running tests | Full test suite must pass before any merge |
| Force-pushing after rebase | Never rewrite history on shared branches |
| Writing commit messages manually | Use `mode=commit` for structured messages |
| Committing without formatting | Always run format+lint before committing — Biome for agent-framework/forge; Prettier+ESLint for platform |
| Running Biome on platform/ | Platform uses Prettier + ESLint. Biome reformats it incorrectly. |

## Integration

- **Called by:** `implement` (git setup, commit, finish branch)
- **Calls:** `review` (optional review step in commit mode, default-on for main)
- **Reads:** `references/commit-spec.md`, `references/branch_naming.md`, `references/worktree_patterns.md`
