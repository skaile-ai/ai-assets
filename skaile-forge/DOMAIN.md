---
name: skaile-forge Agent Skills
description: Agent definitions and skills for the skaile-forge application (forge/skaile-forge)
app: forge/skaile-forge
---

# skaile-forge Agents

The AI coding assistant and orchestrators embedded in the `forge/skaile-forge` web interface. Drives software development sessions within developer project workspaces — reading, writing, and explaining code.

## Contents

```
ai-assets/skaile-forge/
├── DOMAIN.md                      ← this file
├── CHANGELOG.md                   ← release notes (bundle kind removed, preset migration; agents and the preset carry hand-managed `version:` fields)
├── agent/                         ← GitAgent definition (spec_version 0.2) — coding assistant
│   ├── agent.yaml
│   ├── SOUL.md
│   ├── RULES.md
│   ├── DUTIES.md
│   ├── hooks/
│   ├── knowledge/
│   ├── scripts/
│   ├── skills/                    ← code-assistance, file-management, project-exploration
│   └── workflows/
├── base-orchestrator/             ← home workspace orchestrator
│   ├── agent.yaml
│   ├── SOUL.md
│   ├── RULES.md
│   └── knowledge/
├── project-orchestrator/          ← project workspace assistant template
│   ├── agent.yaml
│   ├── SOUL.md
│   └── RULES.md
├── skills/                        ← application-specific skills
│   └── ui-rendering/
└── skaile-forge.preset.yaml
```

## Agents

| Agent | Path | Description |
|-------|------|-------------|
| forge-project-assistant | `agent/` | The AI assistant persona (identity, rules, capabilities) |
| forge-project-base-orchestrator | `base-orchestrator/` | Onboarding orchestrator for the base workspace |
| forge-project-orchestrator | `project-orchestrator/` | Per-project workspace orchestrator |

## Skills

| Skill | Path | When to use |
|-------|------|-------------|
| ui-rendering | `skills/ui-rendering/` | Emit catalog UI components in response to user requests |
| code-assistance | `agent/skills/code-assistance/` | Write, edit, refactor, debug and explain code; read before modifying, propose minimal diffs |
| file-management | `agent/skills/file-management/` | Read, write, create, delete files and list trees, scoped to the active project workspace |
| project-exploration | `agent/skills/project-exploration/` | Understand an unfamiliar codebase: map structure, entry points, data flow, where a feature lives |

The last three live under the assistant agent's directory (`agent/skills/`), not `skills/`, but are published catalog assets (`skill:@skaile-ai/<name>`) listed in `skaile-forge.preset.yaml` and composed by `project-orchestrator`.

## Usage

The agent definition is loaded at runtime by `forge/skaile-forge/server/utils/agent-definition.ts` and injected as the system prompt for each coding session. The canonical path is `ai-assets/skaile-forge/agent` relative to the skaile-dev monorepo root.

`agent.yaml` sets `imprint_on_project_open: true` — the imprint is rebuilt whenever a project is opened in skaile-forge.

## Notes

The `forge-project-orchestrator` and `forge-project-base-orchestrator` are referenced by `forge/skaile-forge/server/utils/workspaces.ts` via scoped `agent:@skaile-ai/<name>` refs.

The UI rendering skill depends on the manifest injected by the SessionDispatcher (`<system-context name="UI_MANIFEST">`). Check the manifest before rendering any component.
