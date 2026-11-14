---
name: hubspot
description: "Use when an agent needs to read or update a HubSpot account's CRM and
  marketing data - search and fetch contacts, companies, deals, tickets and other CRM
  records, query CRM data, read activities (calls, emails, meetings, notes, tasks),
  conversations, campaigns, content and email analytics, and (with confirmation)
  create or update records. Wraps HubSpot's official hosted remote MCP server
  (mcp.hubspot.com) over streamable HTTP. Per-user OAuth through the organization's
  own HubSpot MCP connector: an organization Owner registers that OAuth client in
  Skaile once first; every member then signs in to HubSpot as themselves and sees only
  what their HubSpot permissions allow."
version: 0.1.0
transport: http
url: https://mcp.hubspot.com
keywords:
  - hubspot
  - crm
  - contacts
  - deals
  - marketing
  - mcp
  - remote
  - oauth
---

# HubSpot MCP Server (remote)

HubSpot's official, HubSpot-hosted MCP server. This is a **remote** asset: there is
no binary or image - the runner opens a streamable-HTTP connection straight to
`https://mcp.hubspot.com`. Every tool runs as the HubSpot user who signed in and
respects that user's HubSpot permissions.

Source: [Integrate AI tools with the HubSpot MCP server](https://developers.hubspot.com/docs/apps/developer-platform/build-apps/integrate-with-the-remote-hubspot-mcp-server),
[GA changelog (2026-04-13)](https://developers.hubspot.com/changelog/remote-hubspot-mcp-server-is-now-generally-available).

## When to reach for this

- The user asks about CRM data: find a contact, company, deal or ticket, list deals
  in a stage, summarise the last calls or emails with a prospect, look up an owner.
- The user wants marketing or content figures: campaign analytics and attribution,
  marketing-email statistics, landing page / blog / website page analytics, social
  post performance.
- The user wants to log or change something in HubSpot (create a task, update a deal,
  add a note) - the server asks for confirmation before every write.

## Tool surface

The server advertises its tools at connect time; availability depends on the HubSpot
subscription, the user's permissions and the scopes they granted. Call
`get_user_details` first: it returns the user, the account, and per-object read and
write access. Documented tools include:

- **Account and schema:** `get_user_details`, `get_organization_details`,
  `discover_hubspot_schema`, `search_properties`, `get_properties`, `search_owners`,
  `tool_guidance` (usage notes for other tools; call it before `query_crm_data` and
  campaign attribution).
- **CRM read:** `search_crm_objects` (filter groups, max 200 per page),
  `get_crm_objects` (max 100 IDs), `query_crm_data` (SQL-like; no joins, aliases,
  subqueries or association traversal).
- **CRM write:** `manage_crm_objects`, `manage_segment`, `manage_custom_properties`,
  `manage_custom_pipelines`.
- **Conversations:** `get_conversation_channel_metadata`, `search_conversations`.
- **Marketing and content:** `read_campaign_data`, `get_campaign_attribution_reports`,
  `manage_campaign_objects`, `get_marketing_email_analytics`, `manage_marketing_email`,
  `get_content_analytics_report`, `manage_landing_page`, `search_intent_signals`,
  social (`get_social_channels`, `read_social_posts`, `get_social_post_analytics`,
  `manage_social_posts`), AEO (`get_aeo_metrics`, `manage_aeo_prompts`,
  `manage_aeo_recommendations`; Marketing Hub Professional/Enterprise).
- `manage_website_page`, `manage_blog_post` and `render_asset` are, per HubSpot, only
  offered to select AI clients (Claude, ChatGPT, Gemini) and may not appear here.

## Admin setup (once per organization)

HubSpot's MCP server offers no dynamic client registration, so Skaile cannot sign in
until an organization Owner registers the organization's own HubSpot OAuth client.

**In HubSpot** (a user with access to the account's **Development** area):

1. In Skaile, open **Organization settings → Providers → OAuth clients for MCP
   servers** and copy the redirect URI shown on that card (it ends in
   `/provider-link/oauth/callback`; copy it, never type a host by hand).
2. In HubSpot, go to **Development → MCP Connectors** (direct link:
   <https://app.hubspot.com/l/mcp-auth-apps/>; the GA changelog calls it **MCP Auth
   Apps**) and click **Create MCP connector**.
3. Enter a **Name** (e.g. "Skaile"), optionally a description and icon, and paste the
   Skaile redirect URI as the **Redirect URL**. If you list several redirect URLs,
   the first one is the default. Click **Create**.
4. On the connector's details page, copy the **Client ID** and **Client secret**.

**In Skaile** (organization Owner only):

5. On **Organization settings → Providers → OAuth clients for MCP servers** (or the
   command palette: **Add an OAuth client for an MCP server**), click **Add client**.
6. Enter the MCP server URL `https://mcp.hubspot.com`, the connector's client ID and
   its client secret (stored encrypted, never shown again). Leave **scopes empty**:
   HubSpot connectors have no scopes to configure (see below). Save; Skaile checks
   the server's sign-in on save.
7. From then on every member who connects HubSpot signs in through this connector.

## Scopes

| What | Where it is set |
|---|---|
| Connector scopes | None to configure. HubSpot derives them from the server's current tools and from what the user grants on the consent screen. Leave the Skaile scopes field empty (the metadata's `scopes_supported` is `[]`). |
| Read vs write | Decided per user at consent and by the user's HubSpot permissions; write tools additionally ask for confirmation before acting. |
| Social posting | Needs the `social.publish` permission granted; `social.read` / `social.edit` alone are read-only. |
| Quotes (beta) | Needs the optional `manage revenue objects` permission and Revenue Hub Professional/Enterprise. |

When HubSpot adds tools, new scopes appear and an existing connection may show
`REQUIRES_REAUTHORIZATION`; the user disconnects and reconnects to grant them.

## Limits and gotchas

- **PKCE is mandatory**, and the token endpoint takes the client secret
  (`client_secret_post`); the platform handles both.
- **Sensitive Data:** if the HubSpot account has Sensitive Data turned on,
  conversation and activity data is blocked through the MCP server (not through the
  normal CRM API).
- **Per-user scope:** results reflect the signed-in user's HubSpot permissions; an
  empty result can mean "no access", not "no data". Check `get_user_details`.
- Search is the CRM search API underneath: no vector / semantic search.
- Restricted conversations inboxes are visible only to their assigned users.

## Agent rule

- If connecting fails because no OAuth client is registered, tell the user that an
  organization Owner must register HubSpot under **Organization settings → Providers →
  OAuth clients for MCP servers** (setup above). **Never ask for a client ID, client
  secret or token in chat.**
- Before any write, show the user what will change; the server's own confirmation
  step is not a substitute for asking.

## Authentication (per-user OAuth, bound by the platform)

No `auth` block: like `github/` and `ideogram/`, auth goes through the platform's
remote-MCP OAuth path. Probed 2026-11-14: an unauthenticated `initialize` returns
`401` with `resource_metadata="https://mcp.hubspot.com/.well-known/oauth-protected-resource"`;
the authorization server (`https://mcp.hubspot.com`) publishes no
`registration_endpoint`, hence the organization-registered client.
