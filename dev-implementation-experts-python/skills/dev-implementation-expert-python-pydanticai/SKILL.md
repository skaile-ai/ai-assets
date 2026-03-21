---
name: prog-expert-python-pydanticai
source: MIGRATED
description: Use when you need to implement pydantic-ai Agent API patterns including structured outputs with result_type, message history, OpenAI/Gemini model backends, async streaming with run_stream(), and multi-provider integration. Expert-level programming and pattern management.
keywords: []
reads_from: []
writes_to: []
metadata:
  stage: alpha
  requires:
  - impl-experts-python-contract
---

# Prog Expert Python Pydanticai

## Goal
Expert-level pydantic-ai development. Handles Agent setup, structured output generation, conversation history management, async streaming, and multi-provider backend configuration (OpenAI, Gemini, LiteLLM-compatible). Maintains reusable recipes and atomic examples.

## Core Workflow (Progressive Disclosure)
1. **Context Analysis**: Read the client file and interface before suggesting changes.
2. **Knowledge Retrieval**:
   - Check `recipes/` for matching patterns (streaming, structured output, history).
   - Check `atomic-examples/` for focused API snippets.
   - Fetch latest version: `uv run scripts/track_versions.py`
   - Research new patterns: `uv run scripts/research_knowledge.py "<query>"`
3. **Implementation**: Apply patterns from recipes. Follow constraints below.
4. **Learning**: Extract patterns via `uv run programming/prog-expert-advisor/scripts/learn.py <path>`.

## Key Domains

### Agent Initialization
- `Agent(model)` — basic agent, no result type constraint
- `Agent(model, result_type=MyPydanticModel)` — structured output agent
- Model backends: `OpenAIModel("gpt-4o")`, `GeminiModel("gemini-2.0-flash")`
- OpenAI-compatible endpoints: set `OPENAI_BASE_URL` + `OPENAI_API_KEY` env vars before constructing `OpenAIModel`
- Read atomic example: `view_file ~/.gemini/workspace/skills/prog-expert-python-pydanticai/atomic-examples/agent-init.py`

### Structured Outputs
- `result = agent.run_sync(prompt)` → `result.data` is an instance of `result_type`
- For one-off structured calls, create a temporary agent with `result_type` set
- Dynamic model creation: `type("DynamicModel", (BaseModel,), {field: (type_, ...) for ...})`
- Read recipe: `view_file ~/.gemini/workspace/skills/prog-expert-python-pydanticai/recipes/structured-output.md`

### Async Streaming
- `async with agent.run_stream(message, message_history=history) as result:`
- `async for chunk in result.stream_text():` — yields string chunks
- Return type annotation: `AsyncGenerator[str, None]`
- Always use as async context manager (`async with`) — do not call `.run_stream()` directly
- Read recipe: `view_file ~/.gemini/workspace/skills/prog-expert-python-pydanticai/recipes/async-streaming.md`

### Message History
- Types: `ModelMessage`, `ModelRequest`, `ModelResponse`
- User message: `ModelRequest(parts=[UserPromptPart(content="...")])`
- Assistant message: `ModelResponse(parts=[TextPart(content="...")])`
- Pass as `message_history=[...]` to `run_stream()` or `run_sync()`
- Important: exclude the current user message from history if it's already the `message` argument
- Read atomic example: `view_file ~/.gemini/workspace/skills/prog-expert-python-pydanticai/atomic-examples/message-history.py`

### Multi-Provider Setup
- Gemini: set `GEMINI_API_KEY` env var + `genai.configure(api_key=...)`, use `GeminiModel`
- OpenAI: set `OPENAI_API_KEY` + optional `OPENAI_BASE_URL` for proxies/LiteLLM
- Provider detection pattern: `if config.provider == "gemini": ... else: OpenAIModel(...)`
- Read recipe: `view_file ~/.gemini/workspace/skills/prog-expert-python-pydanticai/recipes/multi-provider.md`

### MaiClientProtocol Compliance
- All clients must implement: `async def stream_chat(self, message: str, history: List[MaiResponse]) -> AsyncGenerator[str, None]`
- History uses `MaiResponse` (from mai-core), not pydantic-ai native types — convert on the way in
- `provider == "user"` marks user messages in history; anything else is assistant

## Instructions
- ALWAYS use `uv` as the package manager.
- Before implementing, check `recipes/` for the relevant pattern.
- Never call `run_stream()` outside of `async with` — it is an async context manager.
- When reusing model from an existing agent in a temporary agent, access via `self.agent.model`.
- Handle Gemini separately from OpenAI-compatible providers; they use different env vars and model classes.
- After successful implementation, extract patterns to `recipes/` or `atomic-examples/`.

## Self-Learning & Research
- Gathers knowledge via web research and `use-context7-api` skill.
- Track latest pydantic-ai version and API changes (library evolves rapidly).
- Learn from message history conversion patterns and streaming edge cases.

## Auto-Improvement
- After each use, analyze the chat for patterns not yet in `recipes/` or `atomic-examples/`.
- Ask the user if improvements should be saved.
- If approved, store ideas in `resources/improvement_ideas.md`.

## References
- [Patterns](references/patterns.md) — Reusable code patterns and best practices.
- [Versions](references/versions.json) — Tracked library versions.
- [Recipes](recipes/README.md) — Coding recipes for recurring tasks.

## Example Code
Load these via `view_file` only when needed:

- **Atomic Examples**:
  - `view_file ~/.gemini/workspace/skills/prog-expert-python-pydanticai/atomic-examples/agent-init.py`
  - `view_file ~/.gemini/workspace/skills/prog-expert-python-pydanticai/atomic-examples/message-history.py`
- **Recipes**:
  - `view_file ~/.gemini/workspace/skills/prog-expert-python-pydanticai/recipes/structured-output.md`
  - `view_file ~/.gemini/workspace/skills/prog-expert-python-pydanticai/recipes/async-streaming.md`
  - `view_file ~/.gemini/workspace/skills/prog-expert-python-pydanticai/recipes/multi-provider.md`
- **Reference Implementations**:
  - `view_file ~/.gemini/workspace/skills/prog-expert-python-pydanticai/reference-implementations/pydantic-ai-client.py`

## Constraints
- Do not perform unauthorized or destructive actions.
- Do not overwrite existing files without explicit user confirmation.

## Script Integration
- **Research**: `uv run scripts/research_knowledge.py "<query>"`
- **Version Tracking**: `uv run scripts/track_versions.py`
- **Recipe Management**: `uv run scripts/manage_recipes.py <action> [args]`
- **Pattern Learning**: `uv run programming/prog-expert-advisor/scripts/learn.py <path/to/file>`
