---
name: use-mcp2cli
description: "Use when you need to interact with MCP (Model Context Protocol) servers, OpenAPI specs, or GraphQL endpoints via CLI. Converts API schemas into token-efficient CLI commands at runtime without code generation."
metadata:
  stage: alpha
  source: MIGRATED
  requires:
    - use-contract
  env_vars:
    MCP2CLI_CACHE_DIR: "Optional. Override cache directory (default: ~/.cache/mcp2cli)"
version: 1.0.0
---

# mcp2cli — Universal API-to-CLI Adapter

mcp2cli transforms MCP servers, OpenAPI specifications, and GraphQL endpoints into CLI commands at runtime. No code generation needed — it introspects schemas dynamically and constructs parameterized commands.

Key advantage for agent workflows: **96-99% token savings** over sending full tool schemas every turn. The CLI interface is dramatically more compact than JSON schema representations.

## Setup

mcp2cli is a Python package that runs via `uvx` (zero install) or `uv tool install`.

```bash
# Zero install — runs directly
uvx mcp2cli --help

# Or install globally
uv tool install mcp2cli
```

No API keys required for mcp2cli itself. Individual MCP servers or APIs may need their own authentication — see the Authentication section.

## Core Usage Patterns

### MCP Servers

```bash
# List tools from an MCP HTTP server
uvx mcp2cli --mcp https://mcp.example.com/sse --list

# Invoke a tool
uvx mcp2cli --mcp https://mcp.example.com/sse search --query "test"

# MCP stdio — run a local server process
uvx mcp2cli --mcp-stdio "npx @mcp/filesystem /tmp" --list
uvx mcp2cli --mcp-stdio "node server.js" --env API_KEY=sk-... tool-name --arg value
```

### OpenAPI Specifications

```bash
# List endpoints from a spec
uvx mcp2cli --spec ./openapi.json --base-url https://api.example.com --list

# Invoke an endpoint
uvx mcp2cli --spec ./openapi.json --base-url https://api.example.com create-pet --name "Fido"

# Pipe JSON body via stdin
echo '{"name":"Fido"}' | uvx mcp2cli --spec ./openapi.json create-pet --stdin
```

### GraphQL Endpoints

```bash
# Query with auto-generated selection set
uvx mcp2cli --graphql https://api.example.com/graphql users --limit 10

# Override field selection
uvx mcp2cli --graphql https://api.example.com/graphql users --fields "id name email"
```

## Bake Mode — Persistent Configurations

Bake saves connection configs for reuse. This avoids repeating auth flags and URLs on every invocation.

```bash
# Create a baked tool
uvx mcp2cli bake create petstore --spec https://petstore3.swagger.io/api/v3/openapi.json

# Use with @shorthand
uvx mcp2cli @petstore --list
uvx mcp2cli @petstore get-pet --petId 1

# Filter included tools
uvx mcp2cli bake create myapi --mcp https://mcp.example.com/sse --include "search.*" --exclude "*_deprecated"
```

Baked configurations are stored in `~/.cache/mcp2cli/baked/`.

## Output Formatting

Control output format for optimal token usage:

| Flag | Description | Best For |
|------|-------------|----------|
| `--pretty` | Pretty-printed JSON (default for TTY) | Human reading |
| `--raw` | Unprocessed response body | Piping to other tools |
| `--jq EXPR` | JSON query filter | Extracting specific fields |
| `--toon` | Token-optimized notation (40-60% fewer tokens) | Agent workflows |
| `--head N` | Truncate to first N records | Large result sets |

```bash
# Extract just names from results
uvx mcp2cli @myapi list-users --jq '.[].name'

# Token-efficient output for agent consumption
uvx mcp2cli @myapi list-users --toon --head 20
```

## Authentication

