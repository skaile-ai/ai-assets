---
name: skaile-platform
description: "Runtime surface of the Skaile platform — MCP servers that give agents typed tool access to external systems, and agent definitions that run against them."
type: domain
building_blocks:
  agents: "agents/ — GitAgent-style agent definitions (currently: assistant/ — the primary Skaile assistant)."
  mcpo: "mcpo/ — MCP servers wrapping external systems as tool surfaces. Currently: xls/ (Excel workbook read/write via Apache POI over stdio)."
  skills: "Companion SKILL.md files sit alongside each MCP/agent so catalog tools can discover and route to them."
stage: alpha
---

# Skaile Platform

This domain hosts the executable runtime pieces of the Skaile ecosystem: MCP servers that agents call for tool access, and the agent definitions that drive them. Everything here is production code or production configuration — not authoring templates.

Assets in this domain are packaged and deployed (Docker images for MCPs, agent manifests for runtime loaders) rather than consumed as skill instructions. They still carry SKILL.md / DOMAIN.md metadata so the skaile CLI can list, install, and deploy them consistently with the rest of the catalog.

## Building Blocks

| Folder | Purpose |
|---|---|
| `agents/` | Agent definitions — one subfolder per agent, each with `agent.yaml`, `SOUL.md`, `RULES.md`, and an optional `knowledge/` tree |
| `mcpo/` | MCP servers — one subfolder per server. Each is a full buildable project (e.g. `xls/` is a Java/Maven project that builds a Docker image). A `SKILL.md` at the subfolder root tells agents when to reach for the tool surface |

## Conventions

- Every MCP server under `mcpo/` has its own `SKILL.md` at the project root describing when and how agents should use its tools.
- Implementation docs (plans, deferral logs, skill notes) stay next to the code — e.g. `mcpo/xls/excel-mcp-server-implementation-plan.md`. SKILL.md points at them rather than duplicating content.
- Versioning: each MCP project carries its own version in source (see `pom.xml` / `package.json`) — the SKILL.md `metadata.version` mirrors that value.
