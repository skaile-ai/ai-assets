---
name: skaileup-implementation
description: "Implementation pipeline: project scaffolding, feature implementation (TDD), database migrations, seed data, and utility workflows — consuming _concept/ artifacts produced by skaileup-conceptualization."
type: domain
building_blocks:
  contracts: "CF and Saxe contracts for implementation structure, acceptance criteria, git workflow, and verification protocols."
  docs: "Implementation workflow guides, TDD patterns, and migration conventions."
  skills: "Numbered skill groups (00–utilities) covering orchestration, project setup, feature implementation, and utilities."
  agents: "agents/implementation/ (GitAgent spec v0.1.0)"
  prompts: "TBD"
  tools: "TBD"
stage: alpha
---

# Dev Implementation

This domain translates `_concept/` artifacts into running code. It covers the full implementation lifecycle: scaffolding a new project, implementing features test-first, applying database migrations, and generating seed data. Verification gates (readiness checks, E2E tests, audits) live in `skaileup-evaluate`.

Skills consume the output of `skaileup-conceptualization` and produce committed, tested code. CF and Saxe variants coexist under their respective subdirectories until merged.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Shared contracts — acceptance criteria, git workflow, implementation structure, skill grammar, prerequisites, verification protocols |
| `docs/` | Implementation workflow guides, TDD patterns, migration conventions |
| `skills/` | Numbered skill groups (see below) |
| `flows/` | Multi-step implementation flow definitions |

## Skill Groups

| Group | Name | Produces |
|-------|------|----------|
| `00_orchestrator/` | Pipeline controller | Dispatches implementation skills end-to-end |
| `10_setup/` | Project foundation | Scaffolded project, infrastructure, base configuration |
| `20_features/` | Feature implementation | TDD-driven feature code committed to the repo |
| `utilities/` | Utility workflows | Database migrations (`cf_migrate`), seed data (`cf_seed`), scaffolding helpers (`cf_scaffold`) |

## Conventions

- Implementation skills search for `skailup-prog-expert-*` skills for tech-stack-specific guidance
- TDD is the default: tests written before implementation code
- Migrations are always reversible; seed data follows scenario conventions from `_concept/06_datamodel/seed.json`
- CF and Saxe variants coexist until merged; merged skills use `source: MERGED`
