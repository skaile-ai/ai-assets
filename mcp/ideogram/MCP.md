---
name: ideogram
description: "Use when an agent needs to create or transform images with Ideogram -
  generate images from a text prompt (incl. high-quality typography), batch-generate
  several at once, edit/inpaint an existing image, reframe/outpaint to a new aspect
  ratio, or upscale. Wraps Ideogram's official hosted remote MCP server
  (mcp.ideogram.ai/mcp) over streamable HTTP. Auth is per-user OAuth (the user signs
  in to their own Ideogram account on first connect), so generations draw on the
  connected user's own Ideogram subscription and credits. Reach for this when image
  work should bill to each user's personal Ideogram plan; for a central, org-billed
  API key use the `ideogram-image` skill instead."
version: 0.1.0
transport: http
url: https://mcp.ideogram.ai/mcp
keywords:
  - ideogram
  - image
  - generation
  - edit
  - upscale
  - mcp
  - remote
  - oauth
---

# Ideogram MCP Server (remote)

Ideogram's official, Ideogram-hosted MCP server. This is a **remote** asset: there
is no binary or Docker image to package - the runner opens a streamable-HTTP
connection straight to `https://mcp.ideogram.ai/mcp`. Tools run as the connected
Ideogram account and consume that account's plan credits.

## When to reach for this

- The user asks to **generate** an image (or several) from a text prompt - posters,
  logos, illustrations, photoreal scenes, and especially legible in-image text.
- The user wants to **edit** an existing image (inpaint a region), **reframe** it
  (outpaint to a new aspect ratio), or **upscale** it.
- The work should bill to the **connected user's own Ideogram subscription** rather
  than a shared org account.

For a central, org-billed key (one Ideogram API account, central billing) reach for
the **`ideogram-image`** skill instead - it calls the REST API with a
platform-injected key and does not require each user to hold an Ideogram login.

## Tool surface

The exact toolset is advertised by the server at connect time; it includes:

- `generate_images_bulk` - generate one or more images from a prompt (batch).
- `edit_image` - inpaint / edit a region of an existing image with a mask.
- `reframe_image` - outpaint / extend an image to a new aspect ratio.
- `upscale_image` - increase resolution of an existing image.
- `upload_image` - upload a local/source image so the edit/reframe/upscale tools can
  operate on it.

Enumerate the live tool list at connect time rather than hard-coding it - Ideogram
adds and renames tools (e.g. describe/remix variants).

## Non-obvious gotchas the agent must respect

- **Per-user identity, not a shared org key.** On first connect the platform's
  remote-MCP OAuth path opens an Ideogram sign-in in the browser; every tool then
  runs as *that* user. Two users of the same asset are two different Ideogram
  accounts. A connection/permission error means the user is not signed in or their
  Ideogram session lapsed - re-auth rather than retrying blindly.
- **Consumes the connected user's own plan credits.** Generations, upscales and edits
  draw down the signed-in user's Ideogram subscription / credit balance - not a
  central org account. Heavy batch runs spend real credits; size `num_images`
  sensibly and tell the user when a request will be large.
- **Results come back as hosted image URLs.** Tool results carry Ideogram-hosted
  image URLs. Surface them to the user as markdown `![alt](url)` so they render inline
  in the Skaile chat. The URLs are **time-limited** - if the user wants to keep a
  result, download it into the session workspace promptly rather than relying on the
  link later.
- **Network egress required.** The session sandbox must be able to reach
  `mcp.ideogram.ai`. Under locked-down session networking the connection fails fast;
  that is a config issue, not a tool bug.

## Transport

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport`
(`workspaces/packages/workspaces/runner/src/external-mcp.ts`). The `MCP.md` manifest
schema accepts `http` and normalizes the MCP-spec alias `streamable-http` to it as of
skaile-ai/workspaces#174. Publish this asset only once that change is released;
against an older resolver `http` fails manifest validation and `streamable-http`
connects to nothing.

## Authentication (per-user OAuth, bound by the platform)

This manifest deliberately carries **no** `Authorization` header and **no** `auth` /
`providerLinkId` - the same shape as `mcp/github/MCP.md`. Ideogram is a per-user
OAuth remote MCP: the platform's existing remote-MCP OAuth path handles the
browser sign-in on first connect and provisions the resulting token off-disk, then
rewrites the request header. There is no static key to bake into this shared catalog
entry.

This is the key contrast with the `ideogram-image` skill: that route is a single,
central **API key** (`auth: { type: api-key, inject: env, env: IDEOGRAM_API_KEY }`)
collected once by an admin and billed to one org-wide Ideogram API account. See
`mcp/DOMAIN.md` ("Authentication declarations") for how the two `inject` modes differ.
