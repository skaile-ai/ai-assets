---
name: intercom
description: "Use when an agent needs to read a US-hosted Intercom workspace - search
  and read conversations and contacts, look up companies, search and read Help Center
  articles - or make the few writes it allows: create or update articles and add
  internal notes to conversations. Wraps Intercom's official hosted remote MCP server
  (mcp.intercom.com/mcp) over streamable HTTP. Auth is per-user OAuth (a teammate
  signs in to their Intercom workspace on first connect), so every tool acts as that
  teammate with their workspace permissions. EU-hosted workspaces use a different
  endpoint and are not covered by this entry."
version: 0.1.0
transport: http
url: https://mcp.intercom.com/mcp
keywords:
  - intercom
  - support
  - conversations
  - contacts
  - help-center
  - mcp
  - remote
  - oauth
---

# Intercom MCP Server (remote)

Intercom's official, Intercom-hosted MCP server for **US-hosted** workspaces. This is
a **remote** asset: there is no binary or Docker image to package - the runner opens
a streamable-HTTP connection straight to `https://mcp.intercom.com/mcp`. Tools act as
the teammate who authorized the connection.

## When to reach for this

- The user asks about **customer conversations**: find conversations by state,
  source, assignee or text, read a thread, or summarize what customers report.
- The user wants **contact or company** details.
- The user wants to **find, read, draft or update Help Center articles**, or leave an
  **internal note** on a conversation.

## Tool surface

The exact toolset is advertised by the server at connect time; Intercom's
[docs](https://developers.intercom.com/docs/guides/mcp) list 14 tools:

- Universal: `search` (query DSL; needs `object_type:conversations` or
  `object_type:contacts`), `fetch` (full record by prefixed ID).
- Conversations and contacts: `search_conversations`, `get_conversation`,
  `search_contacts`, `get_contact`.
- Companies: `list_companies`, `get_company`.
- Help Center: `list_articles`, `search_articles`, `get_article`, `create_article`,
  `update_article`.
- Notes: `add_internal_note`.

Workspaces with Fin Operator also get `send_operator_message`,
`get_operator_response`, `get_proposal_details`, `apply_proposals` and
`reject_proposals`, only on OAuth connections and only for teammates allowed to use
Operator.

## Non-obvious gotchas the agent must respect

- **US region only.** This URL serves US-hosted workspaces (`app.intercom.com`).
  EU-hosted workspaces (`app.eu.intercom.com`) need `https://mcp.eu.intercom.com/mcp`;
  AU-hosted workspaces have no MCP server yet. A sign-in that cannot find the
  workspace usually means the wrong region.
- **Per-user identity.** On first connect the platform's remote-MCP OAuth path opens
  an Intercom consent page, then Intercom's own authorization screen; every tool then
  runs as *that* teammate. Notes and articles are attributed to them.
- **Customer personal data.** Conversations and contacts hold names, emails and
  message content of real customers. Quote only what the task needs.
- **Write-tool caution.** `create_article` defaults to draft; publishing
  (`state: published`) or `update_article` on a live article changes the public Help
  Center - confirm first. `add_internal_note` is teammate-only and never reaches the
  customer, but a note on a snoozed conversation assigned to someone else reopens
  it. `apply_proposals` changes the workspace's live Fin configuration, and
  `send_operator_message` spends Operator credits.
- **Missing write permission.** A 401 on `add_internal_note` usually means the
  connection predates the "Write conversations" permission; disconnect and reconnect.
- **Pagination.** Search tools return 5 items by default (up to 150); follow the
  `starting_after` cursor instead of raising limits blindly.
- **Network egress required.** The session sandbox must reach `mcp.intercom.com`.

## Transport

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport`, the same path `github/` and `ideogram/` ship on.
Intercom's legacy SSE endpoint (`/sse`) is deprecated and not used.

## Authentication (per-user OAuth, bound by the platform)

This manifest carries **no** `Authorization` header and **no** `auth` block - the
same shape as `mcp/ideogram/MCP.md`. Intercom publishes protected-resource metadata
and supports dynamic client registration on `mcp.intercom.com`; the platform's
remote-MCP OAuth path handles the browser sign-in and token storage. (Intercom also
accepts a bearer access token; this entry does not use that route, and bearer
connections do not get the Fin Operator tools.)

Vendor docs: <https://developers.intercom.com/docs/guides/mcp>
