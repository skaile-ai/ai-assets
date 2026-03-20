# dev-standards

This domain handles codebase convention discovery, injection, and synchronization. These skills run in "parallel mode" — they can execute alongside other skills without blocking the main pipeline.

## Skills

| Skill | Path | Purpose |
|---|---|---|
| **discover** | `skills/discover/` | Scan an existing codebase and extract its conventions into `_standards/` |
| **inject** | `skills/inject/` | Match discovered standards to a requesting skill's needs |
| **sync** | `skills/sync/` | Bidirectional sync: update `_standards/` if codebase evolves |

## Profiles

The `profiles/` directory contains technology-specific presets that configure all skills for a given stack. Available profiles:

| Profile | Stack |
|---|---|
| `nuxt-ui/` | Nuxt 4 + @nuxt/ui (Reka UI + Tailwind 4) |
| `nuxt-primevue/` | Nuxt + PrimeVue |
| `nuxt-minimal/` | Nuxt minimal (no UI library) |
| `nextjs-shadcn/` | Next.js + shadcn/ui |
| `nextjs-radix/` | Next.js + Radix UI |
| `postxl/` | PostXL (NestJS + Next.js monorepo) |

## Standards Output

Skills write to `_standards/` in the project:
```
_standards/
├── index.yml          → registry of all discovered standards
├── api/               → API conventions
├── database/          → DB conventions
├── ui/                → component conventions
├── naming/            → naming rules
├── testing/           → testing standards
└── architecture/      → architecture patterns
```

## Source

CF only — no Saxe equivalent for these skills.
