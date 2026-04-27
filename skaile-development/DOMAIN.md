---
name: skaile-development
description: "Day-to-day development workflow for the skaile-dev monorepo — implementing changes, structured commits, git operations, documentation sync, test setup/generation/execution, code-quality audits, readiness gates, and development log maintenance."
type: domain
building_blocks:
  agents: "skaile-development — routes tasks to the right skills, maintains session context."
  skills: "skaile-dev-git, skaile-dev-implement, skaile-dev-design-spec, skaile-dev-review-diff, skaile-dev-test, skaile-dev-test-plan, skaile-dev-test-unit, skaile-dev-test-integration, skaile-dev-test-e2e, skaile-dev-platform-e2e, skaile-dev-code-audit, skaile-dev-release-check, skaile-dev-docs-xref, skaile-dev-skill-validators, skaile-dev-quality-gate, skaile-dev-docs, skaile-dev-devlog, skaile-dev-notify, skaile-dev-faq, skaile-dev-release, skaile-dev-kill-backend, skaile-dev-session-retro, skaile-dev-session-optimize, skaile-dev-verify-ui"
  references: "Branch naming, worktree patterns, test stack map, audit checklists, readiness criteria, devlog entry formats, documentation tier roles, commit spec."
stage: beta
---

# Skaile Development

Development workflow tools for working **on** the skaile-dev monorepo itself. These skills are
for contributors implementing new features, fixing bugs, adding AI skills/domains, and maintaining
the codebase — as opposed to `skaileup-implementation`, which is for building apps *using* skaile.

The domain is self-contained: it does **not** depend on any `skailup-*` skills at runtime.
Code-audit, test-planning, readiness-gate, and sync functionality that historically lived in
`skaileup-evaluate` has been copied and adapted into local skills here, tuned to the monorepo's
actual tech stacks (Bun, Nuxt 4, NestJS, React, drizzle-orm, Prisma, Vitest, Jest, Playwright).

## When to Use This Domain

Reach for `skaile-development` when you are:
- Implementing a feature or fix in a skaile-dev package (platform, forge-*, CLI, agent-*)
- Adding or modifying an AI skill or domain in `ai-assets/`
- Committing changes with structured metadata for the decision log
- Managing git branches, worktrees, or opening PRs
- Keeping documentation accurate after code changes
- Running, planning, generating, or scaffolding tests for any package
- Auditing code quality (build, lint, typecheck, tests, security, UI/UX)
- Checking release readiness for one package or the whole monorepo
- Recording what changed and why in the development log

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `agents/` | `skaile-development` — routes to skills, orchestrates multi-step work |
| `skills/git/` | `skaile-dev-git` — unified git operations: commit, branch, worktree, PR, finish, sync |
| `skills/proposal/` | `skaile-dev-design-spec` — design specs and proposals with structured frontmatter, review tracking, and alternatives analysis |
| `skills/review/` | `skaile-dev-review-diff` — local code review for staged/committed changes (quality, security, scope) |
| `skills/implement/` | `skaile-dev-implement` — monorepo-aware implementation orchestrator |
| `skills/test/` | `skaile-dev-test` — test runner: executes suites across the monorepo; triages failures |
| `skills/test-plan/` | `skaile-dev-test-plan` — per-package test plan from CLAUDE.md + source tree (feeds test-unit/-integration/-e2e) |
| `skills/test-unit/` | `skaile-dev-test-unit` — unit test setup + generation (vitest/jest-aware, per package) |
| `skills/test-integration/` | `skaile-dev-test-integration` — integration test setup + generation (SQLite/drizzle, Postgres/Prisma, temp-dir) |
| `skills/test-e2e/` | `skaile-dev-test-e2e` — E2E test setup + generation (Playwright for web apps; shell harness for CLIs) |
| `skills/e2e-platform/` | `skaile-dev-platform-e2e` — platform-specific E2E test setup + generation |
| `skills/audit/` | `skaile-dev-code-audit` — code quality audit: build + tests + 3 parallel sub-agents (logic/security/UI-UX) |
| `skills/ready/` | `skaile-dev-release-check` — pre-release readiness gate: per-package criteria table, blocker/warning report |
| `skills/sync-docs/` | `skaile-dev-docs-xref` — cross-document reference repair across README/CLAUDE/docs/DOMAIN with diff-before-apply |
| `skills/compile-validators/` | `skaile-dev-skill-validators` — compile MUST/NEVER/CHECKLIST rules from skaile-development SKILL.md into deterministic validators |
| `skills/quality/` | `skaile-dev-quality-gate` — umbrella coordinator: test → audit → doc-audit → ready, aggregated snapshot |
| `skills/doc/` | `skaile-dev-docs` — comprehensive documentation skill (write, update, audit, status) for all 5 doc tiers |
| `skills/devlog/` | `skaile-dev-devlog` — human-readable development log with plain-language entries and detailed reports |
| `skills/notify/` | `skaile-dev-notify` — team notifications and messaging with structured templates |
| `skills/faq/` | `skaile-dev-faq` — FAQ curation for all monorepo packages |
| `skills/release/` | `skaile-dev-release` — changelog, semantic versioning, and git tagging |
| `skills/kill-backend/` | `skaile-dev-kill-backend` — kills the platform backend process chain cleanly (port 3001, nest, bun, dotenvx) |
| `skills/session-review/` | `skaile-dev-session-retro` — end-of-session analysis: token usage, cost, workflow adherence, optimization tips |
| `skills/session-analysis/` | `skaile-dev-session-optimize` — session pattern analysis and workflow optimization recommendations |
| `skills/verify-ui/` | `skaile-dev-verify-ui` — UI verification and visual regression checks |
| `references/doc_tiers.md` | Five-tier documentation role table |
| `references/test_stack_map.md` | Package → framework + run-command map (authoritative) |
| `references/audit_checklists.md` | Logic / security / UI-UX checklists per sub-agent |
| `references/readiness_criteria.md` | Per-package and global readiness criteria |

