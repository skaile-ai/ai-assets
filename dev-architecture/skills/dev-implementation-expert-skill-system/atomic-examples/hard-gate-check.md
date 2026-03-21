# Hard Gate Check Pattern

Skills check their own gates when running standalone:

```typescript
// Read pipeline.json for this step's definition
const pipeline = JSON.parse(readFileSync('pipeline.json', 'utf-8'))
const myStep = pipeline.steps.find(s => s.id === 'my_skill')

// Check all hard gates
const failedGates = myStep.hard_gates.filter(gate => {
  if (gate.type === 'file_exists') {
    return !existsSync(join(conceptDir, gate.path))
  }
  return false
})

if (failedGates.length > 0) {
  const missing = failedGates.map(g => g.path).join(', ')
  console.log(`Cannot run: missing prerequisites: ${missing}`)
  console.log(`Run the prerequisite skill first.`)
  process.exit(1)
}
```

In SKILL.md, this appears as:

```markdown
## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** `03_features/` must exist, `05_techstack/stack.md` must exist
**If gates fail:** Run `skill_features` and `skill_techstack` first
**On completion:** Present summary, then suggest next steps.
```
