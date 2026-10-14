---
name: "git"
description: "Git operations for the standalone skaile repos (platform, workspaces,
  ai-assets, store, brand, …). Five modes: commit (structured commit messages), branch
  (create/switch feature branches), worktree (parallel work in isolated checkouts), pr
  (open a pull request), finish (merge/PR/keep/discard branch). The former sync and
  deploy modes served the retired skaile-dev shell and now stop with a pointer."
metadata:
  tags:
  - "git"
  - "commit"
  - "branch"
  - "worktree"
  - "pull-request"
  - "skaile-development"
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
        | sync, deploy = retired (shell-only); they stop with a pointer"
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

# Git — Git Operations for the Standalone Skaile Repos

## Overview

Handles git operations in any standalone skaile repo. The `skaile-dev` shell repo is retired
(its GitHub remote is archived): no repo has submodules, and the target repo's own `CLAUDE.md`
is the authority for its formatter, test command and commit rule. Works in five live modes:

| Mode | What It Does | When to Use |
|------|-------------|-------------|
| `commit` | Generate a conventional-commits message (title + prose body) | After staging changes, before committing |
| `branch` | Create or switch to a feature branch | Before starting any code change |
| `worktree` | Add an isolated git worktree for parallel work | Multiple unrelated changes simultaneously |
| `pr` | Open a pull request with structured description | When change needs review before merging |
| `finish` | Close a branch: merge, PR, keep, or discard | After implementation is verified |
| `sync` | **Retired** — synced the shell and its submodules | Use `git -C <repo> pull --ff-only` per clone |
| `deploy` | **Retired** — merged the shell's `main` into `deploy` | Never push `deploy`; the platform deploys when its Version PR merges (06:00-07:00 Berlin) |

## When to Use

- Committing changes: `mode=commit` to generate structured messages
- Before any code change: `mode=branch` to get on a feature branch
- Parallel feature work: `mode=worktree` to work in isolation
- Closing out an implementation: `mode=finish`
- Team review: `mode=pr`

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

ROLE  Git operations manager for the standalone skaile repos — enforces structured commit messages, branch naming, worktree conventions, PR templates, and safe commit practices.

READS
  <repo>/CLAUDE.md                    — the repo's formatter, test command and commit rule (the authority)
  references/commit-spec.md           — structured commit message format
  references/branch_naming.md         — branch naming rules per change type
  references/worktree_patterns.md     — worktree setup and teardown patterns
  git status / git log / git diff     — current repository state
  ? <repo_root>/_implementation/git-state.json — existing git state if present (repo_root = the
                                      main checkout, the worktree STEP 3 probe; never a relative path)

WRITES
  .git/                               — commits, branches, worktrees
  <repo_root>/_implementation/git-state.json — records active branch, mode, worktree path, status

MUST  follow the structured commit message format from references/commit-spec.md
MUST  derive branch name from description using references/branch_naming.md rules
MUST  check for dirty working tree before creating branches or worktrees
MUST  require typed confirmation for destructive operations (force-delete, discard)
MUST  write git-state.json when branch or worktree is created
MUST  run full test suite before any merge to <default_branch>
      (finish skips it when status=auto_merge: the suite ran before auto-merge was armed)
MUST  verify the platform backend (`backend/` in the platform repo) starts before committing any structural backend change - see STEP 1b in commit mode
MUST  run formatting and linting on all affected packages before committing — see STEP 1c in commit mode
MUST  push the branch before removing a worktree — unpushed worktree commits exist only locally and are lost when the worktree is removed
NEVER force-push or rewrite published history
NEVER run Biome in the platform or store repos — they use Prettier + ESLint exclusively
NEVER merge to <default_branch> without tests passing
NEVER delete a branch without typed confirmation
NEVER create a worktree for sequential (non-parallel) work — use branch mode instead
NEVER write commit messages without reading the diff first
NEVER omit the scope or type fields in a commit message title
NEVER remove a worktree that contains unpushed commits - they will be permanently lost
NEVER add a `---agent---` YAML block to a commit message — it belonged to the retired shell, and no standalone repo requires or consumes it
NEVER allow format/lint to silently mutate workspace-root files (`bun.lock`, root `package.json`, `.skaile/lock` files). After STEP 1c, run `git status` and revert any side-effect mutations that weren't in the original staged set — they belong in separate, deliberate commits

