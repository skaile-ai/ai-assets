# Flows — How to Read and Write Them

Flows are self-contained execution templates. Each flow file defines a complete,
runnable path through the skill tree for a specific use case (MVP, prototype, concept-only, etc.).

**Pipeline.json is removed.** All skill metadata that was in pipeline.json now lives on
the skill nodes inside each flow. Global agent config lives in `skaileup-shared/agent-config.json`.

---

## Skill Name Convention

Skills are always referenced by their **canonical name** — a flat kebab-case string matching
the `name` field in the skill's `SKILL.md` frontmatter. Never use file paths.

```jsonc
// correct
{ "skill": "datamodel" }

// wrong — never use paths
{ "skill": "concept/blueprint/datamodel" }
{ "skill": "skaileup-conceptualization/skills/30_blueprint/cf_datamodel" }
```

The orchestrator resolves names to skill directories by scanning all `SKILL.md` frontmatter
files at startup and building a name→path registry.

### Canonical Skill Registry

| Name | Domain | Description |
|---|---|---|
| `overview` | concept | Project brief, goals, comparable products |
| `research` | concept | Parallel research mode — domain, competitors, audiences |
| `brand-visual` | concept | Visual identity — colors, fonts, tokens |
| `brand-behavioral` | concept | Communication tone, micro-copy guidelines |
| `journeys` | concept | User journey maps, stories.json, EARS criteria |
| `features` | concept | Feature specs in numbered groups |
| `behaviors` | concept | Behavioral specs (.allium format) |
| `screens` | concept | Screen specs with component inventories |
| `components` | concept | Reusable component inventory |
| `mock` | concept | Interactive HTML mockups |
| `storybook` | concept | Living Storybook prototype (3-layer) |
| `techstack` | concept | Tech stack selection and reasoning |
| `architecture` | concept | System architecture, modules, data flow |
| `datamodel` | concept | Data model (stack-aware schema output) |
| `reverse-engineer` | concept | Extract full concept from existing codebase |
| `add-feature` | concept | Add/modify feature in live concept |
| `concept-orchestrator` | concept | Pipeline controller for concept phase |
| `scaffold` | implement | Project scaffolding |
| `foundation` | implement | Brand tokens, auth config, app shell |
| `design` | implement | High-fidelity UI from production component library |
| `infrastructure` | implement | Custom backend modules (conditional) |
| `migrate` | implement | Database migrations |
| `seed` | implement | Load seed data scenarios |
| `implement-feature` | implement | TDD feature implementation |
| `verify` | implement | Post-implementation verification |
| `implement-orchestrator` | implement | Pipeline controller for implementation phase |
| `audit` | quality | Static code + structure analysis |
| `e2e` | quality | Browser-based E2E journey testing |
| `ready` | quality | Pre-flight readiness gate |
| `review` | quality | Concept structure audit + gardening |
| `sync` | quality | Cross-reference repair |
| `test-unit` | quality | Unit test generation and execution |
| `test-integration` | quality | Integration test generation and execution |
| `test-plan` | quality | Test plan from features, screens, data model |
| `compile-validators` | quality | Compile all validator.py files into unified suite |
| `standards-discover` | standards | Discover codebase conventions → _standards/ |
| `standards-inject` | standards | Match standards to requesting skill |

---

## Where Flows Live

| Domain | Path | Flows |
|---|---|---|
| Conceptualization | `skaileup-conceptualization/flows/` | `concept-only`, `prototype`, `cli-concept`, `reverse-engineer` |
| Implementation | `skaileup-implementation/flows/` | `standard`, `full`, `cli`, `prototype` |
| Quality | `skaileup-quality/flows/` | `audit`, `review`, `readiness` (add as needed) |
| Schema | `skaileup-shared/flow.schema.json` | JSON Schema for all flow files |

Each concept flow ends with a `next_flows` array pointing to the appropriate implementation
(or other follow-on) flows. Use these to chain domains without bundling them into one file.

### Flow Catalogue

**Concept flows** (`skaileup-conceptualization/flows/`):

| ID | Description |
|---|---|
| `concept-only` | Full concept pipeline — brief through screens, components, storybook |
| `prototype` | Fast concept — brief, brand, features, screens, mockups |
| `cli-concept` | CLI concept — brief, features, tech stack, data model (no UI) |
| `reverse-engineer` | Extract full concept from an existing codebase |

