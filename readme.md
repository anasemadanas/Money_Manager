# Money Manager — .claude.md

> **Scope**: University presentation project. Build a *working*, *polished*, *small* desktop app — not a production enterprise system. Cut scope aggressively; favour a tight demo over feature completeness.

---

## 1 — Project Identity

| Field | Value |
|---|---|
| Name | Money Manager |
| Type | JavaFX desktop app |
| Language | Java 17 LTS |
| Build | Apache Maven 3.9+ |
| Database | PostgreSQL 16 (local) |
| Architecture | 3-Tier (Presentation → Service → Repository) |
| Target | University CS course presentation & live demo |

---

## 2 — What to Build (MVP only)

### Core features (must have)

1. **Authentication** — simple login/register screen. Passwords hashed with BCrypt. One user profile is fine.
2. **Transaction CRUD** — add, view, edit, delete income/expense entries (name, amount, category, type, date).
3. **Budget tracker** — set a monthly spending cap per category; show a progress bar; amber at 80 %, red at 100 %.
4. **Dashboard** — single screen showing: total income, total expenses, net balance (current month), a pie chart of spending by category, and a bar chart of monthly trends. Use JavaFX `PieChart` and `BarChart`.
5. **Notes** — lightweight notes list (title + content + date). No tags — keep it simple.
6. **Savings Goals** — user creates a goal with a name (e.g. "New Laptop"), target amount (`BigDecimal`), and optional deadline (`LocalDate`). User manually adds contributions toward a goal. The UI shows a `ProgressBar` per goal with percentage label (e.g. "72 % — $720 / $1,000"). Colour-coded: green when on track, amber when deadline is within 30 days and < 80 %, red when past deadline and incomplete.

### Explicitly out of scope for this build

- PDF / Excel export
- Multi-currency
- AI insights
- Cloud sync
- Android / web ports
- Scheduled reports
- Multi-user / family mode
- i18n / resource bundles (English only)

---

## 3 — Architecture Rules

```
┌─────────────────────────────────────┐
│  PRESENTATION (JavaFX / FXML / CSS) │  ← Controllers, Views, Event Handlers
│  No business logic here. Ever.      │
└──────────────┬──────────────────────┘
               │ calls (DTOs)
┌──────────────▼──────────────────────┐
│  SERVICE LAYER                      │  ← TransactionService, BudgetService, etc.
│  All validation & calculations      │
└──────────────┬──────────────────────┘
               │ calls (interfaces)
┌──────────────▼──────────────────────┐
│  REPOSITORY LAYER (JDBC)            │  ← ITransactionRepo → JdbcTransactionRepo
│  Raw SQL via PreparedStatement      │
└──────────────┬──────────────────────┘
               │
           PostgreSQL
```

### Simplifications vs. the full SRS

- **Use plain JDBC** with `PreparedStatement` — skip JPA / Hibernate / Spring entirely. This is a small university project; an ORM is overkill and adds startup complexity.
- **No Spring framework** — wire dependencies manually via constructor injection in `main()`. Still honours Dependency Inversion (services depend on interfaces, not concrete repos).
- **No Flyway** — include a single `schema.sql` file and run it manually or via a `DatabaseInitializer` utility class on first launch.
- **No HikariCP** — a single `java.sql.Connection` (or a tiny helper that creates connections from a config) is sufficient for a single-user desktop app.

### SOLID — still enforced, just lightweight

| Principle | How |
|---|---|
| **S** — Single Responsibility | Each class does one thing. `TransactionService` ≠ `BudgetService` ≠ `TransactionController`. |
| **O** — Open/Closed | New features = new classes, not edits to existing ones. |
| **L** — Liskov Substitution | Any `ITransactionRepo` impl is swappable. |
| **I** — Interface Segregation | Small repo interfaces: `ITransactionRepo`, `IBudgetRepo`, `IUserRepo`, `INoteRepo`. |
| **D** — Dependency Inversion | Services take repo *interfaces* via constructor. Controllers take services via constructor. |

