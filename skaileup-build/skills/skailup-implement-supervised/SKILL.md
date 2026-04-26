---
name: "skailup-implement-supervised"
description: "Supervised subagent-driven implementation. Dispatches one subagent per task from the superpowers plan, enforces spec-compliance review before code-quality review for each task, handles 4-status implementer reports (DONE / DONE_WITH_CONCERNS / NEEDS_CONTEXT / BLOCKED), and manages escalation paths. The primary orchestrator in this domain. Triggers on: 'start implementing', 'run the plan', 'implement supervised', 'dispatch the tasks'."
metadata:
  version: "1.0.0"
  tags:
    - "implement"
    - "supervised"
    - "subagent"
    - "orchestrate"
    - "review"
    - "spec-compliance"
    - "quality"
    - "4-status"
  stage: beta
  source: "MERGED"
  prerequisites:
    files:
      - path: "_implementation/superpowers-plan.md"
        gate: hard
        description: "Superpowers plan required — run write-plan first"
      - path: "_implementation/git-state.json"
        gate: hard
        description: "Git state required — run git-prepare first"
      - path: "_concept/experience/features"
        gate: hard
        description: "Feature specs required for spec compliance review"
        min_entries: 1
  user_inputs:
    dialog:
      - id: "start_from_task"
        label: "Resume from a specific task ID? (leave blank to start from first pending)"
        type: "text"
        required: false
    files: []
  reads_from:
    - "_implementation/superpowers-plan.md"
    - "_implementation/git-state.json"
    - "_concept/experience/features/**/*.md"
    - "_concept/experience/screens/**/*.md"
  writes_to:
    - "_implementation/superpowers-plan.md"
    - "_implementation/decisions.md"
    - "_implementation/progress.json"
---

# Implement Supervised

## Overview

Executes the superpowers plan by dispatching one subagent per task. Each task gets a fresh
agent context with the full task text pasted verbatim — no file reading required.

After each task:
1. **Spec compliance review** — code is read against the feature spec line by line
2. **Code quality review** — only runs if spec compliance passes

The two-stage review is non-negotiable. Quality review on a misbuilt implementation is
wasted work. Spec compliance must always pass first.

**Status handling:** Every subagent reports one of four statuses. This skill handles each
according to the escalation paths in `contracts/subagent_dispatch.md`.

## When to Use

- `superpowers-plan.md` exists and has been approved
- `git-prepare` has been run (implementation branch exists)
- Ready to start or resume supervised implementation

---

ROLE  Supervised implementation orchestrator — dispatches subagents per task, enforces two-stage review, handles 4-status reports.

READS
  _implementation/superpowers-plan.md        — task list with verbatim specs
  _implementation/git-state.json             — branch, git_mode, worktree path
  _concept/experience/features/**/*.md   — for spec compliance review reference
  _concept/experience/screens/**/*.md    — for spec compliance review reference

WRITES
  _implementation/superpowers-plan.md        — task status updates
  _implementation/decisions.md              — DONE_WITH_CONCERNS, BLOCKED logs
  _implementation/progress.json             — feature status tracking

REFERENCES
  contracts/subagent_dispatch.md            — implementer prompt template, status handling
  skaileup-shared/contracts/agent_patterns.md    — Subagent Dispatch pattern

MUST  read superpowers-plan.md before dispatching any task
MUST  paste full task text verbatim into subagent prompt — never refer to plan file
MUST  run spec-compliance review before code-quality review for every task
MUST  log DONE_WITH_CONCERNS and BLOCKED statuses in decisions.md
MUST  update task status in superpowers-plan.md after each task completes
NEVER dispatch multiple tasks to the same subagent (one task = one subagent)
NEVER skip spec-compliance review even if tests pass
NEVER advance to the next task while the current task status is BLOCKED
NEVER modify _concept/ files

# ── Workflow ───────────────────────────────────────────────────────

STEP 1: Read state
  - Read superpowers-plan.md
  - Read git-state.json (branch, git_mode, worktree_path)
  - Read progress.json if exists (resume state)
  - Determine start task: `start_from_task` input OR first pending task

STEP 2: Report start
  > "Starting supervised implementation.
  > Plan: N tasks, M pending, P complete.
  > Starting from: Task <id> — <name>
  > Git mode: <branch | worktree>"

# ── Task Loop ──────────────────────────────────────────────────────

