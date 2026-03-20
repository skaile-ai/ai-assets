---
name: alpine-shoelace-shared-shell
description: Alpine.js x-data shell component using Shoelace drawer and navigation
---

# Shared Shell — Alpine.js + Shoelace

Build an Alpine.js component for the app chrome. Read `07_screens/00_layout/shell.md` for layout spec.

## Pattern

```javascript
// layout.js — Alpine.js shell component
document.addEventListener('alpine:init', () => {
  Alpine.data('appShell', () => ({
    sidebarOpen: true,
    drawerOpen: false,
    currentPage: window.location.hash.slice(1) || 'dashboard',
    navItems: [
      // Generated from screen specs — each links to a screen HTML
      { id: 'dashboard', label: 'Dashboard', icon: 'mdi:view-dashboard', href: 'screens/dashboard.html' },
      { id: 'settings', label: 'Settings', icon: 'mdi:cog', href: 'screens/settings.html' },
      // ...
    ],
    navigate(page) {
      this.currentPage = page;
    }
  }));
});
```

## Shell Structure

The shell includes:
- **Desktop sidebar** (`w-64`, visible at `lg:` breakpoint) with links to every screen
- **Mobile drawer** using `<sl-drawer>` with `x-on:sl-hide` to close — hamburger triggers it
- **Header** with `<sl-button>` for menu toggle, app name, breadcrumb, user avatar
- **Main content area** where screen-specific content renders
- Navigation links use `<sl-button variant="text">` or plain `<a>` tags with active highlighting
- Use `<sl-icon>` or `<iconify-icon>` for all icons

## Responsive Behavior

- `lg:` and up → fixed sidebar, no drawer
- Below `lg:` → sidebar hidden, hamburger button opens `<sl-drawer>`
