# Startup Guide (Phase 2)

Detailed steps for starting the application stack and verifying it is accessible.

## Environment Setup

Run `pnpm run setup` at root to copy all `.env.example` files to `.env`
(root, backend/apps/api, frontend, e2e). Without this, the backend will fail
with Zod validation errors for missing config values.

## Backend Modes

The PostXL backend has two relevant modes:

| Mode | Command | `/test` endpoints | Use case |
|------|---------|-------------------|----------|
| `dev` | `pnpm run dev` | No | Manual testing with empty data |
| `e2e:stateless` | `pnpm run e2e:stateless` | Yes (`/test/reset`, `/test/reset-scenario`) | E2E tests with seed data |

When E2E tests are planned (Phase 4), start the backend in `e2e:stateless` mode
(`NODE_ENV=test`). The E2E global setup should call `PUT /test/reset-scenario`
to load the "populated" scenario.

**Auth mock roles:** `e2e:stateless` defaults to `AUTH_MOCK_ROLES=viewer`. For
admin page tests, set `AUTH_MOCK_ROLES=admin` in the backend `.env`.

## Startup Steps

1. Run `pnpm run setup` if `.env` files don't exist yet
2. Start Docker services: `docker-compose up -d`
3. Wait for services to be healthy (database, auth)
4. Start backend: `pnpm run dev` or `pnpm run e2e:stateless` for E2E mode
5. Start frontend: `pnpm run dev` (in frontend/)
6. Use `agent-browser` to navigate to the app URL (typically `http://localhost:3000`)
7. Verify:
   - The page loads without errors
   - Login page or default page renders
   - No console errors in browser
8. Take screenshot

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Zod validation errors | Missing `.env` files | Run `pnpm run setup` |
| Database not ready | Container still starting | Wait and retry |
| Port conflicts | Another process on port | Find and kill or change port |
| Missing env variables | Setup not run | Run `pnpm run setup` |
| Auth misconfiguration | Keycloak not started | Check `docker-compose up -d` |
| ECONNREFUSED | Service not running | Verify Docker containers and dev servers |
