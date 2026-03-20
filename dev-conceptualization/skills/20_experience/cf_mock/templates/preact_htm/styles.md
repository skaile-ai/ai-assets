---
name: preact-htm-styles
description: CSS variables and Tailwind config for Preact mockups
---

# Styles — Preact + HTM

## Brand CSS Variables (`cf__shared/styles.css`)

From `tokens.json`, generate CSS custom properties:

```css
:root {
  /* Brand Colors */
  --brand-primary: <colors.primary>;
  --brand-secondary: <colors.secondary>;
  --brand-accent: <colors.accent>;
  --brand-background: <colors.background>;
  --brand-surface: <colors.surface>;
  --brand-text: <colors.text>;
  --brand-text-muted: <colors.text_muted>;
  --brand-border: <colors.border>;
  --brand-error: <colors.error>;
  --brand-success: <colors.success>;
  --brand-warning: <colors.warning>;

  /* Typography */
  --font-heading: '<fonts.heading>', <fonts.heading_fallback>;
  --font-body: '<fonts.body>', <fonts.body_fallback>;
  --font-mono: '<fonts.mono>', monospace;

  /* Spacing (8pt grid) */
  --space-unit: 8px;

  /* Radius */
  --radius-sm: <radius.sm>px;
  --radius-md: <radius.md>px;
  --radius-lg: <radius.lg>px;
}
```

## Tailwind Config

```html
<script>
  tailwind.config = {
    theme: {
      extend: {
        colors: {
          brand: {
            primary: 'var(--brand-primary)',
            secondary: 'var(--brand-secondary)',
            accent: 'var(--brand-accent)',
            bg: 'var(--brand-background)',
            surface: 'var(--brand-surface)',
            text: 'var(--brand-text)',
            muted: 'var(--brand-text-muted)',
            border: 'var(--brand-border)',
          }
        },
        fontFamily: {
          heading: 'var(--font-heading)',
          body: 'var(--font-body)',
        }
      }
    }
  }
</script>
```

## Rules

- **Do NOT invent colors, fonts, or spacing.** Everything must come from brand tokens.
- Preact + HTM uses no component library — all UI is built with Tailwind utilities and native HTML.
- Build custom components (buttons, cards, tables, modals) using Tailwind classes.
- For icons, use `<iconify-icon icon="mdi:icon-name" width="20"></iconify-icon>`.
