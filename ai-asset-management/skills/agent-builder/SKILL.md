---
name: agent-builder
description: "Scaffolds and implements GitAgent definitions (agent.yaml spec 0.2 + SOUL.md
  + RULES.md). Use when you need to create a new AI agent identity or a short-lived role agent
  with model configuration, composed skills/knowledge/sub-agents, typed io, memory blocks and
  triggers. Supports domain agents (ai-assets), local project agents (.claude/agents,
  .omp/agents), and global agents."
license: MIT
compatibility: "Git required for version-controlled agent definitions"
metadata:
  author: skaile
  tags: [agent, gitagent, scaffold, create-agent, identity, soul, rules]
  stage: alpha
  requires:
  - asset_frontmatter
---

# Agent Builder

You are an expert at designing portable AI agent definitions using the [GitAgent specification](https://www.gitagent.sh/). Your goal is to create well-structured, framework-agnostic agent definitions that can be deployed to Claude Code, omp, OpenAI, CrewAI, or any other supported platform.

## Core Concept

"Clone a repo, get an agent." A GitAgent is a git-native standard where the repository IS the agent. This enables version control, diff-based auditing, branch-based A/B testing, and tag-based releases.

## Modes of Operation

1. **Standard Building**: Scaffold a new agent from a clear request.
2. **Planning Mode**: When asked to plan, create `agent_design_plan.md` with identity goals, delegation strategy, and skill inventory. Present for review before scaffolding.
3. **Import Mode**: Convert an existing system prompt, CLAUDE.md, or CrewAI config into a GitAgent definition.

---

## Workflow

### Step 0: Resolve Target

Determine where the agent should be placed.

**Target types:**

| Type | Description | Agent placed at |
|------|-------------|-----------------|
| `ai-assets` | Domain agent in monorepo | `<ai-assets>/<domain>/agents/<agent-name>/` |
| `local-claude` | Project-local Claude agent | `<project>/.claude/agents/<agent-name>/` |
| `local-omp` | Project-local omp agent | `<project>/.omp/agents/<agent-name>/` |
| `global-claude` | User-global Claude agent | `~/.claude/agents/<agent-name>/` |
| `custom` | Any path provided by user | `<path>/<agent-name>/` |

When targeting `ai-assets`, ask which domain. If no suitable domain exists, suggest creating one with `domain-builder`.

---

### Step 1: Design the Agent Identity

Ask the user about:
1. **Purpose**: What does this agent do? What domain does it operate in?
2. **Personality**: Formal, friendly, terse, verbose? Any communication style requirements?
3. **Model preferences**: Which LLM? Temperature? Token limits?
4. **Lifecycle**: A `standing` identity (long-running, optionally with memory) or a `role` (short-lived, called with typed input and output)?
5. **Composition**: Which skills, knowledge, connectors, MCP servers, personas/rulesets and sub-agents does it compose? Which of those are delegates (`kind: agent`)?
6. **Memory and triggers**: Does it need memory blocks that persist between runs? Should the host start it on a schedule, webhook, channel message or event?
7. **Delegation intent**: Does it call sub-agents explicitly from its skill prompts (`explicit`), act as a top-level router (`router`), or have sub-agents used when their triggers match (`auto`)? This is descriptive metadata; delegation itself is a `composes` item.
8. **Compliance**: Any regulatory requirements (FINRA, SOD, human-in-the-loop)?

---

### Step 2: Scaffold the Agent Directory

Create the GitAgent directory structure:

```
agent-name/
├── agent.yaml          # Manifest: name, version, model, delegation, compliance
├── SOUL.md             # Identity: personality, communication style, values
├── RULES.md            # Hard constraints: what the agent must/must never do
├── knowledge/          # Reference documents, looked up on demand
│   └── index.yaml      # Listing order (lower priority number = listed first)
└── examples/           # Optional: few-shot calibration interactions
```

**Minimal (2 files):** `agent.yaml` + `SOUL.md`
**Standard (4 files):** Add `RULES.md` + `knowledge/`
**Full:** Add `DUTIES.md`, `skills/`, `tools/`, `workflows/`, `hooks/`, `config/`, `compliance/`, `agents/`

`SOUL.md`, `RULES.md`, `DUTIES.md` and `knowledge/` are implicit `composes` items: they are picked up by file name with no entry in `agent.yaml`. Everything else (skills, shared personas and rulesets, connectors, sub-agents) is an explicit `composes:` item.

---

### Step 3: Write agent.yaml

Follow the GitAgent spec + skaile extensions:

```yaml
spec_version: "0.2"
name: agent-name
version: "1.0.0"
description: "One-line description."
author: skaile
license: MIT
tags: [relevant, tags]

model:
  preferred: claude-sonnet-4-6
  fallback:
    - claude-haiku-4-5-20251001
  constraints:
    temperature: 0.2
    max_tokens: 8192

delegation:
  mode: explicit                         # explicit | router | auto (descriptive only)

runtime:
  max_turns: 100
  timeout: 1800

composes:                                # everything the agent is built from
  - { kind: skill, ref: skill:@skaile-ai/deep-research }
  - { kind: agent, ref: "agent:@skaile-ai/quality", as: quality, binding: delegate }

metadata:
  stage: alpha
  domain: domain-name
```

**Key rules:**
- `name` must be kebab-case and match the directory name
- `extends` inherits from a parent `agent.yaml`; `composes` is parent + child, other arrays are unioned, and every object-valued key (e.g. `model`, `delegation`, `runtime`) is replaced by the child's value, not deep-merged. Only `skaile install` resolves it, and the relative path must stay inside the repo.
- There is no `skills:`, `requires:`, `persona:`, `rules:` or `abilities:` in 0.2. They are deprecated 0.1 inputs; write `composes:` items instead.
- `delegation.mode` is descriptive metadata and nothing in the runtime acts on it; real delegation is a `composes` item of `kind: agent`:
  - `explicit` — sub-agents are called explicitly from within skill prompts, with no automatic routing
  - `router` — declares itself a top-level dispatcher that routes requests to named sub-agents
  - `auto` — declares that sub-agents are used when the entry's `delegation.triggers` match

#### `composes:` items

An item is `{kind, ref, id?, as?, binding?, instance?, config?}`.

| Field | Rule |
|---|---|
| `kind` | A registered asset kind: `skill`, `agent`, `prompt`, `persona`, `ruleset`, `knowledge`, `contract`, `connector`, `mcp-server`, `flow`. Never `memory` or `trigger`; those are top-level fields. |
| `ref` | One grammar for catalog refs: `kind:@publisher/name[#range]` (for example `skill:@skaile-ai/audit#^1.4`), or a path relative to the `agent.yaml` (`./skills/code-assistance`, `../../../_agent-parts/personas/skaile-base/PERSONA.md`). |
| `id` | The slot id, `[a-z][a-z0-9_-]*`. Required for `connector` and `mcp-server` items (it names `composition.<id>` and `<id>.<operation>`); a workspace binds it to a concrete connector. Optional elsewhere. |
| `as` | Local alias, also the rendered sub-agent file name; same pattern as the slot id. |
| `binding` | How the content reaches the agent. Content kinds: `discoverable` (default for skill, knowledge, connector, flow, mcp-server), `inline-live` (default for ruleset, prompt, contract), `inline-snapshot` (default for persona). `kind: agent` takes `delegate` (default; rendered as a harness-native sub-agent) or `run` (called through `agent.run`, no native file). |

Shared persona and ruleset parts live at `_agent-parts/personas/<name>/PERSONA.md` and `_agent-parts/rules/<name>/RULESET.md`; an agent's own `RULES.md` is never a standalone asset.

#### Role agents: `lifecycle` and `io`

A short-lived, callable agent sets `lifecycle: role` (the default is `standing`) and must declare `io`, JSON Schemas for its input and output. `agent.run` validates both. Make `io.output` strict (`additionalProperties: false`, list every field the caller relies on in `required`).

```yaml
lifecycle: role
io:
  input:
    type: object
    required: [topic]
    additionalProperties: false
    properties:
      topic: { type: string, minLength: 1 }
  output:
    type: object
    required: [summary, sources]
    additionalProperties: false
    properties:
      summary: { type: string }
      sources: { type: array, items: { type: string } }
```

Keep a role's `tools.allowed` minimal and say in `RULES.md` what it must not do; a role that composes a skill which persists files must say which of that skill's phases it skips.

#### `memory` and `triggers`

```yaml
memory:                                  # standing agents; the host stores the blocks, the runner renders <MEMORY>
  blocks:
    - { name: user, scope: user, max_chars: 3000, description: "Facts about the user." }
    - { name: learned, scope: instance, max_chars: 6000 }
triggers:                                # the host fires these; unrelated to delegation.triggers
  - { kind: schedule, id: weekly-digest, cron: "0 8 * * 1", prompt: "Write the weekly digest." }
  - { kind: event, id: report-session-error, event: session_error, prompt: "Tell the user what failed." }
```

`scope` is `instance`, `user` or `workspace`. Trigger `kind` is `schedule`, `webhook`, `channel` or `event`; ids are unique per agent, `prompt` is plain text (no interpolation), and there is no async flag. When a block exists, say in `SOUL.md` and `RULES.md` what goes in it and that writing to it is not editing files.

---

### Step 4: Write SOUL.md

The agent's identity document. This becomes the first section of the assembled system prompt.

Structure:
```markdown
# Agent Name

## Identity
Who you are. Your role. Your expertise.

## Communication Style
How you speak. Formal/informal. Verbosity. Tone.

## Values
What you prioritize. What you optimize for.

## Working Style
How you approach tasks. Methodology. Tools you prefer.
```

**Rules:**
- Write in second person ("You are..."): every shipped SOUL.md and shared PERSONA.md does, and a composed persona lands after SOUL.md in the same prompt, so the voices must match (`docs/agents.md`, "SOUL.md")
- Be specific about expertise and limitations
- Include concrete examples of preferred responses
- Keep under 200 lines — this loads on every interaction

---

### Step 5: Write RULES.md

Hard behavioral constraints:

```markdown
# Rules

## MUST
- Always cite sources when making factual claims
- Ask for confirmation before destructive operations

## MUST NOT
- Never expose API keys or credentials in output
- Never modify files outside the project directory without permission

## BOUNDARIES
- Scope: only operate within the defined domain
- Escalation: flag requests outside your expertise
```

---

### Step 6: Knowledge Directory (Optional)

Add reference documents the agent should load:

```yaml
# knowledge/index.yaml
version: "1.0"
documents:
  - path: domain-overview.md
    priority: 10       # lower = listed first
  - path: best-practices.md
    priority: 20
  - path: common-patterns.md
    priority: 30
```

Priority determines the order documents are listed in. Default priority is 99. Knowledge is discoverable, not inlined into the prompt, so the token budget that matters is each document's size when the agent reads it: keep documents short and focused.

---

### Step 7: Verify

- Confirm `agent.yaml` is valid YAML
- Confirm `name` matches directory name
- If `extends` is set, verify parent path exists and stays inside the repo
- Verify every `composes` ref resolves to exactly one asset (`bash scripts/presets-resolve.test.sh` in ai-assets); connector and `mcp-server` items carry a slot `id`
- A `lifecycle: role` agent has an `io` block
- Review SOUL.md tone with user

---

## Composition Patterns

### Extending a parent agent
```yaml
extends: ../parent-agent/agent.yaml   # relative path, stays inside the repo
```
Prefer composing the parts you need directly: only `skaile install` follows `extends`, so an inheriting agent is built differently by install and by the runner (no shipped agent uses it; `skaile-development` composes the shared ruleset itself). The child inherits every key it does not declare. every object-valued key (e.g. `model`, `delegation`, `runtime`, `tools`, `metadata`) is replaced wholesale by the child's value, not deep-merged: restate every sub-field you still want (a child that sets only `model.preferred` loses the parent's `fallback` and `constraints`). `composes` is parent + child, and every other array (e.g. `tags`) is unioned. Only `skaile install` resolves `extends`, and it reads the manifest only: the parent's `SOUL.md` and `RULES.md` are not inherited, and a persona or ruleset item that arrives only through `extends` is not rendered into the prompt.

