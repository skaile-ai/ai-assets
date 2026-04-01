---
title: Flows
description: What a flow is, how nodes and edges work, how the runner executes a flow, globals reference, and writing a new flow.
---

A **flow** is a `flow.json` file — a directed acyclic graph of skill nodes connected by typed edges. The `agent-flow-engine` computes which nodes are ready to run; `agent-runner` executes them sequentially or in parallel groups.

Flows live at `<domain>/flows/<id>.json`. The `id` field is what you pass to `skaile run <id>`.

## Structure

**Annotated `cli-concept.json` (abbreviated):**

```json
{
  "id": "cli-concept",           // used by `skaile run <id>`
  "version": "1.0",
  "name": "CLI Concept",

  "globals": {
    "research_depth": "light",   // light | standard | deep
    "approval_mode": "checkpoint", // checkpoint | auto | manual
    "subagent_mode": true,       // run each skill as a sub-agent
    "cli_mode": true             // no UI/brand/screen nodes
  },

  "modes": {
    "research": {
      "enabled": true,
      "skill": "research",
      "triggers": ["overview", "features"]  // run research before these nodes
    },
    "standards": { "enabled": false }
  },

  "nodes": [
    { "id": "overview",  "type": "skill", "data": { "skill": "overview", "label": "Project Brief",  "optional": false } },
    { "id": "features",  "type": "skill", "data": { "skill": "features", "label": "Features",       "optional": false } },
    { "id": "techstack", "type": "skill", "data": { "skill": "techstack","label": "Tech Stack",     "optional": false, "parallel_group": "identity" } },
    { "id": "datamodel", "type": "skill", "data": { "skill": "datamodel","label": "Data Model",     "optional": false } },
    { "id": "journeys",  "type": "skill", "data": { "skill": "journeys", "label": "User Journeys",  "optional": true  } }
  ],

  "edges": [
    { "id": "e1", "source": "overview",  "target": "features",  "type": "flow"     },  // hard dep
    { "id": "e2", "source": "overview",  "target": "techstack", "type": "parallel" },  // concurrent
    { "id": "e3", "source": "overview",  "target": "journeys",  "type": "optional" },  // advisory
    { "id": "e4", "source": "features",  "target": "datamodel", "type": "flow"     },
    { "id": "e5", "source": "techstack", "target": "datamodel", "type": "flow"     }
  ],

  "entry": "overview",

  "next_flows": [
    { "id": "cli", "domain": "skaileup-implementation", "label": "Implement CLI →" }
  ]
}
```

## Nodes

Each node is either a **skill node** (executed by the runner) or a **group node** (visual container, ignored by the engine):

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

| Type | Semantics |
|---|---|
| `"flow"` | Hard dependency — target is blocked until source completes |
| `"parallel"` | Soft dependency — target can start while source is still running |
| `"optional"` | Advisory — source enriches target but target can run without it |

Only `"flow"` edges block execution. `"parallel"` and `"optional"` edges never prevent a node from becoming available.

## Globals Reference

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

**Research mode** (`modes.research.enabled: true`): triggers the research skill in parallel before or alongside `triggers` nodes, writing to `_grounding/`. Useful for context gathering on unfamiliar domains.

**Standards mode** (`modes.standards.enabled: true`): triggers `standards-discover` to populate `_standards/` from the existing codebase. Useful when running implementation flows on existing codebases.

## next_flows

`next_flows` is an array of suggested follow-up flows shown after completion — not automatically executed:

```json
"next_flows": [
  { "id": "cli", "domain": "skaileup-implementation", "label": "Implement CLI →", "hint": "Scaffold the project." }
]
```

## Execution

```bash
# Via skaile CLI
skaile run cli-concept --project-dir ./my-project

# With driver and model override
skaile run prototype --project-dir ./my-project --agent omp --model claude-opus-4-6

# Dry run (show plan without executing)
skaile run cli-concept --project-dir ./my-project --dry-run

# Via skaile-agent binary
skaile-agent start cli-concept --project-dir ./my-project
```

1. Flow loaded from `ai-assets/<domain>/flows/<id>.json`
2. `computeFlowState(flow, completedIds)` → `state.available`
3. Runner groups available nodes by `parallel_group`
4. Sequential nodes: one driver instance, one at a time
5. Parallel groups: one driver instance per node, `Promise.all`
6. After each node: session saved to `.skaile/session.json`
7. Repeat until `state.done` or no nodes available

## Available Flows

| ID | Domain | Description |
|---|---|---|
| `cli-concept` | skaileup-conceptualization | Concept phase for CLI tools (brief, features, tech stack, data model) |
| `concept-only` | skaileup-conceptualization | Concept phase without implementation |
| `prototype` | skaileup-conceptualization | Full concept pipeline for quick prototypes |
| `reverse-engineer` | skaileup-conceptualization | Start from an existing codebase |
| `standard` | skaileup-implementation | Standard implementation pipeline |
| `full` | skaileup-implementation | Full implementation with all optional steps |
| `cli` | skaileup-implementation | CLI-focused implementation |
| `prototype` | skaileup-implementation | Rapid prototype implementation |

## Writing a New Flow

1. Create `<domain>/flows/<id>.json`
2. Required fields: `id`, `version`, `name`, `nodes`, `edges`
3. Point `$schema` at `../../skaileup-shared/flow.schema.json` for validation
4. Key rules:
   - Every node `id` must be unique within the flow
   - `data.skill` must match a real skill ID (or be identical to `node.id`)
   - `edges[].source` and `edges[].target` must reference existing node IDs
   - Group nodes (`type: 'group'`) are ignored by the engine — visual only
   - Files starting with `_` are skipped by `loadAllFlows`

See the [flow-engine concepts](/flow-engine/concepts/) and [flow.json reference](/flow-engine/flow-json/) for full engine documentation.
