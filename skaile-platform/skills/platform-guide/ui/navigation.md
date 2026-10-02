# UI: Navigation & Settings (Where Things Live)

How to find anything and walk a user through a click-path. Labels in **bold** are the
real UI strings. When the user would rather be taken there, the agent can open the screen
for them with `platform.navigate` (dashboard, projects, sessions, a project or its settings and
runs, a session, a run group, flows, organization settings, **My Connections**, the store,
preferences) — see `concepts/agent.md`. The call is
`platform.navigate({ route, params?, search?, target? })`; `params` take an id or slug, and `org`
defaults to the project's (else this session's) organization. `target` is `current` (default,
replaces the page they are on) or `tab` (opens a new tab and keeps the current one):

| `route` | Screen | `params` |
| --- | --- | --- |
| `dashboard` | Dashboard | — |
| `projects`, `sessions`, `flows`, `store`, `preferences`, `myConnections` | that organization page (`myConnections` takes `search: { link }` to highlight one connection) | `org?` |
| `org.settings` | Organization settings (org Owners only; `search: { tab }`, e.g. `ai-providers`, `assistants`, or `orphaned-my-space` for the former members' My space review) | `org?` |
| `project`, `project.settings`, `project.runs` | a project's main session, its settings, its run board | `project` |
| `runGroup` | one run group's board | `project`, `runGroup` |
| `session` | one session | `session` (a slug resolves in this session's project; elsewhere pass its id or `project`) |

The capability's own description carries the live list; prefer it if the two differ.

## The app shell

- **Left sidebar** — top-level navigation, top to bottom.
  - The **Skaile logo** is the only entrance to the dashboard; it turns into an animated
    spinner while the dashboard is open. There is no separate Dashboard row and no
    activity badge on it.
  - Below it, the **Work** / **Private** switch, shown only to someone who has both their
    own Private workspace and a business organization. **Private** opens their own Private
    workspace (never one they were invited into); **Work** opens the business organization
    they used last. Inside their own Private workspace the app is repainted in a warm
    palette, so they can see which side they are on. The side the user is not on shows a
    count of approvals waiting for them there. Cmd+K has it as **Switch to Private** /
    **Switch to Work**.
  - Then a **search field** (**Search projects and sessions…**).
  - The **organization** is a pinned header row, not a collapsible group. Its kebab,
    **Organization actions** (also opened by clicking the row), offers **Switch
    organization** (submenu), **New project**, then **Flows**, **Run groups**, **Sessions**
    and **Store**, and **Organization settings** (org Owners and platform admins). This
    menu is the only sidebar entrance to those four pages; Cmd+K has them too.
    **Switch organization** groups the user's orgs into collapsible sections: **Starred**
    (only when something is starred; open by default), **Recent** (the last five business
    orgs the user was in, shown only once they belong to more than five; its orgs stay
    under **Organizations** too), **Organizations** (open unless something is starred), and
    **Invited Private workspaces** (other people's Private orgs the user was invited to;
    collapsed by default). Each row has a star icon
    (**Star \<org\>**) that moves the org into or out of **Starred**. Stars are per user
    and follow them across devices.
  - Then the **projects**, in up to three sections: **Company** (projects an org Owner
    marked as company projects; never in a Private workspace), **Projects** (the
    organization's other projects), and **My space** (the user's own private projects).
    The user's **Home** comes first in My space, labelled with their assistant's name; it
    is where that assistant lives. The **My space** heading carries a **+** (**New My space
    project**, also in Cmd+K) for anyone who is an org User or Owner there; a Viewer has no
    My space. A new My space project opens only for its creator; to share it later, it must
    be moved to Projects (see *Project settings*). Starred projects are lifted into a
    leading **Starred** section, and an empty section has no heading (except My space, so
    its **+** stays reachable). Expanding a project shows, in order: **Apps** (the apps
    its sessions declare; a green **Running** dot marks one that is serving — clicking an
    app opens its session with only that app's preview showing), the project's sessions,
    **Flows** (each flow with its run groups), and **Archive** (archived sessions — only in expert mode).
    A section with one item shows it directly under the project instead of in a group row;
    empty sections are omitted. Clicking the name of a project with exactly one session
    opens that session; its expand toggle still expands it.
  - A project's **...** menu: **Pin to dashboard**, **Star** / **Unstar**, **Mark all
    sessions as read**, **New agent** (opens the **New agent** dialog, which creates a
    session), **New flow**, **Flows**, **Run groups**, **Agent graph**, **Project
    settings** (Owner only), and a **Notifications** submenu. There is no standalone New
    Project row, and no per-session star. Pins decide what the dashboard shows; stars only
    reorder the sidebar's org and project lists.
  - Collapsed, the sidebar is a rail of one icon per project; the flyout lists that
    project's sessions, flows and **New agent**.
  - **Footer**: the user row. At its right end sits a round button with the assistant's
    picture (ringed, with a dot for unread replies): the **assistant launcher**. It opens
    the assistant panel over the current page, or closes it if it is showing, and Cmd+K
    has it as **Open \<name\>**. Which assistant it opens depends on where the user is: in
    a business org where they have a Home, that org's assistant; in their Private
    workspace, on the dashboard, or in an org where they are only a Viewer, their home
    assistant (the one in their Private workspace). Each falls back to the other when it
    does not exist, and **Launcher always opens my home assistant** in **Preferences**
    makes it the home assistant everywhere. On the collapsed icon rail the launcher is its
    own icon above the avatar. The avatar opens the user menu — **Invite someone to
    Skaile**, **Report a problem**, **Account**, **Preferences**, **Invites**,
    **Approvals** (with a count of what waits on the user), **My Connections** (hidden
    from org Viewers), an **Expert mode** toggle, a **Theme** submenu, an **Info** submenu
    (**What's new**, **Open-source licenses** and the frontend/backend version numbers; for
    everyone),
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
    System, Config, Report) and live **member presence**. The Assistant icon opens the
    same assistant as the sidebar launcher; there is no separate assistant button in the
    desktop toolbar.
  - There is **no permanent right sidebar**: a panel icon opens that panel on the right of
    the workspace and a second press dismisses it, leaving nothing on the right edge.
    See `ui/workspace.md`.
  - Reporting a bug, suggesting an idea or asking a question is **Report a problem** in the
    user menu — it opens a short form; the follow-up conversation runs in the **Report**
    panel, and reports reach the Skaile team directly. The agent there shows the drafted
    report for review before sending it, unless the user asks it to send directly. Any
    session's agent can also report a platform problem it runs into on its own (see
    `concepts/agent.md`).
- **Command palette (Cmd+K)** — global fuzzy search and action launcher across sessions,
  projects, settings, and registered actions. This is the primary "how do I do X" entry
  point — most features have a command. It lists one **Switch to \<org\>** entry per other
  organization, and each session as **Switch to \<Project\>: \<Session\>** (prefixed with
  the org name when the user has two or more business orgs). **Star or unstar a project**
  picks a project and flips its star, and each org has a **Star organization "\<org\>"**
  / **Unstar organization "\<org\>"** entry.

## Top-level pages

| Page              | Path                          | What the user does there |
| ----------------- | ----------------------------- | ------------------------ |
| **Dashboard**     | `/dashboard` (and `/<org>`)   | A bento grid of tiles the user rearranges with a pencil toggle (**Edit dashboard layout** / **Done editing dashboard**): **Assistant**, **Create Project**, **Create Session**, **Invite Users**, **Activity**, **Invitations**, **Recent Projects**, **Pinned Projects**, **Pinned Previews**. Those are the names in the layout editor; the three action tiles read **Create project**, **Create session** and **Invite users** on the tile itself. A viewer who cannot invite gets no **Invite Users** tile at all. Each **pinned session** (up to six) is its own live tile — its conversation, a status badge, and a composer the user can send from without leaving the dashboard; **Open full session** opens it, and in edit mode **Unpin \<name\>** removes it. The **Assistant** tile is the same kind of live chat. Pins are filtered to the current organization. |
| **Account**       | `/account`                    | The profile (name, picture, roles) with **Edit profile**; the email is shown read-only. A **Your assistant** card links to the assistant's page (below). **Archived spaces** lists the user's own My space in each Private workspace they have left, with the date it is deleted and **Export** (its conversations, not its files); rejoining the workspace before then restores it. The card is absent when nothing is archived. |
| **Your assistant**| `/assistant`                  | The profile every one of the user's assistants reads, in every workspace: **Name, picture and voice**, and **What your assistant reads** (the IDENTITY, SOUL and USER documents, each with a live character count, plus one for **All three together** against 24 KiB). One **Save**; **Discard changes** drops the draft. If the profile changed elsewhere since the page loaded, the save is refused ("Changed elsewhere — reload") and the draft stays until **Reload**. Opens as its own tab and warns before unsaved changes are lost. Also reached from Cmd+K (**Edit \<name\>'s profile**) and from a **Your assistant** card in the assistant's own session settings. Changing the name, picture (avatar) or voice here sets it for every one of the user's assistants, including one in a business workspace that had set its own. |
| **Preferences**   | `/<org>/preferences`          | Notification mode (All / Mentions / Direct / Off), sound, browser notifications. **Assistant**: **Launcher always opens my home assistant** (only for someone with a home assistant). **My home assistant's reach**: one row per business organization the user belongs to, with how far the assistant in their Private workspace may reach there (**Off**, **Coordinate** or **Full**) and who set it (**Lowered by you.**, **Set for you by an organization admin.**, or the organization's default). The user can only lower a level; **Remove my limit** undoes their own lowering, back to what the organization allows. Lowering from **Full** asks for confirmation and revokes the standing approvals the assistant held there. See `concepts/agent.md` § *Assistant reach*. |
| **My Connections**| `/<org>/my-connections`       | The user's own sign-ins, **one provider at a time**: a tab each for **SharePoint**, **Exchange**, **Box**, **GitHub**, **Google Drive** and **Nextcloud** (with a count of connected accounts), plus **Other** when the org has further provider links. Each tab has a **Skaile-managed** one-click connect and an **IT-managed / bring your own app** section; a connection that stopped working offers **Reconnect**. The **Exchange** tab also holds **Shared mailboxes** — see below. Dropbox has no tab and is **not usable yet** (no driver behind it), even if an admin has added a Dropbox provider. A finished Connect stores a credential only — see `concepts/integrations.md` § *From a connection to files in a session* for the follow-up step users always need. |
| **Store**         | `/<org>/store`                | The organization's asset/skill catalog, reached from the org kebab. Tabs: **Catalog**, **Library**, **Approvals** (approvals are admin-only). |
| **Run groups**    | `/<org>/runs`                 | Batch / unattended processing: the status board for every run group, with click-through into a group's detail page. See `concepts/flows.md`. |
| **Flows**         | `/<org>/flows`                | Browse and author flow definitions; open a flow's graph view/editor. See `concepts/flows.md`. |
| **Sessions**      | `/<org>/sessions`             | The org sessions report (Cmd+K: **Sessions report**): one row per agent session — project, agent, type, owner, members, default connector and folder, message count, cost, last activity — with per-column filters, search, a cost period (**7d** / **30d** / **90d** / **365d** or custom dates) and **Export to Excel** of the filtered rows. Org Owners see every session in the org except other members' private ones (in their assistant's Home or their My space); everyone else sees the sessions they have access to. Personal-assistant sessions are not listed. |
| **Agent graph**   | `/<org>/<project>/graph`      | From a project's **...** menu or Cmd+K: the project's agents as cards, agent-to-agent links as arrows, and its apps and flows in side columns. |
| **Open-source licenses** | `/licenses`            | Third-party components shipped to the browser, with licenses and source links. From **Info** in the avatar menu. |

