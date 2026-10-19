#!/usr/bin/env bash
#
# A catalog ref (`kind:@skaile-ai/name`) in a `*.preset.yaml` or an `agent.yaml`
# `composes:` list is only a string until a consumer installs it. Nothing at
# author time checks it, so a renamed or moved asset leaves a dangling ref and
# the failure surfaces in the consumer's resolver, far from the edit that caused
# it.
#
# This pins six properties, driven by a walk of the tree (no manifest list):
#
#   1. every SKILL.md frontmatter `name:` equals its parent directory name
#      (CLAUDE.md rule; the resolver depends on both agreeing),
#   2. every `kind:@skaile-ai/name` ref in a `*.preset.yaml` item or an
#      `agent.yaml` `composes:` entry resolves to exactly one asset in this tree
#      by kind + name (zero = dangling, more than one = ambiguous),
#   3. the cross-repo allowlist below does not go stale,
#   4. every path ref (`ref: ./skills/x`, `ref: ../../../_agent-parts/...`) and
#      every `extends:` in an agent.yaml / *.preset.yaml exists, resolved against
#      the directory of the file that declares it, stays inside this repo, and is
#      a directory holding an asset manifest (refs) or a regular file (extends).
#      A path that climbs one level too far is silently inert in the resolver
#      (this is how an `extends:` once pointed above the repo root unnoticed),
#   5. every `assets:` entry and `include:` glob of catalogs/*.catalog.yaml
#      resolves to a skill or flow in this tree (an entry from another repo goes
#      on the allowlist as `catalog:<entry>`),
#   6. every `source_path` in catalogs/dist/*.catalog.json exists on disk (dist/ is
#      compiled elsewhere and may lag the YAML; it must not dangle).
#
# How an asset is identified: skill = SKILL.md `name:`, agent = agent.yaml `name:`,
# contract = CONTRACT.md `name:`, persona/ruleset = PERSONA.md/RULESET.md `name:`,
# mcp-server = MCP.md `name:`, preset = `name:` of the *.preset.yaml. Skills may
# sit at any depth (skaile-forge/agent/skills/<n>/, knowledge-research/skills/
# knowledge-writing/<n>/), so the match is by name, never by path.

set -uo pipefail
ROOT="${REPO_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && git rev-parse --show-toplevel)}"
source "$ROOT/scripts/lib/assert.sh"

# Refs that name an asset published from another repo under the same publisher.
# They cannot resolve inside this tree, so they are listed here with their owner.
# A listed ref that DOES resolve in-tree fails the test: remove it from the list.
EXTERNAL_REFS="
agent:skaileup-conceptualize
agent:impl-build-implement
agent:quality
"
# ^ shipped from skaile-ai/ai-assets-skaileup (publisher skaile-ai). Referenced by
#   ai-asset-management/agents/skaile/agent.yaml (composed as delegate agents).

PUBLISHER="skaile-ai"
INDEX="$(mktemp)"
trap 'rm -f "$INDEX"' EXIT

# frontmatter `name:` of a markdown file, quotes stripped
fm_name() {
  awk '/^---[[:space:]]*$/{c++; next} c==1 && /^name:/{sub(/^name:[[:space:]]*/, ""); gsub(/["'"'"']/, ""); sub(/[[:space:]]+$/, ""); print; exit}' "$1"
}

# top-level `name:` of a yaml file, quotes stripped
yaml_name() {
  awk '/^name:/{sub(/^name:[[:space:]]*/, ""); gsub(/["'"'"']/, ""); sub(/[[:space:]]+$/, ""); print; exit}' "$1"
}

# ── Index every asset: kind <TAB> name <TAB> path ───────────────────────────
# Hidden dirs (.git, .worktrees, .scratch) and node_modules are not part of the
# published tree.
while IFS= read -r f; do
  rel="${f#"$ROOT"/}"
  case "$(basename "$f")" in
    SKILL.md)    kind=skill;      name="$(fm_name "$f")" ;;
    CONTRACT.md) kind=contract;   name="$(fm_name "$f")" ;;
    PERSONA.md)  kind=persona;    name="$(fm_name "$f")" ;;
    RULESET.md)  kind=ruleset;    name="$(fm_name "$f")" ;;
    MCP.md)      kind=mcp-server; name="$(fm_name "$f")" ;;
    agent.yaml)  kind=agent;      name="$(yaml_name "$f")" ;;
    *.preset.yaml) kind=preset;   name="$(yaml_name "$f")" ;;
    *) continue ;;
  esac
  printf '%s\t%s\t%s\n' "$kind" "$name" "$rel" >> "$INDEX"
