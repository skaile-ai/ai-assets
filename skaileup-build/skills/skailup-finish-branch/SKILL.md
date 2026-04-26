---
name: "skailup-finish-branch"
description: "Controlled branch completion after supervised implementation. Presents four options: merge to main, create pull request, keep branch for later, or discard. Requires explicit confirmation for merge and discard actions. Triggers on: 'finish the branch', 'close out the implementation', 'we're done — what now', 'merge or PR', after implement-supervised completes."
metadata:
  version: "1.0.0"
  tags:
    - "git"
    - "branch"
    - "merge"
    - "pull-request"
    - "finish"
    - "cleanup"
  stage: beta
  source: "MERGED"
  prerequisites:
    files:
      - path: "_implementation/superpowers-plan.md"
        gate: hard
        description: "Plan required to verify all tasks are complete before finishing"
      - path: "_implementation/git-state.json"
        gate: hard
        description: "Git state required for branch name and worktree path"
  user_inputs:
    dialog:
      - id: "finish_action"
        label: "What to do with the implementation branch?"
        type: "select"
        options:
          - "merge"
          - "pull-request"
          - "keep"
          - "discard"
        required: true
        hint: "merge = squash-merge to main now; pull-request = open PR for review; keep = leave branch as-is; discard = delete branch and worktree"
    files: []
  reads_from:
    - "_implementation/superpowers-plan.md"
    - "_implementation/git-state.json"
---

# Finish Branch

## Overview

Closes out the supervised implementation run by handling the implementation branch.

Four options are presented — the user must choose one. Destructive options (merge and discard)
require typed confirmation before executing.

| Option | When to use |
|---|---|
| `merge` | Implementation is verified and ready for main. Squash-merges to main. |
| `pull-request` | Work is done but needs team review. Opens a PR with implementation summary. |
| `keep` | Work is done but not ready to merge yet. Branch stays; worktree cleaned up. |
| `discard` | Implementation is abandoned or superseded. Branch and worktree deleted. |

## When to Use

- After `implement-supervised` completes and all tasks are done
- After a manual decision to close, archive, or abandon an implementation branch

---

ROLE  Branch completion — presents merge / PR / keep / discard options and executes the chosen action with appropriate safeguards.

READS
  _implementation/superpowers-plan.md   — verify all tasks are done
  _implementation/git-state.json        — branch name, git_mode, worktree path
  _implementation/decisions.md          — concerns to include in PR/merge message

MUST  verify all tasks in superpowers-plan.md are done before merge or PR
MUST  require typed "merge" confirmation before squash-merging to main
MUST  require typed "discard" confirmation before deleting the branch
MUST  clean up worktree if git_mode=worktree and action is merge, keep, or discard
NEVER squash-merge without running the full test suite first
NEVER force-push or rewrite git history
NEVER delete the branch without explicit typed confirmation

# ── Workflow ───────────────────────────────────────────────────────

STEP 1: Pre-flight checks
  - Read superpowers-plan.md
  - Count tasks: total, done, pending, blocked
  IF any tasks are pending or blocked
    - STOP. Report: "N tasks are not complete. Finish remaining tasks before closing the branch."
    - List incomplete tasks
  - Read git-state.json: branch, git_mode, worktree_path

STEP 2: Run final tests
  - Run full test suite
  IF any tests fail
    - STOP. Report failing tests. Do not offer merge/PR until tests pass.

STEP 3: Present options
  > "Implementation is complete. All N tasks done, all tests passing.
  >
  > What would you like to do with the implementation branch?
  >
  > 1. **merge** — Squash-merge to main now. Branch is deleted after merge.
  > 2. **pull-request** — Open a pull request for team review.
  > 3. **keep** — Leave the branch as-is for now. Come back to merge or PR later.
  > 4. **discard** — Delete the branch (and worktree if applicable). Work is lost.
  >
  > Choose an option:"

STEP 4: Execute chosen action

  CASE merge:
    - Ask for typed confirmation: "Type 'merge' to confirm squash-merge to main:"
    IF user did not type "merge" exactly
      - STOP. Do not proceed.
    - Build merge commit message:
      ```
      feat: implement <app-name> — supervised build

      Tasks: N completed
      [If DONE_WITH_CONCERNS logged]: Concerns: see _implementation/decisions.md
      ```
    $ git checkout main
    $ git merge --squash implement/<app-slug>
    $ git commit -m "<merge message>"
    $ git branch -d implement/<app-slug>
    IF worktree_path exists:
      $ git worktree remove .worktrees/<app-slug>
    - Update git-state.json: status = merged
    > "Merged to main. Branch implement/<app-slug> deleted."

  CASE pull-request:
    - Build PR body from:
      - superpowers-plan.md task list (as checklist)
      - decisions.md concerns (if any)
      - Test counts
    $ gh pr create \
        --title "feat: implement <app-name>" \
        --body "<PR body>" \
        --base main \
        --head implement/<app-slug>
    IF worktree_path exists:
      $ git worktree remove .worktrees/<app-slug>
      (worktree removed; branch stays open for PR review)
    > "Pull request opened: <PR URL>
    > The implementation branch remains open for review."

  CASE keep:
    IF worktree_path exists:
      $ git worktree remove .worktrees/<app-slug>
      > "Worktree removed. Branch implement/<app-slug> kept."
    ELSE
      > "Branch implement/<app-slug> kept. Run finish-branch again when ready."
    - Update git-state.json: status = kept

  CASE discard:
    - Ask for typed confirmation: "Type 'discard' to permanently delete this branch:"
    IF user did not type "discard" exactly
      - STOP. Do not proceed.
    IF worktree_path exists:
      $ git worktree remove .worktrees/<app-slug> --force
    $ git checkout main
    $ git branch -D implement/<app-slug>
    - Update git-state.json: status = discarded
    > "Branch implement/<app-slug> deleted. Implementation work has been discarded."

CHECKLIST
  - [ ] All tasks verified complete before merge or PR
  - [ ] Full test suite passed before merge or PR
  - [ ] Typed confirmation received for merge and discard
  - [ ] Worktree cleaned up if git_mode=worktree
  - [ ] git-state.json updated with final status

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Merging without checking all tasks are done | Always count pending/blocked tasks first |
| Merging without typed "merge" confirmation | Always require it — prevents accidental merges |
| Forgetting to remove the worktree | Always clean up the worktree on merge, keep, or discard |
| Using `git branch -D` without typed "discard" confirmation | Require the typed word — lost work destroys trust |
