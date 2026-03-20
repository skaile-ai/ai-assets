"""
TDD Guard — PostToolUse hook for Bash.

Detects test command execution and records outcomes to the TDD log.
This drives state transitions:
  - Test FAILED in writing_tests → state becomes 'red'
  - Test SUCCEEDED in making_tests_pass → state becomes 'initial'
  - Test FAILED in making_tests_pass → stays in 'making_tests_pass' (keep fixing)

Exit codes:
  0 — always (post-hooks don't block, they record)
"""

import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from tdd_common import get_log_path, is_build_command, is_lint_command, is_test_command, read_state


def main():
    raw = sys.stdin.read()
    if not raw.strip():
        sys.exit(0)

    try:
        input_data = json.loads(raw)
    except json.JSONDecodeError:
        sys.exit(0)

    tool_name = input_data.get("tool_name", "")
    if tool_name != "Bash":
        sys.exit(0)

    tool_input = input_data.get("tool_input", {})
    command = tool_input.get("command", "")

    is_test = is_test_command(command)
    is_build = is_build_command(command)
    is_lint = is_lint_command(command)

    if not is_test and not is_build and not is_lint:
        sys.exit(0)

    # Get test result from tool output
    tool_result = input_data.get("tool_result", {})

    # tool_result might be a string or an object depending on Claude Code version
    if isinstance(tool_result, str):
        stdout = tool_result
        stderr = ""
        exit_code = 0
    elif isinstance(tool_result, dict):
        stdout = tool_result.get("stdout", "")
        stderr = tool_result.get("stderr", "")
        # Exit code might be in different places
        exit_code = tool_result.get("exit_code", tool_result.get("exitCode", 0))
    else:
        sys.exit(0)

    # Determine pass/fail
    succeeded = exit_code == 0

    # Record to TDD log
    log_path = get_log_path(input_data)
    state = read_state(log_path)

    ts = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    status = "SUCCEEDED" if succeeded else "FAILED"

    # Truncate command for log readability
    cmd_short = command[:120] + ("..." if len(command) > 120 else "")

    # Only log [build]/[lint] entries when in verify_build state to avoid corrupting state derivation
    if (is_build or is_lint) and state != "verify_build":
        if not succeeded:
            print(f"TDD: Build/lint FAILED (state: {state}). Fix errors.", file=sys.stderr)
        sys.exit(0)

    tag = "[build]" if is_build else "[lint]" if is_lint else "[test]"
    entry = f"{tag} {cmd_short} - {status}\n"

    log_path.parent.mkdir(parents=True, exist_ok=True)
    with open(log_path, "a") as f:
        f.write(entry)

    # Re-read state after writing to get the updated state
    new_state = read_state(log_path)

    # Provide feedback to the agent via stderr (informational, not blocking)
    if is_build or is_lint:
        label = "Build" if is_build else "Lint"
        if not succeeded:
            print(f"TDD: {label} FAILED. Fix errors, then re-run.", file=sys.stderr)
        elif new_state == "initial":
            print(f"TDD: {label} PASSED! Both build and lint verified. State → initial. 🎉", file=sys.stderr)
        else:
            still_needed = "lint" if is_build else "build"
            print(f"TDD: {label} PASSED. Still need {still_needed} to pass. State: verify_build.", file=sys.stderr)
    elif is_test:
        if state == "writing_tests" and not succeeded:
            print(f"TDD: Tests FAILED as expected. State → red. Declare GREEN to start implementing.", file=sys.stderr)
        elif state == "writing_tests" and succeeded:
            print(
                f"TDD: Tests PASSED but should have FAILED (you're in RED phase).\n"
                f"Your tests might not be testing anything new. State → verify_build.",
                file=sys.stderr,
            )
        elif state == "making_tests_pass" and succeeded:
            print(f"TDD: Tests PASSED! State → verify_build. Run build+lint to complete.", file=sys.stderr)
        elif state == "making_tests_pass" and not succeeded:
            print(f"TDD: Tests still FAILING. Keep implementing. State stays: making_tests_pass.", file=sys.stderr)
        elif state == "red" and succeeded:
            print(f"TDD: Tests PASSED (unexpected in red state). State → verify_build.", file=sys.stderr)
        elif state == "initial":
            print(f"TDD: Test run recorded (state: initial).", file=sys.stderr)

    sys.exit(0)


if __name__ == "__main__":
    main()
