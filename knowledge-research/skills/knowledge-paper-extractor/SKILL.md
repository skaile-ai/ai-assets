---
name: "knowledge-paper-extractor"
description: "Use when you need to extract metadata (title, authors, abstract) and references from scientific PDF papers or book chapters."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "knowledge-research-contract"
---

# Knowledge Paper Extractor

Extracts structured metadata and bibliographic references from scientific PDF documents.

## Goal

Given a scientific paper or book chapter in PDF format, produce structured output containing:
- **Title**, **authors**, and **abstract** of the document
- **All cited references** with structured fields (author, title, DOI, journal, year)

## Core Workflow

1. Receive or identify the target PDF file path.
2. Run the extraction script.
3. Review the generated output files in the output directory.

## Script Integration

Extract metadata and references from any scientific PDF:

```bash
uv run .agent/skills/knowledge-paper-extractor/scripts/extract_paper.py /path/to/paper.pdf --output-dir ./output
```

### Output Files

The script creates three files in the output directory:

| File | Content |
|---|---|
| `metadata.json` | Title, authors, abstract, and raw PDF metadata |
| `references.json` | Structured list of all cited references via `refextract` |
| `references.csl.json` | CSL-JSON resolved references via Crossref API |
| `summary.md` | Human-readable Markdown summary with DOIs |

### CLI Options

```
uv run scripts/extract_paper.py <PDF_PATH> [--output-dir DIR]
```

- `PDF_PATH` — Path to the scientific PDF (required)
- `--output-dir` — Output directory (default: `./output`)

## How It Works

- **References**: Extracted via `refextract`, which parses the reference section of scholarly PDFs and returns structured bibliographic data.
- **Metadata**: Extracted via `pdfminer.six` (first-page text with layout/font analysis) and `pypdf` (embedded PDF metadata). Title is identified by the largest font on page 1; abstract is located between "Abstract" and the next major section heading.

## Constraints

- Input must be a PDF file (not DOCX, HTML, etc.).
- Heuristic metadata extraction works best on standard academic paper layouts (two-column or single-column with clear title/abstract sections).
- `refextract` requires that the PDF contains a recognizable reference/bibliography section.
