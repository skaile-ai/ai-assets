# Use Cases for Use Exa

## Research & Information Gathering

- **Current Events**: Search for recent news or developments on a topic.
  ```bash
  uv run scripts/exa_search.py search "latest developments in quantum computing" --start-date 2026-01-01 -n 5
  ```

- **Technical Documentation**: Find official docs, blog posts, or tutorials.
  ```bash
  uv run scripts/exa_search.py search "PEP 723 inline script metadata specification"
  ```

- **Comparative Research**: Search for comparisons or alternatives.
  ```bash
  uv run scripts/exa_search.py search "FastAPI vs Litestar performance comparison 2026"
  ```

## Code Search

- **Implementation Patterns**: Find code examples for specific patterns.
  ```bash
  uv run scripts/exa_search.py code "Python asyncio semaphore rate limiting"
  ```

- **Library Usage**: Find real-world usage of a library or API.
  ```bash
  uv run scripts/exa_search.py code "anthropic SDK tool_use streaming Python"
  ```

- **Bug Solutions**: Search for error messages or known issues.
  ```bash
  uv run scripts/exa_search.py code "TypeError: Cannot read properties of undefined reading 'map' React"
  ```

## Agent Workflow Patterns

- **Verify Before Implementing**: Search for best practices before writing code.
  ```bash
  uv run scripts/exa_search.py search "best practices OAuth2 PKCE flow 2026" -n 3
  ```

- **Raw JSON for Downstream Processing**: Use `--raw` when piping to other tools.
  ```bash
  uv run scripts/exa_search.py search "MCP server implementations" --raw | python3 -c "import sys,json; print(len(json.load(sys.stdin)))"
  ```

- **Scoped Time Windows**: Narrow results to a specific period.
  ```bash
  uv run scripts/exa_search.py search "Python 3.13 new features" --start-date 2025-10-01 --end-date 2025-12-31
  ```

## Endpoint Discovery

- **Check Available Tools**: See what the Exa MCP endpoint offers.
  ```bash
  uv run scripts/exa_search.py tools
  ```
