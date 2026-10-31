package com.careconnect.clinical.repository;

import com.careconnect.clinical.model.Vital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VitalRepository extends JpaRepository<Vital, Long> {

    List<Vital> findByPatientIdOrderByRecordedAtAsc(Long patientId);

    List<Vital> findByPatientIdAndRecordedAtBetweenOrderByRecordedAtAsc(Long patientId, LocalDateTime from, LocalDateTime to);

    List<Vital> findByEncounterIdOrderByRecordedAtDesc(Long encounterId);

    Optional<Vital> findFirstByPatientIdOrderByRecordedAtDesc(Long patientId);

    Optional<Vital> findFirstByPatientIdAndHeightCmIsNotNullOrderByRecordedAtDesc(Long patientId);

    long countByRecordedAtBetween(LocalDateTime from, LocalDateTime to);
}
