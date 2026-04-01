---
name: skaileup-conceptualization
description: "Structured project concept pipeline: from initial discovery through experience design, technical blueprint, and feature specification, to produce a complete _concept/ artifact folder."
type: domain
building_blocks:
  contracts: "CF and Saxe shared contracts (concept_structure.md, frontmatter.md, golden_principles.md, iron_laws.md, feedback_loop.md, semantic_types.md) governing all concept artifacts."
  docs: "Pipeline architecture, observability specifications, and workflow guides for the conceptualization process."
  skills: "Numbered skill groups (00–90) covering orchestration, discovery, experience design, blueprint, feature addition, reverse engineering, and review."
  agents: "agents/conceptualization/ (GitAgent spec v0.1.0)"
  prompts: "Reusable prompt fragments for project briefing, user research, and concept elicitation."
  tools: "TBD"
stage: alpha
---

# Dev Conceptualization

This domain implements the full project conceptualization pipeline — from a raw project idea through structured discovery, experience design, technical blueprinting, and review — producing a versioned `_concept/` artifact folder that downstream implementation domains consume.

Skills are organized into numbered groups reflecting pipeline order. Each group can run standalone (checking its own gates) or be orchestrated end-to-end. CF and Saxe variants coexist under `cf/` and `saxe/` subdirectories until formally merged.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Shared contracts — CF and Saxe rules for artifact structure, frontmatter, naming, cross-references, and semantic types |
| `docs/` | Pipeline architecture, observability specs, workflow guides |
| `skills/` | Numbered skill groups (see below) |
| `prompts/` | Reusable prompt fragments for elicitation and briefing |
| `flows/` | Multi-step flow definitions (mvp.json, prototype.json, etc.) consumed by the CLI |

## Skill Groups

| Group | Name | Produces |
|-------|------|----------|
| `00_orchestrator/` | Pipeline controller | Dispatches skills, manages user communication |
| `10_discovery/` | Project discovery | `01_project/` brief, goals, comparables; `_grounding/`; brand/visual identity |
| `20_experience/` | Experience design | `03_features/`, `03b_behavior/`, `04_brand/` |
| `30_blueprint/` | Technical blueprint | `05_techstack/`, `05b_architecture/`, `06_datamodel/`, `07_screens/`, `08_testing/` |
| `40_add-feature/` | Incremental feature | Extend existing concept with a new feature |
| `80_reverse-engineer/` | Reverse engineering | Derive full `_concept/` from an existing codebase |
| `90_review/` | Quality review | Structure audit, cross-reference repair, readiness gate |

## Conventions

- Numbered group prefixes (`00_`, `10_`, ...) indicate pipeline order, not execution requirement
- CF and Saxe variants coexist until merged; merged skills use `source: MERGED`
- File existence in `_concept/` is the only gate between pipeline steps
- All artifacts use YAML frontmatter with `last_updated` and `cross_refs` fields
