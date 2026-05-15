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
    deadline        DATE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS goal_contributions (
    contribution_id BIGSERIAL       PRIMARY KEY,
    goal_id         BIGINT          NOT NULL REFERENCES savings_goals(goal_id) ON DELETE CASCADE,
    amount          NUMERIC(12,2)   NOT NULL CHECK (amount > 0),
    note            VARCHAR(200),
    contributed_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_tx_user_date ON transactions (user_id, tx_date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_user_cat  ON transactions (user_id, category);
CREATE INDEX IF NOT EXISTS idx_bg_user_ym   ON budgets (user_id, year, month);
CREATE INDEX IF NOT EXISTS idx_goal_user    ON savings_goals (user_id);
CREATE INDEX IF NOT EXISTS idx_contrib_goal ON goal_contributions (goal_id);
