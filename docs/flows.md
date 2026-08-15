---
title: Flows
description: The Flow Engine v2 contract — the seven node kinds, node-level instructions, declared assets, contracts, gates and checks — plus edges, defaults, how the runner executes a flow, and how to write a new one.
sidebar:
  order: 20
---

A **flow** is a definition file — an acyclic graph of **nodes** connected by typed edges. The flow engine computes which nodes are ready to run; the runner executes them and parks at gates for a human.

Flow files live in a `flows/` directory, either flat (`<root>/flows/`) or domain-nested (`<root>/<domain>/flows/`), and are named after the flow's `id`: `<id>.flow.yaml`, `<id>.flow.json`, or legacy `<id>.json`. Files whose name starts with `_` are skipped (drafts / disabled). The `id` is what you pass to `skaile run <id>`.

## The contract

The authority on shape is the published JSON Schema — not this page:

```
@skaile/workspaces/dist/factory-assets/connectors/flow/contract/flow.v2.schema.json
```

Its `$id` is `https://skaile.ai/schemas/flow/v2.json`; point a definition's `$schema` at that. Read the schema when you need the exact field list. Every definition — YAML or JSON, loaded by the CLI or authored on the platform — is parsed through this one contract, so there is a single validator and a single shape.

Required at the top level: `schemaVersion` (always `2`), `id`, `version`, `name`, `nodes`, `edges`. Optional: `$schema`, `description`, `input`, `output`, `defaults`, `entry`, `meta`.

A node requires `id`, `label`, `description` and `run`, and may add `phase`, `contract`, `gate` and `control`. An edge requires `id`, `source`, `target` and `type`.

**Validation is strict.** Every object in the contract is closed, so an unrecognized key anywhere — top level, node, or inside `run` — is a hard error reporting the authored path, dot-separated, with the empty path rendering as `<root>`:

```
<root>: Unrecognized key: "whoops"
nodes.0.run: Invalid discriminator value. Expected 'agent' | 'subprompt' | … | 'sub-flow'
```

There is no lenient mode and no silent drop. Never invent a field.

## Node kinds

`run` is a discriminated union on `run.kind`. Exactly one of these seven:

| `run.kind` | What it is for | Required beyond `kind` |
|---|---|---|
| `agent` | The one kind that needs the session agent's conversation — a turn of real work | `instruction` |
| `subprompt` | A model call on any model with no conversation, only bound inputs — classify a document without burning the expensive agent's context | `instruction` |
| `function` | Deterministic code run for its effect — no model call | `command` |
| `check` | Deterministic code producing pass/fail, no human in it; it decides *whether a human is asked* | `command` |
| `gate` | Executed by nobody — parks the flow for a human decision | `prompt`, `schema` |
| `router` | Branch, so two cases take different paths through one flow | `routes` (≥1 `{ when, target }`) |
| `sub-flow` | Executes no work of its own — starts a nested run of another flow and waits for it | `flow` |

The taxonomy falls out of two questions: does this node cost a model call, and does it need the session agent's conversation? Exactly one kind — `agent` — needs the conversation.

Each arm is closed to its own fields. Only `agent` and `subprompt` accept `model` (`small` | `default` | `deep`). `function` and `check` also accept `runtime` (`shell` | `node` | `python`), `cwd`, `successExit` and `capture`; `check` adds `onFail` (`escalate` | `fail`, default `escalate`). A `gate`'s `schema` is a union of `text`, `choice` (requires `options`), `form` (requires `fields`) and `file`. A `sub-flow` names its child by slug in `flow` and accepts `passContext`. Every kind accepts `assets`.

### Where the instruction lives

**The work goes in `run.instruction` (or `run.command`). `description` is a human label and is never executed.** This is the single most common authoring error and it is silent: a node whose real intent sits in `description` is structurally valid and does nothing useful.

A node is no longer identified by the skill it names. It carries its own instruction, and a skill is one entry in `run.assets`.

### Assets are declared references

`run.assets` is a list of asset references — never inlined content. A bare asset id is valid; normalized legacy flows use `kind:name` refs (`skill:auto-ship`, `flow:legal-escalation`). On the platform the list is resolved once, at run-group creation, over the transitive closure through `sub-flow` nodes, and creation is refused with the missing refs named.

### Contracts, gates and checks

