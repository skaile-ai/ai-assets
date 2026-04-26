---
name: "skailup-architecture"
description: "Use after features and techstack are approved to document system architecture. Produces architecture.md with system overview, backend structure, data flow, communication protocols, external integrations, and infrastructure."
metadata:
  version: "1.0.0"
  tags:
    - "architecture"
    - "modules"
    - "dataflow"
    - "protocols"
    - "backend"
    - "api"
    - "services"
    - "websocket"
    - "agents"
    - "infrastructure"
  source: "MERGED"
  parameters:
    depth:
      type: enum
      values: [none, light, medium, max]
      default: medium
  artifacts:
    requires:
      - id: brief
        gate: hard
      - id: features
        gate: hard
      - id: techstack
        gate: hard
    produces:
      - id: architecture
        description: "System architecture document"
    consumes:
      - id: behaviors
        gate: soft
      - id: onboarding-decisions
        gate: soft
    seed_modes:
      architecture:
        - adopt
        - extend
        - inspire
      default: extend
  prerequisites:
    files:
      - path: "_concept/discovery/brief.md"
        gate: hard
        description: "Project brief required for system context and scope"
      - path: "_concept/experience/features"
        gate: hard
        description: "Features drive architectural decisions (services, protocols, integrations)"
        min_entries: 1
      - path: "_concept/blueprint/techstack.md"
        gate: hard
        description: "Tech stack determines default architecture; extends only where features demand"
    reads:
      - path: "_concept/_grounding/overview/user_input.json"
        description: "Complexity tier for architectural depth"
      - path: "_concept/experience/behaviors"
        description: "Behavioral specs for state machine and lifecycle architecture"
    produces:
      - path: "_concept/blueprint/architecture.md"
        description: "System design: modules, data flow, protocols, infrastructure, integrations"
---

# Architecture — System Architecture

## Overview

The **architecture** skill is the Architecture agent. It analyzes features and
the chosen tech stack to produce `_concept/blueprint/architecture.md`
— a complete system design specification that downstream skills (datamodel, screens,
implementation) can rely on.

The document starts from what the chosen stack provides by default, then extends
only where features demand it.

## When to Use

- Features and tech stack are approved
- The orchestrator dispatches this step
- The user asks about system design, modules, protocols, or infrastructure
- Features imply non-standard requirements (real-time, background processing, custom services)

## When NOT to Use

- Features or tech stack do not exist yet — run **features** and **techstack** first
- The user only wants to add one feature — use **add-feature** instead
- Architecture already exists and user wants to edit it directly

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md`,
`skaileup-shared/contracts/frontmatter.md`, and `skaileup-shared/contracts/semantic_types.md`.

**Hard gates:**
- `_concept/discovery/brief.md` must exist
- `_concept/experience/features/` must contain at least one feature file
- `_concept/blueprint/techstack.md` must exist

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/discovery/brief.md` | Yes |
| Must read | `_concept/experience/features/**/*.md` | Yes |
| Must read | `_concept/blueprint/techstack.md` | Yes |
| Check if present | `_concept/_grounding/overview/user_input.json` | No (complexity) |
| Check if present | `_concept/_grounding/general/onboarding.md` | No |
| Check if present | `_concept/experience/behaviors/*.allium` | No |
| Never load | `_concept/blueprint/datamodel/`, `_concept/experience/screens/` | — |

## Standalone Mode

**Gate check:** brief.md + at least one feature + stack.md must exist.
**On completion:** Present architecture summary and suggest next steps (datamodel, screens).

---

ROLE  Architecture agent — reads features and tech stack, produces
      `_concept/blueprint/architecture.md`.

