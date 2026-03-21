#!/usr/bin/env python3
# /// script
# requires-python = ">=3.11"
# dependencies = [
#   "requests>=2.31.0",
#   "typer>=0.12.0",
#   "rich>=13.7.0",
#   "python-dotenv>=1.0.0",
# ]
# ///
import os
import sys
from pathlib import Path
import typer
import requests
from rich.console import Console
from rich.markdown import Markdown
from dotenv import load_dotenv

app = typer.Typer(
    help="Search the web and ask complex questions via Perplexity through OpenRouter.",
    invoke_without_command=True,
    no_args_is_help=True,
)
console = Console()
err_console = Console(stderr=True)

# Load environment variables from the .env file in the skill's root folder
skill_dir = Path(__file__).resolve().parent.parent
env_path = skill_dir / ".env"
load_dotenv(dotenv_path=env_path)

OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

def get_api_key() -> str:
    """Retrieve the OpenRouter API key from the environment/dotenv."""
    api_key = os.environ.get("OPENROUTER_API_KEY")
    if not api_key:
        err_console.print(
            f"[bold red]Error:[/bold red] OPENROUTER_API_KEY is not set.\n"
            f"Please create [cyan]{env_path}[/cyan] with your API key:\n\n"
            "  OPENROUTER_API_KEY=sk-or-v1-...\n"
        )
        raise typer.Exit(code=1)
    return api_key

@app.command()
def ask(
    query: str = typer.Argument(..., help="The search query or question to research."),
    model: str = typer.Option(
        "perplexity/sonar-reasoning",
        "--model",
        "-m",
        help="The OpenRouter model to use.",
    ),
    raw: bool = typer.Option(
        False, "--raw", help="Output raw text instead of rendering markdown."
    ),
):
    """Research a topic using Perplexity via OpenRouter."""
    api_key = get_api_key()

    headers = {
        "Authorization": f"Bearer {api_key}",
        "HTTP-Referer": "https://github.com/rmyndharis/ai-agent-workspace", # Required by OpenRouter rankings
        "X-Title": "agentic-workspace Agent", # Required by OpenRouter rankings
        "Content-Type": "application/json",
    }

    # Prepare payload. We use the recommended format for chat completions.
    payload = {
        "model": model,
        "messages": [
            {
                "role": "user",
                "content": query
            }
        ],
    }

    try:
        response = requests.post(
            OPENROUTER_URL,
            headers=headers,
            json=payload,
            timeout=120, # Some complex queries via reasoning models can take time
        )
        response.raise_for_status()
        data = response.json()
    except requests.RequestException as e:
        err_console.print(f"[bold red]API Request failed:[/bold red] {e}")
        if 'response' in locals() and hasattr(response, 'text'):
            err_console.print(f"[bold red]Response Payload:[/bold red] {response.text}")
        raise typer.Exit(code=1)

    # Extract answer
    choices = data.get("choices", [])
    if not choices:
        err_console.print("[bold red]No choices returned from OpenRouter.[/bold red]")
        raise typer.Exit(code=1)
    
    answer_content = choices[0].get("message", {}).get("content", "").strip()
    
    if not answer_content:
        err_console.print("[bold red]Empty content returned from the model.[/bold red]")
        raise typer.Exit(code=1)

    if raw:
        print(answer_content)
    else:
        # Output nicely formatted markdown for the agent to read
        print(f"# Research Results for: {query}\n")
        print(f"**Model:** {model}\n")
        print("---\n")
        print(answer_content)

if __name__ == "__main__":
    app()
