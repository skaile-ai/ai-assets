---
name: skaileup-conceptualization
description: "Structured project concept pipeline: from initial discovery through experience design, technical blueprint, and feature specification, to produce a complete _concept/ artifact folder."
type: domain
building_blocks:
  contracts: "Shared contracts (concept_structure.md, frontmatter.md, golden_principles.md, iron_laws.md, feedback_loop.md, semantic_types.md) governing all concept artifacts."
  docs: "Pipeline architecture, observability specifications, and workflow guides for the conceptualization process."
  skills: "Numbered skill groups (10_discovery, 20_experience, 30_blueprint) plus top-level orchestrator, add-feature, reverse-engineer, and review skills."
  agents: "agents/skailup-conceptualize/ (GitAgent spec v0.1.0)"
  flows: "Multi-step flow definitions (cli-concept, concept-only, prototype, reverse-engineer) consumed by the CLI."
  tools: "n/a"
stage: alpha
---

# Dev Conceptualization

This domain implements the full project conceptualization pipeline — from a raw project idea through structured discovery, experience design, technical blueprinting, and review — producing a versioned `_concept/` artifact folder that downstream implementation domains consume.

Skills are organized into numbered groups reflecting pipeline order. Each group can run standalone (checking its own gates) or be orchestrated end-to-end. All skills are unified — there are no per-variant subdirectories.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Shared contracts for artifact structure, frontmatter, naming, cross-references, and semantic types |
| `docs/` | Pipeline architecture, observability specs, workflow guides |
| `skills/` | Numbered skill groups plus top-level skills (see below) |
| `flows/` | Multi-step flow definitions (`.flow.yaml`) consumed by the CLI |
| `agents/` | Agent specs for orchestrated runs |

## Skill Groups

| Group / Skill | Name | Produces |
|---------------|------|----------|
| `skills/skailup-orchestrator/` | Pipeline controller | Dispatches skills, manages user communication |
| `skills/10_discovery/skailup-overview/` | Project overview | `01_project/` brief, goals, comparables |
| `skills/10_discovery/skailup-research/` | Project research | `_grounding/` research artifacts |
| `skills/10_discovery/skailup-brand-behavioral/` | Behavioral brand identity | Brand voice and interaction principles |
| `skills/10_discovery/skailup-brand-visual/` | Visual brand identity | Color, typography, visual style |
| `skills/20_experience/skailup-features/` | Feature definition | `03_features/` feature specs |
| `skills/20_experience/skailup-behaviors/` | Behavior design | `03b_behavior/` behavior specs |
| `skills/20_experience/skailup-journeys/` | User journeys | Journey maps and flow diagrams |
| `skills/20_experience/skailup-screens/` | Screen design | `07_screens/` screen definitions |
| `skills/20_experience/skailup-screens-technical/` | Technical screen specs | Developer-ready screen annotations |
| `skills/20_experience/skailup-components/` | Component library | UI component definitions |
| `skills/20_experience/skailup-mock/` | Mockups | Visual mockups from screen definitions |
| `skills/20_experience/skailup-storybook/` | Storybook setup | Storybook configuration |
| `skills/20_experience/skailup-storybook-setup/` | Storybook initialisation | Base Storybook project scaffold |
| `skills/20_experience/skailup-storybook-components/` | Storybook component stories | Component stories |
| `skills/20_experience/skailup-storybook-journeys/` | Storybook journey stories | Journey-level stories |
| `skills/20_experience/skailup-storybook-pages/` | Storybook page stories | Page-level stories |
| `skills/30_blueprint/skailup-techstack/` | Tech stack selection | `05_techstack/` decisions |
| `skills/30_blueprint/skailup-architecture/` | System architecture | `05b_architecture/` architecture doc |
| `skills/30_blueprint/skailup-datamodel/` | Data model | `06_datamodel/` schema and seed |
| `skills/30_blueprint/skailup-storybook-types/` | Storybook type definitions | Shared TypeScript types for stories |
| `skills/skailup-add-feature/` | Incremental feature | Extend existing concept with a new feature |
| `skills/skailup-reverse-engineer/` | Reverse engineering | Derive full `_concept/` from an existing codebase |
| `skills/skailup-review/` | Quality review | Structure audit, cross-reference repair, readiness gate |

## Flows

| File | Purpose |
|------|---------|
| `flows/cli-concept.flow.yaml` | CLI-app concept flow |
| `flows/concept-only.flow.yaml` | Concept-only run (no implementation) |
| `flows/prototype.flow.yaml` | Prototype flow through discovery and experience |
| `flows/reverse-engineer.flow.yaml` | Reverse-engineer flow from existing codebase |

## Conventions

- Numbered group prefixes (`10_`, `20_`, `30_`) indicate pipeline order, not execution requirement
- File existence in `_concept/` is the only gate between pipeline steps
- All artifacts use YAML frontmatter with `last_updated` and `cross_refs` fields
