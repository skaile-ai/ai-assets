# Audit Checklists

Used by the `audit` skill's three parallel sub-agents. Each checklist is tuned for the skaile-dev tech stacks (NestJS, React, Nuxt 4, TypeScript, Bun).

Every finding MUST include:
- `severity`: `critical | high | medium | low`
- `file:line`
- `category` (from the list below)
- `description` (one sentence)
- `fix` (one sentence — what to change)

---

## Sub-agent A — Logic & Runtime Errors

### TypeScript / JavaScript

- Null/undefined dereference without optional chaining or guards
- Off-by-one errors in loop bounds and array indexing
- `async`/`await` misuse:
  - missing `await` on a Promise-returning call
  - unhandled Promise rejection (Promise.then without `.catch`)
  - `await` inside a `.map()` callback (should use `Promise.all(items.map(...))`)
- Missing error handling at system boundaries:
  - `fetch`, `axios`, `undici` calls without try/catch
  - `fs.*` async calls without try/catch
  - Prisma / drizzle queries without try/catch
  - WebSocket `send`/`on('message')` without error handlers
- Data loss paths:
  - `update({ where, data })` with no preceding `findUnique` existence check
  - `writeFile` without temp-file + rename pattern
- Race conditions:
  - Shared mutable state accessed from multiple async callers
  - File I/O racing with a watcher (`chokidar`, `fs.watch`)
  - Concurrent DB transactions on the same row

### NestJS (platform/backend)

- Controller handler without `@UseGuards()` on protected endpoints
- Missing DTO validation (`@ValidationPipe()`, `class-validator` decorators)
- Inject-by-token drift (`@Inject('TOKEN')` vs. provider token)
- Circular module dependencies

### React (platform/frontend)

- `useEffect` with missing deps or wrong deps
- State mutation (e.g. `state.items.push(...)` instead of `setState(...)`)
- Stale closure in event handler
- `useQuery` / `useMutation` without error or loading states

### Nuxt 4 (forge)

- Server routes (`server/api/*.ts`) without `defineEventHandler` error handling
- `useFetch` without `transform` or error branch
- SSR-unsafe code in `setup()` (browser-only APIs without `import.meta.client`)

---

## Sub-agent B — Security & Data Integrity

### Injection

- SQL injection via raw Prisma `$queryRawUnsafe` or drizzle `sql.raw` with user input
- Command injection via `child_process.exec`, `execSync`, `spawn(shell: true)` with user input
- Path traversal: user input used in `fs.*` calls without `path.resolve` + prefix check
- ReDoS: user-supplied input fed to a regex with catastrophic backtracking

### XSS / Template Escaping

- `v-html` / `dangerouslySetInnerHTML` with user input
- `innerHTML` assignment with user input
- Unescaped rendering in email or PDF templates

### Auth / Authorization

- Route handler missing auth guard on state-changing endpoint
- Insecure direct object reference: `GET /api/resource/:id` without ownership check
- Broken RBAC: role check on GET but not DELETE
- JWT/session secret hardcoded or missing rotation
- Cookie without `HttpOnly`, `Secure`, `SameSite`

### Secrets / Data Leakage

- API key, token, or password hardcoded in source
- `.env` committed to git
- Secrets logged (console.log of request body with auth header)
- Sensitive fields returned in API responses (password hash, session token)
- PII in localStorage / sessionStorage

### Dependency Hygiene

- Run `bun audit` (or `npm audit`) — report HIGH and CRITICAL advisories
- Outdated packages with known CVEs

### CSRF

- State-changing POST/PUT/DELETE endpoints without CSRF protection when auth is cookie-based

---

## Sub-agent C — UI/UX Code Quality

### Accessibility (forge + platform frontend)

- Interactive element (`<button>`, `<a>`, `<div role="button">`) without accessible name (`aria-label`, text content)
- Form input without associated `<label>` or `aria-labelledby`
- Custom interactive element (`<div @click>`) without `role` and keyboard handler (`@keydown.enter`, `@keydown.space`)
- Image without `alt` attribute (or explicit `alt=""` for decorative)
- Color contrast failures (if design tokens are used, verify token-to-token contrast)

### Feedback & State

- Loading state that blocks the UI with no spinner or skeleton
- Error state with no user recovery path (no retry button, no error message)
- Success toast fires but disappears too fast to read (< 3s for actionable ones)
- Long-running action with no progress indicator

### Form Handling

- Submit button not disabled during submission — double-submit possible
- Validation errors not announced to screen readers (`aria-live="polite"`)
- Destructive action (delete, archive) without confirmation
- Unsaved changes lost on navigation with no warning

### Design System Compliance

- Hardcoded colors (`#fff`, `rgb(...)`) instead of design tokens (`var(--color-surface)` / theme CSS vars)
- Hardcoded spacing instead of scale tokens
- Custom component duplicating a primitive already in `theme/` or shadcn/primevue/una variants

### Responsive

- Fixed widths that break below 375px
- Click/tap targets smaller than 44x44px

---

## Sub-agent dispatch template

When the `audit` skill launches the three sub-agents, each gets this prompt shape:

```
You are {Sub-agent A|B|C}: {Logic|Security|UI/UX} Auditor.
Scope: {list of files in this audit run}
Checklists: read ai-assets/skaile-development/references/audit_checklists.md §{A|B|C}
Repo context: read CLAUDE.md of each affected package first.

Return findings as JSON array. Each finding:
{
  "severity": "critical|high|medium|low",
  "category": "<from checklist>",
  "file": "<relative path>",
  "line": <number>,
  "description": "<one sentence>",
  "fix": "<one sentence>"
}

You MUST NOT modify files. You MUST cite a line number for every finding.
You MUST consider framework protections before flagging (e.g. Nuxt auto-escapes
templates; Prisma parameterizes queries by default — only flag raw bypasses).
You MUST skip findings already caught by lint/typecheck (Biome, ESLint, tsc).
```

## Report synthesis

The parent `audit` skill merges the three JSON arrays into `_devlog/reports/audit-<date>.json`:

```json
{
  "run_id": "<uuid>",
  "scope": "diff|package|full",
  "target": "<packages>",
  "build": { "lint": "pass", "typecheck": "pass", "build": "pass" },
  "tests": { "total": 0, "passed": 0, "failed": 0 },
  "findings": [ { ... } ],
  "summary": { "critical": 0, "high": 0, "medium": 0, "low": 0 },
  "verdict": "pass|warn|fail",
  "blocking_issues": [ { ... } ]
}
```

Verdict rules:
- **pass** — build clean + tests pass + zero critical/high findings
- **warn** — build clean + tests pass + only medium/low findings
- **fail** — build fails OR tests fail OR any critical finding
