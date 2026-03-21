# Recipe: Standalone Skill

A skill that runs independently with gate checks, self-collects inputs, and suggests next steps.

## SKILL.md Structure

```markdown
---
name: skill_datamodel
description: "Use when you need to generate a data model from features. Also when user says 'create entities', 'define the database schema'."
keywords: [datamodel, entities, database, schema, dbml]
user_inputs:
  dialog:
    - id: db_preference
      label: "Database preference"
      type: select
      options: [PostgreSQL, SQLite, MySQL, "No preference"]
      default: "No preference"
      required: false
  files:
    - "03_features/"
---

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** `03_features/` must exist, `05_techstack/stack.md` must exist
**If gates fail:** Run `skill_features` and `skill_techstack` first
**On completion:** Present summary, then suggest next steps.

## Workflow

### Step 1: Gate Check
1. Read pipeline.json for this step's hard_gates
2. Verify all gates pass (file existence)
3. If any fail: name missing prerequisites, tell user which skill to run

### Step 2: Self-Collect Inputs
1. Check user_inputs.dialog — ask user directly if running standalone
2. Check user_inputs.files — verify input files exist
3. Adapt question depth to complexity setting

### Step 3: Read Context
1. Read all depends_on folders
2. Optionally read _grounding/, _standards/ if they exist

### Step 4: Execute Workflow
[... skill-specific logic ...]

### Step 5: Completion Summary
Present to user:
- Files created/modified
- Key decisions made
- Suggest next unblocked steps

## Integration
- **Called by:** orchestrator or standalone
- **Reads from:** 03_features/, 05_techstack/, _standards/ (optional)
- **Writes to:** 06_datamodel/
- **Feedback loops:** Updates 03_features/ with data_entities[]
```

## Key Patterns

### Self-Collect Inputs
```
1. Check pipeline.json for user_inputs.dialog
2. Check if _concept/ already has the data
3. For missing: ask user directly
4. Adapt depth:
   - simple: suggest defaults, minimal questions
   - moderate: 3-5 focused questions
   - complex: explore edge cases, multiple rounds
```

### Next-Step Suggestion
After completion:
```
1. Read pipeline.json for all steps
2. Find steps that depend on this one
3. Check which now have all hard_gates satisfied
4. Present: "You can now run: skill_screens, skill_implement"
5. Show what's still blocked and why
```

### Communication Style
```
- simple: "Data model done. 5 entities. Run screens next?"
- moderate: "Created 5 entities with 8 relationships. Key decision: used UUID for all IDs.
  Next: skill_screens (unblocked) or skill_implement (needs screens first)."
- complex: "Data model complete. 5 entities, 8 relationships, 4 seed scenarios.
  Entities: user, project, task, comment, attachment.
  Key decisions: UUIDs, soft deletes, polymorphic attachments.
  Cross-refs updated: 12 features now have data_entities[].
  Next steps: ..."
```
