---
name: app-audit
description: "Static code audit. Launches three parallel sub-agents for logic errors, UI/UX issues, and security concerns. Also checks _concept/ structure integrity. Run before app-e2e or after significant changes."
keywords: audit, security, bugs, code-review, static-analysis, quality, entropy
---

ROLE  Static Code Auditor — analyzes codebase without running it, produces prioritized bug/risk report.

READS
  package.json                             — project exists
  vite.config.ts                           — frontend config
  _concept/3_blueprint/3_datamodel/postxl-schema.json — schema reference
  ? _concept/**/*.md                       — structure integrity input

WRITES
  ? audit-report.md                        — exported report (user opt-in)

REFERENCES
  shared/contracts/concept_structure.md             — expected _concept/ paths
  shared/contracts/frontmatter.md                   — required YAML fields
  references/analysis_checklists.md        — sub-agent checklists + report template

MUST  never start servers or modify files unless user asks to fix
MUST  wait for all three sub-agents before producing the report
NEVER  modify source code without showing diff and getting confirmation

# ── Pre-flight ──────────────────────────────────────────────

STEP 1: Verify source exists
  - Look for project markers: package.json, vite.config.ts, postxl-schema.json
  IF no source files found
    - Report "No application source found to audit." and stop
  EMIT [app-audit] started run_id=<uuid>

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

# ── Phase 2: Structure Integrity ────────────────────────────

STEP 3: Check _concept/ structure
  IF _concept/ exists
    - Check cross-reference integrity (features <-> screens)
    - Check for orphaned files
    - Check frontmatter compliance
    - Check for stale files (last_updated > 30 days)
    EMIT [app-audit] audit_pass check=cross_references
    EMIT [app-audit] audit_warn check=stale_file file=<path> days=<N>
  ELSE
    - Skip structure checks

# ── Phase 3: Consolidated Report ────────────────────────────

STEP 4: Produce report
  - Merge findings from all sub-agents
  - Sort by severity: Critical > High > Medium > Low
  - Append structure integrity summary
  - Format per references/analysis_checklists.md § Report Template
  EMIT [app-audit] completed issues=<N> critical=<C> high=<H> medium=<M> low=<L>

OUTPUT audit-report.md
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

CHECKLIST
  - [ ] All three sub-agents completed
  - [ ] Findings sorted by severity
  - [ ] Structure integrity included (when _concept/ exists)
  - [ ] Report presented to user
  - [ ] No files modified without explicit approval
