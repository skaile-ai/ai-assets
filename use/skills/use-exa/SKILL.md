---
name: "use-exa"
description: "Use when you need to search the web or find code examples using Exa's AI-powered search. Queries Exa's public MCP endpoint with JSON-RPC — no API key required for basic web search and code search."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "contract:use-contract"
---

# Use Exa

Web search and code search via Exa's public MCP endpoint at `https://mcp.exa.ai/mcp`.

## Setup & Environment Variables

**No setup required.** Basic search works without an API key.

For higher rate limits, optionally set:
```bash
export EXA_API_KEY="your_key"
```

## How It Works

This skill uses Exa's Model Context Protocol (MCP) endpoint directly via JSON-RPC 2.0 over HTTPS. The public endpoint serves basic search tools without authentication — the same approach used by [oh-my-pi](https://github.com/can1357/oh-my-pi).

**Available tools (no key needed):**
- `web_search_exa` — AI-powered web search
- `get_code_context_exa` — code-focused search

**API-key-gated tools (not exposed here):**
- LinkedIn search, company research, websets, crawling

## Instructions

- Consult the [Use Cases](resources/use_cases.md) to see how to approach different workflows with this tool.

### Web Search
```bash
uv run scripts/exa_search.py search "your query here"
```

### Code Search
```bash
uv run scripts/exa_search.py code "how to implement JWT auth in FastAPI"
```

### Discover Available Tools
```bash
uv run scripts/exa_search.py tools
```

### Common Options
| Option | Description | Default |
|--------|-------------|---------|
| `-n, --num-results` | Number of results | 10 |
| `-t, --type` | Search type: keyword, neural, auto | auto |
| `--start-date` | Filter from date (YYYY-MM-DD) | None |
| `--end-date` | Filter to date (YYYY-MM-DD) | None |
| `--raw` | Output raw JSON instead of markdown | False |

## Output Format

Results are returned as clean markdown with:
- Title and URL per result
- Relevance score
- Publication date (when available)
- Content snippet (truncated to 1000 chars)

## Auto-Improvement

- Every time this skill is used, analyze the usage chat to find out if further improvement of the skill is advised.
- Ask the user if those changes should be made.
- If approved, store the improvement ideas in the `resources/improvement_ideas.md` file.
