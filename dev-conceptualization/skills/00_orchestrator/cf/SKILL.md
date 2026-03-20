---
name: concept
description: "Use when running the full concept-to-implementation pipeline, resuming an interrupted session, or when the user says 'start a new concept', 'let's build an app', 'continue where we left off'. This is the pipeline controller that also handles user communication directly."
keywords: [pipeline, orchestrate, concept, full, end-to-end, resume, plan, start]
---

# Orchestrator

## Overview

The orchestrator is the **pipeline controller**. It reads `pipeline.json`,
resolves dependencies, dispatches skills (as subagents when configured), manages
completion checks, and tracks progress in PLANS.md. It handles user communication
directly — adapting tone via the Communication Style pattern.

## When to Use
- Starting a new app concept from scratch
- Resuming an interrupted concept session (PLANS.md exists)
- User says "run the full pipeline", "start conceptualizing", "continue"
- After a standalone skill completes, to get next-step suggestions

## When NOT to Use
- Running a single skill in isolation (use that skill directly)
- Auditing or reviewing without running the pipeline (use cf_quality_review or cf_quality_audit)

## Prerequisites

**REQUIRED BACKGROUND:**
- Read `shared/contracts/pipeline.json` — the dependency graph, phases, steps, hard_gates
- Read `shared/contracts/plans.md` — PLANS.md format
- Read `shared/contracts/iron_laws.md` — non-negotiable constraints
- Read `shared/contracts/agent_patterns.md` — reusable workflow patterns

## Context Budget
**Must read:** shared/contracts/pipeline.json, shared/contracts/plans.md, _concept/PLANS.md (if exists)
**Optional:** docs/ARCHITECTURE.md
**Never load:** Individual skill SKILL.md files (subagents load their own)

## Architecture

```
┌──────────────┐
│ Orchestrator │──── user communication (direct)
│  (controller)│
└──────┬───────┘
       │ dispatches
       ▼
┌──────────────┐
│  Skill (as   │──► _concept/ artifacts
│  subagent)   │
└──────────────┘
```

The orchestrator:
1. Reads pipeline.json for the execution plan
2. Collects user inputs directly (Self-Collect Inputs pattern)
3. Dispatches skills (as subagents when `subagent: true`)
4. Runs completion checks between phases
5. Updates PLANS.md at every transition

## Review Modes

### Default Mode
After each phase, orchestrator presents results and asks for feedback.

### Auto-review Mode
Activated when user says "auto-review", "autonomous", or "run without stopping":
1. Execute phase
2. Run lint: `python3 scripts/lint_concept.py _concept`
3. Run review skill in gardening mode
4. If quality score ≥ 70 and 0 CRITICAL/HIGH → continue (advisory)
5. Else → pause and present issues to user

Log in PLANS.md:
```
- [x] features — completed 2026-03-13 (score: 85, 0 blocking)
- [ ] brand_visual — needs attention 2026-03-13 (score: 62, 2 high issues)
```

## Next-Step Suggestion (Standalone Mode)

When invoked after a standalone skill completes:
1. Read pipeline.json for all steps
2. Read _concept/ to check which artifacts exist
3. Find steps where all hard_gates are now satisfied
4. Present to user: "You can now run: /cf_concept_datamodel, /cf_concept_brand_visual"
5. Show remaining blocked steps and what they need

## Orchestration Workflow

### Phase 0: Initialize

**If PLANS.md exists:** Read it. Resume from last incomplete step.
Report progress to user.

**If PLANS.md does not exist:**
1. Collect onboarding inputs directly (Communication Style pattern):
   - App name, one-sentence description
   - **Flow** selection (from `flows/*.json`) — present the available flows from `flows/` listing
   - Research depth override (skip/light/moderate/deep) — defaults to `flow.globals.research_depth`
2. Load the selected `flows/<id>.json` — this is the execution graph
3. Extract skill nodes from `flow.nodes[]` (type: "skill"), resolve topological order via edges
4. Apply `flow.globals` as baseline parameters for all nodes
   Per-node `data.parameters` override globals (node wins on conflict)
5. Create PLANS.md:

