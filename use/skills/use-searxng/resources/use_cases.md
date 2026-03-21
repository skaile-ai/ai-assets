# Use Cases for SearXNG

## General Information Search
Find up-to-date facts on the web with default 10 results from Google.
```bash
uv run /home/matthias/workBench/SKILLS/.agent/skills/use-searxng/scripts/search.py "climate change solutions" --results 20
```

## Category-Specific Search (News, Images, Videos)
Search specific tabs like Google News.
```bash
uv run /home/matthias/workBench/SKILLS/.agent/skills/use-searxng/scripts/search.py "AI breakthroughs" --categories news
```

## Time-Filtered Results
Find information about recent releases or current events.
```bash
uv run /home/matthias/workBench/SKILLS/.agent/skills/use-searxng/scripts/search.py "Python 3.13 features" --time_range month
```

## Programmatic Analysis (JSON output)
Retrieve search results as raw JSON instead of markdown.
```bash
uv run /home/matthias/workBench/SKILLS/.agent/skills/use-searxng/scripts/search.py "open source LLMs" --raw
```