---

## 4 — Project Layout

```
money-manager/
├── pom.xml
├── schema.sql                          ← run once to create tables
├── src/main/java/com/moneymanager/
│   ├── App.java                        ← JavaFX Application entry point
│   ├── config/
│   │   └── DatabaseConfig.java         ← getConnection(), reads db.properties
│   ├── model/
│   │   ├── User.java
│   │   ├── Transaction.java
│   │   ├── Budget.java
│   │   └── Note.java
│   ├── dto/
│   │   ├── TransactionDTO.java
│   │   ├── BudgetDTO.java
│   │   └── GoalDTO.java
│   ├── repository/
│   │   ├── IUserRepo.java
│   │   ├── ITransactionRepo.java
│   │   ├── IBudgetRepo.java
│   │   ├── INoteRepo.java
│   │   ├── IGoalRepo.java
│   │   ├── JdbcUserRepo.java
│   │   ├── JdbcTransactionRepo.java
│   │   ├── JdbcBudgetRepo.java
│   │   ├── JdbcNoteRepo.java
│   │   └── JdbcGoalRepo.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── TransactionService.java
│   │   ├── BudgetService.java
│   │   ├── NoteService.java
│   │   ├── GoalService.java
│   │   └── DashboardService.java
│   └── ui/
│       ├── controller/
│       │   ├── LoginController.java
│       │   ├── DashboardController.java
│       │   ├── TransactionController.java
│       │   ├── BudgetController.java
│       │   ├── NoteController.java
│       │   └── GoalController.java
│       └── util/
│           └── AlertHelper.java        ← reusable JavaFX alert dialogs
├── src/main/resources/
│   ├── db.properties                   ← url, user, password
│   ├── fxml/
│   │   ├── login.fxml
│   │   ├── main.fxml                   ← TabPane shell (tabs: Dashboard, Transactions, Budgets, Goals, Notes)
│   │   ├── dashboard.fxml
│   │   ├── transactions.fxml
│   │   ├── budgets.fxml
│   │   ├── goals.fxml
│   │   └── notes.fxml
│   └── css/
│       └── style.css                   ← single stylesheet
└── src/test/java/com/moneymanager/
    ├── service/
    │   ├── TransactionServiceTest.java
    │   ├── BudgetServiceTest.java
    │   └── GoalServiceTest.java
    └── repository/
        └── JdbcTransactionRepoTest.java
```

---

## 5 — Database Schema (`schema.sql`)

```sql
-- Run once against your local PostgreSQL 16 instance

CREATE TABLE IF NOT EXISTS users (
    user_id     BIGSERIAL       PRIMARY KEY,
    username    VARCHAR(50)     NOT NULL UNIQUE,
    password_hash TEXT          NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id  BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    name            VARCHAR(100)    NOT NULL,
    amount          NUMERIC(12,2)   NOT NULL CHECK (amount > 0),
    category        VARCHAR(50)     NOT NULL,
    tx_type         VARCHAR(10)     NOT NULL CHECK (tx_type IN ('INCOME','EXPENSE')),
    tx_date         DATE            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS budgets (
    budget_id   BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    category    VARCHAR(50)     NOT NULL,
    amount_cap  NUMERIC(12,2)   NOT NULL CHECK (amount_cap > 0),
    month       SMALLINT        NOT NULL CHECK (month BETWEEN 1 AND 12),
    year        SMALLINT        NOT NULL CHECK (year >= 2020),
    UNIQUE (user_id, category, month, year)
);

CREATE TABLE IF NOT EXISTS notes (
    note_id     BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title       VARCHAR(100)    NOT NULL,
    content     TEXT,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS savings_goals (
    goal_id         BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    name            VARCHAR(100)    NOT NULL,
    target_amount   NUMERIC(12,2)   NOT NULL CHECK (target_amount > 0),
    saved_amount    NUMERIC(12,2)   NOT NULL DEFAULT 0 CHECK (saved_amount >= 0),
    deadline        DATE,                           -- optional
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS goal_contributions (
    contribution_id BIGSERIAL       PRIMARY KEY,
    goal_id         BIGINT          NOT NULL REFERENCES savings_goals(goal_id) ON DELETE CASCADE,
    amount          NUMERIC(12,2)   NOT NULL CHECK (amount > 0),
    note            VARCHAR(200),                   -- optional memo, e.g. "birthday money"
    contributed_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_tx_user_date ON transactions (user_id, tx_date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_user_cat  ON transactions (user_id, category);
CREATE INDEX IF NOT EXISTS idx_bg_user_ym   ON budgets (user_id, year, month);
CREATE INDEX IF NOT EXISTS idx_goal_user    ON savings_goals (user_id);
CREATE INDEX IF NOT EXISTS idx_contrib_goal ON goal_contributions (goal_id);
```

