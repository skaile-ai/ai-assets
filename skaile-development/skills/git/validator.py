#!/usr/bin/env python3
"""Auto-generated validator for git.
Re-generate with: skaile-development/compile-validators target=git

This validator is a compact version — the git skill performs runtime git
operations and produces no deterministically-checkable JSON artifact in cwd, so
every rule is classified as semantic/runtime and skipped. When this skill starts
producing structured JSON artifacts, replace the skip() calls with structural checks.
"""
import sys
from pathlib import Path

_HERE = Path(__file__).resolve()
for _cand in (
    _HERE.parents[4] / "ai-assets-skaileup" / "contracts" / "scripts",           # current
    _HERE.parents[4] / "ai-assets-skaileup" / "skaileup-contracts" / "scripts",  # legacy fallback
    _HERE.parents[3] / "skaileup-shared" / "scripts",                            # legacy fallback
):
    if (_cand / "validator_lib.py").exists():
        sys.path.insert(0, str(_cand))
        break
from validator_lib import Validator, main

SKILL = "git"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules (runtime git operations — skipped) ──
    v.skip("follow the structured commit message format from references/commit-spec.md", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("derive branch name from description using references/branch_naming.md rules", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("check for dirty working tree before creating branches or worktrees", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("require typed confirmation for destructive operations (force-delete, discard)", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("write git-state.json when branch or worktree is created", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("run full test suite before any merge to main", rule_type="MUST", reason="see SKILL.md — semantic/runtime")

    # ── CHECKLIST (runtime git operations — skipped) ──
    v.skip("Commit messages follow references/commit-spec.md format", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Branch name follows naming convention from references/branch_naming.md", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Working tree was clean before branch/worktree creation", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Tests pass before merge or PR", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Typed confirmation received for merge and discard", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Worktree cleaned up after finish (merge, keep, discard)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("git-state.json written/updated", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("submodule URLs normalized to HTTPS on deploy after merge (no git@github.com: remains in .gitmodules)", rule_type="CHECKLIST", reason="see SKILL.md — runtime git operation on the deploy branch, not a cwd artifact")
    return v.result()


if __name__ == "__main__":
    main(validate)
