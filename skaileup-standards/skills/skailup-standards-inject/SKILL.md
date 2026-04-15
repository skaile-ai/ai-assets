---
name: "skailup-standards-inject"
description: "Use before dispatching a skill to load applicable codebase standards. Called by orchestrator or as first step in standalone skill execution. Reads _concept/_standards/index.yml and returns matched standards."
metadata:
  tags:
    - "standards"
    - "inject"
    - "match"
    - "conventions"
    - "context"
  stage: "alpha"
  requires:
    - "standards-contract"
---

# Inject Standards

## Overview
Helper skill that reads `_concept/_standards/index.yml`, matches standards to a
requesting skill by `applies_to` + keyword overlap, and returns matched standard
files as additional context. No error if no standards exist.

## When to Use
- Orchestrator calls this before dispatching a skill (automatic)
- Standalone skill calls this as its first step
- User says "apply standards" or "check conventions"

## When NOT to Use
- No `_concept/_standards/` folder exists (graceful no-op)
- Standards discovery hasn't been run yet

## Prerequisites

<HARD-GATE>
No hard gates. If `_concept/_standards/index.yml` doesn't exist, return empty
result with no error.
</HARD-GATE>

## Shared Contracts

Before starting, read:
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget
**Must read:** `_concept/_standards/index.yml` (if exists)
**Must read:** Matched standard files from index
**Never load:** Unrelated standards, source code

## Standalone Mode
This skill is a helper — typically called by other skills or orchestrator.
**Gate check:** None
**If gates fail:** N/A
**On completion:** Returns matched standards as context to the calling skill.

## Workflow

1. Check if `_concept/_standards/index.yml` exists
   - If not: return empty result, log "No standards discovered yet"
2. Read index.yml
3. Match standards to requesting skill:
   - Filter by `applies_to` containing the requesting skill ID
   - Rank by keyword overlap with the requesting skill's keywords
4. Read matched standard files
5. Return matched standards as context

## Matching Algorithm

```
For each standard in index.yml:
  1. Check if requesting_skill_id in standard.applies_to → strong match
  2. Check keyword overlap between standard.keywords and skill.keywords → ranked match
  3. Return all strong matches + top 5 ranked matches
```

## Outputs
- No files written — returns matched standards as context to caller

## Completion Summary
- Number of standards matched
- Domains covered
- Standards applied (file list)

## Common Mistakes

| Rationalization | Reality |
|----------------|---------|
| "Standards don't exist, I'll error out" | No-op is correct. Standards are optional. |
| "I'll load all standards" | Match only relevant ones. Token budget matters. |

## Integration
- **Called by:** orchestrator (before dispatch), standalone skills (first step)
- **Pairs with:** cf_discover_standards (producer)
- **Feedback loops:** None
