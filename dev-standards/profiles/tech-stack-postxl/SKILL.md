---
name: "tech-stack-postxl"
description: "Reference document and invocable skill for the PostXL platform stack (React 19 + Vite + NestJS + Prisma + Keycloak). Read by scaffold, foundation, design, mock, and storybook skills when 3_blueprint/1_techstack/stack.md selects this stack. Fixed production stack with code generation — used by saxe-compatible projects."
metadata:
  tags:
    - "postxl"
    - "react19"
    - "vite"
    - "nestjs"
    - "prisma"
    - "keycloak"
    - "postgresql"
    - "enterprise"
    - "codegen"
    - "saxe"
    - "oidc"
    - "typescript"
  stage: "alpha"
  requires:
    - "standards-contract"
---

# Tech Stack: PostXL (React 19 + Vite + NestJS + Prisma + Keycloak)

## Overview

The PostXL platform stack is a fixed production configuration used by saxe-compatible enterprise projects. It combines React 19 + Vite on the frontend (with `@postxl/ui-components` — a curated library built on Radix UI, Tailwind v4, Vaul, Sonner, and Zustand), NestJS as the application server, Prisma for database access with full migration support, and Keycloak for enterprise-grade OpenID Connect authentication. This stack includes a mandatory code generation step — `pnpm run generate` runs PostXL generators from `postxl-schema.json` to scaffold boilerplate after data model changes. Use this stack for PostXL platform projects; avoid it for greenfield work without PostXL infrastructure.

## Identity

| Field | Value |
|-------|-------|
| Frontend | React 19 + Vite (SPA, client-side rendering) |
| UI Library | @postxl/ui-components (Radix UI + Tailwind CSS 4 + Vaul + Sonner + Zustand) |
| Backend | NestJS (PostXL application server, module architecture) |
| Database | PostgreSQL (via Prisma) |
| Auth | Keycloak (OpenID Connect, RBAC, SSO) |
| ORM / DB Access | Prisma (schema-first, type-safe query builder) |
| Package Manager | pnpm |
| CSS Methodology | Tailwind CSS 4 + @postxl/ui-components design tokens (`--radius`, `--font-sans`, etc.) |

## When to Use

- Best for: enterprise applications on the PostXL platform
- Best for: projects requiring NestJS module architecture for complex business logic
- Best for: organizations running Keycloak for centralized SSO (LDAP, AD, corporate IdP)
- Best for: saxe-compatible projects where the schema-to-code generation pipeline is the primary development workflow
- Best for: teams that need Prisma's type-safe query builder with full migration history
- Avoid when: greenfield projects without PostXL infrastructure — the `pnpm run generate` step has a hard dependency on `postxl-schema.json` and the PostXL generator ecosystem
- Avoid when: you want a managed backend (use nextjs-shadcn with Supabase instead)
- Avoid when: the team has no NestJS/Java-adjacent architecture experience — the module pattern has a steeper learning curve than Nuxt/Next

## Scaffold Recipe

```bash
# 1. Use PostXL scaffold command (creates monorepo with frontend + backend)
# (This command requires PostXL CLI to be installed)
postxl scaffold new my-app

# OR if scaffolding manually:
# Create workspace structure
mkdir -p apps/frontend apps/backend

# 2. Initialize frontend (React 19 + Vite)
cd apps/frontend
pnpm create vite . --template react-ts
pnpm add react@19 react-dom@19 @postxl/ui-components

# 3. Initialize backend (NestJS)
cd apps/backend
pnpm add @nestjs/core @nestjs/common @nestjs/platform-express @nestjs/config \
  @nestjs/jwt passport passport-keycloak-connect prisma @prisma/client

# 4. Install PostXL generators
pnpm add -D @postxl/generators

# 5. Initialize Prisma
cd apps/backend
pnpm prisma init --datasource-provider postgresql

# 6. Initialize Tailwind CSS 4 in frontend
cd apps/frontend
pnpm add tailwindcss @tailwindcss/vite

# 7. Install dependencies and run generators
pnpm install
pnpm run generate  # runs PostXL generators from postxl-schema.json

# 8. Start dev servers
pnpm run dev  # starts both frontend and backend via turbo/nx
```

