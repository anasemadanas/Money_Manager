package com.moneymanager.config;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void init() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            String url = conn.getMetaData().getURL();
            if (url != null && url.startsWith("jdbc:sqlite")) {
                for (String sql : getSqliteBootstrapStatements()) {
                    try {
                        stmt.execute(sql);
                    } catch (java.sql.SQLException e) {
                        if (!shouldIgnoreSqliteBootstrapError(e)) throw e;
                    }
                }
            } else {
                enableForeignKeys(conn);
                stmt.execute(getUsersTableSQL(conn));
            }

        } catch (Exception e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    private static boolean shouldIgnoreSqliteBootstrapError(java.sql.SQLException e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        msg = msg.toLowerCase();
        return msg.contains("duplicate column name")
                || msg.contains("already exists");
    }

    private static void enableForeignKeys(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            String url = conn.getMetaData().getURL();

            if (url.startsWith("jdbc:sqlite")) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

        } catch (Exception ignored) {
        }
    }

    private static String getUsersTableSQL(Connection conn) throws Exception {
        String url = conn.getMetaData().getURL();

        if (url.startsWith("jdbc:postgresql")) {
            return """
                CREATE TABLE IF NOT EXISTS users (
                    user_id BIGSERIAL PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """;
        }

        if (url.startsWith("jdbc:mysql")) {
            return """
                CREATE TABLE IF NOT EXISTS users (
                    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """;
        }

        return """
            CREATE TABLE IF NOT EXISTS users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
            );
        """;
    }

    private static String[] getSqliteBootstrapStatements() {
        return new String[]{
                """
                CREATE TABLE IF NOT EXISTS users (
                    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    security_question TEXT,
                    security_answer_hash TEXT,
                    created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS transactions (
                    transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    amount NUMERIC NOT NULL CHECK (amount > 0),
                    category TEXT NOT NULL,
                    tx_type TEXT NOT NULL CHECK (tx_type IN ('INCOME','EXPENSE')),
                    tx_date TEXT NOT NULL,
                    created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS budgets (
                    budget_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    amount_cap NUMERIC NOT NULL CHECK (amount_cap > 0),
                    month INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
                    year INTEGER NOT NULL CHECK (year >= 2020),
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                    UNIQUE(user_id, category, month, year)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS notes (
                    note_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT,
                    created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS savings_goals (
                    goal_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    target_amount NUMERIC NOT NULL CHECK (target_amount > 0),
                    saved_amount NUMERIC NOT NULL DEFAULT 0 CHECK (saved_amount >= 0),
                    deadline TEXT,
                    created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS goal_contributions (
                    contribution_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    goal_id INTEGER NOT NULL,
                    amount NUMERIC NOT NULL CHECK (amount > 0),
                    note TEXT,
                    contributed_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
                    FOREIGN KEY (goal_id) REFERENCES savings_goals(goal_id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS monthly_balance (
                    balance_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                    month INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
                    year INTEGER NOT NULL CHECK (year >= 2020),
                    total_amount NUMERIC NOT NULL CHECK (total_amount > 0),
                    UNIQUE (user_id, month, year)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS user_settings (
                    user_id INTEGER PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
                    monthly_income NUMERIC(12,2)
                )
                """,
                "CREATE INDEX IF NOT EXISTS idx_tx_user_date ON transactions (user_id, tx_date DESC)",
                "CREATE INDEX IF NOT EXISTS idx_tx_user_cat  ON transactions (user_id, category)",
                "CREATE INDEX IF NOT EXISTS idx_bg_user_ym   ON budgets (user_id, year, month)",
                "CREATE INDEX IF NOT EXISTS idx_goal_user    ON savings_goals (user_id)",
                "CREATE INDEX IF NOT EXISTS idx_contrib_goal ON goal_contributions (goal_id)",
                "ALTER TABLE users ADD COLUMN security_question TEXT",
                "ALTER TABLE users ADD COLUMN security_answer_hash TEXT"
        };
    }
}
