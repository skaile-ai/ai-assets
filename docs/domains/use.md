---
title: use
description: Integration skills for external services and APIs — each skill wraps one service with a consistent, agent-friendly interface.
---

Wraps external services with consistent, agent-friendly interfaces. Each skill knows how to authenticate, format requests, handle errors, and return structured output for one specific service. Calling skills (research, writing, implementation) delegate to `use/` skills rather than calling services directly.

Service-specific knowledge (API quirks, rate limits, auth patterns) is encapsulated in one place and never duplicated across the ecosystem.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | Integration skill conventions — output format (JSON-compatible), error handling protocol, auth patterns |
| `docs/` | Service capability matrix, setup guides, rate-limit notes |
| `use-*/` | One skill directory per service (no `skills/` subdirectory) |

## Skills

| Skill | Service | When to use |
|---|---|---|
| `use-exa` | Exa | AI-native semantic search — returns structured results with summaries |
| `use-perplexity` | Perplexity (via OpenRouter) | Research-grade web search with citations |
| `use-searxng` | SearXNG | Self-hosted, privacy-preserving meta-search engine |
| `use-context7-api` | Context7 | Up-to-date library documentation — resolves library + version → docs |
| `use-docling` | Docling | Document conversion and extraction (PDF, DOCX → structured text) |
| `use-elevenlabs` | ElevenLabs | Text-to-speech — generate high-quality audio from scripts |
| `use-outline` | Outline | Team wiki API — read, write, and search documents |
| `use-outline-cli` | Outline (CLI) | Outline wiki via CLI interface |

## Auth Convention

Each `use-*` skill documents its auth pattern in frontmatter:

```yaml
metadata:
  auth: env         # reads from environment variable
  env_var: EXA_API_KEY
```

Set the required environment variable before invoking the skill. No keys are stored in skill files.
