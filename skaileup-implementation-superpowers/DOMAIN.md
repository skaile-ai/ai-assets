---
name: skaileup-implementation-superpowers
description: "Disciplined, subagent-driven implementation workflow with structured brainstorming, spec-first planning, supervised dispatch, and controlled branch completion. Adapted from obra/superpowers."
type: domain
building_blocks:
  contracts: "subagent_dispatch.md — 4-status implementer report format and escalation paths"
  skills: "Five skills covering git preparation, brainstorming, planning, supervised implementation, and branch finish"
  flows: "superpowers.flow.yaml — full supervised implementation pipeline"
stage: beta
---

# Dev Implementation — Superpowers

This domain wraps `skaileup-implementation` with a disciplined meta-workflow for complex features or
full app builds where subagent quality control and structured review matter most.

It is adapted from [obra/superpowers](https://github.com/obra/superpowers) — a workflow scaffolding
system for AI coding agents. The core insight: agents are fast but undisciplined. This domain
imposes mandatory gates before, during, and after implementation to prevent shortcuts.

Use this domain when:
- Building a complete application from a finished `_concept/`
- The feature set is large enough to require task decomposition across multiple subagents
- You need confident, reviewable output rather than speed

Use `skaileup-implementation` directly when:
- You need a single feature or a small incremental change
- You are already in a supervised session and don't need meta-workflow overhead

---

## Git Handling: Worktrees vs. Branches

This domain supports two git modes. Choose based on your agent environment.

| Mode | When to use | What happens |
|---|---|---|
| `branch` (default) | All agents, any environment | Creates `implement/<app-slug>` branch; feature sub-branches per journey |
| `worktree` | Claude Code running locally | Creates an isolated git worktree per parallel subagent task; merged and cleaned up after completion |

**Recommendation: use `branch` mode by default.**

Worktrees provide value when multiple subagents work in parallel on independent tasks (Claude Code
can spawn parallel agents, each with their own filesystem copy). For the sequential dispatch pattern
used by `implement-supervised`, branches are sufficient and work in any environment.

Only enable `worktree` mode when:
1. You are running Claude Code directly (not via API or remote runner)
2. You have explicitly configured `git_mode: worktree` in the flow globals
3. The tasks being dispatched are genuinely independent (no shared file writes)

The `git-prepare` skill handles both modes and detects the correct behavior from the flow's
`globals.git_mode` value.

---

## Skill Groups

| Group | Skill | Produces |
|-------|-------|----------|
| `00_setup/` | `git-prepare` | Initialized repo, implementation branch (or worktree) |
| `10_brainstorm/` | `brainstorm` | `_implementation/brainstorm.md` — problem decomposition, risks, open questions |
| `20_plan/` | `write-plan` | `_implementation/superpowers-plan.md` — task list ready for subagent dispatch |
| `30_implement/` | `implement-supervised` | Implemented features with spec compliance + quality review per task |
| `40_finish/` | `finish-branch` | Merged, PR'd, or archived implementation branch |

## Flow

`superpowers.flow.yaml` runs all five skills in sequence:

```
git-prepare → brainstorm → write-plan → implement-supervised → finish-branch
```

## Contracts

| File | Purpose |
|---|---|
| `contracts/subagent_dispatch.md` | Implementer status report format (DONE / DONE_WITH_CONCERNS / NEEDS_CONTEXT / BLOCKED) and escalation paths |

## Notes

- This domain does NOT replace `skaileup-implementation`. It orchestrates it. `implement-supervised`
  dispatches `implement-feature` from `skaileup-implementation` as its implementation worker.
- `brainstorm` and `write-plan` are pre-implementation gates — they slow the start but prevent
  rework from misunderstood scope.
- The two-stage review in `implement-supervised` (spec compliance → code quality) is the most
  important gate. Skipping spec compliance and going straight to quality review is a known
  failure mode — spec compliance must always come first.
