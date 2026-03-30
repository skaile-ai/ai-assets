---
name: "git"
description: "Git operations for the skaile-dev monorepo. Six modes: commit (structured commit messages), branch (create/switch feature branches), worktree (parallel work in isolated checkouts), pr (open a pull request), finish (merge/PR/keep/discard branch), sync (submodule synchronization)."
metadata:
  version: "1.0.0"
  tags:
    - "git"
    - "commit"
    - "branch"
    - "worktree"
    - "pull-request"
    - "submodule"
    - "monorepo"
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
        required: true
        hint: "commit = structured commit message | branch = create/switch feature branch | worktree = isolated checkout | pr = open pull request | finish = merge/PR/keep/discard | sync = update submodules"
      - id: "description"
        label: "Change description (plain language) — used to derive branch name and commit message"
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

## When to Use

- Committing changes: `mode=commit` to generate structured messages with `---agent---` metadata
- Before any code change: `mode=branch` to get on a feature branch
- Parallel feature work: `mode=worktree` to work in isolation
- Closing out an implementation: `mode=finish`
- Team review: `mode=pr`
- Submodule alignment: `mode=sync`

## When NOT to Use

- Large multi-package implementations — use `implement` which calls this skill internally
- Resolving merge conflicts that are conceptual design decisions — escalate to user

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
MUST  include the ---agent--- YAML block in every commit to main
NEVER force-push or rewrite published history
NEVER merge to main without tests passing
NEVER delete a branch without typed confirmation
NEVER create a worktree for sequential (non-parallel) work — use branch mode instead
NEVER write commit messages without reading the diff first
NEVER omit the scope or type fields in a commit message

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

  STEP 2: Analyze Changes
    For each modified package:
    1. **Changes**: List each discrete change in imperative mood.
    2. **Exports**: Check if public API surface changed:
       - New types, functions, or classes exported -> `+`
       - Changed signatures or behavior of existing exports -> `~`
       - Removed exports -> `-`
    3. **Breaking**: Determine if any change breaks existing consumers.
    4. **Decisions**: Reflect on architectural choices made during this session:
       - What alternatives existed?
       - Why was this approach chosen?
       - Under what conditions should it be revisited?

  STEP 3: Determine Migration Impact
    For each package in `affects`:
    1. What would that package's maintainer need to know or do?
    2. Is the migration required (breaking) or recommended (non-breaking enhancement)?
    3. Write concrete, actionable instructions — not "update as needed" but specific function calls, config changes, or patterns to adopt.

  STEP 4: Write the Message
    Follow this exact structure:

    ```
    <type>(<scope>): <title>

    <human-description>

    ---agent---
    scope: [<package-paths>]
    type: <type>
    breaking: <true|false>
    affects: [<downstream-packages>]

    changes:
    - <change 1>
    - <change 2>

    decisions:
    - <decision summary>
      reason: <why>
      alternatives: [<alt1>, <alt2>]
      revisit_when: <condition>

    migrate:
    - <package>: <what to do>

    exports:
    <+|~|-> <symbol> (<kind>) from|in <package>
    ```

    Rules:
    - **Title**: max 72 chars, imperative mood, lowercase, no period
    - **Human description**: 1-3 sentences. What and why, not how.
    - **scope**: list of package paths, not npm package names
    - **type**: `feat|fix|refactor|docs|test|chore|perf|build`
    - **changes**: imperative mood, each entry is one discrete change
    - **decisions**: only include when a non-trivial choice was made. Omit for obvious/mechanical changes.
    - **migrate**: only include when `affects` is non-empty and action is needed
    - **exports**: only include when public API surface changed
    - Omit optional sections entirely rather than leaving them empty

    Output the complete commit message ready to be used with `git commit -m`.
    Do not wrap it in a code block or add commentary.

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

  STEP 1: Fetch all submodules
    $ git submodule update --remote --merge

  STEP 2: Report status
    - Detect submodules dynamically via `git submodule status`
    FOR EACH submodule:
      - Show current branch + HEAD SHA
      - Flag if there are local uncommitted changes
      - Flag if submodule pointer changed

  STEP 3: Stage submodule pointer updates
    IF any submodule pointers changed:
      - Stage only the changed submodule paths
      > "Submodule pointers updated. Commit with mode=commit or review first."

  EMIT [git] sync_complete

# ── End Modes ────────────────────────────────────────────────────

CHECKLIST
  - [ ] Commit messages follow references/commit-spec.md format
  - [ ] Branch name follows naming convention from references/branch_naming.md
  - [ ] Working tree was clean before branch/worktree creation
  - [ ] Tests pass before merge or PR
  - [ ] Typed confirmation received for merge and discard
  - [ ] Worktree cleaned up after finish (merge, keep, discard)
  - [ ] git-state.json written/updated

---

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Committing directly to main | Always create a feature branch first |
| Branch name too generic (`fix/stuff`) | Include package + specific description |
| Using worktrees for sequential tasks | Worktrees add overhead; use branch mode for sequential work |
| Merging without running tests | Full test suite must pass before any merge |
| Force-pushing after rebase | Never rewrite history on shared branches |
| Writing commit messages manually | Use `mode=commit` for structured messages with `---agent---` blocks |
| Omitting the ---agent--- block | Every commit to main must include the agent metadata block |

## Integration

- **Called by:** `implement` (git setup, commit, finish branch)
- **Reads:** `references/commit-spec.md`, `references/branch_naming.md`, `references/worktree_patterns.md`
