package com.enterpriseops.user;

import com.enterpriseops.audit.AuditService;
import com.enterpriseops.common.BadRequestException;
import com.enterpriseops.common.NotFoundException;
import com.enterpriseops.common.PageResponse;
import com.enterpriseops.user.UserDtos.*;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserService(UserRepository users, RoleRepository roles, PasswordEncoder encoder, AuditService audit) {
        this.users = users; this.roles = roles; this.encoder = encoder; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String q, String role, Boolean active, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("fullName")), like), cb.like(cb.lower(root.get("email")), like)));
            }
            if (role != null && !role.isBlank()) ps.add(cb.equal(root.get("role").get("name"), role));
            if (active != null) ps.add(cb.equal(root.get("active"), active));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(users.findAll(spec, pageable), UserResponse::from);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> assignableStaff() {
        return users.findByRoleNameInAndActiveTrue(List.of("ADMIN", "MANAGER", "SUPPORT_AGENT"))
            .stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest r) {
        if (users.existsByEmailIgnoreCase(r.email())) throw new BadRequestException("Email already in use");
        User u = new User();
        u.setEmail(r.email().toLowerCase());
        u.setFullName(r.fullName());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(findRole(r.role()));
        u = users.save(u);
        audit.log("USER_CREATED", "User", u.getId(), "role=" + r.role());
        return UserResponse.from(u);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest r) {
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
        if (r.fullName() != null && !r.fullName().isBlank()) u.setFullName(r.fullName());
        if (r.role() != null) u.setRole(findRole(r.role()));
        if (r.active() != null) u.setActive(r.active());
        audit.log("USER_UPDATED", "User", id, "role=" + u.getRole().getName() + ", active=" + u.isActive());
        return UserResponse.from(users.save(u));
    }

    @Transactional
    public UserResponse updateProfile(Long id, UpdateProfileRequest r) {
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
        u.setFullName(r.fullName());
        return UserResponse.from(users.save(u));
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(users.findById(id).orElseThrow(() -> new NotFoundException("User", id)));
    }

    private Role findRole(String name) {
        return roles.findByName(name).orElseThrow(() -> new BadRequestException("Unknown role: " + name));
    }
}
