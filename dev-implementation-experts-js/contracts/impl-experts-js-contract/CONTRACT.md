---
name: "impl-experts-js-contract"
description: "Shared contract for all JavaScript/TypeScript implementation expert skills. Describes the expert skill folder structure (recipes, atomic-examples, reference-implementations), discovery conventions, and how experts are consumed by implementation skills."
metadata:
  stage: "alpha"
  do_not_invoke: true
---

# JS/TS Implementation Experts — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `dev-implementation-experts-js` skills read this before operating.

## Scope

Expert skills are **passive reference libraries** — they are not invoked directly by users, but loaded into implementation subagent contexts when a matching technology is detected in the project's tech stack.

## Expert Skill Folder Structure

Each expert lives in its own directory:

```
skaileup-implementation-expert-<tech>/
├── SKILL.md                      ← Required. Agent prompt + frontmatter
├── recipes/                      ← Reusable implementation patterns (markdown)
│   └── <pattern-name>.md
├── atomic-examples/              ← Minimal, focused code snippets
│   └── <example>.ts / .vue / etc.
├── reference-implementations/    ← Complete working implementations
│   └── <feature>/
├── assets/                       ← Static assets referenced by examples
├── examples/                     ← Larger worked examples
├── references/                   ← Curated external docs / API references
├── resources/                    ← Supporting materials
└── scripts/                      ← Utility scripts for the expert domain
```

## SKILL.md Frontmatter

```yaml
---
name: skaileup-implementation-expert-<tech>
source: MIGRATED
description: "Expert guidance for <tech>. Loaded automatically when <tech> is in the project stack."
metadata:
  type: expert
  technology: <tech>
  discovery_keywords: [<tech>, <alias>]
stage: alpha | beta | production
---
```

## Discovery and Loading

Implementation skills discover experts at runtime:
1. Read `_concept/blueprint/techstack.md`
2. Match stack entries against `discovery_keywords` in known expert skill dirs
3. Load matched SKILL.md + relevant `recipes/` into the implementation subagent context

## Recipe Format

Each recipe in `recipes/` should follow:
```markdown
# <Pattern Name>

**When to use:** <trigger condition>

## Implementation
\`\`\`typescript
// code
\`\`\`

## Pitfalls
- <common mistake>

## References
- <doc link if applicable>
```

## Covered Technologies

| Expert | Technology |
|--------|-----------|
| `nuxt` | Nuxt 4 framework |
| `primevue` | PrimeVue UI component library |
| `directus` | Directus headless CMS / API |
| `tiptap` | TipTap rich text editor |
| `omp` | @oh-my-pi agent core / RPC |
| `vueuse` | VueUse composable library |
| `una` | Una UI design system |
| `uno` | UnoCSS / Unocss |
| `tts-stt-integration` | TTS/STT audio integration |
| `scadcn` | Scadcn component patterns |
| `advisor` | Routes to appropriate JS expert |
