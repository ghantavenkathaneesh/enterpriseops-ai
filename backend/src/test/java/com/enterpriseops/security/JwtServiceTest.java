package com.enterpriseops.security;

import static org.junit.jupiter.api.Assertions.*;

import com.enterpriseops.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static AppProperties props(String secret, long minutes) {
        return new AppProperties(new AppProperties.Jwt(secret, minutes), new AppProperties.Ai(false, "http://x", 1), false, null, List.of());
    }

    @Test
    void validTokenReturnsSubject() {
        JwtService jwt = new JwtService(props("a-test-secret-that-is-at-least-32-chars!", 5));
        String token = jwt.generate(1L, "user@example.com", "ADMIN");
        assertEquals("user@example.com", jwt.validateAndGetSubject(token).orElseThrow());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(props("a-test-secret-that-is-at-least-32-chars!", 5));
        String token = jwt.generate(1L, "user@example.com", "ADMIN");
        assertTrue(jwt.validateAndGetSubject(token + "x").isEmpty());
    }

    @Test
    void tokenSignedWithOtherKeyIsRejected() {
        String token = new JwtService(props("first-secret-that-is-at-least-32-chars!!", 5)).generate(1L, "u@x.io", "ADMIN");
        assertTrue(new JwtService(props("other-secret-that-is-at-least-32-chars!!", 5)).validateAndGetSubject(token).isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(props("a-test-secret-that-is-at-least-32-chars!", -1));
        assertTrue(jwt.validateAndGetSubject(jwt.generate(1L, "u@x.io", "ADMIN")).isEmpty());
    }

    @Test
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class, () -> new JwtService(props("short", 5)));
    }
}
