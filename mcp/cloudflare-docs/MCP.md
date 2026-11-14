---
name: cloudflare-docs
description: "Use when an agent needs current, official Cloudflare documentation -
  Workers, Pages, R2, D1, KV, Durable Objects, Queues, Workers AI, Zero Trust, DNS,
  WAF and the rest of developers.cloudflare.com - to ground an answer, look up a
  limit or wrangler setting, or plan a Pages-to-Workers migration. Wraps
  Cloudflare's hosted documentation MCP server (docs.mcp.cloudflare.com/mcp) over
  streamable HTTP. Open server: no sign-in, no key, read-only, so it is safe to put
  in any job description or agent fleet."
version: 0.1.0
transport: http
url: https://docs.mcp.cloudflare.com/mcp
keywords:
  - cloudflare
  - workers
  - documentation
  - docs
  - mcp
  - remote
  - read-only
---

# Cloudflare Documentation MCP Server (remote)

Cloudflare's official, Cloudflare-hosted MCP server over the Cloudflare developer
documentation. This is a **remote** asset: there is no binary or Docker image to
package - the runner opens a streamable-HTTP connection straight to
`https://docs.mcp.cloudflare.com/mcp`. It needs **no credentials**: there is no
sign-in, no key, and nothing account-specific.

## When to reach for this

- The task touches a Cloudflare product (Workers, Pages, R2, D1, KV, Durable
  Objects, Queues, Workers AI, AI Gateway, Zero Trust, DNS, WAF, CDN) and the
  answer must match current docs: limits, pricing models, `wrangler` configuration,
  bindings, compatibility flags.
- The user is moving a Cloudflare Pages project to Workers.

Because it is open and read-only, it is a good default in **job descriptions** for
agents that build or review Cloudflare deployments: adding it grants no access to
any Cloudflare account.

## Tool surface

The server advertises its toolset at connect time (`tools.listChanged: true`, so it
can change); probed on 2026-11-14 it offered two tools, both annotated
`readOnlyHint: true`:

- `search_cloudflare_documentation` - semantic search over the Cloudflare docs;
  returns matching passages, each with its `developers.cloudflare.com` URL and
  title.
- `migrate_pages_to_workers_guide` - returns Cloudflare's guide for migrating a
  Pages project to Workers. Read it before planning such a migration.

## Non-obvious gotchas the agent must respect

- **Docs only, no account access.** This server cannot list zones, deploy a Worker
  or read any account state. Cloudflare runs separate account-bound MCP servers
  (bindings, builds, observability) that need a Cloudflare sign-in; this entry is
  not one of them.
- **Read-only.** None of the tools write anything; there is no side effect to
  confirm with the user.
- **Passages, not full pages.** Search returns excerpts; cite the returned URL and
  check limits and pricing against the full page when the answer depends on an
  exact number.
- **Rate limits.** Cloudflare documents no quota for this endpoint. It is shared;
  on an HTTP 429 back off instead of retrying in a loop.
- **Network egress required.** The session sandbox must be able to reach
  `docs.mcp.cloudflare.com`. Under locked-down session networking the connection
  fails fast; that is a config issue, not a tool bug.

## Transport and authentication

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport` - the same path `github/` and `ideogram/` ship on.
The server is stateless: it returns no `mcp-session-id`.

This manifest carries **no** `auth` block and **no** `Authorization` header: the
endpoint is open, and there is no per-user OAuth or static key to bind.

Vendor documentation:
<https://developers.cloudflare.com/agents/model-context-protocol/cloudflare/servers-for-cloudflare/>
