---
name: "ready"
description: "Pre-release readiness gate for the skaile-dev monorepo. Verifies every
  package has README + CLAUDE.md with required sections, tests passing, build clean,
  typecheck clean, CHANGELOG present, and Starlight registration for packages with
  docs/. Produces a blocker/warning table and exits non-zero on any hard failure.
  Called by release as Phase 0."
metadata:
  tags:
  - "readiness"
  - "preflight"
  - "checklist"
  - "release"
  - "gate"
  - "quality"
  - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
    - path: "package.json"
      gate: hard
      description: "Monorepo root"
    inputs_optional:
    - id: scope
      label: "Scope"
      type: select
      options:
      - "all"
      - "package"
      - "domain"
      default: "all"
      hint: "all = every package | package = one path | domain = one ai-assets/<domain>"
    - id: target
      label: "Target (for scope=package or domain)"
      type: text
    reads:
    - path: "ai-assets/skaile-development/references/readiness_criteria.md"
      description: "Per-package and global criteria table"
    - path: "<package>/README.md"
    - path: "<package>/CLAUDE.md"
    - path: "<package>/package.json"
    - path: "<package>/CHANGELOG.md"
    - path: "docs/astro.config.mjs"
    - path: "docs/src/content/config.ts"
    produces:
    - path: "_devlog/reports/readiness-<date>.md"
      description: "Human report"
    - path: "_devlog/reports/readiness-<date>.json"
      description: "Structured result for CI / release to consume"
  user_inputs:
    dialog:
    - id: "scope"
      label: "Scope"
      type: "select"
      options: ["all", "package", "domain"]
      required: false
      default: "all"
    - id: "target"
      label: "Target path"
      type: "text"
      required: false
    files: []
---

# Ready — Pre-Release Readiness Gate

## Overview

Binary pre-release gate. Every package (or a single target) is evaluated against `references/readiness_criteria.md`. A package is **ready** only when every `required` criterion passes. Soft criteria (S1-S5) produce warnings but do not block.

Called by `release` as Phase 0 before any version bump or tag. Also callable standalone.

## When to Use

- Before `release bump` on any package
- Before a major PR merge to main
- As part of a weekly monorepo health check
- When onboarding a new package — confirm it meets the baseline bar

## When NOT to Use

- For code-quality issues (findings, bugs) — use `audit`
- For test coverage gaps — use `test-plan`
- For drift between doc surfaces — use `sync-docs`

---

ROLE  Readiness gatekeeper — verifies per-package and global criteria, produces blocker/warning table.

READS
  ! ai-assets/skaile-development/references/readiness_criteria.md
  ! <package>/README.md, CLAUDE.md, package.json, CHANGELOG.md
  ! docs/astro.config.mjs, docs/src/content/config.ts (for Starlight registration)
  ! git status, git submodule status
  ! root package.json workspaces

WRITES
  _devlog/reports/readiness-<YYYYMMDD-HHMM>.md
  _devlog/reports/readiness-<YYYYMMDD-HHMM>.json

MUST  check every required criterion — never sample
MUST  run real commands (not just file presence) for criteria 8-11 (tests, build, lint, typecheck)
MUST  classify each package by category from readiness_criteria.md before applying checks
MUST  exit non-zero on any blocker so `release` and CI can gate on it
MUST  name the exact fix command for every blocker and warning
NEVER mark a package ready when any required criterion fails
NEVER run destructive commands during the check (no `--fix`, no `git reset`)
NEVER modify any files — report only

EMIT [ready] started scope=<scope>

# ── Phase 1: Scope Resolution ────────────────────────────────────

STEP 1: Build package list
  IF scope = all:
    - Parse root package.json workspaces
    - Also include ai-assets/<domain>/ directories (each has DOMAIN.md)
  IF scope = package:
    - target required
    - Validate <target>/package.json exists
  IF scope = domain:
    - target required, must be under ai-assets/
    - Validate <target>/DOMAIN.md exists

STEP 2: Classify each target
  Using readiness_criteria.md Package Categories table:
  - app | library | internal-library | cli | docs-site | ai-domain | theme
  Store category per package.

# ── Phase 2: Check Each Package ──────────────────────────────────

