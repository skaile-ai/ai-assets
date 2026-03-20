---
name: brand-behavioral
description: "Brand behavioral identity. Use when the visual brand exists and you need to define communication tone, error message style, empty state messaging, micro-copy guidelines, and notification voice. Produces behavioral.md + copy_guidelines.md in _concept/04_brand/."
keywords: brand, tone, voice, copy, microcopy, errors, empty-states, notifications, messaging, ux-writing
user_inputs:
  dialog:
    - id: tone
      label: "What tone should your app speak in?"
      type: select
      options: [friendly, professional, playful, serious]
      default: friendly
    - id: formality_level
      label: "How formal should the language be? (1 = casual chat, 5 = corporate)"
      type: select
      options: ["1", "2", "3", "4", "5"]
      default: "3"
  files: []
---

# App Brand — Behavioral Identity

## Overview

Defines how the app communicates with users: tone of voice, error messages,
empty states, notifications, confirmations, tooltips, and all micro-copy.
This is the verbal companion to the visual brand — it ensures every string
in the UI feels like it was written by the same person.

## When to Use

- Visual brand identity exists (`04_brand/identity.md` + `tokens.json`)
- Features are defined (you need to know what screens and states exist)
- You want consistent messaging across all UI states before implementation
- The app has user-facing text beyond simple labels

## When NOT to Use

- No visual brand yet — run `cf_concept_brand_visual` first
- No features defined — run `cf_concept_functionality_features` first
- The app is an API-only service with no user-facing UI
- You only need a color palette or visual identity (that is `cf_concept_brand_visual`)

## Prerequisites

| Artifact | Path | Missing? Run | Gate |
|----------|------|-------------|------|
| Project brief | `_concept/01_project/brief.md` | `cf_concept_overview` | <HARD-GATE> |
| Features | `_concept/03_features/**/*.md` | `cf_concept_functionality_features` | <HARD-GATE> |
| Brand identity | `_concept/04_brand/identity.md` | `cf_concept_brand_visual` | <HARD-GATE> |
| Brand tokens | `_concept/04_brand/tokens.json` | `cf_concept_brand_visual` | <HARD-GATE> |
| Screens | `_concept/07_screens/**/*.md` | `cf_concept_ui_screens` | recommended |
| Data model | `_concept/06_datamodel/seed.json` | `cf_concept_datamodel` | recommended |

