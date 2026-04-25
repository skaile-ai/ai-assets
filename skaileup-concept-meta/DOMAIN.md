---
name: skaileup-concept-meta
description: "Operating on concepts as a whole — quality review, feature extension, reverse engineering, concept evaluation, and multi-product umbrella concepts. Works across all _concept/ artifact types."
type: domain
building_blocks:
  contracts: "n/a — to be populated after skill migration."
  docs: "n/a — to be populated after skill migration."
  skills: "Concept review, feature extension, reverse engineering, evaluation, project overview, subsystem mapping, integration architecture, and project review skills."
  tools: "n/a"
stage: alpha
---

# skaileup-concept-meta

Operating on concepts as a whole — quality review, feature extension, reverse engineering, concept evaluation, and multi-product umbrella concepts. Works across all `_concept/` artifact types. Unlike domain-specific skills that produce a single artifact type, concept-meta skills read and reason over the full concept structure.

Skills will be moved into this domain during the architecture reorganization (Phase 5.2+).

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | Invocable skills (see below) |

## Skills (target)

| Skill | Purpose |
|-------|---------|
| `skailup-review/` | Quality review of the full concept — consistency, completeness, and coherence |
| `skailup-add-feature/` | Extends an existing concept with a new feature across all relevant artifact layers |
| `skailup-reverse-engineer/` | Produces a structured concept from an existing codebase or product |
| `skailup-eval-concept/` | Evaluates the overall concept against golden principles and iron laws |
| `skailup-eval-feature/` | Evaluates a specific feature spec for quality and feasibility |
| `skailup-eval-product/` | Evaluates the product concept against market and user fit criteria |
| `skailup-project-overview/` | Generates a high-level project overview across all concept artifacts |
| `skailup-subsystem-map/` | Maps subsystems, modules, and their dependencies across the concept |
| `skailup-integration-arch/` | Designs integration architecture between subsystems or external services |
| `skailup-project-review/` | Full project review spanning concept, implementation, and quality artifacts |

## Conventions

- Skills in this domain operate on a fully or partially populated `_concept/` directory; they do not produce initial artifacts.
- `skailup-review` and `skailup-eval-*` skills are non-destructive; they produce evaluation reports, not concept edits.
- `skailup-add-feature` and `skailup-reverse-engineer` modify or create `_concept/` content and should be run with an explicit project target.
