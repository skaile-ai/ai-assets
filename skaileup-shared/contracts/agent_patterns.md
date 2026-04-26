# Agent Patterns

Reusable workflow patterns for skills. Reference these from SKILL.md
instead of duplicating the pattern in each skill.

---

## Pattern: Read-Context-First

Before producing any output:
1. Check all `requires` paths from the flow node (or SKILL.md frontmatter in standalone mode) —
   these are file/folder existence checks. Stop immediately if any required path is missing.
2. Read all files in the required input folders
3. Read optional inputs where present (paths in the skill's READS block marked with `?`):
   - If absent, proceed with the defined fallback (`empty_default` = proceed without that data,
     `skip_if_absent` = omit the section from output)
4. Only THEN start your workflow

**Never skip this.** Even if the user has described what they want in conversation,
the `_concept/` artifacts are the source of truth.

---

## Pattern: Self-Collect Inputs

When a skill needs user input before running:
1. Check the flow node `user_inputs.dialog` fields (or SKILL.md frontmatter `user_inputs` in standalone mode)
2. Check `_concept/_grounding/{grounding_folder}/user_input.json` for pre-collected answers
3. Check if `_concept/` already has the required files/data
4. For missing inputs: ask the user directly using friendly, non-technical language
5. Adapt question depth based on the flow's `globals.verbosity`:
   - **brief**: suggest smart defaults, minimal questions
   - **standard**: 3–5 focused questions, brief trade-off explanations
   - **detailed**: explore edge cases, multiple rounds if needed
6. Proceed with workflow

---

## Pattern: Communication Style

Adapt tone based on the flow's `globals.verbosity`:
- **brief**: Casual, concise, suggest smart defaults. "I'll set up a clean login flow — sound good?"
- **standard**: Balanced, explain trade-offs briefly. "For auth, we can do email+password or add social login. What fits your users?"
- **detailed**: Thorough, explore edge cases. "Let's map out the auth flow. Who are the user roles? What happens with failed logins? Do you need SSO?"

**Never use jargon without explanation.** If you must use a technical term, immediately explain it in parentheses.

### Questions Are Standalone Messages

When you need to ask the user a question, **send ONLY the question as its own message**.
Do not append a question to the end of a status update, progress report, or explanation.

**Wrong:**
> I've analyzed the brief and identified 6 feature groups covering auth, dashboard,
> settings, notifications, billing, and reporting. I've also cross-referenced with
> the competitor analysis. Now I need one thing from you — do you want social login
> or just email+password?

**Right:**
> _(first message — status update, no question)_
> I've analyzed the brief and identified 6 feature groups covering auth, dashboard,
> settings, notifications, billing, and reporting.

> _(second message — question only)_
> Do you want social login or just email+password?

**Why:** Questions buried at the end of long messages get overlooked. A dedicated
question message signals clearly that user input is needed and makes it easy to respond.

---

## Pattern: Standalone Mode

Skills can run independently without the orchestrator:
1. Read the skill's own SKILL.md frontmatter `requires` field
2. Check each required path exists in `_concept/`
3. If ALL pass: read input folders → execute workflow → emit `completed` → suggest next steps
4. If ANY fail: name the missing prerequisites, tell the user which skill to run first

Next-step suggestions in standalone mode come from `next_flows` hints in the relevant
flow file, or from the skill's own knowledge of what it unblocks.

---

## Pattern: Next-Step Suggestion

After a skill completes (standalone or orchestrated):
1. Identify which skills now have all their `requires` paths satisfied
2. Present unblocked skills as suggestions (use `next_flows` from the active flow if available)
3. If no skills are unblocked, show what is still missing and which skill would produce it
4. If the orchestrator is active, it handles next-step dispatch automatically

---

## Pattern: Standards Injection

Before executing a skill's main workflow:
1. Check if `_concept/_standards/index.yml` exists
2. If yes: read index, match standards to current skill by `applies_to` + keyword overlap
3. Load matched standard files as additional context
4. Reference applicable standards when making decisions
5. No error if no standards exist — standards are optional

---

## Pattern: Research Mode

When research mode is active for the current step (flow `modes.research.enabled: true`
and this skill is in `modes.research.triggers`):
1. Identify what needs grounding (decisions, alternatives, patterns)
2. Dispatch a parallel research sub-agent (`research` skill) with focused queries
3. Research agent writes cross-cutting findings to `_concept/_grounding/general/`
   and step-specific findings to `_concept/_grounding/{grounding_folder}/`
4. Main skill reads research results before making decisions
5. Reference research sources in output artifacts
6. Check `_grounding/{grounding_folder}/user_input.json` for pre-collected user inputs before asking the user

Cross-cutting topics written to `general/`: domain, competitors, audiences, design_inspiration,
patterns, colors_fonts, behavioral_patterns.

The `grounding_folder` for each skill is defined on the flow node's `data.grounding_folder` field.

---

## Pattern: User Input Persistence

User dialog inputs collected by the UI or by skills are saved to
`_concept/_grounding/{grounding_folder}/user_input.json`. This allows skills to skip
re-asking questions when the user has already provided answers.

1. Before collecting user inputs, check if `_grounding/{grounding_folder}/user_input.json` exists
2. If it exists, read the JSON object — keys are dialog field `id` values from the flow node's `user_inputs.dialog`
3. Use saved values as defaults or skip the question entirely if the value is present
4. After collecting new inputs, merge them into the existing file (or create it)
5. Never overwrite existing values without user confirmation

The `grounding_folder` value comes from the flow node `data.grounding_folder`
(e.g., `"overview/"`, `"features/"`, `"datamodel/"`).

---

## Pattern: Completion Summary

After producing artifacts:
1. Present a summary of what was produced:
   - Files created/modified with brief descriptions
   - Key decisions made
   - Cross-references established
2. Suggest next steps: which skills are now unblocked
3. If the orchestrator is active, it handles next-step suggestion automatically

---

## Pattern: Feedback Loop Update

When a skill modifies upstream files (e.g., screens registers back in features):
1. Read the upstream file
2. Parse frontmatter
3. Append/update the relevant array field
4. Update `last_updated` timestamp
5. Emit `feedback_loop` observability event
6. Do NOT change any other field in the upstream file

See `feedback_loop.md` for the full cross-reference protocol.

---

## Pattern: Subagent Dispatch

When a flow node has `"subagent": true`:
1. Orchestrator creates a fresh agent context
2. Context includes ONLY: the skill's SKILL.md, required `skaileup-shared/` contracts, input `_concept/` folders
3. **Paste the full task text verbatim into the subagent prompt** — never ask the subagent to read the plan file
4. Subagent runs to completion and reports one of four statuses (see Implementer Status Report below)
5. Orchestrator handles the status and collects output artifacts

**Never forward full conversation history to subagents.** Fresh context = focused output.

### Implementer Status Report

Every subagent MUST end with a status block using one of four codes:

```
STATUS: DONE | DONE_WITH_CONCERNS | NEEDS_CONTEXT | BLOCKED

[If DONE_WITH_CONCERNS]  Concerns: <trade-offs, deviations, debt incurred>
[If NEEDS_CONTEXT]       Missing:  <what is absent>
                         Question: <single specific question for the user or orchestrator>
[If BLOCKED]             Reason:   <what cannot be resolved>
                         Route:    context | escalate-model | decompose
```

| Status | Meaning | Orchestrator action |
|---|---|---|
| `DONE` | All requirements met, tests pass | Accept output, advance flow |
| `DONE_WITH_CONCERNS` | Implemented but with trade-offs or notes | Accept, log concerns in `decisions.md`, advance |
| `NEEDS_CONTEXT` | Missing information or ambiguous requirement | Surface specific question to user; resume when answered |
| `BLOCKED` | Cannot proceed — see route | See BLOCKED sub-routes below |

**BLOCKED sub-routes:**

| Route | When to use | Orchestrator response |
|---|---|---|
| `context` | A specific question can unblock the task | Escalate question to user; re-dispatch with answer |
| `escalate-model` | Task exceeded model capability | Note in `decisions.md`; re-dispatch on higher-tier model |
| `decompose` | Task is too large to execute atomically | Break into smaller tasks; re-dispatch each |

**Why this matters:** Subagents that silently produce partial output or fail without a clear status
cause the orchestrator to make incorrect assumptions. Explicit status codes make failure modes
actionable rather than opaque.

---

## Pattern: Expert Discovery (Implementation)

When implementing features:
1. Read `_concept/blueprint/techstack.md` to identify the tech stack
2. Search for matching expert skills in the monorepo:
   - JS/TS stack: `dev-implementation-experts-js/skills/skaileup-implementation-expert-<tech>/`
   - Python stack: `dev-implementation-experts-python/skills/skaileup-implementation-expert-<tech>/`
3. If found: load the expert skill's references and recipes for implementation guidance
4. If not found: proceed with general knowledge
5. After successful implementation: suggest creating or updating expert recipes

The `skaileup-implementation-expert-advisor` skill can route to the correct expert
when the target tech is not immediately obvious from the stack.
