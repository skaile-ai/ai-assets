# Flows, Runs & Run Groups

Repeatable multi-node work. A **flow** is the definition (an acyclic graph of **nodes**); a
run is one execution of it inside a session; a **run group** fans one flow out over many
inputs, each in its own temporary session — unattended batch processing with human
checkpoints.

## Flow definitions

- A flow is an asset (like a skill) and can be owned at any scope: Personal (shown as
  **Only me** in the UI), Session, Project, Team, or Organization. Owners can publish
  upward with **Share to org**.
- The org-level **Flows** page lists flow definitions and opens each into a graph view
  (nodes, dependencies, gate badges). Authorized users (scope admins) can edit visually:
  add/connect/remove nodes, change node settings, undo/redo. Flows import and export as
  YAML or JSON.
- A flow can be **locked**. While it is locked the definition is immutable — no edit, no
  revision by an agent, no exception; unlocking is a deliberate human act. Each stored
  definition also carries a content hash, so "which definition did this run execute" is
  always answerable.

## The shape of a definition

The authority on shape is the published JSON Schema, not this file. Get it at runtime with
`platform.get_flow_schema` — no arguments, no approval, no project context and no existing
flow needed. It returns `schemaId`, `jsonSchema` and `example`, a complete valid
definition to pattern-match against. Read the schema when you need the exact field list. The
same document ships inside `@skaile/workspaces` as
`dist/factory-assets/connectors/flow/contract/flow.v2.schema.json`, but the capability is the
copy the platform actually validates against. Everything below is what a schema cannot say:
which construct to reach for, and where authoring goes quietly wrong.

Required at the top level: `schemaVersion` (always `2`), `id`, `version`, `name`, `nodes`,
`edges`. Optional: `$schema`, `description`, `input`, `output`, `defaults`, `entry`, `meta`.

A node requires `id`, `label`, `description` and `run`, and may add `phase`, `contract`,
`gate` and `control`. An edge requires `id`, `source`, `target` and `type` (`flow`,
`parallel` or `optional`). Edges must form a DAG over existing node ids; a cycle is rejected
with `flow graph must be acyclic`, pathed at the edge that closes it.

The consequence is that **a flow cannot loop**. `control.retries` on a node is the only repeat
the schema has, and it re-runs that one node — it cannot carry a cycle back through a gate.
Any other iteration ("authority asks a follow-up, we respond, authority reviews again") has
to be flattened into a forward path — one node per pass, a router choosing how far the run
goes — or delegated to a `sub-flow` invoked once per pass. Never a back edge; design for this
from the start rather than discovering it at the edge that closes the cycle.

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
with a `lifetime` of `session` or `persistent`).

**`contract.output.fields` is a JSON Schema object** — not a map of field name to type, and
not a list of field descriptors. The field names go under `properties`, on the node that
**produces** them:

```json
"contract": { "output": { "fields": { "type": "object", "properties": { "needsApproval": { "type": "boolean" } } } } }
```

Only the list form fails where it is written (`expected record, received array`). A bare
`{ "needsApproval": { "type": "boolean" } }` — or `{ "needsApproval": "boolean" }` —
**parses**, because it is a valid JSON Schema that declares no `properties`; the failure then
surfaces one layer later as `node <producer> does not declare output field needsApproval`,
pathed at the **consuming router**. The message names one node and the defect is on another,
which is why editing the router never fixes it. When a node declares an output schema and
returns something non-conforming, an `agent` node gets **one corrective re-prompt** and then
fails; a `subprompt` has no conversation to re-prompt in and a `function` fails directly, so
both fail on the first mismatch.

`control` carries `optional`, `retries`, `timeoutSec` and `parallelGroup`; `retries` re-runs
that one node and is the only repeat in the schema (see the DAG consequence above). The
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

### GitHub access from a `function` or `check` node

The node runs in the session workspace with the same Git credential helper the agent has. The
helper exists only for a git mount with `exposeAccessToken` on, shown in the mount editor as
"Allow agent to use git CLI directly"; the runtime's `gh` relies on the same helper, so with the
toggle off neither route below has a supported token and the mount's owner has to turn it on.

