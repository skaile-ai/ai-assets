---
name: linear
description: "Use when an agent needs to find, create or update work in Linear -
  issues, projects, milestones, cycles, comments, documents, teams and users - for
  example to turn a plan into issues, post standup updates as comments, or summarize
  a cycle. Wraps Linear's official hosted remote MCP server (mcp.linear.app/mcp) over
  streamable HTTP. Auth is per-user OAuth (the user signs in to their own Linear
  account and workspace on first connect), so every call runs with that user's
  Linear permissions and shows up as their activity."
version: 0.1.0
transport: http
url: https://mcp.linear.app/mcp
keywords:
  - linear
  - issues
  - projects
  - cycles
  - project-management
  - mcp
  - remote
  - oauth
---

# Linear MCP Server (remote)

Linear's official, Linear-hosted MCP server. This is a **remote** asset: there is no
binary or Docker image to package - the runner opens a streamable-HTTP connection
straight to `https://mcp.linear.app/mcp`. Tools run as the connected Linear user, in
the one workspace they authorized.

## When to reach for this

- The user asks about **Linear issues, projects, milestones or cycles** - look one
  up, list what a team is working on, summarize a cycle, build a timeline.
- The user wants Linear **changed**: create an issue or sub-issues from a plan,
  update status/assignee/labels, add a comment, create or update a project or
  document.

## Tool surface

The live tool list is read at connect time. Per
[Linear's MCP docs](https://linear.app/docs/mcp) it covers finding, creating and
updating issues, projects, milestones, comments, documents, labels, teams, users and
cycles, plus searching Linear's own documentation. Linear adds tools regularly, so
enumerate them at connect time rather than hard-coding names.

## Non-obvious gotchas the agent must respect

- **Per-user identity, one workspace per sign-in.** On first connect the platform's
  remote-MCP OAuth path opens a Linear sign-in; every tool then runs as that user,
  with their Linear permissions (private teams they are not in stay invisible).
  The token is bound to the workspace chosen at sign-in; reconnecting does not switch
  workspace, so a user in two workspaces needs a separate authorization per
  workspace.
- **Writes are visible to the team at once.** Created issues, comments and status
  changes notify subscribers and appear in the team's views under the user's name.
  Show the user the issues/comments you plan to create or the fields you plan to
  change before writing, and never invent dependencies or assignees they did not ask
  for.
- **Read-only is a scope, not a promise of this entry.** The `/mcp` endpoint grants
  read+write by default (`scope="read write"` in its challenge). Linear also serves a
  read-only endpoint (`/mcp/readonly`); this entry is the read-write one.
- **Network egress required.** The session sandbox must reach `mcp.linear.app`
  (resource and authorization server are the same host). Under locked-down session
  networking the connection fails fast; that is a config issue, not a tool bug.

## Transport

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport`
(`workspaces/packages/workspaces/runner/src/external-mcp.ts`), the same path
`github/` and `ideogram/` ship on. Linear's `/sse` endpoint is deprecated; do not
use it.

## Authentication (per-user OAuth, bound by the platform)

This manifest deliberately carries **no** `Authorization` header and **no** `auth`
block - the same shape as `mcp/ideogram/MCP.md`. An unauthenticated `initialize`
returns `401` with a `WWW-Authenticate: Bearer resource_metadata=...` challenge; the
protected-resource metadata names `https://mcp.linear.app` as authorization server,
which supports dynamic client registration and PKCE (S256). The platform's
remote-MCP OAuth path runs that flow on first connect and stores the token off-disk.
Linear also accepts a personal API key as bearer token, but that would be a static
per-user secret this shared catalog entry does not declare.

Vendor docs: [Linear MCP server](https://linear.app/docs/mcp).
