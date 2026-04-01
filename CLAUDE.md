# Skaile AI Assets

## What This Is

All AI skills, domains, contracts, and shared resources for the Skaile ecosystem. Pure content — no code dependencies.

**GitHub:** `github.com/skaile-ai/skaile-ai-assets`

## Structure

Skills are organized into domains. Each domain has a `DOMAIN.md` and contains skills, contracts, and docs.

```
ai-asset-management/   ← meta: skill/domain scaffolding, catalog navigation
skaileup-architecture/         ← system architecture, AI agent design
skaileup-conceptualization/    ← discovery → experience → blueprint pipeline
skaileup-implementation/       ← setup → features → verification pipeline
dev-implementation-experts-js/    ← JS/TS framework experts
dev-implementation-experts-python/ ← Python experts
dev-implementation-experts-typst/  ← Typst expert
skaileup-evaluate/              ← audit, E2E, testing, readiness gates
skaileup-shared/               ← merged contracts (referenced by all skills)
skaileup-standards/            ← codebase standards discovery/injection/sync
external/                 ← tracked third-party resources
knowledge-research/       ← deep research, paper extraction
knowledge-writing/        ← book/podcast generation
use/                      ← external service integrations (exa, perplexity, outline, etc.)
skaile-development/       ← skaile-dev workflow skills (commit-message, etc.)
```

## Skill Structure

Every skill lives in its own directory:

```
my-skill/
├── SKILL.md        ← YAML frontmatter + markdown body (the agent prompt)
├── CLI.md          ← Optional: CLI invocation docs
└── validator.py    ← Optional: output validation script
```

## Contracts

Shared contracts live in `skaileup-shared/contracts/`. All skills reference these.

## How This Is Consumed

The skaile CLI and platform consume these at **runtime** by reading SKILL.md files from disk. Configure the path via `SKAILE_RESOURCES_PATH` env var or `skaile.yaml`.

## Linting

```bash
python3 skaileup-shared/scripts/lint_concept.py
```
