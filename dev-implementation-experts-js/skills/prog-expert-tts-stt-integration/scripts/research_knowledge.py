# /// script
# requires-python = ">=3.11"
# dependencies = ["rich"]
# ///

# NOTE: Use 'use-context7-api' skill and web search for knowledge gathering.
import sys
from rich.console import Console

console = Console()

def main():
    if len(sys.argv) < 2:
        console.print("[bold red]Usage: uv run research_knowledge.py <query>[/bold red]")
        sys.exit(1)
    query = sys.argv[1]
    console.print(f"[bold cyan]Researching: {query}[/bold cyan]")
    console.print("[yellow]Delegate to use-context7-api skill or web search for: Deepgram nova-3, ElevenLabs v2, Nitro WebSocket.[/yellow]")

if __name__ == "__main__":
    main()
