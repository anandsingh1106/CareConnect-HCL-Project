package com.careconnect.audit.repository;

import com.careconnect.audit.model.AuditAction;
import com.careconnect.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:username is null or lower(a.username) = :username)
              and (:patientId is null or a.patientId = :patientId)
              and (:action is null or a.action = :action)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt < :to)
            """)
    Page<AuditLog> search(@Param("username") String username, @Param("patientId") Long patientId,
                          @Param("action") AuditAction action, @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
