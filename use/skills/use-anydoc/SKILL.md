---
name: "use-anydoc"
description: "Use when you need to read the contents of an office document fast - Word, PowerPoint, Excel, OpenDocument, RTF, EPUB, CSV, or a digital-native PDF - and convert it to Markdown for orientation, summarization, or data extraction. Pure-Rust CLI, no API key, no OCR, no ML models. Read-only: it cannot edit a document, and it drops formulas and cell addresses, so it is not a substitute for mcp:excel or mcp:ppt when the task is to modify a file."
metadata:
  stage: "alpha"
  source: "NEW"
  requires:
    - "contract:use-contract"
---

# Use anydoc

Converts office documents to GitHub-Flavored Markdown via the
[anydoc](https://github.com/firecrawl/anydoc) CLI (Firecrawl, MIT). Pure Rust, runs
entirely locally, no API key and no rate limits. Its niche is **fast read-only
extraction from digital-native documents**.

**CRITICAL**: You invoke `anydoc` directly with the `bash` tool. There are no wrapper scripts.

## Pick the right tool first

This skill overlaps two others. Route before you convert:

| Your input / goal | Use | Why |
|---|---|---|
| Digital-native office doc, you only need to **read** it | **`use-anydoc`** | Pure Rust, no ML, millisecond-scale |
| Scanned PDF, photo, screenshot, audio, anything needing **OCR / VLM / speech** | **`use-docling`** | anydoc has no OCR, no VLM, no ASR |
| An Excel workbook you will **edit**, or where **formulas** matter | **`mcp:excel`** | anydoc is read-only and drops formulas |
| A PowerPoint deck you will **edit or render** | **`mcp:ppt`** | anydoc is read-only and cannot render |

anydoc also covers three formats our MCP servers reject outright: **`.xlsb`**
(`mcp:excel` rejects it at open - POI has no binary-workbook reader), and **`.ppt`
(legacy binary) / `.odp`** (`mcp:ppt` does not support either). For those files
anydoc is the only read path we have.

## Setup

No install and no API key. `npx` fetches the package on first use:

```bash
npx -y @firecrawl/anydoc --help
```

If the session container blocks npm registry egress, this fails at the point of use -
fall back to `use-docling`.

## Usage

```bash
npx -y @firecrawl/anydoc report.docx                 # convert to stdout
npx -y @firecrawl/anydoc slides.pptx -o slides.md    # convert to a file
npx -y @firecrawl/anydoc - --format csv < data.csv   # read from stdin
```

**For anything but a small document, write to a file with `-o` and read the parts you
need.** anydoc converts the *whole* document in one pass with no pagination or cell
cap; streaming a large workbook straight into context will flood it.

## Supported formats

Word (`.doc`, `.docx`, `.docm`), PowerPoint (`.ppt`, `.pps`, `.pot`, `.pptx`, `.pptm`,
`.ppsx`, `.ppsm`), Excel (`.xls`, `.xlsx`, `.xlsm`, `.xlsb`), OpenDocument (`.odt`,
`.ods`, `.odp`), RTF, EPUB, CSV, PDF.

## What the conversion drops

Every format serializes through one Markdown generator, so structure survives
(headings, lists, tables with merged cells, footnotes) but presentation and semantics
do not. For spreadsheets specifically:

- **Formulas are gone.** anydoc reads calamine's cached cell values only. It never
  reads formula text.
- **Cell addresses are gone, and this is a trap.** The Markdown grid starts at the
  sheet's **used range**, not at A1, and the offset is not in the output. A sheet whose
  data begins at D11 produces a table whose first column is D and first row is 11, with
  nothing saying so. **Never translate a position in anydoc's output into an A1
  address for a write.** Re-locate the cell with `mcp:excel` (`range.get`,
  `table.list`, `named_range.list`) before editing anything.
- **Number formats are dropped.** A percentage cell displaying `15.5%` converts as
  `0.155`; a currency cell as `1234.5`. You get the stored value, not the rendering.
- **Uncomputed formulas render as empty cells.** `mcp:excel` deliberately clears the
  cached value of every freshly written formula (that is what its
  `type: "formula_uncomputed"` means). A workbook saved without
  `workbook.recalculate` therefore shows *blanks* through anydoc. If you converted a
  file our own tooling just wrote, recalculate and save before trusting the output.
- Named ranges, tables/ListObjects, styling, and VBA are not represented at all.

Sheets become one Markdown table each, with an `## SheetName` heading only when the
workbook has more than one sheet. Empty sheets are skipped. Error cells render as
`#Error`.

## Output contract

Markdown on stdout, or a `.md` file at `-o`. Per the `use-contract`, report the output
path and any skipped/unreadable sheets back to the caller rather than silently
returning partial content - anydoc logs a warning and continues when a single sheet
fails to parse.
