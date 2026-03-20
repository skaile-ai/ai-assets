---
name: alpine-shoelace-cdn-head
description: CDN head block for Alpine.js 3.x + Shoelace web components + Tailwind CSS
---

# CDN Head Block — Alpine.js + Shoelace

Include these CDN resources in every HTML page's `<head>`:

```html
<!-- Tailwind CSS -->
<script src="https://cdn.tailwindcss.com"></script>

<!-- Alpine.js -->
<script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3/dist/cdn.min.js"></script>

<!-- Shoelace Web Components (autoloader registers all sl-* elements on use) -->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2/cdn/themes/light.css" />
<script type="module" src="https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2/cdn/shoelace-autoloader.js"></script>

<!-- Google Fonts (brand-specific, loaded dynamically from tokens.json) -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>

<!-- Iconify (universal icon library) -->
<script src="https://code.iconify.design/iconify-icon/1.0.7/iconify-icon.min.js"></script>
```

## Notes

- No build step. No npm. Every HTML file is self-contained and opens directly in a browser.
- Shoelace uses the autoloader — just write `<sl-button>`, `<sl-dialog>`, `<sl-drawer>`, etc. and they register automatically.
- For dark mode, add the Shoelace dark theme CSS instead: replace `themes/light.css` with `themes/dark.css`.
- Alpine.js handles page-level reactivity (`x-data`, `x-for`, `x-show`, `x-on:click`).
- Shoelace handles UI primitives (buttons, inputs, dialogs, drawers, tabs, menus, tooltips).
