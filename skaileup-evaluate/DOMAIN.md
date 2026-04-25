---
name: skaileup-evaluate
description: "Four-round evaluation pipeline: concept completeness gate (eval-concept), per-feature implementation verification (eval-feature), whole-product goal assessment (eval-product), and code quality audit (eval-code). Also provides standalone test generation and ad-hoc audit tools."
type: domain
building_blocks:
  contracts: "Quality criteria, audit checklists, test structure conventions, and acceptance criteria shared across all quality skills."
  docs: "Quality workflow guides, test strategy documentation, and audit format specifications."
  skills: "Skills for auditing code, generating tests, checking readiness gates, syncing cross-references, and compiling validators."
  agents: "agents/quality/ (GitAgent spec v0.1.0)"
  tools: "Validator scripts, lint tools (lint_concept.py)."
stage: alpha
---

# Dev Evaluate

This domain implements the quality layer of the development pipeline. It covers static code analysis and audits, test generation at all levels (unit, integration, E2E), concept structure validation, cross-reference repair, and readiness gating before deployment.

Quality skills can operate standalone against a project directory or be orchestrated as a gate after implementation. All skills are unified — there are no per-variant subdirectories.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Quality criteria, audit checklists, acceptance criteria, test structure conventions |
| `docs/` | Test strategy, audit format specs, quality workflow guides |
| `skills/` | Invocable quality skills (see below) |

## Skill Groups

| Group | Purpose | When to Use |
|-------|---------|-------------|
| `skailup-eval-concept/` | Concept completeness + clarity gate | After skaileup-conceptualization, before build starts |
| `skailup-eval-feature/` | Implementation vs. concept (per feature group) | After each feature group in skaileup-implementation |
| `skailup-eval-product/` | Whole product vs. goals (graded design criteria) | After all feature groups approved, before release |
| `skailup-eval-code/` | Build, test suite, parallel logic/security/UI audit | scaffold/feature/full scope checkpoints |
| `skailup-test-plan/` | Test plan generation from concept features | After concept complete, before implementation |
| `skailup-test-unit/` | Unit test generation from feature specs | During TDD implementation per feature |
| `skailup-test-integration/` | Integration tests (API + DB + cross-feature flows) | After feature groups, before product eval |
| `skailup-audit/` | Ad-hoc static code audit (standalone use) | On-demand outside pipeline gates |
| `skailup-ready/` | Pre-flight readiness check (standalone use) | Before manual E2E testing |
| `skailup-e2e/` | Browser-based E2E test suite (standalone use) | On-demand or CI |
| `skailup-sync/` | Cross-reference repair in _concept/ | When concept cross-refs break |
| `skailup-compile-validators/` | Compile validator scripts from SKILL.md rules | Tooling maintenance |

## Conventions

- Evaluation skills are adversarial: they assume failures exist and prove correctness
- Pipeline gate skills (`eval-*`) MUST run as fresh sub-agents, never in the same context as the generator
- All eval skills write a JSON output file before reporting — the output file is the source of truth
- Standalone tools (`audit`, `e2e`, `ready`, `sync`) are non-destructive and safe to run at any time
- `sync` shows a diff before applying any cross-reference repairs
