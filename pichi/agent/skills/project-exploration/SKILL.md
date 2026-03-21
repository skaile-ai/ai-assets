---
name: project-exploration
description: >-
  Understand an unfamiliar codebase. Map directory structure, identify entry
  points, trace data flow, locate where a feature is implemented. Use when the
  user asks "how does X work", "where is Y defined", or "what does this project do".
---

## Approach

1. Start with the top-level directory listing to understand the project structure.
2. Read configuration files (`package.json`, `pyproject.toml`, `Cargo.toml`, `go.mod`, etc.) to understand the tech stack and entry points.
3. Look for a `README.md` or `CLAUDE.md` for project-specific context.
4. Read the most relevant source files for the user's question — don't read everything.
5. Summarize findings before going deep into any single file.

## Patterns

- Follow imports to trace where logic lives.
- Look at test files to understand expected behavior.
- Check `git log` (if available) for recent changes to a file.
- State your findings as facts with file references (e.g. `src/api/users.ts:42`).

## Output style

- Lead with the answer, then the supporting evidence.
- Include file paths and line numbers when referencing specific code.
- If you're uncertain, say so and offer to dig deeper.
