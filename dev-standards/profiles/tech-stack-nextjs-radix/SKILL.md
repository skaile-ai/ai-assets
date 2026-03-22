---
name: "tech-stack-nextjs-radix"
description: "Reference document and invocable skill for the Next.js 15 + Radix UI + Directus stack. Read by scaffold, foundation, design, mock, and storybook skills when 05_techstack/stack.md selects this stack."
metadata:
  tags:
    - "nextjs"
    - "next15"
    - "radix-ui"
    - "directus"
    - "postgresql"
    - "react"
    - "rsc"
    - "app-router"
    - "tailwind"
    - "pnpm"
    - "accessible"
    - "enterprise"
    - "vercel"
  stage: "alpha"
  requires:
    - "standards-contract"
---

# Tech Stack: Next.js 15 + Radix UI + Directus

## Overview

Full-stack application built with Next.js 15 (App Router, React Server Components) on the frontend, Radix UI primitives composed with Tailwind CSS 4 for the component layer, and Directus as the headless CMS backend. This stack gives React teams maximum control over component design — Radix provides accessibility and interaction primitives with zero visual opinions, while Tailwind handles all styling. Best suited for large-scale React applications where team familiarity with the React ecosystem, Vercel deployment, and accessible UI primitives outweigh the convenience of a pre-styled component library.

## Identity

| Field | Value |
|-------|-------|
| Frontend | Next.js 15 (App Router, React Server Components), SSR + SSG |
| UI Library | Radix UI primitives + Tailwind CSS 4 |
| Backend | Directus (headless CMS, auto-generated REST + GraphQL API) |
| Database | PostgreSQL |
| Auth | NextAuth.js v5 (Auth.js) with Directus provider |
| ORM / DB Access | Directus SDK (`@directus/sdk`) — no separate ORM; Directus manages the DB. Prisma optional for custom entities |
| Package Manager | pnpm |
| CSS Methodology | Tailwind CSS 4 + CSS custom properties for brand tokens |

## When to Use

- Best for: React teams building large-scale applications
- Best for: Vercel-native deployments with ISR, edge functions, and streaming
- Best for: Applications requiring strong accessibility guarantees (Radix UI is WCAG 2.1 AA by default)
- Best for: Projects where design system ownership matters — components are built, not adopted
- Best for: Teams using TypeScript-first patterns with React Server Components
- Avoid when: the team is Vue/Nuxt-native — context switch cost is high
- Avoid when: you need a full pre-styled component library with minimal customization budget
- Avoid when: the project is a simple CRUD admin panel — nuxt-primevue is faster to ship

## Scaffold Recipe

```bash
# 1. Initialize Next.js 15 with App Router, TypeScript, Tailwind
npx create-next-app@latest . --typescript --tailwind --app --src-dir --import-alias "@/*"

# 2. Install Radix UI packages (install only what you need)
pnpm add @radix-ui/react-dialog @radix-ui/react-dropdown-menu @radix-ui/react-select \
  @radix-ui/react-navigation-menu @radix-ui/react-tooltip @radix-ui/react-popover \
  @radix-ui/react-checkbox @radix-ui/react-radio-group @radix-ui/react-switch \
  @radix-ui/react-tabs @radix-ui/react-toast @radix-ui/react-avatar \
  @radix-ui/react-separator @radix-ui/react-label @radix-ui/react-slot

# 3. Install Directus SDK
pnpm add @directus/sdk

# 4. Install NextAuth.js v5
pnpm add next-auth@beta

# 5. Install utility libraries for component composition
pnpm add class-variance-authority clsx tailwind-merge lucide-react

# 6. Configure next.config.ts
# (standard — no special plugins needed for Radix)

# 7. Run dev server
pnpm dev
```

`next.config.ts` minimum:
```typescript
import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [{ hostname: 'localhost' }, { hostname: process.env.DIRECTUS_HOST ?? '' }],
  },
}

export default nextConfig
```

