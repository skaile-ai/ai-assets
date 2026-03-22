---
name: "scaffold"
description: "Scaffolds a new project from a completed concept. Reads stack.md to determine the tech stack, uses the stack's profile for scaffold commands and conventions, creates the project directory structure, initializes git, and sets up _implementation/ tracking. Run before foundation and feature implementation."
metadata:
  version: "1.0.0"
  tags:
    - "scaffold"
    - "init"
    - "setup"
    - "boilerplate"
    - "project"
    - "bootstrap"
    - "git"
    - "structure"
  source: "MERGED"
---

# Scaffold — Project Scaffolding

## Overview

Creates a buildable project from a completed `_concept/` pipeline. Reads the
tech stack definition from `stack.md` to determine framework, CLI commands, and
project structure conventions. Delegates stack-specific scaffold steps to
`dev-standards/profiles/<tech_stack_skill>/SKILL.md`.

**Framework-agnostic:** all stack-specific scaffold commands come from the
tech stack profile, not from this skill directly.

## When to Use

- Tech stack is chosen (`stack.md` exists) and the project directory does not yet exist
- User says "scaffold", "bootstrap", "create the project", "set up the repo"
- The `implement` orchestrator dispatches this as the first phase

## When NOT to Use

- No tech stack chosen — run `techstack` first
- Project already exists and needs code changes — use `implement-feature`
- Only need mockups — use `mock` (storybook)

## Prerequisites

**Hard gates:**
1. `_concept/3_blueprint/1_techstack/stack.md` exists
2. `_concept/1_discovery/1_overview/brief.md` exists (app name + slug)
3. `_concept/3_blueprint/3_datamodel/model.json` exists (entities for initial setup)

