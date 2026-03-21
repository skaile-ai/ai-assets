# Recipe: Adding a Domain Sub-Agent

## When to Use
When adding a new domain (e.g., DevOps, QA, Data Science) to an existing
multi-agent workspace.

## Steps

### 1. Create domain directory with manifests

```bash
mkdir -p new-domain
```

### 2. Create agent.yaml

```yaml
spec_version: "0.1.0"
name: workspace-new-domain        # prefix with workspace name
version: 1.0.0
description: >-
  New domain agent description.
extends: ../agent.yaml            # inherit from root
model:
  preferred: claude-sonnet-4-6    # can override parent model
skills:
  - skill-a
  - skill-b
runtime:
  max_turns: 50
  timeout: 300
```

### 3. Create SOUL.md

```markdown
# Workspace — New Domain

## Core Identity
You specialize in [domain description].

## Communication Style
[Tailored to domain needs]
```

### 4. Add skills as subdirectories

```
new-domain/
├── agent.yaml
├── SOUL.md
├── skill-a/
│   └── SKILL.md
└── skill-b/
    └── SKILL.md
```

### 5. Register in root agent.yaml

Add to `dependencies:`:
```yaml
dependencies:
  # ... existing
  - name: new-domain
    source: ./new-domain
    version: 1.0.0
    mount: new-domain
```

Add to `agents:`:
```yaml
agents:
  # ... existing
  new-domain:
    description: New domain agent
    delegation:
      mode: auto
      triggers:
        - new_domain_task_detected
```

### 6. Validate

```bash
gitagent validate
```

## Checklist
- [ ] Domain directory created
- [ ] agent.yaml with extends to parent
- [ ] SOUL.md with domain-specific identity
- [ ] Skills added with SKILL.md files
- [ ] Root agent.yaml updated (dependencies + agents)
- [ ] `gitagent validate` passes
