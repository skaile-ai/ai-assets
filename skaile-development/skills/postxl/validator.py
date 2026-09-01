#!/usr/bin/env python3
"""Auto-generated validator for postxl.
Re-generate with: skaile-development/compile-validators target=postxl

Most rules in this skill are agent-runtime / process / semantic guidance
(pick the right mode, run the right command, read the right CLAUDE.md).
Structural checks are limited to the few artifacts the skill writes:

  * postxl-schema.json files in platform/ and store/backend/ must parse as JSON
  * postxl-lock.json (when present) must parse as JSON
  * @custom-start[:name] / @custom-end[:name] markers in PostXL-generated
    source trees must be balanced and matched

Everything else is skipped with a clear reason — that is the correct
shape for a contextual guidance skill (compare with audit/test-plan/etc.).
"""
import re
import sys
from pathlib import Path

# Locate the shared validator_lib by walking up from this file's location and
# probing known relative paths. Location-independent so the same file works
# whether run from the ai-assets source tree or the .claude/skills copy.
_VLIB = None
for _base in (Path(__file__).resolve(), *Path(__file__).resolve().parents):
    for _rel in (
        ("ai-assets-skaileup", "skaileup", "contracts", "scripts"),
        ("ai-assets-skaileup", "contracts", "scripts"),
        ("ai-assets-skaileup", "skaileup-contracts", "scripts"),
        ("skaileup-shared", "scripts"),
    ):
        _cand = _base.joinpath(*_rel)
        if (_cand / "validator_lib.py").exists():
            _VLIB = str(_cand)
            break
    if _VLIB:
        break
if _VLIB:
    sys.path.insert(0, _VLIB)

from validator_lib import Validator, main  # noqa: E402

SKILL = "postxl"

# Source roots inside PostXL-generated projects that are scanned for
# custom-block marker integrity. Limited to project source dirs to keep
# the walk fast (sub-second on the full skaile-dev tree).
_SOURCE_ROOTS = [
    "platform/backend/libs",
    "platform/backend/apps",
    "platform/frontend/src",
    "store/backend/libs",
    "store/backend/apps",
    "store/frontend/src",
]
_SOURCE_GLOBS = ("*.ts", "*.tsx", "*.js", "*.jsx")

# @custom-start[:optional-name]  (line and block comment forms)
_START_RE = re.compile(r"(?://|/\*)\s*@custom-start(?::([\w-]+))?\b")
_END_RE = re.compile(r"(?://|/\*)\s*@custom-end(?::([\w-]+))?\b")


def _custom_blocks_balanced(v: Validator) -> tuple[bool, str]:
    """Walk PostXL-generated source roots and verify every @custom-start
    marker has a matching @custom-end marker.

    The reverse (lone @custom-end without a matching start) is a known and
    intentional PostXL pattern — a "split marker" that delimits a leading
    custom region from trailing generated content. We do NOT flag it.

    The breakage modes this catches:
      * agent writes @custom-start:foo and forgets to close it
      * hand edit accidentally truncates the @custom-end:foo line
      * duplicate @custom-start:foo in the same file
    """
    problems: list[str] = []
    files_scanned = 0
    for root in _SOURCE_ROOTS:
        root_path = v.cwd / root
        if not root_path.is_dir():
            continue
        for glob in _SOURCE_GLOBS:
            for f in root_path.rglob(glob):
                files_scanned += 1
                try:
                    text = f.read_text(encoding="utf-8")
                except (UnicodeDecodeError, OSError):
                    continue
                if "@custom-start" not in text:
                    # Lone @custom-end is a PostXL split marker — intentional.
                    continue
                rel = f.relative_to(v.cwd)
                anon_depth = 0
                named_open: dict[str, int] = {}
                # Track which named ends we have seen so that lone ends
                # (split markers) elsewhere in the file don't confuse us.
                for lineno, line in enumerate(text.splitlines(), 1):
                    if (m := _START_RE.search(line)):
                        name = m.group(1)
                        if name:
                            if name in named_open:
                                problems.append(
                                    f"{rel}:{lineno} duplicate @custom-start:{name}"
                                )
                            else:
                                named_open[name] = lineno
                        else:
                            anon_depth += 1
                    elif (m := _END_RE.search(line)):
                        name = m.group(1)
                        if name:
                            if name in named_open:
                                named_open.pop(name)
                            # else: lone @custom-end:NAME → PostXL split marker, ignore
                        else:
                            if anon_depth > 0:
                                anon_depth -= 1
                            # else: lone anonymous @custom-end → ignore (split marker)
                if anon_depth != 0:
                    problems.append(
                        f"{rel} has {anon_depth} unclosed anonymous @custom-start"
                    )
                for name, lineno in named_open.items():
                    problems.append(
                        f"{rel}:{lineno} @custom-start:{name} never closed"
                    )

    if files_scanned == 0:
        # No PostXL projects in this tree — treat as vacuously OK.
        return True, ""
    if problems:
        return False, "; ".join(problems[:8])
    return True, ""


