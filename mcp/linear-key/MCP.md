---
name: linear-key
description: "Use when a fleet of agents (every hire of one job description) needs
  Linear access under one shared identity - read and change issues, projects,
  cycles, documents and comments. Wraps Linear's official hosted MCP server
  (mcp.linear.app/mcp) over streamable HTTP, authenticated with a Linear API key sent
  as a bearer token instead of the per-user OAuth sign-in. Every agent that receives
  this credential acts as the key's owner. For an individual person working as
  themselves, use the `linear` (per-user OAuth) entry instead."
version: 0.1.0
transport: http
url: https://mcp.linear.app/mcp
auth:
  type: bearer
  inject: header
  header: Authorization
  fields:
    - { key: apiKey, label: "Linear API key", secret: true }
keywords:
  - linear
  - issues
  - projects
  - project-management
  - api-key
  - fleet
  - mcp
  - remote
---

# Linear MCP Server, shared API key (remote)

Linear's official, Linear-hosted MCP server, reached with a static key instead of
OAuth. This is a **remote** asset: the runner opens a streamable-HTTP connection
straight to `https://mcp.linear.app/mcp` and sends the key as
`Authorization: Bearer <key>`, which Linear documents as the non-interactive
alternative to its OAuth flow ([MCP server FAQ](https://linear.app/docs/mcp)). The
toolset is advertised by the server at connect time.

## One key, many agents

The key is a single secret shared by **every recipient**: when a project owner gives
it to a job description, all hires of that job description act as the same Linear
user, with that user's access, and every issue, comment or status change they make is
attributed to that one user. Use this entry for agent fleets that need a common
identity. For a person working as themselves, use the per-user OAuth entry
**`linear`** instead: it is the default for individuals.

## Credential: a least-privilege API key

API keys are created under _Settings > Account > Security & Access_; an admin decides
whether members may create them (_Settings > Administration > API > Member API keys_),
see [API and webhooks](https://linear.app/docs/api-and-webhooks).

- Create the key on a dedicated workspace **member** account (not an admin) that has
  joined only the teams the fleet works in.
- Restrict the key itself: pick only the permissions the agents need (`Read` alone
  for a read-only fleet; add `Create issues` / `Create comments` / `Write` only where
  needed, never `Admin`) and limit it to specific teams.
- A key with only the `Read` permission makes the server read-only for that fleet;
  Linear documents this as its read-only route for API keys.

## Gotchas

- **401 `invalid_token`** means the key is wrong or revoked (or its owner left the
  workspace). Retrying will not help; replace the key.
- **Permission errors are the key's limits, not a bug.** A tool call outside the
  key's permissions or teams fails; widen the key only if the fleet really needs it.
- **Shared rate limit.** All hires spend the API rate limit of the one key's user.

## Vendor docs

- [Linear MCP server](https://linear.app/docs/mcp) (setup, FAQ "Can I authenticate with my own API keys or OAuth access tokens?")
- [API and webhooks: API keys](https://linear.app/docs/api-and-webhooks)
- [Linear developers](https://linear.app/developers)
