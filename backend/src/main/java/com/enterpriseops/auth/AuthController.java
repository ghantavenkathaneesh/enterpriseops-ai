package com.enterpriseops.auth;

import com.enterpriseops.audit.AuditService;
import com.enterpriseops.security.AppUserDetails;
import com.enterpriseops.security.CurrentUser;
import com.enterpriseops.security.JwtService;
import com.enterpriseops.user.UserDtos.*;
import com.enterpriseops.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record LoginResponse(String token, UserResponse user) {}

    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UserService users;
    private final AuditService audit;

    public AuthController(AuthenticationManager authManager, JwtService jwt, UserService users, AuditService audit) {
        this.authManager = authManager; this.jwt = jwt; this.users = users; this.audit = audit;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest r) {
        Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(r.email(), r.password()));
        AppUserDetails d = (AppUserDetails) auth.getPrincipal();
        audit.logAs(d.getUserId(), "LOGIN", "User", d.getUserId(), null);
        return new LoginResponse(jwt.generate(d.getUserId(), d.getUsername(), d.getRole()), users.get(d.getUserId()));
    }

    /** JWTs are stateless; the client discards its token on logout. This endpoint records the event for auditing. */
    @PostMapping("/logout")
    public void logout() {
        audit.log("LOGOUT", "User", CurrentUser.get().getUserId(), null);
    }

    @GetMapping("/me")
    public UserResponse me() { return users.get(CurrentUser.get().getUserId()); }

    @PutMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest r) {
        return users.updateProfile(CurrentUser.get().getUserId(), r);
    }
}
