# ai-assets

AI agent skills for the Skaile ecosystem — development workflow skills, implementation experts, research, writing, and external service integrations.

> **Skaileup skills** (concept, build, quality pipelines) live in the separate [`ai-assets-skaileup`](https://github.com/skaile-ai/ai-assets-skaileup) repo. Its predecessor is archived as [`ai-assets-skaileup-v1`](https://github.com/skaile-ai/ai-assets-skaileup-v1).

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
| [`skaile-development/`](skaile-development/DOMAIN.md)                               | Skaile-dev monorepo workflow skills (git, test, audit, implement, etc.) |
| [`skaile-platform/`](skaile-platform/DOMAIN.md)                                     | Skaile platform-specific skills                                         |
| [`skaile-forge/`](skaile-forge/DOMAIN.md)                                           | skaile-forge app agents and skills                                      |
| [`dev-implementation-experts-js/`](dev-implementation-experts-js/DOMAIN.md)         | Deep JS/TS expertise (Nuxt, Directus, TipTap, PrimeVue, etc.)           |
| [`dev-implementation-experts-python/`](dev-implementation-experts-python/DOMAIN.md) | Deep Python expertise (Python, Pydantic AI, Marimo) + uv CLI builder    |
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

| Goal                          | Skill to use                                     |
| ----------------------------- | ------------------------------------------------ |
| Create skills, agents, domains | `authoring-skaile-assets` (bundled in the skaile CLI) |
| Build a CLI tool for a skill  | `uv-cli-implementer`                             |
| Browse / install assets       | `skaile-manage-workspace` (bundled in the skaile CLI) |

→ [Full docs](/ai-assets/)
