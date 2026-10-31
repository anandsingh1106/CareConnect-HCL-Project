package com.careconnect.scheduling.repository;

import com.careconnect.scheduling.model.Appointment;
import com.careconnect.scheduling.model.SchedulingEnums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            select a from Appointment a
            where (:providerId is null or a.provider.id = :providerId)
              and (:patientId is null or a.patient.id = :patientId)
              and (:status is null or a.status = :status)
              and (:from is null or a.startTime >= :from)
              and (:to is null or a.startTime < :to)
            """)
    Page<Appointment> search(@Param("providerId") Long providerId, @Param("patientId") Long patientId,
                             @Param("status") AppointmentStatus status, @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to, Pageable pageable);

    List<Appointment> findByPatientIdOrderByStartTimeDesc(Long patientId);

    List<Appointment> findByPatientIdAndStartTimeAfterAndStatusInOrderByStartTimeAsc(Long patientId, LocalDateTime after,
                                                                                    Collection<AppointmentStatus> statuses);

    /** Overlap check: another active appointment of the same patient in the same time window. */
    @Query("""
            select count(a) > 0 from Appointment a
            where a.patient.id = :patientId and a.status in :activeStatuses
              and a.startTime < :end and a.endTime > :start
            """)
    boolean patientHasOverlap(@Param("patientId") Long patientId, @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end,
                              @Param("activeStatuses") Collection<AppointmentStatus> activeStatuses);

    List<Appointment> findByProviderIdAndStartTimeBetweenOrderByStartTimeAsc(Long providerId, LocalDateTime from, LocalDateTime to);

    long countByStartTimeBetween(LocalDateTime from, LocalDateTime to);

    long countByStatusAndStartTimeBetween(AppointmentStatus status, LocalDateTime from, LocalDateTime to);

    long countByProviderIdAndStartTimeBetweenAndStatusIn(Long providerId, LocalDateTime from, LocalDateTime to,
                                                         Collection<AppointmentStatus> statuses);
}
