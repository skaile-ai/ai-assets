---
name: atlassian
description: "Use when an agent needs to read, search or change Jira work items,
  Confluence pages, Jira Service Management requests or Bitbucket repositories -
  look up an issue by key, run JQL or CQL, create or transition work items, comment,
  draft or update Confluence content. Wraps Atlassian's official hosted remote MCP
  server (Atlassian MCP v2, mcp.atlassian.com/v2/mcp) over streamable HTTP. Auth is
  per-user OAuth (the user signs in to their own Atlassian account and picks the
  sites to grant on first connect), so every call runs with that user's Jira and
  Confluence permissions, within the permission groups their org admin allows."
version: 0.1.0
transport: http
url: https://mcp.atlassian.com/v2/mcp
keywords:
  - atlassian
  - jira
  - confluence
  - jsm
  - bitbucket
  - rovo
  - mcp
  - remote
  - oauth
---

# Atlassian MCP Server (remote)

Atlassian's official, Atlassian-hosted MCP server (Atlassian MCP v2, formerly
"Atlassian Rovo MCP server"). This is a **remote** asset: there is no binary or
Docker image to package - the runner opens a streamable-HTTP connection straight to
`https://mcp.atlassian.com/v2/mcp`. Tools run as the connected Atlassian account,
on the Atlassian Cloud sites that account granted at sign-in.

## When to reach for this

- The user asks about a **Jira** work item, sprint, board, release or JQL query, or
  wants one created, edited, transitioned, commented on or time-logged.
- The user wants to **read, search (CQL), draft or update Confluence** pages, blog
  posts, live docs, comments or inline tasks.
- The user works a **Jira Service Management** queue (requests, approvals, on-call,
  alerts) or reads **Bitbucket Cloud** repositories linked to their Atlassian org.

## Tool surface

The server does **not** send its whole catalog at connect time. It advertises a
small set of `Primary` tools and a discovery pathway; everything else is reached
through it:

- `getAccessibleAtlassianResources` - lists the sites the user granted, with their
  `cloudId`. **Call it first**: every other tool needs a `cloudId`.
- `atlassianUserInfo` - the signed-in account.
- `discover` - natural-language search over the deferred tool catalog; returns the
  matching tools, their schemas and how to run them.
- `executeRead`, `executeWrite`, `executeDestructive` - run a tool found via
  `discover`. The split exists so a client can ask for confirmation on writes and
  deletes.
- Primary Jira/Confluence tools, e.g. `getJiraIssue`, `searchJiraIssuesUsingJql`,
  `createJiraIssue`, `editJiraIssue`, `transitionJiraIssue`,
  `addOrEditJiraIssueComment`, `getConfluenceContent`, `searchConfluence`,
  `createConfluenceContent`, `updateConfluenceContent`.

The live tool list is read at connect time and changes without a reconnect;
Atlassian groups tools into permission groups (`read_jira`, `write_jira`,
`search_jira`, `delete_jira`, `manage_jira`, `read_confluence`, `write_confluence`,
`search_confluence`, `read_jsm`, `write_jsm`, `read_bitbucket`, ...). Full list:
[Atlassian MCP supported tools](https://support.atlassian.com/atlassian-ai-gateway/docs/supported-tools/).

## Non-obvious gotchas the agent must respect

- **Per-user identity and per-user permissions.** On first connect the platform's
  remote-MCP OAuth path opens an Atlassian sign-in; every tool then runs as that
  user and sees only what their Jira/Confluence permissions allow. Two users of the
  same asset are two Atlassian accounts. A permission error on one project is the
  user's Atlassian access, not a tool bug.
- **The org admin decides which tool groups exist.** Organization admins grant or
  revoke access per permission group. `delete_jira` and `manage_jira` are **off by
  default**; if `discover` does not return a delete or project-admin tool, the admin
  has not enabled it - say so instead of hunting for a workaround.
- **Writes land in shared team spaces.** Creating, transitioning or commenting on a
  work item notifies watchers; Confluence edits create new page versions others see.
  Confirm the target (site, project key, page) and the change with the user before
  any `executeWrite`/`executeDestructive` call. Deletes cannot be undone.
- **Enriched tools spend Rovo credits.** Tools marked *Enriched* in Atlassian's list
  (cross-app search, infographics, interactive visualizations) consume the site's
  Rovo credits even when Rovo AI is turned off. Prefer the plain read/search tools.
- **Bitbucket needs a linked workspace.** Bitbucket tools only appear for workspaces
  linked to an Atlassian organization.
- **Network egress required.** The session sandbox must reach `mcp.atlassian.com`
  (and the OAuth flow `auth.atlassian.com`). Under locked-down session networking the
  connection fails fast; that is a config issue, not a tool bug.

## Transport

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport`
(`workspaces/packages/workspaces/runner/src/external-mcp.ts`), the same path
`github/` and `ideogram/` ship on. The URL is the **v2** endpoint; v1 endpoints are
being retired by Atlassian.

## Authentication (per-user OAuth, bound by the platform)

This manifest deliberately carries **no** `Authorization` header and **no** `auth`
block - the same shape as `mcp/ideogram/MCP.md`. The server answers an
unauthenticated `initialize` with `401` and a `WWW-Authenticate: Bearer
resource_metadata=...` challenge; its protected-resource metadata names the
authorization server `auth.atlassian.com/<tenant>`, which supports dynamic client
registration and PKCE (S256). The platform's remote-MCP OAuth path runs that flow
on first connect and stores the token off-disk. Atlassian also accepts API-token
auth, but that would be a static per-user secret this shared catalog entry does not
declare.

Vendor docs: [Atlassian MCP server](https://support.atlassian.com/atlassian-ai-gateway/docs/supported-tools/).
