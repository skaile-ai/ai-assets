# GitAgent Specification Reference

Summary of the [GitAgent Specification](https://www.gitagent.sh/) for quick reference during agent building.

## Core Principle

"Clone a repo, get an agent." A repository is the agent. Git provides version control, branching for A/B testing, diff-based auditing, and tag-based releases.

## Minimal Agent (2 files)

```
agent-name/
├── agent.yaml    # Manifest
└── SOUL.md       # Identity
```

## Standard Agent (full structure)

```
agent-name/
├── agent.yaml              # Required: manifest
├── SOUL.md                 # Required: identity
├── RULES.md                # Hard constraints
├── DUTIES.md               # Segregation of duties
├── AGENTS.md               # Framework-agnostic instructions
├── skills/                 # Capability modules
├── tools/                  # MCP-compatible schemas
├── workflows/              # SkillsFlow multi-step procedures
├── knowledge/              # Reference documents
│   └── index.yaml          # Priority ordering
├── memory/runtime/         # Live state (gitignored)
├── hooks/                  # Lifecycle handlers
├── config/                 # Environment overrides
├── compliance/             # Regulatory artifacts
├── agents/                 # Sub-agent definitions
└── examples/               # Few-shot calibration
```

## agent.yaml Required Fields

| Field | Description |
|---|---|
| `spec_version` | GitAgent spec version ("0.1.0") |
| `name` | kebab-case identifier |
| `version` | semver |
| `description` | One-line description |

## agent.yaml Optional Fields

| Field | Description |
|---|---|
| `author` | Creator identifier |
| `license` | License name |
| `tags` | Searchable keywords |
| `extends` | Path to parent agent.yaml for inheritance |
| `model` | LLM preferences: preferred, fallback, constraints |
| `skills` | List of skill names the agent can invoke |
| `delegation` | Mode: explicit, router, auto |
| `runtime` | max_turns, timeout |
| `requires` | Dependencies (sub-agents, skills) |
| `compliance` | Regulatory config (risk_tier, supervision, recordkeeping, SOD) |
| `metadata` | Arbitrary extensions |

## Composition

```yaml
# Inherit from parent
extends: https://github.com/org/base-agent.git

# Depend on sub-agents
requires:
  - name: fact-checker
    source: https://github.com/org/fact-checker.git
    version: ^1.0.0
    mount: agents/fact-checker
```

## Export Targets

system-prompt, claude-code, openai, crewai, lyzr, github, git, opencode, openclaw, nanobot

## CLI

```bash
gitagent init --template standard
gitagent validate --compliance
gitagent export --format system-prompt
gitagent run <source> --adapter claude-code
gitagent audit
```

## Spec Version

v0.1.0 — https://github.com/open-gitagent/gitagent
