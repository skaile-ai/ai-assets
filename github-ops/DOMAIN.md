---
name: github-ops
description: GitHub workflow automation — agent that clones repos, implements tasks, and creates PRs
---

## Purpose

Skills and agent definitions for the GitHub worker pattern: an agent spawned via
Mattermost that clones a GitHub repository into an isolated working directory,
implements the requested task, and opens a pull request with the result.

Used by `skaile-mattermost-bot` as the default agent definition.

## Skills

| Skill | Path | What it does | When to use |
|-------|------|-------------|-------------|
| mm-post-to-thread | skills/mm-post-to-thread/ | Structure and post progress or completion summaries to the Mattermost thread | After completing significant work or creating a PR |
| gh-create-pr | skills/gh-create-pr/ | Create a GitHub pull request using `gh` CLI | When changes are committed on a feature branch and ready for review |

## Agents

| Agent | Path | What it does |
|-------|------|-------------|
| github-worker | agents/github-worker/ | Clones a GitHub repo, implements a task, creates a PR, reports back to Mattermost |

## Notes

- Requires `gh` CLI authenticated in the runtime environment (`gh auth login`).
- Requires `git` configured with push access to the target repository.
- Each session runs in an isolated directory under `MM_SESSION_BASE_DIR`.
- The `github-worker` agent is designed to be used with `skaile-mattermost-bot`.
