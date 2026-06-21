---
name: "use-contract"
description: "Shared contract for all use-* integration skills. Describes integration skill conventions, API key handling patterns, MCP vs direct-API usage, output format, and how use skills are composed with other skills."
metadata:
  stage: "alpha"
  do_not_invoke: true
---

# Use (Integration) Domain — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `use-*` skills read this before operating.

## Scope

`use-*` skills are **integration adapters** — thin wrappers that provide a standardized interface to external services (search engines, APIs, file converters, etc.). They are composable: other skills call them as subagents or reference their conventions.

## Skills Overview

| Skill | Service | Access method |
|-------|---------|---------------|
| `use-exa` | Exa AI web search | MCP / JSON-RPC |
| `use-perplexity` | Perplexity research | OpenRouter API |
| `use-searxng` | SearXNG self-hosted search | HTTP API |
| `use-context7-api` | Context7 library docs | MCP |
| `use-outline` | Outline wiki (API) | REST API |
| `use-outline-cli` | Outline wiki (CLI) | CLI subprocess |
| `use-elevenlabs` | ElevenLabs TTS | REST API |
| `ideogram-image` | Ideogram image generation | REST API (BYO-key) |
| `use-docling` | Document conversion | CLI subprocess |

## API Key Handling

Skills must **never hardcode** API keys. Resolution order:

1. Environment variable (preferred): `<SERVICE>_API_KEY`
2. `.env` file in project root
3. Prompt user if not found

```yaml
env_vars:
  - key: EXA_API_KEY
    description: "Exa API key"
    required: false   # false if MCP endpoint used without auth
```

**Platform-injected keys (BYO-key).** A skill may instead declare a top-level `auth`
block so the platform collects the key once and injects it as the named env var (see
`mcp/DOMAIN.md` → "Authentication declarations"). The skill still reads the key from
the env var and never hardcodes or echoes it — only the *source* of the variable
differs. `ideogram-image` uses this path (`auth: { inject: env, env: IDEOGRAM_API_KEY }`).

## MCP vs Direct API

| Method | When to use |
|--------|------------|
| MCP (JSON-RPC) | Service exposes MCP endpoint; no API key needed |
| Direct REST | Service requires key; MCP not available |
| CLI subprocess | Tool available as local CLI binary |

Prefer MCP when available — avoids key management.

## Output Format

Integration skills return structured results, not raw API responses:

```markdown
## Results: <query>

**Source:** <service>  **Retrieved:** YYYY-MM-DD

1. **<Title>** — <URL>
   <1-2 sentence summary>

2. ...
```

## Composition Pattern

When used as a subagent by research or implementation skills:

```
Parent skill → dispatches use-<service> subagent with:
  - query / request parameters
  - desired output format
  - max_results

use-<service> returns:
  - structured results in agreed format
  - source attribution
```

Skills should document their expected input/output contract in `user_inputs` and the SKILL.md body.
