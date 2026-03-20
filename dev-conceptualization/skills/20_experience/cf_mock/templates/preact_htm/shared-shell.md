---
name: preact-htm-shared-shell
description: Preact functional AppShell component with HTM template syntax
---

# Shared Shell — Preact + HTM

Read `07_screens/00_layout/shell.md` and build a Preact functional component.

## Pattern (`cf__shared/app-shell.js`)

```javascript
// app-shell.js — Preact functional shell component
import { html } from 'htm/preact';
import { useState } from 'preact/hooks';

export function AppShell({ pageTitle, currentPage, children }) {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const navItems = [
    // Generated from screen specs
    { id: 'dashboard', label: 'Dashboard', icon: 'mdi:view-dashboard', href: 'screens/dashboard.html' },
    { id: 'settings', label: 'Settings', icon: 'mdi:cog', href: 'screens/settings.html' },
    // ...
  ];

  return html`
    <div class="flex h-screen bg-brand-bg">
      <!-- Desktop Sidebar -->
      ${sidebarOpen && html`
        <aside class="hidden lg:flex w-64 bg-brand-surface border-r border-brand-border flex-col">
          <div class="p-4 font-heading text-xl font-bold">AppName</div>
          <nav class="flex-1 px-2 space-y-1">
            ${navItems.map(item => html`
              <a href=${item.href}
                 class="flex items-center gap-3 px-4 py-2 rounded-md ${
                   currentPage === item.id
                     ? 'bg-brand-primary/10 text-brand-primary'
                     : 'text-brand-muted hover:bg-brand-surface'
                 }">
                <iconify-icon icon=${item.icon} width="20"></iconify-icon>
                <span>${item.label}</span>
              </a>
            `)}
          </nav>
        </aside>
      `}

      <!-- Main -->
      <div class="flex-1 flex flex-col overflow-hidden">
        <header class="flex items-center gap-3 px-4 py-3 border-b border-brand-border">
          <button class="lg:hidden p-2 rounded hover:bg-brand-surface"
                  onClick=${() => setDrawerOpen(!drawerOpen)}>
            <iconify-icon icon="mdi:menu" width="24"></iconify-icon>
          </button>
          <button class="hidden lg:block p-2 rounded hover:bg-brand-surface"
                  onClick=${() => setSidebarOpen(!sidebarOpen)}>
            <iconify-icon icon="mdi:menu" width="24"></iconify-icon>
          </button>
          <span class="font-heading font-semibold">${pageTitle}</span>
        </header>
        <main class="flex-1 overflow-y-auto p-6">
          ${children}
        </main>
      </div>
    </div>
  `;
}
```

## Shell Includes

- **Desktop sidebar** with navigation links to every screen
- **Header** with sidebar toggle and page title
- **Content area** via `${children}` (Preact children prop)
- **Mobile responsive**: sidebar hidden below `lg:`, hamburger toggles mobile drawer
- Use `<iconify-icon>` for all icons
