---
name: dev-lab-contract
description: Shared interfaces and schemas for the lab system
source: MERGED
version: 1.0.0
keywords: [lab, contract, schema, validation, metrics]
---

# Lab Contract

## Test Manifest Schema

```yaml
version: 1
skill: <skill-id>
stack: <stack-name>
stack_override:
  <case-id>: <alt-stack>
defaults:
  timeout: "180s"
  model: haiku
  metrics: [build, typecheck]
cases:
  - id: <unique-id>
    recipe: recipes/<file>.md
    type: recipe | atomic | reference | learned
    prompt: <optional override prompt>
    metrics: <optional override metrics>
    timeout: <optional override>
    stack: <optional override>
custom_metrics:
  <name>:
    command: <shell command>
    type: gate | quality
    parse: exit_code | vitest_json | eslint_json | pytest_output
guards:
  - metric: <name>
    condition: pass | "score >= N" | "duration < Ns"
```

## Built-in Metrics

| Metric | Command | Type | Parser |
|---|---|---|---|
| build | npm run build | gate | exit_code |
| typecheck | npx nuxi typecheck | gate | exit_code |
| lint | npx eslint . --format json | quality | eslint_json |
| test | npx vitest run --reporter=json | quality | vitest_json |
| db-migrate | npx drizzle-kit push | gate | exit_code |
| python-test | uv run pytest --tb=short -q | quality | pytest_output |
| python-typecheck | uv run mypy . --no-error-summary | gate | exit_code |
| typst-compile | typst compile main.typ | gate | exit_code |

## Quality Score Dimensions

| Dimension | Weight | Description |
|---|---|---|
| correctness | 0.35 | Does it do what the recipe describes? |
| idiomatic | 0.20 | Follows framework conventions? |
| completeness | 0.25 | Edge cases handled? Types correct? |
| minimalism | 0.20 | No unnecessary code? |

## Observation Event Types

- `tool_call` — Agent invoked a tool
- `error` — An error occurred during execution
- `correction` — Agent self-corrected after an error
- `success` — Operation completed successfully

## Mutation Strategies (Priority Order)

1. fix_failing_test — Test case with status `fail`
2. incorporate_correction — Unaddressed entry in learnings/corrections.md
3. update_pattern — Version drift detected
4. strengthen_weak_test — Test passes but quality < 70
5. add_learned_recipe — Approved candidate in learnings/candidates.md
6. improve_prompt — SKILL.md sections correlating with low scores
7. add_missing_example — Recipe without matching atomic example
