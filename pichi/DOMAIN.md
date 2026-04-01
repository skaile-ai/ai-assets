---
name: pichi
description: GitAgent definition for the Pichi AI-powered IDE web interface (forge/project)
app: forge/project
---

# Pichi Agent

Pichi is the AI coding assistant embedded in the `forge/project` web interface. It drives software development sessions within developer project workspaces — reading, writing, and explaining code.

## Contents

```
ai-assets/pichi/
├── DOMAIN.md          ← this file
└── agent/             ← GitAgent definition (spec v0.1.0)
    ├── agent.yaml     ← agent metadata, model config, skills list
    ├── SOUL.md        ← agent identity and values
    ├── RULES.md       ← behavioral rules and constraints
    ├── DUTIES.md      ← core responsibilities
    ├── hooks/         ← lifecycle hooks
    ├── knowledge/     ← domain knowledge documents
    ├── scripts/       ← helper scripts
    ├── skills/        ← agent skill definitions
    └── workflows/     ← multi-step workflows
```

## Usage

The agent definition is loaded at runtime by `forge/project/server/utils/agent-definition.ts` and injected as the system prompt for each coding session. The canonical path is `ai-assets/pichi/agent` relative to the skaile-dev monorepo root.

`agent.yaml` sets `imprint_on_project_open: true` — the imprint is rebuilt whenever a project is opened in the forge/project UI.
