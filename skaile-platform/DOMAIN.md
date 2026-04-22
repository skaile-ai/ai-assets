---
name: "skaile-platform"
description: "Platform-level components of the Skaile runtime — agents, assistants, and MCP servers (stateful MCPO services) consumed by Skaile agents and the CLI."
---

# skaile-platform

Platform-level Skaile components. Unlike `skaileup-*` (which describes the product-build pipeline) or `dev-implementation-experts-*` (which is pure skill content), this domain contains the **runtime pieces** an agent invokes: long-lived assistants and the MCP servers they talk to.

## Layout

```
skaile-platform/
├── agents/                ← long-lived agent personas (rules + soul)
│   └── assistant/
│       ├── RULES.md
│       └── SOUL.md
└── mcpo/                  ← stateful MCP servers (MCPO = "MCP over something")
    └── ppt/               ← ppt-mcp-server
        ├── SKILL.md       ← skill descriptor consumed by the CLI / platform
        ├── README.md      ← authoritative tool catalog + JSON-RPC examples
        ├── CLAUDE.md      ← engineering notes for contributors
        ├── TEST_SCENARIOS.md
        ├── Dockerfile     ← shipped runtime image (`ppt-mcp:dev`)
        ├── pom.xml        ← Maven (wrapper pinned to 3.9.9 in `.mvn/`)
        └── src/           ← Java 17 source
```

## Skills in this domain

| Skill | Source | Stage | Purpose |
|---|---|---|---|
| [ppt-mcp-server](mcpo/ppt/SKILL.md) | `skaile-platform/mcpo/ppt` | stable (v1.0.0) | PowerPoint (.pptx / .pptm) authoring, rendering, export via Apache POI + LibreOffice, exposed as 52 JSON-RPC tools over stdio. |

## Agents in this domain

| Agent | Source | Purpose |
|---|---|---|
| assistant | `skaile-platform/agents/assistant` | Default assistant persona — rules (`RULES.md`) and voice (`SOUL.md`) consumed at runtime. |

## Conventions

- Each MCP server lives at `mcpo/<name>/` and ships as its own Docker image. The in-directory `SKILL.md` is the canonical descriptor for agents; `README.md` carries the full tool catalog and is kept in sync with the Java sources.
- Runtime services are self-contained (own `Dockerfile`, own dependency graph). They expose themselves via `ppt.capabilities`-style self-describe tools so agents can branch on feature flags without hard-coding server versions.
- Paths in tool arguments are sandboxed to the server's `MCPO_ALLOWED_ROOT`. Host paths must be translated to container-local paths by the agent before tool invocation.
