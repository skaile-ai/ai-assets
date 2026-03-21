"""
TDD Guard — shared logic for state derivation and file classification.

Adapted from https://codeleash.dev/docs/tdd-guard/ for PostXL projects.

TDD Guard is activated automatically via skill frontmatter hooks in
implement-2-features and implement-2-features-1-page SKILL.md files.
The hooks only run while those skills are active.
"""

import hashlib
import re
from pathlib import Path
from typing import Optional


def get_app_dir(cwd: str = ".") -> Path:
    """
    Find the PostXL app directory by walking up from cwd looking for postxl-schema.json.

    This is where TDD logs are stored and file paths are resolved relative to.
    """
    p = Path(cwd).resolve()
    while True:
        if (p / "postxl-schema.json").exists():
            return p
        if p.parent == p:
            return Path(cwd).resolve()
        p = p.parent


# ---------------------------------------------------------------------------
# File classification patterns (PostXL project structure)
# ---------------------------------------------------------------------------
# Patterns match paths relative to the app directory (where .tdd-guard-active is).
# The classify_file function strips the app dir prefix before matching.

PROD_PATTERNS = [
    r"^frontend/src/pages/",
    r"^frontend/src/components/",
    r"^frontend/src/routes/",
    r"^frontend/src/hooks/",
    r"^frontend/src/lib/",
    r"^backend/libs/.*/src/.*\.ts$",
    r"^backend/apps/.*/src/.*\.ts$",
]

TEST_PATTERNS = [
    r"^e2e/specs/.*\.spec\.ts$",
    r"^e2e/specs/.*\.test\.ts$",
]

# Files that bypass TDD entirely (config, schema, docs, generated, scripts)
BYPASS_PATTERNS = [
    r"^postxl-schema\.json$",
    r"^frontend/src/routeTree\.gen\.ts$",
    r"\.md$",
    r"\.json$",
    r"\.css$",
    r"\.env",
    r"^scripts/",
    r"^docker",
    r"^\.claude/",
    r"^e2e/fixtures/",
]

# Test command patterns (PostXL / pnpm)
TEST_COMMAND_PATTERNS = [
    r"pnpm e2e",
    r"pnpm pw test",
    r"npx playwright test",
    r"pnpm run test",
    r"pnpm e2e:",
]

# Build command patterns — must pass before returning to initial
BUILD_COMMAND_PATTERNS = [
    r"pnpm run build",
    r"pnpm run test:types",
    r"tsc --noEmit",
    r"check-frontend-paths",
]

# Lint command patterns — must ALSO pass before returning to initial
LINT_COMMAND_PATTERNS = [
    r"pnpm run lint",
    r"npx eslint",
]


def classify_file(file_path: str, app_dir: Optional[Path] = None) -> str:
    """
    Classify a file as 'prod', 'test', or 'other'.

    - prod: source files that implement features (TDD enforced)
    - test: test spec files (TDD enforced)
    - other: config, docs, generated files (TDD bypass)

    If app_dir is provided, the file path is made relative to it before matching.
    This handles the case where the agent runs from a parent directory (e.g., saxe root)
    but edits files inside the app directory (e.g., saxe-platform/frontend/src/...).
    """
    p = file_path

    # Make relative to app directory if provided
    if app_dir:
        try:
            p = str(Path(p).resolve().relative_to(app_dir.resolve()))
        except ValueError:
            pass  # Not under app dir — classify as-is

    # Normalize (remove leading ./ or /)
    p = p.lstrip("./")

    # Check bypass first (most permissive)
    for pattern in BYPASS_PATTERNS:
        if re.search(pattern, p):
            return "other"

    # Check test patterns
    for pattern in TEST_PATTERNS:
        if re.search(pattern, p):
            return "test"

    # Check prod patterns
    for pattern in PROD_PATTERNS:
        if re.search(pattern, p):
            return "prod"

    return "other"


def is_test_command(command: str) -> bool:
    """Check if a bash command is a test execution command."""
    for pattern in TEST_COMMAND_PATTERNS:
        if re.search(pattern, command):
            return True
    return False


def is_build_command(command: str) -> bool:
    """Check if a bash command is a build/type-check command."""
    for pattern in BUILD_COMMAND_PATTERNS:
        if re.search(pattern, command):
            return True
    return False


def is_lint_command(command: str) -> bool:
    """Check if a bash command is a lint command."""
    for pattern in LINT_COMMAND_PATTERNS:
        if re.search(pattern, command):
            return True
    return False


