# dev-shared

Shared contracts, documentation, and scripts used by all skills across all domains. **Nothing in this folder is invocable** — it is reference material only.

## Structure

```
dev-shared/
├── DOMAIN.md
├── contracts/                    ← merged contracts (use these)
│   ├── concept_structure.md
│   ├── frontmatter.md
│   ├── golden_principles.md
│   ├── iron_laws.md
│   ├── agent_patterns.md
│   ├── feedback_loop.md
│   ├── semantic_types.md
│   ├── skill_template.md
│   ├── skill_testing.md
│   ├── skill_grammar.md
│   ├── acceptance_criteria.md
│   ├── plans.md
│   ├── flows.md
│   ├── seed_data.md
│   ├── MIGRATION.md              ← path/field changes from CF/Saxe originals
│   ├── cf/                       ← legacy originals (archive, do not reference)
│   └── saxe/                     ← legacy originals (archive, do not reference)
├── docs/
│   ├── cf/                       ← CF architecture + observability docs
│   └── saxe/                     ← Saxe architecture + observability docs
└── scripts/                      ← shared Python linting tools
    ├── lint_concept.py
    ├── validate_skill_rules.py
    └── validator_lib.py
```

## contracts/ (merged)

All skills reference contracts at the root of `contracts/` — not the `cf/` or `saxe/` subdirectories.

| File | Purpose |
|---|---|
| `concept_structure.md` | Canonical `_concept/` paths, naming rules, read direction |
| `frontmatter.md` | Standard YAML fields per file type |
| `golden_principles.md` | Mechanical rules enforced by lint (entities, enums, naming) |
| `iron_laws.md` | Non-negotiable constraints (e.g., NO DATA MODEL WITHOUT FEATURES) |
| `agent_patterns.md` | Reusable patterns: standalone mode, subagent dispatch, research mode |
| `feedback_loop.md` | Cross-reference protocol (features ↔ screens, model → features) |
| `semantic_types.md` | Stack-independent types + translation table |
| `skill_template.md` | SKILL.md template for new skills |
| `skill_testing.md` | Example fixtures + `_validation.json` format for skill self-testing |
| `skill_grammar.md` | MUST/NEVER/CHECKLIST DSL for skill instructions |
| `acceptance_criteria.md` | EARS format acceptance criteria (When/Then/So that) |
| `plans.md` | PLANS.md format (concept plan + implementation plan + decisions log) |
| `flows.md` | Multi-step flow definition format |
| `seed_data.md` | Scenario-based seed data conventions |
| `MIGRATION.md` | Structural changes from legacy CF/Saxe paths — use when handling older projects |

## contracts/cf/ and contracts/saxe/ (legacy archives)

The original pre-merge source files. Do not reference these in new skills. They are kept for:
- Diffing against the merged versions if discrepancies arise
- Supporting projects created with older tooling (see `MIGRATION.md`)

## docs/

Architecture and observability documentation. Still in `cf/` and `saxe/` subdirectories pending a docs merge pass.

| Path | Contents |
|---|---|
| `docs/cf/ARCHITECTURE.md` | CF pipeline boundaries and data flow |
| `docs/cf/OBSERVABILITY.md` | Structured event requirements (started, checkpoint, completed, etc.) |
| `docs/cf/SKILLS.md` | CF skill overview |
| `docs/saxe/ARCHITECTURE.md` | Saxe platform architecture |
| `docs/saxe/OBSERVABILITY.md` | Saxe observability spec |

## scripts/

Shared Python linting and validation scripts available to all quality and implementation skills:

| Script | Purpose |
|---|---|
| `lint_concept.py` | Validates `_concept/` structure and frontmatter |
| `validate_skill_rules.py` | Validates skill grammar (MUST/NEVER/CHECKLIST rules) |
| `validator_lib.py` | Shared validation utilities used by both scripts |
