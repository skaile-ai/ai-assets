---
name: skill-name
description: "Use when [triggering conditions — symptoms, user phrases, workspace state]. NOT a workflow summary."
keywords: [keyword1, keyword2]
user_inputs:
  dialog:
    - id: input_name
      label: "Human-readable label"
      type: text | select | multiselect | boolean | number
      required: true | false
      options: [] # for select/multiselect only
      default: null
      hint: "Help text for UI forms"
  files: [] # paths to _concept/ files this skill needs as input
---

# Skill Name

## Overview
One to two sentences: what this skill produces and why it matters.

## When to Use
- User says: "..." (symptom phrases)
- Workspace state: `_concept/XX_folder/` is empty or missing

## When NOT to Use
- User explicitly wants to skip this step
- Required upstream artifacts don't exist yet (see Prerequisites)
- [Other conditions]

## Prerequisites

**REQUIRED BACKGROUND:** Read `cf__shared/[contract].md` before proceeding.

<HARD-GATE>
DO NOT proceed unless all `hard_gates` from pipeline.json pass. Hard gates use
structured objects:
```json
"hard_gates": [
  { "type": "file_exists", "path": "03_features/" }
]
```
- [condition from pipeline.json hard_gates]

Stop immediately and name the missing prerequisite skill if any gate fails.
</HARD-GATE>

## Context Budget
**Must read:** [list of files/folders]
**Optional:** [list of files/folders — these map to `optional_reads` in pipeline.json with typed fallbacks]
- `_concept/[path]/` _(fallback: empty_default)_ — [description]
- `_concept/[path]/` _(fallback: skip_if_absent)_ — [description]

`empty_default` = proceed with no data from this source. `skip_if_absent` = omit the section entirely.

**Never load:** [list of files/folders] (not needed for this step)

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** [list from pipeline.json hard_gates]
**If gates fail:** [which skill to run first]
**On completion:** Present summary, then orchestrator suggests next steps.

## Workflow
1. [Step]
2. [Step]
3. [Step]

## Outputs
- `_concept/XX_folder/file.md` — description
- `_concept/XX_folder/file.json` — description

## Completion Summary
Present to user:
- Files produced
- Key decisions made
- Suggested next steps (which skills are now unblocked)

## Common Mistakes

| Rationalization | Reality |
|----------------|---------|
| "This is obvious, I'll skip reading context" | Context prevents duplicate/conflicting decisions. Always read first. |
| "The user already told me what they want" | Structure the information per the contract. Don't shortcut formatting. |

## Integration
- **Called by:** orchestrator or standalone
- **Pairs with:** [skills that run before/after]
- **Requires sub-skill:** [if applicable]
- **Feedback loops:** [upstream files this skill modifies]

## Research Mode
When research_mode is active for this step:
- [What to research in parallel]
- [Where to store findings: _concept/_grounding/general/ or _concept/_grounding/{step}/]
