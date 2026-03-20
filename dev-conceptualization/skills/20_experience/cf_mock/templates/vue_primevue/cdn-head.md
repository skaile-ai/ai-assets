---
name: vue-primevue-cdn-head
description: CDN head block for Vue 3 global + PrimeVue UMD + Tailwind CSS
---

# CDN Head Block — Vue 3 + PrimeVue

Include these CDN resources in every HTML page's `<head>`:

```html
<!-- Vue 3 -->
<script src="https://unpkg.com/vue@3/dist/vue.global.prod.js"></script>

<!-- PrimeVue -->
<link rel="stylesheet" href="https://unpkg.com/primevue/resources/themes/aura-light-noir/theme.css">
<link rel="stylesheet" href="https://unpkg.com/primeicons/primeicons.css">
<script src="https://unpkg.com/primevue/umd/primevue.min.js"></script>

<!-- Tailwind CSS (utility layer) -->
<script src="https://cdn.tailwindcss.com"></script>

<!-- Google Fonts (brand-specific, loaded dynamically from tokens.json) -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>

<!-- Iconify -->
<script src="https://code.iconify.design/iconify-icon/1.0.7/iconify-icon.min.js"></script>
```

## Notes

- Theme: `aura-dark-noir` for dark mode, `aura-light-noir` for light mode (based on `tokens.json` mode).
- No build step. No npm. Every HTML file opens directly in a browser.
- Vue 3 is loaded as a global build (`Vue` global variable).
- PrimeVue is loaded as a UMD bundle (`PrimeVue` global variable).
