---
name: "audit"
description: "[skaile-development] Monorepo-aware code quality audit for the skaile-dev codebase. Verifies build (lint + typecheck + build) and tests pass, then dispatches three parallel sub-agents for logic, security, and UI/UX findings. Three scopes: diff (staged or last-commit), package (one package deep), full (whole monorepo). Produces _devlog/reports/audit-<date>.json. Use before opening a PR or cutting a release."
metadata:
  version: "1.0.0"
  tags:
    - "audit"
    - "code-quality"
    - "security"
    - "static-analysis"
    - "build"
    - "tests"
    - "parallel"
    - "adversarial"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
      - path: "package.json"
        gate: hard
        description: "Monorepo root package.json required"
    inputs_required:
      - id: scope
        label: "Audit scope"
        type: select
        options:
          - "diff"
          - "package"
          - "full"
        default: "diff"
        hint: "diff = staged/last-commit (fast) | package = one package deep | full = whole monorepo (slow)"
    inputs_optional:
      - id: target
        label: "Package path (required for scope=package)"
        type: text
        hint: "e.g. forge/L4-project, agent-framework/runner, platform/backend"
      - id: diff_source
        label: "Diff source (only for scope=diff)"
        type: select
        options:
          - "staged"
          - "last-commit"
          - "branch"
        default: "staged"
    reads:
      - path: "ai-assets/skaile-development/references/audit_checklists.md"
        description: "Sub-agent checklists (must read before dispatch)"
      - path: "ai-assets/skaile-development/references/test_stack_map.md"
        description: "Package → test framework mapping"
      - path: "<package>/CLAUDE.md"
        description: "Per-package architecture and conventions"
      - path: "<package>/REVIEW.md"
        description: "Repo-specific review rules if present"
    produces:
      - path: "_devlog/reports/audit-<date>.json"
        description: "Structured audit result with verdict and findings"
  user_inputs:
    dialog:
      - id: "scope"
        label: "Audit scope"
        type: "select"
        options: ["diff", "package", "full"]
        required: true
        default: "diff"
      - id: "target"
        label: "Package path (for scope=package)"
        type: "text"
        required: false
      - id: "diff_source"
        label: "Diff source (for scope=diff)"
        type: "select"
        options: ["staged", "last-commit", "branch"]
        required: false
        default: "staged"
    files: []
---

# Audit — Monorepo Code Quality Audit

## Overview

Three-phase structured code audit for the skaile-dev monorepo.

| Scope | What runs | Typical time | When to use |
|---|---|---|---|
| `diff` | Build of affected packages + tests of affected packages + diff-only sub-agents | < 2 min | Before commit, in `implement` Phase 4b |
| `package` | Full build + full tests of one package + three parallel sub-agents on the whole package | 5–15 min | Before opening a PR for a package-scoped change |
| `full` | Every package built + tested + three parallel sub-agents over the whole monorepo | 30–60 min | Before cutting a release |

The build phase always runs first. If the build fails, sub-agents are skipped and the verdict is `fail` immediately.

## When to Use

- Before committing a non-trivial change (scope=diff)
- Before opening a pull request (scope=package or diff with branch source)
- Before cutting a release (scope=full, called by `release` Phase 0)
- On-demand investigation of a suspected issue (scope=package)

## When NOT to Use

- For quick diff review without build/test — use `review` instead
- For runtime user-journey validation — use `test-e2e` or `verify-ui`
- For documentation audit only — use `doc --mode audit`

---

ROLE  Monorepo code quality auditor — verifies build and tests pass, then surfaces logic, security, and UI/UX issues via parallel sub-agent analysis. Produces a JSON artifact and a user-facing report.

