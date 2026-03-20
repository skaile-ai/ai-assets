"""
TDD Guard — PreToolUse hook for Edit/Write.

Reads the TDD log, classifies the target file, and blocks edits
that violate the RED-GREEN cycle.

Exit codes:
  0 — edit allowed
  2 — edit blocked (message on stderr shown to agent)
"""

import json
import os
import sys
from pathlib import Path

# Add this script's directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from tdd_common import classify_file, get_app_dir, get_green_allowlist, get_log_path, read_state


def main():
    # Read hook input from stdin
    raw = sys.stdin.read()
    if not raw.strip():
        sys.exit(0)

    try:
        input_data = json.loads(raw)
    except json.JSONDecodeError:
        sys.exit(0)  # Can't parse — don't block

    tool_name = input_data.get("tool_name", "")
    if tool_name not in ("Edit", "Write"):
        sys.exit(0)

    tool_input = input_data.get("tool_input", {})
    file_path = tool_input.get("file_path", "")

    if not file_path:
        sys.exit(0)

    # Resolve paths relative to the app directory (where postxl-schema.json is)
    file_dir = str(Path(file_path).resolve().parent)
    app_dir = get_app_dir(file_dir)

    # Classify the file (app_dir strips the prefix so patterns match)
    category = classify_file(file_path, app_dir=app_dir)

    # For display, make path relative to app dir
    try:
        rel_path = str(Path(file_path).resolve().relative_to(app_dir.resolve()))
    except ValueError:
        rel_path = file_path

    # Bypass files don't need TDD
    if category == "other":
        sys.exit(0)

    # Get TDD state — override cwd with app_dir for log path derivation
    input_data_with_app_dir = {**input_data, "cwd": str(app_dir)}
    log_path = get_log_path(input_data_with_app_dir)
    state = read_state(log_path)

    # ── Permission matrix ──────────────────────────────────────────
    #
    # State               | Test Files  | Prod Files
    # --------------------|-------------|---------------------------
    # initial             | BLOCKED     | BLOCKED
    # writing_tests       | ALLOWED     | BLOCKED
    # red                 | BLOCKED     | BLOCKED
    # making_tests_pass   | BLOCKED*    | ALLOWED (if in allowlist)
    # verify_build        | BLOCKED     | BLOCKED
    #
    # * Test files allowed if Green was logged with --skip-red

    if state == "verify_build":
        if category == "test":
            # Block new test files — must finish verification first
            print(
                f"TDD GUARD: Cannot edit test file '{rel_path}' — build verification required.\n"
                f"BOTH build AND lint must pass before starting new work:\n"
                f"  pnpm run build    (includes type check)\n"
                f"  pnpm run lint\n"
                f"Both must succeed — one alone is not enough.",
                file=sys.stderr,
            )
            sys.exit(2)
        # Allow prod file edits during verify_build (needed to fix build/lint errors)
        sys.exit(0)

    if category == "test":
        if state == "writing_tests":
            sys.exit(0)  # Allowed — writing tests
        else:
            print(
                f"TDD GUARD: Cannot edit test file '{rel_path}' in state '{state}'.\n"
                f"Declare RED first:\n"
                f"  python3 scripts/tdd/tdd_log.py --log {log_path} red \\\n"
                f"    --test \"{rel_path}\" \\\n"
                f"    --expects \"<what you expect to fail>\"",
                file=sys.stderr,
            )
            sys.exit(2)

    if category == "prod":
        if state == "making_tests_pass":
            # Check allowlist
            allowlist = get_green_allowlist(log_path)
            if allowlist:
                # Normalize: check if rel_path matches any allowlisted file
                if any(rel_path == f or rel_path.endswith(f) or f.endswith(rel_path) for f in allowlist):
                    sys.exit(0)  # Allowed — file is in GREEN allowlist
                else:
                    print(
                        f"TDD GUARD: File '{rel_path}' is not in the GREEN allowlist.\n"
                        f"Allowed files: {', '.join(allowlist)}\n"
                        f"Declare GREEN again with this file included:\n"
                        f"  python3 scripts/tdd/tdd_log.py --log {log_path} green \\\n"
                        f"    --change \"<description>\" \\\n"
                        f"    --file \"{rel_path}\"",
                        file=sys.stderr,
                    )
                    sys.exit(2)
            else:
                sys.exit(0)  # No allowlist means all prod files allowed
        else:
            print(
                f"TDD GUARD: Cannot edit prod file '{rel_path}' in state '{state}'.\n"
                f"You must:\n"
                f"  1. Declare RED (write failing tests)\n"
                f"  2. Run tests (confirm they FAIL)\n"
                f"  3. Declare GREEN (list files to change)\n"
                f"Then you can edit prod files.\n\n"
                f"Current TDD log: {log_path}\n"
                f"Check state: python3 scripts/tdd/tdd_log.py --log {log_path} status",
                file=sys.stderr,
            )
            sys.exit(2)


if __name__ == "__main__":
    main()
