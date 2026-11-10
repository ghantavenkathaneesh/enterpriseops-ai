package com.enterpriseops.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    public static Optional<AppUserDetails> find() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof AppUserDetails d) return Optional.of(d);
        return Optional.empty();
    }

    public static AppUserDetails get() {
        return find().orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }
}
