package com.careconnect.security;

import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The authenticated user as seen by Spring Security. Built from the JWT on every request,
 * so no database lookup is needed to know who is calling and with which role.
 */
public record UserPrincipal(
        Long id,
        String username,
        String fullName,
        Role role,
        Long patientId,
        String passwordHash,
        boolean active) implements UserDetails {

    public static UserPrincipal from(AppUser user) {
        Long patientId = user.getPatient() == null ? null : user.getPatient().getId();
        return new UserPrincipal(user.getId(), user.getUsername(), user.getFullName(), user.getRole(),
                patientId, user.getPasswordHash(), user.isActive());
    }

    public boolean hasRole(Role other) {
        return role == other;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
