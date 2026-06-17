---
name: sql
description: "A dialect-agnostic SQL toolset giving an agent permission-scoped access to PostgreSQL, MySQL, SQLite, and MSSQL through one unified set of tools. Introspect schemas/tables/columns, run parameterized read-only SELECTs with stateless keyset pagination, and (in dml/full scopes) execute parameterized writes and atomic batches — all behind a statement classifier that blocks statement stacking, comment evasion, and data-modifying CTEs, plus DB-level read-only enforcement. One database (DSN) per server instance; the dialect is selected by config."
version: 0.1.0
transport: stdio
command: node
args:
  - "${workspace}/.skaile/assets/mcp-server/sql/server.js"
payload:
  url: https://github.com/skaile-ai/sql-mcp/releases/download/v0.1.0/server.js
  sha256: 07438c66f52bcb5247aa08c7afa4a4ed269575cfe15b247495eb5129eb8bf4c1
  dest: server.js
env:
  SQL_MCP_DIALECT: postgres
  SQL_MCP_ACCESS: readonly
keywords:
  - sql
  - postgres
  - mysql
  - sqlite
  - mssql
  - database
  - query
  - mcp
---

# SQL MCP Server

A portable, dialect-agnostic SQL MCP server: one toolset over PostgreSQL, MySQL,
SQLite, and MSSQL. The runnable bundle (`server.js`) is delivered as a
sha256-verified GitHub **release** asset (no Nix recipe) and run on the baseline
`node`.

> **Source code:** the server, build, and tests live in
> [`skaile-ai/sql-mcp`](https://github.com/skaile-ai/sql-mcp). This directory is
> the **catalog entry only** — `MCP.md`. Bump `version:` + `payload.url` +
> `payload.sha256` together when adopting a new release.

## When to reach for this

- The user needs to query or modify a relational database (Postgres / MySQL / SQLite / MSSQL).
- The task wants typed, permission-scoped SQL tools rather than hand-rolled connection code.
- The agent needs schema introspection, parameterized reads with pagination, or scoped writes.

## Configuration

One database per instance, supplied via the workspace's env / secret injection:

| Env var | Meaning |
|---|---|
| `SQL_MCP_DIALECT` | `postgres` \| `mysql` \| `sqlite` \| `mssql` |
| `SQL_MCP_DSN` | Connection string (via secret injection). |
| `SQL_MCP_ACCESS` | `readonly` \| `dml` \| `full` — the instance capability scope. |
| `SQL_MCP_CURSOR_SECRET` | Optional. Integrity key for `next_cursor` tokens (derived from the DSN when unset). |
| `SQL_MCP_MAX_ROWS`, `SQL_MCP_MAX_RESULT_BYTES`, `SQL_MCP_STATEMENT_TIMEOUT_MS` | Optional safety-limit overrides. |

To expose several databases, declare several `mcp:sql` instances, each with its
own dialect, DSN, and access scope.

## Capabilities

- **Introspection (all scopes):** `sql.capabilities`, `sql.list_schemas`, `sql.list_tables`, `sql.describe_table`.
- **Read (all scopes):** `sql.query` — parameterized SELECT inside a read-only transaction, with stateless keyset/offset pagination via `next_cursor`.
- **Write (`dml` / `full` only):** `sql.execute` (parameterized INSERT/UPDATE/DELETE), `sql.execute_batch` (ordered atomic DML in one transaction).

Call `sql.capabilities` first — it reports the active dialect, scope, and safety limits so the agent can branch without hard-coding assumptions.

## Non-obvious gotchas the agent must respect

- **Scope is enforced server-side.** Tools beyond the instance's `SQL_MCP_ACCESS` are not registered — a write against a `readonly` instance is impossible, not merely discouraged. Three layers back this: tool-gating, a SQL statement classifier, and DB-level read-only.
- **Parameterize — never interpolate values.** Pass `params`; the classifier rejects statement stacking, comment evasion, and data-modifying CTEs.
- **Pagination is stateless.** Pass the returned `next_cursor` (with an optional `limit`) to read past `max_rows`; no server-side cursor state is held.
- **One database per instance.** Tools take no connection argument. The dialect is fixed per instance by `SQL_MCP_DIALECT`.
- **Dialect differences are handled server-side** (e.g. MSSQL `OFFSET/FETCH` pagination, `@pN` params); the agent uses the same tool surface regardless of engine.

## Delivery

There is no Docker image and no Nix recipe. At session materialization the
platform fetches this `MCP.md` from the public catalog, reads the `payload`
block, downloads `server.js` from the GitHub release, **verifies its sha256**,
and writes it to `.skaile/assets/mcp-server/sql/server.js`. The runner then
launches `node ${workspace}/.skaile/assets/mcp-server/sql/server.js` over stdio.
See `mcp/DOMAIN.md` → "Release-asset (`command: node`) servers" for the pattern.

## Reference

- [`skaile-ai/sql-mcp`](https://github.com/skaile-ai/sql-mcp) — source, build (`bun build`), tests, and the design/plan docs under `docs/`.
