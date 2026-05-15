package com.moneymanager.config;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void init() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            enableForeignKeys(conn);

            stmt.execute(getUsersTableSQL(conn));

        } catch (Exception e) {
            throw new RuntimeException("DB init failed", e);
        }
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
                created_at TEXT DEFAULT (datetime('now'))
            );
        """;
    }
}