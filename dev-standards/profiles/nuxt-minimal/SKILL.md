---
name: tech-stack-nuxt-minimal
description: "Reference document and invocable skill for the Nuxt 4 + Tailwind + Drizzle + SQLite stack. Read by scaffold, foundation, design, mock, and storybook skills when 05_techstack/stack.md selects this stack."
keywords: [nuxt, tailwind, drizzle, sqlite, nitro, nuxt-auth-utils, bun, minimal, personal, blog, tool, prototype, learning]
metadata:
  stage: alpha
  requires:
  - standards-contract
---

# Tech Stack: Nuxt 4 Minimal (Tailwind + Drizzle + SQLite)

## Overview

Lightweight full-stack application built entirely with Nuxt 4 — no external backend, no managed service. The frontend is Vue 3 with Tailwind CSS 4 (no component library). The backend is Nuxt's built-in Nitro server with Drizzle ORM writing to a local SQLite database. Authentication uses `nuxt-auth-utils` for simple cookie-based sessions. This stack has zero external dependencies at runtime beyond the Node/Bun process — ideal for personal projects, internal tools, blogs, simple utilities, and rapid learning prototypes that don't need enterprise-scale infrastructure.

## Identity

| Field | Value |
|-------|-------|
| Frontend | Nuxt 4 (Vue 3, Composition API), SSR |
| UI Library | Tailwind CSS 4 (no component library — custom components only) |
| Backend | Nitro (built-in Nuxt server — server/api/ routes) |
| Database | SQLite (via Drizzle ORM + `better-sqlite3`) |
| Auth | nuxt-auth-utils (cookie-based sessions, password hashing via `scrypt`) |
| ORM / DB Access | Drizzle ORM + Drizzle Kit |
| Package Manager | bun |
| CSS Methodology | Tailwind CSS 4 with `@theme` CSS custom properties |

## When to Use

- Best for: personal projects, blogs, portfolios, internal tools, simple CRUD apps
- Best for: learning Nuxt / Drizzle without infrastructure overhead
- Best for: rapid prototypes where a deployed SQLite file is sufficient
- Best for: single-developer projects, scripts with a web UI
- Best for: apps that need to run as a single process (Docker-friendly, fly.io, Coolify)
- Avoid when: you expect concurrent writes at scale — SQLite is single-writer; use PostgreSQL (other stacks) instead
- Avoid when: multiple service instances need to share data — SQLite is file-local; use Turso (libSQL) or migrate to PostgreSQL
- Avoid when: you need a rich component library (DataTable, Charts, etc.) — there is no component library here; build everything from scratch or switch stacks

## Scaffold Recipe

```bash
# 1. Initialize Nuxt 4 project
bunx nuxi@latest init . --packageManager bun

# 2. Install Tailwind CSS 4 via Nuxt module
bun add -d @nuxtjs/tailwindcss

# 3. Install Drizzle ORM + SQLite driver
bun add drizzle-orm better-sqlite3
bun add -d drizzle-kit @types/better-sqlite3

# 4. Install nuxt-auth-utils
bun add nuxt-auth-utils

# 5. Configure nuxt.config.ts
# modules: ['@nuxtjs/tailwindcss', 'nuxt-auth-utils']

# 6. Create database schema
# server/db/schema.ts

# 7. Run first migration
bun drizzle-kit generate
bun drizzle-kit migrate

# 8. Run dev server
bun run dev
```

`nuxt.config.ts`:
```typescript
export default defineNuxtConfig({
  modules: ['@nuxtjs/tailwindcss', 'nuxt-auth-utils'],
  runtimeConfig: {
    sessionPassword: process.env.NUXT_SESSION_PASSWORD,  // min 32 chars
    dbPath: process.env.DB_PATH ?? './data/app.db',
  },
})
```

`.env`:
```bash
NUXT_SESSION_PASSWORD=replace-with-32-char-random-secret-string
DB_PATH=./data/app.db
```

`drizzle.config.ts`:
```typescript
import { defineConfig } from 'drizzle-kit'

export default defineConfig({
  schema: './server/db/schema.ts',
  out: './server/db/migrations',
  dialect: 'sqlite',
  dbCredentials: {
    url: process.env.DB_PATH ?? './data/app.db',
  },
})
```

