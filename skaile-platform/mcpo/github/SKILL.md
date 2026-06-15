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
metadata:
  tags:
  - github
  - git
  - repos
  - issues
  - pull-requests
  - actions
  - code-search
  - mcp
  - remote
  stage: alpha
version: 0.1.0
---

# GitHub MCP

GitHub's official, GitHub-hosted MCP server. Gives an agent typed tool access to
repositories, issues, pull requests, Actions, and search - all scoped to the
connected GitHub identity's permissions. **Remote**: there is no container to
run; the runner opens a streamable-HTTP connection to
`https://api.githubcopilot.com/mcp/`.

## When to reach for this skill

- The user asks to read, comment on, open, or update GitHub issues or pull requests.
- The agent needs to inspect or edit repository contents, branches, or files on GitHub.
- The task involves GitHub Actions - listing workflow runs, reading logs, or triggering a workflow.
- The agent needs to search code, issues, or PRs across repositories the user can access.
- Prefer this over ad-hoc `curl`/REST when the work spans multiple GitHub operations or benefits from permission-scoped tools.

For a single, simple file fetch from a public repo, plain HTTP is fine - reach
for this skill when the work is genuinely GitHub-shaped and multi-step.

## How it is wired

Unlike the stdio MCPs in this domain (`xls`, `ppt`), this server is remote and
GitHub-hosted, so there is **no Dockerfile and no Nix recipe**. Two layers:

- **Catalog** (`MCP.md`) declares only the endpoint shape (`transport: http`, the
  `url`). It carries no credentials - auth is install-specific.
- **Platform** binds auth when the instance is enabled: `auth: backend` plus a
  GitHub OAuth `providerLinkId`. At session start the backend mints the user's
  GitHub token and serves it as `Authorization: Bearer <token>`; the token never
  lands on disk.

See `MCP.md` for the full per-org wiring steps (GitHub OAuth app, ProviderLink,
the remote-MCP widget).

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

## Local alternative

A local stdio variant exists (`ghcr.io/github/github-mcp-server`, run as
`github-mcp-server stdio`, PAT in `GITHUB_PERSONAL_ACCESS_TOKEN`, with
`--toolsets` / `--read-only` / `--lockdown-mode` flags). If data residency or
locked-down egress later rules out the hosted endpoint, package the Go binary as
a Nix recipe (`mcps.github`) mirroring `mcpo/xls` and ship a second manifest with
`transport: stdio` + `recipe: { attr: mcps.github }`. See `MCP.md` for details.
