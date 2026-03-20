---
name: concept-review
description: "Use when you need to check concept health, find broken cross-references, fix stale files, or get a quality score. Also when user says 'audit the concept', 'check for issues', 'cleanup', 'gardening mode'."
keywords: [review, audit, status, entropy, checklist, progress, health, gardening, cleanup, quality]
user_inputs:
  dialog: []
  files: []
---

# Review — Structure Audit and Gardening

## Overview

The **review** skill is the Structure Auditor and Doc Gardener. It scans the
`_concept/` folder for completeness, consistency, and entropy. It produces a
health report with a quality score and can auto-fix safe issues.

**Two modes:**
- **Audit mode** (default): report issues, recommend fixes, ask before changing anything
- **Gardening mode** (`--garden`): auto-fix safe issues, report what was changed

**Writes to:** `_concept/quality.json` + auto-fixes in gardening mode

## When to Use

- You need to check the overall health of the concept
- Cross-references may be broken between features and screens
- Files may be stale or orphaned
- You want a quality score before proceeding to the next pipeline step
- The user says "audit the concept", "check for issues", "cleanup", "gardening mode"
- After each skill completes (quick pass to catch drift)
- Before `cf_test_e2e` (ensure structure is clean)
- Before merging concept changes (gate on quality score)

## When NOT to Use

- You want to audit source code — use the **audit** skill instead
- You want to check feature readiness for E2E — use the **ready** skill
- You want to run the full pipeline — use the **orchestrator**

## Prerequisites

### HARD-GATE

None. Review can run anytime there is a `_concept/` folder.

### Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — expected paths and folders
- `cf__shared/frontmatter.md` — required YAML fields per file type
- `cf__shared/feedback_loop.md` — cross-reference rules
- `cf__shared/golden_principles.md` — mechanical rules to enforce
- `cf__shared/pipeline.json` — dependency graph and step definitions
- `docs/OBSERVABILITY.md` — audit event format
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/` (all folders and files) | Yes |
| **Must read** | `cf__shared/` (all contracts) | Yes |
| **Optional** | `_concept/_research/` | No |
| **Never load** | Source code | — |

## Standalone Mode

This skill can be invoked directly without the orchestrator.
**Gate check:** None
**If gates fail:** N/A
**On completion:** Present summary, then suggest next steps (cf_quality_sync for repairs, or proceed to next pipeline step).

## Mode Selection

If the user says "review", "audit", or "check": run in **audit mode**.
If the user says "garden", "cleanup", "tidy", or "fix entropy": run in **gardening mode**.

---

## Workflow — Audit Mode (default)

### Step 1: Scan Pipeline Structure

Read `cf__shared/pipeline.json` for the step definitions. For each step, check:

| Check | How |
|-------|-----|
| Folder exists | `_concept/<folder>/` present |
| Has content | At least one expected output file exists |
| Frontmatter valid | All `.md` files have required fields |
| Status | Read `status` from frontmatter |

### Step 2: Check Frontmatter Compliance

For every `.md` file in `_concept/`, verify against `cf__shared/frontmatter.md`:

- Has YAML frontmatter
- `status` field exists and is a valid lifecycle value
- `last_updated` field exists and is a valid ISO date
- Feature files: `priority`, `roles`, `screens`, `data_entities` present
- Screen files: `implements`, `data_entities`, `layout` present

### Step 3: Check Golden Principles

For every applicable rule in `cf__shared/golden_principles.md`:

- Entities have `id` (uuid, primary) + `created_at` + `updated_at`
- Enum values are lowercase_snake_case
- Feature groups are sequential (no gaps)
- Screen groups mirror feature group numbers
- Every feature has at least one requirement checkbox
- Every screen has component inventory + states section
- All paths in frontmatter resolve to existing files

### Step 4: Check Cross-Reference Integrity

For every feature with `screens:` entries:
- Does each referenced screen file exist?
- Does that screen's `implements:` list this feature?

For every screen with `implements:` entries:
- Does each referenced feature file exist?
- Does that feature's `screens:` list this screen?

For every entity in `model.json` with `from_features`:
- Does each referenced feature file exist?

### Step 5: Check Cascade Warnings (Snapshot Diff)

If `_concept/.snapshots/manifest.json` exists:

1. For each snapshot, compare current files to their snapshot versions
2. If a file changed since its snapshot was taken:
   - Read `cf__shared/pipeline.json` to find downstream dependents
   - Check if those downstream steps have completed snapshots too
   - If yes — this is a **CASCADE WARNING** — upstream changed after downstream was built

```
## Cascade Warnings

