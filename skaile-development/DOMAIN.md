---
name: skaile-development
description: "Day-to-day development workflow for the skaile-dev monorepo — implementing changes, structured commits, git operations, documentation sync, test management, and development log maintenance."
type: domain
building_blocks:
  agents: "skaile-workspace-advisor — routes tasks to the right skills, maintains session context."
  skills: "skaildev-implement, skaildev-git-workflow, commit-message, skaildev-doc, skaildev-update-docs, skaildev-run-tests, skaildev-devlog, skaildev-mattermost"
  references: "Branch naming, worktree patterns, test runner map, devlog entry formats, documentation tier roles, commit spec."
stage: beta
---

# Skaile Development

Development workflow tools for working **on** the skaile-dev monorepo itself. These skills are
for contributors implementing new features, fixing bugs, adding AI skills/domains, and maintaining
the codebase — as opposed to `dev-implementation`, which is for building apps *using* skaile.

## When to Use This Domain

Reach for `skaile-development` when you are:
- Implementing a feature or fix in a skaile-dev package (platform, forge-*, CLI, agent-*)
- Adding or modifying an AI skill or domain in `ai-resources/`
- Committing changes with structured metadata for the decision log
- Managing git branches, worktrees, or opening PRs
- Keeping documentation accurate after code changes
- Running or writing tests for a specific package
- Recording what changed and why in the development log

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `agents/` | `skaile-workspace-advisor` — routes to skills, orchestrates multi-step work |
| `skills/commit-message/` | Structured commit messages with agent-readable `---agent---` metadata |
| `skills/skaildev-implement/` | Monorepo-aware implementation orchestrator |
| `skills/skaildev-git-workflow/` | Branch, worktree, PR, and finish-branch management |
| `skills/skaildev-doc/` | Comprehensive documentation skill (write, update, audit, status) for all 5 doc tiers |
| `skills/skaildev-update-docs/` | ~~DEPRECATED~~ — use `skaildev-doc --mode update` |
| `skills/skaildev-run-tests/` | Test construction and execution across the monorepo |
| `skills/skaildev-devlog/` | Human-readable development log with plain-language entries and detailed reports |
| `skills/skaildev-mattermost/` | Post messages, announcements, and devlog summaries to Mattermost |
| `references/doc_tiers.md` | Clarifies the role of README.md, CLAUDE.md, Starlight docs, and _devlog |

## Skills

| Skill | When to Use |
|-------|------------|
| `commit-message` | Writing any commit — generates structured message with `---agent---` block |
| `skaildev-implement` | Starting any non-trivial implementation task in the monorepo |
| `skaildev-git-workflow` | Creating branches/worktrees, opening PRs, finishing branches |
| `skaildev-doc` | Write, update, audit, or check status of documentation across all tiers |
| `skaildev-update-docs` | DEPRECATED — use `skaildev-doc --mode update` |
| `skaildev-run-tests` | Constructing new tests or running/debugging existing ones |
| `skaildev-devlog` | After completing work — records what changed and why in plain language |
| `skaildev-mattermost` | Post messages, announcements, or devlog summaries to Mattermost channels |

## Commit Workflow

The `commit-message` skill is the primary way to commit in skaile-dev. It reads the diff,
identifies affected packages, and generates a structured message with:
- Conventional-commits title line
- Human description (what and why)
- `---agent---` YAML block with scope, type, breaking, changes, decisions, migrate, exports

The format spec lives in `skills/commit-message/commit-spec.md`. The `.githooks/commit-msg`
hook validates or generates the block on merges to main. A GitHub Action validates on PRs.

`skaildev-git-workflow` handles branch/worktree/PR/finish operations but delegates commit
message generation to `commit-message`.

## Documentation Tiers

Every change in skaile-dev may affect up to five documentation surfaces. See
`references/doc_tiers.md` for the full decision table. Short version:

| Tier | File | When to update |
|------|------|---------------|
| User docs | `README.md` | Public API, commands, or usage changes |
| Dev guide | `CLAUDE.md` | Architecture, conventions, or environment changes |
| Reference | `docs/<pkg>/` | New commands, configuration options, component API |
| AI guide | `DOMAIN.md` / `SKILL.md` | Changes to AI resource structure or skill behavior |
| Dev log | `_devlog/DEVLOG.md` | After every meaningful change (always) |

## Relationship to Other Domains

- **dev-implementation**: Builds apps *using* skaile (consumes `_concept/`). This domain is for the skaile codebase itself.
- **dev-quality**: Full QA pipeline (audit, E2E, readiness gates). `run-tests` here is the fast inner-loop equivalent.
- **dev-shared**: Shared contracts referenced by all domains. Commit spec now lives in this domain.

## Notes

- The `skaile-workspace-advisor` agent orchestrates multi-skill workflows. For single-skill operations, call skills directly.
- `devlog` should run after any meaningful change. `implement` triggers it automatically.
- Skills in this domain read `CLAUDE.md` files from affected packages before acting.
