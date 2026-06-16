---
name: "bug-fix"
description: "[skaile-development] End-to-end work-item orchestrator for the Skaile
  platform. Drives any single work item — bug, feature, UI fix, chore, or issue —
  from 'just reported' to 'PR is open' in one flow: opens a GitHub issue on
  skaile-ai/platform (category label, assigned to the current gh user), creates an
  isolated worktree + branch named after the issue number, investigates, writes an
  uncommitted plan, dispatches a fresh agent to implement, dispatches a fresh agent
  to review, fixes valid review concerns, cleans up the plan, commits + pushes the
  branch, removes the worktree, opens a PR into main that closes the issue, and
  reports the PR link. Use when the user reports a platform bug, requests a small
  feature, asks for a UI fix or chore, and wants the whole cycle done in one shot."
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
  - "github-issues"
  - "pr"
  - "skaile-development"
  source: "MERGED"
  stage: "beta"
  user_inputs:
    dialog:
    - id: "description"
      label: "Describe the work item (bug, feature, UI fix, chore, or issue) — plain
        language; the skill refines the wording, picks the category, and opens the
        GitHub issue without asking"
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
> only the GitHub label, branch prefix, and commit type vary. Trigger it whenever you
> have one item that fits a single PR cycle.

## Tracking moved to GitHub Issues

Work items are tracked as **GitHub issues on `skaile-ai/platform`** — not in the old
`platform/issues/` markdown folder. That folder was a standalone file-backed tracker
(the local Kanban app reads/writes it directly via the filesystem); it has **no sync
to GitHub in either direction**. Its contents were bulk-migrated into GitHub Issues
in a one-time cutover, after which the folder is **legacy/read-only**. This skill
therefore:

- **Opens a real GitHub issue** via `gh issue create` and uses the **native GitHub
  issue number** (`#836`) as the work item's identity — for the branch name, the
  commit reference, and the PR.
- **Never writes** `platform/issues/<ID>.md`, **never bumps** `nextIds` in
  `platform/issues/categories.yaml`, and **never allocates** a `<PREFIX>-<n>` id.
  The category is carried as a **GitHub label**, not as an id prefix.
- Lets GitHub assign the number atomically, so there is **no same-id collision** to
  resolve and **no counter** to keep monotonic.

## Overview

Drives a single platform work item from "user just reported it" to "PR is open" without
leaving the main session dirty. The skill works exclusively against the **`platform/`
submodule** — it opens a GitHub issue, creates a worktree of that submodule, branches
off main, investigates, plans, dispatches implementation + review subagents, lands the
change on the branch, cleans up, and opens a PR on `skaile-ai/platform` that closes the
issue.

| Phase | What Happens |
|-------|--------------|
| 0 | Read inputs, verify `gh` auth, resolve git user (for attribution), auto-classify category |
| 1 | Pre-flight platform main (fetch + clean-tree check) |
| 2 | Refine the description; open the GitHub issue → capture issue **number** + URL |
| 3 | Derive branch name from the issue number; create the worktree + branch (off `origin/main`) |
| 4 | Investigate: load relevant files, identify root cause (bugs/issues/ui) or design surface (features/chore) |
| 5 | Write an **uncommitted** plan markdown into the worktree |
| 6 | Dispatch a fresh subagent to implement the plan (gets only the plan + context — no parent history) |
| 7 | Dispatch a fresh subagent to review the diff |
| 8 | Triage review findings, apply valid fixes inline |
| 9 | Delete the plan file; lint; commit (work only); push the branch |
| 9b | Fetch latest `origin/main`, merge it into the branch, resolve conflicts, re-push |
| 10 | Remove the worktree |
| 11 | Open a PR against `main` that `Closes #<number>`, and report the link |

## When to Use

- The user describes a platform work item: a bug, a missing capability ("issue"), a cosmetic
  fix ("UI"), a maintenance task ("chore"), or a small/medium new feature
- You want the item filed as a GitHub issue (with a refined description) and to reach a PR
  in one continuous flow
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

