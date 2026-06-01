---
name: gh-create-pr
description: Use when all changes for a task are committed on a feature branch 
  and you are ready to open a GitHub pull request for human review.
license: MIT
metadata:
  tags: [github, pr, pull-request, git, review, gh-cli]
  stage: stable
  source: CF
  prerequisites:
    inputs_optional:
    - id: pr_title
      label: "PR title"
      type: text
      required: true
    - id: pr_body
      label: "PR description (What / Why / Testing)"
      type: textarea
      required: true
---

## Overview

Creates a GitHub pull request from the current feature branch using the `gh` CLI.
The PR is opened as a draft by default (safer for agent-generated changes — allows
human review before marking ready).

## Prerequisites

Before running this skill:

1. You are on a branch named `agent/<slug>` (not `main` or `master`)
2. All changes are committed: `git status` shows clean working tree
3. The branch has been pushed to the remote: `git push -u origin <branch>`
4. `gh` CLI is available: `which gh` returns a path
5. `gh` is authenticated: `gh auth status` returns success

If `gh` is not authenticated, stop and post to the thread:
```
**Blocked:** `gh` CLI is not authenticated. Run `gh auth login` in the bot's environment and retry.
```

## When to Use

- All task changes are committed and pushed on a feature branch
- The implementation is complete and ready for human review

## When NOT to Use

- Before committing all changes
- When on `main`, `master`, or any protected branch
- When the feature branch has not been pushed to the remote yet

## Workflow

1. **Verify branch name:**
   ```bash
   git branch --show-current
   ```
   Must match `agent/*`. If not, stop — do not create the PR.

2. **Verify clean working tree:**
   ```bash
   git status --porcelain
   ```
   If output is non-empty, commit or stash remaining changes first.

3. **Push the branch:**
   ```bash
   git push -u origin $(git branch --show-current)
   ```

4. **Compose the PR body** following this template:
   ```
   ## What
   <1-2 sentences describing what changed>

   ## Why
   <The user's original request>

   ## Testing
   <How to verify the change works>

   ---
   *Opened by skaile github-worker agent*
   ```

5. **Create the PR as a draft:**
   ```bash
   gh pr create \
     --title "<title>" \
     --body "<body>" \
     --draft
   ```

6. **Capture and output the PR URL:**
   ```bash
   gh pr view --json url --jq .url
   ```

7. **Pass the URL to `mm-post-to-thread`** for the completion report.

## Output

The primary output of this skill is the PR URL (e.g. `https://github.com/owner/repo/pull/42`).
Always output this URL explicitly so it can be included in the Mattermost thread report.

## Common Mistakes

| Mistake | Correct approach |
|---|---|
| Creating PR before pushing the branch | Always `git push` before `gh pr create` |
| Using `--merge` or auto-merge | Never merge — leave the PR open for human review |
| Skipping `--draft` flag | Always `--draft` for agent PRs |
| Not posting the PR URL to the thread | Always follow with `mm-post-to-thread` |