A node declares an output schema **only when something downstream consumes it** — a router branching on it, a check comparing it, or a binding referencing it. No consumer means no schema; a flow with no routers and no checks stays pure prose. Node-level typing lives under `contract`: `requires` (guard expressions, each `{ expr, message? }`), `input` (bindings consumed) and `output` (`fields`, plus `artifacts` with a `lifetime` of `session` or `persistent`).

A **gate** parks the flow and a human decides; the node's `gate.approval` is `none`, `optional` or `mandatory`, and a mandatory gate always stops for a human even in autonomous mode. A **check** is deterministic code producing pass or fail with no human in it — it decides *whether a human is asked at all*. `onFail: escalate` (the default) parks at a gate with the check's output attached; `onFail: fail` terminates the run.

`control` carries `optional`, `retries`, `timeoutSec` and `parallelGroup`.

## Example (v2 — write this shape)

```yaml
$schema: https://skaile.ai/schemas/flow/v2.json
schemaVersion: 2
id: obligation-review
version: 1.0.0
name: Obligation Review
entry: extract
nodes:
  - id: extract
    label: Extract obligations
    description: Pull the obligations out of the contract.
    run:
      kind: agent
      instruction: List every payment obligation with its due date.
      model: default
      assets:
        - skill:contract-analysis
  - id: verify
    label: Verify totals
    description: Check the totals add up.
    run:
      kind: check
      command: python verify_totals.py
      onFail: escalate
  - id: signoff
    label: Sign-off
    description: Ask a reviewer to approve.
    run:
      kind: gate
      prompt: Approve this obligation register?
      schema:
        kind: text
    gate:
      approval: mandatory
edges:
  - { id: e1, source: extract, target: verify, type: flow }
  - { id: e2, source: verify, target: signoff, type: flow }
```

## Edges

| Type | Semantics |
|---|---|
| `"flow"` | Hard dependency — target is blocked until source completes |
| `"parallel"` | Soft dependency — target can start while source is still running |
| `"optional"` | Advisory — source enriches target but target can run without it |

Only `"flow"` edges block execution. `"parallel"` and `"optional"` edges never prevent a node from becoming available. Edges must reference existing node ids and form a DAG; a cycle is rejected with `flow graph must be acyclic`, pathed at the edge that closes it.

## defaults

Flow-level configuration, all optional:

| Field | Values | Description |
|---|---|---|
| `approval` | `checkpoint` / `auto` / `manual` | When the orchestrator pauses for user approval |
| `researchDepth` | string | How much research context to gather |
| `autoReview` | boolean | Run quality review automatically after each node |
| `verbosity` | string | Log output level |
| `cliMode` | boolean | Suppress UI/brand/screen instructions |
| `run_input` | any | Input payload handed to the run |
| `parameters` | object | Free-form values; anything the normalizer does not recognize lands here |

`meta` carries `icon`, `category`, `tags`, `stage` and `onboarding`.

**`modes` (research / standards) and `next_flows` have no v2 equivalent.** They were v1-only conveniences; under strict v2 they are unrecognized keys and fail the parse. Do not author them.

## Legacy (v1) definitions

The flow files shipped in `ai-assets` and `ai-assets-skaileup` are **still v1** and keep running: a definition with no `schemaVersion` is put through a compatibility normalizer before validation. New flows — and anything an agent authors — are v2.

This is the legacy shape (abbreviated from `testing/flows/test-echo.flow.yaml`), shown so you can recognize it, **not** so you can copy it:

```yaml
id: test-echo
version: 1.0.0
name: Test Echo Flow
nodes:
  - id: ask-name
    type: skill
    data:
      skill: test-ask-name
      label: Ask for a name
      optional: false
  - id: write-greeting
    type: skill
    data:
      skill: test-write-greeting
      label: Write greeting file
      optional: false
edges:
  - { id: e1, source: ask-name, target: write-greeting, type: flow }
entry: ask-name
```

How it maps:

