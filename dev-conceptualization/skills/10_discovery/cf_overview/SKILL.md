---
name: overview
description: "Use when starting a new concept and no _concept/01_project/ exists, or when the user says 'I have an app idea', 'new project', 'start from scratch', or wants to redefine an existing brief."
keywords: [brief, idea, project, vision, start, new, pitch, audience, problem]
user_inputs:
  dialog:
  - id: raw_description
    label: "Describe your idea"
    type: textarea
    required: false
    hint: "Free-form description — app name, audience, problem, hero flow can all be inferred from this"
  - id: app_name
    label: "App Name"
    type: text
    required: false
    hint: "Working name for the app (inferred from raw_description if provided)"
  - id: elevator_pitch
    label: "What does the app do?"
    type: text
    required: false
    hint: "One sentence: who is it for and what does it do?"
  - id: target_audience
    label: "Who is the primary user?"
    type: text
    required: false
    hint: "Role, context, skill level"
  - id: problem_statement
    label: "What problem does it solve?"
    type: text
    required: false
    hint: "The single most important problem"
  - id: hero_flow
    label: "Most important user action"
    type: text
    required: false
    hint: "The one thing every user must be able to do"
  - id: comparable_products
    label: "Similar apps"
    type: text
    required: false
    hint: "Apps that do something similar (for reference)"
  - id: success_criteria
    label: "What does success look like?"
    type: text
    required: false
    hint: "Goals, constraints, deadlines"
  files: []
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

# Overview — Project Brief

## Overview

The **overview** skill is the Product Definition agent. It produces the project
brief artifacts in `_concept/01_project/`. It does NOT write features, data
models, screens, brand, or tech stack.

**Phase:** conceptualization / overall-concept
**Pipeline ID:** `overview` (see `cf__shared/pipeline.json`)
**Writes to:** `_concept/01_project/`

## When to Use

- The user has a new app idea and no `_concept/01_project/` exists yet
- The user says things like "I have an app idea", "new project", "start from scratch"
- The user wants to redefine or rewrite an existing project brief
- The orchestrator dispatches this as the first conceptualization step

## When NOT to Use

- `_concept/01_project/brief.md` already exists and is approved — run downstream skills instead
- The user wants to add features to an existing concept — use the **features** skill
- The user wants to research the domain before writing a brief — use the **research** skill first, then come back here

## Prerequisites

### HARD-GATE

None. This is the first step in the pipeline. It has no dependencies (see `pipeline.json`: `depends_on: []`).

### Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid `_concept/` paths and naming rules
- `cf__shared/frontmatter.md` — required YAML fields and status lifecycle
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** None
**If gates fail:** N/A — this is the first step in the pipeline.
**On completion:** Present summary, then orchestrator suggests next steps.

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `cf__shared/concept_structure.md` | Yes |
| **Must read** | `cf__shared/frontmatter.md` | Yes |
| **Never load** | `03_features/`, `06_datamodel/`, `07_screens/`, or any downstream folder | — |

Keep context minimal. This skill writes the foundation — it should not be influenced by artifacts that do not yet exist.

## Workflow

### Step 1: Gather Context

The input can arrive in two forms:
- **Single field** — a free-form description (one text block). Extract as many fields as possible from it and generate the rest. Do NOT ask follow-up questions; work with what you have.
- **Multiple fields** — structured JSON or multiple filled-in fields. Use them directly.

**Check PLANS.md first.** If `_concept/PLANS.md` contains a `## Raw Description` section, treat it as a single-field input.

From whatever input is provided, derive as many of these as possible:
- **App Name** — infer from the description; use a concise working title
- **Elevator Pitch** — condense the core value proposition into one sentence
- **Target Audience** — who the app is for
- **Problem Statement** — the pain point described
- **Hero Flow** — the single most important action mentioned
- **Comparable Products** — similar apps if mentioned
- **Success Criteria** — goals, constraints, deadlines if mentioned

If a field cannot be inferred, make a reasonable assumption and note it in the brief so the user can correct it during review. Never block on missing input — generate the best brief you can and let the user review catch issues.

### Step 2: Write Project Artifacts

Create the directory and write three files:

```bash
mkdir -p _concept/01_project
```

**Output: `_concept/01_project/brief.md`**

```yaml
---
elevator_pitch: ""
audience: ""
problem: ""
hero_flow: ""
comparable_products: []
last_updated: YYYY-MM-DD
---
```

Body: full description in natural language — the app's vision, who it serves,
what problem it solves, and the primary user journey.

**Output: `_concept/01_project/goals.md`**

Success criteria, constraints, deadlines, known limitations.

**Output: `_concept/01_project/comparable.md`**

Apps the user referenced. For each: what it does well, what to borrow, what to avoid.

### Step 3: Emit Events

```
[cf_concept_overview] started
  run_id: <uuid>

[cf_concept_overview] checkpoint phase=brief_written
  files: 01_project/brief.md, 01_project/goals.md, 01_project/comparable.md
```

### Step 4: Hand Off

Emit:

```
[cf_concept_overview] completed
  run_id: <uuid>
  artifacts: 01_project/brief.md, 01_project/goals.md, 01_project/comparable.md
```

## Outputs

| File | Description |
|------|-------------|
| `_concept/01_project/brief.md` | Project vision, audience, problem, hero flow |
| `_concept/01_project/goals.md` | Success criteria, constraints, deadlines |
| `_concept/01_project/comparable.md` | Competitor analysis and reference apps |

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Writing features inside brief.md | The agent tries to be helpful and plan ahead | Stop at the brief. Features are a separate skill. If you catch yourself listing features, delete them. |
| Skipping user review | The agent assumes the brief is good enough | Always present the summary and let the user review before moving on. |
| Loading downstream artifacts | The agent reads 03_features/ or 07_screens/ "for context" | This is the first step. There is nothing valid to read downstream. Stick to the context budget. |
| Inventing comparable products | The user didn't mention any, so the agent makes some up | If the user has no comparables, say so in comparable.md. Do not fabricate references. |
| Setting status to approved immediately | The agent skips the draft stage | Let the user review and confirm the brief before marking it approved. |
| Blocking on missing input fields | The agent refuses to proceed without all fields filled | Generate the best brief from whatever input is given. Note assumptions so the user can correct during review. |

## Research Mode

When research is active, domain research and competitor analysis run in parallel
with this skill. Before writing the brief, check:

- `_concept/_research/general/domain.md` — for domain-specific findings that should inform the problem statement
- `_concept/_research/general/competitors.md` — for competitor analysis that should populate comparable.md

Check `_concept/_research/overview/user_input.json` for pre-collected user inputs before asking the user.

If research data exists, incorporate it into the brief and comparable files.
If it does not exist, proceed without it — research is optional.

**What this skill benefits from researching:** Market landscape, existing solutions
in the problem space, target audience demographics and pain points, domain terminology.

## Integration

- **Called by:** orchestrator or standalone (first step in conceptualization pipeline)
- **Feeds into:** all downstream skills via `_concept/01_project/` — see `pipeline.json` for the full dependency graph
- **Feedback loops:** None inbound (this is the root node). Outbound: every downstream skill reads from `01_project/`.