## Skills

| Skill | When to Use |
|-------|------------|
| `skaile-dev-git` | Any git operation — committing, branching, worktrees, opening PRs, finishing branches, syncing |
| `skaile-dev-design-spec` | Creating, reviewing, or updating design specs for new features or architectural changes |
| `skaile-dev-review-diff` | Before committing or pushing — local quality/security review of changes |
| `skaile-dev-implement` | Starting any non-trivial implementation task in the monorepo |
| `skaile-dev-test` | Running an existing test suite and triaging failures |
| `skaile-dev-test-plan` | Generating or refreshing a per-package `TEST_PLAN.md` |
| `skaile-dev-test-unit` | Setting up unit-test infra and generating unit tests for one package |
| `skaile-dev-test-integration` | Setting up integration-test infra (DB/temp-dir isolation) and generating tests |
| `skaile-dev-test-e2e` | Setting up Playwright (web) or CLI harness (agent-framework/cli) and generating journeys |
| `skaile-dev-platform-e2e` | Platform-specific E2E test setup and generation |
| `skaile-dev-code-audit` | Deep code-quality audit: build + tests + parallel logic/security/UI-UX sub-agents |
| `skaile-dev-release-check` | Pre-release readiness gate; called by `skaile-dev-release` as Phase 0 |
| `skaile-dev-docs-xref` | Repairing broken cross-references across doc tiers (diff first, apply on approval) |
| `skaile-dev-skill-validators` | Generating deterministic Python validators for skaile-development SKILL.md files |
| `skaile-dev-quality-gate` | Umbrella gate: test → audit → doc-audit → ready, with one aggregated snapshot |
| `skaile-dev-docs` | Write, update, audit, or check status of documentation across all tiers |
| `skaile-dev-devlog` | After completing work — records what changed and why in plain language |
| `skaile-dev-notify` | Post messages, announcements, or devlog summaries to team channels |
| `skaile-dev-faq` | After resolving a question — curates FAQ entries with user approval |
| `skaile-dev-release` | Preparing a release — changelog generation, version bumps, and git tagging |
| `skaile-dev-kill-backend` | Killing the platform backend and its full process chain (port 3001, nest, bun, dotenvx) |
| `skaile-dev-session-retro` | At the end of an implementation session — token cost, workflow grade, and improvement tips |
| `skaile-dev-session-optimize` | Session pattern analysis and workflow optimization recommendations |
| `skaile-dev-verify-ui` | UI verification and visual regression checks |

## Quality Gates

The domain now exposes a layered quality pipeline, each step callable standalone or via the `skaile-dev-quality-gate` umbrella:

