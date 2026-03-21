# use-perplexity CLI

This document describes the command-line interface for the `use-perplexity` skill. The primary script is `scripts/ask.py`, which leverages Perplexity's models (and others) via the OpenRouter API.

## Usage

```bash
uv run scripts/ask.py [OPTIONS] QUERY
```

**Description:**  
Research a topic or ask complex questions using Perplexity via OpenRouter. The script takes your query, sends it to the specified OpenRouter model, and prints the generated markdown response to stdout.

### Arguments

- `QUERY` **(required)**: The search query or question to research.

### Options

- `-m, --model <TEXT>`: The OpenRouter model to use.  
  *(default: `perplexity/sonar-reasoning`)*
- `--raw`: Output raw text instead of rendering the standard markdown header with metadata.  
  *(default: `False`)*
- `--help`: Show the help message and exit.

## Environment Variables

The script requires an OpenRouter API key. It attempts to load this from a `.env` file in the skill's root directory:
```env
OPENROUTER_API_KEY=sk-or-v1-...
```

---

## Suggested Subcommands

As the tool grows, `ask.py` could be expanded to support multiple subcommands. Here are 3 logical additions for future implementation:

### 1. `compare`
**Description**: Ask the same query to two different models simultaneously and print a sequential comparison of their answers.  
**Example Usage**:  
```bash
uv run scripts/ask.py compare "Explain quantum computing" -m1 perplexity/sonar-reasoning -m2 anthropic/claude-3-5-sonnet
```

### 2. `history`
**Description**: Keep a local cache (e.g., in SQLite or JSON) of past queries and responses. This subcommand would list previous searches or retrieve cached answers without re-running the API call, saving time and OpenRouter API credits.  
**Example Usage**:  
```bash
uv run scripts/ask.py history --limit 5
```

### 3. `followup`
**Description**: Enable multi-turn conversation capabilities. By caching the conversation history locally, this command would append your new prompt to the existing context array, allowing seamless follow-up questions to the last response.  
**Example Usage**:  
```bash
uv run scripts/ask.py followup "Can you expand on the second point?"
```
