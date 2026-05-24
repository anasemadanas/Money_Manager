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
|-- run-local.bat
|-- run-web.bat
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

For Supabase, copy the **Session pooler** connection details from the project's Connect panel. Session mode on port `5432` is suitable for this long-running Spring Boot server and also works on IPv4 hosts such as Render.

Set these environment variables locally for testing, or on the hosting platform for deployment:

```text
DATABASE_URL=postgres://postgres.PROJECT_REF@aws-0-REGION.pooler.supabase.com:5432/postgres
DATABASE_USERNAME=postgres.PROJECT_REF
DATABASE_PASSWORD=your_database_password
```

The app adds `sslmode=require` automatically for Supabase hosts unless an explicit SSL mode is already in the URL. The username and password can instead be embedded in `DATABASE_URL`, but keep secrets in environment variables and URL-encode special characters if you do that.

This application connects to Supabase through server-side PostgreSQL/JDBC. It does not expose a Supabase public client key in the browser or use the Data API.

## Run Locally

Use a local PostgreSQL database on Windows:

```bat
run-local.bat
```

`run.bat` remains an alias for `run-local.bat`. Override the local defaults when needed:

```powershell
$env:LOCAL_DATABASE_URL='jdbc:postgresql://localhost:5432/moneymanager'
$env:LOCAL_DATABASE_USERNAME='postgres'
$env:LOCAL_DATABASE_PASSWORD='your_local_password'
.\run-local.bat
```

To run the web app locally while connected to online Supabase:

```powershell
$env:DATABASE_URL='postgres://postgres.PROJECT_REF@aws-0-REGION.pooler.supabase.com:5432/postgres'
$env:DATABASE_USERNAME='postgres.PROJECT_REF'
$env:DATABASE_PASSWORD='your_database_password'
.\run-web.bat
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
4. Set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` in Render environment variables using the Supabase Session pooler details shown above.
5. Deploy and open the generated Render URL.

The app uses `PORT` when provided by the host, falling back to `8080` locally.

## Android App

The companion Android WebView application is maintained as the sibling project [`../android-app`](../android-app/README.md). It opens the deployed Money Manager website at:

```text
https://money-manager-t4ed.onrender.com
```

It keeps account and finance data on the Spring Boot server, handles Android back navigation within the site, opens external URLs outside the app, and displays a retry screen when the deployment cannot be reached.

From the repository root, build a debug APK after installing Android SDK API 35 and JDK 17:

```powershell
cd android-app
.\gradlew.bat assembleDebug
```

The generated APK is placed at:

```text
android-app\app\build\outputs\apk\debug\app-debug.apk
```
