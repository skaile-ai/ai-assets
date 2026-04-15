# skaileup-standards

This domain handles codebase convention discovery, injection, and synchronization. These skills run in "parallel mode" — they can execute alongside other skills without blocking the main pipeline.

## Skills

| Skill | Path | Purpose |
|---|---|---|
| **skailup-standards-discover** | `skills/skailup-standards-discover/` | Scan an existing codebase and extract its conventions into `_standards/` |
| **skailup-standards-inject** | `skills/skailup-standards-inject/` | Match discovered standards to a requesting skill's needs |
| **skailup-standards-sync** | `skills/skailup-standards-sync/` | Bidirectional sync: update `_standards/` if codebase evolves |

## Profiles

The `profiles/` directory contains technology-specific presets that configure all skills for a given stack. Available profiles:

| Profile | Stack |
|---|---|
| `skailup-tech-stack-nuxt-ui/` | Nuxt 4 + @nuxt/ui (Reka UI + Tailwind 4) |
| `skailup-tech-stack-nuxt-primevue/` | Nuxt + PrimeVue |
| `skailup-tech-stack-nuxt-minimal/` | Nuxt minimal (no UI library) |
| `skailup-tech-stack-nextjs-shadcn/` | Next.js + shadcn/ui |
| `skailup-tech-stack-nextjs-radix/` | Next.js + Radix UI |
| `skailup-tech-stack-postxl/` | PostXL (NestJS + Next.js monorepo) |

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
