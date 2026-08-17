# Flows, Runs & Run Groups

Repeatable multi-node work. A **flow** is the definition (an acyclic graph of **nodes**); a
run is one execution of it inside a session; a **run group** fans one flow out over many
inputs, each in its own temporary session — unattended batch processing with human
checkpoints.

## Flow definitions

- A flow is an asset (like a skill) and can be owned at any scope: Personal, Session,
  Project, Team, or Organization. Owners can publish upward with **Share to org**.
- The org-level **Flows** page lists flow definitions and opens each into a graph view
  (nodes, dependencies, gate badges). Authorized users (scope admins) can edit visually:
  add/connect/remove nodes, change node settings, undo/redo. Flows import and export as
  YAML or JSON.
- A flow can be **locked**. While it is locked the definition is immutable — no edit, no
  revision by an agent, no exception; unlocking is a deliberate human act. Each stored
  definition also carries a content hash, so "which definition did this run execute" is
  always answerable.
- The Flows page is currently feature-flagged (`ff_flows`) — if a user cannot see it, the
  flag is off for their deployment; don't present it as missing functionality.

## The shape of a definition

The authority on shape is the published JSON Schema, not this file:

```
@skaile/workspaces/dist/factory-assets/connectors/flow/contract/flow.v2.schema.json
```

Read it when you need the exact field list. Everything below is what a schema cannot say:
which construct to reach for, and where authoring goes quietly wrong.

Required at the top level: `schemaVersion` (always `2`), `id`, `version`, `name`, `nodes`,
`edges`. Optional: `$schema`, `description`, `input`, `output`, `defaults`, `entry`, `meta`.

A node requires `id`, `label`, `description` and `run`, and may add `phase`, `contract`,
`gate` and `control`. An edge requires `id`, `source`, `target` and `type` (`flow`,
`parallel` or `optional`). Edges must form a DAG over existing node ids; a cycle is rejected
with `flow graph must be acyclic`, pathed at the edge that closes it.

**Validation is strict.** Every object in the contract is closed, so an unrecognized key
anywhere — top level, node, or inside `run` — is a hard error reporting the authored path,
dot-separated, with the empty path rendering as `<root>`:

```
<root>: Unrecognized key: "whoops"
nodes.0.run: Invalid discriminator value. Expected 'agent' | 'subprompt' | … | 'sub-flow'
```

There is no lenient mode and no silent drop. Never invent a field.

## The seven node kinds

`run` is a discriminated union on `run.kind`. Exactly one of these seven:

| `run.kind` | What it is for | Required beyond `kind` |
|---|---|---|
| `agent` | The one kind that needs the session agent's conversation — a turn of real work | `instruction` |
| `subprompt` | A model call on any model with no conversation, only bound inputs — classify a document without burning the expensive agent's context | `instruction` |
| `function` | Deterministic code run for its effect — no model call | `command` |
| `check` | Deterministic code producing pass/fail, no human in it; it decides *whether a human is asked* | `command` |
| `gate` | Executed by nobody — parks the flow for a human decision | `prompt`, `schema` |
| `router` | Branch, so two cases take different paths through one flow | `routes` (≥1 `{ when, target }`) |
| `sub-flow` | Executes no work of its own — starts a nested run of another flow and waits for it; what makes flows composable | `flow` |

The taxonomy falls out of two questions: does this node cost a model call, and does it need
the session agent's conversation? Exactly one kind — `agent` — needs the conversation.

Each arm is closed to its own fields. Only `agent` and `subprompt` accept `model` (`small` |
`default` | `deep`) — it is not a universal field, and `{ "kind": "function", "model":
"small" }` is rejected as an unrecognized key. `function` and `check` also accept `runtime`
(`shell` | `node` | `python`), `cwd`, `successExit` and `capture` (`{ stdout?, stderr? }`);
`check` adds `onFail` (`escalate` | `fail`, default `escalate`). A `gate`'s `schema` is
itself a union of `text` (optional `multiline`), `choice` (requires `options`, optional
`multiple`), `form` (requires `fields`) and `file` (optional `accept`, `maxSize`,
`multiple`). A `router`'s `routes` entries are `{ when, target }`, where `target` may be
`null`. A `sub-flow` names its child by slug in `flow` and also accepts `passContext`, which
merges the parent run's input ahead of the node's own bindings. **Every kind accepts
`assets`.**

