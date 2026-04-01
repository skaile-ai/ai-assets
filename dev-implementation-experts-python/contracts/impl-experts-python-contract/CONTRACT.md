---
name: "impl-experts-python-contract"
description: "Shared contract for all Python implementation expert skills. Describes the expert skill folder structure, discovery conventions, and how Python experts are consumed by implementation skills."
metadata:
  stage: "alpha"
  do_not_invoke: true
---

# Python Implementation Experts — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `dev-implementation-experts-python` skills read this before operating.

## Scope

Python expert skills are **passive reference libraries** loaded into implementation subagent contexts when a Python technology is detected in the project stack. They follow the same structural conventions as JS experts.

## Expert Skill Folder Structure

```
skaileup-implementation-expert-<tech>/
├── SKILL.md                      ← Required. Agent prompt + frontmatter
├── recipes/                      ← Reusable patterns (markdown)
├── atomic-examples/              ← Minimal focused code snippets (.py)
├── reference-implementations/    ← Complete working implementations
├── assets/                       ← Static assets
├── examples/                     ← Larger worked examples
├── references/                   ← Curated docs / API references
├── resources/                    ← Supporting materials
└── scripts/                      ← Utility scripts
```

## SKILL.md Frontmatter

```yaml
---
name: skaileup-implementation-expert-<tech>
source: MIGRATED
description: "Expert guidance for <tech>. Loaded automatically when <tech> is in the project stack."
metadata:
  type: expert
  technology: <tech>
  language: python
  discovery_keywords: [<tech>, <alias>]
stage: alpha | beta | production
---
```

## Covered Technologies

| Expert | Technology |
|--------|-----------|
| `python` | Core Python patterns (async, typing, project structure) |
| `python-pydanticai` | PydanticAI agent framework |
| `marimo` | Marimo reactive notebook / app framework |

## Python-Specific Conventions

- **Package structure**: `pyproject.toml`-based, `uv` preferred as package manager
- **Type annotations**: Required for all public interfaces
- **Async**: `asyncio` / `anyio` patterns preferred over threading
- **Environment**: `.env` via `python-dotenv` or Pydantic `BaseSettings`
- **Tests**: `pytest` + `pytest-asyncio` for async code
