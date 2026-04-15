---
name: "implementation-contract"
description: "Shared contract for all skaileup-implementation skills. Describes _implementation/ folder layout, scaffold-to-verify pipeline, progress.json format, git workflow, and how implementation reads from _concept/. REQUIRED reading for any implementation skill."
metadata:
  stage: "alpha"
  do_not_invoke: true
---

# Implementation Domain — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `skaileup-implementation` skills read this before operating.

## Scope

This contract covers artifacts and conventions that are **implementation-specific**.
Conventions shared with `skaileup-conceptualization` (semantic types, pipeline graph, plans format) live in `skaileup-shared/contracts/`.

## Prerequisites from _concept/

Implementation reads the following before starting:

```
_concept/1_discovery/1_overview/brief.md         ← app name, slug, complexity_tier
_concept/2_experience/2_features/**/*.md          ← feature list and priorities
_concept/3_blueprint/3_datamodel/model.json       ← data model
_concept/3_blueprint/1_techstack/stack.md         ← tech stack (drives expert skill selection)
_concept/2_experience/3_screens/**/*.md           ← screen specs (for verification)
? _concept/3_blueprint/2_architecture/architecture.md
```

## _implementation/ Folder Layout

```
_implementation/
├── PLANS.md              ← durable implementation plan (phases, feature checklist)
├── progress.json         ← machine-readable feature status
├── decisions.md          ← dated implementation decisions
└── <framework-specific>/ ← scaffold output (managed by foundation/scaffold skills)
```

## progress.json Format

```json
{
  "schema_version": "1.0",
  "app": "<slug>",
  "updated_at": "YYYY-MM-DDTHH:MM:SSZ",
  "features": [
    {
      "id": "<group>/<feature-slug>",
      "label": "<Human Label>",
      "status": "not_started | in_progress | implemented | verified",
      "impl_status": "pending | scaffolded | coded | tested",
      "last_updated": "YYYY-MM-DD"
    }
  ]
}
```

## Pipeline Phase Structure

| Phase | Group | Skills involved |
|-------|-------|-----------------|
| Orchestrate | `00_orchestrator` | orchestrator (drives all phases) |
| Setup | `10_setup` | foundation, scaffold, infrastructure |
| Features | `20_features` | feature (per-feature TDD loop), page |
| Verify | `30_verify` | verify (spec compliance + quality gate) |
| Utilities | `utilities` | migrate, seed, scaffold, generate |

## Expert Skill Discovery

During implementation, skills search for matching `skailup-prog-expert-*` skills:

1. Read `_concept/3_blueprint/1_techstack/stack.md`
2. Search paths: `.claude/skills/`, `.agents/skills/`, paths in `pipeline.json` `config.expert_search_paths`
3. If found: include expert SKILL.md in subagent context
4. Expert recipes guide implementation patterns

## Git Workflow (Saxe lineage)

- Feature branches: `feat/<feature-slug>`
- Commit format: `feat(<group>): <description>`
- Merge only after `verify` step passes

## PLANS.md (Implementation Phase)

```markdown
## Implementation Plan: <App Name>

### Stack
- Framework: <framework>
- Profile: <profile>

### Phases
- [x] scaffold — completed YYYY-MM-DD
- [x] foundation — completed YYYY-MM-DD
- [ ] feature/user_auth — in_progress
- [ ] feature/dashboard — not_started
...

### Decisions
### Blockers
```
