"""
TDD Guard — CLI for RED/GREEN declarations.

Usage:
  python3 scripts/tdd/tdd_log.py red --test "e2e/specs/pages/dashboard.spec.ts" --expects "page renders with project cards"
  python3 scripts/tdd/tdd_log.py green --change "implement project dashboard page" --file "frontend/src/pages/projects/dashboard.page.tsx"
  python3 scripts/tdd/tdd_log.py green --skip-red --reason=refactoring --change "extract helper" --file "frontend/src/lib/utils.ts"
  python3 scripts/tdd/tdd_log.py status

Agents call this CLI to declare TDD phase transitions.
The pre-edit hook reads the resulting log to enforce the state machine.
"""

import argparse
import sys
from datetime import datetime, timezone
from pathlib import Path

from tdd_common import get_green_allowlist, read_state

DEFAULT_LOG = ".tdd-guard.log"


def cmd_red(args):
    log_path = Path(args.log)
    state = read_state(log_path)

    # Red can override any state (for stuck recovery)
    if state not in ("initial", "writing_tests", "red", "making_tests_pass"):
        print(f"ERROR: unexpected state '{state}'", file=sys.stderr)
        sys.exit(1)

    if state not in ("initial",) and state != "writing_tests":
        print(f"OVERRIDE: state was '{state}', forcing to writing_tests", file=sys.stderr)

    ts = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    entry = f"\n## Red - {ts}\nTest: {args.test}\nExpects: {args.expects}\n"
    log_path.parent.mkdir(parents=True, exist_ok=True)

    with open(log_path, "a") as f:
        f.write(entry)

    print(f"RED declared. State: writing_tests. Edit test files only.")
    print(f"Next: run tests to confirm they FAIL, then declare GREEN.")


def cmd_green(args):
    log_path = Path(args.log)
    state = read_state(log_path)

    if args.skip_red:
        if not args.reason:
            print("ERROR: --skip-red requires --reason (refactoring|lint-only|adding-coverage)", file=sys.stderr)
            sys.exit(1)
        allowed_reasons = {"refactoring", "lint-only", "adding-coverage"}
        if args.reason not in allowed_reasons:
            print(f"ERROR: --reason must be one of {allowed_reasons}", file=sys.stderr)
            sys.exit(1)
    else:
        if state not in ("red", "making_tests_pass"):
            print(
                f"ERROR: GREEN requires state 'red' (tests failed), but current state is '{state}'.\n"
                f"Run your tests first to confirm they fail, or use --skip-red --reason=refactoring.",
                file=sys.stderr,
            )
            sys.exit(1)

    if not args.file:
        print("ERROR: at least one --file is required", file=sys.stderr)
        sys.exit(1)

    if len(args.file) > 10:
        print(f"WARNING: {len(args.file)} files declared — consider smaller changes", file=sys.stderr)

    ts = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    entry = f"\n## Green - {ts}\n"
    if args.skip_red:
        entry += f"Skip-Red: {args.reason}\n"
    entry += f"Change: {args.change}\n"
    for f in args.file:
        entry += f"File: {f}\n"

    log_path.parent.mkdir(parents=True, exist_ok=True)

    with open(log_path, "a") as fh:
        fh.write(entry)

    print(f"GREEN declared. State: making_tests_pass.")
    print(f"Allowed files: {', '.join(args.file)}")
    print(f"Next: implement changes, then run tests to confirm they PASS.")


def cmd_status(args):
    log_path = Path(args.log)
    state = read_state(log_path)
    print(f"TDD log: {log_path}")
    print(f"State: {state}")

    if state == "making_tests_pass":
        allowlist = get_green_allowlist(log_path)
        if allowlist:
            print(f"Allowed files: {', '.join(allowlist)}")
        else:
            print("Allowed files: (none found)")
    elif state == "verify_build":
        print("Action required: BOTH build AND lint must pass before continuing")
        print("  pnpm run build    (includes type check)")
        print("  pnpm run lint")
        print("Both must succeed — one alone is not enough.")


def main():
    parser = argparse.ArgumentParser(description="TDD Guard — RED/GREEN declarations")
    parser.add_argument("--log", default=DEFAULT_LOG, help="Path to TDD log file")

    sub = parser.add_subparsers(dest="command", required=True)

    red_parser = sub.add_parser("red", help="Declare RED phase (writing tests)")
    red_parser.add_argument("--test", required=True, help="Test file path")
    red_parser.add_argument("--expects", required=True, help="What the test expects to fail")

    green_parser = sub.add_parser("green", help="Declare GREEN phase (making tests pass)")
    green_parser.add_argument("--change", required=True, help="Description of planned change")
    green_parser.add_argument("--file", action="append", required=True, help="File(s) to modify")
    green_parser.add_argument("--skip-red", action="store_true", help="Skip RED for refactoring/lint")
    green_parser.add_argument("--reason", help="Reason for --skip-red")

    sub.add_parser("status", help="Show current TDD state")

    args = parser.parse_args()

    if args.command == "red":
        cmd_red(args)
    elif args.command == "green":
        cmd_green(args)
    elif args.command == "status":
        cmd_status(args)


if __name__ == "__main__":
    main()
