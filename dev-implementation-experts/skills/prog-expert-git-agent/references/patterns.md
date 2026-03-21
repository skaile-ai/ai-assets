# GitAgent Patterns

## Pattern: Domain Agent in Workspace

Each domain directory contains its own agent.yaml + SOUL.md alongside its skills.
The root agent.yaml references domains via `dependencies:`.

```yaml
# root agent.yaml
dependencies:
  - name: domain-a
    source: ./domain-a
    version: 1.0.0
    mount: domain-a
```

```
workspace/
├── agent.yaml            # root orchestrator
├── SOUL.md
├── knowledge/            # shared context
├── domain-a/
│   ├── agent.yaml        # extends: ../agent.yaml
│   ├── SOUL.md
│   └── <skills...>
└── domain-b/
    ├── agent.yaml
    └── <skills...>
```

## Pattern: Shared Knowledge via Symlinks

Keep source-of-truth files in one location, symlink into `knowledge/` for
GitAgent's native context injection.

```bash
ln -s ../shared/contracts/spec.md knowledge/spec.md
```

## Pattern: Agent Inheritance

Sub-agents use `extends:` to inherit parent configuration:

```yaml
# domain-a/agent.yaml
extends: ../agent.yaml     # inherits model, knowledge, base config
name: domain-a-agent
skills: [skill-x, skill-y] # override: only these skills
```

## Pattern: Skill with Custom Metadata

Use `metadata:` in SKILL.md for framework-specific fields that other
runtimes can safely ignore:

```yaml
---
name: my-skill
description: Does the thing.
metadata:
  phase: conceptualization
  hard_gates:
    - type: file_exists
      path: "input/required.md"
  user_inputs:
    dialog:
      - id: name
        label: "Project name"
        type: text
---
```

## Pattern: Router Delegation

Root agent delegates to sub-agents via a router skill:

```yaml
delegation:
  mode: router
  router: orchestrator     # skill name that decides routing
```

## Pattern: Compliance Agent

For regulated environments, layer compliance on any agent:

```yaml
compliance:
  risk_tier: high
  supervision:
    human_in_the_loop: always
    kill_switch: true
  recordkeeping:
    audit_logging: true
    retention_period: 7y
```
