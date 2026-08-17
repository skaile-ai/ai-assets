---
name: word
description: "A stateful Word (.docx) engine an agent edits in place - not a file it regenerates. Opens an existing .docx behind a handle and mutates it through addressable Blocks (paragraphs, headings, tables) instead of the 'emit a whole new file' pattern that discards a document's template, headers, footers, page numbering and images on every edit. Every Block gets an opaque id plus its own computed Number (resolved by walking numId/ilvl through the style chain, never guessed from run text), so 'the last table' or 'section 13.2' addresses one Block, not the whole document. Named styles are the primary text operation - an undefined style name fails STYLE_NOT_FOUND with the document's real style inventory rather than a fuzzy match or an invented style; direct formatting (bold/size/color/font) is reachable only through one explicitly-named escape hatch. document.save backs up, writes to temp, verifies against the source by Part list and uncompressed content (never file size, which zip recompression alone shifts by design), then atomically renames - a failed verification leaves the original untouched. 14 tools across document lifecycle, outline/addressing, text, style, tables, Revision/Comment reading, and Template discovery."
version: 0.1.0-SNAPSHOT # mcp-catalog-version
transport: stdio
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

Docker/Nix-based MCP server for Word document operations, built on Apache POI XWPF 5.5.1.

> **Source code:** the server source, build (`pom.xml`, `flake.nix`, `Dockerfile`,
> `mvnw`), fixture-suite tests, ADRs and implementation docs live in their own repo,
> [`skaile-ai/word-mcp`](https://github.com/skaile-ai/word-mcp) (a submodule at the
> workspace root as `word-mcp/`). This directory is the **catalog entry only** —
> `MCP.md`. Versioning/PRs/issues happen in that repo; bump `version:` here when
> adopting a new release.

## When to reach for this

- The user asks to read, inspect, summarize, edit, or verify a Word document (`.docx`).
- The task is a **correction to an existing document** — a heading's text, one table's
  contents, a paragraph's style — not a from-scratch document. This server edits in place;
  regenerating the whole file is a data-loss pattern it exists to stop.
- The task needs a document to inherit an org Template's look (fonts, palette, header/footer,
  numbering) rather than have it re-typed as literal formatting.
- The task needs the document's real structure — its heading hierarchy with computed section
  numbers, or a table's actual row/column/merge shape — not text with the layout fused away.

## When NOT to reach for this

- **A genuinely new document with no existing file to build on.** `document.create_from(the
  org Template)` plus edits is the closest fit; there is no from-scratch "create" tool by
  design (regeneration is deliberately not offered as a shortcut).
- **The file isn't a `.docx`** (`.xlsx`, `.pptx`, `.pdf`, `.odt`, `.rtf`) — use the matching
  sibling MCP (`excel`, `ppt`) or `use-anydoc` / `use-docling` for read-only extraction of
  other formats.
- **OOXML Strict documents** (rare; usually produced by non-Microsoft tooling) can't be opened
  at all without the `allow_convert` fallback — see Limitations.
- **Quick prose reading with no edit intent** — `use-anydoc` is cheaper for "what does this
  document say" when nothing needs to change.

## Capabilities

14 tools over stdio, grouped by area:

- **Document lifecycle (3)** — `document.open` (handle + `allow_convert` for OOXML Strict),
  `document.save` (backup → temp → Part-list/content verify → atomic rename),
  `document.create_from` (byte-for-byte copy into scratch, no save target — the from-scratch path)
- **Outline / addressing (1)** — `outline.get` (every Block in order: `block_id`, `kind`,
  `style`, `label`, computed `number`)
- **Text (3)** — `text.find` (literal matches with Block id + context), `text.replace`
  (scoped to an explicit Block/Section, never whole-document), `text.set_direct_format` (the
  one bold/size/color/font escape hatch)
- **Style (2)** — `style.apply` (Named style by id, `STYLE_NOT_FOUND` with the real inventory
  on a miss), `style.list`
