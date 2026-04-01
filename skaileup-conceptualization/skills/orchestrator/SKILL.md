---
name: "orchestrator"
description: "Full concept pipeline orchestrator. Runs the complete conceptualization pipeline (Discovery -> Experience -> Blueprint) with checkpoint approvals, PLANS.md tracking, and auto-review mode. Dispatches sub-skills for each pipeline step and manages user communication."
metadata:
  version: "1.0.0"
  tags:
    - "orchestrate"
    - "concept"
    - "pipeline"
    - "plan"
    - "discovery"
    - "experience"
    - "blueprint"
    - "checkpoint"
  source: "MERGED"
  prerequisites:
    inputs_optional:
      - id: complexity_tier
        label: "Project complexity (controls checkpoint frequency)"
        type: select
        options:
          - small
          - standard
          - complex
        default: standard
        hint: "small = consolidated checkpoints, standard = phase-level checkpoints, complex = per-skill checkpoints"
    reads:
      - path: "_concept/PLANS.md"
        description: "Existing plan for resuming an interrupted pipeline session"
      - path: "_concept/1_discovery/1_overview/brief.md"
        description: "Existing brief to resume from a known pipeline stage"
    produces:
      - path: "_concept/PLANS.md"
        description: "Structured concept plan tracking pipeline phases and checkpoints"
      - path: "_concept/decisions.md"
        description: "Key decisions made during the pipeline session"
      - path: "_concept/eval-concept.json"
        description: "Written by eval-concept sub-agent after Blueprint phase — verdict must be pass before implementation"
  user_inputs:
    dialog:
      - id: "complexity_tier"
        label: "Project complexity (controls checkpoint frequency)"
        type: "select"
        options:
          - "small"
          - "standard"
          - "complex"
        required: false
        default: "standard"
        hint: "small = consolidated checkpoints, standard = phase-level checkpoints, complex = per-skill checkpoints"
    files: []
---

# Orchestrator — Full Concept Pipeline

## Overview

Drives a new project through the three conceptualization phases — Discovery,
Experience, Blueprint — producing a versioned `_concept/` artifact folder.
Creates a structured concept plan (PLANS.md), then executes it phase by phase
with checkpoints at each milestone.

**Complexity tiers** control checkpoint granularity:
- **small** — one checkpoint per phase (3 total)
- **standard** — checkpoint after each major sub-skill
- **complex** — checkpoint after every sub-skill including optional ones

## When to Use

- Starting a new project from scratch
- Resuming an interrupted concept session
- Running the full concept pipeline end-to-end

## When NOT to Use

