---
name: "knowledge-writer-podcast"
description: "Use when you need to generate long form podcast content from documents using a sophisticated multi-agent orchestration process."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "knowledge-writing-contract"
---

# Knowledge Writer Podcast

## Goal
To synthesize broadcast-ready, hyper-realistic two-speaker dialogue scripts from scientific papers or domain knowledge. It replaces monolithic prompts with a mathematically chunked, multi-agent generative framework to enforce pacing, acoustic imperfections (interruptions, backchanneling), and distinct conversational personas.

## Core Workflow (Agentic Orchestration)

This skill relies purely on your agentic abilities and tools. You **MUST** execute the following phases in order, keeping ALL materials within a single episode directory:

### Phase 1: Ingestion & Deep Research
1. **Initialize Project**: Use the `init-podcast.py` CLI to set up the directory structure and templates:
   `uv run .agent/tools/init-podcast.py <episode_name> --type writer`
   This creates `episodes/<episode_name>/` with `30_podcast_concept/en/setup/` and `30_podcast_concept/de/setup/` containing the required templates.
2. **Document Conversion**: If the provided source is a PDF or other non-text format, use the `pdf-to-markdown` skill or `use-docling` to extract and convert the text. Save this as `10_source/<filename>.md`.
3. **Web Context & Research**: Use `web_search`, `web_search_deep`, or `use-perplexity` to research the topic, authors, related controversies, and real-world applications. Synthesize these findings into `20_research/research_notes.md`.

### Phase 2: Planning & Setup
1. **Analyze Materials**: Read both the source `10_source/<filename>.md` and `20_research/research_notes.md`.
2. **Draft Configurations**: Based on the source material and research notes, fill out the templates in both `30_podcast_concept/en/setup/` and `30_podcast_concept/de/setup/`:
   - Update `setup/focus.md` (3-Act structure).
   - Update `setup/host_a.md` and `setup/host_b.md` (Persona method).
   - **CRITICAL:** Use Markdown ONLY for these configurations. Do NOT use JSON for host definitions.
   - **CRITICAL:** Do not translate speaker names between languages.
3. **Review**: Present the drafted configuration files to the user for approval.

### Phase 3: Outlining (The Beat Sheet)
1. **Generate SINGLE Outline**: Based on the approved `en/setup/focus.md`, create a single `outline.md` in `30_podcast_concept/`. This outline will serve BOTH languages to ensure structural parity.
2. **Mathematical Chunking**: Break the narrative into discrete chapters (segments). Each chapter MUST have:
   - A title and sequence number (e.g., `01_intro.md`)
   - The Narrative Act (Act I, II, III, or Epilogue)
   - A specific `dialectical_goal`
   - Target length in characters (~1000-1500)
   - Specific concepts/notes to cover.

### Phase 4: Agentic Delegation (The Writer Subagents)
You will use the `task` tool to fan out the generation of each chapter to separate subagents (`agent: task`). You MUST explicitly generate two variants for each chapter: one in English and one in German.
*Note: To ensure "Contextual Redundancy" across chunks, you **SHOULD** explicitly provide the expected conclusion of the previous chapter in the assignment for the current chapter.*

**Subagent Context (`context` parameter)**:
- Include the content of the relevant language-specific configuration files (`30_podcast_concept/<lang>/setup/focus.md`, `host_a.md`, and `host_b.md`).
- Include the global rules for the **Three-Pass Acoustic Refinement** (see `references/advanced-prompt-architecture.md`).

**Subagent Assignment (`assignment` parameter)**:
- Assign the specific chapter and its `dialectical_goal`.
- Provide the summary of the *previous* chapter to ensure a smooth, contextual transition.
- **Instruct the subagent to perform the Three-Pass Refinement internally**:
  - *Pass 1 (Pressure Cooker)*: Draft dialectical tension and contextual recaps in memory.
  - *Pass 2 (Dialogue Assassin)*: Strip LLM therapy-speak and inject physical beats (`[sigh]`, `[chuckles]`) in memory.
  - *Pass 3 (Anchor Pass)*: Engineer burstiness, backchanneling (`(mhm)`), and overlaps (`—`), then output the final script.
- **Instruct the subagent to write the final result to a language-specific Markdown file** (`40_podcast/en/XX_chapter_name.md` or `40_podcast/de/XX_chapter_name.md`):
  - **Frontmatter**: Must include `description` (summary), `characters` (a dictionary mapping speaker names to host IDs, e.g., `{"Clara": "host_a_expert", "Tristan": "host_b_novice"}`). Names MUST match exactly across languages.
  - **Body**: The dialogue text formatted exactly as `<speaker>: <text>` for each line. No other markdown formatting in the body.

### Phase 5: Director Review
1. Review the generated chapter markdown files in `40_podcast/`.
2. Ensure pacing, acoustic markers, and character constraints were followed.
3. Inform the user that the broadcast-ready dialogue is complete.

## References
- [Advanced Prompt Architecture](references/advanced-prompt-architecture.md) — The theoretical foundation of conversational AI generation.
- [Prompt Templates](references/prompts/) — The Jinja templates for each pass (can be used as instructions for the subagents).

## Constraints
* Do not perform unauthorized or destructive actions.
* Do not overwrite existing files without explicit user confirmation.
* Do not generate single-shot long-form podcasts; you **MUST** follow the multi-pass, chunked architecture to prevent model collapse.
* You **MUST** rely on the `task` tool for Phase 4 to ensure isolated, mathematically bound generation instances.