done < <(find "$ROOT" -mindepth 1 \( -name node_modules -o -name '.*' \) -prune -o -type f \
  \( -name SKILL.md -o -name CONTRACT.md -o -name PERSONA.md -o -name RULESET.md -o -name MCP.md \
     -o -name agent.yaml -o -name '*.preset.yaml' \) -print | sort)

indexed="$(grep -c . "$INDEX" || true)"
if [ "$indexed" -lt 100 ]; then
  fail "indexed only $indexed assets — the tree walk is broken"
  assert_done
fi
pass "indexed $indexed assets"

# An asset with no readable name can never be referenced.
unnamed="$(awk -F'\t' '$2 == "" {print "     " $1 " " $3}' "$INDEX")"
if [ -n "$unnamed" ]; then
  fail "assets with no name: found
$unnamed"
else
  pass "every indexed asset has a name"
fi

# ── 1. SKILL.md name == directory name ──────────────────────────────────────
mismatches=""
while IFS="$(printf '\t')" read -r kind name path; do
  [ "$kind" = skill ] || continue
  dir="$(basename "$(dirname "$path")")"
  if [ "$name" != "$dir" ]; then
    mismatches="$mismatches
     $path — frontmatter name '$name', directory '$dir'"
  fi
done < "$INDEX"
if [ -n "$mismatches" ]; then
  fail "SKILL.md name differs from its directory:$mismatches"
else
  pass "every SKILL.md name matches its directory"
fi

# ── 2. refs resolve to exactly one asset ────────────────────────────────────
# Sources: `ref:` values in *.preset.yaml items and agent.yaml composes entries.
# Comment lines are skipped. Non-`kind:@pub/name` refs (./local, library://) are
# not catalog refs and are left alone.
REFS="$(mktemp)"
# `ref: kind:@pub/name`, bare or quoted, alone on its line or inside `{ ... }`.
# No \b here: BSD sed (macOS) lacks it.
REF_SED="s/.*(^|[^A-Za-z_])ref:[[:space:]]*[\"']?([a-z][a-z0-9-]*:@[^\"',} ]+).*/\\2/p"
trap 'rm -f "$INDEX" "$REFS"' EXIT
while IFS= read -r f; do
  grep -vE '^[[:space:]]*#' "$f" \
    | sed -nE "$REF_SED" \
    | sed -E 's/#.*$//' \
    | while IFS= read -r ref; do printf '%s\t%s\n' "${f#"$ROOT"/}" "$ref"; done >> "$REFS"
done < <(find "$ROOT" -mindepth 1 \( -name node_modules -o -name '.*' \) -prune -o -type f \
  \( -name agent.yaml -o -name '*.preset.yaml' \) -print | sort)

ref_count="$(grep -c . "$REFS" || true)"
if [ "$ref_count" -lt 50 ]; then
  fail "found only $ref_count catalog refs — the ref scan is broken"
  assert_done
fi

dangling=0
ambiguous=0
stale_external=0
checked=0
while IFS="$(printf '\t')" read -r src ref; do
  kind="${ref%%:*}"
  rest="${ref#*:@}"
  publisher="${rest%%/*}"
  name="${rest#*/}"
  [ "$publisher" = "$PUBLISHER" ] || continue
  checked=$((checked + 1))

  matches="$(awk -F'\t' -v k="$kind" -v n="$name" '$1 == k && $2 == n {print $3}' "$INDEX")"
  count="$(printf '%s' "$matches" | grep -c . || true)"

  if printf '%s' "$EXTERNAL_REFS" | grep -qxF "$kind:$name"; then
    if [ "$count" -gt 0 ]; then
      fail "$src — $ref is on the external allowlist but resolves in this tree ($matches); drop it from EXTERNAL_REFS"
      stale_external=$((stale_external + 1))
    fi
    continue
  fi

  if [ "$count" -eq 0 ]; then
    fail "$src — $ref matches no $kind asset in this tree"
    dangling=$((dangling + 1))
  elif [ "$count" -gt 1 ]; then
    fail "$src — $ref is ambiguous, $count $kind assets are named '$name':
