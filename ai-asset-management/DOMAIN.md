---
name: ai-asset-management
description: "Meta-management of the skill ecosystem: scaffolding new skills, domains, and CLI tools, and navigating the asset catalog."
type: domain
building_blocks:
  contracts: "Skill folder structure, SKILL.md frontmatter, CLI.md format, and progressive disclosure loading strategy shared by all skill-creation skills."
  skills: "Invocable skills for creating skills (skill-builder), creating domains (domain-builder), building CLI tools (uv-cli-implementer), and navigating the asset catalog (ai-resource-navigator)."
  agents: "skaile, the root orchestrator: routes by intent to domain agents (the three skaileup pipeline delegates ship from ai-assets-skaileup)."
  prompts: "TBD"
  tools: "scaffold_skill.py (skills/skill-builder/scripts/), scaffold_domain.py (skills/domain-builder/scripts/) — uv-runnable scripts invoked by skill-builder and domain-builder."
stage: alpha
---

# AI Resource Management

This domain owns the meta-layer of the skill ecosystem: the tools and skills used to create, structure, and navigate other skills. It defines the conventions all other domains follow and provides the workflow for extending the ecosystem with new capabilities.

Everything that creates or manages AI assets lives here — from scaffolding a new skill or domain to installing and deploying skills from the catalog.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Shared contracts — the `skill-builder-contract` defines skill folder layout, frontmatter fields, and progressive disclosure loading for all skill-creation workflows |
| `skills/` | Invocable skills — `skill-builder`, `domain-builder`, `uv-cli-implementer`, `ai-resource-navigator`; the scaffold scripts `scaffold_skill.py` / `scaffold_domain.py` sit in `skill-builder/scripts/` and `domain-builder/scripts/` and run with `uv run` |
| `agents/` | `skaile` — the root orchestrator agent: routes by intent to domain agents (see `docs/agents.md`) |

## Contract

The `skill-builder-contract` in `contracts/` is the shared bridge:
- Defines the canonical skill folder structure (`SKILL.md`, `CLI.md`, `resources/`, `examples/`, `scripts/`)
- Specifies required and optional SKILL.md frontmatter fields
- Describes the three-level progressive disclosure loading strategy
- Is `do_not_invoke: true` — loaded as context, never triggered directly

## Skills

| Skill | Purpose |
|-------|---------|
| `skill-builder` | Scaffold and implement a new Agent skill from requirements |
| `domain-builder` | Scaffold a new domain folder with DOMAIN.md and contract skeleton |
| `uv-cli-implementer` | Build single-file Python CLI tools (uv + Typer) for agent invocation |
| `ai-resource-navigator` | Browse, search, install, and deploy skills from the asset catalog |

## Conventions

- All skills in this domain read `skill-builder-contract` before operating
- Skills are named with a clear action verb: `skill-builder`, `domain-builder`, `uv-cli-implementer`, `ai-resource-navigator`
- Tools in `tools/` are single-file Python scripts with PEP 723 inline deps, runnable via `uv run`
- Stage defaults to `alpha` — promote after validation
