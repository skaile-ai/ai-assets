---
name: dev-implementation-experts-typst
description: "Implementation expertise for Typst document composition and a general-purpose expert advisor that routes to the right specialist based on technology context."
type: domain
building_blocks:
  contracts: "Expert skill conventions and routing protocol shared with other implementation-expert domains."
  docs: "Typst version notes, integration patterns with data pipelines, and advisor routing logic."
  skills: "Typst implementation expert and the general expert-advisor router."
  agents: "TBD"
  prompts: "TBD"
  tools: "TBD"
stage: alpha
---

# Dev Implementation Experts — Typst

This domain covers Typst document composition expertise and the cross-domain expert advisor. The `dev-implementation-expert-typst` skill provides deep knowledge of Typst's markup, scripting, and layout system. The `dev-implementation-expert-advisor` skill acts as a general-purpose router: it reads context and delegates to the most relevant expert skill across all expert domains.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Expert skill conventions and advisor routing protocol |
| `docs/` | Typst version notes, template patterns, data pipeline integration |
| `skills/` | Typst expert and advisor router |

## Skills

| Skill | Technology |
|-------|-----------|
| `dev-implementation-expert-typst` | Typst — document markup, scripting, template design, PDF/HTML output |
| `dev-implementation-expert-advisor` | General router — reads tech stack context and delegates to the right expert domain/skill |

## Conventions

- The `dev-implementation-expert-advisor` skill is a routing layer only — it does not implement; it dispatches
- Typst skill covers both document authoring and programmatic generation via Typst's scripting layer
- Expert skills are never the primary entry point — they are delegated to by orchestrator or feature-implementation skills
