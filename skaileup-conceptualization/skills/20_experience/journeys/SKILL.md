---
name: "journeys"
description: "Use after project brief is approved to map user journeys. Reads the approved brief, goals, and optional research to define personas and story maps organized by stage (hero, vital, hygiene, backlog). Writes stories.json with EARS acceptance criteria. Required before features skill."
metadata:
  version: "1.0.0"
  tags:
    - "journeys"
    - "stories"
    - "personas"
    - "story-map"
    - "user-flow"
    - "acceptance-criteria"
    - "ears"
    - "experience"
  source: "MERGED"
  prerequisites:
    files:
      - path: "_concept/1_discovery/1_overview/brief.md"
        gate: hard
        description: "Project brief must exist and be approved before mapping journeys"
    reads:
      - path: "_concept/_grounding/general/audiences.md"
        description: "Audience personas from research phase"
      - path: "_concept/_grounding/general/competitors.md"
        description: "Competitor analysis for journey gaps"
      - path: "_concept/_grounding/general/domain.md"
        description: "Domain context for journey staging"
    produces:
      - path: "_concept/2_experience/1_journeys/stories.json"
        description: "Personas, story maps by stage, EARS acceptance criteria"
---

# Journeys — User Journey Mapping

## Overview

The **journeys** skill is the Journey Mapping agent. It reads the approved project
brief and goals, then produces `_concept/2_experience/1_journeys/stories.json` — a
structured map of user personas, story maps organized by stage, and EARS acceptance
criteria for each story.

The journeys output is the single source of truth for the features skill: features
are *derived* from stories, not invented from scratch.

## When to Use

- The project brief (`_concept/1_discovery/1_overview/brief.md`) has been approved
- You need to define what users will do in the app before speccing features
- The orchestrator dispatches this as step 2 in the concept pipeline

## When NOT to Use

- `_concept/2_experience/1_journeys/stories.json` already exists and is approved
- No approved brief yet — run the **overview** skill first
- You want to add a specific feature — use **features** or **add-feature** directly

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md`,
`skaileup-shared/contracts/frontmatter.md`, and `skaileup-shared/contracts/acceptance_criteria.md`
before proceeding.

**Hard gate:** `_concept/1_discovery/1_overview/brief.md` must exist and be non-empty.

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/1_discovery/1_overview/brief.md` | Yes |
| Must read | `_concept/1_discovery/1_overview/goals.md` | Yes |
| Must read | `references/ears_format.md` | Yes |
| Must read | `references/journey_stages.md` | Yes |
| Check if present | `_concept/_grounding/general/audiences.md` | No |
| Check if present | `_concept/_grounding/general/competitors.md` | No |
| Check if present | `_concept/_grounding/general/domain.md` | No |
| Never load | `2_experience/2_features/`, `3_blueprint/`, or downstream | — |

## Standalone Mode

**Gate check:** `_concept/1_discovery/1_overview/brief.md` must exist.
**On completion:** Show summary table and present next steps (features, orchestrator).

---

ROLE  Journey Mapping agent — reads the approved project brief and produces
      `_concept/2_experience/1_journeys/stories.json` only.

READS
  _concept/1_discovery/1_overview/brief.md              — app name, audience, problem, hero_flow
  _concept/1_discovery/1_overview/goals.md              — success criteria, constraints, deadlines
  ? _concept/_grounding/general/audiences.md            — detailed persona profiles from research
  ? _concept/_grounding/general/competitors.md          — competitor flows and feature gaps
  ? _concept/_grounding/general/domain.md               — domain terminology and workflows

WRITES
  _concept/2_experience/1_journeys/stories.json         — personas, story maps, acceptance criteria

REFERENCES
  skaileup-shared/contracts/concept_structure.md      — valid _concept/ paths and naming rules
  skaileup-shared/contracts/frontmatter.md            — required YAML fields
  skaileup-shared/contracts/acceptance_criteria.md    — EARS patterns and AC rules
  skaileup-shared/contracts/stories_schema.json       — JSON Schema for stories.json validation
  references/ears_format.md                      — EARS syntax patterns and examples
  references/journey_stages.md                   — hero / vital / hygiene / backlog definitions

