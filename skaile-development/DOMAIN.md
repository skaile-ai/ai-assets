---
name: skaile-development
description: 'Day-to-day development workflow for the skaile-dev monorepo — implementing changes, structured commits, git operations, documentation sync, test setup/generation/execution, code-quality audits, readiness gates, and development log maintenance.'
type: domain
building_blocks:
  agents: 'skaile-development — routes tasks to the right skills, maintains session context.'
  skills: 'git, implement, proposal, review, test, test-plan, test-unit, test-integration, test-e2e, e2e-platform, audit, ready, sync-docs, compile-validators, quality, doc, devlog, notify, faq, release, kill-backend, jenkins-debug, session-review, session-analysis, verify-ui'
  references: 'Branch naming, worktree patterns, test stack map, audit checklists, readiness criteria, devlog entry formats, documentation tier roles, commit spec.'
stage: beta
---

# Skaile Development

Development workflow tools for working **on** the skaile-dev monorepo itself. These skills are
for contributors implementing new features, fixing bugs, adding AI skills/domains, and maintaining
the codebase — as opposed to `skaileup-implementation`, which is for building apps _using_ skaile.

The domain is self-contained: it does **not** depend on any `skaileup-*` skills at runtime.
Code-audit, test-planning, readiness-gate, and sync functionality that historically lived in
`skaileup-evaluate` has been copied and adapted into local skills here, tuned to the monorepo's
actual tech stacks (Bun, Nuxt 4, NestJS, React, drizzle-orm, Prisma, Vitest, Jest, Playwright).

## When to Use This Domain

Reach for `skaile-development` when you are:

- Implementing a feature or fix in a skaile-dev package (platform, forge-_, CLI, agent-_)
- Adding or modifying an AI skill or domain in `ai-assets/`
- Committing changes with structured metadata for the decision log
- Managing git branches, worktrees, or opening PRs
- Keeping documentation accurate after code changes
- Running, planning, generating, or scaffolding tests for any package
- Auditing code quality (build, lint, typecheck, tests, security, UI/UX)
- Checking release readiness for one package or the whole monorepo
- Recording what changed and why in the development log

## Building Blocks

| Folder                             | Purpose                                                                                                                           |
| ---------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `agents/`                          | `skaile-development` — routes to skills, orchestrates multi-step work                                                             |
| `skills/git/`                      | `git` — unified git operations: commit, branch, worktree, PR, finish, sync                                             |
| `skills/proposal/`                 | `proposal` — design specs and proposals with structured frontmatter, review tracking, and alternatives analysis     |
| `skills/review/`                   | `review` — local code review for staged/committed changes (quality, security, scope)                              |
| `skills/implement/`                | `implement` — monorepo-aware implementation orchestrator                                                               |
| `skills/test/`                     | `test` — test runner: executes suites across the monorepo; triages failures                                            |
| `skills/test-plan/`                | `test-plan` — per-package test plan from CLAUDE.md + source tree (feeds test-unit/-integration/-e2e)                   |
| `skills/test-unit/`                | `test-unit` — unit test setup + generation (vitest/jest-aware, per package)                                            |
| `skills/test-integration/`         | `test-integration` — integration test setup + generation (SQLite/drizzle, Postgres/Prisma, temp-dir)                   |
| `skills/test-e2e/`                 | `test-e2e` — E2E test setup + generation (Playwright for web apps; shell harness for CLIs)                             |
| `skills/e2e-platform/`             | `e2e-platform` — platform-specific E2E test setup + generation                                                         |
| `skills/audit/`                    | `audit` — code quality audit: build + tests + 3 parallel sub-agents (logic/security/UI-UX)                        |
| `skills/ready/`                    | `ready` — pre-release readiness gate: per-package criteria table, blocker/warning report                       |
| `skills/sync-docs/`                | `sync-docs` — cross-document reference repair across README/CLAUDE/docs/DOMAIN with diff-before-apply                  |
| `skills/compile-validators/`       | `compile-validators` — compile MUST/NEVER/CHECKLIST rules from skaile-development SKILL.md into deterministic validators |
| `skills/quality/`                  | `quality` — umbrella coordinator: test → audit → doc-audit → ready, aggregated snapshot                           |
| `skills/doc/`                      | `doc` — comprehensive documentation skill (write, update, audit, status) for all 5 doc tiers                          |
| `skills/devlog/`                   | `devlog` — human-readable development log with plain-language entries and detailed reports                             |
| `skills/notify/`                   | `notify` — team notifications and messaging with structured templates                                                  |
| `skills/faq/`                      | `faq` — FAQ curation for all monorepo packages                                                                         |
| `skills/release/`                  | `release` — changelog, semantic versioning, and git tagging                                                            |
| `skills/kill-backend/`             | `kill-backend` — kills the platform backend process chain cleanly (port 3001, nest, bun, dotenvx)                      |
| `skills/jenkins-debug/`            | `jenkins-debug` — read-only Jenkins debug for failed `skaile_platform` deploys via the local SSH tunnel                |
| `skills/session-review/`           | `session-review` — end-of-session analysis: token usage, cost, workflow adherence, optimization tips                    |
| `skills/session-analysis/`         | `session-analysis` — session pattern analysis and workflow optimization recommendations                                |
| `skills/verify-ui/`                | `verify-ui` — UI verification and visual regression checks                                                             |
| `references/doc_tiers.md`          | Five-tier documentation role table                                                                                                |
| `references/test_stack_map.md`     | Package → framework + run-command map (authoritative)                                                                             |
| `references/audit_checklists.md`   | Logic / security / UI-UX checklists per sub-agent                                                                                 |
| `references/readiness_criteria.md` | Per-package and global readiness criteria                                                                                         |

