---
name: "prog-expert-python"
description: "Use when you need to implement Python 3.12+ patterns, uv workspace monorepo management, async Python, decorator patterns, pytest/mock testing, ruff/mypy configuration, Rich CLI output, pydantic-settings config, and Protocol-based interface design. Expert-level programming and pattern management."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "impl-experts-python-contract"
---

# Prog Expert Python

## Goal
Expert-level Python 3.12+ development. Handles async patterns, uv monorepo management, testing strategies, type safety with mypy, config management with pydantic-settings, and CLI tooling with Rich. Maintains reusable recipes and atomic examples for this ecosystem.

## Core Workflow (Progressive Disclosure)
1. **Context Analysis**: Read relevant source files before suggesting changes.
2. **Knowledge Retrieval**:
   - Check `recipes/` for matching patterns (async, testing, config, CLI, decorators).
   - Check `atomic-examples/` for small, focused code snippets.
   - Fetch latest versions: `uv run scripts/track_versions.py`
   - Research new patterns: `uv run scripts/research_knowledge.py "<query>"`
3. **Implementation**: Apply patterns from recipes. Follow constraints below.
4. **Learning**: After successful implementations, extract reusable patterns via `uv run programming/prog-expert-advisor/scripts/learn.py <path>`.

## Key Domains

### uv Workspace Monorepo
- Root `pyproject.toml` uses `[tool.uv.workspace]` with `members = [...]`
- Local package sources via `[tool.uv.sources]` with `{ workspace = true }`
- Commands: `uv sync`, `uv run <script>`, `uv build`, `uv add --dev`
- Per-package `pyproject.toml` with `[build-system] requires = ["uv_build>=..."]`
- Read recipe: `view_file ~/.gemini/workspace/skills/prog-expert-python/recipes/uv-workspace.md`

### Python 3.12+ Type Patterns
- `Protocol` for structural subtyping (duck typing without inheritance)
- `TypeVar`, `Generic[T]` for generic classes and functions
- `AsyncGenerator[YieldType, SendType]` for streaming return types
- Read recipe: `view_file ~/.gemini/workspace/skills/prog-expert-python/recipes/type-patterns.md`

### Async Python
- `async def`, `await`, `async for`, `async with`
- `AsyncGenerator` with `yield` inside `async def`
- Async context managers via `__aenter__`/`__aexit__`
- `asyncio.new_event_loop()` for sync wrappers in tests
- Read recipe: `view_file ~/.gemini/workspace/skills/prog-expert-python/recipes/async-patterns.md`

### Decorator Patterns
- Sync decorator wrapping sync functions with `functools.wraps`
- Async-aware: detect `asyncio.iscoroutinefunction()` to wrap both sync and async
- Retry/backoff with configurable `max_retries` and exponential delay
- Read atomic example: `view_file ~/.gemini/workspace/skills/prog-expert-python/atomic-examples/retry-decorator.py`

### pydantic-settings + Config
- `BaseSettings` with `model_config = SettingsConfigDict(env_file=".env")`
- YAML config loading with `pyyaml` + custom `BaseModel`
- `python-dotenv` for `.env` file loading
- Nested provider config: `Dict[str, ProviderSettings]` pattern
- Read recipe: `view_file ~/.gemini/workspace/skills/prog-expert-python/recipes/config-management.md`

### pytest + unittest.mock Testing
- `unittest.mock.patch`, `MagicMock`, `AsyncMock`
- Mocking external modules at import time: `sys.modules["openai"] = MagicMock()`
- `@patch` decorator vs `patch()` context manager
- Async test helper: wrap coroutines with `asyncio.new_event_loop().run_until_complete()`
- `pytest-cov` for coverage; configure `testpaths` and `pythonpath` in `pyproject.toml`
- Read recipe: `view_file ~/.gemini/workspace/skills/prog-expert-python/recipes/testing-patterns.md`

### Rich CLI Output
- `Console`, `console.print()` with markup (`[bold cyan]...[/bold cyan]`)
- `Panel`, `Table`, `Progress` for structured terminal output
- Read atomic example: `view_file ~/.gemini/workspace/skills/prog-expert-python/atomic-examples/rich-cli.py`

### ruff + mypy
- `ruff check . && ruff format .` — prefer over flake8/black
- `[tool.mypy]` in `pyproject.toml`: `strict = true`, `ignore_missing_imports = true`
- Common ignores: `# type: ignore[override]`, `# type: ignore[arg-type]`

## Instructions
- ALWAYS use `uv` as the package manager — never `pip` directly.
- Before implementing, check `recipes/` for the relevant pattern.
- Prefer `Protocol` over `ABC` for interface definitions (structural subtyping).
- For async streaming, use `AsyncGenerator[str, None]` return type consistently.
- When writing tests, mock heavy dependencies at `sys.modules` level before imports.
- After successful implementation, extract patterns to `recipes/` or `atomic-examples/`.

## Self-Learning & Research
- Gathers knowledge via web research and `use-context7-api` skill.
- Learn from successful implementations in this codebase and others.
- Track latest versions: pydantic, pydantic-settings, rich, pytest, ruff, mypy, uv.

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
  - `view_file ~/.gemini/workspace/skills/prog-expert-python/atomic-examples/retry-decorator.py`
  - `view_file ~/.gemini/workspace/skills/prog-expert-python/atomic-examples/rich-cli.py`
  - `view_file ~/.gemini/workspace/skills/prog-expert-python/atomic-examples/protocol-interface.py`
- **Recipes**:
  - `view_file ~/.gemini/workspace/skills/prog-expert-python/recipes/uv-workspace.md`
  - `view_file ~/.gemini/workspace/skills/prog-expert-python/recipes/async-patterns.md`
  - `view_file ~/.gemini/workspace/skills/prog-expert-python/recipes/config-management.md`
  - `view_file ~/.gemini/workspace/skills/prog-expert-python/recipes/testing-patterns.md`
  - `view_file ~/.gemini/workspace/skills/prog-expert-python/recipes/type-patterns.md`

## Constraints
- Do not perform unauthorized or destructive actions.
- Do not overwrite existing files without explicit user confirmation.

## Script Integration
- **Research**: `uv run scripts/research_knowledge.py "<query>"`
- **Version Tracking**: `uv run scripts/track_versions.py`
- **Recipe Management**: `uv run scripts/manage_recipes.py <action> [args]`
- **Pattern Learning**: `uv run programming/prog-expert-advisor/scripts/learn.py <path/to/file>`
