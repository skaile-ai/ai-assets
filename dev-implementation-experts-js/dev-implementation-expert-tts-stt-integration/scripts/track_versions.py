# /// script
# requires-python = ">=3.11"
# dependencies = ["rich", "requests"]
# ///

import json
import os
import requests
from rich.console import Console

console = Console()
VERSIONS_FILE = os.path.join(os.path.dirname(__file__), "../references/versions.json")

PACKAGES = {
    "@deepgram/sdk": "https://registry.npmjs.org/@deepgram/sdk/latest",
    "elevenlabs": "https://registry.npmjs.org/elevenlabs/latest",
}

def main():
    console.print("[bold cyan]Checking latest TTS/STT library versions...[/bold cyan]")
    with open(VERSIONS_FILE) as f:
        versions = json.load(f)

    for pkg, url in PACKAGES.items():
        try:
            res = requests.get(url, timeout=5)
            latest = res.json().get("version", "unknown")
            console.print(f"  [green]{pkg}[/green]: {latest}")
        except Exception as e:
            console.print(f"  [yellow]{pkg}[/yellow]: error ({e})")

    console.print("\n[dim]Update references/versions.json manually after reviewing.[/dim]")

if __name__ == "__main__":
    main()
