# dev-implementation

This domain covers the full implementation lifecycle: project scaffolding, feature-by-feature development (TDD), and verification. Skills read from `_concept/` and write production code.

## Skill Groups

| Group | Path | Purpose |
|---|---|---|
| **orchestrator** | `skills/00_orchestrator/` | Implementation pipeline controller |
| **setup** | `skills/10_setup/` | Project scaffold, foundation setup, infrastructure |
| **features** | `skills/20_features/` | Per-feature implementation (TDD-first) |
| **verify** | `skills/30_verify/` | Verification: unit tests, E2E, visual regression |
| **utilities** | `skills/utilities/` | Migration, seed data, code generation |

## CF vs Saxe Variants

- `cf/` variants are stack-agnostic
- `saxe/` variants are PostXL/NestJS-specific


## Key Patterns

- **Journey-first** (Saxe): Implement features in user journey order, not technical order
- **TDD Guard** (Saxe): Write tests before implementation, enforce before proceeding
- **Brand token application** (Saxe): Foundation step applies `04_brand/tokens.json` to theme config
- **Parallel sub-agents** (Saxe audit): Audit uses 3 simultaneous sub-agents for speed

## Output

Skills write to the project codebase. The implementation structure (from Saxe) is:
```
_implementation/
├── decisions/     → ADRs and tech decisions
├── progress/      → feature completion tracking
└── issues/        → blocked items
```