If any <HARD-GATE> artifact is missing, stop immediately and name the prerequisite skill.

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/01_project/brief.md`, `_concept/03_features/**/*.md`, `_concept/04_brand/identity.md`, `_concept/04_brand/tokens.json` must all exist
**If gates fail:** Run `cf_concept_overview`, `cf_concept_functionality_features`, or `cf_concept_brand_visual` as needed.
**On completion:** Present summary, then orchestrator suggests next steps.

## Context Budget

| Source | Priority | Token estimate |
|--------|----------|---------------|
| `01_project/brief.md` | must read | ~500 |
| `04_brand/identity.md` | must read | ~1500 |
| `04_brand/tokens.json` | must read | ~500 |
| `03_features/**/*.md` | must read | ~2000 |
| `07_screens/**/*.md` | skim for states | ~2000 |
| `06_datamodel/seed.json` | skim for empty/edge scenarios | ~1000 |

**Total budget:** ~7500 tokens input. Keep output under 3000 tokens per file.

## Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid paths (`04_brand/`)
- `cf__shared/frontmatter.md` — brand frontmatter fields
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Workflow

### Step 1: Read Context

Read the project brief to understand audience and problem domain.
Read brand identity to understand the established aesthetic and mood.
Read features to identify all user-facing states that need copy.

Optionally read screens (if they exist) to enumerate concrete UI states:
error states, empty states, loading states, success confirmations.

Optionally read seed.json to understand what "empty" and "edge case"
scenarios look like — these directly inform empty-state messaging.

### Step 2: Ask Tone Questions

Ask the user (one at a time, building on answers):

| # | Question | What it reveals |
|---|----------|----------------|
| 1 | "What tone should your app speak in? (friendly / professional / playful / serious)" | Base tone |
| 2 | "How formal? Scale of 1-5. (1 = 'Oops, that didn't work' / 5 = 'An error has occurred. Please contact support.')" | Formality calibration |
| 3 | "Should the app use humor in error states, or keep it straight?" | Error tone boundary |
| 4 | "First person ('We couldn't find that') or impersonal ('Item not found')?" | Voice perspective |
| 5 | "Any words or phrases you want to avoid? (e.g., 'oops', 'please', jargon)" | Exclusion list |

### Step 3: Derive Copy Patterns

For each category, generate 3-4 example strings that demonstrate the tone:

**Categories:**
- Error messages (validation, server error, not found, permission denied)
- Empty states (no data yet, no results, first-time use)
- Success confirmations (saved, created, deleted, sent)
- Loading states (fetching, processing, uploading)
- Notifications (info, warning, success, error)
- Tooltips and help text
- Button labels and CTAs
- Destructive action confirmations ("Are you sure?")

Present a sample table to the user:

> "Here's how your app will sound:
>
> | Situation | Message |
> |-----------|---------|
> | Empty task list | 'No tasks yet. Create your first one to get started.' |
> | Save success | 'Changes saved.' |
> | Delete confirmation | 'Delete this task? This can't be undone.' |
> | Server error | 'Something went wrong. Try again in a moment.' |
> | Permission denied | 'You don't have access to this page.' |
>
> Does this feel right? Adjust any example and I'll recalibrate."

### Step 4: Write Artifacts

```bash
mkdir -p _concept/04_brand
```

**Output: `_concept/04_brand/behavioral.md`**

```yaml
---
tone: friendly
formality_level: 3
voice_perspective: first_person_plural
last_updated: YYYY-MM-DD
---
```

Body contains:
- Tone definition (one paragraph explaining the voice)
- Formality scale with examples at each level
- Voice perspective rules
- Humor boundaries
- Exclusion list (words/phrases to avoid)
- Per-category tone rules with 3+ examples each

**Output: `_concept/04_brand/copy_guidelines.md`**

```yaml
---
last_updated: YYYY-MM-DD
---
```

Body contains the practical reference for implementers:
- Error message templates (with placeholders: `{entity}`, `{action}`)
- Empty state templates per screen type
- Notification templates per severity
- Button label conventions (verb-first, max length)
- Confirmation dialog patterns
- Tooltip writing rules (max length, when to show)
- Capitalization rules (sentence case vs title case)
- Punctuation rules (periods in messages, exclamation marks policy)
- Do's and don'ts table

### Step 5: Emit Events

```
[cf_concept_brand_behavioral] started
  run_id: <uuid>
  reads: 01_project/brief.md, 04_brand/identity.md, 03_features/

[cf_concept_brand_behavioral] checkpoint phase=tone_calibrated
  tone: friendly
  formality: 3
  perspective: first_person_plural

[cf_concept_brand_behavioral] completed
  run_id: <uuid>
  artifacts: 04_brand/behavioral.md, 04_brand/copy_guidelines.md
```

## Outputs

| File | Purpose |
|------|---------|
| `_concept/04_brand/behavioral.md` | Tone definition, voice rules, per-category examples |
| `_concept/04_brand/copy_guidelines.md` | Practical templates for implementers |

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Generic corporate tone for a playful app | Defaulting to "safe" professional voice | Match tone to brand mood from `identity.md` |
| Writing copy without reading features | Guessing at what states exist | Read all features + screens first to enumerate real states |
| Ignoring the visual brand mood | Treating copy as separate from visual identity | A "dark editorial" brand needs different copy than a "playful pastel" brand |
| Too many examples per category | Trying to cover every edge case | 3-4 examples per category, plus a template pattern |
| Inventing UI text that contradicts features | Making up scenarios not in the concept | Every example must trace to a real feature or screen state |
| Skipping destructive action copy | Forgetting delete/archive/remove confirmations | Always include confirmation patterns for irreversible actions |

## Integration

- **Called by:** orchestrator or standalone
- **Upstream:** reads from `01_project/`, `03_features/`, `04_brand/` (visual)
- **Downstream:** consumed by `cf_concept_ui_screens`, `cf_concept_mock`, `cf_implement_feature`
- **Phase:** brand (optional, runs after `cf_concept_brand_visual`)
- **Pipeline position:** parallel with screens, after brand visual identity
- **Feedback loop:** none (does not modify upstream files)