- Reverse-engineering an existing codebase — use `reverse-engineer`
- Adding a feature to a completed concept — use `add-feature`
- Only need one specific skill (e.g., just the data model) — run it standalone

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md`,
`skaileup-shared/contracts/plans.md`, `skaileup-shared/contracts/iron_laws.md`, and
`skaileup-shared/contracts/agent_patterns.md` before starting.

**Hard gates:** None (this is the entry point).

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `skaileup-shared/contracts/concept_structure.md` | Yes |
| Must read | `skaileup-shared/contracts/plans.md` | Yes |
| Must read | `skaileup-shared/contracts/iron_laws.md` | Yes |
| Resume state | `_concept/PLANS.md` | If resuming |

---

ROLE  Concept Orchestrator — guides a project through Discovery, Experience, and Blueprint phases.

READS
  ? _concept/PLANS.md                                    — resume state (if exists)
  ? _concept/1_discovery/1_overview/brief.md              — app name, complexity tier (after Phase 1)
  skaileup-shared/contracts/concept_structure.md               — canonical _concept/ paths
  skaileup-shared/contracts/plans.md                           — PLANS.md format
  skaileup-shared/contracts/iron_laws.md                       — non-negotiable constraints

WRITES
  _concept/PLANS.md                  — durable concept plan (phases + skill checkboxes)
  _concept/decisions.md              — dated concept decisions
  _concept/eval-concept.json         — verdict written by eval-concept sub-agent
  LEARNINGS.md                       — learnings journal (append)

REFERENCES
  skaileup-shared/contracts/concept_structure.md    — canonical _concept/ paths
  skaileup-shared/contracts/plans.md               — PLANS.md format
  skaileup-shared/contracts/iron_laws.md           — non-negotiable constraints
  skaileup-shared/contracts/agent_patterns.md      — communication style, standalone mode

MUST  create or resume PLANS.md before any work
MUST  follow phase order — no skipping phases
MUST  update PLANS.md at every checkpoint
MUST  emit observability events at every transition
MUST  log significant decisions in _concept/decisions.md
MUST  get user approval at phase boundaries (or auto-review if enabled)
MUST  dispatch eval-concept as a SEPARATE sub-agent after Blueprint — never same context as this pipeline
MUST  block proceeding to skaileup-implementation if eval-concept verdict != "pass"
NEVER skip phase approval checkpoints
NEVER run Experience skills before Discovery is approved
NEVER run Blueprint skills before Experience is approved
NEVER overwrite user-edited _concept/ files without approval

EMIT [orchestrator] started run_id=<uuid> project=<name>

# -- Phase 0: Initialize or Resume ------------------------------------------

STEP 1: Check for existing plan
  IF _concept/PLANS.md exists
    - Read PLANS.md
    - Identify last incomplete phase
    - Report status to user
    - Resume from that phase
  ELSE
    - Continue to STEP 2

  IF LEARNINGS.md does not exist
    - Create LEARNINGS.md with category headings:
      - **Skills & Subskills** — workflow gaps, confusing steps
      - **Concept Quality** — spec gaps, ambiguities found
      - **User Communication** — what worked well in checkpoints
      - **Other** — anything else noteworthy
    - Format: `- [YYYY-MM-DD] [<skill-name>] <learning>`

STEP 2: Self-collect inputs
  - Ask user for: project name, one-line description, target audience
  - Ask for complexity_tier (or use default: standard)
  - Write initial PLANS.md with phase structure

EMIT [orchestrator] plan_created phases=3

# -- Phase 1: Discovery -----------------------------------------------------

STEP 3: Project Overview
  - RUN overview sub-skill → _concept/1_discovery/1_overview/
  - Produces: brief.md, goals.md, comparable.md
  - Read brief.md to extract complexity_tier for checkpoint control
  - DO update_progress

IF complexity_tier is complex
  CHECKPOINT overview
    > "Project brief is ready. Here's what I understood:
    > [summary from brief.md]
    > Approve to continue."

STEP 4: Research (optional, parallel-capable)
  - Ask user if research is needed
  IF yes
    - RUN research sub-skill → _concept/_grounding/
    - DO update_progress
  ELSE
    - Skip research, log decision

STEP 5: Brand Identity
  - RUN brand-visual sub-skill → _concept/1_discovery/2_brand/
  - Produces: identity.md, tokens.json
  - DO update_progress

IF complexity_tier is complex
  CHECKPOINT brand
    > "Brand identity defined: [summary].
    > Approve to continue."

CHECKPOINT discovery_complete
  > "Discovery phase complete:
  > - Project brief: [name] — [one-liner]
  > - Research: [done/skipped]
  > - Brand: [color palette, typography summary]
  >
  > Approve to move to Experience design."
  DO log_learnings

EMIT [orchestrator] phase_complete phase=discovery

# -- Phase 2: Experience ----------------------------------------------------

STEP 6: User Journeys (optional)
  IF stories/journeys are part of the flow
    - RUN journeys sub-skill → _concept/2_experience/1_journeys/
    - Produces: stories.json, journey maps
    - DO update_progress

STEP 7: Features
  - RUN features sub-skill → _concept/2_experience/2_features/
  - Produces: feature group folders with feature specs
  - DO update_progress

IF complexity_tier is complex
  CHECKPOINT features
    > "Features specified: [count] features across [count] groups.
    > Key features: [list top 5]
    > Approve to continue."

STEP 8: Screens
  - RUN screens sub-skill → _concept/2_experience/3_screens/
  - Produces: screen specs with cross-references to features
  - DO update_progress

STEP 9: Storybook / Mockups (optional)
  IF storybook/mockup is part of the flow
    - RUN storybook or mock sub-skill → _concept/2_experience/4_storybook/
    - DO update_progress

CHECKPOINT experience_complete
  > "Experience phase complete:
  > - Features: [count] across [count] groups
  > - Screens: [count] screen specs
  > - Journeys: [done/skipped]
  > - Mockups: [done/skipped]
  >
  > Approve to move to Blueprint."
  DO log_learnings

EMIT [orchestrator] phase_complete phase=experience

# -- Phase 3: Blueprint ------------------------------------------------------

STEP 10: Tech Stack
  - RUN techstack sub-skill → _concept/3_blueprint/1_techstack/
  - Produces: stack.md
  - DO update_progress

IF complexity_tier is complex
  CHECKPOINT techstack
    > "Tech stack selected: [summary].
    > Approve to continue."

STEP 11: Architecture (optional)
  IF project complexity warrants architecture design
    - RUN architecture sub-skill → _concept/3_blueprint/2_architecture/
    - Produces: architecture.md
    - DO update_progress

STEP 12: Data Model
  - RUN datamodel sub-skill → _concept/3_blueprint/3_datamodel/
  - Produces: model.dbml, model.json, model.schema.json
  - DO update_progress

# ── Phase 4: Concept Evaluation Gate ──────────────────────────────

STEP: Dispatch eval-concept as a fresh sub-agent (separate context — not this agent)
  - Sub-agent reads: _concept/ artifacts
  - Sub-agent writes: _concept/eval-concept.json
  - WAIT for eval-concept to complete

  READ _concept/eval-concept.json after sub-agent finishes

  IF verdict = "pass"
    - Update PLANS.md: mark eval-concept DONE
    - Report to user:
      ✓ Concept evaluation passed (overall: <score>/100)
        Ready for skaileup-implementation.

  IF verdict = "needs_resolution"
    - Display all blocking_flags with their resolutions to user
    - PAUSE: ask user to resolve each blocking flag
    - After user confirms fixes: re-dispatch eval-concept
    - Repeat until verdict = "pass" (or user explicitly overrides with reason)

  IF verdict = "fail"
    - Display all flags
    - STOP — concept requires rework
    - Tell user which phase to re-run (add-feature or specific sub-skills)

CHECKPOINT eval_concept
  Gate: CANNOT proceed to skaileup-implementation until eval-concept verdict = "pass"
  User sees: eval-concept scores + verdict + any remaining flags

CHECKPOINT blueprint_complete
  > "Blueprint phase complete:
  > - Tech stack: [framework, database, UI library]
  > - Architecture: [done/skipped]
  > - Data model: [count] entities, [count] relations
  >
  > Your concept is ready for implementation.
  > Next steps: run the implementation pipeline or review individual artifacts."
  DO log_learnings

EMIT [orchestrator] completed run_id=<uuid> features=<count> entities=<count>

# -- Procedures --------------------------------------------------------------

PROCEDURE log_learnings
  - After each checkpoint, reflect on what happened
  - Append to LEARNINGS.md under the most relevant category
  - Only log genuine observations, not status updates

PROCEDURE update_progress
  - Check off completed skill in PLANS.md
  - Commit if in a git repo: "chore: update concept progress"

PROCEDURE auto_review
  - Activated when user says "auto-review", "autonomous", or "run without stopping"
  - Run lint_concept.py on _concept/
  - Run review skill in gardening mode
  - Read quality score from _concept/quality.json
  - Score >= 70 and 0 blocking issues → auto-approve and continue
  - Else → pause and escalate to user

CHECKLIST
  - [ ] PLANS.md created or resumed before any work
  - [ ] Phase order followed (Discovery → Experience → Blueprint)
  - [ ] PLANS.md updated at every checkpoint
  - [ ] User approved each phase boundary
  - [ ] _concept/ files not overwritten without approval
  - [ ] LEARNINGS.md updated at checkpoints

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Skipping the plan phase | Always create PLANS.md first |
| Running datamodel before features | Features define what entities are needed — always features first |
| Not checking for existing _concept/ | Read PLANS.md if it exists — resume, don't restart |
| Presenting raw file contents as checkpoint | Summarize in plain language, not markdown dumps |
| Running all skills without checkpoints | User approval at phase boundaries is mandatory |

## Integration

- **Reads:** `skaileup-shared/contracts/` for rules and structure
- **Writes:** `_concept/` pipeline artifacts, `LEARNINGS.md`
- **Dispatches to:** `overview`, `research`, `brand-visual`, `journeys`, `features`, `screens`, `storybook`, `techstack`, `architecture`, `datamodel`
- **Called by:** user directly, CLI flows, or automated pipeline
- **Hands off to:** Implementation orchestrator (skaileup-implementation)
