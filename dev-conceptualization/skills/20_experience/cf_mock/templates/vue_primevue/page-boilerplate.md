---
name: vue-primevue-page-boilerplate
description: HTML page skeleton with Vue mount and setupPrimeVue() for PrimeVue mockups
---

# Page Boilerplate — Vue 3 + PrimeVue

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
  <!-- PrimeVue Theme -->
  <link rel="stylesheet" href="https://unpkg.com/primevue/resources/themes/aura-light-noir/theme.css">
  <link rel="stylesheet" href="https://unpkg.com/primeicons/primeicons.css">
  <!-- Tailwind -->
  <script src="https://cdn.tailwindcss.com"></script>
  <script>/* tailwind.config from styles.md */</script>
</head>
<body>
  <div id="app">
    <app-shell page-title="Screen Name" current-page="screen-id">
      <!-- Screen-specific content using PrimeVue components -->
    </app-shell>
  </div>

  <!-- Scripts at bottom (order matters) -->
  <script src="https://unpkg.com/vue@3/dist/vue.global.prod.js"></script>
  <script src="https://unpkg.com/primevue/umd/primevue.min.js"></script>
  <script src="https://code.iconify.design/iconify-icon/1.0.7/iconify-icon.min.js"></script>
  <script src="../cf__shared/seed.js"></script>
  <script src="../cf__shared/primevue-setup.js"></script>
  <script src="../cf__shared/app-shell.js"></script>
  <script>
    const { createApp, ref, reactive, computed } = Vue;
    const app = createApp({
      components: { AppShell },
      setup() {
        const scenario = CURRENT_SCENARIO;
        const data = computed(() => SEED[scenario.value]);
        return { data, scenario };
      }
    });
    setupPrimeVue(app);
    app.mount('#app');
  </script>
</body>
</html>
```

## Key Points

- Scripts loaded at bottom in correct order: Vue → PrimeVue → Iconify → shared modules → app init
- `setupPrimeVue(app)` registers all needed PrimeVue components
- `AppShell` provides the shared chrome (sidebar, header, navigation)
- Seed data accessed via `data` computed property that reacts to scenario changes
- All PrimeVue components available: DataTable, Column, Dialog, InputText, Dropdown, Toolbar, Panel, Toast, etc.
