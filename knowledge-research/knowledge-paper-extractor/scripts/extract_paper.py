# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "typer>=0.12.0",
#     "refextract>=0.2.5",
#     "setuptools<81",
#     "pdfminer.six>=20221105",
#     "pypdf>=4.0",
#     "httpx>=0.27.0",
# ]
# ///

"""
Extract metadata (title, authors, abstract) and cited references from a
scientific PDF paper or book chapter.

Outputs:
  metadata.json        — title, authors, abstract, pdf_metadata
  references.json      — structured list of cited references
  references.csl.json  — resolved CSL-JSON via Crossref API
  summary.md           — human-readable Markdown summary
"""

from __future__ import annotations

import asyncio
import json
import re
import sys
from pathlib import Path
import urllib.parse

import httpx
import typer
from pdfminer.high_level import extract_pages
from pdfminer.layout import LAParams, LTChar, LTTextBox, LTTextLine
from pypdf import PdfReader

app = typer.Typer(
    help="Extract metadata and references from scientific PDF papers.",
    add_completion=False,
)


# ---------------------------------------------------------------------------
# Metadata extraction helpers
# ---------------------------------------------------------------------------

def _get_pdf_metadata(pdf_path: Path) -> dict:
    """Read embedded PDF metadata via pypdf."""
    try:
        reader = PdfReader(str(pdf_path))
        meta = reader.metadata
        if meta is None:
            return {}
        return {
            "pdf_title": meta.title or "",
            "pdf_author": meta.author or "",
            "pdf_subject": meta.subject or "",
            "pdf_creator": meta.creator or "",
        }
    except Exception:
        return {}


def _extract_first_page_elements(pdf_path: Path) -> list[dict]:
    """
    Extract text elements from the first page with their font sizes and
    vertical positions, using pdfminer.six layout analysis.
    Returns a list of dicts: {text, font_size, y_pos}
    """
    elements: list[dict] = []
    laparams = LAParams(line_margin=0.5, word_margin=0.1)

    for page_layout in extract_pages(str(pdf_path), maxpages=1, laparams=laparams):
        for element in page_layout:
            if isinstance(element, LTTextBox):
                for line in element:
                    if isinstance(line, LTTextLine):
                        text = line.get_text().strip()
                        if not text:
                            continue
                        # Determine dominant font size in this line
                        sizes: list[float] = []
                        for char in line:
                            if isinstance(char, LTChar):
                                sizes.append(char.size)
                        font_size = max(sizes) if sizes else 0.0
                        elements.append({
                            "text": text,
                            "font_size": round(font_size, 2),
                            "y_pos": round(line.y0, 2),
                        })
    # Sort top-to-bottom (highest y first)
    elements.sort(key=lambda e: -e["y_pos"])
    return elements


def _heuristic_parse_metadata(elements: list[dict], pdf_meta: dict) -> dict:
    """
    Heuristically determine title, authors, and abstract from first-page
    text elements ordered top-to-bottom.
    """
    if not elements:
        return {
            "title": pdf_meta.get("pdf_title", ""),
            "authors": pdf_meta.get("pdf_author", ""),
            "abstract": "",
        }

    # ── Title: largest font text on page 1 ──
    max_font = max(e["font_size"] for e in elements)
    title_parts: list[str] = []
    title_threshold = max_font * 0.90  # allow slight variation
    title_end_idx = 0

    for i, el in enumerate(elements):
        if el["font_size"] >= title_threshold and len(el["text"]) > 3:
            title_parts.append(el["text"])
            title_end_idx = i
        elif title_parts:
            # Stop collecting title once font drops
            break

    title = " ".join(title_parts) if title_parts else pdf_meta.get("pdf_title", "")

    # ── Authors: text block after title, before abstract ──
    author_parts: list[str] = []
    abstract_start_idx = None

    for i in range(title_end_idx + 1, len(elements)):
        text_lower = elements[i]["text"].lower()
        if re.match(r"^abstract\b", text_lower):
            abstract_start_idx = i
            break
        # Heuristic: author lines are typically mid-size font, contain commas
        # or common name patterns, and come before the abstract
        if elements[i]["font_size"] < max_font:
            author_parts.append(elements[i]["text"])

    authors = "; ".join(author_parts) if author_parts else pdf_meta.get("pdf_author", "")

    # ── Abstract: text between "Abstract" heading and next section ──
    abstract_parts: list[str] = []
    if abstract_start_idx is not None:
        # Skip the "Abstract" heading itself
        start = abstract_start_idx + 1
        # But if "Abstract" line contains inline text, grab it
        abstract_heading_text = elements[abstract_start_idx]["text"]
        inline = re.sub(r"(?i)^abstract[:\s.\-—]*", "", abstract_heading_text).strip()
        if inline:
            abstract_parts.append(inline)

        section_patterns = re.compile(
            r"^(1[\.\s]|I[\.\s]|introduction|keywords|key\s*words|"
            r"background|methods|related\s*work|overview)",
            re.IGNORECASE,
        )
        for i in range(start, len(elements)):
            text = elements[i]["text"]
            if section_patterns.match(text):
                break
            abstract_parts.append(text)

    abstract = " ".join(abstract_parts).strip()

    return {
        "title": title,
        "authors": authors,
        "abstract": abstract,
    }


