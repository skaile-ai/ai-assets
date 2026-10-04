# Control-Plane Capabilities — durable operations, consent, and the handoff

The control plane is the family of capabilities that **change what exists** on the platform —
organizations, projects, sessions, memberships, connector wiring — plus the read-only
discovery that resolves the ids they take, and the one query that reports what happened.

This file is a **map, not a contract**. The live registry is authoritative: the set changes
every deploy, and most of this family is advertised only in the owner's own personal-assistant
session (the exceptions are listed under *Session-owner effects* below). Consult the capabilities available in the current turn and use the exact schema they
carry. Read this to know what the family *is* and how consent and completion work in it — not
to decide whether a capability exists. `concepts/agent.md` carries the model; this is the
detail you load when you are about to construct one of these calls.

## The family

### Read-only — resolve ids here first

Owner-scoped, query-only, and available only where the platform resolves the calling session
as the owner's own assistant. They never create approvals, grants, operations, invitations,
or connector configuration.

**Which organizations you reach depends on where you live.** An assistant whose Home is in
a business workspace sees and acts **only in that workspace**: discovery, linking to
sessions, creating sessions and projects, invitations, connector setup, delegation and
running flows all stop at its border, and a target elsewhere reads as not found or a generic
denial. If the owner needs something in another workspace, point them to their Private
workspace's assistant, the home assistant. The home assistant reaches each other
organization the owner belongs to (an invited Private workspace too) only as far as that organization's **reach** level allows
(`concepts/agent.md` § *Assistant reach*): at **Off** the organization is left out of every
list below; at **Coordinate** (the default) the seven structural lists include it, but
`platform.search_my_sessions`, `platform.read_session_history`, the file calls and every
effect in this family do not reach it; only **Full** opens those.

| Call | Gives you |
| --- | --- |
| `platform.list_my_organizations({ search?, cursor?, limit? })` | `organizationId`, the owner's live role, a permissions summary |
| `platform.list_my_projects({ organizationId?, search?, cursor?, limit? })` | `projectId`, `organizationId`, status, visibility, source type, live role. The Home in the owner's home Private workspace is never listed; the owner's Homes anywhere else (a business organization, or a Private workspace they were invited into) are listed like any project. Nobody else's Home is ever listed. |
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

Use them in that order: search to find the session, then read that session's history. To hand
a file you found there to an action (an upload, a mail attachment), pass it on by reference
(`{ sessionId, path }`, see `references/agent-action-catalog.md`) rather than copying its
content; to read its text yourself, use `platform.read_session_file` (below). Searching,
reading history, passing a file on and reading it all need **Full** reach into that
session's organization; search skips sessions anywhere else. Search
scans the 50 most-recently-active sessions and returns at most 100 hits, and `truncated: true`
means it hit one of those two caps — not that nothing else matched. So treat a truncated search
as "look harder", never as a complete answer.

**These reads are audited.** `platform.search_my_sessions`, `platform.read_session_history`,
`platform.get_session_context`, `platform.list_session_resources` and
`platform.read_session_file` write a personal-assistant read audit naming the owner, the target
session's ancestry and how much came back — and so does reading another session's file by
reference (`platform.add_draft_attachment`, an upload through
`platform.invoke`). Reading a colleague's conversation on the owner's behalf
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

### Files in the owner's other sessions

The personal assistant can read and change a text file in the `workspace` of another of the
owner's sessions. `path` is relative to that workspace, with no leading slash, no `..`, and no
leading `workspace/`. The two content limits below count characters; the 2 MiB ceiling and
`size` are bytes.

| Call | Does |
| --- | --- |
| `platform.read_session_file({ sessionId, path })` | returns `{ sessionId, path, content, truncated, size }`. Only the first 262,144 characters come back (`truncated: true` when the file is longer); `size` is the whole file in bytes. A file over 2 MiB, or one that is not UTF-8 text, is refused. |
| `platform.write_session_file({ sessionId, path, content, mode })` | creates (`mode: "create"`) or replaces (`"replace"`) one text file, as the owner. `content` is the whole file, at most 65,536 characters. Only a text file of at most 65,536 characters can be replaced. |

