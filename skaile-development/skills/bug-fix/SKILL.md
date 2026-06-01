---
name: "bug-fix"
description: "[skaile-development] End-to-end work-item orchestrator for the Skaile
  platform. Drives any single work item — bug, feature, UI fix, chore, or issue —
  from 'just reported' to 'PR is open' in one flow: isolated worktree + branch, files
  the item at platform/issues/<ID>.md (status=testing, owner=current git user, bumps
  nextIds in categories.yaml), investigates, writes an uncommitted plan, dispatches
  a fresh agent to implement, dispatches a fresh agent to review, fixes valid review
  concerns, cleans up the plan, commits + pushes the branch, removes the worktree,
  opens a PR into main, and reports the PR link. Use when the user reports a platform
  bug, requests a small feature, asks for a UI fix or chore, and wants the whole cycle
  done in one shot."
metadata:
  tags:
  - "bug-fix"
  - "feature"
  - "ui-fix"
  - "chore"
  - "work-item"
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
    - id: "description"
      label: "Describe the work item (bug, feature, UI fix, chore, or issue) — plain
        language; the skill refines the wording, picks the category, and files it
        without asking"
      type: "text"
      required: true
    - id: "category"
      label: "Category override (rarely needed — auto-derived from description)"
      type: "select"
      options:
      - "bug"
      - "issue"
      - "ui"
      - "chore"
      - "feature"
      required: false
      hint: "Override the auto-classification. bug=broken behavior, issue=existing
        thing should do more, ui=cosmetic only, chore=maintenance/refactor with no
        behavior change, feature=genuinely new capability. Leave empty in the typical
        case."
    - id: "branch_slug"
      label: "Branch slug override (auto-derived from description)"
      type: "text"
      required: false
      hint: "Only set if you want a specific branch name. Leave empty in the typical
        case."
    - id: "complexity"
      label: "Complexity hint"
      type: "select"
      options:
      - "small"
      - "standard"
      required: false
      default: "standard"
      hint: "small = obvious, contained change (skips the review-subagent step) |
        standard = needs review cycle"
    files: []
---

# Bug-Fix — End-to-End Platform Work-Item Orchestrator

> Despite the historical name, this skill handles **any single platform work item** —
> bug, feature, UI fix, chore, or issue. The workflow is identical across categories;
> only the issue prefix, branch prefix, and commit type vary. Trigger it whenever you
> have one item that fits a single PR cycle.

## Overview

Drives a single platform work item from "user just reported it" to "PR is open" without
leaving the main session dirty. The skill works exclusively against the **`platform/`
submodule** — it creates a worktree of that submodule, branches off main, files the item
at `platform/issues/<ID>.md` (and bumps the counter in `platform/issues/categories.yaml`),
investigates, plans, dispatches implementation + review subagents, lands the change on the
branch, cleans up, and opens a PR on `skaile-ai/skaile-platform`.

| Phase | What Happens |
|-------|--------------|
| 0 | Read inputs, resolve current git user, auto-classify category, allocate next ID from `nextIds` |
| 1 | Pre-flight platform main, derive branch name |
| 2 | Create the worktree + branch (outside `platform/` tree, branched from `origin/main`) |
| 2b | Stage `issues/<full_id>.md` + `categories.yaml` bump **on the branch** (not on main) |
| 3 | Investigate: load relevant files, identify root cause (bugs/issues/ui) or design surface (features/chore) |
| 4 | Write an **uncommitted** plan markdown into the worktree |
| 5 | Dispatch a fresh subagent to implement the plan (gets only the plan + context — no parent history) |
| 6 | Dispatch a fresh subagent to review the diff |
| 7 | Triage review findings, apply valid fixes inline |
| 8 | Delete the plan file; commit (work + `issues/<full_id>.md` + `categories.yaml` together); push the branch |
| 9 | Fetch latest `origin/main`, merge it into the branch, resolve conflicts, re-push |
| 10 | Remove the worktree |
| 11 | Open a PR against `main` and report the link |

## When to Use

- The user describes a platform work item: a bug, a missing capability ("issue"), a cosmetic
  fix ("UI"), a maintenance task ("chore"), or a small/medium new feature
- You want the item to land at `platform/issues/<ID>.md` (with a refined description) and
  reach a PR in one continuous flow
- The item is contained enough that one implementation pass + one review pass is plausible —
  not a multi-week refactor

## When NOT to Use

