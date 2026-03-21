---
name: scaffold
description: "Use when tech stack is chosen and user wants to scaffold a new project before starting implementation. Also when user says 'scaffold', 'bootstrap', 'create the project', 'set up the repo'."
keywords: [scaffold, init, setup, boilerplate, nuxt, node, python, bun, uv, skills, project, implement]
user_inputs:
  dialog: []
  files: []
metadata:
  stage: alpha
  requires:
  - implementation-contract
---

# Bootstrap — Project Scaffolding

## Overview

The **bootstrap** skill is the Project Bootstrapper. It scaffolds clean,
production-ready projects using modern toolchains. It reads the tech stack
definition from the concept pipeline and uses it to configure the project.

**Phase:** tools / scaffold
**Writes to:** new project directory

## When to Use

- The tech stack is chosen and the user wants to scaffold a new project
- The user says "scaffold", "bootstrap", "create the project", "set up the repo"
- Starting implementation from a completed concept
- The orchestrator dispatches this after tech stack is approved

## When NOT to Use

- No tech stack has been chosen — run **techstack** first
- The project already exists and needs code changes — use **implement** instead
- The user just wants mockups — use **cf_concept_mock**

## Prerequisites

### HARD-GATE

`_concept/05_techstack/stack.md` must exist. If not:

> "No tech stack found. Run the **techstack** skill first to choose your framework, database, and tools."

### Shared Contracts

Before starting, read:
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

If `_concept/` exists, also read:
- `_concept/05_techstack/stack.md` — technology choices (framework, DB, etc.)
- `_concept/04_brand/tokens.json` — for initial CSS/theme setup
- `cf__shared/concept_structure.md` — to understand _concept/ layout

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/05_techstack/stack.md` | Yes |
| **Must read** | `tech-stack/<tech_stack_skill>/SKILL.md` | Yes (if tech_stack_skill is set in stack.md) |
| **Optional** | `_concept/04_brand/tokens.json` | No (for initial CSS) |
| **Optional** | `_concept/06_datamodel/model.json` | No (for initial schema) |
| **Never load** | Feature files, screen specs, research | — |

## Standalone Mode

This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/05_techstack/stack.md` must exist
**If gates fail:** Run cf_concept_techstack first
**On completion:** Present summary, then suggest next steps (cf_implement, cf_implement_migrate).

## Workflow

### Phase 1: Context Gathering

**If `_concept/05_techstack/stack.md` exists:**
Read it and extract framework, UI library, backend, database, package manager.
Confirm with the user:
> "I found your tech stack: [summary]. Bootstrap with these choices?"

**If no tech stack exists:**
Ask:
- Project type (Nuxt, Flask, Python CLI, etc.)
- Project name and directory
- Key dependencies

### Phase 2: Scaffold

**Step 1: Resolve scaffold recipe**

1. Read `_concept/05_techstack/stack.md`, extract `tech_stack_skill:`
2. If `tech_stack_skill:` is present:
   - Read `tech-stack/<tech_stack_skill>/SKILL.md`
   - Extract the `scaffold_recipe:` section
   - Execute that recipe exactly (commands, config files, folder structure)
3. If `tech_stack_skill:` is absent (legacy stack.md format or custom stack):
   - Fall back to asking the user:
     > "I couldn't find a tech_stack_skill reference in stack.md. What type of project is this?
     > (e.g., Nuxt/Node with bun, Next.js with pnpm, Python with uv, custom)"
   - Based on the answer, proceed with the appropriate manual scaffold steps

**Step 2: Execute the recipe**

Follow the `scaffold_recipe:` from the tech-stack skill. This may include:
- Framework init commands (e.g., `bunx nuxi@latest init`)
- Package manager configuration
- Standard folder structure setup
- Config file generation (e.g., `nuxt.config.ts`, `pyproject.toml`)

If brand tokens exist (`_concept/04_brand/tokens.json`): generate initial CSS
variables from `tokens.json` as part of the scaffold step if the recipe supports it.

### Phase 3: Skill Linking

If global skills exist at `/home/matthias/workBench/SKILLS/.claude/skills`:
1. Scan available skills
2. Recommend 2-3 most relevant for this stack
3. Ask user which to link
4. `ln -s` into `.claude/skills/`

### Phase 4: Finalization

- Run package install and verify clean
- Generate summary or project context doc
- List installed dependencies and linked skills

### Emit Events

```
[bootstrap] started
  run_id: <uuid>
  reads: 05_techstack/stack.md

[bootstrap] checkpoint phase=scaffold_complete
  framework: Nuxt 3
  package_manager: bun

[bootstrap] completed
  run_id: <uuid>
  framework: Nuxt 3
  package_manager: bun
  skills_linked: [cf_concept_mock, cf_concept_datamodel]
  tech_stack_source: _concept/05_techstack/stack.md
```

## Outputs

| Output | Description |
|--------|-------------|
| Project directory | Scaffolded project with framework, config, and dependencies |
| Linked skills | Symlinked relevant skills into `.claude/skills/` |

## Completion Summary

Present to user: files produced (scaffolded project directory, linked skills), key decisions made (framework configuration, dependency versions, skill selections), suggested next steps (which skills are now unblocked — e.g., cf_implement for implementation workflow, cf_implement_migrate for database setup).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Scaffolding without reading stack.md | The agent uses defaults | Always read and confirm the tech stack first. |
| Installing deprecated packages | The agent uses old versions | Use latest stable versions. Check package registry. |
| Skipping brand token integration | The agent ignores tokens.json | If brand tokens exist, generate initial CSS variables. |
| Creating a README without being asked | The agent auto-generates docs | Only create docs if the user asks. |
| Linking all available skills | The agent links everything | Recommend 2-3 relevant skills, let user choose. |

## Integration

- **Called by:** orchestrator or standalone
- **Reads from:** `_concept/05_techstack/stack.md`, optionally `_concept/04_brand/tokens.json`
- **Feeds into:** implementation workflow (the scaffolded project is where code lives)
- **Feedback loops:** None. This is a one-shot scaffolding step.
