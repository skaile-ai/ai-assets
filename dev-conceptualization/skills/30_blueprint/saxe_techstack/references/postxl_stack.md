# PostXL Fixed Stack

The PostXL platform provides a fixed, production-ready stack. There are no
framework choices to make. Every PostXL project uses these technologies.

## Stack Definition

```yaml
platform: web
framework: PostXL
frontend: "Vite + React 19"
ui_library: "@postxl/ui-components (60+ Radix + Tailwind v4 components)"
router: TanStack Router (file-based)
state: TanStack Query + tRPC client
forms: TanStack Form
tables: TanStack Table (via DataGrid component)
backend: "NestJS + Fastify"
api: tRPC (type-safe, end-to-end)
orm: Prisma
database: PostgreSQL
auth: Keycloak (RBAC with roles)
hosting: "Docker Compose (self-hosted)"
package_manager: pnpm
testing: "Jest (backend) + Vitest (frontend) + Playwright (E2E)"
css: "Tailwind CSS v4"
code_generation: "PostXL generators (schema-driven)"
```

## stack.md Template

```markdown
---
status: draft
platform: web
framework: PostXL
frontend: "Vite + React 19"
ui_library: "@postxl/ui-components (Radix + Tailwind v4)"
backend: "NestJS + Fastify + tRPC"
orm: Prisma
database: PostgreSQL
auth: Keycloak
package_manager: pnpm
last_updated: YYYY-MM-DD
---

# Tech Stack

## Platform: PostXL
Full-stack TypeScript platform with schema-driven code generation.
All core technologies are fixed and production-tested.

## Frontend: Vite + React 19
Fast HMR development server with React 19. File-based routing
via TanStack Router.

## UI Library: @postxl/ui-components
60+ components built on Radix UI primitives with Tailwind CSS v4.
Includes DataGrid (TanStack Table), forms (TanStack Form), dialogs,
selects, and layout primitives.

## State Management: TanStack Query + tRPC
Server state via TanStack Query with tRPC for type-safe API calls.
End-to-end type safety from database to UI.

## Backend: NestJS + Fastify
NestJS modules on Fastify for high-performance API serving.
tRPC routers for type-safe procedures.

## ORM: Prisma
Type-safe database access with migrations, seeding, and
schema-driven code generation.

## Database: PostgreSQL
Production-grade relational database. Accessed via Prisma ORM.

## Auth: Keycloak
Enterprise-grade identity management with RBAC. Supports roles,
groups, social login federation, and enterprise SSO.

## Hosting: Docker Compose (self-hosted)
Full stack runs via Docker Compose: app, database, Keycloak,
and any additional services.

## Package Manager: pnpm
Fast, disk-efficient package manager for the monorepo.

## Testing
- Jest for backend unit/integration tests
- Vitest for frontend unit tests
- Playwright for E2E browser tests

## Code Generation
PostXL generators produce CRUD modules, tRPC routers, React components,
and Prisma schemas from a single source definition.

## CSS: Tailwind CSS v4
Utility-first CSS framework. All UI components use Tailwind v4
design tokens from the brand identity.

## Additional Integrations
<!-- List any additional integrations identified during consultation -->
None identified.
```