## Where the instruction lives

**The work goes in `run.instruction` (or `run.command`). `description` is a human label and
is never executed.** This is the single most common authoring error and it is silent: a node
whose real intent sits in `description` is structurally valid and does nothing useful.

A skill is not a node's identity — it is one entry in `run.assets`. So a flow does not need
a skill in order to have a shape, and one skill serves many nodes without being copied.

## Assets are declared references

`run.assets` is a list of asset references — never inlined content. A bare asset id is
valid; normalized legacy flows use `kind:name` refs (`skill:auto-ship`,
`flow:legal-escalation`). The list is resolved **once, at run-group creation**: the group
verifies that its recipe supplies every asset the flow's nodes require — over the transitive
closure through `sub-flow` nodes — and refuses at creation with the missing refs named,
rather than stranding instances mid-batch.

## Contracts — typing where it is consumed

A node always carries free-text `description`. It declares an output schema **only when
something downstream consumes it** — a router branching on it, a check comparing it, or a
binding referencing it. No consumer means no schema; a flow with no routers and no checks
stays pure prose. Typing here is the cost of admission for deterministic behaviour, paid
only where determinism is wanted — a separate axis from the strict validation of the
definition file itself, which always applies.

Node-level typing lives under `contract`: `requires` (guard expressions, each
`{ expr, message? }`), `input` (bindings consumed) and `output` (`fields`, plus `artifacts`
with a `lifetime` of `session` or `persistent`). When a node declares an output schema and
returns something non-conforming, an `agent` node gets **one corrective re-prompt** and then
fails; a `subprompt` has no conversation to re-prompt in and a `function` fails directly, so
both fail on the first mismatch.

`control` carries `optional`, `retries`, `timeoutSec` and `parallelGroup`. The
node-level `gate` object carries `approval`: `none` | `optional` | `mandatory`.

## Gates versus checks

Two distinct concepts, deliberately not two flavours of one.

- A **gate** parks the flow and a **human** decides. Approval strength is the node's
  `gate.approval`: `none`, `optional` or `mandatory`. A mandatory gate always stops for a
  human; the engine enforces it and the agent cannot skip it, even in autonomous mode.
- A **check** is deterministic code the runtime executes, producing pass or fail. There is
  no human in it. It decides *whether a human is asked at all*, so the routine path can run
  dark and only exceptions reach a person.

`onFail: escalate` (the default) parks at a gate with the check's output attached;
`onFail: fail` terminates the run. Both are needed — "purchase order over budget, ask
someone" and "never merge with red CI" are different requirements. A check in a locked flow
cannot be modified by an executing agent, because a locked flow cannot be modified at all.

## Provenance — `verified` versus `asserted`

The engine labels each check by where the values bound into it came from. The label is
**derived, not configured** — there is no field to set, deliberately, because a flag would
have to be set honestly by whoever authors the flow, which is increasingly an agent.

- **verified** — no agent-produced value contributed to any compared value anywhere in its
  ancestry.
- **asserted** — an agent-produced value contributed somewhere in the ancestry.
- **absent** — there were no compared values at all. Neither label is honest about an empty
  set, so the field is omitted rather than defaulted; never read a missing label as
  `asserted`.

The propagation is **transitive**: every executed node persists the origins it resolved, so
a passthrough `function` node that merely carries an agent-produced value forward cannot
launder it into `verified`. A node execution recorded before that shipped has no persisted
origins, and absence reads **fail-closed, as agent-obtained** — so a legacy run reports the
weaker `asserted` rather than a possibly false `verified`, and self-heals as its nodes
re-run.

