# skaileup-conceptualization

This domain covers everything from initial project discovery through experience design to technical blueprint. Skills in this domain take you from a problem statement to a fully-specified concept ready for implementation.

## Skill Groups

| Group | Path | Purpose |
|---|---|---|
| **skailup-orchestrator** | `skills/skailup-orchestrator/` | Pipeline controller — dispatches skills, manages user communication |
| **discovery** | `skills/10_discovery/` | Project overview, research, brand identity |
| **experience** | `skills/20_experience/` | Feature specs, user journeys, screen designs, storybook |
| **blueprint** | `skills/30_blueprint/` | Tech stack, architecture, data model |
| **skailup-add-feature** | `skills/skailup-add-feature/` | Add a single new feature to an existing concept |
| **skailup-reverse-engineer** | `skills/skailup-reverse-engineer/` | Ingest an existing codebase and produce a full concept |
| **skailup-review** | `skills/skailup-review/` | Concept quality audit and cross-reference check |

## CF vs Saxe Variants

Each group contains `cf/` (Concept Forge) and `saxe/` (Saxe platform) subdirectories.

## Flows

Multi-step flows that chain these skills are in `flows/`:
- `mvp.json` — discovery + experience + blueprint + implementation
- `prototype.json` — concept + screens only
- `concept-only.json` — full concept without implementation
- `cli-app.json` — CLI application variant
- `product.json` — full product with all optional steps

## Output

Skills write to a `_concept/` directory in the project:
```
_concept/
├── 01_project/        → brief.md, goals.md, comparable.md
├── _grounding/        → research findings
├── 03_features/       → feature specs
├── 04_brand/          → visual identity, tokens.json
├── 05_techstack/      → stack.md
├── 05b_architecture/  → architecture.md
├── 06_datamodel/      → model.dbml, model.json
└── 07_screens/        → screen specs
```
