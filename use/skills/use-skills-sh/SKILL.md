---
name: use-skills-sh
description: "Use when discovering, researching, or installing agent skills from the open skills.sh ecosystem. Handles discovery via npx skills find, quality verification, and workspace integration via skaile repo add / skaile skill add."
metadata:
  stage: alpha
  source: MIGRATED
  requires:
    - use-contract
version: 1.0.0
---

# use-skills-sh — Skills Discovery & Workspace Integration

Bridge between the [skills.sh](https://skills.sh) open agent skill ecosystem and the skaile workspace. Finds skills, verifies quality, and installs them into the current workspace using native skaile tooling.

## When to Use This Skill

Activate when the user:

- Asks "find a skill for X", "is there a skill that…", "how do I do X with an agent skill"
- Says "research skills", "look up skills", "what skills are available for…"
- Wants to install or add a skill: "install skill X", "add skill Y to my workspace"
- Expresses interest in extending capabilities: "I need something that can…", "can Claude…"

## Workflow

### Step 1 — Understand the Need

Before searching, identify:
- **Domain**: what category of work (web dev, testing, DevOps, writing, data, etc.)
- **Task**: what specific capability the user is trying to add
- **Context**: does the workspace already have something close? (`skaile search <keyword>`)

### Step 2 — Check the Leaderboard

Browse https://skills.sh for established, high-quality solutions before running a CLI search. The leaderboard surfaces the most trusted and widely-used skills by category.

### Step 3 — Search via CLI

```bash
# Search the open ecosystem
npx skills find "<query>"

# Examples
npx skills find "database migration"
npx skills find "react component testing"
npx skills find "markdown to pdf"
```

### Step 4 — Verify Quality

**Do not recommend a skill based solely on search results.** Always verify:

| Signal | Threshold |
|--------|-----------|
| Install count | ≥ 1 000 installs preferred |
| Source reputation | Vercel Labs, Anthropic, Microsoft, or well-known open-source maintainers |
| GitHub stars | Higher is better; check recency of activity |
| Last updated | Stale skills (> 1 year, no activity) should be flagged |

### Step 5 — Present Options

Present 2–3 curated options with:
- Skill name and source
- What it does in one sentence
- Install count / reputation signal
- The skaile commands needed to install it (see Step 6)

### Step 6 — Install into the Skaile Workspace

Once the user selects a skill, install it in two steps:

```bash
# 1. Register the skill's source repository
skaile repo add <github-url> <alias>

# 2. Deploy the skill into the active workspace
skaile skill add <skill-name>
```

**Example — installing a "markdown-to-pdf" skill:**

```bash
# Register the repo
skaile repo add https://github.com/example-org/pdf-skills pdf-skills

# Install the specific skill
skaile skill add markdown-to-pdf
```

**Check what is now available:**

```bash
skaile skill list
```

**Keep skills up to date:**

```bash
# Check for updates across registered repos
skaile repo status <alias>

# Pull latest from a repo
skaile repo sync <alias>
```

## Fallback Strategy

If no relevant skill exists in the ecosystem:

1. **Acknowledge** — confirm the search returned no strong matches
2. **Offer direct assistance** — implement the capability inline for this session
3. **Suggest creation** — guide the user to scaffold a custom skill:

```bash
# Initialise a new custom skill in the current workspace
skaile skill create <skill-name>
```

Custom skills follow the standard `SKILL.md` + frontmatter convention. See `ai-assets/ai-asset-management/skills/skill-builder/` for the guided scaffolding workflow.

## Quality Standards

- Never recommend a skill from a single search hit alone — cross-check with the leaderboard
- Prefer skills from Vercel Labs (`@vercel-labs`), Anthropic, or Microsoft namespaces as first choices
- Flag skills with < 100 installs or no GitHub presence as experimental
- If multiple skills cover the same need, recommend the one with the larger install base and more recent activity

## Common Categories

| Category | Example queries |
|----------|----------------|
| Web development | "nextjs page generation", "tailwind component", "api route scaffolding" |
| Testing | "vitest setup", "playwright e2e", "snapshot testing" |
| DevOps | "docker compose", "github actions", "deployment checklist" |
| Documentation | "openapi docs", "readme generator", "changelog" |
| Code quality | "eslint config", "type coverage", "code review" |
| Data | "csv transform", "sql migration", "schema validation" |
| AI / LLM | "prompt engineering", "rag pipeline", "embeddings" |
