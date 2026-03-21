# ai-resources

The `ai-resources` folder is the skill library for the Skaile ecosystem. It contains all AI agent skills organized into focused domains.

Skills are discovered and managed by the `arm` CLI (`ai-asset-manager`). Each domain has a `DOMAIN.md` describing its purpose and contents.

---

## Quick Start

```bash
# Register this folder as a resource (once)
arm resource add <path-to-ai-resources> --name ai-resources

# Browse everything interactively
arm explore ai-resources

# Search by keyword
arm search <keyword>

# Install a skill (resolves dependencies automatically)
arm install <skill-name>

# Deploy to Claude Code
arm deploy <skill-name>
```

See `ai-resource-management/skills/ai-resource-navigator/SKILL.md` for the full navigation workflow.

---

## Domains

| Domain | Purpose |
|--------|---------|
| [`ai-resource-management/`](ai-resource-management/DOMAIN.md) | Create skills, domains, CLI tools; navigate the catalog |
| [`dev-architecture/`](dev-architecture/DOMAIN.md) | System architecture and AI agent integration design |
| [`dev-conceptualization/`](dev-conceptualization/DOMAIN.md) | Project concept pipeline (discovery → blueprint → review) |
| [`dev-implementation/`](dev-implementation/DOMAIN.md) | Implementation pipeline (setup → features → verify) |
| [`dev-implementation-experts-js/`](dev-implementation-experts-js/DOMAIN.md) | Deep JS/TS framework expertise (Nuxt, Directus, TipTap, etc.) |
| [`dev-implementation-experts-python/`](dev-implementation-experts-python/DOMAIN.md) | Deep Python expertise (Python, Pydantic AI, Marimo) |
| [`dev-implementation-experts-typst/`](dev-implementation-experts-typst/DOMAIN.md) | Typst document expertise + expert advisor router |
| [`dev-quality/`](dev-quality/DOMAIN.md) | Quality assurance (audit, tests, readiness gates, sync) |
| [`dev-shared/`](dev-shared/DOMAIN.md) | Shared contracts and docs read by all domains |
| [`dev-standards/`](dev-standards/DOMAIN.md) | Codebase convention discovery, injection, sync |
| [`external/`](external/DOMAIN.md) | Tracked external/third-party resources |
| [`knowledge-research/`](knowledge-research/DOMAIN.md) | Deep research and paper extraction |
| [`knowledge-writing/`](knowledge-writing/DOMAIN.md) | Content generation from research (podcasts, books) |
| [`use/`](use/DOMAIN.md) | External service integrations (Exa, Perplexity, ElevenLabs, etc.) |

---

## Skill Structure Convention

Every skill lives in a `<domain>/skills/<skill-name>/` directory:

```
<skill-name>/
├── SKILL.md          ← Required. YAML frontmatter + agent prompt
├── CLI.md            ← Optional. Slash command / CLI usage
├── resources/        ← Reference material (loaded on demand)
├── examples/         ← Worked examples
└── scripts/          ← Supporting uv-runnable Python tools
```

All skills follow the progressive disclosure pattern: `name + description` always loaded, full `SKILL.md` on trigger, resources only when needed.

---

## Meta-Skills (Creating New Skills)

To extend this library:

| Goal | Skill to use |
|------|-------------|
| Create a new skill | `skill-builder` |
| Create a new domain | `domain-builder` |
| Build a CLI tool for a skill | `uv-cli-implementer` |
| Browse / install from catalog | `ai-resource-navigator` |
