# Use Exa CLI

The `use-exa` skill provides a command-line interface to search the web via Exa's public MCP endpoint using JSON-RPC 2.0.

## Commands

### `search`
Web search via Exa MCP (no API key required).

**Usage:**
```bash
uv run scripts/exa_search.py search [OPTIONS] QUERY
```

**Arguments:**
- `QUERY`: The search query. (Required)

**Options:**
- `-n, --num-results INTEGER`: Number of results to return. (Default: 10)
- `-t, --type TEXT`: Search type — keyword, neural, or auto. (Default: auto)
- `--start-date TEXT`: Start date filter (YYYY-MM-DD).
- `--end-date TEXT`: End date filter (YYYY-MM-DD).
- `--raw`: Output raw JSON instead of formatted markdown. (Default: False)
- `--help`: Show the help message and exit.

**Example:**
```bash
uv run scripts/exa_search.py search "MCP protocol specification" -n 5
uv run scripts/exa_search.py search "AI agents 2026" --start-date 2026-01-01 -n 3
```

---

### `code`
Code-focused search via Exa MCP (no API key required).

**Usage:**
```bash
uv run scripts/exa_search.py code [OPTIONS] QUERY
```

**Arguments:**
- `QUERY`: The code search query. (Required)

**Options:**
- `-n, --num-results INTEGER`: Number of results to return. (Default: 10)
- `--raw`: Output raw JSON instead of formatted markdown. (Default: False)
- `--help`: Show the help message and exit.

**Example:**
```bash
uv run scripts/exa_search.py code "FastAPI WebSocket implementation"
uv run scripts/exa_search.py code "React Server Components patterns" --raw
```

---

### `tools`
List available MCP tools from the Exa endpoint (discovery).

**Usage:**
```bash
uv run scripts/exa_search.py tools
```

**Options:**
- `--help`: Show the help message and exit.

**Output:** Lists each tool's name, description, and accepted parameters.
