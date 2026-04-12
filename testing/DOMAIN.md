---
name: Testing
description: Deterministic test fixtures for the flow execution engine. Not for production use.
---

## Purpose

This domain contains test-only skills and flows used by unit, integration, and
E2E tests of the flow execution engine. The skills are deliberately trivial —
their instructions are imperative file operations that any competent LLM
follows identically. This makes assertions about state transitions, artifact
handoff, approval gates, and input gates reliable across drivers and model
versions.

Do NOT include this domain in user-facing catalog packages. It is intended to
be loaded only by test harnesses (e.g. the `testing.catalog.yaml` manifest is
registered as a separate CatalogSource row in the E2E test seed-data so the
default platform install never surfaces it).

## Skills

| Skill | Path | Purpose |
|-------|------|---------|
| test-ask-name | `skills/test-ask-name/` | Exercise the input gate by requesting a text input and writing it verbatim |
| test-write-greeting | `skills/test-write-greeting/` | Exercise artifact handoff by reading upstream output and writing a fixed-format file |
| test-count-artifacts | `skills/test-count-artifacts/` | Exercise the optional-skip path by counting files (marked optional in the flow) |

## Flows

| Flow | Path | Purpose |
|------|------|---------|
| test-echo | `flows/test-echo.flow.yaml` | 3-node linear flow chaining the three test skills. Covers input gate, approval gate, artifact handoff, optional skip, and completion. |

## Notes

- File paths in skill bodies are exact — tests assert exact content.
- Skills MUST NOT generate any chat text beyond tool calls. This keeps the
  chat stream deterministic for breadcrumb assertions.
- If you change a skill body, update the corresponding test expectations.
- All skills use `source: TEST` in their frontmatter to signal fixture status.
