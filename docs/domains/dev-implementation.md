---
title: dev-implementation
description: Implementation pipeline — project scaffolding, feature implementation (TDD), verification, migrations, and seed data, consuming _concept/ artifacts.
---

Translates `_concept/` artifacts into running code. Covers the full implementation lifecycle: scaffolding a new project, implementing features test-first, running verification, applying database migrations, and generating seed data.

Skills consume the output of `dev-conceptualization` and produce committed, tested code. CF and Saxe variants coexist under their respective subdirectories until merged.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | Acceptance criteria, git workflow, implementation structure, skill grammar, prerequisites, verification protocols |
| `docs/` | Implementation workflow guides, TDD patterns, migration conventions |
| `flows/` | `standard.json`, `full.json`, `cli.json`, `prototype.json` |
| `skills/` | Numbered skill groups (see below) |
| `agents/implementation/` | Implementation pipeline GitAgent (GitAgent spec v0.1.0) |

## Skill Groups

### Setup (`skills/10_setup/`)

| Skill | What it does |
|---|---|
| `scaffold` | Bootstrap a new project from the tech stack spec → directory structure, config files, package.json |
| `foundation` | Set up shared utilities, base types, and project-wide conventions |
| `infrastructure` | Database setup, auth scaffolding, API layer foundation |

### Core Implementation

| Skill | When to use |
|---|---|
| `implement` | Orchestrate the full feature implementation loop |
| `implement-feature` | Implement one feature end-to-end with TDD |
| `utilities` | Generate utility/helper code (shared functions, converters, validators) |
| `verify` | Verify implementation against the concept spec — checks coverage, correctness, conventions |

## Flows

| Flow | Description |
|---|---|
| `standard` | Standard implementation pipeline (scaffold → implement features → verify) |
| `full` | Full pipeline with all optional steps (migrations, seed data, E2E) |
| `cli` | CLI-focused implementation (no UI) |
| `prototype` | Rapid prototype — minimal ceremony, fast to working code |
