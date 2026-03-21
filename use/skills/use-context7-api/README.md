# Context7 API CLI

A command-line tool for accessing Context7's REST API directly, without requiring the MCP server. Search for programming libraries and fetch up-to-date documentation with code examples.

## Features

- **Search libraries**: Find libraries by name and get Context7-compatible library IDs
- **Fetch documentation**: Retrieve markdown documentation with code examples for any library
- **No MCP required**: Direct REST API access for scripts, CI/CD, or custom tools
- **Rich output**: Formatted tables for search results, clean markdown for docs
- **Smart defaults**: Works out of the box, with optional API key for higher limits

## Installation

### Prerequisites

- Python 3.10+
- UV package manager (recommended) or pip

### Install with UV (Recommended)

```bash
# Clone or download the skill
cd use-context7-api

# Install as a tool
uv tool install .

# Now available globally
use-context7-api --help
```

### Install with pip

```bash
pip install use-context7-api
```

### Development install

```bash
# Install dependencies
uv sync

# Run directly
uv run use-context7-api --help
```

## Configuration

### API Key (Optional but Recommended)

1. Get your free API key at: https://context7.com/dashboard
2. Set the environment variable:

```bash
export CONTEXT7_API_KEY="ctx7sk_..."
```

Add to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.) for persistence.

**Without an API key**:
- Stricter rate limits apply
- Only public repositories accessible
- Still fully functional for casual use

### Custom API URL

If Context7 changes their API endpoint or you're using a self-hosted instance:

```bash
export CONTEXT7_API_URL="https://your-custom-endpoint.com/api"
```

## Usage

### Search for a Library

Find libraries by name and get their Context7 library ID:

```bash
# Basic search
use-context7-api search react

# Search with specific context for better ranking
use-context7-api search express --query "How to create middleware"

# Search for a specific package
use-context7-api search "node-jsonwebtoken"
```

**Output**: A table showing:
- Library ID (use this with `docs` command)
- Title and description
- Benchmark score (quality indicator)
- Available versions

**Example output**:
```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━┳━━━━━━━━━━━━┓
┃ Library ID                    ┃ Title    ┃ Description        ┃ Score  ┃ Versions    ┃
┡━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╇━━━━━━━━━━╇━━━━━━━━━━━━━━━━━━━━╇━━━━━━━━╇━━━━━━━━━━━━┩
│ /facebook/react               │ react    │ React is a...      │ 98     │ v18.2.0, ...│
│ /expressjs/express            │ express  │ Fast, unopinionat… │ 95     │ v4.18.0     │
└──────────────────────────────┴──────────┴────────────────────┴────────┴─────────────┘

Total results: 2
Use the Library ID with: use-context7-api docs <library-id>
```

### Fetch Documentation

Get markdown documentation with code examples for a specific library:

```bash
# Get general documentation
use-context7-api docs /facebook/react

# Get docs for a specific question
use-context7-api docs /auth0/node-jsonwebtoken --query "How to verify JWT with public key"

# Target a specific version
use-context7-api docs /expressjs/express/v4.18.0 --query "How to create middleware"
```

**Output**: Formatted markdown documentation with code examples, sent to stdout. Use pipes to save or page:

```bash
# Save to file
use-context7-api docs /facebook/react > react_docs.md

# Page through output
use-context7-api docs /lodash/lodash | less -R

# Search within docs
use-context7-api docs /nodejs/node | grep -A 5 "require"
```

**Note**: Documentation is returned as plain text/markdown. Your terminal will display it as-is.

### Show Configuration

Check your current setup and status:

```bash
use-context7-api info
```

**Output**:
```
Context7 API CLI - Configuration

Base URL: https://context7.com/api
API Key: ctx7sk_... ✓ configured
Client Version: 1.0.0
Timeout: 10 seconds

Rate Limits:
  With API key: Higher limits, private repos access

Get an API key at: https://context7.com/dashboard
```

### Get Help

```bash
# General help
use-context7-api --help

# Command-specific help
use-context7-api search --help
use-context7-api docs --help
```

## Use Cases

### Quick Library Reference

```bash
# Check how to use a new library
use-context7-api docs /axios/axios --query "How to set timeout"
```