```markdown
# Plans

## Concept Plan: <App Name>

### Settings
- Profile: <profile name> (<profile description>)
- Research depth: <depth>
- Skipped steps: <from profile.skip_steps>

### Progress
- [ ] cf_concept_overview — not started
- [ ] cf_concept_functionality_features — not started
- [ ] cf_concept_functionality_behaviors — not started (optional)
- [ ] cf_concept_brand_visual — not started
- [ ] cf_concept_brand_behavioral — not started (optional)
- [ ] techstack — not started
- [ ] cf_concept_architecture — blocked by features, techstack
- [ ] cf_concept_datamodel — blocked by features, techstack, architecture
- [ ] cf_concept_ui_screens — blocked by features, brand_visual, techstack, datamodel
- [ ] cf_concept_ui_components — not started (optional)

### Decisions
(none yet)

### Open Questions
(none yet)

### Blockers
(none yet)
```

**Profile-specific behavior:**
- **cli_app**: Skip brand, screens, mockups, E2E. Features describe CLI commands.
- **prototype**: Run concept pipeline → design skill. No implementation phases.
  After screens complete, auto-dispatch `cf_concept_mock` to generate
  linked prototype. That's the final deliverable.
- **mvp**: Standard full pipeline. Mockups optional (ask user).
- **product**: All optional steps included. Readiness gate enforced before implementation.
- **reverse_engineer**: Entry via `cf_concept_reverse_engineer` instead of `cf_concept_overview`.

```
[orchestrator] started
  run_id: <uuid>
  plan: new | resumed
  complexity: <level>
  research_depth: <depth>
```

---

### Phase 1: Overview (Conceptualization)

1. Check if research_mode is active (research_depth != "skip")
2. If active: dispatch research skill as parallel subagent scoped to domain + competitors
3. Collect overview user_inputs directly if not already in _concept/
4. Dispatch `cf_concept_overview` skill
5. Present completion summary

**On completion:**
- Update PLANS.md: `- [x] overview — completed YYYY-MM-DD`

---

### Phase 2: Specify (Conceptualization)

#### 2a: Features
1. If research_mode active: dispatch research for patterns, behavioral patterns
2. Dispatch `cf_concept_functionality_features` skill
3. Present completion summary

#### 2b: Behaviors (Optional)
If not in skip_steps:
1. Ask user: "Formalize behavioral rules from features?"
2. If yes: dispatch `cf_concept_functionality_behaviors` skill
3. If skip: log in PLANS.md

---

### Phase 3: Identity (Parallel Group)

These steps can run in parallel. Read `parallel_group: "identity"` from pipeline.json.

#### 3a: Brand Visual
1. If research_mode active: dispatch research for colors_fonts, design_inspiration
2. Collect brand user_inputs (reference_urls, mood, light/dark, fonts)
3. Dispatch `cf_concept_brand_visual` skill

#### 3b: Brand Behavioral (Optional)
If not in skip_steps:
1. Dispatch `cf_concept_brand_behavioral` skill

#### 3c: Tech Stack
1. Collect techstack user_inputs
2. Dispatch `cf_concept_techstack` skill

#### 3d: Architecture
1. Dispatch `cf_concept_architecture` skill

**With `--parallel` flag:** Run 3a, 3b, 3c concurrently. Wait for all. Then completion check each.
**Without:** Run sequentially, user chooses order.

---

### Phase 4: Model (Conceptualization)

**Hard gate:** features folder must exist.

1. Dispatch `cf_concept_datamodel` skill (subagent: true)
2. Present completion summary

---

### Phase 5: UI Planning (Conceptualization)

**Hard gate:** features, brand tokens, datamodel must exist.

#### 5a: Screens
1. Dispatch `cf_concept_ui_screens` skill
2. Present completion summary

#### 5b: Components (Optional)
If not in skip_steps:
1. Dispatch `cf_concept_ui_components` skill

---

### Phase 6: Concept Complete → Implementation Plan

After all conceptualization steps complete:

1. Dispatch `cf_implement` skill to generate implementation plan
2. Implementation plan goes into PLANS.md (see implement skill)
3. Present concept summary:

> "Concept complete. Your `_concept/` folder contains:
> - `01_project/` — brief and goals
> - `_research/` — research (if done)
> - `03_features/` — feature specs
> - `03b_behavior/` — behavioral specs (if done)
> - `04_brand/` — visual + behavioral identity
> - `05_techstack/` — technology choices
> - `06_datamodel/` — data model + seed data
> - `07_screens/` — screen specs + components
>
> Next: bootstrap, migrate, implement features."

---

### Phase 7: Implementation (if user continues)