MUST  produce exactly one hero story map — the single most important user journey
MUST  write EARS acceptance criteria for every story (minimum 2 per hero story, 1 per other)
MUST  derive personas from brief audience and research (when available)
MUST  validate stories.json against skaileup-shared/contracts/stories_schema.json before writing
MUST  include downstream hints (candidate_features, candidate_entities, candidate_screens) for every story
MUST  set status: proposed on all new stories
NEVER  write feature files, screen specs, data models, or any artifact outside 2_experience/1_journeys/
NEVER  define more than one hero story map — hero is always exactly one
NEVER  skip acceptance criteria — every story must have at least one EARS criterion

EMIT  [journeys] started run_id=<uuid>

STEP 1: Read context
  - Read _concept/1_discovery/1_overview/brief.md
  - Stop if missing or empty:
    > "No approved project brief found. Run `overview` first."
  - Read _concept/1_discovery/1_overview/goals.md for success criteria and constraints
  - Extract: app name, problem, audience, hero_flow, success_metrics
  IF _concept/_grounding/general/ exists
    - Read audiences.md for detailed persona profiles (if present)
    - Read competitors.md for competitor user flows and gaps (if present)
    - Read domain.md for domain terminology and workflows (if present)

STEP 2: Define personas
  - Identify distinct user types from brief audience field
  IF _concept/_grounding/general/audiences.md exists
    - Enrich personas with research findings (goals, pain points, context)
  ELSE
    - Derive personas from brief and goals alone
  - Define 2–5 personas; each has:
    - id: slug (e.g. "ops-manager", "field-technician")
    - label: human-readable name (e.g. "Operations Manager")
    - goals: list of what this persona wants to achieve with the app
  - Present persona list to user:
    > "I've identified these personas: [list with goals]. Add, remove, or adjust?"
  - Wait for confirmation before proceeding

STEP 3: Map Hero Journey
  - Use hero_flow from brief.md as the starting point
  - Build a single story map representing the most important end-to-end user journey
  - This is the journey that, if it fails, the app has no value
  - Break the hero flow into 3–8 sequential stories; each has:
    - id: "story-<NNN>" (e.g. "story-001")
    - title: short action phrase
    - persona: references persona id
    - outcome: what the user achieves
    - priority: must (hero stories are always must)
    - status: proposed
    - acceptance_criteria: minimum 2 EARS criteria per story
    - review: { mode: "human_review" }
    - downstream: candidate_features, candidate_entities, candidate_screens
  - Set story map stage: hero

  CHECKPOINT hero_approved
    > "Here is the hero journey for [app name]:
    > [brief narrative of the end-to-end flow]
    >
    > Does this capture the core value loop? Confirm or suggest changes before
    > I map the remaining journeys."

  UNTIL user approves
    - Apply requested changes
    - Show updated hero journey and ask for approval again

STEP 4: Map Vital Journeys
  - Identify other critical user journeys needed for MVP scope
  - These complement the hero flow and make the app viable for daily use
  - Each vital story map covers a distinct user journey (e.g. "manage team", "view reports")
  - Stories use priority must or should
  - Include downstream hints for every story
  - Set story map stage: vital

STEP 5: Map Hygiene Flows
  - Identify admin and operational flows that enable the app to function
  - Examples: user onboarding, settings configuration, data import/export, role management
  - These are not the reason users buy the app, but the app cannot operate without them
  - Stories use priority should or could
  - Set story map stage: hygiene

STEP 6: Map Backlog Flows
  - Capture future user journeys out of MVP scope
  - Sources: competitor analysis gaps, nice-to-have ideas from brief, advanced workflows in goals.md
  - Stories use priority could or wont
  - Set story map stage: backlog

STEP 7: Write EARS acceptance criteria
  - For every story across all story maps, write acceptance criteria using EARS patterns
  - Use the appropriate EARS pattern for each criterion (see references/ears_format.md):
    - Ubiquitous: THE SYSTEM SHALL <action>
    - Event-driven: WHEN <trigger>, THE SYSTEM SHALL <action>
    - State-driven: IF <state>, THE SYSTEM SHALL <action>
    - Optional feature: WHERE <feature>, THE SYSTEM SHALL <action>
    - Complex: IF <state> AND WHEN <event>, THE SYSTEM SHALL <action>
  - Each criterion: kind: "ears", text: <EARS statement>
  - Optionally add gherkin_scenarios for complex stories that benefit from step-by-step spec