# Schema files scanned for the `compositeUnique` trap. Covers a generated app at
# the cwd root and the two skaile-dev PostXL projects.
_SCHEMA_ROOTS = ["", "platform", "store"]


def _no_composite_unique(v: Validator) -> tuple[bool, str]:
    """`compositeUnique` is not a PostXL schema key.

    The model decoder is `passthrough()`, so the key is retained by the parser and
    ignored by every generator: no error, no warning, and no emitted constraint.
    Authors reach for it expecting a composite unique index and silently get none.
    The supported key is `indexes`.
    """
    problems: list[str] = []
    files_scanned = 0
    for root in _SCHEMA_ROOTS:
        base = v.cwd / root if root else v.cwd
        candidates = [base / "postxl-schema.json"]
        schema_dir = base / "schema"
        if schema_dir.is_dir():
            candidates.extend(sorted(schema_dir.glob("*.model.json")))
        for f in candidates:
            if not f.is_file():
                continue
            files_scanned += 1
            try:
                text = f.read_text(encoding="utf-8")
            except (UnicodeDecodeError, OSError):
                continue
            if "compositeUnique" in text:
                problems.append(f"{f.relative_to(v.cwd)} uses compositeUnique (use indexes)")

    if files_scanned == 0:
        return True, ""  # no PostXL schema in this tree — vacuously OK
    if problems:
        return False, "; ".join(problems[:8])
    return True, ""


