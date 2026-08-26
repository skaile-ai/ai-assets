---
name: word-template
description: "Create or edit organization-standard Microsoft Word (.docx) documents with a bundled house template and writing guide. Use when a deliverable must preserve the template's branding, heading numbering, headers, footers, bullets, or Word-native structure; do not use for unrelated Word documents or read-only summaries."
metadata:
  version: "0.1.0"
  stage: "alpha"
  source: "NEW"
  requires:
    - "mcp:word"
keywords:
  - word
  - docx
  - template
  - house-style
  - document-production
---

# Word template

Use the installed `word` MCP dependency for every DOCX operation. Its
runtime guidance is materialized at
`.skaile/assets/mcp-server/word/MCP.md`; its source is the
[Word MCP catalog entry](../../../mcp/docx/MCP.md). Follow the installed server's
live tool descriptions for exact inputs and current capabilities. This skill
adds only the bundled template's mechanics and house writing rules.

Create a template-derived document with `document.create_from`. Preserve an
existing document with `document.open`. A generic document generator or a fresh
OOXML package does not preserve the house template.

If the `word` MCP dependency or a required tool is unavailable, report the
missing capability instead of producing a degraded substitute.

## Load the relevant detail

- Read [template workflow](references/template-workflow.md) when creating a new
  document, changing structure or formatting, setting footer metadata, or
  validating the result.
- Read [house style guide](references/house-style-guide.md) when drafting,
  rewriting, or proofreading prose.

## Core workflow

For a new document:

1. Find `assets/Standard Document Template.docx` with `template.find`, prefer the
   canonical `.skaile/assets/skill/word-template/` copy, and load a detached copy
   with `document.create_from`.
2. Read the copy with `outline.get`; use only block IDs returned for that handle.
3. Replace the title, set document properties, and compose content with the
   template's Named styles.
4. Save to an explicit new path under `DOCX_MCP_ROOT`.
5. Render and inspect a PDF, then leave final pagination and field sign-off to
   Microsoft Word.

For an existing template-derived document, use `document.open`, read its current
structure before editing, make scoped changes, save, and render. Never transfer
a handle or block ID between documents.

## Completion criteria

- The output derives from the bundled template or preserves the Parts of an
  existing template-derived document.
- Title and responsible-person properties drive the footer.
- Approved Named styles carry headings, body text, and bullets.
- Changed prose follows the house style guide.
- The requested DOCX is saved and its rendered PDF has been inspected.
- Any remaining manual Word work is disclosed.
