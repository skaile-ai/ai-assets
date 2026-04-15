---
name: "skailup-prog-expert-integration-ai-agents"
description: "Use when integrating LLM agent sidecars (omp RPC) into a web frontend. Covers the Typed Action Protocol: SkillAction/ConsumerResponse bidirectional events, skill state machine, adapter layer, structured input/approval flows, API key resolution, and default model selection. Expert-level programming and pattern management."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "architecture-contract"
  env_vars:
    OPENROUTER_API_KEY: "Optional — for omp to access models via OpenRouter."
---

# Prog Expert: AI Agent Integration (omp + Nuxt 4)

## Goal
Expert-level implementation guide for integrating omp (pi-coding-agent) RPC sidecars into Nuxt 4 / Nitro web applications. Covers both the naive raw-event pattern (what NOT to do) and the clean Typed Action Protocol (correct pattern), plus API key resolution and model selection.

## Core Workflow (Progressive Disclosure)
1. **Context Analysis**: Is this a new integration, refactoring existing code, or debugging?
2. **Knowledge Retrieval**:
   - Read `references/patterns.md` for the full architecture and protocol specification.
   - Check `recipes/` for implementation recipes (adapter, endpoints, composable, components).
   - Read `references/versions.json` for current library versions.
3. **Implementation**: Use the recipes as templates. Always validate against the Typed Action Protocol spec.
4. **Learning**: Extract new patterns via `programming/prog-expert-advisor/scripts/learn.py`.

## Instructions
- ALWAYS read `references/patterns.md` before starting any integration task.
- NEVER use raw event forwarding to frontend — use the Typed Action Protocol (Skill Adapter pattern).
- NEVER dispatch skills via string matching (`/Run skill\s+(\S+)/i`) — use `POST /api/agent/dispatch`.
- NEVER hardcode input form fields in the frontend — derive them from `pipeline.json` `user_inputs.dialog`.
- ALWAYS use `provider/model` format for model arg (e.g. `anthropic/claude-sonnet-4-6`), NEVER `p-provider/model`.
- ALWAYS await the `ready` event before sending commands via RPC.
- API keys: env var takes priority over stored settings. Key format: `{PROVIDER_UPPER}_API_KEY`.
- Model config: stored in `omp-agent/config/settings.json` as `{ defaultProvider, defaultModel }`. Combined as `${defaultProvider}/${defaultModel}` for the `--model` flag.
- When no frontend is connected (`hasConsumer() === false`): skip structured input dialogs, let LLM ask naturally.

## Self-Learning & Research
- Research new patterns via `scripts/research_knowledge.py`.
- Track library versions via `scripts/track_versions.py`.
- Learn from real implementations via `programming/prog-expert-advisor/scripts/learn.py`.

## Auto-Improvement
- After each use, analyze the session for improvements to this skill.
- Ask the user if those changes should be made.
- Store approved ideas in `resources/improvement_ideas.md`.

## References
- [Patterns](references/patterns.md) — Full architecture: naive vs typed protocol, all type definitions, state machine, API key resolution.
- [Versions](references/versions.json) — Tracked library versions.
- [Recipes](recipes/README.md) — Implementation recipes index.

## Recipes
Load via `view_file` as needed (Progressive Disclosure):
- **Skill Adapter**: `recipes/recipe-skill-adapter.md` — `skill-adapter.ts` implementation
- **API Endpoints**: `recipes/recipe-dispatch-endpoint.md` — dispatch, respond, actions SSE
- **Frontend Composable**: `recipes/recipe-frontend-composable.md` — `useSkillActions` composable
- **UI Components**: `recipes/recipe-action-components.md` — SkillInputDialog, SkillApprovalDialog, SkillStateIndicator, SkillAiDrawer

## Constraints
- Do not maintain backward compatibility with raw event bridge if you own all consumers — remove it cleanly.
- Never hardcode API keys — always use environment variables or `settings.json`.
- When spawning omp, always handle `exit` event to avoid orphan processes.
- Never overwrite user files without explicit confirmation.

## Script Integration
- **Research**: `uv run scripts/research_knowledge.py "<query>"`
- **Version Tracking**: `uv run scripts/track_versions.py`
- **Recipe Management**: `uv run scripts/manage_recipes.py <action> [args]`
- **Pattern Learning**: `uv run programming/prog-expert-advisor/scripts/learn.py <path/to/file>`
