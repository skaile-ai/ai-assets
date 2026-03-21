# Example: Wrapping an API call as an agent-friendly CLI

## Trigger
> "Add a tool so the agent can look up a user by ID from the internal API."

## Input (agent request)
Build a CLI tool that fetches a user from `https://api.example.com/users/{id}`.  
It should optionally include the user's transaction history.

## Expected Output

### File: `scripts/get_user.py`

```python
# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "typer>=0.12.0",
#     "httpx>=0.27.0",
# ]
# ///

import json
import httpx
import typer

app = typer.Typer(
    help="Fetch user profile data from the internal API.",
    add_completion=False,
)

@app.command()
def main(
    user_id: int = typer.Argument(..., help="Numeric user ID to look up."),
    include_history: bool = typer.Option(
        False, "--include-history", help="Also fetch transaction history."
    ),
) -> None:
    """
    Retrieves a user profile by ID and prints JSON to stdout.
    Exits with code 1 on any error.
    """
    try:
        url = f"https://api.example.com/users/{user_id}"
        response = httpx.get(url, timeout=10)
        response.raise_for_status()
        data = response.json()
        if include_history:
            hist = httpx.get(f"{url}/history", timeout=10)
            hist.raise_for_status()
            data["history"] = hist.json()
        print(json.dumps(data, indent=2))
    except httpx.HTTPStatusError as exc:
        typer.secho(f"Error: HTTP {exc.response.status_code} for {exc.request.url}", err=True, fg=typer.colors.RED)
        raise typer.Exit(code=1)
    except Exception as exc:
        typer.secho(f"Error: {exc}", err=True, fg=typer.colors.RED)
        raise typer.Exit(code=1)

if __name__ == "__main__":
    app()
```

### Agent invocation
```bash
uv run scripts/get_user.py 42 --include-history
```

### stdout (success)
```json
{
  "id": 42,
  "name": "Alice",
  "role": "admin",
  "history": ["login", "purchase"]
}
```

### stderr (failure, e.g. 404)
```
Error: HTTP 404 for https://api.example.com/users/42
```
Exit code: `1`
