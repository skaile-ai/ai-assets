# Integrations: Connecting External Systems

How a project gets access to outside data and systems. Use this to explain what a connector
is, why a connection might require a sign-in, and what "read-only" means.

## ProviderLink — the connection record

External provider connections are managed per-organization as **ProviderLinks**. Each
declares:

- **Category**: Git / Files / Transport / Chat / Mail.
- **Provider type**: GitHub, GitLab, Bitbucket, SharePoint, Google Drive, S3, SSH,
  WebDAV, NextCloud, Box, Exchange (mail), and remote MCP servers. **Dropbox is work in progress — not usable yet**: no
  runtime driver exists, so it is no longer offered in **Providers** or **My Connections**,
  and a Dropbox provider an admin added earlier still cannot bring files into any session. Say that plainly and steer the
  user to Box, SharePoint, Google Drive or NextCloud instead; never walk them into
  creating a Dropbox provider.
- **Credential mechanism**: how auth works (see below).
- **App owner**: Org (customer-registered app) or Skaile (vendor-managed).

## Two auth modes

- **User Delegation** — the user signs in to the provider themselves (OAuth, or supplies a
  PAT). The platform stores that credential per user and injects the **session owner's**
  credential into the container at session start. This is why some connections need the
  user to click "Connect" and complete an OAuth flow in the browser. The assistant cannot
  enter the credential, but it can **start** the setup and hand the user one expiring link
  for the part only they can do — see the handoff contract in `concepts/agent.md`. Never
  ask for or accept a token, password, or OAuth code in chat.
- **Service Account** — a shared credential registered by IT/admin, used for all access.

When a stored sign-in has expired or been revoked (or a GitHub App installation is gone),
provider pickers show **Reconnect** — or **Connect account** when the user has no
credential on that connection yet — linking to **My connections**.

## From a connection to files in a session

A personal **My Connections** sign-in stores a credential — it does **not** by itself
put any files anywhere. Three separate objects are involved: the org's **ProviderLink**
(the app registration), the user's **connection** (their credential on that link), and
a **configured connector/mount** on a project or session (which folder, which access).
Users routinely finish the OAuth and then ask why the agent still sees nothing; the
missing piece is always the third object. After a successful Connect, guide them to one
of the two places that create it:

1. **New project from that source** — the project wizard (**New project**) → **Source** step → pick
   the provider (SharePoint / Google Drive / NextCloud / Box / Git / Local Folder) and
   the folder. That folder then *is* the project workspace.
2. **Add a connector to an existing session or project** — in the session workspace,
   open the **Connectors** panel (its icon sits with the other panel icons at the top
   right of the workspace) → use the connect offer for the provider (e.g. **Connect
   Box**) → choose the account/connection and the folder → add for **This session** or
   **Whole project**. New mounts attach on the next session reload/restart — the panel
   prompts for it.

Two rules worth repeating to users:

- A project's cloud-drive mounts (SharePoint / OneDrive, Google Drive, Box, NextCloud)
  run on the **project owner's** connection, whoever added them; adding one is refused
  when the project owner has no usable connection for it. Connectors assigned through the
  organization library run on the **session owner's** connection. Connecting *your*
  account never gives a project or session owned by someone else access to it.
- The agent cannot *silently* create the configured connector — `platform.enable_asset`
  only enables config-less assets or existing configured presets. What the agent can do
  is **propose** one for the owner to approve: a complete non-secret mount (Box,
  SharePoint, Google Drive, or Git) as a single approval card for this session or the
  whole project, or a configuration handoff that parks on a trusted page where the owner
  picks account and folder themselves. Agent-proposed drive mounts (Box, SharePoint,
  Google Drive) are **read-only**; read-write needs the user's own **Connect** flow in the
  Connectors panel. An agent-proposed Git mount is read-write, like any Git connection. Either way,
  verify afterwards (`connector_list`) and, if the mount has not attached yet, call
  `platform.cycle_session` — no approval card, refused unless the person asking is an Owner of the
  session or its project, and it restarts only once your turn has ended, so finish your reply
  and check the mount on the next one (`references/control-plane-capabilities.md`).

## Access levels and policy

Each connector, per project/asset, has an access level: read-write, read-only, or blocked.
The platform's connector runtime enforces, at call time, "can this asset, in this session,
run by this user, do this action on this system?" — plus audit logging of every call.

For drive and folder mounts (SharePoint / OneDrive, Google Drive, Box, NextCloud, Local
Folder) the Connect dialog shows a **Read-only** switch once a folder is picked. It is off
by default, so a mount the user connects is read-write unless they turn it on. A Git
connection has no such switch: it is always read-write today, and the platform has no
read-only Git mount. Never tell a user a Git repo can be connected read-only.

