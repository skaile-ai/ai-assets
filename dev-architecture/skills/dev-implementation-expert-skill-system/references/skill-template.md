# Canonical SKILL.md Template

Every skill in a pipeline system should follow this structure.

## Frontmatter

```yaml
---
name: skill_name
description: "Use when [triggering conditions]. Also when user says '[trigger phrases]'."
keywords: [keyword1, keyword2, keyword3]
user_inputs:
  dialog:
    - id: input_name
      label: "Human-readable label"
      type: text | select | multiselect | boolean | number | textarea
      required: true
      options: []           # for select/multiselect
      default: null
      hint: "Help text"
  files:
    - "path/to/required/file.md"
---
```

## Sections

### 1. Overview (2-3 sentences)
What this skill produces and where it fits in the pipeline.

### 2. When to Use
- Bullet list of triggering conditions
- User phrases that should invoke this skill
- Workspace state that indicates this skill is needed

### 3. When NOT to Use
- Skip conditions (other skill is more appropriate)
- Prerequisite failures (run X first)

### 4. Prerequisites

#### HARD-GATE
From pipeline.json — file existence checks that must pass:
```
Required: path/to/required/artifact
Required: path/to/another/artifact
```
If not met: "Cannot run: [missing artifact]. Run [prerequisite skill] first."

### 5. Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | path/to/input | Yes |
| **Optional** | path/to/optional | No (fallback: description) |
| **Never load** | path/to/irrelevant | -- |

This prevents token waste — skills should never load everything.

### 6. Standalone Mode
```
This skill can be invoked directly without the orchestrator.
**Gate check:** [list from pipeline.json hard_gates]
**If gates fail:** Run [prerequisite skill] first
**On completion:** Present summary, then suggest next steps.
```

### 7. Workflow
Numbered steps. Each step should be concrete and verifiable.

### 8. Outputs

| Output | Description |
|--------|-------------|
| `path/to/output.md` | What this file contains |

### 9. Completion Summary
Template for what the user sees after the skill finishes:
```
"[Skill] complete. N files created in [folder].
 Next: run [unblocked skill] to [what it does]."
```

### 10. Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Skipping prerequisites | Agent assumes it knows enough | Always check hard_gates first |

### 11. Integration
- **Called by:** orchestrator or standalone
- **Reads from:** [artifact folders]
- **Writes to:** [artifact folder]
- **Feedback loops:** Updates [upstream file] with [field]

## Anti-patterns to avoid

1. **Status lifecycle in frontmatter** — use file existence, not `status: approved`
2. **Depending on conversation context** — artifacts are the source of truth
3. **Loading everything** — use Context Budget to limit reads
4. **Monolithic skills** — one skill, one artifact folder, one concern
5. **Implicit dependencies** — if you read it, declare it in `depends_on`
