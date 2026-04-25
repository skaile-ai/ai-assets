---
name: "skailup-onboard"
description: "Collects project identity, complexity tier preferences, and technology decisions through a structured dialog. Produces profile.yaml and decisions.yaml in _grounding/onboarding/. Use at the start of any concept pipeline."
metadata:
  version: "1.0.0"
  tags:
    - "onboarding"
    - "project-setup"
    - "tiers"
    - "decisions"
  source: "NEW"
  prerequisites:
    reads: []
    produces:
      - path: "_concept/_grounding/onboarding/profile.yaml"
        description: "Machine-readable project identity (name, problem, audience, type, tiers)"
      - path: "_concept/_grounding/onboarding/decisions.yaml"
        description: "Collected decisions with confidence levels (locked/preferred/open)"
  user_inputs:
    dialog:
      - id: "project_name"
        label: "Project name"
        type: "text"
        required: true
      - id: "problem_statement"
        label: "What problem does this solve?"
        type: "textarea"
        required: true
      - id: "target_audience"
        label: "Who is the target audience?"
        type: "textarea"
        required: true
      - id: "project_type"
        label: "Project type"
        type: "select"
        required: true
        options:
          - "web-app"
          - "cli-tool"
          - "api-service"
          - "library"
          - "data-pipeline"
          - "mobile-app"
      - id: "tier_preset"
        label: "Complexity preset"
        type: "select"
        required: false
        options:
          - "quick"
          - "standard"
          - "thorough"
        default: "standard"
  artifacts:
    produces:
      - id: onboarding-profile
        description: "Machine-readable project identity"
      - id: onboarding-decisions
        description: "Collected decisions with confidence levels"
    parameters:
      depth:
        type: enum
        values: [none, light, medium, max]
        default: medium
---

# Onboard — Project Identity and Tier Setup

## Overview

The **skailup-onboard** skill is the project initialization agent. It runs a
structured dialog to collect project identity, complexity tier preferences, and
early technology decisions. Outputs are written to `_concept/_grounding/onboarding/`
as machine-readable YAML files consumed by every downstream skill.

This skill does NOT produce `_concept/` pipeline artifacts. It produces grounding
data only — the foundation that all subsequent skills build on.

## When to Use

- Starting a new concept pipeline from scratch
- The orchestrator dispatches this as the first step before any other skill
- The user says "let's start", "new project", "set up onboarding", or "initialize"
- `_concept/_grounding/onboarding/profile.yaml` does not yet exist

## When NOT to Use

