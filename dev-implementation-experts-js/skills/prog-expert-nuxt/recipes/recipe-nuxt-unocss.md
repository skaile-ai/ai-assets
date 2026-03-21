---
name: Nuxt + UnoCSS
description: UnoCSS setup as a Tailwind alternative with Wind4 preset, custom theme, icons, and typography in Nuxt.
libraries_used: [unocss, "@unocss/nuxt", "@unocss/preset-wind3", "@unocss/preset-typography", "@unocss/reset"]
---

# Nuxt + UnoCSS

UnoCSS is a high-performance atomic CSS engine, used as a Tailwind CSS alternative. Uses the Wind4 preset for Tailwind-compatible utilities.

## 1. Install Dependencies

```bash
pnpm add -D unocss @unocss/nuxt @unocss/preset-wind3 @unocss/preset-typography @unocss/preset-uno
pnpm add @unocss/reset
```

## 2. Nuxt Config

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  modules: ['@unocss/nuxt'],
  css: [
    '@unocss/reset/sanitize/sanitize.css',
    '~/assets/css/main.css',
  ],
});
```

## 3. UnoCSS Config

Create `uno.config.ts` in project root:

```typescript
import {
  defineConfig,
  presetWind4,
  presetAttributify,
  presetIcons,
  presetTypography,
} from 'unocss';

export default defineConfig({
  presets: [
    presetWind4(),          // Tailwind-compatible utilities
    presetAttributify(),    // Attributify mode (class-free HTML attrs)
    presetIcons({ scale: 1.2 }),  // Icon support
    presetTypography(),     // Prose/typography utilities
  ],
  theme: {
    fontFamily: {
      sans: ['Roboto', 'sans-serif'],
    },
    colors: {
      primary: '#1976D2',
      secondary: '#ECEFF1',
      success: '#4CAF50',
      warning: '#FB8C00',
      error: '#D32F2F',
      neutral: {
        primary: '#212121',
        secondary: '#757575',
        background: '#FFFFFF',
        divider: '#BDBDBD',
      },
    },
    fontSize: {
      'title-1': '28pt',
      'title-2': '22pt',
      body: '17pt',
      caption: '13pt',
    },
  },
});
```

## 4. Usage in Templates

```vue
<template>
  <!-- Standard Tailwind-like classes -->
  <div class="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4">
    <div class="flex flex-col p-4 bg-white rounded-lg shadow-md">
      <h1 class="text-title-1 font-bold text-primary">Title</h1>
      <p class="text-body text-neutral-secondary">Content</p>
    </div>
  </div>

  <!-- Attributify mode (optional, cleaner HTML) -->
  <div grid grid-cols-3 gap-4>
    <div flex flex-col p-4 bg-white rounded-lg>
      Content
    </div>
  </div>
</template>
```

## 5. With PrimeVue

UnoCSS works well alongside PrimeVue — use UnoCSS for layout/spacing/typography and PrimeVue for interactive components. No conflict handling needed (unlike Tailwind + PrimeVue which requires `tailwindcss-primeui`).

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  modules: [
    '@unocss/nuxt',           // Layout & utilities
    '@primevue/nuxt-module',  // Components
  ],
});
```

## Key Differences from Tailwind

| Feature | UnoCSS | Tailwind |
|---------|--------|----------|
| Performance | On-demand, faster builds | JIT, slightly heavier |
| Config | `uno.config.ts` | `tailwind.config.ts` |
| Reset | `@unocss/reset` | `@tailwindcss/forms` etc. |
| Icons | Built-in `presetIcons` | Requires plugin |
| Attributify | Built-in preset | Not available |
| PrimeVue compat | No conflicts | Needs `tailwindcss-primeui` |
