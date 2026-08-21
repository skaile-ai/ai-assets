# Control-Plane Capabilities — durable operations, consent, and the handoff

The control plane is the family of capabilities that **change what exists** on the platform —
organizations, projects, sessions, memberships, connector wiring — plus the read-only
discovery that resolves the ids they take, and the one query that reports what happened.

This file is a **map, not a contract**. The live registry is authoritative: the set changes
every deploy, and most of this family is advertised only in the owner's own personal-assistant
session. Consult the capabilities available in the current turn and use the exact schema they
carry. Read this to know what the family *is* and how consent and completion work in it — not
to decide whether a capability exists. `concepts/agent.md` carries the model; this is the
detail you load when you are about to construct one of these calls.

## The family

### Read-only — resolve ids here first

Owner-scoped, query-only, and available only where the platform resolves the calling session
as the owner's own assistant. They never create approvals, grants, operations, invitations,
or connector configuration.

| Capability | Gives you |
| --- | --- |
| `platform.list_my_organizations` | `organizationId`, the owner's live role, a permissions summary |
| `platform.list_my_projects` | `projectId`, `organizationId`, status, visibility, source type, live role. The assistant's own workspace is never listed. |
| `platform.list_my_sessions` | `sessionId` with full ancestry (organization → project → session), live role |
| `platform.get_session_context` | one session's ancestry plus the owner's effective role at each level. Not paged. |
| `platform.list_project_members` | every membership *and invitation* row for a project, with `status`: `Active`, `Invited`, `Expired`, `Revoked` |
| `platform.list_session_resources` | the project source plus every library asset in effect on a session, with provenance |
| `platform.list_connector_options` | an organization's connectors, redacted to identity and readiness — `usable`, and when false, `requiredHandoff` |

`platform.search_my_sessions` sits alongside these for finding a session by content rather
than by listing.

Shared shape: `{ cursor?, limit? }` in (limit 1–50), `{ items, nextCursor }` out. `nextCursor`
is non-null only when more rows exist. A cursor replayed after you change a filter or `limit`
is rejected — restart paging without one. Results are already redacted to safe identity, role
and status fields.

Reading connector readiness is the one that most often ends the task early:

- `usable: true` — the owner can already use it. **Do not start a setup.**
- `usable: false`, `requiredHandoff: connect_user_credential` — it takes a per-user credential
  the owner has not connected. That is exactly what a connector setup starts.
- `usable: false`, `requiredHandoff: contact_organization_admin` — it does not take a per-user
  credential, so connecting it is an organization-level change. Say so; do not assume you can
  complete it.

### Effects — each returns a receipt, not a result

Every capability below hands the work to a durable background worker once the owner consents,
and returns `{ operationId, status: "Queued", target, instruction }`. None of them returns the
thing it made. None of them may appear in a batch.

| Capability | Effect | Risk | Autonomy class | How far a grant may reach |
| --- | --- | --- | --- | --- |
| `platform.create_organization` | a new organization | high | `privileged` | everything the owner can reach only |
| `platform.create_project` | a new project in an organization | medium | `routine` | that target, its organization, or everything reachable |
| `platform.create_session` | a new session in a project | medium | `routine` | that target, its project, its organization, or everything reachable |
| `platform.invite_to_organization` | an invitation email | high | `external` | that target, its organization, or everything reachable |
| `platform.invite_to_project` | an invitation email | high | `external` | that target, its project, its organization, or everything reachable |
| `platform.invite_to_session` | an invitation email; the invitee can read that session's whole history | high | `external` | that target, its project, its organization, or everything reachable |
| `platform.begin_connector_setup` | starts connecting a provider; parks on the owner | medium | `routine` | that target, its organization, or everything reachable |
| `platform.configure_project_source` | re-points a project at a different, already-usable connector | medium | `routine` | that target, its project, its organization, or everything reachable |

`platform.delegate_to_session` delivers one message into another session as the owner. It is
also approval-gated and classed `external`, with a grant reaching **that one target and nothing
else** — but it is not durable: it returns its own result rather than an operation. The delivered
message always shows it was sent by the owner via their Personal Assistant; it is never
attributed to the assistant.

### Boundaries that are real, not conservatism

These are refusals by design — proposing around them wastes the owner's approval:

- **Sources.** A project created here can be empty or Skaile-hosted. One backed by a connector
  (Git, SharePoint, Google Drive, Box, NextCloud) is created from the web app. Re-pointing only
  moves a project that *already has* a Git source onto a different, already-usable connector —
  it cannot add a source, create a connector, or create a project.
- **Roles on invite.** `Viewer` (default) or `User`. `Owner` is not assignable through any of
  these. No personal note, personal message, or display name can be attached — the human adds
  those from the web app.
- **Personal workspaces.** A personal-assistant project or session cannot be invited into, and
  a personal organization cannot be invited into. Pick a shared one.
- **Credentials.** No capability in this family accepts a token, password, personal access
  token, OAuth code, client secret, repository URL, or branch. Every such field is rejected.
  Never ask for one, and never accept one if offered.
- **Batches.** None of these is batch-eligible, and none is grantable through a batch.

## The operation lifecycle

