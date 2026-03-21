"""CLI for Context7 API access.

Commands:
- search: Find libraries by name
- docs: Fetch documentation for a specific library
- info: Show configuration status
"""

import os
import re
import sys
from typing import Optional

import httpx
import typer
from rich.console import Console
from rich.table import Table

app = typer.Typer(
    name="use-context7-api",
    help="CLI tool for accessing Context7 REST API directly",
    add_completion=True,
)
console = Console()

# Constants
CONTEXT7_SOURCE = "cli-tool"
SERVER_VERSION = "1.0.0"
DEFAULT_BASE_URL = "https://context7.com/api"
TIMEOUT = 10.0


def get_api_key() -> str:
    """Get API key from environment variable.

    Returns:
        API key string or empty string if not set.

    Note:
        Without API key, rate limits are stricter. Get one at https://context7.com/dashboard
    """
    api_key = os.getenv("CONTEXT7_API_KEY", "").strip()
    if not api_key:
        # Use stderr for warnings
        print(
            "[yellow]Warning:[/yellow] CONTEXT7_API_KEY not set. ",
            "Rate limits will be stricter. ",
            "Get an API key at: https://context7.com/dashboard",
            file=sys.stderr,
        )
    return api_key


def get_base_url() -> str:
    """Get base URL from environment or use default.

    Returns:
        Base URL without trailing slash.
    """
    base_url = os.getenv("CONTEXT7_API_URL", DEFAULT_BASE_URL).rstrip("/")
    return base_url


def get_headers(api_key: str) -> dict[str, str]:
    """Build request headers.

    Args:
        api_key: Context7 API key (may be empty)

    Returns:
        Dictionary of headers.
    """
    headers = {
        "X-Context7-Source": CONTEXT7_SOURCE,
        "X-Context7-Server-Version": SERVER_VERSION,
    }
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"
    return headers


def validate_library_id(library_id: str) -> bool:
    """Validate Context7 library ID format.

    Args:
        library_id: Library ID to validate

    Returns:
        True if format is valid (/org/project or /org/project/version)
    """
    pattern = r"^/[^/]+/[^/]+(?:/[^/]+)?$"
    return bool(re.match(pattern, library_id))


def parse_error_response(response: httpx.Response) -> str:
    """Parse error message from response.

    Args:
        response: HTTP response object

    Returns:
        User-friendly error message.
    """
    try:
        data = response.json()
        if isinstance(data, dict) and "message" in data:
            return str(data["message"])
    except Exception:
        pass

    status = response.status_code
    if status == 401:
        return "Invalid API key. Check your CONTEXT7_API_KEY. Get a key at https://context7.com/dashboard"
    elif status == 404:
        return "Library not found or invalid library ID"
    elif status == 429:
        return "Rate limit exceeded. Use API key for higher limits or wait before retrying"
    elif status >= 500:
        return f"Server error ({status}). Please try again later"
    else:
        return f"Request failed with status {status}"


def format_search_results(results: list[dict]) -> None:
    """Format and display search results in a table.

    Args:
        results: List of search result dictionaries
    """
    if not results:
        console.print("[yellow]No libraries found.[/yellow]")
        console.print("Try different search terms or check the library name.")
        return

    table = Table(title="Search Results", show_header=True, header_style="bold magenta")
    table.add_column("Library ID", style="cyan", no_wrap=True)
    table.add_column("Title", style="green")
    table.add_column("Description", style="white", max_width=60)
    table.add_column("Score", justify="right", style="yellow")
    table.add_column("Versions", style="blue")

    for result in results:
        lib_id = result.get("id", "N/A")
        title = result.get("title", "N/A")
        desc = result.get("description", "N/A")
        if len(desc) > 57:
            desc = desc[:57] + "..."

        score = result.get("benchmarkScore", result.get("trustScore", "N/A"))
        score_str = str(score) if score != "N/A" else "N/A"

        versions = result.get("versions", [])
        versions_str = ", ".join(versions[:3]) if versions else "N/A"
        if len(versions) > 3:
            versions_str += "..."

        table.add_row(lib_id, title, desc, score_str, versions_str)

    console.print(table)
    console.print(f"\nTotal results: {len(results)}")
    console.print("[dim]Use the Library ID with: use-context7-api docs <library-id>[/dim]")


