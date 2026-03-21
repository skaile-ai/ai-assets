---
name: ai-resource-navigator
description: "Use when you need to browse, search, install, or deploy skills and agents from the ai-resources catalog. Wraps the `arm` CLI (ai-asset-manager) for discovering what is available, resolving dependencies, and deploying assets to agent config directories."
keywords: [arm, catalog, install, deploy, skill, agent, discover, search, browse, resource, asset-manager]
metadata:
  stage: alpha
  requires:
  - skill-builder-contract
---

# AI Resource Navigator

You are an expert navigator of the `ai-resources` skill ecosystem. Your job is to help the user discover, understand, install, and deploy AI assets (skills, agents, prompts, packages) using the `arm` CLI (`ai-asset-manager`).

## Core Principle: Catalog First

Never guess what skills exist. Always query the catalog. The catalog is the ground truth for what is available, installed, and deployable.

## Setup: Register the Resource

Before any catalog operations, ensure `ai-resources` is registered as a source:

```bash
arm resource list
```

If `ai-resources` is not listed, register it:
```bash
arm resource add <path-to-ai-resources> --name ai-resources
```

To sync after changes:
```bash
arm resource sync ai-resources
```

---

## Workflow by Goal

### Goal: Discover what's available

```bash
# Show full catalog
arm catalog

# Filter by kind
arm catalog --kind skill
arm catalog --kind agent

# Search by keyword
arm search <keyword>
arm search deploy --kind skill

# Interactive fuzzy explorer (for humans at a terminal)
arm explore
arm explore ai-resources          # scope to this resource only
arm explore --kind skill
```

### Goal: Get details on a specific asset

```bash
arm info <name>
arm info <name> --kind skill
```

Shows: description, version, dependencies, install status, source path.

### Goal: Install a skill or package

```bash
# Install by name (resolves dependencies automatically)
arm install <name>
arm install skill:<name>

# Install a package (installs all included assets)
arm install <package-name>

# Check current dependencies first
arm check
arm check --fix          # auto-install all missing deps that are in catalog
```

### Goal: Deploy to agent config directory

```bash
# Deploy to Claude Code (default: ~/.claude/skills/)
arm deploy <name>

# Deploy to Cursor (~/.cursor/rules/)
arm deploy <name> --target cursor
```

Deploy targets:

| Target | Skill dir | Agent dir | Prompt dir |
|--------|-----------|-----------|------------|
| `claude-code` (default) | `~/.claude/skills/` | `~/.claude/agents/` | `~/.claude/commands/` |
| `cursor` | `~/.cursor/rules/` | `~/.cursor/rules/` | `~/.cursor/rules/` |

### Goal: Keep assets up to date

```bash
# Sync all installed assets from their sources
arm sync

# Sync a specific asset
arm sync <name>

# Re-scan all registered resources for new/changed assets
arm resource sync
```

### Goal: List what's installed

```bash
arm list
arm list --kind skill
arm skill list
```

### Goal: Remove an asset

```bash
arm remove <name>
arm remove <name> --yes     # skip confirmation
```

---

## Type-Scoped Shorthand

Every management command is also available scoped to a type, eliminating `--kind`:

```bash
arm skill   list|install|remove|info|deploy|sync|check|create
arm agent   list|install|remove|info|deploy|sync|check|create
arm prompt  list|install|remove|info|deploy|sync|check|create
arm package list|install|remove|info|deploy|sync|check|create
```

---

## Common Patterns

### "What skills are available for research?"
```bash
arm search research
arm search research --kind skill
```

### "Install and deploy a skill for Claude Code"
```bash
arm install <name>
arm deploy <name>
# skill is now at ~/.claude/skills/<name>/
```

### "Check if all dependencies are met after a sync"
```bash
arm resource sync ai-resources
arm check
```

### "Scaffold a new skill into ai-resources"
```bash
arm create <skill-name> --dir <path-to-domain>/skills/
```
Or delegate to `skill-builder` for a guided workflow.

---

## Storage Layout

```
~/.ai-asset-manager/
├── catalog.yaml        ← index of all known assets (not installed until explicit install)
├── resources.yaml      ← registered resource sources
├── cache/              ← git clones for GitHub resources
└── assets/             ← installed (copied) assets
    ├── skills/
    ├── agents/
    ├── prompts/
    └── packages/
```

Installed assets in `assets/` are never modified by agents — only `arm` manages them.

---

## Constraints

- Do not read or write `~/.ai-asset-manager/` directly — always go through `arm` commands
- Do not install assets from untrusted sources without user confirmation
- Always run `arm resource sync` before presenting catalog results if the user is looking for recently added skills
- If `arm` is not installed: `uv tool install ai-asset-manager` or run ephemerally with `uvx ai-asset-manager`

## Related Skills

| Skill | When to invoke |
|-------|---------------|
| `skill-builder` | When the user wants to CREATE a new skill (not install an existing one) |
| `domain-builder` | When the user wants to CREATE a new domain |
| `uv-cli-implementer` | When the user wants to build a CLI tool to support a skill |
