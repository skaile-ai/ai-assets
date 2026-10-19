# Forge Project Features

## Project Workspaces

Each project is an isolated workspace — a named folder for documents, code, notes, and files. The AI assistant in each project has access only to that project's files.

## File Access

Workspaces are accessible three ways:
- **Browser UI** — the main interface, with file tree, editor, and chat
- **WebDAV network drive** — mount from any OS (Windows, macOS, Linux) as a network folder
- **SSH/SFTP** — direct terminal or file transfer access

## AI Agent

Each project has an AI assistant that can:
- Read and write files in the workspace
- Help with coding, writing, research, and file management
- Search the web (when configured with search tools)
- Browse websites (when configured with browser tools)

## Agent Backends

Two agent backends are available:
- **omp** (default) — model-agnostic, works with any LLM provider (OpenRouter, OpenAI, Anthropic, etc.)
- **Claude Agent SDK** — Anthropic models only, requires an Anthropic API key

## Assistant Identity and Memory

The assistant's identity comes from its agent definition (its `SOUL.md`). There is no per-project Imprint panel and no per-project system prompt to edit. What the assistant learns about you is kept in four memory blocks that are shared across your sessions:
- **user** — persistent facts about you: name, preferences, interests, timezone, work context
- **soul** — tone and personality notes you ask for ("be more concise", "call me Sam")
- **learned** — lessons about what worked or failed when helping you
- **self** — the assistant's own notes about how it works

You can review and correct memory entries in the memory review. The home assistant and the assistants inside a project keep the same four blocks.

## Per-Project Settings

Each project can override global defaults for:
- LLM provider and model
- Agent backend (omp or Claude SDK)
