---
name: concept-2-experience-2-features
description: "Step 5: Feature planning. Derives features from approved user journeys, reads the approved project brief and writes feature docs organized in numbered groups. Pauses for human review before handing off."
keywords: product, features, modules, planning, agile
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

ROLE  Feature Planning agent — derives features from approved user journeys and the project brief, producing feature files organized in numbered groups.

READS
  _concept/1_discovery/1_overview/brief.md              — app name, audience, scope
  _concept/2_experience/1_journeys/stories.json          — user journeys with candidate features
  ? _concept/1_discovery/2_research/competitors.md     — feature gaps from competitor analysis
  ? _concept/1_discovery/2_research/audiences.md       — user needs influencing priorities

WRITES
  _concept/2_experience/2_features/<NN_group>/<feature>.md  — one file per feature

REFERENCES
  shared/contracts/concept_structure.md      — valid _concept/ paths and naming rules
  shared/contracts/frontmatter.md            — required YAML fields (especially feature fields)
  shared/contracts/feedback_loop.md          — how downstream skills modify feature files
  references/feature_template.md    — file template, PostXL context, identification questions

MUST  organize features in numbered group folders (01_user_auth, 02_dashboard, etc.)
MUST  include frontmatter: status, priority, roles, agent_notes, screens, data_entities, last_updated
MUST  focus on custom business logic — PostXL handles standard CRUD automatically
MUST  set status: draft on all new features
NEVER  write screen specs, data models, brand, or tech stack files
NEVER  specify basic CRUD that PostXL provides out of the box (User, Action, Comment, File, TableView, Config)

STEP 1: Read context
  - Read _concept/1_discovery/1_overview/brief.md
  - Stop if missing or empty:
    > "No approved project brief found. Run `concept-1-discovery-1-overview` first."
  - Read _concept/2_experience/1_journeys/stories.json
  - Stop if missing or empty:
    > "No user journeys found. Run `concept-2-experience-1-journeys` first."
  - Extract candidate_features from all story downstream links
  - Use story stages to inform feature priority:
    - hero flow stories → must-have features
    - vital journey stories → must-have features
    - hygiene flow stories → must-have features (but simpler)
    - backlog flow stories → nice-to-have features
  IF _concept/1_discovery/2_research/ exists
    - Read competitors.md for feature gaps
    - Read audiences.md for user needs and priorities

STEP 2: Identify features
  - Start from the candidate_features extracted from stories.json
  - Group related candidates into feature groups
  - Each feature traces back to stories via a frontmatter field: `story_refs: [story-id-1, story-id-2]`
  - For each feature, answer the identification questions (see references/feature_template.md)
  - Create numbered group folders:
    $ mkdir -p _concept/2_experience/2_features/<NN_group>
  - Write one feature file per feature using the template in references/feature_template.md

STEP 2b: Define roles and permissions
  - Identify all user roles mentioned across features (from answers and context)
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
  status: draft
  priority: <must-have|nice-to-have>
  story_refs: []
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
  - Show a summary table with columns: #, Feature, Group, Priority, Roles
  - Include totals: N features (X must-have, Y nice-to-have)
  - Show a permissions matrix across all features:
    | Feature | <role_1> | <role_2> | ... |
    |---------|----------|----------|-----|
    | <feat>  | full     | view+create | view |

EMIT  [concept-2-experience-2-features] checkpoint phase=features_written groups=<N> features=<total> must_have=<X> nice_to_have=<Y>

STEP 4: Human review
  CHECKPOINT features
    > "Does this feature list look complete? Add, rename, remove, or reprioritize anything before I continue."

STEP 5: Hand off
  - Confirm approval and present next steps:
    > "Features approved. Next steps:
    > - Run `concept-2-experience-3-screens` to specify screens
    > - Run `concept-3-blueprint-3-datamodel` to design the data schema
    > - Define brand in `_concept/1_discovery/3_brand/` and tech stack in `_concept/3_blueprint/1_techstack/`"

EMIT  [concept-2-experience-2-features] completed run_id=<uuid> feature_count=<N> groups=<N>

CHECKLIST
  - [ ] brief.md was read and exists
  - [ ] stories.json was read and exists
  - [ ] Every feature traces to at least one story
  - [ ] Every feature has valid frontmatter (status, priority, story_refs, roles, last_updated)
  - [ ] Features focus on custom logic, not PostXL-provided CRUD
  - [ ] Group folders use sequential NN_ numbering
  - [ ] Summary table shown and approved by user
