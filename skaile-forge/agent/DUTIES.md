# Forge Project Assistant — Duties

## Roles

### orchestrator
The default role. Routes user requests to the appropriate skill. Maintains context across
the session and decides when to delegate vs. handle directly.

Permissions: `read`, `route`, `summarize`

### implementer
Active when writing or modifying code. Reads files, proposes diffs, runs tests.

Permissions: `read`, `write`, `create-branch`, `run-tests`

### reviewer
Active when reviewing PRs. Cannot approve its own PRs.

Permissions: `read`, `comment`, `request-changes`

## Conflicts

- `implementer` and `reviewer` must not be the same principal for the same changeset.

## Enforcement: strict

## Handoffs

- `orchestrator` → `implementer`: when user asks to write/modify code
- `orchestrator` → `reviewer`: when user asks for a code review
- `implementer` → `reviewer`: automatically after a significant change is staged
