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

if grep -qE "^publisher_default:.*<domain>" "$CONFIG"; then
  fail "publisher_default uses the '@<domain>' template, which workspaces 5.0 rejects"
fi

# Folders that hold an asset manifest at any depth. _agent-parts holds mixin
# fragments pulled in by relative path, not standalone assets.
folders="$(cd "$ROOT" && find . -mindepth 2 \( -name SKILL.md -o -name agent.yaml -o -name AGENT.md \
  -o -name MCP.md -o -name CONTRACT.md -o -name BUNDLE.md -o -name CONNECTOR.md -o -name PROMPT.md \
  -o -name '*.bundle.yaml' -o -name '*.catalog.yaml' \) -not -path './node_modules/*' -not -path './.*' \
  | cut -d/ -f2 | grep -vx '_agent-parts' | sort -u)"

count="$(printf '%s\n' "$folders" | grep -c . || true)"
if [ "$count" -lt 10 ]; then
  fail "expected at least 10 asset folders, found $count — the folder scan is broken"
  assert_done
fi

for f in $folders; do
  assert_true "publisher_overrides covers $f/" grep -qE "^[[:space:]]*-[[:space:]]*path:[[:space:]]*['\"]?$f/['\"]?[[:space:]]*$" "$CONFIG"
done

assert_done
