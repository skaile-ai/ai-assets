---
title: Agents
description: The GitAgent format — agent.yaml spec, SOUL.md, RULES.md, knowledge/, imprint assembly, and how agents are used by the runner.
_sources:
  - path: ai-assets/ai-asset-management/agents/skaile/agent.yaml
    description: Root orchestrator — persona/ruleset composition, model config, delegation, delegate agents
  - path: ai-assets/skaileup/agents/skaileup/agent.yaml
    description: Guide agent — skaileup-* discovery convention
  - path: ai-assets/skaileup-conceptualization/agents/skaileup-conceptualize/agent.yaml
    description: Concept pipeline agent — extends pattern, metadata fields
  - path: ai-assets/skaileup-implementation/agents/skaileup-implement/agent.yaml
    description: Implementation pipeline agent
  - path: ai-assets/skaile-forge/agent/agent.yaml
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
├── agent.yaml          ← Manifest — spec version, name, model, delegation, composes
├── SOUL.md             ← Agent identity: who it is, what it cares about, its values
├── RULES.md            ← Behavioral constraints: what it must and must never do
└── knowledge/
    ├── index.yaml      ← Priority ordering for knowledge documents (lower = loaded first)
    └── *.md            ← Domain knowledge, reference material, context docs
```

An agent definition is valid when `agent.yaml` is present. All other files are optional and fall back gracefully to empty.

## agent.yaml Fields

```yaml
spec_version: '0.2' # agent spec version — "0.2" (composes:, lifecycle, memory, triggers)
name: skaileup-conceptualize # Unique agent ID
version: 1.0.0 # Semver
description: >
  Concept pipeline orchestrator — runs Discovery → Experience → Blueprint.

extends: ../../../ai-asset-management/agents/skaile/agent.yaml # optional inheritance

# Composition (optional) — shared persona/ruleset parts, skills, delegate agents
composes:
  - { kind: persona, ref: ../../../_agent-parts/personas/skaile-base/PERSONA.md }
  - { kind: ruleset, ref: ../../../_agent-parts/rules/skaile-safety/RULESET.md }
  - { kind: skill, ref: skill:@skaile-ai/git }
  - { kind: agent, ref: 'agent:@skaile-ai/quality', as: quality, binding: delegate }

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

### `composes`

A list of `{kind, ref, as?, binding?}` items. `ref` is either a catalog ref in the one
grammar `kind:@publisher/name[#range]` (for example `skill:@skaile-ai/git`,
`agent:@skaile-ai/quality`) or a path relative to the `agent.yaml` file.

```yaml
composes:
  - { kind: persona, ref: ../../../_agent-parts/personas/skaile-base/PERSONA.md } # shared identity fragment
  - { kind: ruleset, ref: ../../../_agent-parts/rules/skaile-safety/RULESET.md } # shared constraint fragment
  - { kind: skill, ref: ./skills/code-assistance } # a skill inside this agent's directory
  - { kind: agent, ref: 'agent:@skaile-ai/quality', as: quality, binding: delegate }
```

Persona and ruleset parts live in `_agent-parts/<name>/PERSONA.md` and `RULESET.md` at the
ai-assets root and are reused across agents (an agent's own `RULES.md` is never a standalone
asset). `scripts/presets-resolve.test.sh` checks that every ref and every `extends:` path resolves.

### `model`

| Field                     | Description                                |
| ------------------------- | ------------------------------------------ |
| `preferred`               | Primary model ID — used unless unavailable |
| `fallback`                | Ordered list of fallback models            |
| `constraints.temperature` | Sampling temperature (0.0–1.0)             |
| `constraints.max_tokens`  | Maximum output tokens per turn             |

### `delegation`

Descriptive metadata: it states how the author intends the agent to route work to sub-agents. No code in `@skaile/workspaces` (core, runner, asset-manager) acts on `mode`, `router` or `triggers`; `skaile install` only copies `delegation` verbatim into the rendered omp `agent.yaml`. What actually delegates is a `composes` item of `kind: agent` (see "Delegate agents" below). Keep the values accurate, because readers and other tooling rely on them, but do not expect them to change runtime behaviour.

| Mode       | Behaviour                                                                       |
| ---------- | ------------------------------------------------------------------------------- |
| `router`   | Acts as a top-level dispatcher — reads intent and routes to the right sub-agent |
| `explicit` | Sub-agents are called explicitly from within skill prompts                      |
| `auto`     | Sub-agents are triggered automatically based on `triggers` conditions           |

### `agents` (multi-agent only)

Descriptive metadata on orchestrator agents (read by people, not by the runtime; see `delegation` above):

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

### Delegate agents

A `composes` item with `kind: agent` and `binding: delegate` (the default) is rendered at
`skaile install` as a sub-agent file next to the parent. `as:` sets the sub-agent's name.
This replaces the 0.1 `requires:` list. The `agents:` map and `delegation` above stay metadata: removing a delegate means removing its `composes` item; its `agents:` entry is then just stale documentation to remove with it.

### `extends`

Inherits from a parent `agent.yaml`, resolved relative to the child's directory. Only `skaile install` reads it (the runner's composition resolver does not), and it skips the parent with a warning when the path does not exist. Merge rules: scalars and objects (`model`, `delegation`, `agents`, `metadata`) are replaced by the child's value; `composes` is parent + child, deduplicated by kind, ref and slot id; other arrays are unioned.

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
| `skaile-development`              | `skaile-development/agents/skaile-development/`             | skaile-dev monorepo expert — routes tasks to skills and prog-experts           |
| `forge-project-assistant`         | `skaile-forge/agent/`                                       | In-app dev assistant for skaile-forge workspaces                                |
| `forge-project-base-orchestrator` | `skaile-forge/base-orchestrator/`                           | Home workspace guide — creates projects, explains Forge Project features       |
| `forge-project-orchestrator`      | `skaile-forge/project-orchestrator/`                        | Project workspace assistant — coding, writing, research, file management       |
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
