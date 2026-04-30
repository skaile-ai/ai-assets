---
name: "quality"
description: "[skaile-development] Umbrella quality-gate orchestrator for the skaile-dev monorepo. Runs typecheck → test (with coverage + ratchet when the mode requires it) → audit → doc-audit → ready in sequence, early-outing on the first hard failure. Produces a consolidated health snapshot that contributors look at before opening a PR and release uses as its pre-flight check. Supports quick (diff-scoped), package (one package deep), and full (whole monorepo, mirrors test-full.yml CI lane) modes."
metadata:
  version: "1.1.0"
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
        label: "Steps to skip (comma-separated from: typecheck, test, audit, doc, ready)"
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

## Canonical References

- **Test concept:** `_devlog/specs/2026-04-22-test-concept-design.md` — the layer
  taxonomy and coverage policy that this gate enforces.
- **Phase plan:** `_devlog/plans/2026-04-22-test-gap-fill.md` — current gap-fill
  status per package (readiness criteria cross-check against this).
- **User-facing testing overview:** `/testing/`.
- **CI lanes:** `.github/workflows/test-fast.yml` · `test-full.yml` · `test-e2e.yml`
  — the three workflows every PR runs through. `mode=full` below mirrors
  `test-full.yml` locally (pre-PR sanity check).
- **Coverage ratchet:** `_scripts/check-coverage-ratchet.ts` — compares
  `_devlog/reports/coverage-ci/coverage-summary.json` (produced by the coverage
  step) against `_devlog/reports/coverage-baseline-2026-04-22/summary.json`.

## Overview

Coordinates the quality skills in order, early-outing on the first hard failure:

```
1. typecheck           — bun run typecheck (matches test-fast.yml step 1)
2. test                — run affected test suite; with coverage + ratchet on full mode
3. audit               — build, lint, security/logic/UI sub-agents
4. doc                 — doc --mode audit for coverage gaps
5. ready               — per-package readiness criteria
```

Each step feeds its artifact forward; the final report aggregates all into one
health snapshot at `_devlog/reports/quality-<date>.{md,json}`.

## Modes

| Mode | Scope | Steps | Typical time |
|---|---|---|---|
| `quick` (default) | Diff against staged/last-commit | typecheck → test (affected, no coverage) → audit scope=diff → doc status → ready (changed packages) | 2–5 min |
| `package` | One named package | typecheck → test (that pkg) → audit scope=package → doc audit scope=pkg → ready scope=package | 10–20 min |
| `full` | Whole monorepo (mirrors `test-full.yml` CI lane + audit + doc + ready) | typecheck → test all (with istanbul coverage under Bun) → coverage ratchet → audit scope=full → doc audit → ready scope=all | 30–60 min |

**`mode=full` is a pre-PR sanity check, not mandatory.** CI runs all three lanes
(fast, full, E2E) on every PR to main automatically. Use `mode=full` locally when
you want to catch a ratchet regression or an audit finding before pushing, or when
a CI failure needs to be reproduced on your machine.

## When to Use

- Before opening a PR (`mode=quick`)
- Before merging a feature branch that touched multiple packages (`mode=package` per package)
- As the first step of `release bump` (`mode=full`)
- Weekly monorepo health check (`mode=full`, scheduled)

## When NOT to Use

- For one specific concern (bug triage, doc sync) — call the individual skill directly
- When you know a step will fail and want to investigate it alone — call that skill directly

---

ROLE  Quality gate orchestrator. Runs typecheck → test (with coverage ratchet on full mode) → audit → doc-audit → ready in sequence; aggregates their JSON artifacts into a consolidated snapshot.

READS
  ! Artifacts produced by each sub-skill
  ! ai-assets/skaile-development/references/test_stack_map.md
  ! ai-assets/skaile-development/references/readiness_criteria.md
  ! _devlog/reports/coverage-baseline-2026-04-22/summary.json (mode=full only)
  ! _devlog/reports/coverage-ci/coverage-summary.json (mode=full only, written by the test step)

WRITES
  _devlog/reports/quality-<YYYYMMDD-HHMM>.md
  _devlog/reports/quality-<YYYYMMDD-HHMM>.json

MUST  run steps in order: typecheck → test (→ coverage ratchet on full) → audit → doc → ready
MUST  early-exit on the first hard failure (typecheck fail, test fail, ratchet regression, audit fail, ready blocked) unless skip list allows it
MUST  pass artifacts forward (audit reads test results to avoid re-running)
MUST  respect skip list — but still include a "skipped" section in the report
MUST  aggregate every step's JSON into the final JSON (typecheck, test, coverage_ratchet, audit, doc, ready)
MUST  in mode=full — generate istanbul coverage under Bun and run `_scripts/check-coverage-ratchet.ts` before audit
NEVER modify source files — all sub-skills are read-only except sync-docs which is not included here
NEVER skip a step implicitly — every skip must be explicit via the `skip` input
NEVER recommend `@vitest/coverage-v8` under Node for this repo — see the test skill's Known Constraints table

EMIT [quality] started mode=<mode> target=<target>

# ── Phase 1: Plan ────────────────────────────────────────────────

