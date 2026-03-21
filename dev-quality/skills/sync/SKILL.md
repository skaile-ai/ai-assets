---
name: sync
description: "Use when cross-references in _concept/ are broken or out of sync. Scans the entire concept folder, finds broken links, missing bidirectional references, and orphaned entities, then shows a diff before applying fixes."
keywords: [sync, cross-references, repair, links, orphans, consistency, maintenance]
user_inputs:
  dialog: []
  files: []
metadata:
  stage: alpha
  requires:
  - quality-contract
---

# App Sync — Cross-Reference Repair

## Overview

Scans the entire `_concept/` folder and repairs broken cross-references. Finds
missing bidirectional links between features and screens, orphaned data model
entities, dangling file references, and inconsistent frontmatter pointers. Shows
a complete diff of proposed changes before applying — safer than review's
gardening mode because every change is previewed.

## When to Use

- After renaming or moving files in `_concept/`
- After deleting features, screens, or entities
- When `cf_quality_review` reports cross-reference issues and you want targeted fixes
- When the user says "sync", "fix links", "repair cross-refs", or "fix references"
- As routine maintenance between pipeline steps
- After manual edits to `_concept/` files

## When NOT to Use

- For a full structure audit with quality scoring — use `cf_quality_review`
- For auto-fixing frontmatter fields (dates, status) — use `cf_quality_review --garden`
- For generating missing content (features, screens) — use the appropriate pipeline skill
- For code-level fixes — use `cf_quality_audit`

## Prerequisites

No hard dependencies. This skill can run at any time as long as `_concept/` exists
with at least some content.

<HARD-GATE> `_concept/` folder must exist with at least one subfolder. If missing: "No _concept/ folder found. Run a pipeline skill first."

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid _concept/ paths and naming rules
- `cf__shared/frontmatter.md` — required YAML fields per file type
- `cf__shared/feedback_loop.md` — cross-reference protocol (authoritative source for link rules)
- `cf__shared/golden_principles.md` — mechanical rules
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

| Source | Token estimate | Priority |
|--------|---------------|----------|
| `_concept/03_features/**/*.md` (frontmatter) | ~1500 | Required |
| `_concept/07_screens/**/*.md` (frontmatter) | ~1500 | Required |
| `_concept/06_datamodel/model.json` | ~2000 | Required |
| `cf__shared/feedback_loop.md` | ~500 | Required |
| All other `_concept/**/*.md` (frontmatter only) | ~1000 | Optional |

## Standalone Mode

This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/` folder must exist with at least one subfolder
**If gates fail:** Run a pipeline skill first to create `_concept/` content
**On completion:** Present summary, then suggest next steps (cf_quality_review for full health check).

## Workflow

### Step 1: Inventory All Artifacts

Scan `_concept/` and build a complete artifact registry:

```
| Type | Path | Status | Cross-refs |
|------|------|--------|-----------|
| feature | 03_features/01_user_auth/login.md | approved | screens: [07_screens/01_user_auth/login.md] |
| feature | 03_features/02_dashboard/overview.md | draft | screens: [] |
| screen | 07_screens/01_user_auth/login.md | draft | implements: [03_features/01_user_auth/login.md] |
| entity | model.json → user | — | from_features: [03_features/01_user_auth/login.md] |
```

### Step 2: Check Bidirectional Links (Features ↔ Screens)

For every feature file with `screens:` entries:
- Does each referenced screen file exist?
- Does that screen's `implements:` list this feature?

For every screen file with `implements:` entries:
- Does each referenced feature file exist?
- Does that feature's `screens:` list this screen?

Produce a link table:

```
| Source | Target | Forward Link | Back Link | Status |
|--------|--------|-------------|-----------|--------|
| feature/login.md | screen/login.md | Yes | Yes | OK |
| feature/login.md | screen/register.md | Yes | No | MISSING_BACK |
| screen/profile.md | feature/profile.md | Yes | No | MISSING_BACK |
| feature/settings.md | screen/old_prefs.md | Yes | — | BROKEN (file deleted) |
```

### Step 3: Check Data Model Links (model.json → features)

For each entity in `model.json` with `from_features`:
- Does each referenced feature file exist?
- Does that feature's `data_entities:` include this entity name?

```
| Entity | from_features ref | Feature exists | Feature refs entity | Status |
|--------|------------------|----------------|--------------------|---------|
| user | 03_features/01_user_auth/login.md | Yes | Yes | OK |
| task | 03_features/02_tasks/create.md | Yes | No | MISSING_BACK |
| tag | 03_features/99_deleted/tags.md | No | — | BROKEN |
```

### Step 4: Detect Orphans

- **Orphaned entities:** entities in model.json with empty or broken `from_features`
- **Orphaned screens:** screens with no valid `implements:` references
- **Orphaned features:** features referenced by nothing (no screens, no entities)
- **Orphaned files:** files in `_concept/` that don't belong to any known structure

### Step 5: Check Group Alignment

Verify that feature group numbers align with screen group numbers:

```
| Group # | Features Folder | Screens Folder | Status |
|---------|----------------|----------------|--------|
| 01 | 01_user_auth | 01_user_auth | OK |
| 02 | 02_dashboard | 02_dashboard | OK |
| 03 | 03_tasks | — | MISSING_SCREENS |
```

### Step 6: Build Diff

Compile all proposed changes into a clear diff format:

```
## Proposed Changes (N fixes)

