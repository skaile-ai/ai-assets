---
name: excel
description: "Read/write Excel workbooks (.xlsx/.xlsm/.xls) via Apache POI. 26 tools: workbook lifecycle, range I/O, sheet management, tables, named ranges, VBA extraction."
version: 0.1.0
transport: stdio
command: docker
args:
  - run
  - --rm
  - -i
  - -v
  - "${HOME}:/data:rw"
  - -e
  - EXCEL_MCP_ROOT=/data
  - excel-mcp:dev
env:
  EXCEL_MCP_ROOT: /data
keywords:
  - excel
  - xlsx
  - xlsm
  - xls
  - spreadsheet
  - workbook
  - mcp
  - poi
---

# Excel MCP Server

Docker-based MCP server for Excel file operations, built on Apache POI 5.5.1.

Provides 26 tools across workbook lifecycle, range I/O, sheet management,
row/column mutation, tables, named ranges, and read-only VBA module extraction.

## Prerequisites

The Docker image must be built locally before first use:

```bash
cd ai-assets/skaile-platform/mcpo/xls
docker build -t excel-mcp:dev .
```

## Override examples

Override the data mount in `skaile.yaml`:

```yaml
dependencies:
  - mcp:excel

mcp_servers:
  - id: excel
    args: [run, --rm, -i, -v, "/projects:/data:rw", -e, EXCEL_MCP_ROOT=/data, excel-mcp:dev]
```
