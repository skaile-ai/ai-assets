# Use SearXNG CLI

The `use-searxng` skill provides a command-line interface to query a local SearXNG instance and return markdown-formatted results.

## Commands

### `search`
Search the web and return results formatted for agent consumption.

**Usage:**
```bash
python scripts/search.py search [OPTIONS] QUERY
```

**Arguments:**
- `QUERY`: The search query. (Required)

**Options:**
- `-c, --categories TEXT`: Search categories (general, news, images, videos). Comma-separated.
- `-t, --time_range TEXT`: Limit results by time (day, week, month, year).
- `-n, --results INTEGER`: Number of results to show. (Default: 10)
- `--google-only`: Only show results from Google. (Default: False)
- `--raw`: Output raw JSON instead of formatted markdown. (Default: False)
- `--help`: Show the help message and exit.

**Example:**
```bash
python scripts/search.py search "latest ai news" -c news -t week -n 5
```

---

### `status`
Check if the local SearXNG instance is running and reachable.

**Usage:**
```bash
python scripts/search.py status
```

**Options:**
- `--help`: Show the help message and exit.

## Suggested Subcommands

Here are some suggested additions for future development:

1. **`engines`**
   - **Description:** List all available search engines in the local SearXNG instance, including their current status, categories, and response times.
   - **Usage:** `python scripts/search.py engines [--category news]`

2. **`history`**
   - **Description:** Show a local history of recent search queries and the number of results found, stored locally for quick reference.
   - **Usage:** `python scripts/search.py history [--limit 10]`

3. **`config`**
   - **Description:** Display or modify local search preferences (e.g., safe search level, default active categories, language preferences) used when querying the SearXNG instance.
   - **Usage:** `python scripts/search.py config --set-lang en`
