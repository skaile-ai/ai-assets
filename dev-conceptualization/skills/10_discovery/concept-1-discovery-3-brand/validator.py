#!/usr/bin/env python3
"""Auto-generated validator for concept-1-discovery-3-brand.
Re-generate with: /compile-validators concept-1-discovery-3-brand
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-1-discovery-3-brand"
IDENTITY = "_concept/1_discovery/3_brand/identity.md"
TOKENS = "_concept/1_discovery/3_brand/tokens.json"

# All required top-level sections in tokens.json
REQUIRED_TOKEN_SECTIONS = ("colors", "fonts", "radius", "mode", "shadows", "atmosphere", "tailwind")

# Required color keys
REQUIRED_COLORS = (
    "primary", "secondary", "accent", "background", "surface",
    "text", "text_muted", "border", "error", "success", "warning",
)

# Required tailwind CSS custom properties
REQUIRED_TAILWIND_KEYS = (
    "--color-primary", "--color-primary-foreground", "--color-secondary",
    "--color-background", "--color-surface", "--color-foreground",
    "--color-muted", "--color-border", "--color-destructive",
    "--color-success", "--color-warning", "--radius",
)


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    # MUST include all fields downstream skills expect in tokens.json
    v.must("include all required sections in tokens.json", lambda: (
        v.json_field_exists(TOKENS, *REQUIRED_TOKEN_SECTIONS)
    ))

    # MUST include tailwind section with CSS custom properties
    def check_tailwind():
        data = v.read_json(TOKENS)
        if data is None:
            return False, f"Cannot read {TOKENS}"
        tw = data.get("tailwind")
        if not isinstance(tw, dict):
            return False, "tokens.json missing 'tailwind' object"
        missing = [k for k in REQUIRED_TAILWIND_KEYS if k not in tw]
        if missing:
            return False, f"tailwind section missing keys: {', '.join(missing)}"
        return True, ""

    v.must("include tailwind section with CSS custom properties", check_tailwind)

    # MUST include all required color keys
    def check_colors():
        data = v.read_json(TOKENS)
        if data is None:
            return False, f"Cannot read {TOKENS}"
        colors = data.get("colors")
        if not isinstance(colors, dict):
            return False, "tokens.json missing 'colors' object"
        missing = [k for k in REQUIRED_COLORS if k not in colors]
        if missing:
            return False, f"colors section missing: {', '.join(missing)}"
        return True, ""

    v.must("include all required color keys", check_colors)

    # MUST include font keys
    def check_fonts():
        data = v.read_json(TOKENS)
        if data is None:
            return False, f"Cannot read {TOKENS}"
        fonts = data.get("fonts")
        if not isinstance(fonts, dict):
            return False, "tokens.json missing 'fonts' object"
        for key in ("heading", "body", "mono"):
            if key not in fonts:
                return False, f"fonts section missing '{key}'"
        return True, ""

    v.must("include heading, body, mono font keys", check_fonts)

    # identity.md must have correct frontmatter
    v.must("identity.md has correct frontmatter", lambda: (
        v.frontmatter_has_fields(IDENTITY, "status", "mood", "mode", "last_updated")
    ))

    # ── NEVER rules ──

    # NEVER produce generic brand output (primary blue, gray secondary, Inter font)
    def check_not_generic():
        data = v.read_json(TOKENS)
        if data is None:
            return True, ""  # can't check without file
        colors = data.get("colors", {})
        fonts = data.get("fonts", {})
        # Flag obviously generic choices
        generic_issues = []
        primary = (colors.get("primary") or "").lower()
        secondary = (colors.get("secondary") or "").lower()
        heading = (fonts.get("heading") or "").lower()
        body = (fonts.get("body") or "").lower()
        # Common generic blue values
        if primary in ("#3b82f6", "#2563eb", "#1d4ed8", "#0066cc", "#007bff"):
            generic_issues.append(f"primary color {primary} is a common generic blue")
        # Gray secondary
        if secondary.startswith("#8") or secondary.startswith("#9") or secondary.startswith("#6"):
            if all(c in "0123456789abcdef#" for c in secondary):
                # Check if it's a gray (R≈G≈B)
                if len(secondary) == 7:
                    r, g, b = int(secondary[1:3], 16), int(secondary[3:5], 16), int(secondary[5:7], 16)
                    if max(r, g, b) - min(r, g, b) < 20:
                        generic_issues.append(f"secondary color {secondary} is a gray")
        # Inter font (most common generic choice)
        if heading == "inter" or body == "inter":
            generic_issues.append("Inter font detected — choose a more distinctive typeface")
        if generic_issues:
            return False, "; ".join(generic_issues)
        return True, ""

    v.never("produce generic brand output", check_not_generic)

    # NEVER write tokens.json without discussing aesthetic direction first
    v.skip("write tokens.json without discussing aesthetic direction first",
           rule_type="NEVER", reason="process — cannot verify conversation flow")

    # NEVER ignore reference URLs
    v.skip("ignore reference URLs", rule_type="NEVER",
           reason="process — cannot verify URL handling from files")

    # NEVER produce cookie-cutter brand
    v.skip("produce cookie-cutter brand that could belong to any app",
           rule_type="NEVER", reason="semantic — quality judgment")

    # ── CHECKLIST ──

    v.skip("Aesthetic direction discussed and agreed with user",
           rule_type="CHECKLIST", reason="process — user interaction")

    v.skip("Brand has a stated memorable element",
           rule_type="CHECKLIST", reason="semantic — content quality")

    v.skip("Typography choices justified for aesthetic direction",
           rule_type="CHECKLIST", reason="semantic — content quality")

    v.skip("Color usage rules defined (not just hex values)",
           rule_type="CHECKLIST", reason="semantic — content depth")

    v.checklist("tokens.json includes all required sections", lambda: (
        v.json_field_exists(TOKENS, *REQUIRED_TOKEN_SECTIONS)
    ))

    v.checklist("identity.md has correct frontmatter", lambda: (
        v.frontmatter_has_fields(IDENTITY, "status", "mood", "mode", "last_updated")
    ))

    v.skip("Reference URLs extracted and screenshots saved (if provided)",
           rule_type="CHECKLIST", reason="conditional — depends on user input")

    return v.result()


if __name__ == "__main__":
    main(validate)
