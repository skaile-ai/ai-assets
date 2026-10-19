---
title: Agents
description: The GitAgent format — agent.yaml spec 0.2, SOUL.md, RULES.md, knowledge/, system-prompt assembly, and how agents are used by the runner.
_sources:
  - path: ai-assets/ai-asset-management/agents/skaile/agent.yaml
    description: Root orchestrator — persona/ruleset composition, model config, delegation, delegate agents
  - path: ai-assets/skaile-forge/agent/agent.yaml
    description: Forge-Project in-app dev assistant — composes of local skills, event trigger (report-session-error)
  - path: ai-assets/skaile-development/agents/skaile-development/agent.yaml
    description: Skaile-dev workflow agent — extends the skaile agent
  - path: ai-assets/skaile-platform/agents/assistant/agent.yaml
    description: Platform assistant agent
  - path: ai-assets/knowledge-research/agents/researcher/agent.yaml
    description: Role agent — lifecycle, io, composes of skills
  - path: ai-assets/skaile-forge/base-orchestrator/agent.yaml
    description: Standing agent — memory.blocks
# The skaileup agents (skaileup, skaileup-conceptualize, skaileup-implement, quality) ship from the
# ai-assets-skaileup repo and are not sourced here. The workspaces commit below is where
# agent-prompt-sections.ts, install-agent.ts and the 0.2 manifest schema were read.
_based_on_commit: ec12483
_last_synced: '2026-10-19'
sidebar:
  order: 30
---

