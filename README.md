# CollabFlow

A backend for collaborative task and project management. Teams run projects and sprints, work on tasks together, and every change is tracked.

Built with Java 21, Spring Boot 4, PostgreSQL, Flyway, and Spring Security.

## Run locally

You need **JDK 21** and **Docker**.

```bash
docker compose up -d        # start PostgreSQL
./mvnw spring-boot:run      # start the app on http://localhost:8080
```

- API docs (Swagger UI): http://localhost:8080/swagger-ui/index.html
- Health check: http://localhost:8080/actuator/health

If port 8080 is already taken, choose another one:

```bash
SERVER_PORT=8081 ./mvnw spring-boot:run                 # bash
$env:SERVER_PORT=8081; ./mvnw spring-boot:run           # PowerShell
```

Stop the database with `docker compose down` (add `-v` to also delete its data).

## Run the tests

```bash
./mvnw test
```

Tests start their own throwaway PostgreSQL in Docker (Testcontainers), so Docker must be running.

## Configuration

Settings come from environment variables. The defaults work with the Docker Compose file.

| Variable | Default |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `collabflow` |
| `DB_USERNAME` | `collabflow` |
| `DB_PASSWORD` | `collabflow` |
| `SERVER_PORT` | `8080` |
