---
name: "skailup-ingest-seeds"
description: "Scans the _seeds/ directory, auto-classifies each file by content analysis, maps files to artifact slots, validates against schemas, and updates concept.yaml with seed statuses. Use after skailup-onboard when the user has provided existing material."
metadata:
  version: "1.0.0"
  tags:
    - "seeds"
    - "ingestion"
    - "classification"
    - "artifacts"
  source: "NEW"
  prerequisites:
    reads:
      - path: "_concept/_seeds/"
        description: "User-provided material to classify and ingest"
      - path: "_concept/_grounding/onboarding/profile.yaml"
        description: "Project profile for context-aware classification"
    produces:
      - path: "_concept/concept.yaml"
        description: "Updated manifest with seed mappings and statuses"
  artifacts:
    requires:
      - id: onboarding-profile
    produces:
      - id: concept-manifest
        description: "Updated concept.yaml with seed artifact entries"
    parameters:
      depth:
        type: enum
        values: [none, light, medium, max]
        default: medium
---

# Ingest Seeds — Seed Classification and Manifest Update

## Overview

The **skailup-ingest-seeds** skill scans the `_concept/_seeds/` directory,
auto-classifies each file by content analysis, maps files to known artifact
slots, validates them against artifact schemas, and updates `concept.yaml`
with the resulting seed statuses.

After this skill runs, downstream skills know which artifacts are already
seeded — they can skip data collection for those slots and read from the
grounding material instead.

This skill does NOT generate concept artifacts. It classifies and registers
what the user has already provided.

## When to Use

- The user has provided existing material (documents, designs, code, data models)
- `skailup-onboard` has already run and `profile.yaml` exists
- The orchestrator dispatches this after onboarding when `_seeds/` is non-empty
- The user says "I have existing docs", "I have a design spec", "use these files"

## When NOT to Use

- `_concept/_seeds/` is empty or does not exist — skip this skill and proceed to the pipeline
- `skailup-onboard` has not run yet — run it first so profile.yaml is available
- Seeds have already been ingested and `concept.yaml` already has seed entries — re-run only if new seeds were added

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md` before starting.

**Hard gate:** `_concept/_grounding/onboarding/profile.yaml` must exist.

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/_grounding/onboarding/profile.yaml` | Yes |
| Must scan | `_concept/_seeds/` | Yes |
| Read if present | `_concept/concept.yaml` | No (merge, not overwrite) |
| Must read | `skaileup-shared/contracts/concept_structure.md` | Yes |
| Never load | `_concept/1_discovery/`, `_concept/2_experience/`, `_concept/3_blueprint/` | — |

## Standalone Mode

**Gate check:** `_concept/_grounding/onboarding/profile.yaml` must exist.
**On completion:** Show classification results and suggest next steps (orchestrator to start the pipeline).

---

ROLE  Seed Ingestion agent — scans `_concept/_seeds/`, classifies files,
      maps to artifact slots, and updates `concept.yaml`.

READS
  _concept/_grounding/onboarding/profile.yaml    — project type + context for classification
  _concept/_seeds/                               — user-provided seed files
  ? _concept/concept.yaml                        — existing manifest to merge into

WRITES
  _concept/concept.yaml                          — updated manifest with seed entries

REFERENCES
  skaileup-shared/contracts/concept_structure.md — canonical _concept/ paths and artifact slots

MUST  read profile.yaml before classifying any file — project_type affects classification
MUST  assign a seed_state to every file (seed, seed-partial, seed-reference)
MUST  merge into existing concept.yaml if it exists — never overwrite the whole file
MUST  report classification results to the user before writing
NEVER move or rename files in _seeds/ — treat them as read-only input
NEVER create concept pipeline artifacts (brief.md, stack.md, etc.) — this skill only updates concept.yaml
NEVER block on unclassifiable files — assign seed_state: seed-reference and note uncertainty

EMIT [ingest-seeds] started run_id=<uuid>

# -- Step 1: Gate Check ------------------------------------------------------

STEP 1: Verify prerequisites
  IF _concept/_grounding/onboarding/profile.yaml does not exist
    - STOP with message:
      > "Onboarding profile not found. Please run `skailup-onboard` first, then
      > re-run seed ingestion."
  ELSE
    - Read profile.yaml (project_name, project_type, tier_preset)

  IF _concept/_seeds/ does not exist OR is empty
    - Report:
      > "No seeds found in _concept/_seeds/. Nothing to ingest.
      > Run the orchestrator to start the concept pipeline."
    - EXIT cleanly (do not write concept.yaml)

EMIT [ingest-seeds] checkpoint phase=gate_passed project=<project_name>

# -- Step 2: Scan Seeds Directory --------------------------------------------

STEP 2: List all files in _seeds/
  - Recursively list all files under _concept/_seeds/
  - Collect: filename, extension, relative path, approximate size
  - Report count to user:
    > "Found [N] files in _seeds/. Classifying..."

# -- Step 3: Classify Each File ----------------------------------------------

STEP 3: Auto-classify files by content analysis
  For each file:
  a. Read the file content (or a representative excerpt for large files)
  b. Apply classification rules (see Classification Logic section below)
  c. Assign:
     - artifact_slot: the target artifact category
     - seed_state: seed | seed-partial | seed-reference
     - confidence: high | medium | low
     - notes: explanation if confidence is medium or low

  AFTER classifying all files:
  - Present the classification table to the user:
    | File | Classified As | Seed State | Confidence | Notes |
    |---|---|---|---|---|
    | <filename> | <artifact_slot> | <state> | <confidence> | <notes or —> |

  - Ask for corrections:
    > "Does this look right? You can correct any misclassification before I update the manifest."
  UNTIL user confirms (or makes corrections and confirms)

