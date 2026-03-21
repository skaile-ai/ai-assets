---
name: prog-expert-vueuse
source: MIGRATED
description: Use when you need to implement VueUse composables in Nuxt 3, including auto-import configuration, composable selection, conflict handling with Nuxt built-ins, and reactive browser/state/DOM utilities. Expert-level programming and pattern management.
keywords: []
reads_from: []
writes_to: []
metadata:
  stage: alpha
  requires:
  - impl-experts-js-contract
---

# Prog Expert VueUse

## Goal
Expert-level VueUse development in Nuxt 3. Handles composable selection, auto-import conflict resolution, and maintains a repository of reusable patterns, recipes, and examples for reactive browser, state, and DOM utilities.

## Critical Knowledge
- **Package manager**: `pnpm`
- **Conflicting auto-imports** (MUST import explicitly from `@vueuse/core`): `useFetch`, `useHead`, `useCookie`, `useStorage`, `useImage`, `toRef`, `toRefs`, `toValue`
- **All other composables** are auto-imported once `@vueuse/nuxt` is in modules — no import statements needed
- See `references/patterns.md` for the full composable reference table by category

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
  - **Setup & Common Patterns**: `view_file(<absolute_path>/recipes/recipe-vueuse-nuxt-setup.md)`
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
