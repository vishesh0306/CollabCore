# CollabFlow

A backend for collaborative task and project management. Teams run projects and sprints, work on tasks together, and every change is tracked.

Built with Java 21, Spring Boot 4, PostgreSQL, Flyway, and Spring Security (JWT).

## Run locally

You need **JDK 21** and **Docker**.

**1. Create your local settings** (first time only):

```bash
cp config/application.properties.example config/application.properties
```

Fill in `collabflow.jwt.secret` (at least 32 characters; e.g. `openssl rand -base64 48`) and the admin email and password. This file is git-ignored, and Spring Boot reads it automatically.

**2. Start the database and the app:**

```bash
docker compose up -d        # start PostgreSQL
./mvnw spring-boot:run      # start the app on http://localhost:8080
```

On the first start, the admin account is created from your settings.

- API docs (Swagger UI): http://localhost:8080/swagger-ui/index.html
- Health check: http://localhost:8080/actuator/health

Port 8080 already taken? Add `server.port=8081` to `config/application.properties`.

Stop the database with `docker compose down` (add `-v` to also delete its data).

## Try the API in Swagger UI

1. `POST /api/v1/auth/register` to create a user, then `POST /api/v1/auth/login`.
2. Copy the `accessToken` from the response.
3. Click **Authorize**, paste the token, and call the other endpoints, e.g. `GET /api/v1/me`.

## Run the tests

```bash
./mvnw test
```

Tests start their own throwaway PostgreSQL in Docker (Testcontainers), so Docker must be running.

## Configuration

Settings can go in `config/application.properties` or be set as environment variables.

| Setting | Environment variable | Default |
|---|---|---|
| `collabflow.jwt.secret` | `COLLABFLOW_JWT_SECRET` | none (required) |
| `collabflow.jwt.expiry` | `COLLABFLOW_JWT_EXPIRY` | `24h` |
| `collabflow.admin.email` | `COLLABFLOW_ADMIN_EMAIL` | none (required until the admin exists) |
| `collabflow.admin.password` | `COLLABFLOW_ADMIN_PASSWORD` | none (required until the admin exists) |
| Database host / port / name | `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `collabflow` |
| Database user / password | `DB_USERNAME` / `DB_PASSWORD` | `collabflow` / `collabflow` |
| `server.port` | `SERVER_PORT` | `8080` |
