---
name: skaileup-blueprint
description: "Technical decisions and data modeling — technology stack selection, system architecture, and data model design. Produces the technical foundation that implementation skills consume."
type: domain
building_blocks:
  contracts: "n/a — to be populated after skill migration."
  docs: "n/a — to be populated after skill migration."
  skills: "Technology stack selection, system architecture, and data model design skills."
  tools: "n/a"
stage: alpha
---

# skaileup-blueprint

Technical decisions and data modeling — technology stack selection, system architecture, and data model design. Produces the technical foundation that implementation skills consume. Blueprint skills translate discovery and experience artifacts into binding technical decisions that the build domain depends on.

Skills will be moved into this domain during the architecture reorganization (Phase 5.2+).

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | Invocable skills (see below) |

## Skills (target)

| Skill | Purpose |
|-------|---------|
| `skailup-techstack/` | Evaluates and selects the technology stack for the project |
| `skailup-architecture/` | Designs the system architecture: modules, services, and integration points |
| `skailup-datamodel/` | Designs the data model: entities, relationships, and storage strategy |

## Conventions

- Blueprint skills depend on `_concept/10_discovery/` and optionally `_concept/20_experience/`; run discovery first.
- Run skills in order: techstack → architecture → datamodel; each step refines the decisions of the prior step.
- Output is written to `_concept/30_blueprint/`; all skaileup-build skills read from this path before generating code.
