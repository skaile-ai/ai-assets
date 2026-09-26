# UI: Navigation & Settings (Where Things Live)

How to find anything and walk a user through a click-path. Labels in **bold** are the
real UI strings.

## The app shell

- **Left sidebar** — top-level navigation, top to bottom.
  - The **Skaile logo** is the only entrance to the dashboard; it turns into an animated
    spinner while the dashboard is open. There is no separate Dashboard row and no
    activity badge on it.
  - Below it, a **search field** (**Search projects and sessions…**).
  - The **organization** is a pinned header row, not a collapsible group. Its kebab,
    **Organization actions** (also opened by clicking the row), offers **Switch
    organization** (submenu), **New project**, then **Flows**, **Run groups**, **Sessions**
    and **Store**, and **Organization settings** (org Owners and platform admins). This
    menu is the only sidebar entrance to those four pages; Cmd+K has them too.
  - Then the **projects**. Expanding one shows, in order: **Apps** (the apps its sessions
    declare; a green **Running** dot marks one that is serving — clicking an app opens its
    session with only that app's preview showing), the project's sessions, **Flows** (each
    flow with its run groups), and **Archive** (archived sessions — only in expert mode).
    A section with one item shows it directly under the project instead of in a group row;
    empty sections are omitted. Clicking the name of a project with exactly one session
    opens that session; its expand toggle still expands it.
  - A project's **...** menu: **Pin to dashboard**, **Mark all sessions as read**, **New
    agent** (opens the **New agent** dialog, which creates a session), **New flow**,
    **Flows**, **Run groups**, **Agent graph**, **Project settings** (Owner only), and a
    **Notifications** submenu. There is no standalone New Project row, and no per-session
    star — dashboard pins are the one source of truth for favourites.
  - Collapsed, the sidebar is a rail of one icon per project; the flyout lists that
    project's sessions, flows and **New agent**.
  - **Footer**: a **Personal** / **Business** workspace-mode toggle (switching to Personal
    repaints the app in a warm palette, so the user can see which workspace they are in),
    and the user's avatar menu — **Invite someone to Skaile**, **Report a problem**,
    **Account**, **Preferences**, **Invites**, **My Connections** (hidden from org
    Viewers), an **Expert mode** toggle, a **Theme** submenu, an **Info** submenu
    (**Open-source licenses** plus the frontend/backend version numbers; for everyone),
    **Focus organization** (only on deployments with organization subdomains), a **Platform**
    admin submenu (platform admins only: **Admin Dashboard**, **User Dashboard**, **Org
    Dashboard**, **Platform Usage**, **Session Manager**), and **Sign Out**.
  - Toggle the left sidebar with **Cmd/Ctrl+B**, the right one with **Cmd/Ctrl+.**.
- **Top header** — two rows on desktop.
  - Row 1: the sidebar toggle plus a **tab bar** of fully rounded pills, one per open
    session or page. A session tab shows the project icon and the project name in bold,
    then the session name; a page tab shows a type icon. There is no breadcrumb and no
    inline org / project / session switcher. **Escape** closes the current tab when
    nothing else claims the key (not while typing, or while a dialog or menu is open);
    in a session whose agent is working, the first Escape stops the agent instead.
    Ctrl+Shift+W also closes a tab.
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
  point — most features have a command. It lists one **Switch to \<org\>** entry per other
  organization, and each session as **Switch to \<Project\>: \<Session\>** (prefixed with
  the org name when the user has two or more business orgs).

## Top-level pages

| Page              | Path                          | What the user does there |
| ----------------- | ----------------------------- | ------------------------ |
| **Dashboard**     | `/dashboard` (and `/<org>`)   | A bento grid of tiles the user rearranges with a pencil toggle (**Edit dashboard layout** / **Done editing dashboard**): **Assistant**, **Create Project**, **Create Session**, **Invite Users**, **Activity**, **Invitations**, **Recent Projects**, **Pinned Projects**, **Pinned Previews**. Those are the names in the layout editor; the three action tiles read **Create project**, **Create session** and **Invite users** on the tile itself. A viewer who cannot invite gets no **Invite Users** tile at all. Each **pinned session** (up to six) is its own live tile — its conversation, a status badge, and a composer the user can send from without leaving the dashboard; **Open full session** opens it, and in edit mode **Unpin \<name\>** removes it. The **Assistant** tile is the same kind of live chat. Pins are filtered to the current organization. |
| **Account**       | `/account`                    | Edit name, email, profile picture. |
| **Preferences**   | `/<org>/preferences`          | Notification mode (All / Mentions / Direct / Off), sound, browser notifications. |
| **My Connections**| `/<org>/my-connections`       | The user's own sign-ins, **one provider at a time**: a tab each for **SharePoint**, **Exchange**, **Box**, **GitHub**, **Google Drive** and **Nextcloud** (with a count of connected accounts), plus **Other** when the org has further provider links. Each tab has a **Skaile-managed** one-click connect and an **IT-managed / bring your own app** section; a connection that stopped working offers **Reconnect**. The **Exchange** tab also holds **Shared mailboxes** — see below. Dropbox has no tab and is **not usable yet** (no driver behind it), even if an admin has added a Dropbox provider. A finished Connect stores a credential only — see `concepts/integrations.md` § *From a connection to files in a session* for the follow-up step users always need. |
| **Store**         | `/<org>/store`                | The organization's asset/skill catalog, reached from the org kebab. Tabs: **Catalog**, **Library**, **Approvals** (approvals are admin-only). |
| **Run groups**    | `/<org>/runs`                 | Batch / unattended processing: the status board for every run group, with click-through into a group's detail page. See `concepts/flows.md`. |
| **Flows**         | `/<org>/flows`                | Browse and author flow definitions; open a flow's graph view/editor. See `concepts/flows.md`. |
| **Sessions**      | `/<org>/sessions`             | The org sessions report (Cmd+K: **Sessions report**): one row per agent session — project, agent, type, owner, members, default connector and folder, message count, cost, last activity — with per-column filters, search, a cost period (**7d** / **30d** / **90d** / **365d** or custom dates) and **Export to Excel** of the filtered rows. Org Owners see every session in the org; everyone else sees the sessions they have access to. Personal-assistant sessions are not listed. |
| **Agent graph**   | `/<org>/<project>/graph`      | From a project's **...** menu or Cmd+K: the project's agents as cards, agent-to-agent links as arrows, and its apps and flows in side columns. |
| **Open-source licenses** | `/licenses`            | Third-party components shipped to the browser, with licenses and source links. From **Info** in the avatar menu. |

### Shared Exchange mailboxes

A shared mailbox is added **once per Connection** in **My Connections > Exchange >
Shared mailboxes**: open **Add a shared mailbox**, pick the **Acting account**, enter the
**Shared mailbox address**, **Add mailbox**. The acting account needs Full Access to it in
Exchange; if the Connection lacks the permission, the refusal offers **Allow shared mail
access** (a Microsoft sign-in; some tenants need an admin to approve it). Adding enables
nothing: each project then lists the mailbox unchecked until a project Owner enables it in
the project's Connectors settings. **Remove** takes it away from every project. The list
shows only mailboxes on the current organization's Connections.

## Creating a project

Entry: the org kebab (**Organization actions**) > **New project**, the dashboard's
**Create project** tile, or Cmd+K. It is one form, not a wizard:

1. **Organization** picker (in the dialog; locked when opened from an org row).
2. **Source** cards: **On Skaile**, **Git Repository**, **SharePoint / OneDrive**,
   **Google Drive**, **NextCloud**, **Box**, **Local Folder**. A cloud provider appears
   once the org has a matching provider connection (e.g. Box shows up when a Box provider
   exists). The matching picker (git tree, folder/file browser) appears inline; if the
   user's sign-in for it has lapsed the picker shows **Reconnect** (or **Connect
   account**), which opens that connection in My Connections. Dropbox is **not** offered.
