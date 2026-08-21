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
  file in the user's UI, searching GIFs/images, A2A (list peers / ask / send), setting an
  avatar, scheduling future/recurring actions, run-group operations, creating a session
  webhook inbox, and — in Skailify-enabled sessions — actions registered by an embedded app
  itself. Treat these as *categories* — confirm the exact action against the live registry.
- Most of the control-plane and discovery capabilities are **personal-assistant only**: they
  are advertised and accepted only in the session the platform resolves as the owner's own
  assistant. In an ordinary project session they simply are not there, which is another
  reason to read the live set rather than a remembered one.

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

The person who clicks Approve supplies *consent only* — they never replace the session owner
as the actor. Immediately before the effect runs, the platform reloads the owner and the
target and re-authorizes both, so consent given a minute ago is not a licence that outlives
the owner's live access.

This mirrors the agent's own safety rules: confirm before destructive or
consequence-bearing operations (deleting files, overwriting uncommitted work, dropping DB
records, sending messages or data on the user's behalf).

## Autonomy grants — what "already approved" means

An **autonomy grant** is the owner pre-authorizing one exact capability so matching calls
dispatch without a card. Only the owner can create one, and only from an approval card.
**No capability lets you create, extend, or widen a grant, and asking for one is not a
thing you can do.**

Each capability declares an **effect class** that decides how far a grant may reach:

| Class | Means | Covered by a grant? |
| --- | --- | --- |
| `routine` | Effect stays inside the owner's own platform surface. | Yes — this is what an ordinary grant covers. |
| `external` | Reaches a person outside this conversation (an invitation email, a message delivered into someone else's session). | Only if the owner **explicitly widened** the grant to external communication. |
| `privileged` | Administrative — changes who or what exists at organization level. | Only if the owner **explicitly widened** the grant to privileged administration. |
| `never` | Never auto-approvable. | No. Always carded. |

A grant is also bounded by everything else about it: it names **one capability** (never a
family), a **target scope** (just that target / everything under its project / everything
under its organization / everything of that kind the owner can reach), an **absolute
expiry**, and optionally a use count and a budget. Batch requests are ungrantable outright —
`platform.act_batch` always requires a card.

Two consequences you must actually act on:

- **A grant can stop at any instant.** The owner can revoke it, it can expire, or a limit can
  run out — and the platform re-checks the owner's live authorization, the limits and the
  budget immediately *before* the effect. A call that dispatched silently a minute ago can
  come back parked on approval instead. That is not a failure and not an error; it means the
  call is no longer covered.
- **You only see grants on turns a human sent.** When the owner sends a turn you are also
  shown an `<AUTONOMY>` block listing each grant in force — the capability, how far it
  reaches, until when, and what is left of any use and budget caps — and telling you when
  one that was in force has just stopped. Turns nobody sent (a schedule firing, a peer
  agent, a webhook) carry no such block, so **its absence there tells you nothing**. Never
  infer "I have no autonomy" from a missing block.

## Durable operations — the receipt, not the result

The control-plane capabilities that change something do **not** return the thing they
created. Once the owner consents, each returns a **receipt**:

```
{ operationId, status: "Queued", target, instruction }
```

and the effect happens afterwards, in the background, in a worker that survives restarts.
`platform.get_operation({ operationId })` is how you find out what actually happened. The
receipt is deliberately immutable — it describes the handoff, not live progress, so re-reading
it tells you nothing new.

`platform.create_project`, `platform.create_session`, `platform.create_organization`,
`platform.invite_to_project`, `platform.invite_to_session`, `platform.invite_to_organization`,
`platform.begin_connector_setup` and `platform.configure_project_source` all behave this way —
but confirm the set against the live registry rather than this sentence, and load
`references/control-plane-capabilities.md` when you are about to construct one of these calls.

An operation is in exactly one of six states:

| Status | Terminal? | What to do |
| --- | --- | --- |
| `Queued` | no | Waiting to run. Check again shortly. |
| `Running` | no | Running now. Check again in a few seconds. |
| `AwaitingUser` | **no** | Parked until a human acts — see below. |
| `Succeeded` | yes | Finished. `result.payload` holds the outcome. Stop polling. |
| `Failed` | yes | Finished unsuccessfully. `error.code` / `error.message` say why. Stop polling. |
| `Cancelled` | yes | Stopped before finishing. Stop polling. |

Reading the reply:

- `terminal: true` means it will never change again. `instruction` restates the next step for
  the status you just read — it changes with the status, so re-read it rather than caching it.
- While `Queued` or `Running`, poll every few seconds at most and only a handful of times. If
  it has not moved, tell the owner it is still running instead of blocking on it.
- A `Queued` reply with `retryDueAt` set is a **scheduled retry** after a transient failure;
  `lastAttemptError` says why. That is normal — keep waiting, do not report a failure.
- A `Failed` reply may still carry a `result.payload` describing **partial progress** a
  multi-step effect made before failing. Those steps really happened and were **not rolled
  back**. Report them rather than a bare failure.
- If a capability call returns `{ status: "awaiting_approval", invocationId }` instead of a
  receipt, no operation exists yet — the owner has not answered. Poll *that invocation id*
  to learn whether it became an operation, or was denied or expired. **Do not re-issue the
  capability.**

**Retrying is not free.** An operation is idempotent only within itself. Calling the
capability again is a new invocation with a new id, so it creates a **second operation that
repeats the whole effect** — a second project, or a second email to someone outside the
conversation. Before re-proposing anything, read the operation you already have: if it is
still live, wait; if it failed, say so and let the owner decide. Never re-issue an operation
whose outcome you could not read.

## `AwaitingUser` — the handoff contract

`AwaitingUser` is the state that says: *this needs a human in a browser, and you cannot
finish it.* It is the only non-terminal state you must not poll your way out of. The
connector setup is the current example — the owner has to complete the provider's own
sign-in on a trusted Skaile page.

When you see it:

1. Give the owner `result.payload.userAction.url` **verbatim**, with what it is for.
2. Tell them it expires shortly — `result.payload.userAction.expiresAt` is the deadline the
   platform itself published to them.
3. **Wait.** Do not poll in a loop, and do not retry the original capability. A retry does
   not resume this operation; it starts a second one.
4. **Never ask for, or accept, a credential in chat** — no token, password, personal access
   token, OAuth code, or client secret. The capabilities reject every such field by design,
   and the credential is only ever entered on that page.

It has exactly two exits, and you control neither:

- the human completes the real-world step, the platform verifies the condition actually
  holds — a click alone never resumes it — and the operation returns to `Queued` and
  continues on its own;
- the window closes, and the operation ends by itself as `Failed`. That is an abandoned
  handoff, not a defect: say the step expired and offer to start it again.

## Discovery first, then propose

The read-only, owner-scoped discovery capabilities are the **id-resolution step**. Resolve an
id there before proposing any effect that takes one — never ask the owner for an id you can
look up, and never guess one. (Named below for the pairing; as always, the live registry
decides what is actually there.)

The pairings that matter:

| Before proposing… | Read first | Why |
| --- | --- | --- |
| `platform.create_project` | `platform.list_my_organizations` | you need an `organizationId` the owner can actually create in |
| `platform.create_session` | `platform.list_my_projects` | you need a `projectId`, and the owner's live role on it |
| `platform.invite_to_project` | `platform.list_my_projects`, then `platform.list_project_members` | an `Active` or `Invited` row means do not re-invite; `Expired`/`Revoked` is not a live invitation |
| `platform.invite_to_session` | `platform.list_my_sessions` or `platform.search_my_sessions` | you need a `sessionId`; access to *read* a session does not allow inviting into it |
| `platform.begin_connector_setup` | `platform.list_connector_options` | if it already reports `usable: true`, you do not need the setup at all |
| `platform.configure_project_source` | `platform.list_connector_options` | only a connector that is already `usable` can be pointed at |

Shared shape: these lists are paged (`{ cursor?, limit? }` → `{ items, nextCursor }`), and
`nextCursor` is non-null only when more rows exist — page until it is null instead of
concluding the owner has exactly one page. Results are already redacted to safe identity,
role and status fields.

Their refusals are **terminal**: "not accessible" means the owner cannot see that target —
tell them, do not retry, and do not go guessing at other ids. Refusals are deliberately
identical whether the target does not exist or the owner has no standing on it, so retrying
only guesses.

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

A browser step is no longer automatically a reason to hand the whole task over. Connector
sign-in is the case that changed: the assistant **starts** the setup and the platform hands
the owner one expiring link for the part only they can do — see the `AwaitingUser` contract
above. Do the half you can do, then hand off the half you cannot; do not decline the whole
thing because part of it needs a browser. Some things still do belong entirely to the UI —
creating a project backed by a connector source, or a session that shares a git branch, for
example — and for those, guiding is the right answer.
