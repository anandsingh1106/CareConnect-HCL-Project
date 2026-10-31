package com.careconnect.clinical.repository;

import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.model.Encounter;
import com.careconnect.common.exception.ApiException;
import com.careconnect.common.web.AuditContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    default Encounter require(Long id) {
        Encounter encounter = findById(id).orElseThrow(() -> ApiException.notFound("Encounter", id));
        AuditContext.setPatientId(encounter.getPatient().getId());
        return encounter;
    }

    Page<Encounter> findByPatientIdOrderByEncounterDateDesc(Long patientId, Pageable pageable);

    List<Encounter> findByPatientIdAndStatusOrderByEncounterDateDesc(Long patientId, EncounterStatus status);

    List<Encounter> findByProviderIdAndStatusOrderByEncounterDateDesc(Long providerId, EncounterStatus status);

    List<Encounter> findByStatusOrderByEncounterDateDesc(EncounterStatus status);

    long countByProviderIdAndStatus(Long providerId, EncounterStatus status);

    long countByProviderIdAndEncounterDateBetween(Long providerId, LocalDateTime from, LocalDateTime to);

    long countByEncounterDateBetween(LocalDateTime from, LocalDateTime to);
}
