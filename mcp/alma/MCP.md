---
name: alma
description: "Use when an agent needs ALMA scorecard data - regions, scorecards,
  indicators, period-to-date maps, data-point time series and region geo for a
  given ALMA tenant. Wraps ALMA's embedded, read-only hosted MCP endpoint
  (per-tenant `/mcp` on the tenant API host), so every tool runs as the
  connected Personal Access Token's role + region scope. Reach for this over raw
  REST/OpenAPI when the task spans several ALMA reads or you want typed,
  permission-scoped tools (e.g. joining indicator time series against weather)."
version: 0.1.0
transport: http
url: https://alma-next-<slug>-api.demoscorecard.org/mcp
keywords:
  - alma
  - scorecards
  - indicators
  - regions
  - timeseries
  - geo
  - mcp
  - remote
---

# ALMA MCP Server (remote)

ALMA's embedded, **read-only** MCP server. This is a **remote** asset: there is
no binary or Docker image to package - the runner opens a streamable-HTTP
connection straight to the tenant's `POST /mcp` endpoint. Tools are scoped to
whatever the connected ALMA identity (the PAT) is allowed to read.

Design/rationale lives with the code: `alma/docs/mcp-server-spec.md`; the
operator guide is `alma/docs/mcp-server.md`.

## When to reach for this

- The user asks for ALMA regions, scorecards, indicators, or their values.
- The agent needs an indicator's time series over a period range, or a bulk pull
  by region/period (`get_indicator_timeseries` / `get_scorecard_data`).
- The task joins ALMA data points against another source (e.g. weather vs an
  indicator) - read `almatools://data-dictionary` first for the join contract.
- The agent needs region geo (centroid + bbox) to place or map ALMA regions.

For a single ad-hoc value, the tenant REST API is fine - reach for this when the
work is genuinely ALMA-shaped and multi-step.

## Non-obvious gotchas the agent must respect

- **Read-only by construction.** Only read tools are registered; the PAT can
  never mutate regardless of POST verb. Do not attempt writes.
- **Permission- and region-scoped.** Every tool runs as the PAT's user role +
  region scope. A 401 means an invalid/revoked token (fails closed, never
  downgraded to public); an empty result usually means out-of-scope, not absent.
  Call `whoami` first to confirm slug / role / region scope.
- **One tenant per connection.** The host identifies the tenant; the PAT carries
  identity. There is no slug switch at call time - a different tenant is a
  different instance URL + a different PAT.
- **Polarity / source / number format are not returned** by `list_indicators` -
  infer from the indicator name. Periods may be on the Ethiopian calendar
  (`list_periods` flags it); honor the flag when mapping period to date.
- **Rate limits.** Per PAT: 60 req/min and 5,000 req/day. Counters are in-memory
  and reset on backend restart - back off on 429 rather than hammering.

## Transport

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport`
(`workspaces/packages/workspaces/runner/src/external-mcp.ts`). The MCP-spec alias
`streamable-http` normalizes to `http`. ALMA speaks stateless Streamable HTTP
(one backend per slug, no session store), so no `sse` fallback is needed.

## URL is per-tenant (instance override)

Unlike a single-endpoint hosted server, ALMA is multi-tenant: each slug has its
own API host, `https://alma-next-<slug>-api.demoscorecard.org/mcp`. The `<slug>`
placeholder in this manifest is a **default to be overridden per instance** - the
operator sets the concrete tenant URL when enabling the asset (remote-MCP widget
on the platform), the same place the credential is bound.

## Authentication (bound per organization, not here)

This manifest deliberately carries **no** `Authorization` header and **no**
`auth` block. ALMA auth is a **static Personal Access Token** (`alma_pat_…`),
install-specific, applied by the platform when the instance is enabled - not
baked into the shared catalog entry.

Wire the org side (PAT path - the provisioned-secret flow, mirroring how a static
remote MCP key is bound, not GitHub-style OAuth):

1. A tenant `globalAdmin` mints a PAT for the target user
   (`POST /pat` with `forUserId`). The plaintext `alma_pat_…` is shown **once** -
   store it in the platform's secret manager immediately; only its hash persists.
   The PAT inherits that user's role + region scope and does not expire (revoke
   via `DELETE /pat/<id>` is the kill switch; `lastUsedAt` spots a stale token).
2. Provision that token as the instance secret, then set the request header to
   `Authorization: Bearer <provisioned-ref>` (default header, `bearer` scheme) on
   the `McpServerEntry`
   (`platform/backend/libs/session/src/skaile-config.types.ts` /
   `mcp-credential-provision.ts`). ALMA uses the standard bearer scheme, so no
   custom-header (`authHeader` / `authScheme: raw`) override is needed.
3. Bind the per-tenant `url` (the slug above) and the credential in the remote-MCP
   widget
   (`platform/frontend/src/components/ui/configure-instance/remote-mcp-widget.tsx`).

## Tools & resources

Call `whoami` first. Read tools: `whoami`, `list_regions`, `get_regions_by_ids`,
`get_region_ancestors`, `list_scorecards`, `get_scorecard`, `list_indicators`,
`list_periods`, `get_indicator_timeseries`, `get_scorecard_data`,
`get_region_geo`, `get_stats`. Resources: `almatools://openapi` (tenant OpenAPI
spec), `almatools://data-dictionary` (**read before modelling**),
`almatools://examples` (PAT-based curl / Python snippets).
