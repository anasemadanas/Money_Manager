package com.moneymanager.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void readsRenderInternalDatabaseUrlWithEmbeddedCredentials() {
        DatabaseConfig.DatabaseSettings settings = DatabaseConfig.resolveSettings(
                localProperties(),
                Map.of("DATABASE_URL", "postgresql://render-user:render-password@dpg-example-a/render-db")
        );

        assertEquals("jdbc:postgresql://dpg-example-a:5432/render-db", settings.url());
        assertEquals("render-user", settings.username());
        assertEquals("render-password", settings.password());
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

    @Test
    void rejectsLocalFallbackWhenRunningOnRenderWithoutDatabaseUrl() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> DatabaseConfig.resolveSettings(localProperties(), Map.of("RENDER", "true"))
        );

        assertTrue(exception.getMessage().contains("DATABASE_URL is required in production"));
    }

    @Test
    void rejectsLocalFallbackForProdProfileWithoutDatabaseUrl() {
        assertThrows(
                IllegalStateException.class,
                () -> DatabaseConfig.resolveSettings(
                        localProperties(),
                        Map.of("SPRING_PROFILES_ACTIVE", "prod")
                )
        );
    }

    @Test
    void retainsLocalFallbackOutsideProduction() {
        DatabaseConfig.DatabaseSettings settings = DatabaseConfig.resolveSettings(localProperties(), Map.of());

        assertEquals("jdbc:postgresql://localhost:5432/moneymanager", settings.url());
        assertEquals("postgres", settings.username());
        assertEquals("local-password", settings.password());
    }

    private static Properties localProperties() {
        Properties properties = new Properties();
        properties.setProperty("db.url", "jdbc:postgresql://localhost:5432/moneymanager");
        properties.setProperty("db.username", "postgres");
        properties.setProperty("db.password", "local-password");
        return properties;
    }
}
