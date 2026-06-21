---
name: mcp
description: "MCP server catalog entries - MCP.md recipes that give agents typed tool access to external systems (Excel, PowerPoint, GitHub)."
type: domain
building_blocks:
  servers: "One subfolder per MCP server. Currently: xls/ (Excel workbook read/write via Apache POI), ppt/ (PowerPoint authoring/rendering/export via Apache POI + LibreOffice), github/ (GitHub hosted remote MCP - repos, issues, PRs, Actions, code search), sql/ (dialect-agnostic SQL over Postgres/MySQL/SQLite/MSSQL, run on baseline node via a sha256-verified release-asset bundle), alma/ (ALMA per-tenant hosted remote MCP - read-only scorecards, indicators, regions, geo)."
stage: alpha
---

# MCP Servers

Catalog entries for MCP servers that agents call for typed tool access to external systems. These are recipes, not implementations: the code and build for locally-run servers live in standalone repos; only the `MCP.md` catalog entry stays here. Not exclusive to any one Skaile domain.

Each server is a **single `MCP.md`**: the manifest frontmatter (transport, recipe, command, args, env) plus a body that carries the agent-facing guidance — when to reach for it, the typical tool flow, non-obvious gotchas, and any authoring rules. There is no separate `SKILL.md`; the per-tool "how" is carried by the server's own tool descriptions at connect time, and `MCP.md` carries the toolset-level "when + flow + gotchas".

## Layout

```
mcp/                      <- MCP servers (one subfolder per server)
├── xls/               <- excel catalog entry (code: skaile-ai/excel-mcp)
│   └── MCP.md         <- catalog manifest + guidance for mcp:excel resolution
├── ppt/               <- ppt catalog entry (code: skaile-ai/powerpoint-mcp)
│   └── MCP.md         <- catalog manifest + guidance for mcp:ppt resolution
├── github/            <- GitHub hosted remote MCP (no image - api.githubcopilot.com)
│   └── MCP.md         <- catalog manifest + guidance for mcp:github resolution (transport: http)
├── sql/               <- sql catalog entry (code: skaile-ai/sql-mcp)
│   └── MCP.md         <- catalog manifest + guidance for mcp:sql resolution (command: node + release payload)
└── alma/              <- ALMA per-tenant hosted remote MCP (no image - per-slug API host)
    └── MCP.md         <- catalog manifest + guidance for mcp:alma resolution (transport: http, url per tenant)
```

## Servers in this domain

| Server | Source | Stage | Purpose |
|---|---|---|---|
| [excel](xls/MCP.md) | `mcp/xls` | alpha (v0.2.1) | Excel (.xlsx/.xlsm/.xls) read/write via Apache POI, 28 tools over stdio (incl. cell styling & sheet presentation). |
| [ppt](ppt/MCP.md) | `mcp/ppt` | stable (v1.0.0) | PowerPoint (.pptx/.pptm) authoring, rendering, export via Apache POI + LibreOffice, 52 tools over stdio. |
| [github](github/MCP.md) | `mcp/github` | alpha (v0.1.0) | GitHub hosted remote MCP (api.githubcopilot.com) over streamable HTTP - repos, issues, PRs, Actions, code search; permission-scoped to the connected GitHub identity. |
| [sql](sql/MCP.md) | `mcp/sql` | alpha (v0.1.0) | Dialect-agnostic SQL over Postgres/MySQL/SQLite/MSSQL, permission-scoped, stdio. Bundle shipped as a sha256-verified GitHub release asset, run on baseline `node` (first `command: node` entry). |
| [alma](alma/MCP.md) | `mcp/alma` | alpha (v1.0.0) | ALMA per-tenant hosted remote MCP over streamable HTTP - read-only scorecards, indicators, regions, period maps, time series, geo; permission- and region-scoped to a PAT. URL is per-slug; auth is a static PAT bound per-org at the platform. |
| [ideogram](ideogram/MCP.md) | `mcp/ideogram` | alpha (v0.1.0) | Ideogram official hosted remote MCP over streamable HTTP - generate (incl. typography), edit/inpaint, reframe/outpaint, upscale. Per-user OAuth: each user signs in to their own Ideogram account and draws on their own subscription credits. (Central org-key route is the `ideogram-image` skill in `use/`.) |

