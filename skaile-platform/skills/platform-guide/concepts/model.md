# Conceptual Model

The mental model behind everything the user sees. Internally there are technical
terms (mounts, worktrees, containers); the user-facing vocabulary is simpler.
Always speak in the user-facing terms unless the user is technical.

## The hierarchy

```
Organization ──1:N── Project ──1:N── Session
Project      ──1:1── Project data (a git repo, SharePoint site, local folder, or empty)
Session      ──1:1── Workspace (an isolated working copy of the project data)
```

- **Organization** — the company/tenant. Users, projects, and integrations belong to it.
- **Project** — a unit of work with its own **project data** (one data source) and its
  own set of enabled **assets** and **connectors**. A project has a **main session**
  (the primary workspace) and any number of additional sessions.
- **Session** — an isolated workspace where the user chats with the agent and work
  happens. Each session has its own copy of the project data; changes in one session
  do not affect another until the session is closed and synced back to main.
- **Workspace** — the files and data the agent and user work on inside a session. Backed
  on disk per session; the running container is just a compute wrapper around it.

## Project data (source types)

A project's data comes from one **source type**, chosen at project creation. This
determines how sessions are isolated and how a closed session syncs back.

| Source type   | Where data comes from                          | Session isolation        | On close |
| ------------- | ---------------------------------------------- | ------------------------ | -------- |
| **Git**       | New repo, or clone from GitHub/GitLab/Bitbucket | New branch + worktree    | Merge branch to main |
| **SharePoint**| Linked SharePoint site/library                  | Fresh delta-sync copy    | Sync changes back |
| **LocalFolder**| A folder on the host (local/dev deployments)   | Filesystem copy          | Copy changes back |
| **Empty**     | Nothing — the agent populates it                | Empty directory          | No sync needed |

User-facing terms: "data source + mounts" = **project data**; "create project + mount"
= **create project**; "session workspace" = **session**.

## Connectors vs. mounts

Both bring external systems into a session, but differently:

- **Mounts** mount external data into the workspace **as files** the agent reads/writes
  directly — git, local folder, S3, WebDAV, SharePoint. The project's main data source
  is itself a mount.
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

A **flow** is a multi-step pipeline (a DAG of steps) the agent can run on the user's
behalf — for repeatable, structured work rather than a single chat turn. A running flow
has a panel in the workspace and survives session hibernation: on wake it is rehydrated
in the same state and the next user action (approval, input, message) resumes it.

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
- Only **Project Owners** create sessions (including scoped sessions). **Forking,
  reopening, or discarding** a session requires **Org Owner**.

Full matrix: `platform/docs/roles-permissions-matrix.md`.
