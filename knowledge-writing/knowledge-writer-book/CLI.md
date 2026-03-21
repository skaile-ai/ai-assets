# CLI Documentation: Book Writer

This skill operates as an autonomous multi-agent system. It does not provide a traditional executable CLI script but rather interprets conversational "pseudo-commands" or triggers in the chat prompt. These commands orchestrate the 5-agent team (Architect, Researcher, Pedagogue, Writer, Critic).

## Commands

### `/start_book [topic]`
**Agents triggered:** Architect + Researcher  
**Phase:** 1

Initiates the creation of a new medical textbook project.
- **Actions:** 
  - Analyzes the topic scope and target audience.
  - Conducts initial research (e.g., searches for AWMF/NICE guidelines).
  - Prompts for book slug approval.
  - Scaffolds the book directory structure (`books/<slug>/`).
  - Generates the concept documents (`book_bible.md`, `style_decisions.md`).
  - Creates the initial Table of Contents (`toc.md`) with minimum chapter requirements and planned content blocks.
  - Initializes `index.yaml` global state.
- **Output:** Directory structure, `concept/`, initial `research/sources/`, `toc.md`, `index.yaml`.

### `/plan_chapter [chapter_id]`
**Agents triggered:** Researcher + Pedagogue  
**Phase:** 2

Plans the specific content for a single chapter.
- **Actions:** 
  - Reads `index.yaml` to identify the chapter and resolves the appropriate template (e.g., `diagnosis.md`, `etiology.md`).
  - Conducts deep research for guidelines and meta-analyses within the last 5 years, updating `research/sources/`.
  - Designs instructional elements: 3–4 learning objectives (Lernziele) and a clinical vignette (Fallvignette).
  - Drafts the `scaffold.md` file, which includes core content points marked with `[x]` and optional context marked with `[ ]`.
  - Updates `index.yaml` status to `scaffolded`.
- **Output:** Updates to `research/`, `chapters/<ch_id>/scaffold.md`.

### `/write_chapter [chapter_id]`
**Agents triggered:** Writer + Critic  
**Phase:** 3

Drafts, reviews, and finalizes the prose for a scaffolded chapter.
- **Actions:** 
  - Reads chapter summaries (context), the specific `scaffold.md`, and `book_bible.md`.
  - Processes only items marked with `[x]` in the scaffold into German academic prose.
  - Evaluates the draft via the Critic agent (checking definitions, consistency, and guidelines), iterating if needed.
  - Saves intermediate steps to `draft.md` and `review.md`.
  - Generates the final prose in `final.md` and appends a "Kernaussagen" (Key Takeaways) table.
  - Updates `index.yaml` status to `final` with a 2-sentence summary.
- **Output:** `chapters/<ch_id>/draft.md`, `chapters/<ch_id>/review.md`, `chapters/<ch_id>/final.md`.

---

## Suggested Subcommands

For future expansion of the Book Writer skill, the following commands could be implemented to improve the review and export workflow:

### `/review_book`
**Proposed Agents:** Critic + Architect  
**Purpose:** Run a full consistency pass across all `final.md` chapters. Checks for contradictory definitions, missing cross-references, and adherence to `style_decisions.md` before final export. 

### `/export_pdf`
**Proposed Agents:** Architect  
**Purpose:** Assembles the completed book from all `final.md` chapter files, resolves cross-references, applies Markdown-to-PDF formatting (via pandoc or Typst), and generates a publish-ready document in `exports/full_book.pdf`.

### `/update_research [topic]`
**Proposed Agents:** Researcher  
**Purpose:** Refreshes the `research/sources/` for a specific topic with the latest guidelines (e.g., newly published AWMF/NICE papers) and flags corresponding chapter scaffolds if new evidence changes core recommendations.