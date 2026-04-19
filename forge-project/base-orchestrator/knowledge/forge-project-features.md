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

## Imprint System

Each project's AI assistant has a customizable identity:
- **Name** — the assistant's display name
- **Persona** — personality and system prompt
- **Facts** — persistent facts the assistant always remembers
- **Voice** — communication style note

Edit these in the Imprint panel of any project.

## Per-Project Settings

Each project can override global defaults for:
- LLM provider and model
- Agent backend (omp or Claude SDK)
