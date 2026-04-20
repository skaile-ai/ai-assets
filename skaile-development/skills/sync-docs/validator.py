#!/usr/bin/env python3
"""Auto-generated validator for sync-docs.
Re-generate with: skaile-development/compile-validators target=sync-docs
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main

SKILL = "sync-docs"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # sync-docs doesn't always produce a file artifact — it emits a report to stdout and may
    # write frontmatter edits. The meaningful structural check is that the monorepo's CLAUDE.md
    # domain table is consistent with ai-assets/*/DOMAIN.md on disk.

    # ── MUST rules ──
    v.skip("build the full reference registry before proposing any change", rule_type="MUST",
           reason="runtime — agent behavior")
    v.skip("show a complete diff of proposed changes before applying", rule_type="MUST",
           reason="runtime — user interaction")
    v.skip("respect apply=false / apply=interactive modes", rule_type="MUST",
           reason="runtime — input handling")
    v.skip("leave unrelated frontmatter fields and content untouched", rule_type="MUST",
           reason="semantic — content preservation")
    v.skip("report orphans, never auto-delete", rule_type="MUST",
           reason="semantic — action boundary")

    # ── NEVER rules ──
    v.skip("delete pages, files, or content without explicit user approval", rule_type="NEVER",
           reason="runtime — action check")
    v.skip("rewrite prose — only touch frontmatter, links, table rows", rule_type="NEVER",
           reason="semantic — content boundary")
    v.skip("change _based_on_commit or _last_synced", rule_type="NEVER",
           reason="semantic — field ownership boundary")

    # ── CHECKLIST ──
    v.skip("Target registry built before reference scan", rule_type="CHECKLIST",
           reason="runtime — order of operations")
    v.skip("Every auto-fixable issue has a before/after diff", rule_type="CHECKLIST",
           reason="semantic — report content")
    v.skip("Orphans reported, never silently deleted", rule_type="CHECKLIST",
           reason="semantic — action boundary")
    v.skip("User approved writes", rule_type="CHECKLIST",
           reason="runtime — interaction check")
    v.skip("Only frontmatter, links, and table rows touched", rule_type="CHECKLIST",
           reason="semantic — change diff inspection")
    v.skip("Registry of fixes included in report", rule_type="CHECKLIST",
           reason="semantic — report content")

    return v.result()


if __name__ == "__main__":
    main(validate)
