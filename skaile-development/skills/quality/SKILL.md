---
name: "quality"
description: "Umbrella quality-gate orchestrator. Runs test → audit → doc-audit → ready in sequence, early-outing on the first hard failure. Produces a consolidated health snapshot that contributors look at before opening a PR and release uses as its pre-flight check. Supports quick (diff-scoped) and deep (package-scoped or full) modes."
metadata:
  version: "1.0.0"
  tags:
    - "quality"
    - "umbrella"
    - "orchestrator"
    - "gate"
    - "pre-pr"
    - "pre-release"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
      - path: "package.json"
        gate: hard
        description: "Monorepo root"
    inputs_optional:
      - id: mode
        label: "Mode"
        type: select
        options:
          - "quick"
          - "package"
          - "full"
        default: "quick"
        hint: "quick = diff-scoped fast gate | package = deep one package | full = whole monorepo"
      - id: target
        label: "Package path (mode=package)"
        type: text
      - id: skip
        label: "Steps to skip (comma-separated from: test, audit, doc, ready)"
        type: text
    reads:
      - path: "ai-assets/skaile-development/references/test_stack_map.md"
      - path: "ai-assets/skaile-development/references/readiness_criteria.md"
    produces:
      - path: "_devlog/reports/quality-<date>.md"
        description: "Consolidated health snapshot"
      - path: "_devlog/reports/quality-<date>.json"
        description: "Structured consolidated result"
  user_inputs:
    dialog:
      - id: "mode"
        label: "Mode"
        type: "select"
        options: ["quick", "package", "full"]
        required: false
        default: "quick"
      - id: "target"
        label: "Package path"
        type: "text"
        required: false
      - id: "skip"
        label: "Steps to skip"
        type: "text"
        required: false
    files: []
---

# Quality — Umbrella Quality Gate

## Overview

Coordinates the four quality skills in order, early-outing on the first hard failure:

```
1. test    — run test suite for affected packages
2. audit   — build, lint, typecheck, security/logic/UI sub-agents
3. doc     — doc --mode audit for coverage gaps
4. ready   — per-package readiness criteria
```

Each step feeds its artifact forward; the final report aggregates all four into one health snapshot at `_devlog/reports/quality-<date>.{md,json}`.

## Modes

| Mode | Scope | Steps | Typical time |
|---|---|---|---|
| `quick` (default) | Diff against staged/last-commit | test (affected) → audit scope=diff → doc status → ready (changed packages) | 2–5 min |
| `package` | One named package | test (that pkg) → audit scope=package → doc audit scope=pkg → ready scope=package | 10–20 min |
| `full` | Whole monorepo | test all → audit scope=full → doc audit → ready scope=all | 30–60 min |

## When to Use

- Before opening a PR (`mode=quick`)
- Before merging a feature branch that touched multiple packages (`mode=package` per package)
- As the first step of `release bump` (`mode=full`)
- Weekly monorepo health check (`mode=full`, scheduled)

## When NOT to Use

- For one specific concern (bug triage, doc sync) — call the individual skill directly
- When you know a step will fail and want to investigate it alone — call that skill directly

---

ROLE  Quality gate orchestrator. Runs test, audit, doc-audit, ready in sequence; aggregates their JSON artifacts into a consolidated snapshot.

READS
  ! Artifacts produced by each sub-skill
  ! ai-assets/skaile-development/references/test_stack_map.md
  ! ai-assets/skaile-development/references/readiness_criteria.md

WRITES
  _devlog/reports/quality-<YYYYMMDD-HHMM>.md
  _devlog/reports/quality-<YYYYMMDD-HHMM>.json

MUST  run steps in order: test → audit → doc → ready
MUST  early-exit on the first hard failure (test fail, audit fail, ready blocked) unless skip list allows it
MUST  pass artifacts forward (audit reads test results to avoid re-running)
MUST  respect skip list — but still include a "skipped" section in the report
MUST  aggregate all four JSON artifacts into the final JSON
NEVER modify source files — all sub-skills are read-only except sync-docs which is not included here
NEVER skip a step implicitly — every skip must be explicit via the `skip` input

EMIT [quality] started mode=<mode> target=<target>

# ── Phase 1: Plan ────────────────────────────────────────────────

STEP 1: Resolve scope
  Parse mode, target, skip inputs.
  Compute affected packages:
    quick → from `git diff --cached` or `git diff HEAD~1..HEAD`
    package → [target]
    full → all workspaces

STEP 2: Determine step list
  steps = [test, audit, doc, ready] minus any in skip.
  Abort if skip contains unknown names.

