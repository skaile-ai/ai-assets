# Tokens Schema

## identity.md

### Frontmatter

```yaml
---
mood: "dark editorial with electric accents"
mode: dark
last_updated: YYYY-MM-DD
---
```

Note: no `status` field — removed globally.

### Body Sections

The identity.md body must contain all of these sections in natural language:

1. **Aesthetic direction and reasoning** — why this direction fits the app
2. **Color usage rules** — when to use primary vs secondary vs accent (not just hex values)
3. **Typography hierarchy** — H1 size/weight, H2, body, caption, label
4. **Spacing system** — 8px base grid
5. **Elevation** — shadow depth, glassmorphism rules if applicable
6. **Atmosphere** — background treatment (gradients, textures, grain)
7. **Tone of voice** — for UI text (formal/casual, short/descriptive)
8. **Memorable element** — what makes this brand distinctive
9. **Do's and don'ts** — explicit guardrails for downstream consumers

## tokens.json

Complete structure with all required fields:

```json
{
  "colors": {
    "primary": "#6366f1",
    "secondary": "#0ea5e9",
    "accent": "#f59e0b",
    "background": "#0f172a",
    "surface": "#1e293b",
    "text": "#f8fafc",
    "text_muted": "#94a3b8",
    "border": "#334155",
    "error": "#ef4444",
    "success": "#22c55e",
    "warning": "#f59e0b"
  },
  "fonts": {
    "heading": "Clash Display",
    "body": "DM Sans",
    "mono": "JetBrains Mono"
  },
  "radius": "8px",
  "mode": "dark",
  "spacing_base": "8px",
  "shadows": {
    "sm": "0 1px 2px rgba(0,0,0,0.1)",
    "md": "0 4px 12px rgba(0,0,0,0.15)",
    "lg": "0 10px 40px rgba(0,0,0,0.2)"
  },
  "atmosphere": {
    "type": "radial_gradient",
    "description": "Subtle dark-to-darker radial glow behind main content"
  },
  "tailwind": {
    "--color-primary": "#6366f1",
    "--color-primary-foreground": "#ffffff",
    "--color-secondary": "#0ea5e9",
    "--color-background": "#0f172a",
    "--color-surface": "#1e293b",
    "--color-foreground": "#f8fafc",
    "--color-muted": "#94a3b8",
    "--color-border": "#334155",
    "--color-destructive": "#ef4444",
    "--color-success": "#22c55e",
    "--color-warning": "#f59e0b",
    "--radius": "0.5rem"
  }
}
```

### Required Fields

All of these top-level keys must be present in tokens.json:

- `colors` — all 11 color keys (primary, secondary, accent, background, surface, text, text_muted, border, error, success, warning)
- `fonts` — heading, body, mono
- `radius` — border radius value
- `mode` — "light", "dark", or "both"
- `spacing_base` — base grid unit (typically "8px")
- `shadows` — sm, md, lg
- `atmosphere` — type and description
- `tailwind` — CSS custom properties for Tailwind/CSS theming (all --color-* and --radius)

### Downstream Consumers

These skills read tokens.json:

- `screens` — color and font references in screen specs
- `mock` — live component previews using brand tokens
- `scaffold` — initial CSS custom property setup
- `brand-behavioral` — mood and tone context
