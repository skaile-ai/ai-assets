---
name: skaile-platform
description: "Runtime surface of the Skaile platform - MCP servers that give agents typed tool access to external systems, and agent definitions that run against them."
type: domain
building_blocks:
  agents: "agents/ - GitAgent-style agent definitions (currently: assistant/ - the primary Skaile assistant)."
  mcpo: "mcpo/ - MCP servers wrapping external systems as tool surfaces. Currently: xls/ (Excel workbook read/write via Apache POI), ppt/ (PowerPoint authoring/rendering/export via Apache POI + LibreOffice), github/ (GitHub hosted remote MCP - repos, issues, PRs, Actions, code search)."
  skills: "Companion SKILL.md files sit alongside each MCP/agent so catalog tools can discover and route to them."
stage: alpha
---

# Skaile Platform

This domain hosts the executable runtime pieces of the Skaile ecosystem: MCP servers that agents call for tool access, and the agent definitions that drive them. Everything here is production code or production configuration - not authoring templates.

Assets in this domain are packaged and deployed (Docker images for MCPs, agent manifests for runtime loaders) rather than consumed as skill instructions. They still carry SKILL.md / DOMAIN.md metadata so the skaile CLI can list, install, and deploy them consistently with the rest of the catalog.

## Layout

```
skaile-platform/
├── agents/                <- long-lived agent personas (rules + soul)
│   └── assistant/
│       ├── RULES.md
│       └── SOUL.md
└── mcpo/                  <- MCP servers (one subfolder per server)
    ├── xls/               <- excel-mcp-server
    │   ├── SKILL.md       <- skill descriptor for agents
    │   ├── MCP.md         <- catalog manifest for mcp:excel resolution
    │   ├── README.md      <- tool catalog + run instructions
    │   ├── Dockerfile     <- shipped runtime image (excel-mcp:dev)
    │   ├── pom.xml        <- Maven (Java 21, Apache POI 5.5.1)
    │   └── src/
    ├── ppt/               <- ppt-mcp-server
    │   ├── SKILL.md       <- skill descriptor for agents
    │   ├── README.md      <- tool catalog + JSON-RPC examples
    │   ├── CLAUDE.md      <- engineering notes for contributors
    │   ├── Dockerfile     <- shipped runtime image (ppt-mcp:dev)
    │   ├── pom.xml        <- Maven (Java 17, Apache POI + LibreOffice)
    │   └── src/
    └── github/            <- GitHub hosted remote MCP (no image - api.githubcopilot.com)
        ├── SKILL.md       <- skill descriptor for agents
        └── MCP.md         <- catalog manifest for mcp:github resolution (transport: http)
```

## Skills in this domain

| Skill | Source | Stage | Purpose |
|---|---|---|---|
| [excel-mcp-server](mcpo/xls/SKILL.md) | `skaile-platform/mcpo/xls` | alpha (v0.2.0) | Excel (.xlsx/.xlsm/.xls) read/write via Apache POI, 28 tools over stdio (incl. cell styling & sheet presentation). |
| [ppt-mcp-server](mcpo/ppt/SKILL.md) | `skaile-platform/mcpo/ppt` | stable (v1.0.0) | PowerPoint (.pptx/.pptm) authoring, rendering, export via Apache POI + LibreOffice, 52 tools over stdio. |
| [github](mcpo/github/SKILL.md) | `skaile-platform/mcpo/github` | alpha (v0.1.0) | GitHub hosted remote MCP (api.githubcopilot.com) over streamable HTTP - repos, issues, PRs, Actions, code search; permission-scoped to the connected GitHub identity. |

## Agents in this domain

| Agent | Source | Purpose |
|---|---|---|
| assistant | `skaile-platform/agents/assistant` | Default assistant persona - rules and voice consumed at runtime. |

## Conventions

- Every MCP server under `mcpo/` has its own `SKILL.md` at the project root describing when and how agents should use its tools.
- Each MCP server that supports catalog resolution also has an `MCP.md` with default runtime config (transport, command, args, env) so users can add `mcp:<name>` to skaile.yaml dependencies.
- Implementation docs (plans, deferral logs, skill notes) stay next to the code. SKILL.md points at them rather than duplicating content.
- Locally-run services are self-contained (own Dockerfile, own dependency graph) and expose self-describe tools so agents can branch on feature flags without hard-coding server versions. Hosted remote servers (e.g. `github/`) ship no image - their `MCP.md` declares only the endpoint (`transport: http` + `url`), and auth is bound per-org at the platform layer.
- Paths in tool arguments are sandboxed to the server's data root env var. Host paths must be translated to container-local paths by the agent before tool invocation. (Remote servers have no local path sandbox - they are scoped by the connected identity's permissions instead.)
- Versioning: each locally-built MCP carries its own version in source (pom.xml) and mirrors it into the SKILL.md/MCP.md `version`. Remote servers with no source build (e.g. `github/`) carry their version directly in `MCP.md` / SKILL.md frontmatter.
