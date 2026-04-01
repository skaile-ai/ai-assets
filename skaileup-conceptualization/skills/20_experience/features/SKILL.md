---
name: "features"
description: "Use after user journeys are approved to plan features. Derives features from stories.json candidate hints and the project brief. Writes feature files organized in numbered groups. Required before screens and datamodel skills."
metadata:
  version: "1.0.0"
  tags:
    - "features"
    - "requirements"
    - "modules"
    - "functionality"
    - "planning"
    - "story-refs"
    - "must-have"
    - "nice-to-have"
  source: "MERGED"
  prerequisites:
    files:
      - path: "_concept/1_discovery/1_overview/brief.md"
        gate: hard
        description: "Project brief required for feature context and scope"
      - path: "_concept/2_experience/1_journeys/stories.json"
        gate: hard
        description: "User journeys must exist — features are derived from story candidates"
    inputs_optional:
      - id: feature_priorities
        label: "Feature scope"
        type: select
        options:
          - "must-have features only"
          - "must-have + nice-to-have"
          - comprehensive
        default: "must-have + nice-to-have"
        hint: "How broad should the feature set be? Story stages set the default — this overrides the scope."
    reads:
      - path: "_concept/_grounding/general/domain.md"
        description: "Domain context to inform feature naming and scope"
      - path: "_concept/_grounding/general/competitors.md"
        description: "Competitor gaps to incorporate as differentiation features"
      - path: "_concept/_grounding/general/audiences.md"
        description: "Audience needs to prioritize feature depth"
    produces:
      - path: "_concept/2_experience/2_features"
        description: "Feature files organized in numbered groups (one .md per feature)"
  user_inputs:
    dialog:
      - id: "feature_priorities"
        label: "Feature scope"
        type: "select"
        options:
          - "must-have features only"
          - "must-have + nice-to-have"
          - "comprehensive"
        required: false
        default: "must-have + nice-to-have"
        hint: "How broad should the feature set be? Story stages set the default — this overrides the scope."
    files: []
---

# Features — Feature Planning

## Overview

The **features** skill is the Feature Planning agent. It derives features from
approved user journeys (`stories.json`) and the project brief, producing individual
feature files organized in numbered groups under `_concept/2_experience/2_features/`.

Features are the source of truth for screens and the data model. It does NOT write
screen specs, data models, brand, or tech stack.

## When to Use

- `_concept/2_experience/1_journeys/stories.json` exists and is approved
- The user says "define features", "what should the app do", "plan functionality"
- The orchestrator dispatches this after journeys are complete
- The user wants to redo or expand an existing feature set

## When NOT to Use

- No approved project brief or journeys yet — run **overview** then **journeys** first
- The user wants to design screens — use the **screens** skill (features must exist first)
- The user wants to define the data model — use the **datamodel** skill (features must exist first)
- Adding one new feature to an existing concept — use **add-feature** instead

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md`,
`skaileup-shared/contracts/frontmatter.md`, `skaileup-shared/contracts/feedback_loop.md`,
and `skaileup-shared/contracts/golden_principles.md` before proceeding.

**Hard gates:**
- `_concept/1_discovery/1_overview/brief.md` must exist and be non-empty
- `_concept/2_experience/1_journeys/stories.json` must exist and be non-empty

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/1_discovery/1_overview/brief.md` | Yes |
| Must read | `_concept/1_discovery/1_overview/goals.md` | Yes |
| Must read | `_concept/2_experience/1_journeys/stories.json` | Yes |
| Must read | `references/feature_template.md` | Yes |
| Check if present | `_concept/_grounding/general/competitors.md` | No |
| Check if present | `_concept/_grounding/general/audiences.md` | No |
| Check if present | `_concept/_grounding/features/user_input.json` | No |
| Never load | `_concept/2_experience/3_screens/`, `_concept/3_blueprint/` | — |

## Standalone Mode

**Gate check:** Both brief.md and stories.json must exist.
**On completion:** Show summary table and present next steps (screens, datamodel, orchestrator).

---

