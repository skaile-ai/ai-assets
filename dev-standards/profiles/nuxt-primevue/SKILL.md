---
name: tech-stack-nuxt-primevue
description: "Reference document and invocable skill for the Nuxt 4 + PrimeVue 4 + Directus stack. Read by scaffold, foundation, design, mock, and storybook skills when 05_techstack/stack.md selects this stack."
keywords: [nuxt, primevue, directus, postgresql, ssr, vue3, tailwind, bun, saas, dashboard, admin, crm]
---

# Tech Stack: Nuxt 4 + PrimeVue + Directus

## Overview

Full-stack SSR application built with Nuxt 4 (Vue 3) on the frontend, PrimeVue 4 as the component library, and Directus as the headless CMS backend. Directus auto-generates a full REST + GraphQL API from your data model, eliminating the need for hand-written backend code in most cases. Best suited for data-heavy applications where rich table, form, and data visualization components are needed out of the box.

## Identity

| Field | Value |
|-------|-------|
| Frontend | Nuxt 4 (Vue 3, Composition API), SSR |
| UI Library | PrimeVue 4 + @primevue/themes |
| Backend | Directus (headless CMS, auto-generated REST + GraphQL API) |
| Database | PostgreSQL |
| Auth | Directus Auth (JWT, refresh tokens, roles, SSO via OAuth2/LDAP) |
| ORM / DB Access | Directus SDK (`@directus/sdk`) — no separate ORM; Directus manages the DB |
| Package Manager | bun |
| CSS Methodology | Tailwind CSS 4 + PrimeVue design tokens (`--p-*` CSS variables) |

## When to Use

- Best for: data-heavy apps, admin panels, CRM systems, SaaS dashboards, back-office tools, content management UIs
- Best for: teams that want a rich component library (DataTable, TreeTable, Charts, Scheduler) without building from scratch
- Best for: projects where the backend API schema is driven by the data model and needs no custom business logic beyond Directus flows
- Avoid when: you need a minimal, highly custom visual design — PrimeVue's default styling is opinionated and overriding it deeply is costly
- Avoid when: the app is primarily a marketing site or content-only page — overkill
- Avoid when: your team has no Vue/Nuxt experience and a React stack is viable

## Scaffold Recipe

```bash
# 1. Initialize Nuxt 4 project
bunx nuxi@latest init . --packageManager bun

# 2. Install PrimeVue 4 and themes
bun add primevue @primevue/themes

# 3. Install Tailwind CSS 4 via Nuxt module
bun add -d @nuxtjs/tailwindcss

# 4. Install Directus SDK
bun add @directus/sdk

# 5. Install Nuxt PrimeVue module
bun add @primevue/nuxt-module

# 6. Configure nuxt.config.ts
# modules: ['@primevue/nuxt-module', '@nuxtjs/tailwindcss']
# primevue: { options: { theme: { preset: YourPreset } } }

# 7. Run dev server
bun run dev
```

`nuxt.config.ts` minimum configuration:
```typescript
import { defineNuxtConfig } from 'nuxt/config'
import Aura from '@primevue/themes/aura'

export default defineNuxtConfig({
  modules: ['@primevue/nuxt-module', '@nuxtjs/tailwindcss'],
  primevue: {
    options: {
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: '.dark',
          cssLayer: {
            name: 'primevue',
            order: 'tailwind-base, primevue, tailwind-utilities',
          },
        },
      },
    },
  },
})
```

## CSS Variables / Theming

PrimeVue 4 uses a `--p-*` CSS variable system tied to its preset (Aura, Lara, Material, Nora). Brand tokens from `04_brand/tokens.json` are mapped at two levels:

**Level 1 — PrimeVue preset override via `definePreset()`:**
```typescript
// plugins/primevue-theme.ts (or inline in nuxt.config.ts)
import { definePreset } from '@primevue/themes'
import Aura from '@primevue/themes/aura'

const BrandPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{tokens.color.primary.50}',
      500: '{tokens.color.primary.500}',   // from 04_brand/tokens.json
      900: '{tokens.color.primary.900}',
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          ground: '{tokens.color.surface.ground}',
        },
      },
    },
  },
})
```

**Level 2 — Tailwind @theme block in `assets/css/tailwind.css`:**
```css
@import "tailwindcss";

@theme {
  --color-brand-primary: var(--p-primary-500);
  --color-brand-surface: var(--p-surface-ground);
  --font-sans: 'Inter Variable', sans-serif;  /* from tokens.json typography */
  --radius-md: 0.5rem;                        /* from tokens.json borderRadius */
}
```

