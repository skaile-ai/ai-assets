---
name: "use-perplexity"
description: "Use when you need to research topics or answer complex questions by querying Perplexity via the OpenRouter API."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "use-contract"
  env_vars:
    OPENROUTER_API_KEY: "Required. API key for OpenRouter to access Perplexity models."
---

# `use-perplexity`

Provides rigorous web research and question-answering via Perplexity (routed through OpenRouter).

## Setup & Environment Variables

This skill relies on a valid `OPENROUTER_API_KEY`.
The skill reads this from `.env` in the skill's root folder (`.agent/skills/use-perplexity/.env`).
If you receive an error about missing authentication, please ensure the user has populated that file or exported it in their shell.

## Configuration

- **Default Model**: `perplexity/sonar-reasoning`
- You can override the default model using `--model` (or `-m`).
- Use `--raw` to retrieve raw markdown text rather than standard agent-formatted text.
- Use `--help` to view all options.

## Instructions

- Consult the [Use Cases](resources/use_cases.md) to see how to approach different workflows with this tool.
- Run the search CLI using the `uv run` mechanism.

```bash
uv run scripts/ask.py "Your deep research question goes here"
```
