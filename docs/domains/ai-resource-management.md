---
title: ai-resource-management
description: Meta-management of the skill ecosystem — scaffolding new skills, domains, and CLI tools, and navigating the asset catalog.
---

This domain owns the meta-layer of the skill ecosystem: the tools and skills used to create, structure, and navigate other skills. It defines the conventions all other domains follow and provides the workflow for extending the ecosystem with new capabilities.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | `skill-builder-contract` — skill folder layout, frontmatter fields, and progressive disclosure loading for all skill-creation workflows |
| `docs/` | Architecture notes on how the skill system is structured |
| `skills/` | `skill-builder`, `domain-builder`, `uv-cli-implementer`, `ai-resource-navigator` |
| `agents/skaile/` | Root orchestrator GitAgent — routes by intent to domain agents |
| `tools/` | `scaffold_skill.py`, `scaffold_domain.py` — uv-runnable scripts invoked by skills |

## Skills

| Skill | When to use |
|---|---|
| `skill-builder` | Create a new SKILL.md from scratch with guided frontmatter and body sections |
| `domain-builder` | Create a new domain directory with DOMAIN.md and folder structure |
| `uv-cli-implementer` | Build a uv-runnable Python CLI tool that supports a skill |
| `ai-resource-navigator` | Browse the catalog, search by keyword, install and deploy skills |

## Agent

`agents/skaile/` is the root orchestrator. It routes user intent to the appropriate domain agent (concept pipeline, implementation pipeline, quality, etc.). Start here when you want the agent to decide which domain to activate.
