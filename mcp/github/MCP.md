---
name: github
description: "Use when an agent needs to act on GitHub - read or open issues and
  pull requests, inspect or edit repository contents, trigger or read GitHub
  Actions, or run code/issue/PR search across repos the connected identity can
  reach. Wraps GitHub's official hosted remote MCP server
  (api.githubcopilot.com/mcp), so tools are scoped to the user's GitHub
  permissions. Reach for this over raw REST/GraphQL when the task spans several
  GitHub operations or you want typed, permission-scoped tools rather than
  hand-rolled API calls."
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

## When to reach for this

- The user asks to read, comment on, open, or update GitHub issues or pull requests.
- The agent needs to inspect or edit repository contents, branches, or files on GitHub.
- The task involves GitHub Actions - listing workflow runs, reading logs, or triggering a workflow.
- The agent needs to search code, issues, or PRs across repositories the user can access.
- Prefer this over ad-hoc `curl`/REST when the work spans multiple GitHub operations or benefits from permission-scoped tools.

For a single, simple file fetch from a public repo, plain HTTP is fine - reach
for this when the work is genuinely GitHub-shaped and multi-step.

## Non-obvious gotchas the agent must respect

- **Permission-scoped, not omnipotent.** Every tool runs as the connected GitHub
  identity. A 403/404 usually means the user's token lacks access, not that the
  resource is missing - surface that distinction instead of retrying blindly.
- **Network egress required.** The session sandbox must be able to reach
  `api.githubcopilot.com`. Under locked-down session networking the connection
  fails fast; that is a config issue, not a tool bug.
- **Read-only / toolset scoping is endpoint-selected.** The hosted server exposes
  scoped variants via URL path (e.g. a read-only endpoint). To restrict
  capabilities, the operator overrides the instance `url` - the agent cannot
  widen its own scope at call time.
- **Write actions are real.** Opening issues/PRs, pushing edits, or dispatching
  workflows takes effect immediately against live GitHub. Confirm intent on
  destructive or outward-facing actions.

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
