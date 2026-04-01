---
name: "sync"
description: "Use when cross-references in _concept/ are broken or out of sync. Scans the entire concept folder, finds broken links, missing bidirectional references, and orphaned entities, then shows a diff before applying fixes."
metadata:
  version: "1.0.0"
  tags:
    - "sync"
    - "cross-references"
    - "repair"
    - "links"
    - "orphans"
    - "consistency"
    - "maintenance"
  source: "MIGRATED"
  prerequisites:
    files:
      - path: "_concept"
        gate: hard
        description: "_concept/ folder must exist to scan cross-references"
    reads:
      - path: "_concept/2_experience/2_features"
        description: "Feature files to check bidirectional links to screens"
      - path: "_concept/2_experience/3_screens"
        description: "Screen files to check implements: references back to features"
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        description: "Data model to find orphaned entities without feature map entries"
      - path: "_concept/3_blueprint/3_datamodel/feature_map.json"
        description: "Feature map to repair model-to-feature cross-references"
    produces:
      - path: "_concept"
        description: "Repaired cross-reference fields across _concept/ files (shown as diff before applying)"
---

# Sync — Cross-Reference Repair

## Overview

Scans the entire `_concept/` folder and repairs broken cross-references. Finds
missing bidirectional links between features and screens, orphaned data model
entities, dangling file references, and inconsistent frontmatter pointers. Shows
a complete diff of proposed changes before applying — safer than `review`'s
gardening mode because every change is previewed.

## When to Use

- After renaming or moving files in `_concept/`
- After deleting features, screens, or entities
- When `review` reports cross-reference issues and you want targeted fixes
- When the user says "sync", "fix links", "repair cross-refs", or "fix references"
- As routine maintenance between pipeline steps
- After manual edits to `_concept/` files

## When NOT to Use

- For a full structure audit with quality scoring — use `review`
- For auto-fixing frontmatter fields (dates) — use `review` (garden mode)
- For generating missing content (features, screens) — use the appropriate pipeline skill
- For code-level fixes — use `audit`

## Prerequisites

**Hard gate:** `_concept/` folder must exist with at least one subfolder. If missing: "No `_concept/` folder found. Run a pipeline skill first."

## Shared Contracts

Before starting, read:
- `skaileup-shared/contracts/concept_structure.md` — valid _concept/ paths and naming rules
- `skaileup-shared/contracts/frontmatter.md` — required YAML fields per file type
- `skaileup-shared/contracts/feedback_loop.md` — cross-reference protocol (authoritative source for link rules)
- `skaileup-shared/contracts/golden_principles.md` — mechanical rules
- `skaileup-shared/contracts/iron_laws.md` — non-negotiable constraints

## Context Budget

| Source | Priority |
|--------|----------|
| `_concept/2_experience/2_features/**/*.md` (frontmatter) | Required |
| `_concept/2_experience/3_screens/**/*.md` (frontmatter) | Required |
| `_concept/3_blueprint/3_datamodel/model.json` | Required |
| `_concept/3_blueprint/3_datamodel/feature_map.json` | Required |
| `skaileup-shared/contracts/feedback_loop.md` | Required |
| All other `_concept/**/*.md` (frontmatter only) | Optional |

## Standalone Mode

**Gate check:** `_concept/` folder must exist with at least one subfolder
**If gates fail:** Run a pipeline skill first to create `_concept/` content
**On completion:** Present summary, then suggest `review` for full health check.

## Workflow

### Step 1: Inventory All Artifacts

Scan `_concept/` and build a complete artifact registry:

```
| Type | Path | Cross-refs |
|------|------|-----------|
| feature | 2_experience/2_features/01_user_auth/login.md | screens: [...] |
| screen | 2_experience/3_screens/01_user_auth/login.md | implements: [...] |
| entity | model.json → user | — |
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

### Step 3: Check Data Model Links (feature_map.json + model.json → features)

For each entry in `feature_map.json`:
- Does the referenced feature file exist?
- Does that feature's `data_entities:` include this entity name?

For each entity in `model.json`:
- Does its corresponding `feature_map.json` entry exist?

```
| Entity | feature_map ref | Feature exists | Feature refs entity | Status |
|--------|-----------------|----------------|---------------------|--------|
| user | 2_experience/2_features/01_user_auth/login.md | Yes | Yes | OK |
| task | 2_experience/2_features/02_tasks/create.md | Yes | No | MISSING_BACK |
| tag | 2_experience/2_features/99_deleted/tags.md | No | — | BROKEN |
```

### Step 4: Detect Orphans

- **Orphaned entities:** entities in model.json with no `feature_map.json` entry or broken entry
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
File: _concept/2_experience/3_screens/01_user_auth/register.md
  implements:
-   []
+   [2_experience/2_features/01_user_auth/registration.md]

### 2. Remove broken reference
File: _concept/2_experience/2_features/03_settings/preferences.md
  screens:
-   [2_experience/3_screens/03_settings/old_prefs.md]
+   []

### 3. Add missing entity reference
File: _concept/2_experience/2_features/02_tasks/create.md
  data_entities:
-   []
+   [task]

### 4. Remove broken feature_map entry
File: _concept/3_blueprint/3_datamodel/feature_map.json → entity "tag"
  feature: 2_experience/2_features/99_deleted/tags.md → removed (file not found)
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
[sync] fix applied file=2_experience/3_screens/01_user_auth/register.md
  action: added back-link to implements
  target: 2_experience/2_features/01_user_auth/registration.md
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
| 1 | 2_experience/3_screens/01_user_auth/register.md | Added back-link | MISSING_BACK |
| 2 | 2_experience/2_features/03_settings/preferences.md | Removed broken ref | BROKEN |
| 3 | 3_blueprint/3_datamodel/feature_map.json (tag) | Removed broken entry | BROKEN |

### Orphans Detected (not auto-fixed)
| Type | Artifact | Recommendation |
|------|----------|---------------|
| Entity | tag | Assign to a feature or remove from model |
| Screen | 2_experience/3_screens/04_archive/list.md | No feature references it — remove or create feature |

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

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Auto-fixing without showing diff | Always show the complete diff first and wait for approval |
| Deleting orphaned entities | Report orphans but never delete — user may have future plans |
| Creating missing files | Sync only repairs references, not missing content — suggest the right skill |
| Fixing frontmatter fields beyond cross-refs | Only fix cross-reference fields (screens, implements, data_entities, feature_map) |
| Ignoring model.json / feature_map.json | Both are part of the cross-reference contract |
| Running after partial pipeline | Warn the user if pipeline is incomplete — orphans may be expected |

EMIT  [sync] started run_id=<uuid>
EMIT  [sync] checkpoint phase=scan_complete files=<N> issues=<N>
EMIT  [sync] fix applied file=<path> action=<type>
EMIT  [sync] completed run_id=<uuid> issues_found=<N> fixes_applied=<N> orphans_detected=<N>