1. **`gh` is not authenticated** — the GitHub issue cannot be opened. Tell the user to run `gh auth login`.
2. **Working tree is dirty** — would either contaminate the branch or block the worktree creation; the user must decide.
3. **Investigation inconclusive** — root cause / design surface cannot be located; report findings (the GitHub issue stays open, labelled `needs-info`) and stop.
4. **A review finding is "apply-large"** — spawning a second implementation subagent is
   a meaningful escalation; confirm the scope first.
5. **A merge conflict on source code is genuinely mutually-exclusive** — the change and
   main's incoming change cannot both stand; the user must pick.
6. **Push fails for a non-trivial reason** — e.g., branch was rewritten remotely.

Everything else — refining the description, picking the category, choosing the
branch slug, writing the plan, dispatching the implementer, applying nit-level review
findings, deferring out-of-scope findings, merging main when one side is a strict
subset, opening the issue, opening the PR — you do silently. Print short progress notes
(`> "Opened #836 (auto-classified as bug)"`, `> "Plan written, dispatching
implementer..."`) but do NOT solicit confirmation.

The user can always interrupt; they will if they want to redirect.

---

ROLE  Platform work-item orchestrator for the `platform/` submodule. Owns the full cycle: GitHub issue → worktree → plan → implement → review → triage → commit → push → cleanup → PR. Handles any category (bug / issue / ui / chore / feature) identically; the category only influences the GitHub label, branch prefix, and commit type.

READS
  platform/CLAUDE.md                              — platform architecture, conventions
  platform/<files-relevant-to-work-item>          — for investigation
  skaile-dev/CLAUDE.md                            — monorepo conventions
  skills/git/references/branch_naming.md          — branch naming
  skills/git/references/commit-spec.md            — commit message format for platform submodule
  gh label list --repo skaile-ai/platform         — to map category → an existing label
  gh issue list --repo skaile-ai/platform         — optional: detect an obvious duplicate before filing
  git config user.name                            — for human attribution in the issue body (soft, optional)

WRITES
  GitHub issue on skaile-ai/platform              — opened up front (category label + `agent` label, assigned to @me)
  <worktree>/<plan-file>.md                       — uncommitted plan (deleted before the final commit)
  <worktree>/<target-source-files>                — the actual fix / implementation
  PR on skaile-ai/platform                         — opened at the end, body contains `Closes #<number>`

NEVER WRITES
  platform/issues/<*.md>                          — the markdown issue folder is decommissioned (no GitHub sync). Tracking lives in GitHub Issues now.
  platform/issues/categories.yaml                 — no `nextIds` bump, no counter mutation; GitHub assigns numbers.
  platform/issues.md                              — legacy snapshot, superseded long ago.

