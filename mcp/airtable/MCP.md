---
name: airtable
description: "Use when an agent needs to work with a user's Airtable bases - list and
  search bases, read table schemas, query, create, update or delete records, comment,
  change tables and fields, build Interfaces, or draft automations. Wraps Airtable's
  official hosted remote MCP server (mcp.airtable.com/mcp) over streamable HTTP. Auth
  is per-user OAuth (the user signs in to Airtable and picks which bases and
  workspaces to share on first connect), so every tool acts as that user with their
  base role. Available on all Airtable plans."
version: 0.1.0
transport: http
url: https://mcp.airtable.com/mcp
keywords:
  - airtable
  - database
  - records
  - spreadsheet
  - automation
  - mcp
  - remote
  - oauth
---

# Airtable MCP Server (remote)

Airtable's official, Airtable-hosted MCP server. This is a **remote** asset: there is
no binary or Docker image to package - the runner opens a streamable-HTTP connection
straight to `https://mcp.airtable.com/mcp`. Tools act as the connected Airtable user
and reach only the bases, workspaces and Interfaces that user granted during sign-in.

## When to reach for this

- The user asks a question **answered by data in Airtable**, or wants records
  **created, updated or deleted**.
- The user wants to **change a base's structure** (tables, fields) or build an
  **Interface** page.
- The user wants an **automation** drafted, inspected or tidied up.

## Tool surface

The exact toolset is advertised by the server at connect time; Airtable's
[tool reference](https://airtable.com/developers/agents/mcp/tools) currently lists:

- Discovery: `list_workspaces`, `list_bases`, `search_bases`, `list_tables_for_base`,
  `get_table_schema`, `list_views_for_table`.
- Records: `list_records_for_table`, `search_records`, `display_records_for_table`,
  `create_records_for_table`, `update_records_for_table`, `delete_records_for_table`,
  `search_candidate_linked_records`, `revert_action`.
- Schema: `create_base`, `create_table`, `update_table`, `delete_table`,
  `create_field`, `update_field`.
- Interfaces and forms: `create_interface`, `delete_interface`, `create_page`,
  `delete_page`, `list_pages_for_base`, `describe_page_type`, `describe_page_element`,
  `list_records_for_page`, `get_record_for_page`, `get_form_schema`, `submit_form`.
- Automations: `list_automations`, `get_automation`,
  `get_create_automation_instructions`, `create_automation`, `update_automation`,
  `delete_automation`, `list_external_accounts`, `fetch_automation_input_data`,
  `test_automation_webhook_trigger`.

## Non-obvious gotchas the agent must respect

- **Per-user identity and per-base consent.** On first connect the platform's
  remote-MCP OAuth path opens an Airtable sign-in where the user chooses which bases
  and workspaces to share. A base missing from `list_bases` was not granted - the
  user changes that at airtable.com under third-party integrations; retrying won't
  help.
- **Base role decides what works.** Owners and creators can change schema and
  records; editors only records; commenters and read-only users only read;
  Interface-only users only see what their Interfaces expose.
- **Discover before writing.** Field names and table IDs come from
  `get_table_schema`; expect a few discovery calls before the first answer.
- **Write-tool caution.** Deleting records, tables or Interfaces changes the user's
  live base for every collaborator. Confirm before `delete_*` and schema changes;
  `revert_action` can undo some recent actions, not all. `create_automation` saves a
  draft that stays off until the user turns it on in the Airtable UI, and only an
  automation that is off can be deleted.
- **Network egress required.** The session sandbox must reach `mcp.airtable.com`
  and, for sign-in and tokens, `airtable.com`.

## Transport

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport`, the same path `github/` and `ideogram/` ship on.

## Authentication (per-user OAuth, bound by the platform)

This manifest carries **no** `Authorization` header and **no** `auth` block - the
same shape as `mcp/ideogram/MCP.md`. Airtable publishes protected-resource metadata
on `mcp.airtable.com`; its authorization server is `airtable.com/oauth2/v1`, which
supports dynamic client registration. The platform's remote-MCP OAuth path handles
the browser sign-in and token storage. (Airtable also accepts personal access
tokens; this entry does not use that route.)

Vendor docs: <https://airtable.com/developers/agents/mcp/getting-started>