1. Dispatch `cf_implement_bootstrap` skill
2. Dispatch `cf_implement_migrate` skill (subagent)
3. Dispatch `cf_implement_seed` skill
4. For each feature in implementation plan:
   a. Dispatch `cf_implement_feature` skill (subagent) — searches for prog-expert-* skills
   b. Dispatch `cf_test_unit` skill (subagent)
   c. Run two-stage review:
      - Spec compliance: does implementation match feature + screen specs?
      - Code quality: patterns, testing, maintainability
   d. On pass: update feature impl_status → implemented
5. Dispatch `cf_test_integration` (subagent)
6. Dispatch `cf_test_e2e` (subagent)
7. Dispatch `cf_quality_verify` skill
8. Final audit + readiness check

---

### Phase 8: Quality Gates (continuous)

These can run at any time during the pipeline:
- `cf_quality_review` — structure audit + gardening
- `cf_quality_audit` — static code analysis
- `cf_quality_sync` — cross-reference repair
- `cf_quality_ready` — readiness checklist

In auto-review mode, review + sync run automatically between phases (advisory, not blocking).

## Completion Check Protocol

Every phase ends with a completion check:

**Default mode:**
1. Present produced files, key decisions, quality score to user
2. Ask for feedback or confirmation to continue
3. On confirmation: update PLANS.md
4. On feedback: re-dispatch skill with feedback context

**Auto-review mode:**
1. Run `python3 scripts/lint_concept.py _concept`
2. Run `cf_quality_review` skill in gardening mode
3. Read quality score from `_concept/quality.json`
4. Score ≥ 70 + 0 blocking → log and continue
5. Else → pause and present issues to user

## Two-Stage Review (Implementation Phases)

After each implementation step:
1. **Spec compliance review**: Does output match _concept/ specs?
   - Features requirements satisfied?
   - Screen component inventory present?
   - Data entities match model?
2. **Quality review**: Is code well-structured?
   - Tests exist and pass?
   - No security issues?
   - Follows tech stack conventions?

Both must pass before marking a feature as implemented.

## Subagent Dispatch

When pipeline.json marks a step as `"subagent": true`:
1. Create fresh agent context with ONLY:
   - The step's SKILL.md
   - Required shared/contracts/ contracts (listed in SKILL.md Context Budget)
   - Input _concept/ folders (from pipeline.json depends_on)
2. Subagent runs to completion, writes to _concept/
3. Orchestrator collects results, runs completion check

**Never forward conversation history to subagents.**

## Research Mode Integration

When research_depth != "skip":
1. At each phase, check if research is relevant:
   - Overview → domain, competitors
   - Features → patterns, behavioral_patterns
   - Brand → colors_fonts, design_inspiration
   - Datamodel → patterns
   - Screens → design_inspiration, patterns
2. Dispatch research skill as parallel subagent with focused scope
3. Research writes cross-cutting findings to `_concept/_research/general/` and step-specific findings to `_concept/_research/{step}/`
4. Main skill reads research data before making decisions

Research depth controls thoroughness:
- **light**: 1-2 competitors, basic domain scan
- **moderate**: 3-5 competitors, personas, design patterns
- **deep**: 5-8 competitors, detailed personas, extensive pattern library, color/font research

## Expert Discovery (Implementation)

During implementation phases:
1. Read `05_techstack/stack.md`
2. Search for matching `prog-expert-*` skills:
   - Check `.claude/skills/prog-expert-*/`
   - Check `.agents/skills/prog-expert-*/`
   - Check paths in `pipeline.json` config.expert_search_paths
3. If found: include expert's SKILL.md in subagent context
4. Expert recipes guide implementation patterns

## Common Mistakes

| Rationalization | Reality |
|----------------|---------|
| "I'll run all phases without pausing" | Completion checks exist for a reason. Even in auto-review, quality gates apply. |
| "The user said to be fast, skip research" | Use complexity presets. Don't skip ad-hoc — update PLANS.md settings. |
| "The subagent needs the full conversation" | Fresh context only. Full history burns tokens and loses focus. |

## Integration
- **Dispatches:** all pipeline skills
- **Reads:** pipeline.json (execution plan), PLANS.md (progress)
- **Writes:** PLANS.md (progress, decisions, blockers)
- **Quality tools:** review, audit, sync, ready (continuous)

```
[orchestrator] completed
  run_id: <uuid>
  phases_completed: N
  implementation_tasks: N
  quality_score: NN
```
