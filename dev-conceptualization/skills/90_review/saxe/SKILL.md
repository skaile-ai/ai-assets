---
name: concept-review
description: "Structure audit + entropy check + doc gardening. Scans _concept/ for completeness, cross-reference integrity, stale artifacts, and broken links. In gardening mode, auto-fixes safe issues. Presents a health report with quality score."
keywords: review, audit, status, entropy, checklist, progress, health, gardening
---

ROLE  Structure Auditor and Doc Gardener — scans _concept/ for completeness, consistency, and entropy.

READS
  _concept/**/*.md                          — all concept documents
  _concept/3_blueprint/3_datamodel/postxl-schema.json  — data model
  _concept/3_blueprint/3_datamodel/feature_map.json    — model-to-feature mapping
  _concept/3_blueprint/3_datamodel/seed.json           — seed data
  ? _concept/.snapshots/manifest.json       — approval snapshots for cascade detection
  ? _concept/quality.json                   — previous quality score
  ? PLANS.md                                — concept progress plan

WRITES
  _concept/quality.json                     — quality score + issue breakdown

REFERENCES
  shared/contracts/concept_structure.md      — expected paths and folders
  shared/contracts/frontmatter.md            — required YAML fields per file type
  shared/contracts/feedback_loop.md          — cross-reference rules
  shared/contracts/golden_principles.md      — mechanical rules to enforce
  shared/contracts/pipeline.json             — dependency graph and step definitions
  shared/docs/OBSERVABILITY.md             — audit event format
  references/checks.md              — detailed check tables and severity rules
  references/gardening.md           — safe vs unsafe auto-fix rules
  references/report_templates.md    — output templates for audit and gardening reports

REQUIRES
  state: _concept/ directory exists with at least one step folder

# ── Mode Selection ──────────────────────────────────────────────────

IF user says "review", "audit", or "check"
  - Run audit mode (STEP 1–9)
ELSE user says "garden", "cleanup", "tidy", or "fix entropy"
  - Run gardening mode (STEP 10–14)

# ── Audit Mode (default) ───────────────────────────────────────────

STEP 1: Scan pipeline structure
  - Read shared/contracts/pipeline.json for step definitions
  - For each step: check folder exists, has content, frontmatter is valid, read status
  EMIT [concept-review] started mode=audit run_id=<uuid>

STEP 2: Check frontmatter compliance
  - For every .md file in _concept/, verify against shared/contracts/frontmatter.md
  - Check: has YAML frontmatter, valid status, valid last_updated (ISO date)
  - Feature files: priority, roles, screens, data_entities present
  - Screen files: implements, data_entities, layout present

STEP 3: Check golden principles
  - For every rule in shared/contracts/golden_principles.md, verify compliance
  - See references/checks.md for the full check table

STEP 4: Check cross-reference integrity
  - For every feature with screens: entries, verify screen files exist and back-reference
  - For every screen with implements: entries, verify feature files exist and back-reference
  - For every model in postxl-schema.json, verify feature_map.json entries resolve

STEP 5: Check cascade warnings (snapshot diff)
  IF _concept/.snapshots/manifest.json exists
    - For each snapshot, diff current files against snapshot versions
    - If file changed since snapshot, read shared/contracts/pipeline.json for downstream dependents
    - If downstream steps also have completed snapshots → CASCADE WARNING
    - Classify severity per references/checks.md (HIGH/MEDIUM/LOW)
  ELSE
    - Skip cascade checks, note "no snapshots found"

STEP 6: Check entropy indicators
  - Files with last_updated > 30 days old → STALE
  - Features with status: draft untouched 14+ days → STAGNANT
  - Features with empty screens: [] after screens exist → MISSING LINK
  - postxl-schema.json models with no feature_map.json entry → ORPHANED MODEL
  - Files outside expected structure → UNEXPECTED FILE
  - Feature groups without matching screen groups → GROUP MISMATCH
  - PLANS.md progress out of sync with _concept/ state → PLAN DRIFT

STEP 7: Calculate quality score
  - Structure completeness:    steps present / steps required
  - Frontmatter compliance:    valid files / total files
  - Golden principles:         rules passing / rules checked
  - Cross-reference integrity: valid links / total links
  - Feature coverage:          features with screens+data / total features
  - Entropy:                   100 - penalty per stale/orphan/mismatch
  - Overall = weighted average of six categories

OUTPUT _concept/quality.json
  {
    "timestamp": "<ISO-8601>",
    "score": <0-100>,
    "breakdown": {
      "structure": <N>, "frontmatter": <N>, "golden_principles": <N>,
      "cross_references": <N>, "coverage": <N>, "entropy": <N>
    },
    "issues": { "critical": <N>, "high": <N>, "medium": <N>, "low": <N> }
  }

STEP 8: Present health report
  - Render report using template in references/report_templates.md
  - Include: quality score table, pipeline completeness, issues list, recommended actions
  EMIT [concept-review] completed mode=audit run_id=<uuid> score=<N> issues=<N>

STEP 9: Offer fixes
  > "Would you like me to fix any of these issues? I can repair cross-references, add missing frontmatter fields, and clean up stale entries."

# ── Gardening Mode (--garden) ──────────────────────────────────────

STEP 10: Run full audit (steps 1–7)
  - Execute STEP 1 through STEP 7 silently to gather all issues
  - Record score_before
  EMIT [concept-review] started mode=gardening run_id=<uuid>

STEP 11: Apply safe auto-fixes
  - For each issue classified as safe in references/gardening.md, apply fix immediately
  - Emit an auto_fix event per change
  EMIT [concept-review] auto_fix file=<path> action=<description>

STEP 12: Recalculate quality score
  - Re-run scoring (STEP 7) to get score_after

STEP 13: Present gardening report
  - Render report using template in references/report_templates.md
  - Include: auto-fixed list, needs-human-attention list, score_before → score_after

STEP 14: Emit completion
  EMIT [concept-review] completed mode=gardening run_id=<uuid> auto_fixes=<N> remaining=<N> score_before=<N> score_after=<N>

# ── Constraints ────────────────────────────────────────────────────

MUST  read all shared/contracts/ contracts before any checks
MUST  classify every issue by severity (CRITICAL, HIGH, MEDIUM, LOW)
MUST  write _concept/quality.json after every run (audit or gardening)
MUST  emit started and completed events with run_id for correlation
NEVER auto-fix unsafe issues in gardening mode (see references/gardening.md)
NEVER delete files — only remove broken references from frontmatter arrays
NEVER modify postxl-schema.json model fields (changes data model semantics)

# ── Recurring Usage ────────────────────────────────────────────────

CHECKLIST
  - [ ] Run after each skill completes — quick pass to catch drift
  - [ ] Run before app-e2e — ensure clean structure before testing
  - [ ] Run weekly — catch entropy accumulation early
  - [ ] Run before merging concept changes — gate on quality score
  - [ ] Block new pipeline steps if score < 70
