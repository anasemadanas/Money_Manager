package com.moneymanager.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class LoggingConfig {

    private static volatile boolean initialized = false;

    private LoggingConfig() {}

    public static void init() {
        if (initialized) return;
        synchronized (LoggingConfig.class) {
            if (initialized) return;

            try {
                Path dir = DatabaseConfig.getAppDataDir();
                Files.createDirectories(dir);

                String pattern = dir.resolve("activity.log").toString();
                FileHandler fh = new FileHandler(pattern, true);
                fh.setLevel(Level.ALL);
                fh.setFormatter(new SimpleLineFormatter());

                Logger root = Logger.getLogger("");
                root.setLevel(Level.INFO);

                // Avoid duplicate logs to console in some environments
                for (Handler h : root.getHandlers()) {
                    // keep existing handlers; file handler just adds persistence
                }
                root.addHandler(fh);
            } catch (Exception e) {
                // If logging cannot be configured (permissions, etc.), continue without failing the app.
            } finally {
                initialized = true;
            }
        }
    }

    private static final class SimpleLineFormatter extends Formatter {
        @Override
        public String format(LogRecord r) {
            String msg = r.getMessage();
            if (msg == null) msg = "";
            java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(r.getMillis()),
                    java.time.ZoneId.systemDefault()
            );
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return String.format("[%s] %s%n", ldt.format(dtf), msg);
        }
    }
}