**Tailwind CSS 4 (`src/app/globals.css`):**
```css
@import "tailwindcss";

@theme {
  --color-primary-50: oklch(97% 0.02 270);
  --color-primary-500: oklch(55% 0.18 270);
  --color-primary-900: oklch(25% 0.10 270);
  --font-sans: 'Inter Variable', sans-serif;
  --radius-md: 0.5rem;
}
```

## CSS Variables / Theming

Radix UI applies no default styling — it is purely behavioral. All visual design comes from Tailwind classes applied to the component wrappers. Brand tokens from `04_brand/tokens.json` are mapped directly to CSS custom properties in `globals.css`.

**`globals.css` pattern:**
```css
@import "tailwindcss";

@theme {
  /* Primary colors from tokens.json */
  --color-primary-50: {tokens.color.primary.50};
  --color-primary-100: {tokens.color.primary.100};
  --color-primary-500: {tokens.color.primary.500};
  --color-primary-600: {tokens.color.primary.600};
  --color-primary-900: {tokens.color.primary.900};

  /* Neutral / surface */
  --color-surface: {tokens.color.surface};
  --color-surface-raised: {tokens.color.surfaceRaised};

  /* Typography */
  --font-sans: {tokens.typography.fontFamily.sans};
  --font-mono: {tokens.typography.fontFamily.mono};

  /* Border radius */
  --radius-sm: {tokens.borderRadius.sm};
  --radius-md: {tokens.borderRadius.md};
  --radius-lg: {tokens.borderRadius.lg};
  --radius-full: 9999px;
}

/* Semantic aliases for dark mode */
:root {
  --bg: var(--color-white);
  --fg: var(--color-neutral-900);
  --border: var(--color-neutral-200);
}

.dark {
  --bg: var(--color-neutral-950);
  --fg: var(--color-neutral-50);
  --border: var(--color-neutral-800);
}
```

Token mapping table:
| `tokens.json` key | CSS target | Tailwind usage |
|-------------------|-----------|----------------|
| `color.primary.*` | `--color-primary-*` | `bg-primary-500`, `text-primary-600` |
| `color.neutral.*` | `--color-neutral-*` | `bg-neutral-50` |
| `typography.fontFamily.sans` | `--font-sans` | `font-sans` |
| `borderRadius.md` | `--radius-md` | `rounded-md` |
| `shadow.default` | `--shadow-md` | `shadow-md` |

**`cn()` utility (required for Radix component composition):**
```typescript
// src/lib/utils.ts
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

## Auth Setup

NextAuth.js v5 (Auth.js) handles authentication. Directus is used as the data source via a custom credentials provider that calls the Directus `/auth/login` endpoint.

**`src/auth.ts`:**
```typescript
import NextAuth from 'next-auth'
import Credentials from 'next-auth/providers/credentials'
import { createDirectus, rest, authentication } from '@directus/sdk'

const directus = createDirectus(process.env.DIRECTUS_URL!).with(rest()).with(authentication('json'))