READS
  ! ai-assets/skaile-development/references/audit_checklists.md
  ! ai-assets/skaile-development/references/test_stack_map.md
  ! <package>/CLAUDE.md                 — for each affected package
  ? <package>/REVIEW.md                 — repo-specific rules
  ? .github/workflows/*.yml             — CI expectations to align with

WRITES
  _devlog/reports/audit-<YYYYMMDD-HHMM>.json    — structured result (always)
  _devlog/reports/audit-<YYYYMMDD-HHMM>.md      — human report (always)

MUST  run build verification before dispatching sub-agents
MUST  stop immediately if build fails — do not waste tokens on sub-agents
MUST  read every affected package's CLAUDE.md before sub-agent dispatch
MUST  dispatch logic, security, UI/UX sub-agents in parallel (scope=package or full)
MUST  write both JSON and markdown artifacts before reporting
MUST  classify every finding by severity and map to audit_checklists.md category
MUST  skip findings already caught by Biome, ESLint, tsc, or `bun audit`
NEVER modify any source files — audit is read-only
NEVER mark as pass with any critical finding
NEVER dispatch sub-agents if build or tests have already failed

EMIT [audit] started scope=<scope> target=<packages> run_id=<uuid>

# ── Phase 1: Scope Resolution ─────────────────────────────────────

STEP 1: Resolve target packages
  IF scope = diff
    - diff_source = staged → $ git diff --cached --name-only
    - diff_source = last-commit → $ git diff HEAD~1..HEAD --name-only
    - diff_source = branch → $ git diff main...HEAD --name-only
    - Map file paths to package roots (use longest-prefix match against package.json locations)
    IF no affected packages → stop: "No changes to audit"
  IF scope = package
    - target input required
    - Validate: <target>/package.json exists
    - Affected packages = [target]
  IF scope = full
    - Affected packages = every workspace in root package.json

STEP 2: Load context
  FOR EACH affected package:
    - Read <package>/CLAUDE.md — note Tech Stack, Key Conventions, Environment Variables
    - Read <package>/REVIEW.md if present — note repo-specific "always check" rules
  - Read audit_checklists.md once
  - Read test_stack_map.md once

EMIT [audit] context_loaded packages=<N>

# ── Phase 2: Build Verification ──────────────────────────────────

STEP 3: Lint every affected package
  - Use Biome for non-platform packages: `bun x biome check <pkg-path>`
  - Use Prettier/ESLint for platform: check package scripts
  - Record: lint.<pkg> = pass | fail
  IF any lint fails
    - Set verdict = fail
    - Jump to STEP 9 (write artifacts with partial result)

STEP 4: Typecheck every affected package
  - `bun x tsc --noEmit -p <pkg>/tsconfig.json` if tsconfig exists
  - Or invoke package's `typecheck` script
  - Record: typecheck.<pkg> = pass | fail
  IF any typecheck fails
    - Set verdict = fail
    - Jump to STEP 9

STEP 5: Build every affected package that has a build script
  - `bun run build --filter <pkg>` or per-package `bun run build`
  - Skip packages without a build script (internal-library, types-only)
  - Record: build.<pkg> = pass | fail | skipped
  IF any build fails
    - Set verdict = fail
    - Jump to STEP 9

EMIT [audit] build_passed packages=<N>

# ── Phase 3: Test Verification ───────────────────────────────────

STEP 6: Run tests per affected package
  FOR EACH affected package with a test configuration (from test_stack_map.md):
    - Execute the mapped run command
    - Capture: total, passed, failed, skipped, duration
  - Run `bun audit` once for dependency vulnerabilities
  - Record: tests.<pkg>, audit.vulnerabilities
  IF any tests fail
    - Set verdict = fail
    - Include failing test names in blocking_issues
    - Jump to STEP 9

EMIT [audit] tests_passed total=<N>

# ── Phase 4: Parallel Sub-Agent Analysis ─────────────────────────

STEP 7: Dispatch three parallel sub-agents
  Only run this step when scope = package OR full.
  For scope = diff, skip to STEP 8 — the diff alone is lightweight enough for a single-pass review.

  Sub-agent A — Logic & Runtime Auditor:
    Checklists: audit_checklists.md § A
    Scope: source files in affected packages (exclude tests, configs, generated)
    Must read: CLAUDE.md of each package first to understand conventions
    Return: JSON array of findings

  Sub-agent B — Security Auditor:
    Checklists: audit_checklists.md § B
    Scope: source files + package.json + .env.example
    Also run: `bun audit --json`
    Return: JSON array of findings + dependency advisories

  Sub-agent C — UI/UX Code Auditor:
    Checklists: audit_checklists.md § C
    Scope: frontend source files only (forge/**/app/, platform/frontend/src/, forge/common-ui/)
    Skip if no frontend files in scope
    Return: JSON array of findings

  Wait for all three sub-agents to return before proceeding.

