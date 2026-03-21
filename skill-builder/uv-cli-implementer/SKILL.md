---
name: uv-cli-implementer
description: Designs and implements small, focused, agent-friendly CLI tools using uv and Typer. Use when you need to wrap a capability as a command-line interface instead of an MCP server, so that LLM agents can invoke it via shell commands.
metadata:
  stage: alpha
  requires:
  - skill-builder-contract
---

# UV CLI Implementer

You are an expert in building minimal, self-contained CLI tools for LLM agents, using `uv` (PEP 723 inline deps) and `Typer`.

## Goal

Produce a single-file Python CLI that:
- Does exactly **one thing** well (Unix philosophy)
- Is runnable with `uv run <script>.py [args]` with zero setup
- Is self-documenting via `--help` so agents can discover usage dynamically
- Returns structured output (JSON preferred) on `stdout`
- Reports errors with descriptive messages on `stderr` and exits with a non-zero code

## Instructions

1. **Clarify scope**: Confirm the single responsibility of the tool. Reject requests for tools that do more than one conceptual thing — split them into two tools instead.

2. **Produce an artifact**: Before writing any implementation, write an artifact (e.g., `implementation_plan.md`) outlining the subfunctions, usage patterns, command-line arguments, and options.

3. **Ask for feedback**: Ask the user for feedback on the proposed design artifact. Do not proceed with implementation until the user approves.

4. **Choose the output file location**:
   - Agent-only tool used by one skill → `scripts/<tool-name>.py` inside the relevant skill directory
   - Reusable across multiple skills → standalone file at `<workspace>/.agent/tools/<tool-name>.py`

5. **Write the script** following the Template and PEP 723 rules below. Key rules:
   - `# /// script` block first — every dep listed, minimum version pinned (see **PEP 723 Inline Dependencies**)
   - Separate pure logic functions from the typer command — required for testability
   - `typer.Typer(add_completion=False)` — completions add noise for agents
   - Every `Argument` and `Option` must have a concise `help=` string
   - Use strict Python type hints (`int`, `str`, `Path`, `list[str]`, etc.)
   - Output: `print(json.dumps(result, indent=2))` for success
   - Errors: `typer.secho("Error: …", err=True, fg=typer.colors.RED)` + `raise typer.Exit(code=1)`
   - **Never** use interactive prompts (`typer.confirm`, `input()`) in agent tools

6. **Write tests** in a companion `test_<tool-name>.py` file (see **Testing** below):
   - Test pure logic functions — not the CLI wiring
   - Cover happy path, error cases, and key edge cases
   - Run: `uv run pytest test_<tool-name>.py -v`

7. **Verify the tool is agent-usable**:
   - Run `uv run <script>.py --help` and confirm output is readable and complete
   - Run a representative command and confirm JSON on stdout
   - Run a failure case and confirm non-zero exit + stderr message

8. **Store the docs in README.md**: Document usage patterns, subfunctions, and integration details. Add a "Script Integration" block showing the exact agent invocation pattern.

## Template

```python
# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "typer>=0.12.0",
# ]
# ///

import json
import typer

app = typer.Typer(
    help="One-line description of what this tool does.",
    add_completion=False,
)

@app.command()
def main(
    input: str = typer.Argument(..., help="The primary input value."),
    verbose: bool = typer.Option(False, "--verbose", "-v", help="Emit extra detail."),
) -> None:
    """
    Full description of the command's behaviour. Agents read this via --help.
    """
    try:
        result = {"input": input, "processed": True}
        print(json.dumps(result, indent=2))
    except Exception as exc:
        typer.secho(f"Error: {exc}", err=True, fg=typer.colors.RED)
        raise typer.Exit(code=1)

if __name__ == "__main__":
    app()
```

## PEP 723 Inline Dependencies

Every single-file script produced by this skill **must** declare its dependencies inline using the PEP 723 `# /// script` block. This is non-negotiable — it is what makes scripts runnable with `uv run` without any prior setup step.

### Rules

- The `# /// script` block must be the **first lines** of the file, before any imports.
- List **every non-stdlib dependency** with a minimum version pin (`>=`). Do not omit transitive deps that you import directly.
- Set `requires-python = ">=3.12"` unless there is an explicit reason to support older versions.
- Never instruct the user to `pip install` anything. If a dep is needed, it goes in the block.
- Keep the dep list minimal — one dep per capability, no kitchen-sink imports.

### Format

```python
# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "typer>=0.12.0",
#     "rich>=13.0.0",
#     "httpx>=0.27.0",
# ]
# ///
```

### Dev-only dependencies (tests)

Test files are also single-file scripts with their own `# /// script` block. Add `pytest` there, not in the main script:

```python
# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "pytest>=8.0",
# ]
# ///
```

---

## Testing

Every tool should have a companion test file `test_<tool-name>.py` placed next to the main script. Tests verify the pure logic in isolation — without invoking the CLI layer.

### Pattern: Separate Logic from CLI

Extract business logic into plain functions that take and return plain values. The typer command becomes a thin wrapper:

```python
# my_tool.py
# /// script
# requires-python = ">=3.12"
# dependencies = ["typer>=0.12.0"]
# ///

import json
import typer

app = typer.Typer(add_completion=False)

# ← Pure logic — testable without typer
def process(value: str) -> dict:
    if not value:
        raise ValueError("value must not be empty")
    return {"result": value.upper(), "length": len(value)}

@app.command()
def main(value: str = typer.Argument(..., help="Value to process.")) -> None:
    """Process a value and return JSON."""
    try:
        print(json.dumps(process(value), indent=2))
    except ValueError as exc:
        typer.secho(f"Error: {exc}", err=True, fg=typer.colors.RED)
        raise typer.Exit(1)

if __name__ == "__main__":
    app()
```

