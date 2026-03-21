# /// script
# requires-python = ">=3.11"
# dependencies = ["rich"]
# ///

import os
import sys
from rich.console import Console

console = Console()
EXAMPLES_DIR = os.path.join(os.path.dirname(__file__), "../atomic-examples")

def main():
    if len(sys.argv) < 2:
        console.print("Usage: uv run manage_examples.py <list|scaffold> [name]")
        sys.exit(1)
    action = sys.argv[1]
    if action == "list":
        files = os.listdir(EXAMPLES_DIR) if os.path.exists(EXAMPLES_DIR) else []
        console.print("[bold cyan]Atomic examples:[/bold cyan]")
        for f in files:
            console.print(f"  - {f}")
    elif action == "scaffold":
        name = sys.argv[2] if len(sys.argv) > 2 else "example"
        path = os.path.join(EXAMPLES_DIR, name)
        os.makedirs(path, exist_ok=True)
        console.print(f"[green]Scaffolded example dir: {path}[/green]")
    else:
        console.print(f"[red]Unknown action: {action}[/red]")

if __name__ == "__main__":
    main()
