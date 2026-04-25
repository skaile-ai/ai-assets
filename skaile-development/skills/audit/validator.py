#!/usr/bin/env python3
"""Auto-generated validator for audit.
Re-generate with: skaile-development/compile-validators target=audit
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main

SKILL = "audit"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──
    v.must(
        "audit JSON artifact written",
        lambda: v.dir_not_empty("_devlog/reports", "audit-*.json"),
    )
    v.must(
        "audit markdown artifact written",
        lambda: v.dir_not_empty("_devlog/reports", "audit-*.md"),
    )
    v.skip("run build verification before dispatching sub-agents", rule_type="MUST",
           reason="runtime — executed during skill run, not a structural artifact")
    v.skip("stop immediately if build fails", rule_type="MUST",
           reason="runtime — control flow check, not structural")
    v.skip("read every affected package's CLAUDE.md before sub-agent dispatch", rule_type="MUST",
           reason="runtime — agent behavior")
    v.skip("dispatch logic/security/UI-UX sub-agents in parallel", rule_type="MUST",
           reason="runtime — agent orchestration")
    v.skip("classify every finding by severity and category", rule_type="MUST",
           reason="semantic — content quality")
    v.skip("skip findings already caught by Biome/ESLint/tsc/bun audit", rule_type="MUST",
           reason="semantic — content dedup judgment")

    # ── NEVER rules ──
    v.skip("modify any source files — audit is read-only", rule_type="NEVER",
           reason="runtime — enforced by agent behavior")
    v.skip("mark as pass with any critical finding", rule_type="NEVER",
           reason="semantic — verdict logic verified via JSON schema")
    v.skip("dispatch sub-agents if build or tests have already failed", rule_type="NEVER",
           reason="runtime — control flow check")

    # ── CHECKLIST ──
    v.skip("Build verified before sub-agent dispatch", rule_type="CHECKLIST",
           reason="runtime — execution step")
    v.skip("Tests verified before sub-agent dispatch", rule_type="CHECKLIST",
           reason="runtime — execution step")
    v.skip("Three sub-agents dispatched in parallel", rule_type="CHECKLIST",
           reason="runtime — orchestration")
    v.checklist(
        "JSON and markdown artifacts written",
        lambda: v.dir_not_empty("_devlog/reports", "audit-*.json"),
    )
    v.skip("Every finding has severity, file, line, description, fix", rule_type="CHECKLIST",
           reason="semantic — content completeness")
    v.skip("No files modified", rule_type="CHECKLIST",
           reason="runtime — inspect git status")
    v.skip("Verdict matches the rules", rule_type="CHECKLIST",
           reason="semantic — verdict derivation")

    return v.result()


if __name__ == "__main__":
    main(validate)