The helper is scoped to the mount's repository URL, not to the host. A `git credential fill`
that names only `protocol=https` and `host=github.com` carries no path, so it matches no helper
and never returns a password: with `GIT_TERMINAL_PROMPT=0` it fails at once (`could not read
Username for 'https://github.com'`); without it, Git falls back to a prompt, which in a node
fails or waits. Retrying only delays the failure. Either call `gh` from inside the checkout
(for an `https` origin the runtime's `gh` wrapper queries with the origin URL itself and never
prints the token), or query with the checkout's origin URL verbatim. The mount sets `origin` to
the same URL it keys the helper by, and Git compares the path literally, so any other spelling
(adding or dropping `.git`) misses:

```bash
token=$(printf 'url=%s\n\n' "$(git -C <checkout> remote get-url origin)" |
  GIT_TERMINAL_PROMPT=0 git -C <checkout> credential fill | sed -n 's/^password=//p')
[ -n "$token" ] || { echo 'no git credential for this repository' >&2; exit 1; }
```

Keep the guard: the pipeline's exit status is `sed`'s, so without it a failed query leaves
`token` empty and the node fails later with an unrelated authentication error.

Never print the token: `credential fill` writes `password=<token>` to stdout, and
`git remote get-url origin` can print a URL with the token baked in (platform #4977). A node's
stdout can end up in its persisted output and in the evidence a gate renders, so keep both
inside a variable or command substitution.

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

**A project or organization flow write needs a session owner who administers that scope.**
`platform.create_flow` and `platform.revise_flow` are `effect` capabilities: each write is
approved on a card, or covered by a standing grant the owner chose on an earlier card. Nothing
has to be switched on in settings first. The one rule is whose authority the write uses: the
**session owner's**, never the approver's. The owner must be an admin of the scope the write
targets — an Owner of the project for a project flow, an Owner of the organization for an
organization flow. The platform checks this when it prepares the card, again when the card is
decided, and again immediately before the write.

For `platform.revise_flow` the scope is the stored flow's own, not the session's project.

So an owner who is not an admin of that scope is refused **before any card appears**, and
someone else approving cannot change that. The refusal is prose that names the role, plus a
structured `remedy` (`capability`, `requiredScope`, `requiredRole`) that no screen renders;
relay the refusal prose verbatim, not the `remedy` object, and stop — retrying changes nothing.
If organization-wide was not essential, offer to work at project scope, if the owner
administers the project.

A standing grant can cover these writes: `platform.revise_flow` at that exact flow or its
project, `platform.create_flow` at that exact target only. You cannot create, extend or widen
a grant yourself — see [Autonomy grants](agent.md).

`platform.assign_project_asset` and `platform.unassign_project_asset` follow the same rule: the
session owner must administer the project, checked at the same three points, and a grant
reaches that exact target only. Unassigning a skill unloads it from the project's running
sessions at once; other kinds leave on each session's next wake.

From the owner's personal assistant, `platform.create_flow` with scope `project`,
`platform.assign_project_asset` and `platform.unassign_project_asset` also take a `projectId`,
so you can set up a project you just created without handing each step to it. The same rule is
judged on that project, and its organization must allow you **Full** reach
(`concepts/agent.md` § *Assistant reach*). A project the owner does not administer is refused as
unavailable. Any other session that passes `projectId` is refused. Scope `organization` refuses a
`projectId`: it goes with scope `project` only.

**Personal flows are the exception.** `platform.create_flow` with scope `personal` saves the
flow to the session owner's own library, visible only to them (the UI labels this scope
**Only me**; "Private" means the Private workspace, so do not call it that), and needs no
admin role. The write is still approved — per call, or by a standing grant over this
session that only the session owner can issue — and still validated as strict v2 before
the card shows.
`platform.list_flows` lists project and organization flows only; personal flows are listed by
`platform.list_personal_flows`, which is itself approval-gated because listing puts flow names
into a conversation every member can read. `platform.get_flow` and
`platform.revise_flow` do not reach personal flows (they read as not found).

