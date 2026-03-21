---
name: use-searxng
source: MIGRATED
description: Use when you need to search the internet for current information, research topics, find news, or look up web content. Queries a local SearXNG instance with Google as the primary search engine and returns clean, structured markdown results.
env_vars:
  SEARXNG_URL: "Optional. The base URL of your SearXNG instance (default: http://localhost:8080)."
keywords: []
reads_from: []
writes_to: []
metadata:
  stage: alpha
  requires:
  - use-contract
---

# use-searxng Skill

You have access to a local, privacy-respecting SearXNG search engine with Google as the primary engine.

## Setup & Environment Variables

Before searching, ensure the local SearXNG instance is running. It persists across sessions but NOT across reboots:

```bash
docker compose -f ~/.config/workspace/searxng/docker-compose.yml up -d
```

Check status:
```bash
uv run /home/matthias/workBench/SKILLS/.agent/skills/use-searxng/scripts/search.py status
```

If `status` reports it's down, start it as above and wait ~5 seconds.

## Configuration

| File | Purpose |
|------|---------|
| `~/.config/workspace/searxng/docker-compose.yml` | Container definition |
| `~/.config/workspace/searxng/settings.yml` | SearXNG config (engines, formats) |
| `scripts/search.py` | CLI search tool |

## Instructions

- Consult the [Use Cases](resources/use_cases.md) to see how to approach different workflows with this tool.

### Basic Search
```bash
uv run /home/matthias/workBench/SKILLS/.agent/skills/use-searxng/scripts/search.py "your query here"
```

## Output Format
Results are returned as clean markdown with:
- Title and URL per result
- Source engines (Google-weighted results appear first)
- Publication date (for news)
- Content snippet
