---
name: vue-primevue-shared-shell
description: Vue 3 AppShell component with PrimeVue Toolbar and sidebar navigation
---

# Shared Shell — Vue 3 + PrimeVue

Read `07_screens/00_layout/shell.md` and build a Vue 3 shell component.

## Pattern (`cf__shared/app-shell.js`)

```javascript
// app-shell.js — Vue 3 shell component
const AppShell = {
  template: `
    <div class="flex h-screen bg-brand-bg">
      <!-- Sidebar -->
      <aside v-show="sidebarOpen" class="w-64 bg-brand-surface border-r border-brand-border flex flex-col">
        <div class="p-4 font-heading text-xl font-bold">AppName</div>
        <nav class="flex-1 px-2 space-y-1">
          <a v-for="item in navItems" :key="item.id"
             :href="item.href"
             :class="currentPage === item.id
               ? 'bg-[var(--p-primary-color)]/10 text-[var(--p-primary-color)]'
               : 'text-brand-muted hover:bg-brand-surface'"
             class="flex items-center gap-3 px-3 py-2 rounded-md text-sm">
            <iconify-icon :icon="item.icon" width="20"></iconify-icon>
            {{ item.label }}
          </a>
        </nav>
      </aside>

      <!-- Main -->
      <div class="flex-1 flex flex-col overflow-hidden">
        <Toolbar class="border-b border-brand-border">
          <template #start>
            <Button icon="pi pi-bars" text @click="sidebarOpen = !sidebarOpen" />
            <span class="ml-3 font-heading font-semibold">{{ pageTitle }}</span>
          </template>
          <template #end>
            <slot name="toolbar-end" />
          </template>
        </Toolbar>
        <main class="flex-1 overflow-y-auto p-6">
          <slot />
        </main>
      </div>
    </div>
  `,
  props: ['pageTitle', 'currentPage'],
  data() {
    return {
      sidebarOpen: true,
      navItems: [
        // Generated from screen specs
      ]
    };
  }
};
```

## Shell Includes

- **Sidebar** with links to every screen (icons from screen specs)
- **PrimeVue Toolbar** header with sidebar toggle, app name, breadcrumb
- **Main content area** via `<slot />`
- **Mobile responsive**: sidebar toggles via hamburger button