FOR EACH task in superpowers-plan.md (in dependency order, starting from start task):
  IF task.status = done OR skipped → continue to next task

  STEP 3: Prepare dispatch context
    - Create feature branch (branch mode): `feat/<task-slug>`
      $ git checkout implement/<app-slug>
      $ git checkout -b feat/<task-slug>
    - OR: use worktree (worktree mode): subagent works inside .worktrees/<app-slug>/
    - Build subagent prompt using template from contracts/subagent_dispatch.md:
      - Paste task.description verbatim
      - Paste task.relevant_specs verbatim
      - Paste task.tech_context verbatim
      - List task.expert_skills (if any)
      - Include task.acceptance_criteria

  STEP 4: Dispatch subagent
    - Dispatch to fresh agent context at task.model_tier
    - Context: prompt only (no conversation history)
    EMIT [implement-supervised] task_start task=<id> model=<tier>

  STEP 5: Receive status
    - Parse STATUS block from subagent response
    - Handle per contracts/subagent_dispatch.md:

    CASE DONE → advance to STEP 6
    CASE DONE_WITH_CONCERNS
      - Log to _implementation/decisions.md:
        "[YYYY-MM-DD] Task <id> DONE_WITH_CONCERNS: <concerns>"
      - Advance to STEP 6
    CASE NEEDS_CONTEXT
      - Send question to user as standalone message
      - Wait for answer
      - Re-dispatch subagent with answer appended to prompt
      - Loop back to STEP 5
    CASE BLOCKED, route=context
      - Surface reason as standalone message to user
      - Wait for clarification
      - Re-dispatch with clarification appended
      - Loop back to STEP 5
    CASE BLOCKED, route=escalate-model
      - Log to decisions.md: "[YYYY-MM-DD] Task <id> escalated to higher model tier"
      - Re-dispatch at next tier (haiku → sonnet → opus)
      - Loop back to STEP 5
    CASE BLOCKED, route=decompose
      - Ask write-plan to decompose task <id> into sub-tasks
      - Insert sub-tasks into plan after current position
      - Update superpowers-plan.md
      - Resume loop from first sub-task

  STEP 6: Spec compliance review
    - Read the feature spec(s) for this task from _concept/experience/features/
    - Read the code produced by the subagent
    - Check each requirement in the spec against the code — line by line
    - Assume the implementer "finished suspiciously quickly" — do not trust tests alone
    - Verify: every acceptance criterion is present in the code
    - Record: COMPLIANT | NON_COMPLIANT (with list of gaps)
    EMIT [implement-supervised] spec_review task=<id> result=<COMPLIANT|NON_COMPLIANT> gaps=<N>

    IF NON_COMPLIANT
      - Fix gaps directly (or re-dispatch with gap list as additional context)
      - Re-run tests
      - Repeat STEP 6 until COMPLIANT

  STEP 7: Code quality review (runs only after STEP 6 = COMPLIANT)
    - Check test coverage: all new code paths have at least one test
    - Check file boundaries: no bleeding between features
    - Check naming: follows golden_principles.md conventions
    - Check cleanliness: no console.log, TODO, commented-out blocks
    - Record: PASS | FAIL (with issue list)
    EMIT [implement-supervised] quality_review task=<id> result=<PASS|FAIL> issues=<N>

    IF FAIL
      - Fix issues directly
      - Re-run tests
      - Repeat STEP 7 until PASS

  STEP 8: Commit and merge
    $ git add -p   (review staged changes)
    $ git commit -m "feat: <task-name>"
    IF branch mode:
      $ git checkout implement/<app-slug>
      $ git merge --squash feat/<task-slug>
      $ git commit -m "feat: implement <task-name>"
      $ git branch -d feat/<task-slug>
    IF worktree mode:
      - Merge from worktree back to implement/<app-slug>

  STEP 9: Update plan and progress
    - Set task.status = done in superpowers-plan.md
    - Update progress.json
    $ git add _implementation/
    $ git commit -m "chore: mark task <id> complete"
    EMIT [implement-supervised] task_complete task=<id> spec=COMPLIANT quality=PASS

  STEP 10: Run regression tests
    - Run full test suite to catch regressions introduced by this task
    IF any pre-existing tests fail
      - Fix regressions before starting the next task
      - NEVER advance with a broken test baseline

  IF complexity_tier = complex OR every N tasks (configurable, default 3)
    CHECKPOINT progress_check
      > "Tasks complete: N/M
      > Last task: <name>
      > All tests: passing.
      >
      > Continue to the next task?"

END FOR

# ── Final ──────────────────────────────────────────────────────────

STEP 11: Final verification gate
  - Run ALL tests one final time
  - Run full build (backend + frontend + lint)
  EMIT [implement-supervised] completed tasks=<N> spec_reviews=<N> quality_reviews=<N>

STEP 12: Completion summary
  > "All N tasks complete.
  > Spec compliance: N/N passed.
  > Code quality: N/N passed.
  > Tests: all passing.
  > Concerns logged: N (see _implementation/decisions.md)
  >
  > Ready for finish-branch."

CHECKLIST
  - [ ] All tasks dispatched one-at-a-time to fresh subagents
  - [ ] Spec compliance review run for every task
  - [ ] Code quality review run for every task (after compliance)
  - [ ] DONE_WITH_CONCERNS and BLOCKED statuses logged in decisions.md
  - [ ] All regression tests passing after each task
  - [ ] superpowers-plan.md updated with final task statuses
  - [ ] progress.json updated

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Dispatching multiple tasks to one subagent | One task = one subagent. Always. |
| Asking the subagent to read the plan | Paste the task text verbatim. No file reading. |
| Running quality review before spec compliance | Spec compliance is the first gate. Always. |
| Advancing past a BLOCKED task | Never advance while a task is blocked — resolve first |
| Skipping regression tests after a task | Run full test suite after every task. Not just the new ones. |
| Setting all tasks to opus for safety | Use the model tier from the plan — mechanical tasks on haiku saves cost |
