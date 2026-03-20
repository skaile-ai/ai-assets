# cf_concept_mock CLI

## Trigger

Invoke with: "Generate mockups" or "Design the UI" or "Show me what the app looks like"

## Prerequisites

- `_concept/07_screens/**/*.md` — screen specs
- `_concept/04_brand/tokens.json` — brand colors, fonts, mode
- `_concept/06_datamodel/seed.json` — template data for scenarios

## Initialization

```bash
mkdir -p _concept/05_mockups
```

## Stack Selection

Choose a mockup technology stack:

| Stack | ID | When to Use |
|-------|----|-------------|
| Alpine.js + Shoelace | `alpine_shoelace` | Lightweight prototypes, web-component UI primitives |
| Vue 3 + PrimeVue | `vue_primevue` | Data-heavy apps, rich DataTable/Dialog/Form components |
| Preact + HTM | `preact_htm` | Modern ES modules, signal-based reactivity |

All stacks are CDN-only — no build tools required.

## Output

- `_concept/05_mockups/<screen>.html` — standalone HTML mockups
- `_concept/05_mockups/cf__shared/` — shared styles, layout, seed data
- Updates feature docs: adds mockup links to screen frontmatter

## Preview

```bash
python3 -m http.server 8000
# Open http://localhost:8000/_concept/05_mockups/
```
