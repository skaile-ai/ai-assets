# Sessions, Lifecycle & Scoped Sessions

A session is an isolated workspace where the user and the agent collaborate. Understand
its lifecycle to explain why a session is "sleeping", why opening it takes a moment, and
what "closing" actually does.

## Lifecycle

```
PROVISIONING -> RUNNING -> HIBERNATING -> HIBERNATED -> WAKING -> RUNNING
                  |                                                  |
                  | explicit close          (failure on any step)   v
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
- **Closed** — an explicit user action. Changes are **synced back to the project's main
  data** (git merge for git projects; driver-specific sync-back for other sources), then
  the workspace is cleaned up. Closing is the "I'm done, fold this work back in" step.
  A non-main session left hibernated for 30 days is closed automatically; the main
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

## Scoped sessions

A **scoped session** mounts only a **subfolder** of the project's workspace instead of
all of it — for bringing someone in on one folder without exposing the rest.

- Created by a project **Owner** or **User** from a folder of the workspace mount in the
  **Workspace** panel (the folder **...** menu, or Cmd+K) > **Share in new session...**.
  The dialog asks for a session name, then opens the regular add-member dialog, where
  people are added with the usual session roles (Owner / User / Viewer).
- The session is created as a shared session, so project members who already see shared
  sessions see it too.
- The agent inside sees a normal workspace rooted at that subfolder. It runs on the
  **sharer's** connected accounts. Other file mounts are dropped; tool connectors (mail,
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
  **Cycle session** (restart the container without closing).
- Renaming a session or project is a label-only change — it does not move the underlying
  workspace or git branch (those are frozen at creation). An old bookmarked URL after a
  rename shows an "address changed" screen prompting the user to reopen from the explorer.

Source of truth: `platform/docs/session-lifecycle.md`, `platform/docs/scoped-sessions.md`,
`platform/backend/libs/session/src/idle-detection.service.ts` (timeout, stalled-turn and
30-day auto-close defaults), platform PRs #5013/#5090/#5294 (cached-first wake), #5334
(time since last turn), `isProjectSessionCreateRole` (who can create sessions).
