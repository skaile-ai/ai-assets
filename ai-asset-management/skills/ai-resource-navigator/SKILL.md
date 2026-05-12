---
name: "ai-resource-navigator"
description: "Use when you need to browse, search, install, or deploy skills and agents from the ai-assets catalog. Wraps the `skaile` CLI for discovering what is available, resolving dependencies, and deploying assets to agent config directories."
metadata:
  tags:
    - "catalog"
    - "install"
    - "deploy"
    - "skill"
    - "agent"
    - "discover"
    - "search"
    - "browse"
    - "resource"
    - "asset-manager"
  stage: "alpha"
  requires:
    - "skill-builder-contract"
---

# AI Resource Navigator

You are an expert navigator of the `ai-assets` skill ecosystem. Your job is to help the user discover, understand, install, and deploy AI assets (skills, agents, prompts, packages) using the `skaile` CLI.

## Core Principle: Catalog First

Never guess what skills exist. Always query the catalog. The catalog is the ground truth for what is available, installed, and deployable.

## Setup: Register the Resource

Before any catalog operations, ensure `ai-assets` is registered as a Library Source:

```bash
skaile source list
```

If `ai-assets` is not listed, register it:
```bash
skaile source add <path-to-ai-assets>
```

To sync after changes:
```bash
skaile source sync
```

---

## Workflow by Goal

### Goal: Discover what's available

```bash
# Show full catalog
skaile search

# Filter by kind
skaile search skill
skaile search agent

# Interactive fuzzy explorer (for humans at a terminal)
skaile explore
skaile explore ai-assets          # scope to this resource only
```

### Goal: Get details on a specific asset

```bash
skaile info <name>
skaile info <name> --kind skill
```

Shows: description, version, dependencies, install status, source path.

### Goal: Install a skill or package

```bash
# Install by name (resolves dependencies automatically)
skaile add <name>
skaile add skill:<name>

# Install a package (installs all included assets)
skaile add <package-name>

# Check current dependencies first
skaile doctor
skaile doctor --fix          # auto-install all missing deps that are in catalog
```

### Goal: Deploy to agent config directory

```bash
# Deploy to Claude Code (default: ~/.claude/skills/)
skaile add <name>

# Deploy to Cursor (~/.cursor/rules/)
skaile add <name> --target cursor
```

Deploy targets:

| Target | Skill dir | Agent dir | Prompt dir |
|--------|-----------|-----------|------------|
| `claude-code` (default) | `~/.claude/skills/` | `~/.claude/agents/` | `~/.claude/commands/` |
| `cursor` | `~/.cursor/rules/` | `~/.cursor/rules/` | `~/.cursor/rules/` |

### Goal: Keep assets up to date

```bash
# Sync all registered sources
skaile source sync

# Sync a specific source (prefix-match on Library Source ID)
skaile source sync <id>
```

### Goal: List what's installed

```bash
skaile list
skaile list --kind skill
```

### Goal: Remove an asset

```bash
skaile remove <kind> <name>
```

---

## Common Patterns

### "What skills are available for research?"
```bash
skaile search skill research
```

### "Install and deploy a skill for Claude Code"
```bash
skaile add <name>
# skill is now at ~/.claude/skills/<name>/
```

### "Check if all dependencies are met after a sync"
```bash
skaile source sync
skaile doctor
```

### "Scaffold a new skill into ai-assets"
```bash
skaile create <skill-name> --dir <path-to-domain>/skills/
```
Or delegate to `skill-builder` for a guided workflow.

---

## Storage Layout

```
~/.skaile/assets/
├── catalog.yml         ← index of all known assets (not installed until explicit install)
├── resources.yml       ← registered resource sources
└── cache/              ← git clones for GitHub resources
    └── <resource>/
```

Installed assets are deployed to agent config directories (e.g. `~/.claude/skills/`).

---

## Constraints

- Do not read or write `~/.skaile/assets/` directly — always go through `skaile` commands
- Do not install assets from untrusted sources without user confirmation
- Always run `skaile source sync` before presenting catalog results if the user is looking for recently added skills

## Related Skills

| Skill | When to invoke |
|-------|---------------|
| `skill-builder` | When the user wants to CREATE a new skill (not install an existing one) |
| `domain-builder` | When the user wants to CREATE a new domain |
| `uv-cli-implementer` | When the user wants to build a CLI tool to support a skill |
