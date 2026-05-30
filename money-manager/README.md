# Money Manager App - Final Unified Documentation

> A modern personal finance management desktop application built with Java 21, JavaFX, SQLite, JDBC, and a clean 3-tier architecture.

[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org)
[![Maven](https://img.shields.io/badge/Build-Apache%20Maven%203.9%2B-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org)
[![JavaFX](https://img.shields.io/badge/UI-JavaFX%2021-1F7A8C?logo=openjdk&logoColor=white)](https://openjfx.io)
[![Database](https://img.shields.io/badge/Database-SQLite-003B57?logo=sqlite&logoColor=white)](https://www.sqlite.org)
[![Architecture](https://img.shields.io/badge/Architecture-3--Tier-blue)](https://en.wikipedia.org/wiki/Multitier_architecture)
[![Design](https://img.shields.io/badge/Design-SOLID%20Principles-green)](https://en.wikipedia.org/wiki/SOLID)

---

## Table of Contents

- [Introduction](#introduction)
- [Project Objectives](#project-objectives)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [SOLID Design](#solid-design)
- [Project Structure](#project-structure)
- [Database Design](#database-design)
- [Functional Requirements](#functional-requirements)
- [Non-Functional Requirements](#non-functional-requirements)
- [Installation and Build](#installation-and-build)
- [Running the Application](#running-the-application)
- [Testing](#testing)
- [Future Enhancements](#future-enhancements)
- [Current Limitations](#current-limitations)
- [Contributing](#contributing)
- [Contact](#contact)

---

## Introduction

The Money Manager Desktop App is a standalone personal finance management system that helps users:

- Track income and expenses
- Manage monthly and category budgets
- Create savings goals and record contributions
- Store financial notes
- View spending analytics through dashboard charts
- Configure a monthly income amount that can be applied to the current month

The application is designed as an offline-first desktop system. It uses SQLite for local persistence and keeps the code separated into presentation, service, and repository layers.

## Project Objectives

- Track expenses and income with create, update, delete, and filter workflows
- Categorize transactions clearly
- Manage monthly budgets with spending warnings
- Visualize income, expenses, category spending, and monthly trends
- Support savings goals, goal contributions, and note tracking
- Provide secure local authentication with hashed passwords
- Keep a maintainable architecture based on interfaces and service boundaries

## Features

| Feature | Current Implementation |
| --- | --- |
| Authentication system | User registration, login, password reset, BCrypt password hashing |
| Transaction manager | Add, edit, delete, and filter transactions by date/category |
| Dashboard analytics | Summary cards, category PieChart, monthly trend BarChart |
| Budget planner | Category budgets, monthly budget balance, progress indicators, warnings |
| Savings goals | Create/edit/delete goals, add contributions, track progress |
| Financial notes | Create and delete notes/reminders |
| Validation layer | Service-level validation for inputs and business rules |
| Monthly income | Saved monthly income can create a current-month income transaction and budget cap |
| User-scoped data changes | Delete/update operations include `userId` where appropriate |

## Technology Stack

| Layer | Technology |
| --- | --- |
| Programming language | Java 21 |
| GUI framework | JavaFX 21 |
| UI layout | FXML + CSS |
| Database | SQLite |
| Data access | JDBC |
| Build tool | Apache Maven |
| Security | BCrypt password hashing |
| Logging | `java.util.logging` |
| Testing | JUnit 5 |

This codebase does not currently use Spring, JPA/Hibernate, Flyway, HikariCP, PostgreSQL, Mockito, Testcontainers, JaCoCo, SLF4J/Logback, Apache POI, iText, PDF export, Excel export, i18n resources, or multi-currency conversion.

## System Architecture

The application follows a 3-tier architecture:

```text
+-----------------------------------------+
|           PRESENTATION LAYER            |
| JavaFX, FXML, Controllers, CSS          |
+-------------------+---------------------+
                    |
+-------------------v---------------------+
|          BUSINESS LOGIC LAYER           |
| Services, Validation, Calculations      |
+-------------------+---------------------+
                    |
+-------------------v---------------------+
|            DATA ACCESS LAYER            |
| Repository Interfaces, JDBC, SQL        |
+-------------------+---------------------+
                    |
                 SQLite
```

### Presentation Layer

- Contains JavaFX controllers and FXML views
- Handles user interaction and screen updates
- Delegates business rules to services

### Business Logic Layer

- Validates inputs
- Applies financial rules
- Coordinates repository operations
- Depends on repository interfaces instead of concrete implementations

### Data Access Layer

- Contains JDBC repository implementations
- Executes SQL queries against SQLite
- Hides database details behind interfaces such as `ITransactionRepo`, `IBudgetRepo`, and `IGoalRepo`

## SOLID Design

| Principle | How the Project Applies It |
| --- | --- |
| Single Responsibility | Controllers, services, repositories, DTOs, and models have separate roles |
| Open/Closed | Repository interfaces allow data access implementations to be replaced later |
| Liskov Substitution | Services can work with any implementation of the repository interfaces |
| Interface Segregation | Repositories are split by feature area instead of one large data interface |
| Dependency Inversion | Services depend on interfaces such as `IUserRepo`, `ITransactionRepo`, and `IBudgetRepo` |

`ApplicationContext` centralizes repository and service creation, keeping `App` focused on startup and JavaFX scene loading.

## Project Structure

```text
money-manager/
|-- pom.xml
|-- README.md
|-- schema sqlite.sql
|-- src/
|   |-- main/
|   |   |-- java/com/moneymanager/
|   |   |   |-- App.java
|   |   |   |-- Launcher.java
|   |   |   |-- ConnectionTest.java
|   |   |   |-- config/
|   |   |   |   |-- ApplicationContext.java
|   |   |   |   |-- DatabaseConfig.java
|   |   |   |   |-- DatabaseInitializer.java
|   |   |   |   |-- LoggingConfig.java
|   |   |   |-- dto/
|   |   |   |-- model/
|   |   |   |-- repository/
|   |   |   |-- service/
|   |   |   |-- ui/
|   |   |       |-- controller/
|   |   |       |-- util/
|   |   |-- resources/
|   |       |-- css/
|   |       |-- fxml/
|   |       |-- db.properties
|   |-- test/java/com/moneymanager/service/
```

## Database Design

The application uses SQLite as the local relational database. The default database URL is configured in:

```text
src/main/resources/db.properties
```

Default value:

```properties
db.url=jdbc:sqlite:money-manager.db
```

Schema creation is handled by `DatabaseInitializer` on application startup.

### Entity Relationship Overview

```text
users 1--N transactions
users 1--N budgets
users 1--N notes
users 1--N savings_goals
savings_goals 1--N goal_contributions
users 1--1 user_settings
users 1--N monthly_balance
```

### Main Tables

#### `users`

| Column | Type | Notes |
| --- | --- | --- |
| user_id | INTEGER | Primary key |
| username | TEXT | Unique, required |
| password_hash | TEXT | BCrypt hash |
| security_question | TEXT | Used for password reset |
| security_answer_hash | TEXT | Hashed answer |
| created_at | TEXT | Created timestamp |

#### `transactions`

| Column | Type | Notes |
| --- | --- | --- |
| transaction_id | INTEGER | Primary key |
| user_id | INTEGER | Foreign key to `users` |
| name | TEXT | Required |
| amount | NUMERIC | Must be greater than 0 |
| category | TEXT | Required |
| tx_type | TEXT | `INCOME` or `EXPENSE` |
| tx_date | TEXT | Transaction date |
| created_at | TEXT | Created timestamp |

#### `budgets`

| Column | Type | Notes |
| --- | --- | --- |
| budget_id | INTEGER | Primary key |
| user_id | INTEGER | Foreign key to `users` |
| category | TEXT | Required |
| amount_cap | NUMERIC | Must be greater than 0 |
| month | INTEGER | 1 through 12 |
| year | INTEGER | 2020 or later |

#### `savings_goals`

| Column | Type | Notes |
| --- | --- | --- |
| goal_id | INTEGER | Primary key |
| user_id | INTEGER | Foreign key to `users` |
| name | TEXT | Required |
| target_amount | NUMERIC | Must be greater than 0 |
| saved_amount | NUMERIC | Defaults to 0 |
| deadline | TEXT | Optional |
| created_at | TEXT | Created timestamp |

#### `goal_contributions`

| Column | Type | Notes |
| --- | --- | --- |
| contribution_id | INTEGER | Primary key |
| goal_id | INTEGER | Foreign key to `savings_goals` |
| amount | NUMERIC | Must be greater than 0 |
| note | TEXT | Optional |
| contributed_at | TEXT | Created timestamp |

#### Other Tables

- `notes`
- `monthly_balance`
- `user_settings`

### Indexes

- `idx_tx_user_date` for user/date transaction queries
- `idx_tx_user_cat` for user/category transaction queries
- `idx_bg_user_ym` for monthly budget lookups
- `idx_goal_user` for user goal lookups
- `idx_contrib_goal` for contribution lookups

## Functional Requirements

### FR-01 Transaction Management

- Add transactions with name, amount, category, type, and date
- Edit existing transactions
- Delete transactions with confirmation prompts
- Filter transactions by category and date
- Use `TransactionType` enum for income/expense values in Java code

### FR-02 Budget Management

- Create monthly budgets per category
- Display spending progress against each category cap
- Display warning states around 80% and 100% usage
- Set and edit a total monthly budget balance

### FR-03 Dashboard Analytics

- Display income, expenses, net balance, goal savings, and available balance
- Show expense category breakdown using JavaFX PieChart
- Show monthly income/expense trends using JavaFX BarChart

### FR-04 Notes and Goals

- Create financial notes and reminders
- Create savings goals with optional deadlines
- Add contributions to goals
- Track saved amount and completion percentage

## Non-Functional Requirements

| ID | Requirement |
| --- | --- |
| NFR-01 | Passwords are stored using BCrypt hashes |
| NFR-02 | Data is stored locally in SQLite |
| NFR-03 | Monetary values use `BigDecimal` |
| NFR-04 | Services remain independent from JDBC implementation classes |
| NFR-05 | Maven build and test commands are supported |
| NFR-06 | JavaFX UI is separated into controllers, FXML, and CSS |
| NFR-07 | User-scoped update/delete operations reduce cross-user data risk |

## Installation and Build

### Clone Repository

```bash
git clone https://github.com/anasemadanas/Money_Manager.git
cd Money_Manager/money-manager
```

### Compile Project

```bash
mvn clean compile
```

### Package JAR

```bash
mvn clean package
```

## Running the Application

### Launch via Maven

```bash
mvn javafx:run
```

### Launch with Maven Wrapper on Windows

```bat
mvnw.cmd javafx:run
```

### Launch with Maven Wrapper on macOS/Linux

```bash
./mvnw javafx:run
```

## Testing

Run unit tests:

```bash
mvn test
```

Run full Maven verification:

```bash
mvn verify
```

Current tests are JUnit 5 service tests.

## Future Enhancements

| Enhancement | Details |
| --- | --- |
| Export reports | Add PDF/Excel export after selecting libraries |
| Internationalization | Add ResourceBundle files and locale switching |
| Multi-currency support | Add currency preferences and conversion handling |
| Database migrations | Add a migration tool if schema changes become frequent |
| Dark mode | Add theme switching in JavaFX CSS |
| Database encryption | Consider SQLCipher or another local encryption strategy |
| Web or mobile version | Possible future platform expansion |

## Current Limitations

- SQLite local database only
- No PDF or Excel export yet
- No Spring, JPA/Hibernate, Flyway, or HikariCP
- No i18n resources yet
- No multi-currency conversion yet
- No license file is currently included in the repository

## Contributing

Contributions are welcome.

1. Fork the repository
2. Create a feature branch
3. Add or update tests
4. Run `mvn test`
5. Commit changes
6. Open a pull request

Please keep the 3-tier boundaries clean and avoid adding framework claims to the documentation unless the code actually uses those frameworks.

## Contact

| Platform | Link |
| --- | --- |
| GitHub | [https://github.com/anasemadanas](https://github.com/anasemadanas) |
| LinkedIn | [https://www.linkedin.com/in/eng-anasemad/](https://www.linkedin.com/in/eng-anasemad/) |
| Email | [Email](mailto:anasemadanas1@gmail.com) |

---

<p align="center">
  <sub>Built with Java 21, Apache Maven, JavaFX, SQLite, and SOLID-minded design.</sub>
</p>