| v1 | v2 |
|---|---|
| `globals` | `defaults` — `approval_mode`→`approval` (`auto_approve`→`auto`), `research_depth`→`researchDepth`, `auto_review`→`autoReview`, `verbosity`, `cli_mode`→`cliMode`, `run_input`; anything unrecognized lands in `defaults.parameters`, **except `subagent_mode`, which is dropped outright** |
| `${key}` bindings inside parameters | rewritten to `${defaults.<mapped>}` or `${defaults.parameters.<key>}` |
| `metadata` / `meta` | `meta` (`icon`, `category`, `tags`, `stage`, `onboarding`) |
| top-level `requires` list | merged into the entry node's `run.assets` (the first node if there is no `entry`) |
| node `type: "skill"` + `data.skill` | `run.kind: "agent"` + `skill:<name>` in `run.assets` |
| node `data.parameters.instructions` | `run.instruction` — and if there is none, `run.instruction` falls back to the node's `description`, the one case where a description *is* executed |
| node `data.parameters` (rest) | `contract.input` |
| node `data.approval.mandatory` | `gate.approval: "mandatory"` |
| node `data.optional` / `parallel_group` | `control.optional` / `control.parallelGroup` |
| node `data.subagent`, flow-level `subagent_mode` | **dropped** — neither has a v2 equivalent; both were declared-but-unread v1 fields |
| node `type: "sub-flow"` + `data.flow` | `run.kind: "sub-flow"` + `flow:<id>` in `run.assets` |
| any other node `type` (e.g. `group`, `router`) | inert `router` placeholder, `contract.requires: [{ expr: "false" }]`, `control.optional: true` |
| edges | preserved; a missing `type` defaults to `optional`, a missing `id` is generated |

Two consequences are worth internalising before you rely on the normalizer. A `type: "group"` visual container has no v2 equivalent — it becomes the inert placeholder. And that placeholder has **no `run.instruction` field at all**, so instruction text authored on a non-`skill` node is not carried forward *as an instruction* — it survives only as inert data under `contract.input`. The node never becomes available and the work silently does not happen.

## Execution

```bash
# Run a flow
skaile run obligation-review --project-dir ./my-project

# Driver and model override
skaile run obligation-review --project-dir ./my-project --driver omp --model claude-opus-4-6

# Dry run (print the flow and its currently available nodes, execute nothing)
skaile run obligation-review --project-dir ./my-project --dry-run
```

1. `findFlowFile` locates `<id>.flow.yaml` / `.flow.json` / `.json` across every content root.
2. `loadFlow` reads and parses it through the published contract; a parse failure names the path that caused it.
3. `runFlow` starts a session and drives the flow through the orchestrator, autonomous by default.
4. `computeFlowState(flow, completedIds, runningIds, skippedIds)` decides which nodes are available at each step.
5. First-class `gate` nodes always park for a human decision — including in autonomous mode.
6. Session state is written to `.skaile/session.json` in the project directory, so a run can be resumed.

## Available flows

Use `skaile flow list` for the live set — it discovers flows across every content root, and a hard-coded table here rots.

This repo ships one flow, the test fixture `testing/flows/test-echo.flow.yaml`. The skaileup concept / build / quality pipeline flows live in the separate `ai-assets-skaileup` repo under `skaileup/flows/`.

## Writing a new flow

1. Create `<domain>/flows/<id>.flow.yaml` (or `.flow.json`). The file name must match the flow's `id`.
2. Declare `schemaVersion: 2` and point `$schema` at `https://skaile.ai/schemas/flow/v2.json`.
3. Required fields: `schemaVersion`, `id`, `version`, `name`, `nodes`, `edges`.
4. Key rules:
   - Every node `id` is unique within the flow; every node needs `id`, `label`, `description` and `run`.
   - Put the work in `run.instruction` or `run.command` — never in `description`.
   - `edges[].source` and `edges[].target` must reference existing node ids, and the graph must be acyclic.
   - Declare the assets a node needs as references in `run.assets`; never inline asset content.
   - Only add a `contract.output` schema where something downstream consumes it.
   - Files starting with `_` are skipped by `loadAllFlows`.
5. Validate by loading it: `skaile run <id> --project-dir <dir> --dry-run` parses through the contract and prints the available nodes without executing.

If a flow is authored by an agent through the platform's flow-authoring capabilities rather than by hand, the `schemaVersion` declaration is mandatory: those capabilities refuse a definition without it, precisely so the v1 normalizer cannot turn the definition into inert placeholders that run nothing.

`ai-assets-skaileup/skaileup/contracts/flow.schema.json` is not a second runtime — its only consumer is that repo's authoring-time verifier script.

See the [flow-engine concepts](/integrate/flow-engine/concepts/) for engine internals, and `platform/docs/flow-authoring-v2.md` for the platform-side authoring reference.
