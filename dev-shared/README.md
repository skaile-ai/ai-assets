# _shared

Shared contracts, documentation, and scripts used by all skills across all domains.

## Structure

```
_shared/
├── contracts/
│   ├── cf/      ← Original CF shared contracts (do not modify)
│   └── saxe/    ← Original Saxe shared contracts (do not modify)
├── docs/
│   ├── cf/      ← CF architecture and observability docs
│   └── saxe/    ← Saxe docs
└── scripts/     ← Python linting scripts (merged from both sources)
```

## contracts/cf/

Original Concept Forge shared contracts. Key files:

| File | Purpose |
|---|---|
| `concept_structure.md` | Canonical `_concept/` paths, naming rules, read direction |
| `frontmatter.md` | Standard YAML fields per file type |
| `golden_principles.md` | Mechanical rules enforced by lint (entities, enums, naming) |
| `iron_laws.md` | Non-negotiable constraints (e.g., NO DATA MODEL WITHOUT FEATURES) |
| `agent_patterns.md` | Reusable patterns: standalone mode, subagent dispatch, research mode |
| `pipeline.json` | Dependency graph v2, phases, user_inputs, complexity presets |
| `plans.md` | PLANS.md format (concept plan + implementation plan + decisions log) |
| `feedback_loop.md` | Cross-reference protocol (features ↔ screens, model → features) |
| `semantic_types.md` | Stack-independent types + translation table |
| `skill_template.md` | SKILL.md template for new skills |

## contracts/saxe/

Original Saxe shared contracts. Key files:

| File | Purpose |
|---|---|
| `acceptance_criteria.md` | EARS format acceptance criteria (When/Then/So that) |
| `git_workflow.md` | Git branching and commit conventions |
| `implementation_structure.md` | `_implementation/` folder layout |
| `prerequisites.md` | Skill prerequisite gate format |
| `skill_grammar.md` | MUST/NEVER/CHECKLIST DSL for skill instructions |
| `stories_schema.json` | JSON schema for user stories |
| `verification.md` | Verification patterns for implementation skills |

## scripts/

Python scripts merged from both sources:
- `lint_concept.py` (CF) — validates `_concept/` structure and frontmatter
- Any Saxe scripts are merged here without overwriting

## Merge Plan

See `MERGE_CANDIDATES.md` (repo root) — the `_shared contracts` section lists which files need to be merged from `cf/` + `saxe/` into a unified `contracts/` root. Until merged, skills should reference the appropriate source subdirectory.

Merged contracts will live directly in `_shared/contracts/` (not in a `cf/` or `saxe/` subfolder).
