---
name: prog-expert-marimo
source: MIGRATED
description: Use when you need to create, edit, or debug Marimo reactive Python notebooks, including cell structure, reactivity rules, output display, and the Lint-Export-Assert test protocol. Expert-level programming and pattern management.
keywords: []
reads_from: []
writes_to: []
metadata:
  stage: alpha
  requires:
  - impl-experts-python-contract
---

# Prog Expert Marimo

## Goal
Expert-level Marimo notebook development. Generates robust, reproducible `.py` notebooks with correct cell structure, reactivity rules, and self-verification using the Lint-Export-Assert protocol.

## Critical Knowledge (Always Apply)
- **File format**: Pure `.py` only — never `.ipynb`
- **Entry point**: `app = marimo.App()` + every cell is `@app.cell`
- **Reactivity**: Variables returned from a cell are global; redefining a name in two cells is an error
- **No side effects**: Never mutate; always produce new variables (e.g., `df_clean = df.dropna()` not `df.dropna(inplace=True)`)
- **Output**: Last expression in cell auto-displays; use `mo.md(...)` for markdown; return figure objects (no `.show()`)
- **Test protocol**: See `references/patterns.md` — Lint → Export → Assert before declaring done

## Core Workflow (Progressive Disclosure)
1. **Context Analysis**: Analyze the current codebase and project state.
2. **Knowledge Retrieval**:
    - Fetch latest library versions using `scripts/track_versions.py`.
    - Check `recipes/` for reoccuring patterns and implementation guides.
    - If needed, research new patterns via `scripts/research_knowledge.py`.
3. **Implementation**: Execute core logic. Leverage `atomic-examples/` and `recipes/` for guidance.
4. **Learning**: 
    - Extract patterns from successful implementations via `programming/prog-expert-advisor/scripts/learn.py`.
    - Create or refine coding recipes in `recipes/` using `scripts/manage_recipes.py`.

## Instructions
- ALWAYS use the preferred package manager for this ecosystem.
- Before starting any task, search the `recipes/` directory for relevant implementation patterns.
- If a task involves a recurring pattern not yet in `recipes/`, create a new recipe after successful implementation.
- Proactively update your knowledge if library versions are outdated or new patterns are discovered.

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

## Example Code
When learning or implementing, use these code examples. ALWAYS load them via `view_file` to maintain Progressive Disclosure:

- **Atomic Examples** (Small code chunks from docs):
  - *(Add examples here)*: `view_file(<absolute_path>/atomic-examples/...)`
- **Recipes** (Larger patterns):
  - **Notebook Template & Patterns**: `view_file(<absolute_path>/recipes/recipe-marimo-notebook.md)`
- **Reference Implementations** (Complex pseudo-code from existing real-world codebases):
  - *(Add references here)*: `view_file(<absolute_path>/reference-implementations/...)`

## Constraints
* Do not perform unauthorized or destructive actions.
* Do not overwrite existing files without explicit user confirmation.

## Script Integration
- **Research**: `uv run scripts/research_knowledge.py "<query>"`
- **Version Tracking**: `uv run scripts/track_versions.py`
- **Recipe Management**: `uv run scripts/manage_recipes.py <action> [args]`
- **Example Management**: `uv run scripts/manage_examples.py <action> [args]`
- **Pattern Learning**: `uv run programming/prog-expert-advisor/scripts/learn.py <path/to/file>`
