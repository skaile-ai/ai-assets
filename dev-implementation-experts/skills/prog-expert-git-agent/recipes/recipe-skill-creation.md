# Recipe: Creating a New Skill

## When to Use
When you need to create a new Agent Skills spec-compliant SKILL.md.

## Steps

### 1. Create skill directory

Directory name = skill name (kebab-case):
```bash
mkdir -p skills/my-new-skill
```

### 2. Create SKILL.md

```yaml
---
name: my-new-skill
description: >-
  Does X and Y. Use when the user asks about Z or when
  condition ABC is detected in the workspace.
metadata:
  author: your-org
  version: "1.0"
---

# My New Skill

## Overview
One-two sentences: what this skill produces and why.

## When to Use
- User says: "..." (symptom phrases)
- Workspace state: specific condition

## Workflow
1. **Analyze**: Read context files
2. **Execute**: Perform the core task
3. **Output**: Write results

## Constraints
- Do not overwrite without confirmation
- Keep output under X tokens
```

### 3. Add optional directories

```
my-new-skill/
├── SKILL.md
├── scripts/         # deterministic helpers
├── references/      # domain docs (loaded on demand)
├── assets/          # templates, schemas
└── examples/        # input/output examples
```

### 4. Register in agent.yaml

```yaml
skills:
  - existing-skill
  - my-new-skill     # add to list
```

### 5. Validate

```bash
skills-ref validate ./skills/my-new-skill
```

## Naming Rules Checklist
- [ ] 1-64 characters
- [ ] Lowercase alphanumeric + hyphens only
- [ ] No leading/trailing hyphens
- [ ] No consecutive hyphens
- [ ] Directory name matches `name:` field exactly

## Progressive Disclosure Checklist
- [ ] `name` + `description` fit in ~100 tokens
- [ ] SKILL.md body under 500 lines / 5000 tokens
- [ ] Detailed reference material in separate files
- [ ] File references use relative paths, one level deep
