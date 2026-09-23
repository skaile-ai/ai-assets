#!/usr/bin/env bash
#
# skaile.yaml is the publication half of this repo's canonical-identity
# contract: a consumer resolving `<kind>:<name>@skaile-ai` lands on the `root:`
# named here. Nothing validates those roots at author time, so a skill that is
# renamed or moved leaves a `root:` pointing at nothing and the failure only
# surfaces in the consumer's resolver, far from the edit that caused it.
#
# This pins the four properties that make an entry resolvable.

set -uo pipefail
# Anchored at the repo root, not relative to this file, so a test co-located
# inside a skill directory sources the library exactly the same way.
ROOT="${REPO_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && git rev-parse --show-toplevel)}"
source "$ROOT/scripts/lib/assert.sh"

MANIFEST="$ROOT/skaile.yaml"

assert_true "skaile.yaml exists" test -f "$MANIFEST"
[ -f "$MANIFEST" ] || assert_done

# Entries are inline flow mappings: `- { kind: skill, name: x, root: a/b/x }`.
entries="$(grep -E '^[[:space:]]*-[[:space:]]*\{' "$MANIFEST" || true)"
entry_count="$(printf '%s' "$entries" | grep -c . || true)"

# The parser only understands the inline form, so it must also prove it saw
# every entry. A total parser break is obvious; a *partial* miss is not — one
# future entry written in block style (`- kind: skill` over several lines) would
# drop silently out of the checked set while the count stayed plausible, and a
# bad `root:` on exactly that entry would go unnoticed. So count the list items
# in the `assets:` block independently and require the two to agree.
listed="$(awk '/^assets:/{a=1; next} a && /^[A-Za-z_]/{a=0} a && /^[[:space:]]*-[[:space:]]/{n++} END{print n+0}' "$MANIFEST")"

if [ "$listed" -lt 10 ]; then
  fail "expected skaile.yaml to declare at least 10 assets, found $listed list items — the manifest or this parser is broken"
  assert_done
fi
if [ "$entry_count" -ne "$listed" ]; then
  fail "parsed $entry_count inline entries but the assets: block lists $listed items — an entry is in a form this test does not read, and is going unchecked"
  assert_done
fi
pass "parsed all $entry_count asset entries listed under assets:"

field() {
  # field <line> <key> -> value, with surrounding whitespace and the closing
  # brace stripped.
  printf '%s' "$1" | sed -nE "s/.*[{,][[:space:]]*$2:[[:space:]]*([^,}]*).*/\1/p" | sed -E 's/[[:space:]]+$//'
}

missing_roots=0
bad_skills=0
name_mismatches=0
missing_files=0

while IFS= read -r line; do
  [ -n "$line" ] || continue
  kind="$(field "$line" kind)"
  name="$(field "$line" name)"
  root="$(field "$line" root)"

  if [ -z "$kind" ] || [ -z "$name" ] || [ -z "$root" ]; then
    fail "entry is missing kind/name/root: $line"
    continue
  fi

  # 1. the root exists
  if [ ! -d "$ROOT/$root" ]; then
    fail "$kind:$name — root does not exist: $root"
    missing_roots=$((missing_roots + 1))
    continue
  fi

  # 2. a skill root carries a SKILL.md
  if [ "$kind" = "skill" ] && [ ! -f "$ROOT/$root/SKILL.md" ]; then
    fail "skill:$name — no SKILL.md under $root"
    bad_skills=$((bad_skills + 1))
    continue
  fi

  # 3. the declared name matches the directory, and (for skills) the SKILL.md
  #    frontmatter name matches it too. CLAUDE.md states the frontmatter rule;
  #    the resolver depends on all three agreeing.
  base="$(basename "$root")"
  if [ "$name" != "$base" ]; then
    fail "$kind:$name — declared name differs from directory '$base' ($root)"
    name_mismatches=$((name_mismatches + 1))
  fi
  if [ "$kind" = "skill" ]; then
    fm="$(awk '/^---$/{c++; next} c==1 && /^name:/{sub(/^name:[[:space:]]*/, ""); gsub(/["'"'"']/, ""); print; exit}' "$ROOT/$root/SKILL.md")"
    if [ "$fm" != "$base" ]; then
      fail "skill:$name — SKILL.md frontmatter name '$fm' differs from directory '$base'"
      name_mismatches=$((name_mismatches + 1))
    fi
  fi

  # 4. every file listed in `files:` exists under the root
  files="$(printf '%s' "$line" | sed -nE 's/.*files:[[:space:]]*\[([^]]*)\].*/\1/p')"
  if [ -n "$files" ]; then
    IFS=',' read -ra parts <<< "$files"
    for p in "${parts[@]}"; do
      p="$(printf '%s' "$p" | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//')"
      [ -n "$p" ] || continue
      if [ ! -f "$ROOT/$root/$p" ]; then
        fail "$kind:$name — declared file missing: $root/$p"
        missing_files=$((missing_files + 1))
      fi
    done
  fi
done <<< "$entries"

assert_eq 0 "$missing_roots"    "every declared root exists on disk"
assert_eq 0 "$bad_skills"       "every skill root carries a SKILL.md"
assert_eq 0 "$name_mismatches"  "declared name, directory name and SKILL.md frontmatter agree"
assert_eq 0 "$missing_files"    "every file declared in a files: list exists"

assert_done
