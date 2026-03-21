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

## Agents

Each domain that has a pipeline exposes a GitAgent-compatible agent in `<domain>/agents/`. The root orchestrator lives in `ai-resource-management/agents/skaile/` and delegates to domain agents.

```
ai-resource-management/agents/skaile/       ← root orchestrator (router)
dev-conceptualization/agents/orchestrator/  ← concept pipeline (Discovery → Blueprint)
dev-implementation/agents/orchestrator/     ← implementation pipeline (Setup → Verify)
dev-quality/agents/quality/                 ← quality assurance
dev-architecture/agents/architecture/       ← system design
```

All agents follow the GitAgent spec v0.1.0. Each directory contains `agent.yaml` (manifest) + `SOUL.md` (identity) + optional `RULES.md`.

---

### Running with Claude Code

Skills and agents are invoked as slash commands inside Claude Code. Install them first via `arm`:

```bash
# Deploy a skill to your project's .claude/skills/
arm deploy <skill-name>

# Or install all skills from a domain
arm deploy --domain dev-conceptualization
```

Once deployed, trigger from Claude Code chat:

| What you want | Slash command |
|---------------|---------------|
| Full concept pipeline | `/concept` |
| Implementation pipeline | `/implement` |
| Quality audit | `/audit` |
| Readiness gate | `/ready` |
| Cross-reference sync | `/sync` |

**To use an agent directly in Claude Code** (without installing via arm), add the agent's parent directory to your `.claude/skills/` manually or via symlink:

```bash
# Example: use the concept orchestrator
ln -s <path-to>/dev-conceptualization/agents/orchestrator \
      .claude/skills/concept-orchestrator
```

Claude Code will pick up the `SOUL.md` and `RULES.md` as system context when the agent is active.

---

### Running with oh-my-pi (omp)

`omp` discovers skills from `.omp/skills/` in your project root. Point it at a domain's skill directory or the agent directory:

```bash
# Run the concept orchestrator skill (CF variant)
omp run concept --skills-dir <path-to>/dev-conceptualization/skills/00_orchestrator/cf

# Run the implementation orchestrator (merged variant)
omp run implement --skills-dir <path-to>/dev-implementation/skills/00_orchestrator

# Run in RPC mode (used by concept-forge sidecar)
omp --mode rpc --skills-dir <path-to>/dev-conceptualization/skills/00_orchestrator/cf
```

**Recommended: symlink skill directories into your project's `.omp/skills/`:**

```bash
# Link individual orchestrator variants
ln -s <path-to>/dev-conceptualization/skills/00_orchestrator/cf  .omp/skills/concept
ln -s <path-to>/dev-implementation/skills/00_orchestrator        .omp/skills/implement
ln -s <path-to>/dev-quality/skills/audit/cf                      .omp/skills/audit
ln -s <path-to>/dev-quality/skills/ready/cf                      .omp/skills/ready

# Then run by name
omp run concept
omp run implement
```

The agent's `SOUL.md` and `RULES.md` can be passed as a system prompt prefix via `--system-file`:

```bash
omp run concept \
  --system-file <path-to>/dev-conceptualization/agents/orchestrator/SOUL.md \
  --skills-dir  <path-to>/dev-conceptualization/skills/00_orchestrator/cf
```

---

### Running with GitAgent CLI

```bash
# Validate any agent
npx gitagent validate ai-resources/dev-conceptualization/agents/orchestrator/

# Run the concept pipeline
npx gitagent run ai-resources/dev-conceptualization/agents/orchestrator/

# Run the implementation pipeline
npx gitagent run ai-resources/dev-implementation/agents/orchestrator/

# Run via the root orchestrator (auto-routes by intent)
npx gitagent run ai-resources/ai-resource-management/agents/skaile/

# Export to another framework (e.g. CrewAI, OpenAI)
npx gitagent export ai-resources/dev-conceptualization/agents/orchestrator/ --target crewai
```

Add shortcuts to your project's `package.json`:

```json
{
  "scripts": {
    "agent:concept":    "gitagent run ai-resources/dev-conceptualization/agents/orchestrator/",
    "agent:implement":  "gitagent run ai-resources/dev-implementation/agents/orchestrator/",
    "agent:quality":    "gitagent run ai-resources/dev-quality/agents/quality/",
    "agent:validate":   "gitagent validate ai-resources/ai-resource-management/agents/skaile/"
  }
}
```

---

## Meta-Skills (Creating New Skills)

To extend this library:

| Goal | Skill to use |
|------|-------------|
| Create a new skill | `skill-builder` |
| Create a new domain | `domain-builder` |
| Build a CLI tool for a skill | `uv-cli-implementer` |
| Browse / install from catalog | `ai-resource-navigator` |