EMIT [ingest-seeds] checkpoint phase=classified count=<N>

# -- Step 4: Write concept.yaml ----------------------------------------------

STEP 4: Update concept.yaml
  IF _concept/concept.yaml does not exist:
    - Create it with the seeds section only

  IF _concept/concept.yaml exists:
    - Read the file
    - Merge seed entries under the `seeds:` key — do not touch other sections

  OUTPUT _concept/concept.yaml (seeds section):
    ```yaml
    seeds:
      - file: "_concept/_seeds/<relative path>"
        artifact_slot: "<slot name>"
        seed_state: "<seed|seed-partial|seed-reference>"
        confidence: "<high|medium|low>"
        notes: "<classification notes or null>"
        ingested_at: "<YYYY-MM-DD>"
    ```

  Write one entry per file. Preserve all existing content in concept.yaml outside the `seeds:` key.

STEP 5: Report and hand off
  > "Seed ingestion complete.
  >
  > [N] files classified:
  >   - [count] fully seeded (seed)
  >   - [count] partially seeded (seed-partial)
  >   - [count] reference material (seed-reference)
  >
  > Seeded artifact slots: [list]
  > Unresolved files: [list with notes, or 'none']
  >
  > concept.yaml has been updated. Downstream skills will skip data collection
  > for seeded slots and read from your provided material instead.
  >
  > Next step: run `skailup-orchestrator` to start the concept pipeline."

EMIT [ingest-seeds] completed run_id=<uuid> seeds_total=<N> slots_seeded=<count>

CHECKLIST
  - [ ] profile.yaml exists and was read before classification
  - [ ] _seeds/ was scanned recursively
  - [ ] Every file has been assigned artifact_slot, seed_state, and confidence
  - [ ] Classification results were presented to the user and confirmed
  - [ ] concept.yaml was merged (not overwritten) with seed entries
  - [ ] No files in _seeds/ were moved or renamed

---

## Classification Logic

Classification uses filename, extension, and content signals. Project type from
`profile.yaml` is used as a tiebreaker when a file could match multiple slots.

### Artifact Slot Mapping

| Content Signals | Artifact Slot |
|---|---|
| Headings like "Architecture", "System Design", "Component Diagram"; PlantUML/Mermaid blocks; C4 notation | `architecture` |
| Prisma schema (`model`, `datasource`, `generator`); DBML (`Table`, `Ref`); SQL DDL (`CREATE TABLE`); ERD diagrams | `datamodel` |
| Numbered feature lists; "Feature:", "Epic:", "User Story:", bullet lists with user-facing descriptions; acceptance criteria | `features` |
| Brand guidelines; color palettes; typography specs; design tokens (JSON/CSS variables); logos (image files); style guides | `brand` |
| Wireframe images (PNG/JPG with UI-like filenames); Figma exports; mockup files; screen layout descriptions | `screens` |
| Competitor analysis docs; "vs." comparisons; market landscape notes; "Alternatives" sections | `competitors` (research artifact) |
| Code snippets (.ts, .py, .js, .go, etc.); partial implementations; reference implementations | `reference-code` (not mapped to an artifact slot) |
| Does not match any above pattern | `unknown` |

### Classification Notes

- A file may match multiple signals. Apply the strongest signal. If tie: use project_type from profile.yaml to bias.
- Image files (.png, .jpg, .svg) without context: classify as `screens` with confidence: low.
- Markdown files with mixed content: classify by the dominant section type.
- `reference-code` files are recorded in concept.yaml but NOT mapped to an artifact slot — they are available as context only.
- `unknown` files: assign seed_state: seed-reference, confidence: low, and note in the classification table.

### Seed States

| State | Meaning |
|---|---|
| `seed` | File is complete and authoritative for its artifact slot. Downstream skills should use it directly and skip data collection for that slot. |
| `seed-partial` | File covers part of the slot but is incomplete. Downstream skills should use it as a starting point and collect the missing sections. |
| `seed-reference` | File is informational but not authoritative (e.g., competitor docs, code snippets, unclassified files). Downstream skills may read it for context but must not treat it as a completed artifact. |

### Assigning Seed State

Assign seed_state based on completeness signals:

- `seed`: file has a clear structure, covers the expected sections for its slot, and appears complete
- `seed-partial`: file exists but is missing key sections, has placeholder content ("TBD", "TODO"), or covers only a subset of the slot
- `seed-reference`: file does not map to a known artifact slot, or maps weakly (reference-code, unknown, competitors)

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip seed ingestion entirely — proceed as if no seeds exist |
| `light` | List all files with filenames and extensions only; assign artifact_slot and seed_state without reading content; skip validation; present list to user |
| `medium` | Full classification (read content, apply rules, validate format); update concept.yaml (default) |
| `max` | Full classification + format validation + generate a one-paragraph summary for each seed file noting what it covers and what is missing |

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Moving or renaming files in _seeds/ | Treat _seeds/ as read-only — never touch the originals |
| Overwriting existing concept.yaml sections | Read the file first and merge only the `seeds:` key |
| Blocking on unclassifiable files | Assign seed-reference with confidence: low and note it — always proceed |
| Presenting raw classification before user review | Always show the classification table and wait for confirmation |
| Treating reference-code as a concept artifact | reference-code is context only — it does not fill an artifact slot |
| Skipping the gate check | profile.yaml must exist — do not guess at project_type |

## Integration

- **Called by:** `skailup-orchestrator` after `skailup-onboard`, or standalone by the user
- **Requires:** `_concept/_grounding/onboarding/profile.yaml` (hard gate)
- **Feeds into:** all downstream skills via `concept.yaml` seed entries
- **Hands off to:** `skailup-orchestrator` (to start the concept pipeline)
