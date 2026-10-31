package com.careconnect.audit.web;

import com.careconnect.audit.model.AuditAction;
import com.careconnect.audit.service.AuditService;
import com.careconnect.common.web.AuditContext;
import com.careconnect.security.SecurityUtils;
import com.careconnect.security.UserPrincipal;
import com.careconnect.user.model.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automatic audit trail for patient data: every create/read/update/delete request on a patient-data
 * endpoint is recorded after it completes (including denied or failed attempts), with user, action,
 * resource, patient, status code and IP address.
 */
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    /** First path segment after /api that holds patient data. */
    private static final Set<String> PATIENT_DATA_RESOURCES = Set.of(
            "patients", "allergies", "problems", "immunizations", "family-history", "documents",
            "encounters", "notes", "diagnoses", "orders", "prescriptions", "mar", "appointments",
            "portal", "messages");

    private static final Pattern PATIENT_PATH = Pattern.compile("^/api/patients/(\\d+)(?:/([a-z-]+))?.*");
    private static final Pattern RESOURCE_PATH = Pattern.compile("^/api/([a-z0-9-]+)(?:/(\\d+))?.*");

    private final AuditService auditService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String path = request.getRequestURI();
        Matcher resource = RESOURCE_PATH.matcher(path);
        if (!resource.matches() || !PATIENT_DATA_RESOURCES.contains(resource.group(1)) || isNoise(path)) {
            return;
        }
        String resourceType = resource.group(1);
        String resourceId = resource.group(2);
        Long patientId = (Long) request.getAttribute(AuditContext.PATIENT_ID_ATTRIBUTE);

        Matcher patientMatch = PATIENT_PATH.matcher(path);
        if (patientMatch.matches()) {
            patientId = Long.valueOf(patientMatch.group(1));
            resourceId = patientMatch.group(1);
            if (patientMatch.group(2) != null) {
                resourceType = "patients/" + patientMatch.group(2);
            }
        }
        if (patientId == null && "portal".equals(resourceType)) {
            patientId = SecurityUtils.currentUserOptional()
                    .filter(u -> u.hasRole(Role.PATIENT)).map(UserPrincipal::patientId).orElse(null);
        }
        auditService.recordAccess(actionFor(request.getMethod()), resourceType, resourceId, patientId,
                request, response.getStatus());
    }

    /** Frequent background polling that would flood the log without adding information. */
    private boolean isNoise(String path) {
        return path.endsWith("/unread-count");
    }

    private AuditAction actionFor(String method) {
        return switch (method) {
            case "POST" -> AuditAction.CREATE;
            case "PUT", "PATCH" -> AuditAction.UPDATE;
            case "DELETE" -> AuditAction.DELETE;
            default -> AuditAction.READ;
        };
    }
}
