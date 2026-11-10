package com.enterpriseops.security;

import com.enterpriseops.user.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserDetails implements UserDetails {
    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final boolean active;

    public AppUserDetails(User u) {
        this.userId = u.getId();
        this.email = u.getEmail();
        this.passwordHash = u.getPasswordHash();
        this.role = u.getRole().getName();
        this.active = u.isActive();
    }

    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public boolean isEmployee() { return "EMPLOYEE".equals(role); }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role)); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return active; }
}
