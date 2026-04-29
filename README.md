# ai-assets

AI agent skills for the Skaile ecosystem — development workflow skills, implementation experts, research, writing, and external service integrations.

> **Skaileup skills** (concept, build, quality pipelines) have been moved to the separate [`ai-assets-skaileup`](https://github.com/skaile-ai/ai-assets-skaileup) repo.

## Quick Start

```bash
# Browse available skills
skaile skill list

# Install a skill
skaile add <skill-name>
```

## Domains

| Domain                                                                              | Purpose                                                                 |
| ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| [`ai-asset-management/`](ai-asset-management/DOMAIN.md)                             | Create skills, domains, CLI tools; navigate the catalog                 |
| [`skaile-development/`](skaile-development/DOMAIN.md)                               | Skaile-dev monorepo workflow skills (git, test, audit, implement, etc.) |
| [`skaile-platform/`](skaile-platform/DOMAIN.md)                                     | Skaile platform-specific skills                                         |
| [`forge-project/`](forge-project/DOMAIN.md)                                         | Forge app project skills                                                |
| [`dev-implementation-experts-js/`](dev-implementation-experts-js/DOMAIN.md)         | Deep JS/TS expertise (Nuxt, Directus, TipTap, PrimeVue, etc.)           |
| [`dev-implementation-experts-python/`](dev-implementation-experts-python/DOMAIN.md) | Deep Python expertise (Python, Pydantic AI, Marimo)                     |
| [`dev-implementation-experts-typst/`](dev-implementation-experts-typst/DOMAIN.md)   | Typst document expertise + expert advisor router                        |
| [`external/`](external/DOMAIN.md)                                                   | Tracked external/third-party resources                                  |
| [`knowledge-research/`](knowledge-research/DOMAIN.md)                               | Deep research and paper extraction                                      |
| [`knowledge-writing/`](knowledge-writing/DOMAIN.md)                                 | Content generation from research (podcasts, books)                      |
| [`use/`](use/DOMAIN.md)                                                             | External service integrations (Exa, Perplexity, ElevenLabs, etc.)       |

## Skill Structure

Every skill lives in `<domain>/skills/<skill-name>/`:

```
<skill-name>/
├── SKILL.md          ← Required. YAML frontmatter + agent prompt
├── CLI.md            ← Optional. Slash command / CLI usage
├── references/       ← Reference material (loaded on demand)
├── examples/         ← Worked examples
└── validator.py      ← Optional. Output validation
```

## Meta-Skills (Creating New Skills)

| Goal                          | Skill to use            |
| ----------------------------- | ----------------------- |
| Create a new skill            | `skill-builder`         |
| Create a new domain           | `domain-builder`        |
| Build a CLI tool for a skill  | `uv-cli-implementer`    |
| Browse / install from catalog | `ai-resource-navigator` |

→ [Full docs](/ai-assets/)
