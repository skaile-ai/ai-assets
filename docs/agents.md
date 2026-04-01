---
title: Agents
description: The GitAgent format — agent.yaml spec, SOUL.md, RULES.md, knowledge/, imprint assembly, and how agents are used by the runner.
---

An **agent** in ai-assets is a [GitAgent](https://gitagent.sh) — a portable, version-controlled agent definition stored as a plain directory. The GitAgent format defines the structure and schema; the `agent-runner` assembles the definition into a system prompt (called an **imprint**) before executing a flow.

> [GitAgent specification on GitHub](https://github.com/gitagent/spec)

## Directory Structure

```
<agent-name>/
├── agent.yaml          ← Manifest — spec version, name, model, delegation, dependencies
├── SOUL.md             ← Agent identity: who it is, what it cares about, its values
├── RULES.md            ← Behavioral constraints: what it must and must never do
└── knowledge/
    ├── index.yaml      ← Priority ordering for knowledge documents (lower = loaded first)
    └── *.md            ← Domain knowledge, reference material, context docs
```

An agent definition is valid when `agent.yaml` is present. All other files are optional and fall back gracefully to empty.

## agent.yaml Fields

```yaml
spec_version: "0.1.0"           # GitAgent spec version — always "0.1.0"
name: concept-orchestrator       # Unique agent ID
version: 1.0.0                   # Semver
description: >
  Concept pipeline orchestrator — runs Discovery → Experience → Blueprint.

extends: ../../../ai-asset-management/agents/skaile/agent.yaml  # optional inheritance

model:
  preferred: claude-opus-4-6     # Primary model
  fallback:
    - claude-sonnet-4-6          # Ordered fallback list
  constraints:
    temperature: 0.3
    max_tokens: 8192

delegation:
  mode: explicit                 # router | explicit | auto

runtime:
  max_turns: 100                 # Maximum conversation turns
  timeout: 1800                  # Seconds before timeout

author: skaile
license: MIT
tags: [orchestrator, concept-pipeline]

metadata:                        # Freeform — domain, skill paths, pipeline notes
  domain: skaileup-conceptualization
  phases: "Discovery, Experience, Blueprint"
```

## Field Reference

### `model`

| Field | Description |
|---|---|
| `preferred` | Primary model ID — used unless unavailable |
| `fallback` | Ordered list of fallback models |
| `constraints.temperature` | Sampling temperature (0.0–1.0) |
| `constraints.max_tokens` | Maximum output tokens per turn |

### `delegation`

Controls how the agent routes work to sub-agents:

| Mode | Behaviour |
|---|---|
| `router` | Acts as a top-level dispatcher — reads intent and routes to the right sub-agent |
| `explicit` | Sub-agents are called explicitly from within skill prompts |
| `auto` | Sub-agents are triggered automatically based on `triggers` conditions |

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

### `dependencies`

Declares which other agent definitions this agent depends on:

```yaml
dependencies:
  - name: conceptualization
    source: ../../skaileup-conceptualization/agents/orchestrator
    version: 1.0.0
    mount: agents/conceptualization
```

### `extends`

Inherits base configuration from a parent `agent.yaml`. Fields in the child override parent fields; `agents`, `dependencies`, and `tags` are merged.

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
  priority: 1            # lower = loaded earlier
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

| Agent | Path | Role |
|---|---|---|
| `skaile` | `ai-asset-management/agents/skaile/` | Root orchestrator — routes by intent to domain agents |
| `concept-orchestrator` | `skaileup-conceptualization/agents/orchestrator/` | Concept pipeline (Discovery → Blueprint) |
| `impl-orchestrator` | `skaileup-implementation/agents/orchestrator/` | Implementation pipeline (Setup → Verify) |
| `quality` | `skaileup-evaluate/agents/quality/` | Quality assurance |
| `architecture` | `skaileup-architecture/agents/architecture/` | System architecture |
| `pi` | `pichi/agent/` | Default CLI agent — focused software development assistant |

## Running an Agent

```bash
# Via skaile CLI (uses pichi/agent by default)
skaile run cli-concept --project-dir ./my-project

# With a different agent definition
skaile run cli-concept --project-dir ./my-project \
  --agent-dir ./ai-assets/skaileup-conceptualization/agents/orchestrator

# Via GitAgent CLI
gitagent run ai-assets/skaileup-conceptualization/agents/orchestrator/
gitagent validate ai-assets/ai-asset-management/agents/skaile/
```