def _json_parses_if_present(v: Validator, rel_path: str) -> tuple[bool, str]:
    """Pass if the file is absent OR parses as JSON. Fail only on present-but-invalid."""
    p = v.cwd / rel_path
    if not p.exists():
        return True, ""  # not present in this tree
    if v.read_json(rel_path) is None:
        return False, f"Invalid JSON: {rel_path}"
    return True, ""


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──
    v.skip(
        "read the target project's own CLAUDE.md before any edit",
        rule_type="MUST",
        reason="runtime — agent reading behavior, not an artifact",
    )
    v.skip(
        "prefer Mode 1 (schema edit) when the change is expressible in schema",
        rule_type="MUST",
        reason="semantic — mode-selection judgment",
    )
    v.skip(
        "use // @custom-start[:name] … // @custom-end[:name] for in-file custom logic",
        rule_type="MUST",
        reason="semantic — judgment about what counts as 'custom logic'; "
               "marker integrity is checked structurally below",
    )
    v.skip(
        "use the package manager the tree uses — pnpm in a generated app, bun in skaile-dev",
        rule_type="MUST",
        reason="runtime — agent command behavior",
    )
    v.skip(
        "run generate from the project root, not from backend/ or frontend/",
        rule_type="MUST",
        reason="runtime — agent command behavior",
    )
    v.skip(
        "run the dual verify loop (BE typecheck + FE typecheck) after any schema change",
        rule_type="MUST",
        reason="runtime — executed during skill run",
    )
    v.skip(
        "apply the DB migration after model-field changes and read the generated SQL",
        rule_type="MUST",
        reason="runtime — agent command behavior plus reading judgment",
    )
    v.skip(
        "use @postxl/ui-components primitives for any new UI element",
        rule_type="MUST",
        reason="semantic — UI element classification",
    )
    v.skip(
        "register new user-facing platform/frontend/ actions as command palette actions",
        rule_type="MUST",
        reason="semantic — requires identifying 'new' and 'user-facing'",
    )

    # ── NEVER rules ──
    v.skip(
        "edit a file listed in postxl-lock.json outside a // @custom-* block",
        rule_type="NEVER",
        reason="semantic — requires diff context against lockfile",
    )
    v.skip(
        "reach for -f or -p to resolve a generate conflict",
        rule_type="NEVER",
        reason="runtime — agent command behavior",
    )
    v.skip(
        "trust pxl validate as evidence that a constraint was emitted",
        rule_type="NEVER",
        reason="semantic — reasoning about what counts as evidence",
    )
    v.skip(
        "run bunx pxl generate directly in platform/ — use bun run generate",
        rule_type="NEVER",
        reason="runtime — agent command behavior",
    )
    v.skip(
        "run Biome inside platform/",
        rule_type="NEVER",
        reason="runtime — agent command behavior",
    )
    v.skip(
        "create a new barrel index.ts in platform/backend/libs/",
        rule_type="NEVER",
        reason="semantic — requires distinguishing PostXL-generated barrels "
               "(tracked in postxl-lock.json) from new ones",
    )
    v.skip(
        "use the @Optional() NestJS decorator",
        rule_type="NEVER",
        reason="semantic — requires TypeScript decorator analysis",
    )
    v.skip(
        "access the database directly (raw Prisma client, raw SQL) in custom code",
        rule_type="NEVER",
        reason="semantic — requires distinguishing generated from custom call sites",
    )

    # ── Structural checks (the few that are deterministic) ──
    v.never(
        "use compositeUnique — not a PostXL schema key; parses silently, emits nothing",
        lambda: _no_composite_unique(v),
    )
    v.must(
        "platform/postxl-schema.json parses as JSON (when present)",
        lambda: _json_parses_if_present(v, "platform/postxl-schema.json"),
    )
    v.must(
        "platform/postxl-lock.json parses as JSON (when present)",
        lambda: _json_parses_if_present(v, "platform/postxl-lock.json"),
    )
    v.must(
        "store/postxl-schema.json parses as JSON (when present)",
        lambda: _json_parses_if_present(v, "store/postxl-schema.json"),
    )
    v.must(
        "store/postxl-lock.json parses as JSON (when present)",
        lambda: _json_parses_if_present(v, "store/postxl-lock.json"),
    )
    v.must(
        "@custom-start / @custom-end markers are balanced and named-matched "
        "across PostXL-generated source trees",
        lambda: _custom_blocks_balanced(v),
    )

    # ── CHECKLIST ──
    v.skip(
        "Read the target project's CLAUDE.md before any edit",
        rule_type="CHECKLIST",
        reason="runtime — agent reading behavior",
    )
    v.skip(
        "Picked the right mode: create, schema (Mode 1), custom block (Mode 2), new module (Mode 3)",
        rule_type="CHECKLIST",
        reason="semantic — mode-selection judgment",
    )
    v.skip(
        "Used the tree's own package manager (pnpm in a generated app, bun in skaile-dev)",
        rule_type="CHECKLIST",
        reason="runtime — execution step",
    )
    v.skip(
        "pxl validate passed before generating",
        rule_type="CHECKLIST",
        reason="runtime — execution step",
    )
    v.skip(
        "In platform/: used bun run generate, not bare bunx pxl generate",
        rule_type="CHECKLIST",
        reason="runtime — execution step",
    )
    v.skip(
        "Read the generate run's conflict list, if any",
        rule_type="CHECKLIST",
        reason="runtime — agent reading behavior",
    )
    v.checklist(
        "Custom blocks anchored and have matching @custom-start / @custom-end markers",
        lambda: _custom_blocks_balanced(v),
    )
    v.skip(
        "Did NOT edit a file listed in postxl-lock.json outside a custom block",
        rule_type="CHECKLIST",
        reason="semantic — requires diff context against lockfile",
    )
    v.skip(
        "Migration applied and the generated SQL confirms the intended constraint",
        rule_type="CHECKLIST",
        reason="runtime — execution step plus reading judgment",
    )
    v.skip(
        "Both backend and frontend typechecks pass",
        rule_type="CHECKLIST",
        reason="runtime — execution step",
    )
    v.skip(
        "pxl status shows no unintended drift / ejection",
        rule_type="CHECKLIST",
        reason="runtime — execution step",
    )
    v.skip(
        "Lint clean — in platform/, ESLint only, Biome never invoked",
        rule_type="CHECKLIST",
        reason="runtime — execution step",
    )
    v.skip(
        "No new index.ts barrel under platform/backend/libs/",
        rule_type="CHECKLIST",
        reason="semantic — distinguishing PostXL-generated barrels from new ones",
    )
    v.skip(
        "No @Optional() on any NestJS injectable",
        rule_type="CHECKLIST",
        reason="semantic — requires TypeScript decorator analysis",
    )
    v.skip(
        "Custom action handlers inject no DispatcherService-dependent service",
        rule_type="CHECKLIST",
        reason="semantic — requires NestJS dependency-graph analysis",
    )
    v.skip(
        "Custom code reads/writes via modelViewService / modelUpdateService, not raw Prisma",
        rule_type="CHECKLIST",
        reason="semantic — requires distinguishing generated from custom call sites",
    )
    v.skip(
        "New user-facing platform/frontend/ UI registered as a command palette action",
        rule_type="CHECKLIST",
        reason="semantic — requires identifying 'new' and 'user-facing'",
    )
    v.skip(
        "Used @postxl/ui-components primitives for new UI",
        rule_type="CHECKLIST",
        reason="semantic — UI element classification",
    )
    v.skip(
        "Submodule pointer bumped in shell repo after committing inside platform/ or store/",
        rule_type="CHECKLIST",
        reason="runtime — git state",
    )

    return v.result()


if __name__ == "__main__":
    main(validate)
