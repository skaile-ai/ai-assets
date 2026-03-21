# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "typer>=0.12.0",
#     "rich>=13.0",
# ]
# ///
"""
extract_structure.py — Parse a docling-generated paper markdown file into structured JSON.

Usage:
    uv run extract_structure.py <paper.md>

Outputs JSON to stdout:
    {
      "title": str | null,
      "authors": [str],
      "abstract": str | null,
      "year": str | null,
      "doi": str | null,
      "venue": str | null,
      "sections": [{"heading": str, "text": str}],
      "references": [str]
    }
"""
import json
import re
from pathlib import Path

import typer
from rich.console import Console

app = typer.Typer(help="Parse a paper markdown file into structured JSON.", add_completion=False)
err = Console(stderr=True)


def _extract_title(lines: list[str]) -> str | None:
    for line in lines[:30]:
        m = re.match(r'^#{1,2}\s+(.+)', line)
        if m:
            candidate = m.group(1).strip()
            if candidate.lower() not in {"abstract", "introduction", "research", "highlights"}:
                return candidate
    return None


def _extract_authors(lines: list[str], title_line_idx: int) -> list[str]:
    """
    Authors typically appear in the 2-15 lines after the title,
    before the Abstract heading. We look for lines with name-like patterns
    (capital letters, dots, middle initials, ·, comma-separated).
    """
    author_lines: list[str] = []
    in_window = False
    for i, line in enumerate(lines):
        if i < title_line_idx:
            continue
        if i == title_line_idx:
            in_window = True
            continue
        if re.match(r'^#{1,3}\s+(Abstract|Introduction|Background)', line, re.IGNORECASE):
            break
        if in_window and line.strip():
            if re.search(r'[A-Z][a-z]+.{1,3}[A-Z][a-z]+', line) and len(line.strip()) < 300:
                author_lines.append(line.strip())
            if len(author_lines) >= 3:
                break
    raw = ' '.join(author_lines)
    parts = re.split(r'[·,]', raw)
    authors = []
    for p in parts:
        p = re.sub(r'\s*\d+\s*', ' ', p)
        p = re.sub(r'©.*', '', p).strip()   # strip copyright suffix docling includes
        p = re.sub(r'\xa0', ' ', p).strip() # collapse non-breaking spaces
        if re.search(r'[A-Z][a-z]+ [A-Z]', p) and 4 < len(p) < 80:
            authors.append(p.strip())
    return authors[:20]


def _extract_abstract(text: str) -> str | None:
    m = re.search(
        r'(?:^|\n)#{1,3}\s*Abstract\s*\n+(.*?)(?=\n#{1,3}\s)',
        text, re.IGNORECASE | re.DOTALL
    )
    if m:
        return m.group(1).strip()
    return None


def _extract_year(text: str) -> str | None:
    # Copyright line is the most reliable — it's the publication year
    m = re.search(r'©\s*The\s+Author[^\n]*?(20\d{2})', text, re.IGNORECASE)
    if m:
        return m.group(1)
    # Accepted date is closer to publication than Received
    m = re.search(r'Accepted[^\n]*?(20\d{2})', text, re.IGNORECASE)
    if m:
        return m.group(1)
    # Received date
    m = re.search(r'Received[^\n]*?(20\d{2})', text, re.IGNORECASE)
    if m:
        return m.group(1)
    # Last resort: any 4-digit year in first 500 chars
    m = re.search(r'\b(20[012]\d)\b', text[:500])
    return m.group(1) if m else None


def _extract_doi(text: str) -> str | None:
    # Standard compact DOI
    m = re.search(r'\b(10\.\d{4,9}/[\w./:;()+\-]+)', text)
    if m:
        return m.group(1).rstrip('.,)')
    # Docling sometimes space-separates DOI characters — collapse near known anchors
    doi_anchor = re.search(
        r'(?:doi|DOI|https?://doi\.org)[\s:./]*([0-9\s./\-a-zA-Z()]{10,80})', text
    )
    if doi_anchor:
        collapsed = re.sub(r'\s+', '', doi_anchor.group(1))
        m2 = re.search(r'(10\.\d{4,9}/[\w./:;()+\-]+)', collapsed)
        if m2:
            return m2.group(1).rstrip('.,)')
    return None


def _extract_venue(text: str) -> str | None:
    # Try well-known journal/publisher keywords in the first 2000 chars
    known = (
        r'European Child|Lancet|Nature|Science|Cell|JAMA|BMJ|PLOS|PNAS'
        r'|Psychiatry|Pediatrics|Neuron|Brain|Journal of|Annual Review'
        r'|NeurIPS|ICML|ICLR|ACL|EMNLP|CVPR|ICCV|ACM|IEEE'
    )
    hits = re.findall(rf'(?:{known})[\w &\-]+', text[:2000])
    if hits:
        return hits[0].strip()[:120]
    # Fallback: lines near DOI or copyright
    patterns = [
        r'(?:published in|journal of|proceedings of|conference on|symposium on)[\s:]+([^\n]{5,80})',
        r'©[^\n]*?\d{4}[^\n]*?([A-Z][a-zA-Z ]{5,60}(?:Journal|Review|Conference|Symposium|Workshop|Letters|Science|Nature|PLOS|ACM|IEEE|AAAI|NeurIPS|ICML|ICLR)[^\n]{0,40})',
    ]
    for pat in patterns:
        m = re.search(pat, text, re.IGNORECASE)
        if m:
            return m.group(1).strip()[:120]
    return None


def _extract_sections(text: str) -> list[dict]:
    parts = re.split(r'\n(#{1,3} .+)', text)
    sections = []
    i = 1
    while i < len(parts) - 1:
        heading = parts[i].lstrip('#').strip()
        body = parts[i + 1].strip() if i + 1 < len(parts) else ''
        if len(body) > 20 and heading.lower() not in {'references', 'bibliography'}:
            sections.append({'heading': heading, 'text': body[:4000]})
        i += 2
    return sections


def _extract_references(text: str) -> list[str]:
    m = re.search(
        r'\n#{1,3}\s*References?\s*\n(.*)',
        text, re.IGNORECASE | re.DOTALL
    )
    if not m:
        return []
    ref_block = m.group(1)
    refs = re.split(r'\n(?=\d+\.\s|\[\d+\])', ref_block)
    return [r.strip() for r in refs if len(r.strip()) > 10][:150]


@app.command()
def main(
    paper: Path = typer.Argument(..., help="Path to the docling-generated markdown file."),
) -> None:
    """
    Parse a scientific paper markdown file into structured JSON sections.
    Prints JSON to stdout. Always exits 0; missing fields are null.
    """
    if not paper.exists():
        typer.secho(f"Error: file not found: {paper}", err=True, fg=typer.colors.RED)
        raise typer.Exit(code=1)

    text = paper.read_text(encoding='utf-8', errors='replace')
    lines = text.splitlines()

    title_line_idx = 0
    title = None
    for i, line in enumerate(lines[:30]):
        if re.match(r'^#{1,2}\s+', line):
            candidate = re.sub(r'^#+\s+', '', line).strip()
            if candidate.lower() not in {"abstract", "introduction", "research", "highlights"}:
                title = candidate
                title_line_idx = i
                break

    result = {
        "title": title,
        "authors": _extract_authors(lines, title_line_idx),
        "abstract": _extract_abstract(text),
        "year": _extract_year(text),
        "doi": _extract_doi(text),
        "venue": _extract_venue(text),
        "sections": _extract_sections(text),
        "references": _extract_references(text),
    }

    print(json.dumps(result, indent=2, ensure_ascii=False))


if __name__ == '__main__':
    app()
