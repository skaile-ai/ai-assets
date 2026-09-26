# Conceptual Model

The mental model behind everything the user sees. Internally there are technical
terms (mounts, worktrees, containers); the user-facing vocabulary is simpler.
Always speak in the user-facing terms unless the user is technical.

## The hierarchy

```
Organization ──1:N── Project ──1:N── Session
Project      ──1:1── Project data (On Skaile, a git repo, a cloud drive, or a local folder)
Session      ──1:1── Workspace (the session's working view of the project data)
```

- **Organization** — the company/tenant. Users, projects, and integrations belong to it.
  Every user also has a private single-member organization, shown as **My Workspace**,
  which hosts their personal assistant. The sidebar toggle switches between **Personal**
  and **Business**; personal mode uses a warmer colour palette. Only platform
  administrators create new organizations, and a shared organization must keep at least
  one active Owner.
- **Project** — a unit of work with its own **project data** (one data source) and its
  own set of enabled **assets** and **connectors**. A project has a **main session**
  (the primary workspace) and any number of additional sessions.
- **Session** — a workspace where the user chats with the agent and work happens. In the
  UI a session is presented as an **agent** (see below). How far sessions are isolated from
  each other depends on the source type (next section).
- **Workspace** — the files and data the agent and user work on inside a session. Backed
  on disk per session; the running container is just a compute wrapper around it.

## Project data (source types)

A project's data comes from one **source type**, chosen at project creation. The wizard
offers **On Skaile** (the default — Skaile keeps the files, no external provider),
**Git Repository**, **SharePoint / OneDrive**, **Google Drive**, **NextCloud**, **Box**,
and **Local Folder**. Projects an agent creates may also start **Empty**.

- **Git** — each session works on its own branch and worktree; closing merges the branch
  back to main.
- **On Skaile / Local Folder** — sessions of the project work on the same project folder.
- **Cloud drives** (SharePoint / OneDrive, Google Drive, NextCloud, Box) — the drive is
  mounted live; the project's mount always uses the **project owner's** connection.

User-facing terms: "data source + mounts" = **project data**; "create project + mount"
= **create project**; "session workspace" = **session**.

## Explorer and agents

In the Explorer, each project lists its **Apps** (green dot while serving), its live
sessions, its **Flows**, and an **Archive** of closed and archived sessions.

Each session carries an **agent** identity — name (also its @handle), picture, voice,
description, and instructions — set in the **New agent** / **Edit agent** dialog. A picture
can be generated from a text prompt with **Generate** where the deployment has image
generation configured. The agent can propose renaming itself; the user approves it.

The **Agent graph** (project menu or Cmd+K) shows a project's agents as cards, their
agent-to-agent links as arrows, and the project's apps and flows alongside.

## Connectors vs. mounts

Both bring external systems into a session, but differently:

- **Mounts** mount external data into the workspace **as files** the agent reads/writes
  directly — git, local folder, SharePoint / OneDrive, Google Drive, Box, WebDAV /
  NextCloud, S3. The project's main data source is itself a mount.
- **Connectors** expose external systems as **tools** the agent calls (not files) —
  Postgres, Redis, SQLite, and the shared `session`/`presence` state stores. Auth,
  access policy, and audit logging are handled by the platform's connector runtime.

Each connector/mount has an **access level** (read-only vs read-write). The agent must
respect it — never attempt writes against a read-only resource.

## Assets (skills)

An **asset** (often called a **skill**) is a packaged capability — a set of tools plus a
policy declaring which connectors it may touch and at what access level. A project enables
the assets it needs. Assets constrain the attack surface: a research asset has no
"send email" tool, so the agent simply cannot do that. Enabling an asset is what gives the
agent a new capability; the agent then discovers the concrete actions at runtime (see
`concepts/agent.md`).

## Flows

A **flow** is a multi-step pipeline (an acyclic graph of **nodes**) the agent can run on the
user's behalf — for repeatable, structured work rather than a single chat turn. Each node
picks how much intelligence its step needs, from a full agent turn down to deterministic
code with no model call at all. A running flow
has a panel in the workspace and survives session hibernation: on wake it is rehydrated
in the same state and the next user action (approval, input, message) resumes it.
Flows are assets with five-scope ownership, have an org-level Flows page with a visual
editor, and can be fanned out over many inputs as a **run group** — see
`concepts/flows.md` for the full model (editing, gates, run groups, recipes, webhooks).

The `session` state store tracks pipeline context (`activePhase`, `phaseStatus`,
`pipelineProgress`, `mode`) when a session is running a pipeline.

## Roles and permissions

A user holds **three independent roles at once** — one each for **Org**, **Project**, and
**Session** — each being Viewer / User / Owner, plus an optional **PlatformAdmin** flag.

Reading rule: a user may use a feature as soon as **at least one** of their roles allows
it (Org OR Project OR Session OR Admin) — with two exceptions where the most-specific
scope wins instead:

- **Sending messages / talking to the agent** — a Session (or Project) Viewer is
  write-locked even if they are an Org User/Owner; the composer goes read-only.
- **Transferring ownership** — most-specific scope wins.

Other notable rules:

- **Private sessions** are visible only to the Session Owner and explicit session members
  — not even to the Project Owner or PlatformAdmin.
- A **Shared** project/session is visible to Org Users/Owners and project/session members.
- Creating a session (including a scoped session) needs a project role of **User** or
  **Owner**. **Forking, reopening, or discarding** a session requires **Org Owner**.
- A project can also be shared with a **team** at a role (Owner / User / Viewer), which
  the team's members then hold on that project.

Full matrix: `platform/docs/roles-permissions-matrix.md`.

Grounded in: `platform/docs/roles-permissions-matrix.md`, `platform/docs/scoped-sessions.md`,
`platform/docs/mount-connection-binding.md` (owner invariant), the new-project wizard's
source picker, the `is_personal` organization field, platform PRs #3760 (org creation),
#4281 (last Owner), #5023 (Personal/Business), #5076/#5354 (Explorer sections), #5251
(agent graph), #5336/#5352 (agent rename, picture generation), `team-sharing.service.ts`.