# ---------------------------------------------------------------------------
# Reference extraction
# ---------------------------------------------------------------------------

def _extract_references(pdf_path: Path) -> list[dict]:
    """Extract references using refextract."""
    try:
        from refextract import extract_references_from_file
        refs = extract_references_from_file(str(pdf_path))
        return refs if isinstance(refs, list) else []
    except Exception as exc:
        typer.secho(f"Warning: refextract failed: {exc}", err=True, fg=typer.colors.YELLOW)
        return []


# ---------------------------------------------------------------------------
# API Resolution
# ---------------------------------------------------------------------------

def _clean_raw_ref(raw: list[str] | str | None) -> str:
    """Extract and clean the raw reference string from refextract output."""
    if not raw:
        return ""
    text = raw[0] if isinstance(raw, list) else str(raw)
    # Remove leading numbering like "1. " or "[1] "
    text = re.sub(r"^\[?\d+\]?\.?\s+", "", text).strip()
    return text


async def _fetch_crossref_csl(client: httpx.AsyncClient, query: str, sem: asyncio.Semaphore) -> dict | None:
    """Fetch the best matching CSL-JSON from Crossref for a given citation string."""
    if not query:
        return None
        
    url = "https://api.crossref.org/works"
    params = {
        "query.bibliographic": query,
        "select": "DOI,title,author,issued,container-title,volume,issue,page,URL,type,publisher",
        "rows": 1
    }
    
    async with sem:
        try:
            # Setting a user-agent is polite and prevents some rate limits
            headers = {"User-Agent": "AgentPaperExtractor/1.0 (mailto:agent@example.com)"}
            response = await client.get(url, params=params, headers=headers, timeout=10.0)
            response.raise_for_status()
            data = response.json()
            items = data.get("message", {}).get("items", [])
            if items:
                item = items[0]
                # Keep the original query string for traceability
                item["_original_query"] = query
                return item
            return None
        except Exception as exc:
            # We silently fail individual fetches so we don't crash the whole batch
            return {"_original_query": query, "_error": str(exc)}


async def _resolve_all_references(references: list[dict]) -> list[dict]:
    """Concurrently resolve all parsed references against the Crossref API."""
    # Deduplicate raw query strings to minimize API calls
    queries = []
    seen = set()
    for ref in references:
        q = _clean_raw_ref(ref.get("raw_ref"))
        if q and q not in seen:
            queries.append(q)
            seen.add(q)
            
    # Max 5 concurrent requests to be nice to Crossref
    sem = asyncio.Semaphore(5)
    
    async with httpx.AsyncClient() as client:
        tasks = [_fetch_crossref_csl(client, q, sem) for q in queries]
        results = await asyncio.gather(*tasks)
        
    return [r for r in results if r is not None]


# ---------------------------------------------------------------------------
# Output writers
# ---------------------------------------------------------------------------

def _write_metadata_json(output_dir: Path, metadata: dict, pdf_meta: dict) -> Path:
    path = output_dir / "metadata.json"
    payload = {**metadata, "pdf_metadata": pdf_meta}
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False))
    return path


def _write_references_json(output_dir: Path, references: list[dict]) -> Path:
    path = output_dir / "references.json"
    path.write_text(json.dumps(references, indent=2, ensure_ascii=False))
    return path