## CSS Variables / Theming

Pure Tailwind CSS 4 with no component library. Brand tokens from `04_brand/tokens.json` map directly to CSS custom properties in the `@theme` block.

**`assets/css/tailwind.css`:**
```css
@import "tailwindcss";

@theme {
  /* Colors from tokens.json */
  --color-primary-50: {tokens.color.primary.50};
  --color-primary-100: {tokens.color.primary.100};
  --color-primary-500: {tokens.color.primary.500};
  --color-primary-600: {tokens.color.primary.600};
  --color-primary-700: {tokens.color.primary.700};
  --color-primary-900: {tokens.color.primary.900};

  --color-neutral-50: {tokens.color.neutral.50};
  --color-neutral-100: {tokens.color.neutral.100};
  --color-neutral-500: {tokens.color.neutral.500};
  --color-neutral-900: {tokens.color.neutral.900};

  --color-success: {tokens.color.success};
  --color-warning: {tokens.color.warning};
  --color-error: {tokens.color.error};

  /* Typography from tokens.json */
  --font-sans: {tokens.typography.fontFamily.sans};
  --font-mono: {tokens.typography.fontFamily.mono};
  --text-base: {tokens.typography.fontSize.base};

  /* Border radius from tokens.json */
  --radius-sm: {tokens.borderRadius.sm};
  --radius-md: {tokens.borderRadius.md};
  --radius-lg: {tokens.borderRadius.lg};

  /* Shadows from tokens.json */
  --shadow-sm: {tokens.shadow.sm};
  --shadow-md: {tokens.shadow.md};
}

/* Dark mode semantic aliases */
:root {
  --bg: var(--color-white);
  --bg-raised: var(--color-neutral-50);
  --fg: var(--color-neutral-900);
  --fg-muted: var(--color-neutral-500);
  --border: var(--color-neutral-200);
}

.dark {
  --bg: var(--color-neutral-950);
  --bg-raised: var(--color-neutral-900);
  --fg: var(--color-neutral-50);
  --fg-muted: var(--color-neutral-400);
  --border: var(--color-neutral-800);
}
```

Token mapping table:
| `tokens.json` key | CSS custom property | Tailwind class |
|-------------------|--------------------|-|
| `color.primary.*` | `--color-primary-*` | `bg-primary-500` |
| `typography.fontFamily.sans` | `--font-sans` | `font-sans` |
| `borderRadius.md` | `--radius-md` | `rounded-md` |
| `color.error` | `--color-error` | `text-error` |

Since there is no component library, define a small set of reusable component classes using Tailwind `@layer components`:
```css
@layer components {
  .btn-primary {
    @apply inline-flex items-center px-4 py-2 bg-primary-500 text-white rounded-md
           hover:bg-primary-600 focus-visible:outline-none focus-visible:ring-2
           focus-visible:ring-primary-500 focus-visible:ring-offset-2 transition-colors;
  }
  .card {
    @apply bg-bg-raised border border-border rounded-lg shadow-sm p-6;
  }
  .input {
    @apply w-full rounded-md border border-border bg-bg px-3 py-2 text-sm
           focus:outline-none focus:ring-2 focus:ring-primary-500;
  }
}
```

## Auth Setup

`nuxt-auth-utils` provides server-side session management using sealed cookies (encrypted with `NUXT_SESSION_PASSWORD`). No JWT, no external auth provider — sessions live in cookies.

**`server/utils/auth.ts`:**
```typescript
import { scrypt, randomBytes, timingSafeEqual } from 'node:crypto'
import { promisify } from 'node:util'

const scryptAsync = promisify(scrypt)

export async function hashPassword(password: string): Promise<string> {
  const salt = randomBytes(16).toString('hex')
  const derivedKey = await scryptAsync(password, salt, 64) as Buffer
  return `${salt}:${derivedKey.toString('hex')}`
}

export async function verifyPassword(password: string, hash: string): Promise<boolean> {
  const [salt, storedKey] = hash.split(':')
  const derivedKey = await scryptAsync(password, salt, 64) as Buffer
  const storedBuffer = Buffer.from(storedKey, 'hex')
  return timingSafeEqual(derivedKey, storedBuffer)
}
```

