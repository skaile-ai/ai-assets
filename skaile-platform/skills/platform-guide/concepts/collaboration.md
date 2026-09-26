# Collaboration: Multi-User, Sharing, A2A

How people (and other agents) share and work together in the platform.

## Multi-user sessions

Multiple humans can chat in the same session alongside the agent. Each incoming message
carries context about who is online, who sent it, and whether the agent was @mentioned.

- **Mentions** — `@agent`, `@here`, `@all` address the agent; `@<name>` / `@humans`
  address specific people. The agent responds when @mentioned or when it can meaningfully
  contribute; it stays silent (internally `[PASS]`) when a message is human-to-human.
- **Reactions** — emoji reactions on messages (the agent can react too, as a lightweight
  acknowledgment).
- **Threading** — replies can be threaded to a parent message.
- **Presence** — who is online / typing / focused, surfaced as avatars in the toolbar's
  right zone.

When a new user joins mid-session, treat them as entering a shared context — do **not**
assume they hold the same authorizations as the original user, and do not reveal which
user sent which message unless asked.

## Sharing a session with people

- Project roles are Owner / User / Viewer; session roles are Owner / User / Viewer.
- Who can share: a **shared** session — its Project Owner or Session Owner; a **private**
  session — its Session Owner only. Inviting someone by email to a project needs Org Owner
  or Project Owner; to a session, Org Owner or Session Owner.
- Session-access presence: clicking the presence avatars in the toolbar's right zone shows
  everyone with read access to the session, grouped online/offline.

## Notifications

Each member chooses when they are notified: **All**, **Mentions** (the default),
**Direct**, or **Off**, or inherits the setting from the project or their defaults.

- Set per project or per session from the actions menu's notifications submenu, and
  account-wide under **Settings > Notifications**.
- The agent can change the asking member's mode for the current session on request. It
  declines when several members wrote in the same turn, since it cannot tell who asked.
- Browser notifications name the project and session.

## Public file-preview sharing

A **Session Owner** can create a **revocable public link** to share a single workspace
file preview (e.g. a report) with someone **outside** the platform — no login required.

- Links carry a required expiry (7-day default, 30-day max) and can be revoked by the
  Session or Project Owner.
- Only the one file (plus assets in its directory) is exposed; references outside that
  directory will not load, and a pre-flight check warns about them before sharing.
- A link to a hibernated session still works: the viewer sees a "waking this workspace"
  page for 30–60 seconds. Wakes through a link are budgeted (24 per day per link, and on
  broker deployments 24 per day per session across all its links; failed wakes count).
- This is opt-in infrastructure — it only works when the deployment has configured a
  dedicated public-share origin.

## Agent-to-Agent (A2A)

Sessions can talk to each other's agents through directed, two-sided opt-in links.

- A session must be opened to peers (**Allow other sessions to reach this one**) and may
  declare a **Scope** describing what it is willing to do for them before it can be linked.
- Once linked, the agent can **ask** a peer session's agent (waits up to 5 minutes for the
  answer, then reports it as pending) or **send** to it (fire-and-forget).
- Exchanges are bounded: at most 4 hops, cycle detection, and a budget of 20 messages
  between a pair with no human turn in either session — after that the agent stops,
  summarizes, and reports back to its user.
- Users manage this in session settings, **Config** tab > **Agent to agent communication**
  (open-state, scope, linked agents, **Incoming links**), and per agent in the agent
  dialog's **Agent communication** toggles. Inbound A2A messages render distinctly in the
  chat.
- In Expert Mode, the card also lists **Other organizations**: sessions the user owns in
  another organization can be linked by hand. The agent itself only discovers and links
  within its own organization.

Source of truth: `platform/docs/protocol-extensions.md`,
`platform/docs/public-file-preview-sharing.md`, `platform/backend/libs/agent-to-agent/`,
platform PRs #4917 (notification modes), #4566 (cross-org A2A), #5152 (share wake budget),
`platform/docs/roles-permissions-matrix.md` (share and invite permissions).
