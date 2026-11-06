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
│   ├── knowledge/
│   └── skills/                    ← code-assistance, file-management, project-exploration
├── base-orchestrator/             ← home workspace orchestrator
│   ├── agent.yaml
│   ├── SOUL.md
│   ├── RULES.md
│   └── knowledge/
├── project-orchestrator/          ← project workspace assistant template
│   ├── agent.yaml
│   ├── SOUL.md
│   └── RULES.md
└── skaile-forge.preset.yaml
```

`skaile-forge` is listed under `dev_paths` in `skaile.manifest.yaml`: the store sync skips it, because forge loads these agents from the ai-assets checkout or the git source, never from the store. A `skaile.yaml` dependency on one of them still resolves from the git source.

## Agents

| Agent | Path | Description |
|-------|------|-------------|
| forge-project-assistant | `agent/` | The AI assistant persona (identity, rules, capabilities) |
| forge-project-base-orchestrator | `base-orchestrator/` | Onboarding orchestrator for the base workspace |
| forge-project-orchestrator | `project-orchestrator/` | Per-project workspace orchestrator |

## Skills

| Skill | Path | When to use |
|-------|------|-------------|
| code-assistance | `agent/skills/code-assistance/` | Write, edit, refactor, debug and explain code; read before modifying, propose minimal diffs |
| file-management | `agent/skills/file-management/` | Read, write, create, delete files and list trees, scoped to the active project workspace |
| project-exploration | `agent/skills/project-exploration/` | Understand an unfamiliar codebase: map structure, entry points, data flow, where a feature lives |

They live under the assistant agent's directory (`agent/skills/`), are listed in `skaile-forge.preset.yaml` as `skill:@skaile-ai/<name>`, and are composed by `project-orchestrator`.

## Usage

`forge/skaile-forge/server/utils/agent-definition.ts` only locates the definition: it indexes the agent manifests in this domain by their declared `name:` and resolves a workspace's `agent:@skaile-ai/<name>` ref to a directory (the canonical path is `ai-assets/skaile-forge/agent` relative to the skaile-dev monorepo root). It does not build any prompt. The `@skaile/workspaces` runtime assembles the system prompt from that directory in the fixed order described in `docs/agents.md` (System Prompt Assembly), with the `<MEMORY>` section rendered from the agent's `memory.blocks`.

## Notes

The `forge-project-orchestrator` and `forge-project-base-orchestrator` are referenced by `forge/skaile-forge/server/utils/workspaces.ts` via scoped `agent:@skaile-ai/<name>` refs.