Only the **workspace** mount (the project's source: the folder or repo picked in the project
wizard's **Source** step) has an editor on the **Mounts** card. Its pencil (**Edit mount**)
is on that card in **Project settings → Session defaults** (project Owners; the default for
new sessions, existing sessions keep theirs) and in **Session settings → Config** (this
session only). The editor changes its driver, folder (with **Browse**), target path,
credential, access level (a Git workspace is always read-write), the watch switch, and the
Git sync options. Every other mount is listed on the same card with only **Remove**, plus an
account picker for a linked cloud drive. To change such a mount's folder or access level,
the user removes it on the **Mounts** card and connects it again from the workspace
**Connectors** panel (it attaches on the next reload/restart). The raw Skaile config editor
(the workspace **Config** panel) can change any mount, but that is for users who edit YAML.

Practical rules for the agent:
- Respect read-only connectors and read-only mounts — never attempt a write.
- If an action needs a permission the agent is unsure the user has, ask rather than assume.
- Never send the user's data to an external service without explicit permission.

## Exchange mail

Exchange (Outlook mail and calendar) is a native **Mail** connection with its own
Microsoft app registration. Two separate things must both be true before an agent sees a
mailbox: the user has a **connection** (My connections), and the **project owner** has
enabled Exchange for the project — off by default, and only the actual project owner can
switch it (not a co-owner or platform admin). With several connections or shared
mailboxes, the owner picks which mailboxes the project may use.

- Reading mail is not approval-gated, and neither is flagging or tagging a mail with a
  category. Moving, filing into folders, and creating or deleting a category need approval;
  deleting a mail moves it to Deleted Items and is privileged.
- Drafts need no approval — the user reviews and sends them from Outlook. Sending from
  the agent is approved per message unless a standing approval covers it: for the user's own
  mailbox, one pinned to that mailbox or the project; for a shared mailbox, one pinned to that
  mailbox.
- Calendar access covers the user's own mailbox only.
- **Shared mailboxes**: the user adds them once per connection in **My connections**,
  after granting **Allow shared mail access**. A mailbox is admitted only if the user's
  account can actually open its inbox (Full Access). The project owner then enables it
  per project. If that sign-in is refused because the tenant lets only admins approve
  apps, the **not granted** notice offers an **administrator approval link** (valid for 7
  days) to send to an admin; after they approve, the user grants **Allow shared mail
  access** again themselves.
- Filing mail attachments into SharePoint is the agent combining steps (read the
  attachment, write it to a SharePoint mount) — there is no automatic filing rule.
- When the grant expires, the agent tells the user to reconnect.

## AI providers

The models the agents run on are configured under the organization's settings, **AI**
section, **AI Providers** tab, at global, organization, or project scope. A Claude
subscription seat can be connected by pasting a token from `claude setup-token` (the
default) or a credentials file. A setup-token seat does not refresh itself: when it stops
working, the owner re-runs `claude setup-token` and pastes the new token. Each credential
shows a health status (healthy, rate limited, authentication failed, or unreadable — the
last means re-enter it). Seats that hit their usage limit are routed around until the
limit resets, and the chat shows a notice when a seat is parked.

Classifier providers (the models behind flow classifier steps) are configured separately
under organization settings, **Classifiers** — see `concepts/flows.md`.

## Word and Excel tools

Every organization has the **word** and **excel** MCP servers as recommended company
defaults, so a session normally has tools for creating, editing and reviewing `.docx` and
`.xlsx` files in place (the Excel tools also open `.xlsm`). Use them for any Word or Excel
file rather than writing a script or editing the file's XML yourself. File paths you pass
to these tools must be paths inside the session workspace, not paths on the user's
computer. If they are missing from a session, someone opted out: an org Owner manages them
under organization settings, **Catalog**, in **Company defaults** (**Unpin** removes one
from the defaults; it stays in the organization's catalog and can be pinned again) or
hides them for the whole organization with **Filter rules**, which is the lasting
opt-out. A member can turn one off for a single session only (not the whole project) from
its row in the workspace **AI Assets** panel (see `ui/workspace.md`). PowerPoint is
available in the catalog but is not a company default yet.

## Mounts vs. connectors (recap)

- **Mounts** = external data surfaced as **files** in the workspace (git, local, S3,
  WebDAV/NextCloud, SharePoint, Google Drive, Box). The project's primary data source is
  a mount; the workspace **Connectors** panel manages additional ones.
- **Connectors** = external systems surfaced as **tools** (Postgres, Redis, SQLite,
  Exchange mail, the `session`/`presence` state stores). Note the naming overlap: the workspace panel
  called **Connectors** manages file *mounts*.

Source of truth: `platform/docs/integration_architecture.md`,
`platform/docs/mount-connection-binding.md` (owner invariant), `platform/docs/exchange-connector.md`,
`platform/docs/ai-provider-credential-lifecycle.md`, `connector-mount-provisioning.ts`
(agent-proposed mounts), `configure-instance-modal.tsx` and `configure-registry.tsx`
(Read-only switch; Git pinned read-write), platform PRs
#5337/#5357 (Reconnect), #5364 (shared mailboxes per org),
#5531 (shared-mail admin approval link), #4703 (setup-token seats),
#5099/#5109/#5139 (seat health and routing), #5305 (classifier providers),
#6133 (cycle_session without an approval card), #6382 (Word and Excel as company
defaults).
