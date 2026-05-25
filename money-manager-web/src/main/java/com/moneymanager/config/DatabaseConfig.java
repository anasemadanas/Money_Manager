package com.moneymanager.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
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
        DatabaseSettings settings = resolveSettings(props, System.getenv());

        System.out.println("Connecting to PostgreSQL host: " + settings.hostDescription());
        return DriverManager.getConnection(settings.url(), settings.username(), settings.password());
    }

    static DatabaseSettings resolveSettings(Properties properties, Map<String, String> environment) {
        String environmentUrl = firstNonBlank(environment.get("DATABASE_URL"));

        if (environmentUrl == null && isProductionRuntime(environment)) {
            throw new IllegalStateException(
                    "DATABASE_URL is required in production. Set it in the Render service environment, "
                            + "plus DATABASE_USERNAME and DATABASE_PASSWORD unless the URL includes credentials."
            );
        }

        String rawUrl = firstNonBlank(
                environmentUrl,
                properties.getProperty("db.url"),
                "jdbc:postgresql://localhost:5432/postgres"
        );

        String url = normalizeDatabaseUrl(rawUrl);
        String user = firstNonBlank(
                environment.get("DATABASE_USERNAME"),
                extractUsernameFromUrl(rawUrl),
                properties.getProperty("db.username"),
                "postgres"
        );
        String password = firstNonNull(
                environment.get("DATABASE_PASSWORD"),
                extractPasswordFromUrl(rawUrl),
                properties.getProperty("db.password"),
                ""
        );

        return new DatabaseSettings(url, user, password);
    }

    private static boolean isProductionRuntime(Map<String, String> environment) {
        if ("true".equalsIgnoreCase(environment.get("RENDER"))) {
            return true;
        }

        String activeProfiles = environment.get("SPRING_PROFILES_ACTIVE");
        if (activeProfiles != null) {
            for (String profile : activeProfiles.split(",")) {
                if ("prod".equalsIgnoreCase(profile.trim())) {
                    return true;
                }
            }
        }

        return false;
    }

    static String normalizeDatabaseUrl(String rawUrl) {

        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        if (rawUrl.startsWith("jdbc:")) {
            return addSupabaseSslMode(rawUrl);
        }

        if (rawUrl.startsWith("postgres://")
                || rawUrl.startsWith("postgresql://")) {

            try {
                URI uri = URI.create(rawUrl);

                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String query = uri.getQuery();

                String jdbcUrl = "jdbc:postgresql://"
                        + host
                        + ":"
                        + port
                        + path
                        + (query != null ? "?" + query : "");
                return addSupabaseSslMode(jdbcUrl);

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
            int separator = userInfo == null ? -1 : userInfo.indexOf(':');
            if (separator > 0) {
                return userInfo.substring(0, separator);
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    private static String extractPasswordFromUrl(String rawUrl) {

        try {
            URI uri = URI.create(rawUrl);

            String userInfo = uri.getUserInfo();
            int separator = userInfo == null ? -1 : userInfo.indexOf(':');
            if (separator > 0) {
                return userInfo.substring(separator + 1);
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    private static String addSupabaseSslMode(String jdbcUrl) {
        try {
            URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
            String host = uri.getHost();
            String query = uri.getQuery();
            boolean isSupabase = host != null
                    && (host.endsWith(".supabase.co") || host.endsWith(".supabase.com"));
            boolean hasSslMode = query != null && query.toLowerCase().contains("sslmode=");

            if (isSupabase && !hasSslMode) {
                return jdbcUrl + (query == null ? "?" : "&") + "sslmode=require";
            }
        } catch (Exception ignored) {
        }

        return jdbcUrl;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    record DatabaseSettings(String url, String username, String password) {
        String hostDescription() {
            try {
                URI uri = URI.create(url.substring("jdbc:".length()));
                return uri.getHost() + ":" + uri.getPort();
            } catch (Exception ignored) {
                return "configured database";
            }
        }
    }
}
