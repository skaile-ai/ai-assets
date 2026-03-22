---
name: "audit"
description: "Static code audit. Launches three parallel sub-agents for logic errors, UI/UX issues, and security concerns. Also checks _concept/ structure integrity. Run before e2e or after significant changes."
metadata:
  version: "1.0.0"
  tags:
    - "audit"
    - "security"
    - "bugs"
    - "code-review"
    - "static-analysis"
    - "quality"
    - "entropy"
    - "accessibility"
  source: "MERGED"
  prerequisites:
    reads:
      - path: "package.json"
        description: "Project marker for source existence check (or pyproject.toml equivalent)"
      - path: "_concept"
        description: "Optional: _concept/ structure for cross-reference integrity checks"
    produces:
      - path: "audit-report.md"
        description: "Prioritized bug/risk report (user opt-in)"
---

# Audit — Static Code Analysis

## Overview

The **audit** skill is the Static Code Auditor. It analyzes the codebase without
running it. It does not start servers or modify files (unless asked to fix an issue).
Its output is a prioritized bug/risk report.

## When to Use

- Source code exists and the user wants comprehensive code analysis
- The user says "audit the code", "security check", "find bugs"
- Before E2E testing to catch issues statically
- After significant code changes
- The orchestrator dispatches this as a quality gate

## When NOT to Use

- You want to audit the `_concept/` structure — use **review** instead
- You want to check feature readiness — use **ready** instead
- No source code exists — nothing to audit

## Prerequisites

**Hard gate:** None. Verifies source files exist during pre-flight; stops gracefully if none found.

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | Source code files (`package.json`, `pyproject.toml`, etc.) | Yes |
| **Optional** | `_concept/` (for structure integrity check) | No |
| **Never load** | `_concept/_grounding/`, research files | — |

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Running the app to test | Static analysis only. Use **e2e** for runtime testing. |
| Modifying files without asking | Present report first. Fix only when user says yes. |
| Missing structure check | If `_concept/` exists, include lightweight structure integrity check. |
| False positives on security | Consider framework protections (CSRF tokens, sanitization) before flagging. |
| Overwhelming the user | Prioritize. Show critical and high first. Batch medium and low. |

---

ROLE  Static Code Auditor — analyzes codebase without running it, produces prioritized bug/risk report.

READS
  package.json / pyproject.toml          — project exists (choose appropriate marker)
  src/ or app/ or equivalent             — source code to audit
  ? _concept/**/*.md                     — structure integrity input (if _concept/ exists)

WRITES
  ? audit-report.md                      — exported report (user opt-in)

REFERENCES
  dev-shared/contracts/concept_structure.md  — expected _concept/ paths
  dev-shared/contracts/frontmatter.md        — required YAML fields
  dev-shared/contracts/iron_laws.md          — non-negotiable constraints
  references/analysis_checklists.md          — sub-agent checklists + report template

MUST  never start servers or modify files unless user asks to fix
MUST  wait for all three sub-agents before producing the report
NEVER  modify source code without showing diff and getting confirmation

EMIT [audit] started run_id=<uuid>

# ── Pre-flight ──────────────────────────────────────────────

STEP 1: Verify source exists
  - Look for project markers: package.json, pyproject.toml, vite.config.ts, etc.
  IF no source files found
    - Report "No application source found to audit." and stop

# ── Phase 1: Parallel Analysis ──────────────────────────────

STEP 2: Launch three sub-agents (parallel)
  - Sub-agent 1 — Logic & Runtime Errors
    See references/analysis_checklists.md § Logic & Runtime
  - Sub-agent 2 — UI/UX & Accessibility
    See references/analysis_checklists.md § UI/UX & Accessibility
  - Sub-agent 3 — Security & Data Integrity
    See references/analysis_checklists.md § Security & Data Integrity
  - Wait for all three to complete
  - Each sub-agent returns findings as severity-tagged list

EMIT [audit] checkpoint phase=analysis_complete critical=<C> high=<H> medium=<M> low=<L>

# ── Phase 2: Structure Integrity ────────────────────────────

STEP 3: Check _concept/ structure
  IF _concept/ exists
    - Check cross-reference integrity (features <-> screens)
    - Check for orphaned files
    - Check frontmatter compliance
    - Check for stale files (last_updated > 30 days)
    EMIT [audit] audit_pass check=cross_references
    EMIT [audit] audit_warn check=stale_file file=<path> days=<N>
  ELSE
    - Skip structure checks

# ── Phase 3: Consolidated Report ────────────────────────────

STEP 4: Produce report
  - Merge findings from all sub-agents
  - Sort by severity: Critical > High > Medium > Low
  - Append structure integrity summary
  - Format per references/analysis_checklists.md § Report Template

OUTPUT audit-report.md (user opt-in)
  ## Audit Report
  ### Critical (fix before shipping)
  - [Description] — [file:line] — [category]
  ### High / Medium / Low ...
  ### Structure Integrity
  - Cross-references: N valid, N broken
  ### Summary
  Code issues: N  Structure issues: N

# ── Phase 4: Offer Fixes ────────────────────────────────────

STEP 5: Ask user
  > "Would you like me to fix any of these issues? I can start from critical."
  IF user says yes
    - Fix each issue one at a time
    - Show diff for each fix
    - Wait for confirmation before next fix
    UNTIL all accepted fixes applied

# ── Phase 5: Optional Export ────────────────────────────────

STEP 6: Offer export
  > "Save report to audit-report.md?"
  IF user says yes
    - Write audit-report.md to project root

EMIT [audit] completed run_id=<uuid> issues=<N> critical=<C> high=<H> structure_issues=<S>

CHECKLIST
  - [ ] All three sub-agents completed
  - [ ] Findings sorted by severity
  - [ ] Structure integrity included (when _concept/ exists)
  - [ ] Report presented to user
  - [ ] No files modified without explicit approval
