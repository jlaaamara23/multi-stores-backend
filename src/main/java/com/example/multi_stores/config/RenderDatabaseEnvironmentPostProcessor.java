package com.example.multi_stores.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts Render's DATABASE_URL (postgres://...) into a JDBC URL with SSL enabled.
 * Render Postgres rejects non-SSL connections with EOFException during authentication.
 */
public class RenderDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "renderDatabaseConfig";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            return;
        }

        try {
            URI uri = URI.create(databaseUrl.replaceFirst("^postgres://", "postgresql://"));
            String userInfo = uri.getUserInfo();
            if (userInfo == null || !userInfo.contains(":")) {
                throw new IllegalStateException("DATABASE_URL is missing username or password.");
            }

            String[] credentials = userInfo.split(":", 2);
            String username = URLDecoder.decode(credentials[0], StandardCharsets.UTF_8);
            String password = URLDecoder.decode(credentials[1], StandardCharsets.UTF_8);
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath() != null ? uri.getPath() : "";
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + path + "?sslmode=require";

            Map<String, Object> properties = new HashMap<>();
            properties.put("spring.datasource.url", jdbcUrl);
            properties.put("spring.datasource.username", username);
            properties.put("spring.datasource.password", password);
            properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse DATABASE_URL for Render PostgreSQL", e);
        }
    }
}
