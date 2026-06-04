# Canopy Backend

Kotlin Spring Boot backend for the Canopy carbon-footprint tracking application.

## Requirements

- Docker with Docker Compose e.g. Docker Desktop
- Java 17 SDK
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

## Create a local admin user

Newly registered users have the `USER` role by default. To create an admin account for local development:

1. Start PostgreSQL and the backend.
2. Register a new user through Swagger UI.
3. Open a PostgreSQL shell inside the Docker container:

```bash
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```
> Note: Run this command from the project root so Docker Compose can load the `.env` file.

4. Replace the example email address and promote the registered user:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'admin@example.com';
```

5. Confirm the updated role:

```sql
SELECT user_id, email, role
FROM users
WHERE email = 'admin@example.com';
```

6. Exit the PostgreSQL shell:

```sql
\q
```

Log in again through Swagger UI to receive a new access token containing the `ADMIN` role.

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
