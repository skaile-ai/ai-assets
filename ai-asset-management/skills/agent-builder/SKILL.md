---
name: agent-builder
description: "Scaffolds and implements GitAgent definitions (agent.yaml + SOUL.md
  + RULES.md). Use when you need to create a new AI agent identity with model configuration,
  delegation rules, and composable sub-agents. Supports domain agents (ai-assets),
  local project agents (.claude/agents, .omp/agents), and global agents."
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
4. **Delegation**: Does it work alone (`explicit`), route to sub-agents (`router`), or auto-delegate (`auto`)?
5. **Compliance**: Any regulatory requirements (FINRA, SOD, human-in-the-loop)?

---

### Step 2: Scaffold the Agent Directory

Create the GitAgent directory structure:

```
agent-name/
├── agent.yaml          # Manifest: name, version, model, delegation, compliance
├── SOUL.md             # Identity: personality, communication style, values
├── RULES.md            # Hard constraints: what the agent must/must never do
├── knowledge/          # Reference documents sorted by priority
│   └── index.yaml      # Priority ordering (lower number = loaded first)
└── examples/           # Optional: few-shot calibration interactions
```

**Minimal (2 files):** `agent.yaml` + `SOUL.md`
**Standard (4 files):** Add `RULES.md` + `knowledge/`
**Full:** Add `DUTIES.md`, `skills/`, `tools/`, `workflows/`, `hooks/`, `config/`, `compliance/`, `agents/`

---

### Step 3: Write agent.yaml

Follow the GitAgent spec + skaile extensions:

```yaml
spec_version: "0.1.0"
name: agent-name
version: "1.0.0"
description: "One-line description."
author: skaile
license: MIT
tags: [relevant, tags]

extends: ../path/to/parent/agent.yaml   # optional inheritance

model:
  preferred: claude-sonnet-4-6
  fallback:
    - claude-haiku-4-5-20251001
  constraints:
    temperature: 0.2
    max_tokens: 8192

delegation:
  mode: explicit                         # explicit | router | auto

runtime:
  max_turns: 100
  timeout: 1800

requires: []                             # sub-agent or skill dependencies

metadata:
  stage: alpha
  domain: domain-name
```

**Key rules:**
- `name` must be kebab-case and match the directory name
- `extends` enables inheritance — child agents inherit model/delegation/runtime from parent
- `requires` uses the unified dependency format: bare strings or `{name, source, version, mount}` objects
- `delegation.mode`:
  - `explicit` — agent acts alone, no sub-agents
  - `router` — routes requests to named sub-agents based on triggers
  - `auto` — automatically selects sub-agents based on context

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
- Write in second person ("You are...")
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
files:
  - path: domain-overview.md
    priority: 10       # lower = loaded earlier
  - path: best-practices.md
    priority: 20
  - path: common-patterns.md
    priority: 30
```

Priority determines load order. Default priority is 99. Keep total knowledge under 5000 tokens.

---

### Step 7: Verify

- Confirm `agent.yaml` is valid YAML
- Confirm `name` matches directory name
- If `extends` is set, verify parent path exists
- If `requires` has entries, verify sources exist
- Review SOUL.md tone with user

---

## Composition Patterns

### Extending a parent agent
```yaml
extends: ../../../ai-asset-management/agents/skaile/agent.yaml
```
Child inherits model/delegation/runtime. Override any field locally.

### Sub-agent delegation
```yaml
delegation:
  mode: router
  router: orchestrator

agents:
  researcher:
    description: "Deep research and analysis"
    delegation:
      mode: auto
      triggers: [research_requested, analysis_needed]
  implementer:
    description: "Code implementation"
    delegation:
      mode: auto
      triggers: [implementation_requested, code_requested]

requires:
  - name: researcher
    source: ../../knowledge-research/agents/researcher
    version: "1.0.0"
    mount: agents/researcher
```

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

agent-runner natively loads GitAgent directories via `buildAgentImprint()`.

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
