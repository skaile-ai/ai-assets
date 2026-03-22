---
name: skill-name
description: "Use when [triggering conditions — symptoms, user phrases, workspace state]. NOT a workflow summary."
license: MIT
metadata:
  version: "1.0.0"
  tags: [keyword1, keyword2]
  stage: alpha
  source: MERGED           # CF | SAXE | MERGED | MIGRATED — omit for new skills
  requires:
    - contract-name        # bare string for same-resource contract dependencies
  user_inputs:
    dialog:
      - id: input_name
        label: "Human-readable label"
        type: text | select | multiselect | boolean | number
        required: true | false
        options: []   # for select/multiselect only
        default: null
        hint: "Help text for UI forms"
    files: []   # _concept/ paths this skill needs as pre-supplied input
  reads_from: []    # _concept/ paths this skill reads
  writes_to: []     # _concept/ paths this skill creates or modifies
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

## Prerequisites

**REQUIRED BACKGROUND:** Read `dev-shared/contracts/[contract].md` before proceeding.

<HARD-GATE>
Check that every path listed in this skill's `requires` field exists in `_concept/`.

```
requires:
  - 2_experience/2_features/     ← must exist
  - 3_blueprint/1_techstack/stack.md  ← must exist
```

Stop immediately and name the missing prerequisite skill if any check fails.
</HARD-GATE>

## Context Budget
**Must read:** [list of _concept/ files/folders]
**Optional:**
- `_concept/[path]/` _(fallback: empty_default)_ — [description]
- `_concept/[path]/` _(fallback: skip_if_absent)_ — [description]

`empty_default` = proceed with no data from this source.
`skip_if_absent` = omit the section entirely if the path doesn't exist.

**Never load:** [list of files/folders that are not needed for this step]

## Standalone Mode
This skill can be invoked directly without the orchestrator.

**Gate check:** [list from requires above]
**If gates fail:** [which skill to run first]
**On completion:** Present summary, then suggest next steps (which skills are now unblocked).

## Workflow
1. Check `_grounding/{grounding_folder}/user_input.json` — if answers already collected, skip asking
2. [Step]
3. [Step]

## Outputs
Declared in the flow node's `writes` field. This skill writes to:
- `_concept/XX_folder/file.md` — description
- `_concept/XX_folder/file.json` — description

## Feedback Loops
Declared in the flow node's `feedback` field. After completing its own writes, this skill:
- [Describe upstream files it modifies and what field it sets]

See `dev-shared/contracts/feedback_loop.md` for the full protocol.

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

## Research Mode
When `research_depth` is not `skip` for this step:
- [What to research in parallel]
- Results go to `_grounding/{grounding_folder}/` and `_grounding/general/` for cross-cutting findings
