---
name: notion
description: "Use when an agent needs to search, read, create or edit content in
  Notion - find pages and meeting notes, fetch a page or database, draft or update
  pages, query and update databases and views, comment, upload files. Wraps Notion's
  official hosted remote MCP server (mcp.notion.com/mcp) over streamable HTTP. Auth
  is per-user OAuth (the user signs in to their own Notion account and picks the
  workspace on first connect), so every call sees only the pages that user can see
  and edits appear under their name."
version: 0.1.0
transport: http
url: https://mcp.notion.com/mcp
keywords:
  - notion
  - pages
  - databases
  - wiki
  - notes
  - mcp
  - remote
  - oauth
---

# Notion MCP Server (remote)

Notion's official, Notion-hosted MCP server ("Notion MCP"). This is a **remote**
asset: there is no binary or Docker image to package - the runner opens a
streamable-HTTP connection straight to `https://mcp.notion.com/mcp`. Tools run as
the connected Notion user, in the workspace they authorized.

## When to reach for this

- The user asks to **find or read** something in Notion: a page, a database, meeting
  notes, who owns a teamspace.
- The user wants Notion **changed**: draft a page, update content or properties,
  create a database or view, leave a comment or suggested edit, upload a file.

## Tool surface

The live tool list is read at connect time; per
[Notion's supported tools](https://developers.notion.com/guides/mcp/mcp-supported-tools)
it includes:

- Search and read: `notion-search`, `notion-fetch`, `notion-query-meeting-notes`,
  `notion-get-users`, `notion-get-teams`, `notion-get-self`,
  `notion-get-tool-access`.
- Pages: `notion-create-pages`, `notion-update-page`, `notion-move-pages`,
  `notion-duplicate-page`, `notion-delete-pages`.
- Databases and views: `notion-create-database`, `notion-query-data-sources`,
  `notion-update-data-source`, `notion-create-view`, `notion-update-view`.
- Collaboration: `notion-get-comments`, `notion-create-comment`,
  `notion-create-suggested-edit`, version history and restore tools.
- Files: `notion-create-file-upload`, `notion-get-file-download-urls`, folders.

Which tools and parameters a user gets depends on their workspace plan and on what
an admin allows; `notion-get-tool-access` reports it.

## Non-obvious gotchas the agent must respect

- **Per-user identity and page permissions.** On first connect the platform's
  remote-MCP OAuth path opens a Notion sign-in; every tool then runs as that user and
  sees exactly the pages they can see. A page that does not show up is usually not
  shared with them.
- **Plan and admin decide the toolset.** Some tools (e.g. AI search across
  connected sources, calendar tools) depend on the workspace plan; admins can limit
  tools on workspace-owned connections. Check `notion-get-tool-access` before
  promising a capability.
- **Writes are live in a shared workspace.** Page edits, moves and deletes happen
  immediately and are visible to everyone with access. Prefer
  `notion-create-suggested-edit` or a new draft page when the user did not ask for a
  direct edit; confirm before `notion-delete-pages`, `notion-move-pages` or bulk
  updates.
- **Rate limits.** Notion rate-limits MCP calls; on a rate-limit error reduce
  parallel calls and retry later instead of hammering
  ([rate limits](https://developers.notion.com/guides/mcp/rate-limits)).
- **Network egress required.** The session sandbox must reach `mcp.notion.com`
  (resource and authorization server are the same host). Under locked-down session
  networking the connection fails fast; that is a config issue, not a tool bug.

## Transport

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport`
(`workspaces/packages/workspaces/runner/src/external-mcp.ts`), the same path
`github/` and `ideogram/` ship on.

## Authentication (per-user OAuth, bound by the platform)

This manifest deliberately carries **no** `Authorization` header and **no** `auth`
block - the same shape as `mcp/ideogram/MCP.md`. An unauthenticated `initialize`
returns `401` with a `WWW-Authenticate: Bearer resource_metadata=...` challenge; the
protected-resource metadata names `https://mcp.notion.com` as authorization server,
which supports dynamic client registration and PKCE (S256). The platform's
remote-MCP OAuth path runs that flow on first connect and stores the token off-disk.

Vendor docs: [Notion MCP](https://developers.notion.com/guides/mcp/mcp-supported-tools).