```
 ┌──────────────┐    ┌──────────────────────────────┐    ┌──────────────────────────┐    ┌─────────────┐
 │ 1. skaile-dev-test      │ ─▶ │ 2. skaile-dev-code-audit             │ ─▶ │ 3. skaile-dev-docs --mode audit  │ ─▶ │ 4. skaile-dev-release-check    │
 │ runs suites             │    │ build + lint + typecheck +           │    │ doc coverage + drift             │    │ per-package criteria           │
 │                         │    │ 3 parallel sub-agents                │    │                                  │    │                                │
 └─────────────────────────┘    └──────────────────────────────────────┘    └──────────────────────────────────┘    └────────────────────────────────┘
                                                                                                                                    │
                                                                                                                                    ▼
                                                                                               all artifacts aggregated by `skaile-dev-quality-gate`
```

Integration points:
- `skaile-dev-implement` runs `skaile-dev-test` and `skaile-dev-code-audit scope=diff` after implementation, before docs sync
- `skaile-dev-release` runs `skaile-dev-release-check` as Phase 0 and `skaile-dev-code-audit scope=full` before tagging
- `skaile-dev-review-diff` can escalate to `skaile-dev-code-audit scope=package` for large or high-risk diffs
- Scheduled CI can run `skaile-dev-quality-gate mode=full` weekly on main

## Commit Workflow

The `git mode=commit` operation is the primary way to commit in skaile-dev. It reads the diff,
identifies affected packages, and generates a structured message with:
- Conventional-commits title line
- Human description (what and why)
- `---agent---` YAML block with scope, type, breaking, changes, decisions, migrate, exports

The format spec lives in `skills/git/references/commit-spec.md`. The `.githooks/commit-msg`
hook validates or generates the block on merges to main. A GitHub Action validates on PRs.

The `skaile-dev-git` skill handles branch/worktree/PR/finish/sync operations and also covers commit
message generation — all unified under a single skill with mode selection.

In commit mode, the `skaile-dev-git` skill offers an optional review step (via the `skaile-dev-review-diff` skill) before
committing. This defaults to on when committing to main, and can be skipped with an explicit "no".

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

The `skaile-dev-docs` skill covers tiers 1-4. The `skaile-dev-devlog` skill handles the dev log tier.
The `skaile-dev-docs-xref` skill repairs cross-references between tiers.

## Test Layer Map

| Layer | Skill | Used for |
|---|---|---|
| Unit | `skaile-dev-test-unit` | Pure-logic modules: composables, utilities, validators, flow-engine transitions, manifest parsers |
| Integration | `skaile-dev-test-integration` | API routes (forge server/api, NestJS controllers), DB repositories, agent-framework runner/session/bridge lifecycle |
| E2E | `skaile-dev-test-e2e` | Full user journeys (forge apps, platform frontend via platform/e2e), CLI end-to-end for agent-framework/cli |
| Platform E2E | `skaile-dev-platform-e2e` | Platform-specific full-stack journeys |

`skaile-dev-test-plan` generates the layered plan; the setup skills consume it.
`skaile-dev-test` runs any existing suite and triages failures.

## Relationship to Other Domains

- **skaileup-implementation**: Builds apps *using* skaile (consumes `_concept/`). This domain is for the skaile codebase itself.
- **skaileup-evaluate**: Full QA pipeline for generated apps (concept-driven). `skaile-development` borrows its *patterns* (sub-agent audits, readiness gates, sync) but does **not** depend on any skailup-* skill at runtime — all functionality is copied locally and adapted to the monorepo.
- **skaileup-shared**: Shared contracts referenced by all domains. `skaile-dev-skill-validators` reads `scripts/validator_lib.py` from here.

## Notes

- The `skaile-development` agent orchestrates multi-skill workflows. For single-skill operations, call skills directly.
- `skaile-dev-devlog` should run after any meaningful change. `skaile-dev-implement` triggers it automatically.
- Skills in this domain read `CLAUDE.md` files from affected packages before acting.
- The four quality skills (`skaile-dev-test`, `skaile-dev-code-audit`, `skaile-dev-docs --mode audit`, `skaile-dev-release-check`) all emit a JSON artifact to `_devlog/reports/` so `skaile-dev-quality-gate` can aggregate them.
