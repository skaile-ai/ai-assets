---
title: knowledge-research
description: Knowledge acquisition skills — deep multi-phase research, academic paper extraction, and structured synthesis from external sources.
---

Handles knowledge acquisition from external sources. Skills drive structured research processes — from deep web research with synthesis, to systematic extraction and analysis of academic papers. Output feeds directly into `knowledge-writing` for content production.

All three skills are designed to be invoked directly from a project context or in parallel as a research mode alongside other pipeline steps.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | Research artifact structure (findings/, summaries/), metadata fields, citation conventions |
| `docs/` | Research workflow guides, source quality criteria |
| `deep-research/` | `deep-research` skill directory |
| `knowledge-paper-extractor/` | `knowledge-paper-extractor` skill directory |
| `knowledge-paper-research/` | `knowledge-paper-research` skill directory |

## Skills

| Skill | When to use |
|---|---|
| `deep-research` | Integrated 3-phase research — query formulation → multi-source search → synthesis into structured findings |
| `knowledge-paper-extractor` | Extract key metadata, methods, findings, and citations from a single academic PDF |
| `knowledge-paper-research` | Multi-phase paper research — find papers on a topic, extract insights, synthesize across sources |

## Output Structure

Research skills write to `_grounding/`:

```
_grounding/
├── general/
│   ├── domain.md          ← domain overview
│   ├── competitors.md     ← competitive landscape
│   └── ...
└── findings/
    ├── summary.md         ← synthesized findings
    └── sources.md         ← citations and references
```

The `_grounding/` folder is readable by all pipeline skills — concept, implementation, and quality skills can reference research findings without re-running research.
