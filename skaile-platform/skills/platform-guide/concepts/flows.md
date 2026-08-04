# Flows, Runs & Run Groups

Repeatable multi-step work. A **flow** is the definition (a DAG of steps); a run is one
execution of it inside a session; a **run group** fans one flow out over many inputs, each
in its own temporary session — unattended batch processing with human checkpoints.

## Flow definitions

- A flow is an asset (like a skill) and can be owned at any scope: Personal, Session,
  Project, Team, or Organization. Owners can publish upward with **Share to org**.
- The org-level **Flows** page lists flow definitions and opens each into a graph view
  (steps, dependencies, gate badges). Authorized users (scope admins) can edit visually:
  add/connect/remove steps, change step parameters, undo/redo. Flows import and export as
  YAML or JSON.
- The Flows page is currently feature-flagged (`ff_flows`) — if a user cannot see it, the
  flag is off for their deployment; don't present it as missing functionality.

## Running a flow

- A running flow shows a panel in the workspace and progress cards (breadcrumbs) inline in
  chat. It survives hibernation: on wake it resumes in the same state.
- **Gates** pause a run for a human: approval gates (approve/reject) and input gates
  (provide data). Runs can be started in autonomous mode — no pauses — except that a step
  marked **mandatory** always stops for a human; the engine enforces this and the agent
  cannot skip it. Live per-step state on the graph is not yet available.

## Run groups (batch / unattended processing)

- A run group = one flow + one **recipe** + a list of inputs. Each input runs in its own
  temporary session; a scheduler limits how many run at once. Groups can be paused,
  cancelled, retried per item, and new inputs can be appended while running.
- A **recipe** is a saved session configuration (data sources, skills, model, environment)
  created via **Save as recipe** from a configured session. Recipe environment values
  reference stored secrets — never literal secret strings.
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

Source of truth: `platform/features/09-flow-execution/`, `platform/features/31-run-groups/`.