### Scripting & Automation

```bash
#!/bin/bash
# Fetch latest React docs and save to file
use-context7-api docs /facebook/react > react_docs_$(date +%Y-%m-%d).md
```

### CI/CD Documentation Checks

```yaml
# In your CI pipeline
- name: Verify API documentation exists
  run: |
    output=$(use-context7-api docs /myorg/mylib --query "basic usage")
    if [ -z "$output" ]; then
      echo "ERROR: No documentation found for mylib"
      exit 1
    fi
```

### Learning & Exploration

```bash
# Compare multiple libraries
for lib in lodash underscore ramda; do
  echo "=== $lib ==="
  use-context7-api docs "/$lib/$lib" --query "How to debounce function" | head -50
done
```

## Error Handling

The CLI provides clear, actionable error messages:

- **401 Unauthorized**: Invalid or missing API key → Check `CONTEXT7_API_KEY`
- **404 Not Found**: Library doesn't exist or invalid library ID → Use `search` to find correct ID
- **429 Rate Limited**: Too many requests → Add API key or wait before retrying
- **Network errors**: Connection failed → Check internet, proxy settings
- **Empty documentation**: Library not finalized or no matching content → Try different query

## Troubleshooting

### "API key invalid" error
- Verify key starts with `ctx7sk`
- Get a fresh key from https://context7.com/dashboard
- Ensure no extra spaces or quotes in environment variable

### "Library not found"
- Use `search` to verify library exists
- Check library ID format: must be `/org/project` or `/org/project/version`
- Try searching with just the package name

### Rate limiting
- Without API key: ~10 requests/minute
- With API key: ~1000 requests/month free tier
- Upgrade at https://context7.com/plans for higher limits

### Network/proxy issues
- Set `HTTPS_PROXY` environment variable if behind corporate proxy
- Check firewall settings
- Test connectivity: `curl https://context7.com/api/v2/libs/search?libraryName=test`

## How It Works

This CLI is a standalone implementation of the Context7 REST API, derived from analyzing the official MCP server source code. It implements:

1. **Library Search**: `GET /v2/libs/search?query=<task>&libraryName=<name>`
2. **Context Fetch**: `GET /v2/context?query=<question>&libraryId=<id>`
3. **Authentication**: Bearer token via `Authorization` header
4. **Required headers**: `X-Context7-Source`, `X-Context7-Server-Version`
5. **Client IP**: Optional, not sent (privacy-preserving)

No MCP server or additional dependencies required. Just direct HTTP calls to Context7's API.

## Advanced Usage

### Piping to Other Tools

```bash
# Extract code blocks only
use-context7-api docs /facebook/react --query "How to use useState" |
  grep -E '^```' -A 100 | head -n -1

# Convert to PDF (with pandoc)
use-context7-api docs /nodejs/node > node_docs.md
pandoc node_docs.md -o node_docs.pdf
```

### Batch Operations

```bash
# Fetch docs for multiple libraries
libs=("/expressjs/express" "/facebook/react" "/lodash/lodash")
for lib in "${libs[@]}"; do
  echo "Fetching $lib..."
  use-context7-api docs "$lib" > "docs_${lib#/}.md"
done
```

### Custom HTTP Client

The CLI uses `httpx` under the hood. For advanced use cases, import the module:

```python
from context7_api.cli import get_base_url, get_headers, get_api_key

base_url = get_base_url()
headers = get_headers(get_api_key())
# Use httpx directly for async operations, streaming, etc.
```

## Limitations

- **No caching**: Every request hits the API (rate limits apply)
- **No offline mode**: Requires internet connection
- **No result pagination**: Returns all search results (limited by API)
- **Text output only**: Markdown not rendered (by design for pipes)

## Contributing

This is a standalone reimplementation. For issues or feature requests, refer to the upstream:
- Context7 MCP: https://github.com/upstash/context7
- Context7 Docs: https://context7.com/docs

## License

MIT. See LICENSE file for details.

## Acknowledgments

Built by analyzing the Context7 MCP server source code. Not affiliated with Upstash or Context7. Use at your own risk. Respect rate limits and terms of service.
