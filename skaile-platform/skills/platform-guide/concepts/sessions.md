# Sessions, Lifecycle & Scoped Sessions

A session is an isolated workspace where the user and the agent collaborate. Understand
its lifecycle to explain why a session is "sleeping", why opening it takes a moment, and
what "closing" actually does.

## Lifecycle

```
PROVISIONING -> RUNNING -> HIBERNATING -> HIBERNATED -> WAKING -> RUNNING
                  |                                                  |
                  | close                   (failure on any step)   v
                  v                                                ERROR
               CLOSING -> CLOSED (changes synced to main)
```

- **Running** — live container, agent ready, user can chat.
- **Hibernated** — after an idle timeout (workspace default 30 minutes) the container is
  stopped to save resources. Files persist on disk; conversation history persists in the
  database. Nothing is lost. Recent file edits count as activity, and a session in the
  middle of an agent turn is not hibernated (a turn stalled for 2 hours is). A session
  owner can override the timeout per session in session settings, **Config** tab >
  **Idle timeout** (5–1440 minutes; blank uses the workspace default).
- **Waking** — a hibernated session shows its stored conversation immediately, marked
  **Suspended** next to the title, and the composer stays usable. Sending a message wakes
  it (typing already starts the wake in the background; a message sent during the wake
  waits for it), or the user clicks **Resume session**. Waking starts a fresh container,
  restores the conversation to the agent, and rehydrates any running flow. The first turn
  after wake is slightly slower (no prompt cache).
- **Closed** — an explicit user action, or the spawning agent finishing a child it spawned
  (`platform.finish_spawned_instance`, which also archives it). Changes are **synced back to
  the project's main data** (git merge for git projects; driver-specific sync-back for other
  sources), then the workspace is cleaned up. Closing is the "I'm done, fold this work back
  in" step. A non-main session left hibernated for 30 days is closed automatically; the main
  session never is.

After a gap of an hour or more, the agent is told how long it has been since the previous
turn. Treat anything time-sensitive from before such a gap as possibly out of date.

What to tell users:
- "Sleeping/hibernated"/**Suspended** is normal and safe — just send a message or click
  **Resume session**; it wakes automatically.
- "Closing" is not "pausing" — it finalizes and syncs the work back. To pause, just leave
  it; it hibernates on its own.

## Multiple sessions per project

A project can have many sessions running at once, each an isolated copy. This is how
collaborators work on the same project data without stepping on each other. The **main
session** is the canonical one; other sessions branch off it and merge back on close.
In the UI a session is presented as an agent: creating one is **New agent**. It needs a
project role of **User** or **Owner**, and the project must not be archived.

An agent can create one too, from any session and not only the home assistant, in one of two
ways. From one of the project's **agent templates**, it calls `platform.spawn_agent`: the new
agent becomes a child of the calling session, runs as the session owner, and gets its task by a
send afterwards, after an agent-to-agent link if there is none yet (*Agent templates* in
`references/control-plane-capabilities.md`). When its work is done, the spawning session ends it
with `platform.finish_spawned_instance`, which closes it (with the usual sync-back) and archives
it or, once a person has written there, asks the owner to mark it done (same section). From
scratch, it uses the platform action **Create a new agent in a project** (find it with
`platform.find_actions`). That action takes the project id — listing the current project's
sessions returns it as `projectId` — a name, and optionally a one-line description, instructions
(the dialog's **Prompt**), an identity, and **Shared** (the default) or **Private**. In a
private project (the assistant's Home or a My space project) the agent is always Private: left
out, it is made Private; an explicit Shared is refused. Like any action it runs as the session
owner and needs the owner's approval, unless, on the owner's own turn, a standing grant already
covers that action on that project; the owner, and anyone else who asked on this turn, needs the
same project role the **New agent** dialog requires. The card cuts long instructions short; the
full text is in the new agent's **Edit agent** dialog. It does not link the new agent to the
calling session — propose an agent-to-agent link separately (see *Agent-to-Agent* in
`concepts/collaboration.md`).

An agent can also start a copy of itself, to split work across parallel sessions, with
`platform.spawn_subagent`: a clone of its own setup, or a narrowed one with its own
instructions and fewer skills, connectors or MCP servers, never more than it has. The copy is a
child of the calling session, runs as the session owner, and is finished the same way
(*Subagents* in `references/control-plane-capabilities.md`).

## Scoped sessions

A **scoped session** mounts only a **subfolder** of the project's workspace instead of
all of it — for bringing someone in on one folder without exposing the rest.

- Created by a project **Owner** or **User** from a folder of the workspace mount in the
  **Workspace** panel (the folder **...** menu, or Cmd+K) > **Share in new session...**.
  The dialog asks for a session name, then opens the regular add-member dialog, where
  people are added with the usual session roles (Owner / User / Viewer).
- The session is created as a shared session, so project members who already see shared
  sessions see it too. In a private project (the assistant's Home or a My space project)
  it is created Private instead, like every session there.
- The agent inside sees a normal workspace rooted at that subfolder. Its tools and
  library connectors run on the **sharer's** (the scoped session owner's) connected accounts —
  the same session-owner rule as in `concepts/integrations.md`. Other file mounts are dropped; tool connectors (mail,
  databases, ...) are kept. The dialog warns about library file mounts the folder limit
  does not cover. The mounts cannot be widened afterwards.
- Supported for **On Skaile, SharePoint, Google Drive, WebDAV/NextCloud, Box, and Local
  Folder** workspaces. **Not** supported for **Git** (branches isolate instead), **S3**,
  or **Empty** workspaces, nor on broker deployments — the action is shown disabled with
  the reason.
- Scoped sessions can be nested (sharing a folder from inside a scoped session); a child
  is never wider than its parent.

## Forking / reopening / discarding

- **Fork / reopen / discard** a session requires **Org Owner**. Expert Mode also offers
  **Cycle session** (restart the container without closing), which needs only an Owner of the
  session or of its project (or a platform admin); Expert Mode is a display setting, not a
  permission.
- Renaming a session or project is a label-only change — it does not move the underlying
  workspace or git branch (those are frozen at creation). An old bookmarked URL after a
  rename shows an "address changed" screen prompting the user to reopen from the explorer.

Source of truth: `platform/docs/session-lifecycle.md`, `platform/docs/scoped-sessions.md`,
`platform/backend/libs/session/src/idle-detection.service.ts` (timeout, stalled-turn and
30-day auto-close defaults), platform PRs #5013/#5090/#5294 (cached-first wake), #5334
(time since last turn), #6008 (sessions in a private project are Private),
`isProjectSessionCreateRole` (who can create sessions), platform #6065 (agents create agents),
platform #6165, part of #6152 (spawning from agent templates: `spawn-agent.handler.ts`,
`spawn-agent-policy.service.ts`), platform #6185 and #6189, part of #6152 (finishing a
spawned instance: `finish-spawned-instance.handler.ts`), platform #6245 (subagents:
`spawn-subagent.handler.ts`, `subagent-payload.ts`).
