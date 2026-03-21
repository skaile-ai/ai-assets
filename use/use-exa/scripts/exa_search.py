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
Exa web search CLI via Exa's public MCP endpoint.
Uses JSON-RPC 2.0 over HTTPS — no API key required for basic tools.
Optionally uses EXA_API_KEY for higher rate limits.

Based on the architecture of oh-my-pi (github.com/can1357/oh-my-pi).
"""
import json
import os
import sys
import uuid
from typing import Optional

import requests
import typer
from rich.console import Console

app = typer.Typer(
    help="Search the web via Exa's public MCP endpoint (no API key required).",
    invoke_without_command=True,
    no_args_is_help=True,
)
console = Console()
err = Console(stderr=True)

EXA_MCP_BASE = "https://mcp.exa.ai/mcp"


# ---------------------------------------------------------------------------
# MCP JSON-RPC helpers
# ---------------------------------------------------------------------------

def _build_url(tool_name: str, *, for_call: bool = False) -> str:
    """Build the MCP endpoint URL with optional API key."""
    params: dict[str, str] = {}

    api_key = os.environ.get("EXA_API_KEY")
    if api_key:
        params["exaApiKey"] = api_key

    # tools= for calls, toolNames= for listing
    key = "tools" if for_call else "toolNames"
    params[key] = tool_name

    qs = "&".join(f"{k}={v}" for k, v in params.items())
    return f"{EXA_MCP_BASE}?{qs}" if qs else EXA_MCP_BASE


def _jsonrpc_request(
    url: str,
    method: str,
    params: dict | None = None,
    timeout: int = 30,
) -> dict:
    """Send a JSON-RPC 2.0 request to the Exa MCP endpoint."""
    body = {
        "jsonrpc": "2.0",
        "id": uuid.uuid4().hex[:12],
        "method": method,
        "params": params or {},
    }
    resp = requests.post(
        url,
        json=body,
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json, text/event-stream",
        },
        timeout=timeout,
    )
    resp.raise_for_status()

    # Handle SSE (Server-Sent Events) response
    content_type = resp.headers.get("content-type", "")
    if "text/event-stream" in content_type:
        return _parse_sse(resp.text)

    return resp.json()


def _parse_sse(text: str) -> dict:
    """Extract the last JSON-RPC result from an SSE stream."""
    last_data = None
    for line in text.splitlines():
        if line.startswith("data: "):
            last_data = line[6:]
    if last_data:
        return json.loads(last_data)
    raise ValueError("No data found in SSE stream")


def _extract_payload(rpc_response: dict) -> list | dict | str:
    """Normalize the MCP response payload.

    Exa returns results in varying shapes:
      - result.content[] with text items containing JSON
      - result.structuredContent / result.data / result.result
      - direct payload in result
    """
    if "error" in rpc_response and rpc_response["error"]:
        err_msg = rpc_response["error"]
        if isinstance(err_msg, dict):
            err_msg = err_msg.get("message", str(err_msg))
        raise RuntimeError(f"MCP error: {err_msg}")

    result = rpc_response.get("result", {})

    # content[] array — most common shape
    if isinstance(result, dict) and "content" in result:
        content = result["content"]
        if isinstance(content, list):
            texts = []
            for item in content:
                if isinstance(item, dict) and item.get("type") == "text":
                    text = item.get("text", "")
                    # Try to parse embedded JSON
                    try:
                        parsed = json.loads(text)
                        return parsed
                    except (json.JSONDecodeError, TypeError):
                        texts.append(text)
            return "\n".join(texts) if texts else content

    # Nested result shapes
    for key in ("structuredContent", "data", "result"):
        if isinstance(result, dict) and key in result:
            return result[key]

    return result


# ---------------------------------------------------------------------------
# Commands
# ---------------------------------------------------------------------------

@app.command()
def search(
    query: str = typer.Argument(..., help="The search query."),
    num_results: int = typer.Option(10, "--num-results", "-n", help="Number of results."),
    search_type: str = typer.Option(
        "auto", "--type", "-t", help="Search type: keyword, neural, or auto."
    ),
    start_date: Optional[str] = typer.Option(
        None, "--start-date", help="Start date filter (YYYY-MM-DD)."
    ),
    end_date: Optional[str] = typer.Option(
        None, "--end-date", help="End date filter (YYYY-MM-DD)."
    ),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON."),
):
    """Web search via Exa MCP (no API key required)."""
    tool_name = "web_search_exa"
    args: dict = {
        "query": query,
        "numResults": num_results,
    }
    if search_type != "auto":
        args["type"] = search_type
    if start_date:
        args["startPublishedDate"] = start_date
    if end_date:
        args["endPublishedDate"] = end_date

    url = _build_url(tool_name, for_call=True)

    try:
        rpc_resp = _jsonrpc_request(url, "tools/call", {
            "name": tool_name,
            "arguments": args,
        })
        payload = _extract_payload(rpc_resp)
    except Exception as e:
        err.print(f"[bold red]Error:[/bold red] {e}")
        raise typer.Exit(code=1)

    if raw:
        print(json.dumps(payload, indent=2, ensure_ascii=False))
        return

    _render_search_results(query, payload)


@app.command()
def code(
    query: str = typer.Argument(..., help="The code search query."),
    num_results: int = typer.Option(10, "--num-results", "-n", help="Number of results."),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON."),
):
    """Code-focused search via Exa MCP (no API key required)."""
    tool_name = "get_code_context_exa"
    args: dict = {
        "query": query,
        "numResults": num_results,
    }

    url = _build_url(tool_name, for_call=True)

    try:
        rpc_resp = _jsonrpc_request(url, "tools/call", {
            "name": tool_name,
            "arguments": args,
        })
        payload = _extract_payload(rpc_resp)
    except Exception as e:
        err.print(f"[bold red]Error:[/bold red] {e}")
        raise typer.Exit(code=1)

    if raw:
        print(json.dumps(payload, indent=2, ensure_ascii=False))
        return

    _render_search_results(query, payload, heading="Code Search")


@app.command()
def tools():
    """List available MCP tools from the Exa endpoint."""
    # Query with a broad set of known tool names
    known_tools = "web_search_exa,get_code_context_exa,crawling_exa"
    url = _build_url(known_tools, for_call=False)

    try:
        rpc_resp = _jsonrpc_request(url, "tools/list")
    except Exception as e:
        err.print(f"[bold red]Error:[/bold red] {e}")
        raise typer.Exit(code=1)

    result = rpc_resp.get("result", {})
    tool_list = result.get("tools", []) if isinstance(result, dict) else []

    if not tool_list:
        print("No tools returned from the endpoint.")
        return

    print("# Available Exa MCP Tools\n")
    for tool in tool_list:
        name = tool.get("name", "unknown")
        desc = tool.get("description", "No description.")
        print(f"## {name}")
        print(f"{desc}\n")

        # Show input schema if available
        schema = tool.get("inputSchema", {})
        props = schema.get("properties", {})
        if props:
            print("**Parameters:**")
            for pname, pinfo in props.items():
                ptype = pinfo.get("type", "any")
                pdesc = pinfo.get("description", "")
                required = pname in schema.get("required", [])
                req_tag = " (required)" if required else ""
                print(f"- `{pname}` ({ptype}{req_tag}): {pdesc}")
            print()


# ---------------------------------------------------------------------------
# Rendering helpers
# ---------------------------------------------------------------------------

def _render_search_results(
    query: str,
    payload: list | dict | str,
    heading: str = "Search",
) -> None:
    """Format search results as clean markdown."""
    print(f"# Exa {heading} Results\n")
    print(f"**Query:** `{query}`\n")
    print("---\n")

    # Payload may be a list of results or a dict with a results key
    items: list = []
    if isinstance(payload, list):
        items = payload
    elif isinstance(payload, dict):
        items = payload.get("results", payload.get("data", []))
        if isinstance(items, dict):
            items = [items]
    elif isinstance(payload, str):
        # Plain text response
        print(payload)
        return

    if not items:
        print("No results found.")
        return

    print(f"**Results:** {len(items)}\n")

    for i, item in enumerate(items, 1):
        if isinstance(item, str):
            print(f"## {i}. Result")
            print(item)
            print()
            continue

        title = item.get("title", "(No title)").strip()
        url = item.get("url", "")
        text = item.get("text", item.get("content", item.get("snippet", ""))).strip()
        score = item.get("score", "")
        pub_date = item.get("publishedDate", "")

        print(f"## {i}. {title}")
        if url:
            print(f"**URL:** {url}")
        if score:
            print(f"**Score:** {score}")
        if pub_date:
            print(f"**Published:** {pub_date}")
        if text:
            # Truncate very long text for readability
            if len(text) > 1000:
                text = text[:1000] + "..."
            print(f"\n{text}")
        print()


if __name__ == "__main__":
    app()
