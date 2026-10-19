---
name: knowledge-research
description: "Knowledge acquisition skills: deep multi-phase research, academic paper extraction, and structured synthesis from external sources."
type: domain
building_blocks:
  contracts: "Research output structure (findings/, summaries/), metadata fields for research artifacts, and citation conventions."
  docs: "Research workflow guides, quality criteria for sources, and synthesis patterns."
  skills: "Three skills: deep-research (integrated 3-phase research), knowledge-paper-extractor (PDF metadata extraction), knowledge-paper-research (multi-phase paper knowledge extraction). Content-production skills live in the nested domain skills/knowledge-writing/ (see its DOMAIN.md)."
  agents: "researcher — role agent (input {topic}, output {summary, sources}) for one-shot, read-only research runs (skips the `deep-research` storage phase)."
  prompts: "TBD"
  tools: "TBD"
stage: alpha
---

# Knowledge Research

This domain handles knowledge acquisition from external sources. Its skills drive structured research processes — from deep web research with synthesis, to systematic extraction and analysis of academic papers. Output from the research skills feeds directly into the `knowledge-writing` skills (under `skills/knowledge-writing/`) for content production.

The three research skills are designed to be invoked directly from a project context or in parallel as a research mode alongside other pipeline steps.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Research artifact structure, metadata fields, citation conventions |
| `docs/` | Research workflow guides, source quality criteria |
| `skills/` | `deep-research`, `knowledge-paper-extractor`, `knowledge-paper-research`, and the nested domain `knowledge-writing/` (its own `DOMAIN.md` lists its skills) |

## Skills

| Skill | Purpose |
|-------|---------|
| `deep-research` | Integrated 3-phase web research: collection (via exa/perplexity/searxng) → storage → synthesis |
| `knowledge-paper-extractor` | Extract structured metadata (title, authors, abstract, key findings) from PDF academic papers |
| `knowledge-paper-research` | Multi-phase deep analysis of academic papers: extraction → annotation → synthesis → structured output |

The content-production skills (`book-writer`, `knowledge-paper-podcast`, `knowledge-writer-podcast`) belong to the nested domain [`skills/knowledge-writing/`](skills/knowledge-writing/DOMAIN.md); its `DOMAIN.md` is the one index for them.

## Conventions

- Research skills use `use/` domain skills (exa, perplexity, searxng) for web search — never raw HTTP calls
- Synthesis outputs are always structured (markdown with frontmatter) for downstream consumption by the `knowledge-writing` skills
- Paper research produces citable, structured knowledge files — not freeform notes