### 1. Add missing back-link
File: _concept/07_screens/01_user_auth/register.md
  implements:
-   []
+   [03_features/01_user_auth/registration.md]

### 2. Remove broken reference
File: _concept/03_features/03_settings/preferences.md
  screens:
-   [07_screens/03_settings/old_prefs.md]
+   []

### 3. Add missing entity reference
File: _concept/03_features/02_tasks/create.md
  data_entities:
-   []
+   [task]

### 4. Remove broken from_features
File: _concept/06_datamodel/model.json → entity "tag"
  from_features:
-   [03_features/99_deleted/tags.md]
+   []
```

### Step 7: Present Diff and Ask

> "Found N cross-reference issues. Here are the proposed fixes:"
>
> [show diff from Step 6]
>
> "Apply all fixes? Or select specific ones?"

Options:
- **Apply all** — apply every proposed change
- **Select** — user picks which fixes to apply
- **Skip** — do nothing, just save the report

### Step 8: Apply Fixes

For each approved fix:
1. Read the file
2. Apply the frontmatter change
3. Write the file
4. Emit event

```
[cf_quality_sync] fix applied file=07_screens/01_user_auth/register.md
  action: added back-link to implements
  target: 03_features/01_user_auth/registration.md
```

### Step 9: Generate Sync Report

```
## Sync Report

### Summary
Files scanned: N
Issues found: N
Fixes applied: N
Remaining (user skipped or unsafe): N

### Fixes Applied
| # | File | Change | Type |
|---|------|--------|------|
| 1 | 07_screens/01_user_auth/register.md | Added back-link | MISSING_BACK |
| 2 | 03_features/03_settings/preferences.md | Removed broken ref | BROKEN |
| 3 | 06_datamodel/model.json (tag) | Removed broken from_features | BROKEN |

### Orphans Detected (not auto-fixed)
| Type | Artifact | Recommendation |
|------|----------|---------------|
| Entity | tag | Assign to a feature or remove from model |
| Screen | 07_screens/04_archive/list.md | No feature references it — remove or create feature |

### Group Alignment
| Group | Features | Screens | Status |
|-------|----------|---------|--------|
| 01_user_auth | 3 files | 3 files | OK |
| 02_dashboard | 2 files | 1 file | PARTIAL |
| 03_tasks | 4 files | 0 files | MISSING_SCREENS |
```

## Outputs

- Repaired cross-references in `_concept/` files (with user approval)
- Sync report (displayed to user)
- Optional export to `sync-report.md`

## Completion Summary

Present to user: files produced (repaired cross-references, sync report), key decisions made (which fixes were applied, which orphans were flagged), suggested next steps (which skills are now unblocked — e.g., cf_quality_review for full health check, or proceed to next pipeline step).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Auto-fixing without showing diff | Moving too fast | Always show the complete diff first and wait for approval |
| Deleting orphaned entities | Assuming orphan = unwanted | Report orphans but never delete — user may have future plans |
| Creating missing files | Overstepping scope | Sync only repairs references, not missing content — suggest the right skill |
| Fixing frontmatter fields beyond cross-refs | Scope creep into gardening | Only fix cross-reference fields (screens, implements, data_entities, from_features) |
| Ignoring model.json | Only checking markdown files | model.json `from_features` is part of the cross-reference contract |
| Running after partial pipeline | Many false orphans | Warn the user if pipeline is incomplete — orphans may be expected |

## Integration

- **Upstream:** Any pipeline skill that modifies `_concept/` files
- **Downstream:** `cf_quality_review` (sync first, then audit for full health report)
- **Replaces nothing:** complements `cf_quality_review` gardening mode with a preview-first approach
- **Events:**
  ```
  [cf_quality_sync] started
    run_id: <uuid>
  [cf_quality_sync] checkpoint phase=scan_complete
    files: N, issues: N
  [cf_quality_sync] fix applied file=<path> action=<type>
  [cf_quality_sync] completed
    run_id: <uuid>
    issues_found: N
    fixes_applied: N
    orphans_detected: N
  ```
