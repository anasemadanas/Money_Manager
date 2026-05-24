package com.moneymanager.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConfigTest {

    @Test
    void usesHostedEnvironmentCredentialsAndEnablesSslForSupabase() {
        Properties localProperties = localProperties();
        Map<String, String> environment = Map.of(
                "DATABASE_URL", "postgres://postgres.project-ref@aws-0-us-east-1.pooler.supabase.com:5432/postgres",
                "DATABASE_USERNAME", "postgres.project-ref",
                "DATABASE_PASSWORD", "online-password"
        );

        DatabaseConfig.DatabaseSettings settings = DatabaseConfig.resolveSettings(localProperties, environment);

        assertEquals(
                "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require",
                settings.url()
        );
        assertEquals("postgres.project-ref", settings.username());
        assertEquals("online-password", settings.password());
    }

    @Test
    void readsEmbeddedUrlCredentialsWithoutTruncatingPassword() {
        DatabaseConfig.DatabaseSettings settings = DatabaseConfig.resolveSettings(
                localProperties(),
                Map.of("DATABASE_URL", "postgres://remote-user:password:with:colons@db.example.com:5432/app")
        );

        assertEquals("jdbc:postgresql://db.example.com:5432/app", settings.url());
        assertEquals("remote-user", settings.username());
        assertEquals("password:with:colons", settings.password());
    }

    @Test
    void retainsAnExplicitSupabaseSslMode() {
        assertEquals(
                "jdbc:postgresql://db.project.supabase.co:5432/postgres?sslmode=verify-full",
                DatabaseConfig.normalizeDatabaseUrl(
                        "postgresql://db.project.supabase.co:5432/postgres?sslmode=verify-full"
                )
        );
    }

    private static Properties localProperties() {
        Properties properties = new Properties();
        properties.setProperty("db.url", "jdbc:postgresql://localhost:5432/moneymanager");
        properties.setProperty("db.username", "postgres");
        properties.setProperty("db.password", "local-password");
        return properties;
    }
}