MUST  refine the user's description before filing it — 1-3 sentences for bug/issue/ui/chore (up to 5 short sentences for features that need framing), technical but specific, no marketing fluff
MUST  auto-classify the category (bug / issue / ui / chore / feature) from the description without asking the user; the `category` user input only overrides the auto-classification
MUST  verify `gh auth status` succeeds before attempting to open the issue
MUST  open the work item as a GitHub issue on `skaile-ai/platform` via `gh issue create`, capturing the returned issue **number** and URL
MUST  apply the category's GitHub label (bug→`bug`, issue→`issue`, ui→`ui`, chore→`chore`, feature→`enhancement`) plus the `agent` label, and assign the issue to `@me`
MUST  derive the work item's identity from the GitHub issue **number** (`#<n>`) — used in the branch name, the commit title reference, and the PR
MUST  open the GitHub issue BEFORE creating the worktree (the branch name needs the number), but AFTER the clean-tree pre-flight (so a dirty tree never leaves a dangling issue)
MUST  create the worktree at <skaile-dev-root>/.worktrees/platform-<number> (gitignored) — outside platform/ so the parent checkout stays clean, but INSIDE the skaile-dev tree so dispatched subagents auto-load the platform CLAUDE.md (+ nested per-package CLAUDE.md) via on-demand memory discovery; a /tmp path is outside the tree and the subagent never sees platform conventions (e.g. the ≤3-line comment rule)
MUST  write the plan file in the worktree and treat it as ephemeral — never commit it, delete it before the final commit
MUST  dispatch implementation as a fresh subagent with a self-contained prompt (MVC: paste the plan + relevant file paths + acceptance criteria; do NOT pass parent conversation)
MUST  dispatch review as a fresh subagent that only sees the diff + review focus — not the plan
MUST  triage review findings explicitly: each finding gets a verdict (apply / defer / reject) with a one-line reason
MUST  apply valid findings inline in the orchestrator (no second implementation dispatch unless the finding is large enough to warrant a sub-plan)
MUST  push the branch to origin before opening the PR (worktree-only commits are local-only)
MUST  fetch origin/main and merge it into the branch (inside the worktree) BEFORE removing the worktree — keeps the PR diff clean and resolves any conflicts while the full local context is available
MUST  resolve merge conflicts per the policy table (generated files / lockfiles take theirs; source-code in the change's blast radius is blended manually; genuinely mutually-exclusive source conflicts escalate to the user)
MUST  re-push the branch after the main-sync merge completes
MUST  remove the worktree only after both the initial push AND the post-sync push succeed — never before
MUST  use `gh pr create --base main --head <branch>` from the platform repo to open the PR, with `Closes #<number>` in the body so merging auto-closes the issue
MUST  pick the conventional-commit type (and the matching PR title prefix) from the category: bug/issue/ui → `fix`, chore → `chore`, feature → `feat`
MUST  report back to the user with: issue number + URL, branch name, PR URL, 1-2 line summary of what changed, any deferred review findings
NEVER  modify the main platform/ checkout while the worktree exists
NEVER  write to `platform/issues/` (the decommissioned markdown folder), bump `nextIds`, or allocate a `<PREFIX>-<n>` id — tracking is GitHub Issues now
NEVER  read, write, or reference `platform/issues.md` (the legacy snapshot)
NEVER  ask the user to confirm the refined description, the category, the branch name, the plan, or the triage table — those are decisions you make; the user gave you the work item and expects a PR back
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

  Verify GitHub auth — the issue cannot be opened without it:
    $ gh auth status
    IF it reports "not logged in" / fails:
      STOP: "gh is not authenticated. Run `gh auth login` (with repo + issue
             scope on skaile-ai/platform) before running this skill."
      (Legitimate stop-and-ask gate #1.)

  Resolve the human's name for attribution in the issue body (SOFT — optional):
    $ git -C <skaile-dev-root>/platform config user.name → owner_name
    IF empty: $ git config --global user.name → owner_name
    IF still empty: leave owner_name unset (omit the attribution footer).

  This name is only a courtesy footer in the issue body ("Filed by …"). The
  GitHub **assignee** is `@me` (the authenticated gh account — the actual actor
  on GitHub). There is no longer a free-text `owner` field to get wrong, so the
  old hard gate on `git config user.name` is gone.

  Auto-classify the category from `description` (do NOT ask the user):

  | Signal in description | category | GitHub label |
  |-----------------------|----------|--------------|
  | Broken behavior, error message, data loss, security, wrong result, crash, regression in existing functionality | Bug | `bug` |
  | Existing functionality is incomplete; "should also be able to X"; design-level gap with no current breakage | Issue | `issue` |
  | Purely visual: layout, spacing, color, font, alignment, tooltip wording, hover state, icon | UI | `ui` |
  | Maintenance: dependency bump, refactor with no behavior change, code cleanup, infra/tooling adjustment | Chore | `chore` |
  | Genuinely new capability: a screen / endpoint / data shape / interaction that did not exist before | Feature | `enhancement` |

  Tie-breakers (apply in this order):
  - Broken behavior → `bug` (trumps visual concern)
  - New capability that didn't exist before → `feature` (trumps `issue`)
  - Existing thing should do more → `issue` (trumps `ui`)
  - Cosmetic only with no functional impact → `ui`
  - Pure refactor / dependency bump / tooling change with no behavior change → `chore`

  IF the `category` user input is provided, it OVERRIDES the auto-classification.
  Otherwise use the auto-classification silently and print one line:
    > "Auto-classified as <category> (<one-word reason>)."

  Confirm the label exists (labels are pre-created on the repo):
    $ gh label list --repo skaile-ai/platform | grep -i '<label>'
    IF the mapped label is missing:
      File the issue WITHOUT that label (gh errors on unknown labels) and note it
      in the final report — do not invent a label.

# ── Phase 1: Pre-flight against platform main ─────────────────────

STEP 1: Pre-flight (BEFORE opening the issue — a dirty tree must not leave a dangling issue)
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

# ── Phase 2: Refine Description + Open the GitHub Issue ────────────

STEP 2: Refine the description
  Treat the user's input as a draft, not the final wording.
  Produce a refined description that:
  - Is 1-3 sentences total (hard cap for bug / issue / ui / chore — up to 5 short
    sentences acceptable for features that need framing)
  - Names the affected surface explicitly (e.g., "workspace chat panel", "project settings page")
  - States the observed behavior and, if relevant, the expected behavior — or, for
    features, names the new behavior the work item introduces
  - Drops filler ("I noticed that", "sometimes maybe")
  - Preserves any user-stated constraint verbatim (e.g., "open question for Peter: <question>") — wrap such constraints in **bold** so they are visually distinct

  Also derive a concise **issue title** (≤ 72 chars) — an imperative one-liner
  naming the surface and the change. The full refined description is the issue body.

  Examples (style reference — adapt to the actual work item):
  - Bug: "Long inline-code URLs in agent chat messages don't wrap when they contain no `-`, causing horizontal scroll on narrow chat panes."
  - Issue: "Project Settings tabs lose data when the URL uses the project slug instead of the UUID — tabs query by id only. **open question for Peter**: should slug-form URLs be canonical?"
  - UI: "Sidebar project tree doesn't refresh after a session is deleted — the deleted row remains visible until manual reload."
  - Chore: "Bump `@nestjs/common` from 10.4.x to 10.5.x and adapt the two deprecation warnings surfaced in the test logs."
  - Feature: "Turn all top-level screens (admin panes, project settings, session settings) into tabs in the workspace header so navigation stays in-context."

  **Do NOT ask the user to approve the wording.** Write it directly. The user will see
  it as the GitHub issue body and in the PR — minor wording tweaks can be made later by
  editing the issue. Only stop if the user's input is too vague to refine (in which case
  ask one targeted clarification question, not a yes/no on a proposal).

  Optional duplicate check (cheap, do NOT block on it):
    $ gh issue list --repo skaile-ai/platform --state open --search "<2-3 key terms>"
    IF an obviously-identical open issue exists, mention it in the final report —
    but still proceed unless the user said otherwise.

