#!/usr/bin/env bash
#
# Minimal assertion helpers for the repo's `*.test.sh` files, which sit beside
# the things they cover (see scripts/TESTING.md).
#
# Source it from a test:
#   source "$ROOT/scripts/lib/assert.sh"   # $ROOT = repo root; see scripts/TESTING.md
#
# Each helper records a failure and keeps going, so one test file reports every
# problem it found rather than only the first. Call `assert_done` last: it is
# what turns recorded failures into a non-zero exit, so a test file that omits
# it cannot pass by accident — it just never reports.

ASSERT_FAILURES=0
ASSERT_CHECKS=0

fail() {
  ASSERT_FAILURES=$((ASSERT_FAILURES + 1))
  echo "   ✗ $*" >&2
}

pass() {
  echo "   ✓ $*"
}

# assert_eq <expected> <actual> <description>
assert_eq() {
  ASSERT_CHECKS=$((ASSERT_CHECKS + 1))
  if [ "$1" = "$2" ]; then
    pass "$3"
  else
    fail "$3
       expected: $1
       actual:   $2"
  fi
}

# assert_true <description> <command...>
assert_true() {
  local desc="$1"
  shift
  ASSERT_CHECKS=$((ASSERT_CHECKS + 1))
  if "$@"; then
    pass "$desc"
  else
    fail "$desc (command failed: $*)"
  fi
}

# assert_contains <haystack> <needle> <description>
assert_contains() {
  ASSERT_CHECKS=$((ASSERT_CHECKS + 1))
  case "$1" in
    *"$2"*) pass "$3" ;;
    *) fail "$3
       expected to contain: $2
       actual:              $1" ;;
  esac
}

assert_done() {
  if [ "$ASSERT_CHECKS" -eq 0 ]; then
    echo "   ✗ no assertions ran" >&2
    exit 1
  fi
  [ "$ASSERT_FAILURES" -eq 0 ] || exit 1
}
