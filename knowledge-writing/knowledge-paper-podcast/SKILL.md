---
name: knowledge-paper-podcast
source: MIGRATED
description: Use when you need to create a podcast script from the output of the knowledge-paper-research skill.
keywords: []
reads_from: []
writes_to: []
metadata:
  stage: alpha
  requires:
  - knowledge-writing-contract
---

# Knowledge Paper Podcast

## Goal
To synthesize broadcast-ready, engaging two-speaker dialogue scripts explaining a scientific paper. This skill specializes the `knowledge-writer-podcast` framework by grounding the conversation in structured research context files.

## Core Workflow (Specialization)

This skill follows the **Core Workflow** of the `knowledge-writer-podcast` skill but with the following specific overrides and requirements:

### Phase 1: Ingestion
1. **Locate Research Context**: Identify the `20_research/` directory and `10_source/<stem>.md` file produced by the `knowledge-paper-research` skill.
2. **Initialize Project**: Use the `init-podcast.py` CLI with the `paper` type:
   `uv run .agent/tools/init-podcast.py <stem> --type paper`
   Next to the original paper, this creates `30_podcast_concept/en/setup/` and `30_podcast_concept/de/setup/` using paper-specialized templates.
3. **Contextual grounding**: Instead of general research notes, you **MUST** use the following files from `20_research/` as your primary source material:
   - `_background.md`
   - `_paper_description.md`
   - `_clinical_implications.md`
   - `author_*.md` profiles.

### Phase 2: Planning & Setup
1. **Specialized Templates**: Follow `knowledge-writer-podcast` Phase 2, but use the specialized `setup/focus.md` which includes paper-specific sections (Authors, Methods, Results).
2. **Review**: Present the drafted configurations to the user.

### Phase 3: Outlining (The Beat Sheet)
1. **Paper-Specific Outline**: Follow `knowledge-writer-podcast` Phase 3, but the single `outline.md` **MUST** include chapters corresponding to:
   - Paper Introduction
   - Author Backgrounds
   - Theoretical Context
   - Methods & Methodology
   - Results & Key Findings
   - Clinical Implications & Broader Impact.

### Phase 4: Agentic Delegation
- Follow `knowledge-writer-podcast` Phase 4.
- **Subagent Assignment**: For each chapter, you **MUST** explicitly provide the contents of the relevant research files (e.g., provide `author_*.md` for the Authors chapter).

## Constraints
- **Inheritance**: Unless specified above, follow all procedural rules of `knowledge-writer-podcast`.
- **Factuality**: Ground all dialogue strictly in the research context files.
- **Bilingual structure**: Use the `40_podcast/en/` and `40_podcast/de/` subdirectories for output.
- **Single Outline**: Use the same `outline.md` for both English and German versions.