ROLE  Feature Planning agent — derives features from approved user journeys and the
      project brief, producing feature files in numbered groups.

READS
  _concept/1_discovery/1_overview/brief.md              — app name, audience, scope
  _concept/1_discovery/1_overview/goals.md              — success criteria, constraints
  _concept/2_experience/1_journeys/stories.json          — user journeys with candidate features
  ? _concept/_grounding/general/competitors.md           — feature gaps from competitor analysis
  ? _concept/_grounding/general/audiences.md             — user needs influencing priorities
  ? _concept/_grounding/features/user_input.json         — pre-collected dialog answers

WRITES
  _concept/2_experience/2_features/<NN_group>/<feature>.md  — one file per feature

REFERENCES
  skaileup-shared/contracts/concept_structure.md      — valid _concept/ paths and naming rules
  skaileup-shared/contracts/frontmatter.md            — required YAML fields
  skaileup-shared/contracts/feedback_loop.md          — how downstream skills modify feature files
  skaileup-shared/contracts/golden_principles.md      — group naming, entity naming rules
  references/feature_template.md                 — file template, identification questions

MUST  organize features in numbered group folders (01_user_auth, 02_dashboard, etc.)
MUST  include frontmatter: priority, story_refs, roles, permissions, agent_notes, screens, data_entities, last_updated
MUST  include a story_refs field on every feature — traces back to stories.json stories
MUST  leave screens[] and data_entities[] empty — populated by downstream skills via feedback loop
NEVER  write screen specs, data models, brand, or tech stack files
NEVER  populate screens[] or data_entities[] — those fields are filled by the screens and datamodel skills
NEVER  invent features with no traceability to the brief or stories.json

EMIT  [features] started run_id=<uuid>

STEP 1: Read context
  - Read _concept/1_discovery/1_overview/brief.md
  - Stop if missing or empty:
    > "No approved project brief found. Run `overview` first."
  - Read _concept/2_experience/1_journeys/stories.json
  - Stop if missing or empty:
    > "No user journeys found. Run `journeys` first."
  - Check _grounding/features/user_input.json for pre-collected answers (feature_priorities, etc.)
  - Extract candidate_features from all story downstream links
  - Use story stages to set default feature priority:
    - hero flow stories → must-have features
    - vital journey stories → must-have features
    - hygiene flow stories → must-have features (but simpler scope)
    - backlog flow stories → nice-to-have features
  - If feature_priorities user input is set, use it to calibrate scope:
    - "must-have features only" — skip all backlog-derived and nice-to-have candidates
    - "must-have + nice-to-have" — include hygiene and backlog candidates as nice-to-have (default)
    - "comprehensive" — include all candidates including backlog
  IF _concept/_grounding/general/ exists
    - Read competitors.md for feature gaps (if present)
    - Read audiences.md for user needs and priorities (if present)

STEP 2: Identify and group features
  - Start from the candidate_features extracted from stories.json
  - Group related candidates into feature groups (e.g. user authentication, task management)
  - For each feature, answer the identification questions (see references/feature_template.md)
  - Create numbered group folders using NN_ prefix (no letter prefix):
    $ mkdir -p _concept/2_experience/2_features/01_user_auth
  - Write one feature file per feature using the template in references/feature_template.md

STEP 2b: Define roles and permissions
  - Identify all user roles mentioned across the brief, goals, and stories
  - For each feature, determine which roles can:
    - View / Read data
    - Create new records
    - Edit existing records
    - Delete records
    - Perform special actions (approve, assign, export, etc.)
  - Write a ## Permissions section in each feature file with a role-action matrix
  - Add permissions field to each feature's frontmatter

OUTPUT _concept/2_experience/2_features/<NN_group>/<feature>.md
  ---
  priority: <must-have|nice-to-have>
  story_refs: [<story-id-1>, <story-id-2>]
  roles: [<role_list>]
  permissions:
    <role>: [<action_list>]
  agent_notes: |
    <context from user conversation>
  screens: []
  data_entities: []
  last_updated: <YYYY-MM-DD>
  ---
  # Feature: <Name>
  ## Description
  ## User Benefit
  ## Requirements
  ## Success Criteria
  ## Error States
  ## Permissions
  | Action | <role_1> | <role_2> | ... |
  |--------|----------|----------|-----|
  | View   | yes      | yes      |     |
  | Create | yes      | no       |     |
  | Edit   | yes      | own only |     |
  | Delete | admin    | no       |     |