def test_succeeded(stdout: str, stderr: str, exit_code: int) -> Optional[bool]:
    """
    Determine if a test command succeeded, failed, or is indeterminate.

    Returns True (passed), False (failed), or None (can't determine).
    """
    if exit_code == 0:
        return True
    if exit_code != 0:
        return False
    return None


# ---------------------------------------------------------------------------
# State derivation from TDD log
# ---------------------------------------------------------------------------

STATES = ("initial", "writing_tests", "red", "making_tests_pass", "verify_build")


def read_state(log_path: Path) -> str:
    """
    Scan the TDD log bottom-up for the last significant line to derive state.

    Returns one of: initial, writing_tests, red, making_tests_pass, verify_build

    The verify_build state requires BOTH [build] and [lint] to succeed
    after the last [test] success before transitioning to initial.
    """
    if not log_path.exists():
        return "initial"

    text = log_path.read_text().strip()
    if not text:
        return "initial"

    lines = text.splitlines()

    # Track what we've seen scanning bottom-up (most recent first)
    seen_build_pass = False
    seen_lint_pass = False

    for i, line in enumerate(reversed(lines)):
        stripped = line.rstrip()

        # Build result lines
        if stripped.startswith("[build]") and stripped.endswith("- SUCCEEDED"):
            seen_build_pass = True
            continue

        if stripped.startswith("[build]") and "- FAILED" in stripped:
            return "verify_build"

        # Lint result lines
        if stripped.startswith("[lint]") and stripped.endswith("- SUCCEEDED"):
            seen_lint_pass = True
            continue

        if stripped.startswith("[lint]") and "- FAILED" in stripped:
            return "verify_build"

        # Test result lines
        if stripped.startswith("[test]") and stripped.endswith("- SUCCEEDED"):
            # Tests passed — check if build AND lint also passed (above this line)
            if seen_build_pass and seen_lint_pass:
                return "initial"
            return "verify_build"

        if stripped.startswith("[test]") and "- FAILED" in stripped:
            preceding = _find_preceding_declaration(lines, len(lines) - 1 - i)
            if preceding == "green":
                return "making_tests_pass"
            return "red"

        # Declaration lines
        if stripped.startswith("## Red"):
            return "writing_tests"

        if stripped.startswith("## Green"):
            return "making_tests_pass"

    return "initial"


def _find_preceding_declaration(lines: list[str], test_line_idx: int) -> Optional[str]:
    """
    Look backwards from a test result line to find the preceding RED or GREEN declaration.
    """
    for i in range(test_line_idx - 1, -1, -1):
        stripped = lines[i].rstrip()
        if stripped.startswith("## Red"):
            return "red"
        if stripped.startswith("## Green"):
            return "green"
    return None


def get_green_allowlist(log_path: Path) -> list[str]:
    """
    Extract the file allowlist from the last ## Green declaration.

    During making_tests_pass state, only files listed in the Green declaration
    are editable.
    """
    if not log_path.exists():
        return []

    lines = log_path.read_text().strip().splitlines()
    files = []

    # Scan backwards to find the last ## Green header
    for i in range(len(lines) - 1, -1, -1):
        if lines[i].rstrip().startswith("## Green"):
            # Collect File: entries after this header
            for j in range(i + 1, len(lines)):
                stripped = lines[j].rstrip()
                if stripped.startswith("## ") or stripped.startswith("[test]"):
                    break
                if stripped.startswith("File: "):
                    files.append(stripped[6:].strip())
            break

    return files


# ---------------------------------------------------------------------------
# Log path derivation
# ---------------------------------------------------------------------------


def get_log_path(input_data: dict) -> Path:
    """
    Derive the TDD log path for this agent session.

    Uses agent_id for subagents, transcript_path hash for main agent.
    Log files are stored in the app directory (where .tdd-guard-active lives).
    """
    cwd = input_data.get("cwd", ".")
    app_dir = get_app_dir(cwd)

    agent_id = input_data.get("agent_id", "")
    if agent_id:
        return app_dir / f".tdd-{agent_id}.log"

    transcript = input_data.get("transcript_path", "")
    if transcript:
        key = hashlib.md5(transcript.encode()).hexdigest()[:8]
        return app_dir / f".tdd-{key}.log"

    return app_dir / ".tdd-guard.log"