export const { handlers, signIn, signOut, auth } = NextAuth({
  providers: [
    Credentials({
      credentials: { email: {}, password: {} },
      async authorize({ email, password }) {
        const result = await directus.login(String(email), String(password))
        if (!result.access_token) return null
        return { id: result.user?.id, accessToken: result.access_token, refreshToken: result.refresh_token }
      },
    }),
  ],
  callbacks: {
    jwt({ token, user }) {
      if (user) { token.accessToken = user.accessToken; token.refreshToken = user.refreshToken }
      return token
    },
    session({ session, token }) {
      session.accessToken = token.accessToken as string
      return session
    },
  },
})
```

**`src/app/api/auth/[...nextauth]/route.ts`:**
```typescript
export { GET, POST } from '@/auth'
```

**`src/middleware.ts`:**
```typescript
export { auth as middleware } from '@/auth'

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico|login).*)'],
}
```

## App Shell

App Router uses `src/app/layout.tsx` as the root layout and nested layouts for section-level shells.

**Key files:**
- `src/app/layout.tsx` — root HTML shell, fonts, providers
- `src/app/(app)/layout.tsx` — authenticated shell with sidebar + header
- `src/components/shell/AppSidebar.tsx` — `NavigationMenu` from Radix, server component safe
- `src/components/shell/AppHeader.tsx` — breadcrumb, user menu (`DropdownMenu` from Radix)
- `src/components/shell/AppNav.tsx` — nav link list, renders `next/link` inside Radix items

**`src/app/(app)/layout.tsx` pattern:**
```typescript
export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-screen bg-surface overflow-hidden">
      <AppSidebar />
      <div className="flex flex-1 flex-col min-w-0 overflow-hidden">
        <AppHeader />
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  )
}
```

**Route groups:** Use `(app)` route group for authenticated pages and `(auth)` for login/register pages to apply different layouts.

## Component Library

| Generic UI concept | Radix UI Primitive | Package |
|--------------------|--------------------|---------|
| Button | No primitive — compose with `Slot` | `@radix-ui/react-slot` |
| DataTable | No primitive — build with `<table>` + Tailwind | N/A |
| Modal/Dialog | `Dialog.Root`, `Dialog.Trigger`, `Dialog.Content` | `@radix-ui/react-dialog` |
| Form Input | No primitive — use `<input>` + `Label` | `@radix-ui/react-label` |
| Select/Dropdown | `Select.Root`, `Select.Trigger`, `Select.Content` | `@radix-ui/react-select` |
| Navigation | `NavigationMenu.Root`, `NavigationMenu.List` | `@radix-ui/react-navigation-menu` |
| Card | No primitive — compose with `<div>` | N/A |
| Toast/Notification | `Toast.Provider`, `Toast.Root`, `Toast.Viewport` | `@radix-ui/react-toast` |
| Dropdown Menu | `DropdownMenu.Root`, `DropdownMenu.Content` | `@radix-ui/react-dropdown-menu` |
| Tooltip | `Tooltip.Provider`, `Tooltip.Root` | `@radix-ui/react-tooltip` |
| Tabs | `Tabs.Root`, `Tabs.List`, `Tabs.Trigger` | `@radix-ui/react-tabs` |
| Checkbox | `Checkbox.Root`, `Checkbox.Indicator` | `@radix-ui/react-checkbox` |
| Switch | `Switch.Root` | `@radix-ui/react-switch` |
| Avatar | `Avatar.Root`, `Avatar.Image`, `Avatar.Fallback` | `@radix-ui/react-avatar` |
| Popover | `Popover.Root`, `Popover.Trigger`, `Popover.Content` | `@radix-ui/react-popover` |

> Build a `src/components/ui/` folder with styled wrappers around each Radix primitive. These wrappers apply the brand-consistent Tailwind classes and export as the app's design system components (e.g., `<Button>`, `<Dialog>`, `<Select>`). See the shadcn/ui pattern for reference — the difference is no copy-paste CLI; build them from scratch per the concept's brand.

## Mock Adaptation

The mock skill uses CDN-based HTML templates. Radix UI requires a build step, so the mock uses the **`preact_htm`** template as the closest CDN-based React alternative.

```yaml
mock_template: preact_htm
```

Note: The CDN mock renders structural and layout intent only. Radix UI's accessibility semantics and interaction behavior are not present in mocks — they appear in Storybook stories.

## Storybook Config

```yaml
storybook_addon: "@storybook/nextjs"
story_format: CSF3
component_import: "@radix-ui/react-*"
setup_file: .storybook/setup.ts
```

`@storybook/nextjs` provides Next.js 15 App Router context inside stories, including `next/navigation`, `next/image`, and `next/link` mocking.

**`.storybook/main.ts`:**
```typescript
export default {
  framework: '@storybook/nextjs',
  stories: ['../src/components/**/*.stories.tsx'],
  addons: ['@storybook/addon-essentials', '@storybook/addon-a11y'],
}
```

**`.storybook/preview.tsx`:**
```typescript
import '../src/app/globals.css'  // brand tokens via @theme
import type { Preview } from '@storybook/react'

