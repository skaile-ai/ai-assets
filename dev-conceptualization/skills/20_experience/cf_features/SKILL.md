---
name: features
description: "Use when _concept/01_project/brief.md exists and is approved but _concept/03_features/ is empty or missing. Also when user says 'define features', 'what should the app do', 'plan functionality'."
keywords: [features, requirements, modules, functionality, user-stories, groups, must-have, nice-to-have]
user_inputs:
  dialog:
    - id: feature_priorities
      label: "Priority focus"
      type: select
      options: ["must-have features only", "must-have + nice-to-have", "comprehensive"]
      required: false
      default: "must-have + nice-to-have"
      hint: "How broad should the feature set be?"
  files:
    - "01_project/brief.md"
---

# Features — Feature Planning

## Overview

The **features** skill is the Feature Planning agent. It reads the approved project
brief and produces individual feature files organized in numbered groups under
`_concept/03_features/`. It does NOT write screen specs, data models, brand, or tech stack.

**Phase:** conceptualization / functionality
**Pipeline ID:** `features` (see `cf__shared/pipeline.json`)
**Writes to:** `_concept/03_features/`

## When to Use

- `_concept/01_project/brief.md` exists and is approved, but `_concept/03_features/` is empty or missing
- The user says things like "define features", "what should the app do", "plan functionality"
- The orchestrator dispatches this after the overview step is complete
- The user wants to redo or expand an existing feature set

## When NOT to Use

- No project brief exists yet — run the **overview** skill first
- The brief does not exist yet — run **overview** first
- The user wants to design screens — use the **screens** skill (features must exist first)
- The user wants to define the data model — use the **datamodel** skill (features must exist first)

## Prerequisites

### HARD-GATE

- `_concept/01_project/brief.md` must exist
If the condition fails, stop immediately:

> "No approved project brief found. The overview skill must run first (see pipeline.json: features depends_on overview)."

### Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid `_concept/` paths and naming rules
- `cf__shared/frontmatter.md` — required YAML fields (especially feature fields)
- `cf__shared/feedback_loop.md` — how downstream skills will modify your files
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/01_project/brief.md` must exist
**If gates fail:** Run `cf_concept_overview` first.
**On completion:** Present summary, then orchestrator suggests next steps.

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/01_project/brief.md` | Yes |
| **Must read** | `_concept/01_project/goals.md` | Yes |
| **Optional read** | `_concept/_research/general/competitors.md` | If exists |
| **Optional read** | `_concept/_research/general/audiences.md` | If exists |
| **Optional read** | `_concept/_research/features/user_input.json` | If exists |
| **Never load** | `_concept/06_datamodel/` | — |
| **Never load** | `_concept/07_screens/` | — |

Do not load downstream artifacts. Features are defined from the brief and research, not reverse-engineered from screens or data models.

## Workflow

### Step 1: Read Context

Read `_concept/01_project/brief.md`. Verify it exists.
If it fails the hard-gate, stop with the message above.

Also read `_concept/01_project/goals.md` for success criteria and constraints.

Optionally read `_concept/_research/general/competitors.md` and `_concept/_research/general/audiences.md`
if they exist — competitor feature gaps and audience needs should influence feature priorities.

### Step 2: Feature Identification

For each feature group, create a numbered folder. For each feature, consider:

| # | Question |
|---|----------|
| 1 | What should the user be able to do? |
| 2 | What happens when it works? When it fails? |
| 3 | Who uses this — everyone, or a specific role? |
| 4 | Must-have for launch, or nice-to-have? |

Use the `feature_priorities` user input to calibrate scope:
- **must-have features only** — only features required for a viable first release
- **must-have + nice-to-have** — core features plus enhancements (default)
- **comprehensive** — full feature set including future phases

```bash
mkdir -p _concept/03_features/A_01_user_auth
```

**Output per feature: `_concept/03_features/<X_NN_group>/<feature>.md`**