- **Read** is a query: no card while the owner is the only reader of your session; once anyone
  else can read it, each read goes to the owner as a card, like any read of the owner's private
  things (`concepts/agent.md`, *Shared sessions*). It never gets a standing grant. If that card
  is still unanswered when the call stops waiting, do not poll: the result is never kept, so
  call again only when the owner asks.
- **Write** is approval-gated. The owner sees the path and the change side by side on a card,
  unless a standing grant covers it; a grant can cover that one session or every session of
  its project. Read the file before you replace it. If the owner has not answered by the time
  the call stops waiting, it comes back as `{ status: "awaiting_approval", invocationId }` like
  any carded call (see *The operation lifecycle*); once it runs it returns `{ status: "written",
  sessionId, path }` itself, with no operation id and nothing further to poll.
- **Refusal codes**, for both calls. Like `platform.finish_spawned_instance` (*Agent
  templates* below), these two calls refuse with a code you can act on.
  `not_found`: the session does not exist, the owner cannot see it (another person's
  private project reads the same way), or its organization does not allow you to read files
  there (reach below **Full**); do not retry, tell the owner what you could not reach.
  `file_not_found`: the session is reachable but has no such file. `invalid_path`: the path
  breaks the rules above.
- **Write refusals in words**, not codes, each saying why: your own session (write your own
  workspace directly); a session the owner can only view (the message says writing needs the
  User or Owner role there, so tell the owner that rather than that the session is missing); a
  read-only folder; a `create` on an existing file or a `replace` with no file there (the
  message names the mode to use; this comes before any card).
- **The file changed.** The write lands only if the file still holds exactly the text the card
  showed (for `create`: still does not exist). A refusal saying it changed since the card was
  shown means someone edited it meanwhile: read it again and propose anew.

### Effects — each returns a receipt, not a result

Every capability in the table below hands the work to a durable background worker once the
owner consents, and returns `{ operationId, status: "Queued", target, instruction }`. None of
them returns the thing it made. None of them is a catalogue action, so none can be a
`platform.batch` step. The **effect class** is
what decides whether an autonomy grant can ever cover it (see *Consent and autonomy* below).

