---
name: skaile-development
description: "Day-to-day development workflow for the skaile-dev monorepo — implementing changes, structured commits, git operations, documentation sync, test management, and development log maintenance."
type: domain
building_blocks:
  agents: "skaile-development — routes tasks to the right skills, maintains session context."
  skills: "git, implement, proposal, test, doc, devlog, notify, faq, release"
  references: "Branch naming, worktree patterns, test runner map, devlog entry formats, documentation tier roles, commit spec."
stage: beta
---

# Skaile Development

Development workflow tools for working **on** the skaile-dev monorepo itself. These skills are
for contributors implementing new features, fixing bugs, adding AI skills/domains, and maintaining
the codebase — as opposed to `skaileup-implementation`, which is for building apps *using* skaile.

## When to Use This Domain

Reach for `skaile-development` when you are:
- Implementing a feature or fix in a skaile-dev package (platform, forge-*, CLI, agent-*)
- Adding or modifying an AI skill or domain in `ai-assets/`
- Committing changes with structured metadata for the decision log
- Managing git branches, worktrees, or opening PRs
- Keeping documentation accurate after code changes
- Running or writing tests for a specific package
- Recording what changed and why in the development log

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `agents/` | `skaile-development` — routes to skills, orchestrates multi-step work |
| `skills/git/` | Unified git operations: commit, branch, worktree, PR, finish, sync |
| `skills/proposal/` | Design specs and proposals with structured frontmatter, review tracking, and alternatives analysis |
| `skills/implement/` | Monorepo-aware implementation orchestrator |
| `skills/test/` | Test construction and execution across the monorepo |
| `skills/doc/` | Comprehensive documentation skill (write, update, audit, status) for all 5 doc tiers |
| `skills/devlog/` | Human-readable development log with plain-language entries and detailed reports |
| `skills/notify/` | Team notifications and messaging with structured templates |
| `skills/faq/` | FAQ curation for all monorepo packages |
| `skills/release/` | Changelog, semantic versioning, and git tagging |
| `references/doc_tiers.md` | Clarifies the role of README.md, CLAUDE.md, Starlight docs, and _devlog |

## Skills

| Skill | When to Use |
|-------|------------|
| `git` | Any git operation — committing, branching, worktrees, opening PRs, finishing branches, syncing |
| `proposal` | Creating, reviewing, or updating design specs for new features or architectural changes |
| `implement` | Starting any non-trivial implementation task in the monorepo |
| `test` | Constructing new tests or running/debugging existing ones |
| `doc` | Write, update, audit, or check status of documentation across all tiers |
| `devlog` | After completing work — records what changed and why in plain language |
| `notify` | Post messages, announcements, or devlog summaries to team channels |
| `faq` | After resolving a question — curates FAQ entries with user approval |
| `release` | Preparing a release — changelog generation, version bumps, and git tagging |

## Commit Workflow

The `git mode=commit` operation is the primary way to commit in skaile-dev. It reads the diff,
identifies affected packages, and generates a structured message with:
- Conventional-commits title line
- Human description (what and why)
- `---agent---` YAML block with scope, type, breaking, changes, decisions, migrate, exports

The format spec lives in `skills/git/references/commit-spec.md`. The `.githooks/commit-msg`
hook validates or generates the block on merges to main. A GitHub Action validates on PRs.

The `git` skill handles branch/worktree/PR/finish/sync operations and also covers commit
message generation — all unified under a single skill with mode selection.

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

The `doc` skill covers tiers 1-4. The `devlog` skill handles the dev log tier.

## Relationship to Other Domains

- **skaileup-implementation**: Builds apps *using* skaile (consumes `_concept/`). This domain is for the skaile codebase itself.
- **skaileup-evaluate**: Full QA pipeline (audit, E2E, readiness gates). `test` here is the fast inner-loop equivalent.
- **skaileup-shared**: Shared contracts referenced by all domains. Commit spec now lives in this domain.

## Notes

- The `skaile-development` agent orchestrates multi-skill workflows. For single-skill operations, call skills directly.
- `devlog` should run after any meaningful change. `implement` triggers it automatically.
- Skills in this domain read `CLAUDE.md` files from affected packages before acting.
