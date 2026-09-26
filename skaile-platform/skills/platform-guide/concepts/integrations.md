# Integrations: Connecting External Systems

How a project gets access to outside data and systems. Use this to explain what a connector
is, why a connection might require a sign-in, and what "read-only" means.

## ProviderLink — the connection record

External provider connections are managed per-organization as **ProviderLinks**. Each
declares:

- **Category**: Git / Files / Transport / Chat / Mail.
- **Provider type**: GitHub, GitLab, Bitbucket, SharePoint, Google Drive, S3, SSH,
  WebDAV, NextCloud, Box, Exchange (mail), and remote MCP servers. **Dropbox is work in progress — not usable yet**: it still
  appears in some provider pickers, but no runtime driver exists, so a Dropbox
  connection cannot bring files into any session today. Say that plainly and steer the
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
  picks account and folder themselves. Agent-proposed drive mounts are **read-only**;
  read-write needs the user's own **Connect** flow in the Connectors panel. Either way,
  verify afterwards (`connector_list`) and propose a restart if the mount has not
  attached yet.

## Access levels and policy

Each connector, per project/asset, has an access level: read-write, read-only, or blocked.
The platform's connector runtime enforces, at call time, "can this asset, in this session,
run by this user, do this action on this system?" — plus audit logging of every call.

The Connect dialog's **Access** selector defaults to **read-only**. An existing mount
has no settings dialog — to change its folder or access level, the user removes it in
the Connectors panel and re-creates it via **Connect**.

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

- Reading mail is not approval-gated. Moving, filing into folders, and categorising need
  approval; deleting moves to Deleted Items and is privileged.
- Drafts need no approval — the user reviews and sends them from Outlook. Sending from
  the agent is approved per message; only sends from the user's own mailbox can be
  covered by a standing project-level approval.
- Calendar access covers the user's own mailbox only.
- **Shared mailboxes**: the user adds them once per connection in **My connections**,
  after granting **Allow shared mail access**. A mailbox is admitted only if the user's
  account can actually open its inbox (Full Access). The project owner then enables it
  per project. Sends from a shared mailbox are approved every time.
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
limit resets, and the chat shows a notice when a seat is parked. OpenAI / Codex profiles
may appear where a deployment has enabled them.

Classifier providers (the models behind flow classifier steps) are configured separately
under organization settings, **Classifiers** — see `concepts/flows.md`.

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
(agent-proposed mounts), `configure-instance-modal.tsx` (Access default), platform PRs
#5337/#5357 (Reconnect), #5364 (shared mailboxes per org), #4703 (setup-token seats),
#5099/#5109/#5139 (seat health and routing), #5305 (classifier providers).
