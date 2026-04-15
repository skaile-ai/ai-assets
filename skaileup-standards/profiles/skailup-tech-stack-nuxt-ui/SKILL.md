---
name: "skailup-tech-stack-nuxt-ui"
description: "Reference document and invocable skill for the Nuxt 4 + @nuxt/ui v3 + Directus stack. Read by scaffold, foundation, design, mock, and storybook skills when 05_techstack/stack.md selects this stack."
metadata:
  tags:
    - "nuxt"
    - "nuxt-ui"
    - "reka-ui"
    - "radix-vue"
    - "directus"
    - "postgresql"
    - "ssr"
    - "vue3"
    - "tailwind"
    - "bun"
    - "consumer"
    - "branded"
    - "design-forward"
  stage: "alpha"
  requires:
    - "standards-contract"
---

# Tech Stack: Nuxt 4 + @nuxt/ui + Directus

## Overview

Full-stack SSR application built with Nuxt 4 (Vue 3) on the frontend, @nuxt/ui v3 as the component layer (built on Reka UI — the Vue port of Radix UI — with Tailwind CSS 4), and Directus as the headless CMS backend. This stack prioritizes design flexibility and custom branding: @nuxt/ui provides accessible, headless-style primitives that are fully customizable through Tailwind tokens. Best suited for design-forward consumer apps and custom-branded products where PrimeVue's opinionated styling would be a constraint.

## Identity

| Field | Value |
|-------|-------|
| Frontend | Nuxt 4 (Vue 3, Composition API), SSR |
| UI Library | @nuxt/ui v3 (Reka UI + Tailwind CSS 4) |
| Backend | Directus (headless CMS, auto-generated REST + GraphQL API) |
| Database | PostgreSQL |
| Auth | Directus Auth (JWT, refresh tokens, roles, SSO via OAuth2/LDAP) |
| ORM / DB Access | Directus SDK (`@directus/sdk`) — no separate ORM; Directus manages the DB |
| Package Manager | bun |
| CSS Methodology | Tailwind CSS 4 + @nuxt/ui color palette via `app.config.ts` |

## When to Use

- Best for: design-forward apps where brand identity is a primary deliverable
- Best for: consumer-facing products (SaaS front-ends, portals, dashboards) that need polished, accessible UIs
- Best for: teams that want Tailwind-native styling without fighting a component library's opinionated CSS
- Best for: projects where the Nuxt ecosystem (DevTools, modules ecosystem) is a priority
- Avoid when: you need heavy data-grid features (TreeTable, advanced Chart integration) — use nuxt-primevue instead
- Avoid when: the team wants a traditional design system with pre-styled components and no customization overhead

## Scaffold Recipe

```bash
# 1. Initialize Nuxt 4 project
bunx nuxi@latest init . --packageManager bun

# 2. Install @nuxt/ui v3 (includes Tailwind CSS 4 + Reka UI)
bun add @nuxt/ui

# 3. Install Directus SDK
bun add @directus/sdk

# 4. Configure nuxt.config.ts
# modules: ['@nuxt/ui']
# (Tailwind CSS 4 is automatically configured by @nuxt/ui)

# 5. Run dev server
bun run dev
```

`nuxt.config.ts` minimum configuration:
```typescript
export default defineNuxtConfig({
  modules: ['@nuxt/ui'],
  css: ['~/assets/css/main.css'],
})
```

`assets/css/main.css`:
```css
@import "tailwindcss";
@import "@nuxt/ui";
```

## CSS Variables / Theming

@nuxt/ui v3 uses Tailwind CSS 4's CSS-native configuration. Brand tokens from `04_brand/tokens.json` map through two mechanisms:

**Level 1 — `app.config.ts` color palette:**
```typescript
// app.config.ts
export default defineAppConfig({
  ui: {
    colors: {
      primary: 'violet',    // maps to tokens.json color.primary (Tailwind palette name)
      neutral: 'zinc',      // maps to tokens.json color.neutral
    },
  },
})
```