## Skills

| Skill                         | When to Use                                                                                    |
| ----------------------------- | ---------------------------------------------------------------------------------------------- |
| `git`              | Any git operation — committing, branching, worktrees, opening PRs, finishing branches, syncing |
| `proposal`      | Creating, reviewing, or updating design specs for new features or architectural changes        |
| `review`      | Before committing or pushing — local quality/security review of changes                        |
| `implement`        | Starting any non-trivial implementation task in the monorepo                                   |
| `test`             | Running an existing test suite and triaging failures                                           |
| `test-plan`        | Generating or refreshing a per-package `TEST_PLAN.md`                                          |
| `test-unit`        | Setting up unit-test infra and generating unit tests for one package                           |
| `test-integration` | Setting up integration-test infra (DB/temp-dir isolation) and generating tests                 |
| `test-e2e`         | Setting up Playwright (web) or CLI harness (agent-framework/cli) and generating journeys       |
| `e2e-platform`     | Platform-specific E2E test setup and generation                                                |
| `audit`       | Deep code-quality audit: build + tests + parallel logic/security/UI-UX sub-agents              |
| `ready`    | Pre-release readiness gate; called by `release` as Phase 0                          |
| `sync-docs`        | Repairing broken cross-references across doc tiers (diff first, apply on approval)             |
| `compile-validators` | Generating deterministic Python validators for skaile-development SKILL.md files               |
| `quality`     | Umbrella gate: test → audit → doc-audit → ready, with one aggregated snapshot                  |
| `doc`             | Write, update, audit, or check status of documentation across all tiers                        |
| `devlog`           | After completing work — records what changed and why in plain language                         |
| `notify`           | Post messages, announcements, or devlog summaries to team channels                             |
| `faq`              | After resolving a question — curates FAQ entries with user approval                            |
| `release`          | Preparing a release — changelog generation, version bumps, and git tagging                     |
| `kill-backend`     | Killing the platform backend and its full process chain (port 3001, nest, bun, dotenvx)        |
| `jenkins-debug`    | Debugging a failed `skaile_platform` Jenkins deploy via the local SSH tunnel (read-only)       |
| `session-review`    | At the end of an implementation session — token cost, workflow grade, and improvement tips     |
| `session-analysis` | Session pattern analysis and workflow optimization recommendations                             |
| `verify-ui`        | UI verification and visual regression checks                                                   |

## Quality Gates

The domain now exposes a layered quality pipeline, each step callable standalone or via the `quality` umbrella:

