---
name: implement
description: "Full app implementation orchestrator. This skill should be used when the user asks to 'implement the app', 'build the app from concept', 'start implementation', 'continue implementation', or wants to turn a completed _concept/ into a working application. Guides the entire pipeline from project scaffold through feature implementation to verified, deployable application. Manages checkpoints, git workflow, and progress tracking."
---

ROLE  Implementation Orchestrator — drives a completed _concept/ through scaffold, foundation, features, and verification.

READS
  _concept/1_discovery/1_overview/brief.md             — app name, slug, description, complexity_tier
  _concept/2_experience/2_features/**/*.md             — feature list and priorities
  _concept/3_blueprint/3_datamodel/postxl-schema.json — data model for scaffold
  _concept/2_experience/3_screens/**/*.md              — screen specs for verification
  ? _concept/3_blueprint/2_architecture/architecture.md — custom modules, processes, integrations
  ? _implementation/PLANS.md               — resume state (if exists)
  ? _implementation/progress.json          — feature status (if exists)

WRITES
  _implementation/PLANS.md                 — durable implementation plan
  _implementation/progress.json            — machine-readable feature status
  _implementation/decisions.md             — dated implementation decisions
  LEARNINGS.md                             — learnings journal for improving SAXE tooling (append)

REFERENCES
  shared/contracts/prerequisites.md          — tool prerequisite checks
  shared/contracts/implementation_structure.md — _implementation/ layout and progress
  shared/contracts/acceptance_criteria.md    — AC format for TDD
  shared/contracts/git_workflow.md           — branch, commit, merge conventions
  shared/contracts/verification.md          — verification levels and thresholds
  shared/contracts/plans.md                 — PLANS.md format (implementation section)
  shared/contracts/concept_structure.md     — where to read concept artifacts
  concept/concept/references/complexity_tiers.md — tier definitions, checkpoint rules, testing depth
  references/auto_review.md        — feature auto-approval criteria and escalation
  references/startup_guide.md      — Phase 2 startup steps and troubleshooting
  references/decision_logging.md   — decision template and common decision points
  references/output_templates.md   — plan presentation, completion messages

REQUIRES
  hard: pnpm, git
  soft: docker (database/Keycloak — stateless dev works without)
  soft: agent-browser (visual verification — can proceed without)
  state: _concept/1_discovery/1_overview/brief.md exists with status approved
  state: _concept/2_experience/2_features/ has at least one feature group
  state: _concept/3_blueprint/3_datamodel/postxl-schema.json exists
  state: _concept/2_experience/3_screens/ has screen specs

# ── Procedures ────────────────────────────────────────────────────