$(printf '%s\n' "$matches" | sed 's/^/       /')"
    ambiguous=$((ambiguous + 1))
  fi
done < "$REFS"

assert_eq 0 "$dangling"        "every @$PUBLISHER ref resolves to an asset in this tree ($checked checked)"
assert_eq 0 "$ambiguous"       "no ref matches more than one asset"
assert_eq 0 "$stale_external"  "no entry of the cross-repo allowlist resolves in-tree"

# ── 4. path refs and `extends:` exist ───────────────────────────────────────
# `ref:` values that are neither catalog refs (`kind:@pub/name`) nor URLs, plus
# every `extends:` value. Resolved against the declaring file's own directory.
ROOT_REAL="$(cd "$ROOT" && pwd -P)"
PATHREFS="$(mktemp)"
STRIPPED="$(mktemp)"
trap 'rm -f "$INDEX" "$REFS" "$PATHREFS" "$STRIPPED"' EXIT
TAB="$(printf '\t')"
# BSD sed has no `\t` in a replacement, so the sed prints "<what><TAB><value>" via a literal tab.
PATHREF_SED="s/.*(^|[^A-Za-z_])ref:[[:space:]]*[\"']?([^\"',} ]+).*/ref${TAB}\\2/p"
EXTENDS_SED="s/^[[:space:]]*extends:[[:space:]]*[\"']?([^\"'[:space:]]+).*/extends${TAB}\\1/p"
while IFS= read -r f; do
  rel="${f#"$ROOT"/}"
  # One comment-stripped stream feeds both seds, like the catalog-ref scan above.
  grep -vE '^[[:space:]]*#' "$f" > "$STRIPPED"
  { sed -nE "$PATHREF_SED" "$STRIPPED"; sed -nE "$EXTENDS_SED" "$STRIPPED"; } \
    | awk -F'\t' -v src="$rel" '$2 !~ /^[a-z][a-z0-9-]*:/ {print src "\t" $1 "\t" $2}' >> "$PATHREFS"
done < <(find "$ROOT" -mindepth 1 \( -name node_modules -o -name '.*' \) -prune -o -type f \
  \( -name agent.yaml -o -name '*.preset.yaml' \) -print | sort)

path_count="$(grep -c . "$PATHREFS" || true)"
if [ "$path_count" -lt 5 ]; then
  fail "found only $path_count path refs / extends — the path-ref scan is broken"
  assert_done
fi