EMIT [git] started mode=<mode>

# ── Mode: commit ─────────────────────────────────────────────────

IF mode = commit

  You are writing a structured commit message for a standalone skaile repo. Run every
  command from that repo's root.
  The message must follow the format defined in `references/commit-spec.md`.

  STEP 1: Gather Context
    1. Run `git diff --cached` (for staged commits) or `git diff origin/<default_branch>...HEAD` (for a whole branch, e.g. a PR title; default_branch = the worktree STEP 3 probe) to see all changes.
    2. Identify the packages modified by mapping changed file paths to their package roots (e.g. `backend/libs/session/src/foo.ts` -> `backend/libs/session` in platform, `packages/workspaces/runner/src/foo.ts` -> `runner` in workspaces).
    3. For each modified package, read its `CLAUDE.md` to understand architecture and conventions.
    4. Identify downstream packages that import from or depend on the modified packages — name them in the description when they must react.

  STEP 1b: Backend start verification (conditional)
    Platform repo only. Scan the diff for structural `backend/` changes — any of:
    - New or modified `@Injectable()` / `@Module()` class
    - Constructor parameter added, removed, or retyped in a service
    - `providers:`, `imports:`, or `exports:` arrays changed in a `*.module.ts`
    - Import paths changed in a service or module file

    IF none of the above → skip this step.

    IF structural change detected:
      > "Structural backend change detected — verifying the backend starts."
      RUN in background (120 s timeout): cd backend && bun run dev
      Watch stdout for either the startup line `API is running` (the marker the platform's boot-smoke greps for)
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

    | Repo (path) | Tool | Command (run from that repo's root) |
    |---|---|---|
    | workspaces | Biome | `bun run format && bun run lint` |
    | platform (root) | Prettier | `bun run prettier:check` (`bun run prettier:format` to fix) |
    | platform `backend/` | Prettier + ESLint | `cd backend && bun run lint` |
    | platform `frontend/` | Prettier + ESLint | `cd frontend && bun run lint` |
    | store | Prettier + ESLint | `bun run lint` |
    | ai-assets, brand | — | no format script; skip |

    NEVER run Biome in platform or store — they use Prettier + ESLint exclusively.
    A repo not in this table: use the format/lint scripts its `CLAUDE.md` names.
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

    This guard exists because lint chains in a Bun workspace can silently rewrite
    `bun.lock` (one workspace-wide lockfile) — observed concretely with a zod
    4.4.3 → 4.3.6 re-resolution during a one-line walkDir fix.

  STEP 2: Write the Message
    Follow this exact structure:

    ```
    <type>(<scope>): <title>

    <human-description>
    ```

    Rules:
    - **No `---agent---` block** and no YAML after the body. No repo requires or consumes it.
    - Follow the repo's own `CLAUDE.md` where it says more (platform: ADRs are hand-written
      under `decisions/`, never extracted from commit messages).
    - **Title**: max 72 chars, imperative mood, lowercase, no period
    - **scope**: short package or area name(s) in the target repo, comma-separated
    - **type**: `feat|fix|refactor|docs|test|chore|perf|build`
    - **Human description**: 1-3 sentences. What and why, not how.

    Output the complete commit message ready to be used with `git commit -m`.
    Do not wrap it in a code block or add commentary.

  STEP 3: Optional Review (before committing)
    After presenting the commit message, ask:
    > "Run a quick review before committing? (y/n)"

    IF user says yes (or equivalent):
      Run the `review` skill with target=staged (or target=branch for a whole branch).
      Wait for review output.
      IF review finds Important issues:
        > "Review found <N> important issue(s). Fix before committing?"
        STOP — do not commit. Let the user address the findings.
      IF review finds only Nits or no issues:
        > "Review passed. Proceeding with commit."
        Continue to commit.

    IF user says no (or equivalent):
      Proceed to commit without review.

    IF committing to <default_branch>:
      Default to running the review (still skippable with explicit "no").

  EMIT [git] commit_ready

# ── Mode: branch ─────────────────────────────────────────────────

IF mode = branch

  STEP 1: Determine branch name
    - If description provided: derive slug from it (lowercase, hyphens, max 40 chars)
    - If target_packages provided: include package abbreviation in slug
    - Apply naming rule from references/branch_naming.md:
      feature/<pkg>/<slug>    e.g. feature/skaile-forge/workspace-rename
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
    - repo_root = dirname of `git rev-parse --path-format=absolute --git-common-dir` (the
      main checkout, also from inside a linked worktree)
    - Write <repo_root>/_implementation/git-state.json:
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
    The worktree goes BESIDE the clone, not inside it (same rule as the `ship` skill):
      repo_root      = dirname of `git rev-parse --path-format=absolute --git-common-dir`
                       (the MAIN checkout, also when run from inside a linked worktree;
                       `--show-toplevel` would name the worktree and nest `.worktrees/`)
      repo           = basename of repo_root
      default_branch = `git -C <repo_root> symbolic-ref --short refs/remotes/origin/HEAD`
                       with "origin/" stripped; IF unset → `gh repo view --json
                       defaultBranchRef -q .defaultBranchRef.name`. Do NOT assume "main".
      worktree_path  = <dirname repo_root>/.worktrees/<repo>-<slug>
    Outside the clone, it needs no `.gitignore` entry (brand and ai-assets do not ignore
    `.worktrees/`) and the main checkout stays clean.
    The new branch starts from `origin/<default_branch>`, not from the current HEAD (the
    old recipe branched off HEAD). To build on local unpushed work, say so and use the
    current branch as the base instead.
    $ git -C <repo_root> fetch origin
    $ mkdir -p <dirname repo_root>/.worktrees
    $ git -C <repo_root> worktree add <worktree_path> -b <branch-name> origin/<default_branch>

  STEP 4: Write <repo_root>/_implementation/git-state.json
    { "branch": "<name>", "mode": "worktree", "worktree_path": "<worktree_path>", "created_at": "<ISO>", "status": "active" }
    > "Worktree created at <worktree_path>
    > Work in that directory. Main checkout is unaffected."

  EMIT [git] worktree_ready path=<worktree_path>

# ── Mode: pr ─────────────────────────────────────────────────────

IF mode = pr

  STEP 1: Pre-flight
    $ git status   — must be clean
    - default_branch = the same probe as worktree STEP 3 (never assume "main")
    - Run the repo's test command from its `CLAUDE.md` (the domain's `references/test_stack_map.md` is a quick map)
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

    - [ ] Tests pass locally (<the repo's test command>)
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

  STEP 3: Open PR
    $ git push origin <branch-name>
    $ gh pr create \
        --title "<title>" \
        --body "<body>" \
        --base <default_branch> \
        --head <branch-name>
    > "Pull request opened: <URL>"

  EMIT [git] pr_created url=<url>

# ── Mode: finish ─────────────────────────────────────────────────

IF mode = finish

  STEP 1: Pre-flight
    - repo_root = the same `--git-common-dir` probe as worktree STEP 3 (the main checkout)
    - Read <repo_root>/_implementation/git-state.json: branch, mode, worktree_path, status
    - default_branch = the same probe as worktree STEP 3 (never assume "main")
    - slug = the `gh repo view` probe at the top of CASE merge. IF it fails (no `gh` auth,
      no network) → say so and carry on: `keep` and `discard` need no `gh`; only `merge`
      and `pull-request` do.
    IF status is `auto_merge` or `merged` → skip the tests and STEP 2. Go straight to
      "Already merged?" in CASE merge (no new confirmation: it was given before).
    - Run full test suite
    IF tests fail -> STOP

  STEP 1a: Unpushed-commit safeguard (CRITICAL for worktree branches)
    Commits made in a worktree exist ONLY locally until the branch is pushed. Removing
    the worktree and deleting the branch destroys them. `merge`, `pull-request` and `keep`
    run PUSH_BRANCH before they remove the worktree. `discard` deletes them on purpose, so
    it does not push the branch.

    PUSH_BRANCH:
      W = <worktree_path> if set, else the current checkout
      upstream ← $ git -C <W> rev-parse --abbrev-ref --symbolic-full-name @{upstream} 2>/dev/null
      IF upstream is empty (no upstream):
        $ git -C <W> push -u origin <branch-name>
      ELSE IF `git -C <W> log --oneline @{upstream}..HEAD` is non-empty:
        $ git -C <W> push origin <branch-name>
      IF the push fails:
        STOP: "Failed to push <branch-name>. Fix the push error before finishing —
               removing the worktree without pushing will permanently lose those commits."

  STEP 2: Present options
    armed ← $ gh pr list -R <slug> --head <branch-name> --state open --json autoMergeRequest
      -q '[.[] | select(.autoMergeRequest != null)] | length'
    IF armed is not 0 → print option 4 as "discard — unavailable while auto-merge is armed":
      deleting the branch would silently cancel the armed auto-merge.
    IF the probe fails (no slug, no `gh` auth) → print all four options and add a note that
      the auto-merge state is unknown; check it before choosing `discard`.
    > "Implementation complete. What would you like to do with <branch-name>?
    >
    > 1. merge — squash-merge on GitHub into <default_branch>, then delete the branch locally and on origin
    > 2. pull-request — open PR for review (branch stays open)
    > 3. keep — leave branch as-is; come back later
    > 4. discard — delete branch (requires typed 'discard' confirmation)"

  STEP 3: Execute

    CASE merge:
      Changes land by PR in every repo: "always a PR". The merge happens on GitHub only.
      There is no local squash, and never `--admin`. No `--delete-branch` either: CLEANUP
      step 6 deletes the remote branch after the merge is confirmed.

      slug ← $ gh repo view "$(git -C <repo_root> remote get-url origin)" --json nameWithOwner -q .nameWithOwner
      - Ask: "Type 'merge' to confirm: <branch-name> is squash-merged into <default_branch>
        on GitHub, then deleted locally and on origin:"
      IF not confirmed -> STOP

      Already merged? (a re-run must not open a second PR)
        merged_pr ← $ gh pr list -R <slug> --head <branch-name> --state merged --json number -q '.[0].number'
        IF merged_pr is non-empty → say "already merged (#<merged_pr>)" and go to CLEANUP.
        IF status was `auto_merge` and the open PR still has auto-merge armed → say
          "auto-merge still armed; GitHub has not merged yet" and STOP. Do not wait or poll.

      Merge on GitHub:
        RUN PUSH_BRANCH (STEP 1a)
        IF `gh pr list -R <slug> --head <branch-name> --state open --json number -q length` is 0:
          RUN mode=pr
        pr ← $ gh pr list -R <slug> --head <branch-name> --state open --json number -q '.[0].number'
        Probe before any merge — `gh pr merge` gives the same BLOCKED error for pending
        checks, red checks, a missing review and conflicts, so never classify its failure:
        $ gh pr view <pr> -R <slug> --json mergeStateStatus,reviewDecision,statusCheckRollup
        (A check run has `status`/`conclusion`; a commit status has `state`. `reviewDecision`
        is `""`, not null, when no review is required.)
        Let mergeStateStatus pick the branch; it already says whether the required gates pass:
        - CLEAN, UNSTABLE, HAS_HOOKS → mergeable now (UNSTABLE: only advisory checks are
          pending or failing; advisory checks don't gate the merge):
          $ gh pr merge <pr> -R <slug> --squash
          IF it fails → STOP and report the reason. Keep the branch and the worktree;
            status=pr_open.
        - BLOCKED → required gates are not met. Classify from the REQUIRED checks
          (`gh pr checks <pr> -R <slug> --required --json name,bucket`) and reviewDecision.
          Read its JSON, not its exit status: it exits non-zero when a required check is
          failing, when one is still pending, and when the repo has none (that last case
          prints no JSON at all — "no required checks reported").
          - any required check in bucket `fail` or `cancel`, or reviewDecision
            CHANGES_REQUESTED → STOP and name it; status=pr_open
            (`statusCheckRollup` gives the detail for the message)
          - a required check in bucket `pending`, or reviewDecision REVIEW_REQUIRED
            → $ gh pr merge <pr> -R <slug> --auto --squash
            IF that succeeds → report "auto-merge armed". Say the merge may land on someone
              else's approval when a review is still required. Keep the branch and the
              worktree; status=auto_merge. Stop here: do not wait or poll. Re-run finish
              after the merge to clean up.
            IF arming fails → STOP; status=pr_open. IF `gh api repos/<slug> --jq
              .allow_auto_merge` is false, say: "Turn on auto-merge in the repo settings or
              merge by hand." A re-run will not help.
          - otherwise (no JSON at all — "no required checks reported" — or every required
            check already in bucket `pass` or `skipping`) → nothing auto-merge waits on will
            clear the block: an unresolved review conversation, a ruleset, a required
            deployment or a code-owner gate. STOP and say which gate GitHub names;
            status=pr_open
        - UNKNOWN → GitHub has not finished computing mergeability (usually right after the
          PR is created). Wait ~3s, run the probe above again and classify from the new
          state; if it is still UNKNOWN → STOP and say so; status=pr_open
        - DIRTY (conflicts), BEHIND (base moved; update the branch), DRAFT
          → STOP and say which; status=pr_open
        Confirm: `gh pr view <pr> -R <slug> --json state -q .state` is MERGED. IF not (for
        example, a merge queue took it) → report it, status=auto_merge, and STOP.

      CLEANUP — run every command from <repo_root> (`cd <repo_root>` first): the worktree
      is removed here. Steps in this order:
        1. IF `git -C <repo_root> status --porcelain` is non-empty → STOP: "Merged on GitHub,
           but the main checkout has uncommitted changes. Clean it, then re-run finish."
        2. IF worktree_path exists -> $ git -C <repo_root> worktree remove <worktree_path>
           IF it refuses (untracked or modified files) → STOP and list the files. NEVER add
           `--force`: the merge is done, but those files are not in it.
        3. $ git -C <repo_root> checkout <default_branch>
           IF it fails because <default_branch> is checked out in another worktree (`git -C
           <repo_root> worktree list --porcelain` shows `branch refs/heads/<default_branch>`
           at another path) → STOP with a note naming that worktree: the merge is on GitHub;
           <branch-name> was kept locally; update <default_branch> in that worktree.
        4. $ git -C <repo_root> pull --ff-only origin <default_branch>
           IF it fails (local <default_branch> has diverged) → STOP with a note: the merge is
           on GitHub; local <default_branch> and <branch-name> were left as they are.
        5. IF `git -C <repo_root> worktree list --porcelain` still shows
           `branch refs/heads/<branch-name>` → STOP and name that worktree. Never delete a
           branch that is checked out.
           ELSE IF the local branch exists:
             $ git -C <repo_root> branch -D <branch-name>   # -D: a squash merge never marks the branch merged
        6. IF `git -C <repo_root> ls-remote --heads origin <branch-name>` is non-empty:
             $ git -C <repo_root> push origin --delete <branch-name>
        - Update <repo_root>/_implementation/git-state.json: status=merged

    CASE pull-request:
      RUN PUSH_BRANCH (STEP 1a)
      - RUN mode=pr
      Leave the worktree directory first (`cd <repo_root>`): it is removed next.
      IF worktree_path exists -> $ git -C <repo_root> worktree remove <worktree_path>
      - Update <repo_root>/_implementation/git-state.json: status=pr_open

    CASE keep:
      RUN PUSH_BRANCH (STEP 1a)
      Leave the worktree directory first (`cd <repo_root>`): it is removed next.
      IF worktree_path exists -> $ git -C <repo_root> worktree remove <worktree_path>
      - Update <repo_root>/_implementation/git-state.json: status=kept

    CASE discard:
      - Ask: "Type 'discard' to permanently delete <branch-name> (local, and on origin if
        it was pushed):"
      IF not confirmed -> STOP
      IF worktree_path exists -> $ git -C <repo_root> worktree remove <worktree_path> --force
      $ git -C <repo_root> checkout <default_branch>
      $ git -C <repo_root> branch -D <branch-name>
      IF `git -C <repo_root> ls-remote --heads origin <branch-name>` is non-empty:
        $ git -C <repo_root> push origin --delete <branch-name>
      - Update <repo_root>/_implementation/git-state.json: status=discarded

  EMIT [git] finish_complete action=<merge|pr|keep|discard>

# ── Mode: sync (retired) ─────────────────────────────────────────

IF mode = sync

  STOP: "`sync` is retired. It pulled and pushed the skaile-dev shell repo and its
  submodules; the shell's GitHub remote is archived and no repo has submodules now.
  Update each clone on its own: `git -C <repo> fetch origin && git -C <repo> pull --ff-only`
  on its default branch, then `bun install` in that repo if its lockfile changed."

# ── Mode: deploy (retired) ───────────────────────────────────────

IF mode = deploy

  STOP: "`deploy` is retired. Do not push `deploy`. It merged the shell's `main` into its
  `deploy` branch. Each repo now deploys on its own. A platform release deploys when its
  changesets Version PR merges: `version.yml` fast-forwards `deploy`. Merge the
  Version PR only in the 06:00-07:00 Berlin window, except for an outage, security or
  data-loss hotfix. The platform's `deploy` branch is protected by the `deploy-protection`
  ruleset, so a hand push, merge or force-push is rejected. See
  `docs/versioning-and-releases.md` in the platform repo."

# ── End Modes ────────────────────────────────────────────────────

CHECKLIST
  - [ ] Commit messages follow references/commit-spec.md format
  - [ ] Branch name follows naming convention from references/branch_naming.md
  - [ ] Working tree was clean before branch/worktree creation
  - [ ] Tests pass before merge or PR
  - [ ] Typed confirmation received for merge and discard
  - [ ] Worktree cleaned up after finish (merge, keep, discard) — except status=auto_merge, which keeps it until the merge lands
  - [ ] git-state.json written/updated
  - [ ] sync and deploy modes stop with their pointer (retired)
  - [ ] No `---agent---` block in any commit message
  - [ ] Backend start verified (or explicitly skipped) when structural platform `backend/` changes are present
  - [ ] Formatting and linting run on all affected packages before commit (Biome for workspaces; Prettier+ESLint for platform and store)

---

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Committing directly to <default_branch> | Always create a feature branch first |
| Branch name too generic (`fix/stuff`) | Include package + specific description |
| Using worktrees for sequential tasks | Worktrees add overhead; use branch mode for sequential work |
| Merging without running tests | Full test suite must pass before any merge |
| Force-pushing after rebase | Never rewrite history on shared branches |
| Writing commit messages manually | Use `mode=commit` for structured messages |
| Committing without formatting | Always run format+lint before committing — Biome for workspaces; Prettier+ESLint for platform and store |
| Running Biome in platform or store | They use Prettier + ESLint. Biome reformats them incorrectly. |
| Adding a `---agent---` block | It belonged to the retired shell; write a conventional title + prose body only |

## Integration

- **Called by:** `implement` (git setup, commit, finish branch)
- **Calls:** `review` (optional review step in commit mode, default-on for <default_branch>)
- **Reads:** `references/commit-spec.md`, `references/branch_naming.md`, `references/worktree_patterns.md`
