---
name: dev-implementation-experts-python
description: "Deep implementation expertise for Python frameworks and libraries — each skill is a specialist consultant for one technology."
type: domain
building_blocks:
  contracts: "Expert skill conventions: recipe format, atomic examples, reference implementations; same protocol as dev-implementation-experts-js."
  docs: "Python version requirements, library compatibility notes, and ecosystem integration guidance."
  skills: "One expert skill per framework/library: python (core patterns), pydantic-ai (AI agent framework), marimo (reactive notebooks)."
  agents: "TBD"
  prompts: "TBD"
  tools: "TBD"
stage: alpha
---

# Dev Implementation Experts — Python

This domain provides deep, focused implementation expertise for Python frameworks and libraries. Each skill is a specialist: it knows idiomatic patterns, packaging conventions (uv, PEP 723), and integration recipes for exactly one technology.

The same routing protocol applies as in `dev-implementation-experts-js`: feature-implementation skills delegate to these experts when Python-stack-specific guidance is needed.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Expert skill conventions — recipe format, atomic example structure, reference implementation layout |
| `docs/` | Python version matrices, uv/packaging notes, cross-library integration patterns |
| `skills/` | One expert skill per technology |

## Skills

| Skill | Technology |
|-------|-----------|
| `dev-implementation-expert-python` | Core Python — idiomatic patterns, packaging with uv, type hints, testing |
| `dev-implementation-expert-python-pydanticai` | Pydantic AI — AI agent framework, tool use, structured outputs |
| `dev-implementation-expert-marimo` | Marimo — reactive Python notebooks, UI components, deployment |

## Conventions

- Each expert skill includes `recipes/`, `atomic-examples/`, `reference-implementations/`, and `assets/`
- Python ≥ 3.12 assumed unless a skill explicitly documents older version support
- Scripts use PEP 723 inline deps (`# /// script`) for zero-setup execution with `uv run`
- Expert skills are never the primary entry point — they are delegated to by orchestrator or feature-implementation skills
