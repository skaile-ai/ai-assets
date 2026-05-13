---
name: "bug-fix"
description: "[skaile-development] End-to-end bug-fix orchestrator for the Skaile platform. Creates an isolated worktree + branch, registers the bug in platform/issues.md (status=testing, owner=current git user), investigates root cause, writes an uncommitted plan, dispatches a fresh agent to implement, dispatches a fresh agent to review, fixes valid review concerns, cleans up the plan, commits + pushes the branch, removes the worktree, opens a PR into main, and reports the PR link. Use when the user reports a platform bug and wants the whole fix-cycle done in one shot."
metadata:
  version: "1.0.0"
  tags:
    - "bug-fix"
    - "platform"
    - "orchestrator"
    - "worktree"
    - "issues"
    - "pr"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  user_inputs:
    dialog:
      - id: "bug_description"
        label: "Describe the bug (plain language — the skill refines the wording, picks the category, and files it without asking)"
        type: "text"
        required: true
      - id: "category"
        label: "Issue category override (rarely needed — auto-derived from description)"
        type: "select"
        options:
          - "B"
          - "I"
          - "UI"
        required: false
        hint: "Only set this to override the auto-classification (B=Bug, I=Issue, UI=cosmetic). Leave empty in the typical case."
      - id: "branch_slug"
        label: "Branch slug override (auto-derived from description)"
        type: "text"
        required: false
        hint: "Only set if you want a specific branch name. Leave empty in the typical case."
      - id: "complexity"
        label: "Complexity hint"
        type: "select"
        options:
          - "small"
          - "standard"
        required: false
        default: "standard"
        hint: "small = obvious, contained fix (skips the review-subagent step) | standard = needs review cycle"
    files: []
---

# Bug-Fix — End-to-End Platform Bug-Fix Orchestrator

## Overview

Drives a single platform bug from "user just reported it" to "PR is open" without leaving the
main session dirty. The skill works exclusively against the **`platform/` submodule** — it
creates a worktree of that submodule, branches off main, files the bug in `platform/issues.md`,
investigates, plans, dispatches implementation + review subagents, lands the fix on the branch,
cleans up, and opens a PR on `skaile-ai/skaile-platform`.

| Phase | What Happens |
|-------|--------------|
| 0 | Read inputs, resolve current git user, auto-classify category, allocate next ID |
| 1 | Pre-flight platform main, derive branch name |
| 2 | Create the worktree + branch (outside `platform/` tree, branched from `origin/main`) |
| 2b | Stage the bug row in `issues.md` **on the branch** (not on main) |
| 3 | Investigate: load relevant files, identify root cause |
| 4 | Write an **uncommitted** plan markdown into the worktree |
| 5 | Dispatch a fresh subagent to implement the plan (gets only the plan + context — no parent history) |
| 6 | Dispatch a fresh subagent to review the diff |
| 7 | Triage review findings, apply valid fixes inline |
| 8 | Delete the plan file; commit (fix + issues.md row together); push the branch |
| 9 | Fetch latest `origin/main`, merge it into the branch, resolve conflicts, re-push |
| 10 | Remove the worktree |
| 11 | Open a PR against `main` and report the link |

## When to Use

- The user describes a bug observed in the platform UI / backend / agent runtime
- You want the bug to land in `issues.md` (with a refined description) and reach a PR in one continuous flow
- The bug is contained enough that one implementation pass + one review pass is plausible — not a multi-week refactor

## When NOT to Use

- Pure feature work — use `implement` instead
- Bugs outside the `platform/` submodule (e.g., `agent-framework/`, `forge/`) — adapt manually or extend this skill
- Anything that needs design discussion before code — file a proposal first
- Something already on a branch — this skill expects a clean start

---

## Operating Principle — Autonomy by Default

**Run end-to-end without asking the user.** The user already gave you the bug report;
they expect a PR link back, not a flurry of checkpoint prompts. Decisions you can make
on your own based on the evidence — classification (B / I / UI), description wording,
branch name, plan structure, which review findings to apply, conflict resolutions where
one side is a strict superset — you make. The user does not need to approve those.

Only stop and ask when continuing would require a judgment call the evidence cannot
settle on its own. The complete list of legitimate stop-and-ask gates is:

1. **`git config user.name` is unset** — the Owner column would be empty.
2. **Working tree is dirty** — the issues.md commit on main would mix in unrelated work.
3. **Investigation inconclusive** — root cause cannot be located; report findings and stop.
4. **A review finding is "apply-large"** — spawning a second implementation subagent is
   a meaningful escalation; confirm the scope first.
