---
title: Flows
description: Available flows, how to run them, reading flow JSON, globals reference, and writing a new flow.
---

## Available Flows

| ID | Domain | Nodes | Description |
|---|---|---|---|
| `cli-concept` | dev-conceptualization | 6 skills | Concept phase for CLI tools (brief, features, tech stack, data model) |
| `concept-only` | dev-conceptualization | varies | Concept phase without implementation |
| `prototype` | dev-conceptualization | varies | Full concept pipeline for quick prototypes |
| `reverse-engineer` | dev-conceptualization | varies | Start from an existing codebase |
| `standard` | dev-implementation | varies | Standard implementation pipeline |
| `full` | dev-implementation | varies | Full implementation with all optional steps |
| `cli` | dev-implementation | varies | CLI-focused implementation |
| `prototype` | dev-implementation | varies | Rapid prototype implementation |

## How to Run

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

## Reading a Flow Definition

Flows are JSON files at `<domain>/flows/<id>.json`. Full field reference in the [flow-engine docs](/flow-engine/flow-json/).

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
    { "id": "cli", "domain": "dev-implementation", "label": "Implement CLI →" }
  ]
}
```

## Globals Reference

| Field | Values | Description |
|---|---|---|
| `research_depth` | `'light'` \| `'standard'` \| `'deep'` | How much research context to gather |
| `approval_mode` | `'checkpoint'` \| `'auto'` \| `'manual'` | When/how the orchestrator pauses for user approval |
| `auto_review` | `boolean` | Run quality review automatically after each node |
| `subagent_mode` | `boolean` | Execute each node in a dedicated sub-agent session |
| `verbosity` | `'quiet'` \| `'standard'` \| `'verbose'` | Log verbosity for skill output |
| `cli_mode` | `boolean` | Suppress UI/brand/screen-related instructions |

## Modes

**Research mode** (`modes.research.enabled: true`): triggers the research skill in parallel before/during specified flow nodes. Results land in `_grounding/`. Useful for context gathering on unfamiliar domains.

**Standards mode** (`modes.standards.enabled: true`): triggers `cf_discover_standards` to populate `_standards/` with discovered codebase conventions. Useful when running implementation flows on existing codebases.

## next_flows

`next_flows` is an array of suggested follow-up flows shown after completion:

```json
"next_flows": [
  { "id": "cli", "domain": "dev-implementation", "label": "Implement CLI →", "hint": "Scaffold the project." }
]
```

Not automatically executed — informational for the CLI and UI.

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
