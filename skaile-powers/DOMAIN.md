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

Skaile-dev workflow skills forked from [obra/superpowers](https://github.com/obra/superpowers) v5.0.6 and adapted to the skaile-dev monorepo. These skills cover the complete development workflow: brainstorming ideas, writing implementation plans, executing tasks, and reviewing work — all using the correct skaile-dev paths, commit format, and test stack.

These skills will eventually replace the `obra/superpowers` Claude Code plugin entirely. During migration both coexist; `skaile-powers` takes priority.

---

## The Full Workflow

Every meaningful development session follows this arc. Start at `using-skaile-powers`, identify the nature of the work, and follow the path through to `finishing-a-development-branch`.

```
  ┌──────────────────────────────────────────┐
  │  Session start                           │
  │  invoke: using-skaile-powers             │
  └─────────────────┬────────────────────────┘
                    │
        ┌───────────▼────────────┐
        │  What kind of work?    │
        └───┬───────┬────────────┘
            │       │       │
            │       │       └─────────────────────────────┐
            │       │                                      │
      new idea /  bug / failure /               authoring a new skill
      new feature  unexpected behavior                     │
            │       │                                      ▼
            │       │                              writing-skills ──► done
            ▼       ▼
      brainstorming  systematic-debugging
            │                │
            │        test-driven-development
            ▼                │
      writing-plans ◄────────┘  (fix the failing test, commit)
            │
            ▼
  ┌─────────────────────────────────────────────────────────────────┐
  │  Execution mode selection (see chart below)                     │
  └─────────────────┬───────────────────────────────────────────────┘
                    │
                    │  (each task follows test-driven-development)
                    │
                    ▼
  ┌──────────────────────────────────────────┐
  │  Review loop                             │
  │                                          │
  │  verification-before-completion          │
  │         │           │                   │
  │       pass         fail                 │
  │         │           │                   │
  │         │     fix → repeat              │
  │         │                               │
  │  requesting-code-review                 │
  │         │                               │
  │  receiving-code-review                  │
  │  (address each issue, re-verify)        │
  └─────────────────┬────────────────────────┘
                    │
                    ▼
          finishing-a-development-branch
          (merge / PR / keep / discard)
```

---

## Choosing an Execution Mode

Once you have a written plan, choose how to execute it:

```
  Have a written plan?
         │
    no ──┴──► write one first with writing-plans
         │
        yes
         │
    Are the tasks mostly independent of each other?
         │
    no ──┴──► executing-plans
    (tightly     (inline execution with review checkpoints)
    coupled)
         │
        yes
         │
    Stay in this session?
         │
    yes ─┴──► subagent-driven-development
    │         (fresh subagent per task + spec + quality review)
    │
    no ──────► dispatching-parallel-agents
               (run tasks in parallel across sessions)
```

**Rule of thumb:**
- Most feature work → `subagent-driven-development` (best quality gates)
- Tightly coupled multi-step flows → `executing-plans`
- Independent bulk tasks → `dispatching-parallel-agents`

Each individual task inside any execution mode should follow `test-driven-development`.

---

## Isolation: Set Up a Worktree First

Before starting non-trivial implementation work, create an isolated git worktree so the main checkout stays clean. This is especially important in skaile-dev because changes often span both the shell repo and a submodule.

```
  starting a new feature?
          │
          ▼
  using-git-worktrees        ← creates an isolated workspace
          │
          ▼
  [brainstorming → writing-plans → execution mode]
          │
          ▼
  finishing-a-development-branch  ← merge back, clean up worktree
```

When the work spans shell repo + submodule:
1. Commit the submodule changes first (inside the submodule worktree)
2. Bump the submodule pointer in the shell repo (one additional commit)
3. `finishing-a-development-branch` handles both steps

---

## Review Loop Detail

```
  implementation complete
          │
          ▼
  verification-before-completion
  ┌───────────────────────────────────┐
  │  checks:                          │
  │  • bun x --bun vitest run         │
  │  • bun run format                 │
  │  • quality mode=quick             │
  │  • no regressions                 │
  └───────────────┬───────────────────┘
          │               │
        pass            fail
          │               │
          │         fix → re-verify
          ▼
  requesting-code-review
  ┌─────────────────────────────────────┐
  │  self-review first, then:           │
  │  • small/low-risk → inline review   │
  │  • large/high-risk → escalate to    │
  │    skaile audit (scope=diff or      │
  │    scope=package)                   │
  └───────────────┬─────────────────────┘
          │
          ▼
  receiving-code-review
  ┌────────────────────────────────────┐
  │  for each issue:                   │
  │  1. acknowledge                    │
  │  2. fix (use TDD)                  │
  │  3. commit with skaile commit-spec │
  │  4. respond with resolution        │
  └───────────────┬────────────────────┘
          │
          ▼
  finishing-a-development-branch
  ┌──────────────────────────────────────┐
  │  option A — merge locally            │
  │  option B — open PR                  │
  │  option C — keep branch open         │
  │  option D — discard                  │
  └──────────────────────────────────────┘
```

---

## Common Quick Paths

### New feature
```
using-skaile-powers → brainstorming → writing-plans
  → using-git-worktrees (set up worktree)
  → subagent-driven-development (execute plan)
  → verification-before-completion
  → finishing-a-development-branch
```

### Bug fix
```
using-skaile-powers → systematic-debugging (find root cause)
  → test-driven-development (write failing test, fix, pass)
  → verification-before-completion
  → finishing-a-development-branch
```

### Write a new skill
```
using-skaile-powers → brainstorming (design the skill)
  → writing-skills (author SKILL.md with skaile frontmatter)
  → verification-before-completion
```

### Execute an existing plan
```
using-skaile-powers → [pick execution mode]
  → subagent-driven-development  or  executing-plans
  → verification-before-completion
  → finishing-a-development-branch
```

---

## Skill Reference

| Skill | Group | When to Use |
|---|---|---|
| `using-skaile-powers` | meta | Session start — establishes available skills and invocation rules |
| `brainstorming` | planning | Before any creative/feature work — designs before code |
| `writing-plans` | planning | After brainstorming, before implementation |
| `writing-skills` | planning | When creating or editing skills in ai-assets |
| `systematic-debugging` | execution | On any bug, test failure, or unexpected behavior |
| `test-driven-development` | execution | When implementing features or fixing bugs |
| `executing-plans` | execution | Inline execution of a written plan with review checkpoints |
| `dispatching-parallel-agents` | execution | 2+ independent tasks that can run in parallel sessions |
| `subagent-driven-development` | execution | Executing a plan with fresh subagents per task + two-stage review |
| `using-git-worktrees` | execution | Before starting isolated feature work (submodule-aware) |
| `requesting-code-review` | review | After completing tasks or features |
| `receiving-code-review` | review | When responding to code review feedback |
| `verification-before-completion` | review | Before claiming work is complete |
| `finishing-a-development-branch` | review | When implementation is fully reviewed and ready to land |

---

## Building Blocks

| Path | Skill | Purpose |
|---|---|---|
| `references/config.md` | — | Central config: artifact paths, conventions, upstream version, skill inventory |
| `skills/meta/using-skaile-powers/` | `using-skaile-powers` | Session-start skill; replaces `using-superpowers` |
| `skills/planning/brainstorming/` | `brainstorming` | Design before implementation; spec saved to `docs/devlog/specs/` |
| `skills/planning/writing-plans/` | `writing-plans` | Implementation plan authoring; plan saved to `docs/devlog/plans/` |
| `skills/planning/writing-skills/` | `writing-skills` | New skill authoring (skaile SKILL.md frontmatter) |
| `skills/execution/systematic-debugging/` | `systematic-debugging` | Root-cause-first debugging |
| `skills/execution/test-driven-development/` | `test-driven-development` | TDD with Vitest + Playwright (no Jest) |
| `skills/execution/executing-plans/` | `executing-plans` | Inline plan execution with checkpoints |
| `skills/execution/dispatching-parallel-agents/` | `dispatching-parallel-agents` | Parallel agent dispatch |
| `skills/execution/subagent-driven-development/` | `subagent-driven-development` | Subagent-per-task execution with spec + quality review |
| `skills/execution/using-git-worktrees/` | `using-git-worktrees` | Isolated worktree setup (submodule-aware) |
| `skills/review/requesting-code-review/` | `requesting-code-review` | Code review dispatch (escalates to skaile `audit`) |
| `skills/review/receiving-code-review/` | `receiving-code-review` | Code review response |
| `skills/review/verification-before-completion/` | `verification-before-completion` | Verification gate with skaile quality gates |
| `skills/review/finishing-a-development-branch/` | `finishing-a-development-branch` | Branch completion: merge/PR/keep/discard (submodule bump included) |

---

## Central Config

All 14 skills read `skaile-powers/references/config.md` at the start of every invocation. This file is the single source of truth for:

- Upstream fork version
- Artifact paths (`docs/devlog/specs/`, `docs/devlog/plans/`)
- Monorepo conventions (Bun, `bun x --bun vitest run`, Biome, skaile commit-spec)
- Flat skill inventory table

To change a path or convention: edit `references/config.md` only. All skills pick it up automatically.

---

## Upstream Sync

Forked from `obra/superpowers v5.0.6`. To sync with upstream:

1. Check upstream release notes for the relevant skill
2. `diff` the adapted SKILL.md against the new upstream version
3. Cherry-pick changes that don't conflict with skaile adaptations
4. Update the `upstream` field in this DOMAIN.md and in `references/config.md`

---

## Relationship to Other Domains

- **superpowers plugin** (`obra/superpowers`): Generic predecessor. During migration, both coexist; `skaile-powers:*` takes priority, superpowers is the fallback for anything not yet in this domain.
- **skaile-development**: Implementation workflow skills (`implement`, `git`, `audit`, `devlog`). `skaile-powers` provides the meta-workflow layer — how to think about, plan, and review work. The two domains complement each other: skaile-powers for workflow orchestration, skaile-development for monorepo-specific operations.