EMIT [audit] subagents_done critical=<C> high=<H> medium=<M> low=<L>

# ── Phase 5: Verdict & Artifacts ─────────────────────────────────

STEP 8: Synthesize findings
  - Merge sub-agent JSON arrays
  - Deduplicate by (file, line, category)
  - Build blocking_issues = findings with severity in [critical, high]
  - Determine verdict:
    - pass: build clean + tests pass + zero critical + zero high
    - warn: build clean + tests pass + only medium/low findings
    - fail: any build failure OR any test failure OR any critical finding

STEP 9: Write JSON artifact
  OUTPUT _devlog/reports/audit-<YYYYMMDD-HHMM>.json
  {
    "run_id": "<uuid>",
    "started_at": "<iso>",
    "finished_at": "<iso>",
    "scope": "<scope>",
    "target": [<package paths>],
    "build": { "<pkg>": { "lint": "...", "typecheck": "...", "build": "..." } },
    "tests": { "<pkg>": { "total": N, "passed": N, "failed": N } },
    "dependency_vulnerabilities": [...],
    "findings": [
      {
        "severity": "...",
        "category": "...",
        "file": "...",
        "line": N,
        "description": "...",
        "fix": "..."
      }
    ],
    "summary": { "critical": N, "high": N, "medium": N, "low": N },
    "blocking_issues": [...],
    "verdict": "pass|warn|fail"
  }

STEP 10: Write markdown report
  OUTPUT _devlog/reports/audit-<YYYYMMDD-HHMM>.md
  # Audit Report — <date>

  **Scope:** <scope>  **Target:** <packages>  **Verdict:** <verdict>

  ## Build
  | Package | Lint | Typecheck | Build |
  |---|---|---|---|
  ...

  ## Tests
  | Package | Total | Passed | Failed |
  |---|---|---|---|
  ...

  ## Findings
  ### Critical (N)
  - `<file>:<line>` [<category>] <description>
    Fix: <fix>
  ### High (N)
  ...
  ### Medium (N)
  ### Low (N)

  ## Dependency Advisories
  ...

STEP 11: Report to user
  [audit] scope=<scope> → <verdict>
  Build: lint <status> · types <status> · build <status>
  Tests: <pass>/<total>
  Findings: <critical> critical · <high> high · <medium> medium · <low> low
  Full report: _devlog/reports/audit-<stamp>.md

EMIT [audit] completed run_id=<uuid> verdict=<verdict>

CHECKLIST
  - [ ] Build verified before sub-agent dispatch
  - [ ] Tests verified before sub-agent dispatch
  - [ ] Three sub-agents dispatched in parallel (scope=package or full)
  - [ ] JSON and markdown artifacts written
  - [ ] Every finding has severity, file, line, description, fix
  - [ ] No files modified
  - [ ] Verdict matches the rules

---

## Integration

- **Called by:** `implement` (Phase 4b, scope=diff), `release` (Phase 0, scope=full), `quality` (umbrella)
- **Calls:** three parallel sub-agents for logic/security/UI-UX
- **Reads:** `audit_checklists.md`, `test_stack_map.md`, each `<package>/CLAUDE.md`
- **Writes:** `_devlog/reports/audit-*.{json,md}`
