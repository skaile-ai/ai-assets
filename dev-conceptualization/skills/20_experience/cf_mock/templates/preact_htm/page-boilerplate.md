---
name: preact-htm-page-boilerplate
description: HTML page skeleton with script type=module and render() for Preact mockups
---

# Page Boilerplate — Preact + HTM

Every screen page follows this skeleton:

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ScreenName — AppName</title>
  <link rel="stylesheet" href="../cf__shared/styles.css">
  <!-- Google Fonts (from tokens.json) -->
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
  <!-- Import Map -->
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
  <!-- Tailwind -->
  <script src="https://cdn.tailwindcss.com"></script>
  <script>/* tailwind.config from styles.md */</script>
  <!-- Iconify -->
  <script src="https://code.iconify.design/iconify-icon/1.0.7/iconify-icon.min.js"></script>
</head>
<body class="bg-brand-bg text-brand-text font-body">
  <div id="app"></div>

  <script type="module">
    import { html, render } from 'htm/preact';
    import { useState, useComputed } from 'preact/hooks';
    import { signal } from '@preact/signals';
    import { AppShell } from '../cf__shared/app-shell.js';
    import { SEED, currentScenario } from '../cf__shared/seed.js';

    function ScreenPage() {
      const data = useComputed(() => SEED[currentScenario.value]);

      return html`
        <${AppShell} pageTitle="Screen Name" currentPage="screen-id">
          <!-- Screen-specific content here -->
        <//>
      `;
    }

    render(html`<${ScreenPage} />`, document.getElementById('app'));
  </script>
</body>
</html>
```

## Key Points

- All logic inside `<script type="module">` — ES module imports, no globals
- HTM uses tagged template literals: `` html`<div>${value}</div>` ``
- Components use Preact hooks: `useState`, `useEffect`, `useComputed`
- `AppShell` imported from shared module
- Seed data accessed via Preact signals for reactive updates
- No build step — import maps resolve all dependencies
