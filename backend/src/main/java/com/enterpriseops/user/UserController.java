package com.enterpriseops.user;

import com.enterpriseops.common.PageResponse;
import com.enterpriseops.user.UserDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PageResponse<UserResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String role,
                                             @RequestParam(required = false) Boolean active,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.search(q, role, active, PageRequest.of(page, Math.min(size, 100), Sort.by("fullName")));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPPORT_AGENT')")
    public List<UserResponse> staff() { return service.assignableStaff(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest r) { return service.create(r); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest r) { return service.update(id, r); }

    /** Soft delete: accounts are deactivated, never removed, to keep history intact. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deactivate(@PathVariable Long id) { return service.update(id, new UpdateUserRequest(null, null, false)); }
}
