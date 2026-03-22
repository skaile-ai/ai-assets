---
name: "brainstorm"
description: "Structured problem decomposition before implementation planning. Explores unknowns, surfaces risks, identifies dependencies, and formulates open questions that would block implementation if unanswered. Run before write-plan. Triggers on: 'let's think before we start', 'what are the risks', 'brainstorm the implementation', 'what could go wrong', starting a complex feature with unclear scope."
metadata:
  version: "1.0.0"
  tags:
    - "brainstorm"
    - "planning"
    - "risks"
    - "decomposition"
    - "pre-implementation"
    - "unknowns"
  stage: beta
  source: "MERGED"
  prerequisites:
    files:
      - path: "_concept/1_discovery/1_overview/brief.md"
        gate: hard
        description: "Brief required for scope definition"
      - path: "_concept/2_experience/2_features"
        gate: hard
        description: "Features required to identify implementation risks"
        min_entries: 1
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        gate: hard
        description: "Tech stack required to surface stack-specific risks"
  user_inputs:
    dialog:
      - id: "focus_area"
        label: "Focus the brainstorm on a specific area? (leave blank for full-scope)"
        type: "text"
        required: false
    files: []
  reads_from:
    - "_concept/1_discovery/1_overview/brief.md"
    - "_concept/2_experience/2_features/**/*.md"
    - "_concept/3_blueprint/1_techstack/stack.md"
    - "_concept/3_blueprint/3_datamodel/model.json"
    - "_concept/2_experience/1_journeys/stories.json"
  writes_to:
    - "_implementation/brainstorm.md"
---

# Brainstorm

## Overview

Before any plan is written or code is touched, this skill does the thinking that prevents
expensive rework. It surfaces unknowns, risks, dependencies, and questions that would
block implementation if encountered mid-build.

The output is `_implementation/brainstorm.md` — a structured document that `write-plan`
reads to produce a robust, risk-aware implementation plan.

**Why brainstorm before planning?** A plan written without this step assumes everything
is known. It isn't. The brainstorm exposes the gaps before they become blockers.

## When to Use

- Before `write-plan` on any complex feature set or full app build
- When scope feels unclear or risky
- When the tech stack or integrations are unfamiliar

## When NOT to Use

- The feature is a small, well-understood incremental change — skip brainstorm, go to write-plan
- You have already brainstormed in a prior session (check for existing brainstorm.md)

---

ROLE  Pre-implementation problem decomposition — surfaces risks, unknowns, and open questions before planning begins.

READS
  _concept/1_discovery/1_overview/brief.md
  _concept/2_experience/2_features/**/*.md
  _concept/3_blueprint/1_techstack/stack.md
  _concept/3_blueprint/3_datamodel/model.json
  ? _concept/2_experience/1_journeys/stories.json
  ? _concept/3_blueprint/2_architecture/architecture.md
  ? _implementation/brainstorm.md    — check for existing brainstorm (resume/update mode)

WRITES
  _implementation/brainstorm.md

MUST  read all feature specs before brainstorming — never improvise the feature list
MUST  separate known facts from assumptions — label each clearly
MUST  produce at least one open question per major risk area
MUST  ask open questions as standalone messages BEFORE writing brainstorm.md
NEVER write brainstorm.md before unresolved blockers are surfaced to the user

# ── Workflow ───────────────────────────────────────────────────────

STEP 1: Read all concept artifacts
  - Read brief.md, all features, stack.md, model.json
  - Read stories.json if exists (journey order context)
  - Note: what is the app trying to do, who uses it, what are the must-have flows?

STEP 2: Identify risk areas
  For each of the following dimensions, note what is clear vs. unclear:
  - **Data model risks** — relationships that are complex, soft-deletes, versioning, multi-tenancy
  - **Auth / permission risks** — role matrix completeness, edge cases, session management
  - **Integration risks** — external APIs, third-party services, queues, webhooks
  - **Tech stack risks** — unfamiliar libraries, version constraints, known gotchas
  - **Performance risks** — large datasets, real-time requirements, expensive queries
  - **UX/flow risks** — multi-step flows, optimistic UI, offline requirements
  - **Test strategy risks** — hard-to-test areas (real-time, file uploads, payments)

STEP 3: Surface open questions
  - List every question that, if unanswered, would require rework after implementation starts
  - Prioritize: P1 = blocks planning, P2 = blocks a specific feature, P3 = nice to know
  IF there are P1 questions (plan blockers)
    - Send them to the user as standalone messages BEFORE continuing
    - Pause until answered
  ELSE
    - Document P2/P3 questions in brainstorm.md; they do not block planning

STEP 4: Write _implementation/brainstorm.md

  ---
  # Implementation Brainstorm
  date: <ISO date>
  scope: <full | focus_area>
  ---

  ## App Summary
  <2-3 sentence description of what we're building and for whom>

  ## What We Know
  <Confirmed facts from concept: key features, data model shape, tech stack, journey order>

  ## Risks and Unknowns

  ### Data Model
  <risks, unclear relationships, decisions needed>

  ### Auth and Permissions
  <role matrix gaps, edge cases>

  ### Integrations
  <external APIs, async flows, unknowns>

  ### Tech Stack
  <unfamiliar libraries, version constraints, known issues>

  ### Testing
  <hard-to-test areas, seed data gaps>

  ## Open Questions
  | Priority | Question | Blocks |
  |---|---|---|
  | P1 | ... | planning |
  | P2 | ... | <feature> |
  | P3 | ... | nothing critical |

  ## Answered Questions
  | Question | Answer | Source |
  |---|---|---|
  | ... | ... | user / spec / research |

  ## Recommended Mitigations
  <For each major risk: what to do about it in the plan>

STEP 5: Commit
  $ git add _implementation/brainstorm.md
  $ git commit -m "docs: record implementation brainstorm"

CHECKLIST
  - [ ] All concept artifacts read
  - [ ] Risk areas assessed across all six dimensions
  - [ ] P1 open questions surfaced to user and answered before proceeding
  - [ ] brainstorm.md written with structured sections
  - [ ] Committed to implementation branch

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Brainstorming from memory without reading the specs | Always read all feature specs first |
| Writing brainstorm.md before surfacing P1 questions | Surface P1 questions first; document answers before writing |
| Listing risks without recommending mitigations | Every risk section needs a mitigation suggestion |
| Marking everything as P3 to avoid blocking | Honest priority assignment — P1 blockers prevent planning errors |
