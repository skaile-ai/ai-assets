#!/usr/bin/env python3
# /// script
# requires-python = ">=3.12"
# dependencies = ["ruamel.yaml>=0.18"]
# ///
"""Mechanical rewriter from legacy skaile.yaml to canonical-identity shape.

Invoked by the migrate-skaile-manifest skill prompt. The skill's interactive
disambiguation has already resolved publisher ambiguities; this script applies
the deterministic mapping rules and emits the new YAML.

Usage:
    uv run scripts/migrate.py \\
        --in skaile.yaml \\
        --out skaile.yaml.new \\
        --publisher-map '{"<source-url>": "skaile-ai", ...}' \\
        [--dry-run]

The --publisher-map maps each source URL to its canonical publisher. The special
key "_bare_default" supplies the publisher for bare deps when it cannot be
inferred unambiguously (the skill resolves this with the user beforehand).

Exit codes: 0 on clean rewrite, 2 on unresolved ambiguity (the skill should have
caught it; this is a backstop).
"""
import re
import sys
from io import StringIO

from ruamel.yaml import YAML

# Floating refs that are never valid as a dependency #pin.
REJECTED_PINS = {"main", "latest", "head"}


class MigrationError(Exception):
    """Unresolved ambiguity or an illegal construct. Maps to exit code 2."""


def _infer_publisher_from_url(url: str) -> str | None:
    """Return the GitHub org/user for a github.com URL, else None."""
    m = re.match(r"https?://github\.com/([^/]+)/", url.rstrip("/") + "/")
    if m:
        return m.group(1)
    return None


def _publisher_for_url(url: str, pub_map: dict) -> str:
    if url in pub_map:
        return pub_map[url]
    inferred = _infer_publisher_from_url(url)
    if inferred:
        return inferred
    raise MigrationError(
        f"source URL is not on github.com and no publisher mapping provided: {url}. "
        "Re-run with this URL present in --publisher-map."
    )


def _rewrite_pin(pin: str) -> str:
    """Rewrite a dependency #pin (the part after '#'). Raises on floating refs."""
    if pin.lower() in REJECTED_PINS:
        raise MigrationError(
            f"non-canonical floating pin '#{pin}' is rejected at parse time "
            "(spec §Dependency refs: #main/#latest/#HEAD are not valid dep pins). "
            "Pin to a SemVer tag or a 40-char sha instead."
        )
    # Strip a leading 'v' from version-shaped pins, including range operators.
    m = re.match(r"^([\^~]?)v(\d.*)$", pin)
    if m:
        return m.group(1) + m.group(2)
    return pin


def _rewrite_dep(ref: str, pub_map: dict) -> str:
    """Rewrite a single dependency ref to canonical shape."""
    ref = ref.strip()
    pin = None
    if "#" in ref:
        ref, pin = ref.split("#", 1)
        pin = _rewrite_pin(pin)

    if "@" in ref:
        body, publisher = ref.rsplit("@", 1)
        if publisher == "skaile":  # legacy curated namespace rebrand
            publisher = "skaile-ai"
    else:
        body = ref
        publisher = pub_map.get("_bare_default")
        if not publisher:
            raise MigrationError(
                f"bare dep '{ref}' has no inferable publisher. The skill must "
                "resolve this with the user and pass _bare_default in --publisher-map."
            )

    out = f"{body}@{publisher}"
    if pin is not None:
        out += f"#{pin}"
    return out


def _scoped_dep(ref: str, publisher: str) -> str:
    """Scope a lifted dep to a specific publisher (used for ai_resources deps)."""
    ref = ref.strip()
    if "@" in ref.split("#", 1)[0]:
        # already has a publisher; honor it (with rebrand + pin rewrite)
        return _rewrite_dep(ref, {})
    return _rewrite_dep(ref, {"_bare_default": publisher})


def migrate(in_path: str, pub_map: dict) -> str:
    yaml = YAML()
    yaml.preserve_quotes = True
    yaml.indent(mapping=2, sequence=4, offset=2)
    with open(in_path) as f:
        doc = yaml.load(f)
    if doc is None:
        doc = {}

    sources: list = []
    lifted_deps: list = []

    # ── repositories: map → sources ────────────────────────────────────────
    repositories = doc.pop("repositories", None)
    if repositories:
        for _name, entry in repositories.items():
            url = entry["url"]
            src = {"url": url}
            if entry.get("branch"):
                src["pin"] = entry["branch"]
            sources.append(src)

    # ── ai_resources: array or object → sources + lifted deps ──────────────
    ai_resources = doc.pop("ai_resources", None)
    if ai_resources is not None:
        if isinstance(ai_resources, dict):
            # legacy object shape: { sources: [...], requires: [...] }
            res_sources = ai_resources.get("sources", [])
            requires = ai_resources.get("requires", [])
            for entry in res_sources:
                url = entry["path"] if isinstance(entry, dict) and "path" in entry else (
                    entry["url"] if isinstance(entry, dict) else entry)
                src = {"url": url}
                if isinstance(entry, dict) and entry.get("branch"):
                    src["pin"] = entry["branch"]
                sources.append(src)
            # single inferred publisher for the object shape
            pub = _publisher_for_url(sources[-1]["url"], pub_map) if res_sources else None
            for dep in requires:
                lifted_deps.append(_scoped_dep(dep, pub))
        else:
            # array shape: [{ name, path, branch?, dependencies? }]
            for entry in ai_resources:
                url = entry["path"]
                src = {"url": url}
                if entry.get("branch"):
                    src["pin"] = entry["branch"]
                sources.append(src)
                pub = _publisher_for_url(url, pub_map)
                for dep in entry.get("dependencies", []) or []:
                    lifted_deps.append(_scoped_dep(dep, pub))

    # ── dependencies: rewrite to canonical refs ────────────────────────────
    existing_deps = doc.pop("dependencies", None) or []
    new_deps = [_rewrite_dep(d, pub_map) for d in existing_deps]
    new_deps.extend(lifted_deps)

    # ── reassemble in canonical key order, preserving unknown keys ─────────
    out = {}
    if "name" in doc:
        out["name"] = doc.pop("name")
    if "publisher" in doc:
        out["publisher"] = doc.pop("publisher")
    if sources:
        out["sources"] = sources
    if "stores" in doc:
        out["stores"] = doc.pop("stores")
    if new_deps:
        out["dependencies"] = new_deps
    # carry through any remaining keys we did not transform
    for k, v in doc.items():
        out[k] = v

    buf = StringIO()
    yaml.dump(out, buf)
    return buf.getvalue()


if __name__ == "__main__":
    import argparse
    import json

    p = argparse.ArgumentParser()
    p.add_argument("--in", dest="in_path", required=True)
    p.add_argument("--out", dest="out_path", required=True)
    p.add_argument(
        "--publisher-map",
        default="{}",
        help="JSON object mapping source URL → canonical publisher",
    )
    p.add_argument("--dry-run", action="store_true")
    args = p.parse_args()
    pub_map = json.loads(args.publisher_map)
    try:
        result = migrate(args.in_path, pub_map)
    except MigrationError as e:
        sys.stderr.write(f"error: {e}\n")
        sys.exit(2)
    if args.dry_run:
        sys.stdout.write(result)
    else:
        with open(args.out_path, "w") as f:
            f.write(result)
