---
name: vue-primevue-styles
description: CSS variables and PrimeVue --p-* overrides for Vue 3 mockups
---

# Styles — Vue 3 + PrimeVue

## Brand CSS Variables (`cf__shared/styles.css`)

Using `tokens.json`, override **all** PrimeVue CSS variables:

```css
:root {
  /* PrimeVue overrides from brand tokens */
  --p-primary-color: <colors.primary>;
  --p-primary-color-text: <colors.primary_text or white>;
  --p-surface-0: <colors.background>;
  --p-surface-50: <derived lighter>;
  --p-surface-card: <colors.surface>;
  --p-content-border-color: <colors.border>;
  --p-text-color: <colors.text>;
  --p-text-muted-color: <colors.text_muted>;

  /* Brand tokens as CSS variables */
  --brand-primary: <colors.primary>;
  --brand-secondary: <colors.secondary>;
  --brand-accent: <colors.accent>;
  --brand-background: <colors.background>;
  --brand-surface: <colors.surface>;
  --brand-text: <colors.text>;
  --brand-error: <colors.error>;
  --brand-success: <colors.success>;

  /* Typography */
  --font-heading: '<fonts.heading>', <fonts.heading_fallback>;
  --font-body: '<fonts.body>', <fonts.body_fallback>;
}

/* Override PrimeVue font */
.p-component { font-family: var(--font-body); }
h1, h2, h3, h4, h5, h6 { font-family: var(--font-heading); }
```

## Tailwind Config

Same pattern as other stacks — extend with brand variables:

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

- **Do NOT use default PrimeVue colors.** Override everything with brand tokens.
- **Do NOT invent colors or fonts.** Every value traces back to `tokens.json`.
- Derive `--p-surface-*` variants from brand background/surface using lightened/darkened shades.
