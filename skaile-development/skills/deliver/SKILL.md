---
name: "deliver"
description: >-
  Take one piece of work in one repository from "somebody described it" to a traceable,
  reviewed, checks-green change whose fate the human decided. Use whenever someone wants
  something in a repository actually changed rather than explained - however casually they
  put it. "can you sort it", "can you take it", "can we get X on this", "have a look at #12"
  and "fix it and push it" are all this skill; the person does not have to mention branches,
  a pull request, review or merging, and the change can be as small as one constant, one
  string of copy or one dependency bump. Also use when handed an issue number or link to
  build, or an already-open pull request to carry the rest of the way. It runs the whole
  cycle in one flow - files the tracker issue, branches in an isolated worktree, implements,
  gets the diff reviewed, opens the PR, drives CI and review feedback to green - then recaps
  in plain language and asks once whether to squash-merge and clean up. This is the skill
  formerly named `ship`: Claude Code reserves `/ship` and `/steward` for its own commands,
  which start from finished work on a branch and never merge. Do not use for questions,
  diagnosis or code explanation, for reviewing someone else's existing code, for plans or
  design proposals, for filing an issue when implementation is explicitly deferred, for
  throwaway local experiments, or for work spanning several repositories.
version: 2.1.0
metadata:
  tags:
  - "deliver"
  - "ship"
  - "implement"
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
      label: "Describe the work item in plain language (the skill refines the wording
        and opens the GitHub issue without asking)"
      type: "text"
      required: true
    - id: "repo"
      label: "Target repo (e.g. platform, store, workspaces, brand). Leave empty to
        infer it from the description"
      type: "text"
      required: false
    - id: "issue"
      label: "Existing GitHub issue OR pull request (number or URL) to work on"
      type: "text"
      required: false
      hint: "An issue is reused instead of opening a new one. A pull request is
        resumed: the skill skips the build and goes to watching it."
    - id: "allowed_dirty"
      label: "Paths in the target repo's checkout that may be dirty (pre-authorized)"
      type: "text"
      required: false
      hint: "Files or directories relative to the repo root, separated by commas or
        newlines. A directory covers what is below it (`docs` covers `docs/a.md`, not
        `docs-internal/a.md`). The skill never stashes, commits, reverts or touches
        them."
    files: []
---

# Deliver: one work item, from description to merged

One run takes one work item in one repository through the whole cycle:

| Step | What happens |
|---|---|
| 1 | Resolve the repo and its profile |
| 2 | Pre-flight the checkout |
| 3 | Open or reuse the GitHub issue; resume or stop when the work already has a PR |
| 4 | Create the worktree and the branch |
| 5 | Investigate, then size the work: `small` or `standard` |
| 6 | Build it |
| 7 | Review the diff |
| 8 | Verify, commit, sync with the default branch, push once, open the PR |
| 9 | Watch the PR: fix what CI and reviewers find, until it is clean |
| 10 | Sweep for follow-ups |
| 11 | Refresh the PR body, print the recap |
| 12 | Finish: ask the person (or hand back to the caller), then merge and clean up |
| 13 | Final report |

The worktree and the branch stay alive from step 4 until step 12 decides their fate.

## How a run is started

- **Interactive.** A person asked. Run without questions. Step 12 is the one planned
  question.
- **Unattended.** A caller started the run as a subagent, a fork or a lane, or the
  `AskUserQuestion` tool is not available. The rules that change:
  - Step 12 becomes a **hand-back**: never merge, never clean up, never file
    follow-ups. Return the PR URL and head SHA, the CI and review state, the recap, the
    proposed follow-ups and the question step 12 would have asked.
  - Every other stop gate becomes stop-and-report, with the worktree and branch kept.
  - At most **two** fix pushes in step 9, then hand back with the open findings.
  - Hand back **at once** when CI is green, the review is clean, and the only pending
    check is a gate that waits for a person (see `references/watch.md`).
- **No subagents** (a fork cannot spawn children). In step 7 the PR's review bot is the
  independent reviewer; if the repo has none, say in the report
  that the change had no independent review.