def _write_references_csl_json(output_dir: Path, csl_items: list[dict]) -> Path:
    path = output_dir / "references.csl.json"
    path.write_text(json.dumps(csl_items, indent=2, ensure_ascii=False))
    return path


def _write_summary_md(
    output_dir: Path, metadata: dict, references: list[dict], csl_items: list[dict], pdf_name: str
) -> Path:
    path = output_dir / "summary.md"
    lines = [
        f"# {metadata.get('title', 'Untitled')}",
        "",
        f"**Source PDF**: `{pdf_name}`",
        "",
        "## Authors",
        "",
        metadata.get("authors", "_not detected_"),
        "",
        "## Abstract",
        "",
        metadata.get("abstract", "_not detected_"),
        "",
        f"## References ({len(references)} found)",
        "",
    ]
    for i, ref in enumerate(references, 1):
        # refextract returns various keys; build a readable line
        raw = ref.get("raw_ref", [""])
        raw_text = raw[0] if isinstance(raw, list) and raw else str(raw)
        
        # Try to find matching CSL item to append a DOI/URL link if available
        csl_link = ""
        clean_q = _clean_raw_ref(raw_text)
        for item in csl_items:
            if item.get("_original_query") == clean_q:
                if "DOI" in item:
                    csl_link = f" [DOI: {item['DOI']}](https://doi.org/{item['DOI']})"
                elif "URL" in item:
                    csl_link = f" [URL]({item['URL']})"
                break
                
        lines.append(f"{i}. {raw_text}{csl_link}")

    path.write_text("\n".join(lines), encoding="utf-8")
    return path


# ---------------------------------------------------------------------------
# Main CLI command
# ---------------------------------------------------------------------------

@app.command()
def main(
    pdf_path: str = typer.Argument(..., help="Path to the scientific PDF file."),
    output_dir: str = typer.Option(
        "./output", "--output-dir", "-o", help="Directory to write output files."
    ),
) -> None:
    """
    Extract title, authors, abstract, and all cited references from a
    scientific PDF paper or book chapter.

    Outputs metadata.json, references.json, and summary.md into the
    specified output directory.
    """
    pdf = Path(pdf_path)
    if not pdf.is_file():
        typer.secho(f"Error: file not found: {pdf_path}", err=True, fg=typer.colors.RED)
        raise typer.Exit(code=1)
    if pdf.suffix.lower() != ".pdf":
        typer.secho(f"Error: not a PDF file: {pdf_path}", err=True, fg=typer.colors.RED)
        raise typer.Exit(code=1)

    out = Path(output_dir)
    out.mkdir(parents=True, exist_ok=True)

    typer.secho(f"Processing: {pdf.name}", err=True, fg=typer.colors.CYAN)

    # Step 1: PDF metadata
    typer.secho("  → Reading PDF metadata...", err=True)
    pdf_meta = _get_pdf_metadata(pdf)

    # Step 2: First-page text analysis
    typer.secho("  → Analyzing first page layout...", err=True)
    elements = _extract_first_page_elements(pdf)
    metadata = _heuristic_parse_metadata(elements, pdf_meta)

    # Step 3: Reference extraction
    typer.secho("  → Extracting references (this may take a moment)...", err=True)
    references = _extract_references(pdf)

    # Step 4: Resolve against Crossref API
    typer.secho("  → Resolving references via Crossref API...", err=True)
    csl_items = asyncio.run(_resolve_all_references(references))

    # Step 5: Write outputs
    meta_path = _write_metadata_json(out, metadata, pdf_meta)
    refs_path = _write_references_json(out, references)
    csl_path = _write_references_csl_json(out, csl_items)
    summary_path = _write_summary_md(out, metadata, references, csl_items, pdf.name)

    # JSON result on stdout
    result = {
        "status": "success",
        "pdf": str(pdf.resolve()),
        "title": metadata.get("title", ""),
        "authors": metadata.get("authors", ""),
        "abstract_length": len(metadata.get("abstract", "")),
        "references_count": len(references),
        "resolved_csl_count": len([i for i in csl_items if "_error" not in i]),
        "output_files": {
            "metadata": str(meta_path.resolve()),
            "references": str(refs_path.resolve()),
            "references_csl": str(csl_path.resolve()),
            "summary": str(summary_path.resolve()),
        },
    }
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    app()
