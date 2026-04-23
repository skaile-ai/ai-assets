---
name: "implement"
description: "Monorepo-aware implementation orchestrator for the skaile-dev codebase. Reads the affected package CLAUDE.md files, identifies the tech stack, routes to the right prog-experts, produces a plan, executes with supervised dispatch, then triggers test, doc, and devlog on completion. Use for any non-trivial change to the skaile-dev monorepo."
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
- `large` — cross-package change or architectural work; full `skaileup-implementation-superpowers` pattern

## When to Use

- Adding a feature, fixing a bug, or refactoring in any skaile-dev package
- Adding a new AI skill or domain to `ai-assets/`
- Making changes that span multiple packages
- Any task where "just do it" risks missing conventions or cross-package effects

## When NOT to Use

- Pure git operations (no code changes) — use `git` directly
- Documentation-only update — use `doc` directly
- Running existing tests — use `test` directly
- Writing a devlog entry for work already done — use `devlog` directly

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
  skaileup-shared/contracts/golden_principles.md              — naming and structure rules
  skaileup-shared/contracts/iron_laws.md                      — non-negotiable constraints
  skaileup-implementation-superpowers/skills/*/SKILL.md       — supervised dispatch patterns
  skills/git/references/branch_naming.md                 — branch naming for this change
  ! references/sub-agent-dispatch.md                     — fan-out mitigation: MVC prompts, batching, pre-dispatch compaction

MUST  read the target package(s) CLAUDE.md before writing any code
MUST  identify the correct prog-expert for the tech stack and note it in the plan
MUST  run tests after implementation (via test skill)
MUST  run audit scope=diff after tests pass (gate Phase 5 on audit ≠ fail)
MUST  run doc --mode update after any public API or structure change
MUST  annotate all added or modified exported symbols with TSDoc (per references/doc_pattern.md) before running doc --mode update
MUST  update README.md Purpose section when the package's role or public API changes
MUST  update CLAUDE.md when architecture, conventions, or environment variables change
MUST  add a devlog entry after every completed implementation
MUST  create a git branch before implementing (via git skill)
MUST  verify platform/backend starts (bun run dev) after any structural backend change before marking the phase complete — structural changes include: new @Injectable service, constructor parameter changes, *.module.ts providers/imports/exports changes, import path changes in a service or module
NEVER implement across packages without reading each package's CLAUDE.md
NEVER skip the plan phase for standard or large complexity
NEVER commit to main directly — always use a feature branch
NEVER run Biome (biome format / biome lint / bun run format) inside platform/ — platform uses Prettier + ESLint; run bun run lint from within platform/backend or platform/frontend instead
NEVER create barrel index.ts files in platform/backend/libs/ — import directly via subpath alias (@lib/file) instead; barrel files break NestJS DI module boundaries (exception: PostXL-generated barrels tracked in postxl-lock.json)

EMIT [implement] started task=<slug> packages=<list> complexity=<tier>

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
      - agent-framework/asset-manager → asset management library
      - ai-assets/<domain> → AI skill/domain
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
  | ai-assets/<domain> | Markdown, YAML (skill conventions) | (follow CLAUDE.md skill conventions) |
  | docs/ | Astro, Starlight | (follow doc) |
  | agent-framework/cli | TypeScript, Bun | (read package CLAUDE.md) |

STEP 3: Find relevant expert skills
  - Search dev-implementation-experts-* for skills matching the identified stack
  - Note available experts in the plan — they will be referenced in task instructions

EMIT [implement] context_loaded packages=<list> experts=<list>

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
    - Identify git branch name (see git/references/branch_naming.md)
    - Write skaile-plan.md
  IF complexity = large
    - Use `brainstorm` + `write-plan` from skaileup-implementation-superpowers
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
  - RUN git with mode=branch, branch_name=<from plan>
  EMIT [implement] git_ready branch=<name>

# ── Phase 3: Implementation ───────────────────────────────────────

STEP 7: Execute

  IF complexity = small
    - Implement directly (no subagent dispatch needed)
    - Read prog-expert skill recipes if applicable
    - Verify implementation compiles / lint passes
    - $ git add -p
    - RUN git mode=commit to generate structured commit

  PRE-DISPATCH (standard and large): read references/sub-agent-dispatch.md once.
  Then apply the three techniques before any Agent call:

  TECHNIQUE 1 — Minimum Viable Context (MVC) prompt
    Build each sub-agent prompt explicitly using this template.
    NEVER pass the parent conversation or ask the agent to "read the plan":

    ```
    # Task: <one-line summary>

    ## Context
    Package: <name>
    Files to change: <paths — only the ones this task touches>
    Key types / signatures (paste relevant excerpts — do NOT ask agent to read files):
    <paste only the sections of CLAUDE.md or source that this task needs>

    ## What to implement
    <Precise description. Acceptance criteria as a checklist.>
    - [ ] ...

    ## Constraints
    <Naming rules, patterns to follow, anti-patterns to avoid — specific to this task.>

    ## What NOT to do
    <Explicit scope boundary — name files or concerns to leave untouched.>

    ## Output
    Files to create or modify: <list>
    ```

  TECHNIQUE 2 — Batch + parallel dispatch
    Consult the decision table in references/sub-agent-dispatch.md to pick the
    right grouping for the number of tasks at hand. Key rules:
    - Group by package boundary (shared context, one CLAUDE.md excerpt per batch)
    - 4–8 tasks per agent, 3–5 agents in parallel
    - Independent tasks → parallel; dependent tasks → sequential within batch

  TECHNIQUE 3 — Pre-dispatch compaction
    Before sending any Agent call, call /compact if the parent session has:
    - Read 3+ files, OR
    - Run any test commands, OR
    - Been running more than 20 minutes
    The compaction summary is what every sub-agent inherits — keep it small.

  IF complexity = standard (≤5 tasks total)
    STEP 7a: Apply dispatch pattern from decision table
      IF 1–3 tasks: one agent per task, all in parallel, MVC prompts
      IF 4–5 tasks: group by package → one batch agent per package (parallel)

    AFTER each parallel batch returns:
      - Run spec-compliance review for each produced change
        IF NON_COMPLIANT → fix inline (do NOT re-dispatch; fix is cheap at this scale)
      - Run code quality check: naming, no debug artifacts, no cross-task bleed
      - $ git add -p
      - RUN git mode=commit to generate structured commit
      - Update skaile-plan.md: mark tasks done
      - Run quick test pass — pipe output: 2>&1 | tail -60
        IF tests fail → fix before next batch

      COMPACT CHECKPOINT: call /compact before the next batch.

      EMIT [implement] task_complete task=<id>

  IF complexity = large (6+ tasks)
    STEP 7b: Structure as batch waves
      WAVE PLANNING:
        - Group tasks into 3–5 batch agents by package + independence
        - Each batch agent gets 4–8 tasks and a single batch MVC prompt
        - Dispatch all agents in the wave in parallel (one Agent call per wave)
        - Do NOT dispatch one agent per task — this is the fan-out failure mode

      FOR EACH wave:
        - /compact before dispatch (enforce pre-dispatch compaction)
        - Dispatch all batch agents in the wave in parallel
        - After the wave returns: review, commit, quick test pass
        - /compact before the next wave

      After all waves: use `implement-supervised` from skaileup-implementation-superpowers
      for any remaining cross-cutting or integration tasks.

EMIT [implement] implementation_done tasks=<N>

# ── Phase 4: Verification Loop ────────────────────────────────────

STEP 8: Run tests
  - RUN test with mode=run, scope=<affected packages>
  - All test commands must pipe output: 2>&1 | tail -80
  IF tests fail
    - Fix failures
    - Commit fix: "fix: <description>"
    - Re-run tests
  UNTIL all tests pass

  COMPACT CHECKPOINT:
    After tests pass, call /compact before proceeding to audit.
    The test→fix→retest loop is the highest single growth point for context in
    this skill. Compacting here prevents audit + doc phases from paying the cost
    of the entire test loop history.

CHECKPOINT tests_passed
  > "All tests passing. Running diff-scoped audit before doc sync."

# ── Phase 4a: Backend Start Verification ────────────────────────────

STEP 8a: Verify platform/backend starts (conditional)
  IF any structural platform/backend change was made in this implementation — i.e., any of:
    - New or modified `@Injectable()` or `@Module()` class
    - Constructor parameter added, removed, or retyped in a service
    - `providers:`, `imports:`, or `exports:` arrays changed in `*.module.ts`
    - Import paths changed in a service or module file

  THEN:
    RUN in background (15 s timeout): cd platform/backend && bun run dev
    Watch for startup success ("Nest application successfully started") or an exception.

    IF port 3001 already in use:
      ASK:
        > "Port 3001 is in use. Choose:
        >   1. Use kill-backend skill to free it, then retry
        >   2. Kill it manually — confirm when done
        >   3. Skip verification and proceed"
      HANDLE response (retry on options 1/2; skip on option 3).

    IF NestJS DI error / unresolved dependency exception before startup banner:
      Terminate dev process.
      STOP: "Backend failed to start: <error summary>. Fix the DI error before proceeding."
      Fix the issue, re-run tests (STEP 8), then re-run this step.

    IF startup banner appears:
      Terminate dev process.
      > "Backend starts cleanly."

EMIT [implement] backend_start_verified

# ── Phase 4b: Audit (scope=diff) ──────────────────────────────────

STEP 8b: Run audit on the diff
  - RUN audit with scope=diff, diff_source=branch
  - Read _devlog/reports/audit-<stamp>.json
  IF verdict = fail
    - Report blockers to user
    - Ask: "fix now or abort?"
    - IF fix: apply fixes, re-run audit; UNTIL verdict ≠ fail
    - IF abort: stop here (branch is preserved)
  IF verdict = warn
    - Show findings summary; proceed (warnings do not block implementation)

EMIT [implement] audit_done verdict=<verdict>

# ── Phase 5: Documentation Sync ───────────────────────────────────

STEP 9: Update docs

  PRE-CHECK (TypeScript packages only):
    Did this change add or modify any exported symbols?
    IF yes:
      - Annotate all changed exports with TSDoc per references/doc_pattern.md
      - IF docs/api-reference.md exists for this package:
          $ bun _scripts/generate-api-docs.ts --package <pkg>
      - $ git add -p && git commit -m "docs: add TSDoc annotations for <task-slug>"

  - RUN doc --mode update
  - Scope: files changed since branch was created
  IF docs were updated
    - $ git add docs/ && git commit -m "docs: sync documentation for <task-slug>"

EMIT [implement] docs_synced pages_updated=<N>

# ── Phase 6: Devlog ───────────────────────────────────────────────

STEP 10: Write devlog entry
  - RUN devlog with:
    - what_changed: summary of what was implemented (1–2 sentences, plain language)
    - why: motivation and context
    - packages: list of affected packages
    - implications: what downstream effects or breaking changes exist (if any)
    - report_needed: true if this is a conceptual/architectural change

EMIT [implement] devlog_written

SUGGEST session_review
  > "Session complete. Run `/session-review` for a token usage and workflow analysis
  >  of this implementation session."

# ── Phase 6b: Notify (optional) ──────────────────────────────────

STEP 10b: Send notifications (if warranted)
  IF breaking: true in any commit on this branch
    - RUN notify template=breaking-change
  IF complexity = large
    - RUN notify template=plan-complete

EMIT [implement] notified

# ── Phase 7: Branch Completion ────────────────────────────────────

STEP 11: Finish branch
  - RUN git with mode=finish
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

EMIT [implement] completed branch=<name> tasks=<N> tests_passing=<N>

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
  - [ ] Sub-agent dispatch: decision table consulted, batch+parallel applied, MVC prompts written
  - [ ] /compact called before first Agent dispatch (if session has prior context)
  - [ ] Spec compliance review run for every task
  - [ ] Full test suite passing before audit
  - [ ] Backend start verified (or explicitly skipped) if structural platform/backend changes made
  - [ ] audit scope=diff run and verdict ≠ fail before docs sync
  - [ ] TSDoc added to all new/modified exported symbols (TypeScript packages)
  - [ ] doc --mode update run after any public API change
  - [ ] Devlog entry written
  - [ ] Branch finished (merge / PR / keep)

---

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Implementing without reading CLAUDE.md | Always read the package CLAUDE.md first — conventions vary significantly |
| Committing directly to main | Create a feature branch via git before any code change |
| Skipping spec compliance for "simple" tasks | All tasks get spec compliance review — even small ones |
| Forgetting test after implementation | Tests are the safety net; never skip |
| Skipping doc for non-trivial changes | If a public API, command, or structure changed, docs must sync |
| Skipping the devlog | Every completed change gets a devlog entry — this is the institutional memory |
| Dispatching one sub-agent per task sequentially | Use batch+parallel pattern from references/sub-agent-dispatch.md — see decision table |
| Sub-agent prompt says "based on our conversation…" | Always build an explicit MVC prompt — sub-agents must be self-contained |
| Sub-agent prompt asks agent to "read CLAUDE.md" | Paste the relevant CLAUDE.md excerpt directly into the prompt |
| Dispatching agents without /compact first | Pre-dispatch compaction is mandatory when parent session has done prior work |

## Integration

- **Routes to:** `prog-expert-nuxt`, `prog-expert-omp`, `prog-expert-python`, `implement-supervised`
- **Calls:** `git`, `test`, `audit`, `doc`, `devlog`, `notify`
- **Called by:** `skaile-development` or user directly