The label renders at the gate alongside the values and their origins, so an approver can see
which half of a comparison the agent supplied. A second, orthogonal axis records whether the
check's inputs were `live` or `flow-local`. None of this is a reproducibility guarantee: it
records the integrity of *what was observed*, not a promise that observing it again would
agree.

## Authoring a flow as the agent

**Always declare `schemaVersion: 2`.** `platform.create_flow` and `platform.revise_flow`
refuse any definition that does not carry its own, with exactly this message:

```
platform.create_flow: strict v2 flow definitions only — include a `schemaVersion`
```

The reason is worth knowing, because the failure it prevents is silent. An *absent*
`schemaVersion` is what opts a definition into v1 compatibility normalization: a legacy
`skill` node becomes an `agent` node (its `parameters.instructions` becomes
`run.instruction`, the named skill becomes a `skill:<name>` asset) and a legacy `sub-flow`
becomes a real `sub-flow`, but
**every other** node becomes an inert `router` placeholder carrying
`contract.requires: [{ expr: "false" }]` and `control.optional: true`. That placeholder has
no `run.instruction` field at all, so the authored instruction text is not carried forward
as an instruction and the node can never become available. So both halves of the
consequence: authored through the capability, a v1-shaped flow **bounces** with the message
above; arriving by any other route it **normalizes into inert placeholders and the run
executes nothing**.

Both write capabilities also describe the v2 shape in their prompt fragment, so a gated
session can author from context alone. Before writing a new flow, read an existing one with
`platform.get_flow` — it returns the full definition, which is the best worked example to
pattern-match against.

```json
{
  "schemaVersion": 2,
  "id": "obligation-review",
  "version": "1.0.0",
  "name": "Obligation Review",
  "nodes": [
    {
      "id": "extract", "label": "Extract obligations", "description": "Pull the obligations out of the contract.",
      "run": { "kind": "agent", "instruction": "List every payment obligation with its due date.", "model": "default", "assets": ["asset-policy-1"] }
    },
    {
      "id": "summarize", "label": "Summarize", "description": "One-paragraph summary.",
      "run": { "kind": "subprompt", "instruction": "Summarize the obligations in one paragraph.", "model": "small" }
    },
    {
      "id": "export", "label": "Export", "description": "Write the register to disk.",
      "run": { "kind": "function", "command": "python export_register.py" }
    },
    {
      "id": "verify", "label": "Verify totals", "description": "Check the totals add up.",
      "run": { "kind": "check", "command": "python verify_totals.py", "onFail": "escalate" }
    },
    {
      "id": "signoff", "label": "Sign-off", "description": "Ask a reviewer to approve.",
      "run": { "kind": "gate", "prompt": "Approve this obligation register?", "schema": { "kind": "text" } },
      "gate": { "approval": "mandatory" }
    },
    {
      "id": "branch", "label": "Branch", "description": "Escalate when the register is rejected.",
      "run": { "kind": "router", "routes": [{ "when": "true", "target": "escalate" }] }
    },
    {
      "id": "escalate", "label": "Escalate", "description": "Hand off to the escalation flow.",
      "run": { "kind": "sub-flow", "flow": "legal-escalation" }
    }
  ],
  "edges": [
    { "id": "e1", "source": "extract", "target": "summarize", "type": "flow" },
    { "id": "e2", "source": "summarize", "target": "export", "type": "flow" },
    { "id": "e3", "source": "export", "target": "verify", "type": "flow" },
    { "id": "e4", "source": "verify", "target": "signoff", "type": "flow" },
    { "id": "e5", "source": "signoff", "target": "branch", "type": "flow" },
    { "id": "e6", "source": "branch", "target": "escalate", "type": "flow" }
  ]
}
```

## Not in the contract

The validator refuses these, so do not author them:

- `defaults.dispatch` and `run.context` (`shared` / `isolated`) were **rejected** — `run.kind`
  already carries that information, and isolation is what every non-`agent` kind already is.
- Renaming `nodes` to `steps` was **rejected**: the node vocabulary is published protocol.
- Cycles are **deferred**, not rejected — the graph stays acyclic for now, so express
  sequencing with `edges` only.

