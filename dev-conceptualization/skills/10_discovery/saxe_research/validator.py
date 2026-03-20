#!/usr/bin/env python3
"""Auto-generated validator for concept-1-discovery-2-research.
Re-generate with: /compile-validators concept-1-discovery-2-research
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-1-discovery-2-research"
BASE = "_concept/1_discovery/2_research"
COMPETITORS = f"{BASE}/competitors.md"
AUDIENCES = f"{BASE}/audiences.md"
DOMAIN = f"{BASE}/domain.md"
DESIGN_INSP = f"{BASE}/design_inspiration.md"
FINDINGS_IDX = f"{BASE}/findings/index.md"


def _file_contains(v, rel_path: str, pattern: str) -> tuple[bool, str]:
    """Check that a file contains a given substring (case-insensitive)."""
    text = v.read_text(rel_path)
    if text is None:
        return False, f"Cannot read {rel_path}"
    if pattern.lower() not in text.lower():
        return False, f"'{pattern}' not found in {rel_path}"
    return True, ""


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("cite sources or note evidence for all factual claims",
           reason="semantic — content quality")

    v.must("include 'Relevance to Our App' section for every competitor", lambda: (
        _file_contains(v, COMPETITORS, "Relevance")
    ))

    v.must("include 'Design Implications' section for every persona", lambda: (
        _file_contains(v, AUDIENCES, "Design Implications")
    ))

    v.must("always produce design_inspiration.md", lambda: (
        v.file_exists(DESIGN_INSP)
    ))

    v.must("save screenshots to findings/", lambda: (
        v.dir_exists(f"{BASE}/findings")
    ))

    # ── NEVER rules ──

    v.skip("make claims without web search evidence",
           rule_type="NEVER", reason="semantic — evidence quality")

    v.skip("invent competitor features or pricing",
           rule_type="NEVER", reason="semantic — factual accuracy")

    v.skip("use generic personas ('busy professional')",
           rule_type="NEVER", reason="semantic — specificity")

    v.never("skip design inspiration research", lambda: (
        v.file_exists(DESIGN_INSP)
    ))

    # ── CHECKLIST ──

    v.checklist("competitors.md has per-product Relevance section", lambda: (
        _file_contains(v, COMPETITORS, "Relevance")
    ))

    v.checklist("audiences.md has per-persona Design Implications", lambda: (
        _file_contains(v, AUDIENCES, "Design Implications")
    ))

    v.checklist("design_inspiration.md produced with layout + color + typography sections", lambda: (
        _file_contains(v, DESIGN_INSP, "Layout")
        if v.read_text(DESIGN_INSP) is not None
        else (False, f"{DESIGN_INSP} not found")
    ))

    v.skip("All factual claims cite sources",
           rule_type="CHECKLIST", reason="semantic — content quality")

    v.checklist("findings/index.md catalogs all raw material", lambda: (
        v.file_exists(FINDINGS_IDX)
    ))

    v.checklist("domain.md covers terminology and compliance", lambda: (
        v.file_exists(DOMAIN)
    ))

    return v.result()


if __name__ == "__main__":
    main(validate)
