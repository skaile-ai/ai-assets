# Structured Commit Message Spec (v2)

This document defines the commit message format for the standalone skaile repos (platform, workspaces, ai-assets, store, brand, …). It serves as the shared contract between:

- The **git skill, commit mode** (primary path - agent writes the message)
- The repo's **commit-msg git hook**, where it has one (safety net - validates the title format on the default branch)

The target repo's own `CLAUDE.md` wins where it says more. Changes land by squash merge on
GitHub, so GitHub builds the commit from the **PR title**. The PR title must follow the same
title rules.

There is **no `---agent---` YAML block**. It belonged to the retired `skaile-dev` shell; no
standalone repo requires or consumes it, and nothing extracts decisions from commit messages.

## Format

```
<type>(<scope>): <title>

<human-description>
```

### Title Line

Conventional-commits style. Max 72 characters.

- `type`: one of `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `build`
- `scope`: short package or area name(s) in the target repo, comma-separated (e.g. `session, types` in workspaces; `frontend` or `flows` in platform)
- `title`: imperative mood, lowercase, no period

### Human Description

One to three sentences. Plain English. Describes **what** changed and **why** - not how. This section is for humans reading `git log`.

## Examples

### Feature

```
feat(session, types): add event filtering to SessionDispatcher

Adds per-subscriber event filtering so frontends only receive
events they've subscribed to, reducing WebSocket traffic.
```

### Simple fix

```
fix(flow-engine): handle empty parallel groups without throwing

computeFlowState crashed on flows containing parallel groups with
zero nodes after filtering. Now treats them as completed.
```

### Breaking change

```
refactor(bridge): remove legacy OMP driver

Removes the deprecated OMP driver backend. All consumers should
use the Claude SDK or Pi drivers.
```
