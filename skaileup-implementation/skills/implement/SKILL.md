---
name: "implement"
description: "Full app implementation orchestrator. Reads the completed _concept/ and drives the entire pipeline from project scaffold through feature implementation to verified, deployable application. Creates the implementation plan, manages checkpoints, git workflow, and progress tracking. Journey-first feature strategy (hero → vital → hygiene from stories.json). Supports resume from any interrupted phase."
metadata:
  version: "1.0.0"
  tags:
    - "implement"
    - "orchestrate"
    - "build"
    - "app"
    - "pipeline"
    - "plan"
    - "scaffold"
    - "foundation"
    - "features"
    - "evaluate"
    - "journey"
  source: "MERGED"
  prerequisites:
    files:
      - path: "_concept/1_discovery/1_overview/brief.md"
        gate: hard
        description: "Project brief required for app name and implementation scope"
      - path: "_concept/2_experience/2_features"
        gate: hard
        description: "Features required — implementation builds features in journey order"
        min_entries: 1
      - path: "_concept/3_blueprint/1_techstack/stack.md"
        gate: hard
        description: "Tech stack required for scaffold, foundation, and feature recipes"
      - path: "_concept/3_blueprint/3_datamodel/model.json"
        gate: hard
        description: "Data model required for schema generation and feature implementation"
      - path: "_concept/2_experience/3_screens"
        gate: hard
        description: "Screen specs required for feature page implementation"
        min_entries: 1
      - path: "_concept/eval-concept.json"
        gate: hard
        description: "Concept must pass eval-concept before implementation begins — run skaileup-conceptualization orchestrator first"
    inputs_optional:
      - id: complexity_tier
        label: "Project complexity (controls checkpoint frequency and testing depth)"
        type: select
        options:
          - small
          - standard
          - complex
        default: standard
        hint: "small = consolidated setup, standard = separate scaffold+foundation, complex = all checkpoints separate"
    reads:
      - path: "_concept/2_experience/1_journeys/stories.json"
        description: "Journey stages for feature implementation ordering (hero → vital → hygiene)"
    produces:
      - path: "_implementation/PLANS.md"
        description: "Structured implementation plan with dependency-ordered task list"
      - path: "_implementation/progress.json"
        description: "Feature implementation progress tracking"
      - path: "_implementation/decisions.md"
        description: "Implementation decisions log"
      - path: "_implementation/LEARNINGS.md"
        description: "Patterns and gotchas discovered during implementation"
  user_inputs:
    dialog:
      - id: "complexity_tier"
        label: "Project complexity (controls checkpoint frequency and testing depth)"
        type: "select"
        options:
          - "small"
          - "standard"
          - "complex"
        required: false
        default: "standard"
        hint: "small = consolidated setup, standard = separate scaffold+foundation, complex = all checkpoints separate"
    files: []
---

# Implement — Full App Implementation Orchestrator

## Overview

Drives a completed `_concept/` through scaffold, foundation, features, and
verification. Creates a structured implementation plan (dependency-ordered task
list), then executes it phase by phase with checkpoints at each milestone.

**Journey-first strategy:** features are built in user-journey order (hero →
vital → hygiene stages from `stories.json`), not feature-group-number order.
This delivers a working end-to-end flow early and reduces integration surprises.

**Complexity tiers** control checkpoint granularity:
- **small** — consolidated setup (scaffold + startup + foundation in one checkpoint)
- **standard** — scaffold+startup together, foundation separate
- **complex** — every phase has its own checkpoint

## When to Use

- All concept phases are complete (features, datamodel, screens, techstack)
- Ready to transition from concept design to code implementation
- Resuming an interrupted implementation session

## When NOT to Use

- Concept phases are incomplete — finish the pipeline first
- Adding a single feature to an already-built app — use `add-feature` + `implement-feature`
- Auditing existing code — use `audit`

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md`,
`skaileup-shared/contracts/plans.md`, `skaileup-shared/contracts/iron_laws.md`, and
`skaileup-shared/contracts/golden_principles.md` before starting.

**Hard gates (must exist):**
- `_concept/1_discovery/1_overview/brief.md`
- `_concept/2_experience/2_features/` (at least one feature group)
- `_concept/3_blueprint/1_techstack/stack.md`
- `_concept/3_blueprint/3_datamodel/model.json`
- `_concept/2_experience/3_screens/` (at least one screen)
- `_concept/eval-concept.json` (concept must pass eval-concept — run skaileup-conceptualization orchestrator first)

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/1_discovery/1_overview/brief.md` | Yes |
| Must read | `_concept/2_experience/2_features/**/*.md` | Yes |
| Must read | `_concept/3_blueprint/1_techstack/stack.md` | Yes |
| Must read | `_concept/3_blueprint/3_datamodel/model.json` | Yes |
| Must read | `_concept/3_blueprint/3_datamodel/model.dbml` | Yes |
| Must read | `_concept/2_experience/3_screens/**/*.md` | Yes |
| Read if exists | `_concept/2_experience/1_journeys/stories.json` | Recommended |
| Read if exists | `_concept/3_blueprint/2_architecture/architecture.md` | Recommended |
| Read if exists | `_concept/1_discovery/2_brand/tokens.json` | Recommended |
| Read if exists | `_concept/3_blueprint/3_datamodel/seed.json` | Recommended |
| Resume state | `_implementation/PLANS.md` | If resuming |
| Resume state | `_implementation/progress.json` | If resuming |

