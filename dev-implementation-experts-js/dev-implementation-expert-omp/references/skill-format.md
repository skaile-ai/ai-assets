# OMP Skill Authoring Reference

## Skill Discovery Paths

omp discovers skills from multiple locations (in order of precedence):

1. `~/.omp/agent/skills/` — Global agent skills
2. `.omp/skills/` — Project-local skills
3. `.claude/skills/` — Claude-compatible skills (cross-tool)
4. Custom directories via `settings.json` → `skills.customDirectories`

## Skill File Format

Skills are Markdown files with YAML frontmatter:

```markdown
---
name: my-skill-name
description: One-line description used for matching against user prompts
globs: ["*.ts", "src/**/*.vue"]
---

# Skill Title

Skill instructions in markdown...
```

### Frontmatter Fields

| Field | Required | Description |
|-------|----------|-------------|
| `name` | Yes | Unique skill identifier (kebab-case) |
| `description` | Yes | Used for fuzzy matching against user prompts. Start with a verb. |
| `globs` | No | File patterns that activate this skill. Matched against workspace files. |

## Activation Mechanism

1. User sends a prompt
2. omp matches the prompt against all skill `description` fields
3. If `globs` are specified, the skill only activates if matching files exist in the workspace
4. Matched skills are injected into the system prompt automatically
5. Multiple skills can activate simultaneously

## Best Practices

### Description
- Start with an action verb: "Generate...", "Convert...", "Analyze..."
- Be specific enough for reliable matching, broad enough for flexible use
- Include key trigger words the user might say

```yaml
# Good
description: Generate TypeScript API clients from OpenAPI specifications

# Bad (too vague)
description: Help with APIs
```

### Globs
- Use globs to scope skills to relevant projects
- Prevents activation in unrelated workspaces

```yaml
# Only activate in Vue/Nuxt projects
globs: ["*.vue", "nuxt.config.*"]

# Only activate when Docker files exist
globs: ["Dockerfile", "docker-compose.*"]
```

### Content Structure
- Lead with the most important instructions
- Use numbered steps for sequential workflows
- Include code examples for complex patterns
- Reference external docs with links

## Skill Directory Structure

```
my-skill/
├── SKILL.md           # Main skill file (loaded into context)
├── references/        # Large docs loaded on-demand
│   └── api-spec.md
├── recipes/           # Reusable patterns
│   └── common-task.md
└── scripts/           # Automation
    └── helper.py
```

## Configuring Custom Skill Directories

### Per-project: `.omp/settings.json`
```json
{
  "skills": {
    "enabled": true,
    "customDirectories": [
      "/absolute/path/to/skills",
      "./relative/path/from/project"
    ]
  }
}
```

### Via Symlinks
```bash
# Link external skills into project
ln -s /path/to/external/skills .omp/skills/external
```

### Via Environment Variable
```bash
PI_CODING_AGENT_DIR=/path/to/agent-dir omp
# omp discovers skills from $PI_CODING_AGENT_DIR/skills/
```

## Filtering Skills at Runtime

```bash
# Only load skills matching patterns
omp --skills "git-*,docker"

# Disable all skill discovery
omp --no-skills
```

## Example: Complete Skill

```markdown
---
name: vue-component-generator
description: Generate Vue 3 components with TypeScript, Composition API, and PrimeVue integration
globs: ["*.vue", "nuxt.config.*"]
---

# Vue Component Generator

## When to Use
- Creating new Vue 3 components
- Converting Options API to Composition API
- Adding PrimeVue component integration

## Instructions
1. Always use `<script setup lang="ts">` syntax
2. Import types explicitly
3. Use `defineProps` with TypeScript generics
4. Use `defineEmits` with typed event signatures
5. Prefer PrimeVue components over raw HTML elements

## Template

\`\`\`vue
<template>
  <div>
    <!-- Component template -->
  </div>
</template>

<script setup lang="ts">
interface Props {
  // Define props
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update', value: string): void
}>()
</script>
\`\`\`
```