For custom brand colors not in the Tailwind palette, define them in `main.css`:
```css
@import "tailwindcss";
@import "@nuxt/ui";

@theme {
  /* Custom brand primary from tokens.json */
  --color-brand-50: oklch(97% 0.02 270);
  --color-brand-500: oklch(55% 0.18 270);   /* tokens.json color.primary.500 */
  --color-brand-900: oklch(25% 0.10 270);

  /* Typography from tokens.json */
  --font-sans: 'Inter Variable', sans-serif;
  --font-display: 'Cal Sans', sans-serif;

  /* Spacing / radius from tokens.json */
  --radius-sm: 0.25rem;
  --radius-md: 0.5rem;
  --radius-lg: 0.75rem;
}
```

Then reference the custom color in `app.config.ts`:
```typescript
ui: { colors: { primary: 'brand' } }
```

Token mapping table:
| `tokens.json` key | @nuxt/ui target | CSS variable |
|-------------------|----------------|--------------|
| `color.primary.*` | `ui.colors.primary` | `--color-primary-*` |
| `color.neutral.*` | `ui.colors.neutral` | `--color-neutral-*` |
| `typography.fontFamily.sans` | `@theme --font-sans` | `--font-sans` |
| `borderRadius.default` | `@theme --radius-md` | `--radius-md` |
| `color.success` | `ui.colors.success` | `--color-success-*` |
| `color.warning` | `ui.colors.warning` | `--color-warning-*` |
| `color.error` | `ui.colors.error` | `--color-error-*` |

**Level 2 — Component `ui` prop for per-instance overrides:**
Every @nuxt/ui component accepts a `ui` prop with Tailwind classes for slot-level customization. This is the escape hatch for one-off style needs — avoid global overrides.

## Auth Setup

Same Directus JWT auth pattern as nuxt-primevue. @nuxt/ui provides `UModal` and form components that make the login UI straightforward.

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

**Login form pattern using @nuxt/ui:**
```vue
<template>
  <UForm :schema="schema" :state="state" @submit="onSubmit">
    <UFormField label="Email" name="email">
      <UInput v-model="state.email" type="email" />
    </UFormField>
    <UFormField label="Password" name="password">
      <UInput v-model="state.password" type="password" />
    </UFormField>
    <UButton type="submit" :loading="loading">Sign In</UButton>
  </UForm>
</template>
```

## App Shell

@nuxt/ui v3 provides `UApp` as the root wrapper (provides toast, tooltip, and modal contexts), `UNavigationMenu` for nav, and `UDashboardLayout` / `USidebar` for admin-style shells.

**Key files:**
- `layouts/default.vue` — wraps everything in `<UApp>`, composes sidebar + header + `<slot />`
- `components/app/AppSidebar.vue` — uses `UNavigationMenu` with vertical orientation
- `components/app/AppHeader.vue` — uses `UButton`, `UDropdownMenu`, `UAvatar`
- `app.vue` — must include `<UApp>` at root for toast/modal to work globally

**`layouts/default.vue` pattern:**
```vue
<template>
  <UApp>
    <div class="flex h-screen bg-neutral-50 dark:bg-neutral-950">
      <AppSidebar />
      <div class="flex flex-1 flex-col min-w-0">
        <AppHeader />
        <main class="flex-1 overflow-y-auto">
          <div class="container mx-auto px-6 py-8">
            <slot />
          </div>
        </main>
      </div>
    </div>
  </UApp>
</template>
```

**`app.vue` (if not using layouts):**
```vue
<template>
  <UApp>
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </UApp>
</template>
```

## Component Library

| Generic UI concept | @nuxt/ui Component | Import |
|--------------------|--------------------|--------|
| Button | `UButton` | auto-imported |
| DataTable | `UTable` | auto-imported |
| Modal/Dialog | `UModal` | auto-imported |
| Form Input | `UInput` | auto-imported |
| Select/Dropdown | `USelectMenu` | auto-imported |
| Navigation | `UNavigationMenu` | auto-imported |
| Card | `UCard` | auto-imported |
| Toast/Notification | `useToast()` + `UToast` | auto-imported |
| Date Picker | `UDatePicker` (v3) | auto-imported |
| Command Palette | `UCommandPalette` | auto-imported |
| Breadcrumb | `UBreadcrumb` | auto-imported |
| Tabs | `UTabs` | auto-imported |
| Badge | `UBadge` | auto-imported |
| Avatar | `UAvatar` | auto-imported |
| Form (wrapper) | `UForm` | auto-imported |
| Form Field | `UFormField` | auto-imported |
| Slider | `USlider` | auto-imported |

