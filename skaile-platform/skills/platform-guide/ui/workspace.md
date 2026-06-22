# UI: The Workspace (Where Work Happens)

The workspace is the main session surface — chat with the agent, browse files, preview
apps. Path: `/<org>/projects/<project>/<session>`. Labels in **bold** are real UI strings.

## Panes (desktop)

The user toggles which panes are visible (layout persists). Possible combinations:
chat-only, workspace-only, preview-only, chat+workspace, chat+preview.

1. **Chat panel** — the conversation with the agent.
   - Scrollable message history with live typing indicators.
   - **Composer** at the bottom: type a message, attach files (drag-and-drop or the
     attachment tray), and use **@** for mentions and **/** for slash commands (a prefix
     picker appears at the cursor). Attachments show as a tray of thumbnails/icons before
     sending.
   - Agent tool/capability invocations and their results render inline; approval-gated
     actions show an approval card the user clicks to allow or reject.

2. **Resource explorer** — the workspace file tree (the project data + mounted sources).
   - Browse/open files (pluggable viewers per file type).
   - Drag-and-drop upload (with conflict resolution: overwrite / rename / skip).
   - Folder context menu (the **...** on hover): rename, delete, and **Share in new
     session...** (creates a scoped session — hidden for git/empty sources).
   - The empty state shows a dashed drop area for first uploads.

3. **App preview** — a live preview of a runnable app in the workspace.
   - **Start** / **Restart** / **Stop** controls; a device-mode toggle (desktop/tablet/mobile).
   - A tab strip switches between apps when the workspace declares more than one
     (`?app=<id>` in the URL).
   - A **Preview logs** drawer streams console output, filterable by role/level.
   - Empty state explains why a workspace is not previewable (preview contract not met).

## Right-sidebar tabs

Toggle with **.** (period). Tabs (in order):

| Tab           | Purpose |
| ------------- | ------- |
| **Assistant** | A mini chat with the user's personal AI assistant; **Expand** opens its full session. Shown only when the viewer has a personal assistant and is not already in it. |
| **Preview**   | Capability-render previews (fallback when the main layout hides the workspace). |
| **AI Assets** | Connectors, skills/assets, and catalog assignments for this session — the place to enable an asset. |
| **Share**     | Sharing pane: visibility toggle, team access, project/session members + roles, invites, and public preview-share links. |
| **Summary**   | Session snapshot and resume strategy. |
| **Config**    | Skaile config editor for the session (full or mounts-only). |
| **Debug**     | Restart / kill / compact the session, view protocol info, and read live logs. |

## Header & presence

The session header shows the session name, owner, status (Provisioning / Running /
Hibernated / Closed / Error), and live **member presence** (avatars, online/idle/offline).
Click presence to see everyone with access. Member count is badged.

## Mobile

Single pane at a time, switched via a top tab row: **Chat | Workspace | Preview**. A bottom
sheet (the mobile header panel) holds session settings, the member roster, expert-mode
cost/status, and — when A2A is active — a Viewer/Chat view toggle.

## Common click-paths (for guiding users)

- **Invite someone to this session** → right sidebar **Share** tab > add member by email +
  role. (Or the agent can do it via a capability, approval-gated.)
- **Enable a skill/asset** → right sidebar **AI Assets** tab.
- **Share a folder with a collaborator (subset of data)** → resource explorer, folder
  **...** menu > **Share in new session...**.
- **Share a finished report externally** → open the file, use the share action to create a
  revocable public link (Session Owner only).
- **Preview the app** → switch to the preview pane and click **Start**.
- **Make the session sleep / wake it** → just leave it (auto-hibernates); reopen it from
  the sidebar to wake.
- **Close the session (fold work back to main)** → session controls; this syncs changes
  back and is not the same as letting it sleep.

Grounded in: `frontend/src/pages/workspace/workspace.page.tsx`,
`frontend/src/pages/workspace/parts/` (chat-panel, resource-explorer, app-preview,
workspace-content, mobile-header-panel),
`frontend/src/components/ui/sharing-sidepanel/sharing-sidepanel.tsx`.
