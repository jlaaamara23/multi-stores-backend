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
 * Normalizes Postgres URLs for Render / Railway.
 * - Converts postgres:// / postgresql:// into jdbc:postgresql://
 * - Uses sslmode=require for public hosts (Render)
 * - Uses sslmode=disable for Railway private networking (*.railway.internal)
 */
public class RenderDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "cloudDatabaseConfig";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("spring.datasource.url")
        );
        if (databaseUrl == null) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();

        try {
            if (databaseUrl.startsWith("jdbc:postgresql:")) {
                String host = extractJdbcHost(databaseUrl);
                properties.put("spring.datasource.url", withSslMode(databaseUrl, host));
                properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
                return;
            }

            if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
                return;
            }

            URI uri = URI.create(databaseUrl.replaceFirst("^postgres://", "postgresql://"));
            String userInfo = uri.getUserInfo();
            if (userInfo == null || !userInfo.contains(":")) {
                throw new IllegalStateException("Database URL is missing username or password.");
            }

            String[] credentials = userInfo.split(":", 2);
            String username = URLDecoder.decode(credentials[0], StandardCharsets.UTF_8);
            String password = URLDecoder.decode(credentials[1], StandardCharsets.UTF_8);
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath() != null ? uri.getPath() : "";
            String host = uri.getHost();
            String jdbcUrl = withSslMode("jdbc:postgresql://" + host + ":" + port + path, host);

            properties.put("spring.datasource.url", jdbcUrl);
            properties.put("spring.datasource.username", username);
            properties.put("spring.datasource.password", password);
            properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse database URL for cloud PostgreSQL", e);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String extractJdbcHost(String jdbcUrl) {
        try {
            String withoutPrefix = jdbcUrl.substring("jdbc:postgresql://".length());
            int slash = withoutPrefix.indexOf('/');
            int question = withoutPrefix.indexOf('?');
            int end = withoutPrefix.length();
            if (slash >= 0) end = Math.min(end, slash);
            if (question >= 0) end = Math.min(end, question);
            String hostPort = withoutPrefix.substring(0, end);
            int colon = hostPort.indexOf(':');
            return colon >= 0 ? hostPort.substring(0, colon) : hostPort;
        } catch (Exception e) {
            return "";
        }
    }

    private static String withSslMode(String jdbcUrl, String host) {
        if (jdbcUrl.contains("sslmode=")) {
            return jdbcUrl;
        }
        boolean railwayInternal = host != null && host.endsWith(".railway.internal");
        String mode = railwayInternal ? "disable" : "require";
        return jdbcUrl.contains("?") ? jdbcUrl + "&sslmode=" + mode : jdbcUrl + "?sslmode=" + mode;
    }
}
