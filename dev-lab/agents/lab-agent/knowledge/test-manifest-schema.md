# Test Manifest Schema

The `test-manifest.yaml` file defines how a skill is tested.

```yaml
version: 1
skill: prog-expert-nuxt
stack: nuxt
stack_override:
  recipe-drizzle: nuxt-full
defaults:
  timeout: "180s"
  model: haiku
  metrics: [build, typecheck]
cases:
  - id: recipe-setup
    recipe: recipes/setup.md
    type: recipe
  - id: atomic-usefetch
    type: atomic
    prompt: "Create a Nuxt page that fetches data using useFetch"
    metrics: [build, typecheck, lint]
guards:
  - metric: typecheck
    condition: pass
  - metric: lint
    condition: "score >= 80"
```

## Test Case Types

- **recipe** — Run a full recipe from the recipes/ directory
- **atomic** — Run a single prompt to test specific capabilities
- **reference** — Compare against a known-good reference implementation
- **learned** — Auto-generated from observation patterns

## Guard Conditions

- `pass` — Metric must pass (gate type)
- `score >= N` — Quality metric must be at least N
- `duration < Ns` — Metric must complete within N seconds