# ── Phase 2: Test ────────────────────────────────────────────────

STEP 3: Run tests
  - Call: test mode=run scope=<affected packages> or "all" for full mode
  - Capture test JSON (if test skill writes one, otherwise parse stdout)
  IF test fails and test is not in skip:
    - Aggregate partial result
    - Write artifacts
    - Report: "quality gate failed at [test] — <N> tests failing"
    - EXIT non-zero

EMIT [quality] step=test status=pass|fail

# ── Phase 3: Audit ───────────────────────────────────────────────

STEP 4: Run audit
  audit_scope = {quick: diff, package: package, full: full}[mode]
  - Call: audit scope=<audit_scope> target=<target>
  - Read _devlog/reports/audit-<stamp>.json
  IF audit verdict = fail and audit not in skip:
    - Aggregate
    - Report: "quality gate failed at [audit] — <N> blocking issues"
    - EXIT non-zero

EMIT [quality] step=audit verdict=<verdict>

# ── Phase 4: Doc Audit ───────────────────────────────────────────

STEP 5: Run doc audit
  - Call: doc --mode audit [--scope <target>]
  - Parse output (coverage gaps, stale pages)
  doc never hard-fails the gate — it only produces warnings.
  Aggregate gap count into the final report.

EMIT [quality] step=doc gaps=<N>

# ── Phase 5: Ready ───────────────────────────────────────────────

STEP 6: Run readiness
  ready_scope = {quick: affected packages, package: target, full: all}[mode]
  - Call: ready scope=<ready_scope> target=<target>
  - Read _devlog/reports/readiness-<stamp>.json
  IF ready has blockers and ready not in skip:
    - Aggregate
    - Report: "quality gate failed at [ready] — <N> blockers"
    - EXIT non-zero

EMIT [quality] step=ready blocked=<N>

# ── Phase 6: Aggregate ───────────────────────────────────────────

STEP 7: Write consolidated JSON
  OUTPUT _devlog/reports/quality-<stamp>.json
  {
    "run_id": "<uuid>",
    "mode": "<mode>",
    "target": "<target>",
    "steps_run": [...],
    "steps_skipped": [...],
    "test": { "total": N, "passed": N, "failed": N, "artifact": "<path>" },
    "audit": { "verdict": "...", "critical": N, "high": N, "artifact": "<path>" },
    "doc": { "gaps": N, "stale": N },
    "ready": { "ready": N, "blocked": N, "artifact": "<path>" },
    "verdict": "pass|warn|fail"
  }

STEP 8: Write markdown
  OUTPUT _devlog/reports/quality-<stamp>.md
  # Quality Snapshot — <date>

  **Mode:** <mode>  **Target:** <target>  **Verdict:** <verdict>

  ## Steps
  | Step | Status | Detail |
  |---|---|---|
  | test | ✓ | 142 passed / 142 total |
  | audit | ✓ | verdict=pass · 0 critical · 2 medium |
  | doc | ⚠ | 3 coverage gaps |
  | ready | ✓ | 16/16 packages ready |

  ## Artifacts
  - Test log: (embedded / via test skill)
  - Audit: _devlog/reports/audit-<stamp>.md
  - Doc audit: _devlog/reports/doc-audit-<stamp>.md
  - Readiness: _devlog/reports/readiness-<stamp>.md

  ## Findings Summary
  - Blocking issues: <N> — see audit report
  - Readiness blockers: <N> — see readiness report

  ## Recommended Action
  pass → proceed to commit / PR / release
  warn → consider addressing medium findings + doc gaps
  fail → fix blockers before moving forward

STEP 9: Final message
  [quality] mode=<mode> → <verdict>
  <N> step(s) passed · <N> step(s) skipped · <N> step(s) failed
  Snapshot: _devlog/reports/quality-<stamp>.md

EMIT [quality] completed verdict=<verdict>

CHECKLIST
  - [ ] Steps ran in the defined order
  - [ ] Early-exit on first hard failure (unless skipped)
  - [ ] Skip list validated and explicit
  - [ ] Sub-skill artifacts discovered and read
  - [ ] Aggregated JSON and markdown written
  - [ ] Final verdict matches the sub-skill outcomes

---

## Integration

- **Called by:** user before PR, `release` (Phase 0 for full mode), CI on main
- **Calls:** `test`, `audit`, `doc --mode audit`, `ready`
- **Reads:** artifacts from the above skills
- **Writes:** `_devlog/reports/quality-*.{md,json}`
