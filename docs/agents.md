---
title: Agents
description: The GitAgent format — agent.yaml spec, SOUL.md, RULES.md, knowledge/, imprint assembly, and how agents are used by the runner.
_sources:
  - path: ai-assets/ai-asset-management/agents/skaile/agent.yaml
    description: Root orchestrator — persona/rules mixin pattern, model config, delegation, requires
  - path: ai-assets/skaileup/agents/skaileup/agent.yaml
    description: Guide agent — skaileup-* discovery convention
  - path: ai-assets/skaileup-conceptualization/agents/skaileup-conceptualize/agent.yaml
    description: Concept pipeline agent — extends pattern, metadata fields
  - path: ai-assets/skaileup-implementation/agents/skaileup-implement/agent.yaml
    description: Implementation pipeline agent
  - path: ai-assets/forge-project/agent/agent.yaml
    description: Forge-Project in-app dev assistant — skills field, app-specific pattern
  - path: ai-assets/skaile-development/agents/skaile-development/agent.yaml
    description: Skaile-dev workflow agent
  - path: ai-assets/skaile-platform/agents/assistant/agent.yaml
    description: Platform assistant agent
_based_on_commit: 5fd26c1
_last_synced: '2026-04-25'
sidebar:
  order: 30
---

An **agent** in ai-assets is a [GitAgent](https://gitagent.sh) — a portable, version-controlled agent definition stored as a plain directory. The GitAgent format defines the structure and schema; the `agent-runner` assembles the definition into a system prompt (called an **imprint**) before executing a flow.

> [GitAgent specification on GitHub](https://github.com/gitagent/spec)

## Directory Structure

```
<agent-name>/
├── agent.yaml          ← Manifest — spec version, name, model, delegation, requires
├── SOUL.md             ← Agent identity: who it is, what it cares about, its values
├── RULES.md            ← Behavioral constraints: what it must and must never do
└── knowledge/
    ├── index.yaml      ← Priority ordering for knowledge documents (lower = loaded first)
    └── *.md            ← Domain knowledge, reference material, context docs
```

An agent definition is valid when `agent.yaml` is present. All other files are optional and fall back gracefully to empty.

## agent.yaml Fields

```yaml
spec_version: '0.1.0' # GitAgent spec version — always "0.1.0"
name: skaileup-conceptualize # Unique agent ID
version: 1.0.0 # Semver
description: >
  Concept pipeline orchestrator — runs Discovery → Experience → Blueprint.

extends: ../../ai-asset-management/agents/skaile/agent.yaml # optional inheritance

# Mixin composition (optional — pulls in shared persona/rules fragments)
persona:
  - ../../../_agent-parts/personas/skaile-base.md

rules:
  - ../../../_agent-parts/rules/skaile-safety.md

model:
  preferred: claude-opus-4-6 # Primary model
  fallback:
    - claude-sonnet-4-6 # Ordered fallback list
  constraints:
    temperature: 0.3
    max_tokens: 8192

delegation:
  mode: explicit # router | explicit | auto

runtime:
  max_turns: 100 # Maximum conversation turns
  timeout: 1800 # Seconds before timeout

author: skaile
license: MIT
tags: [orchestrator, concept-pipeline]

metadata: # Freeform — domain, skill paths, pipeline notes
  domain: skaileup-conceptualization
  phases: 'Discovery, Experience, Blueprint'
```

## Field Reference

### `persona` and `rules` (mixin composition)

Optional arrays of relative file paths. The runner reads each file and prepends its content to the assembled system prompt, before SOUL.md and RULES.md respectively.

```yaml
persona:
  - ../../../_agent-parts/personas/skaile-base.md # shared identity fragment

rules:
  - ../../../_agent-parts/rules/skaile-safety.md # shared constraint fragment
```

Mixin files live in `_agent-parts/` at the ai-assets root and are reused across multiple agents to avoid duplication. Paths are relative to the `agent.yaml` file.

### `model`

| Field                     | Description                                |
| ------------------------- | ------------------------------------------ |
| `preferred`               | Primary model ID — used unless unavailable |
| `fallback`                | Ordered list of fallback models            |
| `constraints.temperature` | Sampling temperature (0.0–1.0)             |
| `constraints.max_tokens`  | Maximum output tokens per turn             |

### `delegation`

Controls how the agent routes work to sub-agents:

| Mode       | Behaviour                                                                       |
| ---------- | ------------------------------------------------------------------------------- |
| `router`   | Acts as a top-level dispatcher — reads intent and routes to the right sub-agent |
| `explicit` | Sub-agents are called explicitly from within skill prompts                      |
| `auto`     | Sub-agents are triggered automatically based on `triggers` conditions           |

### `agents` (multi-agent only)

Defined on orchestrator agents that manage sub-agents:

```yaml
agents:
  conceptualization:
    description: Structured project concept pipeline
    delegation:
      mode: auto
      triggers:
        - concept_requested
        - discovery_requested
```

### `requires`

Declares which other agent definitions this agent depends on. The runner resolves these at startup and mounts them at the specified path:

```yaml
requires:
  - name: conceptualization
    source: ../../skaileup-conceptualization/agents/skaileup-conceptualize
    version: 1.0.0
    mount: agents/conceptualization
```

### `extends`

Inherits base configuration from a parent `agent.yaml`. Fields in the child override parent fields; `agents`, `requires`, and `tags` are merged.

## SOUL.md

The agent's identity document. Written in first person. Describes:

- What the agent is and what it is trying to accomplish
- Its values, priorities, and working style
- How it relates to the human it is working with

The runner prepends SOUL.md to the assembled system prompt.

## RULES.md

Behavioral constraints. Written as imperative rules. Typically contains:

- MUST: things the agent is always required to do
- NEVER: hard prohibitions
- Workflow rules specific to the agent's domain

## knowledge/

Domain knowledge documents loaded after SOUL.md and RULES.md. Ordered by `knowledge/index.yaml`:

```yaml
# knowledge/index.yaml
- path: concept_structure.md
  priority: 1 # lower = loaded earlier
- path: golden_principles.md
  priority: 2
- path: iron_laws.md
  priority: 3
```

Files not listed in `index.yaml` get a default priority of `99` and are loaded last.

## Imprint Assembly

`buildAgentImprint(agentDir)` in `agent-runner` assembles the system prompt:

```
SOUL.md
---
RULES.md
---
knowledge/doc-1.md   (priority 1)
---
knowledge/doc-2.md   (priority 2)
---
[optional: project CLAUDE.md overlay]
```

Parts are joined with `\n\n---\n\n`. Missing files are silently skipped.

## Agents in ai-assets

| Agent                             | Path                                                        | Role                                                                           |
| --------------------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `skaile`                          | `ai-asset-management/agents/skaile/`                        | Root orchestrator — routes by intent to domain agents                          |
| `skaileup`                        | `skaileup/agents/skaileup/`                                 | Conversational guide — discovers installed orchestrators, guides through flows |
| `skaileup-conceptualize`          | `skaileup-conceptualization/agents/skaileup-conceptualize/` | Concept pipeline (Discovery → Blueprint)                                       |
| `skaileup-implement`              | `skaileup-implementation/agents/skaileup-implement/`        | Implementation pipeline (Setup → Verify)                                       |
| `quality`                         | `skaileup-evaluate/agents/quality/`                         | Quality assurance                                                              |
| `architecture`                    | `skaileup-architecture/agents/architecture/`                | System architecture                                                            |
| `skaile-development`              | `skaile-development/agents/skaile-development/`             | skaile-dev monorepo expert — routes tasks to skills and prog-experts           |
| `forge-project-assistant`         | `forge-project/agent/`                                      | In-app dev assistant for forge/L4-project workspaces                           |
| `forge-project-base-orchestrator` | `forge-project/base-orchestrator/`                          | Home workspace guide — creates projects, explains Forge Project features       |
| `forge-project-orchestrator`      | `forge-project/project-orchestrator/`                       | Project workspace assistant — coding, writing, research, file management       |
| `skaile-assistant`                | `skaile-platform/agents/assistant/`                         | Skaile platform enterprise assistant — research, analysis, writing, code       |

The `skaileup-*` naming convention is significant: any agent installed to `.claude/agents/` whose name starts with `skaileup-` is automatically discovered by the `skaileup` guide agent at session start. New orchestrators (e.g. `skaileup-implement-supabase`) are discovered without any change to `skaileup` itself.

## Running an Agent

```bash
# Via skaile CLI (uses pichi/agent by default)
skaile run cli-concept --project-dir ./my-project

# With a different agent definition
skaile run cli-concept --project-dir ./my-project \
  --agent-dir ./ai-assets/skaileup-conceptualization/agents/skaileup-conceptualize

# Via Claude Code --agent flag (after skaile install)
claude --agent skaileup                  # interactive guide — start here
claude --agent skaileup-conceptualize    # concept pipeline directly
claude --agent skaileup-implement        # implementation pipeline directly

# Via GitAgent CLI
gitagent run ai-assets/skaileup-conceptualization/agents/skaileup-conceptualize/
gitagent validate ai-assets/ai-asset-management/agents/skaile/
```
