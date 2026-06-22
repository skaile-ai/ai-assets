# UI: Navigation & Settings (Where Things Live)

How to find anything and walk a user through a click-path. Labels in **bold** are the
real UI strings.

## The app shell

- **Left sidebar** — top-level navigation.
  - **Dashboard** button (with an activity badge for mentions/unread).
  - **Project tree**, grouped by organization (collapsible). Each project lists its
    sessions with unread indicators. A project/session context menu offers settings,
    **New session**, etc.
  - **Footer**: the user's avatar + name opens a dropdown with **Account**,
    **Preferences**, **My Connections**, an **Expert mode** toggle, a **Theme**
    submenu (Light/Dark/System), a **Platform** admin submenu (admins only), and **Sign Out**.
  - Toggle the sidebar with **B**.
- **Top header** — a breadcrumb with inline **org / project / session switchers**, the page
  title, a row of open **session tabs**, and the right-sidebar toggle.
- **Command palette (Cmd+K)** — global fuzzy search and action launcher across sessions,
  projects, settings, and registered actions. This is the primary "how do I do X" entry
  point — most features have a command. Right sidebar toggles with **.** (period).

## Top-level pages

| Page              | Path                          | What the user does there |
| ----------------- | ----------------------------- | ------------------------ |
| **Dashboard**     | `/<org>/dashboard`            | See all projects + recent activity; create a project; switch org. |
| **Account**       | `/account`                    | Edit name, email, profile picture. |
| **Preferences**   | `/<org>/preferences`          | Notification mode (All / Mentions / Direct / Off), sound, browser notifications. |
| **My Connections**| `/<org>/my-connections`       | Personal **Connect** flows for GitHub, GitLab, SharePoint, Google Drive, NextCloud, Dropbox (OAuth/PAT). |

## Creating a project (wizard)

Entry: **New Project** in the sidebar, or the **Create** button on the dashboard. Steps:

1. **Source** — pick the project data: On Skaile (empty) / Git (GitHub/GitLab/Bitbucket) /
   SharePoint / Google Drive / NextCloud / Local Folder / Empty. The matching picker
   (git tree, folder/file browser) appears inline.
2. **Identity** — name, slug (auto-filled, editable), description.
3. **Members & teams** — add members by email with a role; choose visibility:
   **Private** / **Team** / **Org**.
4. **AI Assistant** — optionally create a dedicated personal-assistant session.
5. **Review & Create**.

First-time users get a first-run onboarding modal that can launch a working session in one
optional step (e.g. "Analyse a document" guides drag-and-drop upload in the workspace).

## Project settings

Path: `/<org>/projects/<project>/settings` (Owner-only). Tabs:

| Tab               | Purpose |
| ----------------- | ------- |
| **Sessions**      | List/manage all sessions in the project; bulk mark-read / delete. |
| **Members**       | Invite users, set Owner/User/Viewer, team access. |
| **Project**       | Name, slug, description, visibility (Private/Team/Org), delete. |
| **Session defaults** | Skaile config template applied to new sessions. |
| **Assets**        | Default asset/connector assignments for the project's sessions. |
| **Security**      | Cross-org sharing, session access rules. |
| **Shares**        | Manage public preview-share links. |
| **Costs**         | Cost tracking/attribution. |

## Session settings

Path: `/<org>/projects/<project>/<session>/settings` (Session or Project Owner). Tabs:

| Tab          | Purpose |
| ------------ | ------- |
| **Members**  | Session-scoped role overrides on top of project membership; add session-only members. |
| **Config**   | Session-scoped Skaile config (overrides project defaults). |
| **Shares**   | Session visibility toggle (**Shared** / **Private**) and public file-preview links. |

## Organization settings

Path: `/<org>/settings` (admin only). Tabs: **Organization** (branding), **Users**
(invite/roles/revoke), **Teams**, **Providers** (org-level connectors: Git / Files / Transport,
with UserDelegation or ServiceAccount credentials), **AI Providers** (model endpoints:
Anthropic/OpenAI/Azure/Custom, scoped Global/Org/Project), **Costs**, **Deployment Targets**,
and **Catalog** (manage reusable assets/skills, assign to teams/projects).

## Where to connect a data source (cheat sheet)

- **Personal OAuth for myself** → **My Connections** (`/<org>/my-connections`).
- **Org-wide provider for everyone / service accounts** → org **Settings > Providers**.
- **Which provider this project's data uses** → set at **project creation** (Source step).
- **AI model/endpoint** → org **Settings > AI Providers**.
- **Enable an asset/skill** → **Catalog** (org) or project **Assets** tab, or the workspace
  **AI Assets** tab (see `ui/workspace.md`).

Grounded in: `frontend/src/pages/` and `frontend/src/components/ui/` (dashboard, settings,
sidebar, new-project-modal, project-setup).
