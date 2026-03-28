# Structured Commit Message Spec (v1)

This document defines the commit message format for the skaile-dev monorepo. It serves as the shared contract between:

- The **commit-message skill** (primary path — agent writes the message)
- The **commit-msg git hook** (safety net — validates or generates on local merges to main)
- The **GitHub Action** (safety net — validates on PR merges to main)
- **Consuming agents** (changelog generation, automated cross-package upgrades)

## Format

```
<type>(<scope>): <title>

<human-description>

---agent---
<yaml-block>
```

### Title Line

Conventional-commits style. Max 72 characters.

- `type`: one of `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `build`
- `scope`: comma-separated package paths relative to repo root (e.g. `agent-framework/session, agent-framework/types`)
- `title`: imperative mood, lowercase, no period

### Human Description

One to three sentences. Plain English. Describes **what** changed and **why** — not how. This section is for humans reading `git log`.

### Agent Block

Everything after the `---agent---` marker is a YAML document consumed by agents. It is **not** intended for human reading, though it should be human-readable for debugging.

## YAML Schema

```yaml
# --- Required ---
scope: [<package-path>, ...]        # packages directly modified in this commit
type: feat|fix|refactor|docs|test|chore|perf|build
breaking: true|false

# --- Dependency tracking ---
affects: [<package-path>, ...]      # packages that should react to this change
                                     # (downstream consumers, not the changed packages)

# --- Change inventory ---
changes:
- <imperative description of a discrete change>

# --- Architecture/design decisions ---
decisions:                           # omit if no architectural decisions were made
- <decision summary>
  reason: <why this over alternatives>
  alternatives: [<rejected option>, ...]
  revisit_when: <condition under which to reconsider>  # optional

# --- Migration guide per affected package ---
migrate:                             # omit if no migration needed
- <package-path>: <what the package maintainer should do>

# --- API surface diff ---
exports:                             # omit if no public API changes
# prefix: + added, ~ changed, - removed
# format: <prefix> <symbol> (<kind>) from|in <package>
+ <symbol> (<kind>) from <package>
~ <symbol> (<description>) in <package>
- <symbol> from <package>
```

### Field Reference

| Field | Required | Type | Description |
|---|---|---|---|
| `scope` | yes | list | Package paths modified in this commit |
| `type` | yes | enum | Change category (mirrors title line) |
| `breaking` | yes | bool | Whether this is a breaking change |
| `affects` | no | list | Downstream packages that should adapt |
| `changes` | yes | list | Discrete changes, imperative mood |
| `decisions` | no | list | Architectural decisions with rationale |
| `migrate` | no | list | Per-package migration instructions |
| `exports` | no | list | Public API surface diff |

### Decision Entry Fields

| Field | Required | Description |
|---|---|---|
| (key line) | yes | One-line summary of the decision |
| `reason` | yes | Why this choice was made |
| `alternatives` | yes | What was considered and rejected |
| `revisit_when` | no | Condition that would warrant revisiting |

### Export Prefixes

| Prefix | Meaning |
|---|---|
| `+` | New export added |
| `~` | Existing export changed (signature, behavior) |
| `-` | Export removed |

## When to Include Each Section

- `affects` + `migrate`: whenever changes in `scope` require or suggest changes in other packages
- `decisions`: whenever an architectural choice was made that has alternatives — even small ones. These feed the decision log.
- `exports`: whenever the public API surface of any package changes (new types, changed signatures, removed functions)

## Examples

### Feature with migration

```
feat(session, types): add event filtering to SessionDispatcher

Adds per-subscriber event filtering so frontends only receive
events they've subscribed to, reducing WebSocket traffic.

---agent---
scope: [agent-framework/session, agent-framework/types]
type: feat
breaking: false
affects: [agent-framework/runner, platform/backend/libs/agent-gateway]

changes:
- SessionDispatcher.subscribe() now accepts optional EventFilter
- New EventFilter type exported from @skaile/agent-types
- dispatch() checks filters before forwarding

decisions:
- Filter at dispatcher level, not at transport level
  reason: transport is protocol-agnostic, filtering is business logic
  alternatives: [transport-level filtering, client-side filtering]
  revisit_when: transport gains protocol-specific optimization needs

migrate:
- agent-framework/runner: update subscribe() calls to pass filter
  or omit for current broadcast behavior (backward compatible)
- platform/backend/libs/agent-gateway: when adopting filtered
  subscriptions, pass EventFilter matching the frontend's
  subscription request

exports:
+ EventFilter (type) from @skaile/agent-types
~ subscribe (signature changed, optional param) in @skaile/agent-session
```

### Simple fix, no migration

```
fix(flow-engine): handle empty parallel groups without throwing

computeFlowState crashed on flows containing parallel groups with
zero nodes after filtering. Now treats them as completed.

---agent---
scope: [agent-framework/flow-engine]
type: fix
breaking: false

changes:
- computeFlowState() returns completed for empty parallel groups
- Added guard in resolveNextNodes() for zero-length groups
```

### Breaking change

```
refactor(bridge): remove legacy OMP driver

Removes the deprecated OMP driver backend. All consumers should
use the Claude SDK or Pi drivers.

---agent---
scope: [agent-framework/bridge]
type: refactor
breaking: true
affects: [agent-framework/runner, agent-framework/cli, forge/project]

changes:
- Remove OmpDriver class and omp.ts module
- Remove omp from DriverType union
- Update createDriver() to throw on driver: "omp"

decisions:
- Hard removal instead of deprecation warning
  reason: OMP backend has zero usage in production for 3 months
  alternatives: [soft deprecation with warning, feature flag]

migrate:
- agent-framework/runner: remove any omp-specific driver config
- agent-framework/cli: remove --driver omp from CLI options
- forge/project: switch to claude-sdk driver in agent config

exports:
- OmpDriver (class) from @skaile/agent-bridge
~ DriverType (removed "omp" variant) in @skaile/agent-bridge
```
