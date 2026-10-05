---
name: "platform-guide"
description: "Deep knowledge of the Skaile platform's UI and conceptual model so the
  assistant can guide users and act on their behalf. Use for 'how do I...', 'where is...',
  'walk me through...' or 'help me with the platform'; for projects, sessions, workspaces,
  flows (run groups, batch runs, recipes, webhooks, personal flows), previews, classifiers,
  Exchange mail and calendar (triage, drafting, sending, shared mailboxes), the project graph,
  Skailify apps, sharing and inviting, connecting a data source, AI providers and Claude
  seats, skills/assets, scoped sessions, agent-to-agent, notifications, roles and permissions,
  Work vs Private workspaces, My space and the Home, assistant reach, hibernation, job
  descriptions (agent templates), temporary hires and helpers, or any platform surface; before
  creating a project/session/organization, inviting someone, starting a connector setup or mount, or
  reading back a durable operation; or when you hit a platform problem, or the user wants
  to report a bug or suggest a feature to the Skaile team. Load on demand, not always-on."
version: 0.16.14
metadata:
  stage: "alpha"
  source: "ORIGINAL"
keywords:
  - platform-guide
  - platform
  - ui
  - navigation
  - workspace
  - session
  - project
  - flow
  - run-group
  - agent-action
  - action-batch
  - find-actions
  - control-plane
  - durable-operation
  - autonomy
  - approval
  - webhook
  - skailify
  - sharing
  - classifier
  - classify
  - exchange
  - mail
  - calendar
  - send-draft
  - connector-mount
  - project-graph
  - agent-graph
  - personal-flow
  - notifications
  - report-a-problem
  - feedback
  - assistant-profile
  - session-file
  - private-workspace
  - my-space
  - assistant-reach
  - agent-template
  - job-description
  - temporary-hire
  - helper
  - spawn-agent
  - spawn-subagent
  - subagent
  - finish-spawned-instance
  - organization-branding
  - create-agent-template
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
(mounts / worktrees / containers), and never narrate implementation mechanics — model and
version names, resolution or parameter tweaks, retries, or step-by-step tool chatter — in a
user-facing turn; report only the user-meaningful outcome. Surface internal terms or
mechanics only when the user is technical or `expertMode=true`. Say **job description** (not
agent template), **temporary hire** (not instance) and **helper** (not subagent) to people;
capability names, ids and fields keep the code words.

## Detail files

### Concepts (the stable model)

| File | Use when the user asks about... |
| ---- | -------------------------------- |
| `concepts/model.md` | The big picture: org/project/session/workspace, source types, mounts vs connectors, assets/skills, agents and the project graph, flows, roles & permissions, business organizations vs Private workspaces, the Home and My space. Start here for orientation. |
| `concepts/sessions.md` | Session lifecycle (hibernate/wake/close), what a sleeping session shows and how it wakes, multiple sessions, **scoped sessions**, forking/renaming. |
| `concepts/flows.md` | Flows and the Flows page/editor, **authoring a flow definition** (the seven node kinds, contracts, gates vs checks, provenance), runs and gates, **classifier nodes**, **run groups** (batch / standing / unattended processing), **personal flows**, running a flow inside an existing session, recipes, webhook triggers and the session webhook inbox. |
| `concepts/integrations.md` | Connecting external systems: providers, auth modes (delegation vs service account), access levels, editing an existing mount, whose connection a mount runs on, **Reconnect**, Exchange mail and shared mailboxes, AI providers and subscription seats, classifier providers. |
| `concepts/collaboration.md` | Multi-user sessions (mentions/reactions/threading/presence), sharing with people, public file-preview links, agent-to-agent (A2A). |
| `concepts/previews.md` | Running and viewing an app preview; what makes a workspace previewable. |
| `concepts/agent.md` | How the agent itself acts: runtime capabilities, approval gates and autonomy grants (incl. asking the owner for one ahead), durable operations and the `AwaitingUser` handoff, discovery-then-propose, **assistant reach** (which organizations you can see and act in), reporting platform problems to the Skaile team, platform actions (`find_actions` / `invoke` / `batch`), shared sessions (whose turn it is), UI steering (`open_file`, `navigate`) and UI-context flags, the `session`/`presence` state stores, guiding vs doing. |

### Reference (load only when constructing an action)

| File | Use when... |
| ---- | ----------- |
| `references/agent-action-catalog.md` | You are about to search for or run a platform action (`platform.find_actions`, `platform.invoke`, `platform.batch`), or pass a file by reference — and need the call shapes, `$ref` syntax, per-step consent, file transfers and shared-session rules. |
| `references/exchange-mail-calendar.md` | You are about to read, triage, file, draft or send mail, or read or change a calendar event, in a connected Microsoft 365 mailbox — and need mailbox selection, the approval tiers, the send grant, and how to read a send result. |
| `references/classifier.md` | You are about to classify many items with closed questions (`platform.classify`) and need the call shape, limits, and how to read `calibrated` / `p`. |
| `references/control-plane-capabilities.md` | You are about to create a project/session/organization, change an organization's name, logo or icon, invite someone, start or repair a connector, propose a connector mount or an asset configuration, run a flow in another session, read or write a text file in another of the owner's sessions, change the assistant profile, create a job description (agent template), list or read the project's job descriptions, take on a temporary hire from one or start a helper (`platform.spawn_subagent`), finish a temporary hire or helper you started, edit a job description's instructions or skills, or read an operation back — and need the family's shape, effect classes, real boundaries, the operation lifecycle, and the `AwaitingUser` handoff. |

### UI (where things live, click-paths)

| File | Use when the user asks... |
| ---- | -------------------------- |
| `ui/navigation.md` | "Where is...", "how do I get to...", the **Work** / **Private** switch, the sidebar's Company / Projects / My space sections and the assistant launcher, project/session/org settings (incl. **Classifiers**, **Assistants**), a read-only Private workspace, creating a project, connecting a data source, shared Exchange mailboxes, the org **Sessions** report, **Project graph**, Escape and Cmd+K switching — the app shell, sidebar, command palette, settings hierarchy. |
| `ui/workspace.md` | Anything about the workspace itself: chat composer, the workspace panel and its file explorer, the preview pane, the side panels opened from the toolbar icons, presence, mobile, common in-workspace click-paths. |

## Hard rules

- Never enumerate the named `platform.*` capabilities from memory — that set changes every
  deploy. Reference them by concept and consult the live registry (`concepts/agent.md`).
  The corollary binds equally: never tell the user you *cannot* do something because you do
  not remember a capability for it. Look, then answer.
  The `references/` tier is where exact names live, for a call you are about to construct:
  `references/agent-action-catalog.md`, `references/control-plane-capabilities.md`,
  `references/exchange-mail-calendar.md` and `references/classifier.md`. All four are maps of a live registry, not substitutes for it.
  Platform actions are discovered with `platform.find_actions`, never recalled or inferred
  from the data model: run only an action it returned, with the input schema it returned
  (`references/agent-action-catalog.md`).
- Never invent UI labels, paths, or platform facts. If a detail file does not cover it, say
  so or check the live UI/capabilities rather than guess.
- Respect approval gates and access levels (read-only connectors/mounts, role restrictions).
