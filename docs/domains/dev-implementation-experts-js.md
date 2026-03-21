---
title: dev-implementation-experts-js
description: Deep implementation expertise for JavaScript and TypeScript frameworks — each skill is a specialist consultant for one technology.
---

Provides focused implementation expertise for JavaScript and TypeScript frameworks and libraries used in the Skaile ecosystem. Each skill is a specialist: it knows the idiomatic patterns, common pitfalls, current API surface, and integration recipes for exactly one technology.

Implementation skills in `dev-implementation` route to these experts when tech-stack-specific guidance is needed. The `dev-implementation-expert-advisor` skill (in `dev-implementation-experts-typst`) acts as the general-purpose router.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | Expert skill conventions — recipe format, atomic example structure, reference implementation layout |
| `docs/` | Framework version matrices, breaking-change notes, cross-expert integration patterns |
| `prog-expert-*/` | One skill directory per technology (no `skills/` subdirectory — experts live at domain root) |

## Skills

| Skill | Technology | When to use |
|---|---|---|
| `prog-expert-nuxt` | Nuxt 4 | Server API, composables, layouts, Nitro config, SSR patterns |
| `prog-expert-directus` | Directus | CMS schema design, permissions, SDK usage, webhook patterns |
| `prog-expert-tiptap` | TipTap | Rich text editor setup, extensions, collaborative editing (Yjs/Hocuspocus) |
| `prog-expert-primevue` | PrimeVue 4 | Component patterns, theming, data tables, form validation |
| `prog-expert-vueuse` | VueUse | Composable selection, reactivity patterns, browser API wrappers |
| `prog-expert-una` | Una UI | Component library usage and theming patterns |
| `prog-expert-uno` | UnoCSS | Utility class config, custom rules, presets |
| `prog-expert-scadcn` | shadcn-vue | Component installation, customization, form patterns |
| `prog-expert-omp` | oh-my-pi (omp) | omp integration, RPC mode, skill deployment, session management |
| `prog-expert-tts-stt-integration` | TTS/STT | Text-to-speech and speech-to-text integration patterns |
