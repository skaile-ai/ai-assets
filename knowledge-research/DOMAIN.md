---
name: knowledge-research
description: "Knowledge acquisition skills: deep multi-phase research, academic paper extraction, and structured synthesis from external sources."
type: domain
building_blocks:
  contracts: "Research output structure (findings/, summaries/), metadata fields for research artifacts, and citation conventions."
  docs: "Research workflow guides, quality criteria for sources, and synthesis patterns."
  skills: "Three skills: knowledge-deep-research (integrated 3-phase research), knowledge-paper-extractor (PDF metadata extraction), knowledge-paper-research (multi-phase paper knowledge extraction)."
  agents: "TBD"
  prompts: "TBD"
  tools: "TBD"
stage: alpha
---

# Knowledge Research

This domain handles knowledge acquisition from external sources. Its skills drive structured research processes — from deep web research with synthesis, to systematic extraction and analysis of academic papers. Output from these skills feeds directly into `knowledge-writing` for content production.

All three skills are designed to be invoked directly from a project context or in parallel as a research mode alongside other pipeline steps.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Research artifact structure, metadata fields, citation conventions |
| `docs/` | Research workflow guides, source quality criteria |
| `skills/` | `knowledge-deep-research`, `knowledge-paper-extractor`, `knowledge-paper-research` |

## Skills

| Skill | Purpose |
|-------|---------|
| `knowledge-deep-research` | Integrated 3-phase web research: scoping → searching (via exa/perplexity/searxng) → synthesis |
| `knowledge-paper-extractor` | Extract structured metadata (title, authors, abstract, key findings) from PDF academic papers |
| `knowledge-paper-research` | Multi-phase deep analysis of academic papers: extraction → annotation → synthesis → structured output |

## Conventions

- Research skills use `use/` domain skills (exa, perplexity, searxng) for web search — never raw HTTP calls
- Synthesis outputs are always structured (markdown with frontmatter) for downstream consumption by `knowledge-writing`
- Paper research produces citable, structured knowledge files — not freeform notes