**`apps/frontend/vite.config.ts`:**
```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    proxy: {
      '/api': { target: 'http://localhost:3001', changeOrigin: true },
    },
  },
})
```

**`apps/backend/src/main.ts`:**
```typescript
import { NestFactory } from '@nestjs/core'
import { AppModule } from './app.module'
import { ValidationPipe } from '@nestjs/common'

async function bootstrap() {
  const app = await NestFactory.create(AppModule)
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }))
  app.setGlobalPrefix('api')
  await app.listen(3001)
}
bootstrap()
```

## CSS Variables / Theming

@postxl/ui-components uses a CSS custom property system similar to shadcn/ui but with PostXL-specific token names. Brand tokens from `1_discovery/2_brand/tokens.json` are applied in `globals.css` and Tailwind's `@theme` block.

**`apps/frontend/src/styles/globals.css`:**
```css
@import "tailwindcss";
@import "@postxl/ui-components/styles";

:root {
  /* Border radius from tokens.json */
  --radius: {tokens.borderRadius.default};             /* e.g. 0.5rem */

  /* Typography from tokens.json */
  --font-sans: {tokens.typography.fontFamily.sans};    /* e.g. 'Inter Variable', sans-serif */
  --font-mono: {tokens.typography.fontFamily.mono};

  /* @postxl/ui-components semantic color tokens */
  --background: {tokens.color.background.hsl};
  --foreground: {tokens.color.foreground.hsl};
  --card: {tokens.color.card.hsl};
  --card-foreground: {tokens.color.cardForeground.hsl};
  --primary: {tokens.color.primary.hsl};
  --primary-foreground: 0 0% 100%;
  --secondary: {tokens.color.secondary.hsl};
  --secondary-foreground: {tokens.color.secondaryForeground.hsl};
  --muted: {tokens.color.muted.hsl};
  --muted-foreground: {tokens.color.mutedForeground.hsl};
  --accent: {tokens.color.accent.hsl};
  --accent-foreground: {tokens.color.accentForeground.hsl};
  --destructive: {tokens.color.error.hsl};
  --destructive-foreground: 0 0% 100%;
  --border: {tokens.color.border.hsl};
  --input: {tokens.color.input.hsl};
  --ring: {tokens.color.primary.hsl};
}

.dark {
  --background: {tokens.color.dark.background.hsl};
  --foreground: {tokens.color.dark.foreground.hsl};
  --primary: {tokens.color.primary.hsl};
  /* ... dark values from tokens.json */
}

@theme inline {
  --color-background: hsl(var(--background));
  --color-foreground: hsl(var(--foreground));
  --color-primary: hsl(var(--primary));
  --color-primary-foreground: hsl(var(--primary-foreground));
  --font-sans: var(--font-sans);
  --radius-sm: calc(var(--radius) - 4px);
  --radius-md: var(--radius);
  --radius-lg: calc(var(--radius) + 4px);
}
```

Token mapping table:
| `tokens.json` key | CSS custom property | Usage |
|-------------------|--------------------|-|
| `color.primary.hsl` | `--primary` | `bg-primary text-primary-foreground` |
| `color.background.hsl` | `--background` | `bg-background` |
| `borderRadius.default` | `--radius` | `rounded-md` |
| `typography.fontFamily.sans` | `--font-sans` | `font-sans` |
| `color.error.hsl` | `--destructive` | `bg-destructive` |

## Auth Setup

Keycloak provides OpenID Connect authentication. The NestJS backend validates JWTs issued by Keycloak. The frontend uses Keycloak.js to manage the auth code flow and token refresh.

**NestJS — `apps/backend/src/auth/keycloak.guard.ts`:**
```typescript
import { Injectable, ExecutionContext } from '@nestjs/common'
import { AuthGuard } from '@nestjs/passport'

@Injectable()
export class KeycloakAuthGuard extends AuthGuard('jwt') {
  canActivate(context: ExecutionContext) {
    return super.canActivate(context)
  }
}
```

