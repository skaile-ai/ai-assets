---
name: "brand-behavioral"
description: "Use when the visual brand exists and you need to define communication tone, error message style, empty state messaging, micro-copy guidelines, and notification voice. Produces behavioral.md + copy_guidelines.md in _concept/1_discovery/2_brand/."
metadata:
  version: "1.0.0"
  tags:
    - "brand"
    - "tone"
    - "voice"
    - "copy"
    - "microcopy"
    - "errors"
    - "empty-states"
    - "notifications"
    - "messaging"
    - "ux-writing"
  source: "MERGED"
  user_inputs:
    dialog:
      - id: "tone"
        label: "What tone should your app speak in?"
        type: "select"
        options:
          - "friendly"
          - "professional"
          - "playful"
          - "serious"
        default: "friendly"
      - id: "formality_level"
        label: "How formal should the language be? (1 = casual chat, 5 = corporate)"
        type: "select"
        options:
          - "1"
          - "2"
          - "3"
          - "4"
          - "5"
        default: "3"
    files: []
---

# Brand Behavioral Identity

## Overview

The **brand-behavioral** skill defines how the app communicates with users: tone of voice,
error messages, empty states, notifications, confirmations, tooltips, and all micro-copy.
This is the verbal companion to the visual brand — it ensures every string in the UI feels
like it was written by the same person.

## When to Use

- Visual brand identity exists (`1_discovery/2_brand/identity.md` + `tokens.json`)
- Features are defined (`2_experience/2_features/`)
- You want consistent messaging across all UI states before implementation
- The app has user-facing text beyond simple labels

## When NOT to Use

- No visual brand yet — run `brand-visual` first
- No features defined — run `features` first
- The app is an API-only service with no user-facing UI
- You only need a color palette or visual identity — use `brand-visual`

## Prerequisites

**REQUIRED BACKGROUND:** Read `dev-shared/contracts/concept_structure.md` and
`dev-shared/contracts/frontmatter.md` before proceeding.

| Artifact | Path | Missing? Run | Gate |
|---|---|---|---|
| Project brief | `_concept/1_discovery/1_overview/brief.md` | `overview` | Hard |
| Features | `_concept/2_experience/2_features/**/*.md` | `features` | Hard |
| Brand identity | `_concept/1_discovery/2_brand/identity.md` | `brand-visual` | Hard |
| Brand tokens | `_concept/1_discovery/2_brand/tokens.json` | `brand-visual` | Hard |
| Screens | `_concept/2_experience/3_screens/**/*.md` | `screens` | Soft (skim for states) |
| Seed data | `_concept/3_blueprint/3_datamodel/seed.json` | `datamodel` | Soft (empty/edge scenarios) |

