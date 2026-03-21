---
title: Flows
description: What a flow is, how nodes and edges work, how the runner executes a flow, and the available flows in ai-resources.
---

A **flow** is a `flow.json` file — a directed acyclic graph of skill nodes connected by typed edges. The `skaile-agent-flow-engine` computes which nodes are ready to run; `skaile-agent-runner` executes them sequentially or in parallel groups.

Flows live at `<domain>/flows/<id>.json`. The `id` field is what you pass to `skaile run <id>`.

## Structure

```json
{
  "id": "cli-concept",
  "version": "1.0",
  "name": "CLI Concept",
  "description": "Concept phase for CLI tools.",
  "globals": { ... },
  "modes": { ... },
  "nodes": [ ... ],
  "edges": [ ... ],
  "entry": "overview",
  "next_flows": [ ... ]
}
```

## Nodes

Each node is either a **skill node** (executed by the runner) or a **group node** (visual container, ignored by the engine):

```json
{ "id": "overview", "type": "skill", "data": {
    "skill": "overview",
    "label": "Project Brief",
    "optional": false,
    "parallel_group": "phase-1",
    "parameters": { "cli_mode": true }
}}
```

| Field | Description |
|---|---|
| `id` | Unique node identifier within the flow |
| `type` | `"skill"` (executed) or `"group"` (visual only) |
| `data.skill` | Skill ID passed to the runner for resolution (can differ from `id`) |
| `data.label` | Human-readable name shown in `flow show` output |
| `data.optional` | If `true`, node can be skipped without blocking the flow |
| `data.parallel_group` | Nodes sharing this tag run concurrently via `Promise.all` |
| `data.parameters` | Extra context injected into the skill prompt |

## Edges

```json
{ "id": "e1", "source": "overview", "target": "features", "type": "flow" }
```

| Type | Semantics |
|---|---|
| `"flow"` | Hard dependency — target is blocked until source completes |
| `"parallel"` | Soft dependency — target can start while source is still running |
| `"optional"` | Advisory — source enriches target but target can run without it |

Only `"flow"` edges block execution. `"parallel"` and `"optional"` edges never prevent a node from becoming available.

## Globals

Flow-level configuration passed to skills and the orchestrator:

| Field | Values | Description |
|---|---|---|
| `research_depth` | `light` / `standard` / `deep` | How much research context to gather |
| `approval_mode` | `checkpoint` / `auto` / `manual` | When the orchestrator pauses for user approval |
| `auto_review` | boolean | Run quality review automatically after each node |
| `subagent_mode` | boolean | Execute each node as a sub-agent session |
| `verbosity` | `quiet` / `standard` / `verbose` | Log output level |
| `cli_mode` | boolean | Suppress UI/brand/screen instructions |

## Modes

**Research mode** — when `modes.research.enabled: true`, a research skill runs in parallel before or alongside `triggers` nodes, writing to `_grounding/`.

**Standards mode** — when `modes.standards.enabled: true`, `standards-discover` runs in parallel to populate `_standards/` from the existing codebase.

## next_flows

`next_flows` is an array of suggested follow-up flows shown after completion — not automatically executed:

```json
"next_flows": [
  { "id": "cli", "domain": "dev-implementation", "label": "Implement CLI →", "hint": "Scaffold the project." }
]
```

## Execution

```bash
skaile run cli-concept --project-dir ./my-project
skaile run prototype --project-dir ./my-project --agent omp --model claude-opus-4-6
skaile run cli-concept --project-dir ./my-project --dry-run
```

1. Flow loaded from `ai-resources/<domain>/flows/<id>.json`
2. `computeFlowState(flow, completedIds)` → `state.available`
3. Runner groups available nodes by `parallel_group`
4. Sequential nodes: one driver instance, one at a time
5. Parallel groups: one driver instance per node, `Promise.all`
6. After each node: session saved to `.skaile/session.json`
7. Repeat until `state.done` or no nodes available

## Available Flows

| ID | Domain | Description |
|---|---|---|
| `cli-concept` | dev-conceptualization | Concept phase for CLI tools (brief, features, tech stack, data model) |
| `concept-only` | dev-conceptualization | Concept phase without implementation |
| `prototype` | dev-conceptualization | Full concept pipeline for quick prototypes |
| `reverse-engineer` | dev-conceptualization | Start from an existing codebase |
| `standard` | dev-implementation | Standard implementation pipeline |
| `full` | dev-implementation | Full implementation with all optional steps |
| `cli` | dev-implementation | CLI-focused implementation |
| `prototype` | dev-implementation | Rapid prototype implementation |

## Writing a New Flow

1. Create `<domain>/flows/<id>.json`
2. Required fields: `id`, `version`, `name`, `nodes`, `edges`
3. Point `$schema` at `../../dev-shared/flow.schema.json` for validation
4. Key rules:
   - Every node `id` must be unique within the flow
   - `data.skill` must match a real skill ID (or be identical to `node.id`)
   - `edges[].source` and `edges[].target` must reference existing node IDs
   - Group nodes (`type: 'group'`) are ignored by the engine — visual only
   - Files starting with `_` are skipped by `loadAllFlows`

See the [flow-engine concepts](/flow-engine/concepts/) and [flow.json reference](/flow-engine/flow-json/) for full engine documentation.
