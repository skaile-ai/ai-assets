---
name: atlassian-key
description: "Use when a fleet of agents (every hire of one job description) needs
  Jira, Confluence or other Atlassian Cloud access under one shared, non-personal
  identity. Wraps Atlassian's official hosted Rovo MCP server
  (mcp.atlassian.com/v2/mcp) over streamable HTTP, authenticated with an Atlassian
  service account API key sent as a bearer token instead of the per-user OAuth
  sign-in. Every agent that receives this credential acts as that one service
  account. For an individual person working as themselves, use the `atlassian`
  (per-user OAuth) entry instead."
version: 0.1.0
transport: http
url: https://mcp.atlassian.com/v2/mcp
auth:
  type: bearer
  inject: header
  header: Authorization
  fields:
    - { key: apiKey, label: "Atlassian service account API key", secret: true }
keywords:
  - atlassian
  - jira
  - confluence
  - rovo
  - api-key
  - service-account
  - fleet
  - mcp
  - remote
---

# Atlassian Rovo MCP Server, shared API key (remote)

Atlassian's official, Atlassian-hosted Rovo MCP server, reached with a static key
instead of OAuth. This is a **remote** asset: the runner opens a streamable-HTTP
connection straight to `https://mcp.atlassian.com/v2/mcp` and sends the key as
`Authorization: Bearer <key>`. The toolset is advertised by the server at connect
time.

## One key, many agents

The key is a single secret shared by **every recipient**: when a project owner gives
it to a job description, all hires of that job description act as the same Atlassian
identity, with the same permissions, and their actions show up under that one name
in Jira and Confluence history. Use this entry for agent fleets that need a common,
non-personal identity. For a person working as themselves, use the per-user OAuth
entry **`atlassian`** instead: it is the default for individuals.

## Credential: a service account API key (least privilege)

Atlassian documents two static-credential forms for this server
([Configuring authentication via API token](https://developer.atlassian.com/cloud/rovo-mcp/guides/configuring-authentication-via-api-token/)):

- **Service account API key, `Authorization: Bearer <key>`**. This is the form the
  `auth` block declares. An organization admin creates a
  [service account](https://support.atlassian.com/user-management/docs/manage-api-tokens-for-service-accounts/)
  and issues it a scoped API key.
- Personal API token, `Authorization: Basic base64(email:api_token)`. Not used here:
  it needs two values (the owner's email and the token) combined into one header,
  which a single-secret `auth` block does not express, and it makes every agent act
  as a real person.

Give the service account only what the fleet needs:

- Add it only to the Jira projects and Confluence spaces the agents work in, through
  groups or project/space roles, never org-admin or site-admin.
- Select only the `agent-interface` scopes for the apps and permission groups in use
  (e.g. `read:jira:agent-interface` + `search:jira:agent-interface` for a read-only
  Jira fleet; add `write:*` only where the agents must change data).

## Gotchas

- **Admin must enable API-token auth.** Authentication via API token is off unless
  the organization admin allows it in the
  [Rovo MCP server settings](https://support.atlassian.com/security-and-access-policies/docs/control-atlassian-rovo-mcp-server-settings/).
  While it is off the server rejects the key and only OAuth 2.1 works.
- **No bound cloud ID.** Unlike an OAuth token, the key is not tied to one Atlassian
  site, so pass `cloudId` explicitly wherever a tool asks for it (the site's cloud ID
  is in `https://<site>.atlassian.net/_edge/tenant_info`).
- **Fewer tools than OAuth.** Some tools need scopes that cannot be selected for an
  API key; code search and Teams tools work only over OAuth 2.1.
- **Only the IP allowlist applies.** There is no redirect URI, so domain allowlists do
  not restrict key-based calls; the organization's IP allowlist still does.
- **401 with `invalid_token`** means the key is wrong, expired or revoked, or the
  admin has not enabled API-token auth. Retrying will not help; the key or the setting
  has to change.

## Vendor docs

- [Configuring authentication via API token](https://developer.atlassian.com/cloud/rovo-mcp/guides/configuring-authentication-via-api-token/)
- [Authentication and authorization](https://developer.atlassian.com/cloud/rovo-mcp/guides/authentication-and-authorization/)
- [Supported tools and scopes](https://developer.atlassian.com/cloud/rovo-mcp/guides/supported-tools/)
- [Manage API tokens for service accounts](https://support.atlassian.com/user-management/docs/manage-api-tokens-for-service-accounts/)
