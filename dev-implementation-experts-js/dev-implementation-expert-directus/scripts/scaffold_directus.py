# /// script
# dependencies = [
#   "typer",
#   "rich",
# ]
# ///

import typer
from rich.console import Console
from pathlib import Path

app = typer.Typer()
console = Console()

DOCKER_COMPOSE_TEMPLATE = """services:
  database:
    image: postgres:15
    volumes:
      - postgres_data:/var/lib/postgresql/data
    environment:
      POSTGRES_USER: directus
      POSTGRES_PASSWORD: directus_password
      POSTGRES_DB: directus
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U directus"]
      interval: 5s
      timeout: 5s
      retries: 5

  cache:
    image: redis:6
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5

  directus:
    image: directus/directus:11.1
    ports:
      - "8055:8055"
    volumes:
      - ./uploads:/directus/uploads
      - ./extensions:/directus/extensions
    depends_on:
      database:
        condition: service_healthy
      cache:
        condition: service_healthy
    environment:
      KEY: "replace-with-random-key"
      SECRET: "replace-with-random-secret"
      DB_CLIENT: "pg"
      DB_HOST: "database"
      DB_PORT: "5432"
      DB_DATABASE: "directus"
      DB_USER: "directus"
      DB_PASSWORD: "directus_password"
      CACHE_ENABLED: "true"
      CACHE_STORE: "redis"
      CACHE_REDIS: "redis://cache:6379"
      ADMIN_EMAIL: "admin@example.com"
      ADMIN_PASSWORD: "admin"
      PUBLIC_URL: "http://localhost:8055"

volumes:
  postgres_data:
"""

ENV_TEMPLATE = """# Directus
DIRECTUS_URL="http://localhost:8055"
DIRECTUS_TOKEN="admin_token_here"

# Admin Initial
ADMIN_EMAIL="admin@example.com"
ADMIN_PASSWORD="admin"
"""

@app.command()
def scaffold(project_dir: Path):
    """
    Scaffold a new Directus project directory with docker-compose and .env
    """
    if project_dir.exists():
        console.print(f"[red]Error:[/red] Directory {project_dir} already exists.")
        raise typer.Exit(1)

    project_dir.mkdir(parents=True)
    (project_dir / "uploads").mkdir()
    (project_dir / "extensions").mkdir()

    with open(project_dir / "docker-compose.yml", "w") as f:
        f.write(DOCKER_COMPOSE_TEMPLATE)
    
    with open(project_dir / ".env", "w") as f:
        f.write(ENV_TEMPLATE)

    console.print(f"[green]Successfully scaffolded Directus project in:[/green] {project_dir}")
    console.print("\\nNext steps:")
    console.print(f"1. cd {project_dir}")
    console.print("2. docker compose up -d")

if __name__ == "__main__":
    app()
