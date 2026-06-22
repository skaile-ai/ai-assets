# Integrations: Connecting External Systems

How a project gets access to outside data and systems. Use this to explain what a connector
is, why a connection might require a sign-in, and what "read-only" means.

## ProviderLink — the connection record

External provider connections are managed per-organization as **ProviderLinks**. Each
declares:

- **Category**: Git / Files / Transport.
- **Provider type**: GitHub, GitLab, Bitbucket, SharePoint, Google Drive, S3, Dropbox,
  SSH, WebDAV, NextCloud, Box.
- **Credential mechanism**: how auth works (see below).
- **App owner**: Org (customer-registered app) or Skaile (vendor-managed).

## Two auth modes

- **User Delegation** — the user signs in to the provider themselves (OAuth, or supplies a
  PAT). The platform stores that credential per user and injects the **session owner's**
  credential into the container at session start. This is why some connections need the
  user to click "Connect" and complete an OAuth flow in the browser — the agent cannot do
  that step for them.
- **Service Account** — a shared credential registered by IT/admin, used for all access.
  When a session gains multiple members, access can shift from the owner's delegated
  credentials toward a service account (or shared delegation with the owner's
  acknowledgment).

## Access levels and policy

Each connector, per project/asset, has an access level: read-write, read-only, or blocked.
The platform's connector runtime enforces, at call time, "can this asset, in this session,
run by this user, do this action on this system?" — plus audit logging of every call.

Practical rules for the agent:
- Respect read-only connectors and read-only mounts — never attempt a write.
- If an action needs a permission the agent is unsure the user has, ask rather than assume.
- Never send the user's data to an external service without explicit permission.

## Mounts vs. connectors (recap)

- **Mounts** = external data surfaced as **files** in the workspace (git, local, S3,
  WebDAV, SharePoint). The project's primary data source is a mount.
- **Connectors** = external systems surfaced as **tools** (Postgres, Redis, SQLite, the
  `session`/`presence` state stores).

Source of truth: `platform/docs/integration_architecture.md`.