```yaml
---
priority: must-have
roles: [all_users]
agent_notes: |
  Context from user conversation.
screens: []
data_entities: []
last_updated: YYYY-MM-DD
---

# Feature: <Name>

## Description
What does this feature do?

## User Benefit
Why is this valuable to the user?

## Requirements
- [ ] Requirement 1
- [ ] Requirement 2

## Success Criteria
What proves this feature works?

## Error States
What happens when things go wrong?
```

### Step 3: Emit Events

```
[cf_concept_functionality_features] started
  run_id: <uuid>

[cf_concept_functionality_features] checkpoint phase=features_written
  groups: A_01_user_auth, B_02_dashboard, C_03_settings
  features: 8 total (5 must-have, 3 nice-to-have)
```

### Step 4: Present Summary

Show a summary table:

```
| # | Feature | Group | Priority | Roles |
|---|---------|-------|----------|-------|
| 1 | Login | A_01_user_auth | must-have | all_users |
| 2 | Registration | A_01_user_auth | must-have | all_users |
| 3 | Dashboard | B_02_dashboard | must-have | all_users |
```

### Step 5: Hand Off

Emit:

```
[cf_concept_functionality_features] completed
  run_id: <uuid>
  feature_count: 8
  groups: 3
```

## Outputs

| File | Description |
|------|-------------|
| `_concept/03_features/<X_NN_group>/<feature>.md` | Individual feature spec with frontmatter |

Feature files are organized in letter-and-number prefixed groups (e.g., `A_01_user_auth/`, `B_02_dashboard/`).
The letter prefix (A, B, C, …) increments sequentially alongside the two-digit number.
Group naming must align with `07_screens/` group naming per `cf__shared/concept_structure.md`.

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Writing screen specs inside feature files | The agent conflates "what the app does" with "what the UI looks like" | Features describe functionality and requirements. Screens are a separate skill. Remove any UI layout details. |
| Skipping the hard-gate check | The agent assumes the brief exists without verifying | Always check that brief.md exists and has status != draft before proceeding. |
| Creating features not grounded in the brief | The agent invents features based on general knowledge | Every feature must trace back to the brief's problem statement, audience, or hero flow. If you can't justify it from the brief, don't include it. |
| Flat file structure without groups | The agent writes all features as top-level files | Features must be organized in letter-prefixed numbered groups (A_01_user_auth/, B_02_dashboard/, etc.). Groups map to screen groups downstream. |
| Leaving screens[] and data_entities[] populated | The agent guesses what screens or entities a feature needs | These fields start empty. They are populated by downstream skills via the feedback loop (see cf__shared/feedback_loop.md). |

## Research Mode

When research is active, pattern and competitor research runs in parallel with
this skill. Before writing features, check:

- `_concept/_research/general/patterns.md` — for common UX patterns and best practices in this domain
- `_concept/_research/general/competitors.md` — for competitor feature sets, gaps, and differentiators

Check `_concept/_research/features/user_input.json` for pre-collected user inputs before asking the user.

If research data exists, use it to inform feature priorities and identify gaps.
If it does not exist, proceed without it — research is optional.

**What this skill benefits from researching:** Competitor feature matrices,
common feature patterns for the app's domain, user workflow best practices,
accessibility requirements for the target audience.

## Integration

- **Called by:** orchestrator or standalone (after overview; see `pipeline.json`: `depends_on: ["overview"]`)
- **Feedback from downstream:**
  - **datamodel** skill populates `data_entities[]` in feature frontmatter (see `pipeline.json` feedback_loops)
  - **screens** skill populates `screens[]` in feature frontmatter (see `pipeline.json` feedback_loops)
  - **cf_implement_feature** skill sets `impl_status: implemented` in feature frontmatter
  - **verify** skill sets `impl_status: tested` in feature frontmatter
- **Feeds into:** behaviors (optional), datamodel, screens — all depend on features existing
