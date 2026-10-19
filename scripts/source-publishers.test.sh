#!/usr/bin/env bash
#
# .skaile-source.yaml maps every top-level asset folder to its own publisher.
# Until workspaces 5.0 that came from the folder name; now it is spelled out,
# and a folder with no entry silently falls back to `publisher_default`,
# changing every `@<publisher>/<name>` ref under it. This catches that at
# author time.

set -uo pipefail
ROOT="${REPO_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && git rev-parse --show-toplevel)}"
source "$ROOT/scripts/lib/assert.sh"

CONFIG="$ROOT/.skaile-source.yaml"
assert_true ".skaile-source.yaml exists" test -f "$CONFIG"
[ -f "$CONFIG" ] || assert_done

if grep -qE "^[[:space:]]*(publisher_default|publisher):.*<[a-z-]+>" "$CONFIG"; then
  fail "a publisher value uses an '@<...>' template, which workspaces 5.0 rejects"
fi

# Only the publisher_overrides block, so a later v2 `assets:` block with its
# own `path:` keys is not mistaken for an override.
overrides="$(awk '/^publisher_overrides:/{a=1; next} a && /^[A-Za-z_]/{a=0} a' "$CONFIG")"

# Each folder keeps the publisher its name gave it before 5.0. catalogs/ was
# already overridden to @skaile before the switch.
expected_publisher() {
  case "$1" in
    catalogs) echo "@skaile" ;;
    *) echo "@$1" ;;
  esac
}

# Folders that hold an asset manifest at any depth. _agent-parts holds mixin
# fragments pulled in by relative path, not standalone assets.
folders="$(cd "$ROOT" && find . -mindepth 2 \( -name SKILL.md -o -name agent.yaml -o -name AGENT.md \
  -o -name MCP.md -o -name CONTRACT.md -o -name CONNECTOR.md -o -name PROMPT.md \
  -o -name '*.preset.yaml' -o -name '*.catalog.yaml' \) -not -path './node_modules/*' -not -path './.*' \
  | cut -d/ -f2 | grep -vx '_agent-parts' | sort -u)"

count="$(printf '%s\n' "$folders" | grep -c . || true)"
if [ "$count" -lt 10 ]; then
  fail "expected at least 10 asset folders, found $count — the folder scan is broken"
  assert_done
fi

for f in $folders; do
  # The publisher line directly follows its `- path:` line.
  got="$(printf '%s\n' "$overrides" | awk -v want="$f/" '
    found { sub(/^[[:space:]]*publisher:[[:space:]]*/, ""); gsub(/["'\'']/, ""); print; exit }
    { line=$0; sub(/^[[:space:]]*-[[:space:]]*path:[[:space:]]*/, "", line); gsub(/["'\'' ]/, "", line)
      if ($0 ~ /^[[:space:]]*-[[:space:]]*path:/ && line == want) found=1 }')"
  assert_eq "$(expected_publisher "$f")" "$got" "publisher for $f/"
done

assert_done
