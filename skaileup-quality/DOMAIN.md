---
name: skaileup-quality
description: "Quality assurance pipeline: code audits, test generation (unit, integration, E2E), readiness gates, structure audits, and cross-reference repair for both concept artifacts and implementation code."
type: domain
building_blocks:
  contracts: "Quality criteria, audit checklists, test structure conventions, and acceptance criteria shared across all quality skills."
  docs: "Quality workflow guides, test strategy documentation, and audit format specifications."
  skills: "Skills for auditing code, generating tests, checking readiness gates, syncing cross-references, and compiling validators."
  agents: "agents/quality/ (GitAgent spec v0.1.0)"
  prompts: "TBD"
  tools: "Validator scripts, lint tools (lint_concept.py)."
stage: alpha
---

# Dev Quality

This domain implements the quality layer of the development pipeline. It covers static code analysis and audits, test generation at all levels (unit, integration, E2E), concept structure validation, cross-reference repair, and readiness gating before deployment.

CF and Saxe variants coexist under `cf/` and `saxe/` subdirectories. Quality skills can operate standalone against a project directory or be orchestrated as a gate after implementation.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Quality criteria, audit checklists, acceptance criteria, test structure conventions |
| `docs/` | Test strategy, audit format specs, quality workflow guides |
| `skills/` | Invocable quality skills (see below) |

## Skill Groups

| Group | Purpose |
|-------|---------|
| `audit/` | Static code analysis, concept structure audit, lint, code quality review |
| `e2e/` | End-to-end browser test generation and execution |
| `ready/` | Readiness gate — blocks progression until all criteria are met |
| `sync/` | Cross-reference repair between concept artifacts (features ↔ screens ↔ data model) |
| `test-unit/` | Unit test generation |
| `test-integration/` | Integration test generation |
| `test-plan/` | Test plan generation from concept features |
| `compile-validators/` | Compile `_validation.json` files from skill outputs |

## Conventions

- Quality skills are non-destructive: they report issues rather than silently fixing them
- `sync/` skills show a diff before applying any cross-reference repairs
- Audit skills produce structured findings with severity levels (error, warn, info)
- Readiness gates are binary: pass or blocked (with reasons)
