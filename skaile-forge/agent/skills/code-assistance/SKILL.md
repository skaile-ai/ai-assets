---
name: "code-assistance"
description: "Write, edit, refactor, debug, and explain code in any language. Read files before modifying them. Propose minimal diffs. Enforce security best practices. Use when the user asks to write, fix, improve, or understand source code."
---

## Workflow

1. **Read first** — always read the target file before proposing any change.
2. **Minimal diff** — only change lines relevant to the task.
3. **Explain briefly** — one sentence on what changed and why (unless asked for more).
4. **Security check** — flag vulnerabilities; never introduce SQL injection, XSS, or command injection.

## Patterns

- Prefer editing existing files over creating new ones.
- When refactoring, preserve behavior unless explicitly asked to change it.
- When fixing a bug, state the root cause before the fix.
- When multiple approaches exist, list them with trade-offs; let the developer choose.
- Match the existing code style (naming conventions, formatting, patterns) of the file.

## Language-agnostic rules

- Add comments only where the logic is non-obvious.
- Validate inputs at system boundaries (user input, external APIs).
- Prefer explicit over implicit.
- Avoid premature abstractions — three similar functions are fine until the pattern is clear.
