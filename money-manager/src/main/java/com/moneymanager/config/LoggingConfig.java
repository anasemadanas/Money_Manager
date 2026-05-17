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

                // Rotate logs: ~1MB each, keep 5 files
                String pattern = dir.resolve("money-manager-%g.log").toString();
                FileHandler fh = new FileHandler(pattern, 1024 * 1024, 5, true);
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
            String thrown = "";
            if (r.getThrown() != null) {
                thrown = " | " + r.getThrown().toString();
            }
            return String.format(
                    "%1$tF %1$tT.%1$tLZ | %2$-7s | %3$s | %4$s%5$s%n",
                    java.time.Instant.ofEpochMilli(r.getMillis()),
                    r.getLevel().getName(),
                    r.getLoggerName(),
                    msg,
                    thrown
            );
        }
    }
}

