---
name: "skailup-tech-stack-nextjs-shadcn"
description: "Reference document and invocable skill for the Next.js 15 + shadcn/ui + Supabase stack. Read by scaffold, foundation, design, mock, and storybook skills when 05_techstack/stack.md selects this stack."
metadata:
  tags:
    - "nextjs"
    - "next15"
    - "shadcn"
    - "supabase"
    - "postgresql"
    - "react"
    - "rsc"
    - "app-router"
    - "tailwind"
    - "pnpm"
    - "mvp"
    - "saas"
    - "indie"
    - "prototype"
  stage: "alpha"
  requires:
    - "standards-contract"
---

# Tech Stack: Next.js 15 + shadcn/ui + Supabase

## Overview

Full-stack application built with Next.js 15 (App Router, React Server Components), shadcn/ui (copy-paste Radix UI components styled with Tailwind CSS 4), and Supabase as the managed backend (Postgres + Auth + Storage + Realtime). This is the fastest stack for getting a production-quality application to market. Supabase eliminates backend infrastructure management, shadcn/ui provides immediately usable, well-styled components that are fully owned by the project, and Next.js 15 handles SSR/SSG with excellent Vercel DX. The definitive choice for MVPs, indie hackers, and teams that need to ship quickly without sacrificing quality.

## Identity

| Field | Value |
|-------|-------|
| Frontend | Next.js 15 (App Router, React Server Components), SSR + SSG |
| UI Library | shadcn/ui (Radix UI + Tailwind CSS 4, copy-paste components) |
| Backend | Supabase (managed Postgres + Auth + Storage + Realtime + Edge Functions) |
| Database | PostgreSQL (Supabase-managed) |
| Auth | Supabase Auth (JWT, OAuth providers, magic links, OTP, MFA) |
| ORM / DB Access | Supabase client SDK (`@supabase/supabase-js`, `@supabase/ssr`) — no separate ORM; Prisma optional |
| Package Manager | pnpm |
| CSS Methodology | Tailwind CSS 4 + shadcn/ui CSS custom properties (`--background`, `--foreground`, `--primary`, etc.) |

## When to Use

- Best for: MVPs, indie hackers, and early-stage products that need to move fast
- Best for: SaaS applications where managed infrastructure removes ops burden
- Best for: Teams who want shadcn's copy-paste ownership model (components live in `src/components/ui/` and are fully modifiable)
- Best for: Projects using OAuth social login out of the box without extra config
- Best for: Real-time features (Supabase Realtime is built in — no extra infra)
- Best for: Fast prototyping where backend schema evolves rapidly
- Avoid when: you need complex backend business logic — Supabase Edge Functions have cold start and size limits; consider NestJS (postxl stack) instead
- Avoid when: corporate policy prohibits managed SaaS backends — use nextjs-radix with self-hosted Directus
- Avoid when: the database schema is highly relational with complex stored procedures

## Scaffold Recipe

```bash
# 1. Initialize Next.js 15 with App Router, TypeScript, Tailwind
npx create-next-app@latest . --typescript --tailwind --app --src-dir --import-alias "@/*"

# 2. Initialize shadcn/ui (configures components.json, installs dependencies)
npx shadcn@latest init
# When prompted:
#   Style: New York (recommended)
#   Base color: match tokens.json primary color
#   CSS variables: Yes

# 3. Add shadcn/ui components you need
npx shadcn@latest add button card dialog input label select table tabs toast badge avatar

# 4. Install Supabase clients
pnpm add @supabase/supabase-js @supabase/ssr

# 5. Install Supabase CLI (dev dependency for migrations + type gen)
pnpm add -D supabase

# 6. Initialize Supabase local dev
pnpm supabase init
pnpm supabase start  # starts local Supabase stack via Docker

# 7. Configure environment variables
# .env.local:
# NEXT_PUBLIC_SUPABASE_URL=https://your-project.supabase.co
# NEXT_PUBLIC_SUPABASE_ANON_KEY=your-anon-key
# SUPABASE_SERVICE_ROLE_KEY=your-service-role-key

# 8. Run dev server
pnpm dev
```

**`next.config.ts`:**
```typescript
import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      { hostname: '*.supabase.co' },
      { hostname: '*.supabase.in' },
    ],
  },
}

export default nextConfig
```

## CSS Variables / Theming