---

ROLE  Implementation Orchestrator — drives a completed _concept/ through scaffold, foundation, features, and verification.

READS
  _concept/1_discovery/1_overview/brief.md                    — app name, slug, description, complexity tier
  _concept/2_experience/2_features/**/*.md                    — feature list, priorities, story_refs
  _concept/3_blueprint/3_datamodel/model.json                 — data model for dependency graph
  _concept/3_blueprint/3_datamodel/model.dbml                 — human-readable data model
  _concept/2_experience/3_screens/**/*.md                     — screen specs for plan tasks
  ? _concept/2_experience/1_journeys/stories.json             — journey order (hero/vital/hygiene)
  ? _concept/3_blueprint/2_architecture/architecture.md       — custom modules, processes
  ? _concept/1_discovery/2_brand/tokens.json                  — brand tokens (for foundation)
  ? _concept/3_blueprint/3_datamodel/seed.json                — seed scenarios
  ? _implementation/PLANS.md                                  — resume state (if exists)
  ? _implementation/progress.json                             — feature status (if resuming)

WRITES
  _implementation/PLANS.md                — durable implementation plan (phases + feature checkboxes)
  _implementation/progress.json           — machine-readable feature status
  _implementation/decisions.md            — dated implementation decisions
  LEARNINGS.md                            — learnings journal (append)

REFERENCES
  skaileup-shared/contracts/concept_structure.md    — canonical _concept/ paths
  skaileup-shared/contracts/plans.md               — PLANS.md format
  skaileup-shared/contracts/iron_laws.md           — non-negotiable constraints
  skaileup-shared/contracts/golden_principles.md   — entity naming and structure rules
  references/output_templates.md     — plan presentation, completion messages
  references/procedures.md           — log_learnings, update_progress, eval_feature_gate

MUST  create or resume PLANS.md before any work
MUST  follow phase order — no skipping phases
MUST  update PLANS.md at every checkpoint
MUST  use git branching (feature branches → squash-merge to implement/<slug>)
MUST  emit observability events at every transition
MUST  log significant decisions in _implementation/decisions.md
MUST  get user approval for: plan, foundation, each feature group, final verification
NEVER skip feature group approval checkpoints
NEVER implement features out of journey order without user consent
NEVER modify _concept/ files — concept is read-only
NEVER force-push or rewrite git history
NEVER continue after failed eval-code or eval-product without fixing issues or explicit user acceptance

EMIT [implement] started run_id=<uuid> app=<app-name> features=<count>

# ── Phase 0: Initialize or Resume ─────────────────────────────────

STEP 1: Check for existing plan
  IF _implementation/PLANS.md exists
    - Read PLANS.md and progress.json
    - Identify last incomplete phase
    - Report status to user (see references/output_templates.md resume template)
    - Resume from that phase
  ELSE
    - Continue to STEP 2

  IF LEARNINGS.md does not exist
    - Create LEARNINGS.md with category headings:
      - **Skills & Subskills** — workflow gaps, confusing steps, missing procedures
      - **Tech Stack** — stack-specific surprises, profile gaps, ORM issues
      - **Generated Code** — quality issues, patterns that worked well
      - **Concept-to-Code** — spec gaps, translation issues
      - **Testing** — E2E patterns, seed data, TDD guard observations
      - **Other** — anything else noteworthy
    - Format: `- [YYYY-MM-DD] [<skill-name>] <learning>`

STEP 2: Read concept
  - Read brief.md for app name, slug, complexity_tier (use user_input if not in brief)
  - Read features/ to enumerate all must-have features and their story_refs
  - Read model.json to build entity dependency graph
  - Read screens/ for screen specs
  IF stories.json exists
    - Read journey order: hero → vital → hygiene (skip backlog)
    - Map each journey to its pages and features (via candidate_screens → screen specs → features.screens[])
  ELSE
    - Fall back to feature group numeric order
  IF architecture.md exists
    - Extract custom_modules[], apps[], external_integrations[], protocols[]

