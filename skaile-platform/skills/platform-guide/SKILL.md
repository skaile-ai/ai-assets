---
name: "platform-guide"
description: "Deep knowledge of the Skaile platform's UI and conceptual model so the
  assistant can guide users and act on their behalf. Use when the user asks 'how do I...',
  'where is...', 'where do I find...', 'walk me through...', or 'help me with the platform';
  or asks about projects, sessions, workspaces, flows, run groups, batch runs, recipes,
  webhooks, in-session flow runs, personal flows, previews, classifiers (classifier
  providers, classifying many items), Exchange mail and calendar (inbox triage, drafting and
  sending mail, shared mailboxes), agents and the agent graph, agent-controlled apps
  (Skailify), sharing, inviting people, connecting a data source, AI providers and Claude
  subscription seats, enabling a skill/asset, scoped sessions, agent-to-agent, notification
  modes, roles and permissions, personal vs business workspace, hibernation, or any platform
  surface; or when you are about to create a project/session/organization, invite someone,
  start a connector setup or propose a connector mount, or read back a durable operation you
  started. Load on demand, not always-on."
version: 0.12.0
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
  - agent-graph
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
mechanics only when the user is technical or `expertMode=true`.

## Detail files

### Concepts (the stable model)

| File | Use when the user asks about... |
| ---- | -------------------------------- |
| `concepts/model.md` | The big picture: org/project/session/workspace, source types, mounts vs connectors, assets/skills, agents and the agent graph, flows, roles & permissions, personal vs business workspace. Start here for orientation. |
| `concepts/sessions.md` | Session lifecycle (hibernate/wake/close), what a sleeping session shows and how it wakes, multiple sessions, **scoped sessions**, forking/renaming. |
| `concepts/flows.md` | Flows and the Flows page/editor, **authoring a flow definition** (the seven node kinds, contracts, gates vs checks, provenance), runs and gates, **classifier nodes**, **run groups** (batch / standing / unattended processing), **personal flows**, running a flow inside an existing session, recipes, webhook triggers and the session webhook inbox. |
| `concepts/integrations.md` | Connecting external systems: providers, auth modes (delegation vs service account), access levels, whose connection a mount runs on, **Reconnect**, Exchange mail and shared mailboxes, AI providers and subscription seats, classifier providers. |
| `concepts/collaboration.md` | Multi-user sessions (mentions/reactions/threading/presence), sharing with people, public file-preview links, agent-to-agent (A2A). |
| `concepts/previews.md` | Running and viewing an app preview; what makes a workspace previewable. |
| `concepts/agent.md` | How the agent itself acts: runtime capabilities, approval gates and autonomy grants, durable operations and the `AwaitingUser` handoff, discovery-then-propose, the target-bound `platform.act` / `platform.act_batch` allowlist, UI-context flags, the `session`/`presence` state stores, guiding vs doing. |

### Reference (load only when constructing an action)

| File | Use when... |
| ---- | ----------- |
| `references/agent-action-catalog.md` | You are about to call `platform.act` or `platform.act_batch` and need the exact sole allowlisted action, batch-reference syntax, consequences, and target-role rules. |
| `references/exchange-mail-calendar.md` | You are about to read, triage, file, draft or send mail, or read or change a calendar event, in a connected Microsoft 365 mailbox — and need mailbox selection, the approval tiers, the send grant, and how to read a send result. |
| `references/classifier.md` | You are about to classify many items with closed questions (`platform.classify`) and need the call shape, limits, and how to read `calibrated` / `p`. |
| `references/control-plane-capabilities.md` | You are about to create a project/session/organization, invite someone, start or repair a connector, propose a connector mount or an asset configuration, run a flow in this session, or read an operation back — and need the family's shape, effect classes, real boundaries, the operation lifecycle, and the `AwaitingUser` handoff. |

### UI (where things live, click-paths)

| File | Use when the user asks... |
| ---- | -------------------------- |
| `ui/navigation.md` | "Where is...", "how do I get to...", project/session/org settings (incl. **Classifiers**), creating a project, connecting a data source, shared Exchange mailboxes, the org **Sessions** report, **Agent graph**, Escape and Cmd+K switching — the app shell, sidebar, command palette, settings hierarchy. |
| `ui/workspace.md` | Anything about the workspace itself: chat composer, the workspace panel and its file explorer, the preview pane, the side panels opened from the toolbar icons, presence, mobile, common in-workspace click-paths. |

## Hard rules

- Never enumerate the named `platform.*` capabilities from memory — that set changes every
  deploy. Reference them by concept and consult the live registry (`concepts/agent.md`).
  The corollary binds equally: never tell the user you *cannot* do something because you do
  not remember a capability for it. Look, then answer.
  The `references/` tier is where exact names live, for a call you are about to construct:
  `references/agent-action-catalog.md`, `references/control-plane-capabilities.md`,
  `references/exchange-mail-calendar.md` and `references/classifier.md`. All four are maps of a live registry, not substitutes for it.
  `platform.act` and `platform.act_batch` are default-deny: use only the exact action
  documented in `references/agent-action-catalog.md`, and never infer generic CRUD from
  the data model.
- Never invent UI labels, paths, or platform facts. If a detail file does not cover it, say
  so or check the live UI/capabilities rather than guess.
- Respect approval gates and access levels (read-only connectors/mounts, role restrictions).