---

## 6 — GUI Design Guidelines

### Navigation

The main window uses a **TabPane** with 5 tabs: **Dashboard**, **Transactions**, **Budgets**, **Goals**, **Notes**. The login screen is a separate FXML loaded first; on successful auth, swap to `main.fxml`.

### Visual style (JavaFX CSS)

- **Color palette**: Dark sidebar / header (`#1e293b`), white content area, green accent for income (`#16a34a`), red accent for expense (`#dc2626`), amber for budget warnings (`#f59e0b`).
- **Font**: System default is fine for a university project. If you want polish, load *"Segoe UI"* on Windows / *"SF Pro"* on macOS via CSS `-fx-font-family`.
- **Spacing**: 16 px padding on all content panes. 8 px gaps between form fields.
- **Tables**: Use `TableView` with alternating row colours for readability. Zebra stripe via CSS.
- **Charts**: `PieChart` for category breakdown. `BarChart` for monthly trend (last 6 months). Use the accent colours above.
- **Buttons**: Rounded corners (`-fx-background-radius: 6`), filled primary buttons (green for add, red for delete), outlined secondary buttons.
- **Alerts**: Use `Alert` dialogs for confirmation (delete) and validation errors.

### Key screens

1. **Login** — centred card with username, password, Login button, Register link.
2. **Dashboard** — top row of 3 KPI cards (Income / Expenses / Balance), pie chart left, bar chart right.
3. **Transactions** — `TableView` with columns: Date, Name, Category, Type, Amount. Toolbar with Add / Edit / Delete buttons. Filter by date range and category via `ComboBox` + `DatePicker`.
4. **Budgets** — list of category budgets with `ProgressBar` per row. Add / Edit / Delete. Colour-coded progress (green < 80%, amber 80-99%, red ≥ 100%).
5. **Goals** — each goal is a card/row showing: goal name, target amount, saved amount, `ProgressBar` with percentage label (e.g. "72 % — $720 / $1,000"), and deadline (if set). Colour logic: green = on track or no deadline, amber = deadline within 30 days and < 80 % saved, red = past deadline and incomplete. Toolbar with "New Goal" and "Add Contribution" buttons. "Add Contribution" opens a dialog: pick a goal from a dropdown, enter amount, optional memo. Contribution history shown in a `TableView` below the selected goal.
6. **Notes** — simple `ListView` on the left, detail pane on the right. Add / Delete.

---

## 7 — Coding Standards

- **Java version**: 17. Use `record` for DTOs, `var` where type is obvious, `switch` expressions.
- **Null safety**: Never return `null` from service methods. Return `Optional<T>` or empty `List<T>`.
- **Money**: `BigDecimal` everywhere. Never `double` or `float` for monetary values.
- **Dates**: `java.time.LocalDate` for transaction dates. `java.time.OffsetDateTime` for timestamps.
- **SQL**: Use `PreparedStatement` with parameter binding (`?`). Never concatenate user input into SQL strings.
- **Error handling**: Wrap `SQLException` in a custom `DataAccessException` (unchecked). Controllers catch and show user-friendly `Alert`.
- **Logging**: `System.out.println` is acceptable for a university project. If time permits, use `java.util.logging`.
- **Javadoc**: At minimum, document every `public` method in the service layer.

