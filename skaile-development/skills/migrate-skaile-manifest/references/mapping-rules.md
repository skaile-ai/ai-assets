# Mapping rules — legacy skaile.yaml → canonical-identity

This reference lifts the migration rules from the canonical spec
(`workspaces/.../_devlog/specs/2026-05-31-manifest-canonical-identity.md`,
§Migration → Migration skill) verbatim, then adds worked examples for the three
interactive-disambiguation cases and a pin-rewrite table.

## Canonical mapping rules (verbatim from spec §"Migration skill")

- Legacy `repositories:{...}` map → `sources:[{url, pin?}]` (drop the user-chosen
  name; cache key derived from URL).
- Legacy `ai_resources:[{name, path, dependencies}]` → `sources:[{url}]` plus deps
  lifted to top-level `dependencies:` with publisher inferred from URL org.
- Legacy `ai_resources` legacy object shape (`{sources: [...], requires: [...]}`)
  → same treatment as the array shape; `requires:` becomes top-level
  `dependencies:` scoped to the inferred publisher.
- Bare deps (`skill:foo` with no `@`) → `skill:foo@<inferred-publisher>` using the
  URL inheritance rule. If the project has multiple sources from different GitHub
  orgs and the dep is ambiguous, the skill asks the user.
- Dep refs that name the legacy curated `@skaile` namespace → rebrand to
  `@skaile-ai`.
- `metadata.version` in any SKILL.md frontmatter → stripped (version ownership
  moves to source `skaile.yaml`).
- For repos that are also sources: write the publication half (`publisher`,
  `version`, optionally `assets:` list).

## Canonical dep-ref grammar (verbatim from spec §"Dependency refs")

```
kind:name@<publisher>[#pin]
```

Pin forms (`#pin`):

| Pin | Meaning |
|---|---|
| `#1.4.0` | exact SemVer match |
| `#^1.4.0` | SemVer caret (`>=1.4.0, <2.0.0`) |
| `#~1.4.0` | SemVer tilde (`>=1.4.0, <1.5.0`) |
| `#<40-char-sha>` | exact source-commit pin |
| absent | resolver picks highest SemVer-sorted candidate |
| `#main`, `#latest`, `#HEAD`, any non-canonical floating ref | **rejected at parse time** |

Pre-releases follow standard SemVer: excluded from constraint matches unless the
pin itself is a pre-release (`#^1.4.0-beta`).

## Pin-form rewrites

| Legacy pin | New pin | Rule |
|---|---|---|
| `branch: main` (on a source) | `pin: main` | branch → pin; allowed on a *source* entry |
| `#main` (on a dep ref) | **REJECT** | floating refs are not valid dep pins |
| `#latest` (on a dep ref) | **REJECT** | floating refs are not valid dep pins |
| `#HEAD` (on a dep ref) | **REJECT** | floating refs are not valid dep pins |
| `#v1.4.0` | `#1.4.0` | strip leading `v` |
| `#1.4.0` | `#1.4.0` | already canonical, passthrough |
| `#^v1.4.0` | `#^1.4.0` | strip leading `v`, keep range op |
| `#^1.4.0-beta` | `#^1.4.0-beta` | pre-release passes through (standard SemVer) |
| `#<40-char-sha>` | `#<40-char-sha>` | exact commit pin, passthrough |

Note: a `pin:` on a `sources[]` entry may be a branch, tag, or 40-char sha
(`branch: main` → `pin: main` is fine there). The REJECT rules above apply to the
`#pin` suffix on **dependency refs**, not to the source `pin:` field.

## Worked example: (a) bare dep, multiple GitHub orgs

Input:

```yaml
repositories:
  a: { url: https://github.com/skaile-ai/ai-assets }
  b: { url: https://github.com/acme/extra-assets }
dependencies:
  - skill:audit          # bare — could be skaile-ai OR acme
```

The skill stops and asks:

```
Dep `skill:audit` is bare. Candidate publishers from your sources:
  1. skaile-ai   (https://github.com/skaile-ai/ai-assets)
  2. acme        (https://github.com/acme/extra-assets)
Which publisher owns skill:audit?
```

User answers `skaile-ai` → `--publisher-map` gets
`{"_bare_default": "skaile-ai"}` (or a per-dep override). Output:
`skill:audit@skaile-ai`.

## Worked example: (b) source URL not on github.com

Input:

```yaml
repositories:
  internal: { url: https://gitlab.example.com/team/assets }
```

The skill stops and asks:

```
Source https://gitlab.example.com/team/assets is not on github.com.
Provide the canonical publisher string (GitHub-shaped: ≤39 chars,
alphanumeric + hyphens):
```

User answers `example-team` → `--publisher-map` gets
`{"https://gitlab.example.com/team/assets": "example-team"}`. If the user
refuses to provide one, the migration is refused with a clear error.

## Worked example: (c) SKILL.md name disagrees with directory

Source repo migration only. If `foo-skill/SKILL.md` declares `name: foo` but its
parent directory is `foo-skill`:

```
SKILL.md name mismatch:
  directory: foo-skill
  declared : foo
Directory name wins. Edit SKILL.md `name:` to `foo-skill`? (y/N)
```

Directory name wins per spec §Reconciliation
("SKILL.md `name:` MUST equal the corresponding source-entry `name:` when both
exist. Mismatch is an index-time hard error."). Never rename directories — that
breaks consumer dep refs that were already canonical.

## Version-derivation note (verbatim from spec §"Version Semantics")

When a source declares no per-asset version, the resolver derives it:

1. The asset's entry in the source `skaile.yaml`'s `assets:` list declares
   `version:` → use it.
2. The source `skaile.yaml` top-level `version:` → use it.
3. `git describe --tags --abbrev=0 <source.pin>` returns a SemVer-shaped tag →
   strip leading `v` if present, use it.
4. Synthetic `0.0.0-sha.<7-char-of-pin-commit>`.

SHA-synthetic versions never satisfy SemVer-constraint pins (caret/tilde/
wildcard); they satisfy only their exact literal pin or the commit-SHA pin that
produced them.