5. **A merge conflict on source code is genuinely mutually-exclusive** — the fix and
   main's incoming change cannot both stand; the user must pick.
6. **Push fails for a non-trivial reason** — e.g., branch was rewritten remotely.

Everything else — refining the bug description, picking the category, choosing the
branch slug, writing the plan, dispatching the implementer, applying nit-level review
findings, deferring out-of-scope findings, merging main when one side is a strict
subset, opening the PR — you do silently. Print short progress notes (`> "Filing as
B-54 (auto-classified as Bug)"`, `> "Plan written, dispatching implementer..."`) but
do NOT solicit confirmation.

The user can always interrupt; they will if they want to redirect.

---

ROLE  Bug-fix orchestrator for the `platform/` submodule. Owns the full cycle: worktree → issues.md entry → plan → implement → review → triage → commit → push → cleanup → PR.

READS
  platform/issues.md                              — current bug/issue tables, ID allocation
  platform/CLAUDE.md                              — platform architecture, conventions
  platform/<files-relevant-to-bug>                — for investigation
  skaile-dev/CLAUDE.md                            — monorepo conventions
  skills/git/references/branch_naming.md          — branch naming
  skills/git/references/commit-spec.md            — commit message format for platform submodule
  git config user.name                            — for the `Owner` column

WRITES (in the worktree only, except step 2 which commits to platform main)
  platform/issues.md                              — new row in the matching table (committed to main BEFORE branching off)
  <worktree>/<plan-file>.md                       — uncommitted plan (deleted before the final commit)
  <worktree>/<target-source-files>                — the actual fix
  PR on skaile-ai/skaile-platform                 — opened at the end

MUST  refine the user's bug description before filing it — 1-3 sentences, technical but specific, no marketing fluff
MUST  auto-classify the category (B / I / UI) from the description without asking the user; the `category` user input only overrides the auto-classification
MUST  resolve the next free ID in the chosen category by scanning the existing table (highest +1)
MUST  set Status=testing and Owner=`git config user.name` in the new row
MUST  write the issues.md row INSIDE the worktree, on the feature branch — never commit it to platform main directly; the row reaches main only when the PR is merged
MUST  create the worktree outside platform/ (e.g., /tmp/platform-<id>-worktree) so the parent platform/ stays clean
MUST  write the plan file in the worktree and treat it as ephemeral — never commit it, delete it before the final commit
MUST  dispatch implementation as a fresh subagent with a self-contained prompt (MVC: paste the plan + relevant file paths + acceptance criteria; do NOT pass parent conversation)
MUST  dispatch review as a fresh subagent that only sees the diff + review focus — not the plan
MUST  triage review findings explicitly: each finding gets a verdict (apply / defer / reject) with a one-line reason
MUST  apply valid findings inline in the orchestrator (no second implementation dispatch unless the finding is large enough to warrant a sub-plan)
MUST  push the branch to origin before opening the PR (worktree-only commits are local-only)
MUST  fetch origin/main and merge it into the branch (inside the worktree) BEFORE removing the worktree — keeps the PR diff clean and resolves any conflicts while the full local context is available
MUST  resolve merge conflicts that arise from the main-sync merge: prefer policy-based resolution (issues.md, generated files, lockfiles) but blend manually for source-code conflicts in the fix's blast radius; ASK the user only on genuinely mutually-exclusive intents
MUST  re-push the branch after the main-sync merge completes
MUST  remove the worktree only after both the initial push AND the post-sync push succeed — never before
MUST  use `gh pr create --base main --head <branch>` from the platform repo to open the PR
MUST  report back to the user with: branch name, PR URL, 1-2 line summary of what changed, any deferred review findings
NEVER  modify the main platform/ checkout while the worktree exists
NEVER  commit the issues.md row to platform main directly — it lives on the branch and travels via the PR
NEVER  ask the user to confirm the refined description, the category, the branch name, the plan, or the triage table — those are decisions you make; the user gave you the bug report and expects a PR back
NEVER  commit the plan file
NEVER  pass the parent conversation transcript to the implementation or review subagent — both must be fresh and self-contained
NEVER  open a PR before the branch is pushed
NEVER  remove the worktree if `git -C <worktree> status` shows unpushed commits
NEVER  force-push, rewrite history, or commit to platform main from inside the worktree
NEVER  run Biome inside platform/ — platform uses Prettier + ESLint exclusively