**`server/api/auth/login.post.ts`:**
```typescript
import { getUserByEmail } from '~/server/db/queries/users'
import { verifyPassword } from '~/server/utils/auth'

export default defineEventHandler(async (event) => {
  const { email, password } = await readBody(event)
  const user = await getUserByEmail(email)
  if (!user || !(await verifyPassword(password, user.passwordHash))) {
    throw createError({ statusCode: 401, message: 'Invalid credentials' })
  }
  await setUserSession(event, {
    user: { id: user.id, email: user.email, name: user.name },
  })
  return { success: true }
})
```

**`server/api/auth/logout.post.ts`:**
```typescript
export default defineEventHandler(async (event) => {
  await clearUserSession(event)
  return { success: true }
})
```

**`middleware/auth.ts`** (Nuxt route middleware):
```typescript
export default defineNuxtRouteMiddleware(async (to) => {
  const { loggedIn } = useUserSession()
  if (!loggedIn.value && to.meta.requiresAuth) {
    return navigateTo('/login')
  }
})
```

Apply to pages with `definePageMeta({ middleware: 'auth', requiresAuth: true })`.

## App Shell

Nuxt 4 layouts are minimal for this stack — typically a single `layouts/default.vue` with a simple header and content area.

**Key files:**
- `layouts/default.vue` — header + main + optional footer
- `components/app/AppHeader.vue` — site name, nav links, user menu (if logged in)
- `pages/index.vue`, `pages/login.vue`, `pages/app/[...].vue`

**`layouts/default.vue` pattern:**
```vue
<template>
  <div class="min-h-screen flex flex-col bg-bg text-fg">
    <AppHeader />
    <main class="flex-1 container mx-auto px-4 py-8 max-w-5xl">
      <slot />
    </main>
    <footer class="border-t border-border py-6 text-center text-sm text-fg-muted">
      &copy; {{ new Date().getFullYear() }} {{ appName }}
    </footer>
  </div>
</template>
```

For apps with a sidebar (admin-style), create `layouts/dashboard.vue` with a flex row layout and apply it via `definePageMeta({ layout: 'dashboard' })`.

## Component Library

There is no external component library. Build a small set of reusable Vue components in `components/ui/`:

| Generic UI concept | Custom component | Notes |
|--------------------|-----------------|-------|
| Button | `components/ui/UiButton.vue` | Accepts `variant`, `size`, `loading` props |
| DataTable | `components/ui/UiTable.vue` | Simple `<table>` wrapper with slot columns |
| Modal/Dialog | `components/ui/UiDialog.vue` | Use HTML `<dialog>` element + Tailwind |
| Form Input | `components/ui/UiInput.vue` | Input + label + error message |
| Select/Dropdown | `components/ui/UiSelect.vue` | Native `<select>` styled with Tailwind |
| Navigation | `components/app/AppNav.vue` | NuxtLink list with active class |
| Card | `components/ui/UiCard.vue` | `<div class="card">` wrapper with slots |
| Toast/Notification | `composables/useToast.ts` | Custom composable + `<TransitionGroup>` |

> Keep this component set small. If component complexity grows significantly, consider switching to nuxt-ui or nuxt-primevue instead.

## Mock Adaptation

The mock skill uses CDN-based HTML templates. This stack's Tailwind-only styling is closest to **`alpine_shoelace`** — both are minimal and rely on utility classes rather than a heavy component library.

```yaml
mock_template: alpine_shoelace
```

Note: The mock uses Shoelace components as placeholders. The actual implementation replaces Shoelace with custom Vue components styled with Tailwind. Structural layout intent is preserved.

## Storybook Config

```yaml
storybook_addon: "@storybook/vue3"
story_format: Vue SFC
component_import: "@/components/ui"
setup_file: .storybook/setup.ts
```

**`.storybook/main.ts`:**
```typescript
export default {
  framework: '@storybook/vue3-vite',
  stories: ['../components/**/*.stories.ts'],
  addons: ['@storybook/addon-essentials'],
}
```

