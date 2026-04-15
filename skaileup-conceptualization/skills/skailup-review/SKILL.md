---
name: "skailup-review"
description: "Structure audit + entropy check + doc gardening for _concept/. In audit mode: scan completeness, cross-reference integrity, golden principle compliance, and entropy; produce quality.json with score. In gardening mode: auto-fix safe issues and report what changed."
metadata:
  version: "1.0.0"
  tags:
    - "review"
    - "audit"
    - "status"
    - "entropy"
    - "checklist"
    - "progress"
    - "health"
    - "gardening"
    - "cleanup"
    - "quality"
  source: "MERGED"
  prerequisites:
    inputs_optional:
      - id: mode
        label: "Audit or garden?"
        type: select
        options:
          - audit
          - garden
        default: audit
        hint: "audit = report issues; garden = auto-fix safe issues"
    reads:
      - path: "_concept"
        description: "All _concept/ files scanned for completeness, cross-references, and entropy"
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        description: "Data model for cross-reference integrity checks"
      - path: "_concept/3_blueprint/3_datamodel/feature_map.json"
        description: "Feature map for entity-to-feature cross-reference validation"
    produces:
      - path: "_concept/quality.json"
        description: "Health report with quality score (0–100) and issue inventory"
  user_inputs:
    dialog:
      - id: "mode"
        label: "Audit or garden?"
        type: "select"
        options:
          - "audit"
          - "garden"
        required: false
        default: "audit"
        hint: "audit = report issues; garden = auto-fix safe issues"
    files: []
---

# Review — Structure Audit and Gardening

## Overview

The **review** skill is the Structure Auditor and Doc Gardener. It scans `_concept/`
for completeness, consistency, and entropy. It produces a health report with a quality
score (0–100) and can auto-fix safe issues in gardening mode.

**Two modes:**
- **Audit mode** (default): report issues, recommend fixes, ask before changing anything
- **Gardening mode** (`--garden`): auto-fix safe issues, report what was changed

## When to Use

- Check overall health of the concept
- Cross-references may be broken between features and screens
- Files may be stale or orphaned
- Want a quality score before proceeding to the next pipeline step
- User says "audit the concept", "check for issues", "cleanup", "gardening mode"
- After each skill completes — quick pass to catch drift
- Before `e2e` — ensure structure is clean before testing
- Before merging concept changes — gate on quality score

## When NOT to Use

- Auditing source code — use the `audit` quality skill instead
- Checking readiness for E2E — use the `ready` quality skill
- Running the full pipeline — use the `concept-orchestrator`

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md`,
`skaileup-shared/contracts/frontmatter.md`, `skaileup-shared/contracts/feedback_loop.md`,
and `skaileup-shared/contracts/golden_principles.md` before running any checks.

**Hard gate:** None — review can run anytime `_concept/` exists.

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/**/*.md` | Yes |
| Must read | `_concept/3_blueprint/3_datamodel/model.json` | If exists |
| Must read | `_concept/3_blueprint/3_datamodel/feature_map.json` | If exists |
| Check if present | `_concept/quality.json` | No (previous score) |
| Check if present | `PLANS.md` | No (plan drift check) |
| Never load | Source code | — |

## Standalone Mode

**Gate check:** None.
**On completion:** Present report and suggest next steps (`sync` for cross-ref repair, `ready` for readiness gate, or proceed if score ≥ 70).

---

ROLE  Structure Auditor and Doc Gardener — scans _concept/ for completeness, consistency, and entropy.

READS
  _concept/**/*.md                                         — all concept documents
  ? _concept/3_blueprint/3_datamodel/model.json           — data model (canonical cross-ref)
  ? _concept/3_blueprint/3_datamodel/feature_map.json     — model-to-feature mapping
  ? _concept/3_blueprint/3_datamodel/seed.json            — seed data (format checks)
  ? _concept/quality.json                                  — previous quality score
  ? PLANS.md                                               — concept progress plan

WRITES
  _concept/quality.json                                    — quality score + issue breakdown

REFERENCES
  skaileup-shared/contracts/concept_structure.md      — expected phase-grouped paths and folders
  skaileup-shared/contracts/frontmatter.md            — required YAML fields per file type
  skaileup-shared/contracts/feedback_loop.md          — cross-reference rules
  skaileup-shared/contracts/golden_principles.md      — mechanical rules to enforce
  skaileup-shared/docs/OBSERVABILITY.md              — audit event format
  references/checks.md                           — detailed check tables and severity rules
  references/gardening.md                        — safe vs unsafe auto-fix rules
  references/report_templates.md                 — output templates for audit and gardening reports

MUST  read all skaileup-shared/contracts/ contracts before any checks
MUST  classify every issue by severity (CRITICAL, HIGH, MEDIUM, LOW)
MUST  write _concept/quality.json after every run (audit or gardening)
MUST  emit started and completed events with run_id for correlation
NEVER auto-fix unsafe issues in gardening mode (see references/gardening.md)
NEVER delete files — only remove broken references from frontmatter arrays
NEVER modify model.json or model.dbml directly (changes data model semantics)

EMIT  [review] started mode=<audit|gardening> run_id=<uuid>

# ── Mode Selection ──────────────────────────────────────────────────

IF user says "review", "audit", or "check"
  - Run audit mode (STEP 1–9)
IF user says "garden", "cleanup", "tidy", or "fix entropy"
  - Run gardening mode (STEP 10–14)

# ── Audit Mode (default) ─────────────────────────────────────────────