| Changed file | Snapshot | Downstream at risk |
|--------------|----------|--------------------|
| 03_features/01_user_auth/login.md | 03_features_approved (Mar 11) | 06_datamodel, 07_screens |
| 04_brand/tokens.json | 04_brand_approved (Mar 11) | 07_screens, mockups |

Recommended: re-run downstream skills to pick up changes.
```

Severity:
- Frontmatter fields added/removed: HIGH
- Body content edited: MEDIUM
- Only `last_updated` or `status` changed: LOW (no cascade)
- File deleted: HIGH (cross-references will break)
- New file added: MEDIUM

### Step 6: Check Entropy Indicators

- Files with `last_updated` older than 30 days: STALE
- Features with `last_updated` older than 14 days and no `impl_status`: STAGNANT
- Features with empty `screens: []` after screens exist: MISSING LINK
- model.json entities with empty `from_features: []`: ORPHANED ENTITY
- Files outside expected structure: UNEXPECTED FILE
- Feature groups without matching screen groups: GROUP MISMATCH
- PLANS.md progress out of sync with actual `_concept/` state: PLAN DRIFT

### Step 7: Calculate Quality Score

```
Pipeline Health Score: NN/100

  Structure completeness:  NN/100  (steps present / steps required)
  Frontmatter compliance:  NN/100  (valid files / total files)
  Golden principles:       NN/100  (rules passing / rules checked)
  Cross-reference integrity: NN/100  (valid links / total links)
  Feature coverage:        NN/100  (features with screens+data / total)
  Entropy:                 NN/100  (100 - penalty per stale/orphan/mismatch)
```

Write score to `_concept/quality.json`:

```json
{
  "timestamp": "YYYY-MM-DDTHH:mm:ss",
  "score": 78,
  "breakdown": {
    "structure": 85,
    "frontmatter": 60,
    "golden_principles": 90,
    "cross_references": 90,
    "coverage": 70,
    "entropy": 75
  },
  "issues": {
    "critical": 0,
    "high": 1,
    "medium": 2,
    "low": 3
  }
}
```

### Step 8: Present Health Report

```
## Structure Audit Report

### Quality Score: 78/100

| Category | Score | Details |
|----------|-------|---------|
| Structure | 85 | 6 of 7 steps present |
| Frontmatter | 60 | 4 files missing last_updated |
| Golden Principles | 90 | 1 entity missing created_at |
| Cross-references | 90 | 1 broken link |
| Coverage | 70 | 5 of 7 features have screens |
| Entropy | 75 | 2 stale files, 1 orphan |

### Pipeline Completeness
| Step | Status | Files |
|------|--------|-------|
| 01_project | approved | 3 |
| _research | present | N |
| 03_features | 5 approved, 2 draft | 7 |
| 03b_behavior | optional, present | 2 |
| 04_brand | approved | 3 |
| 05_techstack | approved | 1 |
| 06_datamodel | approved | 4 |
| 07_screens | partial | 4 |

### Issues
| # | Severity | Category | Details |
|---|----------|----------|---------|
| 1 | HIGH | Cross-ref | Screen references deleted feature |
| 2 | MEDIUM | Coverage | Feature profile.md has no screens |
| 3 | LOW | Entropy | _research/general/domain.md stale (45 days) |
| 4 | LOW | Principle | Entity "tag" missing created_at |

