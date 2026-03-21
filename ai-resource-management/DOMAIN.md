---
name: ai-resource-management
description: "Meta-management of the skill ecosystem: scaffolding new skills, domains, and CLI tools, and navigating the asset catalog."
type: domain
building_blocks:
  contracts: "Skill folder structure, SKILL.md frontmatter, CLI.md format, and progressive disclosure loading strategy shared by all skill-creation skills."
  docs: "Architecture notes for the skill ecosystem and how skills relate to each other."
  skills: "Invocable skills for creating skills (skill-builder), creating domains (domain-builder), building CLI tools (uv-cli-implementer), and navigating the asset catalog (ai-resource-navigator)."
  agents: "TBD"
  prompts: "TBD"
  tools: "scaffold_skill.py, scaffold_domain.py — uv-runnable scripts invoked by skill-builder and domain-builder."
stage: alpha
---

# AI Resource Management

This domain owns the meta-layer of the skill ecosystem: the tools and skills used to create, structure, and navigate other skills. It defines the conventions all other domains follow and provides the workflow for extending the ecosystem with new capabilities.

Everything that creates or manages AI assets lives here — from scaffolding a new skill or domain to installing and deploying skills from the catalog.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Shared contracts — the `skill-builder-contract` defines skill folder layout, frontmatter fields, and progressive disclosure loading for all skill-creation workflows |
| `docs/` | Domain documentation — architecture notes on how the skill system is structured |
| `skills/` | Invocable skills — `skill-builder`, `domain-builder`, `uv-cli-implementer`, `ai-resource-navigator` |
| `tools/` | CLI tools — `scaffold_skill.py`, `scaffold_domain.py`; run with `uv run` by skills in this domain |

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
