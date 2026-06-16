---
name: "ship"
description: "[skaile-development] Implement AND ship a single work item end-to-end in
  ANY skaile-dev repo — bug, feature, UI fix, chore, or issue. Drives it from 'just
  reported' to 'merged' in one flow: resolves the target repo + its conventions, opens
  a GitHub issue (category label, assigned to the current gh user), creates an isolated
  worktree + branch named after the issue number, investigates, writes an uncommitted
  plan, dispatches a fresh agent to implement, dispatches a fresh agent to review,
  fixes valid review concerns, commits + pushes, opens a PR that closes the issue,
  reports the implementation summary, then BABYSITS the PR — watching CI to green,
  waiting for automated review bots, and fixing every change-request related to the
  work (including style nits) in a loop — and finally asks whether to squash-merge +
  clean up, clean up only, or stop. Use when the user reports a bug, requests a feature,
  asks for a UI fix or chore in any repo, and wants the whole cycle done in one shot."
version: 1.0.0
metadata:
  tags:
  - "ship"
  - "implement"
  - "deliver"
  - "bug-fix"
  - "feature"
  - "ui-fix"
  - "chore"
  - "work-item"
  - "any-repo"
  - "orchestrator"
  - "worktree"
  - "github-issues"
  - "pr"
  - "babysit-pr"
  - "ci"
  - "merge"
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
    - id: "repo"
      label: "Target repo / submodule (e.g. platform, store, workspaces, brand). Leave
        empty to auto-infer from the description"
      type: "text"
      required: false
      hint: "The skaile-dev submodule the work lands in. Auto-inferred from the
        description when obvious (named package, 'platform', 'store frontend', a CLI
        command…); the skill confirms or asks only when it cannot tell."
    - id: "issue"
      label: "Existing GitHub issue number to attach to (skip opening a new one)"
      type: "text"
      required: false
      hint: "If the work item already has a GitHub issue on the target repo, pass its
        number and the skill reuses it instead of opening a new one."
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

# Ship — Implement & Ship a Work Item End-to-End (Any Repo)

> One skill, one work item, all the way to merged. `ship` **implements** the change and
> then **ships** it: it files the work item, builds it, opens the PR, drives the PR
> through CI and review, and merges it. It is repo-agnostic — it works against any
> skaile-dev submodule (platform, store, workspaces, brand, …), adapting to each repo's
> own conventions, formatter, and lint/test commands. The category (bug / feature / ui /
> chore / issue) only changes the GitHub label, branch prefix, and commit type.

## Tracking is GitHub Issues; repos are generalized

- **Tracking lives in GitHub Issues** on the target repo — not in any per-repo markdown
  folder. (Platform's old `platform/issues/` folder was a standalone file-backed tracker
  with **no GitHub sync**; it was bulk-migrated into GitHub Issues and is now legacy.
  Never write to it.) The work item's identity is the **native GitHub issue number**
  (`#836`); the category travels as a **label**, not an id prefix.
- **The target repo is resolved at runtime** (input `repo`, else inferred from the
  description, else asked). Its GitHub slug, default branch, conventions, formatter, and
  lint/test commands are **derived from that repo** — never hardcoded to platform.

## Overview

