# Integrations: Connecting External Systems

How a project gets access to outside data and systems. Use this to explain what a connector
is, why a connection might require a sign-in, and what "read-only" means.

## ProviderLink — the connection record

External provider connections are managed per-organization as **ProviderLinks**. Each
declares:

- **Category**: Git / Files / Transport.
- **Provider type**: GitHub, GitLab, Bitbucket, SharePoint, Google Drive, S3, SSH,
  WebDAV, NextCloud, Box. **Dropbox is work in progress — not usable yet**: it still
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
  When a session gains multiple members, access can shift from the owner's delegated
  credentials toward a service account (or shared delegation with the owner's
  acknowledgment).

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

- Mounts run on the **session owner's** connection. Connecting *your* account never
  gives a session owned by someone else access to it.
- The agent cannot *silently* create the configured connector — `platform.enable_asset`
  only enables config-less assets or existing configured presets. What the agent can do
  is **propose** one for the owner to approve: a complete non-secret mount as a single
  approval card, or a configuration handoff that parks on a trusted page where the
  owner picks account and folder themselves (see
  `references/control-plane-capabilities.md` for both). Agent-proposed mounts are
  always **read-only**; read-write needs the user's own **Connect** flow in the
  Connectors panel. Either way, verify afterwards (`connector_list`) and propose a
  restart if the mount has not attached yet.

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

## Mounts vs. connectors (recap)

- **Mounts** = external data surfaced as **files** in the workspace (git, local, S3,
  WebDAV/NextCloud, SharePoint, Google Drive, Box). The project's primary data source is
  a mount; the workspace **Connectors** panel manages additional ones.
- **Connectors** = external systems surfaced as **tools** (Postgres, Redis, SQLite, the
  `session`/`presence` state stores). Note the naming overlap: the workspace panel
  called **Connectors** manages file *mounts*.

Source of truth: `platform/docs/integration_architecture.md`.
