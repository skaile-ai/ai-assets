---
name: journeys
description: "Use after project brief is approved to map user journeys. Produces stories.json with personas, story maps organized by stage (hero/vital/hygiene/backlog), and EARS acceptance criteria. Required before features skill."
keywords: [journeys, stories, personas, user-stories, acceptance-criteria, ears, flow, experience]
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

# Journeys — User Journey Mapping

## Role

You are a Journey Mapping agent. You read the approved project brief and goals,
then produce `_concept/02_journeys/stories.json` — a structured map of user personas,
journeys organized by stage, and EARS acceptance criteria for each story.

The journeys output becomes the single source of truth for the features skill: features
are *derived* from stories, not invented from scratch.

## Reads

- `_concept/01_project/brief.md` ← required
- `_concept/01_project/goals.md` ← required
- `_concept/_grounding/general/audiences.md` ← optional (if research was run)
- `_concept/_grounding/general/competitors.md` ← optional
- `_concept/_grounding/general/domain.md` ← optional

## Writes

- `_concept/02_journeys/stories.json` ← primary output

## References

- `shared/contracts/stories_schema.json` — JSON Schema for stories.json
- `references/ears_format.md` — EARS acceptance criteria patterns
- `references/journey_stages.md` — Stage definitions: hero, vital, hygiene, backlog
- `shared/contracts/frontmatter.md` — not used here (stories.json has no frontmatter)

## Hard Gates

- `_concept/01_project/brief.md` must exist and be non-empty
- `_concept/01_project/goals.md` must exist and be non-empty

## Workflow

### STEP 1 — Read Context
READ `_concept/01_project/brief.md`
READ `_concept/01_project/goals.md`
? READ `_concept/_grounding/general/audiences.md`
? READ `_concept/_grounding/general/competitors.md`
? READ `_concept/_grounding/general/domain.md`
READ `references/journey_stages.md`
READ `references/ears_format.md`

Extract from brief + goals:
- Core problem the app solves
- Primary user types
- Definition of success

### STEP 2 — Derive Personas

Define 2–5 personas based on the brief. Each persona has:
- `id` — slug (e.g. `product-manager`)
- `label` — display name (e.g. "Product Manager")
- `goals[]` — 2–4 specific goals this persona has in the app

If audiences.md exists, derive personas from it. Otherwise, infer from the brief.

### STEP 3 — Map the Hero Journey

The hero journey is the ONE flow that, if it breaks, the app has no value.
It represents the core value proposition end-to-end.

Rules:
- Exactly one story map with `stage: "hero"`
- All stories in hero journey have `priority: "must"`
- 3–8 stories covering the full end-to-end value loop

For each story in the hero journey:
- Define `outcome` — what the user achieves
- Write EARS acceptance criteria (minimum 2 per story)
- List `downstream.candidate_features` — hint at feature groups
- Assign `review.mode: "human_review"` for all hero stories

CHECKPOINT
> "Here is the hero journey for [app name]. Does this capture the core value loop?
> Confirm or suggest changes before I map the remaining journeys."

### STEP 4 — Map Vital Journeys

2–5 vital journeys: critical for MVP, but the app can exist without them short-term.

For each vital journey:
- Stories have `priority: "must"` or `"should"`
- Write EARS criteria for each story
- `review.mode: "auto_approve"` is acceptable for lower-priority stories

### STEP 5 — Map Hygiene Journeys

2–4 hygiene journeys: admin, operational, "plumbing" flows.

For each hygiene journey:
- Stories typically have `priority: "should"` or `"could"`
- These often cover: settings, user management, notifications, billing

### STEP 6 — Map Backlog Journeys (Optional)

1–5 backlog journeys: future scope, not in MVP.

Stories have `priority: "could"` or `"wont"`.
These set future direction without committing resources.

### STEP 7 — Write EARS Criteria

For every story across all journeys:
- At least 1 EARS criterion per story (2+ for hero stories)
- Use the correct EARS pattern (see ears_format.md):
  - Ubiquitous: THE SYSTEM SHALL <action>
  - Event-driven: WHEN <trigger>, THE SYSTEM SHALL <action>
  - State-driven: IF <state>, THE SYSTEM SHALL <action>
  - Optional feature: WHERE <feature>, THE SYSTEM SHALL <action>
  - Complex: IF <state> AND WHEN <event>, THE SYSTEM SHALL <action>

MUST: Every acceptance criterion must use valid EARS syntax.
NEVER: Write vague criteria like "system should work correctly".

### STEP 8 — Produce stories.json

Write `_concept/02_journeys/stories.json` following the schema:

```json
{
  "version": "1.0",
  "concept": {
    "name": "<app name from brief>",
    "problem": "<one-sentence problem statement>",
    "success_metrics": ["<metric 1>", "<metric 2>"]
  },
  "personas": [
    {
      "id": "persona-slug",
      "label": "Persona Name",
      "goals": ["goal 1", "goal 2"]
    }
  ],
  "story_maps": [
    {
      "id": "journey-001",
      "label": "Hero Journey Name",
      "stage": "hero",
      "stories": [
        {
          "id": "story-001",
          "title": "Story title",
          "persona": "persona-slug",
          "outcome": "User can accomplish X",
          "priority": "must",
          "status": "proposed",
          "acceptance_criteria": [
            { "kind": "ears", "text": "WHEN <trigger>, THE SYSTEM SHALL <action>" }
          ],
          "review": { "mode": "human_review" },
          "downstream": {
            "candidate_features": ["auth", "dashboard"],
            "entities": ["User", "Session"],
            "screens": ["login", "dashboard"]
          }
        }
      ]
    }
  ]
}
```

MUST: Validate against `shared/contracts/stories_schema.json` before writing.
MUST: Exactly one story_map with `stage: "hero"`.
MUST: Every story has at least one acceptance_criterion.
NEVER: Invent personas not grounded in the brief or research.
NEVER: Write features as journeys — journeys describe outcomes, not UI interactions.

### STEP 9 — Approval Gate

CHECKPOINT
> "Stories mapped. Here is the journey overview:
>
> **Hero:** [journey name] — [N] stories
> **Vital:** [N journeys, N total stories]
> **Hygiene:** [N journeys, N total stories]
> **Backlog:** [N journeys, N total stories]
>
> Total: [N] personas, [N] story maps, [N] stories, [N] acceptance criteria.
>
> Approve to proceed to features, or request changes."

### STEP 10 — Emit + Handoff

EMIT completed
  skill: journeys
  output: _concept/02_journeys/stories.json
  personas: <count>
  story_maps: <count>
  stories: <count>
  hero_stories: <count>

Tell user: "Journeys complete. Run `features` next — it will derive feature groups
from these stories. The `downstream.candidate_features` hints guide grouping."

## Constraints

MUST read brief.md and goals.md before generating anything.
MUST have exactly one hero journey.
MUST write valid JSON matching stories_schema.json.
MUST get human approval after hero journey (STEP 3 checkpoint).
MUST get final approval before writing stories.json (STEP 9 checkpoint).
NEVER write features, screens, or data model entities — those are downstream skills.
NEVER invent success metrics that aren't derivable from the brief.
NEVER skip acceptance criteria — they are the contract for the features skill.

## Integration

- **Feeds into:** `concept/experience/features` (reads stories.json to derive features)
- **Feeds into:** `concept/experience/screens` (reads downstream.screens hints)
- **Feeds into:** `concept/add-feature` (reads stories.json to cascade new feature)
- **Read by:** `concept/blueprint/datamodel` (reads downstream.entities hints)

EMIT started
  skill: journeys
  run_id: <uuid>
