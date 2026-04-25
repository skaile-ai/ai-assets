---
name: skaileup-build
description: "Implementation pipeline — project scaffolding, TDD feature implementation, database migrations, seed data, docs sync, and supervised multi-step build workflows. Consumes _concept/ artifacts."
type: domain
building_blocks:
  contracts: "n/a — to be populated after skill migration."
  docs: "n/a — to be populated after skill migration."
  flows: "n/a — to be populated after skill migration."
  skills: "Scaffolding, foundation, feature implementation, page implementation, docs sync, migration, seed, code generation, and supervised build workflow skills."
  tools: "n/a"
stage: alpha
---

# skaileup-build

Implementation pipeline — project scaffolding, TDD feature implementation, database migrations, seed data, docs sync, and supervised multi-step build workflows. Consumes `_concept/` artifacts. Build skills translate the technical decisions captured in blueprints and experience specs into working code, following a TDD discipline at each step.

Skills will be moved into this domain during the architecture reorganization (Phase 5.2+).

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | Invocable skills (see below) |
| `flows/` | Supervised build flow definitions (moved from skaileup-implementation later) |
| `contracts/` | Domain-local contracts for implementation patterns (moved from skaileup-implementation later) |

## Skills (target)

| Skill | Purpose |
|-------|---------|
| `skailup-scaffold/` | Bootstraps a new project from the technology stack decision |
| `skailup-foundation/` | Establishes core infrastructure, auth, routing, and data access layers |
| `skailup-implement/` | Implements a feature end-to-end using TDD from a feature spec |
| `skailup-implement-feature/` | Implements a specific feature module within an existing project |
| `skailup-implement-page/` | Implements a single page from a screen design spec |
| `skailup-update-docs/` | Syncs inline and reference documentation with current implementation |
| `skailup-migrate/` | Generates and applies database migration files |
| `skailup-seed/` | Generates and runs seed data scripts from the data model |
| `skailup-generate/` | Generates boilerplate code from templates and concept artifacts |
| `skailup-git-prepare/` | Prepares a clean git state for a supervised build session (superpowers) |
| `skailup-brainstorm/` | Structured brainstorm to resolve an implementation decision (superpowers) |
| `skailup-write-plan/` | Writes a step-by-step implementation plan before coding begins (superpowers) |
| `skailup-implement-supervised/` | Runs a supervised multi-step implementation session with checkpoints (superpowers) |
| `skailup-finish-branch/` | Finalizes, tests, and prepares a feature branch for review (superpowers) |

## Conventions

- Build skills require `_concept/30_blueprint/` artifacts; run skaileup-blueprint before invoking this domain.
- Superpowers skills (`skailup-git-prepare`, `skailup-brainstorm`, `skailup-write-plan`, `skailup-implement-supervised`, `skailup-finish-branch`) wrap core skills in a supervised workflow with explicit human checkpoints.
- All implementation follows TDD: tests are written and run before implementation is marked complete.
