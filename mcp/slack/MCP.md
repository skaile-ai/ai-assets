---
name: slack
description: "Use when an agent needs to work with a Slack workspace as the signed-in
  user - search messages, files, channels and people, read channel and thread history,
  read canvases, lists and user profiles, and (when write scopes are granted) send or
  schedule messages, react, create channels, canvases and lists, or upload files.
  Wraps Slack's official hosted remote MCP server (mcp.slack.com/mcp) over streamable
  HTTP. Per-user OAuth through the organization's own internal Slack app: an
  organization Owner registers that OAuth client in Skaile once first; every member
  then signs in to Slack as themselves and sees only what they can see in Slack."
version: 0.1.0
transport: http
url: https://mcp.slack.com/mcp
keywords:
  - slack
  - chat
  - messages
  - search
  - canvases
  - mcp
  - remote
  - oauth
---

# Slack MCP Server (remote)

Slack's official, Slack-hosted MCP server. This is a **remote** asset: there is no
binary or image - the runner opens a streamable-HTTP connection straight to
`https://mcp.slack.com/mcp`. Every tool runs with the signed-in user's Slack **user
token**, so it sees and does only what that user can in Slack.

Source: [Slack MCP server overview](https://docs.slack.dev/ai/slack-mcp-server),
[Developing with the Slack MCP server](https://docs.slack.dev/ai/slack-mcp-server/developing),
[Connect to agent harnesses](https://docs.slack.dev/ai/slack-mcp-server/connect-to-harnesses).

## When to reach for this

- The user asks what was decided or said in Slack: search messages and files, read a
  channel or a thread, find a person or a channel.
- The user wants Slack content as context for other work (summarise a thread, export
  a canvas as Markdown, read a list).
- The user asks to post, schedule or react in Slack, or to create a channel, canvas
  or list - only if the write scopes were granted (see Scopes).

## Tool surface

The server advertises its tools at connect time, filtered by the scopes on the
user's token - enumerate the live list rather than relying on names here. Slack
documents these capabilities:

- **Search:** messages and files (filter by date, user, content type), users (name,
  email, ID), public and private channels, the user's own channels, custom emoji.
- **Messages:** read channel history, read threads, get reactions; send, schedule and
  draft messages, add reactions, create a channel / group DM / DM.
- **Canvases:** read (as Markdown), create, update.
- **Users:** full profile incl. custom fields and status; list channel members.
- **Files:** read; upload in two steps (`slack_get_file_upload_url`, POST the bytes,
  then `slack_complete_file_upload`).
- **Lists:** create, read, update lists and their records.

## Admin setup (once per organization)

Slack's MCP server supports no dynamic client registration and only accepts clients
backed by a registered Slack app, so Skaile cannot sign in until an organization Owner
registers the organization's own Slack app.

**In Slack** (someone who can create apps in the workspace; installing may need a
workspace admin's approval):

1. In Skaile, open **Organization settings → Providers → OAuth clients for MCP
   servers** and copy the redirect URI shown on that card (it ends in
   `/provider-link/oauth/callback`; copy it, never type a host by hand).
2. At <https://api.slack.com/apps> click **Create New App** (from scratch or from a
   manifest), pick the workspace the app belongs to.
3. In the app's **Agents** section, toggle **On** the **Slack Model Context Protocol
   (MCP) Server** feature.
4. Under **OAuth & Permissions → Redirect URLs**, add the Skaile redirect URI and
   save.
5. Under **OAuth & Permissions → Scopes → User Token Scopes**, add the scopes from the
   table below (the read set, plus any write scopes you want to allow). These are
   **user** scopes; the MCP server does not use bot scopes.
6. Keep the app **internal**: do not activate public distribution. Slack lets only
   internal apps and Slack Marketplace apps use MCP; unlisted distributed apps are
   refused.
7. If the workspace requires app approval, have a workspace admin approve the app
   and its MCP use.
8. From **Basic Information → App Credentials**, copy the **Client ID** and **Client
   Secret**.

**In Skaile** (organization Owner only):

9. On **Organization settings → Providers → OAuth clients for MCP servers** (or the
   command palette: **Add an OAuth client for an MCP server**), click **Add client**.
10. Enter the MCP server URL `https://mcp.slack.com/mcp`, the app's client ID and
    client secret (stored encrypted, never shown again), and the scopes as one
    space-separated list - the same user scopes you added in step 5. Do **not**
    leave scopes empty: the server then picks from everything it advertises,
    including all write scopes. Save; Skaile checks the server's sign-in on save.
11. From then on every member who connects Slack signs in through this app.

## Scopes

User-token scopes, per [Slack's scope table](https://docs.slack.dev/ai/slack-mcp-server#oauth-scopes).

| Use | Scopes | Kind |
|---|---|---|
| Search messages and channels | `search:read.public` `search:read.private` `search:read.mpim` `search:read.im` | read |
| Search files / read files | `search:read.files` `files:read` | read |
| Search users, read profiles | `search:read.users` `users:read` `users:read.email` | read |
| Read channels and threads | `channels:history` `groups:history` `mpim:history` `im:history` | read |
| List channels and members | `channels:read` `groups:read` `im:read` `mpim:read` | read |
| Canvases, lists, reactions, emoji | `canvases:read` `lists:read` `reactions:read` `emoji:read` | read |
| Send / schedule messages | `chat:write` | write, optional |
| Create conversations | `channels:write` `groups:write` `im:write` `mpim:write` | write, optional |
| Reactions, canvases, lists, uploads | `reactions:write` `canvases:write` `lists:write` `files:write` | write, optional |

Smallest useful read set (public channels only):
`search:read.public search:read.users channels:read channels:history users:read`.
Drop the `private` / `groups` / `im` / `mpim` scopes to keep agents out of private
channels and DMs.

## Limits and gotchas

- **Rate limits per tool**, the same as the Web API methods underneath (Tier 2 =
  20+/min for search users/channels, list channels, create channel; message search and
  sending have special limits). Back off on `ratelimited` errors instead of retrying
  in a loop.
- If the Slack app has **allowed IP ranges** configured, calls from Skaile's egress
  must be inside them.
- MCP activity is visible in Slack's audit logs (Enterprise).
- No SSE transport; streamable HTTP only.

## Agent rule

- If connecting fails because no OAuth client is registered, tell the user that an
  organization Owner must register Slack under **Organization settings → Providers →
  OAuth clients for MCP servers** (setup above). **Never ask for a client ID, client
  secret or token in chat.**
- A missing tool usually means the scope was not granted, not a server fault; say
  which scope is missing (table above) and send the user to their Owner.
- Before sending, scheduling or reacting, show the user the exact text and target
  conversation and get their go-ahead: messages go out under the user's own name.

## Authentication (per-user OAuth, bound by the platform)

No `auth` block: like `github/` and `ideogram/`, auth goes through the platform's
remote-MCP OAuth path. Probed 2026-11-14: an unauthenticated `initialize` returns
`401` with `resource_metadata="https://mcp.slack.com/.well-known/oauth-protected-resource"`
(`resource` `https://mcp.slack.com`, same origin as the URL); the authorization
server metadata points to `https://slack.com/oauth/v2_user/authorize` and
`https://slack.com/api/oauth.v2.user.access`, accepts PKCE (S256) with
`client_secret_post`, and publishes no `registration_endpoint`.
