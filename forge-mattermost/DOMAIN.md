---
name: Forge-Mattermost Agent Skills
description: Agent definitions for the Mattermost channel workspace bot (forge/L3-mattermost)
app: forge/L3-mattermost
---

# Forge Mattermost Agent

Agents and skills for the Mattermost channel workspace system. The bot turns each Mattermost
channel into a collaborative Skaile workspace with a persistent orchestrator and per-thread
worker agents.

## Contents

```
ai-assets/forge-mattermost/
├── DOMAIN.md                         <- this file
└── workspace-orchestrator/           <- channel workspace orchestrator
    ├── agent.yaml
    ├── SOUL.md
    └── RULES.md
```

## Agents

| Agent | Path | Description |
|-------|------|-------------|
| workspace-orchestrator | `workspace-orchestrator/` | Persistent channel orchestrator — manages threads, shared state, and team coordination |

## Usage

The agent definition is loaded at runtime by `forge/L3-mattermost` via the `agent.definition`
field in `skaile.yaml`. The orchestrator session uses this agent; thread agents may use the
same definition or a different one depending on the channel workspace config.

## Notes

The workspace orchestrator has access to workspace management tools via the `WorkspaceAdapter`
connector: `spawn_thread`, `list_threads`, `hibernate_thread`, `wake_thread`, `kill_thread`,
`get_state`, `set_state`, `post_to_channel`.