**Always declare `schemaVersion: 2`.** `platform.create_flow` and `platform.revise_flow`
refuse any definition that does not carry its own, with exactly this message:

```
platform.create_flow: strict v2 flow definitions only — include a `schemaVersion`
```

The reason is worth knowing, because the failure it prevents is silent. An *absent*
`schemaVersion` is what opts a definition into v1 compatibility normalization: a legacy
`skill` node becomes an `agent` node (its `parameters.instructions` becomes
`run.instruction`, the named skill becomes a `skill:<name>` asset), a legacy `sub-flow`
becomes a real `sub-flow`, and a legacy `type: gate` becomes a real gate node
(`run.kind: "gate"`, `run.schema: { kind: "text" }`). No `gate.approval` is set on it, and none
is needed: `gate.approval` (see *Gates versus checks*) is the approval policy for another
node's output, such as an agent node's, while a node whose `run.kind` is `gate` is itself the
human decision point and always parks the run, whatever `defaults.approval` says. (The
`data.approval.mandatory` → `gate.approval` mapping applies to v1 skill nodes, not to v1 gates.) Its
`run.prompt` is the v1 `data.message` (or the description) followed by
`Approval criterion (not evaluated automatically): <data.check>`; the check is shown, never
evaluated. But **every other** node becomes an inert `router` placeholder carrying
`contract.requires: [{ expr: "false" }]` and `control.optional: true`. That placeholder has
no `run.instruction` field at all, so the authored instruction text is not carried forward
as an instruction and the node can never become available.

One v1 shape does not normalize at all: a gate with `data.optional: true`, or a
`data.on_fail` other than `pause-for-human`. That fails the whole definition's parse with an
issue at `nodes.<i>.data.optional` / `nodes.<i>.data.on_fail`, so the flow does not load.

So, in practice: authored through the capability, a v1-shaped flow **bounces** with the
message above. Arriving by any other route, a malformed gate makes the **whole definition
fail to load**; otherwise its **gate nodes park the run until a human approves** and nodes of
the remaining kinds **normalize into inert placeholders that never run**.

Both write capabilities also describe the v2 shape in their prompt fragment, so a gated
session can author from context alone. Two ungated queries make the rest discoverable at
runtime. Both resolve no project context, so they work in a brand-new project that has no
flows yet:

- **`platform.get_flow_schema`** (no arguments) returns `schemaId`, `jsonSchema` and
  `example` — a complete valid definition that exercises exactly the constructs authoring
  fails on: the JSON Schema `contract.output.fields` shape, a router reading it, a bare
  `default` route, choice options, hyphenated node ids. Start here.
- **`platform.validate_flow`** with `{ definition }` checks a definition against the same
  parser the writes use — no grant, no approval, nothing stored. **Call it before every
  `platform.create_flow` and before the full-`definition` form of `platform.revise_flow`.**
  The patch form has no `definition` to pass; to check a patch, apply it to the
  `platform.get_flow` definition yourself and validate the result. The flow goes *under* a
  `definition` key, not spread at the top level. A definition the contract rejects is still a
  successful call — `{ ok: true, valid: false, issues: [{ path, message }] }`, never an
  `error` — so loop on `issues`. Validation runs in layers (structural parse first, then
  cross-node reference and expression checks), so clearing every issue can reveal the next
  layer: re-call after each fix until it reports `valid: true`.

Reading an existing flow with `platform.get_flow` (pick one from `platform.list_flows`) is the
secondary worked example: it needs a flow to already exist, which is exactly what a first
authoring attempt in a new project lacks.

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

## Classifier nodes

A `subprompt` can be answered by the organization's **classifier provider** (TypeSafe's Jev
today) instead of a generative model. A classifier answers one closed question with a
probability a router can put a threshold on. There is no eighth node kind: a `subprompt` opts
in by declaring the reserved output fields `confidence` and `calibrated` next to exactly one
**decision** property. The decision's shape picks the question kind:

