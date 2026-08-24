# Control-Plane Capabilities — durable operations, consent, and the handoff

The control plane is the family of capabilities that **change what exists** on the platform —
organizations, projects, sessions, memberships, connector wiring — plus the read-only
discovery that resolves the ids they take, and the one query that reports what happened.

This file is a **map, not a contract**. The live registry is authoritative: the set changes
every deploy, and this family is advertised only in the owner's own personal-assistant
session. Consult the capabilities available in the current turn and use the exact schema they
carry. Read this to know what the family *is* and how consent and completion work in it — not
to decide whether a capability exists. `concepts/agent.md` carries the model; this is the
detail you load when you are about to construct one of these calls.

## The family

### Read-only — resolve ids here first

Owner-scoped, query-only, and available only where the platform resolves the calling session
as the owner's own assistant. They never create approvals, grants, operations, invitations,
or connector configuration.

| Call | Gives you |
| --- | --- |
| `platform.list_my_organizations({ search?, cursor?, limit? })` | `organizationId`, the owner's live role, a permissions summary |
| `platform.list_my_projects({ organizationId?, search?, cursor?, limit? })` | `projectId`, `organizationId`, status, visibility, source type, live role. The assistant's own workspace is never listed. |
| `platform.list_my_sessions({ organizationId?, projectId?, archived?, search?, cursor?, limit? })` | `sessionId` with full ancestry (organization → project → session), live role. Omit `archived` for both. |
| `platform.get_session_context({ sessionId })` | one session's ancestry plus the owner's effective role at each level. Not paged. |
| `platform.list_project_members({ projectId, search?, cursor?, limit? })` | every membership *and invitation* row, with `status`: `Active`, `Invited`, `Expired`, `Revoked` |
| `platform.list_session_resources({ sessionId, search?, cursor?, limit? })` | the project source plus every library asset in effect, with provenance |
| `platform.list_connector_options({ organizationId, projectId?, search?, cursor?, limit? })` | an organization's connectors, redacted to identity and readiness — `usable`, and when false, `requiredHandoff` |

Shared shape for the seven above: `{ cursor?, limit? }` in (limit 1–50), `{ items, nextCursor }`
out. `nextCursor` is non-null only when more rows exist. A cursor replayed after you change a
filter or `limit` is rejected — restart paging without one. Results are already redacted to safe
identity, role and status fields.

Two more read the *conversations* rather than the structure. Neither uses the cursor contract
above:

| Call | Gives you |
| --- | --- |
| `platform.search_my_sessions({ query, limit? })` | `hits` — snippet, `sessionId`, `projectId`, `seq`, `createdAt` — across the sessions the owner can read. `limit` caps at 100. |
| `platform.read_session_history({ sessionId, limit?, beforeSeq? })` | `messages`, newest-first, from one session the owner can reach, plus `hasMore`. `limit` defaults to 50 and caps at 200; `beforeSeq` pages backwards, returning only messages with `seq` strictly below it. |

Use them in that order: search to find the session, then read that session's history. Search
scans the 50 most-recently-active sessions and returns at most 100 hits, and `truncated: true`
means it hit one of those two caps — not that nothing else matched. So treat a truncated search
as "look harder", never as a complete answer.

**These reads are audited.** Four capabilities write a personal-assistant read audit naming the
owner, the target session's ancestry and how much came back: `platform.search_my_sessions`,
`platform.read_session_history`, `platform.get_session_context` and
`platform.list_session_resources`. Reading a colleague's conversation on the owner's behalf
leaves a record. That is not a reason to avoid it when the owner asks — it is a reason not to
go trawling sessions speculatively.

Do not confuse `platform.read_session_history` with `platform.read_own_session_history`. The
latter is **not** part of this family: it is available in ordinary project sessions, always
targets the calling session, and takes no `sessionId` at all.

Reading connector readiness is the one that most often ends the task early:

- `usable: true` — the owner can already use it. **Do not start a setup.**
- `usable: false`, `requiredHandoff: connect_user_credential` — it takes a per-user credential
  the owner has not connected. That is exactly what a connector setup starts.
- `usable: false`, `requiredHandoff: contact_organization_admin` — it does not take a per-user
  credential, so connecting it is an organization-level change. Say so; do not assume you can
  complete it.

### Effects — each returns a receipt, not a result

Every capability in the table below hands the work to a durable background worker once the
owner consents, and returns `{ operationId, status: "Queued", target, instruction }`. None of
them returns the thing it made. None of them may appear in a batch. The **effect class** is
what decides whether an autonomy grant can ever cover it (see *Consent and autonomy* below).

| Call | Effect | Effect class | Grant may reach |
| --- | --- | --- | --- |
| `platform.create_organization({ name, slug?, logoUrl?, iconSvg? })` | a new organization | `privileged` | only the widest scope: every target of that kind the owner can reach |
| `platform.create_project({ organizationId, name, sourceType, description?, visibility?, agentName?, agentAvatarUrl?, initialMessage? })` | a new project. `sourceType` is `Empty` or `OnSkaile`; `visibility` `Private` (default) or `Shared`. | `routine` | that target, its organization, or everything reachable |
| `platform.create_session({ projectId, name, slug?, followMain?, visibility? })` | a new session | `routine` | that target, its project, its organization, or everything reachable |
| `platform.invite_to_organization({ organizationId, email, role? })` | an invitation email | `external` | that target, its organization, or everything reachable |
| `platform.invite_to_project({ projectId, email, role? })` | an invitation email | `external` | that target, its project, its organization, or everything reachable |
| `platform.invite_to_session({ sessionId, email, role? })` | an invitation email; the invitee can then read that session's whole history | `external` | that target, its project, its organization, or everything reachable |
| `platform.begin_connector_setup({ organizationId, providerType, providerLinkId? })` | reuses an already-usable connector, otherwise parks on the owner. `result.payload.reused` says which happened. | `routine` | that target, its organization, or everything reachable |
| `platform.configure_project_source({ projectId, providerLinkId })` | re-points a project at an already-usable connector. `result.payload.changed` says whether anything actually had to move; re-pointing at the current one is a no-op, not an error. | `routine` | that target, its project, its organization, or everything reachable |

