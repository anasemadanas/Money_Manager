# 💰 Money Manager Desktop App — Unified Project Documentation

> A modern personal finance management desktop application built using Java 17, JavaFX, and a strict 3‑Tier Architecture following SOLID principles.

[![Java](https://img.shields.io/badge/Java-17%20LTS-007396?logo=openjdk&logoColor=white)](https://openjdk.org)
[![Maven](https://img.shields.io/badge/Build-Apache%20Maven%203.9%2B-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org)
[![JavaFX](https://img.shields.io/badge/UI-JavaFX%2021-1F7A8C?logo=openjdk&logoColor=white)](https://openjfx.io)
[![Architecture](https://img.shields.io/badge/Architecture-3--Tier-blue)](https://en.wikipedia.org/wiki/Multitier_architecture)
[![SOLID](https://img.shields.io/badge/Design-SOLID%20Principles-green)](https://en.wikipedia.org/wiki/SOLID)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

# 📑 Table of Contents

1. Introduction
2. Project Objectives
3. Features
4. Technology Stack
5. System Architecture
6. SOLID Principles
7. Folder Structure
8. Database Design
9. Functional Requirements
10. Non‑Functional Requirements
11. Installation & Build
12. Running the Application
13. Testing
14. Future Enhancements
15. Constraints & Assumptions
16. Contributing
17. License
18. Contact

---

# 🧾 Introduction

The **Money Manager Desktop App** is a standalone personal finance management system that enables users to:

- Track income and expenses
- Manage budgets
- Create savings goals
- Store financial notes
- Visualize spending analytics through charts and dashboards

The application is designed with a strict **3‑Tier Architecture** and follows all **SOLID principles** to ensure maintainability, scalability, and clean separation of concerns.

The project is built using:

| Technology | Purpose |
|---|---|
| Java 17 LTS | Core programming language |
| JavaFX 21 | Desktop GUI framework |
| SQLite / PostgreSQL | Data persistence layer |
| Maven | Dependency management & build automation |
| BCrypt | Secure password hashing |
| JUnit 5 | Unit testing |

The application is designed as an offline‑first system while remaining flexible for future migration toward cloud and multi‑platform environments.

---

# 🎯 Project Objectives

- Track expenses and income with full CRUD functionality
- Categorize transactions efficiently
- Manage monthly budgets with spending alerts
- Visualize financial reports using charts and dashboards
- Support financial goals and note tracking
- Provide secure local data storage
- Build a scalable architecture suitable for desktop, Android, and web expansion

---

# ✨ Features

| Feature | Description |
|---|---|
| 🔐 Authentication System | User login and registration with secure password hashing |
| 💸 Transaction Manager | Add, edit, delete, and filter transactions |
| 📊 Dashboard Analytics | Pie charts and monthly trend reports |
| 📅 Budget Planner | Monthly budget tracking with warning indicators |
| 🎯 Savings Goals | Set target savings goals and monitor progress |
| 📝 Financial Notes | Store notes and reminders related to finances |
| 🛡️ Validation Layer | Prevent invalid inputs and business rule violations |
| 📤 Reports Export | PDF and Excel financial reports |
| 🌍 Internationalization | Multi‑language support using ResourceBundle |
| 💱 Multi‑Currency Support | ISO‑4217 currency formatting |

---

# 🧰 Technology Stack

| Layer | Technology |
|---|---|
| Programming Language | Java 17 LTS |
| GUI Framework | JavaFX 21 |
| Database | SQLite / PostgreSQL 16 |
| Build Tool | Apache Maven 3.9+ |
| ORM / Data Access | JDBC / JPA + Hibernate |
| Dependency Injection | Constructor Injection / Spring Framework |
| Connection Pooling | HikariCP |
| Migration Tool | Flyway |
| Security | BCrypt Password Hashing |
| Testing | JUnit 5 + Mockito |
| Coverage | JaCoCo |
| Logging | SLF4J + Logback |
| Export Libraries | Apache POI, iText |

---

# 🏗️ System Architecture

The application follows a strict **3‑Tier Architecture**:

```text
┌─────────────────────────────────────────┐
│          PRESENTATION LAYER             │
│ JavaFX • FXML • Controllers • CSS       │
└───────────────────┬─────────────────────┘
                    │
┌───────────────────▼─────────────────────┐
│        BUSINESS LOGIC LAYER             │
│ Services • Validation • Calculations    │
└───────────────────┬─────────────────────┘
                    │
┌───────────────────▼─────────────────────┐
│          DATA ACCESS LAYER              │
│ Repositories • JPA/JDBC • SQL           │
└───────────────────┬─────────────────────┘
                    │
             SQLite / PostgreSQL
```

## A. Presentation Layer

- Responsible for the graphical user interface
- Uses JavaFX 21 with FXML and CSS
- Contains no business logic
- Communicates only with the service layer

## B. Business Logic Layer

- Contains validations and financial calculations
- Implements application business rules
- Depends only on repository interfaces

## C. Data Access Layer

- Handles database communication
- Contains repository implementations
- Responsible for SQL/JPA operations

---

# 🧱 SOLID Principles

| Principle | Application |
|---|---|
| Single Responsibility | Each class handles a single concern |
| Open/Closed | New features can be added without modifying existing code |
| Liskov Substitution | Repository implementations are interchangeable |
| Interface Segregation | Small focused repository interfaces |
| Dependency Inversion | Services depend on abstractions/interfaces |

---

# 📁 Folder Structure

```text
money-manager/
├── pom.xml
├── src/main/java/com/moneymanager/
│   ├── App.java
│   ├── config/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   ├── service/
│   ├── ui/
│   └── util/
├── src/main/resources/
│   ├── fxml/
│   ├── css/
│   ├── i18n/
│   ├── db/migration/
│   └── application.yml
└── src/test/java/
    ├── service/
    └── repository/
```

---

# 🗃️ Database Design

The system supports either SQLite (offline mode) or PostgreSQL (enterprise mode).

## Main Entities

```text
users ────< transactions
users ────< budgets
users ────< goals
```

## Example Tables

### users

| Column | Type |
|---|---|
| user_id | BIGSERIAL |
| username | VARCHAR(50) |
| password_hash | TEXT |
| created_at | TIMESTAMP |

### transactions

| Column | Type |
|---|---|
| transaction_id | BIGSERIAL |
| user_id | BIGINT |
| amount | NUMERIC(12,2) |
| category | VARCHAR(50) |
| tx_type | VARCHAR(10) |
| tx_date | DATE |

### budgets

| Column | Type |
|---|---|
| budget_id | BIGSERIAL |
| user_id | BIGINT |
| category | VARCHAR(50) |
| amount_cap | NUMERIC(12,2) |
| month | SMALLINT |
| year | SMALLINT |

---

# 📌 Functional Requirements

## FR‑01 Transaction Management

- Add transactions
- Edit transactions
- Delete transactions
- Filter transactions by category/date/type
- Categorize transactions automatically

## FR‑02 Budget Management

- Create monthly budgets
- Monitor spending against budget limits
- Display alerts at 80% and 100%
- Full CRUD operations for budgets

## FR‑03 Dashboard & Reports

- Display total income and expenses
- Visualize spending using charts
- Export reports to PDF and Excel

## FR‑04 Goals & Notes

- Create savings goals
- Track progress percentages
- Store notes and reminders
- Notify users about deadlines

---

# ⚙️ Non‑Functional Requirements

| ID | Requirement |
|---|---|
| NFR‑01 | Application launches within 4 seconds |
| NFR‑02 | GUI responses under 200 ms |
| NFR‑03 | Passwords stored using BCrypt |
| NFR‑04 | Clean and maintainable codebase |
| NFR‑05 | Unit test coverage above 80% |
| NFR‑06 | Compatible with Windows, Linux, and macOS |
| NFR‑07 | Scalable repository architecture |
| NFR‑08 | Database integrity enforced with constraints |
| NFR‑09 | Maven build reproducibility |
| NFR‑10 | Localization support |

---

# ⚙️ Installation & Build

## Clone Repository

```bash
git clone https://github.com/anasemadanas/Money_Manager.git
cd Money_Manager/money-manager
```

## Compile Project

```bash
mvn clean compile
```

---

# ▶️ Running the Application

## Launch via Maven

```bash
mvn javafx:run
```

## Package Executable JAR

```bash
mvn clean package
```

Run the generated JAR:

```bash
java -jar target/money-manager-1.0.0.jar
```

---

# 🧪 Testing

## Run Unit Tests

```bash
mvn test
```

## Full Verification

```bash
mvn verify
```

Testing stack includes:

- JUnit 5
- Mockito
- Testcontainers
- JaCoCo

---

# 🔮 Future Enhancements

| Enhancement | Details |
|---|---|
| Android App | Kotlin + Jetpack Compose |
| Web Platform | Spring Boot + React |
| Cloud Synchronization | AWS / Supabase / Neon |
| AI Insights | Transaction prediction and anomaly detection |
| Family Mode | Multi-user access and role management |
| Dark Mode | Advanced theme customization |
| SQLCipher | Database encryption |

---

# 🔒 Constraints & Assumptions

## Constraints

- Minimum Java version: JDK 17
- JavaFX 21 required
- PostgreSQL required for enterprise mode
- SQLite supported for local offline mode
- Desktop-only release in v1.0

## Assumptions

- Users understand basic finance concepts
- Maven 3.9+ installed
- Monetary values use BigDecimal
- Database migrations handled by Flyway

---

# 🤝 Contributing

Contributions are welcome.

Steps:

1. Fork the repository
2. Create a feature branch
3. Add tests
4. Commit changes
5. Push branch
6. Open a Pull Request

Please ensure:

- SOLID principles are respected
- 3‑Tier boundaries remain clean
- `mvn verify` passes successfully

---

# 📄 License

Distributed under the MIT License.

---

# 🔗 Contact

| Platform | Link |
|---|---|
| GitHub | https://github.com/anasemadanas |
| LinkedIn | https://www.linkedin.com/in/eng-anasemad/ |
| Email | anaspython3@gmail.com |

---

# 📚 Source References

This unified document was created by merging and organizing content from the uploaded project files. fileciteturn0file0L1-L260 fileciteturn0file1L1-L700

