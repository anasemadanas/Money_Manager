package com.moneymanager.mobile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

final class MoneyDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "money_manager_offline.db";
    private static final int DATABASE_VERSION = 1;

    MoneyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "amount_cents INTEGER NOT NULL CHECK(amount_cents > 0)," +
                "category TEXT NOT NULL," +
                "type TEXT NOT NULL CHECK(type IN ('INCOME', 'EXPENSE'))," +
                "occurred_on TEXT NOT NULL," +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        database.execSQL("CREATE TABLE budgets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category TEXT NOT NULL COLLATE NOCASE UNIQUE," +
                "limit_cents INTEGER NOT NULL CHECK(limit_cents > 0))");
        database.execSQL("CREATE TABLE goals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "target_cents INTEGER NOT NULL CHECK(target_cents > 0)," +
                "saved_cents INTEGER NOT NULL DEFAULT 0 CHECK(saved_cents >= 0)," +
                "deadline TEXT)");
        database.execSQL("CREATE TABLE notes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "content TEXT NOT NULL," +
                "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        database.execSQL("CREATE INDEX idx_transactions_date ON transactions(occurred_on DESC)");
        database.execSQL("CREATE INDEX idx_transactions_category_date ON transactions(category, occurred_on)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // Database version 1 is the first native offline release.
    }

    long saveTransaction(TransactionRecord record) {
        ContentValues values = new ContentValues();
        values.put("name", record.name);
        values.put("amount_cents", record.amountCents);
        values.put("category", record.category);
        values.put("type", record.type);
        values.put("occurred_on", record.occurredOn);
        if (record.id == 0) {
            return getWritableDatabase().insertOrThrow("transactions", null, values);
        }
        getWritableDatabase().update("transactions", values, "id = ?",
                new String[]{String.valueOf(record.id)});
        return record.id;
    }

    void deleteTransaction(long id) {
        getWritableDatabase().delete("transactions", "id = ?", new String[]{String.valueOf(id)});
    }

    List<TransactionRecord> getTransactions(String monthPrefix, int limit) {
        List<TransactionRecord> records = new ArrayList<>();
        String where = monthPrefix == null ? null : "occurred_on LIKE ?";
        String[] args = monthPrefix == null ? null : new String[]{monthPrefix + "%"};
        String rowLimit = limit > 0 ? String.valueOf(limit) : null;
        try (Cursor cursor = getReadableDatabase().query("transactions",
                new String[]{"id", "name", "amount_cents", "category", "type", "occurred_on"},
                where, args, null, null, "occurred_on DESC, id DESC", rowLimit)) {
            while (cursor.moveToNext()) {
                records.add(new TransactionRecord(
                        cursor.getLong(0), cursor.getString(1), cursor.getLong(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5)));
            }
        }
        return records;
    }

    Summary getMonthlySummary(String monthPrefix) {
        long income = 0;
        long expenses = 0;
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT type, COALESCE(SUM(amount_cents), 0) FROM transactions " +
                        "WHERE occurred_on LIKE ? GROUP BY type",
                new String[]{monthPrefix + "%"})) {
            while (cursor.moveToNext()) {
                if ("INCOME".equals(cursor.getString(0))) {
                    income = cursor.getLong(1);
                } else {
                    expenses = cursor.getLong(1);
                }
            }
        }
        return new Summary(income, expenses);
    }

    long saveBudget(BudgetRecord record) {
        ContentValues values = new ContentValues();
        values.put("category", record.category);
        values.put("limit_cents", record.limitCents);
        if (record.id == 0) {
            return getWritableDatabase().insertOrThrow("budgets", null, values);
        }
        getWritableDatabase().update("budgets", values, "id = ?", new String[]{String.valueOf(record.id)});
        return record.id;
    }

    void deleteBudget(long id) {
        getWritableDatabase().delete("budgets", "id = ?", new String[]{String.valueOf(id)});
    }

    List<BudgetRecord> getBudgets(String monthPrefix) {
        List<BudgetRecord> records = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT b.id, b.category, b.limit_cents, " +
                        "COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' AND t.occurred_on LIKE ? " +
                        "THEN t.amount_cents ELSE 0 END), 0) " +
                        "FROM budgets b LEFT JOIN transactions t " +
                        "ON LOWER(t.category) = LOWER(b.category) " +
                        "GROUP BY b.id, b.category, b.limit_cents ORDER BY b.category",
                new String[]{monthPrefix + "%"})) {
            while (cursor.moveToNext()) {
                records.add(new BudgetRecord(cursor.getLong(0), cursor.getString(1),
                        cursor.getLong(2), cursor.getLong(3)));
            }
        }
        return records;
    }

    long saveGoal(GoalRecord record) {
        ContentValues values = new ContentValues();
        values.put("name", record.name);
        values.put("target_cents", record.targetCents);
        values.put("saved_cents", record.savedCents);
        values.put("deadline", record.deadline);
        if (record.id == 0) {
            return getWritableDatabase().insertOrThrow("goals", null, values);
        }
        getWritableDatabase().update("goals", values, "id = ?", new String[]{String.valueOf(record.id)});
        return record.id;
    }

    void addGoalContribution(long id, long amountCents) {
        getWritableDatabase().execSQL(
                "UPDATE goals SET saved_cents = saved_cents + ? WHERE id = ?",
                new Object[]{amountCents, id});
    }

    void deleteGoal(long id) {
        getWritableDatabase().delete("goals", "id = ?", new String[]{String.valueOf(id)});
    }

    List<GoalRecord> getGoals() {
        List<GoalRecord> records = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("goals",
                new String[]{"id", "name", "target_cents", "saved_cents", "deadline"},
                null, null, null, null, "id DESC")) {
            while (cursor.moveToNext()) {
                records.add(new GoalRecord(cursor.getLong(0), cursor.getString(1), cursor.getLong(2),
                        cursor.getLong(3), cursor.getString(4)));
            }
        }
        return records;
    }

    long saveNote(NoteRecord record) {
        ContentValues values = new ContentValues();
        values.put("title", record.title);
        values.put("content", record.content);
        if (record.id == 0) {
            return getWritableDatabase().insertOrThrow("notes", null, values);
        }
        getWritableDatabase().update("notes", values, "id = ?", new String[]{String.valueOf(record.id)});
        getWritableDatabase().execSQL("UPDATE notes SET updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                new Object[]{record.id});
        return record.id;
    }

    void deleteNote(long id) {
        getWritableDatabase().delete("notes", "id = ?", new String[]{String.valueOf(id)});
    }

    List<NoteRecord> getNotes() {
        List<NoteRecord> records = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("notes",
                new String[]{"id", "title", "content", "updated_at"},
                null, null, null, null, "updated_at DESC, id DESC")) {
            while (cursor.moveToNext()) {
                records.add(new NoteRecord(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3)));
            }
        }
        return records;
    }

    static final class Summary {
        final long incomeCents;
        final long expenseCents;

        Summary(long incomeCents, long expenseCents) {
            this.incomeCents = incomeCents;
            this.expenseCents = expenseCents;
        }
    }

    static final class TransactionRecord {
        final long id;
        final String name;
        final long amountCents;
        final String category;
        final String type;
        final String occurredOn;

        TransactionRecord(long id, String name, long amountCents, String category, String type,
                          String occurredOn) {
            this.id = id;
            this.name = name;
            this.amountCents = amountCents;
            this.category = category;
            this.type = type;
            this.occurredOn = occurredOn;
        }
    }

    static final class BudgetRecord {
        final long id;
        final String category;
        final long limitCents;
        final long spentCents;

        BudgetRecord(long id, String category, long limitCents, long spentCents) {
            this.id = id;
            this.category = category;
            this.limitCents = limitCents;
            this.spentCents = spentCents;
        }
    }

    static final class GoalRecord {
        final long id;
        final String name;
        final long targetCents;
        final long savedCents;
        final String deadline;

        GoalRecord(long id, String name, long targetCents, long savedCents, String deadline) {
            this.id = id;
            this.name = name;
            this.targetCents = targetCents;
            this.savedCents = savedCents;
            this.deadline = deadline;
        }
    }

    static final class NoteRecord {
        final long id;
        final String title;
        final String content;
        final String updatedAt;

        NoteRecord(long id, String title, String content, String updatedAt) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.updatedAt = updatedAt;
        }
    }
}
