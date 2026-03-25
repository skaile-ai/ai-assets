---
name: skaile-dev-ops
description: "Day-to-day development workflow for the skaile-dev monorepo — implementing changes, git operations, documentation sync, test management, and development log maintenance."
type: domain
building_blocks:
  agents: "skaile-workspace-advisor — routes tasks to the right skills, maintains session context."
  skills: "skaildev-implement, skaildev-git-workflow, skaildev-update-docs, skaildev-run-tests, skaildev-devlog"
  references: "Branch naming, worktree patterns, test runner map, devlog entry formats, documentation tier roles."
stage: beta
---

# Skaile Dev Ops

Development workflow tools for working **on** the skaile-dev monorepo itself. These skills are
for contributors implementing new features, fixing bugs, adding AI skills/domains, and maintaining
the codebase — as opposed to `dev-implementation`, which is for building apps *using* skaile.

## When to Use This Domain

Reach for `skaile-dev-ops` when you are:
- Implementing a feature or fix in a skaile-dev package (platform, forge-*, CLI, agent-*)
- Adding or modifying an AI skill or domain in `ai-resources/`
- Managing git branches, worktrees, or opening PRs
- Keeping documentation accurate after code changes
- Running or writing tests for a specific package
- Recording what changed and why in the development log

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `agents/` | `skaile-workspace-advisor` — routes to skills, orchestrates multi-step work |
| `skills/skaildev-implement/` | Monorepo-aware implementation orchestrator |
| `skills/skaildev-git-workflow/` | Branch, worktree, PR, and commit management |
| `skills/skaildev-update-docs/` | Post-implementation documentation sync (all 4 doc tiers + devlog) |
| `skills/skaildev-run-tests/` | Test construction and execution across the monorepo |
| `skills/skaildev-devlog/` | Human-readable development log with plain-language entries and detailed reports |
| `references/doc_tiers.md` | Clarifies the role of README.md, CLAUDE.md, Starlight docs, and _devlog |

## Skills

| Skill | When to Use |
|-------|------------|
| `skaildev-implement` | Starting any non-trivial implementation task in the monorepo |
| `skaildev-git-workflow` | Creating branches/worktrees, opening PRs, making clean commits |
| `skaildev-update-docs` | After any code change — syncs all affected doc tiers |
| `skaildev-run-tests` | Constructing new tests or running/debugging existing ones |
| `skaildev-devlog` | After completing work — records what changed and why in plain language |

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
- **dev-implementation-superpowers**: Generic supervised-dispatch workflow. `implement-skaile` uses its patterns but is pre-configured for skaile-dev conventions.
- **dev-quality**: Full QA pipeline (audit, E2E, readiness gates). `run-tests` here is the fast inner-loop equivalent — construct and run tests quickly during development.

## Notes

- The `skaile-workspace-advisor` agent orchestrates multi-skill workflows. For single-skill operations, call skills directly.
- `devlog` should run after any meaningful change. `implement-skaile` triggers it automatically.
- Skills in this domain read `CLAUDE.md` files from affected packages before acting.
