# Skill Monorepo Cleanup — Orchestrators, Renames, TODO

Date: 2026-03-22

## What changed

### Orchestrators fixed
- **dev-implementation** agent.yaml: `skill_merged` now points to `skills/implement/SKILL.md` (was referencing nonexistent `skills/00_orchestrator/SKILL.md`)
- **dev-conceptualization** agent.yaml: `skill_merged` now points to `skills/orchestrator/SKILL.md` (was referencing nonexistent CF/Saxe variants)
- Created **`dev-conceptualization/skills/orchestrator/SKILL.md`** — MERGED concept orchestrator with 3 phases (Discovery → Experience → Blueprint), DSL format, checkpoint protocol, complexity tiers

### File renames
- 8 flow files: `<name>.json` → `<name>.flow.json` (4 in dev-conceptualization, 4 in dev-implementation)
- 17 package files: `package.yaml` → `<name>.skillpack.yaml` (all domains + 4 meta-packages)

### Devlog
- Created `_devlog/TODO_PESTER.md` — tracks ~80 missing frontmatter fields, experimental skills, stale DOMAIN.md entries, legacy dirs to remove, documentation references to update

## Context

Part of the ongoing CF/Saxe merge and monorepo consolidation. Prior work in this session deleted 34 old skill directories, updated path references in surviving skills, and moved test skills from dev-quality to dev-implementation.