One unattended PR runs best as a fork: it already has the caller's context.

## Rules

Each rule is stated once, here or in the step it belongs to.

- **Decide from evidence, do not ask.** Repo, wording, branch name, size, which
  findings to apply: decide, print one short progress line, continue. Ask only at a
  stop gate (listed at the end).
- **Tracking is GitHub Issues** on the target repo. The work item's identity is the
  issue number.
- **The repo is the authority.** Slug, default branch, formatter, lint and test
  commands, changeset rule, PR template and commit convention come from the target
  repo's `CLAUDE.md` / `AGENTS.md`, `package.json` and `.github/`. Never carry one
  repo's toolchain to another. A `CLAUDE.md` in a parent directory is not a source.
- **Read `references/<repo>.md`** beside this skill when it exists (today:
  `references/platform.md`) and follow it. Repo-specific facts live there, not in this
  file.
- **Scope is this change.** Fix what the change caused or exposed. Report unrelated,
  pre-existing or architectural problems; do not fix them.
- **Git safety.**
  - Never force-push, rewrite history, or commit to the default branch.
  - Never `git stash`: the stash is shared by every worktree of the repository.
  - Never discard edits with `git checkout -- <path>`: it restores from the index,
    which a platform commit may already hold the bad content in (a Skaile session
    commits at hibernate and close, onto its `session/<slug>` branch; with Git session
    branches off it also auto-commits when idle). Use
    `git restore --source=HEAD --staged --worktree <paths>`.
  - Stage the files you changed by name. Check `git status --short` before each commit.
  - Never touch a path listed in `allowed_dirty`.
- **Closing keywords.** `Closes #<n>` appears in the PR body and in the squash body,
  for the issue this run delivers, and nowhere else. GitHub ignores negation, so
  `not Closes #5` closes #5. Every other issue gets `Part of #<n>` or a bare `#<n>`.
  Branch commits carry `(#<n>)` in the title and no keyword.
- **Merge only by squash**, and only after the person chose it. Never force past branch
  protection.

## 1. Resolve the repo and its profile

Check `gh auth status` first. If it fails, stop: "Run `gh auth login`."

`repo` = the input, else inferred from the description (a named repo, a package, a
command, a path), else ask.

`repo_path` = the **main checkout** of the local clone, never a linked worktree:

```bash
common=$(git -C <dir> rev-parse --path-format=absolute --git-common-dir)
wt=$(git --git-dir="$common" config core.worktree)   # set only for a submodule checkout
if [ -n "$wt" ]; then repo_path=$(cd "$common" && cd "$wt" && pwd)
else repo_path=$(dirname "$common"); fi
```

Use the current directory when its `origin` names `<repo>`; else the sibling directory
`<repo>` next to the current clone; else ask for the path.

Then derive:

- `github_slug` from `git -C <repo_path> remote get-url origin`.
- `default_branch` from `gh repo view <slug> --json defaultBranchRef -q .defaultBranchRef.name`.
- `lint_cmd`, `test_cmd`, `format_cmd` from the repo's docs and `package.json`. A
  content repo can have none: skip them.
- `changeset_required` = `.changeset/` exists and a changeset check workflow is present.
- `pr_template` = `.github/pull_request_template.md` (either case) when present.
- `repos_root` = the parent directory of `repo_path`.

Print: `Targeting <repo> (<slug>, default branch <default_branch>).`

## 2. Pre-flight

Run this before the issue is opened, so a stop leaves no dangling issue.

```bash
git -C <repo_path> fetch origin
git -C <repo_path> status --porcelain=v1 -z --untracked-files=all
```

A dirty path is **covered** when it equals an `allowed_dirty` entry or starts with
`<entry>/`. Files of the harness itself (`.claude/scheduled_tasks.lock`, `.claude/tmp/`)
are always covered. If a dirty path is not covered, stop and name the paths: the
person commits them or passes them in `allowed_dirty`. The worktree branches off
`origin/<default_branch>`, so a covered dirty checkout never reaches the change.

## 3. Issue, resume, or nothing to do

