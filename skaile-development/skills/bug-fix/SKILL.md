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
        label: "Describe the bug (plain language — the skill will refine it for issues.md)"
        type: "text"
        required: true
      - id: "category"
        label: "Issue category in platform/issues.md"
        type: "select"
        options:
          - "B"
          - "I"
          - "UI"
        required: false
        default: "B"
        hint: "B = Bugs (default) | I = Issues (general functionality / quality) | UI = UI/UX issues"
      - id: "branch_slug"
        label: "Optional branch slug (auto-derived from bug_description if empty)"
        type: "text"
        required: false
      - id: "complexity"
        label: "Complexity hint"
        type: "select"
        options:
          - "small"
          - "standard"
        required: false
        default: "standard"
        hint: "small = obvious, contained fix | standard = needs investigation + review cycle"
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
| 0 | Read inputs, resolve current git user, choose ID + branch name |
| 1 | Create the worktree + branch (outside the platform/ tree to avoid polluting it) |
| 2 | Register the bug in `platform/issues.md` (status=testing, owner=git user) and commit that change to main first |
| 3 | Investigate: load relevant files, identify root cause |
| 4 | Write an **uncommitted** plan markdown into the worktree |
| 5 | Dispatch a fresh subagent to implement the plan (gets only the plan + context — no parent history) |
| 6 | Dispatch a fresh subagent to review the diff |
| 7 | Triage review findings, apply valid fixes inline |
| 8 | Delete the plan file, commit, push the branch |
| 9 | Remove the worktree |
| 10 | Open a PR against `main` and report the link |

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
MUST  resolve the next free ID in the chosen category by scanning the existing table (highest +1)
MUST  set Status=testing and Owner=`git config user.name` in the new row
MUST  commit the issues.md row to platform main FIRST, then branch from updated main — keeps the row alive even if the fix is later abandoned
MUST  create the worktree outside platform/ (e.g., /tmp/platform-<id>-worktree) so the parent platform/ stays clean
MUST  write the plan file in the worktree and treat it as ephemeral — never commit it, delete it before the final commit
MUST  dispatch implementation as a fresh subagent with a self-contained prompt (MVC: paste the plan + relevant file paths + acceptance criteria; do NOT pass parent conversation)
MUST  dispatch review as a fresh subagent that only sees the diff + review focus — not the plan
MUST  triage review findings explicitly: each finding gets a verdict (apply / defer / reject) with a one-line reason
MUST  apply valid findings inline in the orchestrator (no second implementation dispatch unless the finding is large enough to warrant a sub-plan)
MUST  push the branch to origin before opening the PR (worktree-only commits are local-only)
MUST  remove the worktree only after the push succeeds — never before
MUST  use `gh pr create --base main --head <branch>` from the platform repo to open the PR
MUST  report back to the user with: branch name, PR URL, 1-2 line summary of what changed, any deferred review findings
NEVER  modify the main platform/ checkout while the worktree exists (except the initial issues.md commit on main)
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
  - Read category (default "B")
  - Read complexity (default "standard")
  - $ git config user.name → owner_name
  IF owner_name is empty
    STOP: "git config user.name is not set. Set it before running this skill."

  - Map category → section label in platform/issues.md:
    | category | section heading           | ID prefix |
    |----------|---------------------------|-----------|
    | B        | ## Bugs                   | B-        |
    | I        | ## Issues                 | I-        |
    | UI       | ## UI / UX Issues *(or similar — match the actual heading)* | UI- |

  IF category heading does not exist in platform/issues.md
    STOP: "issues.md has no section matching category=<category>. Pick another category or add the section manually first."

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

  Show the refined wording to the user for one quick confirmation before writing it.

STEP 2: Pick the next free ID
  Scan platform/issues.md for all rows matching ID prefix `<category>-`.
  next_id = max(existing numeric suffixes) + 1, zero-padded to 2 digits.
  full_id = "<prefix><next_id>"  e.g. "B-54", "I-30", "UI-34"

EMIT [bug-fix] id_allocated id=<full_id> owner=<owner_name>

# ── Phase 2: Commit issues.md Row to Platform Main ────────────────

