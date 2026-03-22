---
name: "prog-expert-una"
description: "Use when you need to implement UnaUI (@una-ui/nuxt) in Nuxt 3/4 — N* component usage, color tokens, slot patterns, UnoCSS-driven theming, and UnaUI module configuration."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "impl-experts-js-contract"
---

# Prog Expert UnaUI

## Goal
Expert-level implementation of UnaUI in Nuxt projects. Covers the `@una-ui/nuxt` module setup, `N*` component API (NButton, NCard, NAlert, NInput, NCheckbox, NAvatar, NBadge, etc.), color prop conventions, slot composition patterns, and UnoCSS design token integration.

## Core Workflow (Progressive Disclosure)
1. **Context Analysis**: Analyze `nuxt.config.ts` modules section, existing `N*` component usage, and `uno.config.ts` theming.
2. **Knowledge Retrieval**:
    - Fetch latest library versions using `scripts/track_versions.py`.
    - Check `recipes/` for recurring patterns and implementation guides.
    - If needed, research new patterns via `scripts/research_knowledge.py`.
3. **Implementation**: Execute core logic. Leverage `atomic-examples/` and `recipes/` for guidance.
4. **Learning**:
    - Extract patterns from successful implementations via `scripts/learn_from_success.py`.
    - Create or refine coding recipes in `recipes/` using `scripts/manage_recipes.py`.

## Instructions
- ALWAYS use pnpm as the package manager for this ecosystem.
- Before starting any task, search the `recipes/` directory for relevant implementation patterns.
- If a task involves a recurring pattern not yet in `recipes/`, create a new recipe after successful implementation.
- Proactively update your knowledge if library versions are outdated or new patterns are discovered.
- Scope: UnaUI component integration only. For raw UnoCSS config use `prog-expert-uno`. For Nuxt core concerns use `prog-expert-nuxt`.

## Key Technologies
- `@una-ui/nuxt` (alpha channel)
- `unocss`, `@unocss/nuxt`, `@unocss/reset`
- `@iconify-json/lucide`, `@iconify-json/radix-icons`, `@iconify-json/tabler`
- `@vueuse/core`, `pinia`

## Self-Learning & Research
- Gathers knowledge using web research and the `use-context7-api` skill.
- Learn from successful implementations, other codebases, and documentation.
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
- **Recipes** (Larger patterns like form layouts, nav, data display):
  - *(Add recipes here)*: `view_file(<absolute_path>/recipes/...)`
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
- **Pattern Learning**: `uv run scripts/learn_from_success.py <path/to/file>`
