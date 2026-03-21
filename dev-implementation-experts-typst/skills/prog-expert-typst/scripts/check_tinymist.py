# /// script
# requires-python = ">=3.11"
# dependencies = [
#     "typer",
#     "rich",
# ]
# ///

import subprocess
import sys
from pathlib import Path
import typer
from rich.console import Console
from rich.panel import Panel

app = typer.Typer(help="Check a Typst file for errors using tinymist LSP CLI.")
console = Console()

@app.command()
def check(
    typst_file: Path = typer.Argument(..., help="Path to the main.typ file"),
):
    """Run tinymist compile to analyze the file for errors."""
    if not typst_file.exists():
        console.print(f"[red]Typst file {typst_file} not found![/red]")
        raise typer.Exit(1)

    console.print(f"[cyan]Analyzing {typst_file.name} with tinymist...[/cyan]")
    
    try:
        # Run tinymist compile. Input file is typst_file, output to /dev/null or bit bucket
        # So we just analyze the document without producing output files
        result = subprocess.run(
            ["tinymist", "compile", str(typst_file), "-", "-f", "pdf"],
            capture_output=True,
            text=True
        )
        
        stderr = result.stderr.strip()
        
        if result.returncode != 0:
            console.print(Panel(stderr if stderr else "No output", title="[red]Errors Found[/red]", border_style="red"))
            raise typer.Exit(result.returncode)
        else:
            if stderr:
                console.print(Panel(stderr, title="[yellow]Warnings[/yellow]", border_style="yellow"))
            console.print("[green]✓ Analysis passed. No critical errors found.[/green]")
    
    except FileNotFoundError:
        console.print("[red]tinymist CLI not found! Please ensure it is installed and in PATH.[/red]")
        raise typer.Exit(1)

if __name__ == "__main__":
    app()
