package com.careconnect.medication.repository;

import com.careconnect.common.exception.ApiException;
import com.careconnect.common.web.AuditContext;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    default Prescription require(Long id) {
        Prescription prescription = findById(id).orElseThrow(() -> ApiException.notFound("Prescription", id));
        AuditContext.setPatientId(prescription.getPatient().getId());
        return prescription;
    }

    @Query("""
            select p from Prescription p
            where (:status is null or p.status = :status)
              and (:patientId is null or p.patient.id = :patientId)
            """)
    Page<Prescription> search(@Param("status") PrescriptionStatus status, @Param("patientId") Long patientId,
                              Pageable pageable);

    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<Prescription> findByPatientIdAndStatusIn(Long patientId, Collection<PrescriptionStatus> statuses);

    Optional<Prescription> findByOrderId(Long orderId);

    List<Prescription> findByOrderIdIn(Collection<Long> orderIds);

    long countByStatus(PrescriptionStatus status);

    long countByStatusAndDispensedAtBetween(PrescriptionStatus status, LocalDateTime from, LocalDateTime to);

    long countByPrescriberIdAndStatus(Long prescriberId, PrescriptionStatus status);
}
