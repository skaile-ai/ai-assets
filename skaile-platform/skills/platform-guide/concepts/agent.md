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
- Capabilities cover, conceptually: **owner-scoped discovery** (listing the organizations,
  projects and sessions the owner can reach, resolving a session's ancestry, listing a
  project's members, a session's resources, or an organization's connectors); **control-plane
  changes** (creating an organization, a project or a session; inviting someone at any of
  those three levels; starting a connector setup; re-pointing a project's source connector;
  delivering a message into another session as the owner); **reading a durable operation's
  status**; and the session-level actions — enabling/searching/listing assets, opening a
  file in the user's UI, searching GIFs, A2A (list peers / ask / send), setting an
  avatar, scheduling future/recurring actions, run-group operations, creating a session
  webhook inbox, and — in Skailify-enabled sessions — actions registered by an embedded app
  itself. Treat these as *categories* — confirm the exact action against the live registry.
- The control-plane and discovery capabilities are **personal-assistant only**: advertised and
  accepted only in the session the platform resolves as the owner's own assistant. In an
  ordinary project session they are simply not there — another reason to read the live set
  rather than a remembered one.

**The corollary matters as much as the rule: never tell a user you cannot do something
because you do not remember a capability for it.** Look first. Saying "I can't connect that
— you'll have to do it in the UI" is wrong the moment the registry disagrees.

## Approval-gated actions

Some capabilities carry a **consequence** and cannot just run. When the agent invokes one,
the platform decides — per call, itself — between exactly three outcomes:

1. it puts the request to the owner as an **approval card** and parks the call;
2. it **dispatches automatically**, because the owner already granted autonomy covering
   this exact shape of call (see below);
3. it **refuses** the request outright.

You do not choose which, and you cannot tell in advance. So **never promise the user that a
confirmation card will appear.** Say what you are about to do, then read the real result.

This mirrors the agent's own safety rules: confirm before destructive or
consequence-bearing operations (deleting files, overwriting uncommitted work, dropping DB
records, sending messages or data on the user's behalf).

## Autonomy grants — what "already approved" means

An **autonomy grant** is a human pre-authorizing one exact capability so matching calls dispatch
without a card. Only a human can mint one — an owner of this session, from a card they themselves
approved. **No capability lets you create, extend, or widen a grant, and asking for one is not a
thing you can do.**

Each capability declares an **effect class** that decides how far a grant may reach:

| Class | Means | Covered by a grant? |
| --- | --- | --- |
| `routine` | Effect stays inside the owner's own platform surface. | Yes — this is what an ordinary grant covers. |
| `external` | Reaches a person outside this conversation (an invitation email, a message delivered into someone else's session). | Only if the owner **explicitly widened** the grant to external communication. |
| `privileged` | Administrative — changes who or what exists at organization level. | Only if the owner **explicitly widened** the grant to privileged administration. |
| `never` | Never auto-approvable. | No — it is carded, or refused outright. Never dispatched silently. |

A grant is narrow: it names **one capability**, one target scope, and an absolute expiry, and
may carry use and budget caps. Batch requests are ungrantable outright — `platform.act_batch`
always requires a card.

Two consequences you must actually act on:

- **A grant can stop at any instant.** The owner can revoke it, it can expire, or a limit can
  run out — and the platform re-checks the owner's live authorization, the limits and the
  budget immediately *before* the effect. A call that dispatched silently a minute ago can
  come back parked on approval instead. That is not a failure and not an error; it means the
  call is no longer covered.
- **You only see grants on turns a human sent.** When the owner sends a turn you are also
  shown an `<AUTONOMY>` block listing each grant in force — the capability, how far it
  reaches, until when, and what is left of any use and budget caps — and naming any that have
  been **revoked**. A grant that merely expired produces no notice; it just stops appearing.
  Turns nobody sent (a schedule firing, a peer agent, a webhook) carry no block at all, so
  **its absence there tells you nothing**. Never infer "I have no autonomy" from a missing
  block.

## Durable operations — the receipt, not the result

Most control-plane capabilities that change something do **not** return the thing they
created. Once the owner consents, they hand the work to a background worker and return a
**receipt** — `{ operationId, status: "Queued", target, instruction }` — and the effect
happens afterwards. (Delivering a message into another session is the exception: it returns
its own result. Read the receipt you actually get rather than assuming either shape.)

`platform.get_operation` is how you find out what happened. It takes either an `operationId`
or, before an operation exists, the `invocationId` of a call still parked on approval.

Four rules carry the whole model:

- **Six states, three of them terminal.** `Queued` and `Running` mean keep waiting;
  `Succeeded`, `Failed` and `Cancelled` will never change again, so stop polling and report.
  `AwaitingUser` is the odd one — non-terminal, but you cannot advance it yourself.
- **Follow `instruction`, do not cache it.** It restates the next step for the status you
  just read and changes with the status.
- **Poll sparingly.** Every few seconds at most, a handful of times, then tell the owner it
  is still running rather than blocking the turn on it.
- **Retrying is not free.** Calling the capability again is a new invocation, so it creates a
  **second operation that repeats the whole effect** — a second project, or a second email to
  someone outside the conversation. Read the operation you already have first: if it is live,
  wait; if it failed, say so and let the owner decide. Never re-issue one whose outcome you
  could not read.

Field-level detail — the retry and partial-progress fields, the exact refusal codes, which
capabilities are durable and what each one takes — is in
`references/control-plane-capabilities.md`. Load it when you are about to construct one of
these calls.

## `AwaitingUser` — the handoff contract

`AwaitingUser` is the state that says: *this needs a human in a browser, and you cannot
finish it.* Connector setup is the case in the product today — the owner completes the
provider's own sign-in on a trusted Skaile page.

When you see it: give the owner the URL the operation published, **verbatim**, say what it is
for and that it expires shortly, and then **wait**. Do not poll in a loop and do not retry the
capability — a retry does not resume this operation, it starts a second one. And **never ask
for, or accept, a credential in chat**: no token, password, personal access token, OAuth code
or client secret. The capabilities reject every such field by design, and the credential is
only ever entered on that page.

It has exactly two exits and you control neither. Either the human completes the real-world
step and the platform verifies the condition genuinely holds — a click alone never resumes it
— and the operation returns to `Queued` and continues on its own; or the window closes and the
operation ends by itself as `Failed`. The second is an abandoned handoff, not a defect: say
the step expired and offer to start it again.

## Discovery first, then propose

The read-only, owner-scoped discovery capabilities are the **id-resolution step**. Resolve an
id there before proposing any effect that takes one — never ask the owner for an id you can
look up, and never guess one.

The pairings that matter:

| Before proposing... | Read first | Why |
| --- | --- | --- |
| creating a project | the owner's organizations | you need an organization id the owner can actually create in |
| creating a session | the owner's projects | you need a project id, and the owner's live role on it |
| inviting to a project | the owner's projects, then that project's members | an `Active` or `Invited` row means do not re-invite; `Expired`/`Revoked` is not a live invitation |
| inviting to a session | the owner's sessions | you need a session id; access to *read* a session does not allow inviting into it |
| starting a connector setup | that organization's connectors | if one already reports `usable: true`, you do not need the setup at all |
| re-pointing a project's source | that organization's connectors | only a connector that is already `usable` can be pointed at |

These lists are paged: page until the cursor comes back null rather than concluding the owner
has exactly one page. Their refusals are **terminal** — "not accessible" means the owner cannot
see that target, so tell them rather than retrying or guessing at other ids. The refusal is
deliberately identical whether the target does not exist or the owner has no standing on it.

## Target-bound actions via `platform.act` and `platform.act_batch`

These are a separate, much narrower thing from the dedicated control-plane capabilities
above. `platform.act` is a narrow, default-deny capability. Its sole current action is:

`{ scope: "project", type: "markAllSessionsRead", payload: { id: "<projectId>" }, rationale }`

The platform parses the request, resolves the canonical target project, and checks the
session owner's effective role on that target **before** showing an approval card. Unknown,
malformed, lifecycle, membership, credential/provider, runtime, and destructive actions
fail without a card. If approved, the platform reloads the current owner and target and
reauthorizes immediately before execution. The human who clicks Approve supplies consent;
they do not replace the session owner as the action actor.

Rules:

- **Prefer a dedicated capability when one exists.** Never infer a `platform.act` scope,
  type, or payload from the data model.
- **Always pass a specific `rationale`** and wait for the real result.
- Target-project `User` and `Owner` roles are allowed; `Viewer` and no-access roles are
  denied. PlatformAdmin remains the explicit break-glass role.
- The action clears unread indicators for every member of every session in the target
  project. Describe that consequence accurately when proposing it.

`platform.act_batch` can run an ordered, bounded list of that same action. It is not a
broad CRUD or lifecycle escape hatch. The platform validates every step, exact target,
compatible action family, and typed symbolic dependency before showing one approval
proposal. A step may set its `payload.id` to `{ "$ref": [earlierStepIndex, "id"] }` to
reuse the canonical project id produced by an earlier step; forward, missing, malformed,
or type-incompatible references are rejected before any effect.

Batch execution is ordered and best-effort. The platform reloads and reauthorizes each
target immediately before that step, stops on the first failure, and reports exactly
which steps completed, failed, and remain unexecuted. Completed effects are never rolled
back; retrying is a new batch. To keep that receipt truthful, the platform waits for each
authoritative dispatcher result instead of declaring a non-cancelling timeout a failure,
so a slow step can keep the batch pending. The batch currently always requires an explicit
approval card; a wildcard or standing grant cannot suppress it.

## UI context the platform feeds the agent

User prompts may be prefixed with a silent `<ui_context speaker="...">` block telling the
agent the speaker's current UI state. Never echo or mention it. Adapt to it:

| Key                     | Adapt by                                                                 |
| ----------------------- | ----------------------------------------------------------------------- |
| `audioMode=true`        | Reply will be read aloud — short spoken sentences, no markdown/tables/code/paths. |
| `expertMode=true`       | Terse, technical; skip basics; lean on exact identifiers and paths.     |
| `selectedFile=<path>`   | "this file" / ambiguous references mean this file — not proof the Workspace pane is visible now; see below. |
| `selectedResource=<id>` | Same, for a connector/volume the user is browsing.                      |

Missing block ⇒ behave as if all flags are false.

`selectedFile` is durable reference-resolution state: set once, it persists across reloads
and reconnects. Pane visibility is **separate**, ephemeral, per-tab, in-memory state that
resets independently — a reload, a new tab, or time passing can close the pane while
`selectedFile` stays set. Never infer "the pane is open" from `selectedFile` alone. Before
telling a user a file or the workspace is already open, re-assert it: call
`platform.open_file` again for a specific file, or `platform.set_session_view({ action:
"activate_workspace" })` to reveal the workspace generally. Both are idempotent and cheap —
prefer re-invoking over guessing from stale context.

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
prefer guiding when the user wants to learn the UI, or when the capability genuinely is not
in the live set for this session.

A browser step is not by itself a reason to hand the whole task over. Connector sign-in is the
case to have in mind: the assistant **starts** the setup and the platform hands the owner one
expiring link for the part only they can do — see the `AwaitingUser` contract above. Do the
half you can do, then hand off the half you cannot; do not decline the whole thing because
part of it needs a browser. Some things genuinely do belong entirely to the UI — creating a
project backed by a connector source, or a session that shares a git branch — and for those,
guiding is the right answer.
