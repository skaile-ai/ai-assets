---
name: word-document
description: "Create or edit an organization's Microsoft Word (.docx) documents with the Word MCP, starting from the organization's own template when one exists and from the neutral blank baseline otherwise. Use for reports, letters, memos, minutes and other Word deliverables that must keep Named styles, numbering, headers, footers and fields intact; do not use for read-only summaries or non-.docx files."
metadata:
  version: "0.1.0"
  stage: "alpha"
  source: "NEW"
  requires:
    - "mcp:word"
keywords:
  - word
  - docx
  - document
  - template
  - named-styles
  - pdf
  - document-production
---

# Word document

Use the installed `word` MCP dependency for every DOCX operation. The server
sends its own runtime guidance as its MCP `initialize` instructions. Follow
the installed server's live tool descriptions for exact inputs and current
capabilities. This skill adds the generic production workflow and a set of
default writing rules.

If the `word` MCP dependency or a tool the request needs is unavailable, report
the missing capability instead of producing a degraded substitute. Never write a
Python or Node script, unzip the package, or regenerate an existing document
from scratch.

## When to use

- The user asks for a new Word document: a report, letter, memo, minutes,
  proposal or similar deliverable.
- The user asks to change an existing `.docx`: text, structure, tables, styles,
  properties or fields.
- The deliverable must look like the organization's other documents, so it must
  inherit the organization's template rather than re-type its formatting.

Do not use it to only read or summarize a document (`use-anydoc` is cheaper),
or for files that are not `.docx`.

## Load the relevant detail

- Read [document workflow](references/document-workflow.md) before creating or
  editing a document, adding tables, changing page layout, or exporting a PDF.
- Read [writing rules](references/writing-rules.md) when drafting, rewriting or
  proofreading prose. The organization's own style guide or template overrides
  them.

## Core workflow

1. **Find the organization's template.** Call `template.find`, which searches
   for `.docx` files under `DOCX_MCP_ROOT`, narrowed by `DOCX_MCP_TEMPLATE_GLOB`
   only where the session sets it, so it may list every document in the
   workspace. Use a file only if it is clearly the organization's template; if
   the choice is not obvious, ask the user. Never derive from an ordinary draft.
2. **Start the document.** If a template was found, call `document.create_from`
   with it. If none exists, call `document.create_blank` and tell the user the
   result uses the neutral baseline, not a house template. To change an existing
   document, call `document.open` instead and never use a create call on it.
3. **Read before writing.** Call `outline.get` and use only Block ids returned
   for that handle. Call `style.list` to learn the real Named styles.
4. **Compose with Named styles.** Insert content with `block.insert` and a real
   `style_name`, and restyle with `style.apply`. Never use manual formatting to
   imitate a style; `text.set_direct_format` is only for a property no Named
   style provides.
5. **Set properties and fields.** Set `title` and `creator` with
   `document.set_properties`, then call `document.mark_fields_dirty`.
6. **Save under `DOCX_MCP_ROOT`.** Save a new document to an explicit `.docx`
   path under `DOCX_MCP_ROOT`; a created handle has no save target. Save an
   edited document to a new path when the original must stay unchanged.
7. **Render when asked.** When the user asks for a PDF, or before handing over a
   new document, call `document.render` and inspect the result.

## Completion criteria

- A new document derives from the organization's template when one exists, or
  the user was told it uses the neutral blank baseline.
- An edited document was changed in place, preserving its Parts.
- Headings, body text, lists and tables carry Named styles, not manual
  formatting.
- Changed prose follows the organization's style guide, or the default writing
  rules where it has none.
- The DOCX is saved under `DOCX_MCP_ROOT`; a requested PDF was rendered.
- Any capability the server lacks, and any remaining manual Word work, is
  disclosed.
