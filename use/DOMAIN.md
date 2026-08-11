---
name: use
description: 'Integration skills for external services and APIs — each skill wraps one service with a consistent, agent-friendly interface.'
type: domain
building_blocks:
  contracts: 'Integration skill conventions: output format (always JSON-compatible), error handling protocol, authentication patterns, and when to use CLI vs API variant.'
  docs: 'Service capability matrix, authentication setup guides, and rate-limit notes.'
  skills: 'One skill per service: exa (AI search), perplexity (research via OpenRouter), searxng (local privacy search), context7-api (library docs), anydoc (fast office-doc to Markdown, no OCR), docling (document conversion incl. OCR/VLM/ASR), elevenlabs (TTS), use-ideogram-image (image generation via REST), outline (wiki API), outline-cli (wiki CLI), revealjs (presentation decks, zero-install), slidev (presentation decks, markdown).'
  agents: 'TBD'
  prompts: 'TBD'
  tools: 'TBD'
stage: alpha
---

# Use

This domain wraps external services with consistent, agent-friendly interfaces. Each skill knows how to authenticate, format requests, handle errors, and return structured output for one specific service. Calling skills (research, writing, implementation) delegate to `use/` skills rather than calling services directly.

This separation means service-specific knowledge (API quirks, rate limits, auth patterns) is encapsulated in one place and never duplicated across the ecosystem.

## Building Blocks

| Folder       | Purpose                                                                      |
| ------------ | ---------------------------------------------------------------------------- |
| `contracts/` | Integration skill conventions — output format, error protocol, auth patterns |
| `docs/`      | Service capability matrix, setup guides, rate-limit notes                    |
| `skills/`    | One skill per service                                                        |

## Skills

| Skill              | Service                     | Primary Use                                               |
| ------------------ | --------------------------- | --------------------------------------------------------- |
| `use-exa`          | Exa                         | AI-powered semantic web search                            |
| `use-perplexity`   | Perplexity (via OpenRouter) | Research-focused web search with citations                |
| `use-searxng`      | SearXNG                     | Privacy-first local web search (self-hosted)              |
| `use-context7-api` | Context7                    | Library documentation lookup by package name              |
| `use-anydoc`       | anydoc (Firecrawl)          | Fast read-only office-doc → markdown; no OCR, no ML       |
| `use-docling`      | Docling                     | Document conversion (PDF, DOCX → markdown); OCR/VLM/ASR   |
| `use-elevenlabs`   | ElevenLabs                  | Text-to-speech audio generation                           |
| `use-ideogram-image` | Ideogram                  | Image generation/editing via REST API (central org key)   |
| `use-outline`      | Outline                     | Wiki management via REST API                              |
| `use-outline-cli`  | Outline                     | Wiki management via CLI                                   |
| `use-slidev`       | Slidev (sli.dev)            | Build slide decks in Skaile brand design                  |
| `use-skills-sh`    | skills.sh                   | Discover and install agent skills from the open ecosystem |

## Conventions

- All `use/` skills return structured, parseable output — never raw HTML or unformatted text
- Authentication credentials are read from environment variables — never hardcoded
- Skills handle pagination and rate limits internally; callers see a flat result set
- When a service has both API and CLI variants (`outline`), prefer the API variant for agent use and the CLI variant for human-interactive use
- Document conversion has two skills: reach for `use-anydoc` on digital-native office documents (fast, pure Rust, no ML), and `use-docling` when the input needs OCR, a vision model, or speech transcription. Neither replaces `mcp:excel` / `mcp:ppt` for tasks that **modify** a document - both are read-only and drop formulas, cell addresses, and styling
