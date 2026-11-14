---
name: monday
description: "Use when an agent needs to read or change work in monday.com - find
  items on a board, read a board's columns and groups, create or update items, post
  updates, create boards, groups and columns, look up users and teams, or work with
  WorkForms. Wraps monday.com's official hosted remote MCP server
  (mcp.monday.com/mcp) over streamable HTTP. Auth is per-user OAuth (the user signs in
  to their own monday.com account on first connect), so every call runs with that
  user's board permissions, within the workspaces the account admin allows for the
  monday MCP app."
version: 0.1.0
transport: http
url: https://mcp.monday.com/mcp
keywords:
  - monday
  - boards
  - items
  - work-management
  - project-management
  - mcp
  - remote
  - oauth
---

# monday.com MCP Server (remote)

monday.com's official, monday-hosted MCP server ("monday MCP"). This is a
**remote** asset: there is no binary or Docker image to package - the runner opens a
streamable-HTTP connection straight to `https://mcp.monday.com/mcp`. Tools run as the
connected monday.com user, in their account.

## When to reach for this

- The user asks about **items on a monday.com board** - find them by name, read
  their column values, see a board's structure.
- The user wants monday.com **changed**: create or update an item, move it to
  another group, post an update, create a board, group or column, create a form.
- The user needs **who is who** in the account (users and teams).

## Tool surface

The live tool list is read at connect time. Per
[monday's MCP repository](https://github.com/mondaycom/mcp) it includes:

- Items: `create_item`, `delete_item`, `get_board_items_by_name`, `create_update`,
  `change_item_column_values`, `move_item_to_group`.
- Boards: `create_board`, `get_board_schema`, `create_group`, `create_column`,
  `delete_column`.
- Account: `list_users_and_teams`.
- WorkForms: `create_form`, `get_form`.

The hosted server may expose more (or differently named) tools than the open-source
package; trust the connect-time list.

## Non-obvious gotchas the agent must respect

- **Per-user identity and board permissions.** On first connect the platform's
  remote-MCP OAuth path opens a monday.com sign-in; every tool then runs as that user
  and sees only boards they can access. Account admins install the monday MCP app
  from the marketplace and can **limit it to specific workspaces**; a board missing
  from results may be outside those.
- **Read the schema before writing.** Column values are typed per column; call
  `get_board_schema` before `create_item` or `change_item_column_values` and use
  the real column IDs instead of guessing.
- **Write-tool caution.** Item, column and board changes are live for the whole team
  and trigger the board's automations and notifications. `delete_item` and
  `delete_column` are destructive (a deleted column takes every item's value with
  it). Confirm with the user before any delete or bulk change.
- **API complexity limits.** monday.com rate-limits its API by query complexity per
  account; on a limit error slow down and retry later.
- **Network egress required.** The session sandbox must reach `mcp.monday.com` (and
  the OAuth flow `auth.monday.com`). Under locked-down session networking the
  connection fails fast; that is a config issue, not a tool bug.

## Transport

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport`
(`workspaces/packages/workspaces/runner/src/external-mcp.ts`), the same path
`github/` and `ideogram/` ship on.

## Authentication (per-user OAuth, bound by the platform)

This manifest deliberately carries **no** `Authorization` header and **no** `auth`
block - the same shape as `mcp/ideogram/MCP.md`. An unauthenticated `initialize`
returns `401` with a `WWW-Authenticate: Bearer resource_metadata=...` challenge; the
protected-resource metadata names `https://auth.monday.com/mcp` as authorization
server, which supports dynamic client registration and PKCE (S256). The platform's
remote-MCP OAuth path runs that flow on first connect and stores the token off-disk.

Vendor docs: [Get started with monday MCP](https://support.monday.com/hc/en-us/articles/28515034903314-Get-started-with-monday-MCP).
