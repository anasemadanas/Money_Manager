package com.moneymanager.config;

import java.sql.Connection;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;

public class DatabaseInitializer {

    public static void init() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            enableForeignKeys(conn);
            for (String sql : stripLineComments(loadSchemaSql()).split(";")) {
                String statement = sql.trim();
                if (!statement.isEmpty()) {
                    stmt.execute(statement);
                }
            }

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

    private static String loadSchemaSql() throws Exception {
        try (var in = DatabaseInitializer.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql not found on classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String stripLineComments(String sql) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("--")) {
                cleaned.append(line).append('\n');
            }
        }
        return cleaned.toString();
    }
}
