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

---

## Evaluation Round Output Schemas

The four evaluation rounds each produce a JSON file. All eval skills MUST write their output file before reporting to the user.

### eval-concept.json — `_concept/eval-concept.json`

```json
{
  "schema_version": "1.0",
  "evaluated_at": "YYYY-MM-DDTHH:MM:SSZ",
  "completeness_score": 0,
  "clarity_score": 0,
  "traceability_score": 0,
  "overall_score": 0,
  "verdict": "pass | needs_resolution | fail",
  "blocking_flags": [
    {
      "type": "missing | ambiguous | contradiction | orphan | untraceable",
      "severity": "blocking",
      "location": "<path to artifact>",
      "description": "<quotes the problematic text>",
      "resolution": "<what the user must do to fix this>"
    }
  ],
  "warning_flags": [],
  "summary": "<2-3 sentence assessment>"
}
```

Overall score weights: completeness 0.4, clarity 0.35, traceability 0.25.
Verdict: pass = all three scores ≥ 80 AND no blocking flags / needs_resolution = score 60–79 or blocking flags / fail = any score < 60.

### eval-feature/{group}.json — `_implementation/eval-feature/{group-name}.json`

```json
{
  "schema_version": "1.0",
  "evaluated_at": "YYYY-MM-DDTHH:MM:SSZ",
  "feature_group": "<group name>",
  "acceptance_criteria": [
    {
      "id": "<feature_id>/<criterion_index>",
      "text": "<criterion text>",
      "interaction": "<what the evaluator did to test it>",
      "result": "pass | fail | partial | untestable",
      "evidence": "<screenshot path or description>",
      "deviation": "<if fail/partial: what app did vs what spec says>"
    }
  ],
  "screen_fidelity_score": 0,
  "journey_completable": "true | false | partial",
  "regression_issues": [],
  "deviations": [],
  "verdict": "approved | needs_revision | escalate",
  "revision_instructions": "<specific actionable feedback if needs_revision>"
}
```

Verdict thresholds: approved = ≥90% criteria pass + journey_completable=true + fidelity ≥80 + no regressions / needs_revision = 70–89% pass or fidelity 60–79 / escalate = <70% pass or regressions or critical criterion failed.

### eval-product.json — `_implementation/eval-product.json`

```json
{
  "schema_version": "1.0",
  "evaluated_at": "YYYY-MM-DDTHH:MM:SSZ",
  "goals": [
    {"goal": "<text>", "achieved": "achieved | partial | not_achieved", "evidence": "<description>"}
  ],
  "design": {
    "quality": 0,
    "originality": 0,
    "craft": 0,
    "functionality": 0,
    "notes": "<specific observations per dimension>"
  },
  "performance": {"lcp_ms": 0, "cls": 0.0, "first_interaction_ms": 0},
  "accessibility_score": 0,
  "mobile_score": 0,
  "improvement_priorities": ["<ranked list>"],
  "verdict": "approved | needs_iteration | fail"
}
```

Verdict: approved = ≥2/3 goals achieved/partial + design average ≥7 + accessibility ≥70 + LCP <4s / needs_iteration = any goal not_achieved or design avg <7 / fail = majority goals not_achieved.

Design rubrics (0–10 each):
- **Quality**: coherent experience vs. disconnected components — 9-10 requires intentional visual hierarchy
- **Originality**: distinctive identity vs. AI defaults — penalise purple/blue gradients, generic card grids, default Tailwind palette, generic hero sections
- **Craft**: typography, spacing, color harmony, consistency
- **Functionality**: every UI element serves a clear user purpose

### eval-code.json — `_implementation/eval-code.json`

```json
{
  "schema_version": "1.0",
  "evaluated_at": "YYYY-MM-DDTHH:MM:SSZ",
  "scope": "scaffold | feature | full",
  "build": {"lint": "pass | fail", "types": "pass | fail", "bundle": "pass | fail"},
  "tests": {"pass": 0, "fail": 0, "skip": 0, "coverage_lines": 0},
  "logic": {
    "score": 0,
    "findings": [{"severity": "critical|high|medium|low", "location": "<file:line>", "description": "...", "fix": "..."}]
  },
  "security": {"score": 0, "findings": []},
  "ui_ux": {"score": 0, "findings": []},
  "blocking_issues": [],
  "verdict": "pass | warn | fail"
}
```

Verdict: pass = build clean + tests pass + no critical/high findings / warn = medium findings only / fail = build fails OR tests fail OR any critical finding.
Scopes: scaffold = build+lint+types only / feature = +unit tests / full = +parallel logic/security/ui_ux sub-agents.
