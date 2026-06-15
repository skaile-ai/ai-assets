---
name: github
description: "GitHub's official remote MCP server (api.githubcopilot.com/mcp). Repos, issues, pull requests, Actions, code search and more, scoped by the connected GitHub identity. Hosted by GitHub - no local runtime to build or deploy."
version: 0.1.0
transport: http
url: https://api.githubcopilot.com/mcp/
keywords:
  - github
  - git
  - repos
  - issues
  - pull-requests
  - actions
  - code-search
  - mcp
  - remote
---

# GitHub MCP Server (remote)

GitHub's official, GitHub-hosted MCP server. This is a **remote** asset: there is
no binary or Docker image to package - the runner opens a streamable-HTTP
connection straight to `https://api.githubcopilot.com/mcp/`. Tools are scoped to
whatever the connected GitHub identity is allowed to do.

## Transport

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport`
(`workspaces/packages/workspaces/runner/src/external-mcp.ts`). The `MCP.md`
manifest schema accepts `http` and normalizes the MCP-spec alias
`streamable-http` to it as of skaile-ai/workspaces#174
(`workspaces/packages/workspaces/types/src/manifests/mcp-server.ts` +
`core/src/workspace-config.ts`). Publish this asset only once that change is
released; against an older resolver, `http` fails manifest validation and
`streamable-http` connects to nothing.

## Authentication (bound per organization, not here)

This manifest deliberately carries **no** `Authorization` header and **no**
`auth` / `providerLinkId`. Those are install-specific and are applied by the
platform when the asset instance is enabled, not baked into the shared catalog
entry:

- The platform sets `auth: backend` + a GitHub OAuth `providerLinkId` on the
  `McpServerEntry`
  (`platform/backend/libs/session/src/skaile-config.types.ts`).
- At session start, `mintAndProvisionMcpCredentials`
  (`platform/backend/libs/session/src/mcp-credential-provision.ts`) mints the
  user's GitHub token via `CredentialMediatorService.mintAccessToken`, provisions
  it off-disk as `MCP__github__AUTH`, and rewrites the request header to
  `Authorization: env:MCP__github__AUTH`.

To wire the org side:

1. Configure the platform's GitHub OAuth app credentials - either per-link
   (`oauthClientId` / `oauthClientSecret` on the ProviderLink) or the
   platform-wide GitHub OAuth app in `gitProviderConfig`.
2. Create a `ProviderLink` with `category: Git`, `providerType: GitHub`,
   `credentialMechanism: UserDelegation` (tRPC
   `providerLinkActions.createProviderLink`).
3. Have each user connect their GitHub account
   (`providerLinkActions.startOAuthFlow` / `completeOAuthFlow`).
4. Bind this MCP instance to that link in the remote-MCP widget
   (`platform/frontend/src/components/ui/configure-instance/remote-mcp-widget.tsx`)
   - it writes `auth: backend` + `providerLinkId` into the session config.

The remote endpoint accepts the GitHub user OAuth token as
`Authorization: Bearer <token>`; the backend-auth flow supplies exactly that.

## Scoping (read-only / toolsets)

The remote server exposes scoped variants via URL path - e.g. a read-only or
per-toolset endpoint. To restrict capabilities, override `url` on the instance
(for example a read-only path) rather than editing this default manifest.

## Local alternative

A local stdio variant exists (`ghcr.io/github/github-mcp-server`, run as
`github-mcp-server stdio`, PAT in `GITHUB_PERSONAL_ACCESS_TOKEN`, with
`--toolsets` / `--read-only` / `--lockdown-mode` flags). If data residency or
locked-down session egress later rules out the hosted endpoint, package the Go
binary as a Nix recipe (`mcps.github`) mirroring `mcp/xls` and ship a second
manifest with `transport: stdio` + `recipe: { attr: mcps.github }`.
