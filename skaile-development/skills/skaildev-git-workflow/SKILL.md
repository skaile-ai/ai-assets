---
name: "skaildev-git-workflow"
description: "Git operations for the skaile-dev monorepo. Four modes: branch (create/switch feature branches), worktree (parallel work in isolated checkouts), pr (open a pull request with structured description), finish (merge/PR/keep/discard branch). For committing, use the commit-message skill directly."
metadata:
  version: "1.0.0"
  tags:
    - "git"
    - "branch"
    - "worktree"
    - "pull-request"
    - "commit"
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
          - "branch"
          - "worktree"
          - "pr"
          - "finish"
        required: true
        hint: "branch = create/switch to a feature branch | worktree = create isolated checkout | pr = open pull request | finish = merge/PR/keep/discard branch. For commits, use the commit-message skill."
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

# Git Workflow — Monorepo Git Operations

## Overview

Handles git operations for the skaile-dev monorepo. Works in four modes:

| Mode | What It Does | When to Use |
|------|-------------|-------------|
| `branch` | Create or switch to a feature branch | Before starting any code change |
| `worktree` | Add an isolated git worktree for parallel work | Multiple unrelated changes simultaneously |
| `pr` | Open a pull request with structured description | When change needs review before merging |
| `finish` | Close a branch: merge, PR, keep, or discard | After implementation is verified |

**For committing:** use the `commit-message` skill directly — it generates structured messages with `---agent---` metadata blocks.

## When to Use

- Before any code change: `mode=branch` to get on a feature branch
- Parallel feature work: `mode=worktree` to work in isolation
- Closing out an implementation: `mode=finish`
- Team review: `mode=pr`

## When NOT to Use

- For committing changes — use the `commit-message` skill instead
- Large multi-package implementations — use `skaildev-implement` which calls this skill internally
- Resolving merge conflicts that are conceptual design decisions — escalate to user

---

ROLE  Git operations manager for the skaile-dev monorepo — enforces branch naming, worktree conventions, PR templates, and safe commit practices.

READS
  references/branch_naming.md       — branch naming rules per change type
  references/worktree_patterns.md   — worktree setup and teardown patterns
  git status / git log              — current repository state
  ? _implementation/git-state.json  — existing git state if present

WRITES
  .git/                             — branches, worktrees
  _implementation/git-state.json    — records active branch, mode, worktree path

MUST  derive branch name from description using references/branch_naming.md rules
MUST  check for dirty working tree before creating branches or worktrees
MUST  require typed confirmation for destructive operations (force-delete, discard)
MUST  write git-state.json when branch or worktree is created
MUST  run full test suite before any merge to main
NEVER force-push or rewrite published history
NEVER merge to main without tests passing
NEVER delete a branch without typed confirmation
NEVER create a worktree for sequential (non-parallel) work — use branch mode instead

EMIT [skaildev-git-workflow] started mode=<mode>

# ── Mode: branch ──────────────────────────────────────────────────

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

  EMIT [skaildev-git-workflow] branch_ready name=<branch-name>

# ── Mode: worktree ────────────────────────────────────────────────

IF mode = worktree

  STEP 1: Determine branch name (same as branch mode)

  STEP 2: Assess if worktree is appropriate
    - Worktrees are for genuinely parallel, file-independent work
    - If both changes touch the same files → recommend branch mode instead
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

  EMIT [skaildev-git-workflow] worktree_ready path=.worktrees/<slug>

# ── Mode: pr ──────────────────────────────────────────────────────

IF mode = pr

  STEP 1: Pre-flight
    $ git status   — must be clean
    - Run full test suite: `bun x --bun vitest run` (or appropriate command for package)
    IF tests fail
      - STOP: "Tests must pass before opening a PR. Fix failing tests first."

  STEP 2: Build PR title and body
    - Title: same format as Conventional Commits message for the branch's primary change
    - Body template (from references/pr_template.md or inline):

    ```
    ## What

    <1–3 sentences: what was changed>

    ## Why

    <1–3 sentences: motivation, problem solved, or feature requested>

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

  EMIT [skaildev-git-workflow] pr_created url=<url>

# ── Mode: finish ──────────────────────────────────────────────────

IF mode = finish

  STEP 1: Pre-flight
    - Read git-state.json: branch, mode, worktree_path
    - Run full test suite
    IF tests fail → STOP

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
      IF not confirmed → STOP
      $ git checkout main
      $ git merge --squash <branch-name>
      $ git commit -m "feat(<scope>): <summary of branch work>"
      $ git branch -d <branch-name>
      IF worktree_path exists → $ git worktree remove <worktree_path>
      - Update git-state.json: status=merged

    CASE pull-request:
      - RUN mode=pr
      IF worktree_path exists → $ git worktree remove <worktree_path>
      - Update git-state.json: status=pr_open

    CASE keep:
      IF worktree_path exists → $ git worktree remove <worktree_path>
      - Update git-state.json: status=kept

    CASE discard:
      - Ask: "Type 'discard' to permanently delete <branch-name>:"
      IF not confirmed → STOP
      IF worktree_path exists → $ git worktree remove <worktree_path> --force
      $ git checkout main
      $ git branch -D <branch-name>
      - Update git-state.json: status=discarded

  EMIT [skaildev-git-workflow] finish_complete action=<merge|pr|keep|discard>

CHECKLIST
  - [ ] Branch name follows naming convention from references/branch_naming.md
  - [ ] Working tree was clean before branch/worktree creation
  - [ ] Commit messages generated via commit-message skill
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
| Writing commit messages manually | Use the `commit-message` skill for structured messages with `---agent---` blocks |

## Integration

- **Called by:** `skaildev-implement` (step 6: git setup; step 11: finish branch)
- **Delegates to:** `commit-message` for all commit message generation
- **Reads:** `references/branch_naming.md`, `references/worktree_patterns.md`