## Running a flow

- A running flow shows a panel in the workspace and progress cards (breadcrumbs) inline in
  chat. It survives hibernation: on wake it resumes in the same state.
- **Gates** pause a run for a human: approval gates (approve/reject) and input gates
  (provide data). Runs can be started in autonomous mode — no pauses — except that a node
  marked **mandatory** always stops for a human; the engine enforces this and the agent
  cannot skip it. Live per-node state on the graph is not yet available.

## Flow files on disk

On the platform a flow is a stored asset. On disk — in a workspace, where the session
container ships the `skaile` CLI — a flow is a file, and there its identity is the `id` it
declares, never its filename; `skaile run <id>` and `skaile flow list` resolve that same
declared `id`. A flow file sits in a `flows/` directory in one of exactly two layouts:

- `flows/<name>.flow.yaml` — a flow that is one file.
- `flows/<name>/<name>.flow.yaml` — a flow that carries sibling assets (README, fixtures,
  prompts). Discovery descends exactly one level and the file must be named after its
  directory. This is not recursion.

Extensions, in probe order: `.flow.yaml`, `.flow.yml`, `.flow.json`, legacy `.json`. The CLI
walks the project install target `<project>/.skaile/` (`skaile install` lands a flow in
`<project>/.skaile/flows/`), then the bundled `factory-assets/` tree, `~/.skaile/libraries/`,
then the user-global `~/.skaile/` (`~/.skaile/flows/`) — de-duplicating by declared `id` with
the first root winning, and reading both `<root>/flows/` and `<root>/<domain>/flows/` inside
each. That list and its order are `aiResourceRoots`; read it rather than trusting this
sentence to age well.

A misnamed flow is not dropped in silence: where discovery looks, a directory holding a
`flow.yaml` or `<anything>.flow.yaml` file but none named after itself is reported on stderr, as
is a file that fails to parse. Silence is not proof of reachability, though — `_`-prefixed
entries are the deliberate opt-out, a stray bare `.json` inside a per-flow directory stays quiet
so sibling `package.json` files do not earn warnings, and dot-directories and `node_modules` are
never walked. So trust the stderr of `skaile flow list` over any page, including this one.
Fuller treatment: `ai-assets/docs/flows.md`.

## Run groups (batch / unattended processing)

- A run group = one flow + one **recipe** + a list of inputs. Each input runs in its own
  temporary session; a scheduler limits how many run at once. Groups can be paused,
  cancelled, retried per item, and new inputs can be appended while running.
- A **recipe** is a saved session configuration (data sources, skills, model, environment)
  created via **Save as recipe** from a configured session. Recipe environment values
  reference stored secrets — never literal secret strings. Creation fails up front if the
  recipe does not supply every asset the flow's nodes declare.
- A status board shows per-item progress and cost with click-through into each session.
  Approvals and input requests raised by unattended runs land in an org-level inbox
  (badge in the navigation), filtered to the named approvers.
- Triggers: manual, **webhook** (external systems post signed requests that append
  inputs), or the agent itself (approval-gated capabilities; appending to a specific
  group can be pre-approved in config). Time-based scheduling of groups is not yet
  available.

## Webhooks that wake a session

Separate from run groups, a session can have a **webhook inbox**: a secret token URL that,
when called by an external system (e.g. GitHub), wakes the session and delivers the payload
to the agent as untrusted data. The token is shown once at creation and can be rotated.
The agent can request creating one (approval-gated). There is no UI surface for this yet —
it is managed via the API/agent.

Source of truth: the published contract
(`@skaile/workspaces/dist/factory-assets/connectors/flow/contract/flow.v2.schema.json`),
`platform/docs/flow-authoring-v2.md`, `platform/features/09-flow-execution/`,
`platform/features/31-run-groups/`. For on-disk discovery: `loadFlowEntriesFromDir` in
`@skaile/workspaces` → `factory-assets/connectors/flow/engine/loader.ts`, and `aiResourceRoots`
in `cli/src/paths.ts`.
