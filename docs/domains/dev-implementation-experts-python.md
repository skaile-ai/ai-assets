---
title: dev-implementation-experts-python
description: Deep implementation expertise for Python frameworks and libraries — each skill is a specialist consultant for one technology.
---

Provides focused implementation expertise for Python frameworks and libraries. Each skill knows idiomatic patterns, packaging conventions (uv, PEP 723), and integration recipes for exactly one technology.

The same routing protocol applies as in `dev-implementation-experts-js`: feature-implementation skills delegate to these experts when Python-stack-specific guidance is needed.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | Expert skill conventions — recipe format, atomic example structure, reference implementation layout |
| `docs/` | Python version matrices, uv/packaging notes, cross-library integration patterns |
| `skills/` | One expert skill per technology |

## Skills

| Skill | Technology | When to use |
|---|---|---|
| `prog-expert-python` | Python (core) | Idiomatic Python patterns, uv packaging, PEP 723 inline deps, type annotations |
| `prog-expert-python-pydanticai` | Pydantic AI | AI agent framework — structured outputs, tool use, model-agnostic agent patterns |
| `prog-expert-marimo` | Marimo | Reactive notebook authoring, cell dependencies, UI widgets, data pipeline patterns |
