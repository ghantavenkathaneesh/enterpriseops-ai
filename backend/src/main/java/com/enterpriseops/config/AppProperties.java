package com.enterpriseops.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Ai ai, boolean seedDemo, String demoPassword, List<String> corsAllowedOrigins) {
    public record Jwt(String secret, long expirationMinutes) {}
    public record Ai(boolean enabled, String baseUrl, int timeoutSeconds) {}
}