**The input is a pull request** (or the issue already has one open PR, read from
`gh issue view <n> --json closedByPullRequestsReferences` and, as a fallback,
`gh pr list --search "<n> in:body" --state open`): resume it. Several open PRs: ask
which one. Take the branch from the PR and the issue `<n>` from the PR's closing
references. Then find the worktree for the branch, in this order:

1. `git -C <repo_path> worktree prune`, then `git -C <repo_path> fetch origin <branch>`.
2. Look for a worktree that has `<branch>` checked out
   (`git -C <repo_path> worktree list --porcelain`).
3. None: `git -C <repo_path> worktree add <repos_root>/.worktrees/<repo>-<n> <branch>`
   (no `-b`). That path is `<worktree>` for every later step.
4. One exists (an earlier run kept it): stop and report (gate 10) when it is
   `<repo_path>` itself, when it has uncommitted changes that step 2's rule does not
   cover, or when `git -C <it> merge --ff-only origin/<branch>` fails. Check in that
   order, and change nothing in it before all three pass. Otherwise it is `<worktree>`.
   Never add a second worktree for the branch, and never drop its commits or edits.

Then go to step 9.

A PR with no linked issue is resumed without one: the worktree is
`<repos_root>/.worktrees/<repo>-pr<pr>`, the titles carry `(#<pr>)`, and every step
that reads, confirms or closes the issue is skipped. Do not open an issue for it.

**The issue is closed and a merged PR closed it:** do nothing. Report the merged PR,
its merge commit and whether it is deployed (the profile reference names the deploy
path; without one say "deploy state unknown"). An open issue whose change is already
on the default branch gets the same report plus "needs a hand close".

**Otherwise:**

- With an `issue` input: reuse it. Self-assign and add the `agent` and `in-progress`
  labels where the repo has them. Remove a label that queues the issue for another
  runner (the profile reference names it), so no second agent builds it.
