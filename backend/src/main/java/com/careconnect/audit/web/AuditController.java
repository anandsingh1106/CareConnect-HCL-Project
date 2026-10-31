package com.careconnect.audit.web;

import com.careconnect.audit.dto.AuditLogDto;
import com.careconnect.audit.model.AuditAction;
import com.careconnect.audit.model.AuditLog;
import com.careconnect.audit.repository.AuditLogRepository;
import com.careconnect.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Admin - Audit logs")
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditLogRepository repository;

    @Operation(summary = "Search the audit trail (newest first)")
    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<AuditLogDto> search(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String user = username == null || username.isBlank() ? null : username.trim().toLowerCase();
        return PageResponse.of(repository.search(user, patientId, action,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(), pageable), AuditController::toDto);
    }

    private static AuditLogDto toDto(AuditLog a) {
        return new AuditLogDto(a.getId(), a.getUserId(), a.getUsername(), a.getUserRole(), a.getAction(),
                a.getResourceType(), a.getResourceId(), a.getPatientId(), a.getHttpMethod(), a.getRequestPath(),
                a.getStatusCode(), a.getIpAddress(), a.getDetails(), a.getCreatedAt());
    }
}
