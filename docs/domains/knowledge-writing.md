---
title: knowledge-writing
description: Content generation skills — transform structured research into podcast scripts, book chapters, and audio productions.
---

Produces polished written and audio content from structured research. Consumes the output of `knowledge-research` (synthesis files, paper extractions) and transforms them into audience-ready artifacts: podcast scripts, book chapters, and full productions.

Writing skills prioritize voice, narrative arc, and accessibility — they are the final production stage of the knowledge pipeline.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | Output format conventions, speaker protocols, quality criteria per content type |
| `docs/` | Writing workflow guides, format templates, production notes |
| `prompts/` | Reusable speaker personas, tone instructions, chapter structure templates |
| `knowledge-paper-podcast/` | Podcast-from-paper skill |
| `knowledge-writer-podcast/` | Full hyper-realistic podcast skill |
| `book-writer/` | Long-form book writing skill |

## Skills

| Skill | When to use |
|---|---|
| `knowledge-paper-podcast` | Convert an academic paper (or paper extraction) into a podcast script — accessible, conversational |
| `knowledge-writer-podcast` | Full hyper-realistic two-speaker podcast episode from any topic — includes intro, segments, banter |
| `book-writer` | Autonomous multi-chapter book writing — structured outline, chapter drafts, revision loop |

## Pipeline

```
knowledge-research   →   knowledge-writing   →   (ElevenLabs via use/)
    findings/                 scripts/                  audio files
    extractions/              chapters/
```

Research output from `_grounding/` feeds directly into writing skills. For audio production, combine with `use/use-elevenlabs` after generating scripts.
