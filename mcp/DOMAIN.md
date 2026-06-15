---
name: mcp
description: "MCP server catalog entries - SKILL.md + MCP.md recipes that give agents typed tool access to external systems (Excel, PowerPoint, GitHub)."
type: domain
building_blocks:
  servers: "One subfolder per MCP server. Currently: xls/ (Excel workbook read/write via Apache POI), ppt/ (PowerPoint authoring/rendering/export via Apache POI + LibreOffice), github/ (GitHub hosted remote MCP - repos, issues, PRs, Actions, code search)."
  skills: "Companion SKILL.md files sit alongside each MCP so catalog tools can discover and route to them."
stage: alpha
---

# MCP Servers

Catalog entries for MCP servers that agents call for typed tool access to external systems. These are recipes, not implementations: the code and build for locally-run servers live in standalone repos; only the `SKILL.md` + `MCP.md` catalog entry stays here. Not exclusive to any one Skaile domain.

## Layout

```
mcp/                      <- MCP servers (one subfolder per server)
├── xls/               <- excel catalog entry (code: skaile-ai/excel-mcp)
│   ├── SKILL.md       <- skill descriptor for agents
│   └── MCP.md         <- catalog manifest for mcp:excel resolution
├── ppt/               <- ppt catalog entry (code: skaile-ai/powerpoint-mcp)
│   ├── SKILL.md       <- skill descriptor for agents
│   └── MCP.md         <- catalog manifest for mcp:ppt resolution
└── github/            <- GitHub hosted remote MCP (no image - api.githubcopilot.com)
    ├── SKILL.md       <- skill descriptor for agents
    └── MCP.md         <- catalog manifest for mcp:github resolution (transport: http)
```

## Skills in this domain

| Skill | Source | Stage | Purpose |
|---|---|---|---|
| [excel-mcp-server](xls/SKILL.md) | `mcp/xls` | alpha (v0.2.1) | Excel (.xlsx/.xlsm/.xls) read/write via Apache POI, 28 tools over stdio (incl. cell styling & sheet presentation). |
| [ppt-mcp-server](ppt/SKILL.md) | `mcp/ppt` | stable (v1.0.0) | PowerPoint (.pptx/.pptm) authoring, rendering, export via Apache POI + LibreOffice, 52 tools over stdio. |
| [github](github/SKILL.md) | `mcp/github` | alpha (v0.1.0) | GitHub hosted remote MCP (api.githubcopilot.com) over streamable HTTP - repos, issues, PRs, Actions, code search; permission-scoped to the connected GitHub identity. |

## Conventions

- Every MCP server has its own `SKILL.md` describing when and how agents should use its tools.
- Each MCP server that supports catalog resolution also has an `MCP.md` with default runtime config (transport, command, args, env) so users can add `mcp:<name>` to skaile.yaml dependencies.
- Implementation docs (plans, deferral logs, engineering notes) live with the code in the standalone MCP repos; the `SKILL.md`/`MCP.md` here link to them rather than duplicating content.
- Locally-run services are self-contained (own Dockerfile, own dependency graph) and expose self-describe tools so agents can branch on feature flags without hard-coding server versions. Their **code and build live in standalone repos** (`xls` -> `skaile-ai/excel-mcp`, `ppt` -> `skaile-ai/powerpoint-mcp`); only the `SKILL.md` + `MCP.md` catalog entry stays here, and the platform Nix flake (`mcps.*`) sources the build from those repos. Hosted remote servers (e.g. `github/`) ship no image - their `MCP.md` declares only the endpoint (`transport: http` + `url`), and auth is bound per-org at the platform layer.
- Paths in tool arguments are sandboxed to the server's data root env var. Host paths must be translated to container-local paths by the agent before tool invocation. (Remote servers have no local path sandbox - they are scoped by the connected identity's permissions instead.)
- Versioning: each locally-built MCP carries its own version in source (`pom.xml`, in its standalone repo) and mirrors it into the SKILL.md/MCP.md `version` here (synced on release via the `mcp-release` dispatch, or bump manually). Remote servers with no source build (e.g. `github/`) carry their version directly in `MCP.md` / SKILL.md frontmatter.
