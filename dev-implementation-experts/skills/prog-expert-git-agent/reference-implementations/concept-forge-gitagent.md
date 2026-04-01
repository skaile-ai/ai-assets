# Reference: Concept Forge — GitAgent Integration

Real-world example of integrating GitAgent into an existing multi-skill pipeline.

## Architecture

concept-forge-skills is a 45-skill pipeline framework organized into domains.
Each domain directory becomes a GitAgent sub-agent.

```
concept-forge-skills/
├── agent.yaml                      # root orchestrator
├── SOUL.md                         # root identity
├── RULES.md                        # iron laws
├── knowledge/                      # symlinks to shared/contracts/
│   ├── index.yaml
│   └── *.md → ../shared/contracts/*
├── skaileup-conceptualization/          # domain: concept design
│   ├── agent.yaml                  # extends: ../agent.yaml
│   ├── SOUL.md
│   ├── 00_orchestrator/SKILL.md
│   ├── 10_discovery/overview/SKILL.md
│   ├── 20_experience/features/SKILL.md
│   └── 30_blueprint/datamodel/SKILL.md
├── skaileup-implementation/             # domain: build
│   ├── agent.yaml
│   ├── SOUL.md
│   ├── scaffold/SKILL.md
│   └── feature/SKILL.md
├── support/                        # domain: quality
│   ├── agent.yaml
│   ├── SOUL.md
│   ├── audit/SKILL.md
│   └── e2e/SKILL.md
├── tech-stack/                     # reference skills (dual-purpose)
│   └── nuxt-ui/SKILL.md
├── flows/                          # pipeline flow graphs
│   └── mvp.json
├── shared/contracts/               # source of truth (symlinked into knowledge/)
└── package.json                    # npm compatibility
```

## Key Design Decisions

1. **No restructuring**: Agent manifests added alongside existing structure
2. **Symlinks for knowledge/**: shared/contracts/ remains source of truth
3. **Domain = agent**: Each domain dir has agent.yaml + SOUL.md at its root
4. **Metadata for CF fields**: SKILL.md uses `metadata:` for framework-specific fields
5. **Dual distribution**: npm package (concept-forge app) + GitAgent CLI

## SKILL.md Adaptation

CF-specific fields moved into `metadata:` block:

```yaml
---
name: features
description: "Use when _concept/03_features/ is empty..."
metadata:
  phase: conceptualization
  sub_phase: experience
  folder: "03_features"
  keywords: [features, requirements]
  subagent: true
  hard_gates:
    - type: file_exists
      path: "01_project/brief.md"
  user_inputs:
    dialog:
      - id: scope
        label: "Feature scope"
        type: select
        options: [mvp, full]
---
```

GitAgent runtimes ignore `metadata:` fields they don't understand.
The concept-forge Nuxt app reads `metadata.user_inputs` for its UI.
