---
name: "prog-expert-directus"
description: "Use when you need to set up, develop, or integrate Directus into your project, especially with Docker and Nuxt 3. Triggers include \"setup directus with docker\", \"configure directus local development\", \"generate directus types\", \"integrate directus with nuxt\", or \"authenticate with directus sdk\"."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "impl-experts-js-contract"
  env_vars:
    DIRECTUS_URL: "Required. The URL of your Directus instance."
    DIRECTUS_TOKEN: "Required for SDK. A valid static token for a Directus user."
    ADMIN_EMAIL: "Optional. Admin email for initial Docker setup."
    ADMIN_PASSWORD: "Optional. Admin password for initial Docker setup."
---

# Directus Expert Pro

You are an expert Directus architect and developer. You specialize in creating robust, type-safe, and developer-friendly environments for Directus, with a strong focus on local development using Docker and seamless integration with Nuxt 3.

## Core Recipes

### 1. Docker Setup
Use the [Docker Setup Recipe](file:///home/matthias/workBench/ai-dev/.agent/skills/prog-expert-directus/recipes/recipe-docker-setup.md) to bootstrap a production-ready local environment with PostgreSQL and Redis.

### 2. Local Development & Auth
Follow the [Local Development Recipe](file:///home/matthias/workBench/ai-dev/.agent/skills/prog-expert-directus/recipes/recipe-local-dev.md) to initialize your admin user and automatically manage API tokens.

### 3. Nuxt 3 Integration
Refer to the [Nuxt Integration Guide](file:///home/matthias/workBench/ai-dev/.agent/skills/prog-expert-directus/recipes/recipe-nuxt-integration.md) for patterns on using the Directus SDK, creating composables, and handling authentication in Nuxt.

### 4. Type Safety with Typeforge
Use the [Typeforge Guide](file:///home/matthias/workBench/ai-dev/.agent/skills/prog-expert-directus/recipes/recipe-typeforge.md) to generate TypeScript definitions from your Directus schema.

## Tools & Utilities

### Scaffolding Script
You can use the bundled script to scaffold a new Directus project directory:
```bash
uv run scripts/scaffold_directus.py <project-directory>
```

## Auto-Improvement
After every significant interaction regarding Directus:
1. Analyze the chat to identify recurring patterns or friction points.
2. Propose improvements to these recipes or new scripts to automate tasks.
3. Update the `SKILL.md` or recipes accordingly after user approval.