shadcn/ui uses a CSS custom property system where semantic color names (`--background`, `--foreground`, `--primary`, `--primary-foreground`, etc.) map to HSL values. Brand tokens from `04_brand/tokens.json` map **1:1** to these semantic variables in `globals.css`.

**`src/app/globals.css` pattern:**
```css
@import "tailwindcss";
@import "@/components/ui/styles.css";  /* shadcn base styles */

@layer base {
  :root {
    /* Map from tokens.json */
    --background: {tokens.color.background};        /* e.g. 0 0% 100% */
    --foreground: {tokens.color.foreground};        /* e.g. 240 10% 3.9% */
    --card: {tokens.color.card};
    --card-foreground: {tokens.color.cardForeground};
    --primary: {tokens.color.primary.hsl};          /* e.g. 262 83% 58% */
    --primary-foreground: 0 0% 100%;
    --secondary: {tokens.color.secondary.hsl};
    --secondary-foreground: {tokens.color.secondaryForeground};
    --muted: {tokens.color.muted};
    --muted-foreground: {tokens.color.mutedForeground};
    --accent: {tokens.color.accent};
    --accent-foreground: {tokens.color.accentForeground};
    --destructive: {tokens.color.error};
    --destructive-foreground: 0 0% 100%;
    --border: {tokens.color.border};
    --input: {tokens.color.input};
    --ring: {tokens.color.primary.hsl};
    --radius: {tokens.borderRadius.default};        /* e.g. 0.5rem */
  }

  .dark {
    --background: {tokens.color.dark.background};
    --foreground: {tokens.color.dark.foreground};
    --primary: {tokens.color.primary.hsl};
    /* ... dark mode values from tokens.json */
  }
}

@theme inline {
  --color-background: hsl(var(--background));
  --color-foreground: hsl(var(--foreground));
  --color-primary: hsl(var(--primary));
  --color-primary-foreground: hsl(var(--primary-foreground));
  --font-sans: {tokens.typography.fontFamily.sans};
  --radius-sm: calc(var(--radius) - 4px);
  --radius-md: var(--radius);
  --radius-lg: calc(var(--radius) + 4px);
}
```

Token mapping table:
| `tokens.json` key | shadcn/ui CSS var | Usage class |
|-------------------|------------------|-------------|
| `color.primary.hsl` | `--primary` | `bg-primary text-primary-foreground` |
| `color.background` | `--background` | `bg-background` |
| `color.foreground` | `--foreground` | `text-foreground` |
| `color.muted` | `--muted` | `bg-muted` |
| `color.border` | `--border` | `border-border` |
| `borderRadius.default` | `--radius` | `rounded-md` |
| `color.error` | `--destructive` | `bg-destructive` |

## Auth Setup

Supabase Auth with Next.js SSR requires `@supabase/ssr` for cookie-based session management that works across Server Components, Route Handlers, and Middleware.

**`src/lib/supabase/server.ts`:**
```typescript
import { createServerClient } from '@supabase/ssr'
import { cookies } from 'next/headers'
import type { Database } from '@/types/supabase'

export async function createSupabaseServerClient() {
  const cookieStore = await cookies()
  return createServerClient<Database>(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
    {
      cookies: {
        getAll: () => cookieStore.getAll(),
        setAll: (cookiesToSet) => {
          try {
            cookiesToSet.forEach(({ name, value, options }) =>
              cookieStore.set(name, value, options)
            )
          } catch {}
        },
      },
    }
  )
}
```

**`src/lib/supabase/client.ts`:**
```typescript
import { createBrowserClient } from '@supabase/ssr'
import type { Database } from '@/types/supabase'

export function createSupabaseBrowserClient() {
  return createBrowserClient<Database>(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!
  )
}
```

**`src/middleware.ts`:**
```typescript
import { createServerClient } from '@supabase/ssr'
import { NextResponse, type NextRequest } from 'next/server'

export async function middleware(request: NextRequest) {
  let supabaseResponse = NextResponse.next({ request })

  const supabase = createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
    {
      cookies: {
        getAll: () => request.cookies.getAll(),
        setAll: (cookiesToSet) => {
          cookiesToSet.forEach(({ name, value }) => request.cookies.set(name, value))
          supabaseResponse = NextResponse.next({ request })
          cookiesToSet.forEach(({ name, value, options }) =>
            supabaseResponse.cookies.set(name, value, options)
          )
        },
      },
    }
  )

  const { data: { user } } = await supabase.auth.getUser()

  if (!user && !request.nextUrl.pathname.startsWith('/login')) {
    return NextResponse.redirect(new URL('/login', request.url))
  }

  return supabaseResponse
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico|login|auth).*)'],
}
```

