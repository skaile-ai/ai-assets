#!/usr/bin/env bash
#
# Test runner for skaile-ai/ai-assets.
#
# Discovers every `*.test.sh` under `tests/` and runs each one in its own
# subshell. A test passes by exiting 0 and fails by exiting non-zero.
#
# Two rules this runner exists to enforce:
#
#   1. Finding zero tests is a FAILURE, not a pass. A discovery glob that
#      matches nothing exits 0 in the naive form, which turns the lane into a
#      generator of false confidence the moment a rename or a moved directory
#      makes the pattern miss.
#   2. One failing test fails the run, but every test still runs, so a single
#      break does not hide the ones behind it.
#
# Tests live under `tests/` and NOT next to the script they cover, because
# every `<domain>/skills/<name>/` directory in this repo is an asset root that
# `skaile.yaml` publishes wholesale into consumer repos (platform's
# `.claude/skills/<name>/` is a byte copy, `scripts/` included). A test file
# inside an asset root would ship to every consumer and register as asset
# drift there. Mirror the subject's path under `tests/` instead.
#
# Usage:
#   npm test                    # all tests
#   bash tests/run.sh           # same
#   bash tests/run.sh <path>    # one test file, for iterating

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export REPO_ROOT
TESTS_DIR="$REPO_ROOT/tests"

declare -a FILES=()
if [ "$#" -gt 0 ]; then
  for arg in "$@"; do
    if [ ! -f "$arg" ]; then
      echo "runner: no such test file: $arg" >&2
      exit 2
    fi
    FILES+=("$arg")
  done
else
  while IFS= read -r f; do
    FILES+=("$f")
  done < <(find "$TESTS_DIR" -type f -name '*.test.sh' | sort)
fi

if [ "${#FILES[@]}" -eq 0 ]; then
  echo "runner: discovered 0 tests under ${TESTS_DIR#"$REPO_ROOT"/} — refusing to report success." >&2
  echo "runner: a lane that passes with nothing to run is worse than no lane." >&2
  exit 1
fi

passed=0
declare -a FAILED=()

for f in "${FILES[@]}"; do
  rel="${f#"$REPO_ROOT"/}"
  echo "── $rel"
  if bash "$f"; then
    passed=$((passed + 1))
  else
    status=$?
    FAILED+=("$rel (exit $status)")
    echo "   FAIL: $rel exited $status" >&2
  fi
done

echo
echo "runner: ${#FILES[@]} test file(s), $passed passed, ${#FAILED[@]} failed"

if [ "${#FAILED[@]}" -gt 0 ]; then
  for entry in "${FAILED[@]}"; do
    echo "  failed: $entry" >&2
  done
  exit 1
fi