---

## 8 — Maven pom.xml (Minimal)

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.moneymanager</groupId>
    <artifactId>money-manager</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <javafx.version>21.0.2</javafx.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- JavaFX -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
            <version>${javafx.version}</version>
        </dependency>

        <!-- PostgreSQL JDBC -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.3</version>
        </dependency>

        <!-- BCrypt for password hashing -->
        <dependency>
            <groupId>at.favre.lib</groupId>
            <artifactId>bcrypt</artifactId>
            <version>0.10.2</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <version>0.0.8</version>
                <configuration>
                    <mainClass>com.moneymanager.App</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 9 — Build & Run Cheatsheet

```bash
# 1. Create the database
psql -U postgres -c "CREATE DATABASE moneymanager;"
psql -U postgres -d moneymanager -f schema.sql

# 2. Configure connection
# Edit src/main/resources/db.properties:
#   db.url=jdbc:postgresql://localhost:5432/moneymanager
#   db.user=postgres
#   db.password=your_password

# 3. Build
mvn clean compile

# 4. Run
mvn javafx:run

# 5. Test
mvn test
```

---

## 10 — Implementation Order (build in this sequence)

1. **Schema + DatabaseConfig** — get a connection working, verify with a simple SELECT.
2. **User model + JdbcUserRepo + AuthService** — register & login.
3. **Login FXML + LoginController** — wire up the UI. On success, load `main.fxml`.
4. **Transaction model + repo + service + FXML + controller** — full CRUD with TableView.
5. **Budget model + repo + service + FXML + controller** — budget list with progress bars.
6. **Goal + Contribution models + JdbcGoalRepo + GoalService + FXML + controller** — goal cards with progress bars, contribution dialog, contribution history table.
7. **DashboardService + dashboard FXML + controller** — KPI cards, PieChart, BarChart.
8. **Note model + repo + service + FXML + controller** — simple notes list.
9. **CSS polish** — apply `style.css`, colour-code everything, make it look clean for the demo.
10. **Write 3–5 unit tests** for `TransactionService`, `BudgetService`, and `GoalService` (enough to demonstrate testing knowledge).

---

## 11 — Presentation Demo Script

For a live demo, follow this flow:

1. Launch the app → show login screen → register a new user → log in.
2. Add 5–6 sample transactions (mix of income and expenses across 2–3 categories).
3. Switch to Dashboard tab → show the KPI cards updating, pie chart, bar chart.
4. Switch to Budgets tab → create a budget for "Food" at $200 → add a $170 food expense → show amber warning → add another $40 → show red alert.
5. Switch to Goals tab → create a goal "New Laptop" with target $1,000 and a deadline 3 months out → add a $400 contribution → show progress bar at 40 % (green) → add $450 more → show 85 % → point out deadline colour logic.
6. Switch to Notes tab → add a quick note.
7. Briefly show the project structure in your IDE to explain the 3-tier architecture and SOLID principles.
8. Show one unit test running green.

---

## 12 — Common Pitfalls to Avoid

- **Don't over-engineer.** No Spring, no Hibernate, no microservices. Plain Java + JDBC + JavaFX.
- **Don't skip the GUI.** A terminal-only app won't impress. The JavaFX GUI is the centrepiece of the demo.
- **Don't use `double` for money.** Use `BigDecimal`. This is a common exam question — your professor will check.
- **Don't hardcode SQL credentials.** Use `db.properties` loaded from the classpath.
- **Don't forget input validation.** Negative amounts, empty names, future dates — handle them in the service layer and show error alerts in the UI.
- **Don't leave the dashboard empty.** Seed the database with sample data if needed so the charts look good during the demo.