**OAuth setup (Google, GitHub, etc.):** Configured in Supabase Dashboard → Authentication → Providers. No code changes needed beyond calling `supabase.auth.signInWithOAuth({ provider: 'google' })`.

## App Shell

Standard Next.js App Router layout with route groups for auth vs. app sections.

**Key files:**
- `src/app/layout.tsx` — root HTML, fonts, `ThemeProvider`
- `src/app/(app)/layout.tsx` — authenticated shell, sidebar + header
- `src/app/(auth)/layout.tsx` — centered auth card layout
- `src/components/shell/AppSidebar.tsx` — nav using shadcn `Button` (variant="ghost") + Lucide icons
- `src/components/shell/AppHeader.tsx` — top bar, `DropdownMenu` for user menu, `Breadcrumb`
- `src/components/shell/SidebarNav.tsx` — data-driven nav link list

**`src/app/(app)/layout.tsx` pattern:**
```typescript
export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-screen bg-background text-foreground">
      <AppSidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <AppHeader />
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  )
}
```

## Component Library

| Generic UI concept | shadcn/ui Component | Location after `npx shadcn add` |
|--------------------|---------------------|----------------------------------|
| Button | `Button` | `src/components/ui/button.tsx` |
| DataTable | `DataTable` (compose with TanStack Table) | `src/components/ui/data-table.tsx` |
| Modal/Dialog | `Dialog` | `src/components/ui/dialog.tsx` |
| Form Input | `Input` + `Label` | `src/components/ui/input.tsx` |
| Select/Dropdown | `Select` | `src/components/ui/select.tsx` |
| Navigation | `NavigationMenu` | `src/components/ui/navigation-menu.tsx` |
| Card | `Card`, `CardHeader`, `CardContent` | `src/components/ui/card.tsx` |
| Toast/Notification | `Toaster` + `useToast()` | `src/components/ui/toaster.tsx` |
| Dropdown Menu | `DropdownMenu` | `src/components/ui/dropdown-menu.tsx` |
| Tabs | `Tabs`, `TabsList`, `TabsTrigger` | `src/components/ui/tabs.tsx` |
| Badge | `Badge` | `src/components/ui/badge.tsx` |
| Avatar | `Avatar` | `src/components/ui/avatar.tsx` |
| Form (wrapper) | `Form` (react-hook-form + Zod) | `src/components/ui/form.tsx` |
| Popover | `Popover` | `src/components/ui/popover.tsx` |
| Sheet (drawer) | `Sheet` | `src/components/ui/sheet.tsx` |
| Skeleton | `Skeleton` | `src/components/ui/skeleton.tsx` |
| Separator | `Separator` | `src/components/ui/separator.tsx` |

> All shadcn/ui components live in `src/components/ui/` and are fully editable. The `npx shadcn add` command only adds new files — it never overwrites existing ones.

**`<Toaster />` must be placed in the root layout:**
```typescript
// src/app/layout.tsx
import { Toaster } from '@/components/ui/toaster'
export default function RootLayout({ children }) {
  return (
    <html><body>{children}<Toaster /></body></html>
  )
}
```

## Mock Adaptation

The mock skill uses CDN-based HTML templates. shadcn/ui requires a build pipeline, so the mock uses the **`preact_htm`** template as the closest CDN-based React alternative.

```yaml
mock_template: preact_htm
```

Note: CDN mocks are structural/layout approximations. shadcn/ui's exact visual styling (CSS variables, Radix animation) is reflected accurately only in Storybook.

## Storybook Config

```yaml
storybook_addon: "@storybook/nextjs"
story_format: CSF3
component_import: "@/components/ui"
setup_file: .storybook/preview.tsx
```

**`.storybook/main.ts`:**
```typescript
export default {
  framework: '@storybook/nextjs',
  stories: ['../src/**/*.stories.tsx'],
  addons: ['@storybook/addon-essentials', '@storybook/addon-a11y'],
}
```

**`.storybook/preview.tsx`:**
```typescript
import '../src/app/globals.css'  // loads all CSS vars + brand tokens
import type { Preview } from '@storybook/react'
import { ThemeProvider } from '@/components/theme-provider'

const preview: Preview = {
  decorators: [
    (Story) => (
      <ThemeProvider attribute="class" defaultTheme="system">
        <Story />
      </ThemeProvider>
    ),
  ],
}
export default preview
```

