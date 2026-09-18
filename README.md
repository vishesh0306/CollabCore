# CollabFlow

[![CI](https://github.com/vishesh0306/CollabCore/actions/workflows/ci.yml/badge.svg)](https://github.com/vishesh0306/CollabCore/actions/workflows/ci.yml)

A backend for collaborative task and project management in a company with many teams. Each team works in its own private workspace: it runs several projects, plans work in sprints, and collaborates on tasks.

Built with Java 21, Spring Boot 4, PostgreSQL, Flyway, and Spring Security (JWT), as a modular monolith.

## What it does

- **Accounts**: sign up, log in with a JWT, change email or password. A single admin account is created on the first start.
- **Teams**: the admin creates teams, and managers add people as managers or members. A team is a private workspace: anyone outside it gets `404` for everything in it.
- **Projects**: each team runs several projects, each with a short code that is unique across the company (e.g. `PAY`). Completed projects are read-only.
- **Tasks**: keys like `PAY-12`, a status, an expected date, and any number of assignees. Members change only their own tasks; managers change any. The task list supports filters, sorting and pages.
- **Sprints**: each team runs one sprint at a time, holding tasks from any of its projects, with progress shown per project. Completing a sprint sends unfinished tasks back to their project's backlog.
- **Comments** on tasks.

## Design highlights

- **Modular monolith.** Each module (`identity`, `team`, `project`, `sprint`, `task`, `comment`) only depends on the ones below it. When a lower module has to trigger work in a higher one, it publishes an event instead of calling it (e.g. completing a sprint, removing a team member).
- **Rules the database enforces too**: unique project codes, one active sprint per team, a single admin. They hold even when two requests arrive at the same moment.
- **Safe sequential task numbers** per project, using a row lock while a number is taken.
- **No N+1 queries** on lists, thanks to fetch joins and batch fetching.
- **One error format** for every response (RFC 9457 Problem Details).
- **No secrets in git**: local settings live in a git-ignored file, and CI uses test-only values.

## API overview

All paths start with `/api/v1`. Except for sign-up and login, every request needs `Authorization: Bearer <token>`. The full, interactive documentation is in Swagger UI (see below).

| Area | Endpoints |
|---|---|
| Account | `POST /auth/register`, `POST /auth/login`, `GET /me`, `PUT /me/email`, `PUT /me/password` |
| Teams | `POST /teams` (admin), `GET /teams`, `GET /teams/{id}`, `PUT /teams/{id}` (admin) |
| Team members | `POST /teams/{id}/members`, `PUT /teams/{id}/members/{userId}`, `DELETE /teams/{id}/members/{userId}` |
| Projects | `POST /teams/{id}/projects`, `GET /teams/{id}/projects`, `GET /projects/{id}`, `PUT /projects/{id}`, `POST /projects/{id}/complete`, `POST /projects/{id}/reopen`, `GET /projects/{id}/backlog` |
| Tasks | `POST /projects/{id}/tasks`, `GET /teams/{id}/tasks`, `GET /tasks/{key}`, `PUT /tasks/{key}`, `PUT /tasks/{key}/status`, `PUT /tasks/{key}/assignees`, `PUT /tasks/{key}/sprint`, `DELETE /tasks/{key}` |
| Sprints | `POST /teams/{id}/sprints`, `GET /teams/{id}/sprints`, `GET /sprints/{id}`, `PUT /sprints/{id}`, `POST /sprints/{id}/start`, `POST /sprints/{id}/complete`, `GET /sprints/{id}/tasks` |
| Comments | `POST /tasks/{key}/comments`, `GET /tasks/{key}/comments`, `PUT /comments/{id}`, `DELETE /comments/{id}` |

## Project layout

```
src/main/java/com/collabflow/
├── identity/   users, sign-up, login (JWT), the admin account
├── team/       teams, members and roles, access rules (TeamAccess)
├── project/    projects
├── sprint/     sprints
├── task/       tasks, backlogs, the sprint view
├── comment/    comments on tasks
└── shared/     error handling, paging, the current user
src/main/resources/db/migration/   the database schema, as Flyway migrations
```

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
4. Log in as the admin to create a team (`POST /api/v1/teams`) with your user as its manager.

## Run the tests

```bash
./mvnw test
```

Tests start their own throwaway PostgreSQL in Docker (Testcontainers), so Docker must be running. [`TeamSprintScenarioTest`](src/test/java/com/collabflow/TeamSprintScenarioTest.java) walks through one full story from team setup to a completed sprint. GitHub Actions runs every test on each push.

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
