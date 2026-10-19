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
  - path: workspaces/docs/concepts/agent-composition.md
    description: Concept page for composition bindings, the compile manifest (schema 2), `skaile rebuild` and run snapshots
  - path: workspaces/packages/workspaces/runner/src/composition/types.ts
    description: Default binding per kind (persona inline-snapshot, ruleset inline-live)
  - path: workspaces/packages/workspaces/runner/src/composition/resolve.ts
    description: How inline-snapshot and inline-live items are served and when a run snapshot applies
  - path: workspaces/packages/workspaces/runner/src/composition/compile.ts
    description: Compile manifest entries and what `skaile rebuild` captures
  - path: workspaces/packages/workspaces/cli/src/commands/rebuild.ts
    description: The `skaile rebuild` command
# The skaileup agents (skaileup, skaileup-conceptualize, skaileup-implement, quality) ship from the
# ai-assets-skaileup repo and are not sourced here. The workspaces commit below is where
# agent-prompt-sections.ts, install-agent.ts and the 0.2 manifest schema were read; the
# composition sources above (binding defaults, compile manifest, run snapshots) were read at the
# workspaces commit 85b3904b, and the concept page names the terms used in assembly row 8.
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
name: <agent-name> # Unique agent ID; kebab-case, equals the directory name
version: 1.0.0 # Semver
description: >
  One or two sentences: what the agent does and when to use it.

# optional inheritance (see `extends`); the path must stay inside the repo
extends: ../../../ai-asset-management/agents/skaile/agent.yaml

# Composition (optional) — shared persona/ruleset parts, skills, delegate agents
composes:
  - { kind: persona, ref: ../../../_agent-parts/personas/skaile-base/PERSONA.md }
  - { kind: ruleset, ref: ../../../_agent-parts/rules/skaile-safety/RULESET.md }
  - { kind: skill, ref: skill:@skaile-ai/git }
  - { kind: agent, ref: 'agent:@skaile-ai/quality', as: quality, binding: delegate }

model:
  preferred: claude-opus-4-6 # Primary model (full ID, see `model`)
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
tags: [orchestrator, workspace]

metadata: # Freeform — domain, routing notes, knowledge notes
  domain: <domain-folder>
  stage: beta
  routing_notes: where to route which kind of request
```

This example is deliberately generic. The shipped agents are the faithful references:
`skaile-development/agents/skaile-development/agent.yaml` (extends, family-alias model, delegation
`explicit`), `ai-asset-management/agents/skaile/agent.yaml` (persona, ruleset and delegate
composition) and `knowledge-research/agents/researcher/agent.yaml` (a role).

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

Persona and ruleset parts live at the ai-assets root as `_agent-parts/personas/<name>/PERSONA.md`
and `_agent-parts/rules/<name>/RULESET.md` and are reused across agents (an agent's own `RULES.md` is never a standalone
asset). `scripts/presets-resolve.test.sh` checks that every ref and every `extends:` path resolves.

### `model`

| Field                     | Description                                |
| ------------------------- | ------------------------------------------ |
| `preferred`               | Primary model ID — used unless unavailable |
| `fallback`                | Ordered list of fallback models            |
| `constraints.temperature` | Sampling temperature (0.0–1.0)             |
| `constraints.max_tokens`  | Maximum output tokens per turn             |

Two forms of model ID are in use. A full ID (`claude-opus-4-6`) pins a generation; a family alias
(`opus`, `sonnet`) auto-rolls to the current top model of that family and is the only form that
works for a model newer than the manifest's `ModelIdSchema` knows. Use the full ID by default; use the alias, with a
`# Family alias:` comment saying why, when the agent should follow the newest model
(`skaile-development` and the three `skaile-forge` agents do).

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

Inherits from a parent `agent.yaml`, resolved relative to the child's directory. Only `skaile install` reads it (the runner's composition resolver does not), and it skips the parent with a warning when the path does not exist. Merge rules: scalars and every object-valued key (e.g. `model`, `delegation`, `runtime`, `tools`, `metadata`) are replaced wholesale by the child's value, not deep-merged, so restate every sub-field you still want; `composes` is parent + child, deduplicated by kind, ref and slot id; every other array (e.g. `tags`) is unioned, so the child's `tags` carry the parent's too.

Inheritance covers the manifest only. The parent's `SOUL.md`, `RULES.md`, `DUTIES.md` and `knowledge/` are not read: the install renders the child directory's own files. The merged `composes` drives the install (delegate sub-agent files, abilities, contracts, MCP servers) but the install renders no composed `persona` or `ruleset` blocks, and the runner builds those from the agent's own `agent.yaml` without resolving `extends` (see System Prompt Assembly). A `persona` or `ruleset` item that only arrives through `extends` is therefore never rendered; declare it in the child if it must reach the prompt. `skaile-development` inherits the `skaile` agent's persona and ruleset this way and ships without them.

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
version: '1.0'
description: What this knowledge base holds
documents:
  - path: concept_structure.md
    priority: 1 # lower = listed first
  - path: golden_principles.md
    priority: 2
  - path: iron_laws.md
    priority: 3
```

`documents:` is the one shape to write: the runtime reads each entry's `path:` and optional
`priority:`, and recognises `documents: []` as a valid, empty knowledge base (as in the two shipped
`index.yaml` files, `skaile-forge/agent/` and `skaile-platform/agents/assistant/`, which are
empty on purpose and carry no `priority`). Files not listed in `index.yaml` get a default priority of
`99` and are listed last. `retrieval:` (for example `strategy: load_all`) is not read by
`@skaile/workspaces`; leave it out.

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
| 8   | composed      | inline blocks from `composes:`, including `persona` parts (`PERSONA.md`, served from the compile manifest: `skaile rebuild` captures it) and `ruleset` parts (`RULESET.md`, read from its source at each new run; a resumed run reuses the bytes); the runner resolves them at session start, install renders none; see the compile manifest in workspaces `docs/concepts/agent-composition.md` |
| 9   | extensions    | per-project `prompt-extensions`                                                        |

Which part builds what: the installed agent file carries sections 1 and 3-7 (and 9, when the
workspace configures `prompt-extensions`). `<MEMORY>` (2) and the composed blocks (8) need a
resolved agent instance, so the runner adds them when the session starts, from the agent's own
`agent.yaml` (it does not follow `extends`, see above). A host may also add prompt text of its own
around these sections: skaile-forge's work channel, for example, adds the list of the user's other
workspaces and a working-directory constraint as a system-prompt extension. Section 1 is
therefore the first section of the agent part, not necessarily the first thing the model reads.

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
