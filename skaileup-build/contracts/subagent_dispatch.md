# Subagent Dispatch Contract

Detailed specification for the Implementer Status Report and escalation paths used by
`implement-supervised`. Referenced by all skills in this domain.

This contract extends `skaileup-shared/contracts/agent_patterns.md` Pattern: Subagent Dispatch
with concrete prompt templates and orchestrator handling logic.

---

## Implementer Prompt Template

Every task dispatched to a subagent MUST use this structure. The full task text is pasted
verbatim — the subagent never reads the plan file.

```
You are implementing a single task. Your full context is below.

## Your Task
<paste task text verbatim from superpowers-plan.md>

## Relevant Specs
<paste feature spec(s) for this task>
<paste screen spec(s) for this task>

## Tech Context
<paste relevant section of stack.md>

## Expert Skills Available
<list of matched skaileup-implementation-expert-* skills, if any>

## Rules
- Write failing tests FIRST (TDD Red → Green)
- Do NOT read the full conversation history
- Do NOT proceed if any prerequisite path is missing — report BLOCKED instead
- End your response with a STATUS block (see below)

## Status Block (REQUIRED at end of response)
STATUS: DONE | DONE_WITH_CONCERNS | NEEDS_CONTEXT | BLOCKED

[If DONE_WITH_CONCERNS]  Concerns: <trade-offs, deviations, technical debt>
[If NEEDS_CONTEXT]       Missing:  <what is absent or ambiguous>
                         Question: <one specific question>
[If BLOCKED]             Reason:   <what cannot proceed>
                         Route:    context | escalate-model | decompose
```

---

## Orchestrator Handling Logic

```
RECEIVE status from subagent

CASE DONE:
  - Run auto-review checklist (references/auto_review.md in skaileup-implementation)
  - IF all pass → accept, log approval_method: "auto", advance flow
  - ELSE → surface failing checks to user; do not advance until resolved

CASE DONE_WITH_CONCERNS:
  - Log concerns in _implementation/decisions.md with date + task reference
  - Run auto-review checklist
  - IF all pass → accept, log approval_method: "auto_with_concerns"
  - ELSE → surface concerns + failing checks to user

CASE NEEDS_CONTEXT:
  - Send Question as a standalone message to user (never at end of an update)
  - Pause flow until answer received
  - Re-dispatch subagent with original prompt + answer appended

CASE BLOCKED, route=context:
  - Surface Reason as a standalone question to user
  - Re-dispatch with answer

CASE BLOCKED, route=escalate-model:
  - Log in _implementation/decisions.md: "Task <id> exceeded model tier — re-dispatched"
  - Re-dispatch on higher-tier model (note in flow globals)

CASE BLOCKED, route=decompose:
  - Ask write-plan to decompose the blocked task into smaller sub-tasks
  - Insert sub-tasks into the plan at the current position
  - Resume dispatch from first sub-task
```

---

## Review Gate Order (non-negotiable)

```
Step 1: Spec Compliance Review
  - Read each requirement in the feature spec against the actual code
  - Confirm every acceptance criterion is addressable by a test
  - Result: COMPLIANT | NON_COMPLIANT (with gap list)

Step 2: Code Quality Review (only after Step 1 = COMPLIANT)
  - Test coverage: all code paths have at least one test
  - File boundaries: no feature bleeds into another's files
  - Naming: follows golden_principles.md conventions
  - No artifacts: no console.log, TODO, commented-out blocks
  - Result: PASS | FAIL (with issue list)
```

Running quality review before spec compliance passes is wasted work.
A clean, well-structured implementation of the wrong requirements fails both gates.

---

## Model Tiering Guidance

| Task type | Recommended tier |
|---|---|
| Mechanical (rename, boilerplate, config) | Haiku |
| Standard feature implementation | Sonnet |
| Complex logic, architecture decisions, blocked tasks | Opus |

The `implement-supervised` skill sets `model_tier` per task in `superpowers-plan.md`.
The orchestrator respects this when dispatching subagents.
