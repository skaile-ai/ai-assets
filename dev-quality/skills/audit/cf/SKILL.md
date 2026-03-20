---
name: audit
description: "Use when source code exists and user wants a comprehensive code analysis for logic errors, security issues, and accessibility. Also when user says 'audit the code', 'security check', 'find bugs'."
keywords: [audit, security, bugs, code-review, static-analysis, quality, entropy, accessibility]
user_inputs:
  dialog: []
  files: []
---

# Audit — Static Code Analysis

## Overview

The **audit** skill is the Static Code Auditor. It analyzes the codebase without
running it. It does not start servers or modify files (unless asked to fix an issue).
Its output is a prioritized bug/risk report.

**Phase:** quality / code
**Writes to:** audit report (presented to user, optionally saved to file)

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

### HARD-GATE

None. This skill can run on any codebase. It verifies source files exist during
pre-flight and stops gracefully if none are found:

> "No application source found to audit."

### Shared Contracts

For structure audit capabilities, read:
- `cf__shared/concept_structure.md` — expected _concept/ paths
- `cf__shared/frontmatter.md` — required YAML fields
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | Source code files (package.json, nuxt.config.ts, etc.) | Yes |
| **Optional** | `_concept/` (for structure integrity check) | No |
| **Never load** | `_concept/_research/`, research files | — |

## Standalone Mode

This skill can be invoked directly without the orchestrator.
**Gate check:** None
**If gates fail:** N/A
**On completion:** Present summary, then suggest next steps (fix critical issues, then run cf_test_e2e or cf_quality_ready).

## Workflow

### Pre-flight

Verify source files exist (`package.json`, `nuxt.config.ts`, `pyproject.toml`, etc.).
If no source found: "No application source found to audit."

### Phase 1: Parallel Analysis (Three Sub-agents)

Launch all three simultaneously:

#### Sub-agent 1: Logic and Runtime Errors
- Incorrect conditionals, off-by-one errors
- Missing null/undefined checks
- Race conditions in async code
- Unhandled promise rejections
- Missing error boundaries
- Incorrect type assumptions

#### Sub-agent 2: UI/UX and Accessibility
- Forms missing error/loading states
- Broken responsive layouts
- Missing ARIA attributes
- Poor contrast ratios (WCAG AA)
- Missing focus states
- Missing empty states

#### Sub-agent 3: Security and Data Integrity
- SQL/NoSQL injection risks
- XSS vulnerabilities
- Missing auth checks on protected routes
- Exposed secrets (hardcoded keys)
- Missing input validation at API boundaries
- CSRF vulnerabilities

**Wait for all three to complete.**

### Phase 2: Structure Integrity Check

If `_concept/` exists, also run a lightweight structure audit:

- Check cross-reference integrity (features <-> screens)
- Check for orphaned files
- Check frontmatter compliance
- Check for stale files (last_updated > 30 days)

This is a subset of what the **review** skill does — just the mechanical checks.

```
[cf_quality_audit] audit_pass check=cross_references
[cf_quality_audit] audit_warn check=stale_file file=_research/general/domain.md days=45
```

### Phase 3: Consolidated Report

```
## Audit Report

### Critical (fix before shipping)
- [Description] — [file:line] — [category]

### High (fix before E2E testing)
- [Description] — [file:line] — [category]

### Medium (fix before launch)
- [Description] — [file:line] — [category]

### Low (nice to fix)
- [Description] — [file:line] — [category]

### Structure Integrity
- Cross-references: N valid, N broken
- Stale files: N
- Frontmatter compliance: N%

### Summary
Code issues: N (C critical, H high, M medium, L low)
Structure issues: N
```

### Phase 4: Ask

> "Would you like me to fix any of these issues? I can start from critical."

If yes, fix each issue, show diff, confirm before next.

### Emit Events

```
[cf_quality_audit] started
  run_id: <uuid>

[cf_quality_audit] checkpoint phase=analysis_complete
  critical: N
  high: N
  medium: N
  low: N

[cf_quality_audit] completed
  run_id: <uuid>
  total_issues: N
  critical: N
  structure_issues: N
```

## Outputs

| Output | Description |
|--------|-------------|
| Audit report | Prioritized list of issues by severity |
| `audit-report.md` (optional) | Saved report if user requests |

## Completion Summary

Present to user: files produced (audit report, optionally audit-report.md), key decisions made (issue categorization and prioritization), suggested next steps (which skills are now unblocked — e.g., cf_test_e2e after fixes are applied, cf_quality_ready for readiness gate).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Running the app to test | The agent starts a dev server | This is static analysis. Do not run the app. Use **cf_test_e2e** for runtime testing. |
| Modifying files without asking | The agent fixes issues proactively | Present the report first. Only fix when the user says yes. |
| Missing the structure check | The agent only audits code | If `_concept/` exists, include a lightweight structure integrity check. |
| False positives on security | The agent flags framework-handled security | Consider framework protections (CSRF tokens, sanitization) before flagging. |
| Overwhelming the user | The agent lists 200 issues | Prioritize. Show critical and high first. Batch medium and low. |

## Integration

- **Called by:** orchestrator or standalone
- **Reads from:** source code, optionally `_concept/` for structure checks
- **Feeds into:** `cf_test_e2e` (fixes should be applied before E2E), `cf_quality_ready` (quality gate)
- **Feedback loops:** None. This is a read-only analysis.