- Large feature work that warrants a proposal / design doc first — file a proposal first
- Bugs or features outside the `platform/` submodule (e.g., `workspaces/`, `forge/`) — adapt manually or extend this skill
- Anything that needs design discussion before code — file a proposal first
- Something already on a branch — this skill expects a clean start

---

## Operating Principle — Autonomy by Default

**Run end-to-end without asking the user.** The user already gave you the work item;
they expect a PR link back, not a flurry of checkpoint prompts. Decisions you can make
on your own based on the evidence — classification (bug / issue / ui / chore / feature),
description wording, branch name, plan structure, which review findings to apply, conflict
resolutions where one side is a strict superset — you make. The user does not need to
approve those.

Only stop and ask when continuing would require a judgment call the evidence cannot
settle on its own. The complete list of legitimate stop-and-ask gates is:

1. **`git config user.name` is unset** — the `owner` frontmatter would be empty.
2. **Working tree is dirty** — would either contaminate the branch or block the worktree creation; the user must decide.
3. **Investigation inconclusive** — root cause / design surface cannot be located; report findings and stop.
4. **A review finding is "apply-large"** — spawning a second implementation subagent is
   a meaningful escalation; confirm the scope first.
5. **A merge conflict on source code is genuinely mutually-exclusive** — the change and
   main's incoming change cannot both stand; the user must pick.
6. **Push fails for a non-trivial reason** — e.g., branch was rewritten remotely.
7. **Same-ID add/add conflict on `issues/<full_id>.md`** — both branches allocated the same ID; the operator must approve the re-allocation to a fresh ID.

Everything else — refining the description, picking the category, choosing the
branch slug, writing the plan, dispatching the implementer, applying nit-level review
findings, deferring out-of-scope findings, merging main when one side is a strict
subset, opening the PR — you do silently. Print short progress notes (`> "Filing as
B-84 (auto-classified as bug)"`, `> "Plan written, dispatching implementer..."`) but
do NOT solicit confirmation.

The user can always interrupt; they will if they want to redirect.

---

ROLE  Platform work-item orchestrator for the `platform/` submodule. Owns the full cycle: worktree → `issues/<full_id>.md` + `categories.yaml` bump → plan → implement → review → triage → commit → push → cleanup → PR. Handles any category (bug / issue / ui / chore / feature) identically; the category only influences the ID prefix, branch prefix, and commit type.

READS
  platform/issues/categories.yaml                 — category registry + `nextIds` counter
  platform/issues/CLAUDE.md                       — tracker invariants (per-file format, nextIds rules)
  platform/issues/<*.md>                          — existing items (only when nextIds fallback is needed, or to avoid duplicate filenames)
  platform/CLAUDE.md                              — platform architecture, conventions
  platform/<files-relevant-to-work-item>          — for investigation
  skaile-dev/CLAUDE.md                            — monorepo conventions
  skills/git/references/branch_naming.md          — branch naming
  skills/git/references/commit-spec.md            — commit message format for platform submodule
  git config user.name                            — for the `owner` frontmatter field

WRITES (in the worktree only — nothing ever goes to platform main outside of a merged PR)
  <worktree>/issues/<full_id>.md                  — new work-item file (frontmatter + body), staged on the feature branch
  <worktree>/issues/categories.yaml               — `nextIds[<category_id>]` incremented by 1, staged on the feature branch
  <worktree>/<plan-file>.md                       — uncommitted plan (deleted before the final commit)
  <worktree>/<target-source-files>                — the actual fix / implementation
  PR on skaile-ai/skaile-platform                 — opened at the end

NEVER WRITES
  platform/issues.md                              — legacy snapshot, superseded by `platform/issues/`. The folder is the source of truth.

