---
title: dev-quality
description: Quality assurance pipeline — code audits, test generation (unit, integration, E2E), readiness gates, structure audits, and cross-reference repair.
---

Implements the quality layer of the development pipeline. Covers static code analysis and audits, test generation at all levels (unit, integration, E2E), concept structure validation, cross-reference repair, and readiness gating before deployment.

CF and Saxe variants coexist under `cf/` and `saxe/` subdirectories. Quality skills can operate standalone against a project directory or be orchestrated as a gate after implementation.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | Quality criteria, audit checklists, acceptance criteria, test structure conventions |
| `docs/` | Test strategy, audit format specs, quality workflow guides |
| `skills/` | `audit`, `e2e`, `test-plan`, `test-unit`, `test-integration`, `ready`, `sync`, `compile-validators` |
| `agents/quality/` | Quality GitAgent (GitAgent spec v0.1.0) |
| `tools/` | `lint_concept.py` — concept structure linter |

## Skills

| Skill | When to use |
|---|---|
| `audit` | Static code analysis + concept structure audit — catches missing files, broken cross-refs, frontmatter violations |
| `test-plan` | Generate a test plan from the concept spec (unit, integration, E2E coverage matrix) |
| `test-unit` | Generate unit tests for a feature or module |
| `test-integration` | Generate integration tests (API, database, service boundaries) |
| `e2e` | Generate end-to-end browser tests (Playwright) from screen specs |
| `ready` | Readiness gate — checks all required artifacts exist and pass quality criteria before deployment |
| `sync` | Repair broken cross-references between concept artifacts (features ↔ screens, model → features) — shows diff before applying |
| `compile-validators` | Compile validator scripts from skill validation definitions |
