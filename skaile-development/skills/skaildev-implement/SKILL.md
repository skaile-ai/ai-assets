---
name: "skaildev-implement"
description: "Monorepo-aware implementation orchestrator for the skaile-dev codebase. Reads the affected package CLAUDE.md files, identifies the tech stack, routes to the right prog-experts, produces a plan, executes with supervised dispatch, then triggers skaildev-run-tests, skaildev-update-docs, and skaildev-devlog on completion. Use for any non-trivial change to the skaile-dev monorepo."
metadata:
  version: "1.0.0"
  tags:
    - "implement"
    - "skaile-development"
    - "monorepo"
    - "orchestrator"
    - "plan"
    - "supervised"
    - "routing"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
      - path: "skaile-dev/CLAUDE.md"
        gate: soft
        description: "Monorepo CLAUDE.md for conventions — read before starting"
    inputs_required:
      - id: task_description
        label: "What needs to be implemented? (plain language)"
        type: text
    inputs_optional:
      - id: target_package
        label: "Target package(s) if known (e.g., forge/project, platform/backend/libs/session-manager)"
        type: text
      - id: complexity
        label: "Complexity hint"
        type: select
        options:
          - "small"
          - "standard"
          - "large"
        default: "standard"
  produces:
    - path: "_implementation/skaile-plan.md"
      description: "Structured plan for this implementation task"
    - path: "_devlog/DEVLOG.md"
      description: "Devlog entry added after completion"
  user_inputs:
    dialog:
      - id: "task_description"
        label: "What needs to be implemented?"
        type: "text"
        required: true
      - id: "target_package"
        label: "Target package(s) if known"
        type: "text"
        required: false
      - id: "complexity"
        label: "Complexity"
        type: "select"
        options: ["small", "standard", "large"]
        required: false
        default: "standard"
    files: []
---

# Implement Skaile — Monorepo-Aware Implementation Orchestrator

## Overview

Drives implementation tasks in the skaile-dev monorepo from task description to committed,
tested, documented code. Unlike the generic `implement` skill (which consumes `_concept/`
to build apps), this skill works on the codebase itself.

**Workflow:** Read context → Identify packages → Route to experts → Plan → Execute (supervised) → Test → Docs → Devlog

**Complexity tiers:**
- `small` — single-file or contained change; direct implementation without full subagent dispatch
- `standard` — multi-file change in 1–2 packages; plan + supervised dispatch
- `large` — cross-package change or architectural work; full `dev-implementation-superpowers` pattern

## When to Use

- Adding a feature, fixing a bug, or refactoring in any skaile-dev package
- Adding a new AI skill or domain to `ai-resources/`
- Making changes that span multiple packages
- Any task where "just do it" risks missing conventions or cross-package effects

## When NOT to Use

- Pure git operations (no code changes) — use `skaildev-git-workflow` directly
- Documentation-only update — use `skaildev-update-docs` directly
- Running existing tests — use `skaildev-run-tests` directly
- Writing a devlog entry for work already done — use `skaildev-devlog` directly

---

ROLE  Monorepo-aware implementation orchestrator — reads context, plans, executes, and closes the loop with tests + docs + devlog.

READS
  skaile-dev/CLAUDE.md                                   — monorepo conventions, package map
  <package>/CLAUDE.md                                     — package architecture, tech stack, key conventions
  <package>/README.md                                     — public API and usage patterns
  ? _implementation/skaile-plan.md                       — resume state if exists

WRITES
  _implementation/skaile-plan.md                         — task plan with checklist
  _implementation/decisions.md                           — decisions and concerns log
  <target source files>                                   — actual code changes
  _devlog/DEVLOG.md                                      — devlog entry on completion