READS
  _concept/discovery/brief.md             — app name, audience, problem domain
  _concept/experience/features/**/*.md             — feature requirements (drives module needs)
  _concept/blueprint/techstack.md           — confirmed tech stack and constraints
  ? _concept/_grounding/overview/user_input.json      — complexity field (drives involvement level)
  ? _concept/_grounding/general/onboarding.md         — pre-answered architecture notes
  ? _concept/experience/behaviors/*.allium        — entity state machines, complex workflows

WRITES
  _concept/blueprint/architecture.md — complete system architecture specification

REFERENCES
  skaileup-shared/contracts/concept_structure.md      — valid _concept/ paths and naming rules
  skaileup-shared/contracts/frontmatter.md            — required YAML fields
  skaileup-shared/contracts/semantic_types.md         — field types (for data flow context)
  references/output_template.md                  — six-section architecture.md template

MUST  include all six sections in architecture.md (overview, backend, data flow, protocols, integrations, infrastructure)
MUST  start from the stack's defaults and extend only where features demand it
MUST  include all required frontmatter fields
MUST  document all non-standard protocols with endpoints, message types, lifecycle, and error handling
NEVER  skip external integration error handling or credential management documentation
NEVER  invent stack defaults — read them from stack.md, not from assumptions

EMIT  [architecture] started run_id=<uuid>

STEP 1: Read context
  - Read brief.md for app name, audience, and problem domain
  - Read all feature files for functional requirements
  - Read stack.md for tech stack decisions and constraints
  - Check _grounding/overview/user_input.json for complexity field
  IF _grounding/general/onboarding.md exists
    - Read onboarding notes — user may have specified architecture constraints
    - Use these as pre-answered context; skip redundant questions
  IF _concept/experience/behaviors/*.allium exists
    - Read all .allium files; look for:
      - Entity state machines → may require event-driven patterns
      - Complex workflows → may need background processing
      - Multi-actor interactions → may need real-time protocols
      - External system interactions → may need adapter modules
  - Analyze all feature requirements for architecture signals:
    - Real-time features (chat, live updates, notifications, collaborative editing)
    - Background processing (scheduling, long-running tasks, file processing)
    - External system integrations (payments, email, APIs, cloud services)
    - Non-standard data flows (event sourcing, file pipelines, saga orchestration)

STEP 1b: Determine involvement level
  - Read complexity from _grounding/overview/user_input.json (default: standard)
  IF complexity is "small"
    - > "I'll design the architecture automatically based on your features. I'll show you a summary when done. Want to review the details instead?"
    - Proceed in automatic mode: make best-judgment decisions, skip to STEP 3
  IF complexity is "complex"
    - > "The architecture is a key decision point. I recommend we go through the design together — or I can propose something and you review?"
    - Default to involved mode → continue to STEP 2
  ELSE (standard)
    - > "Would you like to be involved in the architecture design, or should I handle it based on your features?"
    - Wait for user preference

STEP 2: Analyze architecture needs (involved mode)
  - For each feature, assess whether stack defaults are sufficient or extensions are needed
  - Ask the user:
    1. Does this app need anything in the background — like processing, scheduling, or long-running tasks?
    2. Do users need to see updates in real-time — like live chat, notifications, or collaborative editing?
    3. Does the app connect to external services — like payment systems, email providers, or third-party APIs?
    4. Are there any special communication needs — like streaming data or instant updates?
    5. Does the app handle data in any non-standard way — like event logging, multi-step workflows, or file processing?
  - If user is uncertain, analyze features and suggest what is likely needed

STEP 3: Write architecture document
  - $ mkdir -p _concept/blueprint/architecture.md
  - Write architecture.md following references/output_template.md
  - Baseline every section with what stack.md's chosen stack provides out of the box
  - Add project-specific extensions identified in STEP 1–2

  OUTPUT _concept/blueprint/architecture.md
    ---
    apps: [<list of app/service names>]
    custom_modules: [<list of custom backend modules>]
    protocols: [<list of protocols>]
    external_integrations: [<list of third-party services>]
    last_updated: <YYYY-MM-DD>
    ---
    Six sections: System Overview, Backend Structure, Data Flow,
    Communication Protocols, External Integrations, Infrastructure.
    (See references/output_template.md for full section structure.)

EMIT  [architecture] checkpoint phase=architecture_documented apps=<N> custom_modules=<N> protocols=<N>

STEP 4: Human approval
  CHECKPOINT architecture_approved
    Show business summary:
    > "Your app's structure is designed to support [primary business capability].
    > [If real-time]: Features like [feature] will show updates instantly using [protocol].
    > [If custom modules]: Custom logic handles [business process description].
    > [If external integrations]: Your app connects to [service] for [purpose].
    > [If no extensions]: The standard [stack] setup covers everything your app needs.
    >
    > Technical summary (if interested):
    >   Apps: N, Custom modules: N, Protocols: N, Integrations: N
    >
    > Approve, or tell me what to change."

  UNTIL user explicitly approves

STEP 5: Hand off
  > "Architecture documented. This informs:
  > - `datamodel` — knows which entities need custom modules vs standard CRUD
  > - `screens` — knows which protocols screens use for real-time features
  > - `implement` — knows which custom modules and apps to build
  >
  > Next: run `datamodel` or continue with the concept pipeline."

EMIT  [architecture] completed run_id=<uuid> apps=<N> custom_modules=<N> external_integrations=<N>

CHECKLIST
  - [ ] _concept/blueprint/architecture.md exists with all frontmatter fields
  - [ ] All six sections present (overview, backend structure, data flow, protocols, integrations, infrastructure)
  - [ ] Every section shows the stack default baseline before project extensions
  - [ ] Custom modules have purpose and dependencies listed
  - [ ] Non-standard protocols document endpoints, message types, lifecycle, and error handling
  - [ ] External integrations document API/SDK, data exchanged, error handling, and credential management
  - [ ] User has explicitly approved the architecture

---

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip this skill entirely |
| `light` | Key decisions only — technology names, brief rationale |
| `medium` | Standard documentation — decisions with reasoning, diagrams, trade-offs (default) |
| `max` | Comprehensive documentation — full ADRs, alternative analysis, migration paths |

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Inventing stack defaults without reading stack.md | Read stack.md first; derive the default architecture from it. |
| Documenting only extensions, not defaults | Every section must show what the stack provides by default before listing additions. |
| Skipping the involvement level check | Small-complexity apps should proceed automatically; complex apps warrant user involvement. |
| Forgetting error handling on integrations | Every external integration must document retry strategy, fallbacks, and credential management. |
| Writing implementation code | Architecture documents decisions and structures, not implementation. No code snippets. |

## Integration

- **Called by:** `concept-orchestrator` or standalone
- **Requires:** brief.md + features + techstack/stack.md
- **Feeds into:**
  - `datamodel` — module boundaries inform entity grouping
  - `screens` — protocol knowledge enables real-time screen specs
  - `implement` and `scaffold` — module layout drives project structure
