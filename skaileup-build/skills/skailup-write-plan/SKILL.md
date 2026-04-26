---
name: "skailup-write-plan"
description: "Converts concept artifacts and brainstorm output into a decomposed, dependency-ordered implementation plan ready for supervised subagent dispatch. Each task is self-contained with pasted spec text, tech context, and a model tier recommendation. Triggers on: 'write the plan', 'decompose into tasks', 'create the implementation plan', 'plan before we build'."
metadata:
  version: "1.0.0"
  tags:
    - "plan"
    - "decompose"
    - "tasks"
    - "subagent"
    - "implementation"
    - "dependency-order"
  stage: beta
  source: "MERGED"
  prerequisites:
    files:
      - path: "_concept/experience/features"
        gate: hard
        description: "Features required to derive task list"
        min_entries: 1
      - path: "_concept/blueprint/datamodel/model.json"
        gate: hard
        description: "Data model required for entity dependency ordering"
      - path: "_implementation/brainstorm.md"
        gate: soft
        description: "Brainstorm recommended — plan will note which risks are unaddressed if absent"
  user_inputs:
    dialog:
      - id: "task_granularity"
        label: "Task granularity: 'feature' (one task per feature) or 'page' (one task per page, finer)"
        type: "select"
        options:
          - "feature"
          - "page"
        required: false
        default: "feature"
      - id: "model_default"
        label: "Default model tier for implementation tasks"
        type: "select"
        options:
          - "haiku"
          - "sonnet"
          - "opus"
        required: false
        default: "sonnet"
    files: []
  reads_from:
    - "_concept/experience/features/**/*.md"
    - "_concept/experience/screens/**/*.md"
    - "_concept/blueprint/datamodel/model.json"
    - "_concept/blueprint/techstack.md"
    - "_concept/experience/journeys/stories.json"
    - "_implementation/brainstorm.md"
  writes_to:
    - "_implementation/superpowers-plan.md"
---

# Write Plan

## Overview

Produces `_implementation/superpowers-plan.md` — a structured task list where each task
is self-contained and ready for direct paste into a subagent prompt. No task requires the
subagent to read the plan file or navigate the concept directory independently.

Tasks are ordered by dependency (infrastructure before features, parents before children)
and journey stage (hero → vital → hygiene).

Each task includes:
- Verbatim spec text from the relevant feature and screen specs
- Tech context (relevant section of stack.md)
- Matched expert skills
- Model tier recommendation
- Task type tag for dispatch routing

## When to Use

- After `brainstorm` (strongly recommended)
- Before `implement-supervised`

---

ROLE  Implementation plan writer — produces a dependency-ordered, subagent-ready task list from concept artifacts.

READS
  _concept/discovery/brief.md
  _concept/experience/features/**/*.md
  _concept/experience/screens/**/*.md
  _concept/blueprint/datamodel/model.json
  _concept/blueprint/techstack.md
  ? _concept/experience/journeys/stories.json
  ? _implementation/brainstorm.md
  ? _implementation/superpowers-plan.md   — resume: update existing plan

WRITES
  _implementation/superpowers-plan.md

MUST  read all feature and screen specs before writing any task
MUST  paste spec text verbatim into each task — never refer subagents to external files
MUST  order tasks by dependency (infrastructure first, then features in journey order)
MUST  assign model_tier per task based on complexity
NEVER write tasks that require the subagent to read the plan file
NEVER group independent tasks into one task — finer decomposition is better than coarser

# ── Workflow ───────────────────────────────────────────────────────

STEP 1: Read all artifacts
  - Read brief, all features + screens, model.json, stack.md
  - Read stories.json if exists (journey ordering)
  - Read brainstorm.md if exists (note risks and mitigations to include in tasks)

STEP 2: Build dependency graph
  - From model.json: derive entity insert order (parents before children)
  - From features: identify auth/permissions (must come before domain features)
  - From architecture.md (if exists): identify custom modules (before feature implementation)

STEP 3: Build task list

  Task groups in order:
  1. **Infrastructure tasks** (one task each):
     - [ ] Scaffold (if not done)
     - [ ] Foundation (brand, auth, app shell)
     - [ ] Database migrations
     - [ ] Seed data (if seed.json exists)

  2. **Feature tasks** (ordered by journey stage → feature dependency):
     - Group by journey stage: hero first, vital second, hygiene last
     - Within each stage: parents before children (entity dependency order)
     - Granularity per `task_granularity` input: feature (default) or page

  3. **Verification tasks**:
     - [ ] Unit + integration tests (if not TDD-covered)
     - [ ] E2E run
     - [ ] Verify (final gate)

  For each task, determine:
  - `task_type`: infra | feature | page | test | verify
  - `model_tier`: haiku (mechanical) | sonnet (standard) | opus (complex)
    - haiku: config, rename, boilerplate-only tasks
    - opus: complex logic, security-critical, blocked tasks, architectural decisions
    - sonnet: everything else (default)
  - `parallel_safe`: true if task has no shared file writes with concurrent tasks

STEP 4: Write _implementation/superpowers-plan.md

  ---
  # Superpowers Implementation Plan
  app: <app-name>
  created: <ISO date>
  task_granularity: feature | page
  total_tasks: N
  ---

  ## Risk Notes
  <If brainstorm.md exists: summarize top risks and mitigations>
  <If brainstorm.md absent: note "brainstorm not run — risks unassessed">

  ## Task List

  For each task:

  ---
  ### Task <N>: <Task Name>
  id: <slug>
  status: pending | in_progress | done | blocked
  task_type: infra | feature | page | test | verify
  model_tier: haiku | sonnet | opus
  parallel_safe: true | false
  depends_on: [task-id, ...]

  #### Your Task
  <Full task description — written as if pasted directly into a subagent prompt>
  <Include: what to build, what tests to write first, what done looks like>
  <Do NOT say "see the spec" — paste the relevant spec content here>

  #### Relevant Specs
  <Verbatim feature spec content>
  <Verbatim screen spec content (if applicable)>

  #### Tech Context
  <Relevant section of stack.md>
  <Expert skills: list matched skaileup-implementation-expert-* skills>

  #### Acceptance Criteria
  <EARS-format requirements from the feature spec>
  ---

STEP 5: Present plan for approval
  - Show task summary: N tasks across M phases
  - List risk notes if brainstorm was present
  CHECKPOINT plan_approval
    > "Here's the implementation plan:
    > - N infrastructure tasks
    > - M feature tasks across X journeys
    > - P verification tasks
    >
    > [list task names in order]
    >
    > Approve the plan, or tell me what to adjust."

STEP 6: Commit
  $ git add _implementation/superpowers-plan.md
  $ git commit -m "docs: write supervised implementation plan"

CHECKLIST
  - [ ] All feature and screen specs read
  - [ ] Dependency graph derived from model.json
  - [ ] Tasks ordered: infrastructure → hero journey → vital journey → hygiene journey → verify
  - [ ] Each task has verbatim spec text (no external file references)
  - [ ] model_tier assigned per task
  - [ ] Plan approved by user before committing

---

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Writing "see feature spec at path/X" in a task | Paste the spec content verbatim — subagents don't navigate |
| Grouping all features into one task | Decompose — one task per feature (or page if `task_granularity: page`) |
| Ignoring entity dependency order | Parents must be implemented before children — derive from model.json |
| Skipping the brainstorm risk notes | Include them; subagents need risk context to make good decisions |
| Setting all tasks to `sonnet` | Use `haiku` for mechanical tasks; use `opus` for complex/security/blocked |