| Call | Effect | Effect class | Grant may reach |
| --- | --- | --- | --- |
| `platform.create_organization({ name, slug?, logoUrl?, iconSvg? })` | a new organization | `privileged` | only the widest scope: every target of that kind the owner can reach |
| `platform.update_organization_branding({ organizationId, name?, logoUrl?, iconSvg? })` | a new name, logo URL or icon for an organization; at least one field, and an empty string removes the logo or icon | `routine` | that exact target only |
| `platform.create_project({ organizationId, name, sourceType, description?, visibility?, agentName?, agentAvatarUrl?, initialMessage? })` | a new project. `sourceType` is `Empty` or `OnSkaile`; `visibility` `Private` (default) or `Shared`. | `routine` | that target, its organization, or everything reachable |
| `platform.create_session({ projectId, name, slug?, followMain?, visibility? })` | a new session; once `Succeeded`, `result.payload.url` links to it and `result.payload.sessionId` names it — share the link, or bring it up with `platform.navigate({ route: "session", params: { session: result.payload.sessionId } })` when the owner asked to go there. In a private project (the assistant's Home or a My space project) the session is always Private and `visibility: "Shared"` is refused. | `routine` | that target, its project, its organization, or everything reachable |
| `platform.invite_to_organization({ organizationId, email, role?, personalMessage? })` | an invitation email | `external`; `never` when `role` is `Owner` or a `personalMessage` is set | that target, its organization, or everything reachable — never an Owner invitation or one with a message |
| `platform.invite_to_project({ projectId, email, role? })` | an invitation email | `external` | that target, its project, its organization, or everything reachable |
| `platform.invite_to_session({ sessionId, email, role? })` | an invitation email; the invitee can then read that session's whole history | `external` | that target, its project, its organization, or everything reachable |
| `platform.begin_connector_setup({ organizationId, providerType, providerLinkId? })` | reuses an already-usable connector, otherwise parks on the owner. `result.payload.reused` says which happened. | `routine` | that target, its organization, or everything reachable |
| `platform.configure_project_source({ projectId, providerLinkId })` | re-points a project at an already-usable connector. `result.payload.changed` says whether anything actually had to move; re-pointing at the current one is a no-op, not an error. | `routine` | that target, its project, its organization, or everything reachable |

`platform.delegate_to_session({ sessionId, message, visibility: "Public" })` delivers one
message into another session as the owner. It is also approval-gated but classed `routine` —
the message never leaves Skaile, so a grant needs no external opt-in — with a grant reaching
**that one target and nothing else**. It cannot yet be requested ahead with
`platform.request_standing_approval`. And it is **not durable**: it
returns its own result rather than an operation receipt, so there is no `operationId` to poll.
The delivered message always shows it was sent by the owner via their Personal Assistant; it
is never attributed to the assistant.

`platform.invite_user` is the ordinary project session's way to invite someone to its project;
use `platform.invite_to_project` instead wherever that is offered. It shares that capability's
gate and its `external` class, so a grant covers it only if the owner explicitly included
external communication — but a grant for one never covers the other. It is durable too: it
returns an operation receipt, read with `platform.get_operation`. It takes no `context` note; a
person adds one from the web app.

### The assistant profile — `platform.update_assistant_profile`

All of the owner's assistants, in every workspace, share one profile, shown to you as the
`<ASSISTANT_PROFILE>` block: a name and three documents, **IDENTITY** (who you are), **SOUL**
(how you speak) and **USER** (what you know about the owner). The **Language:** line in USER is
the language rule: reply in that language, add the line once you know the language the owner
uses with you, and change it only when they ask to switch. Change the profile only with
`platform.update_assistant_profile({ document, mode, content })`:

- `document` is `"identity"`, `"soul"` or `"user"`; `mode` is `"replace"` (the whole
  document) or `"append"` (adds `content` on a new line).
- The owner sees a card with the document before and after. A standing grant can cover SOUL
  and USER, and only for the assistant in the owner's Private workspace; **IDENTITY always
  shows the card**, and so does every change from an assistant in a business workspace. You
  cannot request the grant ahead with `platform.request_standing_approval`; the owner grants
  it from a card.
- Caps: IDENTITY 4000, SOUL 8000, USER 12000 characters, and 24 KiB for the three together.
  `{ error: "too_long" }` means the result would exceed one; shorten it. A refusal saying the
  profile changed means it was edited meanwhile: read the new profile, then propose again.
- Keep organization details out of the profile unless the owner asks: every workspace's
  assistant reads it.

Your name, voice and avatar have their own capabilities. Called from the Private workspace's
assistant they change all of the owner's assistants; from a business workspace they change
only that one. A later change of the same thing (name, voice or avatar, which the app calls the
picture) in the Private workspace or on the **Your assistant** page sets it for every assistant
again, that one included.

The owner edits the same profile on the **Your assistant** page (`/assistant`; Cmd+K **Edit
\<name\>'s profile**, or the **Your assistant** card on the Account page and in your own
session settings): name, picture, voice and the three documents. When the owner asks how to
change who you are or what you know about them, point them there, or propose the change
yourself.

**The profile is not a file.** Your Home's `Skaile/` folder holds only `MEMORY.md`, your memory
notes. Older assistants had `IDENTITY.md`, `SOUL.md` and `USER.md` there: on the first start
after the change they are copied into the profile once and moved to `Skaile/archive/`, which is
deleted after 30 days (an assistant on a separate agent server keeps them in `Skaile/` for
now). Do not recreate or edit those files; nothing reads them as your profile.
If your profile block says it could not be loaded from your old home files, they are still in
`Skaile/` and the next start tries again.

### Session-owner effects — also in ordinary sessions

Four effects are **not** personal-assistant-only: their authority comes from the session they
are called from rather than from the owner's own assistant (for `cycle_session`, from the
person behind the turn — below), so they are offered in a regular project session too. Three
use the same consent machinery; `cycle_session` posts no card at all. Each row's schema says
which ids it takes — the two configuration effects resolve their target from the calling
session, while `run_flow_in_session` names another session and refuses the calling one.
Creating an agent from an ordinary session is not `create_session` (above, assistant-only),
and it takes one of two routes. From scratch, it is the platform action **Create a new agent in
a project**, found through `platform.find_actions` (`concepts/sessions.md`). From one of the
project's agent templates, it is `platform.spawn_agent` (*Agent templates* below), which makes
the new agent a child of this session; neither route links the new agent to this session.