**Implementation flows** (`skaileup-implementation/flows/`):

| ID | Description |
|---|---|
| `standard` | Scaffold, foundation, migrate/seed, feature TDD, tests, verify |
| `full` | Readiness gate, scaffold, foundation, design(opt), full test suite, verify |
| `cli` | CLI scaffold, foundation(headless,opt), feature TDD, unit+integration, verify |
| `prototype` | Minimal: scaffold, implement, smoke E2E — fast path for user testing |

---

## Flow File Structure

```jsonc
{
  "id": "standard",                  // kebab-case, matches filename
  "name": "Standard Implementation", // shown in flow picker UI
  "description": "...",
  "meta": { ... },                   // icon, category, tags, onboarding dialog
  "globals": { ... },                // parameters injected into ALL nodes
  "modes": { ... },                  // parallel modes (research, standards)
  "nodes": [ ... ],                  // skill nodes + visual group containers
  "edges": [ ... ],                  // directed execution graph
  "entry": "scaffold",               // first node to execute
  "next_flows": [                    // suggested follow-on flows shown at completion
    { "id": "audit", "domain": "skaileup-quality", "label": "Audit →", "hint": "Run static analysis." }
  ]
}
```

---

## Skill Node — Full Field Reference

```jsonc
{
  "id": "datamodel",
  "type": "skill",
  "parentNode": "g-concept",           // visual group container
  "position": { "x": 840, "y": 200 }, // canvas position
  "data": {

    // ── Identity ──────────────────────────────────────────────────────────
    "skill": "concept/3_blueprint/3_datamodel",  // skill path from skills root
    "label": "Data Model",                        // display label in editor

    // ── Execution control ─────────────────────────────────────────────────
    "optional": false,          // orchestrator may skip if user opts out
    "parallel_group": "...",    // nodes with same group run concurrently
    "subagent": true,           // run in isolated subagent context

    // ── Location ──────────────────────────────────────────────────────────
    "writes": "3_blueprint/3_datamodel/",
    // The _concept/ subfolder this skill writes to.
    // Used by the orchestrator for gate checks and UI progress display.

    // ── Prerequisites ─────────────────────────────────────────────────────
    "requires": [
      "2_experience/2_features/",
      "3_blueprint/1_techstack/stack.md"
    ],
    // File or folder paths (relative to _concept/) that must exist on disk
    // before this skill can run. Orchestrator checks these before dispatching.
    // Different from edges — edges express preferred order; requires expresses
    // hard blockers (skill will fail or produce garbage without them).

    // ── Research grounding ────────────────────────────────────────────────
    "grounding_folder": "datamodel/",
    // Subfolder within _grounding/ for this skill's research and user_input.json.
    // The orchestrator saves user dialog inputs to _grounding/{grounding_folder}/user_input.json
    // and research results to _grounding/{grounding_folder}/*.md

    // ── User inputs ───────────────────────────────────────────────────────
    "user_inputs": {
      "dialog": [
        {
          "id": "field_id",
          "label": "Human-readable label",
          "type": "text | textarea | select | multiselect | boolean | number",
          "required": false,
          "options": [],      // for select / multiselect
          "default": null,
          "hint": "Help text shown in the UI form"
        }
      ],
      "files": [
        "3_blueprint/3_datamodel/model.json"  // existing _concept/ files user can upload/confirm
      ]
    },
    // Dialog fields shown in the UI before this skill starts.
    // Answers are saved to _grounding/{grounding_folder}/user_input.json
    // and read back by the skill at runtime — no need to re-ask.
    // Omit user_inputs entirely if the skill needs no pre-collection.

    // ── Feedback loops ────────────────────────────────────────────────────
    "feedback": [
      {
        "updates": "2_experience/2_features/**/*.md",
        "field": "data_entities",
        "description": "Sets data_entities[] in each feature file that the model serves"
      }
    ],
    // Upstream files this skill modifies AFTER completing its own writes.
    // Orchestrator executes these cross-reference updates post-completion.
    // See skaileup-shared/contracts/feedback_loop.md for the full protocol.

    // ── Runtime parameters ────────────────────────────────────────────────
    "parameters": {
      "tdd": true
      // Skill-specific config. Merged over globals (node wins on conflict).
    },

    // ── Prerequisite overrides ─────────────────────────────────────────────
    "overrides": {
      "skip_checks": [
        "_concept/1_discovery/1_overview/brief.md"
        // Paths listed here are skipped during gate validation for this node.
        // Use when a preceding node in the flow guarantees the file exists,
        // making the hard-gate check redundant and avoiding false failures.
      ]
    }
    // Use overrides sparingly. Prefer expressing ordering guarantees via edges
    // (edge ordering ensures a node runs after its dependency). Only add
    // skip_checks when the dependency is produced by a node that is not a
    // direct predecessor in the edge graph (e.g. produced by a parallel branch
    // or guaranteed by the flow entry node).

  }
}
```

