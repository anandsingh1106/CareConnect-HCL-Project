package com.careconnect.audit.dto;

import com.careconnect.audit.model.AuditAction;

import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        Long userId,
        String username,
        String userRole,
        AuditAction action,
        String resourceType,
        String resourceId,
        Long patientId,
        String httpMethod,
        String requestPath,
        Integer statusCode,
        String ipAddress,
        String details,
        LocalDateTime createdAt) {
}
