---
name: ppt
description: "Stateful PowerPoint manipulation via Apache POI 5.5.x + LibreOffice. 52 tools: workbook lifecycle, slides, text, shapes, tables, images, charts, render to PNG, export to PDF."
version: 1.0.0
transport: stdio
recipe:
  attr: mcps.ppt
command: ${recipe:ppt:bin}/java
args:
  - -jar
  - ${recipe:ppt:lib}/ppt-mcp.jar
env:
  MCPO_ALLOWED_ROOT: /skaile/workspace
  SOFFICE_PATH: ${recipe:ppt:bin}/soffice
  JAVA_HOME: ${recipe:ppt}
keywords:
  - powerpoint
  - pptx
  - presentation
  - slides
  - libreoffice
  - mcp
  - poi
---

# PowerPoint MCP Server

MCP server for PowerPoint file operations, built on Apache POI 5.5.x with LibreOffice
for PDF/image export and high-fidelity rendering.

Provides 52 tools across document lifecycle, slide management, text/shape/table/image
manipulation, chart editing, rendering to PNG, and export to PDF/HTML.

## Runtime

Built and pinned by the platform Nix flake (`platform/nix/flake.nix`'s `mcps.ppt` derivation).
At session start the runner resolves `${recipe:ppt}` to the closure's `/nix/store` path. The
recipe bundles both the JRE (`$out/bin/java`) and LibreOffice (`$out/bin/soffice`) — no
`docker build` step required for platform-deployed sessions.

For local standalone testing without the platform: build the docker image
(`docker build -t ppt-mcp:dev .`) and override `command`/`args` in `skaile.yaml`'s
`mcp_servers:` block.

## Override examples

Override command and workspace root in `skaile.yaml` for standalone use:

```yaml
dependencies:
  - mcp:ppt

mcp_servers:
  - id: ppt
    command: docker
    args: [run, --rm, -i, -v, "/projects:/workspace/resources:rw", -e, MCPO_ALLOWED_ROOT=/workspace/resources, ppt-mcp:dev]
```
