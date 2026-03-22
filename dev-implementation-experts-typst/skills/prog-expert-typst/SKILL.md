---
name: "prog-expert-typst"
description: "Use when you need to implement a typst template from an image or pdf reference iteratively. Expert-level programming and pattern management."
metadata:
  stage: "alpha"
  source: "MIGRATED"
---

# Prog Expert Typst

## Goal
Expert-level project management for Typst. Handles complex integrations, gathers knowledge from docs and web research, and maintains a repository of reusable patterns, recipes, atomic examples, and reference implementations.
The primary goal is to iteratively implement a pixel-perfect Typst template matching a reference image or PDF, utilizing local tools for visual comparison and syntactic correctness.

## Core Workflow (Progressive Disclosure)
1. **Analyze Reference**: Look at the reference image or PDF.
2. **Initial Structure**: Create a basic `main.typ` with the correct page dimensions and basic layout structure.
3. **Iterative Comparison**:
   - Run the compile and compare script.
   - Analyze the visual difference map (`diff_output.png`) to spot misalignments, wrong fonts, or incorrect spacing.
   - Adjust `main.typ`.
4. **Correctness Check**: Periodically run the tinymist script to ensure there are no syntax errors or warnings.
5. **Final Polish**: Refine until the difference score is minimized and the layout looks identical to the human eye.
6. **Learning**: 
    - Extract patterns from successful implementations via `programming/prog-expert-advisor/scripts/learn.py`.
    - Create or refine coding recipes in `recipes/` using `scripts/manage_recipes.py`.

## Instructions
- ALWAYS use the preferred package manager for this ecosystem.
- Before starting any task, search the `recipes/` directory for relevant implementation patterns.
- If a task involves a recurring pattern not yet in `recipes/`, create a new recipe after successful implementation.
- Proactively update your knowledge if library versions are outdated or new patterns are discovered.
- Use `uv run scripts/typst_compile_and_compare.py main.typ <reference_file>` to compile your current Typst code and generate a difference map against the reference.
- By default, it outputs `compiled_output.png` and `diff_output.png`.
- The script automatically converts reference PDFs using `pdftoppm` if needed.
- Use `uv run scripts/check_tinymist.py main.typ` to validate the Typst file syntactically using the local `tinymist` installation.

## Example Code
When learning or implementing, use these code examples. ALWAYS load them via `view_file` to maintain Progressive Disclosure:

- **Atomic Examples** (Small code chunks from docs):
  - *(Add examples here)*: `view_file(<absolute_path>/atomic-examples/...)`
- **Recipes** (Larger patterns like Auth, Pages):
  - *(Add recipes here)*: `view_file(<absolute_path>/recipes/...)`
- **Reference Implementations** (Complex pseudo-code from existing real-world codebases):
  - *(Add references here)*: `view_file(<absolute_path>/reference-implementations/...)`

## Constraints
* Always ensure `poppler-utils` (for `pdftoppm`), `typst`, and `tinymist` are available in the system PATH.
* Do not hallucinate Typst features; rely on the `check_tinymist.py` feedback if you are unsure of syntax.
* Minimize the perceptual difference score, but prioritize semantic structural layout over 100% pixel matching if font rendering differences occur.
* Do not perform unauthorized or destructive actions.
* Do not overwrite existing files without explicit user confirmation.

## Self-Learning & Research
- Gathers knowledge using web research and the `use-context7-api` skill.
- Learn from successful implementations when used, from other code bases and from researching the documentation.
- Keep track of the latest library versions.
- Refine recipes to be more granular based on implementation experience.

## Auto-Improvement
- Every time this skill is used, analyze the usage chat to find out if further improvement of the skill is advised.
- Ask the user if those changes should be made.
- If approved, store the improvement ideas in the `resources/improvement_ideas.md` file.

## References
- [Patterns](references/patterns.md) — Reusable code patterns and best practices.
- [Versions](references/versions.json) — Tracked library versions.
- [Recipes](recipes/README.md) — Coding recipes for recurring tasks.

## Script Integration
- **Compile & Compare**: `uv run scripts/typst_compile_and_compare.py main.typ <reference>`
- **Linting**: `uv run scripts/check_tinymist.py main.typ`
- **Research**: `uv run scripts/research_knowledge.py "<query>"`
- **Version Tracking**: `uv run scripts/track_versions.py`
- **Recipe Management**: `uv run scripts/manage_recipes.py <action> [args]`
- **Example Management**: `uv run scripts/manage_examples.py <action> [args]`
- **Pattern Learning**: `uv run programming/prog-expert-advisor/scripts/learn.py <path/to/file>`
