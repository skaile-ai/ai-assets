---
name: Project-Forge Agent Skills
description: Skills for the Pichi personal AI assistant (forge/project)
---

## Purpose

Agent skills specific to the Project-Forge (Pichi) application. These skills
are injected into the system prompt of the project-forge-project-orchestrator
agent to teach it application-specific capabilities.

## Skills

| Skill | Path | When to use |
|-------|------|-------------|
| ui-rendering | `skills/ui-rendering/` | Emit catalog UI components in response to user requests |

## Notes

These skills depend on the UI manifest injected by the dispatcher
(`<system-context name="UI_MANIFEST">`). Always check the manifest before
attempting to render a component.
