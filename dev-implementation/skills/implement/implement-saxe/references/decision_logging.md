# Decision Logging

Record significant implementation decisions in `_implementation/decisions.md`.

## Template

```markdown
## YYYY-MM-DD: <Decision Title>

**Context:** <what prompted this decision>
**Decision:** <what was decided>
**Reasoning:** <why>
**Impact:** <what this affects>
```

## Common Decision Points

- Deviating from a screen spec (component not available, better UX found)
- Adding a dependency not in the tech stack
- Splitting or combining features differently than the concept
- Deferring a requirement to a later phase
- Skipping a soft prerequisite (Docker, agent-browser)
- Schema changes during feature implementation
- Conflict resolution choices during re-generation