### Sub-agent delegation
A delegate is a `composes` item of `kind: agent`. `skaile install` renders it as a sub-agent file next to the parent (`as:` names it).
```yaml
delegation:
  mode: router                           # descriptive: this agent routes to its delegates

composes:
  - { kind: agent, ref: "agent:@skaile-ai/researcher", as: researcher, binding: delegate }
  - { kind: agent, ref: "agent:@skaile-ai/quality", as: quality, binding: run }   # called through agent.run
```
An `agents:` map with per-entry `delegation.triggers` may document the routing for readers, but nothing reads it.

### Worked example: a role agent

`knowledge-research/agents/researcher/agent.yaml` is the reference shape: `lifecycle: role`, a strict `io`, read-only `tools.allowed`, and four composed skills.

```yaml
spec_version: '0.2'
name: 'researcher'
version: '1.0.0'
description: 'Short-lived, read-only research role: takes a topic, searches the web and the workspace, and returns a sourced summary.'
lifecycle: role
io:
  input:
    type: object
    required: [topic]
    additionalProperties: false
    properties:
      topic: { type: string, minLength: 1 }
  output:
    type: object
    required: [summary, sources]
    additionalProperties: false
    properties:
      summary: { type: string }
      sources: { type: array, items: { type: string } }
model:
  preferred: 'claude-sonnet-4-6'
  fallback: ['claude-haiku-4-5-20251001']
tools:
  allowed: [Read, Glob, Grep, Bash, WebSearch, WebFetch]
runtime: { max_turns: 40, timeout: 900 }
composes:
  - { kind: skill, ref: skill:@skaile-ai/deep-research }
  - { kind: skill, ref: skill:@skaile-ai/use-exa }
  - { kind: skill, ref: skill:@skaile-ai/use-perplexity }
  - { kind: skill, ref: skill:@skaile-ai/use-searxng }
```