### Companion Test File

```python
# test_my_tool.py
# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "pytest>=8.0",
# ]
# ///

import sys
from pathlib import Path
import pytest

sys.path.insert(0, str(Path(__file__).parent))
from my_tool import process  # import pure functions only


def test_process_happy_path():
    result = process("hello")
    assert result["result"] == "HELLO"
    assert result["length"] == 5


def test_process_empty_raises():
    with pytest.raises(ValueError, match="must not be empty"):
        process("")


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
```

Run tests:
```bash
uv run pytest test_my_tool.py -v
# or self-execute:
uv run test_my_tool.py
```

### What to Test

Cover at minimum:
- **Happy path**: valid input → expected output shape and values
- **Error cases**: invalid input → correct exception or error signal
- **Edge cases**: empty strings, zero values, missing optional fields — wherever the logic branches

Skip testing:
- The typer CLI wiring itself (argument parsing, exit codes) — that is typer's responsibility
- External I/O (HTTP calls, filesystem writes) — mock or skip; test the logic around them

### When Tests Are Not Practical

Some scripts are pure glue (call an API, reformat output). If there is no extractable pure logic, write at minimum a **smoke test** that verifies the module imports cleanly and the core function exists:

```python
def test_imports():
    import my_tool
    assert callable(my_tool.process)
```

---

## Human-Facing Tools: Interactive Choices with InquirerPy

Some CLI tools are invoked **directly by humans** rather than agents. When the tool's purpose is to guide a user through a selection or configuration step, use **InquirerPy** for rich interactive prompts.

### When to Use InquirerPy

Use InquirerPy when **all** of these are true:
- The tool will be run by a human at a terminal (not piped or called by an agent)
- The user needs to choose from a list, confirm an action, or provide freeform input
- The number of options is too large or dynamic to list as CLI flags

Do **not** use InquirerPy when:
- The tool is agent-only (agents cannot respond to interactive prompts)
- stdout is piped or redirected (not a TTY)
- The choices are always known at invocation time (use CLI flags instead)

### TTY Guard

Always check for a TTY before invoking InquirerPy. Provide a non-interactive fallback:

```python
import sys

def _is_tty() -> bool:
    return sys.stdin.isatty() and sys.stdout.isatty()
```

### InquirerPy Dependency

Add to the PEP 723 header:
```python
# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "typer>=0.12.0",
#     "InquirerPy>=0.3.4",
# ]
# ///
```

### Question Types

```python
from InquirerPy import prompt

# Single selection from a list
answers = prompt([{
    "type": "list",
    "name": "target",
    "message": "Select a target:",
    "choices": ["option-a", "option-b", "option-c"],
}])
chosen = answers.get("target")

# Yes/no confirmation
answers = prompt([{
    "type": "confirm",
    "name": "confirmed",
    "message": "Save as default?",
    "default": True,
}])
save = answers.get("confirmed", False)

# Free-text input with optional default
answers = prompt([{
    "type": "input",
    "name": "path",
    "message": "Enter path:",
    "default": "/some/default",
}])
path = answers.get("path", "")
```

### Template: Tool with Interactive + Non-Interactive Modes

```python
# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "typer>=0.12.0",
#     "InquirerPy>=0.3.4",
#     "rich>=13.0.0",
# ]
# ///

import json
import sys
from typing import Optional
import typer
from InquirerPy import prompt
from rich.console import Console

app = typer.Typer(help="One-line description.", add_completion=False)
console = Console()

def _is_tty() -> bool:
    return sys.stdin.isatty() and sys.stdout.isatty()

@app.command()
def main(
    choice: Optional[str] = typer.Option(None, "--choice", "-c",
        help="Selection value. If omitted and running interactively, a menu is shown."),
) -> None:
    """
    Full description. Agents pass --choice explicitly.
    Humans can omit it for an interactive selection menu.
    """
    if choice is None:
        if not _is_tty():
            typer.secho("Error: --choice is required in non-interactive mode.", err=True, fg=typer.colors.RED)
            raise typer.Exit(1)
        answers = prompt([{
            "type": "list",
            "name": "v",
            "message": "Select an option:",
            "choices": ["alpha", "beta", "gamma"],
        }])
        choice = answers.get("v")
        if not choice:
            raise typer.Exit(0)

    result = {"choice": choice, "processed": True}
    print(json.dumps(result, indent=2))

if __name__ == "__main__":
    app()
```

---

## Constraints

* One command per file. Multiple sub-commands are allowed only with explicit justification.
* Do not use `input()`, `typer.confirm()`, or any blocking stdin reads — use InquirerPy for human tools, or flags for agent tools.
* Do not write to the file system unless that is the tool's explicit purpose.
* Do not assume any globally installed packages beyond `uv` and a Python runtime.
* Do not suppress exceptions silently — always map them to a stderr message + exit code 1.
* Minimum Python version: 3.12 (use modern `list[str]`, `str | None` syntax).

## Agent Invocation Pattern

Agents call these tools as shell commands, always passing explicit flags:

```bash
uv run scripts/<tool-name>.py <arg> [--option value]
# stdout → parsed as JSON result
# stderr → read on failure for self-correction
# exit code 0 = success, non-zero = failure
```

Human invocation (interactive tools):

```bash
uv run scripts/<tool-name>.py
# → InquirerPy menus appear for selection/confirmation
```