**`.storybook/preview.ts`:**
```typescript
import '../assets/css/tailwind.css'  // brand tokens via @theme
import type { Preview } from '@storybook/vue3'

export default {
  parameters: { backgrounds: { default: 'light' } },
} satisfies Preview
```

Write stories for `components/ui/` components only. Server-side composables and Nitro API routes are not testable in Storybook.

## Migration / ORM

Drizzle ORM with Drizzle Kit manages SQLite migrations.

**`server/db/schema.ts`:**
```typescript
import { sqliteTable, text, integer } from 'drizzle-orm/sqlite-core'

export const users = sqliteTable('users', {
  id: text('id').primaryKey().$defaultFn(() => crypto.randomUUID()),
  email: text('email').notNull().unique(),
  name: text('name').notNull(),
  passwordHash: text('password_hash').notNull(),
  createdAt: integer('created_at', { mode: 'timestamp' })
    .$defaultFn(() => new Date()).notNull(),
  updatedAt: integer('updated_at', { mode: 'timestamp' })
    .$defaultFn(() => new Date()).notNull(),
})
```

**`server/db/index.ts`:**
```typescript
import { drizzle } from 'drizzle-orm/better-sqlite3'
import Database from 'better-sqlite3'
import * as schema from './schema'

const sqlite = new Database(process.env.DB_PATH ?? './data/app.db')
export const db = drizzle(sqlite, { schema })
```

**Migration commands:**
```bash
# Generate migration SQL from schema changes
bun drizzle-kit generate

# Apply pending migrations
bun drizzle-kit migrate

# Inspect current schema
bun drizzle-kit introspect

# Open Drizzle Studio (browser DB viewer)
bun drizzle-kit studio
```

**Migration files** are stored in `server/db/migrations/` and committed to version control. Never edit migration files after they have been applied.

## Codegen

None. Drizzle ORM generates TypeScript types directly from the schema file — no codegen step required. Types are inferred at compile time:

```typescript
import type { InferSelectModel, InferInsertModel } from 'drizzle-orm'
import { users } from '~/server/db/schema'

type User = InferSelectModel<typeof users>
type NewUser = InferInsertModel<typeof users>
```

## Expert Skills

Which `prog-expert-*` skills to look for:

- `prog-expert-nuxt` — Nuxt 4 routing, SSR, server routes (Nitro), composables, auto-imports, Nuxt DevTools

## Key Implementation Patterns

**1. Nitro server routes for all API logic:**
All backend logic lives in `server/api/`. Use the file-based routing convention: `server/api/posts/index.get.ts`, `server/api/posts/[id].put.ts`. Access the Drizzle `db` instance via a server utility — never instantiate it per-request:
```typescript
// server/api/posts/index.get.ts
export default defineEventHandler(async () => {
  return db.select().from(posts).orderBy(desc(posts.createdAt))
})
```

**2. Session validation in every protected server route:**
Use `requireUserSession()` from `nuxt-auth-utils` at the top of every protected API handler. It throws 401 automatically if no valid session exists:
```typescript
export default defineEventHandler(async (event) => {
  const { user } = await requireUserSession(event)
  return db.select().from(posts).where(eq(posts.authorId, user.id))
})
```

**3. SQLite WAL mode for better concurrency:**
Enable Write-Ahead Logging immediately after opening the database connection. This is critical for any app with concurrent reads and writes:
```typescript
// server/db/index.ts
const sqlite = new Database(process.env.DB_PATH ?? './data/app.db')
sqlite.pragma('journal_mode = WAL')
sqlite.pragma('foreign_keys = ON')
export const db = drizzle(sqlite, { schema })
```

**4. Keep data directory outside the project for Docker deployments:**
Mount `DB_PATH` as a Docker volume. Use an absolute path or a path outside the app directory to avoid losing data on container rebuild:
```dockerfile
VOLUME ["/data"]
ENV DB_PATH=/data/app.db
```

**5. Use `$fetch` for client-side API calls, `useFetch` for SSR:**
In Vue templates/composables: use `useFetch` (SSR-safe, cached) for data that needs to render on the server. Use `$fetch` (direct, no cache) for mutations and client-only interactions. Never mix `fetch()` directly with Nuxt's hydration system.