@app.command()
def search(
    library_name: str = typer.Argument(..., help="Name of the library to search for"),
    query: Optional[str] = typer.Option(
        None,
        "--query",
        "-q",
        help="Additional context for relevance ranking",
    ),
) -> None:
    """Search for libraries by name.

    Examples:
        use-context7-api search react
        use-context7-api search express --query "How to create middleware"
        use-context7-api search "node-jsonwebtoken"
    """
    base_url = get_base_url()
    api_key = get_api_key()
    headers = get_headers(api_key)

    # Build URL
    url = f"{base_url}/v2/libs/search"
    params = {"libraryName": library_name}
    if query:
        params["query"] = query

    try:
        with httpx.Client(timeout=TIMEOUT) as client:
            response = client.get(url, headers=headers, params=params)

        if not response.is_success:
            error_msg = parse_error_response(response)
            console.print(f"[red]Error:[/red] {error_msg}")
            raise typer.Exit(code=1)

        data = response.json()
        results = data.get("results", [])

        if response.status_code == 200:
            format_search_results(results)

    except httpx.RequestError as e:
        console.print(f"[red]Network error:[/red] Failed to connect: {e}")
        console.print(f"URL: {url}")
        raise typer.Exit(code=1)
    except Exception as e:
        console.print(f"[red]Unexpected error:[/red] {e}")
        raise typer.Exit(code=1)


@app.command()
def docs(
    library_id: str = typer.Argument(..., help="Library ID (e.g., /org/project or /org/project/version)"),
    query: Optional[str] = typer.Option(
        None,
        "--query",
        "-q",
        help="Specific question about the library (default: 'How to use this library')",
    ),
) -> None:
    """Fetch documentation for a specific library.

    Examples:
        use-context7-api docs /facebook/react
        use-context7-api docs /auth0/node-jsonwebtoken --query "How to verify JWT"
        use-context7-api docs /expressjs/express/v4.18.0
    """
    # Validate library ID format
    if not validate_library_id(library_id):
        console.print("[red]Error:[/red] Invalid library ID format.")
        console.print("Library ID must be in the format: /org/project or /org/project/version")
        console.print("Example: /facebook/react or /auth0/node-jsonwebtoken/v9.0.0")
        console.print("Use 'use-context7-api search <name>' to find the correct ID")
        raise typer.Exit(code=1)

    base_url = get_base_url()
    api_key = get_api_key()
    headers = get_headers(api_key)

    # Build URL
    url = f"{base_url}/v2/context"
    params = {"libraryId": library_id}
    if query:
        params["query"] = query
    else:
        params["query"] = "How to use this library"

    try:
        with httpx.Client(timeout=TIMEOUT) as client:
            response = client.get(url, headers=headers, params=params)

        if not response.is_success:
            error_msg = parse_error_response(response)
            console.print(f"[red]Error:[/red] {error_msg}")
            raise typer.Exit(code=1)

        content = response.text

        if not content or content.strip() == "":
            console.print("[yellow]Warning:[/yellow] Empty documentation returned.")
            console.print("This might mean:")
            console.print("  - The library documentation is not yet finalized")
            console.print("  - The library ID is valid but content is being processed")
            console.print("  - The specific query has no matching content")
            raise typer.Exit(code=0)

        # Output markdown directly to stdout
        console.print(content)

    except httpx.RequestError as e:
        console.print(f"[red]Network error:[/red] Failed to connect: {e}")
        console.print(f"URL: {url}")
        raise typer.Exit(code=1)
    except Exception as e:
        console.print(f"[red]Unexpected error:[/red] {e}")
        raise typer.Exit(code=1)


@app.command()
def info() -> None:
    """Show configuration and status information.

    Displays:
    - API key configuration (present/missing)
    - Base URL
    - Current server version
    - Rate limit warnings
    """
    console.print("[bold]Context7 API CLI - Configuration[/bold]\n")

    api_key = get_api_key()
    base_url = get_base_url()

    console.print(f"[cyan]Base URL:[/cyan] {base_url}")

    if api_key:
        # Mask the key for display
        masked = api_key[:8] + "..." if len(api_key) > 8 else "***"
        console.print(f"[cyan]API Key:[/cyan] {masked} [green]✓ configured[/green]")
    else:
        console.print("[cyan]API Key:[/cyan] [red]✗ not set[/red]")
        console.print("  Set CONTEXT7_API_KEY environment variable for higher limits")

    console.print(f"[cyan]Client Version:[/cyan] {SERVER_VERSION}")
    console.print(f"[cyan]Timeout:[/cyan] {TIMEOUT} seconds")

    console.print("\n[yellow]Rate Limits:[/yellow]")
    if api_key:
        console.print("  With API key: Higher limits, private repos access")
    else:
        console.print("  Without API key: Stricter limits, public repos only")

    console.print("\n[dim]Get an API key at: https://context7.com/dashboard[/dim]")


@app.callback(invoke_without_command=True)
def main(
    ctx: typer.Context,
) -> None:
    """Context7 API CLI - Access Context7 documentation without MCP.

    Use this tool to search for libraries and fetch up-to-date documentation
    directly from the command line.
    """
    if ctx.invoked_subcommand is None:
        console.print(ctx.get_help())


if __name__ == "__main__":
    app()