### Shared Exchange mailboxes

A shared mailbox is added **once per Connection** in **My Connections > Exchange >
Shared mailboxes**: open **Add a shared mailbox**, pick the **Acting account**, enter the
**Shared mailbox address**, **Add mailbox**. The acting account needs Full Access to it in
Exchange; if the Connection lacks the permission, the refusal offers **Allow shared mail
access** (a Microsoft sign-in). If Microsoft refuses because the tenant lets only admins
approve apps, the **not granted** notice offers **Get an administrator approval link**: a
dialog with **Copy link** and **Email administrator**. The link is valid for 7 days. After the admin
approves, the user runs **Allow shared mail access** again themselves. Adding enables
nothing: each project then lists the mailbox unchecked until a project Owner enables it in
the project's Connectors settings. **Remove** takes it away from every project. The list
shows only mailboxes on the current organization's Connections.

## Creating a project

Entry: the org kebab (**Organization actions**) > **New project**, the dashboard's
**Create project** tile, or Cmd+K. It creates a project under **Projects**; a private one
is **New My space project** on the sidebar's My space heading instead. It is one form, not
a wizard:

1. **Organization** picker (in the dialog; locked when opened from an org row).
2. **Source** cards: **On Skaile**, **Git Repository**, **SharePoint / OneDrive**,
   **Google Drive**, **NextCloud**, **Box**, **Local Folder**. A cloud provider appears
   once the org has a matching provider connection (e.g. Box shows up when a Box provider
   exists). The matching picker (git tree, folder/file browser) appears inline; if the
   user's sign-in for it has lapsed the picker shows **Reconnect** (or **Connect
   account**), which opens that connection in My Connections. Dropbox is **not** offered.
