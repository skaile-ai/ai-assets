---
name: dev-implementation-experts-python
description: "Deep implementation expertise for Python frameworks and libraries — each `prog-expert-*` skill is a specialist consultant for one technology; `uv-cli-implementer` is a standalone builder skill."
type: domain
building_blocks:
  contracts: "Expert skill conventions: recipe format, atomic examples, reference implementations; same protocol as dev-implementation-experts-js."
  skills: "One expert skill per framework/library: python (core patterns), pydantic-ai (AI agent framework), marimo (reactive notebooks), plus uv-cli-implementer (agent-friendly uv/Typer CLIs)."
  agents: "TBD"
  prompts: "TBD"
  tools: "TBD"
stage: alpha
---

# Dev Implementation Experts — Python

This domain provides deep, focused implementation expertise for Python frameworks and libraries. Each `prog-expert-*` skill is a specialist: it knows idiomatic patterns, packaging conventions (uv, PEP 723), and integration recipes for exactly one technology.

The same routing protocol applies as in `dev-implementation-experts-js`: feature-implementation skills delegate to these experts when Python-stack-specific guidance is needed (`uv-cli-implementer` is not an expert consultant; it is invoked directly).

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Expert skill conventions — recipe format, atomic example structure, reference implementation layout |
| `skills/` | One expert skill per technology |

## Skills

| Skill | Technology |
|-------|-----------|
| `prog-expert-python` | Core Python — idiomatic patterns, packaging with uv, type hints, testing |
| `prog-expert-python-pydanticai` | Pydantic AI — AI agent framework, tool use, structured outputs |
| `prog-expert-marimo` | Marimo — reactive Python notebooks, UI components, deployment |
| `uv-cli-implementer` | uv + Typer — single-file, agent-friendly CLI tools (PEP 723) |

## Conventions

- Each `prog-expert-*` skill includes `recipes/`, `atomic-examples/`, `reference-implementations/`, and `assets/`; `uv-cli-implementer` ships `CLI.md`, `examples/` and `resources/` instead
- Python ≥ 3.12 assumed unless a skill explicitly documents older version support
- Scripts use PEP 723 inline deps (`# /// script`) for zero-setup execution with `uv run`
- `prog-expert-*` skills are never the primary entry point — they are delegated to by orchestrator or feature-implementation skills; `uv-cli-implementer` is a primary entry point, invoked directly
