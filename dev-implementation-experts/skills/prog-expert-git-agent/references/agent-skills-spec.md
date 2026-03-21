# Agent Skills Specification

Source: https://agentskills.io/specification

## Directory Structure

A skill is a directory containing, at minimum, a `SKILL.md` file:

```
skill-name/
├── SKILL.md          # Required: metadata + instructions
├── scripts/          # Optional: executable code
├── references/       # Optional: documentation
├── assets/           # Optional: templates, resources
└── ...               # Any additional files
```

## SKILL.md Format

YAML frontmatter followed by Markdown content.

### Frontmatter Fields

| Field | Required | Constraints |
|-------|----------|-------------|
| `name` | Yes | Max 64 chars. Lowercase letters, numbers, hyphens. No leading/trailing/consecutive hyphens. Must match parent directory name. |
| `description` | Yes | Max 1024 chars. Non-empty. What it does AND when to use it. |
| `license` | No | License name or reference to bundled file. |
| `compatibility` | No | Max 500 chars. Environment requirements. |
| `metadata` | No | Arbitrary key-value mapping (string keys, string values). |
| `allowed-tools` | No | Space-delimited pre-approved tools. Experimental. |

### Minimal Example

```yaml
---
name: skill-name
description: A description of what this skill does and when to use it.
---
```

### Full Example

```yaml
---
name: pdf-processing
description: Extract PDF text, fill forms, merge files. Use when handling PDFs.
license: Apache-2.0
compatibility: Requires Python 3.14+ and uv
metadata:
  author: example-org
  version: "1.0"
allowed-tools: Bash(git:*) Read
---
```

### Name Validation Rules

- 1-64 characters
- Only lowercase alphanumeric (`a-z`, `0-9`) and hyphens (`-`)
- Cannot start or end with a hyphen
- No consecutive hyphens (`--`)
- **Must match parent directory name exactly**

### Description Best Practices

Good: "Extracts text and tables from PDF files, fills PDF forms, and merges multiple PDFs. Use when working with PDF documents or when the user mentions PDFs, forms, or document extraction."

Poor: "Helps with PDFs."

Include specific keywords that help agents identify relevant tasks.

## Body Content

Free-form markdown after frontmatter. No format restrictions.

Recommended sections:
- Step-by-step instructions
- Examples of inputs and outputs
- Common edge cases

Keep SKILL.md under 500 lines. Move detailed reference material to separate files.

## Progressive Disclosure (3-Tier Loading)

1. **Metadata** (~100 tokens): `name` + `description` — loaded at startup for all skills
2. **Instructions** (<5000 tokens recommended): Full SKILL.md body — loaded when skill activates
3. **Resources** (as needed): Files in `scripts/`, `references/`, `assets/` — loaded on demand

## File References

Use relative paths from skill root:

```markdown
See [the reference guide](references/REFERENCE.md) for details.
Run: scripts/extract.py
```

Keep references one level deep from SKILL.md. Avoid deeply nested chains.

## Optional Directories

### scripts/
Executable code agents can run. Should be self-contained with clear error messages.

### references/
Additional documentation loaded on demand:
- `REFERENCE.md` — detailed technical reference
- `FORMS.md` — form templates
- Domain-specific files

### assets/
Static resources: templates, images, data files, schemas.

## Validation

```bash
skills-ref validate ./my-skill
```

Checks frontmatter validity and naming conventions.