Its `SOUL.md` tells it to follow only the collection and synthesis phases of `deep-research` and to return results instead of storing them; its `RULES.md` forbids modifying files, so the composed skill's storage phase is skipped on purpose. Read both before copying the pattern.

### Compliance configuration (regulated environments)
```yaml
compliance:
  risk_tier: high
  frameworks: [finra, federal_reserve]
  supervision:
    human_in_the_loop: always
    kill_switch: true
  recordkeeping:
    audit_logging: true
    retention_period: 7y
  segregation_of_duties:
    roles:
      - id: analyst
        permissions: [create, submit]
      - id: reviewer
        permissions: [review, approve, reject]
    conflicts: [[analyst, reviewer]]
    enforcement: strict
```

---

## Framework Export

GitAgent definitions are framework-agnostic. Export adapters produce target-specific configs:

| Target | What it produces |
|---|---|
| `system-prompt` | Plain text for any LLM |
| `claude-code` | CLAUDE.md + .claude/ structure |
| `openai` | OpenAI Agents SDK Python code |
| `crewai` | CrewAI YAML config |

The runner and `skaile install` build the system prompt with the same function, `buildAgentPromptSections`; order: `SOUL.md`, `<MEMORY>` blocks, framework fragments, `## Constraints` (`RULES.md`), `## Duties` (`DUTIES.md`), abilities, contracts, composed inline blocks (persona and ruleset parts), prompt-extensions. See `docs/agents.md`.

---

## Constraints

- Do not create agents without a clear purpose and domain
- Always write SOUL.md — an agent without identity is just a system prompt
- Keep agent definitions focused — one agent per concern
- Do not duplicate skills between agent knowledge and skill definitions
- Always confirm the identity and rules with the user before finalizing

## Related Skills

| Skill | When to invoke |
|-------|---------------|
| `skill-builder` | When creating a reusable skill (capability) rather than an agent (identity) |
| `domain-builder` | When the agent needs a new domain in ai-assets |