- `profile.yaml` already exists and is approved — resume the pipeline instead
- A project brief already exists in `_concept/1_discovery/1_overview/` — use the orchestrator to pick up from there
- Adding a feature to a running concept — use `add-feature`

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md` and
`skaileup-shared/contracts/iron_laws.md` before starting.

**Hard gates:** None — this is the entry point.

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `skaileup-shared/contracts/concept_structure.md` | Yes |
| Must read | `skaileup-shared/contracts/iron_laws.md` | Yes |
| Resume state | `_concept/_grounding/onboarding/profile.yaml` | If resuming |
| Never load | `_concept/1_discovery/`, `_concept/2_experience/`, `_concept/3_blueprint/` | — |

## Standalone Mode

**Gate check:** None — first step.
**On completion:** Present profile summary and suggest next steps (ingest-seeds if the user has material, or the orchestrator to start the pipeline).

---

ROLE  Project Initialization agent — runs structured onboarding dialog and writes
      `_concept/_grounding/onboarding/profile.yaml` and `decisions.yaml`.

READS
  ? _concept/_grounding/onboarding/profile.yaml   — existing profile (resume support)
  ? _concept/_grounding/onboarding/decisions.yaml — existing decisions (resume support)

WRITES
  _concept/_grounding/onboarding/profile.yaml    — project identity + tier configuration
  _concept/_grounding/onboarding/decisions.yaml  — technology decisions with confidence levels

REFERENCES
  skaileup-shared/contracts/concept_structure.md — canonical _concept/ paths
  skaileup-shared/contracts/iron_laws.md         — non-negotiable constraints
  skaileup-shared/contracts/agent_patterns.md    — communication style, standalone mode

MUST  check for existing profile.yaml before asking any questions (resume support)
MUST  write profile.yaml and decisions.yaml atomically after Phase 4 completes
MUST  ask all Phase 1 questions before moving to Phase 2
MUST  offer tier_preset shortcut before asking per-domain overrides
NEVER write features, screens, data models, or tech specs — this skill collects context only
NEVER block on optional fields — proceed with defaults if the user skips
NEVER overwrite an existing profile.yaml without user confirmation

EMIT [onboard] started run_id=<uuid>

# -- Phase 0: Resume Check ---------------------------------------------------

STEP 0: Check for existing profile
  IF _concept/_grounding/onboarding/profile.yaml exists
    - Read profile.yaml and decisions.yaml
    - Display a summary:
      > "I found an existing onboarding profile for [project_name].
      > Here's what was collected:
      > [summary of name, problem, audience, type, tier_preset]
      >
      > Would you like to continue from where we left off, update specific answers,
      > or start fresh?"
    - IF user says continue → skip to Phase 4 (write files) with existing data
    - IF user says update → jump to the relevant phase and re-ask those questions
    - IF user says start fresh → proceed to Phase 1 (overwrite confirmed)
  ELSE
    - Continue to Phase 1

# -- Phase 1: Project Identity -----------------------------------------------

STEP 1: Collect project identity
  Ask the following questions one at a time. Wait for each answer before asking the next.
  Infer answers from context if the user has already provided them in the conversation.

  1. What is the project name? (working title is fine)
  2. What problem does this project solve?
     (Ask for a concrete description: who has the problem, what is the pain point?)
  3. Who is the target audience?
     (Role, technical skill level, context of use)
  4. What type of project is this?
     Options: web-app | cli-tool | api-service | library | data-pipeline | mobile-app
     (Offer examples if the user is unsure)

  After all four answers are collected:
  > "Got it. You're building a [project_type] called [project_name] for [target_audience],
  > solving [brief problem summary]. Does that sound right?"
  UNTIL user confirms

EMIT [onboard] checkpoint phase=identity_collected project=<project_name>

# -- Phase 2: Tier Configuration ---------------------------------------------

STEP 2: Select complexity preset
  Present the three presets with brief descriptions:
  > "How thorough should the concept pipeline be?
  >
  > **quick** — focused output, fewer steps, good for prototypes and small tools
  > **standard** — balanced depth, recommended for most projects (default)
  > **thorough** — maximum depth, all optional phases included, best for complex systems
  >
  > Which fits your project? (Press Enter for 'standard')"

  - Record the chosen tier_preset (default: standard)

STEP 3: Per-domain tier overrides (optional)
  IF user chose standard or thorough:
    > "The preset applies default depths to each pipeline domain. Would you like to
    > adjust any domain individually? (Skip to use all defaults)"

    IF user wants overrides:
      Present the domain table with preset defaults pre-filled:
      | Domain                | Default (from preset) | Override? |
      |---|---|---|
      | skaileup-research     | <preset value>        |           |
      | skaileup-discovery    | <preset value>        |           |
      | skaileup-experience   | <preset value>        |           |
      | skaileup-prototype    | <preset value>        |           |
      | skaileup-storybook    | <preset value>        |           |
      | skaileup-blueprint    | <preset value>        |           |
      | skaileup-concept-meta | <preset value>        |           |

      Valid values per domain: none | light | medium | max
      Collect overrides one at a time; skip domains the user does not want to change.
    ELSE
      Use preset defaults for all domains

EMIT [onboard] checkpoint phase=tiers_configured preset=<tier_preset>

# -- Phase 3: Technology and Architecture Decisions --------------------------

STEP 4: Technology decisions (optional)
  > "Do you have early technology preferences? These help downstream skills make better
  > recommendations. Skip any you haven't decided yet."

  Ask in this order, one at a time:

  4a. Framework / language
      Prompt: "Do you have a preferred framework or language?"
      Examples: Vue, React, Nuxt, NestJS, Python, Go, or 'no preference'
      Confidence options: locked | preferred | open

  4b. ORM / data access layer
      Prompt: "ORM or data access preference?"
      Examples: Prisma, TypeORM, Drizzle, SQLAlchemy, or 'no preference'
      Confidence options: locked | preferred | open

  4c. Database
      Prompt: "Database preference?"
      Examples: PostgreSQL, MySQL, SQLite, MongoDB, or 'no preference'
      Confidence options: locked | preferred | open

  4d. UI component library
      Prompt: "UI component library preference?"
      Examples: Shadcn/ui, PrimeVue, Radix, Tailwind only, or 'no preference'
      Confidence options: locked | preferred | open

  4e. Authentication
      Prompt: "Authentication approach?"
      Examples: Keycloak, Auth.js, Clerk, custom JWT, or 'no preference'
      Confidence options: locked | preferred | open

  For each answered item, record:
    - decision: <the value given>
    - confidence: <locked|preferred|open>
    - rationale: <reason given, or null if none>

STEP 5: Brand and architecture decisions (optional)
  > "Two more quick questions:"

  5a. Existing brand?
      Prompt: "Does this project have an existing brand (logo, colors, typography)?"
      If yes: "Do you have brand files to share? (images, design tokens, style guide?)"
      Record: has_brand: true|false, brand_source: <description or null>

  5b. Architecture pattern
      Prompt: "Any preference for the overall architecture pattern?"
      Examples: monolith, microservices, event-driven, serverless, or 'no preference'
      Record: architecture_pattern: <value or null>, confidence: <locked|preferred|open>

EMIT [onboard] checkpoint phase=decisions_collected

# -- Phase 4: Write Outputs --------------------------------------------------

STEP 6: Write profile.yaml
  - $ mkdir -p _concept/_grounding/onboarding

  OUTPUT _concept/_grounding/onboarding/profile.yaml
    ```yaml
    project_name: "<collected value>"
    problem_statement: "<collected value>"
    target_audience: "<collected value>"
    project_type: "<web-app|cli-tool|api-service|library|data-pipeline|mobile-app>"
    tier_preset: "<quick|standard|thorough>"
    tier_overrides:
      skaileup-research: "<none|light|medium|max>"
      skaileup-discovery: "<none|light|medium|max>"
      skaileup-experience: "<none|light|medium|max>"
      skaileup-prototype: "<none|light|medium|max>"
      skaileup-storybook: "<none|light|medium|max>"
      skaileup-blueprint: "<none|light|medium|max>"
      skaileup-concept-meta: "<none|light|medium|max>"
    has_brand: <true|false>
    brand_source: "<description or null>"
    created_at: "<YYYY-MM-DD>"
    last_updated: "<YYYY-MM-DD>"
    ```

  If a domain had no override, use the preset default value from the Tier Presets table.

STEP 7: Write decisions.yaml
  OUTPUT _concept/_grounding/onboarding/decisions.yaml
    ```yaml
    decisions:
      - key: framework
        decision: "<value or null>"
        confidence: "<locked|preferred|open>"
        rationale: "<reason or null>"
      - key: orm
        decision: "<value or null>"
        confidence: "<locked|preferred|open>"
        rationale: "<reason or null>"
      - key: database
        decision: "<value or null>"
        confidence: "<locked|preferred|open>"
        rationale: "<reason or null>"
      - key: ui_library
        decision: "<value or null>"
        confidence: "<locked|preferred|open>"
        rationale: "<reason or null>"
      - key: auth
        decision: "<value or null>"
        confidence: "<locked|preferred|open>"
        rationale: "<reason or null>"
      - key: architecture_pattern
        decision: "<value or null>"
        confidence: "<locked|preferred|open>"
        rationale: "<reason or null>"
    created_at: "<YYYY-MM-DD>"
    last_updated: "<YYYY-MM-DD>"
    ```

  Omit keys where the user provided no answer — do not include null-decision entries.

STEP 8: Confirm and hand off
  Present a summary to the user:
  > "Onboarding complete for **[project_name]**.
  >
  > Project: [project_type] — [one-line problem summary]
  > Audience: [target_audience]
  > Pipeline depth: [tier_preset][, with overrides for: <list if any>]
  > Decisions locked: [count or 'none yet']
  >
  > Next steps:
  > - If you have existing material (docs, designs, code): run `skailup-ingest-seeds`
  > - Otherwise: run the orchestrator to start the concept pipeline"

EMIT [onboard] completed run_id=<uuid> project=<project_name> preset=<tier_preset>

CHECKLIST
  - [ ] Existing profile.yaml checked before asking any questions
  - [ ] All four Phase 1 identity fields collected and confirmed
  - [ ] tier_preset selected (default: standard)
  - [ ] Per-domain tier_overrides applied where provided
  - [ ] Phase 3 decisions collected with confidence levels
  - [ ] _concept/_grounding/onboarding/profile.yaml written
  - [ ] _concept/_grounding/onboarding/decisions.yaml written (omit skipped keys)
  - [ ] Summary presented with next-step suggestions

---

## Tier Presets

Preset defaults applied per domain when no override is given:

| Domain                | quick  | standard | thorough |
|---|---|---|---|
| skaileup-research     | light  | medium   | max      |
| skaileup-discovery    | light  | medium   | max      |
| skaileup-experience   | light  | medium   | max      |
| skaileup-prototype    | none   | light    | medium   |
| skaileup-storybook    | none   | none     | max      |
| skaileup-blueprint    | light  | medium   | max      |
| skaileup-concept-meta | none   | light    | medium   |

## Depth Behavior

This skill's own behavior adapts based on the `depth` parameter:

| Depth  | Behavior |
|---|---|
| `none`  | Skip this skill entirely — onboarding is assumed complete |
| `light` | Collect Phase 1 (identity) only; use tier_preset defaults; skip Phase 3 decisions |
| `medium`| Run all four phases (default) |
| `max`   | Run all four phases; ask follow-up clarifying questions for each decision; validate answers against known constraints before writing |

## Resume Support

If `_concept/_grounding/onboarding/profile.yaml` already exists when this skill starts:

1. Read the existing file and display a summary.
2. Ask the user: continue, update specific fields, or start fresh.
3. If continuing: skip to STEP 6 and re-write both files with existing data plus any corrections.
4. If updating: identify which phase(s) need re-asking, run only those, then re-write files.
5. If starting fresh: confirm intent, then proceed to Phase 1 (overwrite on STEP 6).

## Decision Confidence Levels

| Level      | Meaning |
|---|---|
| `locked`   | Decided and not open to change — downstream skills must use this value |
| `preferred`| Strong preference but open to recommendation if there's a better fit |
| `open`     | No preference — downstream skills should recommend based on project signals |

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Asking all questions at once | One question at a time — wait for each answer |
| Writing null entries to decisions.yaml | Omit keys where the user gave no answer |
| Using preset defaults without checking for overrides | Apply overrides from STEP 3 before writing tier_overrides in profile.yaml |
| Skipping the resume check | Always read profile.yaml first if it exists |
| Writing feature or architecture detail | This skill collects identity and preferences only — nothing more |

## Integration

- **Called by:** `skailup-orchestrator` as the first step, or standalone by the user
- **Feeds into:** all downstream skills via `_concept/_grounding/onboarding/profile.yaml` and `decisions.yaml`
- **Hands off to:** `skailup-ingest-seeds` (if seeds exist) or `skailup-orchestrator` (to start pipeline)
