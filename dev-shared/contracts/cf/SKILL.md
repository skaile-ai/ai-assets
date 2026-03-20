---
name: cf__shared
description: "Shared contracts for the Concept Forge skill suite. REQUIRED dependency — install this alongside any cf_* skill. Contains pipeline definition, concept folder structure, frontmatter rules, semantic types, iron laws, profiles, and agent patterns that all cf_* skills read before operating."
metadata:
  type: system
  required_by: all cf_* skills
  do_not_invoke: true
---

# Concept Forge — Shared Contracts

This is the shared contract library for the Concept Forge skill suite.
**Do not invoke this skill directly.** It is a dependency package — every
`cf_*` skill reads from this directory before doing any work.

## Contents

| File | Purpose |
|------|---------|
| `pipeline.json` | Machine-readable dependency graph, phases, step definitions, routes, complexity presets, standalone mode |
| `concept_structure.md` | Canonical `_concept/` folder layout, naming rules, read direction |
| `frontmatter.md` | YAML frontmatter fields per file type |
| `semantic_types.md` | 18 stack-independent data types + translation table |
| `feedback_loop.md` | Two-way cross-reference protocol (features ↔ screens, model → features) |
| `iron_laws.md` | Non-negotiable pipeline constraints |
| `golden_principles.md` | Mechanical rules enforced by lint and review |
| `agent_patterns.md` | Reusable workflow patterns (standalone mode, self-collect inputs, communication style, subagent dispatch) |
| `plans.md` | PLANS.md format and rules |
| `snapshots.md` | DEPRECATED (was tied to approval mechanism) |
| `profiles.json` | Reusable configuration presets with inheritance |
| `seed_data.md` | Scenario-based seed data convention |
| `skill_template.md` | Template for creating new cf_* skills |
| `skill_testing.md` | Example fixtures and validation format |
