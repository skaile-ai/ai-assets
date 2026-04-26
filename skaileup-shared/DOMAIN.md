---
name: skaileup-shared
description: "Shared contracts, documentation, and scripts that all skills across all domains read — the single source of truth for cross-cutting conventions."
type: domain
building_blocks:
  contracts: "Merged contracts at contracts/ root: concept_structure, frontmatter, golden_principles, iron_laws, agent_patterns, feedback_loop, semantic_types, skill_template, skill_testing, skill_grammar, acceptance_criteria, plans, flows, seed_data, wireframe_conventions, MIGRATION. Legacy cf/ and saxe/ subdirs are archived originals."
  docs: "Architecture docs and observability specs — still split into docs/cf/ and docs/saxe/ pending a docs merge pass."
  scripts: "Shared Python linting/validation scripts: lint_concept.py, validate_skill_rules.py, validator_lib.py."
  agents: "n/a — this domain contains reference material, not invocable skills"
  prompts: "n/a"
  tools: "Shared scripts used by quality and implementation skills."
stage: alpha
---

# Dev Shared

This domain is the single source of truth for cross-cutting conventions. It is **not invoked directly** — all domains read from it. It defines the vocabulary, rules, and file structures that skills must follow to remain interoperable.

Contracts are unified at `contracts/` root. The original `cf/` and `saxe/` subdirectories are kept as legacy archives for reference and to support projects created with older tooling (see `contracts/MIGRATION.md`). Documentation in `docs/` retains `cf/` and `saxe/` subdirectories as separate reference archives.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Merged contracts — single source of truth for all cross-cutting conventions |
| `contracts/cf/` | Legacy CF originals — archive only, do not reference in new skills |
| `contracts/saxe/` | Legacy Saxe originals — archive only, do not reference in new skills |
| `docs/cf/` | CF architecture docs and observability specifications |
| `docs/saxe/` | Saxe architecture and observability docs |
| `scripts/` | Shared Python linting/validation scripts available to all skills |

## Contract

This domain IS the contract layer — there is no separate contract skill. All other domains are its consumers. Skills reference `contracts/<file>.md` directly (not `contracts/cf/` or `contracts/saxe/`).

## Conventions

- Nothing in this domain is invocable
- When a contract changes, all domains that read it must be reviewed for impact
- `contracts/cf/` and `contracts/saxe/` are legacy archives — the merge is complete
- `docs/cf/` and `docs/saxe/` are separate reference archives; consult both if coverage is uncertain
- Scripts here are shared utilities; domain-specific scripts live within their own domain's `tools/`
