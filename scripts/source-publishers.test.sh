#!/usr/bin/env bash
#
# .skaile-source.yaml is the v1 source file. skaile.manifest.yaml is the
# canonical publish manifest and the store's sync prefers it, so every asset
# here ships under its one publisher (skaile-ai). The v1 file must not say
# anything different: a per-folder publisher_overrides entry would give
# v1 readers (the store's curation preview) a namespace the published refs do
# not use. This pins the agreement: the default publisher is '@' plus the
# manifest's publisher, and there are no overrides.

set -uo pipefail
ROOT="${REPO_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && git rev-parse --show-toplevel)}"
source "$ROOT/scripts/lib/assert.sh"

CONFIG="$ROOT/.skaile-source.yaml"
MANIFEST="$ROOT/skaile.manifest.yaml"
assert_true ".skaile-source.yaml exists" test -f "$CONFIG"
assert_true "skaile.manifest.yaml exists" test -f "$MANIFEST"
[ -f "$CONFIG" ] && [ -f "$MANIFEST" ] || assert_done

unquote() { awk -F: -v key="$2" '$1 == key { v=$2; sub(/[[:space:]]*#.*/, "", v); gsub(/[[:space:]"'"'"']/, "", v); print v; exit }' "$1"; }

manifest_publisher="$(unquote "$MANIFEST" publisher)"
assert_true "skaile.manifest.yaml names a publisher" test -n "$manifest_publisher"

assert_eq "@$manifest_publisher" "$(unquote "$CONFIG" publisher_default)" \
  "publisher_default is '@' + the skaile.manifest.yaml publisher"

if grep -qE '^publisher_overrides:' "$CONFIG"; then
  fail "publisher_overrides present - per-folder publishers contradict the one canonical publisher in skaile.manifest.yaml"
else
  pass "no publisher_overrides"
fi

if grep -qE "^[[:space:]]*(publisher_default|publisher):.*<[a-z-]+>" "$CONFIG"; then
  fail "a publisher value uses an '@<...>' template, which workspaces 5.0 rejects"
else
  pass "no '@<...>' publisher template"
fi

assert_done