`platform.get_operation({ operationId })` reads one operation. Its lifecycle is `Queued` →
`Running` → one of `Succeeded` / `Failed` / `Cancelled`, with `AwaitingUser` as a park in the
middle.

| Status | Terminal | Meaning |
| --- | --- | --- |
| `Queued` | no | Waiting to run. With `retryDueAt` set it is a scheduled retry after a transient failure; `lastAttemptError` says why. |
| `Running` | no | An attempt is in flight. |
| `AwaitingUser` | **no** | Parked until a human acts. See below. |
| `Succeeded` | yes | Done; `result.payload` holds the outcome. |
| `Failed` | yes | Done unsuccessfully; `error.code` and `error.message` say why. |
| `Cancelled` | yes | Stopped before finishing. |

Reading a status reply:

- `terminal: true` means it can never change again — stop polling and report.
- `instruction` restates the next step for the status you just read. It changes with the
  status, so re-read it each time rather than caching the first one.
- `attempts` counts claimed executions, including recovery of an attempt whose worker died.
  A rising count is the system being reliable, not the effect happening twice.
- A `Failed` reply may carry a `result.payload` reporting **partial progress**. Those steps
  really happened and were not rolled back — report them rather than a bare failure.
- An unknown, malformed, or someone-else's operation id answers the same way as a missing one,
  by design. It is terminal; do not retry and do not probe other ids.
- Where a session is not the owner's personal assistant, the refusal is that the capability is
  unavailable here. Terminal — say so.

Two identifiers, two phases. Before consent there is no operation at all: a call parked on the
owner's approval answers with `{ status: "awaiting_approval", invocationId }`, and
`platform.get_operation({ invocationId })` is what you poll to learn whether it became an
operation or was denied or expired. After consent there is an **operation id**,
and that is what you poll for progress. **Never re-issue the capability to find out** — a
second call is a second operation and repeats the whole effect.

Polling discipline: while `Queued` or `Running`, check every few seconds at most and only a
handful of times. Then tell the owner it is still running. Do not block a turn on it.

## The `AwaitingUser` handoff

A parked operation is waiting on a human in a browser, and no autonomy setting can complete it.
The connector setup is the case in the product today: the platform mints a single-use ticket and
publishes a trusted Skaile page for it, and the credential is entered only there.

`result.payload.userAction` carries the handoff: `kind`, the `url` to give the owner **verbatim**,
a `label`, and `expiresAt` — the deadline the platform published to the owner, and the one the
platform itself enforces.

What resumes it is not a click. The trusted page re-checks four things live, in order: the
ticket is found only among that caller's *own* parked operations, so a stolen link is inert in
anyone else's session; its window is still open; the caller is still authorized on the
operation's organization *this instant*; and the connector has genuinely become usable. Only
then does the operation return to `Queued`. Redemption is single-use — a replay, a double-click,
and two racing tabs all collapse onto exactly one resume.

If the window closes first, the operation terminalizes itself as `Failed` with an
expired-user-action code. That is an abandoned handoff, not a defect: tell the owner the step
expired and offer to start it again.

So, on `AwaitingUser`: hand over the URL, say it expires shortly, wait. Do not poll in a loop,
do not retry the capability, and never take a secret in chat.

## Consent and autonomy

Every effect here is approval-gated. Per call, the platform either cards it, dispatches it under
an existing autonomy grant, or refuses it — **you do not choose, and cannot predict, which**. Never
promise the owner a card.

A grant is minted only by the owner, only from a card, and it is narrow by construction:

- **One capability.** A grant never spans a family.
- **One scope** — that exact target, everything under its project, everything under its
  organization, or everything of that kind the owner can reach. Only the scopes a capability
  declares, and its own target's ancestry supports, are ever offered.
- **A named window.** The owner picks a duration by name from a server-owned list; the expiry is
  computed on the server. The one-click default alongside "approve once" is deliberately small —
  time-boxed, that exact target, ten minutes, with both effect opt-ins off.
- **Optional use and budget caps**, clamped down to the organization's own ceilings.
- **Effect opt-ins.** Because the safe default leaves both off, an `external` or `privileged`
  effect has no one-click option at all — the owner has to widen it deliberately. An effect
  classed `never` is ungrantable, and so is any batch.

Revocation is immediate, and the platform re-checks the owner's live authorization, the limits
and the budget right before the effect. **A call that dispatched silently a minute ago can come
back parked on approval** — that means it is no longer covered, not that something failed.

You see grants only on turns a human sent, in the `<AUTONOMY>` block: which capability, how far
it reaches, until when, and what is left of any caps, plus notice when one that was in force has
stopped. A schedule firing, a peer agent, or a webhook carries no such block — **its absence
there tells you nothing.** And nothing agent-facing can create, extend, or widen a grant.

## What this family is not

It is not generic CRUD over the data model, and it is not a lifecycle escape hatch. At the time
of writing it carries nothing for deleting an organization, project, or session; for changing or
removing a membership; for editing an organization's settings; or for handling a credential — but
that is a statement about this file, not a promise about the product. **Check the live registry
before telling the owner something is impossible**, and if it genuinely is not there, guide them
to the UI. What you must not do is reach for `platform.act` / `platform.act_batch` to synthesize
one: that surface is default-deny and documented separately in `references/agent-action-catalog.md`,
and every unlisted scope/type pair is blocked there too.
