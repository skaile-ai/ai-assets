---
name: prog-expert-omp
source: MIGRATED
description: Use when you need to work with omp (pi-coding-agent), including RPC mode integration, skill authoring, extension development, agent embedding, and CLI configuration. Expert-level programming and pattern management.
env_vars:
  OPENROUTER_API_KEY: "Typically required for omp to access models via OpenRouter."
keywords: []
reads_from: []
writes_to: []
metadata:
  stage: alpha
  requires:
  - impl-experts-js-contract
---

# Prog Expert OMP

## Goal
Expert-level project management for OMP (pi-coding-agent / @oh-my-pi/pi-coding-agent). Handles RPC sidecar integration, skill authoring, extension development, CLI configuration, and maintains a repository of reusable patterns, recipes, and atomic examples for embedding and extending omp.

## Core Workflow (Progressive Disclosure)
1. **Context Analysis**: Analyze the current codebase — is this an embedding task (RPC sidecar), skill authoring, extension development, or CLI usage?
2. **Knowledge Retrieval**:
    - Check `recipes/` for relevant implementation patterns (RPC integration, skill format, extension hooks).
    - Read `references/rpc-protocol.md` for the full RPC wire protocol specification.
    - Read `references/patterns.md` for architectural patterns and gotchas.
    - Read `references/cli-reference.md` for CLI flags and configuration.
    - If needed, research new patterns via `scripts/research_knowledge.py`.
3. **Implementation**: Execute core logic. Leverage `atomic-examples/` and `recipes/` for guidance. Always validate against the RPC protocol spec.
4. **Learning**:
    - Extract patterns from successful implementations via `programming/prog-expert-advisor/scripts/learn.py`.
    - Create or refine coding recipes in `recipes/` using `scripts/manage_recipes.py`.

## Instructions
- ALWAYS use `bun` for omp-native development (skills, extensions). Use the host runtime (node/bun) for embedding.
- Before starting any task, search the `recipes/` directory for relevant implementation patterns.
- If a task involves a recurring pattern not yet in `recipes/`, create a new recipe after successful implementation.
- When embedding omp via RPC: ALWAYS read `references/rpc-protocol.md` first for the wire format.
- When authoring skills: ALWAYS read `references/skill-format.md` for the frontmatter and activation spec.
- Model format is ALWAYS `provider/model` (e.g. `openrouter/anthropic/claude-sonnet-4`), NEVER `p-provider/model`.
- omp emits `message_end` for BOTH user and assistant messages — NEVER double-persist user messages.
- The `ready` event MUST be awaited before sending any commands via RPC.
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
- [RPC Protocol](references/rpc-protocol.md) — Full wire protocol specification for `--mode rpc`.
- [CLI Reference](references/cli-reference.md) — Complete CLI flags, env vars, and configuration.
- [Skill Format](references/skill-format.md) — How to author omp skills.
- [Patterns](references/patterns.md) — Architectural patterns, gotchas, and best practices.
- [Versions](references/versions.json) — Tracked library versions.
- [Recipes](recipes/README.md) — Coding recipes for recurring tasks.

## Key Embedding Pattern

When embedding omp in a web app, use the **Typed Action Protocol** (not raw event forwarding).
See `recipes/embed-typed-action-protocol.md` and the `prog-expert-integration-ai-agents` skill.

**DO NOT** forward raw omp events to the frontend via SSE — use the Skill Adapter pattern instead.
**DO NOT** trigger skills via string matching (`"Run skill X"`) — use structured dispatch.

## Example Code
When learning or implementing, use these code examples. ALWAYS load them via `view_file` to maintain Progressive Disclosure:

- **Atomic Examples** (Small code chunks from docs):
  - *(Add examples here)*: `view_file(<absolute_path>/atomic-examples/...)`
- **Recipes** (Larger patterns like Auth, Pages):
  - *(Add recipes here)*: `view_file(<absolute_path>/recipes/...)`
- **Reference Implementations** (Complex pseudo-code from existing real-world codebases):
  - *(Add references here)*: `view_file(<absolute_path>/reference-implementations/...)`

## Constraints
* Do not perform unauthorized or destructive actions.
* Do not overwrite existing files without explicit user confirmation.
* When spawning omp processes, always handle exit/error events to avoid orphan processes.
* Never hardcode API keys — always use environment variables.

## Script Integration
- **Research**: `uv run scripts/research_knowledge.py "<query>"`
- **Version Tracking**: `uv run scripts/track_versions.py`
- **Recipe Management**: `uv run scripts/manage_recipes.py <action> [args]`
- **Example Management**: `uv run scripts/manage_examples.py <action> [args]`
- **Pattern Learning**: `uv run programming/prog-expert-advisor/scripts/learn.py <path/to/file>`
