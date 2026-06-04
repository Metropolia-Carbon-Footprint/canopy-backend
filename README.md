# Canopy Backend

Kotlin Spring Boot backend for the Canopy carbon-footprint tracking application.

## Requirements

- Docker with Docker Compose e.g. Docker Desktop
- A Java SDK compatible with the project Gradle configuration
- IntelliJ IDEA or another Kotlin-compatible IDE

## Local setup

Copy the example environment file into a local `.env` file:

```bash
cp .env.example .env
```

On Windows PowerShell, use:

```powershell
Copy-Item .env.example .env
```

The default values are intended only for local development. The `.env` file must not be committed.

Start PostgreSQL:

```bash
docker compose up -d
```

## Run the backend

Open the project in the IDE and run the Spring Boot application.

The backend imports the project-root `.env` file when it starts. No Dockerfile is required for the backend during local development.

## Swagger UI

After the backend has started, open:

```text
http://localhost:8080/swagger-ui.html
```

## Run tests

macOS and Linux:

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew.bat test
```

Integration tests use Testcontainers, so Docker must be running.

## Stop PostgreSQL

```bash
docker compose down
```

To also delete the local PostgreSQL data volume and start with an empty database:

```bash
docker compose down -v
```