**NestJS — `apps/backend/src/auth/auth.module.ts`:**
```typescript
import { Module } from '@nestjs/common'
import { PassportModule } from '@nestjs/passport'
import { JwtModule } from '@nestjs/jwt'
import { KeycloakStrategy } from './keycloak.strategy'

@Module({
  imports: [
    PassportModule,
    JwtModule.register({}),  // validation uses Keycloak's public key
  ],
  providers: [KeycloakStrategy],
  exports: [PassportModule],
})
export class AuthModule {}
```

**NestJS — `apps/backend/src/auth/keycloak.strategy.ts`:**
```typescript
import { Injectable } from '@nestjs/common'
import { PassportStrategy } from '@nestjs/passport'
import { ExtractJwt, Strategy } from 'passport-jwt'
import { passportJwtSecret } from 'jwks-rsa'

@Injectable()
export class KeycloakStrategy extends PassportStrategy(Strategy) {
  constructor() {
    super({
      secretOrKeyProvider: passportJwtSecret({
        cache: true,
        rateLimit: true,
        jwksUri: `${process.env.KEYCLOAK_URL}/realms/${process.env.KEYCLOAK_REALM}/protocol/openid-connect/certs`,
      }),
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      issuer: `${process.env.KEYCLOAK_URL}/realms/${process.env.KEYCLOAK_REALM}`,
      algorithms: ['RS256'],
    })
  }

  validate(payload: Record<string, unknown>) {
    return {
      id: payload.sub,
      email: payload.email,
      roles: (payload.realm_access as { roles: string[] })?.roles ?? [],
    }
  }
}
```

**Frontend — `apps/frontend/src/lib/keycloak.ts`:**
```typescript
import Keycloak from 'keycloak-js'

export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
})

export async function initKeycloak(): Promise<boolean> {
  return keycloak.init({
    onLoad: 'login-required',
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
  })
}
```

**Frontend — `apps/frontend/src/components/auth/AuthProvider.tsx`:**
```typescript
import { useEffect, useState } from 'react'
import { keycloak, initKeycloak } from '@/lib/keycloak'

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [authenticated, setAuthenticated] = useState(false)

  useEffect(() => {
    initKeycloak().then(setAuthenticated)

    // Token refresh every 5 minutes
    const interval = setInterval(() => {
      keycloak.updateToken(60).catch(() => keycloak.login())
    }, 300_000)

    return () => clearInterval(interval)
  }, [])

  if (!authenticated) return <div>Loading...</div>
  return <>{children}</>
}
```

**Environment variables:**
```bash
# Backend
KEYCLOAK_URL=http://keycloak:8080
KEYCLOAK_REALM=postxl
DATABASE_URL=postgresql://app:password@postgres:5432/appdb

# Frontend
VITE_KEYCLOAK_URL=http://localhost:8080
VITE_KEYCLOAK_REALM=postxl
VITE_KEYCLOAK_CLIENT_ID=postxl-frontend
VITE_API_URL=http://localhost:3001
```

## App Shell

React Router v7 handles client-side routing. `@postxl/ui-components` provides `AppShell`, `NavigationHeader`, and `SidebarNav` components for the standard PostXL layout.

**Key files:**
- `apps/frontend/src/main.tsx` — React root, `AuthProvider`, `RouterProvider`
- `apps/frontend/src/layouts/AppLayout.tsx` — `AppShell` from @postxl/ui-components
- `apps/frontend/src/components/navigation/NavigationHeader.tsx` — top bar
- `apps/frontend/src/components/navigation/SidebarNav.tsx` — left navigation
- `apps/frontend/src/router.tsx` — React Router v7 route definitions

**`apps/frontend/src/main.tsx`:**
```typescript
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider, createBrowserRouter } from 'react-router-dom'
import { AuthProvider } from './components/auth/AuthProvider'
import { routes } from './router'
import './styles/globals.css'

const router = createBrowserRouter(routes)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  </StrictMode>
)
```

**`apps/frontend/src/layouts/AppLayout.tsx`:**
```typescript
import { Outlet } from 'react-router-dom'
import { AppShell, NavigationHeader, SidebarNav } from '@postxl/ui-components'
import { navigationItems } from '@/config/navigation'

export function AppLayout() {
  return (
    <AppShell
      header={<NavigationHeader />}
      sidebar={<SidebarNav items={navigationItems} />}
    >
      <Outlet />
    </AppShell>
  )
}
```

