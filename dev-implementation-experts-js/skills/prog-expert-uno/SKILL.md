---
name: "prog-expert-uno"
description: "Use when you need to implement or configure UnoCSS in Nuxt 3/4 projects — presets (wind4, icons, attributify, typography), uno.config.ts theming, dark mode, safelist, layers, and UnoCSS module integration."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "impl-experts-js-contract"
---

# Prog Expert UnoCSS

## Goal
Expert-level implementation of UnoCSS in Nuxt projects. Covers `uno.config.ts` authoring, preset configuration, custom color tokens, icon integration via `presetIcons`, dark mode strategies, layer ordering, safelist management, and the `@unocss/nuxt` module setup.

## Core Workflow (Progressive Disclosure)
1. **Context Analysis**: Analyze `uno.config.ts`, `nuxt.config.ts` modules section, and existing utility usage.
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
- Scope: UnoCSS configuration and integration only. For component-library-specific concerns use `prog-expert-una` (UnaUI) or `prog-expert-scadcn` (shadcn-vue).

## Key Technologies
- `unocss`, `@unocss/nuxt`, `@unocss/reset`
- Presets: `presetWind4`, `presetAttributify`, `presetIcons`, `presetTypography`
- `@iconify-json/*` icon sets
- Dark mode via `class` strategy in `presetWind4`

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
- **Recipes** (Larger patterns like theme setup, icon config):
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
