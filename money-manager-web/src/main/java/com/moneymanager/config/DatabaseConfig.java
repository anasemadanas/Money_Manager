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
        try (InputStream in = DatabaseConfig.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (in != null) {
                props.load(in);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
    }

    public static Connection getConnection() throws SQLException {

        String rawUrl = System.getenv("DATABASE_URL");

   
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = props.getProperty(
                    "db.url",
                    "jdbc:postgresql://localhost:5432/postgres"
            );
        }

        String url = normalizeDatabaseUrl(rawUrl);

        String user = extractUsernameFromUrl(rawUrl);
        String password = extractPasswordFromUrl(rawUrl);

 
        if (user == null || user.isBlank()) {
            user = props.getProperty("db.username", "postgres");
        }

        if (password == null) {
            password = props.getProperty("db.password", "");
        }

        System.out.println("Connecting to DB:");
        System.out.println(url);
        System.out.println(user);

        return DriverManager.getConnection(url, user, password);
    }

    private static String normalizeDatabaseUrl(String rawUrl) {

        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }


        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl;
        }


        if (rawUrl.startsWith("postgres://")
                || rawUrl.startsWith("postgresql://")) {

            try {
                URI uri = URI.create(rawUrl);

                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String query = uri.getQuery();

                return "jdbc:postgresql://"
                        + host
                        + ":"
                        + port
                        + path
                        + (query != null ? "?" + query : "");

            } catch (Exception e) {
                throw new RuntimeException("Invalid DATABASE_URL", e);
            }
        }

        return rawUrl;
    }

    private static String extractUsernameFromUrl(String rawUrl) {

        try {
            URI uri = URI.create(rawUrl);

            String userInfo = uri.getUserInfo();

            if (userInfo != null && userInfo.contains(":")) {
                return userInfo.split(":")[0];
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    private static String extractPasswordFromUrl(String rawUrl) {

        try {
            URI uri = URI.create(rawUrl);

            String userInfo = uri.getUserInfo();

            if (userInfo != null && userInfo.contains(":")) {
                return userInfo.split(":")[1];
            }

        } catch (Exception ignored) {
        }

        return null;
    }
}