Token mapping table:
| `tokens.json` key | PrimeVue target | Tailwind target |
|-------------------|----------------|----------------|
| `color.primary.*` | `--p-primary-*` | `--color-primary-*` |
| `color.surface.*` | `--p-surface-*` | `--color-surface-*` |
| `typography.fontFamily` | N/A | `--font-sans` |
| `borderRadius.default` | `--p-border-radius-md` | `--radius-md` |
| `shadow.default` | `--p-shadow-md` | `--shadow-md` |

## Auth Setup

Directus Auth uses JWT with refresh token rotation. No separate auth library needed on the frontend — the Directus SDK handles sessions.

**`plugins/directus.ts`:**
```typescript
import { createDirectus, authentication, rest } from '@directus/sdk'

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const directus = createDirectus(config.public.directusUrl)
    .with(authentication('cookie', { credentials: 'include' }))
    .with(rest())

  return { provide: { directus } }
})
```

**`middleware/auth.ts`:**
```typescript
export default defineNuxtRouteMiddleware(async (to) => {
  const { $directus } = useNuxtApp()
  try {
    await $directus.refresh()
  } catch {
    if (to.meta.requiresAuth) {
      return navigateTo('/login')
    }
  }
})
```

**`nuxt.config.ts` runtime config:**
```typescript
runtimeConfig: {
  public: {
    directusUrl: process.env.DIRECTUS_URL ?? 'http://localhost:8055',
  },
},
```

**Docker Compose for local Directus:**
```yaml
# docker-compose.yml
services:
  directus:
    image: directus/directus:latest
    ports: ['8055:8055']
    environment:
      SECRET: 'replace-with-secure-secret'
      ADMIN_EMAIL: 'admin@example.com'
      ADMIN_PASSWORD: 'admin'
      DB_CLIENT: pg
      DB_HOST: postgres
      DB_PORT: 5432
      DB_DATABASE: directus
      DB_USER: directus
      DB_PASSWORD: directus
  postgres:
    image: postgres:16
    environment:
      POSTGRES_USER: directus
      POSTGRES_PASSWORD: directus
      POSTGRES_DB: directus
```

## App Shell

Nuxt 4 uses `layouts/` for persistent shell structure.

**Key files:**
- `layouts/default.vue` — root layout, composes sidebar + header + slot
- `components/app/AppSidebar.vue` — collapsible sidebar with PrimeVue `PanelMenu` or `Menu`
- `components/app/AppHeader.vue` — top bar with breadcrumb + user menu
- `components/app/AppNav.vue` — navigation link list (data-driven from route config)

**`layouts/default.vue` pattern:**
```vue
<template>
  <div class="flex h-screen overflow-hidden">
    <AppSidebar :collapsed="sidebarCollapsed" />
    <div class="flex flex-1 flex-col overflow-hidden">
      <AppHeader @toggle-sidebar="sidebarCollapsed = !sidebarCollapsed" />
      <main class="flex-1 overflow-y-auto p-6">
        <slot />
      </main>
    </div>
  </div>
</template>
```

**Route-based page guard:** Add `definePageMeta({ middleware: 'auth' })` to protected pages.

## Component Library

| Generic UI concept | PrimeVue Component | Import |
|--------------------|--------------------|--------|
| Button | `Button` | `primevue/button` |
| DataTable | `DataTable` + `Column` | `primevue/datatable` |
| Modal/Dialog | `Dialog` | `primevue/dialog` |
| Form Input | `InputText`, `InputNumber` | `primevue/inputtext` |
| Select/Dropdown | `Select` (v4) | `primevue/select` |
| Navigation | `PanelMenu`, `Menubar`, `Menu` | `primevue/panelmenu` |
| Card | `Card` | `primevue/card` |
| Toast/Notification | `Toast` + `useToast()` | `primevue/toast` |
| Date Picker | `DatePicker` (v4) | `primevue/datepicker` |
| File Upload | `FileUpload` | `primevue/fileupload` |
| Chart | `Chart` (Chart.js wrapper) | `primevue/chart` |
| Tree Table | `TreeTable` | `primevue/treetable` |
| Tabs | `Tabs`, `TabList`, `Tab`, `TabPanels` | `primevue/tabs` |
| Breadcrumb | `Breadcrumb` | `primevue/breadcrumb` |

> Note: PrimeVue 4 renamed several components. `Dropdown` → `Select`, `Calendar` → `DatePicker`. Always use v4 names.