const preview: Preview = {
  parameters: {
    backgrounds: { default: 'light' },
  },
}
export default preview
```

Write stories for the styled wrapper components in `src/components/ui/`, not for raw Radix primitives.

## Migration / ORM

Directus manages the PostgreSQL schema for core application tables.

```bash
# Export schema snapshot for version control
npx directus schema snapshot ./directus-schema.yaml

# Apply to another environment
npx directus schema apply ./directus-schema.yaml

# Bootstrap Directus from scratch
npx directus bootstrap
```

If custom entities are needed outside Directus, add Prisma:
```bash
pnpm add prisma @prisma/client
pnpm prisma init --datasource-provider postgresql
pnpm prisma migrate dev --name init
pnpm prisma generate
```

Keep Directus-managed and Prisma-managed tables clearly separated in documentation.

## Codegen

None for Directus (API is auto-generated).

If Prisma is added for custom entities:
```bash
# After schema changes, regenerate types
pnpm prisma generate
```

For TypeScript types from the Directus SDK:
```typescript
// src/types/directus.ts
export interface DirectusSchema {
  posts: Post[]
  categories: Category[]
}
```

## Expert Skills

Which `prog-expert-*` skills to look for:

- `prog-expert-nextjs` — Next.js 15 App Router, RSC patterns, data fetching, caching strategies, middleware, route handlers
- `prog-expert-directus` — Directus collection setup, permissions, flows, SDK usage, schema snapshot workflow

## Key Implementation Patterns

**1. Server Components as the default for data fetching:**
In App Router, all components are Server Components by default. Fetch Directus data directly in RSC — no `useEffect` or client-side fetching for initial data. Mark components as `'use client'` only when they need interactivity or browser APIs:
```typescript
// src/app/(app)/posts/page.tsx — Server Component
async function PostsPage() {
  const directus = getDirectusClient()  // server-side client with service token
  const posts = await directus.request(readItems('posts'))
  return <PostList posts={posts} />
}
```

**2. Radix primitive + CVA for variant-aware components:**
Build styled wrappers using `class-variance-authority` to handle component variants. This keeps variant logic type-safe and co-located with the component:
```typescript
const buttonVariants = cva('rounded-md font-medium transition-colors focus-visible:outline-none', {
  variants: {
    variant: {
      primary: 'bg-primary-500 text-white hover:bg-primary-600',
      outline: 'border border-border bg-transparent hover:bg-surface-raised',
    },
    size: { sm: 'px-3 py-1.5 text-sm', md: 'px-4 py-2 text-base' },
  },
  defaultVariants: { variant: 'primary', size: 'md' },
})
```

**3. Next.js caching strategy for Directus data:**
Use Next.js `fetch` cache tags for ISR-compatible caching of Directus content. Tag by collection name so revalidation is surgical:
```typescript
const data = await fetch(`${process.env.DIRECTUS_URL}/items/posts`, {
  next: { tags: ['posts'], revalidate: 60 },
  headers: { Authorization: `Bearer ${serviceToken}` },
})
```
Call `revalidateTag('posts')` in a route handler when Directus webhooks fire on collection updates.

**4. `Tooltip.Provider` and `Toast.Provider` must wrap the app:**
Radix providers must be placed high in the tree. Put them in `src/app/layout.tsx` (as a client component wrapper) or in a dedicated `src/components/providers.tsx` that is imported in the root layout.

**5. Type-safe route params with `next/navigation`:**
Next.js 15 makes `params` and `searchParams` async. Always await them:
```typescript
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
}
```