If any hard gate artifact is missing, stop immediately and name the prerequisite skill.

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/1_discovery/1_overview/brief.md` | Yes |
| Must read | `_concept/1_discovery/2_brand/identity.md` | Yes |
| Must read | `_concept/1_discovery/2_brand/tokens.json` | Yes |
| Must read | `_concept/2_experience/2_features/**/*.md` | Yes |
| Skim for states | `_concept/2_experience/3_screens/**/*.md` | No |
| Skim for scenarios | `_concept/3_blueprint/3_datamodel/seed.json` | No |

## Standalone Mode

**Gate check:** brief.md, features, identity.md, tokens.json must all exist.
**On completion:** Present summary. Suggest screens or implementation as next step.

---

ROLE  Brand Voice Writer — defines tone, copy patterns, and UX writing guidelines
      to ensure every UI string feels consistent with the visual brand.

READS
  _concept/1_discovery/1_overview/brief.md              — app name, audience, problem
  _concept/1_discovery/2_brand/identity.md              — established aesthetic, mood, tone of voice hints
  _concept/1_discovery/2_brand/tokens.json              — mode, atmosphere context
  _concept/2_experience/2_features/**/*.md              — all user-facing states that need copy
  ? _concept/2_experience/3_screens/**/*.md             — concrete UI states to enumerate
  ? _concept/3_blueprint/3_datamodel/seed.json          — empty/edge scenarios for empty-state copy

WRITES
  _concept/1_discovery/2_brand/behavioral.md            — tone definition, voice rules, per-category examples
  _concept/1_discovery/2_brand/copy_guidelines.md       — practical templates for implementers

REFERENCES
  dev-shared/contracts/concept_structure.md      — valid _concept/ paths
  dev-shared/contracts/frontmatter.md            — brand frontmatter fields

MUST  match tone to brand mood from identity.md — never default to generic professional voice
MUST  read all features + screens before writing to enumerate real UI states
MUST  include templates for all 8 copy categories (errors, empty states, confirmations, loading, notifications, tooltips, buttons, destructive actions)
NEVER invent copy examples for UI states that don't exist in the concept
NEVER treat copy as separate from visual identity — they share a tone
NEVER skip destructive action confirmations

EMIT  [brand-behavioral] started run_id=<uuid>

STEP 1: Read context
  - Read brief.md for audience and domain
  - Read identity.md for established aesthetic, mood, and any tone hints
  - Read all feature files to enumerate user-facing states
  IF screens exist
    - Skim for concrete UI states: error states, empty states, loading, confirmations
  IF seed.json exists
    - Skim for empty and edge case scenarios — informs empty-state copy

STEP 2: Ask tone calibration questions
  - If user_inputs were pre-collected, use them and skip those questions
  - Otherwise ask one at a time:
    1. "What tone should your app speak in? (friendly / professional / playful / serious)"
    2. "How formal? Scale of 1-5. (1 = 'Oops, that didn't work' / 5 = 'An error has occurred. Please contact support.')"
    3. "Should the app use humor in error states, or keep it straight?"
    4. "First person ('We couldn't find that') or impersonal ('Item not found')?"
    5. "Any words or phrases you want to avoid? (e.g., 'oops', 'please', jargon)"
  EMIT  [brand-behavioral] checkpoint phase=tone_calibrated tone=<tone> formality=<N> perspective=<perspective>

STEP 3: Present sample copy and calibrate
  Generate 3-4 example strings per category, present a sample table:
  > "Here's how your app will sound:
  >
  > | Situation | Message |
  > |---|---|
  > | Empty task list | 'No tasks yet. Create your first one to get started.' |
  > | Save success | 'Changes saved.' |
  > | Delete confirmation | 'Delete this task? This can't be undone.' |
  > | Server error | 'Something went wrong. Try again in a moment.' |
  > | Permission denied | 'You don't have access to this page.' |
  >
  > Does this feel right? Adjust any example and I'll recalibrate."

STEP 4: Write artifacts
  $ mkdir -p _concept/1_discovery/2_brand

  OUTPUT _concept/1_discovery/2_brand/behavioral.md
    ---
    tone: <friendly|professional|playful|serious>
    formality_level: <1-5>
    voice_perspective: <first_person_plural|first_person_singular|impersonal>
    last_updated: <YYYY-MM-DD>
    ---
    <tone definition — one paragraph explaining the voice>
    <formality scale with examples at each level>
    <voice perspective rules>
    <humor boundaries>
    <exclusion list — words/phrases to avoid>
    <per-category tone rules with 3+ examples each:
      errors, empty states, confirmations, loading, notifications,
      tooltips, button labels, destructive action confirmations>

  OUTPUT _concept/1_discovery/2_brand/copy_guidelines.md
    ---
    last_updated: <YYYY-MM-DD>
    ---
    <practical reference for implementers:>
    - Error message templates (with placeholders: {entity}, {action})
    - Empty state templates per screen type
    - Notification templates per severity
    - Button label conventions (verb-first, max length)
    - Confirmation dialog patterns
    - Tooltip writing rules (max length, when to show)
    - Capitalization rules (sentence case vs title case)
    - Punctuation rules (periods in messages, exclamation marks policy)
    - Do's and don'ts table

EMIT  [brand-behavioral] completed run_id=<uuid> artifacts=behavioral.md,copy_guidelines.md

STEP 5: Present summary
  > "Brand voice written to `_concept/1_discovery/2_brand/`:
  >
  > - **behavioral.md** — tone rules + per-category examples
  > - **copy_guidelines.md** — templates for implementers
  >
  > Voice: [tone] at formality [N], [perspective]
  >
  > Next steps:
  > - Run `screens` if you haven't yet — screen specs will reference these copy patterns
  > - Run `concept-orchestrator` to continue the full pipeline"

CHECKLIST
  - [ ] Tone calibrated with user
  - [ ] All 8 copy categories covered (errors, empty states, confirmations, loading, notifications, tooltips, buttons, destructive actions)
  - [ ] Templates use real feature and screen states — not invented scenarios
  - [ ] behavioral.md has correct frontmatter (tone, formality_level, voice_perspective, last_updated)
  - [ ] copy_guidelines.md includes button label conventions and capitalization rules

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Generic corporate tone for a playful app | Match tone to brand mood from identity.md |
| Writing copy without reading features | Read all features + screens first to enumerate real states |
| Ignoring the visual brand mood | A "dark editorial" brand needs different copy than a "playful pastel" brand |
| Too many examples per category | 3-4 examples per category plus a template pattern |
| Inventing copy for screens that don't exist | Every example must trace to a real feature or screen state |
| Skipping destructive action copy | Always include confirmation patterns for irreversible actions |

## Integration

- **Called by:** `concept-orchestrator` or standalone (optional, runs after `brand-visual`)
- **Requires:** `brief.md`, `features/`, `identity.md`, `tokens.json`
- **Feeds into:** `screens`, `mock`, `scaffold` — UI text should reference these guidelines
- **Feedback loop:** none (does not modify upstream files)
