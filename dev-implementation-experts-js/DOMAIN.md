---
name: dev-implementation-experts-js
description: "Deep implementation expertise for JavaScript and TypeScript frameworks and libraries — each skill is a specialist consultant for one technology."
type: domain
building_blocks:
  contracts: "Expert skill conventions: how to structure recipes, atomic examples, and reference implementations; routing protocol for the advisor skill."
  docs: "Framework version matrices, known breaking changes, and integration notes."
  skills: "One expert skill per framework/library (nuxt, directus, tiptap, uno, primevue, vueuse, omp, una, scadcn, tts-stt); plus an advisor skill for routing."
  agents: "TBD"
  prompts: "TBD"
  tools: "TBD"
stage: alpha
---

# Dev Implementation Experts — JavaScript / TypeScript

This domain provides deep, focused implementation expertise for JavaScript and TypeScript frameworks and libraries used in the Skaile ecosystem. Each skill is a specialist: it knows the idiomatic patterns, common pitfalls, current API surface, and integration recipes for exactly one technology.

Implementation skills in `skaileup-implementation` route to these experts when tech-stack-specific guidance is needed. The `skaileup-implementation-expert-advisor` skill acts as a router — it reads the project's tech stack and delegates to the right specialists.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Expert skill conventions — recipe format, atomic example structure, reference implementation layout |
| `docs/` | Framework version matrices, breaking-change notes, cross-expert integration patterns |
| `skills/` | One expert skill per technology + the advisor router |

## Skills

| Skill | Technology |
|-------|-----------|
| `skaileup-implementation-expert-advisor` | Router — delegates to the right expert based on tech stack |
| `skaileup-implementation-expert-nuxt` | Nuxt 4 (framework, routing, SSR/SSG, modules) |
| `skaileup-implementation-expert-directus` | Directus (headless CMS, data model, permissions, API) |
| `skaileup-implementation-expert-tiptap` | TipTap (rich text editor, extensions, Yjs collaboration) |
| `skaileup-implementation-expert-uno` | UnoCSS (atomic CSS, shortcuts, icons) |
| `skaileup-implementation-expert-primevue` | PrimeVue (UI component library, theming) |
| `skaileup-implementation-expert-vueuse` | VueUse (composables collection) |
| `skaileup-implementation-expert-omp` | oh-my-pi agent core (AI integration, pi-agent-core) |
| `skaileup-implementation-expert-una` | Una UI (Nuxt UI layer) |
| `skaileup-implementation-expert-scadcn` | Shadcn-vue (component system) |
| `skaileup-implementation-expert-tts-stt-integration` | Text-to-speech / speech-to-text integration |
| `skaileup-implementation-expert-skill-system` | Skill system design and conventions |
| `skaileup-implementation-expert-integration-ai-agents` | AI agent integration patterns |

## Conventions

- Each expert skill includes `recipes/` (common patterns), `atomic-examples/` (minimal working snippets), and `reference-implementations/` (full working examples)
- Expert skills are never the primary entry point — they are delegated to by orchestrator or feature-implementation skills
- Version pins in frontmatter declare which library versions the skill covers; update when APIs change