STEP 3: Add the row in platform main (NOT in a branch)
  This commit goes to platform's main directly so the row exists in the
  issue tracker even if the fix is later abandoned. Standard pattern from
  recent bug cycles (B-52, B-48, UI-29).

  Working dir: /home/kolja/skaile/skaile-dev/platform (or wherever the
  user is — verify the platform submodule is on `main` and clean).

  PRE-FLIGHT:
    $ cd <skaile-dev-root>/platform
    $ git fetch origin
    $ git status --short
    IF dirty:
      STOP: "platform/ has uncommitted changes. Stash or commit them before running bug-fix."
    $ git rev-parse --abbrev-ref HEAD
    IF branch ≠ main:
      ASK:
        > "platform/ is on '<branch>', not main. Switch to main? (y/n)"
      IF yes: $ git checkout main && $ git pull --rebase origin main
      IF no:  STOP

  EDIT platform/issues.md:
    - Append a new row to the section matching `category` (match the
      pipe-padded column widths in the existing table — look at the
      surrounding rows for column widths and pad accordingly)
    - Row format: `| <full_id> | <refined_description> | testing | <owner_name> |`

  COMMIT to platform main:
    $ git add issues.md
    $ git commit -m "$(cat <<EOF
docs(issues): add <full_id> — <short summary, ≤60 chars>

<one-line context: where the bug shows up + assigned to <owner_name>>
EOF
)"
    $ git push origin main
    IF push rejected:
      $ git pull --rebase origin main
      $ git push origin main
      IF still rejected:
        STOP: "Could not push issues.md row to main — resolve and re-run."

EMIT [bug-fix] issue_filed id=<full_id>

# ── Phase 3: Create Worktree + Branch ─────────────────────────────

STEP 4: Branch name
  Derive slug:
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

  CHECKPOINT plan_ready
    > "Plan written to <plan_path>. Dispatching implementation subagent.
    >  (Plan file is intentionally uncommitted and will be deleted before the final commit.)"

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
  - Do NOT touch issues.md — it was already updated on main.

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
  For each finding in review_findings, decide:

  | Verdict | When | Action |
  |---------|------|--------|
  | apply   | Important + the finding is real + fix is ≤ 20 LOC | Implement inline in this orchestrator |
  | apply-large | Important + fix needs ≥ 20 LOC or new abstraction | Dispatch a second implementation subagent with a focused sub-prompt |
  | defer   | Nit OR not blocking OR out of scope for this PR | Note it in the PR description under "Deferred follow-ups" |
  | reject  | Disagreement with the reviewer | Note it in the PR description with one-line reasoning |

  Present the triage table to the user before applying:
    > "Review found <N> findings. Triage:
    >  - apply: <list>
    >  - apply-large: <list>
    >  - defer: <list> (will go in PR body)
    >  - reject: <list> (will go in PR body with reasoning)
    >  Proceed? (yes / change)"

  IF user approves: apply the apply + apply-large fixes inside the worktree.

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

    Commit message (platform repo conventional-commits format, no
    skaile-dev agent block since this is a submodule):
    ```
    fix(<scope>): <title — what the fix does> (<full_id>)

    <1-3 sentence explanation: root cause + fix approach + acceptance>
    ```

    $ git commit -m "<message>"

  Push:
    $ git push -u origin <branch_name>
    IF push fails:
      STOP: "Push failed — worktree preserved at <worktree_path>. Investigate."

EMIT [bug-fix] pushed branch=<branch_name>

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
  - [ ] Category section exists in platform/issues.md
  - [ ] Description refined (1-3 sentences, ≤280 chars)
  - [ ] Next free ID allocated
  - [ ] Row committed to platform main BEFORE branching
  - [ ] Worktree created outside platform/ tree
  - [ ] Worktree branch is fresh off origin/main (post-issues.md commit)
  - [ ] Plan written inside worktree, never staged
  - [ ] Implementation dispatched with self-contained MVC prompt
  - [ ] Review dispatched (or explicitly skipped for trivial fixes)
  - [ ] Triage table shown to user; user approved
  - [ ] Plan file deleted before final commit
  - [ ] Lint passed in every affected platform package
  - [ ] Branch pushed
  - [ ] Worktree removed only after push confirmed
  - [ ] PR opened with link, deferred findings noted
  - [ ] Final report printed

---

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Filing the bug in issues.md AFTER fixing it | File first, then branch — keeps the row alive if the fix is abandoned |
| Creating the worktree inside platform/ | Use `/tmp/platform-<id>-worktree` so the parent checkout stays clean |
| Committing the plan file | Delete it in STEP 11 before `git add -A` |
| Passing parent conversation to the implementer | Build a self-contained MVC prompt — the implementer must never read this chat |
| Skipping the review dispatch on a non-trivial fix | Only skip review when `complexity=small` AND ≤ 2 files AND ≤ 30 LOC |
| Removing the worktree before pushing | Worktrees contain LOCAL commits — push first, remove second |
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
