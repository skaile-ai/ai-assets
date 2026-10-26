---
name: word
description: "A stateful Word (.docx) engine for source-free document creation, Template composition, and editing a real document in place instead of regenerating it. Its 39 tools expose a neutral blank-package baseline; identity-bound Blocks across body, headers, footers and text boxes; Named styles and resolved style audits; scoped text, paragraph and table composition; live PDF previews; core properties and Word fields; PNG/JPEG replacement and insertion; and complete Revision/Comment collaboration, including a one-call before/after review of a marked-up document. document.save backs up, verifies the OOXML Part inventory and content, and atomically replaces the target. Use it when creating a new document or preserving a Template, numbering, review markup, media and package fidelity."
version: 0.9.0 # mcp-catalog-version
transport: stdio
# Start the JVM on the first tool call, not at session start.
lazy: true
recipe:
  attr: mcps.word
command: ${recipe:word:bin}/java
args:
  - -jar
  - ${recipe:word:lib}/word-mcp.jar
env:
  # DOCX_MCP_ROOT is the path-sandbox root; the server validates it exists at
  # startup and refuses to start unbound (ADR-0006, ADR-0002 in skaile-ai/word-mcp).
  # Unlike the excel-mcp sibling this is a real per-session path, not `/` — word-mcp's
  # own design explicitly rejects an "effectively open" default root.
  # Unset on purpose: DOCX_MCP_MAX_OPEN_HANDLES (default 8; past it document.open
  # fails HANDLE_LIMIT_REACHED). document.save keeps the last 3 backups per file in
  # .docx-backups/ at the root of DOCX_MCP_ROOT, not next to the saved file, so they
  # show up as workspace-root clutter in a synced workspace.
  DOCX_MCP_ROOT: /skaile/workspace
  DOCX_MCP_SOFFICE_PATH: ${recipe:word:bin}/soffice
  JAVA_HOME: ${recipe:word}
keywords:
  - word
  - docx
  - document
  - ms-word
  - office
  - mcp
  - poi
---

# Word MCP Server

Stateful Word (.docx) engine over stdio, built on Apache POI XWPF 5.5.1, with LibreOffice
(`soffice`) for OOXML Strict conversion and PDF previews. Source, build, run instructions,
environment variables, ADRs and implementation docs: [`skaile-ai/word-mcp`](https://github.com/skaile-ai/word-mcp)
(the `word-mcp/` submodule). This directory is the catalog entry only; bump `version:` here when
adopting a release.

The agent guidance (when to use this server instead of a script, the edit loop and Template flow,
the rules that span tools) is sent by the server itself as its MCP `initialize` instructions, so
every client gets it and it always matches the running jar. Each tool's description carries that
tool's own limits and error codes. Keep this body short: the platform puts it in the prompt on every
turn.

## Capabilities

39 tools over stdio, grouped by area as in the server's instructions:

<!-- mcp-catalog-tools -->
- **Documents (9)** — `document.open`, `document.create_from`, `document.create_blank`, `document.save`, `document.close`, `document.render`, `document.get_properties`, `document.set_properties`, `document.mark_fields_dirty`
- **Structure (4)** — `outline.get`, `block.insert`, `block.set_format`, `block.delete`
- **Text (3)** — `text.find`, `text.replace`, `text.set_direct_format`
- **Named styles (3)** — `style.list`, `style.get`, `style.apply`
- **Tables (3)** — `table.get`, `table.edit`, `table.insert`
- **Headers and footers (2)** — `header.get`, `footer.get`
- **Fields (1)** — `field.insert`
- **Media (3)** — `media.list`, `media.replace`, `image.insert`
- **Revisions (6)** — `revision.track`, `revision.enable`, `revision.list`, `revision.diff`, `revision.accept`, `revision.reject`
- **Comments (4)** — `comment.list`, `comment.add`, `comment.reply`, `comment.resolve`
- **Templates (1)** — `template.find`
<!-- /mcp-catalog-tools -->
