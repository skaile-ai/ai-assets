# Skaile AI Assets

## What This Is

All AI skills, domains, contracts, and shared resources for the Skaile ecosystem. Pure content — no code dependencies.

**GitHub:** `github.com/skaile-ai/skaile-ai-assets`

## Structure

Skills are organized into domains. Each domain has a `DOMAIN.md` and contains skills, contracts, and docs.

```
ai-asset-management/   ← meta: skill/domain scaffolding, catalog navigation
skaileup-onboard/              ← project initialization, onboarding, seed ingestion, orchestration
skaileup-research/             ← agentic web research, knowledge grounding
skaileup-discovery/            ← problem space: brief, goals, brand identity
skaileup-experience/           ← user experience: journeys, features, screens, components
skaileup-prototype/            ← simple mockup prototypes
skaileup-storybook/            ← living Storybook prototypes
skaileup-blueprint/            ← technical decisions: techstack, architecture, datamodel
skaileup-concept-meta/         ← concept operations: review, extend, evaluate, umbrella
skaileup-build/                ← implementation: scaffolding, TDD features, migrations
skaileup-quality/              ← code quality: audit, tests, readiness gates
skaileup-shared/               ← merged contracts (referenced by all skills)
skaileup-standards/            ← codebase standards discovery/injection/sync
skaileup-lab/                  ← skill testing and improvement
dev-implementation-experts-js/    ← JS/TS framework experts
dev-implementation-experts-python/ ← Python experts
dev-implementation-experts-typst/  ← Typst expert
external/                 ← tracked external resources
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
