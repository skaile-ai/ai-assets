# Agent Patterns

Reusable workflow patterns for concept-forge skills. Reference these from SKILL.md
instead of duplicating the pattern in each skill.

## Pattern: Read-Context-First

Before producing any output:
1. Read `pipeline.json` to understand your step's dependencies and hard_gates
2. Check all `hard_gates` — these use structured objects, e.g.:
   ```json
   "hard_gates": [
     { "type": "file_exists", "path": "03_features/" }
   ]
   ```
   Stop immediately if any hard gate fails.
3. Read all files in `depends_on` step folders
4. Optionally read files in `optional_reads` — these use typed objects with fallback behavior, e.g.:
   ```json
   "optional_reads": [
     { "step": "cf_concept_functionality_behaviors", "artifact": "03b_behavior/", "fallback": "empty_default" },
     { "step": "cf_concept_architecture", "artifact": "05b_architecture/", "fallback": "skip_if_absent" }
   ]
   ```
   `empty_default` means proceed with no behavioral data. `skip_if_absent` means skip the section entirely.
5. Only THEN start your workflow

**Never skip this.** Even if the user has described what they want in conversation,
the _concept/ artifacts are the source of truth.

## Pattern: Self-Collect Inputs

When a skill needs user input (replaces facilitator handoff):
1. Check `user_inputs` in pipeline.json for this step
2. Check `_concept/_grounding/{step}/user_input.json` for pre-collected user inputs
3. Check if `_concept/` already has the required files/data
4. For missing inputs: ask the user directly using friendly, non-technical language
4. Adapt question depth based on complexity setting:
   - **simple**: suggest defaults, minimal questions
   - **moderate**: 3-5 focused questions
   - **complex/enterprise**: explore edge cases, multiple rounds
5. Proceed with workflow

## Pattern: Communication Style

Adapt tone based on complexity setting:
- **simple**: Casual, brief, suggest smart defaults. "I'll set up a clean login flow — sound good?"
- **moderate**: Balanced, explain trade-offs briefly. "For auth, we can do email+password or add social login. What fits your users?"
- **complex/enterprise**: Detailed, explore edge cases. "Let's map out the auth flow. Who are the user roles? What happens with failed logins? Do you need SSO?"

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

## Pattern: Standalone Mode

Skills can run independently without the orchestrator:
1. Read pipeline.json to find your step definition
2. Check all `hard_gates` (file_exists checks)
3. If ALL pass: read depends_on folders → execute workflow → emit completed → call orchestrator for next-step suggestion
4. If ANY fail: name missing prerequisites, tell user which skill to run first

## Pattern: Next-Step Suggestion (Orchestrator)

After a standalone skill completes:
1. Read pipeline.json to find steps that depend on the completed step
2. Check which of those steps now have all hard_gates satisfied
3. Present unblocked steps as suggestions
4. If no steps unblocked, show what's still missing

## Pattern: Standards Injection

Before executing a skill's main workflow:
1. Check if `_concept/_standards/index.yml` exists
2. If yes: read index, match standards to current skill by `applies_to` + keyword overlap
3. Load matched standard files as additional context
4. Reference applicable standards when making decisions
5. No error if no standards exist — standards are optional

## Pattern: Research Mode

When research_mode is active for the current step:
1. Identify what needs grounding (decisions, alternatives, patterns)
2. Dispatch a parallel research sub-agent with focused queries
3. Research agent writes cross-cutting findings to `_concept/_grounding/general/`
   and step-specific findings to `_concept/_grounding/{step}/`
4. Main skill reads research results before making decisions
5. Reference research sources in output artifacts
6. Check `_grounding/{step}/user_input.json` for pre-collected user inputs before asking the user

Cross-cutting topics in `general/`: domain, competitors, audiences, design_inspiration, patterns, colors_fonts, behavioral_patterns

Step folders (see `pipeline.json` `step_folders` mapping for `cf_concept_*` step IDs): overview/, features/, behaviors/, brand_visual/, brand_behavioral/, techstack/, architecture/, datamodel/, screens/, components/

**Backward compatibility:** If `_research/` exists but `_grounding/` does not, read from `_research/`. Prefer `_grounding/` when both exist.

## Pattern: User Input Persistence

User dialog inputs collected by the UI or by skills are saved to
`_concept/_grounding/{step}/user_input.json`. This allows skills to skip
re-asking questions when the user has already provided answers.

1. Before collecting user inputs, check if `_grounding/{step}/user_input.json` exists
2. If it exists, read the JSON object — keys are dialog field IDs from `pipeline.json`
3. Use saved values as defaults or skip the question entirely if the value is present
4. After collecting new inputs, merge them into the existing file (or create it)
5. Never overwrite existing values without user confirmation

The step subfolder name is derived from the step ID using the `step_folders`
mapping in `pipeline.json` (e.g., `cf_concept_overview` -> `overview/`,
`cf_concept_functionality_features` -> `features/`).

## Pattern: Completion Summary

After producing artifacts:
1. Present a summary of what was produced:
   - Files created/modified (with brief descriptions)
   - Key decisions made
   - Cross-references established
2. Suggest next steps: which skills are now unblocked
3. If orchestrator is active, it handles next-step suggestion automatically

## Pattern: Feedback Loop Update

When a skill modifies upstream files (e.g., screens registers in features):
1. Read the upstream file
2. Parse frontmatter
3. Append/update the relevant array field
4. Update `last_updated` timestamp
5. Emit `feedback_loop` observability event
6. Do NOT change any other field in the upstream file

## Pattern: Subagent Dispatch

When pipeline.json marks a step as `"subagent": true`:
1. Orchestrator creates a fresh agent context
2. Context includes ONLY: the step's SKILL.md, required cf__shared/ contracts, input _concept/ folders
3. Subagent runs to completion
4. Orchestrator collects output artifacts
5. Orchestrator runs completion check

**Never forward full conversation history to subagents.** Fresh context = focused output.

## Pattern: Expert Discovery (Implementation)

When implementing features:
1. Read `05_techstack/stack.md` to identify the tech stack
2. Search for `prog-expert-*` skills matching the stack (e.g., prog-expert-nuxt, prog-expert-primevue)
3. If found: load the expert's recipes/ and references/ for implementation guidance
4. If not found: proceed with general knowledge
5. After successful implementation: suggest creating/updating expert recipes

Search paths for prog-expert skills:
- `.claude/skills/prog-expert-*/`
- `.agents/skills/prog-expert-*/`
- Configurable via `cf__shared/pipeline.json` config.expert_search_paths
