---
name: skaileup-quality
description: "Code quality, testing, and readiness gates — static code audit, test generation at all levels (unit, integration, E2E), readiness checks, cross-reference repair, and validator compilation."
type: domain
building_blocks:
  contracts: "n/a — to be populated after skill migration."
  docs: "n/a — to be populated after skill migration."
  skills: "Code audit, test plan, unit tests, integration tests, E2E tests, audit, readiness gate, cross-reference sync, and validator compilation skills."
  tools: "n/a"
stage: alpha
---

# skaileup-quality

Code quality, testing, and readiness gates — static code audit, test generation at all levels (unit, integration, E2E), readiness checks, cross-reference repair, and validator compilation. Quality skills operate on an implemented codebase and produce test suites, audit reports, and readiness verdicts that gate promotion to the next pipeline stage.

Skills will be moved into this domain during the architecture reorganization (Phase 5.2+).

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | Invocable skills (see below) |

## Skills (target)

| Skill | Purpose |
|-------|---------|
| `skailup-eval-code/` | Static code audit against standards, patterns, and iron laws |
| `skailup-test-plan/` | Generates a test plan from feature specs and acceptance criteria |
| `skailup-test-unit/` | Generates and runs unit tests for a target package or module |
| `skailup-test-integration/` | Generates and runs integration tests across module boundaries |
| `skailup-e2e/` | Generates and runs end-to-end tests against running application scenarios |
| `skailup-audit/` | Full quality audit combining code, test coverage, and standards compliance |
| `skailup-ready/` | Readiness gate — checks all quality criteria before promotion |
| `skailup-sync/` | Repairs cross-references and stale links across concept and implementation artifacts |
| `skailup-compile-validators/` | Compiles skill output validators for automated quality checking |

## Conventions

- Quality skills operate on an existing implementation; run skaileup-build skills before invoking this domain.
- `skailup-ready` is the gating skill — it must pass before a feature branch is merged or a pipeline stage is promoted.
- `skailup-sync` is safe to run at any time; it does not modify implementation code, only cross-reference metadata.
