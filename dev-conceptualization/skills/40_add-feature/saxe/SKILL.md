---
name: concept-add-feature
description: "Add or modify a feature in an existing concept. This skill should be used when the user asks to 'add a feature', 'add X to the concept', 'I want a new feature for Y', 'modify the login feature', 'change the requirements for X', 'update feature Y', or wants to extend an existing concept with new or changed functionality. Cascades changes through downstream artifacts (data model, screens, architecture) and optionally implements if the app is already built."
keywords: feature, add, modify, update, extend, concept, cascade, incremental
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

ROLE  Feature Addition agent — surgically adds or modifies features while cascading changes through all downstream artifacts.

READS
  _concept/1_discovery/1_overview/brief.md              — app purpose, audience
  _concept/2_experience/2_features/**/*.md              — all existing features (names, groups, priorities)
  ? _concept/2_experience/1_journeys/stories.json       — user journey context
  ? _concept/1_discovery/3_brand/tokens.json           — brand tokens
  ? _concept/3_blueprint/1_techstack/stack.md          — tech stack
  ? _concept/3_blueprint/2_architecture/architecture.md — architecture
  ? _concept/3_blueprint/3_datamodel/postxl-schema.json  — data model
  ? _concept/3_blueprint/3_datamodel/feature_map.json    — model-to-feature mapping
  ? _concept/3_blueprint/3_datamodel/seed.json           — seed data
  ? _concept/2_experience/3_screens/**/*.md               — screen specs
  ? _implementation/progress.json             — implementation status

WRITES
  _concept/2_experience/2_features/<NN_group>/<feature>.md   — new or updated feature spec
  _concept/2_experience/1_journeys/stories.json              — updated journey map (if new stories needed)
  _concept/3_blueprint/1_techstack/stack.md                 — updated tech stack (if new integrations)
  _concept/3_blueprint/2_architecture/architecture.md      — updated architecture (if needed)
  _concept/3_blueprint/3_datamodel/postxl-schema.json       — updated schema (if needed)
  _concept/3_blueprint/3_datamodel/feature_map.json         — updated model-feature mapping
  _concept/3_blueprint/3_datamodel/seed.json                — updated seed data
  _concept/2_experience/3_screens/<NN_group>/<screen>.md     — new or updated screen specs

REFERENCES
  shared/contracts/concept_structure.md              — valid paths, naming rules
  shared/contracts/frontmatter.md                    — required YAML fields per file type
  shared/contracts/feedback_loop.md                  — cross-reference protocol (features <-> screens <-> datamodel)
  shared/contracts/golden_principles.md              — naming conventions, required fields
  shared/contracts/semantic_types.md                 — PostXL field types
  shared/contracts/snapshots.md                      — cascade detection
  shared/contracts/pipeline.json                     — dependency graph
  ? shared/contracts/implementation_structure.md     — tracking files (if implementing)
  ? shared/contracts/acceptance_criteria.md          — AC format (if implementing)
  ? shared/contracts/git_workflow.md                 — branch conventions (if implementing)
  ? shared/contracts/verification.md                 — verification levels (if implementing)
  references/feature_spec_template.md       — frontmatter template, discovery questions, impact/cascade templates
  references/cascade_rules.md               — per-artifact cascade update details
  references/implementation_workflow.md     — TDD implementation sub-workflow and regression checks

REQUIRES
  state: _concept/1_discovery/1_overview/brief.md exists
  state: _concept/2_experience/2_features/**/*.md at least one group exists
  soft: pnpm, git (only if Phase 4 applies)
  soft: agent-browser (visual verification during implementation)

STEP 1: Read existing concept
  - Scan all of _concept/ to understand current state
  - Read brief.md, all features, and every optional artifact that exists
  - Check if _implementation/progress.json exists (determines if Phase 4 runs)

STEP 2: Understand the request
  - Ask discovery questions per references/feature_spec_template.md
  - For new features: what, success/failure, roles, priority, group placement
  - For modifications: which feature, what's changing, new entities/screens needed

STEP 3: Assess cascade impact
  - Analyze which downstream artifacts will be affected
  - Present impact assessment per references/feature_spec_template.md template
  CHECKPOINT impact_assessment
    > "Review the impact assessment. Approve to proceed, or clarify your request."

STEP 4: Write feature spec
  IF adding a new feature
    - If fits existing group, add file to that group
    - If new group needed, create next sequential numbered folder
    - Write feature file per references/feature_spec_template.md
    - Cross-check against existing features for overlap or conflicts
  ELSE (modifying existing feature)
    - Read current feature file
    - Apply requested changes (requirements, roles, priority, scope)
    - Preserve existing screens: and data_entities: arrays
    - Update last_updated to today
    - Flag which downstream artifacts may need updating
  CHECKPOINT feature_spec
    > "Here's the feature spec. Approve to proceed with cascade updates, or request changes."

