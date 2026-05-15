# Skaile Powers Configuration

> **Every skaile-powers skill reads this file first.** It is the single source of truth for all paths, conventions, and the skill inventory. To change a path or convention, edit only this file — all skills pick it up automatically.

## Upstream

Forked from: **obra/superpowers v5.0.6**

## Artifact Paths

| Artifact | Path |
|---|---|
| Design specs | `docs/devlog/specs/YYYY-MM-DD-<topic>-design.md` |
| Implementation plans | `docs/devlog/plans/YYYY-MM-DD-<feature>.md` |
| Audit reports | `_devlog/reports/` |
| Devlog | `_devlog/DEVLOG.md` |

## Monorepo Conventions

| Convention | Detail |
|---|---|
| Package manager | Bun (`bun@1.3.9`) — never use npm or yarn |
| Test runner (unit) | `bun x --bun vitest run` — for all `workspaces/*` packages |
| Test runner (E2E) | Playwright only |
| No Jest | Jest and `platform/` references are out of scope for skaile-powers |
| Formatter | Biome for all packages **except** `platform/` — never run Biome on `platform/` |
| Commit format | Skaile commit-spec (see below) |
| Submodule commits | When changes span shell repo + submodule: commit submodule first, then bump in shell repo as a separate commit |

## Skaile Commit-Spec Format

All commit steps in skaile-powers skills use this format:

```
type(scope): title (max 72 chars)

Human description: 1-3 sentences — what changed and why.

---agent---
scope: [<package-paths>]
type: feat|fix|refactor|docs|test|chore|perf|build
breaking: true|false
affects: [<downstream-packages>]

changes:
- <imperative description of each discrete change>

decisions:                  # omit section if no architectural choices
- <decision summary>
  reason: <why>
  alternatives: [<rejected options>]
  revisit_when: <condition>
```

Types: `feat` (new feature), `fix` (bug fix), `refactor`, `docs`, `test`, `chore`, `perf`, `build`.

## Skill Inventory

All 14 skills by flat name — use these names when invoking via `Skill` tool or skaile CLI.

| Skill | Group | Path |
|---|---|---|
| `using-skaile-powers` | meta | `skills/meta/using-skaile-powers/` |
| `brainstorming` | planning | `skills/planning/brainstorming/` |
| `writing-plans` | planning | `skills/planning/writing-plans/` |
| `writing-skills` | planning | `skills/planning/writing-skills/` |
| `systematic-debugging` | execution | `skills/execution/systematic-debugging/` |
| `test-driven-development` | execution | `skills/execution/test-driven-development/` |
| `executing-plans` | execution | `skills/execution/executing-plans/` |
| `dispatching-parallel-agents` | execution | `skills/execution/dispatching-parallel-agents/` |
| `subagent-driven-development` | execution | `skills/execution/subagent-driven-development/` |
| `using-git-worktrees` | execution | `skills/execution/using-git-worktrees/` |
| `requesting-code-review` | review | `skills/review/requesting-code-review/` |
| `receiving-code-review` | review | `skills/review/receiving-code-review/` |
| `verification-before-completion` | review | `skills/review/verification-before-completion/` |
| `finishing-a-development-branch` | review | `skills/review/finishing-a-development-branch/` |