**Required background:** Read `dev-shared/contracts/iron_laws.md` before starting.

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/3_blueprint/1_techstack/stack.md` | Yes |
| Must read | `dev-standards/profiles/<tech_stack_skill>/SKILL.md` | Yes (resolved from stack.md) |
| Must read | `_concept/1_discovery/1_overview/brief.md` | Yes |
| Must read | `_concept/3_blueprint/3_datamodel/model.json` | Yes |
| Read if exists | `_concept/3_blueprint/2_architecture/architecture.md` | Recommended |
| Read if exists | `_concept/3_blueprint/3_datamodel/seed.json` | Recommended |

---

ROLE  Scaffold agent — creates a buildable project from concept artifacts using the tech stack profile.

READS
  _concept/3_blueprint/1_techstack/stack.md                   — tech stack choice + tech_stack_skill field
  dev-standards/profiles/<tech_stack_skill>/SKILL.md          — scaffold commands + project structure
  _concept/1_discovery/1_overview/brief.md                    — app name, slug
  _concept/3_blueprint/3_datamodel/model.json                 — entity count, relationships
  ? _concept/3_blueprint/2_architecture/architecture.md       — custom modules, processes
  ? _concept/3_blueprint/3_datamodel/seed.json                — seed data for initial setup

WRITES
  <app-slug>/                            — project directory (stack-specific structure)
  _implementation/PLANS.md              — initialized from concept features
  _implementation/progress.json         — all features in pending status
  _implementation/decisions.md          — empty decision log with header

REFERENCES
  dev-shared/contracts/concept_structure.md  — canonical _concept/ paths
  dev-shared/contracts/plans.md             — PLANS.md format
  dev-shared/contracts/seed_data.md         — seed scenario format

MUST  read tech stack profile before any scaffold commands
MUST  run Level 1 verification (build) before any commit
MUST  initialize _implementation/ tracking structure
MUST  create git branch: implement/<app-slug>
MUST  confirm with user before creating the project directory
NEVER commit code that does not build
NEVER modify _concept/ files

EMIT [scaffold] started run_id=<uuid>

# ── Workflow ──────────────────────────────────────────────────────

STEP 1: Read concept context
  - Read brief.md: app name, slug, elevator pitch
  - Read stack.md: extract tech_stack_skill value
  - Read profile: `dev-standards/profiles/<tech_stack_skill>/SKILL.md`
    Extract from profile:
    - `scaffold_command` — CLI command to create the project
    - `project_structure` — expected directory layout
    - `build_command` — how to verify build
    - `package_manager` — bun/pnpm/npm/uv/etc.
    - `env_setup_command` — how to copy .env files (if applicable)
  - Read model.json: entity count for summary
  IF architecture.md exists
    - Extract custom_modules[], apps[], external_integrations[]

STEP 2: Confirm with user
  - Present summary: app name, slug, tech stack, entity count, git branch name
  CHECKPOINT scaffold_confirm
    > "Ready to set up '<app-name>' using <tech-stack>.
    >
    > Technical details (if interested):
    >   Stack: <tech_stack_skill>, Entities: N, Branch: implement/<app-slug>
    >
    > Proceed with setup?"

STEP 3: Create project
  - Run the scaffold_command from the tech stack profile
  IF inside an existing git repository
    - Pass any `--skip-git` or equivalent flag from profile
  - Install dependencies using package_manager

STEP 4: Install architecture dependencies (if applicable)
  IF architecture.md exists AND custom_modules or external_integrations present
    - Install required npm/pip packages per the profile's dependency mapping
    - Add any necessary configuration (TypeScript paths, module aliases, etc.)
    - This step is additive only — module code is created by `infrastructure` skill later

STEP 5: Environment setup
  IF env_setup_command exists in profile
    - Run it to copy .env.example → .env files
  ELSE
    - Check if .env.example exists and copy manually if needed

STEP 6: Database migration (if applicable)
  IF stack includes a relational database AND migration tool
    - Run initial migration from model.json/model.dbml
    - IF docker is available: start database container first
    - ELSE: warn that migration is deferred, app will run without data

STEP 7: Seed data (initial setup)
  IF seed.json exists AND stack profile has seed setup instructions
    - Configure the `populated` scenario from seed.json
    - Write to the stack-appropriate seed file location

STEP 8: Verify build
  - Run build_command from profile
  - Run lint (from profile lint_command if defined)
  - Run type check (from profile type_check_command if defined)
    UNTIL all checks pass

STEP 9: Initialize implementation tracking
  IF git init needed (standalone project)
    $ git init
    $ git checkout -b implement/<app-slug>
  ELSE
    $ git checkout -b implement/<app-slug>
  - Create _implementation/PLANS.md (scope, source artifacts, phase checkboxes)
  - Create _implementation/progress.json (all must-have features in pending status)
  - Create _implementation/decisions.md (empty header)
  $ git add -A
  $ git commit -m "scaffold: initialize project from concept"

EMIT [scaffold] completed project=<app-slug> stack=<tech_stack_skill> build=passed branch=implement/<app-slug>

CHECKLIST
  - [ ] Tech stack profile read — scaffold command, build command, package manager known
  - [ ] User confirmed before creating project directory
  - [ ] Project directory created with correct structure
  - [ ] Dependencies installed
  - [ ] Environment setup complete
  - [ ] Database migrated (or deferred with docker warning)
  - [ ] Build passes
  - [ ] Git branch created: implement/<app-slug>
  - [ ] _implementation/ tracking initialized
  - [ ] Initial commit created

---

## Standalone Mode

**Gate check:** `stack.md`, `brief.md`, `model.json`
**If gates fail:** Run `techstack`, `overview`, or `datamodel` as needed.
**On completion:** Suggest running `foundation` next.

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Hardcoding scaffold commands | Read them from the tech stack profile |
| Skipping user confirmation | Always confirm at STEP 2 before creating the directory |
| Not running build verification | Always verify build before committing |
| Missing _implementation/ setup | Create PLANS.md + progress.json in same session as scaffold |
