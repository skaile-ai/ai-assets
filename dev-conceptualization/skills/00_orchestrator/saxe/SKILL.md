---
name: concept
description: "Full app conceptualization orchestrator. Creates a PLANS.md, then runs the 3-phase pipeline (Discovery → Experience → Blueprint) step by step with human approval at each checkpoint. Maintains progress and decisions log throughout. Run this for end-to-end concept sessions."
keywords: product, pm, spec, requirements, screens, architecture, planning
---

ROLE  App Concept Orchestrator — guides the full conceptualization pipeline through 3 phases (Discovery, Experience, Blueprint) with durable PLANS.md and checkpoint approvals.

READS
  ? PLANS.md                            — resume from last incomplete step if exists
  ? _concept/**/*                       — any existing concept artifacts (for resume)
  ? _concept/1_discovery/1_overview/brief.md        — complexity_tier for phase control

WRITES
  PLANS.md                              — durable plan with progress, decisions, blockers
  LEARNINGS.md                          — learnings journal for improving SAXE tooling
  _concept/.snapshots/manifest.json     — approval snapshot tracking
  _concept/.snapshots/*_approved/       — per-step approval snapshots

REFERENCES
  shared/contracts/concept_structure.md   — _concept/ folder structure
  shared/contracts/pipeline.json          — dependency graph and step definitions
  shared/contracts/plans.md               — PLANS.md format and rules
  shared/contracts/snapshots.md           — how to create approval snapshots
  shared/contracts/frontmatter.md         — YAML field definitions
  shared/contracts/prerequisites.md       — tool prerequisite checks
  shared/docs/ARCHITECTURE.md             — pipeline boundaries and data flow
  references/review_modes.md              — auto-review vs default mode details
  references/plans_template.md            — PLANS.md initial template
  references/implementation_plan_template.md — implementation plan appended after concept
  references/complexity_tiers.md          — tier definitions, checkpoint consolidation rules

REQUIRES
  soft: python3 (needed for auto-review mode and skill rule validation)

MUST  run prerequisites check from shared/contracts/prerequisites.md before any work
MUST  follow the Checkpoint Protocol after every phase
MUST  create snapshots after every approval (human or auto)
MUST  update PLANS.md progress and decisions at every checkpoint
MUST  execute sub-skills completely — never partial runs
MUST  log learnings to LEARNINGS.md at every checkpoint — what worked, what didn't, what should improve
MUST  run skill rule validation after each sub-skill completes (before human approval)
MUST  persist validation results in _concept/.snapshots/manifest.json
NEVER  skip phases or continue without approval
NEVER  proceed to Phase 3 (Blueprint) until Phase 2 (Experience) is approved

EMIT  [concept] started run_id=<uuid> plan=new|resumed

# ── Reusable Procedures ─────────────────────────────────────────────

PROCEDURE log_learnings
  - After each checkpoint, reflect on what happened during this step
  - Append learnings to LEARNINGS.md under the appropriate category
  - Categories (use ## headings):
    - **Skills & Subskills** — did the skill instructions work well? Missing steps? Confusing flow? Unnecessary steps?
    - **pxl CLI** — did pxl commands work as expected? Missing features? Unclear errors?
    - **PostXL Generator & Schema** — did the schema format support what was needed? Generator gaps?
    - **Generated App Quality** — structure, patterns, or quality issues in the generated PostXL app
    - **UI Component Library** — missing components? Unclear API? Layout issues?
    - **Other** — anything else noteworthy
  - Format each entry as: `- [YYYY-MM-DD] [<skill-name>] <learning>`
  - Focus on actionable insights, not status updates
  - It is OK to have no learnings for a given checkpoint — only log genuine observations

PROCEDURE validate_skill_rules <sub-skill-name>
  - Run: $ python3 skills/shared/scripts/validate_skill_rules.py --skill <sub-skill-name> --cwd .
  - Capture exit code and output:
    - Exit 0 (PASS): record {"verdict": "pass", "rules_checked": N, "violations": 0}
    - Exit 2 (FAIL): record {"verdict": "fail", "violations": [...violation strings...]}
      - Show violations to user BEFORE asking for approval
      - Fix violations before proceeding
      - Re-run validation after fixes to confirm PASS
    - Exit 1 (ERROR): record {"verdict": "error", "reason": "..."}
      - Log warning but do not block — validation infrastructure issue, not a rule violation
  - If python3 is not available, record {"verdict": "skipped", "reason": "python3 not available"}

PROCEDURE snapshot <sub-skill-name>
  - Copy step folder to _concept/.snapshots/<step>_approved/
  - Update _concept/.snapshots/manifest.json — include validation result:
    {
      "step": "<step-path>",
      "approved_at": "YYYY-MM-DD",
      "folder": "<snapshot-folder>",
      "validation": { "skill": "<sub-skill-name>", "verdict": "pass|fail|error|skipped", ... }
    }
  - Update PLANS.md: `- [x] <step> — approved YYYY-MM-DD`
  - Log any decisions made during the step

PROCEDURE checkpoint <sub-skill-name>
  # ── Step 1: Validate sub-skill rules ──
  DO validate_skill_rules <sub-skill-name>
  IF validation verdict is FAIL
    - Show violations to user
    - Fix all violations
    - Re-run validation until PASS
  # ── Step 2: Approval ──
  IF auto-review mode is active
    - $ python3 shared/scripts/lint_concept.py _concept
    - Run `concept-review --garden`
    - Read score from _concept/quality.json
    IF score >= 70 AND 0 critical/high issues
      - Auto-approve; log: `- [x] <step> — auto-approved YYYY-MM-DD (score: NN, 0 blocking issues)`
    ELSE
      - Show health report to user
      - Log: `- [ ] <step> — auto-review escalated YYYY-MM-DD (score: NN, N issues)`
      - > "Auto-review found issues. Please review and approve or tell me what to fix."
      - Wait for human response
  ELSE
    - Show the user what was produced (include validation result summary)
    - > "Approve to continue, or tell me what to change."
    - Wait for explicit human approval
  # ── Step 3: Persist ──
  DO snapshot <sub-skill-name>
  DO log_learnings

# ── Workflow ─────────────────────────────────────────────────────────

STEP 0: Initialize plan
  IF PLANS.md exists
    - Read it; resume from last incomplete step
    - Read complexity_tier from _concept/1_discovery/1_overview/brief.md (default: standard)
    - Show progress checkboxes to user
    - > "Found an existing plan. Resuming from [next incomplete step]."
  ELSE
    - Ask the user for the app name
    - Create PLANS.md from references/plans_template.md

  IF LEARNINGS.md does not exist
    - Create LEARNINGS.md with category headings:
      ```
      # Learnings

      Observations collected during concept and implementation runs.
      Used to improve: skills, pxl CLI, PostXL generators, app quality, UI components.

      ## Skills & Subskills

      ## pxl CLI

      ## PostXL Generator & Schema

      ## Generated App Quality

      ## UI Component Library

      ## Other
      ```

# ═══════════════════════════════════════════════════════════════════════
# PHASE 1 — DISCOVERY
# Steps: 1 (brief), 2 (research), 3 (brand)
# Goal: Understand the idea, research the domain, establish identity
# ═══════════════════════════════════════════════════════════════════════

STEP 1: Project brief
  - Run sub-skill `concept-1-discovery-1-overview` completely → 1_discovery/1_overview/
  - Read complexity_tier from produced _concept/1_discovery/1_overview/brief.md
  - Log complexity_tier in PLANS.md scope section

# ── Tier-based Phase 1 pipeline ──────────────────────────────────────
# See references/complexity_tiers.md for full behavior matrix.

IF complexity_tier is small
  # ── Small: Phase 1 consolidates brief + brand (~1 checkpoint) ──

  STEP 2: Research
    - Skip: Update PLANS.md: `- [x] 1_discovery/2_research — skipped (small tier)`
    - Log decision: `Skipped research — small tier, user knows the domain`

  STEP 3: Brand
    - Run sub-skill `concept-1-discovery-3-brand` completely → 1_discovery/3_brand/
    DO checkpoint concept-1-discovery-3-brand (Phase 1 — Discovery)
      > "Here's your project brief and brand identity. Does this capture your vision? Approve or tell me what to change."

IF complexity_tier is standard
  # ── Standard: Phase 1 consolidates brief + optional research + brand (~1 checkpoint) ──

  STEP 2: Research (optional)
    - > "Would you like me to research the domain — competitors, target audience, design inspiration? Or skip and move to brand?"
    IF user wants research
      - Run sub-skill `concept-1-discovery-2-research` completely → 1_discovery/2_research/
    ELSE
      - Update PLANS.md: `- [x] 1_discovery/2_research — skipped (user chose not to)`
      - Log decision: `Skipped research — user knows the domain well`

  STEP 3: Brand
    - Run sub-skill `concept-1-discovery-3-brand` completely → 1_discovery/3_brand/
    DO checkpoint concept-1-discovery-3-brand (Phase 1 — Discovery)
      > "Discovery phase complete — project brief, research (if run), and brand identity are ready. Approve or tell me what to change."

IF complexity_tier is complex
  # ── Complex: Each Discovery step gets its own checkpoint ──

  DO checkpoint concept-1-discovery-1-overview (Step 1 — Brief)
    > "Project brief written. Does this capture your vision? Approve or tell me what to change."

  STEP 2: Research (optional)
    - > "Would you like me to research the domain — competitors, target audience, design inspiration, layout patterns? Or skip and move to brand?"
    IF user wants research
      - Run sub-skill `concept-1-discovery-2-research` completely → 1_discovery/2_research/
      DO checkpoint concept-1-discovery-2-research (Step 2 — Research)
        > "Research complete. Approve or tell me what to change."
    ELSE
      - Update PLANS.md: `- [x] 1_discovery/2_research — skipped (user chose not to)`
      - Log decision: `Skipped research — user knows the domain well`

  STEP 3: Brand
    - Run sub-skill `concept-1-discovery-3-brand` completely → 1_discovery/3_brand/
    DO checkpoint concept-1-discovery-3-brand (Step 3 — Brand)
      > "Brand identity complete. Does this capture the look and feel you want? Approve or tell me what to change."

# ═══════════════════════════════════════════════════════════════════════
# PHASE 2 — EXPERIENCE
# Steps: 4 (journeys), 5 (features), 6 (screens), then Storybook
# Goal: Design what users do and see — and make it visual
# ═══════════════════════════════════════════════════════════════════════

IF complexity_tier is small
  # ── Small: Phase 2 consolidates journeys + features + screens + storybook (~1 checkpoint) ──

  STEP 4: User journeys
    - Run sub-skill `concept-2-experience-1-journeys` completely → 2_experience/1_journeys/

  STEP 5: Features (derived from journeys)
    - Run sub-skill `concept-2-experience-2-features` completely → 2_experience/2_features/
    - Pass _concept/2_experience/1_journeys/stories.json as context so features derive from journeys

  STEP 6: Screens + Storybook
    - Run sub-skill `concept-2-experience-3-screens` completely → 2_experience/3_screens/
    - Run sub-skill `concept-2-experience-4-storybook` completely → _concept/2_experience/4_storybook/
    DO checkpoint concept-2-experience-4-storybook (Phase 2 — Experience)
      > "Experience design complete:
      > - User journeys mapped (hero + vital + hygiene flows)
      > - Features derived from journeys
      > - Screen specs written
      > - **Storybook ready** — run `cd _concept/2_experience/4_storybook && pnpm run storybook dev` to explore:
      >   - Building blocks (components with your brand tokens)
      >   - Screen compositions (all pages with data)
      >   - Clickable user journeys (walk through each flow)
      >
      > Approve or tell me what to change."

IF complexity_tier is standard
  # ── Standard: Journeys checkpoint, then features + screens + storybook consolidated (~2 checkpoints) ──

  STEP 4: User journeys
    - Run sub-skill `concept-2-experience-1-journeys` completely → 2_experience/1_journeys/
    DO checkpoint concept-2-experience-1-journeys (Step 4 — Journeys)
      > "User journeys defined. These map out how people will use your app. Approve or tell me what to change."

  STEP 5: Features (derived from journeys)
    - Run sub-skill `concept-2-experience-2-features` completely → 2_experience/2_features/
    - Pass _concept/2_experience/1_journeys/stories.json as context so features derive from journeys

  STEP 6: Screens + Storybook
    - Run sub-skill `concept-2-experience-3-screens` completely → 2_experience/3_screens/
    - Run sub-skill `concept-2-experience-4-storybook` completely → _concept/2_experience/4_storybook/
    DO checkpoint concept-2-experience-4-storybook (Phase 2 — Features + Screens + Storybook)
      > "Experience design complete:
      > - Features derived from journeys
      > - Screen specs written
      > - **Storybook ready** — run `cd _concept/2_experience/4_storybook && pnpm run storybook dev` to explore:
      >   - Building blocks (components), Screens (pages), User journeys (clickable flows)
      >
      > Approve or tell me what to change."

IF complexity_tier is complex
  # ── Complex: Each Experience step gets its own checkpoint ──

  STEP 4: User journeys
    - Run sub-skill `concept-2-experience-1-journeys` completely → 2_experience/1_journeys/
    DO checkpoint concept-2-experience-1-journeys (Step 4 — Journeys)
      > "User journeys defined. These map out how people will use your app. Approve or tell me what to change."

  STEP 5: Features (derived from journeys)
    - Run sub-skill `concept-2-experience-2-features` completely → 2_experience/2_features/
    - Pass _concept/2_experience/1_journeys/stories.json as context so features derive from journeys
    DO checkpoint concept-2-experience-2-features (Step 5 — Features)
      > "Feature list complete, derived from your approved journeys. Does this cover everything? Approve or tell me what to change."

  STEP 6: Screens + Storybook
    - Run sub-skill `concept-2-experience-3-screens` completely → 2_experience/3_screens/
    DO checkpoint concept-2-experience-3-screens (Step 6 — Screens)
      > "Screen specs complete — here's what your users will see. Approve or tell me what to change."
    - Run sub-skill `concept-2-experience-4-storybook` completely → _concept/2_experience/4_storybook/
    DO checkpoint concept-2-experience-4-storybook (Storybook)
      > "Storybook generated with 3 layers:
      > - Building blocks (components with brand tokens)
      > - Screen compositions (all pages with data)
      > - Clickable user journeys (walk through each flow as each persona)
      >
      > Run `cd _concept/2_experience/4_storybook && pnpm run storybook dev` to explore. Approve or tell me what to adjust."

# ═══════════════════════════════════════════════════════════════════════
# PHASE 3 — BLUEPRINT
# Steps: 7 (tech stack), 8 (architecture), 9 (data model)
# Goal: Define the technical foundation
# ═══════════════════════════════════════════════════════════════════════

IF complexity_tier is small
  # ── Small: Phase 3 consolidates all tech (~1 checkpoint) ──

  STEP 7: Tech stack
    - Run sub-skill `concept-3-blueprint-1-techstack` in automatic mode → 3_blueprint/1_techstack/

  STEP 8: Architecture
    - Skip: Update PLANS.md: `- [x] 3_blueprint/2_architecture — skipped (small tier)`
    - Log decision: `Skipped architecture — small tier, standard PostXL setup`

  STEP 9: Data model
    - Run sub-skill `concept-3-blueprint-3-datamodel` in automatic mode → 3_blueprint/3_datamodel/

  STEP 9b: Storybook type integration (conditional)
    IF _concept/2_experience/4_storybook/ exists
      - Run sub-skill `concept-3-blueprint-4-storybook-types` → updates 2_experience/4_storybook/src/@types/
    ELSE
      - Update PLANS.md: `- [x] 3_blueprint/4_storybook_types — skipped (no storybook)`

    DO checkpoint concept-3-blueprint-3-datamodel (Phase 3 — Blueprint)
      > "I've designed the technical foundation for your app:
      > - Your app tracks: [entities in plain language]
      > - Stack: PostXL (proven production stack)
      > [IF storybook types updated] - Storybook now uses real schema types (all compiling)
      >
      > Approve, or ask me to change anything."
    - Create `_concept/.snapshots/full_concept_approved/` snapshot

IF complexity_tier is standard
  # ── Standard: Phase 3 consolidates all tech (~1 checkpoint) ──

  STEP 7: Tech stack
    - Run sub-skill `concept-3-blueprint-1-techstack` completely (with involvement choice) → 3_blueprint/1_techstack/

  STEP 8: Architecture (optional)
    - Run sub-skill `concept-3-blueprint-2-architecture` completely (with involvement choice) → 3_blueprint/2_architecture/

  STEP 9: Data model
    - Run sub-skill `concept-3-blueprint-3-datamodel` completely (with involvement choice) → 3_blueprint/3_datamodel/

  STEP 9b: Storybook type integration (conditional)
    IF _concept/2_experience/4_storybook/ exists
      - Run sub-skill `concept-3-blueprint-4-storybook-types` → updates 2_experience/4_storybook/src/@types/
    ELSE
      - Update PLANS.md: `- [x] 3_blueprint/4_storybook_types — skipped (no storybook)`

    DO checkpoint concept-3-blueprint-3-datamodel (Phase 3 — Blueprint)
      > "Technical design complete:
      > - Your app tracks [entities in plain language]
      > - Architecture: [summary of capabilities]
      > - Stack: PostXL with [integrations if any]
      > [IF storybook types updated] - Storybook now uses real schema types (all compiling)
      >
      > Approve the technical design, or tell me what to change."
    - Create `_concept/.snapshots/full_concept_approved/` snapshot

IF complexity_tier is complex
  # ── Complex: Each Blueprint step gets its own checkpoint ──

  STEP 7: Tech stack
    - Run sub-skill `concept-3-blueprint-1-techstack` completely (with involvement choice — recommend involved) → 3_blueprint/1_techstack/
    DO checkpoint concept-3-blueprint-1-techstack (Step 7 — Tech stack)
      > "Tech stack defined. Approve or tell me what to change."

  STEP 8: Architecture
    - Run sub-skill `concept-3-blueprint-2-architecture` completely (with involvement choice — recommend involved) → 3_blueprint/2_architecture/
    DO checkpoint concept-3-blueprint-2-architecture (Step 8 — Architecture)
      > "Architecture designed. Approve or tell me what to change."

  STEP 9: Data model
    - Run sub-skill `concept-3-blueprint-3-datamodel` completely (with involvement choice — recommend involved) → 3_blueprint/3_datamodel/
    DO checkpoint concept-3-blueprint-3-datamodel (Step 9 — Data model)
      > "Data model designed. Approve or tell me what to change."

  STEP 9b: Storybook type integration (conditional)
    IF _concept/2_experience/4_storybook/ exists
      - Run sub-skill `concept-3-blueprint-4-storybook-types` → updates 2_experience/4_storybook/src/@types/
      DO checkpoint concept-3-blueprint-4-storybook-types (Step 9b — Storybook types)
        > "Storybook types replaced with schema-generated types. All components and stories compile. Approve or tell me what to change."
    ELSE
      - Update PLANS.md: `- [x] 3_blueprint/4_storybook_types — skipped (no storybook)`

    - Create `_concept/.snapshots/full_concept_approved/` snapshot

# ── Concept Complete (all tiers) ─────────────────────────────────────

STEP 10: Concept complete — generate implementation plan
  - Append implementation plan to PLANS.md using references/implementation_plan_template.md
  - List must-have features from _concept/2_experience/2_features/ with their screens
  - Count models from postxl-schema.json, screens from 2_experience/3_screens/
  - > "Concept pipeline complete! Your app's design is ready."
  - > "Recommended next steps:"
  - > "1. Run `concept-review` to audit for gaps"
  - > "2. Run `app-design` to generate mockups"
  - > "3. Run `implement` to build the app"

EMIT  [concept] completed run_id=<uuid> concept_steps_completed=10 implementation_tasks_created=N plan=PLANS.md complexity_tier=<tier>

CHECKLIST
  - [ ] PLANS.md exists with all progress checkboxes checked
  - [ ] All step snapshots exist in _concept/.snapshots/
  - [ ] _concept/.snapshots/manifest.json is up to date with validation results per step
  - [ ] Every sub-skill has a validation entry in manifest.json (pass, fail, error, or skipped)
  - [ ] Implementation plan section appended to PLANS.md
  - [ ] Every decision logged in PLANS.md Decisions section
