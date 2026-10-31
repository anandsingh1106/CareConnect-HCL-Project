package com.careconnect.user.dto;

import com.careconnect.auth.dto.AuthDtos;
import com.careconnect.user.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** DTOs for user and department management. Password hashes are never part of any response. */
public final class UserDtos {

    private UserDtos() {
    }

    public record UserDto(
            Long id,
            String username,
            String email,
            String fullName,
            Role role,
            Long departmentId,
            String departmentName,
            String phone,
            String specialty,
            String licenseNo,
            Long patientId,
            boolean active,
            LocalDateTime lastLoginAt,
            LocalDateTime createdAt) {
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                    message = "Use letters, digits, dot, underscore or hyphen only.") String username,
            @Email @Size(max = 120) String email,
            @NotBlank @Size(max = 120) String fullName,
            @NotNull Role role,
            Long departmentId,
            @Size(max = 20) String phone,
            @Size(max = 100) String specialty,
            @Size(max = 50) String licenseNo,
            Long patientId,
            @NotBlank @Pattern(regexp = AuthDtos.PASSWORD_REGEX, message = AuthDtos.PASSWORD_MESSAGE) String password) {
    }

    public record UpdateUserRequest(
            @Email @Size(max = 120) String email,
            @NotBlank @Size(max = 120) String fullName,
            @NotNull Role role,
            Long departmentId,
            @Size(max = 20) String phone,
            @Size(max = 100) String specialty,
            @Size(max = 50) String licenseNo,
            Long patientId) {
    }

    public record StatusRequest(boolean active) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Pattern(regexp = AuthDtos.PASSWORD_REGEX, message = AuthDtos.PASSWORD_MESSAGE) String newPassword) {
    }

    /** Public directory entry for a doctor (used for booking and messaging). */
    public record ProviderDto(Long id, String fullName, String specialty, Long departmentId, String departmentName) {
    }

    public record DepartmentDto(Long id, String code, String name, String description, boolean active) {
    }

    public record DepartmentRequest(
            @NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Z0-9_]+$", message = "Use capital letters, digits or underscore.") String code,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description,
            boolean active) {
    }
}
