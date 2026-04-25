---
name: skaileup-implementation
description: "Implementation pipeline: project scaffolding, feature implementation (TDD), database migrations, seed data, and utility workflows — consuming _concept/ artifacts produced by skaileup-conceptualization."
type: domain
building_blocks:
  contracts: "Contracts for implementation structure, acceptance criteria, git workflow, and verification protocols."
  docs: "Implementation workflow guides, TDD patterns, and migration conventions."
  skills: "Setup skill group, top-level implement and orchestrate skills, utilities skill group, and a Starlight docs updater."
  agents: "agents/implementation/ (GitAgent spec v0.1.0)"
  flows: "Multi-step implementation flow definitions."
  tools: "n/a"
stage: alpha
---

# Dev Implementation

This domain translates `_concept/` artifacts into running code. It covers the full implementation lifecycle: scaffolding a new project, implementing features test-first, applying database migrations, and generating seed data. Verification gates (readiness checks, E2E tests, audits) live in `skaileup-evaluate`.

Skills consume the output of `skaileup-conceptualization` and produce committed, tested code. All skills are unified — there are no per-variant subdirectories.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Shared contracts — acceptance criteria, git workflow, implementation structure, skill grammar, prerequisites, verification protocols |
| `docs/` | Implementation workflow guides, TDD patterns, migration conventions |
| `skills/` | All implementation skills (see below) |
| `flows/` | Multi-step implementation flow definitions |
| `agents/` | Agent specs for orchestrated runs |

## Skills

| Skill | Name | Produces |
|-------|------|----------|
| `skills/skailup-implement/` | Pipeline controller | Dispatches implementation skills end-to-end |
| `skills/setup/skailup-foundation/` | Project foundation | Scaffolded project with base configuration |
| `skills/setup/skailup-infrastructure/` | Infrastructure setup | CI, Docker, environment configuration |
| `skills/setup/skailup-scaffold/` | Code scaffolding | Boilerplate structure for a new feature area |
| `skills/skailup-implement-feature/` | Feature implementation | TDD-driven feature code committed to the repo |
| `skills/skailup-implement-feature/skailup-implement-feature-page/` | Page implementation | Single-page feature implementation |
| `skills/skailup-update-starlight-docs/` | Starlight docs update | Keeps Starlight documentation in sync with code changes |
| `skills/utilities/skailup-migrate/` | Database migrations | Reversible schema migrations |
| `skills/utilities/skailup-seed/` | Seed data | Scenario seed data from `_concept/06_datamodel/seed.json` |
| `skills/utilities/skailup-generate/` | Code generation | Utility code generation helpers |

## Conventions

- Implementation skills search for `skailup-prog-expert-*` skills for tech-stack-specific guidance
- TDD is the default: tests written before implementation code
- Migrations are always reversible; seed data follows scenario conventions from `_concept/06_datamodel/seed.json`