## Component Library

| Generic UI concept | @postxl/ui-components | Notes |
|--------------------|-----------------------|-------|
| Button | `Button` | variants: `default`, `outline`, `ghost`, `destructive` |
| DataTable | `DataTable` | TanStack Table v8 wrapper |
| Modal/Dialog | `Dialog`, `DialogContent` | Radix Dialog wrapper |
| Form Input | `Input`, `Label`, `FormField` | react-hook-form compatible |
| Select/Dropdown | `Select`, `SelectContent` | Radix Select wrapper |
| Navigation | `SidebarNav`, `NavigationHeader` | PostXL-specific shell components |
| Card | `Card`, `CardHeader`, `CardContent` | Shadcn-style card |
| Toast/Notification | `Toaster` + `toast()` from Sonner | Sonner toast library |
| Drawer | `Drawer` | Vaul drawer (mobile-first) |
| Badge | `Badge` | |
| Avatar | `Avatar`, `AvatarFallback` | |
| App Shell | `AppShell` | Layout wrapper |
| Command | `CommandPalette` | PostXL global command palette |

All components are imported from `@postxl/ui-components`:
```typescript
import { Button, DataTable, Dialog, Input } from '@postxl/ui-components'
```

## Mock Adaptation

The mock skill uses CDN-based HTML templates. React 19 + Vite requires a build step, so the mock uses the **`preact_htm`** template as the closest CDN-based React alternative.

```yaml
mock_template: preact_htm
```

Note: The CDN mock approximates the PostXL layout structure. `@postxl/ui-components` visual fidelity is reflected in Storybook, not in CDN mocks.

## Storybook Config

```yaml
storybook_addon: "@storybook/react"
story_format: CSF3
component_import: "@postxl/ui-components"
setup_file: apps/frontend/.storybook/setup.ts
```

Use `@storybook/react` with the Vite builder for React 19 compatibility.

**`apps/frontend/.storybook/main.ts`:**
```typescript
export default {
  framework: {
    name: '@storybook/react-vite',
    options: {},
  },
  stories: ['../src/**/*.stories.tsx'],
  addons: ['@storybook/addon-essentials', '@storybook/addon-a11y'],
}
```

**`apps/frontend/.storybook/preview.tsx`:**
```typescript
import '../src/styles/globals.css'
import type { Preview } from '@storybook/react'
import { Toaster } from '@postxl/ui-components'

const preview: Preview = {
  decorators: [
    (Story) => (
      <>
        <Story />
        <Toaster />
      </>
    ),
  ],
}
export default preview
```

Write stories for feature-level composite components (e.g., a `PostEditor` that uses multiple `@postxl/ui-components`), not for primitives.

## Migration / ORM

Prisma manages PostgreSQL schema with a full migration history.

**`apps/backend/prisma/schema.prisma`:**
```prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

model User {
  id        String   @id @default(cuid())
  email     String   @unique
  name      String
  posts     Post[]
  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt
}
```

**Migration commands:**
```bash
# Create and apply new migration (dev only — generates SQL + applies)
pnpm prisma migrate dev --name add_posts_table

# Apply migrations in CI/production (no SQL generation, fails if drift)
pnpm prisma migrate deploy

# Reset development database and re-apply all migrations
pnpm prisma migrate reset

# Generate Prisma Client after schema changes
pnpm prisma generate

# Open Prisma Studio (browser DB viewer)
pnpm prisma studio
```

**Prisma Client usage in NestJS:**
```typescript
// apps/backend/src/prisma/prisma.service.ts
import { Injectable, OnModuleInit } from '@nestjs/common'
import { PrismaClient } from '@prisma/client'

@Injectable()
export class PrismaService extends PrismaClient implements OnModuleInit {
  async onModuleInit() {
    await this.$connect()
  }
}
```

Inject `PrismaService` into NestJS services. Never instantiate `PrismaClient` directly in controllers.

## Codegen

PostXL has a mandatory code generation step that runs PostXL generators from `postxl-schema.json`. This step scaffolds boilerplate (NestJS modules, DTOs, repository classes, React query hooks) from the data model definition.