```
 ┌──────────────┐    ┌──────────────────────────────┐    ┌──────────────────────────┐    ┌─────────────┐
 │ 1. test      │ ─▶ │ 2. audit             │ ─▶ │ 3. doc --mode audit  │ ─▶ │ 4. ready    │
 │ runs suites             │    │ build + lint + typecheck +           │    │ doc coverage + drift             │    │ per-package criteria           │
 │                         │    │ 3 parallel sub-agents                │    │                                  │    │                                │
 └─────────────────────────┘    └──────────────────────────────────────┘    └──────────────────────────────────┘    └────────────────────────────────┘
                                                                                                                                    │
                                                                                                                                    ▼
                                                                                               all artifacts aggregated by `quality`
```

Integration points:

- `implement` runs `test` and `audit scope=diff` after implementation, before docs sync
- `release` runs `ready` as Phase 0 and `audit scope=full` before tagging
- `review` can escalate to `audit scope=package` for large or high-risk diffs
- Scheduled CI can run `quality mode=full` weekly on main

## Commit Workflow

The `git mode=commit` operation is the primary way to commit in skaile-dev. It reads the diff,
identifies affected packages, and generates a structured message with:

- Conventional-commits title line (`type(scope): description`)
- Human description (what and why, 1-3 sentences)

The format spec lives in `skills/git/references/commit-spec.md`. The `.githooks/commit-msg`
hook validates the title format on merges to main.

The `git` skill handles branch/worktree/PR/finish/sync operations and also covers commit
message generation - all unified under a single skill with mode selection.

In commit mode, the `git` skill offers an optional review step (via the `review` skill) before
committing. This defaults to on when committing to main, and can be skipped with an explicit "no".

## Documentation Tiers

Every change in skaile-dev may affect up to five documentation surfaces. See
`references/doc_tiers.md` for the full decision table. Short version:

| Tier      | File                     | When to update                                     |
| --------- | ------------------------ | -------------------------------------------------- |
| User docs | `README.md`              | Public API, commands, or usage changes             |
| Dev guide | `CLAUDE.md`              | Architecture, conventions, or environment changes  |
| Reference | `docs/<pkg>/`            | New commands, configuration options, component API |
| AI guide  | `DOMAIN.md` / `SKILL.md` | Changes to AI resource structure or skill behavior |
| Dev log   | `_devlog/DEVLOG.md`      | After every meaningful change (always)             |

The `doc` skill covers tiers 1-4. The `devlog` skill handles the dev log tier.
The `sync-docs` skill repairs cross-references between tiers.

## Test Layer Map

| Layer        | Skill                         | Used for                                                                                                            |
| ------------ | ----------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| Unit         | `test-unit`        | Pure-logic modules: composables, utilities, validators, flow-engine transitions, manifest parsers                   |
| Integration  | `test-integration` | API routes (forge server/api, NestJS controllers), DB repositories, agent-framework runner/session/bridge lifecycle |
| E2E          | `test-e2e`         | Full user journeys (forge apps, platform frontend via platform/e2e), CLI end-to-end for agent-framework/cli         |
| Platform E2E | `e2e-platform`     | Platform-specific full-stack journeys                                                                               |

`test-plan` generates the layered plan; the setup skills consume it.
`test` runs any existing suite and triages failures.

## Relationship to Other Domains

- **skaileup-implementation**: Builds apps _using_ skaile (consumes `_concept/`). This domain is for the skaile codebase itself.
- **skaileup-evaluate**: Full QA pipeline for generated apps (concept-driven). `skaile-development` borrows its _patterns_ (sub-agent audits, readiness gates, sync) but does **not** depend on any skaileup-\* skill at runtime — all functionality is copied locally and adapted to the monorepo.
- **skaileup-shared**: Shared contracts referenced by all domains. `compile-validators` reads `scripts/validator_lib.py` from here.

## Notes

- The `skaile-development` agent orchestrates multi-skill workflows. For single-skill operations, call skills directly.
- `devlog` should run after any meaningful change. `implement` triggers it automatically.
- Skills in this domain read `CLAUDE.md` files from affected packages before acting.
- The four quality skills (`test`, `audit`, `doc --mode audit`, `ready`) all emit a JSON artifact to `_devlog/reports/` so `quality` can aggregate them.
