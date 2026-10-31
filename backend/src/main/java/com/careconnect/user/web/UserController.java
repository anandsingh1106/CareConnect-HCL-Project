package com.careconnect.user.web;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.user.dto.UserDtos.CreateUserRequest;
import com.careconnect.user.dto.UserDtos.ProviderDto;
import com.careconnect.user.dto.UserDtos.ResetPasswordRequest;
import com.careconnect.user.dto.UserDtos.StatusRequest;
import com.careconnect.user.dto.UserDtos.UpdateUserRequest;
import com.careconnect.user.dto.UserDtos.UserDto;
import com.careconnect.user.model.Role;
import com.careconnect.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Users")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "List users (admin)")
    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserDto> search(@RequestParam(required = false) String search,
                                        @RequestParam(required = false) Role role,
                                        @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return userService.search(search, role, pageable);
    }

    @GetMapping("/api/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto get(@PathVariable Long id) {
        return userService.get(id);
    }

    @Operation(summary = "Create a user account (admin)")
    @PostMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/api/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @Operation(summary = "Activate or deactivate a user (deactivation ends their sessions)")
    @PatchMapping("/api/admin/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto setStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return userService.setActive(id, request.active());
    }

    @PostMapping("/api/admin/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Doctors directory (for appointments and messaging)")
    @GetMapping("/api/users/providers")
    public List<ProviderDto> providers(@RequestParam(required = false) Long departmentId) {
        return userService.providers(departmentId);
    }
}