STEP 5: Cascade through downstream artifacts
  - Only update artifacts that already exist — never create new pipeline steps
  - Follow references/cascade_rules.md for each artifact type:
  IF _concept/2_experience/1_journeys/stories.json exists AND feature introduces new user flows
    - Add stories to appropriate story map, update downstream links
  IF _concept/3_blueprint/1_techstack/stack.md exists AND feature needs new integrations
    - Add integration to tech stack
  IF _concept/3_blueprint/2_architecture/architecture.md exists AND feature needs new modules/protocols
    - Update architecture sections and frontmatter arrays
  IF _concept/3_blueprint/3_datamodel/postxl-schema.json exists AND feature needs data changes
    - Update schema, feature_map.json, seed.json
    - $ pxl validate _concept/3_blueprint/3_datamodel/postxl-schema.json
    - Feedback loop: update feature's data_entities: array
  IF _concept/2_experience/3_screens/ exists
    - Add new or update existing screen specs
    - Check shell.md for navigation updates
    - Feedback loop: update feature's screens: array and screen implements: arrays
  CHECKPOINT cascade
    > "Review the cascade changes. Approve to continue, or request adjustments."

STEP 6: Quality gate
  - $ python3 shared/scripts/lint_concept.py _concept
  - Verify all cross-references are bidirectional and valid
  IF _concept/.snapshots/ exists
    - Create new snapshots for modified steps
    - Update manifest.json
  - Report quality status; fix issues before proceeding

STEP 7: Implementation (conditional)
  IF _implementation/progress.json exists
    - Follow references/implementation_workflow.md
    IF postxl-schema.json was modified in Step 5
      - Re-generate code, run migration, Level 1 verification
      - $ pnpm run generate
      - $ pnpm prisma migrate dev --name add_<feature_slug>
      - $ pnpm run build && pnpm run lint && pnpm run test:types
    - Create feature branch, implement via TDD (ACs -> E2E tests -> stories -> code)
    UNTIL all E2E tests pass and build succeeds
    CHECKPOINT implementation
      > "Feature implemented. Review verification results. Approve to merge."
    - Merge feature branch after approval
    - $ pnpm run e2e
    - Report regression status

EMIT [concept-add-feature] started run_id=<uuid> mode=add|modify feature=<group>/<feature> implementation_exists=true|false
EMIT [concept-add-feature] checkpoint phase=feature_spec mode=add|modify feature=<name> group=<NN_group>
EMIT [concept-add-feature] checkpoint phase=cascade behavior=updated|skipped|not_present datamodel=<summary> screens=<summary>
EMIT [concept-add-feature] checkpoint phase=quality lint=pass|fail cross_refs=valid|N_broken
EMIT [concept-add-feature] checkpoint phase=implementation regenerated=true|false feature_tests=N/N regression=N/N
EMIT [concept-add-feature] completed run_id=<uuid> feature=<group>/<feature> cascade_updates=N implemented=true|false

MUST  read the full existing concept before making any changes
MUST  present impact assessment before starting cascade
MUST  get user approval after feature spec AND after cascade changes
MUST  follow feedback loop protocol — all cross-references must be bidirectional
MUST  validate postxl-schema.json with pxl validate after any schema changes
MUST  ensure labelField resolves to a String-type field on every model
MUST  run regression check after implementation (Step 7)
MUST  use backend-compatible format for seed data (camelCase plural keys, snake_case fields)
NEVER make cascade changes without user approval
NEVER create pipeline steps that don't already exist (ask first)
NEVER invent colors or fonts — consume from 1_discovery/3_brand/tokens.json
NEVER skip regression check during implementation
NEVER modify existing features' functionality without explicit user request
NEVER break existing cross-references (verify integrity in Step 6)
NEVER implement before tests exist (TDD is non-negotiable)

CHECKLIST
  - [ ] Existing concept fully read and understood
  - [ ] Impact assessment presented and approved
  - [ ] Feature spec written and approved
  - [ ] All affected downstream artifacts updated (cascade)
  - [ ] Cross-references bidirectional and valid
  - [ ] postxl-schema.json validates (if modified)
  - [ ] Lint passes
  - [ ] Snapshots updated (if .snapshots/ exists)
  - [ ] Implementation complete with passing E2E tests (if applicable)
  - [ ] Regression check passed (if applicable)