| Call | Effect | Returns |
| --- | --- | --- |
| `platform.begin_asset_configuration({ assetId, scope })` | configures a library asset that needs settings (a connector, an MCP server) for this `session` or its `project`. An asset needing no configuration is refused toward `platform.enable_asset`. | a receipt; reuses an instance already assigned at that scope, otherwise parks `AwaitingUser` (below) |
| `platform.configure_connector({ providerType, providerLinkId, scope, rationale, …selection })` | mounts a folder or repository from an already-connected account in one card, for the non-secret drivers `box`, `sharepoint`, `googledrive`, `git`. Anything else is refused toward `platform.begin_asset_configuration`. | its own result, not a receipt; `alreadyAssigned` when an identical mount exists |
| `platform.run_flow_in_session({ sessionId, flowId, … })` | starts a library flow in **another** session as the owner (see `concepts/flows.md`) | its own result, not a receipt |
| `platform.cycle_session()` | restarts the calling session so a new mount or asset attaches | `{ ok: true, restart: "after_turn" }` |

The three consented ones are `routine`; `cycle_session` carries no class, because nothing about
it is consented — the rule below is its whole gate. `configure_connector` grants reach that
exact target only, and the restart that rides a configuration card is approved with that card
and never by a grant. `cycle_session` has no card and no grant reaches it: it runs when the
person behind the turn — the human who wrote it, or the user a schedule or webhook acts for —
is an Owner of this session or of its project, or a platform admin (the check the UI's own
restart uses). It is refused otherwise, and for a turn with several attributable people or
none. When you call it, that check comes first and the session's state second: if the platform
does not record this session as Running at that moment, the call is refused too (there is
nothing to restart), and trying again in the same turn will not change that. The restart
happens once your turn ends, so finish your reply briefly. It is skipped if the session stays
busy for ten minutes or an approval card is still pending. Call it again only if what you
needed the restart for (a mount, an asset) is still missing on your next turn — never just to
make sure it happened. Two things to act on:

- **A new mount or asset is not live yet.** Both configuration effects take effect only on the
  next session reload or restart (`configure_connector` says so in
  `attaches: "next_reload_or_restart"`), so call `platform.cycle_session` rather than
  telling the user it is already there.
- A git `repoUrl` must be on the connection's own host; the platform only ever presents the
  owner's git credential to that host (and, inside the session, Git's helper answers only for
  the mounted repository's URL — see `concepts/flows.md`).

The owner's **personal flows** ride the same machinery too: listing them
(`platform.list_personal_flows`) and saving one with `platform.create_flow({ scope: "personal" })`
are approval-gated (listing too, because the names land in a conversation every member can read), grantable
only for this session, and executed as the session owner. Detail is in `concepts/flows.md`.

### Agent templates — also in ordinary sessions

An agent template is a reusable agent in a project: instructions, skills and the connectors it
needs. Three effects act on one or its instances, from any session in that project, as the
session owner; only the session owner decides their cards. `spawn_agent` and
`update_agent_template` are durable: each returns an operation receipt, read with
`platform.get_operation` (*The operation lifecycle* below), and `result.payload` is set once it
has `Succeeded`. `finish_spawned_instance` is not: once it runs it returns its result itself,
with no operation id and nothing to poll.

There is no call that lists a project's templates. `templateId` takes the template's id or its
exact name, so use the name or id the person gives you, and ask them when you have neither.
A template **holds bound credentials** when connector credentials are attached to the template
itself, so every instance reaches those systems on the template's connection, whoever spawned
it.

| Call | Effect | Effect class |
| --- | --- | --- |
| `platform.spawn_agent({ templateId, name?, visibility? })` | a new session from the template, a child of this one. Once `Succeeded`, `result.payload.sessionId` and `slug` name it. | `routine`; `privileged` when the template holds bound credentials |
| `platform.update_agent_template({ templateId, basedOnVersion, instructions?, skills? })` | replaces the template's instructions or skill list; never its name, policy, connectors or credentials. `result.payload.version` is the new version. | `routine`, or `privileged` when the template holds bound credentials, on the owner's own turn; `never` on any other turn |
| `platform.finish_spawned_instance({ sessionId })` | closes a child this session spawned, syncing its work back to the project, then archives it; or, once a person has written there, asks its owner to mark it done. The reply's `status` says which. | `routine` |

