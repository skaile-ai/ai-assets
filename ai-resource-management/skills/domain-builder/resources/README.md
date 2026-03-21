# Domain Builder — Resources

## DOMAIN.md Format Reference

`DOMAIN.md` uses the same YAML frontmatter syntax as `SKILL.md` but with a `type: domain` marker and a `building_blocks` map:

```yaml
---
name: <domain-name>         # kebab-case
description: "<purpose>"    # one sentence
type: domain
building_blocks:
  contracts: "<what shared knowledge the contract holds>"
  skills: "<what invocable skills live here>"
  agents: "<what autonomous agents live here>"
  prompts: "<what reusable prompt fragments live here>"
  tools: "<what CLI tools live here>"
stage: alpha | beta | production
---
```

## Domain vs Skill

| | Domain | Skill |
|-|--------|-------|
| Manifest | `DOMAIN.md` | `SKILL.md` |
| Invocable? | No — organizational only | Yes — triggered by user or orchestrator |
| Contains | Other skills, agents, prompts, tools, contracts | Scripts, examples, resources, references |
| Contract | `contracts/<domain>-contract/SKILL.md` | Reads from domain contract |

## Improvement Ideas

<!-- Append notes here after each use if the scaffold or templates could be improved. -->