STEP 3: Per-package required criteria (1–12 from readiness_criteria.md)

  FOR EACH package based on its category:
    Criteria applied (see Package Categories table):

    For `app`, `library`, `cli`: 1-12 required + S1-S5 soft
    For `internal-library`: 3, 5-12 (README optional)
    For `docs-site`: 1, 3, 5
    For `ai-domain`: D1-D5
    For `theme`: 1, 3, 5

    For each applied criterion:
      - Run the check per the "How to verify" column
      - Record pass / fail / soft_warn with an evidence string

    Required-criterion outcomes:
    - criterion passes → ok
    - criterion fails → blocker (package not ready)

    Soft-criterion outcomes:
    - passes → ok
    - fails → warning (does not block)

  Build per-package result:
    {
      "package": "<path>",
      "category": "<cat>",
      "required_pass": N,
      "required_total": M,
      "soft_pass": N,
      "soft_total": M,
      "status": "ready" | "blocked",
      "blockers": [...],
      "warnings": [...]
    }

# ── Phase 3: Global Criteria ─────────────────────────────────────

STEP 4: Global checks (G1-G5 from readiness_criteria.md)
  - G1: parse root package.json workspaces glob; ensure every submodule package.json matched
  - G2: `git submodule status` — look for `+` (uncommitted) or `-` (not initialized) prefixes
  - G3: `bun run format --check` (dry run) — any diff → fail
  - G4: parse root CLAUDE.md domain table vs. `ls ai-assets/*/DOMAIN.md`
  - G5: parse skaile.yaml path fields — verify each path resolves on disk

# ── Phase 4: Artifacts ───────────────────────────────────────────

STEP 5: Write JSON
  OUTPUT _devlog/reports/readiness-<stamp>.json
  {
    "run_id": "<uuid>",
    "run_at": "<iso>",
    "scope": "<scope>",
    "global": { "G1": {...}, "G2": {...}, ... },
    "packages": [ <per-package result> ],
    "summary": {
      "total": N,
      "ready": N,
      "blocked": N,
      "with_warnings": N
    },
    "verdict": "pass|blocked"
  }

STEP 6: Write markdown
  OUTPUT _devlog/reports/readiness-<stamp>.md
  # Readiness Report — <date>

  **Scope:** <scope>  **Verdict:** <pass|blocked>

  ## Global
  | ID | Criterion | Status |
  |---|---|---|
  | G1 | Root workspaces complete | ✓ |
  | G2 | Submodules tracked cleanly | ✗ (forge has uncommitted) |
  ...

  ## Packages
  | Package | Category | Required | Soft | Status |
  |---|---|---|---|---|
  | forge/L4-project | app | 11/12 | 4/5 | BLOCKED |
  ...

  ## Blockers
  ### forge/L4-project
  - [!] 8 Tests fail — 3 failing in tests/e2e/chat.spec.ts
    Fix: `bun x --bun vitest run --project forge-project` and triage
  - [!] 10 Lint not clean — 2 biome errors in app/pages/index.vue
    Fix: `bun x biome check --write forge/L4-project`

  ## Warnings
  ### forge/L4-project
  - [~] S1 CHANGELOG.md missing — create one before releasing

  ## Summary
  Packages ready: 14/16
  Packages blocked: 2/16
  Global: 4/5 passing

  Recommendation: <pass → proceed to release bump | blocked → fix blockers>

STEP 7: Exit
  - Print a short one-line verdict
  - EMIT [ready] completed verdict=<verdict> ready=<N> blocked=<N>
  - Return non-zero to the caller if any blocker exists

CHECKLIST
  - [ ] Every package classified by category
  - [ ] Every applicable required criterion checked (no sampling)
  - [ ] Real subprocesses run for tests/build/lint/typecheck
  - [ ] Global criteria evaluated
  - [ ] JSON and markdown artifacts written
  - [ ] Fix command named for every blocker and warning
  - [ ] Exit code reflects pass/blocked

---

## Integration

- **Called by:** `release` (Phase 0), `quality` (umbrella), CI on main
- **Reads:** `references/readiness_criteria.md`, per-package files
- **Writes:** `_devlog/reports/readiness-*.{md,json}`
