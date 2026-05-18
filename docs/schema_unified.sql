-- ===========================================================================
-- MONEY MANAGER JAVA SUITE - UNIFIED DATABASE SCHEMAS
-- ===========================================================================
-- This file compiles both physical database schemas used in the project:
-- 1. SQLite Schema (Used by the JavaFX Offline-First Desktop Application)
-- 2. PostgreSQL Schema (Used by the Spring Boot Enterprise Web Application)
--
-- Both schemas implement identical logical structures, relations, constraints,
-- and indexes, but are optimized for their respective database engines.
-- ===========================================================================

-- ===========================================================================
-- PART 1: SQLITE SCHEMA (DESKTOP CLIENT)
-- Persistent database file: money-manager.db
-- Driver: org.xerial:sqlite-jdbc
-- ===========================================================================

-- 1. Users Profile Table (Local Profiles)
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    security_question TEXT,
    security_answer_hash TEXT,
    created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

-- 2. Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    amount REAL NOT NULL CHECK (amount > 0),
    category TEXT NOT NULL,
    tx_type TEXT NOT NULL CHECK (tx_type IN ('INCOME','EXPENSE')),
    tx_date TEXT NOT NULL,
    created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 3. Monthly Category Budgets
CREATE TABLE IF NOT EXISTS budgets (
    budget_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    category TEXT NOT NULL,
    amount_cap REAL NOT NULL CHECK (amount_cap > 0),
    month INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    year INTEGER NOT NULL CHECK (year >= 2020),

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE(user_id, category, month, year)
);

-- 4. User Scratch Notes
CREATE TABLE IF NOT EXISTS notes (
    note_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    content TEXT,
    created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 5. Savings Goals
CREATE TABLE IF NOT EXISTS savings_goals (
    goal_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    target_amount REAL NOT NULL CHECK (target_amount > 0),
    saved_amount REAL NOT NULL DEFAULT 0 CHECK (saved_amount >= 0),
    deadline TEXT,
    created_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 6. Goal Contributions
CREATE TABLE IF NOT EXISTS goal_contributions (
    contribution_id INTEGER PRIMARY KEY AUTOINCREMENT,
    goal_id INTEGER NOT NULL,
    amount REAL NOT NULL CHECK (amount > 0),
    note TEXT,
    contributed_at TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),

    FOREIGN KEY (goal_id) REFERENCES savings_goals(goal_id) ON DELETE CASCADE
);

-- 7. Monthly Balance Tracking
CREATE TABLE IF NOT EXISTS monthly_balance (
    balance_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    month INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    year INTEGER NOT NULL CHECK (year >= 2020),
    total_amount REAL NOT NULL CHECK (total_amount > 0),

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE (user_id, month, year)
);

-- 8. User Custom Settings
CREATE TABLE IF NOT EXISTS user_settings (
    user_id INTEGER PRIMARY KEY,
    monthly_income REAL,

    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 9. SQLite Indexes for Optimization
CREATE INDEX IF NOT EXISTS idx_tx_user_date ON transactions (user_id, tx_date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_user_cat  ON transactions (user_id, category);
CREATE INDEX IF NOT EXISTS idx_bg_user_ym   ON budgets (user_id, year, month);
CREATE INDEX IF NOT EXISTS idx_goal_user    ON savings_goals (user_id);
CREATE INDEX IF NOT EXISTS idx_contrib_goal ON goal_contributions (goal_id);


-- ===========================================================================
-- PART 2: POSTGRESQL SCHEMA (SPRING BOOT WEB APP)
-- Driver: org.postgresql:postgresql
-- ===========================================================================

-- 1. Users Table (Enterprise Profiles)
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    category VARCHAR(50) NOT NULL,
    tx_type VARCHAR(10) NOT NULL CHECK (tx_type IN ('INCOME','EXPENSE')),
    tx_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Monthly Category Budgets
CREATE TABLE IF NOT EXISTS budgets (
    budget_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    amount_cap NUMERIC(12,2) NOT NULL CHECK (amount_cap > 0),
    month SMALLINT NOT NULL CHECK (month BETWEEN 1 AND 12),
    year SMALLINT NOT NULL CHECK (year >= 2020),
    UNIQUE (user_id, category, month, year)
);

-- 4. User Scratch Notes
CREATE TABLE IF NOT EXISTS notes (
    note_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    content TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 5. Savings Goals
CREATE TABLE IF NOT EXISTS savings_goals (
    goal_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    target_amount NUMERIC(12,2) NOT NULL CHECK (target_amount > 0),
    saved_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (saved_amount >= 0),
    deadline DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 6. Goal Contributions
CREATE TABLE IF NOT EXISTS goal_contributions (
    contribution_id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES savings_goals(goal_id) ON DELETE CASCADE,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    note VARCHAR(200),
    contributed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 7. Monthly Balance Tracking
CREATE TABLE IF NOT EXISTS monthly_balance (
    balance_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    month SMALLINT NOT NULL CHECK (month BETWEEN 1 AND 12),
    year SMALLINT NOT NULL CHECK (year >= 2020),
    total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount > 0),
    UNIQUE (user_id, month, year)
);

-- 8. User Custom Settings
CREATE TABLE IF NOT EXISTS user_settings (
    user_id BIGINT PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    monthly_income NUMERIC(12,2)
);

-- 9. PostgreSQL Indexes for Query Optimization
CREATE INDEX IF NOT EXISTS idx_tx_user_date ON transactions (user_id, tx_date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_user_cat  ON transactions (user_id, category);
CREATE INDEX IF NOT EXISTS idx_bg_user_ym   ON budgets (user_id, year, month);
CREATE INDEX IF NOT EXISTS idx_goal_user    ON savings_goals (user_id);
CREATE INDEX IF NOT EXISTS idx_contrib_goal ON goal_contributions (goal_id);
