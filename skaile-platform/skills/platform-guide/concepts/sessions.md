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
- **Hibernated** — after an idle timeout (platform default ~15 min) the container is
  stopped to save resources. Files persist on disk; conversation history persists in the
  database. Nothing is lost. Recent file edits count as activity, so the platform will not
  hibernate while a user or the agent is actively editing.
- **Waking** — navigating to a hibernated session spins a fresh container back up, replays
  the conversation history to the agent, and rehydrates any running flow. The first turn
  after wake is slightly slower (no prompt cache).
- **Closed** — an explicit user action. Changes are **synced back to the project's main
  data** (git merge for git projects; driver sync-back for SharePoint/S3/WebDAV/local),
  then the workspace is cleaned up. Closing is the "I'm done, fold this work back in" step.

What to tell users:
- "Sleeping/hibernated" is normal and safe — just reopen the session, it wakes
  automatically.
- "Closing" is not "pausing" — it finalizes and syncs the work back. To pause, just leave
  it; it hibernates on its own.

## Multiple sessions per project

A project can have many sessions running at once, each an isolated copy. This is how
collaborators work on the same project data without stepping on each other. The **main
session** is the canonical one; other sessions branch off it and merge back on close.

## Scoped sessions

A **scoped session** mounts only a **subfolder** of the project data instead of all of it
— for sharing a slice of a large data source with a collaborator without exposing the
rest.

- Created by a **Project Owner** from a folder in the resource explorer ("Share in new
  session..."), choosing the collaborator's role and who to invite.
- The agent inside sees a normal workspace rooted at that subfolder — it does not know it
  is a subset.
- Supported for **SharePoint, S3, WebDAV, and LocalFolder** sources. **Not** supported for
  **Git** (needs the full `.git`) or **Empty** projects — the action is hidden for those.
- Scoped sessions can be nested (a scoped session can create a further-scoped child).
- Visible only to users who are members of that session.

## Forking / reopening / discarding

- **Fork / reopen / discard** a session requires **Org Owner**.
- Renaming a session or project is a label-only change — it does not move the underlying
  workspace or git branch (those are frozen at creation). An old bookmarked URL after a
  rename shows an "address changed" screen prompting the user to reopen from the explorer.

Source of truth: `platform/docs/session-lifecycle.md`, `platform/docs/scoped-sessions.md`.