- **Tables (2)** — `table.get` (rows/cells/merges as structure), `table.edit`
  (`set_cell_text` / `delete_row`, fenced to one table's Block id)
- **Revision / Comment, read-only (2)** — `revision.list` (`w:ins`/`w:del`/`w:moveFrom`/
  `w:moveTo`/`w:rPrChange`), `comment.list` (anchored ranges) — both preserved unchanged
  through a save; neither is authored
- **Template (1)** — `template.find` (discovers `.docx` files under the sandbox root via a
  neutral glob, for use as `document.create_from`'s source)

Highlights: Block ids bound to POI object identity (stable across an edit elsewhere in the
document, never written into the file); Number resolution across both literal-heading-text and
style-chain-numbered documents, `null` rather than a guess when a format is unsupported; a
save-time Part-loss guard that only a tool's own declared removals can satisfy — no
`allow_part_loss` escape hatch exists anywhere in the design.

## Limitations

- **No Revision authoring.** Revisions and Comments are read/preserved only; there is no
  `revision.enable`, and no tool sets `w:trackChanges` — POI writes would land untracked
  regardless of the flag, so the server refuses to claim otherwise.
- **No on-demand style creation.** An undefined style name is always an error with the real
  inventory attached, never invented or fuzzy-matched.
- **`text.find`/`text.replace` don't reach table-cell text.** Only paragraph Blocks; edit table
  content through `table.edit` instead.
- **No image insertion, header/footer editing, or numbering-definition authoring.** Reading
  and in-place text/style/table edits only.
- **No rendering or export** to image/PDF — unlike the `ppt` sibling, `soffice` here exists
  solely for the OOXML Strict conversion fallback below.
- **OOXML Strict documents cannot be opened directly** (POI bug #57699). `document.open` fails
  `OOXML_STRICT_UNSUPPORTED` unless `allow_convert: true` is passed, which opens a
  `soffice`-converted scratch copy instead; that handle carries no save target, so it can never
  be renamed over the Strict original.
- **Handle registry is process-local, no idle-TTL eviction.** A handle lives for the server
  process's lifetime; past `DOCX_MCP_MAX_OPEN_HANDLES` (default 8) a further `document.open`
  fails `HANDLE_LIMIT_REACHED`.

## Runtime

Built and pinned by the platform Nix flake (`platform/nix/flake.nix`'s `mcps.word`
derivation, which sources `word-mcp`'s own self-contained `flake.nix`). At session start the
runner resolves `${recipe:word}` to the closure's `/nix/store` path — the closure bundles the
JRE and a `soffice` binary (LibreOffice) for the Strict-conversion fallback. No `docker build`
step required for platform-deployed sessions.

For local standalone testing without the platform: clone
[`skaile-ai/word-mcp`](https://github.com/skaile-ai/word-mcp), build the docker image there
(`docker build -t word-mcp:dev .`), and override `command`/`args` in `skaile.yaml`'s
`mcp_servers:` block.

## Override examples

Override command and workspace root in `skaile.yaml` for standalone use:

```yaml
dependencies:
  - mcp:word

mcp_servers:
  - id: word
    command: docker
    args: [run, --rm, -i, -v, "/projects:/workspace:rw", -e, DOCX_MCP_ROOT=/workspace, word-mcp:dev]
```

## Core flow

A typical session looks like:

```
document.open (path, or allow_convert:true for OOXML Strict)
  → outline.get                          # every Block: id, style, label, computed number
  → text.find / table.get                # locate the Block(s) that need to change
  → text.replace / style.apply /         # write, fenced to an explicit Block or Section
    table.edit / text.set_direct_format
  → document.save                        # backup, temp, verify, atomic rename
```

Producing a fresh document from an org Template instead of editing an upload:

```
template.find                            # discover the org Template via DOCX_MCP_TEMPLATE_GLOB
  → document.create_from (source)        # byte-for-byte copy into scratch, no save target
  → outline.get / style.apply / ...      # edit the copy exactly like an opened document
  → document.save (path: <new destination>)  # required — a create_from handle has none
```

## Non-obvious gotchas the agent must respect

- **Regeneration is not a tool.** There is no "rewrite this document" call. A from-scratch
  document is always `document.create_from(<a real source, usually a Template>)` plus edits —
  never hand-assembled or produced by another library and handed to this server to "fix up."
- **A Number is computed, never text.** A heading whose visible number is `"13.2"` may carry
  that literal text in `label`, or carry bare text with the number resolved from
  `numbering.xml` via the style chain — `outline.get`'s `number` field is `null`, not a guess,
  when the format isn't one of the resolved set (`decimal`, `lowerLetter`, `upperLetter`,
  `lowerRoman`, `upperRoman`, bullets).
- **`text.replace` needs an explicit scope, always.** There is no whole-document replace; a
  `block_id` (from `outline.get` or `text.find`) is required, and `include_subtree` only
  widens to a heading's Section when `block_id` names a heading.
- **Style application never invents or fuzzy-matches.** `style.apply` with an undefined
  `style_name` fails `STYLE_NOT_FOUND` and hands back the document's actual style inventory —
  retry with a real name from that list, don't guess a close one.
- **Direct formatting is one call, on purpose.** `text.set_direct_format` is the only place
  bold/size/color/font can be set directly; every other text operation goes through Named
  styles, so bypassing the house style is always a deliberate, visible choice.
- **A `create_from` (or Strict-converted) handle can't overwrite its source.** `document.save`
  against one without an explicit destination fails `SAVE_TARGET_MISSING` — it can never
  resolve to the Template or the Strict original.
- **`PART_LOSS` has no override.** An unexplained missing Part after a save is always a hard
  refusal; only a tool's own declared removal (e.g. `table.edit`'s `delete_row` dropping an
  image it owns) is accounted for. There is no flag to suppress this check.
- **Sandbox is fail-closed.** The server refuses to start without `DOCX_MCP_ROOT` unless
  `DOCX_MCP_ALLOW_UNSANDBOXED=true` is explicitly set — an opt-in with no default, logged
  loudly at startup when active, for local development only.

## Reference documents (in the [`word-mcp`](https://github.com/skaile-ai/word-mcp) repo)

- `README.md` — full tool inventory, env-var reference, error-code table, run instructions.
- `CONTEXT.md` — the domain glossary (Block, Section vs. Layout section, Named style vs.
  Direct formatting, Revision vs. Comment vs. Track-changes mode) the tool surface is named
  after.
- `docs/adr/0001`–`0006` — the six hard-to-reverse design decisions (engine choice, edit vs.
  regeneration, named styles vs. direct formatting, Block id identity, save protocol,
  deployment neutrality).
- `docs/testing.md` — what `mvn verify` gates and the fixture-suite convention.
- `_devlog/` — one dated entry per landed change, each linking its issue/PR.
