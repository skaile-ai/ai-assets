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
| `deploy` | **Retired** — merged the shell's `main` into `deploy` | Platform deploys when its Version PR merges |

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
MUST  verify the platform backend (`backend/` in the platform repo) starts before committing any structural backend change - see STEP 1b in commit mode
MUST  run formatting and linting on all affected packages before committing — see STEP 1c in commit mode
MUST  push the branch before removing a worktree — unpushed worktree commits exist only locally and are lost when the worktree is removed
NEVER force-push or rewrite published history
NEVER run Biome in the platform or store repos — they use Prettier + ESLint exclusively
NEVER merge to main without tests passing
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
    1. Run `git diff --cached` (for staged commits) or `git diff main...HEAD` (for squash-merge prep) to see all changes.
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

  STEP 4: Write git-state.json
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
    - Read git-state.json: branch, mode, worktree_path
    - default_branch = the same probe as worktree STEP 3 (never assume "main")
    - Run full test suite
    IF tests fail -> STOP

  STEP 1a: Unpushed-commit safeguard (CRITICAL for worktree branches)
    Commits made in a worktree exist ONLY locally until the branch is pushed. Removing
    the worktree and deleting the branch destroys them. `pull-request` and `keep` run
    PUSH_BRANCH before they remove the worktree. `merge` squashes the commits into the
    default branch first, and `discard` deletes them on purpose, so neither pushes.

    PUSH_BRANCH:
      upstream ← $ git rev-parse --abbrev-ref --symbolic-full-name @{upstream} 2>/dev/null
      IF upstream is empty (no upstream):
        $ git push -u origin <branch-name>
      ELSE IF `git log --oneline @{upstream}..HEAD` is non-empty:
        $ git push origin <branch-name>
      IF the push fails:
        STOP: "Failed to push <branch-name>. Fix the push error before finishing —
               removing the worktree without pushing will permanently lose those commits."

  STEP 2: Present options
    > "Implementation complete. What would you like to do with <branch-name>?
    >
    > 1. merge — squash-merge to <default_branch> and delete branch
    > 2. pull-request — open PR for review (branch stays open)
    > 3. keep — leave branch as-is; come back later
    > 4. discard — delete branch (requires typed 'discard' confirmation)"

  STEP 3: Execute

    CASE merge:
      IF the repo protects its default branch (`gh api
        repos/<owner>/<repo>/branches/<default_branch> --jq .protected` is true) → use
        CASE pull-request instead; the squash merge happens on GitHub.
      - Ask: "Type 'merge' to confirm:"
      IF not confirmed -> STOP
      $ git checkout <default_branch>
      $ git merge --squash <branch-name>
      - Generate commit message using mode=commit
      $ git branch -d <branch-name>
      IF worktree_path exists -> $ git worktree remove <worktree_path>
      - Update git-state.json: status=merged

    CASE pull-request:
      RUN PUSH_BRANCH (STEP 1a)
      - RUN mode=pr
      IF worktree_path exists -> $ git worktree remove <worktree_path>
      - Update git-state.json: status=pr_open

    CASE keep:
      RUN PUSH_BRANCH (STEP 1a)
      IF worktree_path exists -> $ git worktree remove <worktree_path>
      - Update git-state.json: status=kept

    CASE discard:
      - Ask: "Type 'discard' to permanently delete <branch-name> (local, and on origin if
        it was pushed):"
      IF not confirmed -> STOP
      IF worktree_path exists -> $ git worktree remove <worktree_path> --force
      $ git checkout <default_branch>
      $ git branch -D <branch-name>
      IF `git ls-remote --heads origin <branch-name>` is non-empty:
        $ git push origin --delete <branch-name>
      - Update git-state.json: status=discarded

  EMIT [git] finish_complete action=<merge|pr|keep|discard>

# ── Mode: sync (retired) ─────────────────────────────────────────

IF mode = sync

  STOP: "`sync` is retired. It pulled and pushed the skaile-dev shell repo and its
  submodules; the shell's GitHub remote is archived and no repo has submodules now.
  Update each clone on its own: `git -C <repo> fetch origin && git -C <repo> pull --ff-only`
  on its main, then `bun install` in that repo if its lockfile changed."

# ── Mode: deploy (retired) ───────────────────────────────────────

IF mode = deploy

  STOP: "`deploy` is retired. It merged the shell's `main` into its `deploy` branch. Each
  repo now deploys on its own. The platform deploys when its changesets Version PR merges
  (its `version.yml` fast-forwards `deploy`); never merge `main` into `deploy` by hand,
  because that races the release job. See the platform `CLAUDE.md` and
  `docs/versioning-and-releases.md` in the platform repo."

# ── End Modes ────────────────────────────────────────────────────

CHECKLIST
  - [ ] Commit messages follow references/commit-spec.md format
  - [ ] Branch name follows naming convention from references/branch_naming.md
  - [ ] Working tree was clean before branch/worktree creation
  - [ ] Tests pass before merge or PR
  - [ ] Typed confirmation received for merge and discard
  - [ ] Worktree cleaned up after finish (merge, keep, discard)
  - [ ] git-state.json written/updated
  - [ ] sync and deploy modes stop with their pointer (retired)
  - [ ] No `---agent---` block in any commit message
  - [ ] Backend start verified (or explicitly skipped) when structural platform `backend/` changes are present
  - [ ] Formatting and linting run on all affected packages before commit (Biome for workspaces; Prettier+ESLint for platform and store)

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
| Committing without formatting | Always run format+lint before committing — Biome for workspaces; Prettier+ESLint for platform and store |
| Running Biome in platform or store | They use Prettier + ESLint. Biome reformats them incorrectly. |
| Adding a `---agent---` block | It belonged to the retired shell; write a conventional title + prose body only |

## Integration

- **Called by:** `implement` (git setup, commit, finish branch)
- **Calls:** `review` (optional review step in commit mode, default-on for main)
- **Reads:** `references/commit-spec.md`, `references/branch_naming.md`, `references/worktree_patterns.md`
