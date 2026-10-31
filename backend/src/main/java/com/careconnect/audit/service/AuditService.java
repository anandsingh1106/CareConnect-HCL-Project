package com.careconnect.audit.service;

import com.careconnect.audit.model.AuditAction;
import com.careconnect.audit.model.AuditLog;
import com.careconnect.audit.repository.AuditLogRepository;
import com.careconnect.security.SecurityUtils;
import com.careconnect.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Writes audit records in their own transaction so an audit entry is kept even when the
 * business transaction rolls back, and an audit failure never breaks the user's request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    /** Records an authentication event (login, failed login, logout). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthEvent(AuditAction action, Long userId, String username, String role, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setUserId(userId);
        entry.setUsername(truncate(username, 50));
        entry.setUserRole(role);
        entry.setResourceType("auth");
        entry.setDetails(truncate(details, 1000));
        fillRequest(entry);
        save(entry);
    }

    /** Records an API access to patient data (called by the audit interceptor after each request). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAccess(AuditAction action, String resourceType, String resourceId, Long patientId,
                             HttpServletRequest request, int statusCode) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        SecurityUtils.currentUserOptional().ifPresent(user -> applyUser(entry, user));
        entry.setResourceType(truncate(resourceType, 50));
        entry.setResourceId(truncate(resourceId, 50));
        entry.setPatientId(patientId);
        entry.setHttpMethod(request.getMethod());
        entry.setRequestPath(truncate(request.getRequestURI(), 255));
        entry.setStatusCode(statusCode);
        entry.setIpAddress(request.getRemoteAddr());
        save(entry);
    }

    private void applyUser(AuditLog entry, UserPrincipal user) {
        entry.setUserId(user.id());
        entry.setUsername(user.username());
        entry.setUserRole(user.role().name());
    }

    private void fillRequest(AuditLog entry) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            entry.setHttpMethod(request.getMethod());
            entry.setRequestPath(truncate(request.getRequestURI(), 255));
            entry.setIpAddress(request.getRemoteAddr());
        }
    }

    private void save(AuditLog entry) {
        entry.setCreatedAt(LocalDateTime.now());
        try {
            repository.save(entry);
        } catch (RuntimeException ex) {
            log.error("Could not write audit log entry {} {}", entry.getAction(), entry.getRequestPath(), ex);
        }
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
