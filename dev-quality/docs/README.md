# quality

This domain contains quality assurance skills: static audit, end-to-end testing, readiness gates, and test generation.

## Skills

| Skill | Path | Sources | Purpose |
|---|---|---|---|
| **audit** | `skills/audit/` | CF + Saxe | Static code analysis + concept structure audit |
| **e2e** | `skills/e2e/` | CF + Saxe | End-to-end browser tests |
| **ready** | `skills/ready/` | CF + Saxe | Pre-flight readiness gate before next pipeline step |
| **sync** | `skills/sync/` | CF only | Cross-reference repair (features ↔ screens, model → features) |
| **test-unit** | `skills/test-unit/` | CF only | Unit test generation |
| **test-integration** | `skills/test-integration/` | CF only | Integration test generation |
| **test-plan** | `skills/test-plan/` | CF only | Test plan document → `08_testing/test_plan.md` |
| **compile-validators** | `skills/compile-validators/` | Saxe only | Compile all `validator.py` files into a unified validation suite |

## Notable Patterns

- **Saxe audit**: Uses 3 parallel sub-agents (structure + code + dependency analysis simultaneously)
- **Saxe e2e**: Adds screenshot diffing and database state validation
- **compile-validators**: A compelling Saxe pattern — gather all skill validators and run them as a suite. Candidate for adoption across all skills.

## CF vs Saxe Variants

`audit/`, `e2e/`, `ready/` each have `cf/` and `saxe/` subdirectories. See `MERGE_CANDIDATES.md` for merge priorities.
