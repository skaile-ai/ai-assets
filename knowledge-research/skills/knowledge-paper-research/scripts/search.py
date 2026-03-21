# /// script
# requires-python = ">=3.11"
# dependencies = [
#     "typer>=0.12.0",
#     "rich>=13.0",
#     "httpx>=0.27.0",
# ]
# ///
"""
search.py — Query a SearXNG instance and print results as markdown.

Usage:
    uv run search.py "<query>" [--categories news] [--time_range year] [--results 15]

Environment:
    SEARXNG_URL — base URL of the SearXNG instance (default: http://localhost:8080)

Output:
    Markdown-formatted list of results to stdout.
    Each result includes: title, URL, and snippet.
"""
import os
import sys
from typing import Optional

import httpx
import typer
from rich.console import Console

app = typer.Typer(help="Search via SearXNG and print results.", add_completion=False)
err = Console(stderr=True)

DEFAULT_URL = "http://localhost:8080"


@app.command()
def main(
    query: str = typer.Argument(..., help="Search query string."),
    categories: Optional[str] = typer.Option(None, help="SearXNG category (e.g. 'news', 'science')."),
    time_range: Optional[str] = typer.Option(None, help="Time range filter: day, week, month, year."),
    results: int = typer.Option(10, help="Maximum number of results to return."),
    engines: Optional[str] = typer.Option(None, help="Comma-separated engine names to restrict to."),
) -> None:
    """
    Query a SearXNG instance and print results as a markdown list.
    Reads SEARXNG_URL from the environment (default: http://localhost:8080).
    """
    base_url = os.environ.get("SEARXNG_URL", DEFAULT_URL).rstrip("/")
    search_url = f"{base_url}/search"

    params: dict = {
        "q": query,
        "format": "json",
        "pageno": 1,
    }
    if categories:
        params["categories"] = categories
    if time_range:
        params["time_range"] = time_range
    if engines:
        params["engines"] = engines

    try:
        response = httpx.get(search_url, params=params, timeout=30)
        response.raise_for_status()
    except httpx.HTTPStatusError as exc:
        err.print(f"[red]HTTP error {exc.response.status_code}:[/red] {exc.response.text[:200]}")
        raise typer.Exit(1)
    except httpx.RequestError as exc:
        err.print(f"[red]Connection error:[/red] {exc}")
        err.print(f"Is SEARXNG_URL={base_url} correct and the instance running?")
        raise typer.Exit(1)

    data = response.json()
    items = data.get("results", [])

    if not items:
        err.print("[yellow]No results found.[/yellow]")
        raise typer.Exit(0)

    count = min(results, len(items))
    output_lines: list[str] = [f"## Search results for: {query}\n"]

    for i, item in enumerate(items[:count], 1):
        title = item.get("title", "(no title)").strip()
        url = item.get("url", "").strip()
        snippet = item.get("content", "").strip()
        output_lines.append(f"### {i}. [{title}]({url})")
        if snippet:
            output_lines.append(f"{snippet}\n")
        else:
            output_lines.append("")

    print("\n".join(output_lines))


if __name__ == "__main__":
    app()