| Kind | Decision property | Answer |
|---|---|---|
| `binary` | `{ "type": "boolean" }` | `true` when p(yes) ≥ 0.5 |
| `choice` | `oneOf` of `{ "const": "<label>", "description": "<rubric>" }` (2–255) | the most probable label |
| `score` | `oneOf` of `{ "const": <integer>, "description": "<level>" }` (2–10) | the authored integer `const` of the most probable level |

Prefer `oneOf` with a rubric per option — the rubric is what the classifier's accuracy rests
on. `run.instruction` becomes the question; the resolved `contract.input` bindings are the
text being classified. Keep arithmetic and date comparisons out of the question: put them in
a router on `flow.input` before the classifier runs.

The whole answer is also stored on the node output as `classification`: its `value` (the
decision again), `providerKind`, `modelVersion`, the per-option `distribution` and, for a
score, `expected`. For a score, `classification.value`, `expected` and the `distribution` keys
are all on the **authored `const` scale** as of
`@skaile/workspaces` 3.27.1 (earlier runtimes keyed a score's `distribution` by level index).
`distribution` is raw provider output — never present it as a confidence.

**Thresholds need the guard.** `confidence` is `null` whenever the answer is uncalibrated, and
an uncalibrated answer can never pass a confidence threshold. So a `<`/`<=`/`>`/`>=` on a
classifier's `confidence` must be preceded, earlier in the same `&&` chain, by the literal
`nodes.<n>.output.fields.calibrated == true` for the same node — and a router that reads
either reserved field must end with an unconditional **`default`** route —
`{ "when": "default", "target": "<node id>" }` (`"when": "true"` is equivalent; `target` may be
`null` to end the run) — which is where an uncertain answer reaches a human:

```
nodes.assess.output.fields.calibrated == true && nodes.assess.output.fields.verdict == "payout" && nodes.assess.output.fields.confidence >= 0.95
```

**A route on the decision alone is not a threshold.** `verdict == "payout"` is valid and acts
on the most probable answer, calibrated or not; a `binary` `== true` fires at a coin flip.
Where you mean "confidently yes", add the guarded `confidence` conjunct and let the rest fall
through to the `default`. Never let a classifier answer be the last control before an
irreversible action — keep a `check` or a `gate` next to it.

**When no classifier answers, the flow still runs.** With no usable provider
(`no_classifier_provider` — none configured, not acknowledged, or not callable), a provider
failure, or a refused request, the node falls back to the ordinary generative subprompt: it
completes with `confidence = null` and `calibrated = false`, so every guarded route is false
and the router takes its `default`. Only an org admin can switch the classifier on — see
**Settings > Classifiers** in `ui/navigation.md`.

`platform.get_flow_schema` returns `classifierExample`, a complete valid classifier flow
(ticket triage, with guarded thresholds and a `default` route) — pattern-match against it
rather than writing one from memory. Classifier output comes from a `subprompt`, so a check
comparing it is `asserted`, never `verified`.

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
  cannot skip it.
- The session's **Flow** tab shows per-node progress of the running flow. The org **Flows**
  page graph shows the definition only.

### Running a library flow inside a session

A flow from the library (organization, project, or personal) can run inside an existing
session, in that session's conversation — one run per session at a time; a start while one
is in progress is refused. While it runs, chat messages to the session are handled as part
of the run. It can be started:

- by a user, from the session's **Flow** tab (**Run a flow…**) or Cmd+K **Run a flow in
  this session**, choosing the flow and an optional input;
- by the session's own agent, with no approval (`platform.run_flow`);
- by an agent in another session, with the session owner's approval or a standing grant —
  and only if that owner may send to the target session.

The input reaches the flow unchanged as `defaults.run_input`; it cannot override values the
author set — only declared `parameters` can. Sessions created for a run group cannot host a
second run.

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
- Every group has a mode, fixed at creation: **Batch** (a fixed set of inputs; the group
  finishes when every run has finished) or **Standing** (trigger-fed and long-running; it
  keeps taking new inputs until someone clicks **Close**). A Standing group can mint its
  webhook at creation.
- A **recipe** is a saved session configuration (data sources, skills, model, environment)
  created via **Save as recipe** from a configured session. Recipe environment values
  reference stored secrets — never literal secret strings. Creation fails up front if the
  recipe does not supply every asset the flow's nodes declare. Before proposing a group,
  the agent can check this without creating anything
  (`platform.preflight_flow_requirements`, no approval); it does not check binaries,
  credential scopes, or node kinds.
- A status board shows per-item progress and cost, with click-through into the run group's
  detail page. Approvals and input requests raised by unattended runs surface inside the
  run itself — the flow gate panel in the session, and the run group detail page. There is
  no org-level inbox that collects them outside a session. (The **Approvals** tab on the
  org **Store** page is a different surface: it holds asset-share requests.)
- Triggers: manual, **webhook** (external systems post signed requests that append
  inputs), or the agent itself (`platform.append_run_inputs`, approval-gated; a standing
  grant can cover appends to that group or its project). Whether approved on a card or
  matched by a grant, the append returns an operation receipt; read it with
  `platform.get_operation`. Only rare fallback cases, where the request cannot be represented
  for the background worker, return the direct append result.
  Time-based scheduling of groups is not yet available.
- The agent can also create, pause and cancel a group (`platform.create_run_group`,
  `platform.pause_run_group`, `platform.cancel_run_group`), each approval-gated and
  grantable. The session owner must be among those the group's **startableBy** allows (for
  a new group, the startableBy being set), or the call is refused before any card appears.
