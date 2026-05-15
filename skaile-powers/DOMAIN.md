---
name: skaile-powers
description: Skaile-specific workflow skills — a version-tracked fork of obra/superpowers v5.0.6 adapted to the skaile-dev monorepo. Covers the full development workflow from brainstorming through planning, execution, and review.
type: domain
building_blocks:
  references: 'config.md — central config for all paths and conventions'
  skills: 'using-skaile-powers, brainstorming, writing-plans, writing-skills, systematic-debugging, test-driven-development, executing-plans, dispatching-parallel-agents, subagent-driven-development, using-git-worktrees, requesting-code-review, receiving-code-review, verification-before-completion, finishing-a-development-branch'
stage: beta
upstream: obra/superpowers v5.0.6
---

# Skaile Powers

Skaile-dev workflow skills forked from [obra/superpowers](https://github.com/obra/superpowers) v5.0.6 and adapted to the skaile-dev monorepo. These skills cover the complete development workflow: brainstorming ideas, writing implementation plans, executing tasks, and reviewing work.

These skills will eventually replace the `obra/superpowers` Claude Code plugin entirely.

## When to Use This Domain

Reach for `skaile-powers` whenever the generic `obra/superpowers` plugin would apply but you want skaile-specific conventions: correct artifact paths, skaile commit-spec format, Bun + Vitest test stack, and submodule-aware git operations.

## Building Blocks

| Folder | Skill | Purpose |
|---|---|---|
| `references/config.md` | — | Central config: paths, conventions, upstream version, skill inventory |
| `skills/meta/using-skaile-powers/` | `using-skaile-powers` | Session-start skill; replaces `using-superpowers` |
| `skills/planning/brainstorming/` | `brainstorming` | Design before implementation |
| `skills/planning/writing-plans/` | `writing-plans` | Implementation plan authoring |
| `skills/planning/writing-skills/` | `writing-skills` | New skill authoring (skaile frontmatter) |
| `skills/execution/systematic-debugging/` | `systematic-debugging` | Root-cause-first debugging |
| `skills/execution/test-driven-development/` | `test-driven-development` | TDD with Vitest + Playwright |
| `skills/execution/executing-plans/` | `executing-plans` | Inline plan execution with checkpoints |
| `skills/execution/dispatching-parallel-agents/` | `dispatching-parallel-agents` | Parallel agent dispatch |
| `skills/execution/subagent-driven-development/` | `subagent-driven-development` | Subagent-per-task execution with review |
| `skills/execution/using-git-worktrees/` | `using-git-worktrees` | Isolated worktree setup (submodule-aware) |
| `skills/review/requesting-code-review/` | `requesting-code-review` | Code review dispatch (escalates to skaile `audit`) |
| `skills/review/receiving-code-review/` | `receiving-code-review` | Code review response with technical rigor |
| `skills/review/verification-before-completion/` | `verification-before-completion` | Verification gate before claiming completion |
| `skills/review/finishing-a-development-branch/` | `finishing-a-development-branch` | Branch completion: merge/PR/keep/discard |

## Skills

| Skill | When to Use |
|---|---|
| `using-skaile-powers` | Session start — establishes available skills and invocation rules |
| `brainstorming` | Before any creative/feature work — designs before code |
| `writing-plans` | After brainstorming, before implementation |
| `writing-skills` | When creating or editing skills in ai-assets |
| `systematic-debugging` | On any bug, test failure, or unexpected behavior |
| `test-driven-development` | When implementing features or fixing bugs |
| `executing-plans` | Executing a written plan inline with review checkpoints |
| `dispatching-parallel-agents` | When facing 2+ independent tasks |
| `subagent-driven-development` | Executing plans with fresh subagents per task |
| `using-git-worktrees` | Before starting isolated feature work |
| `requesting-code-review` | After completing tasks or features |
| `receiving-code-review` | When receiving code review feedback |
| `verification-before-completion` | Before claiming work is complete |
| `finishing-a-development-branch` | When implementation is complete |

## Upstream Sync

Forked from `obra/superpowers v5.0.6`. To sync with upstream:

1. Check upstream release notes for the relevant skill
2. `diff` the adapted SKILL.md against the new upstream version
3. Cherry-pick changes that don't conflict with skaile adaptations
4. Update `upstream` in this DOMAIN.md and in `references/config.md`

## Relationship to Other Domains

- **superpowers plugin** (`obra/superpowers`): Generic predecessor; skaile-powers replaces it for skaile-dev work. During migration, both coexist; skaile-powers takes priority.
- **skaile-development**: Implementation workflow skills (implement, git, audit, devlog). Skaile-powers provides the meta-workflow layer: how to think about and plan work before calling skaile-development skills.
