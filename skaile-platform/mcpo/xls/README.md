# Excel MCP Server

MCP server (stdio) exposing Apache POI-backed Excel tools to an AI agent. Current status: **Phase 0 — bootstrap**.

See:
- `excel-mcp-server-skill.md` — design intent.
- `excel-mcp-server-implementation-plan.md` — the contract. Every tool shape, error code, and build decision is there.
- `excel-mcp-server-future-work.md` — everything explicitly deferred past v1.

## Build

The repo ships with the Maven Wrapper pinned to **Maven 3.9.9**. A global `mvn` is not required — and is not what this project uses. **Always invoke the build through `./mvnw` (`mvnw.cmd` on Windows).** CI and local dev must use the same pinned Maven to avoid build-reproducibility drift.

```bash
./mvnw -DskipTests package          # build the shaded fat jar (target/excel-mcp-<version>.jar)
./mvnw spotless:apply               # auto-format sources to Google Java Format
./mvnw verify                       # Spotless check + tests (the pre-commit gate)
./mvnw clean                        # wipe target/
```

The output of `package` is a single fat jar at `target/excel-mcp-<version>.jar` runnable with `java -jar`.

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