Auto-import is enabled via `@primevue/nuxt-module` — no manual imports needed in SFCs.

## Mock Adaptation

The mock skill uses CDN-based HTML templates. This stack uses the **`vue_primevue`** template, which loads PrimeVue via CDN and applies the default Aura theme. This accurately reflects the PrimeVue component visual style.

```yaml
mock_template: vue_primevue
```

Note: The CDN mock uses the default Aura preset. Brand token customization (the `definePreset()` override) is not applied in mocks — the real Storybook stories reflect actual brand tokens.

## Storybook Config

```yaml
storybook_addon: "@storybook/vue3"
story_format: Vue SFC
component_import: primevue/button
setup_file: .storybook/setup.ts
```

**`.storybook/setup.ts`:**
```typescript
import { setup } from '@storybook/vue3'
import PrimeVue from 'primevue/config'
import Aura from '@primevue/themes/aura'
import { BrandPreset } from '../plugins/primevue-theme'

setup((app) => {
  app.use(PrimeVue, {
    theme: { preset: BrandPreset },
  })
})
```

**`.storybook/main.ts`:**
```typescript
export default {
  framework: '@storybook/vue3-vite',
  stories: ['../components/**/*.stories.ts'],
  addons: ['@storybook/addon-essentials'],
}
```

## Migration / ORM

Directus manages the PostgreSQL schema directly. There is no separate migration tool for core application tables.

**Schema workflow:**
```bash
# Export current schema snapshot
npx directus schema snapshot ./schema-snapshot.yaml

# Apply snapshot to another environment
npx directus schema apply ./schema-snapshot.yaml

# Bootstrap Directus on a fresh DB
npx directus bootstrap
```

**Collections (tables) are created via:**
1. Directus Admin UI (`/admin/settings/data-model`) — interactive
2. Directus CLI / schema apply — for CI/CD automation
3. Directus SDK `createCollection()` — for programmatic setup scripts

For any tables that Directus does NOT manage (e.g., custom audit logs), use raw SQL migration files in `migrations/` and run with a bun script:
```typescript
// scripts/migrate.ts
import { sql } from './db'
await sql.file('migrations/001_custom_audit.sql')
```

## Codegen

None. The Directus REST and GraphQL APIs are auto-generated from the collection schema. No codegen step is required.

For TypeScript type safety with the Directus SDK, define your schema types manually or use community tools:

```typescript
// types/directus.ts — define schema for SDK type inference
interface DirectusSchema {
  posts: Post[]
  authors: Author[]
}

const directus = createDirectus<DirectusSchema>('http://localhost:8055')
  .with(rest())
```

## Expert Skills

Which `prog-expert-*` skills to look for:

- `prog-expert-nuxt` — Nuxt 4 routing, SSR hydration, composables, server routes, Nitro config
- `prog-expert-primevue` — PrimeVue 4 component recipes, preset customization, DataTable patterns, form validation with Vee-Validate
- `prog-expert-directus` — Directus collection setup, permissions, flows (automation), SDK usage patterns, schema snapshot workflow

## Key Implementation Patterns

**1. Directus SDK composable for data fetching:**
Always wrap SDK calls in a composable using `useAsyncData` for SSR-safe data fetching and caching:
```typescript
// composables/useDirectusFetch.ts
export function useCollection<T>(collection: string, query?: Query) {
  const { $directus } = useNuxtApp()
  return useAsyncData(collection, () =>
    $directus.request(readItems(collection, query))
  )
}
```

**2. PrimeVue DataTable with lazy loading:**
For large datasets always use `lazy` mode with server-side pagination. Never load all records into client memory:
```vue
<DataTable :value="rows" lazy :totalRecords="total"
           @page="onPage" @sort="onSort" @filter="onFilter">
```

**3. Directus permissions as the authorization layer:**
Do not implement authorization in Nuxt middleware for data access — use Directus role-based permissions. The Nuxt middleware only gates page navigation. Data access control lives in Directus.

**4. Form validation with PrimeVue + Vee-Validate:**
PrimeVue 4 integrates natively with Vee-Validate via the `Form` component. Use the `@primevue/forms` resolver adapter:
```typescript
import { zodResolver } from '@primevue/forms/resolvers/zod'
const resolver = zodResolver(mySchema)
```

**5. Toast service must be globally available:**
Add `<Toast />` once in `layouts/default.vue`. Use `useToast()` composable anywhere in the app. Do not add multiple `<Toast />` instances.