EMIT [bug-fix] started description="<short>"

# ── Phase 0: Inputs and Identity ──────────────────────────────────

STEP 0: Resolve inputs
  - Read bug_description (required)
  - Read complexity (default "standard")
  - $ git config user.name → owner_name
  IF owner_name is empty
    STOP: "git config user.name is not set. Set it before running this skill."

  Auto-classify the category from the bug_description (do NOT ask the user):

  | Signal in description | category | section heading | ID prefix |
  |-----------------------|----------|-----------------|-----------|
  | Broken feature, error message, data loss, security, wrong result, crash, regression in functionality | B | `## Bugs` | B- |
  | Missing functionality, behavior that should change but isn't strictly broken, design-level "should also" | I | `## Issues` | I- |
  | Purely visual: layout, spacing, color, font, alignment, tooltip wording, hover state, icon | UI | `## UI / UX Issues` (match the actual heading in the file) | UI- |

  Tie-breakers:
  - Behavior breakage trumps visual concern → B
  - "Should also be able to X" with no current breakage → I
  - Cosmetic-only with no functional impact → UI

  IF the `category` user input is provided, it OVERRIDES the auto-classification.
  Otherwise use the auto-classification silently and print one line:
    > "Auto-classified as <category> (<one-word reason>)."

  IF the chosen category's section heading does not exist in platform/issues.md
    STOP: "issues.md has no section matching category=<category>. The section needs to be added manually first."

# ── Phase 1: Refine Description + Allocate ID ─────────────────────

STEP 1: Refine the description
  Treat the user's input as a draft, not the final wording.
  Produce a refined description that:
  - Is 1-3 sentences total (hard cap — never longer)
  - Names the affected surface explicitly (e.g., "workspace chat panel", "project settings page")
  - States the observed behavior and, if relevant, the expected behavior
  - Drops filler ("I noticed that", "sometimes maybe")
  - Preserves any user-stated constraint verbatim (e.g., "open question for Peter: <question>") — wrap such constraints in **bold** so they are visually distinct

  Examples (style reference — adapt to the actual bug):
  - "Long inline-code URLs in agent chat messages don't wrap when they contain no `-`, causing horizontal scroll on narrow chat panes."
  - "Project Settings tabs lose data when the URL uses the project slug instead of the UUID — tabs query by id only. **open question for Peter**: should slug-form URLs be canonical?"

  **Do NOT ask the user to approve the wording.** Write it directly. The user will see
  it in the resulting issues.md row and in the PR body — minor wording tweaks can be
  made later by editing the row. Only stop if the user's input is too vague to refine
  (in which case ask one targeted clarification question, not a yes/no on a proposal).

STEP 2: Pick the next free ID
  Scan platform/issues.md for all rows matching ID prefix `<category>-`.
  next_id = max(existing numeric suffixes) + 1, zero-padded to 2 digits.
  full_id = "<prefix><next_id>"  e.g. "B-54", "I-30", "UI-34"

EMIT [bug-fix] id_allocated id=<full_id> owner=<owner_name>

# ── Phase 2: Pre-flight + Branch + Worktree ───────────────────────

STEP 3: Pre-flight against platform main
  Working dir: <skaile-dev-root>/platform

  $ cd <skaile-dev-root>/platform
  $ git fetch origin
  $ git status --short
  IF dirty:
    STOP: "platform/ has uncommitted changes. Stash or commit them before running bug-fix."
    (Legitimate stop-and-ask gate #2 — the dirty state would either travel into
    the worktree or block the worktree creation; either way the user must decide.)

  We do NOT need to checkout main here — the worktree below branches off
  origin/main regardless of which branch the parent checkout currently points at.

STEP 4: Branch name
  Derive silently:
    IF branch_slug provided:
      slug = branch_slug
    ELSE:
      Build slug from bug_description: lowercase, hyphens, max 40 chars,
      strip articles, keep concrete nouns.

  branch_name = "fix/<full_id_lowercase>-<slug>"
    e.g. "fix/b-54-chat-link-wrap"

STEP 5: Worktree
  worktree_path = "/tmp/platform-<full_id_lowercase>-worktree"

  $ cd <skaile-dev-root>/platform
  $ git worktree add <worktree_path> -b <branch_name> origin/main
  > "Worktree created at <worktree_path> on branch <branch_name> (based on origin/main, which now contains the issues.md row)."

  All subsequent file operations until Phase 9 happen INSIDE <worktree_path>.