- A group the agent creates is a **Draft**: it runs nothing until it is activated (the
  board's create wizard activates straight away). The order is create, then optionally
  switch autonomous mode on, then activate:
  - `platform.list_run_groups` (no approval) lists the project's groups, newest first —
    use it to find a group whose create was approved after your call stopped waiting.
  - `platform.set_run_group_autonomous_mode({ groupId, autonomousMode })` switches
    autonomous mode on or off. It is approval-gated, and classed `privileged`, so it has no
    one-click grant option: a grant reaches it only if the owner deliberately turned that
    opt-in on. It affects only runs admitted afterwards (so set it before activating), and
    gates the flow marks mandatory still stop every run. `create_run_group` itself still
    refuses `autonomousMode: true`.
  - `platform.activate_run_group({ groupId })` starts a Draft group, with the same check as
    the **Activate** button on the group's detail page. Approval-gated and grantable, for
    that group or its project. It starts only a Draft: a paused group is resumed by a
    person from that page.
- A person does the same on the group's detail page (click through from the board):
  **Activate**, and the **Autonomous** switch.

## Webhooks that wake a session

Separate from run groups, a session can have a **webhook inbox**: a secret token URL that,
when called by an external system (e.g. GitHub), wakes the session and delivers the payload
to the agent as untrusted data. The token is shown once at creation and can be rotated.
The agent can request creating one (approval-gated). There is no UI surface for this yet —
it is managed via the API/agent.

Source of truth: the published contract — `platform.get_flow_schema` at runtime, shipped
as `@skaile/workspaces/dist/factory-assets/connectors/flow/contract/flow.v2.schema.json` —
`platform/docs/flow-authoring-v2.md` (incl. git credentials in nodes, platform #5603),
`platform/features/09-flow-execution/`,
`platform/features/31-run-groups/`, `platform/features/09-flow-execution/in-session-flow-runs.md`
(platform #5233), `platform/docs/flow-authoring-v2.md` "Personal flows" (platform #5252),
the "Only me" scope label (platform #5989), agent activation and autonomous mode for run groups (platform #6061),
the run-group create wizard (Batch / Standing), `RunGroupRecipePreflightService`. For on-disk discovery: `loadFlowEntriesFromDir` in
`@skaile/workspaces` → `factory-assets/connectors/flow/engine/loader.ts`, and `aiResourceRoots`
in `cli/src/paths.ts`.
