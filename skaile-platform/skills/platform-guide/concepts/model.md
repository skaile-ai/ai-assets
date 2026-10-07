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
  There are two kinds. A **business organization** is a company's. A **Private
  workspace** is one person's own organization (up to six members, the owner included,
  invited by the owner; on an org subdomain, which is off in production, it is at
  `/private` and old `/personal` links redirect). A user gets a Private workspace when an
  organization sponsors one for them (or a platform administrator creates it). It is paid
  for by an employer organization or a platform admin; there is no self-service payment
  yet, so never send a user looking for one. A Private workspace that existed before Work
  and Private spaces were switched on is kept as it was and never locks. With nobody
  paying, it gets 30 days of grace with a notice, then turns read-only
  (`ui/navigation.md`). A user's **home
  Private workspace** is the one they own or, if they own none, the first one they joined
  (which can be someone else's); it is where their home assistant lives. The **Work** /
  **Private** switch at the top of the sidebar moves between a business organization and
  that home Private workspace, and is shown only to someone who has both; the home Private
  workspace uses a warmer colour palette. Only platform administrators create new
  organizations, and a business organization must keep at least one active Owner.
- **Home and the assistant** — in every organization where they are a User or Owner, a
  member has a **Home**: a private project, first in their **My space**, whose main session
  is their **assistant** there. The assistant in their home Private workspace is their
  **home assistant**; every other one is an organization assistant. A Viewer has no Home.
  All of a user's assistants share one profile (picture, voice; the **Your assistant** page),
  but each has a name of its own, so the Private one and a work one can be called
  differently, and renaming one never renames another. The assistant is opened from the round
  launcher button at the end of the user row in the sidebar. The member can add more
  projects to My space (**New My space project**); they open only for their owner, and one
  can be moved to Projects (one-way) to share it. A Home never moves.
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

The Explorer groups an organization's projects into **Company** (projects an org Owner
marked as company projects; business organizations only), **Projects**, and **My space**
(the user's own private projects, their Home first under the assistant's name). Each
project lists its **Apps** (green dot while serving), its live sessions, its **Flows**, and
an **Archive** of closed and archived sessions.

Each session carries an **agent** identity — name (also its @handle), picture, voice,
description, and instructions — set in the **New agent** / **Edit agent** dialog. A picture
can be generated from a text prompt with **Generate** where the deployment has image
generation configured. The agent can propose renaming itself; the user approves it.

The **Project graph** (project menu or Cmd+K) shows a project's agents as cards, their
agent-to-agent links as arrows, and the project's job descriptions, apps and flows in boxes
alongside. Clicking a job description's card opens a dialog whose button is **Take on a
temporary hire**, with an optional first message, and **Open latest** when one is running; the
pen on the card opens the job description's edit dialog, as **Edit job description…** does; the
graph's Add menu has **New job description** and **New temporary hire…** (which first asks
which job description). A project Owner can arrange the graph: cards drag, and every box (the
**Job descriptions**, **Apps** and **Flows** boxes as well as groups the user adds with **New
Group**) moves, resizes, renames and deletes; **Reset layout** restores the
automatic arrangement.

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
scope wins instead, and one rule above all of them:

- **A private project is its owner's alone.** The personal assistant's own project (its
  Home), and any project in a user's My space, opens only for that user. No Org role,
  Project role or PlatformAdmin reaches it, nor does whoever inherits the owner's other
  projects when the owner leaves the organization or is deleted (a deleted user's private
  projects are sealed for everyone). A leaver's Home — and in a Private workspace, all of
  their My space — is archived, not handed over, and stays closed to everyone while
  archived. It comes back if they rejoin within 30 days; after that it is deleted. Their
  other My space projects in a business organization stay closed until an org Owner takes
  one over (it moves to Projects as theirs) or deletes it, under **Organization settings >
  Former members' My space** (the **Users** tab's callout to it shows only in Expert mode;
  the tab itself still opens by link).

- **Sending messages / talking to the agent** — a Session (or Project) Viewer is
  write-locked even if they are an Org User/Owner; the composer goes read-only.
- **Transferring ownership** — most-specific scope wins.

Other notable rules:

- **Private sessions** are visible only to the Session Owner and explicit session members
  — not even to the Project Owner or PlatformAdmin.
- A **Shared** project/session is visible to Org Users/Owners and project/session members —
  but never inside someone's private project, where only its owner sees anything.
- Creating a session (including a scoped session) needs a project role of **User** or
  **Owner**. **Forking, reopening, or discarding** a session requires **Org Owner**.
- A project can also be shared with a **team** at a role (Owner / User / Viewer), which
  the team's members then hold on that project.

Full matrix: `platform/docs/roles-permissions-matrix.md`.

Grounded in: `platform/docs/roles-permissions-matrix.md`, `platform/docs/scoped-sessions.md`,
`platform/docs/mount-connection-binding.md` (owner invariant), the new-project wizard's
source picker, the `is_personal` organization field, platform PRs #3760 (org creation),
#4281 (last Owner), #5023 (Personal/Business), #5076/#5354 (Explorer sections), #5251
(agent graph, renamed Project graph in #6240; editable boxes #6244), #5336/#5352 (agent
rename, picture generation), #5988 (Private workspace seats), #6008 (private projects:
`canAccessMySpaceProject`), #6044 (a leaver's Home is
archived, not inherited, and deleted after 30 days), #6228 (each assistant has its own name),
`team-sharing.service.ts`. Work and
Private spaces with the rollout flag on: `landing.utils.ts`, `my-space.utils.ts`,
`private-sponsorship.service.ts`, `sidebar-projects-tree.helpers.ts`,
`orphaned-my-space.page.tsx`.
Job descriptions on the project graph (card dialog, pen, Add menu entries):
`pages/project-graph/`, `agent-templates/start-instance-dialog.tsx`; skaile-ai/platform#6315
(the labels) and #6312 (the vocabulary).
The former members callout on the Users tab, shown only in Expert mode: skaile-ai/platform#6690.
