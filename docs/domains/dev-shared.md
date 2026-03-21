---
title: dev-shared
description: Shared contracts, documentation, and scripts that all skills across all domains read — the single source of truth for cross-cutting conventions.
---

The single source of truth for cross-cutting conventions. **Not invoked directly** — all domains read from it. Defines the vocabulary, rules, and file structures that skills must follow to remain interoperable.

CF and Saxe contracts have been merged into a unified vocabulary at `contracts/` root. The original `cf/` and `saxe/` subdirectories are kept as legacy archives. Documentation in `docs/` is still split pending a merge pass.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | Merged contracts — single source of truth |
| `contracts/cf/` | Legacy CF originals — archive only, do not reference in new skills |
| `contracts/saxe/` | Legacy Saxe originals — archive only |
| `docs/cf/` | CF architecture docs and observability specs |
| `docs/saxe/` | Saxe architecture and observability docs |
| `scripts/` | Shared Python linting/validation scripts available to all skills |
| `flow.schema.json` | JSON Schema for flow definition files |

## Contracts Reference

| Contract | What it defines |
|---|---|
| `skill_template.md` | Canonical SKILL.md template — all new skills start from this |
| `frontmatter.md` | Standard YAML frontmatter fields per file type |
| `concept_structure.md` | `_concept/` path conventions, naming rules, read direction |
| `golden_principles.md` | Mechanical rules enforced by lint (entities, enums, numbering, naming) |
| `iron_laws.md` | Non-negotiable constraints (e.g. NO DATA MODEL WITHOUT FEATURES) |
| `feedback_loop.md` | Two-way cross-reference protocol (features ↔ screens, model → features) |
| `semantic_types.md` | Stack-independent data types + translation table |
| `agent_patterns.md` | Reusable workflow patterns (standalone mode, research mode, subagent dispatch) |
| `plans.md` | PLANS.md format (concept plan + implementation plan + decisions log) |
| `seed_data.md` | Scenario-based seed data conventions (empty/single_user/populated/edge_cases) |
| `skill_testing.md` | Example fixtures + `_validation.json` format for skill self-testing |
| `skill_grammar.md` | Skill body section vocabulary and writing rules |
| `acceptance_criteria.md` | How to write and verify acceptance criteria |
| `MIGRATION.md` | Migration status — which skills have been merged, deprecated, or moved |