STEP 1: Resolve scope
  Parse mode, target, skip inputs.
  Compute affected packages:
    quick → from `git diff --cached` or `git diff HEAD~1..HEAD`
    package → [target]
    full → all workspaces

STEP 2: Determine step list
  steps = [typecheck, test, audit, doc, ready] minus any in skip.
  Abort if skip contains unknown names.
  For mode=full, add the coverage-ratchet sub-step after test.

# ── Phase 1.5: Typecheck ─────────────────────────────────────────

STEP 2.5: Run typecheck
  - $ bun run typecheck
  This is the L0 gate from the test concept — no point running tests if tsc
  fails. Matches the first step of `test-fast.yml`.
  IF typecheck fails and typecheck is not in skip:
    - Report: "quality gate failed at [typecheck] — TypeScript errors"
    - EXIT non-zero

EMIT [quality] step=typecheck status=pass|fail

# ── Phase 2: Test ────────────────────────────────────────────────

STEP 3: Run tests
  IF mode = full:
    - Mirror the test-full.yml CI lane locally (truncate to summary):
      $ bun x --bun vitest run \
          --coverage.enabled --coverage.provider=istanbul \
          --coverage.reporter=text-summary \
          --coverage.reporter=json-summary \
          --coverage.reportsDirectory=_devlog/reports/coverage-ci \
          2>&1 | tail -40
    - Then run the ratchet:
      $ bun _scripts/check-coverage-ratchet.ts
      Exit codes: 0 pass, 1 regression (fail the gate), 2 invalid input (fail the gate).
      A `baseline-improved` verdict is informational — the ratchet does not
      fail on improvement; updating the committed baseline is a manual PR step.
    - Note: forge/L5-concept (vitest 4.1), forge/L4-project, forge/L4-assistant, platform/*,
      and all Playwright suites are not part of the root istanbul run — run them
      via their scoped test commands (`bun run --filter <pkg> test 2>&1 | tail -60`)
      if they're in scope.
    - mode=full SESSION NOTE: If this is running in a long session that already has
      significant context, consider starting a fresh session for mode=full — the
      coverage + audit + doc sequence is resource-intensive and benefits from a clean
      context window.
  ELSE (quick, package):
    - Call: test mode=run scope=<affected packages>
    - Capture test JSON (if test skill writes one, otherwise parse stdout)
  IF test fails and test is not in skip:
    - Aggregate partial result
    - Write artifacts
    - Report: "quality gate failed at [test] — <N> tests failing" (or
      "coverage ratchet regression: <pkg> -<delta>pts below baseline")
    - EXIT non-zero

  COMPACT CHECKPOINT (before audit):
    After the test step completes (pass or skip), call /compact before proceeding
    to audit. Test output is the largest context contributor in this pipeline;
    compacting here prevents audit + doc phases from re-reading full test history
    on every API call.

EMIT [quality] step=test status=pass|fail coverage_ratchet=<pass|fail|skipped>

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
    "typecheck": { "status": "pass|fail" },
    "test": { "total": N, "passed": N, "failed": N, "artifact": "<path>" },
    "coverage_ratchet": { "status": "pass|fail|skipped", "regressed": [<pkg>...], "improved": [<pkg>...], "artifact": "_devlog/reports/coverage-ci/coverage-summary.json" },
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
  | typecheck | pass | bun run typecheck |
  | test | pass | 142 passed / 142 total |
  | coverage ratchet | pass | 0 regressions · 2 improvements |
  | audit | pass | verdict=pass · 0 critical · 2 medium |
  | doc | warn | 3 coverage gaps |
  | ready | pass | 16/16 packages ready |

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
  - [ ] mode=full: istanbul coverage generated + ratchet checked
  - [ ] Aggregated JSON and markdown written
  - [ ] Final verdict matches the sub-skill outcomes

---

## Local `mode=full` Recipe

When the user asks for "the equivalent of what CI runs", this is the sequence
that mirrors `.github/workflows/test-full.yml`:

```bash
bun run typecheck
bun x --bun vitest run \
  --coverage.enabled --coverage.provider=istanbul \
  --coverage.reporter=text --coverage.reporter=text-summary \
  --coverage.reporter=json-summary \
  --coverage.reportsDirectory=_devlog/reports/coverage-ci
bun _scripts/check-coverage-ratchet.ts
```

Then layer audit + doc + ready on top per STEP 4–6. The full quality gate always
includes everything above plus audit/doc/ready; CI only runs typecheck + test +
coverage ratchet in `test-full.yml`. The E2E lane (`test-e2e.yml`) is separate —
the `quality` skill does not run Playwright.

## Integration

- **Called by:** user before PR, `release` (Phase 0 for full mode), CI on main
- **Calls:** `test` (which may delegate to `test-plan`/`test-unit`/`test-integration`/`test-e2e`), `audit`, `doc --mode audit`, `ready`, and `_scripts/check-coverage-ratchet.ts` (mode=full)
- **Reads:** artifacts from the above skills + `_devlog/reports/coverage-ci/coverage-summary.json` + `_devlog/reports/coverage-baseline-2026-04-22/summary.json`
- **Writes:** `_devlog/reports/quality-*.{md,json}`