STEP 3: Present summary
  - Show summary table:
    | # | Feature | Group | Priority | Roles |
    |---|---------|-------|----------|-------|
    | 1 | Login | 01_user_auth | must-have | all_users |
    | 2 | Registration | 01_user_auth | must-have | all_users |
    | 3 | Dashboard | 02_dashboard | must-have | all_users |
  - Include totals: N features (X must-have, Y nice-to-have)
  - Show permissions overview across all features:
    | Feature | <role_1> | <role_2> | ... |
    |---------|----------|----------|-----|
    | <feat>  | full     | view     |     |

EMIT  [features] checkpoint phase=features_written groups=<N> features=<total> must_have=<X> nice_to_have=<Y>

STEP 4: Human review
  CHECKPOINT features_approved
    > "Does this feature list look complete? Add, rename, remove, or reprioritize anything before I continue."

  UNTIL user approves

STEP 5: Hand off
  > "Features approved. Next steps:
  > - Run `screens` to specify the UI screens for these features
  > - Run `datamodel` to design the data schema
  > - Or run `concept-orchestrator` to continue the full pipeline"

EMIT  [features] completed run_id=<uuid> feature_count=<N> groups=<N>

CHECKLIST
  - [ ] _concept/1_discovery/1_overview/brief.md was read and exists
  - [ ] _concept/2_experience/1_journeys/stories.json was read and exists
  - [ ] Every feature traces to at least one story via story_refs
  - [ ] Every feature has required frontmatter (priority, story_refs, roles, last_updated)
  - [ ] screens[] and data_entities[] are empty (not pre-populated)
  - [ ] Group folders use sequential NN_ numbering (no letter prefix)
  - [ ] Summary table shown and user has explicitly approved

---

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---|---|---|
| Writing screen specs inside feature files | Agent conflates "what the app does" with "what the UI looks like" | Features describe functionality and requirements. Screens are a separate skill. |
| Missing story_refs on features | Agent creates features without linking to journeys | Every feature must list which stories drive it. If you can't find a story, question whether the feature belongs. |
| Populating screens[] or data_entities[] | Agent tries to be helpful and pre-fills downstream fields | Leave these empty. They are populated by the screens and datamodel skills via the feedback loop. |
| Letter-prefixed group folders (A_01_) | Agent follows old CF naming convention | Use NN_ prefix only (01_user_auth, not A_01_user_auth). |
| Creating features not in the brief or stories | Agent invents features from domain knowledge | Every feature must trace back to the brief's problem statement, audience, hero flow, or a story candidate. |
| Inventing features from scratch when stories exist | Agent ignores candidate_features hints | Start from stories.json downstream.candidate_features — these are the journeys team's intent. |

## Research Mode

When research data exists, check before writing features:

- `_concept/_grounding/general/competitors.md` — competitor feature sets, gaps, differentiators
- `_concept/_grounding/general/audiences.md` — user needs influencing priorities and scope

If research data exists, use it to inform feature priorities and identify gaps.
If it does not exist, proceed from brief and stories alone.

**What this skill benefits from researching:** Competitor feature matrices,
common feature patterns for the app's domain, user workflow best practices,
accessibility requirements for the target audience.

## Integration

- **Called by:** `concept-orchestrator` or standalone (after journeys)
- **Requires:** `_concept/1_discovery/1_overview/brief.md` and `_concept/2_experience/1_journeys/stories.json`
- **Feedback from downstream:**
  - **datamodel** skill populates `data_entities[]` in feature frontmatter
  - **screens** skill populates `screens[]` in feature frontmatter
- **Feeds into:** screens, datamodel — both depend on features existing