REFERENCES
  dev-shared/contracts/golden_principles.md              — naming and structure rules
  dev-shared/contracts/iron_laws.md                      — non-negotiable constraints
  dev-implementation-superpowers/skills/*/SKILL.md       — supervised dispatch patterns
  skills/skaildev-git-workflow/references/branch_naming.md        — branch naming for this change

MUST  read the target package(s) CLAUDE.md before writing any code
MUST  identify the correct prog-expert for the tech stack and note it in the plan
MUST  run tests after implementation (via skaildev-run-tests skill)
MUST  run skaildev-update-docs after any public API or structure change
MUST  add a devlog entry after every completed implementation
MUST  create a git branch before implementing (via skaildev-git-workflow skill)
NEVER implement across packages without reading each package's CLAUDE.md
NEVER skip the plan phase for standard or large complexity
NEVER commit to main directly — always use a feature branch

EMIT [skaildev-implement] started task=<slug> packages=<list> complexity=<tier>

# ── Phase 0: Context Loading ──────────────────────────────────────

STEP 1: Parse task
  - Identify: what type of change (feature, fix, refactor, skill/domain, infrastructure)
  - Identify: target package(s) from task_description or target_package input
  IF target_package is not provided
    - Infer from task_description by matching keywords against the monorepo package map:
      - forge/project / forge/concept → forge apps (Nuxt 4)
      - platform/backend → NestJS + Fastify + Prisma
      - platform/frontend → React 19 + Vite + TanStack
      - agent-framework/* → agent runtime stack
      - agent-framework/cli/src/arm → ARM library (asset management)
      - ai-resources/<domain> → AI skill/domain
      - docs/ → Starlight docs site
      - agent-framework/cli → CLI tool

STEP 2: Load context
  - Read skaile-dev/CLAUDE.md (monorepo overview, conventions)
  - Read <package>/CLAUDE.md for each target package
  - If package has a `docs/` directory, note the doc structure
  - Identify tech stack per package:

  | Package | Stack | Prog Expert |
  |---------|-------|-------------|
  | forge/project, forge/concept | Nuxt 4, drizzle-orm, SQLite, UnoCSS | prog-expert-nuxt |
  | platform/backend | NestJS, Fastify, Prisma, tRPC, Jest | (read platform/CLAUDE.md) |
  | platform/frontend | React 19, Vite, TanStack, Tailwind CSS 4, Vitest | (read platform/CLAUDE.md) |
  | agent-framework/* | TypeScript, Bun, OMP | prog-expert-omp |
  | agent-framework/cli | TypeScript, Bun, Commander | (read package CLAUDE.md) |
  | ai-resources/<domain> | Markdown, YAML (skill conventions) | (follow CLAUDE.md skill conventions) |
  | docs/ | Astro, Starlight | (follow skaildev-update-docs) |
  | agent-framework/cli | TypeScript, Bun | (read package CLAUDE.md) |

STEP 3: Find relevant expert skills
  - Search dev-implementation-experts-* for skills matching the identified stack
  - Note available experts in the plan — they will be referenced in task instructions

EMIT [skaildev-implement] context_loaded packages=<list> experts=<list>

# ── Phase 1: Plan ─────────────────────────────────────────────────

STEP 4: Create or resume plan
  IF _implementation/skaile-plan.md exists
    - Read it — identify last incomplete task
    - Report resume state to user
    - Skip to STEP 6
  ELSE
    - Continue to STEP 5

STEP 5: Build plan
  IF complexity = small
    - Write a 3–5 bullet list of what will be changed and why
    - No formal task structure needed
    - Write skaile-plan.md with the summary + git branch name
  IF complexity = standard
    - Break into tasks: max 5 tasks, each self-contained
    - For each task: describe file changes, acceptance criteria, expert skill to use
    - Identify git branch name (see skaildev-git-workflow/references/branch_naming.md)
    - Write skaile-plan.md
  IF complexity = large
    - Use `brainstorm` + `write-plan` from dev-implementation-superpowers
    - Each task gets model tier assignment (haiku/sonnet/opus)
    - Write skaile-plan.md with full task breakdown

  CHECKPOINT plan_approval
    > "Here's the plan:
    > Branch: <branch-name>
    > Tasks: [list]
    > Expert skills: [list]
    >
    > Approve the plan, or tell me what to modify."

# ── Phase 2: Git Setup ────────────────────────────────────────────

STEP 6: Prepare git
  - RUN skaildev-git-workflow with mode=branch, branch_name=<from plan>
  EMIT [skaildev-implement] git_ready branch=<name>

# ── Phase 3: Implementation ───────────────────────────────────────

STEP 7: Execute

  IF complexity = small
    - Implement directly (no subagent dispatch needed)
    - Read prog-expert skill recipes if applicable
    - Verify implementation compiles / lint passes
    - $ git add -p
    - RUN commit-message skill to generate structured commit

  IF complexity = standard
    FOR EACH task in skaile-plan.md:
      - Dispatch subagent with verbatim task text (do NOT ask subagent to read plan)
      - Include: task spec, tech context, expert skill references, acceptance criteria
      - Run spec-compliance review: does the produced code fulfill the task spec?
        IF NON_COMPLIANT → fix and re-review
      - Run code quality check: naming, no debug artifacts, no cross-task bleed
        IF FAIL → fix and re-check
      - $ git add -p
      - RUN commit-message skill to generate structured commit
      - Update skaile-plan.md: mark task done
      - Run full test suite (quick pass to catch regressions)
        IF tests fail → fix before moving to next task
      EMIT [skaildev-implement] task_complete task=<id>

  IF complexity = large
    - Use `implement-supervised` from dev-implementation-superpowers
    - Feed it skaile-plan.md as the superpowers-plan.md equivalent

EMIT [skaildev-implement] implementation_done tasks=<N>

# ── Phase 4: Verification Loop ────────────────────────────────────

STEP 8: Run tests
  - RUN skaildev-run-tests with mode=run, scope=<affected packages>
  IF tests fail
    - Fix failures
    - Commit fix: "fix: <description>"
    - Re-run tests
  UNTIL all tests pass

CHECKPOINT tests_passed
  > "All tests passing. Ready to update docs and log the change."

# ── Phase 5: Documentation Sync ───────────────────────────────────

STEP 9: Update docs
  - RUN skaildev-update-docs
  - Scope: files changed since branch was created
  IF docs were updated
    - $ git add docs/ && git commit -m "docs: sync documentation for <task-slug>"

EMIT [skaildev-implement] docs_synced pages_updated=<N>

# ── Phase 6: Devlog ───────────────────────────────────────────────

STEP 10: Write devlog entry
  - RUN skaildev-devlog with:
    - what_changed: summary of what was implemented (1–2 sentences, plain language)
    - why: motivation and context
    - packages: list of affected packages
    - implications: what downstream effects or breaking changes exist (if any)
    - report_needed: true if this is a conceptual/architectural change

EMIT [skaildev-implement] devlog_written

# ── Phase 7: Branch Completion ────────────────────────────────────

STEP 11: Finish branch
  - RUN skaildev-git-workflow with mode=finish
  - Options: merge (direct), pull-request, keep

CHECKPOINT completion
  > "Implementation complete.
  > Branch: <name>
  > Tasks: <N> done
  > Tests: all passing
  > Docs: <N pages> updated
  > Devlog: entry added
  >
  > What would you like to do with the branch? (merge / pull-request / keep)"

EMIT [skaildev-implement] completed branch=<name> tasks=<N> tests_passing=<N>

# ── Procedures ────────────────────────────────────────────────────

PROCEDURE spec_compliance_review(task, produced_code)
  - Read the task acceptance criteria line by line
  - Verify each criterion is addressable by the produced code
  - Return: COMPLIANT | NON_COMPLIANT (with gap list)

PROCEDURE code_quality_check(produced_code)
  - Naming follows golden_principles.md
  - No console.log, TODO, commented-out blocks remain
  - No cross-task file bleed
  - Imports are clean (no unused imports)
  - Return: PASS | FAIL (with issue list)

CHECKLIST
  - [ ] Package CLAUDE.md(s) read before any implementation
  - [ ] Tech stack identified and prog-expert noted in plan
  - [ ] Plan approved before implementation starts
  - [ ] Git branch created (never commit to main)
  - [ ] Spec compliance review run for every task
  - [ ] Full test suite passing before docs sync
  - [ ] skaildev-update-docs run after any public API change
  - [ ] Devlog entry written
  - [ ] Branch finished (merge / PR / keep)

---

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Implementing without reading CLAUDE.md | Always read the package CLAUDE.md first — conventions vary significantly |
| Committing directly to main | Create a feature branch via skaildev-git-workflow before any code change |
| Skipping spec compliance for "simple" tasks | All tasks get spec compliance review — even small ones |
| Forgetting skaildev-run-tests after implementation | Tests are the safety net; never skip |
| Skipping skaildev-update-docs for non-trivial changes | If a public API, command, or structure changed, docs must sync |
| Skipping the devlog | Every completed change gets a devlog entry — this is the institutional memory |

## Integration

- **Routes to:** `prog-expert-nuxt`, `prog-expert-omp`, `prog-expert-python`, `implement-supervised`
- **Calls:** `skaildev-git-workflow`, `commit-message`, `skaildev-run-tests`, `skaildev-update-docs`, `skaildev-devlog`
- **Called by:** `skaile-workspace-advisor` or user directly
