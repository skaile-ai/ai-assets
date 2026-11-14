---
name: clickup
description: "Use when an agent needs to work in a user's ClickUp workspace - search
  tasks, Docs and chats, create or update tasks, comment, track time, read and edit
  Docs, post in Chat, or navigate Spaces, Folders and Lists. Wraps ClickUp's official
  hosted remote MCP server (mcp.clickup.com/mcp) over streamable HTTP. Auth is
  per-user OAuth (the user signs in to ClickUp on first connect), so every tool acts
  as that user with their ClickUp permissions, and daily call limits depend on the
  workspace plan."
version: 0.1.0
transport: http
url: https://mcp.clickup.com/mcp
keywords:
  - clickup
  - tasks
  - project-management
  - docs
  - time-tracking
  - mcp
  - remote
  - oauth
---

# ClickUp MCP Server (remote)

ClickUp's official, ClickUp-hosted MCP server. This is a **remote** asset: there is
no binary or Docker image to package - the runner opens a streamable-HTTP connection
straight to `https://mcp.clickup.com/mcp`. Every tool acts as the signed-in user,
with that user's ClickUp permissions; the server adds no access of its own.

## When to reach for this

- The user asks about **work in ClickUp**: find tasks, Docs, comments or chat
  messages; summarize a List, a thread or a sprint.
- The user wants to **create or change tasks** (status, assignees, due dates, custom
  fields, tags, dependencies), including bulk changes.
- The user wants to **log or read time**, read or write **Docs**, or post in **Chat**.

## Tool surface

The exact toolset is advertised by the server at connect time; ClickUp's
[tool reference](https://developer.clickup.com/docs/mcp-tools) lists, among others:

- Search: `clickup_search`, `clickup_filter_tasks`.
- Tasks: `clickup_create_task`, `clickup_get_task`, `clickup_update_task`,
  `clickup_delete_task`, `clickup_move_task`, `clickup_add_task_to_list`,
  `clickup_remove_task_from_list`, `clickup_merge_tasks`, dependency, link and tag
  tools, `clickup_get_custom_fields`.
- Comments: `clickup_get_task_comments`, `clickup_get_threaded_comments`,
  `clickup_create_comment`, `clickup_update_comment`, `clickup_delete_comment`.
- Time: `clickup_start_time_tracking`, `clickup_stop_time_tracking`,
  `clickup_add_time_entry`, `clickup_get_time_entries`, time-in-status tools.
- Docs: `clickup_create_document`, `clickup_list_document_pages`,
  `clickup_get_document_pages`, `clickup_create_document_page`,
  `clickup_update_document_page`, page-attachment tools.
- Chat: `clickup_get_chat_channels`, `clickup_get_chat_channel_messages`,
  `clickup_send_chat_message`, `clickup_update_chat_message`.
- Hierarchy and people: `clickup_get_workspace_hierarchy`, List and Folder
  create/update tools, `clickup_get_workspace_members`, `clickup_resolve_assignees`,
  `clickup_get_user`, reminder tools.
- Generic operations: `clickup_get_operators`, `clickup_get_schema`,
  `clickup_execute_operator` (bulk create/update, archive, statuses, views, forms).

## Non-obvious gotchas the agent must respect

- **Per-user identity.** On first connect the platform's remote-MCP OAuth path opens
  a ClickUp sign-in; every tool then runs as *that* user. Comments and chat messages
  are posted under their name, and assignees and watchers get notified. ClickUp
  supports OAuth only - no API keys or personal tokens on this server.
- **Pass `workspace_id` when the user is in several workspaces**; without it the
  server answers with the list of choices instead of acting.
- **Write tools are real and some are irreversible.** ClickUp marks these as
  sensitive: `clickup_delete_task`, `clickup_merge_tasks` (no undo),
  `clickup_delete_comment`, `clickup_update_document_page` (replaces the whole page
  unless `content_edit_mode` is `append`/`prepend`), `clickup_send_chat_message`,
  `clickup_execute_operator`. Confirm with the user before calling them. Creating
  tasks or time entries is not idempotent: a retry makes a duplicate. Don't retry a
  timed-out `clickup_execute_operator`; the operation may have completed.
- **Daily call limits by plan.** Without the Everything AI add-on a workspace gets a
  rolling 24-hour budget per client (Free 100, Unlimited 300, Business 1,000, up to
  Enterprise Plus 25,000 calls). Exceeding it returns `RATE_LIMIT_EXCEEDED` with
  `retryAfter`; the limit cannot be reset. Batch with the bulk operators and avoid
  exploratory loops.
- **Network egress required.** The session sandbox must reach `mcp.clickup.com`.

## Transport

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport`, the same path `github/` and `ideogram/` ship on.

## Authentication (per-user OAuth, bound by the platform)

This manifest carries **no** `Authorization` header and **no** `auth` block - the
same shape as `mcp/ideogram/MCP.md`. ClickUp publishes OAuth protected-resource
metadata and supports dynamic client registration on `mcp.clickup.com`; the
platform's remote-MCP OAuth path handles the browser sign-in and token storage.

Vendor docs: <https://developer.clickup.com/docs/connect-an-ai-assistant-to-clickups-mcp-server>