All components are auto-imported by the `@nuxt/ui` Nuxt module — no manual imports needed.

## Mock Adaptation

The mock skill uses CDN-based HTML templates. @nuxt/ui is not available via CDN (it requires Tailwind v4 build pipeline), so the mock uses the **`vue_primevue`** template as the closest visual approximation for Vue-based component UI.

```yaml
mock_template: vue_primevue
```

Note: The CDN mock is a structural approximation only. Actual visual fidelity to @nuxt/ui's styling is achieved in Storybook stories, not in mocks.

## Storybook Config

```yaml
storybook_addon: "@storybook/nuxt"
story_format: Vue SFC
component_import: "@nuxt/ui"
setup_file: .storybook/setup.ts
```

`@storybook/nuxt` provides full Nuxt context (auto-imports, composables, plugins) inside stories. This is preferred over `@storybook/vue3` for @nuxt/ui because the module's auto-import system must be active.

**`.storybook/main.ts`:**
```typescript
export default {
  framework: '@storybook/nuxt',
  stories: ['../components/**/*.stories.ts'],
  addons: ['@storybook/addon-essentials'],
}
```

**Story example:**
```typescript
// components/app/AppButton.stories.ts
import type { Meta, StoryObj } from '@storybook/vue3'

export default {
  title: 'App/Button',
  component: { template: '<UButton v-bind="args" />' },
} satisfies Meta

export const Primary: StoryObj = {
  args: { label: 'Click me', color: 'primary', variant: 'solid' },
}
```

## Migration / ORM

Identical to nuxt-primevue. Directus manages the PostgreSQL schema. Use `directus schema snapshot` and `directus schema apply` for environment promotion.

```bash
# Export schema for version control
npx directus schema snapshot ./schema-snapshot.yaml

# Apply to staging/production
npx directus schema apply ./schema-snapshot.yaml

# Bootstrap fresh environment
npx directus bootstrap
```

For any custom tables outside Directus management, use SQL migration files in `migrations/` run via bun script.

## Codegen

None. The Directus API is auto-generated from the collection schema.

For TypeScript schema types with the Directus SDK:
```typescript
// types/directus.ts
export interface DirectusSchema {
  articles: Article[]
  categories: Category[]
  users: DirectusUser[]
}

// composables/useDirectus.ts
export function useDirectus() {
  const { $directus } = useNuxtApp()
  return $directus as DirectusClient<DirectusSchema>
}
```

## Expert Skills

Which `prog-expert-*` skills to look for:

- `prog-expert-nuxt` — Nuxt 4 routing, SSR hydration, composables, server routes, Nitro config, module authoring
- `prog-expert-directus` — Directus collection setup, permissions model, flows (automation), SDK usage patterns, schema snapshot workflow

## Key Implementation Patterns

**1. `UApp` must wrap the entire application:**
`useToast()`, `useModal()`, and `useOverlay()` composables only work inside a `<UApp>` context. Always place `<UApp>` in `app.vue` or the root layout — never nest multiple `<UApp>` instances.

**2. Form validation via Zod + `UForm`:**
@nuxt/ui's `UForm` accepts a `schema` prop with a Zod schema and automatically maps validation errors to field names. Always use Zod for form validation — do not use manual error state:
```typescript
import { z } from 'zod'
const schema = z.object({
  email: z.string().email('Invalid email'),
  password: z.string().min(8, 'Minimum 8 characters'),
})
```

**3. `UTable` with server-side data:**
@nuxt/ui's `UTable` is a display component, not a full DataGrid. For server-side pagination and sorting, compose `UTable` with `UPagination` manually and manage state in a composable. Do not expect built-in lazy loading.

**4. Dark mode via Tailwind + Nuxt Color Mode:**
@nuxt/ui integrates with `@nuxtjs/color-mode`. The `dark:` Tailwind variant is activated by the `dark` class on `<html>`. Use `useColorMode()` composable to toggle. Brand token CSS vars should have both light and dark values defined in `@theme`.

**5. Slot-based component customization via `ui` prop:**
Every @nuxt/ui component exposes granular slots and a `ui` prop for Tailwind class overrides. Always prefer the `ui` prop over global CSS overrides to avoid specificity issues:
```vue
<UButton :ui="{ base: 'font-bold tracking-wide', leadingIcon: 'text-brand-500' }">
  Custom Button
</UButton>
```