3. **Project Details** — **Project Name** (required) and **Description**.
4. **Agent** — the name and picture of the project's first agent.
5. **Sharing** — **Private** or **Shared** (Shared needs an org Owner); for Shared,
   **Teams with access**.
6. **Create Project**.

First-time users get a first-run onboarding modal that can launch a working session in one
optional step (e.g. "Analyse a document" guides drag-and-drop upload in the workspace).

## Project settings

Path: `/<org>/projects/<project>/settings` (Owner-only). Tabs:

| Tab               | Purpose |
| ----------------- | ------- |
| **Sessions**      | List/manage all sessions in the project; bulk mark-read / delete. |
| **Members**       | Invite users, set Owner/User/Viewer, team access. |
| **Project**       | Name, slug, description, **Visibility** (**Private** / **Shared**), delete. |
| **Session defaults** | Skaile config template applied to new sessions — including additional mounts — plus the default asset assignments for the project's sessions. (There is no separate "Assets" tab; asset defaults live here.) |
| **Security**      | **Network egress**: **Open**, **Off — LLM provider only**, or **Allowlist specific domains**. |
| **Connectors**    | Project-level connector enablement and account selection (today: Exchange — the project's mailbox access switch, and per-mailbox enable/disable including shared mailboxes admitted in My Connections). For file mounts use the workspace **Connectors** panel instead. |
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

Path: `/<org>/settings` (org Owners and platform admins). Tabs: **Organization**
(branding), **Users** (invite/roles/revoke), **Teams**, **Providers** (org-level connectors:
Git / Files / Transport, with UserDelegation or ServiceAccount credentials), **AI** (org-wide
AI defaults: available clouds, flow authoring, and the driver/provider/model defaults
inherited by all projects), **AI Providers** (model endpoints: Anthropic/OpenAI/Custom,
scoped Global/Org/Project, delivered direct or via a cloud transport — AWS Bedrock, GCP
Vertex, Azure AI Foundry, custom gateway — with per-config health checks; a **Claude
subscription** seat is bound by pasting the output of `claude setup-token`, with the
credentials-file upload as the alternative; the page also offers **Add Codex
subscription**), **Classifiers** (classifier providers — see below), **Costs**,
**Deployment Targets**, and **Catalog** (manage reusable assets/skills, assign to
teams/projects). The org sessions report is not a tab — it is **Sessions** in the org
kebab.

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

- **Personal OAuth for myself** → **My Connections** (`/<org>/my-connections`), on that
  provider's tab. The assistant can also start this in-conversation and hand over the
  sign-in link; it is not a UI-only step (see `concepts/agent.md`).
- **A picker says Reconnect / Connect account** → click it; it opens the exact connection
  in My Connections.
- **Org-wide provider for everyone / service accounts** → org **Settings > Providers**.
- **Which provider this project's data uses** → set at **project creation** (Source cards).
- **Bring a connected cloud folder into an existing session/project** → workspace
  **Connectors** panel → **Connect \<provider\>** (see `concepts/integrations.md` §
  *From a connection to files in a session*) — or create a new project with that Source.
- **A shared Exchange mailbox** → **My Connections > Exchange > Add a shared mailbox**,
  then enable it per project (project **Settings > Connectors** or the workspace
  **Connectors** panel).
- **AI model/endpoint** → org **Settings > AI Providers**.
- **Classifier provider (TypeSafe Jev)** → org **Settings > Classifiers**.
- **Enable an asset/skill** → the workspace **AI Assets** panel (session- or
  project-scope add), or org **Settings > Catalog** (see `ui/workspace.md`).

Grounded in: `frontend/src/components/ui/app-sidebar-navigation/app-sidebar-navigation.tsx`,
`frontend/src/components/ui/org-actions-menu/org-actions-menu.tsx`,
`frontend/src/components/ui/workspace-explorer/sidebar-projects-tree.tsx` (+ `.helpers.ts`
sections), `frontend/src/components/ui/project-actions-menu/project-actions-menu.tsx`,
`frontend/src/actions/project-actions.tsx`,
`frontend/src/components/ui/workspace-mode-toggle/`,
`frontend/src/components/ui/session-tabs-bar/` (incl. `escape-closes-tab.ts`),
`frontend/src/components/ui/application-toolbar/`,
`frontend/src/pages/dashboard/` (bento tile catalogue, session widget tiles),
`frontend/src/pages/org-sessions/org-sessions.page.tsx`,
`frontend/src/pages/project-graph/project-graph.page.tsx`,
`frontend/src/pages/licenses/licenses.page.tsx`,
`frontend/src/pages/store/store.page.tsx`, `frontend/src/pages/settings/` (incl.
`my-connections.page.tsx`, `ai-providers.page.tsx`, `classifier-providers.page.tsx`),
`frontend/src/components/ui/exchange-connect-card/`,
`frontend/src/components/ui/provider-reauth-notice/provider-reauth-notice.tsx`,
`frontend/src/pages/projects/project-setup.page.tsx`,
`frontend/src/pages/projects/settings/` (tabs, security, connectors).
Verified against platform `main` @ `bb6449b20` (2026-09-26).
