---
name: mslearn
description: "Use when an agent needs current, official Microsoft documentation -
  Azure, Microsoft 365, .NET, Windows, PowerShell, SQL Server, Power Platform and
  the rest of Microsoft Learn - to ground an answer, find an official code sample,
  or read a full doc page as Markdown. Wraps Microsoft's hosted Learn MCP server
  (learn.microsoft.com/api/mcp) over streamable HTTP. Open server: no sign-in, no
  key, read-only, so it is safe to put in any job description or agent fleet."
version: 0.1.0
transport: http
url: https://learn.microsoft.com/api/mcp
keywords:
  - microsoft
  - azure
  - learn
  - documentation
  - docs
  - mcp
  - remote
  - read-only
---

# Microsoft Learn MCP Server (remote)

Microsoft's official, Microsoft-hosted MCP server over the Microsoft Learn
documentation. This is a **remote** asset: there is no binary or Docker image to
package - the runner opens a streamable-HTTP connection straight to
`https://learn.microsoft.com/api/mcp`. It needs **no credentials**: there is no
sign-in, no key, and nothing user-specific, so every session gets the same public
documentation.

## When to reach for this

- The task touches a Microsoft product (Azure services, Entra ID, Microsoft 365 /
  Graph, .NET / C#, PowerShell, Windows, SQL Server, Power Platform, Dynamics) and
  the answer must match current official docs rather than training data.
- The agent needs an official code sample for a Microsoft API or SDK.
- The agent has a `learn.microsoft.com` URL and needs the full page content.

Because it is open and read-only, it is a good default in **job descriptions** for
agents that write or review code against Microsoft stacks: adding it grants no
access to anything private.

## Tool surface

The server advertises its toolset at connect time (`tools.listChanged: true`, so it
can change); probed on 2026-11-14 it offered three tools, all annotated
`readOnlyHint: true`:

- `microsoft_docs_search` - search Microsoft Learn; returns up to 10 content chunks
  (each at most about 500 tokens) with title, URL and excerpt. Use first.
- `microsoft_code_sample_search` - search official code samples (up to 20); the
  optional `language` argument filters by language.
- `microsoft_docs_fetch` - fetch one Learn page and return it as Markdown. Use after
  search when the excerpts are not enough.

The server also sends `initialize` instructions describing this search -> code
sample -> fetch workflow, so the client sees the flow without this body.

## Non-obvious gotchas the agent must respect

- **No identity, no private data.** The server only serves public Learn content. It
  cannot read a tenant, subscription or any customer resource - for that the agent
  needs an authenticated Azure / Graph tool, not this one.
- **Read-only.** None of the tools write anything; there is no side effect to
  confirm with the user.
- **Search returns excerpts.** Chunks are cut at about 500 tokens; quote a setting,
  limit or command only after confirming it in the full page with
  `microsoft_docs_fetch`, and cite the page URL.
- **Rate limits.** Microsoft documents no quota for anonymous use. The endpoint is
  shared and can throttle; on an HTTP 429 back off instead of retrying in a loop.
- **Network egress required.** The session sandbox must be able to reach
  `learn.microsoft.com`. Under locked-down session networking the connection fails
  fast; that is a config issue, not a tool bug.

## Transport and authentication

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport` - the same path `github/` and `ideogram/` ship on.
The server returns an `mcp-session-id` on `initialize`; the client SDK carries it on
later requests.

This manifest carries **no** `auth` block and **no** `Authorization` header: the
endpoint is open, and there is no per-user OAuth or static key to bind.

Vendor documentation: <https://learn.microsoft.com/training/support/mcp>