```bash
# OAuth2 (interactive browser flow)
uvx mcp2cli --spec ./spec.json --oauth --client-id MY_ID --auth-url https://auth.example.com/authorize --token-url https://auth.example.com/token

# Bearer token from environment variable
uvx mcp2cli --spec ./spec.json --auth-header "Authorization: Bearer env:MY_API_TOKEN"

# Header from file
uvx mcp2cli --spec ./spec.json --auth-header "X-API-Key: file:/path/to/key"

# Direct header (avoid in scripts — prefer env: or file:)
uvx mcp2cli --spec ./spec.json --auth-header "Authorization: Bearer sk-..."
```

## Search and Discovery

```bash
# Search for tools by keyword
uvx mcp2cli --mcp https://mcp.example.com/sse --search "task"

# List with descriptions
uvx mcp2cli @myapi --list
```

## Integration with arm (AI Resource Manager)

mcp2cli can also be managed as an external resource via arm, the AI Resource Manager used in this project. This is useful for:

- Cataloging mcp2cli's bundled skills alongside your project's skills
- Tracking upstream updates to mcp2cli
- Managing mcp2cli as a fork with local additions

### Register mcp2cli as an arm resource

```bash
# Register the upstream repo
arm resource add https://github.com/knowsuchagency/mcp2cli --name mcp2cli

# Or register your fork with upstream tracking
arm resource add https://github.com/youruser/mcp2cli --name mcp2cli --upstream https://github.com/knowsuchagency/mcp2cli

# Sync to discover any skills shipped with mcp2cli
arm resource sync mcp2cli
```

### Check for updates

```bash
# See if your cached clone is behind the remote
arm resource status mcp2cli

# Compare your fork against upstream
arm contrib compare mcp2cli

# See what you've added locally
arm contrib additions mcp2cli
```

### Keep up to date

```bash
# Pull latest (preserves local edits via stash)
arm contrib sync-with-stash mcp2cli

# Or re-sync and re-index
arm resource sync mcp2cli
```

### Contribute changes back

```bash
# Check what you've changed
arm contrib changes mcp2cli
arm contrib diff mcp2cli

# Commit and push to your fork
arm contrib commit mcp2cli -m "add: new skill for X" -b feature-branch
arm contrib push mcp2cli https://github.com/youruser/mcp2cli feature-branch

# Open a PR
arm contrib pr https://github.com/knowsuchagency/mcp2cli --title "Add X" --body "..." --head youruser:feature-branch
```

### Pin a specific version via lock file

```bash
# Lock current versions for reproducible installs
arm project lock

# Install from lock file (checks out exact commit SHAs)
arm project install --locked
```

## Common Workflows

### Expose a local MCP server to agent tools

```bash
# 1. Start your MCP server
# 2. Bake it for easy access
uvx mcp2cli bake create local-tools --mcp-stdio "node ./my-mcp-server.js"

# 3. Agent can now discover and call tools
uvx mcp2cli @local-tools --list
uvx mcp2cli @local-tools my-tool --param value --toon
```

### Wrap a REST API for agent use

```bash
# 1. Bake the OpenAPI spec
uvx mcp2cli bake create my-api --spec https://api.example.com/openapi.json --base-url https://api.example.com

# 2. Use in agent workflows with token-efficient output
uvx mcp2cli @my-api search --query "relevant data" --jq '.results[:5]' --toon
```

### Discover tools from an unknown MCP server

```bash
# List all available tools
uvx mcp2cli --mcp https://new-server.example.com/sse --list

# Search for relevant tools
uvx mcp2cli --mcp https://new-server.example.com/sse --search "file"

# Try a tool
uvx mcp2cli --mcp https://new-server.example.com/sse tool-name --help
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `uvx: command not found` | Install uv: `curl -LsSf https://astral.sh/uv/install.sh \| sh` |
| MCP connection refused | Verify server URL; try `--mcp-transport sse` or `--mcp-transport streamable` |
| OAuth token expired | Delete cached token: `rm ~/.cache/mcp2cli/oauth/<service>.json` |
| Baked tool outdated | Refresh: `uvx mcp2cli bake create <name> --spec <url>` (overwrites) |
| Stale cache | Force refresh: `uvx mcp2cli @myapi --refresh --list` |
| Large responses | Use `--head N` to truncate, `--jq` to filter |
