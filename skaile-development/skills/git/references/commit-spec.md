# Structured Commit Message Spec (v2)

This document defines the commit message format for the skaile-dev monorepo. It serves as the shared contract between:

- The **git skill, commit mode** (primary path - agent writes the message)
- The **commit-msg git hook** (safety net - validates title format on main)

## Format

```
<type>(<scope>): <title>

<human-description>
```

### Title Line

Conventional-commits style. Max 72 characters.

- `type`: one of `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `build`
- `scope`: comma-separated package paths relative to repo root (e.g. `agent-framework/session, agent-framework/types`)
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
