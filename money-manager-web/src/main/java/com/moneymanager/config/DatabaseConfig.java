package com.moneymanager.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
        String rawUrl = firstNonBlank(
                System.getenv("DATABASE_URL"),
                System.getenv("DB_URL"),
                props.getProperty("db.url")
        );
        String url = normalizeDatabaseUrl(rawUrl);

        String user = firstNonBlank(
                System.getenv("DATABASE_USERNAME"),
                System.getenv("DB_USERNAME"),
                props.getProperty("db.username"),
                extractUsernameFromUrl(rawUrl)
        );
        String password = firstNonBlank(
                System.getenv("DATABASE_PASSWORD"),
                System.getenv("DB_PASSWORD"),
                props.getProperty("db.password"),
                extractPasswordFromUrl(rawUrl)
        );

        if (url == null || url.isBlank()) {
            throw new SQLException("Database URL is not configured");
        }

        if (user == null || user.isBlank()) {
            return DriverManager.getConnection(url);
        }

        return DriverManager.getConnection(url, user, password);
    }

    private static String normalizeDatabaseUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl;
        }

        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                URI uri = URI.create(rawUrl);
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath() != null ? uri.getPath() : "";
                String query = uri.getQuery();
                return "jdbc:postgresql://" + host + ":" + port + path + (query == null ? "" : "?" + query);
            } catch (IllegalArgumentException e) {
                return rawUrl;
            }
        }

        return rawUrl;
    }

    private static String extractUsernameFromUrl(String rawUrl) {
        if (rawUrl == null || !(rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
            return null;
        }
        try {
            URI uri = URI.create(rawUrl);
            String userInfo = uri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                return userInfo.substring(0, userInfo.indexOf(':'));
            }
            return userInfo;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String extractPasswordFromUrl(String rawUrl) {
        if (rawUrl == null || !(rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
            return null;
        }
        try {
            URI uri = URI.create(rawUrl);
            String userInfo = uri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                return userInfo.substring(userInfo.indexOf(':') + 1);
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
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
