#!/usr/bin/env python3
"""
Validator for the session-review skill.

Checks that session-review output contains all four required blocks and
that any persisted JSON report has the expected fields.

Usage:
    python3 validator.py <output_file_or_stdin>

Known Limitations:
    - compact_before_agent: This metric is a coarse first-version approximation. It confirms
      that compaction occurred at some point before some agent dispatch in the session, not
      that each individual dispatch was immediately preceded by a compact. A future version
      could check that a compact event appears within N events before each specific dispatch.

Pricing constants (update when model changes):
    Model: claude-sonnet-4-6
    Input:          $3.00/M tokens
    Output:         $15.00/M tokens
    Cache read:     $0.30/M tokens
    Cache creation: $3.75/M tokens (flat — ephemeral_1h and ephemeral_5m treated identically)
"""

import json
import sys
from pathlib import Path

# ── Pricing constants (configurable) ─────────────────────────────────────────
CURRENT_MODEL = "claude-sonnet-4-6"
PRICING = {
    "input_per_m": 3.00,
    "output_per_m": 15.00,
    "cache_read_per_m": 0.30,
    "cache_creation_per_m": 3.75,
}

# ── Required blocks in markdown output ───────────────────────────────────────
REQUIRED_BLOCKS = [
    "Block 1",
    "Block 2",
    "Block 3",
    "Block 4",
]

# ── Required fields in persisted JSON report ─────────────────────────────────
REQUIRED_JSON_FIELDS = [
    "input_tokens",
    "cache_read",
    "cache_creation_total",
    "output_tokens",
    "cache_efficiency_pct",
    "estimated_cost_usd",
    "agent_dispatches",
    "compact_count",
    "compact_before_agent",
    "skill_calls",
    "worktree_used",
]

# ── Required workflow steps in Block 2 ───────────────────────────────────────
REQUIRED_WORKFLOW_STEPS = [
    "Branch created",
    "implement",
    "test",
    "audit",
    "doc",
    "devlog",
    "compact",
    "Worktree",
]

VALID_GRADES = {"A", "B", "C", "D"}


def validate_markdown(text: str) -> list[str]:
    """Validate that a session-review markdown output has all required blocks."""
    errors = []
    for block in REQUIRED_BLOCKS:
        if block not in text:
            errors.append(f"Missing required block: '{block}'")
    for step in REQUIRED_WORKFLOW_STEPS:
        if step not in text:
            errors.append(f"Workflow step missing from Block 2: '{step}'")
    if not any(f"Grade: {g}" in text for g in VALID_GRADES):
        errors.append("Block 2 missing overall grade (A/B/C/D)")
    if "Optimization Tips" not in text and "Block 3" in text:
        errors.append("Block 3 missing 'Optimization Tips' heading")
    if "Skillset Improvement" not in text and "Block 4" in text:
        errors.append("Block 4 missing 'Skillset Improvement' content")
    return errors


def validate_json_report(data: dict) -> list[str]:
    """Validate a persisted JSON session-review report."""
    errors = []
    for field in REQUIRED_JSON_FIELDS:
        if field not in data:
            errors.append(f"Missing required field: '{field}'")
    cost = data.get("estimated_cost_usd", 0)
    if cost < 0:
        errors.append(f"estimated_cost_usd is negative: {cost}")
    eff = data.get("cache_efficiency_pct", -1)
    if not (0 <= eff <= 100):
        errors.append(f"cache_efficiency_pct out of range: {eff}")
    return errors


def main():
    if len(sys.argv) < 2 or sys.argv[1] == "-":
        content = sys.stdin.read()
    else:
        path = Path(sys.argv[1])
        if not path.exists():
            print(f"ERROR: File not found: {path}", file=sys.stderr)
            sys.exit(1)
        content = path.read_text()

    errors = []
    # Try JSON first (persisted report), then treat as markdown
    try:
        data = json.loads(content)
        errors = validate_json_report(data)
        mode = "json"
    except json.JSONDecodeError:
        errors = validate_markdown(content)
        mode = "markdown"

    if errors:
        print(f"FAIL ({mode} validation):")
        for e in errors:
            print(f"  - {e}")
        sys.exit(1)
    else:
        print(f"PASS ({mode} validation) — all required sections present")
        print(f"Pricing model: {CURRENT_MODEL}")
        sys.exit(0)


if __name__ == "__main__":
    main()
