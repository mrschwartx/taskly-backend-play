# Taskly Backend (Play Framework)

A task manager backend: sign up and sign in, manage your profile and avatar, and create, filter, sort and paginate to-do tasks. It is a REST API built with Play Framework 3 (Java) and PostgreSQL, organised as a modular monolith with Domain-Driven Design and Clean Architecture. It is a port of the Spring Boot version and keeps the same HTTP contract.

The browser client is a separate Taskly frontend that talks to this API.

**Stack:** Java 21 · Play Framework 3.0 (Pekko HTTP) · sbt · Guice · Play JDBC (HikariCP) · PostgreSQL 18 · Flyway · JWT (jjwt) · bcrypt (Bouncy Castle) · JUnit 4 · AssertJ · Docker · GitHub Actions

## Contents

- [Quick start](#quick-start)
- [Features](#features)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Database](#database)
- [API](#api)
- [Testing](#testing)
- [CI](#ci)
- [Limits](#limits)
- [References](#references)

## Quick start

Requires JDK 21, [sbt](https://www.scala-sbt.org/download/) and Docker.

```sh
cd taskly-docker
make docker/dev-up
cd ../taskly-backend
cp .env.example .env
openssl rand -base64 32
make dev
```

Paste the generated value into `AUTH_SECRET` in `.env`. The API runs at http://localhost:8080 and pgAdmin at http://localhost:8081 (login `admin@taskly.dev` / `admin`, server host `taskly-postgres`, user `dev_user`, password `dev_pass`). Flyway migrations run automatically on startup. Play does not read `.env` by itself: the `make` targets load it into the environment, and you can also export the variables yourself before running `sbt`.

| Goal               | Command                                                                        |
| ------------------ | ------------------------------------------------------------------------------ |
| Run in development | `make dev` (or `sbt run` with the variables exported)                          |
| Build the package  | `sbt stage`                                                                    |
| Run the package    | `make run` (or `target/universal/stage/bin/taskly-backend`)                    |
| Seed demo data     | `make seed` (or `bin/taskly-backend -main com.taskly.backend.config.SeedMain`) |
| Run with Docker    | `make docker/up` (from `taskly-docker`)                                        |
| Seed in Docker     | `make docker/seed` (from `taskly-docker`)                                      |
| Stop Docker        | `make docker/down` (from `taskly-docker`)                                      |

`make docker/up` creates `taskly-docker/.env` with a random database password and `AUTH_SECRET` on the first run, then starts PostgreSQL and the API and waits until both are healthy. `PORT` overrides the host port in the Docker setup. The container always listens on port 8080. Demo login after seeding: `johndoe123@mail.com` / `password123`.

## Features

- **Authentication**: sign up, sign in and sign out with a JWT stored in an HTTP-only cookie.
- **Users**: list users, update the profile, upload an avatar and delete the account.
- **Tasks**: create, view, update and delete tasks.
- **Listing**: filter by status, sort and paginate tasks.
- **Platform**: versioned Flyway migrations, request logging, CORS, health checks on `/` and `/actuator/health`.

## Configuration

Configuration lives in `conf/application.conf` and is overridden by environment variables.

| Variable                     | Default                 | Purpose                                                             |
| ---------------------------- | ----------------------- | ------------------------------------------------------------------- |
| `APP_HOST`                   | `0.0.0.0`               | Bind address                                                        |
| `APP_PORT`                   | `8080`                  | Port the API listens on                                             |
| `APP_LEVEL`                  | `development`           | Environment level, `production` hardens the cookie                  |
| `DATABASE_URL`               | required                | JDBC URL, for example `jdbc:postgresql://localhost:5432/taskly_dev` |
| `DATABASE_USER`              | required                | PostgreSQL user                                                     |
| `DATABASE_PASSWORD`          | required                | PostgreSQL password                                                 |
| `DATABASE_POOL_MAX`          | `10`                    | Maximum connections in the pool                                     |
| `DATABASE_TIMEOUT`           | `30000`                 | Connection timeout in milliseconds                                  |
| `AUTH_SECRET`                | required                | JWT secret, at least 32 characters                                  |
| `APPLICATION_SECRET`         | `AUTH_SECRET`           | Play application secret, only needed to use a separate value        |
| `COOKIE_DOMAIN`              | empty                   | Domain of the auth cookie in production                             |
| `BCRYPT_ROUNDS`              | `10`                    | bcrypt cost factor                                                  |
| `CLIENT_URL`                 | `http://localhost:3000` | Allowed CORS origin                                                 |
| `LOG_LEVEL`                  | `debug`                 | Log level of the `com.taskly` packages                              |
| `LOG_FILE`                   | `logs/taskly-api.log`   | Log file used by `conf/logback.xml`                                 |
| `PUBLIC_DIR`                 | `public`                | Directory that holds the avatars                                    |
| `SEED_ALLOW_PRODUCTION`      | `false`                 | Allows seeding when `APP_LEVEL` is `production`                     |
| `FLYWAY_BASELINE_ON_MIGRATE` | `false`                 | Baselines an existing database on the first start                   |
| `FLYWAY_BASELINE_VERSION`    | `2`                     | Version recorded by the baseline                                    |

Logging goes to the console and to `LOG_FILE`. To log to the console only, start the app with `-Dlogger.resource=logback-console.xml`. The Docker image does this already.

Docker only, in `taskly-docker/.env`:

| Variable      | Default  | Purpose                                |
| ------------- | -------- | -------------------------------------- |
| `PORT`        | `8080`   | Host port used by Docker               |
| `DB_NAME`     | `taskly` | PostgreSQL database name               |
| `DB_USER`     | `taskly` | PostgreSQL user                        |
| `DB_PASSWORD` | required | PostgreSQL password, generated by make |

The frontend must set `VITE_API_BASE_URL=http://localhost:8080/api/v1` and `VITE_SERVER_BASE_URL=http://localhost:8080`.

## Architecture

### Project layout

```
taskly-backend/
  app/com/taskly/backend/
    config/        typed configuration, Guice module, migrator, authentication action, seeder
    shared/        errors, ids, event bus, HTTP and persistence helpers
    users/         authentication and user management
    tasks/         task management
  conf/
    application.conf
    routes
    logback.xml
    db/migration/  Flyway migrations
  test/            unit and integration suites
taskly-docker/
  taskly-backend/Dockerfile
  docker-compose.local.yml
  docker-compose.dev.yml
  Makefile
.github/workflows/ci.yml
```

### Module layout

```
<module>/
  domain/          entities, value objects, repository and service contracts
  application/     use cases
  infrastructure/  JDBC, JWT, bcrypt and file system adapters
  presentation/    Play controllers
```

### Dependency rules

- Dependencies point inward: presentation and infrastructure depend on application, application depends on domain.
- Modules never import each other. They communicate through the in-process event bus, so deleting a user removes that user's tasks.
- Authentication is cross-cutting: the `@Authenticated` action composition in `config` verifies the cookie and exposes the user id to controllers through a request attribute.
- Domain exceptions are mapped to the `{ "status", "message" }` JSON shape by `ApiActionCreator` (outermost action) and `ApiErrorHandler` (client errors and unexpected failures).

### Spring to Play mapping

| Spring Boot                              | Play                                                |
| ---------------------------------------- | --------------------------------------------------- |
| `@RestController`, `@RequestMapping`     | `Controller` classes and `conf/routes`              |
| Servlet filter `JwtAuthenticationFilter` | `@Authenticated` action composition                 |
| `GlobalExceptionHandler`                 | `ApiActionCreator` and `ApiErrorHandler`            |
| `RequestLoggingFilter`                   | `RequestLoggingFilter` (Play `Filter`)              |
| Spring Security CORS                     | `play.filters.cors.CORSFilter`                      |
| Spring Data JPA                          | Play JDBC with prepared statements                  |
| `@ConfigurationProperties`               | `TasklyConfig` over Typesafe Config                 |
| `@Bean` wiring                           | Guice `AppModule`                                   |
| `SeedRunner` with `ApplicationRunner`    | `SeedMain` entry point                              |
| Actuator health                          | `/actuator/health` controller with a database check |

## Database

Schema changes live in `src/main/resources/db/migration` as Flyway `V<number>__<name>.sql` files. Flyway applies pending files in order on every startup and records them in `flyway_schema_history`.

| Table   | Notes                                                                                                                                               |
| ------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| `users` | `id` UUID, case-insensitive unique `email` and `username`, `password_hash`, `avatar_file`, timestamps                                               |
| `tasks` | `id` UUID, `owner_id` UUID (foreign key to `users`), `due` timestamptz, `status` and `priority` limited by `CHECK`, indexed on `(owner_id, status)` |

- Identifiers are UUIDs. API responses expose them as `_id` and `owner`.
- Listings are ordered by `created_at` and `id`, so ties on the creation time still give a stable order.
- `tasks.owner_id` references `users(id)` with `ON DELETE CASCADE`. The `user.deleted` event still removes a user's tasks.
- `status` accepts `todo`, `in_progress` and `done` (default `todo`). `priority` accepts `low`, `medium` and `high`.
- `due` is stored as a timestamp with time zone and returned as an ISO 8601 UTC string, for example `2026-12-08T10:00:00.000Z`.

## API

All routes are under `/api/v1`. Routes other than sign up and sign in require the `taskly_token` cookie.

| Method | Path                 | Purpose                                         |
| ------ | -------------------- | ----------------------------------------------- |
| POST   | `/auth/signup`       | Register and sign in                            |
| POST   | `/auth/signin`       | Sign in                                         |
| GET    | `/auth/signout`      | Sign out                                        |
| GET    | `/users`             | List users, filter with `username` and `email`  |
| GET    | `/users/{id}`        | Get a user                                      |
| PATCH  | `/users/update/{id}` | Update own profile                              |
| POST   | `/users/upload/{id}` | Upload own avatar (`avatar` field, 2 MB)        |
| DELETE | `/users/delete/{id}` | Delete own account                              |
| POST   | `/tasks/create`      | Create a task                                   |
| GET    | `/tasks/user/{id}`   | List own tasks with `status`, `orderBy`, `page` |
| GET    | `/tasks/{id}`        | Get a task                                      |
| PATCH  | `/tasks/{id}`        | Update a task                                   |
| DELETE | `/tasks/{id}`        | Delete a task                                   |

Errors are returned as `{ "status": <code>, "message": "<text>" }`. Unknown routes return `{ "message": "Not found" }` with status 404.

## Testing

| Layer       | Command                | What it proves                                          |
| ----------- | ---------------------- | ------------------------------------------------------- |
| Unit        | `sbt "testOnly *Test"` | Domain, use cases, JWT, bcrypt and file storage, no I/O |
| Integration | `sbt "testOnly *IT"`   | Full HTTP flows on a real PostgreSQL                    |
| All         | `make test`            | Both suites                                             |

Integration tests start the application with Play's `WithServer` and need a PostgreSQL server where the user can create databases. They read `TEST_DATABASE_URL` and default to the development database from `make docker/dev-up`. The suite creates its own temporary database, runs the migrations and drops it when the JVM exits.

```sh
TEST_DATABASE_URL=postgres://postgres:postgres@localhost:5432/postgres sbt test
```

## CI

`.github/workflows/commitlint.yml` lints commit messages on pull requests, configured in `.commitlintrc.json`.
`.github/workflows/ci.yml` runs these jobs:

1. **Build**: compiles the application and the tests, and stages the package with JDK 21.
2. **Tests**: unit and integration suites against a PostgreSQL 18 service.
3. **Docker**: builds the image, starts the Compose stack, then checks `/`, signs up a user and runs the seed.

## Limits

- The API needs a running PostgreSQL.
- Uploaded avatars are stored on local disk and served from `/images/users`. The Compose stack keeps them in the `taskly-avatars` volume.
- Events are delivered in-process, so the app runs as a single instance.

## References

- [Play Framework](https://www.playframework.com/documentation/latest/Home)
- [Play Java Action composition](https://www.playframework.com/documentation/latest/JavaActionsComposition)
- [Play JDBC](https://www.playframework.com/documentation/latest/AccessingAnSQLDatabase)
- [Play CORS filter](https://www.playframework.com/documentation/latest/CorsFilter)
- [Flyway](https://documentation.red-gate.com/flyway)
- [PostgreSQL](https://www.postgresql.org/docs/18/)
- [jjwt](https://github.com/jwtk/jjwt)
- [Bouncy Castle](https://www.bouncycastle.org/)
- [JUnit 4](https://junit.org/junit4/)
- [Docker multi-stage builds](https://docs.docker.com/build/building/multi-stage/)
