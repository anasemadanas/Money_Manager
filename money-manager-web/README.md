# 🌐 Money Manager Web App
> Centralized finance management. Connected, modern, and accessible anywhere.

A robust, responsive personal financial management web application built in **Java 26** using **Spring Boot 4.0.6**, **Thymeleaf** templates for dynamic HTML rendering, **PostgreSQL** for relational centralized data storage, and stylized with premium custom styling.

[![GitHub Repo](https://img.shields.io/badge/GitHub-Money__Manager-black?style=for-the-badge&logo=github)](https://github.com/anasemadanas/Money_Manager)
[![License](https://img.shields.io/github/license/anasemadanas/Money_Manager?style=for-the-badge)](https://github.com/anasemadanas/Money_Manager/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/Java-26-blue?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-green?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-HTML5-teal?style=for-the-badge&logo=thymeleaf)](https://www.thymeleaf.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Central-blue?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)

---

## 📑 Table of Contents

- [🧾 Introduction](#-introduction)
- [✨ Web-Specific Features](#-web-specific-features)
- [🧱 Architecture Details](#-architecture-details)
- [📂 Project Structure](#-project-structure)
- [📦 Requirements](#-requirements)
- [⚙️ Database Configuration](#️-database-configuration)
- [▶️ Run the App](#️-run-the-app)
- [🧪 Running Unit Tests](#-running-unit-tests)
- [🔮 Future Enhancements](#-future-enhancements)
- [📝 License](#-license)
- [🔗 Contact](#-contact)

---

## 🧾 Introduction

**Money Manager Web** provides a centralized platform for managing expenses, income, budgets, goals, and notes. Built on top of Spring Boot's lightweight framework, it uses standard REST-style routing, secure Thymeleaf-driven views, and a dedicated database connection to a PostgreSQL instance.

This web version lets users access their financial records from multiple browsers and platforms synchronously, serving as the central hub of the Money Manager Suite.

Built using premium enterprise engineering:

| Technology | Purpose |
| :--- | :--- |
| **Java 26** | Cutting edge JVM capabilities, records, and pattern matching |
| **Spring Boot 4.0.6** | Centralized web controller mapping and dependency injection container |
| **Thymeleaf** | Dynamic HTML view rendering with semantic tags |
| **PostgreSQL** | Enterprise relational storage for heavy user transaction data |
| **Lombok** | Boilerplate-free models, repositories, and DTO builders |
| **Maven Wrapper** | Fully contained builds without pre-installed local build tools |

---

## ✨ Web-Specific Features

- 🌐 **Responsive Web Layout**: Fully styled HTML5 tables and dashboards that seamlessly fit desktop screens, tablets, and phones.
- 🔑 **Centralized Cookie Sessions**: Cookie-backed HTTP Sessions handling registration, login, and access restriction on secure pages.
- 📊 **Dynamic HTML Dashboards**: Visual financial tracking summaries rendered instantly using modern CSS progress bars, widgets, and charts.
- 💸 **Interactive Transactions Grid**: Filter, order, and CRUD transactions. Category settings default to `Food`, `Bills`, `Groceries`, `Entertainment`, `Travel`, and `Other`.
- 📅 **Dynamic Budget Limits**: Real-time checking logic during transaction creation. Automatically alerts you if category budgets or monthly income thresholds are breached.
- 🛡️ **PostgreSQL Integration**: Complete connection pooling, transactional robustness, and optimized indexes on user tables.

---

## 🧱 Architecture Details

The Web Application matches the overall 3-tier standard of the suite while binding the controller routes directly to browser requests:

```
    👤 USER BROWSER
         │ (HTTP GET/POST)
         ▼
┌─────────────────────────────────────────┐
│            PRESENTATION LAYER           │
│  - WebAuthController.java               │  ← Maps routes to Thymeleaf pages.
│  - WebDashboardController.java          │
│  - WebTransactionController.java        │
└────────────────────┬────────────────────┘
                     │ (DTOs)
┌────────────────────▼────────────────────┐
│            BUSINESS SERVICES            │  ← AuthService, BudgetService, etc.
│  - Checks limits, computes balances,    │    Shares core services with desktop module.
│    performs math & validation.          │
└────────────────────┬────────────────────┘
                     │ (Interfaces)
┌────────────────────▼────────────────────┐
│            REPOSITORY LAYER             │  ← JdbcUserRepo, JdbcTransactionRepo, etc.
│  - Executes native PostgreSQL queries.   │
└────────────────────┬────────────────────┘
                     ▼
            PostgreSQL Database
```

---

## 📂 Project Structure

```bash
money-manager-web
├─ pom.xml                  # Web Maven packaging & spring-boot configurations
├─ run.bat                  # Local start script with default DB environment variables
├─ start.bat                # Packaging + Jar running launch script
├─ src
│  ├─ main
│  │  ├─ java
│  │  │  └─ com
│  │  │     ├─ moneytracker
│  │  │     │  ├─ MoneytrackerApplication.java   # Spring Boot bootstrapper
│  │  │     │  └─ web/                           # Thymeleaf web controllers
│  │  │     └─ moneymanager                      # Shared core domain logic
│  │  │        ├─ model/                         # Core Models
│  │  │        ├─ repository/                    # Database JDBC Repositories
│  │  │        └─ service/                       # Calculations & Validation
│  │  └─ resources
│  │     ├─ application.properties               # Spring Boot environment properties
│  │     ├─ db.properties                        # Relational database URL templates
│  │     ├─ schema.sql                           # PostgreSQL database layout DDL
│  │     ├─ static/                              # Custom CSS, JS, and image assets
│  │     └─ templates/                           # Thymeleaf views (dashboard, transactions)
│  └─ test
│     └─ java                                    # Spring Boot WebMVC Mock tests
```

---

## 📦 Requirements

- **Java JDK**: Version 26 installed (since the project uses cutting edge JDK 26 features).
- **PostgreSQL**: Version 16+ running locally or in the cloud.
- **Maven**: Version 3.9+ (or use included `mvnw.cmd` wrapper).

---

## ⚙️ Database Configuration

By default, the application reads its connection parameters from environment variables or from `src/main/resources/db.properties`.

For local deployment, configure the target database details inside `src/main/resources/db.properties`:
```properties
db.url=jdbc:postgresql://localhost:5432/moneymanager
db.username=postgres
db.password=your_db_password
```

For Supabase deployment, set the database URL in the environment. The app supports Supabase-style URLs and will convert them to JDBC automatically.

```powershell
set DATABASE_URL=postgres://postgres:[YOUR-PASSWORD]@db.sygsiukwcfwlydpheypd.supabase.co:5432/postgres
set DATABASE_USERNAME=postgres
set DATABASE_PASSWORD=[YOUR-PASSWORD]
```

If the URL already contains the username and password, only `DATABASE_URL` is required.

> Note: If your network is IPv4-only, Supabase may require the Session Pooler or an IPv4 add-on. This is a Supabase networking requirement, not a change in the application code.

---

## ▶️ Run the App

There are multiple convenient ways to launch the Web Application:

### 1. Using `run.bat` (Recommended on Windows)
Simply double-click `run.bat` or run it from a PowerShell/CMD terminal:
```bash
run.bat
```
> 💡 *This script automatically configures default credentials (`postgres`/`123456`) and triggers `mvnw.cmd spring-boot:run`. Once the server boots, visit [http://localhost:8080](http://localhost:8080).*

### 2. Using `start.bat` (Builds and runs production JAR)
Double-click `start.bat` or execute:
```bash
start.bat
```
> 💡 *This will compile the code, bundle it into a production-ready JAR, skip execution of unit tests, and launch it natively via `java -jar`.*

### 3. Using Standard Maven Command
```bash
mvn clean spring-boot:run
```

---

## 🧪 Running Unit Tests

To run the Spring Boot test cases and verify endpoint routing:
```bash
mvn test
```

---

## 🔮 Future Enhancements

- 🛡️ **Spring Security Integration**: Add OAuth2, multi-factor authentication, and JWT authorization for RESTful APIs.
- 🐳 **Dockerization**: Provide a `Dockerfile` and `docker-compose.yml` to bundle the app and PostgreSQL instance into lightweight Docker containers.
- 📡 **REST API Layer**: Expose secure REST endpoints to allow mobile apps or third-party tracking software to sync data effortlessly.

---

## 📝 License

Distributed under the MIT License. See [LICENSE](../LICENSE) for more details.

---

## 🔗 Contact

| Platform | Link |
|:---|:---|
| 🐙 GitHub | [anasemadanas](https://github.com/anasemadanas/) |
| 💼 LinkedIn | [Anas Emad](https://www.linkedin.com/in/eng-anasemad/) |
| 📧 Email | [anaspython3@gmail.com](mailto:anaspython3@gmail.com) |

[↩️ Back to Table of Contents](#-table-of-contents)