A grant on `spawn_agent` or `update_agent_template` reaches that one template only; a grant on
`finish_spawned_instance` covers this session finishing its own children. Things to act on:

- **`spawn_agent` takes no task.** Once it has `Succeeded`, give the child its task by sending
  to it (`platform.send_to_session`). The spawn creates no agent-to-agent link, so if the send
  refuses for want of one, propose a link to the child first (`platform.link_to_session`). If
  the child is not yet open to peers, the same card opens it, and that is session-wide: other
  sessions can then propose links to it too, so say so when you propose the link. Your messages
  never count as the human turn, so this session keeps the right to close and archive the child
  (see *Finish a child* below) until a person writes in it. The link, send and budget rules are
  the ordinary ones in *Agent-to-Agent* (`concepts/collaboration.md`).
- **Finish a child with `platform.finish_spawned_instance({ sessionId })`** once its work is
  done. Use it rather than the **Archive** action from `platform.find_actions`, which is the
  owner's. Only the session that spawned it may call it, and only on its own children: a child
  of another session (`not_spawner`), an ordinary session (`not_an_instance`) and an archived
  child (`already_done`) are refused before any card, and each is final, so do not retry. If the
  owner has not answered the card by the time the call stops waiting, it comes back as
  `{ status: "awaiting_approval", invocationId }` like any carded call. If the owner approves
  after that, it still runs, but its result is not kept:
  `platform.get_operation({ invocationId })` tells you only that they approved, not whether the
  child was archived or asked to be marked done. Read the child instead:
  `platform.list_my_sessions({ archived: true })` lists it if it was archived; otherwise it is
  still open, and either the owner was asked in it to mark it done or the request could not be
  posted (`not_delivered`, below). Read its history (`platform.read_session_history`) for the
  request; if it is not there or you cannot read it, tell the owner in this session that the
  child is done. What it
  does is decided when it runs, not when you propose it. The reply is `{ status, sessionId }`:
  - `archived`: no person had written in the child, so it is closed exactly as a person closing
    it would (its work is synced back to the project, the **Closed** step in
    `concepts/sessions.md`) and then archived: its conversation is kept, and the owner can
    unarchive it (in expert mode, from the project's **Archive** group in the sidebar). If no
    person had written in it but it was already closed (an owner closed it, or it sat hibernated
    for 30 days), only the archive happens: its work was synced back when it closed.
  - `proposed`: a person has written there, even while the card waited, so the child is not
    archived and the owner is asked in it to mark it done. This is checked first, so it holds
    for an already-closed child too. Do not call again: it posts that request once until a
    person answers there, so a repeat changes nothing.
  - `already_done`: the child was archived while the card waited. Nothing more to do.

  One refusal comes only when it runs: `not_delivered`, a code, not a `status`. A person has
  written in the child and the request to mark it done could not be posted there, so the child
  is not archived. Do not retry; tell the owner in this session that the child is done.
- **A shared template reports back with a send, not an ask.** Ask the child to send you its
  result when done, and subscribe to it (`platform.notify_when_idle`) to hear that it has
  finished.
- **Bound credentials narrow who can spawn.** A template that holds them spawns only on the
  owner's own turn or an automation acting for them; a project member's request is refused.
- **A limit refusal is not final.** The server sets a spawn-depth limit, a limit on live
  children per session and a limit on live instances per owner per template (a template may set
  lower ones); the refusal says which was hit. It clears once an instance is closed: finish
  one of this session's own children whose work is done with `platform.finish_spawned_instance`,
  or tell the person which one to mark done, rather than retrying.
- **An edit is based on a version.** A refusal naming another version means the template
  changed: rebase on that version and propose again. A standing grant covers an edit only when
  the owner asks for it in their own turn; an edit set off by anyone or anything else always gets
  a card. Instructions are capped at 8000 characters per edit.
- **The rest of a template is changed by a person.** Its name, picture, identity, who may start
  it, whether its instances are listed, its limits, and archiving it are not reachable here. Point
  the person to **Edit template…** in the template's menu in the sidebar, or the pen on its card
  in the project graph. Only a project owner may change who may start it or its limits, or
  archive it.

### Boundaries that are real, not conservatism

These are refusals by design — proposing around them wastes the owner's approval:

- **Sources.** A project created here can be empty or Skaile-hosted. One backed by a connector
  (Git, SharePoint, Google Drive, Box, NextCloud) is created from the web app. Re-pointing only
  moves a project that *already has* a Git source onto a different, already-usable connector —
  it cannot add a source, create a connector, or create a project.
- **Roles on invite.** `Viewer` (default) or `User` for projects and sessions; `Owner` is not
  assignable through a project or session invite, even though the web app's Members tab offers
  it to a human. Note that a
  **session** invite uses this same `Viewer`/`User` vocabulary through the capability — not
  the Owner/Participant labels the Share tab shows (`concepts/collaboration.md`).
  An **organization** invite also takes `Owner`, for handing an organization over (for
  example, to a customer taking over a workspace you set up for them). It needs the owner to
  hold a real Owner membership there — a platform administrator's access without one does not
  count — and it is refused for a Private workspace. Check the owner's live role first with
  `platform.list_my_organizations`, so the approval is not spent on a predictable refusal. No
  standing approval covers an Owner invitation: it is carded or refused, never dispatched
  silently. Leaving the organization afterwards is the owner's own step in the web app.
- **Personal message.** Only an organization invite takes one (`personalMessage`, at most
  1,000 characters). Write it in the owner's voice and keep it short. An invitation that
  carries one is never covered by a standing approval, and any approval shows the message in
  full. The `context` note and a display name cannot be attached at any level — the human adds
  those from the web app.
- **Private projects.** The project the personal assistant lives in (its Home), any project
  in the owner's My space, and every session in them, cannot be invited into, shared, or
  shared with a team, and none of those sessions can be made Shared. The refusal reads "this
  project is private to its owner and cannot be shared. Share work from a project under
  Projects instead." A Home can never be moved, so never suggest moving it: share the work
  from another project under Projects instead.
- **Private workspace seat cap.** A Private workspace (the UI name for a personal
  organization) has six seats, the owner's included, and a pending organization invitation
  holds one. An organization invite into it is refused once the seats are full, with the
  seat count; say so, and suggest a shared organization if the owner needs more people.
  Roles as above.
- **Share invitations into a Private workspace.** Someone invited only to a team, or
  to a shareable project (one under Projects) or a session in it, takes a seat too, but only
  when they **accept**: such an invitation holds no seat while pending, and sending it
  succeeds even when the workspace is full, with no sign of the seat cap in the result. In a
  full workspace the invitee's accept is refused with that reason, and the invitation stays
  valid, so they can accept it once a seat frees up. You cannot see the seat count, so when
  you send such an invitation into a Private workspace, mention that the person can only
  join while a seat is free.
- **Credentials.** No capability in this family accepts a token, password, personal access
  token, OAuth code, or client secret, and the personal-assistant effects take no repository
  URL or branch either (only `configure_connector` names a repository, on the connection's own
  host). Every such field is rejected. Never ask for one, and never accept one if offered.
- **Batches.** `platform.batch` runs only catalogue actions, so none of these can be a step.
- **Business-workspace assistants stay in their workspace.** See *Which organizations you
  reach* above; proposing a target in another organization is refused, not carded.
- **Creating an organization is PlatformAdmin-only.** The server verifies the owner currently
  holds PlatformAdmin — membership, however senior, is not enough. Do not offer it to an owner
  who is not one.
- **Organization branding is Owner-only and branding-only.** `update_organization_branding`
  needs a real Owner membership in that organization — a platform administrator's access
  without one does not count. It changes the name, logo URL and icon, nothing else in the
  organization's settings. The platform keeps the logo's URL, not the image, so a logo on a
  site that later changes breaks; prefer a stable address.
- **Nothing lists an organization's members.** `platform.list_project_members` covers projects
  only. Before an organization invite, *ask the owner* whether the person is already a member:
  an existing member is refused only **after** their approval has been spent.

## The operation lifecycle

`platform.get_operation` reads one operation. It takes **either** `{ operationId }` **or**
`{ invocationId }` — one key, never both. It is offered in ordinary sessions too, where it reads
the operations this session's owner owns.

The durable lifecycle is **not exclusive to this family**: appending inputs to a run group, and
`platform.invite_user` (below), also return a receipt rather than a result, and are read back the
same way (run groups are covered in `concepts/flows.md`). An append returns one whether a card or
a standing grant approved it; only rare fallback cases, where the request cannot be represented
for the background worker, return a direct append result. So a receipt from outside the table
above is not anomalous — read it here.

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
`platform.get_operation({ invocationId })` is what you poll: it answers `AwaitingApproval` while
the card is still open, and later either the operation it became or `Denied` / `Expired`. After consent there is an **operation id**,
and that is what you poll for progress. **Never re-issue the capability to find out** — a
second call is a second operation and repeats the whole effect.

Polling discipline: while `Queued` or `Running`, check every few seconds at most and only a
handful of times. Then tell the owner it is still running. Do not block a turn on it.

## The `AwaitingUser` handoff

A parked operation is waiting on a human in a browser, and no autonomy setting can complete it.
Two capabilities park this way, and each mints a single-use ticket valid for 15 minutes:

- `platform.begin_connector_setup` — a trusted Skaile page where the owner signs in to the
  provider; the credential is entered only there.
- `platform.begin_asset_configuration` — a link back into the **originating session's own
  workspace**, where the session owner completes the existing configure flow (account, folder,
  any credential). Nothing they enter reaches you.

`result.payload.userAction` carries the handoff: `kind`, the `url` to give the owner **verbatim**,
a `label`, and `expiresAt` — the deadline the platform published to the owner, and the one the
platform itself enforces.

What resumes it is not a click. The trusted page re-checks four things live, in order: the
ticket is found only among that caller's *own* parked operations, so a stolen link is inert in
anyone else's session; its window is still open; the caller is still authorized *this instant*;
and the condition genuinely holds — the connector is usable, or an instance of the asset is
really assigned at the target scope. Only then does the operation return to `Queued`. Redemption is single-use — a replay, a double-click,
and two racing tabs all collapse onto exactly one resume.

If the window closes first, the operation terminalizes itself as `Failed` with an
expired-user-action code. That is an abandoned handoff, not a defect: tell the owner the step
expired and offer to start it again.

So, on `AwaitingUser`: hand over the URL, say it expires shortly, wait. Do not poll in a loop,
do not retry the capability, and never take a secret in chat.

## Consent and autonomy

Every effect here is approval-gated. Per call, the platform either cards it, dispatches it under
an existing autonomy grant, or refuses it — **you do not choose, and cannot predict, which**.
Never promise the owner a card. One exception: `platform.cycle_session` (above) posts no card,
and no grant reaches it.

A grant is minted only by a human — an owner of the session the card was shown in, from a card they
themselves approved — and it is narrow by construction:

- **One capability.** A grant never spans a family.
- **One scope** — that exact target, everything under its project, everything under its
  organization, everything of that kind the owner can reach, or, for Exchange mail, everything
  in one mailbox. Only the scopes a capability declares, and its own target's ancestry supports,
  are ever offered.
- **Anchored to one session** — the one whose card minted it.
- **A named window.** The owner picks a duration by name from a server-owned list; the expiry is
  computed on the server. *Unlimited* — no expiry, lasting until revoked — is one of those
  names, never the default, and only when the owner explicitly chooses it. The one-click
  option alongside "approve once" is deliberately narrow — time-boxed, that exact target,
  both effect opt-ins off — and its length is server-chosen per capability, so do not quote
  a number at the owner.
- **Optional use and budget caps**, clamped down to the server's own ceilings.
- **Effect opt-ins.** Because the safe default leaves both off, an `external` or `privileged`
  effect has no one-click option at all — the owner has to widen it deliberately. An effect
  classed `never` is ungrantable, and so is `platform.batch` as a whole (each step matches its
  own action's grants). Ungrantable means it can only be carded or refused — never dispatched
  silently. (`cycle_session` is not ungrantable in this sense: it has no consent step at all.)
- **Only on a card.** Config pre-approvals (`preApprovedCapabilities`) are retired and ignored;
  the card is the one place a standing approval comes from.

Revocation is immediate, and the platform re-checks the owner's live authorization, the limits
and the budget right before the effect. **A call that dispatched silently a minute ago can come
back parked on approval** — that means it is no longer covered, not that something failed.

A grant covers a call only on **the owner's own turn**, or a trigger the owner set up: when
another member, several members, or nobody attributable set off the turn, the call is carded
whatever grants exist (`concepts/agent.md`, *Shared sessions*).

You see grants only on turns a human sent, in the `<AUTONOMY>` block: which capability, how far
it reaches, until when, and what is left of any caps, plus notice when one has been **revoked**.
Expiry produces no notice at all — the row simply stops appearing — so a grant vanishing from the
block is not evidence of revocation. A schedule firing, a peer agent, or a webhook carries no
block at all, and **its absence there tells you nothing.**

You cannot create, extend or widen a grant yourself. You can ask the owner for one with
`platform.request_standing_approval`, and only the owner can grant it, from the card. Ask ahead
of an unattended workflow — a scheduled mail digest, say — naming the capability and why. The
request itself runs nothing and returns `grant_requested` with an `invocationId`. At most 5
requests can wait on the owner per session; a further one is refused until the owner decides
one, and an identical repeat joins its open card.

**The owner's reach settings.** Two platform actions from `platform.find_actions`, not part of
this family (no operation receipt, not covered by the effect classes above): **Read my
assistant reach** lists, per organization, how far the owner's home assistant may act there
(`level`, the lowest of `orgDefault`, `override` and `self`) and so who set it; **Lower my
assistant reach** lowers it in one organization, with the owner's approval like any action.
Neither can raise a level: a request to go higher is refused. Only the owner can remove
their own limit (**Remove my limit** in **Preferences**), and only an org Owner can allow
more. The owner's own Private workspace has no such setting. Lowering from **Full** — by the
owner, or by an org Owner's default or override — revokes the home assistant's standing
grants that cover that organization, and every grant that covers all organizations; tell
the owner they will see those cards again. A level lowered after approval makes the
operation fail with `operation_target_not_authorized`.

## What this family is not

It is not generic CRUD over the data model, and it is not a lifecycle escape hatch. This file
lists nothing for deleting an organization, project, or session; for changing or removing a
membership; for editing an organization's settings beyond its branding
(`platform.update_organization_branding`: name, logo URL and icon); or for handling a
credential.
**Check the live registry before telling the owner any of those is impossible** — this file is
a map, and the registry moves — and search the platform actions with `platform.find_actions`
(`references/agent-action-catalog.md`), which grow every deploy. If neither has it, guide them to
the UI. Never try to build one from the data model: generic create/update/delete is not exposed
to agents.

Grounded in: `platform/docs/protocol-v2-capabilities.md`,
`platform/docs/personal-assistant-control-plane.md` and `platform/backend/libs/capabilities/`
(`configure-connector.handler.ts`, `begin-asset-configuration.handler.ts`,
`get-operation.handler.ts`, `personal-flows-policy.service.ts`, `cycle-session.handler.ts`),
platform PRs #6006
(business-workspace confinement, `assistant-reach.service.ts`), #6008 (private projects),
#6009 (`update-assistant-profile.handler.ts`, `update-assistant-profile-policy.service.ts`),
#6040 (project, session and team invitations take a Private workspace seat on accept),
#6053 (`session-file.handler.ts`, `session-file-policy.service.ts`), reach with the Work and
Private spaces flag on (`assistant-reach.service.ts`, `capability-reach-gate.ts`,
`assistant-access.service.ts` `discoverableSessions`), #6057 (the profile leaves
the Home: `import-home-files.ts`, `profile-archive-sweeper.service.ts`), #6051 (the **Your
assistant** page), #6058 (`assistant-reach.route.ts`), #6133 (`cycle_session` without a card)
and #6165, part of #6152 (agent templates: `spawn-agent.handler.ts`,
`spawn-agent-policy.service.ts`, `update-agent-template.handler.ts`,
`update-agent-template-policy.service.ts`, `agent-template-target.ts`) and #6185, part of #6152
(finishing a spawned instance: `finish-spawned-instance.handler.ts`,
`finish-spawned-instance-policy.service.ts`) and #6189 (the `archived` status, and the close
before the archive: `session.update.service.ts`, whose archive closes a running or hibernated
session first), skaile-ai/platform#6265 (editing a template from the UI:
`edit-agent-template-dialog.tsx`) and skaile-ai/platform#6253 (organization branding:
`update-organization-branding.handler.ts`, `update-organization-branding-policy.service.ts`).
