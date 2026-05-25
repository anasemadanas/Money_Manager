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

The app reads database settings from environment variables first, then falls back to `src/main/resources/db.properties` for local development. On Render, or with the `prod` Spring profile enabled, `DATABASE_URL` is required so a deployed service cannot accidentally try local PostgreSQL.

For local development:

```properties
db.url=jdbc:postgresql://localhost:5432/moneymanager
db.username=postgres
db.password=123456
```

For a Render Postgres database used by a Render web service, copy the **Internal Database URL** from the database Connections page. The internal URL keeps database traffic on Render's private network and includes the database username and password.

Set this environment variable on the Render web service:

```text
DATABASE_URL=postgresql://DB_USER:DB_PASSWORD@INTERNAL_HOST/DB_NAME
```

The app reads credentials embedded in Render's generated database URL, which take precedence over separate credential variables. It also supports `DATABASE_USERNAME` and `DATABASE_PASSWORD` when the URL does not contain credentials. Do not store the generated URL in source control because it contains the database password.

Supabase remains supported: use its Session pooler URL for an IPv4 Render service and set `DATABASE_USERNAME` and `DATABASE_PASSWORD` separately, as needed. Supabase hosts automatically receive `sslmode=require` unless the URL already specifies an SSL mode.

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

To run the web app locally while connected to hosted PostgreSQL:

```powershell
$env:DATABASE_URL='postgresql://DB_USER:DB_PASSWORD@EXTERNAL_HOST/DB_NAME'
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
2. Create a new Render Blueprint to apply `render.yaml`, or configure an existing Web Service manually.
3. Use the included Dockerfile.
4. Set `DATABASE_URL` to your Render Postgres **Internal Database URL** on the web service Environment page.
5. For an existing service, add the variable and choose **Save and deploy**. Render prompts for `sync: false` Blueprint secrets only during initial Blueprint creation; later Blueprint syncs do not populate them. See the [Render Blueprint reference](https://render.com/docs/blueprint-spec#prompting-for-secret-values).
6. Deploy and confirm startup logs show the Render internal database host rather than `localhost:5432`.

The app uses `PORT` when provided by the host, falling back to `8080` locally.

## Android App

The companion Android WebView application is maintained as the sibling project [`../android-app`](../android-app/README.md). It opens the deployed Money Manager website at:

```text
https://money-manager-t4ed.onrender.com/
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
