package com.enterpriseops.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {}

    public record UserResponse(Long id, String email, String fullName, String role, boolean active) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole().getName(), u.isActive());
        }
    }
    public record CreateUserRequest(@NotBlank @Email String email, @NotBlank @Size(max = 150) String fullName,
                                    @NotBlank @Size(min = 8, max = 72) String password, @NotBlank String role) {}
    public record UpdateUserRequest(@Size(max = 150) String fullName, String role, Boolean active) {}
    public record UpdateProfileRequest(@NotBlank @Size(max = 150) String fullName) {}
}