EMIT [bug-fix] worktree_ready path=<worktree_path> branch=<branch_name>

# ── Phase 3b: File the Bug Row on the Branch ──────────────────────

STEP 5b: Write the issues.md row inside the worktree
  The row lands on the feature branch and travels to main when the PR is
  merged. It does NOT go to platform main directly — keeps main free of
  rows for fixes that never ship.

  $ cd <worktree_path>

  EDIT issues.md:
    - Locate the section heading matching the auto-classified category
      (`## Bugs` / `## Issues` / `## UI / UX Issues`).
    - Append a new row at the end of that section's table.
    - Match the surrounding rows' column padding (look at the longest
      existing row in the section and align to the same pipe positions).
    - Row format: `| <full_id> | <refined_description> | testing | <owner_name> |`

  STAGE the change but do NOT commit yet — the row lands in the same
  commit as the actual fix at the end (Phase 9), keeping the branch
  to a single commit when the diff is small. (If the fix grows, the
  row can be split into its own commit during Phase 9 — but never two
  commits for the same row+fix.)

  $ git add issues.md

  Print a one-line status:
    > "Filed <full_id> as <category> (testing / <owner_name>) on branch <branch_name>."

EMIT [bug-fix] issue_filed id=<full_id> location=branch

# ── Phase 4: Investigate ─────────────────────────────────────────

STEP 6: Investigate
  Read in this order (stop when the root cause is clear):
  1. platform/CLAUDE.md sections relevant to the bug's surface area
  2. The exact file(s) the user named, if any
  3. Adjacent files following the bug's data flow (e.g., page → hook → tRPC route → service → repo)
  4. Recent commits touching the same area (`git log --oneline -- <path>`)

  Produce internally (not yet written to disk):
  - root_cause: 1-2 sentences naming the precise mechanism
  - blast_radius: which files / which user paths are affected
  - fix_candidates: 1-3 plausible fix shapes with trade-offs
  - chosen_fix: which candidate to apply, with one-line justification

  IF root cause cannot be located with confidence:
    REPORT to user: "Investigation inconclusive. <what I checked>. <what would help>."
    STOP — do not proceed to plan.

EMIT [bug-fix] investigated root_cause="<short>"

# ── Phase 5: Write Plan (uncommitted) ─────────────────────────────

STEP 7: Write the plan
  plan_path = "<worktree_path>/<full_id>-plan.md"

  Write a plan with this structure:

  ```
  # <full_id> — <bug-title> — Implementation Plan

  ## Bug Summary
  <refined description from STEP 1, verbatim>

  ## Root Cause
  <1-2 sentences>

  ## Files to Touch
  - <path 1> — <what changes there>
  - <path 2> — <what changes there>
  - tests: <test file path or "add new test at <path>">

  ## Implementation Steps
  1. <imperative step>
  2. <imperative step>
  3. <imperative step>

  ## Acceptance Criteria
  - [ ] <observable behavior 1>
  - [ ] <observable behavior 2>
  - [ ] Lint passes (`cd backend && bun run lint` / `cd frontend && bun run lint`)
  - [ ] No regression to <adjacent feature>

  ## Out of Scope
  - <thing intentionally not changed>

  ## Notes for the Implementer
  <anything non-obvious about platform conventions — e.g., "all
  exported services need @Injectable() decorator", "tRPC routes
  must use authMiddleware", "don't add barrel files in libs/">
  ```

  DO NOT commit this file. It lives only in the worktree's working dir.

  Print a one-line status (NOT a prompt — do not wait for input):
    > "Plan written. Dispatching implementer."

# ── Phase 6: Implementation Dispatch (fresh subagent) ─────────────

