# UI: Navigation & Settings (Where Things Live)

How to find anything and walk a user through a click-path. Labels in **bold** are the
real UI strings.

## The app shell

- **Left sidebar** — top-level navigation, top to bottom.
  - The **Skaile logo** is the only entrance to the dashboard; it turns into an animated
    spinner while the dashboard is open. There is no separate Dashboard row and no
    activity badge on it.
  - Below it, a **search field** over projects and sessions.
  - The **organization** is a pinned header row, not a collapsible group. Its
    always-visible kebab, **Organization actions**, offers **Switch organization**
    (submenu), **New project**, **Store**, and **Organization settings** (admin only).
    Flows is deliberately not in that menu.
  - Two fixed entries under the org: **Run groups** (`/<org>/runs`) and **Flows**
    (`/<org>/flows`).
  - Then the **projects**. Each expands to its sessions and flows; a project's **...**
    menu offers **New session**. There is no standalone New Project row, and no
    per-session star — dashboard pins are the one source of truth for favourites.
  - Collapsed, the sidebar is a rail of one icon per project; the flyout lists that
    project's sessions, flows and **New session**.
  - **Footer**: a **Personal** / **Business** workspace-mode toggle, and the user's
    avatar menu — **Invite someone to Skaile**, **Report a problem**, **Account**,
    **Preferences**, **Invites**, **My Connections**, an **Expert mode** toggle, **Info**
    (expert mode only), a **Theme** submenu, **Focus organization**, a **Platform** admin
    submenu (admins only), and **Sign Out**.
  - Toggle the left sidebar with **Cmd/Ctrl+B**, the right one with **Cmd/Ctrl+.**.
- **Top header** — two rows on desktop.
  - Row 1: the sidebar toggle plus a **tab bar** of fully rounded pills, one per open
    session or page. A session tab shows the project icon and the project name in bold,
    then the session name; a page tab shows a type icon. There is no breadcrumb and no
    inline org / project / session switcher.
  - Row 2: the **toolbar**, a three-zone row — left the page or session title, centre the
    **panel switcher** (which panes are visible) plus a round swap button, right the
    **panel icons** (Assistant, Preview, AI Assets, Connectors, Share, Summary, Flow,
    System, Config, Report) and live **member presence**.
  - There is **no permanent right sidebar**: a panel icon opens that panel on the right of
    the workspace and a second press dismisses it, leaving nothing on the right edge.
    See `ui/workspace.md`.
  - Reporting a bug, suggesting an idea or asking a question is **Report a problem** in the
    user menu — it opens a short form; the follow-up conversation runs in the **Report**
    panel, and reports reach the Skaile team directly.
- **Command palette (Cmd+K)** — global fuzzy search and action launcher across sessions,
  projects, settings, and registered actions. This is the primary "how do I do X" entry
  point — most features have a command.

## Top-level pages

| Page              | Path                          | What the user does there |
| ----------------- | ----------------------------- | ------------------------ |
| **Dashboard**     | `/dashboard` (and `/<org>`)   | A bento grid of tiles the user rearranges with a pencil toggle (**Edit dashboard layout** / **Done editing dashboard**): **Assistant**, **Create Project**, **Create Session**, **Invite Users**, **Activity**, **Invitations**, **Recent Projects**, **Pinned Projects**, **Pinned Sessions**, **Pinned Previews**. Those are the names in the layout editor; the three action tiles read **Create project**, **Create session** and **Invite users** on the tile itself. A viewer who cannot invite gets no **Invite Users** tile at all. Pins are filtered to the current organization. |
| **Account**       | `/account`                    | Edit name, email, profile picture. |
| **Preferences**   | `/<org>/preferences`          | Notification mode (All / Mentions / Direct / Off), sound, browser notifications. |
| **My Connections**| `/<org>/my-connections`       | Personal **Connect** flows for GitHub, GitLab, SharePoint, Google Drive, NextCloud, Box (OAuth/PAT). Dropbox is listed but **work in progress — not usable yet** (no driver behind it). A finished Connect stores a credential only — see `concepts/integrations.md` § *From a connection to files in a session* for the follow-up step users always need. |
| **Store**         | `/<org>/store`                | The organization's asset/skill catalog, reached from the org kebab. Tabs: **Catalog**, **Library**, **Approvals** (approvals are admin-only). |
| **Run groups**    | `/<org>/runs`                 | Batch / unattended processing: the status board for every run group, with click-through into a group's detail page. See `concepts/flows.md`. |
| **Flows**         | `/<org>/flows`                | Browse and author flow definitions; open a flow's graph view/editor. See `concepts/flows.md`. |