---

## Edge Types

| Type | Meaning |
|---|---|
| `flow` | Required sequence — target cannot start until source completes |
| `optional` | Target may be skipped based on user preference or `optional: true` on node |
| `parallel` | Source and target can execute concurrently once their own prerequisites are met |

**Edges express preferred execution order. `requires` expresses hard blockers.**
A skill with `requires` checks those paths regardless of which edges reached it.

### Overrides vs. edge ordering

Use `data.overrides.skip_checks` when a node's prerequisite is guaranteed by the flow but the
gate would still fire (e.g. the producer is in a parallel branch with no direct edge to the
consumer). Do **not** use overrides as a shortcut to silence gates that represent real
dependencies.

| Situation | Correct approach |
|---|---|
| Node B depends on Node A's output and A runs first | Add a `flow` edge A → B. No override needed. |
| Node B depends on Node A's output; A runs in a parallel branch | Add `skip_checks` for A's output path in B's `overrides`. |
| Node B depends on the flow entry node's output | Add `skip_checks` for the entry output in B's `overrides`. |
| Gate fires but the file will not exist until user uploads it | Keep the gate; surface the error to the user. Never skip. |

---

## Globals and Parameter Merging

At runtime, each node receives a merged parameter object:

```
effective_params = globals + node.data.parameters
```

Node parameters win on conflict. `user_inputs` dialog values are also merged in
(from `_grounding/{grounding_folder}/user_input.json` if already collected).

---

## Modes (Parallel Utilities)

`modes.research` and `modes.standards` are not nodes — they run alongside the graph:

- **research** — fires the research skill in parallel after specified trigger nodes. Results go to `_grounding/general/` and `_grounding/{grounding_folder}/`.
- **standards** — fires once after `trigger_after` node when the flow is running against an existing codebase. Results go to `_standards/`.

---

## Standalone Mode

When a skill is invoked directly (no flow, no orchestrator):

1. Skill reads its `requires` from its own SKILL.md frontmatter
2. Checks each path exists in `_concept/`
3. Runs if gates pass; otherwise names the missing prerequisite
4. On completion, suggests next skills using the orchestrator

See `skaileup-shared/agent-config.json` for standalone_mode settings.

---

## Writing a New Flow

1. Copy the closest existing flow as a starting point
2. Set `id` (kebab-case, matches filename), `name`, `description`, `meta`
3. Set `globals` — choose `research_depth`, `approval_mode`, `subagent_mode`
4. Configure `modes.research` — which nodes trigger parallel research
5. Add nodes:
   - Every skill node needs at minimum: `skill`, `writes`, `requires`
   - Add `user_inputs` only for skills that need pre-collection
   - Add `feedback` only for skills that modify upstream files
   - Add `grounding_folder` for skills that save research or user inputs
6. Add edges — use `parallel` for truly concurrent paths, `optional` for skippable steps
7. Set `entry` to the first node

**Validate:** `ajv validate --schema flow.schema.json --data your-flow.json`

---

## Updating an Existing Flow

When a skill changes its `writes`, `requires`, or `user_inputs`:
- Update the node `data` in **every flow** that includes that skill
- Update MIGRATION.md with what changed and why

When adding a new skill to an existing flow:
- Add the node with full metadata
- Wire edges from its dependencies and to its dependents
- If it has feedback loops, add the `feedback` field

---

## Relationship to SKILL.md

Each skill's `SKILL.md` frontmatter also declares `user_inputs` — this is the
**standalone default**. Flow node `user_inputs` overrides it for that specific flow context
(e.g., a prototype flow may ask fewer questions than a full-product flow).

The `writes`, `requires`, `grounding_folder`, and `feedback` fields on a skill node
should match the skill's SKILL.md Prerequisites and Outputs sections.
If they diverge, the flow takes precedence at runtime; SKILL.md is documentation.