STEP 3: Open the GitHub issue
  Build the issue body: the refined description, plus an optional attribution footer:

    <refined description from STEP 2 — free-form markdown, may contain code,
    links, multiple paragraphs, **bolded user-stated open questions**>

    ---
    Filed via the bug-fix skill[ by <owner_name>].   ← omit "by …" if owner_name unset

  Create it (capture BOTH the number and the URL from gh output):

    $ gh issue create \
        --repo skaile-ai/platform \
        --title "<issue title from STEP 2>" \
        --body "<issue body above>" \
        --label <category_label> --label agent \
        --assignee @me

  `gh issue create` prints the new issue URL (e.g. `…/platform/issues/836`).
  Parse the trailing number → `issue_number` (e.g. `836`). Keep `issue_url` too.

  Print a one-line status:
    > "Opened #<issue_number> (<category>, labels: <label>,agent, assigned @me) — <issue_url>."

EMIT [bug-fix] issue_opened number=<issue_number> url=<issue_url> category=<category>

# ── Phase 3: Branch + Worktree ────────────────────────────────────

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

  branch_name = "<branch_prefix><issue_number>-<slug>"
    Examples:
      "fix/836-chat-link-wrap"
      "fix/842-sidebar-refresh-on-delete"
      "chore/848-nestjs-bump"
      "feat/851-screens-as-tabs"

STEP 5: Worktree
  # INSIDE the skaile-dev tree (NOT /tmp), gitignored via skaile-dev/.gitignore
  # `.worktrees/`. Living under the tree is what lets a dispatched subagent
  # auto-load the platform CLAUDE.md (+ nested per-package CLAUDE.md) through
  # on-demand memory discovery — a /tmp path sits outside the tree and the
  # subagent loads no platform conventions at all (e.g. the ≤3-line comment rule).
  worktree_path = "<skaile-dev-root>/.worktrees/platform-<issue_number>"

  $ cd <skaile-dev-root>/platform
  $ git worktree add <worktree_path> -b <branch_name> origin/main
  > "Worktree created at <worktree_path> on branch <branch_name> (based on origin/main)."

  All subsequent file operations until Phase 9b happen INSIDE <worktree_path>.

