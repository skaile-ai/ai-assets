#!/usr/bin/env python3
# /// script
# requires-python = ">=3.11"
# dependencies = [
#   "requests>=2.31.0",
#   "typer>=0.12.0",
#   "rich>=13.7.0",
# ]
# ///
"""
SearXNG search CLI for agentic-workspace agents.
Queries a local SearXNG instance and returns markdown-formatted results.
"""
import sys
from typing import Optional
import typer
import requests
from rich.console import Console

app = typer.Typer(
    help="Search the web via a local SearXNG instance.",
    invoke_without_command=True,
    no_args_is_help=True,
)
console = Console()
err_console = Console(stderr=True)

SEARXNG_URL = "http://localhost:8080"


def is_running() -> bool:
    """Check if the local SearXNG instance is reachable."""
    try:
        r = requests.get(f"{SEARXNG_URL}/", timeout=3)
        return r.status_code < 500
    except Exception:
        return False


def ensure_running():
    """Print a helpful error if SearXNG is not running."""
    if not is_running():
        err_console.print(
            "[bold red]Error:[/bold red] Local SearXNG is not running.\n"
            "Start it with:\n\n"
            "  [cyan]docker compose -f ~/.config/workspace/searxng/docker-compose.yml up -d[/cyan]\n",
        )
        raise typer.Exit(code=1)


@app.command()
def search(
    query: str = typer.Argument(..., help="The search query."),
    categories: Optional[str] = typer.Option(
        None,
        "--categories",
        "-c",
        help="Search categories (general, news, images, videos). Comma-separated.",
    ),
    time_range: Optional[str] = typer.Option(
        None,
        "--time_range",
        "-t",
        help="Limit results by time (day, week, month, year).",
    ),
    results: int = typer.Option(
        10, "--results", "-n", help="Number of results to show."
    ),
    google_only: bool = typer.Option(
        False, "--google-only", help="Only show results from Google."
    ),
    raw: bool = typer.Option(
        False, "--raw", help="Output raw JSON instead of formatted markdown."
    ),
):
    """Search the web and return results formatted for agent consumption."""
    ensure_running()

    params: dict = {
        "q": query,
        "format": "json",
        "pageno": 1,
    }
    if categories:
        params["categories"] = categories
    if time_range:
        params["time_range"] = time_range

    try:
        response = requests.get(
            f"{SEARXNG_URL}/search",
            params=params,
            timeout=30,
        )
        response.raise_for_status()
        data = response.json()
    except requests.RequestException as e:
        console.print(f"[bold red]Request failed:[/bold red] {e}", err=True)
        raise typer.Exit(code=1)

    items = data.get("results", [])

    if not items:
        print("No results found.")
        return

    # Sort: Google results first (highest weight), then by score
    def sort_key(r):
        engines = r.get("engines", [])
        google_boost = 100 if "google" in engines else 0
        return -(google_boost + r.get("score", 0))

    items.sort(key=sort_key)

    if google_only:
        items = [r for r in items if "google" in r.get("engines", [])]

    items = items[:results]

    if raw:
        import json
        print(json.dumps(items, indent=2, ensure_ascii=False))
        return

    # Markdown output for the agent
    print(f"# Search Results for: {query}\n")
    print(f"**Query:** `{query}`  ")
    if categories:
        print(f"**Categories:** {categories}  ")
    if time_range:
        print(f"**Time Range:** {time_range}  ")
    print(f"**Results:** {len(items)}\n")
    print("---\n")

    for i, result in enumerate(items, 1):
        title = result.get("title", "(No title)").strip()
        url = result.get("url", "")
        content = result.get("content", "").strip()
        engines_used = ", ".join(result.get("engines", []))
        pub_date = result.get("publishedDate") or result.get("pubdate", "")

        print(f"## {i}. {title}")
        print(f"**URL:** {url}")
        if engines_used:
            print(f"**Sources:** {engines_used}")
        if pub_date:
            print(f"**Published:** {pub_date}")
        if content:
            print(f"\n{content}")
        print()


@app.command()
def status():
    """Check if the local SearXNG instance is running."""
    if is_running():
        print("✅ SearXNG is running at http://localhost:8080")
    else:
        print("❌ SearXNG is NOT running.")
        print("Start it with:")
        print("  docker compose -f ~/.config/workspace/searxng/docker-compose.yml up -d")
        raise typer.Exit(code=1)


if __name__ == "__main__":
    app()
