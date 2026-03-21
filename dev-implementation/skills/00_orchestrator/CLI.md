# implement CLI

## Trigger

Invoke with: "implement the app", "build the app from concept", "start implementation",
"continue implementation", "build it", or "implement <app-name>".

## Modes

- **New implementation**: reads `_concept/`, creates PLANS.md, runs all phases
- **Resume**: detects existing `_implementation/PLANS.md`, resumes from last phase
- **Journey-first**: features built in hero → vital → hygiene order (from `stories.json`)

## Complexity Tiers

| Tier | Checkpoints | When to use |
|------|------------|-------------|
| `small` | Consolidated setup | Simple apps, ≤10 features, no custom backend |
| `standard` | Scaffold+Startup, Foundation, Groups | Most apps |
| `complex` | Every phase separate | Complex apps, custom infrastructure, large feature sets |

## Output

- `_implementation/PLANS.md` — implementation plan with phase and feature checkboxes
- `_implementation/progress.json` — machine-readable status tracking
- `_implementation/decisions.md` — decision log
- `LEARNINGS.md` — institutional knowledge journal
- Project source code (via `scaffold`, `foundation`, `implement-feature` sub-skills)
- `_implementation/verification/` — verification reports and screenshots

## Sub-skills dispatched

1. `scaffold` — project setup
2. `foundation` — brand tokens, auth, app shell
3. `infrastructure` — custom backend modules (if architecture.md defines them)
4. `implement-feature` — journey-first feature implementation
5. `verify` — full-stack verification gate

## Next Steps (after completion)

- Merge `implement/<app-slug>` branch to `main`
- Deploy to staging
- Run `audit` for static code analysis