STEP 8: Dispatch implementation
  Build an MVC prompt for the implementer. The prompt MUST be self-contained:

  ```
  # Task: Fix <full_id> in the platform submodule

  ## Working Directory
  <worktree_path>

  All file paths below are relative to that directory. The branch
  <branch_name> is already checked out — make commits there.

  ## Bug
  <refined description from STEP 1>

  ## Root Cause
  <root_cause from STEP 6>

  ## Plan
  <paste the FULL content of <plan_path> here>

  ## Platform Conventions (do not violate)
  - NEVER run Biome — platform uses Prettier + ESLint. Use `bun run lint`
    inside `backend/` or `frontend/`.
  - NEVER create barrel `index.ts` files under `backend/libs/` — break NestJS DI.
  - Use direct subpath imports (e.g., `@credential/credential.service`,
    not `@credential`).
  - Follow @custom-start / @custom-end markers when modifying generated files.
  - tRPC routes: use `authMiddleware` and `ctx.viewer.user.id` for ownership fields.

  ## What to Implement
  Follow the plan exactly. Apply each step. Run the lint command in
  any package you touch. Verify acceptance criteria pass.

  ## What NOT to Do
  - Do NOT delete or edit <plan_path> — the orchestrator owns plan lifecycle.
  - Do NOT commit anything yet. Leave changes staged or unstaged — the
    orchestrator will commit after review.
  - Do NOT push the branch.
  - Do NOT open a PR.
  - Do NOT touch issues.md — its row is already staged in the worktree;
    the orchestrator will commit it together with your fix at the end.

  ## Output
  When done, report:
  - Files changed (relative paths)
  - Acceptance criteria you verified
  - Anything you noticed that wasn't in the plan
  ```

  DISPATCH via Agent tool:
    Agent({
      subagent_type: "implement",   // or "general-purpose" if implement is not appropriate for this scope
      description: "Implement <full_id> fix",
      prompt: "<the MVC prompt above>"
    })

  WAIT for the agent to return.
  Capture: files_changed, criteria_verified, agent_notes.

EMIT [bug-fix] implementation_done files=<N>

# ── Phase 7: Review Dispatch (fresh subagent) ─────────────────────

STEP 9: Dispatch review
  IF complexity = small AND files_changed ≤ 2 AND total LOC ≤ 30:
    Skip the review subagent — the orchestrator reviews the diff inline.
    Set review_findings = [].
    Continue to STEP 10.

  Otherwise dispatch a review subagent with a tight MVC prompt:

  ```
  # Task: Review the staged/unstaged diff in <worktree_path>

  ## Working Directory
  <worktree_path>

  ## Context
  This worktree contains a fix for <full_id>: <refined description>.

  ## What to Review
  Run `git diff` inside the worktree. Review for:
  - Correctness: does the change actually fix the described bug?
  - Scope: are there changes that aren't related to the bug?
  - Platform conventions:
    * No Biome inside platform/ (Prettier + ESLint only)
    * No barrel `index.ts` in backend/libs/
    * tRPC routes use authMiddleware
    * @custom-start / @custom-end markers respected for generated files
    * Direct subpath imports (e.g., `@credential/credential.service`)
  - Test coverage: is there a test that locks in the fix?
  - Performance / N+1 / unbounded loops
  - Security (input validation at boundaries; no secrets in code)

  ## What NOT to Report
  - Style issues that Prettier/ESLint would catch
  - Pre-existing issues in files the diff didn't introduce
  - Anything outside the diff
  - More than 5 nits — summarise if more

  ## Output Format
  Findings as a list, each:
    severity: important | nit | preexisting
    file:line: <one-line summary>
    detail: <1-2 sentences>

  End with a one-line recommendation: "ready to merge" | "fix important findings first" | "needs broader change".
  ```

  DISPATCH:
    Agent({
      subagent_type: "review",
      description: "Review <full_id> fix",
      prompt: "<the MVC prompt above>"
    })

  WAIT for the review to return. Parse review_findings list.

EMIT [bug-fix] review_done important=<N> nits=<N>

# ── Phase 8: Triage + Apply ───────────────────────────────────────

