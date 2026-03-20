---
name: alpine-shoelace-styles
description: CSS variables, Shoelace --sl-color-* overrides, and Tailwind config for Alpine.js mockups
---

# Styles — Alpine.js + Shoelace

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

  /* Shoelace overrides — map brand to Shoelace design tokens */
  --sl-color-primary-600: <colors.primary>;
  --sl-color-primary-500: <colors.primary>;  /* lighter variant */
  --sl-color-primary-700: <colors.primary>;  /* darker variant */
  --sl-color-danger-600: <colors.error>;
  --sl-color-success-600: <colors.success>;
  --sl-color-warning-600: <colors.warning>;
  --sl-color-neutral-0: <colors.background>;
  --sl-color-neutral-50: <colors.surface>;
  --sl-color-neutral-700: <colors.text>;
  --sl-color-neutral-500: <colors.text_muted>;
  --sl-input-border-color: <colors.border>;
  --sl-panel-background-color: <colors.surface>;
  --sl-font-sans: '<fonts.body>', <fonts.body_fallback>;

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
- Derive Shoelace `--sl-color-*` variants (lighter/darker) from the brand primary using CSS `color-mix()` or manual hex adjustments.
- For dark mode, also switch Shoelace theme CSS to `themes/dark.css` and adjust `--sl-color-neutral-*` accordingly.
