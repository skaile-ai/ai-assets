---
name: skaileup-evaluate
description: "Four-round evaluation pipeline: concept completeness gate (eval-concept), per-feature implementation verification (eval-feature), whole-product goal assessment (eval-product), and code quality audit (eval-code). Also provides standalone test generation and ad-hoc audit tools."
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

# Dev Evaluate

This domain implements the quality layer of the development pipeline. It covers static code analysis and audits, test generation at all levels (unit, integration, E2E), concept structure validation, cross-reference repair, and readiness gating before deployment.

CF and Saxe variants coexist under `cf/` and `saxe/` subdirectories. Quality skills can operate standalone against a project directory or be orchestrated as a gate after implementation.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Quality criteria, audit checklists, acceptance criteria, test structure conventions |
| `docs/` | Test strategy, audit format specs, quality workflow guides |
| `skills/` | Invocable quality skills (see below) |

## Skill Groups

### Pipeline Gates (wired into orchestrators)

| Skill | When It Runs | What It Evaluates | Output |
|-------|-------------|-------------------|--------|
| `eval-concept/` | After skaileup-conceptualization Blueprint phase | Concept completeness, clarity, traceability | `_concept/eval-concept.json` |
| `eval-feature/` | After each feature group in skaileup-implementation | Implementation vs. acceptance criteria + screen specs | `_implementation/eval-feature/{group}.json` |
| `eval-product/` | After all feature groups approved | Whole product vs. goals + graded design (0–10 per dimension) | `_implementation/eval-product.json` |
| `eval-code/` | scaffold / feature / full checkpoints | Build, tests, logic/security/UI audit (parallel sub-agents) | `_implementation/eval-code.json` |

### Test Generation (TDD support, used by skaileup-implementation)

| Skill | Purpose |
|-------|---------|
| `test-plan/` | Generate test plan from concept features before implementation |
| `test-unit/` | Generate unit test files per feature spec (TDD red phase) |
| `test-integration/` | Generate integration tests for API endpoints and cross-feature flows |

### Standalone Tools (ad-hoc use outside pipeline)

| Skill | Purpose |
|-------|---------|
| `audit/` | Static code analysis on demand (outside pipeline gates) |
| `e2e/` | Browser-based E2E test suite (CI or manual verification) |
| `ready/` | Pre-flight readiness check before manual E2E runs |
| `sync/` | Cross-reference repair in `_concept/` when links break |
| `compile-validators/` | Compile validator scripts from SKILL.md rule blocks |

## Conventions

- Evaluation skills are adversarial: they assume failures exist and prove correctness
- Pipeline gate skills (`eval-*`) MUST run as fresh sub-agents, never in the same context as the generator
- All eval skills write a JSON output file before reporting — the output file is the source of truth
- Standalone tools (`audit`, `e2e`, `ready`, `sync`) are non-destructive and safe to run at any time
- `sync` shows a diff before applying any cross-reference repairs