STEP 3: Search for expert skills
  - Read stack.md → tech_stack_skill field
  - Search dev-implementation-experts-* for skills matching the stack
  - Note available experts — referenced in individual task instructions

STEP 4: Build implementation plan
  - Analyze model.json relationships → determine entity insert order (parents before children)
  - Determine feature implementation order (journey-first if stories.json exists, else numeric)
  - For each feature (in order): create task entry with file list, entities, screens, expert skills
  - Prepend infrastructure tasks: migrations, seed, auth, brand/theme, app shell layout
  IF architecture.md has custom_modules beyond standard
    - Add infrastructure sub-phase (between foundation and features)
  - Write _implementation/PLANS.md (see skaileup-shared/contracts/plans.md format)
  - Write _implementation/progress.json with all features in pending status
  - Write _implementation/decisions.md with header
  - Log decision: "Complexity tier: <tier> — controls checkpoint frequency and testing depth"

CHECKPOINT plan_approval
  > "Here's the plan to build your app:
  > 1. Set up the project structure and dependencies
  > 2. Apply your brand, configure auth, create the app shell
  > [If infrastructure]: 3. Set up custom backend capabilities
  > 3. Build each feature in journey order: [list journeys / feature groups]
  > 4. You test the app against your requirements (UAT)
  > 5. Final testing and verification
  >
  > Features: N must-have across M journeys/groups
  > Expert skills available: [list]
  >
  > Approve the plan, or tell me what to modify."
  DO log_learnings

# ── Phase 1: Setup (complexity-tier based) ────────────────────────

IF complexity_tier is small
  STEP 3+4+5: Setup (consolidated)
    - RUN scaffold sub-skill (creates project, initializes git, _implementation/ tracking)
    - Verify project builds
    - Start dev server — verify app loads (use browser skill if available)
    - RUN foundation sub-skill (brand tokens, auth, app shell)
    - DO update_progress

    CHECKPOINT setup
      > "Your app is set up, running, and branded.
      > Approve to start building features."
      DO log_learnings

  STEP 5b: Infrastructure
    - Skip for small tier — no custom backend by definition
    - Set infrastructure phase to skipped in progress.json

IF complexity_tier is standard
  STEP 3+4: Scaffold and Startup (consolidated)
    - RUN scaffold sub-skill
    - Verify project builds
    - Start dev server — verify app loads
    - DO update_progress

    CHECKPOINT scaffold_startup
      > "Your project is scaffolded and running.
      > Approve to continue with foundation."

  STEP 5: Foundation
    - RUN foundation sub-skill
    - DO update_progress

    CHECKPOINT foundation
      > "Your app now has your brand's look and feel — colors, fonts, and layout are applied.
      > Approve to continue."
      DO log_learnings

  STEP 5b: Infrastructure (conditional)
    IF architecture.md exists AND custom_modules or non-standard apps present
      - RUN infrastructure sub-skill
      - Verify all custom modules build
      - Verify additional processes start
      - DO update_progress
      CHECKPOINT infrastructure
        > "The custom backend is ready. Your app can now [list capabilities].
        > Approve to continue."
    ELSE
      - Skip: no custom infrastructure needed
      - Set infrastructure phase to skipped in progress.json

IF complexity_tier is complex
  STEP 3: Scaffold
    - RUN scaffold sub-skill
    - Verify project builds
    - DO update_progress

    CHECKPOINT scaffold
      > "The project foundation is ready — structure set up and building successfully.
      > Approve to continue."

  STEP 4: Startup
    - Run environment setup (if .env files missing)
    - Start all services (database, backend, frontend)
    - Verify app loads in browser
    - DO update_progress

    CHECKPOINT startup
      > "Your app is running and accessible in the browser.
      > Approve to continue."

  STEP 5: Foundation
    - RUN foundation sub-skill
    - DO update_progress

    CHECKPOINT foundation
      > "Your app now has your brand's look and feel.
      > Approve to continue."
      DO log_learnings

  STEP 5b: Infrastructure (conditional)
    IF architecture.md exists AND custom_modules or non-standard apps present
      - RUN infrastructure sub-skill
      - Verify all modules build and processes start
      - DO update_progress
      CHECKPOINT infrastructure
        > "Custom backend ready. Modules: N, Processes: M.
        > Approve to continue."
    ELSE
      - Skip
      - Set infrastructure phase to skipped in progress.json

EMIT [implement] infrastructure_complete modules=<N> processes=<M>

# ── Phase 2: Features (journey-first) ─────────────────────────────

