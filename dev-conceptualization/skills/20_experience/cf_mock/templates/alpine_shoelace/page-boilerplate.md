---
name: alpine-shoelace-page-boilerplate
description: Full HTML page skeleton for Alpine.js + Shoelace mockups
---

# Page Boilerplate — Alpine.js + Shoelace

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
  <!-- Shoelace -->
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2/cdn/themes/light.css" />
  <script type="module" src="https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2/cdn/shoelace-autoloader.js"></script>
  <!-- Tailwind -->
  <script src="https://cdn.tailwindcss.com"></script>
  <script>/* tailwind.config from styles.md */</script>
  <!-- Alpine.js -->
  <script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3/dist/cdn.min.js"></script>
  <!-- Iconify -->
  <script src="https://code.iconify.design/iconify-icon/1.0.7/iconify-icon.min.js"></script>
  <!-- Shared -->
  <script src="../cf__shared/seed.js"></script>
  <script src="../cf__shared/layout.js"></script>
</head>
<body class="bg-brand-bg text-brand-text font-body">
  <div x-data="appShell" class="flex h-screen">
    <!-- Sidebar (shared shell from layout.js) -->
    <aside x-show="sidebarOpen" class="hidden lg:flex w-64 bg-brand-surface border-r border-brand-border flex-col">
      <div class="p-4 font-heading text-xl font-bold">AppName</div>
      <nav class="flex-1 px-2 space-y-1">
        <template x-for="item in navItems">
          <a :href="item.href"
             :class="currentPage === item.id ? 'bg-brand-primary/10 text-brand-primary' : 'text-brand-muted'"
             class="flex items-center gap-3 px-4 py-2 rounded-md hover:bg-brand-surface">
            <iconify-icon :icon="item.icon" width="20"></iconify-icon>
            <span x-text="item.label"></span>
          </a>
        </template>
      </nav>
    </aside>

    <!-- Mobile drawer -->
    <sl-drawer :open="drawerOpen" label="Navigation" placement="start"
               x-on:sl-hide="drawerOpen = false">
      <nav class="space-y-1">
        <template x-for="item in navItems">
          <a :href="item.href" class="flex items-center gap-3 px-4 py-2 rounded-md">
            <iconify-icon :icon="item.icon" width="20"></iconify-icon>
            <span x-text="item.label"></span>
          </a>
        </template>
      </nav>
    </sl-drawer>

    <!-- Main content (screen-specific) -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <header class="flex items-center gap-3 px-4 py-3 border-b border-brand-border">
        <sl-button class="lg:hidden" variant="text" x-on:click="drawerOpen = true">
          <iconify-icon icon="mdi:menu" width="24"></iconify-icon>
        </sl-button>
        <span class="font-heading font-semibold">Page Title</span>
      </header>
      <main class="flex-1 overflow-y-auto p-6">
        <!-- Screen content here, driven by Alpine.js + seed data -->
        <!-- Use Shoelace components: <sl-button>, <sl-input>, <sl-dialog>, <sl-tab-group>, etc. -->
      </main>
    </div>
  </div>
</body>
</html>
```

## Key Points

- All screen pages include the shared shell (sidebar + header) with current page highlighted
- Links between screens use real `<a href>` tags — working navigation
- Seed data rendered with `x-for`, `x-text`, `x-show` from Alpine.js
- Interactive elements use Shoelace: `<sl-dialog>`, `<sl-tab-group>`, `<sl-select>`, `<sl-menu>`
- Responsive: mobile drawer replaces sidebar below `lg:` breakpoint
