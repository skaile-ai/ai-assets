---
name: use
description: "Integration skills for external services and APIs — each skill wraps one service with a consistent, agent-friendly interface."
type: domain
building_blocks:
  contracts: "Integration skill conventions: output format (always JSON-compatible), error handling protocol, authentication patterns, and when to use CLI vs API variant."
  docs: "Service capability matrix, authentication setup guides, and rate-limit notes."
  skills: "One skill per service: exa (AI search), perplexity (research via OpenRouter), searxng (local privacy search), context7-api (library docs), docling (document conversion), elevenlabs (TTS), outline (wiki API), outline-cli (wiki CLI)."
  agents: "TBD"
  prompts: "TBD"
  tools: "TBD"
stage: alpha
---

# Use

This domain wraps external services with consistent, agent-friendly interfaces. Each skill knows how to authenticate, format requests, handle errors, and return structured output for one specific service. Calling skills (research, writing, implementation) delegate to `use/` skills rather than calling services directly.

This separation means service-specific knowledge (API quirks, rate limits, auth patterns) is encapsulated in one place and never duplicated across the ecosystem.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Integration skill conventions — output format, error protocol, auth patterns |
| `docs/` | Service capability matrix, setup guides, rate-limit notes |
| `skills/` | One skill per service |

## Skills

| Skill | Service | Primary Use |
|-------|---------|-------------|
| `use-exa` | Exa | AI-powered semantic web search |
| `use-perplexity` | Perplexity (via OpenRouter) | Research-focused web search with citations |
| `use-searxng` | SearXNG | Privacy-first local web search (self-hosted) |
| `use-context7-api` | Context7 | Library documentation lookup by package name |
| `use-docling` | Docling | Document conversion (PDF, DOCX → markdown) |
| `use-elevenlabs` | ElevenLabs | Text-to-speech audio generation |
| `use-outline` | Outline | Wiki management via REST API |
| `use-outline-cli` | Outline | Wiki management via CLI |

## Conventions

- All `use/` skills return structured, parseable output — never raw HTML or unformatted text
- Authentication credentials are read from environment variables — never hardcoded
- Skills handle pagination and rate limits internally; callers see a flat result set
- When a service has both API and CLI variants (`outline`), prefer the API variant for agent use and the CLI variant for human-interactive use
