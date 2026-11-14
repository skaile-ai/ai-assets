---
name: canva
description: "Use when an agent needs to work in a user's Canva account - search and
  read designs, generate a design from a text description, edit a design's text and
  elements, autofill brand templates, resize, export (PDF, PNG, JPG, PPTX, MP4),
  upload assets, organize folders, or comment on designs. Wraps Canva's official
  hosted remote MCP server (mcp.canva.com/mcp) over streamable HTTP. Auth is per-user
  OAuth (the user signs in to their own Canva account on first connect), so every
  tool acts as that user and is limited by their Canva plan: brand templates,
  autofill and resize need Canva Pro or above."
version: 0.1.0
transport: http
url: https://mcp.canva.com/mcp
keywords:
  - canva
  - design
  - presentation
  - brand-template
  - export
  - mcp
  - remote
  - oauth
---

# Canva MCP Server (remote)

Canva's official, Canva-hosted MCP server. This is a **remote** asset: there is no
binary or Docker image to package - the runner opens a streamable-HTTP connection
straight to `https://mcp.canva.com/mcp`. Tools run as the connected Canva user and
see only the designs, folders, brand kits and templates that user can see.

## When to reach for this

- The user wants to **find or read** existing Canva designs (search, pages, text
  content, presenter notes, comments).
- The user wants a **new design** from a description (`generate-design`, then
  `create-design-from-candidate`) or from a **brand template** (autofill).
- The user wants to **edit** a design in place, **resize** it for another channel,
  or **export** it as PDF, PNG, JPG, PPTX or MP4.
- The user wants to **upload assets**, organize **folders**, or leave **comments**.

## Tool surface

The exact toolset is advertised by the server at connect time; Canva's
[tool reference](https://www.canva.dev/docs/apps/mcp/tools/) currently lists:

- Designs: `search-designs`, `get-design`, `get-design-pages`, `get-design-content`,
  `get-presenter-notes`, `get-export-formats`, `generate-design`,
  `create-design-from-candidate`, `create-design`, `get-create-design-async-job`,
  `copy-design`, `merge-designs`, `import-design-from-url`, `resolve-shortlink`.
- Editing: `start-editing-transaction`, `perform-editing-operations`,
  `commit-editing-transaction`, `cancel-editing-transaction`, `get-design-thumbnail`.
- Brand templates and autofill (Pro and above): `search-brand-templates`,
  `list-brand-kits`, `create-design-from-brand-template`,
  `create-brand-template-draft`, `publish-brand-template`, `autofill-design`,
  `get-brand-template-dataset`, `get-design-dataset`.
- Export and resize: `export-design`, `resize-design` (Pro and above; a limited trial
  on Free).
- Images and assets: `generate-image`, `get-generate-image-job`, `remove-background`,
  `separate-image-layers`, `get-separate-image-layers-job`, `upload-asset-from-url`,
  `get-assets`, `create-upload-url`.
- Folders and comments: `create-folder`, `list-folder-items`, `search-folders`,
  `move-item-to-folder`, `comment-on-design`, `reply-to-comment`, `list-comments`,
  `list-replies`, plus `help`.

Read the live tool list at connect time rather than hard-coding it; Canva adds tools.

## Non-obvious gotchas the agent must respect

- **Per-user identity, not a shared org account.** On first connect the platform's
  remote-MCP OAuth path opens a Canva sign-in in the browser; every tool then runs
  as *that* user. A permission or auth error means the user is not signed in, their
  session lapsed, or they lack access to that design - re-auth or ask, don't retry
  blindly.
- **Plan limits.** Brand-template, autofill and dataset tools need Canva Pro,
  Business or Enterprise; on Free they fail. `export-design` works on every plan,
  but Free exports are standard quality, and a design with premium elements can fail
  with `license_required`.
- **Edits are transactions.** `start-editing-transaction`, then
  `perform-editing-operations`, then `commit-editing-transaction`; nothing is saved
  until the commit. Cancel the transaction rather than leaving it open when the user
  backs out. Commits change the user's real design, which collaborators also see -
  confirm before editing a shared design, or `copy-design` first.
- **Async jobs.** Design creation, image generation and layer separation return a job;
  poll the matching `get-…-job` tool instead of calling the create tool again.
- **Rate limits.** Per tool, per user, across all of that user's MCP clients: for
  example `generate-image` 10/min, `generate-design` and `export-design` 20/min; tools
  without their own limit share a default of 300/min per user.
- **Network egress required.** The session sandbox must reach `mcp.canva.com`.

## Transport

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport`, the same path `github/` and `ideogram/` ship on.

## Authentication (per-user OAuth, bound by the platform)

This manifest carries **no** `Authorization` header and **no** `auth` block - the
same shape as `mcp/ideogram/MCP.md`. Canva publishes OAuth protected-resource
metadata and supports dynamic client registration on `mcp.canva.com`; the platform's
remote-MCP OAuth path handles the browser sign-in and token storage.

Vendor docs: <https://www.canva.dev/docs/apps/mcp/>
