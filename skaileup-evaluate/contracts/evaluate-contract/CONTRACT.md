---
name: "evaluate-contract"
description: "Shared contract for all skaileup-evaluate skills. Describes quality report formats, score schema, audit output structure, test file conventions, and cross-reference integrity rules."
metadata:
  stage: "alpha"
  do_not_invoke: true
---

# Quality Domain — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `skaileup-evaluate` skills read this before operating.

## Scope

Quality skills operate on both `_concept/` (structure audits) and source code (static analysis, tests). This contract defines their shared output formats and conventions.

## Skills Overview

| Skill | Trigger | Output |
|-------|---------|--------|
| `audit` | code exists, user requests analysis | `_quality/audit-report.md`, `_quality/quality.json` |
| `ready` | before implementation starts | `_quality/readiness.json` |
| `sync` | broken cross-references detected | patches to `_concept/` frontmatter |
| `test-plan` | concept complete | `_concept/4_testing/test_plan.md` |
| `test-unit` | feature implemented | test files alongside source |
| `test-integration` | features complete | integration test suite |
| `e2e` | app deployed / staging env | e2e test suite (Playwright/Cypress) |
| `compile-validators` | schema files exist | compiled validator scripts |

## quality.json Format

```json
{
  "schema_version": "1.0",
  "run_id": "<uuid>",
  "scored_at": "YYYY-MM-DDTHH:MM:SSZ",
  "scope": "concept | code | full",
  "score": 0-100,
  "grades": {
    "structure": 0-100,
    "cross_refs": 0-100,
    "completeness": 0-100,
    "freshness": 0-100
  },
  "issues": [
    {
      "severity": "CRITICAL | HIGH | MEDIUM | LOW | INFO",
      "category": "structure | cross_ref | stale | missing | orphan",
      "file": "<path>",
      "message": "<description>",
      "auto_fixable": true | false
    }
  ]
}
```

## Audit Report Format

`_quality/audit-report.md`:
```markdown
# Audit Report — <scope>
**Date:** YYYY-MM-DD  **Run ID:** <uuid>  **Score:** NN/100

## Summary
<N CRITICAL, N HIGH, N MEDIUM, N LOW>

## Issues
### CRITICAL
- `path/to/file`: <description>

## Auto-fixable
<list of issues sync can repair>
```

## Test File Conventions

- Unit tests: co-located with source (`*.test.ts`, `*.spec.ts`)
- Integration tests: `tests/integration/`
- E2E tests: `tests/e2e/` (Playwright preferred)
- Validators: `_quality/validators/`

## Quality Gate Thresholds

| Gate | Minimum score | Max blocking issues |
|------|--------------|---------------------|
| Continue auto-review | 70 | 0 CRITICAL, 0 HIGH |
| Readiness (pre-implementation) | 80 | 0 CRITICAL |
| Deployment | 90 | 0 CRITICAL, 0 HIGH |
