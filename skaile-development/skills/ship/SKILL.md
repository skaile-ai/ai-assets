---
name: "ship"
description: >-
  Take one piece of work in one repository from "somebody described it" to a traceable,
  reviewed, checks-green change whose fate the human decided. Use whenever someone wants
  something in a repository actually changed rather than explained - however casually they
  put it. "can you sort it", "can you take it", "can we get X on this", "have a look at #12"
  and "fix it and push it" are all this skill; the person does not have to mention branches,
  a pull request, review or merging, and the change can be as small as one constant, one
  string of copy or one dependency bump. Also use when handed an issue number or link to
  build, or an already-open pull request from this kind of work to carry the rest of the way.
  It runs the whole cycle in one flow - files the tracker issue, branches in an isolated
  worktree, implements, gets the diff reviewed, opens the PR, drives CI and review feedback
  to green - then recaps in plain language and asks once whether to squash-merge and clean up.
  Do not use for questions, diagnosis or code explanation, for reviewing someone else's
  existing code, for plans or design proposals, for filing an issue when implementation is
  explicitly deferred, for throwaway local experiments, or for work spanning several
  repositories.
version: 1.5.0
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
| 12 | **Babysit the PR**: act on whichever lands first — a review or CI — instead of waiting out the suite; drive CI to green and fix every related change-request in a loop (nits for the first three fix pushes, substantive only from the fourth); report unrelated/architectural problems without fixing them |
| 12b | Sweep for follow-ups: ship the small leftovers into this PR; propose as issues only the ones that clear the **issue bar** (user harm / money / stability / security / team drag / significant benefit, with evidence), at most 3 |
| 13 | **Recap in plain language** (no jargon — for a person returning after hours away), then ask the user: **squash-merge + clean up** \| **clean up only** \| **stop here** (plus **reading diff first** via the `meat` skill, if installed) — then execute the choice, and file the follow-ups the user picked |
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
8. **The babysit loop cannot converge** — CI stays red on something unrelated to the change, the same review item recurs after a good-faith fix, or ~15 min pass with no signal at all (no review, no check reaching a conclusion); report and ask.
9. **The final disposition** — merge + cleanup / cleanup only / stop here (Phase 13), asked together with which proposed follow-ups to file. This is the ONE planned interactive checkpoint at the end; both questions go in a single AskUserQuestion call.

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
  gh pr view --json headRefOid,reviews,statusCheckRollup,mergeStateStatus
                                                  — the babysit poll's head SHA, reviews and check rollup (Phase 12)
  gh api repos/<owner>/<repo>/pulls/<n>/comments  — inline review comments WITH `original_commit_id` (the poll's staleness key)
  gh api repos/<owner>/<repo>/issues/<n>/comments — top-level review comments WITH `updated_at` (no other surface exposes it)
  gh api user                                     — own login, to discard self-authored signals (403s under an App token; see Phase 12)

WRITES
  GitHub issue on <repo's slug>                   — opened up front (category label if it exists + `agent`, assigned @me); reused if `issue` is passed
  <worktree>/<plan-file>.md                       — uncommitted plan (deleted before the final commit)
  <worktree>/<target-source-files>                — the implementation + babysit fixes
  PR on <repo's slug>                              — opened after implementation; body contains `Closes #<number>`
  commits on the feature branch                   — implementation, main-sync merge, and babysit fix commits
  replies on PR review threads                    — one per resolved or declined item during babysitting
  the PR body (`gh pr edit --body-file`)          — refreshed in STEP 14c to match what actually shipped
  follow-up GitHub issues on <repo's slug>        — only the ones the user selects at the Phase 13 gate

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
MUST  babysit the PR (Phase 12): poll for the FIRST signal — a review or CI completion, whichever lands first — and act on a review the moment it arrives instead of waiting out the whole check suite; fix every actionable item that relates to the work item, looping push → re-poll until CI is green and the only remaining review notes are ones the reviewer explicitly blesses as fine to keep
MUST  apply the nit cutoff, counted in FIX PUSHES not loop iterations: fix related nits for the first three fix pushes, but from the fourth on fix ONLY substantive items (failing required check, correctness/security/data-loss/performance defect, explicit blocking change-request, public-API/contract/migration problem) and decline the rest — comment and doc-wording nits above all
MUST  run the Phase 12b follow-up sweep BEFORE the disposition gate: ship small leftovers (<15 min, inside this diff, no design decision) into THIS PR, and propose as issues only follow-ups that clear the issue bar (a YES with evidence on one of the six impact questions), at most 3, filing only the ones the user selects
MUST  fix only items RELATED to the change (e.g. lint/type/test failures the change caused, review nits on the diff); for unrelated/pre-existing/architectural problems, REPORT them to the user and do NOT fix them
MUST  converge the babysit loop — cap fix rounds, and if CI stays red on something unrelated or a review item recurs after a good-faith fix, stop and ask (gate #8)
MUST  print the plain-language recap (Phase 13, STEP 14d) immediately BEFORE the final question — jargon-free, no paths or symbols, written for someone who was not watching
MUST  ask the user at the end (Phase 13) to choose: squash-merge + cleanup | cleanup only | stop here — and execute exactly that
MUST  after a MERGE of user-visible platform work, sync the capability docs (Phase 13b) — `platform/features/SKAILE-PLATFORM-CAPABILITIES.md` is the LEADING copy and is always updated; the business doc mirror and the platform-guide skill only when their paths are accessible on this machine
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
      SKIP STEP 4..STEP 13 and JUMP to Phase 12 (STEP 14). Everything from there runs
      normally: Phase 12b (follow-up sweep), Phase 13 (STEP 14c body refresh, STEP 14d
      recap, STEP 15 disposition), Phase 13b (capability-docs sync) and Phase 14.
      Note for STEP 14c: `fix_rounds` starts at 0 on this path, but the body was written
      by a PREVIOUS session, so it still needs the refresh — that is the case with the
      widest gap between what the body says and what the diff does.
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
  EXPLICITLY blesses, or nits declined past the cutoff. Read each review the MOMENT it
  lands — never hold it until the check suite finishes. Wait for automated review bots
  to report; do NOT block on absent human reviewers.

  babysit_round = 0; MAX_ROUNDS = 10; cumulative budget ≈ 45 min wall-clock
  fix_rounds = 0   # incremented ONLY in (c), and only when a commit is actually PUSHED
  Keep the two counters separate. With the early break in (a) an iteration is no longer
  one CI cycle — it can end on a review arriving mid-run, or on an already-terminal
  rollup with nothing to do — so counting the nit cutoff in iterations would start
  declining nits after one or two pushes. `babysit_round`/MAX_ROUNDS is the runaway
  guard only; `fix_rounds` is the cost counter the cutoff uses. MAX_ROUNDS is 10 rather
  than 6 because iterations are now cheaper and more numerous.
  LOOP:
    babysit_round += 1
    IF babysit_round > MAX_ROUNDS OR cumulative babysit time > ~45 min
       → STOP (gate #8): report current state, ask how to proceed.

    (a) Poll for the FIRST actionable signal — a review OR CI completion, whichever
        lands first. Do NOT sit on the whole check suite before you read the reviews.
        A review that arrives while tests are still running is actionable NOW, and
        pushing its fix supersedes the in-flight run anyway, so reading it early
        SHORTENS the turnaround instead of wasting a cycle.
        head_sha = the PR's `headRefOid` — NOT the local worktree HEAD. Reviews are
        attached to what GitHub actually has, and the two diverge whenever a push did not
        land; filtering live reviews against a local SHA discards all of them until the
        15-min cap. Read it in FULL; never poll against a truncated or remembered SHA
        (a padded SHA matches on poll 1).
        $ gh pr view <pr_number> --repo <github_slug> --json headRefOid -q .headRefOid

        Poll every ~20s (wall-clock cap ~15 min with no signal at all → gate #8). THREE
        sources, because no single one carries every signal WITH its commit:
        $ gh pr view <pr_number> --repo <github_slug> \
            --json headRefOid,reviews,reviewDecision,mergeStateStatus,statusCheckRollup
        $ gh api repos/<owner>/<repo>/pulls/<pr_number>/comments    # inline review comments
        $ gh api repos/<owner>/<repo>/issues/<pr_number>/comments   # top-level comments
        There is NO `reviewThreads` field on `gh pr view --json`. Asking for one makes the
        WHOLE call exit non-zero, so the loop gets no payload at all — and since this poll
        is now the only data source, that failure is silent and total. Thread RESOLUTION
        state needs `gh api graphql`; you rarely need it here.

        SHA-scope every signal and DISCARD anything tied to an older commit — it is stale
        from an earlier round and you already handled it:
          - `reviews[]` → `.commit.oid`
          - `pulls/<n>/comments[]` → judge staleness on `.original_commit_id`, NEVER
            `.commit_id`. GitHub re-anchors `commit_id` FORWARD to the newest commit a
            comment still applies to, so a comment from round N can read the current head;
            `original_commit_id` is the commit it was written against and never moves.
            The re-anchoring is per-comment (it follows whether that line survived the
            new diff), so it is not predictable from here — two siblings from one review
            can end up on different commits.
          - `issues/<n>/comments[]` → carries NO commit. Scope it by time instead: ignore
            any top-level comment whose `updated_at` predates the push of head_sha.

        DISCARD every review, comment and reply authored by YOU. Your own reply is not a
        signal — it is an echo of one you already consumed.
        PRIMARY rule: track the id of every comment and reply YOU post — step (c) posts
        them, so it has the ids from `gh`'s own output — and discard those. Identity by
        construction, no lookup, works under any token.
        $ me=$(gh api user -q .login)   # cross-check only; may fail, see below
        Use that only as a cross-check, and do NOT assume it is the PR author: `ship`
        pushes and replies as whatever `gh` is authenticated as, which on a PR you did not
        open is someone else — a rule written against the PR author inverts, discarding
        the genuine reviewer while keeping your own echoes. Under a GitHub App
        installation token the call 403s ("Resource not accessible by integration"),
        because such a token has no authenticated user; `me` is then EMPTY. An empty `me`
        means "fall back to the id list" — never "no comments are mine", which would
        silently disable the only guard against breaking on your own replies, in exactly
        the bot-token setup where this loop runs unattended.
        Step (c) posts its thread replies AFTER pushing, and they reach the current head
        by two different routes:
          - `issues/<n>/comments` — a `gh pr comment` decline carries no commit and is
            scoped by `updated_at` > push, so it always lands in scope. This is the main
            one, and it fires on EVERY declined nit past the cutoff, i.e. exactly where
            the budget is tightest.
          - `pulls/<n>/comments` — an inline reply inherits its PARENT's `commit_id`, not
            the head (verified: 9/9 replies on this skill's own PR matched their parent).
            So a reply to an older thread carries the older commit and the SHA scope
            discards it anyway — but if the re-anchoring above moved the parent onto the
            new head, the reply comes with it.
        Without this rule the break fires on them, (b) finds nothing, and a round is
        burned. SHA-scoping does not catch it: that guard is keyed on identity-of-signal,
        not authorship.

        IGNORE progress placeholders. A review bot configured with `track_progress` (this
        repo's is) posts ONE top-level comment when the run STARTS and then EDITS THAT SAME
        COMMENT IN PLACE with the finished review minutes later. Two consequences:
          - a Bot comment is a SPINNER while it ANNOUNCES WORK IN PROGRESS: an unchecked
            `- [ ]` box, or in-progress wording like "Claude Code is working… I'll analyze
            this and get back to you" (the placeholder for a reply-triggered run, which
            carries no checklist at all, so a test hinging on the checkbox shape misses
            it). Never break on one: (b) would find nothing and burn a round on the bot's
            own progress bar.
            Do NOT define a spinner as "no findings". A FINISHED review with nothing to
            report — the clean pass — also has no findings section, and it is how this
            loop is SUPPOSED to end: classing it as a spinner leaves (e)'s "bots have
            completed" permanently unsatisfied and runs to MAX_ROUNDS. Finished and empty
            is a completed review; announcing work is a spinner.
            Apply the spinner test ONLY to `user.type == "Bot"` (e.g. `claude[bot]`). A
            human writing "looks good, but `- [ ] worth a test later`" must still count as
            a review, or it never breaks the poll for as long as that box stays unchecked.
          - the comment id is NOT a usable consumed-key, because the real review arrives
            under the SAME id — marking the placeholder consumed would silently skip the
            round-1 review. Key on (id, hash of the body), per the consumed-set rule
            below: the spinner body differs from the review that replaces it, so the hash
            tells them apart. Read them from the REST issue-comments endpoint;
            `gh pr view --json comments` exposes neither a body-stable edit signal nor a
            commit, which is the one reason that endpoint is used here.

        BREAK out of the poll as soon as EITHER:
          - a review bot or a human has posted a review, an inline comment, or a FINISHED
            top-level comment for head_sha that you have NOT yet consumed → go straight to
            (b) WITH CHECKS STILL RUNNING; do not wait for them, OR
          - every check in statusCheckRollup has a terminal conclusion AND the rollup has
            been non-empty, all-terminal and UNCHANGED IN SIZE for ≥2 consecutive polls
            AND ≥60s have passed since the push, or
          - ci_state = "no-checks".
        The stability clause is not belt-and-braces: `statusCheckRollup` is a GROWING set,
        not a fixed one. A re-triggered workflow is absent from it until it registers, so
        for a few seconds after a push every check already listed reads terminal and the
        break fires on a suite that is not done. This bit the very loop that wrote this
        rule: 16/16 terminal, break taken, and `claude-review` appeared as IN_PROGRESS
        moments later. Cross-check: an OPEN Bot spinner comment means a review is in
        flight no matter what the rollup says — never conclude "bots have completed" past
        one. BOUND IT by `created_at`: only a spinner created AFTER the push of head_sha
        counts as in flight. An earlier one never is, whichever way the repo is wired —
        and do NOT assume which, because it differs per repo:
          - IF the review workflow cancels superseded runs (a per-PR concurrency group
            with `cancel-in-progress: true`), the run is dead and its progress comment
            sits at unchecked boxes FOREVER. Check before relying on either branch: a
            workflow with no `concurrency:` block does NOT cancel on push.
          - IF it does NOT cancel, the superseded run keeps going and will post — but
            against the OLD head, which the staleness scope above discards anyway.
        Either way an earlier spinner must not count as in flight. Treating one as such
        makes (e)'s "bots have completed" permanently false from the second fix push on —
        the same stall as the spinner bug above, re-entered through the cross-check that
        fixed it. Use `created_at`, NOT `updated_at`: the metadata bumps documented below
        can carry an old spinner past a time filter keyed on the latter.

        Set ci_state = "no-checks" ONLY after the rollup has come back EMPTY on ≥3
        consecutive polls AND ≥60s have passed since the push. An empty
        `statusCheckRollup` is ALSO what a CI-having repo returns for the first ~10-30s
        while workflows queue — concluding "no-checks" there exits the loop and offers a
        squash-merge on a PR whose suite simply had not started yet. A genuinely CI-less
        repo (content repos like ai-assets/infra, workspace-isolated marketing) stays
        empty and trips the condition a minute later at no cost.

        Mark each consumed signal consumed GLOBALLY, keyed on (id, hash of the BODY) —
        NOT per head_sha, and NOT on `updated_at`. GitHub bumps `updated_at` for thread
        metadata alone — replies, resolution, round bookkeeping — including bumps this
        loop causes itself: on this skill's own PR nine bot comments bumped in two tight
        batches (five inside 3 seconds), one of them a comment whose `commit_id` never
        moved, so a push is not the trigger. A body hash is immune to that and still
        solves the case the timestamp was chosen for, since a spinner body genuinely
        differs from the review that later replaces it under the same id.
        `reviews[]` is keyed on `id` ALONE: it carries no edit timestamp at all (the
        fields are `author, authorAssociation, body, commit, id, includesCreatedEdit,
        reactionGroups, state, submittedAt`) and its `commit.oid` never moves. Do NOT
        reach for `updatedAt` to fill the gap — that IS a valid `--json` field, so the
        call succeeds, but it is the PR's last-activity time and changes on every event
        on the PR, which would un-consume every review on almost every poll.
        The consumed-set is the PRIMARY guard; the commit scoping above is a secondary
        filter. When the two disagree, trust the consumed-set.
        And GLOBAL, because a push must not un-consume anything: the signal did not
        change, only the head did. Per-head keying looks equivalent and is not, because of
        the re-anchoring above — an old comment that moves onto the new head arrives
        unconsumed, passes the SHA scope, and is not caught by the authorship rule either
        (its author is the reviewer). It then lands in (c)'s fingerprint check as a
        REPEAT, which by that block's own rule means the fix did not satisfy the reviewer
        → gate #8. So the cost is not a burned round; it is the loop stopping and
        reporting a recurring item that was in fact accepted.
        An already-consumed signal must NOT re-trigger the early break — otherwise (b)
        finds nothing to do and the loop spins on its own signal.
        `gh pr checks --watch` is the WRONG tool in this step: it blocks until the whole
        suite finishes, which is exactly the latency this removes. Note also that it
        exits 0 while checks are still pending. A `cancelled` check left behind by a
        superseded push is normal — it is not a failure and never an actionable item.

    (b) Collect actionable items:
        - FAILED required checks (lint, typecheck, tests, changeset-check, build).
        - Review change-requests + inline review comments (bot e.g. claude-code-review,
          and any already-posted human reviews). Include STYLE NITS.
        SKIP anything whose fingerprint is already in the `declined-set` — it was decided,
        not left open, and re-collecting it is what makes an all-declined round loop.
        EXCEPTION: if the item comes back as an EXPLICIT blocking change-request, or is
        now claimed to be a correctness / security / data-loss defect, do NOT skip it —
        remove it from the `declined-set` and escalate to gate #8 with your decline reason
        and the reviewer's objection. A decline is YOUR severity call; a reviewer
        contesting that call is new information, not a repeat. Without this the skip runs
        before classification and before the severity filter, so the one item the cutoff
        says is never declinable becomes unhearable once you have declined it once, and
        (e) then counts it as addressed. "Do NOT re-litigate" binds you, not the reviewer.
        For each item classify (PROCEDURE classify_babysit_item):
          RELATED  → caused/exposed by this change (our lint/type/test failure, a nit on a
                     line we touched, a missing changeset, a bot suggestion on our code, OR
                     a check that is green on origin/<default_branch> but red on the branch)
          UNRELATED→ failure that is ALSO red on main, flaky infra, broken CI architecture,
                     tech debt elsewhere, a comment about code we didn't touch

    (c) IF there are RELATED actionable items:
        CHURN CHECK first, before severity. Count how many of this round's items fault
        lines ADDED BY THE IMMEDIATELY PRECEDING COMMIT (`git diff HEAD~1 HEAD`) rather
        than by the original change. IF three consecutive rounds are mostly fixes-of-fixes,
        STOP and escalate to gate #8: report the chain (each fix and the defect it
        introduced) and offer to restructure, simplify, or ship as-is.
        This is a DIFFERENT axis from the nit cutoff below and neither substitutes for the
        other. The cutoff asks "is this item a nit?"; churn asks "is this a defect in the
        fix I wrote ninety seconds ago?". A section can churn while every single finding is
        substantive — and then the cutoff correctly never fires and nothing stops the loop.
        Churn means the DESIGN is wrong, not the wording: each new guard is adding a new
        edge, and ten more rounds of patching will not converge. Restructure instead.

        For each, compute a fingerprint = <file>:<line-or-near> + <rule / short text>.
        IF a fingerprint MATCHES one already in the seen-set (you fixed it in an earlier
        round and it came back), the fix didn't satisfy the reviewer or the bot disagrees
        with it → do NOT re-fix blindly; escalate to gate #8 (report the recurring item +
        your reasoning, ask how to proceed) rather than burning rounds.

        Severity filter — NIT_CUTOFF_ROUND = 3, counted in `fix_rounds` (pushes), NOT
        in `babysit_round` (loop iterations):
          While fix_rounds < 3 — the first three fix pushes: fix every RELATED item,
            nits included. The bar is "clean," not "only must-fixes." The only items you
            leave are ones the reviewer explicitly marked optional / fine-to-keep.
          Once fix_rounds ≥ 3 — the fourth fix push onward: fix ONLY substantive items. An item is substantive iff it
            is one of:
              - a failing required check (lint, typecheck, test, changeset, build),
              - a correctness, security, data-loss, or performance defect,
              - an EXPLICIT blocking change-request from a reviewer,
              - a public-API, contract, or migration problem.
            Everything else is a nit and is DECLINED from the fourth fix push on — in particular
            comment-wording and doc-polish nits, naming preferences, formatting taste,
            optional "consider …" refactors, and re-phrasings of text that is already
            correct. Comment nits are the clearest case: decline them.
            Do NOT argue the merit and do NOT re-litigate. Record each as
            `declined (nit, fix push <fix_rounds + 1>)` for the final report — `fix_rounds`
            is not incremented until the push at the end of (c), so the bare counter is
            one behind here. If this round declines everything and therefore pushes
            nothing, record `declined (nit, no push)` instead: there is no fix push to
            number. Reply once on the open thread with that reason, and move on.
          Rationale: past the third fix push each nit costs a full CI cycle and changes
          nothing a reader would notice. Nit loops are the known way this skill stalls.

        Record every DECLINED item's fingerprint in a `declined-set`. A decline is a
        decision, not an open item, and without this it has no terminal state: the
        seen-set holds only FIXED fingerprints, so a declined nit is not a REPEAT, (b)
        re-collects it from every poll, the severity filter declines it again, and the
        round ends here — costing a poll and producing nothing until MAX_ROUNDS trips
        gate #8, i.e. asking the user how to proceed on a PR the skill has already decided
        is done. That is this change's own stall, moved from nine nit fixes to ten empty
        polls. (b) SKIPS anything already in the `declined-set`, and (e) treats those
        items as settled.

        IF NOTHING survived the severity filter — every item was declined, which past
        the cutoff is the COMMON round — skip the lint/commit/push block entirely: record
        the declines, reply once on each thread, do NOT increment `fix_rounds`, and FALL
        THROUGH to (d) and (e). Do NOT jump back to (a) — (c)'s `CONTINUE loop` below is
        correct only because that path PUSHED and has new CI to wait for. Here nothing
        was pushed: no new check run is coming, the rollup is already all-terminal from
        the previous iteration, every remaining signal is in the consumed-set or the
        declined-set, and the decline replies just posted are discarded as self-authored.
        So (a) has nothing left that can break it and blocks to its ~15 min cap → gate #8,
        while (e) — which counts declined items as addressed and would exit cleanly — is
        never reached. Running the push block instead means `git commit` on an empty
        index, which fails, and the label below then promises a fix push that never
        happened.
        Fix the surviving items in the worktree (small fixes inline; if one is
        apply-large, gate #5).
        IF a fix RENAMED anything or CHANGED A STATED RULE — a placeholder, a symbol, a
        step id, a key, a threshold — grep the changed files for the OLD form before
        committing, and fix every consumer you find.
        $ grep -rn "<old form>" <changed paths>
        A producer updated without its consumer is the cheapest and most common review
        finding there is, it costs a whole CI cycle to hear about, and it is strictly worse
        than the ambiguity it replaced: two things that merely read inconsistently now read
        wrongly. Contradictory instructions are a CORRECTNESS defect, not wording — a
        reader has no way to tell which of two stated rules is current.
        $ cd <worktree_path> && <lint_cmd> && <test_cmd>
        $ git add -A && git commit -m "fix(<scope>): address review feedback (#<issue_number>)"
        $ git push origin <branch_name>
        fix_rounds += 1  (only here, only on an actual push)
        Add each fixed item's fingerprint to the seen-set; reply briefly on resolved review
        threads and mark them resolved where possible.
        CONTINUE loop. The new commit supersedes the running checks and re-triggers CI +
        bots — that cancellation is intended, not a failure.

    (d) IF there are UNRELATED items:
        Do NOT fix them. Record each (what, where, why out of scope) for the final report.
        IF an unrelated failure BLOCKS merge (e.g. a required check is red for reasons
        not caused by us): note it; it will surface in Phase 13 as a merge blocker.

    (e) EXIT the loop when ALL hold:
        - every required check is green, OR ci_state = "no-checks", OR the only red checks
          are unrelated + recorded,
        - automated review bots have completed for the latest commit (or there are none),
        - no unaddressed RELATED change-requests/comments remain — reviewer-blessed
          leftovers and items in the `declined-set` COUNT AS ADDRESSED. Declining past the
          cutoff is an answer; if this clause required them fixed, the cutoff could never
          let the loop exit.
        Do NOT wait for a human who hasn't reviewed.

  Do NOT print "Babysit done" here and do NOT emit `babysit_done` yet — Phase 12b may push once more and re-enter this loop,
  and emitting here would understate `rounds=`.

# ── Phase 12b: Follow-up Sweep (before the merge gate) ────────────

STEP 14b: Decide what ships in THIS PR and what becomes a follow-up issue
  Run this BEFORE the disposition question. A follow-up proposed after the merge is a
  follow-up nobody files, and the PR body is not a tracker.

  Gather every loose end you are holding: review findings deferred in Phase 8, UNRELATED
  items recorded in Phase 12(d), nits declined at the cutoff, and anything you noticed
  while implementing but did not do.

  Split each one. The test is cost and blast radius, NOT how loudly it was reported:
    SHIP IT NOW → it touches files already in this diff, needs no design decision, and
      is roughly <15 min of work. Fix it in the worktree and push. Small things belong in
      THIS PR — never open an issue for work that is cheaper to do than to file.
      Nits DECLINED at the cutoff do not return through this door: they were declined on
      merit, not on size.
    FOLLOW-UP ISSUE → it cannot ride along AND it clears the ISSUE BAR: at least one
      of these six answers YES, and you can cite the evidence in one line —
        1 User harm   a real user gets a wrong result, loses work or data, is blocked,
                      or has to work around it (report, prod log, repro, or a path
                      normal use reaches)
        2 Money       an estimable cost (infra, tokens, paid API calls, support hours)
                      or held-up revenue
        3 Stability   a crash, hang, wedged session, corruption or broken deploy that
                      has happened or that normal operation triggers
        4 Security    an exploitable path NOW, with the controls it gets past
        5 Team drag   a red or flaky required check, or a trap that ALREADY cost a
                      real run
        6 Benefit     a significant gain users or the team would notice — measurably
                      faster on a path people hit, a clearly better experience on a
                      used flow, a simplification that unblocks named planned work
      No evidence counts as NO. A rare trigger is YES only when the damage is severe
      (data loss, cross-tenant leak, credential exposure). Unsure on one question: keep
      it as a candidate and name the open question in its option line; the user decides
      at the gate.
    DROP → everything else, whatever its size. Defense in depth behind a control that
      holds, hypothetical edges, cleanup, refactors, renames, consistency, test gaps on
      code with no known bug, "log more", docs polish: these answer NO to all six by
      default, however they were phrased by the reviewer. Say nothing about them.
    (The full catalog, and how to use it to sweep an existing backlog, is the
    `issue-bar` skill. The six questions above are all this step needs.)

  Propose AT MOST 3 follow-ups. Each gets a one-line title and its YES as the why:
  "<question>: <evidence>". If nothing clears the bar, propose NONE and say so — an
  empty sweep is the normal outcome, and a padded list trains the user to ignore the
  good one.

  IF you pushed anything in this step, RE-ENTER Phase 12 so its exit conditions re-apply
  (the push restarts CI and re-triggers the review bot, which can produce fresh items).
  Three constraints, without which a chatty bot alternates the two phases indefinitely —
  in a skill whose whole point is convergence:
    - the re-entry SHARES `babysit_round`, `fix_rounds` and the ~45 min budget. 12b's push
      is a fix push like any other: it increments `fix_rounds` and does NOT reset the nit
      bar.
    - Phase 12b runs AT MOST ONCE. Coming back out of the re-entry, go straight to the
      Phase 13 gate — do not sweep again.
    - emit `babysit_done` only after the re-entry settles, so `rounds=` is accurate.

  Print:
  > "Babysit done after <R> round(s) / <F> fix push(es). CI: <green|blocked-by-unrelated>."
  > "Fixed <N> related item(s); <M> unrelated/reported."
  > "Follow-up sweep: shipped <S> small item(s) into this PR; proposing <P> follow-up(s); dropped <D>."

  This is the ONLY "Babysit done" line the user sees. Phase 12 deliberately prints none:
  it runs up to twice (12b re-enters it), and both of its passes would report an `<R>`
  that the re-entry then invalidates.

EMIT [ship] babysit_done rounds=<R> fixed=<N> unrelated=<M>
EMIT [ship] followup_sweep shipped=<S> proposed=<P>

# ── Phase 13: Final Disposition (the one planned checkpoint) ───────

STEP 14c: Refresh the PR description with what ACTUALLY shipped
  Run this after the babysit loop has settled and before the gate. The PR body was
  written in Phase 11, at open time, from the plan — and every babysit fix push since has
  changed what the PR does. By now it describes a proposal, not the change. It is also
  the last thing a human reads before merging, and on a squash-merge it is what the
  repo's history inherits.

  Re-read the final diff (`git diff origin/<default_branch>...HEAD`), then fetch the body
  AS IT STANDS NOW rather than recomposing it from memory — `--body-file` replaces the
  whole body, and after a loop that may have run ten rounds the memory of it is the least
  reliable thing in the session. The live body may also carry a human's edit from the
  babysit window or a PR-template section honored at open time.
    $ gh pr view <pr_number> --repo <github_slug> --json body -q .body > <tmp>
    IF that command fails or <tmp> comes back EMPTY, STOP — do NOT run the edit. The two
    commands are independent: a failed fetch prints nothing, the redirect leaves <tmp>
    truncated to empty, and `gh` ACCEPTS an empty `--body-file` and clears the body,
    `Closes #<n>` included. That is the loss this step exists to prevent, reached from
    the other side.
    # edit <tmp> in place, then CONFIRM the line survived YOUR OWN rewrite — the
    # empty-fetch guard above does not cover a mangled edit, and this step exists to
    # rewrite the body wholesale:
    $ grep -qF "Closes #<issue_number>" <tmp>
    IF it is absent, restore it before editing. Never push a body without it.
    $ gh pr edit <pr_number> --repo <github_slug> --body-file <tmp>
  Keep the original structure and keep `Closes #<issue_number>` EXACTLY as it was. Losing
  it fails SILENTLY in two places that both assume it is there: STEP 15 deliberately does
  not close the issue after merging because the body does it, so the issue simply stays
  open; and STEP 3b finds an existing PR with `--search "Closes #<n> in:body"`, so a
  mangled line makes a later `ship` on the same issue match zero PRs and open a SECOND
  branch and PR for work that already has one. Update
  anything the babysit rounds made false (version numbers, counts, "only X changes"
  claims, described behaviour that was later corrected), and ADD a short recap at the top:

    **What this does:** 1-2 sentences, plain language, describing the change as it now
    stands. Not a changelog of the fix commits, not a restatement of the issue — what a
    reader needs to know to understand the PR without reading the diff.

  This is NOT the same text as STEP 14d's recap and neither replaces the other. This one
  is WRITTEN, for whoever opens the PR later or reads the squashed commit in the history;
  14d's is PRINTED, for the person in this session who walked away. Same change, different
  readers: this may name the mechanism, 14d's may not.

  With zero fix pushes there is nothing to correct — but still add the recap if the body
  lacks one, and still refresh on the STEP 3b resume path, where `fix_rounds` starts at 0
  against a body a PREVIOUS session wrote. That body has the widest gap of all between
  text and diff, which is the case this step exists for.

STEP 14d: Plain-language recap (print BEFORE asking anything)
  Distinct from STEP 14c's written recap in the PR body: that one is for a future reader of
  the PR and may name mechanism; this one is spoken to the person here, now, and may not.
  The person may have walked away hours ago and come back to a wall of scrollback. This
  block is the one thing they read to remember what this was about. Write it for someone
  who does not know this codebase and was not watching.

  Hard rules for the recap text:
    - No jargon. Banned unless the person used the word first: refactor, regression,
      race condition, idempotent, nullable, migration, hydrate, memoize, endpoint,
      payload, mutation, invariant, coerce, upstream, downstream.
    - No file paths, no function names, no class names, no line numbers, no error
      strings, no commit hashes, no code. Those are already in the PR.
    - Describe the effect a person could notice, not the mechanism. "Customers saw a
      spelling mistake on the payment page" - not "corrected two i18n string literals".
    - Say what is different now that was not before, in one breath.
    - Plain past tense, short sentences, no bullets inside the three lines.
    - Never overstate: if the fix is partial, or something related is still broken, say
      so in "Still open". If nothing is left, write "Nothing.".

  Print:
  ```
  ── In plain words ──────────────────────────────────────────────
  You asked for:  <1 sentence, the original request in everyday language>
  What was wrong: <1-2 sentences, what was actually happening and who it affected.
                   If nothing was broken (a new feature or a chore), say what was
                   missing or awkward instead.>
  What I did:     <2-3 sentences, the change as a person would notice it, plus how it
                   was checked. Name the test or the manual check in everyday terms.>
  Still open:     <anything related that is NOT fixed by this, in one sentence - or
                   "Nothing.">
  ────────────────────────────────────────────────────────────────
  ```

  Worked example (a real one, for calibration):
  ```
  ── In plain words ──────────────────────────────────────────────
  You asked for:  The two spelling mistakes on the checkout screen to be fixed.
  What was wrong: Customers paying for an order saw "try agin later" and "reciept"
                  on screen. It had been live for several weeks.
  What I did:     Corrected both words so the screen now reads "try again later" and
                  "receipt". I ran the project's own checks and took a look at the
                  checkout screen to confirm nothing else moved.
  Still open:     Nothing.
  ────────────────────────────────────────────────────────────────
  ```

EMIT [ship] recap_printed

STEP 15: Ask the user how to finish (gate #9)
  Optional reading-diff preview: IF a `meat` skill is available in this session's
  skill list, offer it as a 4th option below. If absent, ask with the three
  disposition options only — never fail or warn about it.
  Present concise state, then ask (use the AskUserQuestion tool):
    Context line: "PR <pr_url> — CI <green|running|no-checks|blocked: …>, reviews <addressed|none yet|N blessed-nits>.
                   <if reviewDecision requires approval and none is present: 'Note: branch
                    protection needs a human approval — auto-merge will be blocked.'>
                   <if unrelated blockers: 'Note: <X> is red for unrelated reasons.'>"
    IF a Phase 12b push left checks mid-flight, either wait for them to go terminal first
    or say "CI running" in that context line. The merge pre-check below reads
    `mergeStateStatus,mergeable`, which catches a BLOCKED state but not a still-PENDING
    one — so without this the user can pick "Squash-merge" against an unfinished suite.
    Question: "How should I finish this PR?"
      - "Squash-merge + clean up" — squash-merge the PR, delete the remote branch, remove the worktree, delete the local branch.
      - "Clean up only" — leave the PR open; remove the worktree + delete the local branch (keep the remote branch + PR).
      - "Stop here" — leave everything as-is (worktree, branches, PR all intact).
      - [only if the meat skill is available] "Reading diff first" — distill the PR's
        diff into a reading diff before deciding.
  IF the Phase 12b sweep proposed follow-ups (P > 0), ask this as a SECOND question in
  the SAME AskUserQuestion call (multiSelect) so the user is interrupted ONCE, not twice:
    Question: "Which follow-ups should I file as issues?"
      - one option per proposed follow-up: "<title>" — <question>: <evidence>
  IF P = 0: ask the disposition question alone and print
    > "Follow-ups: none worth filing."

  IF the user picks "Reading diff first":
    Invoke the `meat` skill from inside <worktree_path> with
    range = origin/<default_branch>...HEAD, print its reading diff, then re-ask the
    question with the three disposition options only.

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

  THEN file the SELECTED follow-ups (only those the user picked — never the whole list):
    $ gh issue create --repo <github_slug> --title "<title>" \
        --body "<what is wrong, in 2-4 sentences. Then 'Issue bar: <question> — <evidence>' for each YES, so a later backlog sweep can check it. Refs #<issue_number> — <pr_url>>" \
        --label <category label if that label exists in the repo>
    Reference the origin issue and PR with a BARE `#<n>` or `Refs #<n>` only. NEVER write
    a closing keyword (`Closes`/`Fixes`/`Resolves`) in a follow-up body — GitHub fires it
    on merge and would close the work you are trying to track.
    Do NOT assign them and do NOT add the `agent` label: these are backlog, not claimed
    work, and an `agent`-labelled issue is picked up by other agents as ready to build.
    Capture the created numbers for the final report.

EMIT [ship] finished disposition=<merge+cleanup|cleanup|stop> followups=<list of #n>

# ── Phase 13b: Capability-Docs Sync (conditional) ─────────────────

STEP 15b: Sync the platform capability docs
  Run ONLY if BOTH hold:
    - disposition = merge+cleanup (the change is on main)
    - repo = platform AND the change adds/alters a USER-VISIBLE capability
      (new feature, new provider/connector/integration, new agent capability, new UI
      surface — NOT internal refactors, bug fixes without behavior change, CI/chores)
  Otherwise print > "Capability docs: skipped (<not merged | not platform | not user-visible>)." and continue.

  1. Platform capabilities doc — the LEADING copy, always available (it lives in the repo):
     `platform/features/SKAILE-PLATFORM-CAPABILITIES.md`
     — add or extend the matching section in its business/use-case voice (What it does /
     Capabilities / Business value; no code-level detail); bump the `updated:` frontmatter
     date. Also add/refresh the matching feature doc under `platform/features/<NN-section>/`.
  2. Business mirror (SharePoint-synced; edit = publish) — ONLY if accessible
     (`test -e <path>`; absent on most machines, skip with a one-line note):
     `/mnt/c/Users/peter/Skaile GmbH/Management - Documents/General/concept/SKAILE-PLATFORM-CAPABILITIES.md`
     — copy the updated leading doc there verbatim.
  3. Platform-guide skill — ONLY if the ai-assets checkout is accessible:
     `<skaile-dev-root>/ai-assets/skaile-platform/skills/platform-guide/`
     — update the matching `concepts/` or `ui/` detail file (and the SKILL.md index +
     keywords only if a new topic area appeared). Honor its hard rules: real UI labels in
     **bold**, never enumerate live `platform.*` capabilities from memory. If the
     ai-assets checkout is clean, commit there with a one-line message; else leave the
     edit uncommitted and note it.

  Print: > "Capability docs: <updated all | updated <X>, skipped <Y> (not accessible)>."

EMIT [ship] capability_docs_synced targets=<N>

# ── Phase 14: Final Report ────────────────────────────────────────

STEP 16: Print the final block
  ```
  ── Ship Complete ───────────────────────────────────────────────
  Repo:    <repo> (<github_slug>)
  Issue:   #<issue_number> (<category>) — <title>   <issue_url>
  Branch:  <branch_name>   [removed | kept]
  PR:      <pr_url>   [merged (squash) | open]
  Disposition: <merge+cleanup | cleanup | stop>

  What shipped: <1-2 sentences, plain language — what the change DOES, as merged.
                 The same recap as the PR body's "What this does". Never a list of the
                 fix commits: the user wants the outcome, not the route to it.>
  Babysit: <R> round(s); fixed <N> related review/CI items.
  Reported (not fixed — out of scope):
    - <unrelated item> — <why>
  Declined nits (from fix push <NIT_CUTOFF_ROUND>+1 on):
    - <item> — <where>
  Follow-up issues filed:
    - #<n> <title>   <url>
  Deferred follow-ups (not filed):
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
  - nit → defer (pre-PR) / fix (first three babysit fix pushes — the bar is "clean") / decline (fourth on)
  - preexisting/unrelated → report, don't fix
  - disputed → reject (reason in PR)

PROCEDURE classify_babysit_item(item)
  - RELATED iff it concerns this change's diff, or a check our change caused/exposed to
    fail, or a missing artifact our change needs (e.g. changeset). Fix these — incl. nits
    for the first three fix pushes; from the fourth on, substantive RELATED items only
    (see the cutoff).
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
  - [ ] Babysit loop run: reviews read as they LANDED (never blocked on the full check suite first); CI green / bots finished; related items fixed — nits for the first three fix pushes, substantive only from the fourth; unrelated items reported, not fixed; loop converged
  - [ ] Churn checked each round (fixes-of-fixes counted; 3 consecutive → gate #8, not another patch)
  - [ ] Any rename or rule change in a fix was grepped for its old form before committing
  - [ ] PR description refreshed to match what actually shipped (recap added; stale claims from the babysit rounds corrected; `Closes #<number>` preserved)
  - [ ] Follow-up sweep done BEFORE the gate: small leftovers shipped into this PR; ≤3 follow-ups proposed, each with a cited issue-bar YES (or none); only user-selected ones filed, unassigned, no closing keyword in the body
  - [ ] Plain-language recap printed before the final question (asked for / what was wrong / what I did / still open; no jargon, no paths, no symbols)
  - [ ] Final disposition asked (merge+cleanup / cleanup / stop; reading-diff option offered iff the `meat` skill is available) and executed; squash used for merge
  - [ ] Capability docs synced after a merged user-visible platform change: `platform/features/` leading doc always; business mirror + platform-guide skill skipped gracefully when not accessible
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
| Only fixing "must-fix" review items in the first rounds | Through the third fix push the bar is a clean PR — fix related nits too. The only items you leave are ones the reviewer explicitly blesses as fine to keep. |
| Still fixing nits on the fourth fix push and beyond | Past the cutoff each nit costs a full CI cycle for nothing. Fix substantive items only; decline comment/wording nits with a one-line reason and move on. |
| Waiting for the whole check suite before reading the reviews | Poll for the first signal. A review that lands mid-run is actionable now, and pushing its fix supersedes the running checks anyway — reading it early is what makes the turnaround short. |
| Filing a follow-up issue for something cheaper to fix than to file | If it is inside this diff, needs no design decision, and is <15 min, ship it in THIS PR. Issues are only for leftovers that clear the issue bar, capped at 3. |
| Filing defense in depth, a rare mild edge case, or cleanup as a follow-up | Run the six issue-bar questions (user harm / money / stability / security / team drag / significant benefit). Six NOs is a DROP, however the reviewer phrased it. |
| Merging with the PR description that was written before babysitting | The body is written at open time from the plan; every fix push since changes what the PR does, and on a squash-merge that stale text becomes the repo's history. Refresh it in STEP 14c and lead with a 1-2 sentence plain-language recap. |
| Patching on when every round faults the previous round's fix | The nit cutoff will not catch this — the findings are all substantive. Count fixes-of-fixes; three rounds running means the design is wrong, so stop at gate #8 and restructure rather than adding another guard with another edge. |
| Renaming a placeholder or rule and updating only where it is defined | Grep the changed files for the old form before committing. A stated rule contradicting another stated rule is a correctness defect — the reader cannot tell which is current — and it is worse than the ambiguity it replaced. |
| Proposing follow-ups after the merge | Sweep in Phase 12b, before the gate. A follow-up proposed after the merge is a follow-up nobody files. |
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
- **Calls (optional, soft dependency):** the `meat` skill for a reading diff at the Phase 13 gate — only offered when it is installed; ship works unchanged without it
- **Uses:** `gh` CLI for issue + PR + CI/review state + merge; `git` directly for repo/worktree/branch/commit/push
- **Reads:** the target repo's `CLAUDE.md` + `package.json`, the root `skaile-dev/CLAUDE.md` Formatting/Testing tables, affected source, `gh label/issue/pr` state
- **Writes:** a GitHub issue + a PR on the target repo, any user-approved follow-up issues, implementation + babysit commits on the branch, a transient plan file (deleted); on merge, a squashed commit on the repo's main
- **Writes (conditional, Phase 13b):** after merging user-visible platform work — `platform/features/SKAILE-PLATFORM-CAPABILITIES.md` (the leading copy, always) plus its `features/<NN-section>/` doc; mirrored to the business doc (`/mnt/c/.../concept/`) and reflected in the ai-assets `platform-guide` skill only when those paths are accessible
- **Never writes:** any repo's legacy markdown issue folder — tracking is GitHub Issues
