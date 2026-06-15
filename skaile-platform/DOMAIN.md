---
name: skaile-platform
description: "Runtime surface of the Skaile platform - agent definitions that run against the MCP tool catalog."
type: domain
building_blocks:
  agents: "agents/ - GitAgent-style agent definitions (currently: assistant/ - the primary Skaile assistant)."
stage: alpha
---

# Skaile Platform

This domain hosts the executable runtime pieces of the Skaile ecosystem: the agent definitions that drive the platform. The MCP servers those agents call live in the top-level [`mcp/`](../mcp/DOMAIN.md) domain.

Assets in this domain are packaged and deployed (agent manifests for runtime loaders) rather than consumed as skill instructions. They still carry SKILL.md / DOMAIN.md metadata so the skaile CLI can list, install, and deploy them consistently with the rest of the catalog.

## Layout

```
skaile-platform/
└── agents/                <- long-lived agent personas (rules + soul)
    └── assistant/
        ├── RULES.md
        └── SOUL.md
```

## Agents in this domain

| Agent | Source | Purpose |
|---|---|---|
| assistant | `skaile-platform/agents/assistant` | Default assistant persona - rules and voice consumed at runtime. |
