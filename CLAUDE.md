# Skaile AI Assets

## What This Is

AI agent skills, domains, and shared resources for the Skaile ecosystem. Pure content — no code dependencies.

**GitHub:** `github.com/skaile-ai/skaile-ai-assets`

> **Skaileup skills** (concept, build, quality pipelines) have been extracted to the separate `ai-assets-skaileup/` submodule (`github.com/skaile-ai/ai-assets-skaileup`).

## Structure

Skills are organized into domains. Each domain has a `DOMAIN.md` and contains skills, contracts, and docs.

```
ai-asset-management/              ← meta: skill/domain scaffolding, catalog navigation
skaile-development/               ← skaile-dev monorepo workflow skills (git, test, audit, implement, etc.)
skaile-platform/                  ← skaile platform-specific skills
forge-project/                    ← forge app project skills
dev-implementation-experts-js/    ← JS/TS framework experts
dev-implementation-experts-python/ ← Python experts
dev-implementation-experts-typst/  ← Typst expert
external/                         ← tracked external resources
knowledge-research/               ← deep research, paper extraction
knowledge-writing/                ← book/podcast generation
use/                              ← external service integrations (exa, perplexity, outline, etc.)
```

## Skill Structure

Every skill lives in its own directory:

```
my-skill/
├── SKILL.md        ← YAML frontmatter + markdown body (the agent prompt)
├── CLI.md          ← Optional: CLI invocation docs
├── references/     ← Optional: reference material
└── validator.py    ← Optional: output validation script
```

## How This Is Consumed

The skaile CLI and platform consume these at **runtime** by reading SKILL.md files from disk. Configure the path via `SKAILE_RESOURCES_PATH` env var or `skaile.yaml`.