STEP 8: Write stories.json
  - $ mkdir -p _concept/2_experience/1_journeys
  - Populate concept section from brief.md (name, problem, success_metrics)
  - Set review.mode: human_review for hero and vital stories, auto_approve for hygiene and backlog

  OUTPUT _concept/2_experience/1_journeys/stories.json
    {
      "version": "1.0",
      "concept": { "name": "<app>", "problem": "...", "success_metrics": [...] },
      "personas": [{ "id": "...", "label": "...", "goals": [...] }],
      "story_maps": [{
        "id": "journey-<NNN>", "label": "...", "stage": "hero",
        "stories": [{
          "id": "story-<NNN>", "title": "...", "persona": "<persona-id>",
          "outcome": "...", "priority": "must",
          "status": "proposed",
          "acceptance_criteria": [{ "kind": "ears", "text": "WHEN ... THE SYSTEM SHALL ..." }],
          "review": { "mode": "human_review" },
          "downstream": {
            "candidate_features": ["..."],
            "candidate_entities": ["..."],
            "candidate_screens": ["..."]
          }
        }]
      }]
    }

  - Validate against skaileup-shared/contracts/stories_schema.json

EMIT  [journeys] checkpoint phase=stories_written personas=<N> story_maps=<N> stories=<total>

STEP 9: Present summary and get final approval
  - Show summary table:

    | # | Journey | Stage | Stories | Must | Should | Could | Wont |
    |---|---------|-------|---------|------|--------|-------|------|
    | 1 | <label> | hero  | N       | N    | 0      | 0     | 0    |
    | 2 | <label> | vital | N       | N    | N      | 0     | 0    |
    | ...                                                          |
    | Total                | N       | N    | N      | N     | N    |

  - Show persona summary: N personas defined
  - Show acceptance criteria count: N EARS criteria across all stories
  - Show downstream coverage: N candidate features, N candidate entities, N candidate screens

  CHECKPOINT journeys_approved
    > "Here are your user journeys:
    > - Hero: [label] — the core flow that defines your app
    > - Vital: [N] journeys for MVP scope
    > - Hygiene: [N] operational flows
    > - Backlog: [N] future flows
    >
    > Total: [N] stories with [N] acceptance criteria.
    > Does this capture the right user experience? Add, remove, or reprioritize?"

  UNTIL user approves

STEP 10: Hand off
  > "User journeys approved. Next steps:
  > - Run `features` to derive features from these journeys
  > - Run `concept-orchestrator` to continue the full pipeline"

EMIT  [journeys] completed run_id=<uuid> personas=<N> story_maps=<N> stories=<total> hero_stories=<N> acceptance_criteria=<N>

CHECKLIST
  - [ ] _concept/1_discovery/1_overview/brief.md was read and exists
  - [ ] Personas defined with ids, labels, and goals
  - [ ] Exactly one story map has stage: hero
  - [ ] Hero journey approved by user before remaining journeys were mapped
  - [ ] Every story has at least one EARS acceptance criterion
  - [ ] Every story has downstream hints (candidate_features, candidate_entities, candidate_screens)
  - [ ] stories.json validates against skaileup-shared/contracts/stories_schema.json
  - [ ] Priority distribution: hero stories are must, backlog stories are could or wont
  - [ ] Summary table shown and user has explicitly approved

---

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---|---|---|
| Defining more than one hero | Agent maps multiple "important" flows as hero | Pick the single flow that defines the app's core value. Everything else is vital or hygiene. |
| Skipping EARS criteria | Agent writes vague "system should work" criteria | Every story must have at least one properly-formed EARS criterion. See ears_format.md. |
| Hero journey not from brief.hero_flow | Agent ignores the brief's hero_flow field | Always start the hero journey from brief.md hero_flow. It is the approved core flow. |
| Inventing competitor flows | No research was run, agent fills gaps | If no research exists, derive journeys from brief alone. Do not fabricate competitor patterns. |
| Including downstream features in stories.json | Agent tries to be helpful and spec features early | Only include candidate hints (strings). Full feature specs are the features skill's job. |
| Proceeding past hero CHECKPOINT | Agent rushes to finish | Always wait for explicit user approval of the hero journey before mapping vital/hygiene. |

## Integration

- **Called by:** `concept-orchestrator` or standalone (step 2 in the concept pipeline)
- **Requires:** `_concept/1_discovery/1_overview/brief.md` to exist and be approved
- **Feeds into:** `features` skill reads `stories.json` to derive feature specs
- **Feedback loops:** None inbound. Features skill uses `story_refs` to link back.
