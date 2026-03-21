# Patterns

Reusable code patterns for skill system design.

## Pattern: DAG Execution Order

Topologically sort pipeline steps based on `depends_on`:

```typescript
function topologicalSort(steps: Step[]): Step[] {
  const visited = new Set<string>()
  const result: Step[] = []
  const stepMap = new Map(steps.map(s => [s.id, s]))

  function visit(id: string) {
    if (visited.has(id)) return
    visited.add(id)
    const step = stepMap.get(id)
    if (!step) return
    for (const dep of step.depends_on) visit(dep)
    result.push(step)
  }

  for (const step of steps) visit(step.id)
  return result
}
```

## Pattern: Context Budget Enforcement

Skills should declare what they read to prevent token waste:

```markdown
| Action | Path | Required |
|--------|------|----------|
| **Must read** | 03_features/ | Yes |
| **Optional** | _grounding/ | No (fallback: skip) |
| **Never load** | 04_brand/ | -- |
```

Implementation: skill reads ONLY declared paths before starting workflow.

## Pattern: Observability Middleware

Wrap skill execution with event emission:

```typescript
async function runWithObservability(skillName: string, fn: () => Promise<void>) {
  const runId = crypto.randomUUID()
  emit(`[${skillName}] started`, { run_id: runId })
  try {
    await fn()
    emit(`[${skillName}] completed`, { run_id: runId })
  } catch (error) {
    emit(`[${skillName}] failed`, { run_id: runId, error: error.message })
    throw error
  }
}
```

## Pattern: Artifact Folder Convention

```
XX_name/           numbered = execution order, lowercase_snake_case
_special/          underscore prefix = readable by all, not numbered
config.json        root level = machine state
PLANS.md           root level = agent plan
```
