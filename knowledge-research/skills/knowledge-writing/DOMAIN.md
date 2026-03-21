---
name: knowledge-writing
description: "Content generation skills that transform structured research (from knowledge-research) into polished written artifacts: podcast scripts, book chapters, and audio productions."
type: domain
building_blocks:
  contracts: "Output format conventions for each content type (podcast scripts, book structure), speaker/character protocols, and quality criteria."
  docs: "Writing workflow guides, format templates, and production notes for audio/text output."
  skills: "Three skills: knowledge-paper-podcast (academic paper → podcast script), knowledge-writer-book (autonomous 5-agent book writing), knowledge-writer-podcast (hyper-realistic two-speaker podcast)."
  agents: "TBD"
  prompts: "Reusable prompt fragments for tone, voice, speaker personas, and chapter structures."
  tools: "TBD"
stage: alpha
---

# Knowledge Writing

This domain produces polished written and audio content from structured research. It consumes the output of `knowledge-research` (synthesis files, paper extractions) and transforms them into audience-ready artifacts: podcast scripts, book chapters, and full productions.

Writing skills prioritize voice, narrative arc, and accessibility — they are the final production stage of the knowledge pipeline.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Output format conventions, speaker protocols, quality criteria per content type |
| `docs/` | Writing workflow guides, format templates, production notes |
| `skills/` | `knowledge-paper-podcast`, `knowledge-writer-book`, `knowledge-writer-podcast` |
| `prompts/` | Reusable speaker personas, tone instructions, chapter structure templates |

## Skills

| Skill | Purpose |
|-------|---------|
| `knowledge-paper-podcast` | Transform an academic paper (or paper extraction) into a two-speaker podcast script |
| `knowledge-writer-book` | Autonomous 5-agent system for writing a structured book from a research corpus |
| `knowledge-writer-podcast` | Hyper-realistic two-speaker podcast production with natural dialogue, interruptions, and pacing |

## Conventions

- Writing skills expect structured input (frontmatter + markdown) from `knowledge-research` — not raw PDFs or URLs
- Podcast skills produce scripts in a standard format with speaker labels, stage directions, and timing cues
- Book writing uses a multi-agent pattern — outline agent, chapter agents, review agent, editor agent
- Use `use-elevenlabs` for TTS rendering after a script is produced