## Creating a project (wizard)

Entry: the org kebab (**Organization actions**) > **New project**, the dashboard's
**Create project** tile, or Cmd+K. Steps:

1. **Source** — pick the project data: On Skaile (empty) / Git (GitHub/GitLab/Bitbucket) /
   SharePoint / Google Drive / NextCloud / **Box** / Local Folder / Empty. A cloud
   provider appears once the org has a matching provider connection (e.g. Box shows up
   when a Box provider exists). The matching picker (git tree, folder/file browser)
   appears inline. Dropbox is **not** offered (work in progress).
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
| **Session defaults** | Skaile config template applied to new sessions — including additional mounts — plus the default asset assignments for the project's sessions. (There is no separate "Assets" tab; asset defaults live here.) |
| **Security**      | Cross-org sharing, session access rules. |
| **Connectors**    | Project-level connector enablement and account selection (today: Exchange mailboxes). For file mounts use the workspace **Connectors** panel instead. |
| **Costs**         | Cost tracking/attribution. |
| **Shares**        | Manage public preview-share links. |

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
with UserDelegation or ServiceAccount credentials), **AI** (org-wide AI defaults: available
clouds, flow authoring, and the driver/provider/model defaults inherited by all projects),
**AI Providers** (model endpoints:
Anthropic/OpenAI/Custom, scoped Global/Org/Project, delivered direct or via a cloud
transport — AWS Bedrock, GCP Vertex, Azure AI Foundry, custom gateway — with per-config
health checks), **Classifiers** (classifier providers — see below), **Costs**,
**Deployment Targets**, and **Catalog** (manage reusable assets/skills, assign to
teams/projects).

### Classifiers

**Settings > Classifiers** (org admin only) holds the organization's **classifier
providers** — the fast, cheap yes/no/choice/score answerers behind classifier flow nodes and
`platform.classify` (`references/classifier.md`). **Add Provider** asks for the **Provider**
(**TypeSafe Jev**, or a **Jev-compatible endpoint** plus its **Endpoint**), a **Name**, a
**Model version** — a pinned version such as `jev-1.13.0`; floating aliases like `jev-latest`
are refused — and the **API key**, which is encrypted on save and never shown again. The
dialog shows the vendor's data-processing notice: ticking it while adding makes the provider
active at once; otherwise the admin clicks **Acknowledge terms** later and switches it
**Active**. **Test** checks the key and model from the platform.

Setting this up is the organization's **opt-in to sending content to the classifier vendor**,
so it is an admin decision — never do it for the user. Until an active, acknowledged provider
exists, classifier flow nodes run on the generative fallback and `platform.classify` returns
`no_classifier_provider`. If the vendor's notice changes, the provider stops answering until
an admin acknowledges the new one.

## Where to connect a data source (cheat sheet)

- **Personal OAuth for myself** → **My Connections** (`/<org>/my-connections`). The
  assistant can also start this in-conversation and hand over the sign-in link; it is not
  a UI-only step (see `concepts/agent.md`).
- **Org-wide provider for everyone / service accounts** → org **Settings > Providers**.
- **Which provider this project's data uses** → set at **project creation** (Source step).
- **Bring a connected cloud folder into an existing session/project** → workspace
  **Connectors** panel → **Connect \<provider\>** (see `concepts/integrations.md` §
  *From a connection to files in a session*) — or create a new project with that Source.
- **AI model/endpoint** → org **Settings > AI Providers**.
- **Classifier provider (TypeSafe Jev)** → org **Settings > Classifiers**.
- **Enable an asset/skill** → the workspace **AI Assets** panel (session- or
  project-scope add), or org **Settings > Catalog** (see `ui/workspace.md`).

Grounded in: `frontend/src/components/ui/app-sidebar-navigation/app-sidebar-navigation.tsx`,
`frontend/src/components/ui/org-actions-menu/org-actions-menu.tsx`,
`frontend/src/components/ui/workspace-explorer/sidebar-projects-tree.tsx`,
`frontend/src/components/ui/session-tabs-bar/session-tabs-bar.tsx`,
`frontend/src/components/ui/application-toolbar/`,
`frontend/src/pages/dashboard/` (bento grid + tile catalogue),
`frontend/src/pages/store/store.page.tsx`, `frontend/src/pages/settings/` (incl.
`classifier-providers.page.tsx`),
`frontend/src/components/ui/new-project-modal/new-project-modal.tsx`,
`frontend/src/pages/projects/project-setup.page.tsx`.
