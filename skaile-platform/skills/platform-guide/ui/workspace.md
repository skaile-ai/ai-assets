# UI: The Workspace (Where Work Happens)

The workspace is the main session surface — chat with the agent, browse files, preview
apps. Path: `/<org>/projects/<project>/<session>`. Labels in **bold** are real UI strings.

## Panes (desktop)

Chat sits on the **left** by default, the other panel on the right. The toolbar's centre
zone holds the **panel switcher**, a capsule of toggles (**Show chat** / **Hide chat**,
**Show workspace** / **Hide workspace**, **Preview** / **Hide preview**) with a round swap
button seated in a notch between the chat toggle and the rest, labelled **Move chat to the
right** / **Move chat to the left**, that flips the two sides.
The chat side persists per user. Which panes are visible is per session tab and is not
kept after the tab closes. The swap is also a Cmd+K action. Possible combinations:
chat-only, workspace-only, preview-only, chat+workspace, chat+preview.

Panels are separate rounded boxes on the page ground, each with its own header (icon,
title, actions) — not flush-joined panes.

1. **Chat panel** — the conversation with the agent.
   - Scrollable message history with live typing indicators.
   - **Composer** at the bottom: type a message, attach files (drag-and-drop or the
     attachment tray), and use **@** for mentions and **/** for slash commands (a prefix
     picker appears at the cursor). Attachments show as a tray of thumbnails/icons before
     sending.
   - Agent tool/capability invocations and their results render inline; approval-gated
     actions show an approval card the user clicks to allow or reject.
   - When the agent asks a question, the card shows its options as chips plus a dashed
     **Other answer…** chip (**Type an answer…** when there are no options) that opens a
     free-text field.
   - **Escape** while the agent is working stops it (the same Escape closes the tab once
     the agent is idle).
   - **A sleeping (hibernated) session** opens straight to its stored conversation, with a
     **Suspended** badge by the session title — no waiting to read. Typing starts waking
     it in the background; while it wakes the composer stays usable but sending is held,
     with a note such as *Session is waking — sending resumes in a moment* (after about
     five minutes: *Resuming is taking longer than expected*). Sending also wakes it.

2. **Workspace panel** — the project data plus the mounted sources.
   - The panel title is a **resource switcher**: a dropdown naming the active resource
     (the project data, a mount, ...). With only one switchable resource it renders as a
     plain title.
   - Browse/open files (pluggable viewers per file type; iPhone HEIC/HEIF photos open in
     the image viewer, `.jsonl` files are highlighted per line). The header carries a
     search field (**Search files by name…**); beside it **Upload files** (the **+**) and
     **New file** (creates at the root).
   - A three-state layout button cycles **Explorer and preview** → **Explorer only** →
     **Preview only**; the choice persists per user.
   - Drag-and-drop upload (with conflict resolution: overwrite / rename / skip).
   - An open file's header offers **View source / raw** (it becomes **Show rendered
     view**), a word-wrap toggle (**Turn word wrap on** / **off**, for code and plain
     text), **Save**, **Edit**, **Edit in \<app\>** / **Open on Desktop**,
     **Share file preview**, **Copy source to clipboard**, **Download**,
     **Copy link to file**, **Open in new tab** and **Toggle fullscreen**. HTML, Markdown
     and diagram files also get a **Preview** / **Code** / **Split** switch.
   - Row menu (the **...** on hover): **Copy reference**, **Copy contents** (files; up to
     10,000 characters), **New file here** and **Upload file(s) here** (folders),
     **Share in new session...** (folders; creates a scoped session — hidden for
     git/empty sources), **Rename**, **Properties** (size; for a folder, the files and
     folders directly inside it) and **Delete**. There is no "new folder" action.
   - The empty state shows a dashed drop area for first uploads.
   - While the session sleeps the panel shows **Session suspended** with **Resume
     session** instead of the files (sending a message wakes it too).

3. **Preview pane** — a live preview of a runnable app. It is a main pane, not one of the
   right-side panels: toggle it from the **panel switcher** in the toolbar's centre zone
   (tooltip **Preview**, **Hide preview** once open, **No Preview available** when the
   workspace declares no app).
   - With nothing running, the pane lists every previewable app to **Start**. Once one
     runs, the pane header names it as a dropdown that switches apps — there is no tab
     strip. Stopping the app returns the pane to the list.
   - The open preview is remembered per user per project. Opening an app from the
     sidebar's **Apps** (or Cmd+K) shows that session's preview alone, with the app
     selected.
   - **Restart** / **Stop** controls; a device-mode toggle (desktop/tablet/mobile).
   - A **Preview Logs** drawer streams console output, filterable by role/level.
   - Empty state explains why a workspace is not previewable (preview contract not met).
   - The separate **Preview** icon in the toolbar's right zone is a different surface: it
     shows capability renders when the main layout hides the workspace.

## Side panels (the toolbar icons)

There is **no permanent right sidebar**. The header toolbar's right zone shows **one
icon per panel**; clicking an icon opens that panel on the right of the workspace, and
closing it (the X, or clicking the icon again) leaves nothing behind on the right edge.
When guiding a user, name the icon: "the row of icons at the top right of the
workspace". Panels (in icon order):

| Panel          | Purpose |
| -------------- | ------- |
| **Assistant**  | A mini chat with the user's personal AI assistant; **Open full session** opens its full session. Shown only when the viewer has a personal assistant and is not already in it. |
| **Preview**    | Capability-render previews (fallback when the main layout hides the workspace). |
| **AI Assets**  | Skills, MCP servers, agents and contracts for this session — the place to enable an asset, for **This session** or the **Whole project**. Connectors are *not* here; they have their own panel. |
| **Connectors** | The session's data-source mounts: shows what is mounted and on whose account, and offers **Connect Box / SharePoint / Google Drive / NextCloud / Git** flows — pick the account/connection and folder, scope **This session** or **Whole project**. New mounts attach on the next reload/restart (the panel prompts). If the user's sign-in has lapsed, the folder picker shows **Reconnect** (or **Connect account**). It also holds **Exchange project access**: the project Owner's switch for the project's mailbox, and per-mailbox enable/disable (shared mailboxes are added in **My Connections** first). This is the answer to "I connected my \<provider\> in My Connections — now what?". |
| **Share**      | Sharing pane: visibility toggle, team access, project/session members + roles, invites, and public preview-share links. |
| **Summary**    | Session snapshot and resume strategy. |
| **Flow**       | Flow-run view when the session runs a flow; with none running, **Run a flow…** starts one in this session. |
| **System**     | Restart / kill / compact the session, view protocol info, and read live logs. (Formerly "Debug".) |
| **Config**     | Skaile config editor for the session (full or mounts-only). |
| **Report**     | The error-agent conversation behind **Report a problem** — filing, refining and tracking a problem report. Appears only once the user has a report in play. |

## Header & presence

There is no separate session header row on desktop. The toolbar's **left zone** carries the
session name (plus its git-sync state, and a **Suspended** badge while it sleeps); its **right zone** carries the panel icons and,
closing the row, live **member presence** — avatars showing who is online, idle or offline.
Click presence to see everyone with access to the session and, with the right, to add
members.

## Mobile

Single pane at a time, switched via a top tab row: **Chat | Workspace | Preview**. Tapping
**Preview** first opens a bottom sheet to pick or start an app. A second bottom
sheet (the mobile header panel) holds session settings, the member roster, expert-mode
cost/status, and — when A2A is active — a Viewer/Chat view toggle.

## Common click-paths (for guiding users)

- **Invite someone to this session** → **Share** panel (toolbar icon) > add member by
  email + role. (Or the agent can do it via a capability, approval-gated.)
- **Enable a skill/asset** → **AI Assets** panel (toolbar icon).
- **Mount a cloud folder (Box / SharePoint / Google Drive / NextCloud)** →
  **Connectors** panel (toolbar icon) → **Connect \<provider\>** → pick account +
  folder → reload/restart when prompted. Requires the **project owner's** connection to
  exist first (**My Connections**) — mounts run on the project owner's connection, whoever
  adds them (see `concepts/integrations.md`).
- **Share a folder with a collaborator (subset of data)** → the **Workspace** panel, folder
  **...** menu > **Share in new session...**.
- **Share a finished report externally** → open the file, use the share action to create a
  revocable public link (Session Owner only).
- **Preview the app** → the **panel switcher** in the toolbar's centre zone, then
  **Start** on the app in the pane's list. Switch between running apps from the dropdown
  in the pane header.
- **Make the session sleep / wake it** → just leave it (auto-hibernates). Reopening shows
  the conversation at once; typing or sending wakes it, and **Resume session** in the
  Workspace panel wakes it without a message. The session's **...** menu also has
  **Hibernate session** / **Wake session**.
- **Close the session (fold work back to main)** → the session's **...** menu > **Close
  session**; this syncs changes back and is not the same as letting it sleep.
- **Create a file** → Workspace panel: **New file** beside the search, or a folder's
  **...** > **New file here**.
- **Chat with a pinned session without opening it** → its live tile on the dashboard.

Grounded in: `frontend/src/pages/workspace/workspace.page.tsx`,
`frontend/src/pages/workspace/workspace-header-bridge.tsx`,
`frontend/src/pages/workspace/parts/` (chat-panel, resource-explorer,
explorer-layout-store, create-file-dialog, resource-properties-dialog,
workspace-suspended-pane, flow-run-pane, session-chat-panel-header, app-preview,
preview-list, workspace-content, mobile-header-panel),
`frontend/src/hooks/session-hibernation.helpers.ts` (held-send copy, wake on typing),
`frontend/src/components/ui/chat-message/question-message.tsx`,
`frontend/src/lib/resource-viewer-registry.ts`,
`frontend/src/components/resource-viewers/resource-viewer-host.tsx`,
`frontend/src/components/ui/workspace-layout-toggles/`,
`frontend/src/components/ui/application-toolbar/` (application-toolbar,
toolbar-sidebar-actions), `frontend/src/components/ui/presence-bar/presence-bar.tsx`,
`frontend/src/components/ui/ai-assets-panel/ai-assets-panel.tsx` (AI Assets +
Connectors panels), `frontend/src/components/ui/sharing-sidepanel/sharing-sidepanel.tsx`,
`frontend/src/components/ui/error-agent-companion-tab/error-agent-companion-tab.tsx`,
`frontend/src/pages/projects/settings/project-connectors-workspace.tsx`,
`frontend/src/components/ui/provider-reauth-notice/provider-reauth-notice.tsx`.
Verified against platform `main` @ `bb6449b20` (2026-09-26).