Write stories for compound components (e.g., `DataTable` with real data) not for individual shadcn primitives.

## Migration / ORM

Supabase manages PostgreSQL schema via its migration system.

```bash
# Create a new migration
pnpm supabase migration new create_posts_table

# Edit the generated SQL in supabase/migrations/

# Apply migrations to local dev database
pnpm supabase db push

# Apply migrations to production (via CI or directly)
pnpm supabase db push --linked

# Reset local database to clean state
pnpm supabase db reset
```

**Migration file example (`supabase/migrations/20240101000000_create_posts.sql`):**
```sql
create table posts (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  content text,
  author_id uuid references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table posts enable row level security;

create policy "Users can view own posts" on posts
  for select using (auth.uid() = author_id);
```

**RLS (Row Level Security):** Always enable RLS on every table. Define policies based on `auth.uid()`. This is the Supabase authorization pattern — server-side query filtering is a fallback, not the primary layer.

## Codegen

Supabase generates TypeScript types from the live database schema:

```bash
# Generate types from local Supabase instance
pnpm supabase gen types typescript --local > src/types/supabase.ts

# Generate from linked production project
pnpm supabase gen types typescript --linked > src/types/supabase.ts
```

**Run codegen after every migration.** Commit `src/types/supabase.ts` to version control. The Supabase client SDK uses these types for type-safe queries:

```typescript
import { createSupabaseBrowserClient } from '@/lib/supabase/client'
import type { Database } from '@/types/supabase'

const supabase = createSupabaseBrowserClient()
const { data: posts } = await supabase
  .from('posts')
  .select('id, title, created_at')
  .order('created_at', { ascending: false })
// `posts` is typed as Database['public']['Tables']['posts']['Row'][]
```

## Expert Skills

Which `prog-expert-*` skills to look for:

- `prog-expert-nextjs` — Next.js 15 App Router, RSC patterns, data fetching strategies, caching, middleware, route handlers
- `prog-expert-supabase` — Supabase Auth patterns, RLS policy design, Realtime subscriptions, Storage buckets, Edge Functions, migration workflow

## Key Implementation Patterns

**1. Use the service role client only in Route Handlers and Server Actions — never in RSC:**
The service role key bypasses RLS. Only use it in server-side code paths where you intentionally need admin-level access (e.g., webhook handlers). Normal data fetching in RSC should use the anon key client so RLS policies apply:
```typescript
// Server Component — uses anon key, RLS applies
const supabase = await createSupabaseServerClient()  // anon key + user session
const { data } = await supabase.from('posts').select('*')  // filtered by RLS
```

**2. shadcn Form + react-hook-form + Zod for all forms:**
The shadcn `Form` component wraps react-hook-form. Always combine with Zod for schema validation. This is the canonical pattern — do not use uncontrolled forms or manual state:
```typescript
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Form, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
```

**3. Supabase Realtime for live data:**
Use Supabase Realtime channels for live updates instead of polling. Subscribe in a `useEffect` with proper cleanup:
```typescript
useEffect(() => {
  const channel = supabase
    .channel('posts-changes')
    .on('postgres_changes', { event: '*', schema: 'public', table: 'posts' }, handleChange)
    .subscribe()
  return () => { supabase.removeChannel(channel) }
}, [])
```

**4. TanStack Table for data tables:**
shadcn's `DataTable` uses TanStack Table v8. Always implement with `useReactTable` hook for client-side sorting/filtering, or implement server-side with URL-based state for large datasets:
```typescript
const table = useReactTable({
  data,
  columns,
  getCoreRowModel: getCoreRowModel(),
  getSortedRowModel: getSortedRowModel(),
  getFilteredRowModel: getFilteredRowModel(),
})
```

**5. Server Actions for mutations:**
Use Next.js Server Actions for all form submissions and data mutations. This keeps mutation logic on the server, works without JavaScript, and integrates with the `useFormState`/`useFormStatus` hooks:
```typescript
'use server'
export async function createPost(formData: FormData) {
  const supabase = await createSupabaseServerClient()
  const { error } = await supabase.from('posts').insert({ title: formData.get('title') as string })
  if (error) throw new Error(error.message)
  revalidatePath('/posts')
}
```
