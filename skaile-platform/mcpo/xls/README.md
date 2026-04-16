# Excel MCP Server

MCP server (stdio) exposing Apache POI-backed Excel tools to an AI agent. Current status: **Phase 0 — bootstrap**.

See:
- `excel-mcp-server-skill.md` — design intent.
- `excel-mcp-server-implementation-plan.md` — the contract. Every tool shape, error code, and build decision is there.
- `excel-mcp-server-future-work.md` — everything explicitly deferred past v1.

## Build

**If you only want to test via Docker / MCP Inspector: skip this section.** `docker build -t excel-mcp:dev .` compiles the fat jar inside the build stage of the `Dockerfile`, so the image always contains freshly-compiled code regardless of what's in your host `target/`. See "Manual testing with the MCP Inspector" below.

The commands below are for host-side development only — running the server as `java -jar …` without Docker, running tests, and pre-commit formatting. They are independent alternatives, not a sequence.

The repo ships with the Maven Wrapper pinned to **Maven 3.9.9**. A global `mvn` is not required — and is not what this project uses. **Always invoke the build through `./mvnw` (`mvnw.cmd` on Windows).** CI and local dev must use the same pinned Maven to avoid build-reproducibility drift.

| Command | When to run it |
|---|---|
| `./mvnw verify` | Before declaring a change done. Runs compile + tests + Spotless check; also produces the fat jar. |
| `./mvnw -DskipTests package` | Only when you want the fat jar but want to skip tests. |
| `./mvnw spotless:apply` | When `./mvnw verify` fails on formatting. Auto-rewrites sources to Google Java Format. |
| `./mvnw clean` | For a truly from-scratch host rebuild. Rarely needed. |

The output is a single fat jar at `target/excel-mcp-<version>.jar`, runnable with `java -jar`.

## Run

The server speaks MCP over **stdio only** in v1. No HTTP. Launched via the shaded jar:

```bash
java -jar target/excel-mcp-0.1.0-SNAPSHOT.jar
```

Or via Docker:

```bash
docker build -t excel-mcp:dev .

# With sandboxed filesystem root (production shape):
docker run --rm -i \
  -v "$(pwd)/data:/data:rw" \
  -e EXCEL_MCP_ROOT=/data \
  -e LOG_LEVEL=INFO \
  excel-mcp:dev

# Without sandboxing (dev only; accepts any path the agent gives):
docker run --rm -i excel-mcp:dev
```

### MCP client descriptor

```json
{
  "mcpServers": {
    "excel": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "-v", "/host/data:/data:rw",
        "-e", "EXCEL_MCP_ROOT=/data",
        "excel-mcp:dev"
      ]
    }
  }
}
```

## Manual testing with the MCP Inspector

Run the server under [`@modelcontextprotocol/inspector`](https://github.com/modelcontextprotocol/inspector) to click tools by hand and watch raw request/response frames. Useful when verifying a new tool before wiring it into an agent.

1. **Rebuild the image** after any source change — `docker build` caches layers aggressively on WSL, so if behaviour looks stale, force a rebuild:

   ```bash
   cd skaile-platform/mcpo/xls/
   docker build -t excel-mcp:dev .
   # or, if you suspect a cache issue:
   docker build --no-cache -t excel-mcp:dev .
   ```

2. **Smoke-run the image standalone** (no inspector, no mounts) to confirm it starts and exits cleanly on EOF. Should log `EXCEL_MCP_ROOT not set; path sandboxing disabled` and `mcp server started … tools=<N>`:

   ```bash
   docker run --rm -i --user 1000:1000 --name excel-mcp-dev excel-mcp:dev
   ```

3. **Launch the inspector pointed at the image**, mounting a host directory as the sandbox root:

   ```bash
   # Run from skaile-platform/mcpo/xls/
   pwd && npx @modelcontextprotocol/inspector \
     docker run --rm -i --user 1000:1000 --name excel-mcp-dev \
       -v "$PWD/test-data:/data" \
       -e EXCEL_MCP_ROOT=/data \
       excel-mcp:dev
   ```

   `--user 1000:1000` makes the container write as the host user so `workbook.save` can atomically replace files in the bind-mounted directory. Adjust the UID/GID if your host account differs (see the Phase 10 stop-gate in `excel-mcp-server-future-work.md`).

4. **Open the inspector URL** printed to the terminal — typically `http://localhost:6274/?MCP_PROXY_AUTH_TOKEN=<token>`.

5. **Connect** (the inspector picks up the docker command automatically), then browse the tool list.

### Paths and handles when clicking tools

- **File paths are container-local.** The inspector is outside the container; tool arguments are evaluated inside. Host path `./test-data/file1.xlsx` is mounted at `/data/file1.xlsx` (per the `-v` above), so in `workbook.open` you pass `/data/file1.xlsx`, not a host path.
- **Workbook handles** returned by `workbook.open` / `workbook.create` look like `wb-50f0d1e7`. Copy the value verbatim into the `handle` argument of subsequent tools (`range.get`, `workbook.save`, `workbook.close`, …). Handles live for the container's lifetime — a second `docker run` starts fresh.
- Any path outside the sandbox root returns `PATH_OUTSIDE_ROOT`; missing files inside the sandbox return `FILE_NOT_FOUND`. If you see something different, the image is probably stale — rebuild with `--no-cache`.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `EXCEL_MCP_ROOT` | unset | If set, every path passed to a tool must resolve inside this subtree. Unset → all paths accepted (developer convenience; warned at startup). |
| `EXCEL_MCP_MAX_FILE_BYTES` | `100000000` (100 MB) | Upper bound on workbook file size at open. |
| `EXCEL_MCP_MAX_CELLS` | `1000000` | Upper bound on total cell count after POI loads the workbook. |
| `LOG_LEVEL` | `INFO` | Logback root level. Accepts `ERROR` / `WARN` / `INFO` / `DEBUG`. |

## v1 limits (by design)

- Formats: `.xlsx`, `.xlsm`, `.xls` are supported. `.xlsb` is **rejected at open** — see `excel-mcp-server-future-work.md` for the calamine / LibreOffice future options.
- Tool surface: 25 tools across workbook lifecycle, range I/O, sheet management, row/col mutation, tables, named ranges, and read-only VBA. No charts, no pivots, no formatting writes, no Power Query / DAX.
- Transport: stdio only. No HTTP / SSE.
- Tenancy: one process per agent session; no per-handle locking or idle-handle eviction (process death is the eviction).

## Logging

Log output goes **only to stderr** — stdio transport reserves stdout for MCP protocol messages. `System.out` is redirected to stderr at startup as a belt-and-braces measure. POI's log4j output is routed through SLF4J/Logback via `log4j-to-slf4j`.

## Production hardening (expected of the deployment)

These are delegated to whoever owns the container image:

- `--read-only` rootfs with `tmpfs:/tmp`.
- `--network=none` (the MCP itself never needs outbound traffic).
- Memory + CPU caps at the orchestrator level.
- `EXCEL_MCP_ROOT` always set.
