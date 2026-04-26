---
name: "skailup-eval-code"
description: "Code quality evaluator. Verifies build passes, then dispatches three parallel sub-agents for logic errors, security vulnerabilities, and UI/UX code concerns. Three scopes: scaffold (build only), feature (build + unit tests), full (everything). Produces _implementation/eval-code.json. Runs at multiple pipeline checkpoints: after scaffold, after each feature group, and before release."
metadata:
  version: "1.0.0"
  tags:
    - "evaluate"
    - "code-quality"
    - "security"
    - "static-analysis"
    - "tests"
    - "build"
    - "parallel"
    - "adversarial"
  source: "MERGED"
  stage: "alpha"
  prerequisites:
    files:
      - path: "package.json"
        gate: hard
        description: "Source code must exist (or pyproject.toml equivalent)"
    inputs_optional:
      - id: scope
        label: "Evaluation scope"
        type: select
        options:
          - scaffold
          - feature
          - full
        default: full
        hint: "scaffold = build+lint+types | feature = +unit tests | full = +logic/security/ui analysis"
    reads:
      - path: "package.json"
        description: "Project dependencies and scripts"
      - path: "_concept/blueprint/techstack.md"
        description: "Expected tech stack for architecture compliance"
      - path: "_standards/index.yml"
        description: "Project coding standards (if available)"
    produces:
      - path: "_implementation/eval-code.json"
        description: "Code quality evaluation result"
---

# Eval Code — Code Quality Evaluator

## Overview

Evaluate source code quality at one of three scopes. Build verification always runs first —
if it fails, stop immediately. Sub-agent analysis only runs for `full` scope.

Checkpoints in the pipeline:
- After scaffold: scope=scaffold (build + lint + types)
- After each feature group: scope=feature (+ unit tests for that group)
- Before release: scope=full (+ parallel logic/security/UI analysis)

READS
  ! package.json (or pyproject.toml)          — scripts and dependencies
  ? _concept/blueprint/techstack.md — architecture compliance check
  ? _standards/index.yml                      — project coding standards

WRITES
  _implementation/eval-code.json   — MUST write before reporting

MUST  run build verification before any sub-agent analysis
MUST  dispatch logic, security, and ui_ux as parallel sub-agents (scope=full)
MUST  provide a specific fix for every finding
MUST  stop immediately if build fails — do not continue to sub-agents
NEVER mark as pass if any critical security finding exists
NEVER skip build verification

## Process

STEP 1: Detect build commands from package.json scripts.
  Look for: lint, typecheck (or type-check, tsc), build.
  Fallback: `bun run lint`, `bun run typecheck`, `bun run build`.

STEP 2: Run build verification (all scopes).
  Execute in sequence: lint → typecheck → build

  IF any command fails:
    Set build.<key> = "fail"
    Set verdict = "fail"
    Write eval-code.json with partial results
    Report: "[eval-code] FAIL — build did not pass (<command>). Fix before re-running."
    STOP.

STEP 3: Run test suite (scope=feature or full).
  Detect test runner from package.json.
  Capture: pass count, fail count, skip count, coverage summary.

  IF tests fail:
    Set verdict = "fail"
    Include failing test names in blocking_issues
    Write _implementation/eval-code.json with partial results
    Report: "[eval-code] FAIL — {n} tests failing. Fix before re-running."
    STOP.

STEP 4: Dispatch three parallel sub-agents (scope=full only).

  Sub-agent A — Logic Auditor:
    Read all source files. Look for:
    - Null/undefined dereference without guards
    - Off-by-one errors in loops and array access
    - async/await misuse (missing await, swallowed rejections)
    - Missing error handling at system boundaries (API calls, file I/O, DB)
    - Data loss paths (update without existence check)
    - Race conditions in concurrent operations

  Sub-agent B — Security Auditor:
    Read all source files and dependencies. Look for:
    - SQL/NoSQL injection vectors
    - XSS vectors (unsanitized user input in HTML/DOM)
    - Auth bypass paths (missing auth middleware, broken RBAC)
    - Insecure direct object references (no ownership check)
    - Sensitive data in logs, responses, or localStorage
    - CSRF on state-changing endpoints
    Run: `bun audit` or `npm audit` or `pip-audit`

  Sub-agent C — UI/UX Code Auditor:
    Read frontend source files. Look for:
    - Interactive elements without accessible labels
    - Custom interactive elements missing keyboard handlers
    - Loading states that block UI with no feedback
    - Error states with no user recovery path
    - Unguarded form submissions (double-submit possible)
    - Hardcoded colors/sizes overriding design tokens

  All findings use severity: critical | high | medium | low

STEP 5: Synthesize. blocking_issues = all critical and high findings.

STEP 6: Determine verdict:
  - pass: build clean AND tests pass AND no critical/high findings
  - warn: build clean AND tests pass AND medium findings only
  - fail: build fails OR tests fail OR any critical finding

STEP 7: Write _implementation/eval-code.json

STEP 8: Report
  [eval-code] scope={scope} → {verdict}
  Build: lint {status} · types {status} · bundle {status}
  Tests: {pass}/{total} ({coverage}% coverage)
  Logic: {score}/100 · Security: {score}/100 · UI/UX: {score}/100
  Blocking: {n} issues