MUST  refine the user's description before filing it — 1-3 sentences for bug/issue/ui/chore (up to 5 short sentences for features that need framing), technical but specific, no marketing fluff
MUST  auto-classify the category (bug / issue / ui / chore / feature) from the description without asking the user; the `category` user input only overrides the auto-classification
MUST  validate the resolved category exists in `platform/issues/categories.yaml` before proceeding
MUST  resolve the next free ID from `nextIds[<category_id>]` in `categories.yaml` (fallback: max numeric suffix of `platform/issues/<PREFIX>-*.md` + 1 when the counter is missing)
MUST  set `status: testing` and `owner: <git config user.name>` in the new file's frontmatter
MUST  increment `nextIds[<category_id>]` in `categories.yaml` by 1 in the same staged set as the new work-item file — the high-water mark must stay monotonic
MUST  write the new `issues/<full_id>.md` and the `categories.yaml` bump INSIDE the worktree, on the feature branch — never commit them to platform main directly; both reach main only when the PR is merged
MUST  create the worktree outside platform/ (e.g., /tmp/platform-<id>-worktree) so the parent platform/ stays clean
MUST  write the plan file in the worktree and treat it as ephemeral — never commit it, delete it before the final commit
MUST  dispatch implementation as a fresh subagent with a self-contained prompt (MVC: paste the plan + relevant file paths + acceptance criteria; do NOT pass parent conversation)
MUST  dispatch review as a fresh subagent that only sees the diff + review focus — not the plan
MUST  triage review findings explicitly: each finding gets a verdict (apply / defer / reject) with a one-line reason
MUST  apply valid findings inline in the orchestrator (no second implementation dispatch unless the finding is large enough to warrant a sub-plan)
MUST  push the branch to origin before opening the PR (worktree-only commits are local-only)
MUST  fetch origin/main and merge it into the branch (inside the worktree) BEFORE removing the worktree — keeps the PR diff clean and resolves any conflicts while the full local context is available
MUST  resolve merge conflicts per the policy table (categories.yaml blends `nextIds` as `max(HEAD, main)`; generated files / lockfiles take theirs; source-code in the change's blast radius is blended manually; same-ID add/add on `issues/<full_id>.md` escalates to the user)
MUST  re-push the branch after the main-sync merge completes
MUST  remove the worktree only after both the initial push AND the post-sync push succeed — never before
MUST  use `gh pr create --base main --head <branch>` from the platform repo to open the PR
MUST  pick the conventional-commit type (and the matching PR title prefix) from the category: bug/issue/ui → `fix`, chore → `chore`, feature → `feat`
MUST  report back to the user with: full_id, branch name, PR URL, 1-2 line summary of what changed, any deferred review findings
NEVER  modify the main platform/ checkout while the worktree exists
NEVER  commit the new `issues/<full_id>.md` or the `categories.yaml` bump to platform main directly — both live on the branch and travel via the PR
NEVER  read, write, or reference `platform/issues.md` (the legacy snapshot). The `platform/issues/` folder is the source of truth.
NEVER  ask the user to confirm the refined description, the category, the branch name, the plan, or the triage table — those are decisions you make; the user gave you the work item and expects a PR back
NEVER  derive the `owner` field from anything other than `git config user.name` — never from system-prompt userEmail, GitHub handle, gitStatus "Git user:" line, conversation context, or the Claude account. Wrong-attribution is a silent data-quality bug that strangers downstream will inherit.
NEVER  reset, decrement, or skip past free counters in `nextIds` — the only allowed mutation is incrementing `nextIds[<category_id>]` by 1 in the same set as the new file
NEVER  rename the new `issues/<full_id>.md` file or mutate its `id:` frontmatter field after creation (filename and id field must match)
NEVER  commit the plan file
NEVER  pass the parent conversation transcript to the implementation or review subagent — both must be fresh and self-contained
NEVER  open a PR before the branch is pushed
NEVER  remove the worktree if `git -C <worktree> status` shows unpushed commits
NEVER  force-push, rewrite history, or commit to platform main from inside the worktree
NEVER  run Biome inside platform/ — platform uses Prettier + ESLint exclusively

EMIT [bug-fix] started description="<short>"

# ── Phase 0: Inputs and Identity ──────────────────────────────────

STEP 0: Resolve inputs
  - Read `description` (required)
  - Read `complexity` (default "standard")
  - Read `category` (optional override)

  Resolve the owner name — STRICTLY from `git config`, nothing else:
    $ git -C <skaile-dev-root>/platform config user.name → owner_name
    IF owner_name is empty:
      $ git config --global user.name → owner_name
    IF owner_name is STILL empty:
      STOP: "git config user.name is not set in platform/ or global config.
             Set it (`git -C platform config user.name '<Full Name>'`) before
             running this skill — the owner frontmatter field requires a real name."

  **owner_name MUST come from `git config user.name` only.** Do NOT use:
  - any `userEmail` shown in the agent's system prompt or session context
  - any GitHub username from `gh auth status` or remote URLs
  - any "Git user: X" line from gitStatus context
  - any name inferred from the conversation
  - the Claude account, the API key holder, or the system prompt author

  The user running the skill on their machine has `git config user.name`
  set to THEIR own name. That is the ground truth. Other signals are
  ambient noise and will silently mis-attribute the work item to the wrong person.

  Auto-classify the category from `description` (do NOT ask the user):

  | Signal in description | category | category id | ID prefix |
  |-----------------------|----------|-------------|-----------|
  | Broken behavior, error message, data loss, security, wrong result, crash, regression in existing functionality | Bug | `bug` | `B-` |
  | Existing functionality is incomplete; "should also be able to X"; design-level gap with no current breakage | Issue | `issue` | `I-` |
  | Purely visual: layout, spacing, color, font, alignment, tooltip wording, hover state, icon | UI | `ui` | `UI-` |
  | Maintenance: dependency bump, refactor with no behavior change, code cleanup, infra/tooling adjustment | Chore | `chore` | `C-` |
  | Genuinely new capability: a screen / endpoint / data shape / interaction that did not exist before | Feature | `feature` | `F-` |

  Tie-breakers (apply in this order):
  - Broken behavior → `bug` (trumps visual concern)
  - New capability that didn't exist before → `feature` (trumps `issue`)
  - Existing thing should do more → `issue` (trumps `ui`)
  - Cosmetic only with no functional impact → `ui`
  - Pure refactor / dependency bump / tooling change with no behavior change → `chore`

  IF the `category` user input is provided, it OVERRIDES the auto-classification.
  Otherwise use the auto-classification silently and print one line:
    > "Auto-classified as <category> (<one-word reason>)."

  Validate against the registry:
    Read `platform/issues/categories.yaml`.
    IF the chosen `category_id` is not present in the `categories` list:
      STOP: "categories.yaml has no entry with id=<category_id>. Add the category
             there first (see platform/issues/CLAUDE.md § To add a new category)."

# ── Phase 1: Refine Description + Allocate ID ─────────────────────

STEP 1: Refine the description
  Treat the user's input as a draft, not the final wording.
  Produce a refined description that:
  - Is 1-3 sentences total (hard cap for bug / issue / ui / chore — up to 5 short
    sentences acceptable for features that need framing)
  - Names the affected surface explicitly (e.g., "workspace chat panel", "project settings page")
  - States the observed behavior and, if relevant, the expected behavior — or, for
    features, names the new behavior the work item introduces
  - Drops filler ("I noticed that", "sometimes maybe")
  - Preserves any user-stated constraint verbatim (e.g., "open question for Peter: <question>") — wrap such constraints in **bold** so they are visually distinct

  Examples (style reference — adapt to the actual work item):
  - Bug: "Long inline-code URLs in agent chat messages don't wrap when they contain no `-`, causing horizontal scroll on narrow chat panes."
  - Issue: "Project Settings tabs lose data when the URL uses the project slug instead of the UUID — tabs query by id only. **open question for Peter**: should slug-form URLs be canonical?"
  - UI: "Sidebar project tree doesn't refresh after a session is deleted — the deleted row remains visible until manual reload."
  - Chore: "Bump `@nestjs/common` from 10.4.x to 10.5.x and adapt the two deprecation warnings surfaced in the test logs."
  - Feature: "Turn all top-level screens (admin panes, project settings, session settings) into tabs in the workspace header so navigation stays in-context."

  **Do NOT ask the user to approve the wording.** Write it directly. The user will see
  it as the body of `issues/<full_id>.md` and in the PR body — minor wording tweaks can
  be made later by editing the file. Only stop if the user's input is too vague to refine
  (in which case ask one targeted clarification question, not a yes/no on a proposal).

STEP 2: Pick the next free ID from `categories.yaml`
  Read `platform/issues/categories.yaml`.
  - Look up the `prefix` from the matching entry in the `categories` list (e.g.
    `bug → B`, `feature → F`).
  - Look up `nextIds[<category_id>]` — the numeric counter.
    IF the counter is present: `n = nextIds[<category_id>]`.
    ELSE (counter missing): scan `platform/issues/<PREFIX>-*.md`, take
    `max(numeric suffix) + 1`.
  - `full_id = "<PREFIX>-<n_zero_padded_to_2_digits>"` e.g. `B-84`, `F-48`, `UI-50`.

  REMEMBER to increment `nextIds[<category_id>]` from `n` to `n+1` when staging
  `categories.yaml` in STEP 5b — the counter is a monotonic high-water mark, and
  STEP 5b commits the bump in the same staged set as the new file.

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
      Build slug from refined description: lowercase, hyphens, max 40 chars,
      strip articles, keep concrete nouns.

  Branch prefix follows the category:
    bug / issue / ui → "fix/"
    chore            → "chore/"
    feature          → "feat/"

  branch_name = "<branch_prefix><full_id_lowercase>-<slug>"
    Examples:
      "fix/b-84-chat-link-wrap"
      "fix/ui-49-sidebar-refresh-on-delete"
      "chore/c-04-nestjs-bump"
      "feat/f-48-screens-as-tabs"

STEP 5: Worktree
  worktree_path = "/tmp/platform-<full_id_lowercase>-worktree"

  $ cd <skaile-dev-root>/platform
  $ git worktree add <worktree_path> -b <branch_name> origin/main
  > "Worktree created at <worktree_path> on branch <branch_name> (based on origin/main)."

  All subsequent file operations until Phase 9 happen INSIDE <worktree_path>.

EMIT [bug-fix] worktree_ready path=<worktree_path> branch=<branch_name>

# ── Phase 3b: File the Work-Item File on the Branch ───────────────

STEP 5b: Write `issues/<full_id>.md` and bump `categories.yaml` inside the worktree
  Both files land on the feature branch and travel to main only when the
  PR is merged. They do NOT go to platform main directly — keeps main free
  of work items for changes that never ship.

  $ cd <worktree_path>

  Safety check: `issues/<full_id>.md` must not already exist on the branch
  (the worktree was just created from `origin/main`, so this only fails if
  someone allocated the same ID concurrently or `nextIds` was incorrect).
    IF `ls issues/<full_id>.md` returns the file → STOP:
      "issues/<full_id>.md already exists at HEAD — re-allocate the ID by
       re-reading categories.yaml or scanning the directory."

  Create `issues/<full_id>.md` with this exact shape (YAML frontmatter + body):
    ---
    id: <full_id>
    category: <category_id>
    status: testing
    owner: <owner_name>
    ---

    <refined description from STEP 1, verbatim, as the body — free-form
    markdown, may contain code, links, multiple paragraphs, **bolded
    user-stated open questions**>

  Edit `issues/categories.yaml`:
    - Increment `nextIds.<category_id>` from `<n>` to `<n+1>`.
    - Do not change the `categories` list or the `statuses` list.
    - Do not touch other categories' counters.
    - Do not reset or decrement anything.

  STAGE both files but do NOT commit yet — they land in the same commit as
  the actual work changes at the end (Phase 9). This keeps the branch to a
  single commit when the work is small. (If the work grows, the work-item
  file + bump can be split into their own commit during Phase 9 — but
  always one commit per logical unit.)

  $ git add issues/<full_id>.md issues/categories.yaml

  Print a one-line status:
    > "Filed <full_id> (<category>, status=testing, owner=<owner_name>) on branch <branch_name>."

EMIT [bug-fix] issue_filed id=<full_id> category=<category_id> location=branch

# ── Phase 4: Investigate ─────────────────────────────────────────

STEP 6: Investigate
  Read in this order (stop when the cause / design surface is clear):
  1. `platform/CLAUDE.md` sections relevant to the work item's surface area
  2. The exact file(s) the user named, if any
  3. Adjacent files following the work item's data flow (e.g., page → hook → tRPC route → service → repo)
  4. Recent commits touching the same area (`git log --oneline -- <path>`)

  Produce internally (not yet written to disk):
  - For bug / issue / ui:
    - `root_cause`: 1-2 sentences naming the precise mechanism
    - `blast_radius`: which files / which user paths are affected
    - `fix_candidates`: 1-3 plausible fix shapes with trade-offs
    - `chosen_fix`: which candidate to apply, with one-line justification
  - For chore:
    - `target_surface`: what is being refactored / bumped / cleaned
    - `risk_areas`: files whose behavior must not change
    - `chosen_approach`: one-line plan
  - For feature:
    - `design_note`: 2-3 sentences on the new behavior and where it plugs in
    - `surfaces_touched`: page / route / service / schema
    - `chosen_approach`: which design candidate to apply, with one-line justification

  IF the root cause / design surface cannot be located with confidence:
    REPORT to user: "Investigation inconclusive. <what I checked>. <what would help>."
    STOP — do not proceed to plan.

EMIT [bug-fix] investigated note="<short>"

# ── Phase 5: Write Plan (uncommitted) ─────────────────────────────

STEP 7: Write the plan
  plan_path = "<worktree_path>/<full_id>-plan.md"

  Write a plan with this structure:

  ```
  # <full_id> — <work-item-title> — Implementation Plan

  ## Summary
  <refined description from STEP 1, verbatim>

  ## Category
  <bug | issue | ui | chore | feature>

  ## Root Cause / Design Note
  <1-2 sentences — root cause for bug/issue/ui; design note for chore/feature>

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
  - [ ] No regression to <adjacent surface>

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
  # Task: Implement <full_id> in the platform submodule

  ## Working Directory
  <worktree_path>

  All file paths below are relative to that directory. The branch
  <branch_name> is already checked out — make commits there.

  ## Work Item
  <refined description from STEP 1>

  ## Category
  <bug | issue | ui | chore | feature>

  ## Root Cause / Design Note
  <root_cause or design_note from STEP 6>

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
  - Do NOT touch `issues/<full_id>.md` or `issues/categories.yaml` — both
    are already staged in the worktree; the orchestrator will commit them
    together with your changes at the end. NEVER read or write
    `platform/issues.md` (legacy snapshot).

  ## Output
  When done, report:
  - Files changed (relative paths)
  - Acceptance criteria you verified
  - Anything you noticed that wasn't in the plan
  ```

  DISPATCH via Agent tool:
    Agent({
      subagent_type: "implement",   // or "general-purpose" if implement is not appropriate for this scope
      description: "Implement <full_id>",
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
  This worktree contains the implementation of <full_id> (<category>):
  <refined description>.

  ## What to Review
  Run `git diff` inside the worktree. Review for:
  - Correctness: does the change actually implement the work item as described?
    (For bug/issue/ui: does it fix the reported behavior? For chore: are the only
    visible effects the maintenance ones? For feature: does the new behavior match
    the description?)
  - Scope: are there changes that aren't related to the work item?
  - Platform conventions:
    * No Biome inside platform/ (Prettier + ESLint only)
    * No barrel `index.ts` in backend/libs/
    * tRPC routes use authMiddleware
    * @custom-start / @custom-end markers respected for generated files
    * Direct subpath imports (e.g., `@credential/credential.service`)
  - Test coverage: is there a test that locks in the new behavior?
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
      description: "Review <full_id>",
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

    The staged set now contains the work changes AND the new
    `issues/<full_id>.md` AND the `categories.yaml` `nextIds` bump (both
    staged earlier in STEP 5b). They go into one commit so everything
    arrives on main together via the PR.

    Conventional-commit type follows the category:
      bug / issue / ui → `fix`
      chore            → `chore`
      feature          → `feat`

    Commit message (platform repo conventional-commits format, no
    skaile-dev agent block since this is a submodule):
    ```
    <type>(<scope>): <title — what the change does> (<full_id>)

    <1-3 sentence explanation: root cause + fix approach + acceptance for
    bug/issue/ui; motivation + implementation outline for chore/feature>

    Adds platform/issues/<full_id>.md (status: testing, owner: <owner_name>)
    and bumps nextIds.<category_id> in categories.yaml.
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
    | `issues/<full_id>.md` (add/add conflict) | Both branches allocated the same ID — **escalate to user** (legitimate stop-and-ask gate #7). The branch must re-allocate to a fresh ID: pick a new `nextIds[<category_id>]` (typically `max(HEAD, main) + 1`), `git mv issues/<full_id>.md issues/<new_full_id>.md`, update the `id:` frontmatter field to match, and amend the commit message + PR title with the new ID. The branch name keeps its original slug (renaming a branch mid-flight breaks the PR). |
    | `issues/categories.yaml` | Blend automatically: for each entry in `nextIds`, take `max(HEAD, main)`. Preserve the `categories` list and `statuses` list verbatim from whichever side changed them (main's changes win if both sides edited those — categories/statuses are infrastructure that travels via main, not via work-item branches). |
    | Other `issues/<other_id>.md` (modify/modify) | Concurrent edit to an unrelated work item — **escalate to user**. A work-item branch should not normally edit other work-item files; if it did, the operator must decide. |
    | Generated PostXL files (in `postxl-lock.json`) | `git checkout --theirs` — let main's regen win; rerun `bun run generate` if needed |
    | Lockfiles (`bun.lock`, `package-lock.json`) | `git checkout --theirs` then `bun install` to re-resolve |
    | Source code touched by both this work item and main | **Blend manually** — keep HEAD's change logic + integrate main's new behavior. Never blindly take one side. |

    FOR EACH conflicted file:
      Read both sides of the conflict markers.
      Decide: theirs / ours / blend.

      IF the file is on the policy table above AND policy is unambiguous:
        Apply the policy decision directly.
        Note the resolution: "<file>: <theirs|ours|blend> — <one-line reason>"

      IF the conflict is in code the change actually touches (source files in
         the change's blast radius) OR the policy is ambiguous:
         The orchestrator MUST resolve manually:
           - Read both sides carefully
           - Identify what HEAD's diff was trying to achieve (the work item)
           - Identify what main's incoming change is trying to achieve
           - Produce a blend that preserves BOTH intents
           - If genuinely conflicting (HEAD's change and main's change are
             mutually exclusive), ASK the user:
             > "<file>: HEAD does <X>, main does <Y>. They conflict.
             >  Choose: 1=keep HEAD, 2=take main, 3=I'll explain a blend."

      Verify no markers remain:
         $ grep -nE '^(<<<<<<<|=======|>>>>>>>)' <file>; echo "(empty = clean)"
         IF markers remain → STOP, fix and re-check.

      $ git add <file>

    Run lint on resolved files in the change's blast radius:
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
  PR title type follows the category (same mapping as the commit type):
    bug / issue / ui → `fix`
    chore            → `chore`
    feature          → `feat`

  $ cd <skaile-dev-root>/platform
  $ gh pr create \
      --base main \
      --head <branch_name> \
      --title "<type>(<scope>): <title> (<full_id>)" \
      --body "$(cat <<'EOF'
## What

<1-3 sentences: what changed>

## Why

Implements <full_id> (`platform/issues/<full_id>.md`): <refined description>

## Root Cause / Design Note

<for bug/issue/ui: 1-2 sentences naming the precise mechanism; for chore/feature: 1-2 sentences on the design approach>

## Changes

- <bullet list of meaningful changes — file by file>

## Testing

- [ ] Tests pass locally
- [ ] Manually verified <observable behavior>
- [ ] No regression in <adjacent surface>

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
  ── Work Item Complete ─────────────────────────────────────────

  Item:    <full_id> (<category>) — <short title>
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

PROCEDURE refine_description(raw_description, category)
  - Cap at 3 sentences for bug / issue / ui / chore; up to 5 short sentences for feature
  - Cap at ~280 chars total for bug / issue / ui / chore; ~500 for feature
  - Lead with the observable surface (where the user sees it / where the new behavior lives)
  - State observed vs expected for bug/issue/ui; state new behavior for feature; state motivation for chore
  - Preserve user-stated constraints verbatim (bold them if they
    require human follow-up — e.g., "**open question for Peter**: …")
  - No emojis, no hedging adverbs, no "the user reports that"
  - Return: refined_text

PROCEDURE next_id_for_category(category_id)
  - Read `platform/issues/categories.yaml`
  - prefix = categories[?id == category_id].prefix
  - IF `nextIds[<category_id>]` is set: n = nextIds[<category_id>]
    ELSE: n = max(numeric suffix of `platform/issues/<PREFIX>-*.md` files) + 1
  - Return `<PREFIX>-<n_zero_padded_to_2_digits>`

PROCEDURE triage_finding(finding)
  - severity=important + scope=in-diff + fix-size ≤ 20 LOC → apply
  - severity=important + scope=in-diff + fix-size  > 20 LOC → apply-large
  - severity=nit → defer
  - severity=preexisting → defer (note in PR body)
  - finding disputed → reject (with reason in PR body)

CHECKLIST
  - [ ] Owner resolved from `git config user.name` ONLY — not from system-prompt userEmail, GitHub handle, or conversation context
  - [ ] Category auto-classified from description (or user override applied)
  - [ ] Category exists in `platform/issues/categories.yaml` (validated, not assumed)
  - [ ] Description refined (1-3 sentences for bug/issue/ui/chore, ≤5 short for feature) WITHOUT asking the user
  - [ ] Next free ID allocated from `nextIds[<category_id>]` (or file-scan fallback)
  - [ ] Worktree created outside platform/ tree, branched off origin/main
  - [ ] `issues/<full_id>.md` written and staged inside the worktree (on the branch, not on main)
  - [ ] `nextIds[<category_id>]` incremented by 1 in `categories.yaml` and staged in the same set
  - [ ] Plan written inside worktree, never staged
  - [ ] Implementation dispatched with self-contained MVC prompt
  - [ ] Review dispatched (or explicitly skipped for trivial work items)
  - [ ] Triage decisions silent (apply / defer / reject) unless apply-large or rejecting an "important" finding
  - [ ] Plan file deleted before final commit
  - [ ] Lint passed in every affected platform package
  - [ ] Commit type matches category (`fix` for bug/issue/ui, `chore` for chore, `feat` for feature)
  - [ ] Branch pushed (initial push, before main-sync)
  - [ ] origin/main fetched and merged into the branch
  - [ ] Merge conflicts (if any) resolved per the policy table; same-ID add/add escalated
  - [ ] Branch re-pushed after main-sync merge
  - [ ] Worktree removed only after both pushes confirmed
  - [ ] PR opened with type derived from category, link reported, deferred findings noted
  - [ ] Final report printed

---

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Reading or writing `platform/issues.md` | That file is a legacy snapshot. The source of truth is the `platform/issues/` folder (one file per item) + `categories.yaml`. Never touch `issues.md`. |
| Committing the new work-item file or `categories.yaml` bump to platform main | Stage both inside the worktree (on the branch); they arrive on main only when the PR merges. Keeps main clean of items whose changes never ship. |
| Forgetting to bump `nextIds[<category_id>]` in `categories.yaml` | The counter is a monotonic high-water mark. If you skip the bump and the item you just created is later deleted, the next allocator can reuse its id and silently overwrite history. Bump it. |
| Resetting or decrementing `nextIds` | Never. The only allowed mutation is +1 for the category you just allocated in. |
| Renaming `issues/<full_id>.md` or changing its `id:` frontmatter field after creation | The filename and the `id:` field are the issue's identity — they're referenced by URL and external links. The only time a rename happens is the rare same-ID add/add merge conflict, and that is operator-driven. |
| Asking the user to confirm the refined description / category / branch name / plan | Decide silently. Print one-line status updates. Only ask on the 7 legitimate stop-and-ask gates listed in "Operating Principle". |
| Putting the userEmail / GitHub handle / "Peter Albert" / Claude account into the `owner` field | The owner is the human at the keyboard. Run `git config user.name` — that's the only source. The system prompt's userEmail belongs to the account that pays the API bill, not necessarily the person running the skill. |
| Creating the worktree inside platform/ | Use `/tmp/platform-<id>-worktree` so the parent checkout stays clean |
| Committing the plan file | Delete it in STEP 11 before `git add -A` |
| Passing parent conversation to the implementer | Build a self-contained MVC prompt — the implementer must never read this chat |
| Skipping the review dispatch on a non-trivial change | Only skip review when `complexity=small` AND ≤ 2 files AND ≤ 30 LOC |
| Removing the worktree before pushing | Worktrees contain LOCAL commits — push first, remove second |
| Skipping the main-sync merge before opening the PR | Main may have advanced during investigation/implementation; merge it now so the PR diff is clean and conflicts are resolved while local context is still available — not later in the GitHub UI |
| Resolving merge conflicts by blindly taking one side on source files | For source code in the change's blast radius, both sides usually carry intent — blend them. Policy-based `--theirs` is only for generated files and lockfiles. |
| Running Biome inside the worktree | Platform uses Prettier + ESLint. `bun run lint` inside `backend/` or `frontend/` |
| Verbatim-copying the user's wording into the work-item file | Refine it (1-3 sentences for bug/issue/ui/chore; up to 5 for feature) — but preserve user-stated open questions verbatim, in bold |
| Opening the PR before the branch is pushed | `gh pr create` will fail; push first |
| Picking the wrong commit type for a non-bug | Map category → type: bug/issue/ui → `fix`, chore → `chore`, feature → `feat`. The PR title prefix and commit type must match. |

## Integration

- **Called by:** `skaile-development` agent when the user reports a platform bug, requests a small feature, asks for a UI fix, files a chore, or raises an issue
- **Calls (via Agent tool):** `implement` (or `general-purpose`) for the change; `review` for the diff review
- **Uses:** `gh` CLI for the PR; `git` directly for branch / worktree / commit / push (does NOT call the `git` skill because the flow is too tightly choreographed)
- **Reads:** `platform/issues/categories.yaml`, `platform/issues/CLAUDE.md`, `platform/CLAUDE.md`, affected source files
- **Writes:** new `platform/issues/<full_id>.md` (on the branch), `nextIds[<category_id>]` bump in `platform/issues/categories.yaml` (on the branch), implementation commits on the branch, a transient plan file (deleted), a PR on `skaile-ai/skaile-platform`
- **Never reads or writes:** `platform/issues.md` (legacy snapshot — superseded by the `issues/` folder)
