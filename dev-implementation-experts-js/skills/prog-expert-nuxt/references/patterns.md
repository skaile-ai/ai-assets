# Patterns

Store reusable code patterns extracted from successful implementations here.

## 1. Project Organization (Nuxt 3/4)
- Use a `src/` or `app/` directory approach for the core application by setting `srcDir: "app/"` in `nuxt.config.ts`.
- Move standard Nuxt folders (`pages/`, `components/`, `layouts/`, `composables/`, `middleware/`) into the `app/` directory for better structural separation.
- Configure ESLint systematically with `@nuxt/eslint`.

## 2. API & Data Fetching

### Directus
- Use the `@nuxt-directus` module. Configure tokens and URLs securely using `.env` and `nuxt.config.ts` runtime config. For highly automated setups, consider `@nuxtus/nuxt-module`, and for decoupled custom GraphQL/REST setups, consider `@bg-dev/nuxt-directus`. The official `@directus/sdk` is also highly recommended when used directly with Nuxt plugins.
- Keep data operations abstracted: create typed wrapper composables (`useArticles.ts` etc.) to handle Directus items, rather than sprinkling Directus Client logic in Vue components.
- Use `directus-typeforge` to generate TypeScript interfaces for type-safe CRUD operations.
- Data Lifecycle using `<client-only>`: To protect data operations, always use `<client-only>` where manipulating DOM directly (like Tiptap) or carefully sync server/client state using Nuxt's `useAsyncData`.

### Drizzle ORM + SQLite
- Place schema in `server/db/schema.ts`, DB utility in `server/utils/db.ts` (auto-imported by Nitro).
- Use Drizzle's relational query builder (`db.query.items.findMany({ with: { ... } })`) for complex queries with eager loading.
- Define `relations()` separately from table definitions for clean separation.
- Use composite `primaryKey()` for junction tables and many-to-many relationships.
- Pattern: `onConflictDoUpdate` for upsert operations, `onConflictDoNothing` for idempotent inserts.
- Seed scripts use `tsx` runner: `"db:seed": "tsx server/db/seed.ts"`.
- Migration workflow: `drizzle-kit generate` → `tsx server/db/migrate.ts`.

## 3. UI Framework & Styling

### PrimeVue + Tailwind
- When installing PrimeVue, configure it in the `nuxt.config.ts` modules. Setup the `Aura` theme (or similar) inside the `primevue` plugin configuration.
- Avoid CSS conflicts by letting PrimeVue handle components while Tailwind CSS is used strictly for layout and typography utilities, using `tailwindcss-primeui` integration and ensuring module order (`@nuxtjs/tailwindcss` before `@primevue/nuxt-module`).

### PrimeVue + UnoCSS (preferred)
- UnoCSS with `presetWind4()` provides Tailwind-compatible utilities without CSS conflicts with PrimeVue.
- No special integration package needed (unlike Tailwind which requires `tailwindcss-primeui`).
- Module order: `@unocss/nuxt` before `@primevue/nuxt-module`.
- Use `@unocss/reset/sanitize/sanitize.css` as CSS reset.
- Configure custom colors, fonts, and font sizes in `uno.config.ts` theme.
- Tree-shake PrimeVue components: `primevue: { components: { include: ['Button', ...] } }`.

### PrimeVue Forms + Zod
- Use `@primevue/forms` with `zodResolver()` for type-safe form validation.
- `FormField v-slot="$field"` exposes validation state (`$field.invalid`, `$field.error`).
- Combine with `useToast()` for submission-level feedback.

## 4. Protected Routes & Authentication
- Use Nuxt Global Middleware (`middleware/auth.global.ts`) to intercept routes and enforce authorization. Use Nuxt's built-in `navigateTo` helper.
- Exclude login/public pages from the auth check explicitly by checking `to.path`.

## 5. Rich Text Editing (Tiptap)
- Use `nuxt-tiptap-editor` for robust integration. **Best Practice:** For rapid setups, `nuxt-tiptap-editor` is great, but for highly customized editors, direct integration using `@tiptap/vue-3` and `@tiptap/pm` is recommended. If using Nuxt UI, use `UEditor`.
- Isolate Tiptap initialization inside a Vue reusable wrapper component (e.g. `MarkdownEditor.vue`) with two-way data binding `v-model` for emitting changes back to the parent component.
- Always mount editor components inside `<client-only>` to avoid SSR hydration mismatches.

## 6. Content Management (Nuxt Content 3)
- Define typed collections in `content.config.ts` with Zod schemas for data collections.
- Use `type: 'page'` for markdown, `type: 'data'` for JSON/YAML.
- `queryCollection()` for data fetching, `queryCollectionNavigation()` for auto-generated nav trees.
- Pattern: composable + `useState()` for modal content loading (avoids Pinia for simple global state).
- `ContentRenderer` component for rendering markdown content.

## 7. State Management (Lightweight)
- For simple global state, use `useState()` composables instead of Pinia/Vuex.
- Cookie-backed user identification: `useCookie()` + UUID generation for anonymous user tracking.
- Event emission pattern: child components emit events upward (e.g., `@rate`, `@filter-change`), parent handles API calls.
- `useAsyncData()` for server-aware data fetching with SSR support.

## 8. SPA Mode (SSR Disabled)
- Set `ssr: false` in `nuxt.config.ts` for client-only applications.
- Suitable when SEO is not a priority (e.g., internal tools, authenticated apps).
- All API routes still run server-side via Nitro even with SSR disabled.
- No `<client-only>` wrappers needed when SSR is off.
