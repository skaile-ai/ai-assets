---
name: preact-htm-cdn-head
description: Import map (esm.sh) for Preact + HTM + hooks + signals
---

# CDN Head Block — Preact + HTM

Use ES module imports via import maps — no bundler, no build step.

```html
<!-- Import Map for Preact ecosystem -->
<script type="importmap">
{
  "imports": {
    "preact": "https://esm.sh/preact@10",
    "preact/": "https://esm.sh/preact@10/",
    "preact/hooks": "https://esm.sh/preact@10/hooks",
    "htm/preact": "https://esm.sh/htm@3/preact",
    "@preact/signals": "https://esm.sh/@preact/signals@1"
  }
}
</script>

<!-- Tailwind CSS -->
<script src="https://cdn.tailwindcss.com"></script>

<!-- Google Fonts (brand-specific, loaded dynamically from tokens.json) -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>

<!-- Iconify -->
<script src="https://code.iconify.design/iconify-icon/1.0.7/iconify-icon.min.js"></script>
```

## Notes

- All component code goes inside `<script type="module">` blocks.
- HTM provides JSX-like syntax using tagged template literals — no transpiler needed.
- Preact Signals provide fine-grained reactivity (like Vue refs but lighter).
- No build step. No npm. Every HTML file opens directly in a browser.
- Import map must appear before any `<script type="module">`.