| Phase | What Happens |
|-------|--------------|
| 0 | Read inputs, verify `gh` auth, resolve the **target repo + its profile**, auto-classify category |
| 1 | Pre-flight the target repo's main (fetch + clean-tree check) |
| 2 | Refine the description; open the GitHub issue (or reuse `issue`) → capture issue **number** + URL |
| 3 | Derive branch name from the issue number; create the worktree + branch (off the repo's `origin/main`) |
| 4 | Investigate: load the repo's CLAUDE.md + relevant files; identify root cause / design surface |
| 5 | Write an **uncommitted** plan markdown into the worktree |
| 6 | Dispatch a fresh subagent to implement the plan (gets only the plan + context — no parent history) |
| 7 | Dispatch a fresh subagent to review the diff (skippable for trivial work) |
| 8 | Triage review findings, apply valid fixes inline |
| 9 | Delete the plan; run the repo's lint + affected tests; commit (work only); push the branch |
| 9b | Fetch latest `origin/main`, merge into the branch, resolve conflicts, re-push |
| 10 | Open a PR against `main` that `Closes #<number>` (respect PR template + changeset rules) |
| 11 | **Report the implementation summary** to the user |
| 12 | **Babysit the PR**: watch CI to green, wait for review bots, fix every related change-request (incl. nits) in a loop; report unrelated/architectural problems without fixing them |
| 13 | Ask the user: **squash-merge + clean up** \| **clean up only** \| **stop here** — then execute the choice |
| 14 | Final report |

The worktree and local branch **persist** through phases 9–12 (they are where babysit
fixes are made) and are torn down only by the Phase 13 choice — never automatically.

## When to Use

- The user describes a work item in any skaile-dev repo: a bug, a missing capability
  ("issue"), a cosmetic fix ("UI"), a maintenance task ("chore"), or a small/medium feature
- They want it filed as a GitHub issue, implemented, and **driven to merge** in one continuous flow
- It is contained enough for one implementation pass + one review pass — not a multi-week refactor

## When NOT to Use

- Large feature work that warrants a proposal / design doc first — file a proposal first
- Work that spans multiple repos at once — split it; run `ship` per repo
- Anything that needs design discussion before code — file a proposal first
- Something already on a branch with no PR — this skill expects either a clean start or an
  existing PR to resume babysitting (pass `repo` + `issue` + the branch already pushed)

---

## Operating Principle — Autonomy by Default

**Run end-to-end without asking the user.** They gave you the work item; they expect it
shipped. Decisions you can settle from evidence — target repo (when inferable),
classification, description wording, branch name, plan, which review findings to apply,
conflict resolutions where one side is a strict superset, which babysit fixes are
in-scope — you make silently. Print short progress notes (`> "Targeting store; opened
#214 (auto-classified as feature)"`, `> "CI green; bot left 2 nits — fixing"`) but do
NOT solicit confirmation.

Only stop and ask when continuing needs a judgment the evidence cannot settle. The
complete list of legitimate stop-and-ask gates is:

1. **`gh` is not authenticated** — the issue/PR cannot be created. Tell the user to run `gh auth login`.
2. **The target repo cannot be determined** — input absent and the description is ambiguous across submodules. Ask which repo.
3. **Working tree of the target repo is dirty** — the worktree branches off `origin/<default_branch>` so a dirty checkout doesn't strictly block it, but an unexpected dirty state is ambiguous (in-progress work, a failed earlier run); stop so the user resolves it consciously.
4. **Investigation inconclusive** — root cause / design surface cannot be located; report (issue stays open, labelled `needs-info`) and stop.
5. **A review finding is "apply-large"** — spawning a second implementation subagent is a meaningful escalation; confirm scope.
6. **A merge conflict on source code is genuinely mutually-exclusive** — both intents cannot stand; the user must pick.
7. **Push fails for a non-trivial reason** — e.g., the branch was rewritten remotely.
8. **The babysit loop cannot converge** — CI stays red on something unrelated to the change, or the same review item recurs after a good-faith fix; report and ask.
9. **The final disposition** — merge + cleanup / cleanup only / stop here (Phase 13). This is the ONE planned interactive checkpoint at the end.

Everything else you do silently. The user can always interrupt to redirect.

---

ROLE  End-to-end work-item shipper for any skaile-dev repo. Owns the full cycle: resolve repo → GitHub issue → worktree → plan → implement → review → PR → babysit (CI + reviews) → merge/cleanup. Category only influences the GitHub label, branch prefix, and commit type.

READS
  skaile-dev/CLAUDE.md                            — monorepo conventions, the Formatting + Testing tables (per-repo formatter/lint/test)
  <repo>/CLAUDE.md                                — target repo architecture, conventions, lint/test recipes
  <repo>/package.json                             — the repo's lint / test / format scripts
  <repo>/.changeset/, <repo>/.github/             — changeset enforcement + PR template detection
  <repo>/<files-relevant-to-work-item>            — for investigation
  skills/git/references/branch_naming.md          — branch naming
  skills/git/references/commit-spec.md            — commit message format
  gh auth status                                  — verify GitHub auth before issue/PR creation
  gh label list / gh issue list / gh pr ...       — labels, dup detection, PR + CI + review state

WRITES
  GitHub issue on <repo's slug>                   — opened up front (category label if it exists + `agent`, assigned @me); reused if `issue` is passed
  <worktree>/<plan-file>.md                       — uncommitted plan (deleted before the final commit)
  <worktree>/<target-source-files>                — the implementation + babysit fixes
  PR on <repo's slug>                              — opened after implementation; body contains `Closes #<number>`
  commits on the feature branch                   — implementation, main-sync merge, and babysit fix commits

NEVER WRITES
  platform/issues/ or any repo's legacy markdown issue folder — tracking is GitHub Issues now

MUST  verify `gh auth status` succeeds before opening the issue or PR
MUST  resolve the target repo (input `repo` → inference from description → ask) and validate the submodule path + GitHub remote exist
MUST  derive the repo profile from the repo itself — GitHub slug (`git -C <repo> remote get-url origin`), default branch, formatter, lint/test/format commands, changeset requirement — and NEVER hardcode platform's toolchain onto another repo
MUST  respect each repo's formatter: platform = Prettier + ESLint (NEVER Biome); workspaces / brand / docs = Biome; defer to the repo's CLAUDE.md + package.json scripts when unsure
MUST  refine the user's description before filing it — 1-3 sentences for bug/issue/ui/chore (up to 5 short for features), technical and specific, no marketing fluff
MUST  auto-classify the category without asking; the `category` input only overrides
MUST  open the work item as a GitHub issue on the target repo (or reuse `issue`), capturing the **number** and URL; apply the category label if it exists in the repo, plus `agent`; assign `@me`
MUST  derive the work item's identity from the GitHub issue **number** (`#<n>`) for branch, commit reference, and PR
MUST  open the issue BEFORE the worktree (branch name needs the number) but AFTER the clean-tree pre-flight (so a dirty tree never leaves a dangling issue)
MUST  create the worktree at <skaile-dev-root>/.worktrees/<repo>-<number> (gitignored) — outside the repo so its checkout stays clean, but INSIDE the skaile-dev tree so dispatched subagents auto-load the repo's CLAUDE.md via on-demand memory discovery
MUST  keep the worktree and local branch ALIVE through the PR + babysit phases — tear them down ONLY in Phase 13 per the user's choice
MUST  write the plan in the worktree and treat it as ephemeral — never commit it, delete it before the final commit
MUST  dispatch implementation as a fresh, self-contained subagent (paste the plan + repo conventions + acceptance criteria; do NOT pass parent conversation)
MUST  dispatch review as a fresh subagent that only sees the diff + review focus — not the plan (skippable only for complexity=small AND ≤2 files AND ≤30 LOC)
MUST  triage review findings explicitly: each gets a verdict (apply / defer / reject) with a one-line reason
MUST  run the repo's lint AND affected tests locally before pushing; add a changeset if the repo enforces them
MUST  push the branch before opening the PR
MUST  fetch origin/main and merge it into the branch BEFORE opening the PR, resolving conflicts while the full local context is available
MUST  open the PR with `gh pr create --base <default_branch> --head <branch>`, body containing `Closes #<number>`, honoring the repo's PR template if present
MUST  report a clear implementation summary to the user after the PR is opened (Phase 11) — before babysitting
MUST  babysit the PR (Phase 12): watch CI to completion (this also waits for automated review-bot checks), read the PR's reviews + comments, and fix every actionable item — including style nits — that relates to the work item, looping push → re-watch until CI is green and the only remaining review notes are ones the reviewer explicitly blesses as fine to keep
MUST  fix only items RELATED to the change (e.g. lint/type/test failures the change caused, review nits on the diff); for unrelated/pre-existing/architectural problems, REPORT them to the user and do NOT fix them
MUST  converge the babysit loop — cap fix rounds, and if CI stays red on something unrelated or a review item recurs after a good-faith fix, stop and ask (gate #8)
MUST  ask the user at the end (Phase 13) to choose: squash-merge + cleanup | cleanup only | stop here — and execute exactly that
MUST  use squash-and-merge (`gh pr merge <n> --squash`) when merging
MUST  on cleanup: remove the worktree and delete the local branch; on merge+cleanup also delete the remote branch
MUST  report back with: repo, issue number + URL, branch, PR URL + final state (merged / open), 1-2 line summary, deferred/unrelated items
NEVER  hardcode platform paths (`backend/`, `frontend/`), platform's slug, or platform's toolchain onto another repo
NEVER  run Biome inside platform/ — platform uses Prettier + ESLint exclusively
NEVER  write to any repo's legacy markdown issue folder, bump a `nextIds` counter, or allocate a `<PREFIX>-<n>` id
NEVER  ask the user to confirm the refined description, category, branch name, plan, or triage table
NEVER  remove the worktree or delete a branch before Phase 13 — fixes during babysitting need them
NEVER  open a PR before the branch is pushed
NEVER  force-push, rewrite history, or commit to the repo's main from inside the worktree
NEVER  start fixing problems that are out of the change's scope (broken CI architecture, pre-existing failures, unrelated tech debt) — report them instead

EMIT [ship] started description="<short>"

# ── Phase 0: Inputs, Identity, Repo Profile ───────────────────────

STEP 0: Resolve inputs + GitHub auth
  - Read `description` (required), `complexity` (default "standard"),
    `repo`, `issue`, `category`, `branch_slug` (all optional).

  Verify GitHub auth:
    $ gh auth status
    IF not logged in: STOP (gate #1): "gh is not authenticated. Run `gh auth login`."

  Resolve the human's name for attribution (SOFT — optional courtesy footer only):
    $ git -C <repo> config user.name → owner_name   (fallback: global)
    The GitHub assignee is `@me`; owner_name is just a body footer if present.

STEP 0b: Resolve the target repo + profile
  Determine `repo`:
    1. IF `repo` input given → use it.
    2. ELSE infer from the description: an explicitly named repo ("platform",
       "store", "workspaces", "brand"…), a package name, a CLI command, a file
       path, or a feature clearly owned by one repo.
    3. IF still ambiguous across submodules → ASK (gate #2): "Which repo does this
       land in?" with the candidate submodules.

  Validate + build the profile (derive, do NOT hardcode):
    repo_path     = "<skaile-dev-root>/<repo>"
    IF repo_path is not a git submodule/checkout → STOP: "No such repo '<repo>'."
    github_slug   = parse `git -C <repo_path> remote get-url origin`
                    (e.g. git@github.com:skaile-ai/skaile-store.git → skaile-ai/skaile-store)
    default_branch= `gh repo view <github_slug> --json defaultBranchRef -q .defaultBranchRef.name`
                    (fallback: `git -C <repo_path> symbolic-ref --short refs/remotes/origin/HEAD`
                    with "origin/" stripped; final fallback "main"). Do NOT assume "main" —
                    some repos default elsewhere, and `origin/HEAD` is often unset in submodules.
    Read `<repo_path>/CLAUDE.md` + `<repo_path>/package.json` "scripts" + the root
    `skaile-dev/CLAUDE.md` Formatting + Testing tables to fill:
      formatter   = Biome | Prettier+ESLint | (none, for non-JS content repos)
      lint_cmd    = how to lint the packages this change will touch
      test_cmd    = how to run the repo's (affected) tests
      format_cmd  = the repo's format script
    changeset_required = TRUE iff `<repo_path>/.changeset/` exists AND a
                         changeset-check workflow is present
    pr_template = `<repo_path>/.github/PULL_REQUEST_TEMPLATE.md` if present

  Known specifics to honor (examples — always defer to the repo itself):
    - platform: Prettier + ESLint, `bun run lint` inside `backend/`/`frontend/`;
      NEVER Biome; changesets enforced (`'@postxl/...': patch`).
    - store: its own backend/frontend/shared; check its scripts + changeset config.
    - workspaces / brand / docs: Biome.
    - marketing: workspace-isolated — run `bun install` / build inside `marketing/`.
    - non-JS content repos (infra, ai-assets): may have no lint/test — skip gracefully.

  Print: > "Targeting <repo> (<github_slug>, default branch <default_branch>, formatter <formatter>)."

EMIT [ship] repo_resolved repo=<repo> slug=<github_slug>

STEP 0c: Auto-classify the category (do NOT ask)
  | Signal in description | category | GitHub label |
  |-----------------------|----------|--------------|
  | Broken behavior, error, data loss, security, wrong result, crash, regression | Bug | `bug` |
  | Existing functionality incomplete; "should also be able to X"; design gap, no breakage | Issue | `issue` |
  | Purely visual: layout, spacing, color, font, alignment, tooltip, hover, icon | UI | `ui` |
  | Maintenance: dependency bump, behavior-preserving refactor, cleanup, tooling | Chore | `chore` |
  | Genuinely new capability that didn't exist before | Feature | `enhancement` |

  Tie-breakers (in order): broken→bug; new capability→feature; existing-should-do-more→issue;
  cosmetic-only→ui; behavior-preserving maintenance→chore.

  `category` input overrides. Print: > "Auto-classified as <category> (<one-word reason>)."

  Map category → label via PROCEDURE category_label, then build label_args from ONLY the
  labels that actually exist (a missing label passed to `gh issue create` aborts the whole
  command — so never pass an unconfirmed label):
    $ gh label list --repo <github_slug> --json name -q '.[].name' → existing_labels
    label_args = ""
    FOR each of [<category_label>, "agent"]:
      IF it is in existing_labels → append `--label <name>` to label_args
      ELSE → note "label <name> missing — filing without it"; never invent a label.

# ── Phase 1: Pre-flight ───────────────────────────────────────────

STEP 1: Pre-flight the target repo (BEFORE opening the issue — a dirty tree must not leave a dangling issue)
  $ cd <repo_path>
  $ git fetch origin
  $ git status --short
  IF dirty: STOP (gate #3): "<repo> has uncommitted changes — ambiguous state. Stash or
    commit them first." (The worktree itself branches off origin/<default_branch> and is
    not contaminated by a dirty checkout; we stop because an unexpected dirty tree usually
    means in-progress work or a failed earlier run the user should resolve consciously.)
  (No need to checkout main — the worktree branches off origin/<default_branch>.)

# ── Phase 2: Refine + Open the GitHub Issue ───────────────────────

STEP 2: Refine the description (PROCEDURE refine_description)
  - 1-3 sentences (≤5 short for features); name the affected surface; observed vs
    expected (or the new behavior); drop filler; preserve user-stated constraints
    verbatim, bolded (e.g. "**open question for Peter**: …").
  - Also derive a concise **issue title** (≤72 chars), imperative, naming the surface.
  Do NOT ask the user to approve wording. Only ask one targeted question if the input
  is too vague to refine at all.

STEP 3: Open (or reuse) the GitHub issue
  IF `issue` input given:
    $ gh issue view <issue> --repo <github_slug> --json number,url,title
    Reuse its number + URL; ensure it's open; add the `agent` label if missing.
    Skip creation.
  ELSE:
    Optional cheap dup check (do NOT block):
      $ gh issue list --repo <github_slug> --state open --search "<2-3 key terms>"
    Build the body = refined description + optional footer
      "\n\n---\nFiled via the ship skill[ by <owner_name>]."
    $ issue_url=$(gh issue create \
        --repo <github_slug> \
        --title "<issue title>" \
        --body "<body>" \
        <label_args> \
        --assignee @me)
    $ issue_number=$(basename "$(printf '%s' "$issue_url" | tail -n1)")   # last stdout line = the URL
    IF issue_number is not all-digits → STOP: "Could not parse the new issue number from gh output."
    (Robust extraction — take the final stdout line, basename it, validate it's numeric.)

  Print: > "Opened #<issue_number> (<category>) — <issue_url>."

EMIT [ship] issue_ready number=<issue_number> url=<issue_url>

STEP 3b: Resume detection (skip building if a PR already exists for this work)
  IF `issue` was passed AND an open PR already closes it:
    $ existing=$(gh pr list --repo <github_slug> --state open \
        --search "Closes #<issue_number> in:body" --json number,headRefName,url)
    IF exactly one match → RESUME:
      pr_number = its number; pr_url = its url; branch_name = its headRefName
      $ cd <repo_path> && git fetch origin <branch_name>
      $ git worktree prune
      $ git worktree add <worktree_path> <branch_name>   # attach to the EXISTING branch (no -b)
      Print: > "Resuming PR <pr_url> for #<issue_number> — skipping to babysitting."
      SKIP STEP 4..STEP 13 and JUMP to Phase 12 (STEP 14). Phase 13 + 14 follow normally.
    IF more than one match → ASK which PR to resume.
  ELSE → continue to STEP 4 (fresh flow).

# ── Phase 3: Branch + Worktree ────────────────────────────────────

STEP 4: Branch name
  slug = branch_slug if given, else built from the refined description
         (lowercase, hyphens, ≤40 chars, concrete nouns).
  Branch prefix by category: bug/issue/ui → "fix/", chore → "chore/", feature → "feat/".
  branch_name = "<prefix><issue_number>-<slug>"   e.g. "feat/214-bulk-export", "fix/836-chat-link-wrap"

STEP 5: Worktree (fresh flow; persists until Phase 13)
  worktree_path = "<skaile-dev-root>/.worktrees/<repo>-<issue_number>"
  $ cd <repo_path>
  $ git worktree prune   # clear any ghost entry from a prior force-removed run
  $ git worktree add <worktree_path> -b <branch_name> origin/<default_branch>
  All file ops until Phase 13 happen INSIDE <worktree_path>.

EMIT [ship] worktree_ready path=<worktree_path> branch=<branch_name>

# ── Phase 4: Investigate ─────────────────────────────────────────

STEP 6: Investigate
  Read (stop when the cause / design surface is clear):
  1. `<repo>/CLAUDE.md` sections for the work item's surface area
  2. The exact file(s) the user named
  3. Adjacent files along the data flow
  4. Recent commits touching the area (`git log --oneline -- <path>`)

  Produce internally (not yet on disk):
  - bug/issue/ui: root_cause, blast_radius, fix_candidates, chosen_fix
  - chore: target_surface, risk_areas, chosen_approach
  - feature: design_note, surfaces_touched, chosen_approach

  IF the cause / design surface cannot be located confidently (gate #4):
    $ gh issue edit <issue_number> --repo <github_slug> --add-label needs-info  (if label exists)
    $ cd <repo_path> && git worktree remove <worktree_path> --force   (no commits made)
    REPORT: "Investigation inconclusive. Opened #<issue_number> (needs-info). <what I checked>. <what would help>." → STOP.

EMIT [ship] investigated note="<short>"

# ── Phase 5: Plan (uncommitted) ───────────────────────────────────

STEP 7: Write the plan at plan_path = "<worktree_path>/<issue_number>-plan.md"
  ```
  # #<issue_number> — <title> — Implementation Plan
  ## Summary            <refined description, verbatim>
  ## Category           <bug | issue | ui | chore | feature>
  ## Root Cause / Design Note   <1-2 sentences>
  ## Files to Touch     - <path> — <what changes>   (+ tests)
  ## Implementation Steps   1. … 2. … 3. …
  ## Acceptance Criteria
  - [ ] <observable behavior>
  - [ ] Lint passes (<repo's lint_cmd>)
  - [ ] Tests pass (<repo's test_cmd>)
  - [ ] No regression to <adjacent surface>
  ## Out of Scope       - <thing intentionally not changed>
  ## Notes for the Implementer   <repo conventions: see <repo>/CLAUDE.md>
  ```
  DO NOT commit this file. Print: > "Plan written. Dispatching implementer."

# ── Phase 6: Implementation Dispatch (fresh subagent) ─────────────

STEP 8: Dispatch implementation (self-contained MVC prompt)
  ```
  # Task: Implement #<issue_number> in the <repo> repo
  ## Working Directory
  <worktree_path>
  (Branch <branch_name> is checked out. The repo's CLAUDE.md auto-loads here — follow it.)
  ## Work Item            <refined description>
  ## Category             <…>
  ## Root Cause / Design Note   <…>
  ## Plan                 <paste FULL <plan_path>>
  ## Conventions (do not violate)
  - Follow <repo>/CLAUDE.md and the nested per-package CLAUDE.md files exactly.
  - Use the repo's OWN formatter/linter: <formatter> via <lint_cmd> / <format_cmd>.
    (Platform: Prettier + ESLint, never Biome. Workspaces/brand/docs: Biome.)
  - Run <test_cmd> for the area you touch; add/extend a test that locks in the behavior.
  - IF the repo enforces changesets: add one (e.g. platform: `.changeset/<slug>.md`).
  ## What to Implement    Follow the plan exactly; verify acceptance criteria.
  ## What NOT to Do
  - Do NOT edit/delete <plan_path> (orchestrator owns it).
  - Do NOT commit, push, or open a PR — leave changes in the working tree.
  - Do NOT write to any legacy markdown issue folder (tracking is GitHub Issues).
  ## Output   Files changed (relative paths); acceptance criteria verified; anything off-plan you noticed.
  ```
  DISPATCH: Agent({ subagent_type: "implement", description: "Implement #<issue_number>", prompt: "<above>" })
  WAIT; capture files_changed, criteria_verified, agent_notes.

EMIT [ship] implementation_done files=<N>

# ── Phase 7: Review Dispatch (fresh subagent) ─────────────────────

STEP 9: Dispatch review
  Measure the diff (don't trust the implementer's free-text count):
    $ git -C <worktree_path> diff --numstat origin/<default_branch>...  → files + added/removed LOC
  IF complexity = small AND files_changed ≤ 2 AND total changed LOC ≤ 30:
    Skip — review the diff inline; review_findings = []. Continue to STEP 10.
  ELSE dispatch:
  ```
  # Task: Review the diff in <worktree_path> (#<issue_number>, <category>): <refined description>
  Run `git diff`. Review for: correctness vs the work item; scope creep; the repo's
  conventions (read <repo>/CLAUDE.md — formatter, DI/import rules, framework patterns);
  test coverage; performance (N+1, unbounded loops); security (input validation, no secrets).
  Do NOT report: style the formatter/linter handles; pre-existing issues outside the diff; >5 nits (summarize).
  Output each finding: severity(important|nit|preexisting) — file:line — one-line — detail.
  End with: "ready to merge" | "fix important findings first" | "needs broader change".
  ```
  DISPATCH: Agent({ subagent_type: "review", description: "Review #<issue_number>", prompt: "<above>" })
  WAIT; parse review_findings.

EMIT [ship] review_done important=<N> nits=<N>

# ── Phase 8: Triage + Apply ───────────────────────────────────────

STEP 10: Triage (PROCEDURE triage_finding) — decide silently
  IF review was skipped in STEP 9 (review_findings empty): nothing to triage — continue to STEP 11.
  Otherwise, for each finding:
  | Verdict | When | Action |
  |---------|------|--------|
  | apply       | Important + real + ≤20 LOC | Fix inline now |
  | apply-large | Important + ≥20 LOC / new abstraction | ASK before a 2nd implementation subagent (gate #5) |
  | defer       | Nit / non-blocking / out of scope for the PR | Note under "Deferred follow-ups" in the PR |
  | reject      | Disagreement | Note in the PR with one-line reason |
  Apply `apply` items immediately; record defer/reject for the PR body. ASK only on
  apply-large or when rejecting an `important` finding. Else: > "Triaged: <a>/<d>/<r>. Continuing."

EMIT [ship] triage_done applied=<N> deferred=<N> rejected=<N>

# ── Phase 9: Lint, Test, Commit, Push (NO cleanup) ────────────────

STEP 11: Drop plan, lint, test, commit, push
  $ cd <worktree_path>
  $ rm -f <issue_number>-plan.md

  Lint + test the affected area using the REPO'S commands (from the profile):
    $ <lint_cmd>      # e.g. platform: cd backend && bun run lint / cd frontend && bun run lint
    $ <test_cmd>      # the repo's affected-tests command
    IF lint/tests fail: make ONE focused fix attempt; if they still fail, STOP: "Lint/test
      failures persist after one fix attempt — worktree preserved at <worktree_path> for
      manual inspection." (Do not loop indefinitely trying to fix.)
    IF a formatter mutated files: $ git add <files>
  IF changeset_required AND no changeset staged: add one now (repo's format), $ git add it.

  Commit (work only — no issue file, no counter bump):
    $ git add -A
    $ git status --short   (verify the plan file is gone; if present → STOP)
    Conventional type by category: bug/issue/ui → fix, chore → chore, feature → feat.
    ```
    <type>(<scope>): <title> (#<issue_number>)

    <1-3 sentence explanation (root cause + fix, or motivation + outline)>
    ```
    (Reference `#<issue_number>` in the title; the auto-close `Closes #<n>` goes in the
    PR body only, so the issue closes exactly once, on merge. Follow the repo's commit
    convention — read skills/git/references/commit-spec.md and the repo's CLAUDE.md.)
    $ git commit -m "<message>"

  Push:
    $ git push -u origin <branch_name>
    IF push fails → STOP (gate #7): "Push failed — worktree preserved. Investigate."

EMIT [ship] pushed branch=<branch_name>

# ── Phase 9b: Sync Branch with Main ───────────────────────────────

STEP 11b: Fetch + merge origin/<default_branch> into the branch (keeps the PR diff clean)
  $ cd <worktree_path>
  $ git fetch origin <default_branch>
  $ git log HEAD..origin/<default_branch> --oneline → incoming
  IF empty: > "Up-to-date with main." → STEP 12.
  $ git merge origin/<default_branch> --no-edit

  IF conflicts — resolve per policy:
    | File pattern | Resolution |
    |--------------|-----------|
    | Generated files (lock-listed) | `git checkout --theirs`; regenerate if needed |
    | Lockfiles (`bun.lock`, …) | `git checkout --theirs` then re-resolve (`bun install`) |
    | Source touched by both the change and main | **Blend manually** — preserve BOTH intents |
    (No legacy issue-folder or counter conflicts exist — tracking is GitHub Issues.)
    (In `git merge origin/<default_branch>`, `--ours` = the feature branch and `--theirs` =
    main. Always merge here, never rebase, so this mapping stays fixed.)
    For source in the change's blast radius, blend; if genuinely mutually-exclusive,
    ASK (gate #6): "<file>: HEAD does X, main does Y — keep HEAD / take main / explain a blend?"
    Verify no `<<<<<<< ======= >>>>>>>` markers remain; $ git add each; re-run <lint_cmd>
    AND <test_cmd> (a manual blend can break behavior lint won't catch); complete:
    $ git commit --no-edit   (re-format + re-add if a pre-commit hook rewrites files)

  Verify clean, then push:
    $ git push origin <branch_name>
    IF non-fast-forward (main moved again): re-fetch, re-merge, re-push.
  > "Synced with main: <N> commits. Conflicts: <files+resolution>. Pushed."

EMIT [ship] synced_with_main incoming=<N>

# ── Phase 10: Open PR ─────────────────────────────────────────────

STEP 12: Open the PR
  Title type by category (matches commit type): bug/issue/ui → fix, chore → chore, feature → feat.
  IF pr_template exists: fill its sections; otherwise use the default body below.
  $ cd <repo_path>
  $ gh pr create --base <default_branch> --head <branch_name> \
      --title "<type>(<scope>): <title> (#<issue_number>)" \
      --body "$(cat <<'EOF'
## What
<1-3 sentences>
## Why
Closes #<issue_number>: <refined description>
## Changes
- <file-by-file bullets>
## Testing
- [ ] Tests pass locally
- [ ] Manually verified <observable behavior>
- [ ] No regression in <adjacent surface>
## Deferred Follow-ups
[only if any] - <finding> — <reason>
EOF
)"
  Capture pr_url and pr_number. `Closes #<n>` auto-closes the issue on merge.

EMIT [ship] pr_opened url=<pr_url> number=<pr_number>

# ── Phase 11: Implementation Summary ──────────────────────────────

STEP 13: Report the implementation summary (before babysitting)
  Print:
  ```
  ── Implemented & PR opened ─────────────────────────────────────
  Repo:    <repo> (<github_slug>)
  Issue:   #<issue_number> — <title>   <issue_url>
  Branch:  <branch_name>
  PR:      <pr_url>
  What changed: <1-2 sentence plain-language summary>
  Files (<N>): <list>
  Review: applied <A> / deferred <D> / rejected <R>
  Next: babysitting the PR — watching CI and review bots, fixing related feedback.
  ────────────────────────────────────────────────────────────────
  ```

EMIT [ship] summary_reported

# ── Phase 12: Babysit the PR ──────────────────────────────────────

STEP 14: Drive the PR to a clean, reviewed state
  Goal: end with CI GREEN and the only remaining review notes being ones a reviewer
  EXPLICITLY says are fine to keep. Wait for automated review bots to finish; do NOT
  block on absent human reviewers.

  babysit_round = 0; MAX_ROUNDS = 6; cumulative budget ≈ 45 min wall-clock
  LOOP:
    babysit_round += 1
    IF babysit_round > MAX_ROUNDS OR cumulative babysit time > ~45 min
       → STOP (gate #8): report current state, ask how to proceed.

    (a) Wait for CI + review bots, handling the no-checks case first:
        Detect checks reliably via JSON (exits 0 even when empty), NOT by scraping text:
        $ gh pr view <pr_number> --repo <github_slug> --json statusCheckRollup -q '.statusCheckRollup | length'
        IF 0 (no CI configured — common for content repos like ai-assets/infra, or
        workspace-isolated marketing): set ci_state = "no-checks"; skip the watch.
        ELSE: $ gh pr checks <pr_number> --repo <github_slug> --watch --interval 30
              (this also waits for review-bot check runs to finish; note `gh pr checks`
              exits non-zero on failure (1) and on no-checks (8) — rely on the JSON above
              for the no-checks decision and on the post-watch snapshot for pass/fail, not
              on the exit code alone). Wall-clock cap: if checks stay queued / never start
              for ~15 min, stop watching and treat it as gate #8.
        Then read review + merge state:
        $ gh pr view <pr_number> --repo <github_slug> \
            --json reviews,reviewThreads,comments,reviewDecision,mergeStateStatus,statusCheckRollup

    (b) Collect actionable items:
        - FAILED required checks (lint, typecheck, tests, changeset-check, build).
        - Review change-requests + inline review comments (bot e.g. claude-code-review,
          and any already-posted human reviews). Include STYLE NITS.
        For each item classify (PROCEDURE classify_babysit_item):
          RELATED  → caused/exposed by this change (our lint/type/test failure, a nit on a
                     line we touched, a missing changeset, a bot suggestion on our code, OR
                     a check that is green on origin/<default_branch> but red on the branch)
          UNRELATED→ failure that is ALSO red on main, flaky infra, broken CI architecture,
                     tech debt elsewhere, a comment about code we didn't touch

    (c) IF there are RELATED actionable items:
        For each, compute a fingerprint = <file>:<line-or-near> + <rule / short text>.
        IF a fingerprint MATCHES one already in the seen-set (you fixed it in an earlier
        round and it came back), the fix didn't satisfy the reviewer or the bot disagrees
        with it → do NOT re-fix blindly; escalate to gate #8 (report the recurring item +
        your reasoning, ask how to proceed) rather than burning rounds.
        Otherwise fix them in the worktree (small fixes inline; if one is apply-large,
        gate #5). Address nits too — the bar is "clean," not "only must-fixes." The ONLY
        items you may leave are ones the reviewer explicitly marked optional / fine-to-keep.
        $ cd <worktree_path> && <lint_cmd> && <test_cmd>
        $ git add -A && git commit -m "fix(<scope>): address review feedback (#<issue_number>)"
        $ git push origin <branch_name>
        Add each fixed item's fingerprint to the seen-set; reply briefly on resolved review
        threads and mark them resolved where possible.
        CONTINUE loop (new commit re-triggers CI + bots — wait for them in the next round).

    (d) IF there are UNRELATED items:
        Do NOT fix them. Record each (what, where, why out of scope) for the final report.
        IF an unrelated failure BLOCKS merge (e.g. a required check is red for reasons
        not caused by us): note it; it will surface in Phase 13 as a merge blocker.

    (e) EXIT the loop when ALL hold:
        - every required check is green, OR ci_state = "no-checks", OR the only red checks
          are unrelated + recorded,
        - automated review bots have completed for the latest commit (or there are none),
        - no unaddressed RELATED change-requests/comments remain (only reviewer-blessed leftovers).
        Do NOT wait for a human who hasn't reviewed.

  Print: > "Babysit done after <R> round(s). CI: <green|blocked-by-unrelated>. Fixed: <N> related items. Unrelated/reported: <M>."

EMIT [ship] babysit_done rounds=<R> fixed=<N> unrelated=<M>

# ── Phase 13: Final Disposition (the one planned checkpoint) ───────

STEP 15: Ask the user how to finish (gate #9)
  Present concise state, then ask (use the AskUserQuestion tool):
    Context line: "PR <pr_url> — CI <green|no-checks|blocked: …>, reviews <addressed|none yet|N blessed-nits>.
                   <if reviewDecision requires approval and none is present: 'Note: branch
                    protection needs a human approval — auto-merge will be blocked.'>
                   <if unrelated blockers: 'Note: <X> is red for unrelated reasons.'>"
    Question: "How should I finish this PR?"
      - "Squash-merge + clean up" — squash-merge the PR, delete the remote branch, remove the worktree, delete the local branch.
      - "Clean up only" — leave the PR open; remove the worktree + delete the local branch (keep the remote branch + PR).
      - "Stop here" — leave everything as-is (worktree, branches, PR all intact).

  EXECUTE the choice:
    SQUASH-MERGE + CLEANUP:
      Pre-check mergeability:
        $ gh pr view <pr_number> --repo <github_slug> --json mergeStateStatus,mergeable
        IF not mergeable (branch protection needs an approval the agent can't give, or a
        required check is red): report exactly why; do NOT force. Offer to wait / stop /
        let the user merge. Do not proceed to delete anything.
      Merge (squash). The local branch is still checked out in the worktree, so do NOT pass
      `--delete-branch` here — it would try to delete a checked-out branch. Delete branches
      explicitly afterward, and set an explicit subject/body so the squash commit is clean
      rather than a concatenation of every babysit fix commit:
        $ gh pr merge <pr_number> --repo <github_slug> --squash \
            --subject "<type>(<scope>): <title> (#<issue_number>)" \
            --body "<one-line summary of what shipped>"
        (The issue closes via the PR body's `Closes #<issue_number>` — don't repeat the
        keyword in the squash body.)
      Cleanup, in this EXACT order:
        $ cd <repo_path>
        Guard (submodule worktree commits are local-only — losing them is unrecoverable):
        $ git -C <worktree_path> log @{upstream}..HEAD --oneline
        IF non-empty → STOP: "Unpushed commits in the worktree — refusing to remove. Push first." (do NOT --force)
        $ git worktree remove <worktree_path> --force   # --force: babysit/formatters may have left the tree dirty
        $ git branch -D <branch_name>                    # now safe — no longer checked out
        $ git push origin --delete <branch_name> || true # GitHub may auto-delete the head branch on merge; ignore "already gone"

    CLEANUP ONLY (PR stays open):
      $ cd <repo_path>
      $ git -C <worktree_path> log @{upstream}..HEAD --oneline   # must be empty — these commits live only here
      IF non-empty → STOP: "Unpushed commits in the worktree — push before cleanup." (do NOT --force)
      $ git worktree remove <worktree_path> --force
      $ git branch -D <branch_name>
      Keep the remote branch + PR. (The local branch is safe to delete — it's pushed.)

    STOP HERE:
      Leave the worktree, local + remote branches, and PR untouched.
      Remind the user where the worktree is so they can resume.
      (Still fall through to the Phase 14 final report.)

EMIT [ship] finished disposition=<merge+cleanup|cleanup|stop>

# ── Phase 14: Final Report ────────────────────────────────────────

STEP 16: Print the final block
  ```
  ── Ship Complete ───────────────────────────────────────────────
  Repo:    <repo> (<github_slug>)
  Issue:   #<issue_number> (<category>) — <title>   <issue_url>
  Branch:  <branch_name>   [removed | kept]
  PR:      <pr_url>   [merged (squash) | open]
  Disposition: <merge+cleanup | cleanup | stop>

  What shipped: <1-2 sentence summary>
  Babysit: <R> round(s); fixed <N> related review/CI items.
  Reported (not fixed — out of scope):
    - <unrelated item> — <why>
  Deferred follow-ups:
    - <finding> — <reason>
  Worktree: <removed | kept at <worktree_path>>
  ────────────────────────────────────────────────────────────────
  ```

EMIT [ship] complete number=<issue_number> pr=<pr_url> disposition=<…>

# ── Procedures ────────────────────────────────────────────────────

PROCEDURE resolve_repo_profile(repo)   # STEP 0b is authoritative; this is the summary
  - repo_path = <skaile-dev-root>/<repo>; validate it is a checked-out submodule
  - github_slug = parse `git -C <repo_path> remote get-url origin`
  - default_branch = `gh repo view <github_slug> --json defaultBranchRef -q .defaultBranchRef.name` (fallback: origin/HEAD; final fallback main)
  - formatter / lint_cmd / test_cmd / format_cmd = from <repo_path>/package.json scripts
    + <repo_path>/CLAUDE.md + root CLAUDE.md Formatting/Testing tables (NEVER hardcode)
  - changeset_required = .changeset/ + changeset-check workflow present
  - pr_template = .github/PULL_REQUEST_TEMPLATE.md if present
  - Honor: platform never Biome; marketing workspace-isolated; content repos may have no lint/test

PROCEDURE refine_description(raw, category)
  - ≤3 sentences for bug/issue/ui/chore; ≤5 short for feature; ~280/~500 char caps
  - Lead with the observable surface; observed vs expected (or new behavior / motivation)
  - Preserve user constraints verbatim, bold ones needing human follow-up
  - No emojis, no hedging, no "the user reports that"
  - Return refined_text + a ≤72-char issue title

PROCEDURE category_label(category)
  - bug→`bug`, issue→`issue`, ui→`ui`, chore→`chore`, feature→`enhancement`; always add `agent`
  - Verify each exists via `gh label list`; if missing, file without it and note it
  - Repos commonly have `bug`/`enhancement` but not `issue`/`ui`/`chore`/`agent` — those are best-effort

PROCEDURE triage_finding(finding)
  - important + in-diff + ≤20 LOC → apply
  - important + in-diff + >20 LOC → apply-large
  - nit → defer (pre-PR) / fix (during babysit — the bar is "clean")
  - preexisting/unrelated → report, don't fix
  - disputed → reject (reason in PR)

PROCEDURE classify_babysit_item(item)
  - RELATED iff it concerns this change's diff, or a check our change caused/exposed to
    fail, or a missing artifact our change needs (e.g. changeset). Fix these — incl. nits.
  - Tie-breaker for ambiguous CI failures: if the check is GREEN on origin/<default_branch>
    but RED on the branch, it is RELATED regardless of which file fails (our change exposed
    it) — fix it. Only checks ALSO red on main are UNRELATED.
  - UNRELATED otherwise (failure also present on main, flaky infra, broken CI architecture,
    comment on untouched code, unrelated tech debt). Report; never fix here.

CHECKLIST
  - [ ] `gh auth status` verified
  - [ ] Target repo resolved (input / inferred / asked) and validated; profile derived (slug, branch, formatter, lint/test, changeset)
  - [ ] Category auto-classified; mapped to an existing repo label (or filed unlabeled, noted)
  - [ ] Description refined WITHOUT asking; ≤72-char title derived
  - [ ] Clean-tree pre-flight done BEFORE opening the issue
  - [ ] GitHub issue opened/reused on the target repo (category label + `agent`, assigned @me); number + URL captured
  - [ ] Worktree at <skaile-dev-root>/.worktrees/<repo>-<number> (gitignored, inside the tree), branched off origin/<default_branch>
  - [ ] Branch name uses the issue number; nothing written to any legacy issue folder
  - [ ] Plan written in worktree, never staged
  - [ ] Implementation dispatched with self-contained MVC prompt (repo conventions, repo formatter, repo tests)
  - [ ] Review dispatched (or explicitly skipped for trivial work)
  - [ ] Triage silent unless apply-large or rejecting an important finding
  - [ ] Plan deleted before commit; repo lint + affected tests pass; changeset added if required
  - [ ] Commit type matches category; title references `#<number>`; branch pushed
  - [ ] origin/<default_branch> merged into the branch; conflicts resolved; re-pushed
  - [ ] PR opened (Closes #<number>; PR template honored); implementation summary reported
  - [ ] Babysit loop run: CI green / bots finished / related items (incl. nits) fixed; unrelated items reported, not fixed; loop converged
  - [ ] Final disposition asked (merge+cleanup / cleanup / stop) and executed; squash used for merge
  - [ ] Cleanup (when chosen) removed worktree + local branch (+ remote branch on merge); worktree/branches kept on "stop"
  - [ ] Final report printed

---

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Hardcoding platform's toolchain (`backend/`/`frontend/`, Prettier, its slug) onto another repo | Derive the profile from the target repo: slug from its remote, formatter + lint/test from its package.json + CLAUDE.md. Platform≠store≠workspaces. |
| Running Biome inside platform/ | Platform uses Prettier + ESLint. Use the repo's own `lint`/`format` scripts. |
| Writing to `platform/issues/` or any legacy markdown issue folder | Tracking is GitHub Issues. `gh issue create` on the target repo; identity is the native number; category is a label. |
| Cleaning up the worktree right after opening the PR | The worktree + branch must live through babysitting — they're where fix commits come from. Tear down only in Phase 13 per the user's choice. |
| Blocking forever waiting for a human review | Wait for automated bots to finish (they're CI checks); do NOT wait for absent humans. Exit when CI is green and posted feedback is resolved. |
| Only fixing "must-fix" review items | The bar is a clean PR. Fix related nits too. The only items you leave are ones the reviewer explicitly blesses as fine to keep. |
| Fixing unrelated/pre-existing/architectural problems during babysitting | Report them; don't fix. Scope is this change. (Fix our lint; don't rebuild a broken CI pipeline.) |
| Merging with a merge commit | Use squash-and-merge (`gh pr merge --squash`). |
| Deleting a branch that's checked out in the worktree | Order: squash-merge → `git worktree remove --force` → `git branch -D` (local) → `git push origin --delete` (remote). Do NOT pass `--delete-branch` to `gh pr merge` while the branch is still checked out in the worktree. |
| Force-merging past branch protection | If the PR isn't mergeable (needs an approval / a red required check), report exactly why and stop — don't force. |
| Opening the issue before the clean-tree pre-flight | A dirty tree would STOP with a dangling issue. Pre-flight first, then open the issue. |
| Asking the user to confirm description / category / repo (when inferable) / branch / plan | Decide silently. The only planned checkpoint is the final merge/cleanup/stop choice. |
| Passing parent conversation to the implementer/reviewer | Build self-contained MVC prompts. |
| Putting `Closes #<n>` in both the commit and the PR body | Keep the auto-close keyword in the PR body only; reference `#<n>` (no keyword) in the commit title. |

## Integration

- **Called by:** the `skaile-development` agent when the user reports a bug, requests a feature, asks for a UI fix, files a chore, or raises an issue in any skaile-dev repo
- **Calls (via Agent tool):** `implement` (or `general-purpose`) for the change; `review` for the diff review
- **Uses:** `gh` CLI for issue + PR + CI/review state + merge; `git` directly for repo/worktree/branch/commit/push
- **Reads:** the target repo's `CLAUDE.md` + `package.json`, the root `skaile-dev/CLAUDE.md` Formatting/Testing tables, affected source, `gh label/issue/pr` state
- **Writes:** a GitHub issue + a PR on the target repo, implementation + babysit commits on the branch, a transient plan file (deleted); on merge, a squashed commit on the repo's main
- **Never writes:** any repo's legacy markdown issue folder — tracking is GitHub Issues
