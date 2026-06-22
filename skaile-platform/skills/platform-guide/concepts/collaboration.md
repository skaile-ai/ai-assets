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
- **Presence** — who is online / typing / focused, surfaced in the workspace header.

When a new user joins mid-session, treat them as entering a shared context — do **not**
assume they hold the same authorizations as the original user, and do not reveal which
user sent which message unless asked.

## Sharing a session with people

- A **Project Owner** invites users to a project (Owner / User / Viewer roles); a
  **Session Owner** invites to a session (Owner / Participant). Session membership requires
  project membership.
- Inviting people may change how the agent accesses connected systems (credential mode
  shifts from the owner's delegated credentials toward a service account) — relevant when
  the user asks why a connector behaves differently after sharing.
- Session-access presence: the workspace header shows everyone with read access to the
  session, grouped online/offline.

## Public file-preview sharing

A **Session Owner** can create a **revocable public link** to share a single workspace
file preview (e.g. a report) with someone **outside** the platform — no login required.

- Links carry a required expiry (7-day default, 30-day max) and can be revoked by the
  Session or Project Owner.
- Only the one file (plus assets in its directory) is exposed; references outside that
  directory will not load, and a pre-flight check warns about them before sharing.
- This is opt-in infrastructure — it only works when the deployment has configured a
  dedicated public-share origin.

## Agent-to-Agent (A2A)

Sessions can talk to each other's agents through directed, two-sided opt-in links.

- A session must be marked **open** (and may declare an **external scope** describing what
  it is willing to do for peers) before it can be linked.
- Once linked, the agent can **ask** a peer session's agent (synchronous, waits for the
  answer) or **send** to it (fire-and-forget). Exchanges are bounded (max hops, cycle
  detection).
- The user manages open-state and links from the session's external-comms settings;
  inbound A2A messages render distinctly in the chat.

Source of truth: `platform/CLAUDE.md` (A2A, multi-user, public-share sections),
`platform/docs/protocol-extensions.md`.
