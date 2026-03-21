---
name: OMP Skill Authoring
description: Create custom omp skills with frontmatter, globs, and instructions
libraries_used: omp
---

# OMP Skill Authoring

## Objective
Author an omp skill that activates on matching prompts and provides domain-specific instructions.

## Prerequisites
- omp installed
- Target directory in a discovery path

## Instructions

### 1. Choose a discovery path

| Path | Scope |
|------|-------|
| `~/.omp/agent/skills/my-skill/` | Global (all projects) |
| `.omp/skills/my-skill/` | Project-local |
| `.claude/skills/my-skill/` | Cross-tool compatible |

### 2. Create SKILL.md with frontmatter

```markdown
---
name: my-domain-skill
description: Generate optimized database queries for PostgreSQL with proper indexing hints
globs: ["*.sql", "server/db/**"]
---

# PostgreSQL Query Expert

## Instructions
1. Always use parameterized queries to prevent SQL injection
2. Suggest appropriate indexes for WHERE clauses
3. Use CTEs for complex joins
4. Include EXPLAIN ANALYZE hints in comments

## Patterns

### Pagination
\`\`\`sql
SELECT * FROM items
WHERE id > $1  -- cursor-based, not OFFSET
ORDER BY id ASC
LIMIT $2;
\`\`\`

### Upsert
\`\`\`sql
INSERT INTO items (key, value)
VALUES ($1, $2)
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;
\`\`\`
```

### 3. Add globs for context-aware activation

```yaml
# Only activate in projects with these files
globs: ["*.sql", "drizzle.config.*", "server/db/**"]
```

### 4. Test activation

```bash
# Verify skill is discovered
omp --skills "my-domain*" -p "test query"

# List all discovered skills (check omp logs)
omp --mode rpc --no-session  # then check ~/.omp/logs/
```

### 5. Add custom directories in settings

```json
// .omp/settings.json
{
  "skills": {
    "customDirectories": ["/path/to/my/skills"]
  }
}
```

## Tips
- Description should start with a verb for reliable matching
- Keep SKILL.md under 500 lines — use `references/` for large docs
- Globs prevent activation in irrelevant projects
- Multiple skills can activate simultaneously
