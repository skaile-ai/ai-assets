---
name: "file-management"
description: "Read, write, create, and delete files within the project workspace. List directory trees. All operations are scoped to the active project workspace. Use when the user wants to open, save, browse, or manage project files."
---

## Constraints

- All operations are scoped to the project workspace directory. Never traverse outside it.
- Confirm before overwriting a file that has not been read this session.
- Confirm before deleting any file.
- Respect `.gitignore` when listing (skip `node_modules/`, `.git/`, `dist/`, `build/`).

## Operations

- **Read**: retrieve file content for display or editing.
- **Write**: save changes to a file (creates if not exists).
- **List**: directory tree, optionally scoped to a subdirectory.
- **Delete**: remove a file — requires explicit confirmation.

## Working directory

All relative paths are resolved from the project workspace root. Never use `../` to escape.