### Recommended Actions
1. Fix broken cross-reference in 07_screens/01_user_auth/login.md
2. Run `cf_concept_ui_screens` for uncovered features
3. Refresh or remove stale research files
4. Add created_at/updated_at to "tag" entity
```

### Step 9: Ask

> "Would you like me to fix any of these issues? I can repair cross-references,
> add missing frontmatter fields, and clean up stale entries."

---

## Workflow — Gardening Mode (`--garden`)

Gardening mode auto-fixes **safe** issues without asking. It reports what it changed.

### Safe auto-fixes (applied immediately):

| Issue | Fix |
|-------|-----|
| Missing `last_updated` in frontmatter | Set to today's date |
| `last_updated` format invalid | Rewrite as YYYY-MM-DD |
| Missing `status` field | Set to `draft` |
| Missing `screens: []` in feature | Add empty array |
| Missing `data_entities: []` in feature | Add empty array |
| Broken screen reference in feature | Remove the broken entry from `screens:[]` |
| Broken feature reference in screen | Remove from `implements:[]` |
| Broken `from_features` in model.json | Remove the broken path |
| PLANS.md progress doesn't match reality | Update checkboxes to match actual state |

### Unsafe issues (reported but NOT auto-fixed):

| Issue | Why not auto-fix |
|-------|-----------------|
| Missing pipeline steps | Requires running a skill |
| Orphaned entities | User may want them for future features |
| Stale files (30+ days old) | User may still need them |
| Golden principle violations (missing fields in model) | Changes data model semantics |
| Feature group number gaps | Renumbering cascades to screens |

### Gardening Output

```
## Doc Gardening Report

### Auto-fixed (N changes)
- Added last_updated to 03_features/01_user_auth/login.md
- Added last_updated to 03_features/02_dashboard/overview.md
- Removed broken screen reference from 03_features/01_user_auth/login.md
  (07_screens/01_user_auth/old_screen.md no longer exists)
- Updated PLANS.md: checked off 03_features (was unchecked but approved)

### Needs human attention (N issues)
- Entity "tag" in model.json has no from_features — remove or assign?
- _research/general/domain.md is 45 days old — refresh or remove?
- Feature group gap: 01_, 03_ exist but no 02_ — renumber?

### Quality Score: 78 -> 83 (after fixes)
```

### Emit Events

```
[cf_quality_review] started mode=gardening
  run_id: <uuid>

[cf_quality_review] auto_fix file=03_features/01_user_auth/login.md
  action: added last_updated
  value: 2026-03-13

[cf_quality_review] auto_fix file=03_features/01_user_auth/login.md
  action: removed broken screen reference
  removed: 07_screens/01_user_auth/old_screen.md

[cf_quality_review] audit_warn check=stale_file
  file: _research/general/domain.md
  days: 45

[cf_quality_review] completed mode=gardening
  run_id: <uuid>
  auto_fixes: 4
  remaining_issues: 3
  score_before: 78
  score_after: 83
```

## Outputs

| File | Description |
|------|-------------|
| `_concept/quality.json` | Quality score breakdown and issue counts |
| Auto-fixed files (gardening mode) | Frontmatter repairs, broken reference removal |

## Completion Summary

Present to user: files produced (quality.json, any auto-fixed files), key decisions made (which issues were fixed vs. flagged), suggested next steps (which skills are now unblocked — e.g., cf_quality_sync for cross-ref repair, cf_quality_ready for readiness gate, or proceed to next pipeline step if score is acceptable).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Auto-fixing unsafe issues | The agent tries to be helpful and fixes everything | Only fix issues in the safe auto-fix list. Report everything else. |
| Running gardening without reporting | The agent fixes silently | Always report every change made, even in gardening mode. |
| Ignoring _research/ folder | The agent only checks numbered pipeline folders | Check _research/ for stale files and broken references too. |
| Modifying data model semantics | The agent adds missing fields to model.json | Golden principle violations in the data model need human review. |
| Skipping cascade warnings | The agent only checks current state | Always compare against snapshots if they exist. |

## Integration

- **Called by:** orchestrator or standalone (after each phase, before `cf_test_e2e`)
- **Reads from:** `_concept/` (all), `cf__shared/` (all contracts)
- **Feeds into:** quality gate for proceeding to next pipeline step
- **Feedback loops:** Fixes cross-references in both directions (features <-> screens, model -> features)

## Recurring Usage

Gardening mode is designed to run frequently:

- **After each skill completes** — quick pass to catch any drift
- **Before `cf_test_e2e`** — ensure structure is clean before testing
- **Weekly** — catch entropy accumulation early
- **Before merging concept changes** — gate on quality score

The quality score in `_concept/quality.json` tracks health over time.
A score below 70 should block new pipeline steps until issues are resolved.
