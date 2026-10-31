package com.careconnect.security;

import com.careconnect.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Convenience access to the logged-in user from services. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> currentUserOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UserPrincipal currentUser() {
        return currentUserOptional()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required."));
    }

    /** Patient id of a logged-in PATIENT user; the portal uses this so patients only ever see their own record. */
    public static Long currentPatientId() {
        Long patientId = currentUser().patientId();
        if (patientId == null) {
            throw ApiException.forbidden("This account is not linked to a patient record.");
        }
        return patientId;
    }
}