```bash
# Run PostXL generators — required after data model changes
pnpm run generate

# Prisma client generation — required after schema.prisma changes
pnpm prisma generate
```

**When to run `pnpm run generate`:**
- After modifying `postxl-schema.json` (data model definitions)
- After adding new entities or relations in the concept's `3_blueprint/3_datamodel/`
- After changing field types that affect DTO validation
- Before running `prisma migrate dev` — generators update `schema.prisma` from `postxl-schema.json`

**`postxl-schema.json` is the source of truth** for the data model in PostXL projects. It drives both Prisma schema generation and frontend TypeScript type generation. Never edit `schema.prisma` directly in PostXL projects — edit `postxl-schema.json` and regenerate.

Generated output locations:
- `apps/backend/prisma/schema.prisma` — Prisma schema (generated)
- `apps/backend/src/*/dto/*.dto.ts` — NestJS DTOs (generated)
- `apps/backend/src/*/entities/*.entity.ts` — Prisma model wrappers (generated)
- `apps/frontend/src/types/api.ts` — TypeScript API types (generated)
- `apps/frontend/src/hooks/use-*.ts` — React query hooks (generated)

## Expert Skills

Which `prog-expert-*` skills to look for:

- `prog-expert-nestjs` — NestJS module architecture, dependency injection, guards, interceptors, pipes, decorators, exception filters
- `prog-expert-prisma` — Prisma schema design, migration patterns, relation queries, transactions, Prisma Client extensions
- `prog-expert-keycloak` — Keycloak realm configuration, client setup, RBAC policies, token customization, SSO federation

## Key Implementation Patterns

**1. NestJS module per domain, never per layer:**
Organize NestJS modules by business domain, not by technical layer. Each domain module (e.g., `PostsModule`) owns its controller, service, and repository. Cross-cutting concerns (auth, logging, validation) live in shared modules:
```
apps/backend/src/
├── posts/
│   ├── posts.module.ts
│   ├── posts.controller.ts
│   ├── posts.service.ts
│   └── dto/
├── users/
│   ├── users.module.ts
│   └── ...
└── shared/
    ├── auth/
    └── prisma/
```

**2. Keycloak JWT carries roles — do not replicate roles in the database:**
Keycloak `realm_access.roles` in the JWT payload is the source of truth for authorization. NestJS guards read roles from the validated JWT, not from a database roles table. Use `@Roles('admin')` decorator pattern on controllers:
```typescript
@Get()
@UseGuards(KeycloakAuthGuard, RolesGuard)
@Roles('posts:read')
findAll() { return this.postsService.findAll() }
```

**3. Always run `pnpm run generate` before `prisma migrate dev`:**
The PostXL generator updates `schema.prisma` from `postxl-schema.json`. Running `prisma migrate dev` before `generate` will produce migrations based on a stale schema. The correct order is: edit `postxl-schema.json` → `pnpm run generate` → `pnpm prisma migrate dev`.

**4. React query hooks from codegen for all server state:**
Generated React query hooks (`use-posts.ts`, `use-users.ts`) wrap TanStack Query and the typed API client. Never use raw `fetch` or `axios` in components — always use the generated hooks. This ensures consistent loading states, error handling, and cache invalidation:
```typescript
import { useGetPosts, useCreatePost } from '@/hooks/use-posts'

function PostList() {
  const { data: posts, isLoading } = useGetPosts()
  const { mutate: createPost } = useCreatePost()
  // ...
}
```

**5. Sonner `toast()` for all user notifications:**
@postxl/ui-components bundles Sonner. Use the `toast()` function (not a hook) anywhere in the application — no context provider needed beyond the `<Toaster />` in the app root. Standardize notification patterns: `toast.success()` for completed actions, `toast.error()` for failures, `toast.loading()` for async operations:
```typescript
import { toast } from '@postxl/ui-components'

async function handleSave() {
  const toastId = toast.loading('Saving...')
  try {
    await createPost(data)
    toast.success('Post created', { id: toastId })
  } catch (err) {
    toast.error('Failed to save', { id: toastId })
  }
}
```