STEP 10: Triage review findings
  For each finding in review_findings, decide silently:

  | Verdict | When | Action |
  |---------|------|--------|
  | apply   | Important + the finding is real + fix is ≤ 20 LOC | Implement inline in this orchestrator |
  | apply-large | Important + fix needs ≥ 20 LOC or new abstraction | ASK user before dispatching a second implementation subagent (legitimate stop-and-ask gate #4) |
  | defer   | Nit OR not blocking OR out of scope for this PR | Note it in the PR description under "Deferred follow-ups" |
  | reject  | Disagreement with the reviewer | Note it in the PR description with one-line reasoning |

  Default behavior: do NOT ask the user. Apply the `apply` items immediately; record
  `defer` and `reject` items for the PR body; proceed.

  ASK the user only when:
    (a) At least one finding triages as `apply-large` (would spawn another subagent), OR
    (b) You are rejecting an `important` severity finding (judgment call — record the
        reasoning in the PR but flag it to the user first)

  IF asking is warranted, present the relevant subset only (not the full table):
    > "Review flagged <N>:
    >  apply-large: <finding summary> — proceed with another implementation pass? (yes/skip)
    >  rejecting important: <finding summary> — reason: <one line>. Override and apply instead? (no/yes)"

  Otherwise print a one-line status:
    > "Triaged: <N applied> / <N deferred> / <N rejected>. Continuing."

EMIT [bug-fix] triage_done applied=<N> deferred=<N> rejected=<N>

# ── Phase 9: Commit, Cleanup, Push ────────────────────────────────

STEP 11: Drop the plan, lint, commit, push
  $ cd <worktree_path>

  $ rm -f <full_id>-plan.md

  Lint affected areas:
    IF backend/ files changed: $ cd backend && bun run lint
    IF frontend/ files changed: $ cd frontend && bun run lint
    IF lint reports unfixable errors:
      STOP: "Lint errors — fix before committing. Worktree preserved at <worktree_path>."
    IF lint mutated files: $ git add <mutated files>

  Stage and commit:
    $ git add -A
    $ git status --short    (verify <full_id>-plan.md is gone)
    IF any plan file appears in status:
      STOP: "Plan file still tracked — abort and investigate."

    The staged set now contains BOTH the fix changes AND the issues.md row
    (staged earlier in STEP 5b). They go into one commit so the row and
    the fix arrive on main together via the PR.

    Commit message (platform repo conventional-commits format, no
    skaile-dev agent block since this is a submodule):
    ```
    fix(<scope>): <title — what the fix does> (<full_id>)

    <1-3 sentence explanation: root cause + fix approach + acceptance>

    Adds <full_id> row to issues.md (status: testing, owner: <owner_name>).
    ```

    $ git commit -m "<message>"

  Push:
    $ git push -u origin <branch_name>
    IF push fails:
      STOP: "Push failed — worktree preserved at <worktree_path>. Investigate."

EMIT [bug-fix] pushed branch=<branch_name>

# ── Phase 9b: Sync Branch with Main ───────────────────────────────

STEP 11b: Fetch main and merge into the branch
  Origin/main may have advanced since the worktree was created (other PRs
  landed during investigation, implementation, or review). Merging main
  in NOW — inside the worktree, while we still have the full local
  context — keeps the eventual PR diff clean and avoids surprising the
  reviewer with a non-fast-forward state. Conflicts are also easier to
  resolve here than in the GitHub UI.

  $ cd <worktree_path>

  Fetch latest main:
    $ git fetch origin main

  Check if anything moved:
    $ git log HEAD..origin/main --oneline → incoming_commits
    IF incoming_commits is empty:
      > "Branch is already up-to-date with origin/main. Skipping merge."
      Continue to STEP 12.

  Merge:
    $ git merge origin/main --no-edit

  IF merge succeeds without conflict:
    Continue to "After-merge verification" below.

  IF merge has conflicts:
    $ git status --short | grep '^UU' → conflicted_files

    Conflict resolution policy (per file):

    | File pattern | Default resolution |
    |--------------|-------------------|
    | `issues.md` | Blend automatically: keep HEAD's `<full_id>` row, take main's column padding + any other rows main added or status-bumped. The HEAD row is unique to this branch and must survive; main's other changes are unrelated and must also survive. Use `--theirs` only if main contains a row for the SAME `<full_id>` (concurrent fix — escalate to user). |
    | Generated PostXL files (in `postxl-lock.json`) | `git checkout --theirs` — let main's regen win; rerun `bun run generate` if needed |
    | Lockfiles (`bun.lock`, `package-lock.json`) | `git checkout --theirs` then `bun install` to re-resolve |
    | Source code touched by both this fix and main | **Blend manually** — keep HEAD's fix logic + integrate main's new behavior. Never blindly take one side. |

    FOR EACH conflicted file:
      Read both sides of the conflict markers.
      Decide: theirs / ours / blend.

      IF the file is on the policy table above AND policy is unambiguous:
        Apply the policy decision directly.
        Note the resolution: "<file>: <theirs|ours|blend> — <one-line reason>"

      IF the conflict is in code the fix actually touches (source files in the
         fix's blast radius) OR the policy is ambiguous:
         The orchestrator MUST resolve manually:
           - Read both sides carefully
           - Identify what HEAD's diff was trying to achieve (the bug fix)
           - Identify what main's incoming change is trying to achieve
           - Produce a blend that preserves BOTH intents
           - If genuinely conflicting (the fix and main's change are
             mutually exclusive), ASK the user:
             > "<file>: HEAD does <X>, main does <Y>. They conflict.
             >  Choose: 1=keep HEAD, 2=take main, 3=I'll explain a blend."

      Verify no markers remain:
         $ grep -nE '^(<<<<<<<|=======|>>>>>>>)' <file>; echo "(empty = clean)"
         IF markers remain → STOP, fix and re-check.

      $ git add <file>

    Run lint on resolved files in the fix's blast radius:
      IF backend/ files changed in the merge: $ cd backend && bun run lint
      IF frontend/ files changed in the merge: $ cd frontend && bun run lint
      IF a formatter rewrites a resolved file: $ git add <file>

    Complete the merge:
      $ git commit --no-edit
      IF pre-commit hook fails on formatting (Prettier check):
        $ cd <pkg> && bun prettier --write <files-the-hook-complained-about>
        $ git add <those files>
        $ git commit --no-edit

  After-merge verification:
    $ grep -rnE '^(<<<<<<<|=======|>>>>>>>)' --include='*.ts' --include='*.tsx' --include='*.md' .
    IF any output → STOP: "Conflict markers still present — abort and investigate."

  Push the merge:
    $ git push origin <branch_name>
    IF push fails (e.g., non-fast-forward because main moved AGAIN during merge resolution):
      $ git fetch origin main
      Repeat the merge loop above against the new origin/main.
      $ git push origin <branch_name>
    IF push fails for any other reason:
      STOP: "Push failed after sync — worktree preserved at <worktree_path>. Investigate."

  Report a one-line summary of what landed:
    > "Synced with main: <N> incoming commits merged. Conflicts: <list of files + resolution>. Pushed."

EMIT [bug-fix] synced_with_main incoming=<N> conflicts=<N>

# ── Phase 10: Remove Worktree ─────────────────────────────────────

STEP 12: Remove worktree
  $ cd <skaile-dev-root>/platform
  $ git worktree list    (sanity check — find <worktree_path>)
  $ git -C <worktree_path> status --short
  $ git -C <worktree_path> log origin/<branch_name>..HEAD --oneline
  IF the log output is non-empty (unpushed commits):
    STOP: "Worktree has unpushed commits — refusing to remove. Push them first."
  $ git worktree remove <worktree_path>

EMIT [bug-fix] worktree_removed

# ── Phase 11: Open PR ─────────────────────────────────────────────

STEP 13: Open the PR
  $ cd <skaile-dev-root>/platform
  $ gh pr create \
      --base main \
      --head <branch_name> \
      --title "fix(<scope>): <title> (<full_id>)" \
      --body "$(cat <<'EOF'
## What

<1-3 sentences: what changed>

## Why

Fixes <full_id> in `platform/issues.md`: <refined description>

## Root Cause

<1-2 sentences>

## Changes

- <bullet list of meaningful changes — file by file>

## Testing

- [ ] Tests pass locally
- [ ] Manually verified <observable behavior>
- [ ] No regression in <adjacent feature>

## Deferred Follow-ups

[only if STEP 10 produced any defer/reject items]
- <finding> — <reason>

## Notes

<optional: open questions, follow-up issues>
EOF
)"

  Capture the PR URL from gh output.

EMIT [bug-fix] pr_opened url=<pr_url>

# ── Phase 12: Report ──────────────────────────────────────────────

STEP 14: Final report to user
  Print this exact block:

  ```
  ── Bug-Fix Complete ────────────────────────────────────────────

  Issue:   <full_id> — <short title>
  Branch:  <branch_name>
  PR:      <pr_url>

  What changed:
  <1-2 sentence plain-language summary>

  Files touched (<N>):
  - <file 1>
  - <file 2>
  …

  Review:
    Findings: <total>   Applied: <N>   Deferred: <N>   Rejected: <N>
    [if deferred or rejected: brief list]

  Worktree: removed
  ────────────────────────────────────────────────────────────────
  ```

EMIT [bug-fix] complete id=<full_id> url=<pr_url>

# ── Procedures ────────────────────────────────────────────────────

PROCEDURE refine_description(raw_description)
  - Cap at 3 sentences
  - Cap at ~280 chars total
  - Lead with the observable surface (where the user sees it)
  - State observed vs expected if non-obvious
  - Preserve user-stated constraints verbatim (bold them if they
    require human follow-up — e.g., "**open question for Peter**: …")
  - No emojis, no hedging adverbs, no "the user reports that"
  - Return: refined_text

PROCEDURE next_id_in_section(category)
  - Read platform/issues.md
  - Find the section heading matching category
  - Collect all IDs of the form `<prefix>-NN` in that section's table
  - Return prefix + (max(NN) + 1, zero-padded to 2 digits)

PROCEDURE triage_finding(finding)
  - severity=important + scope=in-diff + fix-size ≤ 20 LOC → apply
  - severity=important + scope=in-diff + fix-size  > 20 LOC → apply-large
  - severity=nit → defer
  - severity=preexisting → defer (note in PR body)
  - finding disputed → reject (with reason in PR body)

CHECKLIST
  - [ ] git config user.name resolved
  - [ ] Category auto-classified from description (or user override applied)
  - [ ] Category section exists in platform/issues.md
  - [ ] Description refined (1-3 sentences, ≤280 chars) WITHOUT asking the user
  - [ ] Next free ID allocated
  - [ ] Worktree created outside platform/ tree, branched off origin/main
  - [ ] Issues.md row staged inside the worktree (on the branch, not on main)
  - [ ] Plan written inside worktree, never staged
  - [ ] Implementation dispatched with self-contained MVC prompt
  - [ ] Review dispatched (or explicitly skipped for trivial fixes)
  - [ ] Triage table shown to user; user approved
  - [ ] Plan file deleted before final commit
  - [ ] Lint passed in every affected platform package
  - [ ] Branch pushed (initial push, before main-sync)
  - [ ] origin/main fetched and merged into the branch
  - [ ] Merge conflicts (if any) resolved with blend-not-overwrite for source code
  - [ ] Branch re-pushed after main-sync merge
  - [ ] Worktree removed only after both pushes confirmed
  - [ ] PR opened with link, deferred findings noted
  - [ ] Final report printed

---

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Committing the issues.md row to platform main | Stage the row inside the worktree (on the branch); it arrives on main only when the PR merges. Keeps main clean of rows for fixes that never ship. |
| Asking the user to confirm the refined description / category / branch name / plan | Decide silently. Print one-line status updates. Only ask on the 6 legitimate stop-and-ask gates listed in "Operating Principle". |
| Creating the worktree inside platform/ | Use `/tmp/platform-<id>-worktree` so the parent checkout stays clean |
| Committing the plan file | Delete it in STEP 11 before `git add -A` |
| Passing parent conversation to the implementer | Build a self-contained MVC prompt — the implementer must never read this chat |
| Skipping the review dispatch on a non-trivial fix | Only skip review when `complexity=small` AND ≤ 2 files AND ≤ 30 LOC |
| Removing the worktree before pushing | Worktrees contain LOCAL commits — push first, remove second |
| Skipping the main-sync merge before opening the PR | Main may have advanced during investigation/implementation; merge it now so the PR diff is clean and conflicts are resolved while local context is still available — not later in the GitHub UI |
| Resolving merge conflicts by blindly taking one side on source files | For source code in the fix's blast radius, both sides usually carry intent — blend them. Policy-based `--theirs` is only for issues.md, generated files, and lockfiles. |
| Running Biome inside the worktree | Platform uses Prettier + ESLint. `bun run lint` inside `backend/` or `frontend/` |
| Verbatim-copying the user's bug wording into issues.md | Refine it (1-3 sentences) — but preserve user-stated open questions verbatim, in bold |
| Opening the PR before the branch is pushed | `gh pr create` will fail; push first |
| Using one giant ID space across categories | Each section has its own counter (B-, I-, UI-) — scan only within the matching section |

## Integration

- **Called by:** `skaile-development` agent when the user reports a platform bug
- **Calls (via Agent tool):** `implement` (or `general-purpose`) for the fix; `review` for the diff review
- **Uses:** `gh` CLI for the PR; `git` directly for branch / worktree / commit / push (does NOT call the `git` skill because the flow is too tightly choreographed)
- **Reads:** `platform/issues.md`, `platform/CLAUDE.md`, affected source files
- **Writes:** new row in `platform/issues.md` (on main), fix commits on the branch, a transient plan file (deleted), a PR on `skaile-ai/skaile-platform`