PROCEDURE log_learnings
  - After each checkpoint, reflect on what happened during this phase
  - Append learnings to LEARNINGS.md under the appropriate category
  - Categories (use ## headings):
    - **Skills & Subskills** — did the skill instructions work well? Missing steps? Confusing flow?
    - **pxl CLI** — did pxl commands work as expected? Missing features? Unclear errors?
    - **PostXL Generator & Schema** — did the schema format support what was needed? Generator gaps?
    - **Generated App Quality** — structure, patterns, or quality issues in the generated PostXL app
    - **UI Component Library** — missing components? Unclear API? Layout issues?
    - **Other** — anything else noteworthy
  - Format each entry as: `- [YYYY-MM-DD] [<skill-name>] <learning>`
  - Focus on actionable insights, not status updates
  - It is OK to have no learnings for a given checkpoint — only log genuine observations

PROCEDURE update_progress
  - Update PLANS.md: check off completed phase/feature
  - Update progress.json with new status and timestamp
  - Commit: "chore: update implementation progress"

PROCEDURE feature_auto_review
  - Check all criteria in references/auto_review.md
  - IF all pass → auto-approve, log approval_method: "auto" in progress.json
  - ELSE → escalate to human with failing checks highlighted

# ── Phase 0: Initialize or Resume ────────────────────────────────

STEP 1: Check for existing plan
  IF _implementation/PLANS.md exists
    - Read PLANS.md and progress.json
    - Identify last incomplete phase
    - Report status to user (see references/output_templates.md resume template)
    - Resume from that phase
  ELSE
    - Continue to STEP 2
  IF LEARNINGS.md does not exist
    - Create LEARNINGS.md with category headings (Skills, pxl CLI, PostXL Generator, App Quality, UI Components, Other)
  EMIT [implement] started run_id=<uuid> app=<app-name> features=<count>

STEP 2: Create implementation plan
  - Read brief.md for app name, description, and complexity_tier (default: standard)
  - Read _concept/2_experience/2_features/ to build feature list
  - Filter to must-have features (priority: must-have)
  - Order feature groups by numeric prefix (01, 02, 03...)
  IF _concept/3_blueprint/2_architecture/architecture.md exists
    - Extract custom_modules[], apps[], external_integrations[] from frontmatter
    - Include infrastructure phase in plan with module/process list
  - Create _implementation/PLANS.md (scope, source artifacts, phase/feature checkboxes)
    Include phases: 1. Scaffold, 2. Startup, 3. Foundation, 3.5. Infrastructure (if arch exists), 4. Features, 5. Re-generate, 5.5. UAT, 6. Verify
    Include complexity_tier in scope section
  - Create _implementation/progress.json with all features in pending status, include complexity_tier
  - Create _implementation/decisions.md with header
  - Log decision: "Complexity tier: <tier> — controls checkpoint frequency and testing depth"

CHECKPOINT plan_approval
  > "Here's the plan to build your app:
  > 1. Set up the project structure
  > 2. Apply your brand look and feel
  > [If infrastructure]: 3. Set up custom backend capabilities
  > 3. Build each feature group: [list groups in business terms]
  > 4. You test the app against your requirements (UAT)
  > 5. Final testing and verification
  >
  > Feature groups: N, Must-have features: M
  >
  > Approve the plan, or tell me what to modify."
  DO log_learnings

# ── Phase 1: Setup (tier-based consolidation) ───────────────────────
# See concept/concept/references/complexity_tiers.md for consolidation rules.

IF complexity_tier is small
  # ── Small Tier: Consolidated Setup (one checkpoint) ────────────

  STEP 3+4+5: Setup (consolidated)
    - RUN implement-1-setup-1-scaffold (invoke sub-skill with concept context)
    - Verify project builds
    - Verify _implementation/progress.json was initialized
    - Follow references/startup_guide.md for full startup procedure
    - Run pnpm run setup if .env files missing
    - Start Docker services, backend, frontend
    - Use agent-browser to verify app loads at http://localhost:3000
    - Take screenshot
    - RUN implement-1-setup-2-foundation (invoke sub-skill with concept context)
    - DO update_progress

    CHECKPOINT setup
      > "Your app is set up, running, and branded. [Show screenshot]
      >
      > Approve to start building features."

  STEP 5b: Infrastructure
    - Skip for small tier (no custom backend by definition)
    - Set infrastructure phase to skipped in progress.json

IF complexity_tier is standard
  # ── Standard Tier: Scaffold+Startup consolidated ──────────────

  STEP 3+4: Scaffold and Startup (consolidated)
    - RUN implement-1-setup-1-scaffold (invoke sub-skill with concept context)
    - Verify project builds
    - Verify _implementation/progress.json was initialized
    - Follow references/startup_guide.md for full startup procedure
    - Run pnpm run setup if .env files missing
    - Start Docker services, backend, frontend
    - Use agent-browser to verify app loads at http://localhost:3000
    - Take screenshot
    - DO update_progress

    CHECKPOINT scaffold_startup
      > "Your project is scaffolded and running. [Show screenshot if available]
      >
      > Approve to continue with foundation."

  STEP 5: Apply foundation
    - RUN implement-1-setup-2-foundation (invoke sub-skill with concept context)
    - DO update_progress

    CHECKPOINT foundation
      > "Your app now has your brand's look and feel — colors, fonts, and layout are applied.
      > [Show screenshots if available]
      >
      > Approve to continue."

  STEP 5b: Set up infrastructure
    IF _concept/3_blueprint/2_architecture/architecture.md exists AND frontmatter contains custom_modules or apps beyond [api, web]
      - RUN implement-1-setup-3-infrastructure (invoke sub-skill with concept context)
      - Verify all custom modules build
      - Verify additional processes start
      - Verify communication infrastructure (WebSocket handshake, SSE stream if applicable)
      - DO update_progress
      - Restart all services to verify inter-process connectivity
      CHECKPOINT infrastructure
        > "The custom backend is ready. Your app can now [list capabilities in business terms].
        >
        > Approve to continue."
    ELSE
      - Skip: no custom infrastructure needed (standard PostXL only)
      - Set infrastructure phase to skipped in progress.json

IF complexity_tier is complex
  # ── Complex Tier: All checkpoints separate ─────────────────────

  STEP 3: Scaffold project
    - RUN implement-1-setup-1-scaffold (invoke sub-skill with concept context)
    - Verify project builds
    - Verify _implementation/progress.json was initialized
    - DO update_progress

    CHECKPOINT scaffold
      > "The project foundation is ready — your app's structure is set up and building successfully.
      >
      > Technical details (if interested):
      >   Models: N, Build: passed, Branch: implement/<slug>
      >
      > Approve to continue."

  STEP 4: Start and verify app stack
    - Follow references/startup_guide.md for full startup procedure
    - Run pnpm run setup if .env files missing
    - Start Docker services, backend (e2e:stateless mode for E2E), frontend
    - Use agent-browser to verify app loads at http://localhost:3000
    - Take screenshot
    - If startup fails, diagnose and fix per references/startup_guide.md troubleshooting
    - DO update_progress

    CHECKPOINT startup
      > "Your app is running and accessible in the browser. [Show screenshot if available]
      >
      > Approve to continue."

  STEP 5: Apply foundation
    - RUN implement-1-setup-2-foundation (invoke sub-skill with concept context)
    - DO update_progress

    CHECKPOINT foundation
      > "Your app now has your brand's look and feel — colors, fonts, and layout are applied.
      > [Show screenshots of shell, theme, auth if available]
      >
      > Approve to continue."

  STEP 5b: Set up infrastructure
    IF _concept/3_blueprint/2_architecture/architecture.md exists AND frontmatter contains custom_modules or apps beyond [api, web]
      - RUN implement-1-setup-3-infrastructure (invoke sub-skill with concept context)
      - Verify all custom modules build
      - Verify additional processes start
      - Verify communication infrastructure (WebSocket handshake, SSE stream if applicable)
      - DO update_progress
      - Restart all services (mini Phase 2 repeat) to verify inter-process connectivity
      CHECKPOINT infrastructure
        > "The custom backend is ready. Your app can now [list capabilities in business terms].
        >
        > Technical details (if interested):
        >   Modules: N, Processes: M, Connectivity: verified
        >
        > Approve to continue."
    ELSE
      - Skip: no custom infrastructure needed (standard PostXL only)
      - Set infrastructure phase to skipped in progress.json

EMIT [implement] infrastructure_complete modules=<N> processes=<M>

# ── Phase 2: Features (Iterative) ────────────────────────────────

STEP 6: Implement feature groups
  - Process groups in numeric order (01, 02, 03...)
  - For each group, announce: group name, feature list
  IF complexity_tier is small
    - Skip Storybook stories (log decision: "Storybook skipped — small tier")
    - Reduce seed scenarios to populated only (skip edge_cases)
    - Individual features auto-approved (group checkpoint still required)
  - For each feature in group:
    - RUN implement-2-features with <group>/<feature>
    - DO feature_auto_review
    - On approval: squash merge to implementation branch
    - DO update_progress
    EMIT [implement] feature_complete feature=<group>/<feature> tests=<count>
  - After all features in group:
    - Run Level 1 verification (build passes after all merges)
    - If build breaks: fix integration issues
    UNTIL build passes
  CHECKPOINT feature_group
    > "Feature group '<name>' is built and tested.
    > Users can now: [list what users can do with this group's features in plain language]
    > All automated tests: passing.
    >
    > Approve to continue to the next group."
    DO log_learnings

  # ── Optional: Preview/Demo after feature group ──
  IF agent-browser is available
    - > "Would you like a quick preview of what we just built? I'll walk through the features in the browser."
    IF user wants preview
      - For each feature in the completed group:
        - Navigate to the feature's primary screen
        - Demonstrate the core user action (from feature spec's hero flow or success criteria)
        - Show what the user sees at each step
        - Take screenshots
      - Save to _implementation/verification/screenshots/<group>_preview/
      - > "That's what [group name] looks like in action. Any feedback before we move on?"
      IF user has feedback
        - Log feedback in _implementation/decisions.md
        - If feedback requires code changes: create fix tasks, implement, re-verify
      EMIT [implement] preview group=<group> screenshots=<N> feedback=<yes|no>

  - If group required schema changes, RUN implement-generate to re-sync

STEP 7: Repeat for all groups
  - Continue STEP 6 for each remaining feature group
  UNTIL all feature groups are complete

# ── Phase 3: Final Re-generation ──────────────────────────────────

STEP 8: Re-generate
  - RUN implement-generate for final generation pass
  - Resolve any conflicts
  - Run Level 1 verification
  - DO update_progress
  IF no conflicts AND build passes
    - Auto-approve
  ELSE
    CHECKPOINT regeneration
      > "Re-generation produced conflicts. Review and approve."

# ── Phase 3: User Acceptance Testing ──────────────────────────────

STEP 8b: User acceptance testing
  - Identify key user journeys from _concept/2_experience/2_features/**/*.md (hero flow + must-have features)
  - For each journey:
    - Describe to the user what they should test in plain language:
      > "Try [action] and see if [expected result]."
    - Navigate through the journey using agent-browser
    - Show the user what the app does at each step (screenshots)
    - Ask: "Does this match what you expected? (pass / fail / needs changes)"
    - Record result: pass/fail, user comments, screenshots

  OUTPUT _implementation/verification/reports/uat-report.json
    {
      "journeys": [
        {
          "name": "<journey description>",
          "feature_refs": ["2_experience/2_features/<group>/<feature>.md"],
          "result": "pass | fail | needs_changes",
          "user_comments": "<feedback>",
          "screenshots": ["path/to/screenshot.png"]
        }
      ],
      "overall": "pass | fail",
      "iteration": 1
    }

  IF any journey fails or needs changes
    - Collect all feedback
    - Prioritize fixes
    - Implement fixes (re-enter Phase 4 flow for affected features)
    - Re-run UAT for fixed journeys
    UNTIL all journeys pass or user accepts remaining issues

  - DO update_progress

  CHECKPOINT uat_approval
    > "You've tested all the key things your app should do:
    > [List journeys with pass/fail results]
    >
    > Approve to proceed to final verification, or identify additional issues."

  DO log_learnings
  EMIT [implement] uat_complete run_id=<uuid> journeys=N passed=P failed=F iterations=I

# ── Phase 3: Full Verification ────────────────────────────────────

STEP 9: Verify
  - RUN implement-3-verify for complete verification gate
  - DO update_progress
  EMIT [implement] completed run_id=<uuid> features=<count> e2e_tests=<count>

CHECKPOINT final_verification
  > "Your app is fully built and verified!
  > Users can: [list top 3-5 capabilities in plain language]
  > All N features implemented and tested.
  >
  > Technical details (if interested):
  >   E2E tests: N passing, Build: passed, Verification: PASS
  >
  > Approve the final state."
  DO log_learnings
  - On approval: present completion summary (see references/output_templates.md)
  - Final commit: "chore: mark implementation complete in PLANS.md"

# ── Constraints ───────────────────────────────────────────────────

MUST  log learnings to LEARNINGS.md at every checkpoint — what worked, what didn't, what should improve
MUST  create or resume PLANS.md before any work
MUST  follow phase order — no skipping phases
MUST  update PLANS.md at every checkpoint
MUST  use git branching per shared/contracts/git_workflow.md
MUST  emit observability events at every transition
MUST  log significant decisions in _implementation/decisions.md
MUST  get user approval for plan, foundation, feature groups, and final verification
NEVER skip feature approval checkpoints
NEVER implement features out of priority order without user consent
NEVER modify _concept/ files — concept is read-only
NEVER force-push or rewrite git history
NEVER continue after failed verification without fixing issues
NEVER merge feature branches without squash
