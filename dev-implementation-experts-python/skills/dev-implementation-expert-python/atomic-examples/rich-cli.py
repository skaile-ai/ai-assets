"""
Atomic Example: Rich terminal output patterns.
"""
from rich.console import Console
from rich.panel import Panel
from rich.table import Table
from rich.progress import Progress, SpinnerColumn, TextColumn

console = Console()

# Basic markup
console.print("[bold cyan]Starting...[/bold cyan]")
console.print("[bold red]Error:[/bold red] something went wrong")
console.print("[yellow]Warning:[/yellow] check your config")

# Panel
console.print(Panel("[green]Success![/green]", title="Result", border_style="green"))

# Table
table = Table(title="Providers")
table.add_column("Name", style="cyan")
table.add_column("Model", style="magenta")
table.add_column("Status", style="green")
table.add_row("openai", "gpt-4o", "active")
table.add_row("gemini", "gemini-2.0-flash", "active")
console.print(table)

# Progress spinner
with Progress(SpinnerColumn(), TextColumn("[progress.description]{task.description}")) as progress:
    task = progress.add_task("Generating response...", total=None)
    # ... do work ...
    progress.remove_task(task)