An **agent** in ai-assets is a [GitAgent](https://gitagent.sh) — a portable, version-controlled agent definition stored as a plain directory. The GitAgent format defines the structure and schema; the runner assembles the definition into a system prompt (see System Prompt Assembly below) before executing a flow.

> [GitAgent specification on GitHub](https://github.com/gitagent/spec)

## Directory Structure

```
<agent-name>/
├── agent.yaml          ← Manifest — spec version, name, model, delegation, composes
├── SOUL.md             ← Agent identity: who it is, what it cares about, its values
├── RULES.md            ← Behavioral constraints: what it must and must never do
└── knowledge/
    ├── index.yaml      ← Order of the knowledge documents (lower priority number = listed first)
    └── *.md            ← Domain knowledge, reference material, context docs
```

An agent definition is valid when `agent.yaml` is present. All other files are optional and fall back gracefully to empty.

## agent.yaml Fields

```yaml
spec_version: '0.2' # agent spec version — "0.2" (composes:, lifecycle, memory, triggers)
name: skaile-development # Unique agent ID
version: 1.2.0 # Semver
description: >
  Expert advisor for the skaile-dev monorepo — routes tasks to the right skills.

extends: ../../../ai-asset-management/agents/skaile/agent.yaml # optional inheritance; the path must stay inside the repo

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

# lifecycle, io, memory and triggers are covered under "Field Reference" below;
# a complete role agent is knowledge-research/agents/researcher/agent.yaml.

author: skaile
license: MIT
tags: [orchestrator, concept-pipeline]

metadata: # Freeform — domain, routing notes, knowledge notes
  domain: skaile-development
  stage: beta
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

Descriptive metadata: it states how the author intends the agent to route work to sub-agents. No code in `@skaile/workspaces` (core, runner, asset-manager) acts on `delegation.mode`, `delegation.router` or the per-entry `delegation.triggers` inside an `agents:` map; `skaile install` only copies `delegation` verbatim into the rendered omp `agent.yaml`. What actually delegates is a `composes` item of `kind: agent` (see "Delegate agents" below). Keep the values accurate, because readers and other tooling rely on them, but do not expect them to change runtime behaviour.

This is **not** the top-level `triggers:` list of spec 0.2, which is live (see [`triggers`](#triggers)). The two share a word and nothing else: `delegation.triggers` is a list of intent names for readers; top-level `triggers:` declares things the host fires.

| Mode       | What the author declares                                                                     |
| ---------- | -------------------------------------------------------------------------------------------- |
| `router`   | Declares itself a top-level dispatcher: it reads intent and routes to the right sub-agent    |
| `explicit` | Declares that sub-agents are called explicitly from within skill prompts                     |
| `auto`     | Declares that sub-agents are meant to be used when the entry's `delegation.triggers` match   |

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

### `lifecycle` and `io`

`lifecycle` is `standing` (default: a long-running identity) or `role` (short-lived and callable). A
role declares `io.input` and `io.output` as JSON Schema objects; `agent.run` validates the
caller's input and the role's result against them. The 0.2 manifest schema rejects a role without `io`.

```yaml
lifecycle: role
io:
  input:
    type: object
    required: [topic]
    additionalProperties: false
    properties: { topic: { type: string, minLength: 1 } }
  output:
    type: object
    required: [summary, sources]
    additionalProperties: false
    properties:
      summary: { type: string }
      sources: { type: array, items: { type: string } }
```

`knowledge-research/agents/researcher/` is the reference role: read-only tools, `deep-research`
composed for its collection and synthesis phases only, and a final message that is the JSON result.

### `memory`

Declares named, scoped, bounded text blocks. The agent asset only declares them; the host stores
them and the runner renders them as `<MEMORY>` in the system prompt. The agent reads and changes
them through the `memory.read`, `memory.write`, `memory.append` and `memory.list` capabilities.

```yaml
memory:
  blocks:
    - { name: user, scope: user, max_chars: 3000, description: 'Facts about the user.' }
    - { name: learned, scope: instance, max_chars: 6000, source: SOUL.md } # source seeds the block
```

| Field       | Description                                                                                  |
| ----------- | -------------------------------------------------------------------------------------------- |
| `name`      | `[a-z][a-z0-9_-]*`; unique per agent                                                         |
| `scope`     | `instance` (this agent instance), `user` (shared across the user's instances), `workspace`   |
| `max_chars` | Hard cap on the rendered content; the host enforces it on write                              |
| `source`    | Optional convention file the block is seeded from (for example `SOUL.md`)                    |

`description` is not read by the runner; it documents the block for authors and memory editors.
Memory is a top-level field only, never a `composes:` kind. An agent that declares no blocks has no
`<MEMORY>` section.

### `triggers`

A list of things the **host** fires to start a run of this agent: the host's trigger store
schedules `schedule`, receives `webhook`, and routes `channel` and `event` triggers. The runner
does not fire them. A workspace instance may override or extend them by `id`, and `enabled: false`
disables one. This is a top-level 0.2 key, unrelated to `delegation.triggers`.

| `kind`     | Fields besides `id`, `prompt`, `enabled`   | Fires when                                      |
| ---------- | ------------------------------------------ | ----------------------------------------------- |
| `schedule` | `cron`, optional `timezone`                | the cron expression matches                     |
| `webhook`  | none                                       | the host receives a call on the trigger's hook  |
| `channel`  | `when` (for example `mention`)             | a channel message matches `when`                |
| `event`    | `event` (for example `session_error`)      | the named host or protocol event is emitted     |

`prompt` is plain text sent to the agent when the trigger fires; `@skaile/workspaces` defines no
placeholder interpolation for it. Example, from `skaile-forge/agent/agent.yaml`:

```yaml
triggers:
  - kind: event
    id: report-session-error
    event: session_error
    prompt: 'A session error was reported for this project. Tell the user what failed.'
```

The 0.2 trigger schema has no async or background flag. Ids must be unique per agent
(`[a-z][a-z0-9_-]*`).

### `extends`

Inherits from a parent `agent.yaml`, resolved relative to the child's directory. Only `skaile install` reads it (the runner's composition resolver does not), and it skips the parent with a warning when the path does not exist. Merge rules: scalars and objects (`model`, `delegation`, `agents`, `metadata`) are replaced by the child's value; `composes` is parent + child, deduplicated by kind, ref and slot id; other arrays are unioned.

## SOUL.md

The agent's identity document. Written in second person ("You are …"), addressed to the model — the voice of every shipped `SOUL.md` and of the shared `PERSONA.md` parts, so that a composed persona (section 8 of the assembled prompt) continues the identity instead of switching voice. Describes:

- What the agent is and what it is trying to accomplish
- Its values, priorities, and working style
- How it relates to the human it is working with

SOUL.md is the first section of the assembled system prompt (see System Prompt Assembly).

## RULES.md

Behavioral constraints. Written as imperative rules. Typically contains:

- MUST: things the agent is always required to do
- NEVER: hard prohibitions
- Workflow rules specific to the agent's domain

## knowledge/

Domain knowledge documents. The `knowledge/` directory is an implicit `composes` item of kind
`knowledge` (binding `discoverable`): the agent can look documents up on demand, they are not
inlined into the prompt. Ordered by `knowledge/index.yaml`:

```yaml
# knowledge/index.yaml
- path: concept_structure.md
  priority: 1 # lower = listed first
- path: golden_principles.md
  priority: 2
- path: iron_laws.md
  priority: 3
```

Files not listed in `index.yaml` get a default priority of `99` and are listed last.

## System Prompt Assembly

The agent part of the system prompt is built by one function, `buildAgentPromptSections` in
`@skaile/workspaces` (`core/src/agent-prompt-sections.ts`). `skaile install` uses it to bake
the prompt into `.claude/agents/<name>.md` (and the omp and codex equivalents), and the runner
uses it for the live prompt, so the two cannot disagree on order. The order is fixed there:

| #   | Section       | Source                                                                                 |
| --- | ------------- | -------------------------------------------------------------------------------------- |
| 1   | soul          | `SOUL.md`                                                                              |
| 2   | `<MEMORY>`    | `memory.blocks`: one framed `<block name=… scope=…>` per block, each capped at `max_chars` |
| 3   | fragments     | framework fragments (agent mode, handoff, skill discovery, connector usage)            |
| 4   | `## Constraints` | `RULES.md`                                                                          |
| 5   | `## Duties`   | `DUTIES.md`                                                                            |
| 6   | `## Installed Abilities` | composed skills, as slash commands                                          |
| 7   | `## Contracts` | composed contracts                                                                    |
| 8   | composed      | inline blocks from `composes:`, including `persona` parts (`PERSONA.md`, snapshotted) and `ruleset` parts (`RULESET.md`, read live) |
| 9   | extensions    | per-project `prompt-extensions`                                                        |

Consequences for authors: a shared `PERSONA.md` composed through `composes:` lands in section 8,
after the agent's own `SOUL.md`, `RULES.md` and `DUTIES.md`; it supplements the identity and does
not replace it. `knowledge/` is discoverable and is not part of the prompt text. Empty parts are
dropped, and missing convention files are silently skipped.

## Agents in ai-assets

| Agent                             | Path                                                        | Role                                                                           |
| --------------------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `skaile`                          | `ai-asset-management/agents/skaile/`                        | Root orchestrator — routes by intent to domain agents                          |
| `skaileup`                        | `ai-assets-skaileup/00_skaileup-orchestrator/agents/skaileup/` | Conversational guide — discovers installed orchestrators, guides through flows |
| `skaileup-conceptualize`          | `ai-assets-skaileup/00_skaileup-orchestrator/agents/skaileup-conceptualize/` | Concept pipeline (Discovery → Blueprint)                                       |
| `skaileup-implement`              | `ai-assets-skaileup/10_impl-build/agents/skaileup-implement/` | Implementation pipeline (Setup → Verify)                                       |
| `quality`                         | `ai-assets-skaileup/13_impl-quality/agents/quality/` | Quality assurance                                                              |
| `skaile-development`              | `skaile-development/agents/skaile-development/`             | skaile-dev monorepo expert — routes tasks to skills and prog-experts           |
| `forge-project-assistant`         | `skaile-forge/agent/`                                       | In-app dev assistant for skaile-forge workspaces                                |
| `forge-project-base-orchestrator` | `skaile-forge/base-orchestrator/`                           | Home workspace guide — creates projects, explains Forge Project features       |
| `forge-project-orchestrator`      | `skaile-forge/project-orchestrator/`                        | Project workspace assistant — coding, writing, research, file management       |
| `skaile-assistant`                | `skaile-platform/agents/assistant/`                         | Skaile platform enterprise assistant — research, analysis, writing, code       |
| `researcher`                      | `knowledge-research/agents/researcher/`                     | Read-only research role (`lifecycle: role`): `{topic}` in, `{summary, sources}` out |

The four `skaileup*` and `quality` agents ship from the separate `ai-assets-skaileup` repo (paths above are relative to it); the other agents live in this repo.

The `skaileup-*` naming convention is significant: any agent installed to `.claude/agents/` whose name starts with `skaileup-` is automatically discovered by the `skaileup` guide agent at session start. New orchestrators (e.g. `skaileup-implement-supabase`) are discovered without any change to `skaileup` itself.

## Running an Agent

```bash
# Via skaile CLI (uses pichi/agent by default)
skaile run cli-concept --project-dir ./my-project

# With a different agent definition
skaile run cli-concept --project-dir ./my-project \
  --agent-dir ./ai-assets-skaileup/00_skaileup-orchestrator/agents/skaileup-conceptualize

# Via Claude Code --agent flag (after skaile install)
claude --agent skaileup                  # interactive guide — start here
claude --agent skaileup-conceptualize    # concept pipeline directly
claude --agent skaileup-implement        # implementation pipeline directly

# Via GitAgent CLI
gitagent run ai-assets-skaileup/00_skaileup-orchestrator/agents/skaileup-conceptualize/
gitagent validate ai-asset-management/agents/skaile/
```
