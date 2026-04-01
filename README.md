# ai-resources

The master library of AI agent skills, flows, and agents for the Skaile ecosystem — organized into focused domains covering conceptualization, implementation, quality, research, writing, and more.

## Quick Start via skaile CLI

```bash
# See available flows
skaile flow list

# Run a flow on your project
skaile run cli-concept --project-dir ./my-project

# Browse available skills
skaile skill list
skaile skill list skaileup-conceptualization
```

## Quick Start via skaile (asset management)

```bash
# Register this folder as a resource (once)
skaile repo add <path-to-ai-resources> ai-resources

# Browse everything interactively
skaile explore ai-resources

# Install a skill (resolves dependencies automatically)
skaile add <skill-name>
```

## Domains

| Domain | Purpose |
|---|---|
| [`ai-asset-management/`](ai-asset-management/DOMAIN.md) | Create skills, domains, CLI tools; navigate the catalog |
| [`skaileup-architecture/`](skaileup-architecture/DOMAIN.md) | System architecture and AI agent integration design |
| [`skaileup-conceptualization/`](skaileup-conceptualization/DOMAIN.md) | Project concept pipeline (brief → features → data model) |
| [`skaileup-implementation/`](skaileup-implementation/DOMAIN.md) | Implementation pipeline (scaffold → features → verify) |
| [`dev-implementation-experts-js/`](dev-implementation-experts-js/DOMAIN.md) | Deep JS/TS expertise (Nuxt, Directus, TipTap, PrimeVue, etc.) |
| [`dev-implementation-experts-python/`](dev-implementation-experts-python/DOMAIN.md) | Deep Python expertise (Python, Pydantic AI, Marimo) |
| [`dev-implementation-experts-typst/`](dev-implementation-experts-typst/DOMAIN.md) | Typst document expertise + expert advisor router |
| [`skaileup-evaluate/`](skaileup-evaluate/DOMAIN.md) | Quality assurance (audit, tests, readiness gates, sync) |
| [`skaileup-shared/`](skaileup-shared/DOMAIN.md) | Shared contracts and docs read by all domains |
| [`skaileup-standards/`](skaileup-standards/DOMAIN.md) | Codebase convention discovery, injection, sync |
| [`external/`](external/DOMAIN.md) | Tracked external/third-party resources |
| [`knowledge-research/`](knowledge-research/DOMAIN.md) | Deep research and paper extraction |
| [`knowledge-writing/`](knowledge-writing/DOMAIN.md) | Content generation from research (podcasts, books) |
| [`use/`](use/DOMAIN.md) | External service integrations (Exa, Perplexity, ElevenLabs, etc.) |

## Skill Structure

Every skill lives in `<domain>/skills/<skill-name>/`:

```
<skill-name>/
├── SKILL.md          ← Required. YAML frontmatter + agent prompt
├── CLI.md            ← Optional. Slash command / CLI usage
├── resources/        ← Reference material (loaded on demand)
├── examples/         ← Worked examples
└── scripts/          ← Supporting Python tools
```

## Flows

JSON state machine definitions in `<domain>/flows/`:

| Flow ID | Domain | Description |
|---|---|---|
| `cli-concept` | skaileup-conceptualization | Concept phase for CLI tools (brief, features, tech stack, data model) |
| `prototype` | skaileup-conceptualization | Full concept pipeline for quick prototypes |
| `concept-only` | skaileup-conceptualization | Concept phase only (no implementation) |
| `reverse-engineer` | skaileup-conceptualization | Start from an existing codebase |
| `standard` | skaileup-implementation | Standard implementation pipeline |
| `full` | skaileup-implementation | Full implementation with all optional steps |
| `cli` | skaileup-implementation | CLI-focused implementation |
| `prototype` | skaileup-implementation | Rapid prototype implementation |

Run flows with:
```bash
skaile run <flow-id> --project-dir ./my-project
```

## Running with Claude Code

Deploy skills to your project's `.claude/skills/` via the skaile CLI:

```bash
skaile add <skill-name>
```

Then trigger from Claude Code chat using slash commands (`/concept`, `/implement`, `/audit`, `/ready`).

## Meta-Skills (Creating New Skills)

| Goal | Skill to use |
|---|---|
| Create a new skill | `skill-builder` |
| Create a new domain | `domain-builder` |
| Build a CLI tool for a skill | `uv-cli-implementer` |
| Browse / install from catalog | `ai-resource-navigator` |

→ [Full docs](/ai-resources/)