EMIT [bug-fix] worktree_ready path=<worktree_path> branch=<branch_name>

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
    Add the `needs-info` label to the issue (it stays open as a real backlog item):
      $ gh issue edit <issue_number> --repo skaile-ai/platform --add-label needs-info
    Remove the worktree (no commits were made) to leave the tree clean:
      $ cd <skaile-dev-root>/platform && git worktree remove <worktree_path> --force
    REPORT to user: "Investigation inconclusive. Opened #<issue_number> (labelled
      needs-info). <what I checked>. <what would help>."
    STOP — do not proceed to plan. (Legitimate stop-and-ask gate #3.)

EMIT [bug-fix] investigated note="<short>"

# ── Phase 5: Write Plan (uncommitted) ─────────────────────────────

STEP 7: Write the plan
  plan_path = "<worktree_path>/<issue_number>-plan.md"

  Write a plan with this structure:

  ```
  # #<issue_number> — <work-item-title> — Implementation Plan

  ## Summary
  <refined description from STEP 2, verbatim>

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
  # Task: Implement #<issue_number> in the platform submodule

  ## Working Directory
  <worktree_path>

  All file paths below are relative to that directory. The branch
  <branch_name> is already checked out — make changes there.

  ## Work Item
  <refined description from STEP 2>

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
  - Do NOT write anything under `platform/issues/` — that markdown folder is
    decommissioned (tracking is GitHub Issues now). The orchestrator already
    opened the GitHub issue; you only touch source + tests.

  ## Output
  When done, report:
  - Files changed (relative paths)
  - Acceptance criteria you verified
  - Anything you noticed that wasn't in the plan
  ```

  DISPATCH via Agent tool:
    Agent({
      subagent_type: "implement",   // or "general-purpose" if implement is not appropriate for this scope
      description: "Implement #<issue_number>",
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
  This worktree contains the implementation of #<issue_number> (<category>):
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
      description: "Review #<issue_number>",
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

  $ rm -f <issue_number>-plan.md

  Lint affected areas:
    IF backend/ files changed: $ cd backend && bun run lint
    IF frontend/ files changed: $ cd frontend && bun run lint
    IF lint reports unfixable errors:
      STOP: "Lint errors — fix before committing. Worktree preserved at <worktree_path>."
    IF lint mutated files: $ git add <mutated files>

  Stage and commit:
    $ git add -A
    $ git status --short    (verify <issue_number>-plan.md is gone)
    IF any plan file appears in status:
      STOP: "Plan file still tracked — abort and investigate."

    The staged set contains ONLY the work changes (source + tests). There is
    no issue file and no categories.yaml bump — tracking is on GitHub now.

    Conventional-commit type follows the category:
      bug / issue / ui → `fix`
      chore            → `chore`
      feature          → `feat`

    Commit message (platform repo conventional-commits format, no
    skaile-dev agent block since this is a submodule):
    ```
    <type>(<scope>): <title — what the change does> (#<issue_number>)

    <1-3 sentence explanation: root cause + fix approach + acceptance for
    bug/issue/ui; motivation + implementation outline for chore/feature>
    ```
    (Reference `#<issue_number>` in the title so GitHub cross-links the commit
    to the issue. The actual auto-close keyword `Closes #<issue_number>` goes in
    the PR body in STEP 13, not here — that way the issue closes exactly once,
    when the PR merges.)

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
    | Generated PostXL files (in `postxl-lock.json`) | `git checkout --theirs` — let main's regen win; rerun `bun run generate` if needed |
    | Lockfiles (`bun.lock`, `package-lock.json`) | `git checkout --theirs` then `bun install` to re-resolve |
    | Source code touched by both this work item and main | **Blend manually** — keep HEAD's change logic + integrate main's new behavior. Never blindly take one side. |

    (Note: there is no longer any `platform/issues/*` or `categories.yaml`
    conflict to resolve — that folder is decommissioned and the skill writes
    nothing into it. GitHub assigns issue numbers atomically, so same-id
    add/add conflicts cannot occur.)

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
      --title "<type>(<scope>): <title> (#<issue_number>)" \
      --body "$(cat <<'EOF'
## What

<1-3 sentences: what changed>

## Why

Closes #<issue_number>: <refined description>

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

  Capture the PR URL from gh output. The `Closes #<issue_number>` line auto-closes
  the GitHub issue when the PR merges into main.

EMIT [bug-fix] pr_opened url=<pr_url>

# ── Phase 12: Report ──────────────────────────────────────────────

STEP 14: Final report to user
  Print this exact block:

  ```
  ── Work Item Complete ─────────────────────────────────────────

  Issue:   #<issue_number> (<category>) — <short title>
           <issue_url>
  Branch:  <branch_name>
  PR:      <pr_url>  (Closes #<issue_number> on merge)

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

EMIT [bug-fix] complete number=<issue_number> url=<pr_url>

# ── Procedures ────────────────────────────────────────────────────

PROCEDURE refine_description(raw_description, category)
  - Cap at 3 sentences for bug / issue / ui / chore; up to 5 short sentences for feature
  - Cap at ~280 chars total for bug / issue / ui / chore; ~500 for feature
  - Lead with the observable surface (where the user sees it / where the new behavior lives)
  - State observed vs expected for bug/issue/ui; state new behavior for feature; state motivation for chore
  - Preserve user-stated constraints verbatim (bold them if they
    require human follow-up — e.g., "**open question for Peter**: …")
  - No emojis, no hedging adverbs, no "the user reports that"
  - Return: refined_text + a ≤72-char issue title

PROCEDURE category_label(category)
  - bug     → `bug`
  - issue   → `issue`
  - ui      → `ui`
  - chore   → `chore`
  - feature → `enhancement`
  - Always add `agent` alongside the category label.
  - Verify the label exists via `gh label list`; if missing, file without it and note it.

PROCEDURE triage_finding(finding)
  - severity=important + scope=in-diff + fix-size ≤ 20 LOC → apply
  - severity=important + scope=in-diff + fix-size  > 20 LOC → apply-large
  - severity=nit → defer
  - severity=preexisting → defer (note in PR body)
  - finding disputed → reject (with reason in PR body)

CHECKLIST
  - [ ] `gh auth status` verified before opening the issue
  - [ ] Category auto-classified from description (or user override applied)
  - [ ] Category mapped to an existing GitHub label (verified, not assumed)
  - [ ] Description refined (1-3 sentences for bug/issue/ui/chore, ≤5 short for feature) WITHOUT asking the user
  - [ ] Clean-tree pre-flight done BEFORE opening the issue (no dangling issue on a dirty tree)
  - [ ] GitHub issue opened on skaile-ai/platform (category label + `agent`, assigned @me); number + URL captured
  - [ ] Worktree created at <skaile-dev-root>/.worktrees/platform-<number> (gitignored, inside the skaile-dev tree for CLAUDE.md discovery), branched off origin/main
  - [ ] Branch name uses the GitHub issue number (`fix/<n>-…` etc.)
  - [ ] Nothing written under `platform/issues/`; no `nextIds` bump; no `<PREFIX>-<n>` id allocated
  - [ ] Plan written inside worktree, never staged
  - [ ] Implementation dispatched with self-contained MVC prompt
  - [ ] Review dispatched (or explicitly skipped for trivial work items)
  - [ ] Triage decisions silent (apply / defer / reject) unless apply-large or rejecting an "important" finding
  - [ ] Plan file deleted before final commit
  - [ ] Lint passed in every affected platform package
  - [ ] Commit type matches category (`fix` for bug/issue/ui, `chore` for chore, `feat` for feature); title references `#<number>`
  - [ ] Branch pushed (initial push, before main-sync)
  - [ ] origin/main fetched and merged into the branch
  - [ ] Merge conflicts (if any) resolved per the policy table
  - [ ] Branch re-pushed after main-sync merge
  - [ ] Worktree removed only after both pushes confirmed
  - [ ] PR opened with type derived from category, body contains `Closes #<number>`, link reported, deferred findings noted
  - [ ] Final report printed

---

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Writing `platform/issues/<ID>.md` or bumping `nextIds` in `categories.yaml` | That folder is decommissioned and has no GitHub sync. Open a GitHub issue with `gh issue create` instead. The category is a label; the identity is the native GitHub number. |
| Allocating a `<PREFIX>-<n>` id or prefixing the issue title with `[B-84]` | Native GitHub numbers are the identity now. Plain issue title; category travels as a label. |
| Reading or writing `platform/issues.md` | Legacy snapshot — never touch it. |
| Opening the GitHub issue before the clean-tree pre-flight | If the tree is dirty you'd STOP with a dangling open issue. Pre-flight first (STEP 1), then open the issue (STEP 3). |
| Opening the worktree before the issue | The branch name needs the issue number. Open the issue first, then name the branch `<prefix>/<number>-<slug>`. |
| Putting `Closes #<n>` in BOTH the commit and the PR body | Keep the auto-close keyword in the PR body only; reference `#<n>` (no keyword) in the commit title. The issue then closes exactly once, on merge. |
| Asking the user to confirm the refined description / category / branch name / plan | Decide silently. Print one-line status updates. Only ask on the 6 legitimate stop-and-ask gates listed in "Operating Principle". |
| Assigning the issue to a guessed GitHub handle | Use `--assignee @me` — the authenticated gh account is the actor on GitHub. The human's `git config user.name` is only an optional courtesy footer in the body. |
| Creating the worktree inside platform/ — or in /tmp | Use `<skaile-dev-root>/.worktrees/platform-<number>` (gitignored): keeps the parent checkout clean AND keeps the worktree inside the skaile-dev tree so dispatched subagents auto-load the platform CLAUDE.md. A /tmp path is outside the memory tree, so the subagent never sees platform conventions (e.g. the ≤3-line comment rule) and silently violates them. |
| Committing the plan file | Delete it in STEP 11 before `git add -A` |
| Passing parent conversation to the implementer | Build a self-contained MVC prompt — the implementer must never read this chat |
| Skipping the review dispatch on a non-trivial change | Only skip review when `complexity=small` AND ≤ 2 files AND ≤ 30 LOC |
| Removing the worktree before pushing | Worktrees contain LOCAL commits — push first, remove second |
| Skipping the main-sync merge before opening the PR | Main may have advanced during investigation/implementation; merge it now so the PR diff is clean and conflicts are resolved while local context is still available — not later in the GitHub UI |
| Resolving merge conflicts by blindly taking one side on source files | For source code in the change's blast radius, both sides usually carry intent — blend them. Policy-based `--theirs` is only for generated files and lockfiles. |
| Running Biome inside the worktree | Platform uses Prettier + ESLint. `bun run lint` inside `backend/` or `frontend/` |
| Verbatim-copying the user's wording into the issue | Refine it (1-3 sentences for bug/issue/ui/chore; up to 5 for feature) — but preserve user-stated open questions verbatim, in bold |
| Opening the PR before the branch is pushed | `gh pr create` will fail; push first |
| Picking the wrong commit type for a non-bug | Map category → type: bug/issue/ui → `fix`, chore → `chore`, feature → `feat`. The PR title prefix and commit type must match. |

## Integration

- **Called by:** `skaile-development` agent when the user reports a platform bug, requests a small feature, asks for a UI fix, files a chore, or raises an issue
- **Calls (via Agent tool):** `implement` (or `general-purpose`) for the change; `review` for the diff review
- **Uses:** `gh` CLI for the GitHub issue + the PR; `git` directly for branch / worktree / commit / push (does NOT call the `git` skill because the flow is too tightly choreographed)
- **Reads:** `platform/CLAUDE.md`, affected source files, `gh label list` / `gh issue list`
- **Writes:** a GitHub issue on `skaile-ai/platform`, implementation commits on the branch, a transient plan file (deleted), a PR on `skaile-ai/platform` that closes the issue
- **Never reads or writes:** `platform/issues/` (decommissioned markdown folder) or `platform/issues.md` (legacy snapshot) — tracking is GitHub Issues now