- Without one: search open issues for two or three key terms. A clear duplicate is
  reused. Else refine the description (one to three sentences: the surface, observed
  against expected or the new behaviour, the person's constraints kept verbatim), derive
  an imperative title of at most 72 characters, and create the issue:

  ```bash
  gh issue create --repo <slug> --title "<title>" --body "<text>" --assignee @me <labels>
  ```

  Pass only labels that `gh label list` shows: `bug` or `enhancement` by the kind of
  work, plus `agent`. A label the repo lacks makes the command fail.

Print: `Opened #<n> - <url>.`

## 4. Worktree and branch

```bash
git -C <repo_path> worktree prune
git -C <repo_path> worktree add <repos_root>/.worktrees/<repo>-<n> \
    -b <prefix>/<n>-<slug> origin/<default_branch>
```

`<prefix>` is `fix`, `feat` or `chore`; `<slug>` is up to 40 characters of concrete
nouns. The worktree sits beside the clone, never inside it. Then do the setup the
profile reference lists for a fresh worktree (platform: `.env` files and `bun install`),
or install dependencies when the repo needs them. All work until step 12 happens
there.

## 5. Investigate and size the work

Read the repo's docs for the surface, the files the person named, the neighbours along
the data flow, and `git log --oneline -- <path>` for the area. Stop when the cause or
the design is clear.

For a feature, or a chore that adds a package, service or integration: run the
`skaile-inventory` skill and record its reuse / extend / port / build verdict. Where it
is not installed, record "skaile-inventory not available".

If the cause or the design cannot be found: label the issue `needs-info` (where the
label exists), remove the empty worktree, report what was checked and what would help,
and stop.

Then size the work:

| Size | When | Consequence |
|---|---|---|
| `small` | The fix is known, touches at most 2 files and about 30 lines, and needs no design choice | You read the diff once; no review subagent |
| `standard` | Everything else | A fresh subagent reviews the diff (step 7) |

Print: `Sized as <size>: <one-line cause or design>.`

## 6. Build

Every build adds or extends a test that pins the behaviour, and adds a changeset when
the repo requires one.

Make the change in the worktree yourself, whatever the size: the session that
investigated holds the context, and a hand-off to a build subagent only loses it.

## 7. Review

Measure the diff with `git diff --numstat origin/<default_branch>`. A `small` change
that stayed small is read once by you, and the step ends.

Otherwise dispatch a fresh `general-purpose` subagent that sees the diff and the work
item, not your reasoning. A run that cannot spawn subagents reads the diff once itself,
and the PR's review bot is the independent reviewer in step 9. It checks: correctness against the work item, scope creep, the
repo's conventions, test coverage, performance, security. It reports each finding as
`important | nit | preexisting - file:line - text`, with at most five nits.

Triage every finding yourself, without asking:

| Finding | Action |
|---|---|
| Important, real, up to about 20 lines | Fix now |
| Important, larger, or needs a new abstraction | Stop gate: confirm the scope first |
| Nit | Fix when it is one line; else leave it |
| Pre-existing or out of scope | Keep for the step 10 sweep |
| You disagree | Reject, with a one-line reason in the PR body |

## 8. Verify, commit, push, open the PR

1. Run the checks the profile reference lists for this point (platform: docs impact,
   invariant suites, a boot of the backend).
2. Run `lint_cmd`, `format_cmd` (as a check) and the affected tests. On a failure make one
   focused fix; if it still fails, stop with the worktree kept.
3. Commit with the repo's convention: `<type>(<scope>): <title> (#<n>)` and one to
   three sentences of plain prose.
4. `git fetch origin <default_branch>` and merge it into the branch (never rebase).
   Conflicts: take the default branch's side for generated files and lockfiles, then
   regenerate; blend source so both intents stand; ask when they exclude each other.
   Re-run lint and the tests after a blend.
5. Push once: `git push -u origin <branch>`. A push that fails for anything other than
   a moved default branch is a stop gate.
6. Open the PR against `<default_branch>`. Fill the repo's PR template when it has
   one; else write What / Why / Changes / Testing. The body carries `Closes #<n>` as
   bare prose on its own line, and any rejected review finding with its reason.

Print one block: repo, issue, branch, PR URL, a one-sentence summary. This is the only
report before the end.

## 9. Watch the PR

Read `references/watch.md` now and follow it. In short: poll with
`scripts/babysit-poll.sh`, act on the first signal (a review or the end of CI,
whichever comes first), fix what relates to the change, decline nits after the third
fix push, and stop when CI is green and no related finding is open.

## 10. Follow-up sweep

Run once, before step 12. Gather the loose ends: deferred review findings, unrelated
items from step 9, things you noticed and did not do. Nits declined in step 9 stay
declined. Sort each one:

- **Deliver now:** inside this diff, no design decision, under about 15 minutes. Fix
  and push, then run step 9 once more with the same counters. Do not sweep again.
  This push is a fix push like any other: an unattended run that has already used its
  two fix pushes lists the item in the hand-back instead.
- **Follow-up issue:** it cannot ride along, and one of the six issue-bar questions
  answers YES with one line of evidence: user harm, money, stability, security, team
  drag, significant benefit. The `issue-bar` skill has the full catalogue.
- **Open question:** one answer hangs on a specific fact you cannot reach, or a
  reviewer raised a product call.
- **Drop:** everything else, in silence. Defence in depth, hypothetical edges, cleanup,
  renames and test gaps on code with no known bug answer NO six times.

Propose at most three, evidenced ones first. Proposing none is the normal outcome.

## 11. Refresh the PR body and print the recap

**PR body.** Fix pushes changed what the PR does, and on a squash-merge the body is
what the history keeps. Re-read the final diff, fetch the live body to a file, and stop
if the fetch fails or the file is empty (an empty `--body-file` clears the body). Edit
the file: correct what is no longer true, and lead with `**What this does:**` and one
or two plain sentences. Confirm `Closes #<n>` is still there, then
`gh pr edit <pr> --body-file <file>`.

**Recap.** The person may have walked away hours ago. Print four lines for someone who
does not know the codebase: no file paths, no symbol names, no code, no jargon. State
the effect a person can notice, not the mechanism.

```
In plain words
You asked for:  <the request, one sentence>
What was wrong: <what happened and who it affected; for new work, what was missing>
What I did:     <the change as a person notices it, and how it was checked>
Still open:     <what this does not fix, or "Nothing.">
```

## 12. Finish

**Unattended:** print the step 13 block with `Disposition: handed back to caller` and
stop. The PR stays open.

**Interactive:** if checks still run, wait for them or say so. Then ask once, with
`AskUserQuestion`, and put the follow-up choice (multi-select) in the same call when
step 10 proposed any:

- **Squash-merge and clean up**
- **Clean up only** (the PR and the remote branch stay)
- **Stop here** (everything stays; say where the worktree is)
- **Reading diff first**, only when the `meat` skill is installed: run it over
  `origin/<default_branch>...HEAD`, then ask again without this option.

**Merge.**

1. Run the pre-merge check of the profile reference (platform has a required one).
2. `gh pr view <pr> --json mergeStateStatus,mergeable,reviewDecision`. If the PR is
   blocked (a required approval, a red required check): say exactly
   why, offer to wait or stop, and delete nothing.
3. Merge. The branch is still checked out in the worktree, so do not pass
   `--delete-branch`:

   ```bash
   gh pr merge <pr> --repo <slug> --squash --match-head-commit <full head SHA> \
     --subject "<type>(<scope>): <title> (#<n>)" \
     --body "<one-line summary>

   Closes #<n>"
   ```

4. Read `gh issue view <n> --json state`. If it is still open after 10 seconds, close
   it by hand with a comment that names the PR and the squash commit. The PR body's
   keyword alone has left issues open after a squash-merge with a custom body.

**Clean up** (after a merge, or for "clean up only"):

```bash
git -C <worktree> log @{upstream}..HEAD --oneline   # must be empty; else stop, never force
git -C <repo_path> worktree remove <worktree> --force
git -C <repo_path> branch -D <branch>
git -C <repo_path> push origin --delete <branch> || true   # after a merge only
```

**Follow-ups.** File only the ones the person selected. The body: what is wrong in two
to four sentences, an `Issue bar: <question> - <evidence>` line, and `Refs #<n>`. No
closing keyword, no assignee, no `agent` label: these are backlog, not claimed work.

## 13. Final report

```
Delivered
Repo:        <repo> (<slug>)
Issue:       #<n> <title> <url>   [closed by merge | closed by hand | open]
PR:          <url>   [merged (squash) | open]
Deployed:    <no - waits for <release step> | yes (<how verified>) | unknown>
Disposition: <merge + clean up | clean up | stop | handed back to caller>
What changed: <one or two plain sentences: the outcome, not the list of commits>
Watch:       <F> fix push(es); fixed <N>; declined <D> nit(s)
Reported, not fixed: <unrelated items, each with its reason>
Follow-ups filed:    <#n title>
Worktree:    <removed | kept at <path>>
```

Merged is not deployed. Take the release step from the profile reference.

## Stop gates

Stop and ask (interactive) or stop and report (unattended) only for these:

1. `gh` is not authenticated.
2. The target repo cannot be determined.
3. The checkout is dirty outside `allowed_dirty`.
4. The investigation is inconclusive.
5. An important review finding needs a large change or a new abstraction.
6. A merge conflict where both intents cannot stand.
7. A push fails for a reason other than a moved default branch.
8. The watch does not converge (the cases are in `references/watch.md`).
9. The finish (step 12).
10. The resume cannot attach a clean worktree to the PR's branch (step 3).

## Integration

- **Uses:** `gh` and `git`; `scripts/babysit-poll.sh` for the step 9 poll.
- **Dispatches:** one `general-purpose` subagent for the review of a `standard` change.
- **Optional skills:** `skaile-inventory` (step 5), `issue-bar` (step 10), `meat`
  (step 12).
- **Not the same as** Claude Code's `/ship` and `/steward`. Those start from finished
  work on a branch, only a typed line starts them, and they never merge. A person who
  only wants an existing branch pushed and watched can use them.