## Conventions

- Each MCP server has a single `MCP.md` carrying both the default runtime config (transport, command, args, env) — so users can add `mcp:<name>` to skaile.yaml dependencies — and the agent-facing guidance in its body.
- Implementation docs (plans, deferral logs, engineering notes) live with the code in the standalone MCP repos; `MCP.md` here links to them rather than duplicating content.
- Locally-run services are self-contained (own Dockerfile, own dependency graph) and expose self-describe tools so agents can branch on feature flags without hard-coding server versions. Their **code and build live in standalone repos** (`xls` -> `skaile-ai/excel-mcp`, `ppt` -> `skaile-ai/powerpoint-mcp`); only the `MCP.md` catalog entry stays here, and the platform Nix flake (`mcps.*`) sources the build from those repos. Hosted remote servers (e.g. `github/`) ship no image - their `MCP.md` declares only the endpoint (`transport: http` + `url`), and auth is bound per-org at the platform layer.
- Paths in tool arguments are sandboxed to the server's data root env var. Host paths must be translated to container-local paths by the agent before tool invocation. (Remote servers have no local path sandbox - they are scoped by the connected identity's permissions instead.)
- **Release-asset (`command: node`) servers.** A pure-JS server may ship its bundle as a public GitHub **release** asset instead of a Nix recipe or a remote endpoint. Its `MCP.md` declares `command: node`, `args: ["${workspace}/.skaile/assets/mcp-server/<name>/server.js"]`, and a `payload: { url, sha256, dest }` block. At session materialization the platform fetches the payload (https + GitHub host-allowlist), **verifies the sha256**, and writes it next to `MCP.md`; the runner substitutes `${workspace}` → the session workspace root. The bundle must be **publicly** fetchable (the catalog fetch path is unauthenticated) even when the source repo is private. `sql/` is the first such entry; bump `version` + `payload.url` + `payload.sha256` together on each release.
- Versioning: each locally-built MCP carries its own version in source (`pom.xml`, in its standalone repo) and mirrors it into the `MCP.md` `version` here (synced on release via the `mcp-release` dispatch, or bump manually). Remote servers with no source build (e.g. `github/`) carry their version directly in `MCP.md` frontmatter.

## Authentication declarations (the `auth` block)

The platform's per-instance secret capability ("Bring Your Own Key") lets an org
admin (or a project/session owner) paste a static credential for an asset; the
platform stores it **encrypted** and injects it into the session at materialization.
An asset opts in by declaring the auth **shape** in its manifest frontmatter - the
manifest **never** contains the secret value, only a description of what to collect
and how to inject it:

```yaml
auth:
  type: api-key            # api-key | bearer | basic
  inject: env              # header | env
  env: IDEOGRAM_API_KEY    # (inject: env)    the variable name the secret is provisioned as
  header: Api-Key          # (inject: header) the request header the secret is written to
  fields:
    - { key: apiKey, label: "Ideogram API key", secret: true }
```

- `type` - the credential kind the UI collects (`api-key`, `bearer`, `basic`).
- `inject` - where the platform places the resolved secret:
  - `env` - provisioned as an **environment variable** named by `env:`; this is how a
    **skill** receives a key (e.g. the `ideogram-image` skill reads
    `$IDEOGRAM_API_KEY`).
  - `header` - written into a request **header** named by `header:`; this is how a
    **keyed remote MCP** (the `alma`-style static-PAT shape) receives a credential
    without baking it into the catalog entry.
- `fields` - what the configure-instance UI prompts for; mark secrets `secret: true`
  so they are masked and stored encrypted.

When **not** to declare an `auth` block: remote MCPs whose auth is handled by the
platform's OAuth path carry **no** `auth` block at all - `github/` (per-user GitHub
OAuth) and `ideogram/` (per-user Ideogram OAuth) leave auth entirely to the platform.
Use an `auth` block only for the static-key / paste-a-credential routes.