`platform.delegate_to_session({ sessionId, message, visibility: "Public" })` delivers one
message into another session as the owner. It is also approval-gated and classed `external`,
with a grant reaching **that one target and nothing else** — but it is **not durable**: it
returns its own result rather than an operation receipt, so there is no `operationId` to poll.
The delivered message always shows it was sent by the owner via their Personal Assistant; it
is never attributed to the assistant.

### Boundaries that are real, not conservatism

These are refusals by design — proposing around them wastes the owner's approval:

- **Sources.** A project created here can be empty or Skaile-hosted. One backed by a connector
  (Git, SharePoint, Google Drive, Box, NextCloud) is created from the web app. Re-pointing only
  moves a project that *already has* a Git source onto a different, already-usable connector —
  it cannot add a source, create a connector, or create a project.
- **Roles on invite.** `Viewer` (default) or `User`, at all three levels. `Owner` is not
  assignable through any of these. Note that a **session** invite uses this same
  `Viewer`/`User` vocabulary through the capability — not the Owner/Participant labels the
  Share tab shows (`concepts/collaboration.md`). No personal note, personal message, or
  display name can be attached — the human adds those from the web app.
- **Personal workspaces.** A personal-assistant project or session cannot be invited into, and
  a personal organization cannot be invited into. Pick a shared one.
- **Credentials.** No capability in this family accepts a token, password, personal access
  token, OAuth code, client secret, repository URL, or branch. Every such field is rejected.
  Never ask for one, and never accept one if offered.
- **Batches.** None of these is batch-eligible, and none is grantable through a batch.
- **Creating an organization is PlatformAdmin-only.** The server verifies the owner currently
  holds PlatformAdmin — membership, however senior, is not enough. Do not offer it to an owner
  who is not one.
- **Nothing lists an organization's members.** `platform.list_project_members` covers projects
  only. Before an organization invite, *ask the owner* whether the person is already a member:
  an existing member is refused only **after** their approval has been spent.

## The operation lifecycle

`platform.get_operation` reads one operation. It takes **either** `{ operationId }` **or**
`{ invocationId }` — one key, never both.

The durable lifecycle is **not exclusive to this family**: appending inputs to a run group can
also return a receipt rather than a result, and is read back the same way (run groups are covered
in `concepts/flows.md`). That happens on the approval-card durable path; pre-approved card-free
appends and fallback sync appends return a direct append result. So a receipt from outside the
table above is not anomalous — read it here.

The operation lifecycle is `Queued` →
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
  For these effects a recovered attempt resumes from a checkpoint rather than repeating the
  write, so a rising count is not the effect happening twice.
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

A grant is minted only by a human — an owner of the assistant session, from a card they
themselves approved — and it is narrow by construction:

- **One capability.** A grant never spans a family.
- **One scope** — that exact target, everything under its project, everything under its
  organization, or everything of that kind the owner can reach. Only the scopes a capability
  declares, and its own target's ancestry supports, are ever offered.
- **A named window.** The owner picks a duration by name from a server-owned list; the expiry is
  computed on the server. The one-click option alongside "approve once" is deliberately narrow —
  time-boxed, that exact target, both effect opt-ins off — and its length is server-chosen per
  capability, so do not quote a number at the owner.
- **Optional use and budget caps**, clamped down to the server's own ceilings.
- **Effect opt-ins.** Because the safe default leaves both off, an `external` or `privileged`
  effect has no one-click option at all — the owner has to widen it deliberately. An effect
  classed `never` is ungrantable, and so is any batch. Ungrantable means it can only be carded
  or refused — never dispatched silently.

Revocation is immediate, and the platform re-checks the owner's live authorization, the limits
and the budget right before the effect. **A call that dispatched silently a minute ago can come
back parked on approval** — that means it is no longer covered, not that something failed.

You see grants only on turns a human sent, in the `<AUTONOMY>` block: which capability, how far
it reaches, until when, and what is left of any caps, plus notice when one has been **revoked**.
Expiry produces no notice at all — the row simply stops appearing — so a grant vanishing from the
block is not evidence of revocation. A schedule firing, a peer agent, or a webhook carries no
block at all, and **its absence there tells you nothing.** And nothing agent-facing can create, extend, or widen a grant.

## What this family is not

It is not generic CRUD over the data model, and it is not a lifecycle escape hatch. This file
lists nothing for deleting an organization, project, or session; for changing or removing a
membership; for editing an organization's settings; or for handling a credential.
**Check the live registry before telling the owner any of those is impossible** — this file is
a map, and the registry moves. If it genuinely is not there, guide them to the UI. What you
must not do is reach for `platform.act` / `platform.act_batch` to synthesize one: that surface
is default-deny and documented separately in `references/agent-action-catalog.md`, where every
unlisted scope/type pair is blocked too.

Grounded in: `platform/docs/protocol-v2-capabilities.md` and
`platform/backend/libs/capabilities/`.
