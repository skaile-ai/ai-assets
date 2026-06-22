---
name: "platform-guide"
description: "Deep knowledge of the Skaile platform's UI and conceptual model so the
  assistant can guide users and act on their behalf. Use when the user asks 'how do I...',
  'where is...', 'where do I find...', 'walk me through...', or 'help me with the platform';
  or asks about projects, sessions, workspaces, flows, previews, sharing, inviting people,
  connecting a data source, enabling a skill/asset, scoped sessions, agent-to-agent, roles
  and permissions, hibernation, or any platform surface. Load on demand, not always-on."
license: "Apache-2.0"
metadata:
  stage: "alpha"
  author: "skaile-ai"
---

# Skaile Platform Guide

A map of how the Skaile platform works — its conceptual model and its UI — so the assistant
can answer "how do I X / where is Y" and act on the user's behalf. This file is a thin
index; load the linked detail file for the topic at hand and stop there.

## How to use this skill

1. Identify the topic from the user's question.
2. Open the **one or two** detail files that match (table below) — do not load them all.
3. To **guide**: give the click-path from the UI files (labels in **bold** are real UI
   strings). To **do it for the user**: use a live platform capability if one fits (see
   `concepts/agent.md`) — capabilities are discovered at runtime, never assumed.

Speak in user-facing business terms (project / session / project data), not internal terms
(mounts / worktrees / containers), unless the user is technical or `expertMode=true`.

## Detail files

### Concepts (the stable model)

| File | Use when the user asks about... |
| ---- | -------------------------------- |
| `concepts/model.md` | The big picture: org/project/session/workspace, source types, mounts vs connectors, assets/skills, flows, roles & permissions. Start here for orientation. |
| `concepts/sessions.md` | Session lifecycle (hibernate/wake/close), multiple sessions, **scoped sessions**, forking/renaming. |
| `concepts/integrations.md` | Connecting external systems: providers, auth modes (delegation vs service account), access levels. |
| `concepts/collaboration.md` | Multi-user sessions (mentions/reactions/threading/presence), sharing with people, public file-preview links, agent-to-agent (A2A). |
| `concepts/previews.md` | Running and viewing an app preview; what makes a workspace previewable. |
| `concepts/agent.md` | How the agent itself acts: runtime capabilities, approval gates, UI-context flags, the `session`/`presence` state stores, guiding vs doing. |

### UI (where things live, click-paths)

| File | Use when the user asks... |
| ---- | -------------------------- |
| `ui/navigation.md` | "Where is...", "how do I get to...", project/session/org settings, creating a project, connecting a data source — the app shell, sidebar, command palette, settings hierarchy. |
| `ui/workspace.md` | Anything about the workspace itself: chat composer, file/resource explorer, preview pane, right-sidebar tabs (Assistant/AI Assets/Share/Config/Debug), presence, mobile, common in-workspace click-paths. |

## Hard rules

- Never enumerate a fixed list of platform actions from memory — the action catalog changes
  every deploy. Reference capabilities by concept and consult the live registry (`concepts/agent.md`).
- Never invent UI labels, paths, or platform facts. If a detail file does not cover it, say
  so or check the live UI/capabilities rather than guess.
- Respect approval gates and access levels (read-only connectors/mounts, role restrictions).