3. **Project Details** — **Project Name** (required) and **Description**.
4. **Agent** — the name and picture of the project's first agent.
5. **Sharing** — **Invited only** or **Everyone in \<Org\>** (the second needs an org
   Owner; stored as `Private` / `Shared`); for **Everyone in \<Org\>**,
   **Teams with access**.
6. **Create Project**. It is disabled, with a tooltip saying why, in a Private workspace
   that has become read-only (below).

There is no onboarding form. On a new user's first visit the app opens their Home's
assistant chat, chat-only, in the organization they were invited to, and the assistant
takes it from there. Someone who is a Viewer everywhere gets no Home: they land on an
empty dashboard (**No Home yet**) that tells them to ask an admin for the User role.

### A read-only Private workspace

A Private workspace stays writable while an organization sponsors it. When the
sponsorship ends, a notice strip at the top of every page in that workspace says when it
becomes read-only (a grace period) and then that it **is read-only**, with **Ask your
admin** and **Export your data** (each explains what to do; there is no one-click export of
a whole workspace). Once it is read-only, write buttons such as **Create Project** and
**Move to Projects** are disabled with the tooltip "This Private workspace is read-only."
Nothing is deleted, everything stays readable, and it unlocks as soon as someone sponsors
it again.

## Project settings

Path: `/<org>/projects/<project>/settings` (Owner-only). Tabs:

