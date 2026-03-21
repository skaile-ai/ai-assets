# Recipe: Directus Docker Setup

This recipe provides a standard, robust `docker-compose.yml` for local Directus development.

## 1. Create `docker-compose.yml`

Create a `docker-compose.yml` file in your Directus project root:

```yaml
services:
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
      # Public URL for the admin panel and API
      PUBLIC_URL: "http://localhost:8055"

volumes:
  postgres_data:
```

## 2. Key Configuration Points

- **Persistence**: Database data is stored in the `postgres_data` volume. Uploaded files are stored in the local `./uploads` directory.
- **Admin User**: The `ADMIN_EMAIL` and `ADMIN_PASSWORD` variables automatically create the initial admin user on first launch.
- **Healthchecks**: The `directus` service waits for the database and cache to be healthy before starting.
- **Extensions**: Mapping `./extensions` allows you to develop custom Directus extensions locally.

## 3. Launching the Environment

Run the following command to start Directus:

```bash
docker compose up -d
```

Access the admin panel at [http://localhost:8055/admin](http://localhost:8055/admin).
