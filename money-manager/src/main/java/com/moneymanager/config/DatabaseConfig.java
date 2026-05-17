package com.moneymanager.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        String url = resolveUrl(props.getProperty("db.url"));
        String user = props.getProperty("db.username");
        String password = props.getProperty("db.password");

        Connection conn;
        if (user == null || user.isBlank()) {
            conn = DriverManager.getConnection(url);
        } else {
            conn = DriverManager.getConnection(url, user, password);
        }

        enableSqliteForeignKeysIfNeeded(conn);
        return conn;
    }

    /** Directory where the app stores SQLite DB + logs (cross-platform). */
    public static Path getAppDataDir() {
        return getDefaultSqliteDbDir();
    }

    private static String resolveUrl(String configuredUrl) {
        if (configuredUrl == null || configuredUrl.isBlank()) {
            return "jdbc:sqlite:" + getDefaultSqliteDbFile().toString();
        }

        if (!configuredUrl.startsWith("jdbc:sqlite:")) return configuredUrl;

        String pathPart = configuredUrl.substring("jdbc:sqlite:".length());
        if (pathPart.isBlank() || pathPart.equals(":memory:")) return configuredUrl;

        Path p = Paths.get(pathPart);
        if (p.isAbsolute() || pathPart.startsWith("/") || pathPart.startsWith("\\\\")) {
            return configuredUrl;
        }

        return "jdbc:sqlite:" + getDefaultSqliteDbDir().resolve(p).toString();
    }

    private static Path getDefaultSqliteDbFile() {
        return getDefaultSqliteDbDir().resolve("money-manager.db");
    }

    private static Path getDefaultSqliteDbDir() {
        Path baseDir = pickSharedAppDir();
        try {
            Files.createDirectories(baseDir);
        } catch (Exception ignored) {
        }
        return baseDir;
    }

    private static Path pickSharedAppDir() {
        String os = System.getProperty("os.name", "").toLowerCase();

        // Windows: C:\Users\Public\MoneyManager
        if (os.contains("win")) {
            String publicDir = System.getenv("PUBLIC");
            if (publicDir == null || publicDir.isBlank()) publicDir = "C:\\Users\\Public";
            return Paths.get(publicDir, "MoneyManager");
        }

        // macOS: /Users/Shared/MoneyManager (shared between users)
        if (os.contains("mac")) {
            return Paths.get("/Users/Shared", "MoneyManager");
        }

        // Linux: prefer a shared location if writable; otherwise fallback to per-user app data
        Path shared = Paths.get("/var", "lib", "MoneyManager");
        if (isCreatableDir(shared)) return shared;

        return Paths.get(System.getProperty("user.home"), ".local", "share", "MoneyManager");
    }

    private static boolean isCreatableDir(Path dir) {
        try {
            Files.createDirectories(dir);
            return Files.isDirectory(dir) && Files.isWritable(dir);
        } catch (Exception e) {
            return false;
        }
    }

    private static void enableSqliteForeignKeysIfNeeded(Connection conn) {
        try {
            String url = conn.getMetaData().getURL();
            if (url != null && url.startsWith("jdbc:sqlite")) {
                try (var stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }
            }
        } catch (Exception ignored) {
        }
    }
}
