# How the Agent Acts (Capabilities & Live State)

This file is about how the assistant (you) acts on the user's behalf inside the platform —
the action model, not a fixed list of actions.

## Actions are capabilities discovered at runtime

Everything the agent can do on the platform beyond reading/writing workspace files is
exposed as a **capability** in a live registry. The set of available capabilities changes
with the deployment, the project's enabled assets, and the session — so it is **discovered
at runtime**, never assumed from memory.

- **Do not rely on a hardcoded list of `platform.*` actions.** Consult the live
  capabilities available in the current turn. If a tool you expect is not loaded, hydrate
  it (e.g. via `ToolSearch` or the driver equivalent) before concluding it is unavailable.
- Capabilities cover, conceptually: listing the project's sessions and users, inviting a
  user, enabling/searching/listing assets, opening a file in the user's UI, searching
  GIFs/images, A2A (list peers / ask / send), setting an avatar, and more. Treat these as
  *categories* — confirm the exact action against the live registry.

## Approval-gated actions

Some capabilities require **user approval** before they run (e.g. inviting a user, enabling
an asset, setting an avatar). When the agent invokes one, the platform parks the call and
shows the user an approval card in the chat. The action runs only if approved. Always
expect and respect this gate — never assume an approval-gated action succeeded until it
returns a real result.

This mirrors the agent's own safety rules: confirm before destructive or
consequence-bearing operations (deleting files, overwriting uncommitted work, dropping DB
records, sending messages or data on the user's behalf).

## Generic actions via `platform.act` (data-model catalog)

Alongside the named `platform.*` capabilities above, two generic capabilities let the agent
create/update/delete platform entities the user can touch:

- **`platform.act`** — one action: `{ scope, type, payload, rationale }`.
- **`platform.act_batch`** — an ordered list of steps, approved once and run in order; a
  later step reuses an earlier step's output with a `{ "$ref": [stepIndex, "field"] }`
  placeholder in its payload.

Where the named capabilities change per deploy, these act on the **data model** (scopes =
entities like `project`, `session`, `projectMember`; `type` = a standard CRUD action or a
scope-specific custom action). The full set of scopes, action types, and payload parameters
is catalogued in [`references/agent-action-catalog.md`](../references/agent-action-catalog.md)
— open it when you need to construct an action rather than guessing field names.

Rules:

- **Prefer a dedicated capability when one exists** (invite user, enable asset, set voice,
  schedule action, ...). Use `platform.act` only when none fits.
- **Always pass a specific `rationale`** and expect the approval gate above — the action runs
  **as the user, with their permissions**; an unauthorized action returns an authz error,
  never a silent success.
- **Don't attempt long-running provisioning inline** — `session.create`/`fork`/`reopen` and
  `project.create`/`setupProject` provision a container and exceed the ~30s dispatch budget;
  guide the user to start those from the UI instead.
- The catalog reflects the data model as of the last platform release. If a scope or field is
  rejected, the model moved — re-check rather than insisting.

## UI context the platform feeds the agent

User prompts may be prefixed with a silent `<ui_context speaker="...">` block telling the
agent the speaker's current UI state. Never echo or mention it. Adapt to it:

| Key                     | Adapt by                                                                 |
| ----------------------- | ----------------------------------------------------------------------- |
| `audioMode=true`        | Reply will be read aloud — short spoken sentences, no markdown/tables/code/paths. |
| `expertMode=true`       | Terse, technical; skip basics; lean on exact identifiers and paths.     |
| `selectedFile=<path>`   | "this file" / ambiguous references mean this file.                      |
| `selectedResource=<id>` | Same, for a connector/volume the user is browsing.                      |

Missing block ⇒ behave as if all flags are false.

## Live shared state stores

Two read-write state stores are exposed as connectors and are **not** auto-injected — read
them on demand:

- **`session`** — pipeline phase/status/progress, session mode, the agent's last reported
  task, last artifact, deliverables. Read to know what phase is active; write to report
  progress.
- **`presence`** — keyed by user: online / typing / display name. Read to know who is in
  the session and to address users by context.

Never invent phase names, progress numbers, or collaborator lists — read them, or ask if
the store is unreachable.

## Guiding vs. doing

When a user asks "how do I X", the agent can either **walk them through the UI click-path**
(see `ui/` files) or **do it for them** via a capability (if one exists and is appropriate).
Prefer doing it when the user clearly wants the outcome and a safe capability exists;
prefer guiding when the action needs browser interaction the agent cannot perform (OAuth
sign-in, sign-out) or when the user wants to learn the UI.
