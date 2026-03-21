---
name: OMP Project Configuration
description: Configure omp for a project with custom skills, model defaults, and extensions
libraries_used: omp
---

# OMP Project Configuration

## Objective
Set up per-project omp configuration with custom skills, model settings, and extensions.

## Instructions

### 1. Create project config directory

```bash
mkdir -p .omp/skills
```

### 2. Configure settings

```json
// .omp/settings.json
{
  "skills": {
    "enabled": true,
    "customDirectories": [
      "./custom-skills",
      "/shared/team/skills"
    ]
  }
}
```

### 3. Link external skills

```bash
# Symlink a shared skills directory
ln -s /path/to/shared/app-skills .omp/skills/app-skills
```

### 4. Set model defaults via environment

```bash
# .env or shell profile
export ANTHROPIC_API_KEY="sk-ant-..."
export PI_SMOL_MODEL="ollama/gemma3:4b"
export PI_SLOW_MODEL="anthropic/claude-opus-4"
```

### 5. Project-specific system prompt

```bash
# Append project context to system prompt
omp --append-system-prompt "This project uses Nuxt 4, PrimeVue 4, and Drizzle ORM."
```

### 6. Minimal agent for CI/scripts

```bash
# Strip all extras for fast, focused execution
omp --no-lsp --no-extensions --no-skills --no-pty -p "Run the test suite"
```

## Directory Layout

```
my-project/
├── .omp/
│   ├── settings.json     # Project omp config
│   └── skills/           # Project-local skills
│       ├── my-skill/
│       │   └── SKILL.md
│       └── shared/       # Symlink to shared skills
├── .env                  # API keys
└── src/
```
