# Example: Knowledge Writer Podcast (Agentic Orchestration)

## Input
Desired trigger for the skill:
"Generate a podcast script about the transition from monolithic software architecture to microservices from the paper `monolith_to_microservices.pdf`."

## Workflow Execution

1. **Ingestion & Deep Research**: The agent creates the folder `monolith_microservices_podcast/`. It calls the `pdf-to-markdown` skill (or uses `use-docling`) to extract the text from the PDF, saving it as `source_material.md`. It then uses the `web_search_deep` tool to find recent industry examples, related controversies, and background on the authors, saving these findings to `research_notes.md`.
2. **Configuration**: The agent analyzes both the source material and the research notes, then drafts `focus.md`, `host_a.json`, and `host_b.json` inside the episode folder, asking the user for approval.
3. **Outlining**: The agent generates an `outline.md` defining 4 distinct chapters (Act I, Act IIa, Act IIb, Act III/Epilogue), complete with character limits, dialectical goals, and specific concepts to cover for each.
4. **Delegation**: The agent uses the `task` tool, fanning out to 4 subagents. Each subagent is assigned one chapter from the outline.
5. **Generation**: Inside its assignment, each subagent acts as the "Screenwriter" and "Director":
   - It runs Pass 1 (The Pressure Cooker) in its reasoning steps.
   - It runs Pass 2 (The Dialogue Assassin) to strip LLM therapy-speak.
   - It runs Pass 3 (The Anchor Pass) to inject backchanneling (`(mhm)`), hard cut-offs (`—`), and burstiness.
6. **Output**: Each subagent writes its final broadcast-ready dialogue to its respective file (e.g., `01_the_hook.md`, `02_the_friction.md`) with a YAML frontmatter summary.

## Expected Output
A single unified folder `monolith_microservices_podcast/` containing:
- `source_material.md` (converted from PDF)
- `research_notes.md` (web context)
- `focus.md`, `host_a.json`, `host_b.json` (configuration)
- `outline.md` (beat sheet)
- Individual markdown files for each chapter (`01_...md`, `02_...md`), ready for TTS ingestion.