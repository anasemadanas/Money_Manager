package com.moneymanager.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (in == null) {
                throw new RuntimeException("db.properties not found on classpath");
            }

            props.load(in);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = firstNonBlank(
                System.getenv("DATABASE_URL"),
                System.getenv("DB_URL"),
                props.getProperty("db.url")
        );
        String user = firstNonBlank(
                System.getenv("DATABASE_USERNAME"),
                System.getenv("DB_USERNAME"),
                props.getProperty("db.username")
        );
        String password = firstNonBlank(
                System.getenv("DATABASE_PASSWORD"),
                System.getenv("DB_PASSWORD"),
                props.getProperty("db.password")
        );

        if (url == null || url.isBlank()) {
            throw new SQLException("Database URL is not configured");
        }

        if (user == null || user.isBlank()) {
            return DriverManager.getConnection(url);
        }

        return DriverManager.getConnection(url, user, password);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
