---
name: "skailup-git-prepare"
description: "Prepare the git repository for a supervised implementation run. Initializes git if needed, creates the implementation branch, and optionally sets up a git worktree (Claude Code / local mode only). Run before brainstorm or write-plan. Triggers on: 'set up git', 'prepare the repo', 'create implementation branch', no git repo found before implementation starts."
metadata:
  version: "1.0.0"
  tags:
    - "git"
    - "setup"
    - "worktree"
    - "branch"
    - "prepare"
    - "repository"
  stage: beta
  source: "MERGED"
  prerequisites:
    files:
      - path: "_concept/discovery/brief.md"
        gate: hard
        description: "Brief required for the app slug used in branch naming"
  user_inputs:
    dialog:
      - id: "git_mode"
        label: "Git mode: 'branch' (default, all agents) or 'worktree' (Claude Code local only)"
        type: "select"
        options:
          - "branch"
          - "worktree"
        required: false
        default: "branch"
        hint: "Use 'worktree' only when running Claude Code locally and parallel subagents are expected"
    files: []
---

# Git Prepare

## Overview

Initializes or verifies the git repository and creates a clean implementation branch
(or worktree) ready for the supervised implementation run.

This is a prerequisite for the superpowers workflow — all implementation work happens
on a dedicated `implement/<app-slug>` branch, not on `main`.

## When to Use

- Starting a new supervised implementation run
- Resuming after a git state was lost or corrupted
- Setting up the repo before `brainstorm` or `write-plan`

## When NOT to Use

- Git is already initialized and the branch exists — skip or run in verify-only mode
- You want to add a single feature to an existing branch — use `skaileup-implementation` directly

## Git Mode: Branch vs. Worktree

See `DOMAIN.md` for the full recommendation. Short version:

**Use `branch` mode (default)** unless you are running Claude Code locally AND the tasks
being dispatched are genuinely independent (no shared file writes). Worktrees are overkill
for sequential dispatch.

| Mode | Behavior |
|---|---|
| `branch` | Creates `implement/<slug>` branch from `main`. All work on this branch. |
| `worktree` | Creates `implement/<slug>` branch AND a git worktree at `.worktrees/<slug>`. Subagents are dispatched to work inside the worktree. Worktree is removed after `finish-branch`. |

---

ROLE  Git repository preparation — initializes repo, creates implementation branch, optionally creates worktree.

READS
  _concept/discovery/brief.md   — app name + slug for branch naming
  ? _implementation/PLANS.md                 — detect if resuming

WRITES
  .git/                                      — git init if not present
  .gitignore                                 — created if not present (standard template)
  _implementation/git-state.json             — records branch, mode, worktree path (if any)

MUST  read brief.md to derive the app slug before creating branches
MUST  check if the implementation branch already exists — do not recreate if resuming
MUST  record git_mode and branch name in _implementation/git-state.json
MUST  use `branch` mode by default unless git_mode=worktree is explicitly set
NEVER create a worktree if git_mode=branch
NEVER force-push or rewrite git history
NEVER commit application code in this skill — only git structure setup

# ── Workflow ───────────────────────────────────────────────────────

STEP 1: Read brief
  - Read _concept/discovery/brief.md
  - Extract: app_name, app_slug (lowercase, hyphenated)

STEP 2: Check git state
  IF .git/ does not exist
    $ git init
    $ git add .gitignore (create standard Node/Bun .gitignore if absent)
    $ git commit -m "chore: init repository"
  ELSE
    - Verify git is clean (no uncommitted changes that would block branching)
    IF dirty working tree
      - STOP. Report: "Git working tree is dirty. Commit or stash changes before starting."

STEP 3: Check for existing implementation branch
  IF branch `implement/<app-slug>` already exists
    - Check if _implementation/PLANS.md exists → resuming mode
    - Report: "Resuming implementation on existing branch implement/<app-slug>"
    $ git checkout implement/<app-slug>
    - Skip to STEP 5
  ELSE
    $ git checkout -b implement/<app-slug>

STEP 4: Initial commit (new branch only)
  $ git commit --allow-empty -m "chore: start supervised implementation of <app-name>"

STEP 5: Worktree setup (worktree mode only)
  IF git_mode = worktree
    IF .worktrees/<app-slug>/ does not exist
      $ git worktree add .worktrees/<app-slug> implement/<app-slug>
      - Add .worktrees/ to .gitignore (worktrees are local, not committed)
    - Record worktree_path: ".worktrees/<app-slug>" in git-state.json
    - Report: "Worktree created at .worktrees/<app-slug>"

STEP 6: Write git-state.json
  - Write _implementation/git-state.json:
    {
      "app_slug": "<slug>",
      "branch": "implement/<slug>",
      "git_mode": "branch | worktree",
      "worktree_path": ".worktrees/<slug>" | null,
      "created_at": "<ISO timestamp>",
      "status": "ready"
    }
  $ git add _implementation/git-state.json
  $ git commit -m "chore: record git state for supervised implementation"

CHECKLIST
  - [ ] brief.md read, app_slug derived
  - [ ] Git repo initialized or verified
  - [ ] Implementation branch created or verified (implement/<slug>)
  - [ ] Worktree created if git_mode=worktree
  - [ ] git-state.json written and committed

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Creating branch with uncommitted changes on main | Stash or commit first; clean working tree is required |
| Using worktree mode for sequential subagent work | Use branch mode; worktrees add overhead without benefit for sequential dispatch |
| Forgetting to write git-state.json | Downstream skills read this file; always write it |
