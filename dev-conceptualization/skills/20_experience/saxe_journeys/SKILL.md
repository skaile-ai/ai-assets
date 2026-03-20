---
name: concept-2-experience-1-journeys
description: "Step 4: User journey mapping. Reads the approved project brief, goals, and optional research to define personas and story maps organized by stage (hero, vital, hygiene, backlog). Writes stories.json with EARS acceptance criteria. Features (step 5) derive from these journeys."
keywords: journeys, stories, personas, story map, user flow, acceptance criteria, EARS
---

ROLE  Journey Mapping agent — reads the approved project brief and produces user journey
      story maps with personas, prioritized stories, and EARS acceptance criteria.

READS
  _concept/1_discovery/1_overview/brief.md              — app name, audience, problem, hero_flow
  _concept/1_discovery/1_overview/goals.md              — success criteria, constraints, deadlines
  ? _concept/1_discovery/2_research/audiences.md       — detailed persona profiles from research
  ? _concept/1_discovery/2_research/competitors.md     — competitor flows and feature gaps
  ? _concept/1_discovery/2_research/domain.md          — domain terminology and workflows

WRITES
  _concept/2_experience/1_journeys/stories.json         — personas, story maps, acceptance criteria

REFERENCES
  shared/contracts/concept_structure.md      — valid _concept/ paths and naming rules
  shared/contracts/frontmatter.md            — required YAML fields and status lifecycle
  shared/contracts/stories_schema.json       — JSON Schema for stories.json validation
  references/ears_format.md         — EARS requirement syntax patterns and examples
  references/journey_stages.md      — hero / vital / hygiene / backlog stage definitions

MUST  produce exactly one hero story map — the single most important user journey
MUST  write EARS acceptance criteria for every story
MUST  derive personas from brief audience and research (when available)
MUST  validate stories.json against shared/contracts/stories_schema.json before finishing
MUST  include downstream hints (candidate_features, candidate_entities, candidate_screens) for every story
MUST  set status: proposed on all new stories
NEVER  write feature files, screen specs, data models, or any artifact outside 2_experience/1_journeys/
NEVER  define more than one hero story map — hero is always exactly one
NEVER  skip acceptance criteria — every story must have at least one EARS criterion

EMIT  [concept-2-experience-1-journeys] started run_id=<uuid>

# -- Workflow ----------------------------------------------------------------

STEP 1: Read context
  - Read _concept/1_discovery/1_overview/brief.md
  - Stop if missing or empty:
    > "No approved project brief found. Run `concept-1-discovery-1-overview` first."
  - Read _concept/1_discovery/1_overview/goals.md for success criteria and constraints
  - Extract: app name, problem, audience, hero_flow, success_metrics
  IF _concept/1_discovery/2_research/ exists
    - Read audiences.md for detailed persona profiles
    - Read competitors.md for competitor user flows and gaps
    - Read domain.md for domain terminology and workflows

STEP 2: Define personas
  - Identify distinct user types from brief audience field
  IF _concept/1_discovery/2_research/audiences.md exists
    - Enrich personas with research findings (goals, pain points, context)
  ELSE
    - Derive personas from brief and goals alone
  - For each persona, define:
    - id: slug (e.g. "ops-manager", "field-technician")
    - label: human-readable name (e.g. "Operations Manager")
    - goals: list of what this persona wants to achieve with the app
  - Present persona list to user:
    > "I've identified these personas: [list with goals]. Add, remove, or adjust?"
  - Wait for confirmation before proceeding

STEP 3: Map Hero Flow
  - Use hero_flow from brief.md as the starting point
  - Build a single story map representing the most important end-to-end user journey
  - This is the journey that, if it fails, the app has no value
  - Break the hero flow into sequential stories, each with:
    - id: "story-<NNN>" (e.g. "story-001")
    - title: short action phrase
    - persona: references persona id
    - outcome: what the user achieves
    - priority: must (hero stories are always must)
    - status: proposed
  - Set story map stage: hero

STEP 4: Map Vital Journeys
  - Identify other critical user journeys needed for MVP scope
  - These are flows that complement the hero flow and make the app complete
  - Each vital story map covers a distinct user journey (e.g. "manage team", "view reports")
  - Stories within vital journeys use priority must or should
  - Set story map stage: vital

STEP 5: Map Hygiene Flows
  - Identify admin and operational flows that enable the app to function
  - Examples: user onboarding, settings configuration, data import/export, role management
  - These are not the reason users buy the app, but the app cannot operate without them
  - Stories within hygiene flows use priority should or could
  - Set story map stage: hygiene

STEP 6: Map Backlog Flows
  - Capture future user journeys that are out of MVP scope
  - These come from: competitor analysis gaps, nice-to-have ideas from the brief,
    advanced workflows mentioned in goals.md
  - Stories within backlog flows use priority could or wont
  - Set story map stage: backlog

STEP 7: Write EARS acceptance criteria
  - For every story across all story maps, write acceptance criteria using EARS patterns
  - Use the appropriate EARS pattern for each criterion (see references/ears_format.md):
    - Ubiquitous: for always-true system behavior
    - Event-driven: for trigger-response behavior
    - State-driven: for conditional behavior based on system state
    - Optional feature: for configurable behavior
    - Complex: for combined state + event conditions
  - Each acceptance criterion has: kind ("ears"), text (the EARS statement)
  - Optionally add gherkin_scenarios for complex stories that benefit from
    step-by-step behavioral specification

STEP 8: Write stories.json
  - Populate the concept section from brief.md (name, problem, success_metrics)
  - Populate downstream hints for each story:
    - candidate_features: likely features this story implies
    - candidate_entities: likely data entities this story touches
    - candidate_screens: likely screens this story needs
  - Set review.mode to human_review for hero and vital stories, auto_approve for hygiene and backlog
  - $ mkdir -p _concept/2_experience/1_journeys
  - Write _concept/2_experience/1_journeys/stories.json
  - Validate against shared/contracts/stories_schema.json

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

EMIT  [concept-2-experience-1-journeys] checkpoint phase=stories_written personas=<N> story_maps=<N> stories=<total>

STEP 9: Present summary and get approval
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

  CHECKPOINT journeys
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
  - Confirm approval and present next steps:
    > "User journeys approved. Next steps:
    > - Run `concept-2-experience-2-features` to derive features from these journeys
    > - Run `concept` to continue the full pipeline"

EMIT  [concept-2-experience-1-journeys] completed run_id=<uuid> personas=<N> story_maps=<N> stories=<total> hero_stories=<N> acceptance_criteria=<N>

CHECKLIST
  - [ ] brief.md was read and exists
  - [ ] Personas defined with ids, labels, and goals
  - [ ] Exactly one story map has stage: hero
  - [ ] Every story has at least one EARS acceptance criterion
  - [ ] Every story has downstream hints (candidate_features, candidate_entities, candidate_screens)
  - [ ] stories.json validates against shared/contracts/stories_schema.json
  - [ ] Priority distribution: hero stories are must, backlog stories are could or wont
  - [ ] Summary table shown and approved by user
