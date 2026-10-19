---
name: knowledge-writing
description: "Content generation skills that transform structured research (from knowledge-research) into polished written artifacts: podcast scripts, book chapters, and audio productions."
type: domain
building_blocks:
  contracts: "Output format conventions for each content type (podcast scripts, book structure), speaker/character protocols, and quality criteria (`contracts/`)."
  skills: "Three skills (preset `knowledge-writing`), each a directory next to this file: knowledge-paper-podcast (academic paper → podcast script), book-writer (autonomous 5-agent book writing), knowledge-writer-podcast (hyper-realistic two-speaker podcast)."
  agents: "TBD"
  prompts: "TBD"
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
| `book-writer/`, `knowledge-paper-podcast/`, `knowledge-writer-podcast/` | The three skills, each directly next to this `DOMAIN.md` (no `skills/` subfolder) |

There is no `docs/` or `prompts/` folder yet.

This is the one nested domain in the repo: it sits at `knowledge-research/skills/knowledge-writing/`, one level deeper than the usual `<domain>/skills/<name>/`. The skills are still published catalog assets (`skill:@skaile-ai/<name>`, listed in `knowledge-writing.preset.yaml`); discovery finds them by file name, not by depth, and `scripts/presets-resolve.test.sh` checks that each ref resolves.

## Skills

| Skill | Purpose |
|-------|---------|
| `knowledge-paper-podcast` | Transform an academic paper (or paper extraction) into a two-speaker podcast script |
| `book-writer` | Autonomous 5-agent system for writing a structured book from a research corpus |
| `knowledge-writer-podcast` | Hyper-realistic two-speaker podcast production with natural dialogue, interruptions, and pacing |

## Conventions

- Writing skills expect structured input (frontmatter + markdown) from `knowledge-research` — not raw PDFs or URLs
- Podcast skills produce scripts in a standard format with speaker labels, stage directions, and timing cues
- Book writing uses a multi-agent pattern — outline agent, chapter agents, review agent, editor agent
- Use `use-elevenlabs` for TTS rendering after a script is produced
