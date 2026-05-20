# Money Manager Web

Money Manager Web is a Spring Boot web application for managing personal finances from a browser. It provides registration and login, dashboard summaries, transaction tracking, budgets, goals, notes, and PostgreSQL-backed persistence.

This repository is web-only. It does not include a desktop GUI, JavaFX, Swing, or local desktop launcher code.

## Tech Stack

| Area | Technology |
| --- | --- |
| Runtime | Java 21 |
| Backend | Spring Boot 3.3.2 |
| Views | Thymeleaf |
| Database | PostgreSQL |
| Build | Maven Wrapper |
| Deployment | Docker / Render |

## Project Structure

```text
money-manager-web
|-- pom.xml
|-- Dockerfile
|-- render.yaml
|-- run.bat
|-- start.bat
|-- src
|   |-- main
|   |   |-- java
|   |   |   |-- com.moneytracker
|   |   |   |   |-- MoneytrackerApplication.java
|   |   |   |   |-- web
|   |   |   |-- com.moneymanager
|   |   |       |-- model
|   |   |       |-- repository
|   |   |       |-- service
|   |   |-- resources
|   |       |-- application.properties
|   |       |-- db.properties
|   |       |-- schema.sql
|   |       |-- static
|   |       |-- templates
|   |-- test
```

## Local Requirements

- Java JDK 21
- PostgreSQL 16 or newer
- Maven is optional because the Maven Wrapper is included

## Database Configuration

The app reads database settings from environment variables first, then falls back to `src/main/resources/db.properties`.

For local development:

```properties
db.url=jdbc:postgresql://localhost:5432/moneymanager
db.username=postgres
db.password=123456
```

For hosted databases such as Supabase, set environment variables on the hosting platform:

```text
DATABASE_URL=postgres://user:password@host:5432/database
DATABASE_USERNAME=user
DATABASE_PASSWORD=password
```

If `DATABASE_URL` already includes the username and password, the separate username and password variables are optional.

## Run Locally

On Windows:

```bat
run.bat
```

Or run the Maven Wrapper directly:

```bat
mvnw.cmd spring-boot:run
```

After startup, open:

```text
http://localhost:8080
```

## Build

```bat
mvnw.cmd clean package
```

Run the packaged jar:

```bat
java -jar target\moneytracker-0.0.1-SNAPSHOT.jar
```

## Test

```bat
mvnw.cmd test
```

## Deploy

This repo includes a `Dockerfile` and `render.yaml` for Render deployment.

1. Push this repository to GitHub.
2. Create a new Render Blueprint or Web Service.
3. Use the included Dockerfile.
4. Set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` in Render environment variables.
5. Deploy and open the generated Render URL.

The app uses `PORT` when provided by the host, falling back to `8080` locally.