STEP 6: Implement feature groups / journeys
  - Process journeys in stage order: hero → vital → hygiene (if stories.json exists)
  - OR process feature groups in numeric order (if no stories.json)
  - For each journey/group:
    - Announce: name, page/feature list in business terms
    - RUN implement-feature sub-skill with journey context
      - Journey-first: implement-feature receives the full journey (pages + features)
      - Single-feature fallback: implement-feature can also receive one feature_id
    - On completion: page tests + all feature tests pass
    - Run ALL E2E tests to catch regressions
    - CALL eval_feature_gate(feature_group=<group-name>, app_url=<dev-server-url>)
    - DO update_progress
    EMIT [implement] feature_complete feature=<group>/<feature> tests=<count>

    CHECKPOINT feature_group
      > "Journey/group '<name>' is complete. Users can now:
      > [list what users can do in plain language]
      > All automated tests: passing.
      >
      > Approve to continue to the next journey/group."
      DO log_learnings

    IF browser skill is available
      > "Would you like a quick preview of what we just built?"
      IF user wants preview
        - Navigate to journey/group's primary screens
        - Show core user flows
        - Ask for feedback

STEP 7: Repeat for all journeys/groups
  UNTIL all implemented

# ── Phase 3: User Acceptance Testing ──────────────────────────────

STEP 8: UAT
  - Identify key user journeys from stories.json or feature hero flows
  - For each journey, describe what to test in plain language:
    > "Try [action] and see if [expected result]."
  - Walk through journeys using browser skill (navigate, screenshot, show user)
  - Record: pass/fail, user comments
  OUTPUT _implementation/uat-report.json
    { journeys: [...], overall: "pass|fail", iteration: 1 }

  IF any journey fails or needs changes
    - Collect all feedback
    - Implement fixes (re-enter Phase 2 for affected features)
    - Re-run UAT for fixed journeys
    UNTIL all journeys pass or user accepts remaining issues

  DO update_progress

  CHECKPOINT uat_approval
    > "You've tested all the key things your app should do:
    > [List journeys with pass/fail results]
    >
    > Approve to proceed to final verification, or identify additional issues."
  DO log_learnings
  EMIT [implement] uat_complete journeys=N passed=P failed=F

# ── Phase 4: Product Evaluation ────────────────────────────────────

STEP 9: Run eval-code (full scope) as a fresh sub-agent
  DISPATCH eval-code, scope=full
  READ _implementation/eval-code.json after completion

  IF eval-code verdict = "fail"
    - Show blocking_issues to user
    - Re-enter Phase 2 for affected features
    - Re-run eval-code after fixes

  IF eval-code verdict = "warn"
    - Show medium findings to user
    - User decides which (if any) to address before continuing

STEP 10: Run eval-product as a fresh sub-agent
  DISPATCH eval-product, app_url=<dev-server-url>
  READ _implementation/eval-product.json after completion

  IF eval-product verdict = "approved"
    - Proceed to CHECKPOINT final_verification

  IF eval-product verdict = "fail"
    - Show blocking issues (goals not achieved, performance blockers) to user
    - STOP — product requires rework; re-enter Phase 2 for affected features
    - Re-dispatch eval-product after iteration

  IF eval-product verdict = "needs_iteration"
    - Show improvement_priorities to user (ranked)
    - User decides which to address
    - Re-enter Phase 2 for selected items
    - Re-dispatch eval-product after iteration

  DO update_progress
  EMIT [implement] completed run_id=<uuid> features=<count> e2e_tests=<count>

CHECKPOINT final_verification
  Gate: eval-code verdict = pass (or warn with user acceptance) AND eval-product verdict = approved (or user explicitly accepts remaining priorities)
  User sees: eval-code verdict + eval-product verdict + all improvement priorities
  DO log_learnings
  - On approval: present completion summary (see references/output_templates.md)
  - Final commit: "chore: mark implementation complete in PLANS.md"

CHECKLIST
  - [ ] PLANS.md created or resumed before any work
  - [ ] Journey-first order followed (hero → vital → hygiene)
  - [ ] PLANS.md updated at every checkpoint
  - [ ] User approved: plan, foundation, each feature group, final verification
  - [ ] _concept/ files not modified
  - [ ] LEARNINGS.md updated at checkpoints

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Skipping the plan phase | Always create PLANS.md first — it defines the dependency order |
| Feature-group-number order instead of journey order | Use stories.json hero→vital→hygiene order when available |
| Running eval-code/eval-product before UAT | UAT comes first — user confirms app works, then eval-code and eval-product gates |
| Not logging learnings | Append to LEARNINGS.md at every checkpoint — institutional knowledge |
| Mixing feature code with progress-tracking commits | One commit per feature, separate commit for progress updates |

## Integration

- **Reads:** entire `_concept/` pipeline
- **Writes:** `_implementation/` tracking, `LEARNINGS.md`
- **Dispatches to:** `scaffold`, `foundation`, `infrastructure`, `implement-feature`, `eval-feature`, `eval-product`, `eval-code`
- **Called by:** user directly or automated pipeline
