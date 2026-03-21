# Recipe: Shared Knowledge / Context Injection

## When to Use
When multiple agents or skills need access to the same reference documents
(contracts, specs, schemas) without duplicating files.

## Approach: Symlinks + knowledge/

### 1. Keep source of truth in one location

```
shared/contracts/
├── pipeline.json
├── iron-laws.md
├── patterns.md
└── ...
```

### 2. Create knowledge/ with symlinks

```bash
mkdir -p knowledge
ln -s ../shared/contracts/pipeline.json knowledge/pipeline.json
ln -s ../shared/contracts/iron-laws.md knowledge/iron-laws.md
ln -s ../shared/contracts/patterns.md knowledge/patterns.md
```

### 3. Create index.yaml for retrieval hints

```yaml
# knowledge/index.yaml
files:
  - path: pipeline.json
    description: "Dependency graph and phase definitions"
    priority: high
  - path: iron-laws.md
    description: "Non-negotiable constraints"
    priority: high
  - path: patterns.md
    description: "Reusable workflow patterns"
    priority: medium
```

### 4. Sub-agents inherit knowledge

Sub-agents with `extends: ../agent.yaml` automatically have access to
the parent's knowledge/ directory.

## Cross-Repo Knowledge Sharing

For sharing across separate repositories:

```yaml
# In consuming agent's agent.yaml
dependencies:
  - name: shared-knowledge
    source: https://github.com/org/shared-knowledge.git
    version: ^1.0.0
    mount: knowledge/shared
```

## Key Points
- Symlinks prevent drift between source and knowledge/
- index.yaml helps the runtime prioritize which files to load
- Sub-agents inherit parent knowledge via `extends:`
- Cross-repo sharing uses `dependencies:` with git URLs