STEP 1: Scan pipeline structure
  - Use skaileup-shared/contracts/concept_structure.md for expected phase-grouped paths
  - For each expected folder, check:
    - Folder exists in _concept/
    - Has at least one expected output file
    - All .md files have valid frontmatter
  See references/checks.md for the full check table

STEP 2: Check frontmatter compliance
  - For every .md file in _concept/, verify against skaileup-shared/contracts/frontmatter.md:
    - Has YAML frontmatter delimiters (`---`)
    - `last_updated` field exists and is a valid ISO date (YYYY-MM-DD)
    - NO `status` field (globally removed — flag as issue if present)
    - Feature files: `priority`, `story_refs`, `roles`, `screens`, `data_entities` present
    - Screen files: `implements`, `data_entities`, `layout` present
    - Architecture: `apps`, `custom_modules`, `protocols`, `external_integrations` present
    - Stack: `platform`, `frontend`, `backend`, `database`, `tech_stack_skill` present

STEP 3: Check golden principles
  - For every applicable rule in skaileup-shared/contracts/golden_principles.md:
    - Entity IDs: `snake_case`
    - Field names: `snake_case`
    - Enum values: `PascalCase`
    - Relation fields: `_id` suffix
    - Feature groups: sequential, no gaps
    - Screen groups mirror feature group numbers
    - Every feature has at least one requirement
    - All paths in frontmatter resolve to existing files
  See references/checks.md for the complete check table

STEP 4: Check cross-reference integrity
  - For every feature with `screens:` entries:
    - Does each referenced screen file exist?
    - Does that screen's `implements:` list this feature?
  - For every screen with `implements:` entries:
    - Does each referenced feature file exist?
    - Does that feature's `screens:` list this screen?
  - If feature_map.json exists:
    - For each entity mapped, do the referenced feature files exist?
  - If model.json exists:
    - For each entity, is there a corresponding entry in feature_map.json?

STEP 5: Check entropy indicators
  - Files with `last_updated` > 30 days old → STALE
  - Features with empty `screens: []` when screens folder exists → MISSING LINK
  - model.json entities with no feature_map.json entry → ORPHANED ENTITY
  - Files outside expected `_concept/` structure → UNEXPECTED FILE
  - Feature groups without matching screen groups → GROUP MISMATCH
  - PLANS.md progress out of sync with actual _concept/ state → PLAN DRIFT
  - Any file with `status:` field → STALE STATUS FIELD (globally removed)
  See references/checks.md for full entropy indicator table

STEP 6: Calculate quality score
  - Structure completeness:     steps present / steps required
  - Frontmatter compliance:     valid files / total files
  - Golden principles:          rules passing / rules checked
  - Cross-reference integrity:  valid links / total links
  - Feature coverage:           features with screens+data / total features
  - Entropy:                    100 - penalty per stale/orphan/mismatch
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

STEP 7: Present health report
  - Render report using template in references/report_templates.md
  - Include: quality score table, pipeline completeness, issues list, recommended actions
  EMIT  [review] completed mode=audit run_id=<uuid> score=<N> issues=<N>

STEP 8: Offer fixes
  > "Would you like me to fix any of these issues? I can repair cross-references,
  > add missing frontmatter fields, and clean up stale entries."

# ── Gardening Mode (--garden) ───────────────────────────────────────

STEP 10: Run full audit silently (steps 1–6)
  - Execute STEP 1 through STEP 6 to gather all issues
  - Record score_before
  EMIT  [review] started mode=gardening run_id=<uuid>

STEP 11: Apply safe auto-fixes
  - For each issue classified as safe in references/gardening.md, apply fix immediately
  - Emit an auto_fix event per change
  EMIT  [review] auto_fix file=<path> action=<description> value=<new_value>

STEP 12: Recalculate quality score
  - Re-run scoring (STEP 6) to get score_after

STEP 13: Present gardening report
  - Render report using template in references/report_templates.md
  - Include: auto-fixed list, needs-human-attention list, score_before → score_after

STEP 14: Emit completion
  EMIT  [review] completed mode=gardening run_id=<uuid> auto_fixes=<N> remaining=<N> score_before=<N> score_after=<N>

CHECKLIST
  - [ ] All skaileup-shared/contracts/ read before checks
  - [ ] quality.json written with all 6 breakdown fields
  - [ ] Every issue classified by severity (CRITICAL/HIGH/MEDIUM/LOW)
  - [ ] Audit: offer to fix; Gardening: report every change made
  - [ ] Score < 70 blocks new pipeline steps (flag to user)
  - [ ] No files deleted — only broken references removed from arrays
  - [ ] model.json/model.dbml not directly modified

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Auto-fixing unsafe issues | Only fix issues in references/gardening.md safe list. Report everything else. |
| Running gardening without reporting | Always report every change made, even in gardening mode. |
| Checking status field | `status` is globally removed — flag its presence as an issue, not its absence. |
| Ignoring _grounding/ folder | Check _grounding/ for stale files and broken references too. |
| Modifying data model semantics | Golden principle violations in model.json/model.dbml need human review. |
| Checking postxl-schema.json | Use model.json + feature_map.json as canonical data model targets. |

## Integration

- **Called by:** `concept-orchestrator` or standalone (after each phase, before `e2e`)
- **Reads:** `_concept/` (all), `skaileup-shared/contracts/` (all)
- **Writes:** `_concept/quality.json`; auto-fixes in gardening mode
- **Feeds into:** quality gate for proceeding to next pipeline step

## Recurring Usage

Gardening mode is designed to run frequently:
- **After each skill completes** — quick pass to catch drift
- **Before `e2e`** — ensure structure is clean before testing
- **Before merging concept changes** — gate on quality score ≥ 70