| Tab               | Purpose |
| ----------------- | ------- |
| **Sessions**      | List/manage all sessions in the project; bulk mark-read / delete. |
| **Members**       | Invite users, set Owner/User/Viewer, team access. For a My space project there are no share controls: **Only you can open this project**, and, except on the Home, **Move to Projects** — one-way, after which it stays **Invited only** and can be shared. The Home can never be moved. |
| **Project**       | Name, slug, description, **Visibility** (**Invited only** / **Everyone in \<Org\>**), delete. For an org Owner in a business organization (not on a My space project), a **Company project** card with **Mark as company project** / **Unmark as company project**: it changes only where the project is listed (under **Company**), not who can open it. |
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
| **Shares**   | Session visibility (**Everyone in the project** / **Invited only**) and public file-preview links. |

In the user's own assistant session, a **Your assistant** card above the tabs links to the
**Your assistant** page.

## Organization settings

Path: `/<org>/settings` (org Owners and platform admins). Tabs: **Organization**
(branding), **Users** (invite/roles/revoke), **Teams**, **Providers** (org-level connectors:
Git / Files / Transport, with UserDelegation or ServiceAccount credentials), **AI** (org-wide
AI defaults: available clouds and the driver/provider/model defaults inherited by all
projects), **AI Providers** (model endpoints: Anthropic/OpenAI/Custom,
scoped Global/Org/Project, delivered direct or via a cloud transport — AWS Bedrock, GCP
Vertex, Azure AI Foundry, custom gateway — with per-config health checks; a **Claude
subscription** seat is bound by pasting the output of `claude setup-token`, with the
credentials-file upload as the alternative), **Classifiers** (classifier providers — see below), **Costs**,
**Deployment Targets**, **Catalog** (manage reusable assets/skills, assign to
teams/projects), and, in a business organization, **Assistants** (below). The org sessions
report is not a tab — it is **Sessions** in the org kebab.

**Former members' My space** is a tab reached only by link: while projects that people
left behind in their My space are waiting, the **Users** tab shows a callout with
**Review**. Each listed project offers **Take over** (it moves to Projects, **Invited
only**, with that Owner as its owner) or **Delete** (with its sessions and files; cannot be
undone). Nobody can open those projects until then. Homes are never listed.

### Assistants

**Settings > Assistants** (business organizations only) governs members' assistants:

- **Assistant reach** — **Default for every member**: how far each member's home
  assistant (the one in their Private workspace) may reach into this organization:
  **Off** (cannot see the organization at all), **Coordinate** (sees the member's projects
  and sessions here, names only, and may message them; the default) or **Full** (may also
  read and write files and act with the member's authority, behind the usual approvals).
  Lowering from **Full** asks for confirmation and revokes the standing approvals that
  assistants held here.
- **Members** — per member, a reach below the default (or **Organization default**), and
  whether the organization sponsors their Private workspace (**Sponsor private seats for
  new members** sets the default). A sponsored member without a Private workspace gets one
  on their next visit.
- **My space storage** — **Store Homes** **On Skaile** or **In a SharePoint folder for each
  member**; it applies to Homes created from then on.

A member's own assistant in this organization (the one in their Home here) always works
in this organization; reach governs only the home assistant coming in from outside.

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
Work and Private spaces (the switch, sidebar sections, launcher, first run, Assistants and
Former members' My space tabs, Company mark, Archived spaces, the read-only notice):
`workspace-mode-toggle/`, `workspace-explorer/sidebar-projects-tree.helpers.ts`
(`buildSectionRows`), `assistant-launcher/`, `onboarding/first-run-onboarding-gate.tsx`,
`pages/settings/` (`org-assistants-settings.page.tsx`, `orphaned-my-space.page.tsx`,
`my-assistant-reach-card.tsx`, `archived-spaces-card.tsx`, `preferences.page.tsx`),
`pages/projects/my-space-project-controls.tsx`, `private-status-notice/`, and backend
`landing.utils.ts`; platform `main` @ `7687851fb` (2026-10-02), with the rollout flag on.
The **Report a problem** review-or-send-directly choice: platform `main` @ `c59fd243b` (2026-09-28).