bad_paths=0
while IFS="$(printf '\t')" read -r src what ref; do
  target="$(dirname "$ROOT/$src")/$ref"
  if [ ! -e "$target" ]; then
    fail "$src — $what '$ref' does not exist (resolved from $(dirname "$src")/)"
    bad_paths=$((bad_paths + 1))
    continue
  fi
  real="$(cd "$(dirname "$target")" && pwd -P)/$(basename "$target")"
  case "$real" in
    "$ROOT_REAL"/*) ;;
    *) fail "$src — $what '$ref' resolves outside the repository ($real)"
       bad_paths=$((bad_paths + 1)); continue ;;
  esac
  if [ "$what" = extends ]; then
    [ -f "$target" ] || { fail "$src — extends '$ref' is not a file"; bad_paths=$((bad_paths + 1)); }
  elif [ -d "$target" ]; then
    found=0
    for m in SKILL.md CONTRACT.md PERSONA.md RULESET.md MCP.md agent.yaml; do
      [ -f "$target/$m" ] && found=1
    done
    [ "$found" -eq 1 ] || { fail "$src — ref '$ref' is a directory with no asset manifest"; bad_paths=$((bad_paths + 1)); }
  fi
done < "$PATHREFS"
assert_eq 0 "$bad_paths" "every path ref and extends: exists inside the repo ($path_count checked)"

# ── 5. catalogs/*.catalog.yaml entries resolve ──────────────────────────────
# A catalog lists `assets:` as `<domain>/<name>` (the skill `<name>` or flow
# `<name>.flow.yaml` somewhere under `<domain>/`; `*` allowed, e.g.
# `knowledge-writing/*`) and `include:` as globs relative to the repo root. An
# entry that names a domain which ships from another repo goes on EXTERNAL_REFS
# as `catalog:<entry>`; like the ref allowlist, a listed entry that resolves
# in-tree fails.
CATALOG_COUNT=0
bad_catalog=0
for cat in "$ROOT"/catalogs/*.catalog.yaml; do
  [ -f "$cat" ] || continue
  crel="${cat#"$ROOT"/}"
  grep -vE '^[[:space:]]*#' "$cat" > "$STRIPPED"
  while IFS= read -r entry; do
    CATALOG_COUNT=$((CATALOG_COUNT + 1))
    dom="${entry%%/*}"
    pat="${entry#*/}"
    found=""
    if [ -d "$ROOT/$dom" ]; then
      found="$(find "$ROOT/$dom" \( -name node_modules -o -name '.*' \) -prune -o -type f \
        \( -path "*/$pat/SKILL.md" -o -path "*/$pat.flow.yaml" -o -path "*/$pat/CONTRACT.md" \) -print | head -1)"
    fi
    if printf '%s' "$EXTERNAL_REFS" | grep -qxF "catalog:$entry"; then
      [ -z "$found" ] || { fail "$crel — catalog:$entry is on the external allowlist but resolves in this tree; drop it from EXTERNAL_REFS"; bad_catalog=$((bad_catalog + 1)); }
      continue
    fi
    [ -n "$found" ] || { fail "$crel — assets entry '$entry' matches no skill or flow under $dom/"; bad_catalog=$((bad_catalog + 1)); }
  done < <(sed -nE 's/^[[:space:]]*-[[:space:]]*["'"'"']?([a-z0-9][^"'"'"' ]*\/[^"'"'"' ]+)["'"'"']?[[:space:]]*$/\1/p' "$STRIPPED" | grep -v '/SKILL\.md$' | grep -v '\.flow\.yaml$')
  # include: globs (and only those: they end in SKILL.md or .flow.yaml)
  while IFS= read -r glob; do
    CATALOG_COUNT=$((CATALOG_COUNT + 1))
    if ! compgen -G "$ROOT/$glob" > /dev/null; then
      fail "$crel — include glob '$glob' matches no file"
      bad_catalog=$((bad_catalog + 1))
    fi
  done < <(sed -nE 's/^[[:space:]]*-[[:space:]]*["'"'"']?([^"'"'"' ]+(SKILL\.md|\.flow\.yaml))["'"'"']?[[:space:]]*$/\1/p' "$STRIPPED")
  # catalog.manifest.schema.json also allows `exclude:` globs. None is used; one that
  # appeared would be invisible to the scan above, so it fails until the scan handles it.
  if grep -qE '^[[:space:]]*exclude:' "$STRIPPED"; then
    fail "$crel — uses exclude:, which this scan does not evaluate; extend section 5"
    bad_catalog=$((bad_catalog + 1))
  fi
done
if [ "$CATALOG_COUNT" -lt 45 ]; then
  fail "checked only $CATALOG_COUNT catalog entries and globs - the catalog scan is broken"
  assert_done
fi
assert_eq 0 "$bad_catalog" "every catalogs/*.catalog.yaml asset entry and include glob resolves ($CATALOG_COUNT checked)"

# ── 6. catalogs/dist/*.catalog.json source paths exist ──────────────────────
# dist/ is build output compiled from the YAML above by a compiler that lives in
# the consumer monorepo (catalogs/build.ts), so it cannot be regenerated here and
# may lag its manifest (see the CHANGELOG of ai-asset-management). It must not
# point at a directory that no longer exists: a rename leaves a dangling path.
DIST_COUNT=0
bad_dist=0
for json in "$ROOT"/catalogs/dist/*.catalog.json; do
  [ -f "$json" ] || continue
  jrel="${json#"$ROOT"/}"
  while IFS= read -r sp; do
    DIST_COUNT=$((DIST_COUNT + 1))
    [ -d "$ROOT/$sp" ] || [ -f "$ROOT/$sp" ] || { fail "$jrel — source_path '$sp' does not exist"; bad_dist=$((bad_dist + 1)); }
  done < <(sed -nE 's/^[[:space:]]*"source_path":[[:space:]]*"([^"]+)".*$/\1/p' "$json")
done
if [ "$DIST_COUNT" -lt 40 ]; then
  fail "checked only $DIST_COUNT dist source paths - the dist scan is broken"
  assert_done
fi
assert_eq 0 "$bad_dist" "every catalogs/dist/*.catalog.json source_path exists ($DIST_COUNT checked)"

assert_done
