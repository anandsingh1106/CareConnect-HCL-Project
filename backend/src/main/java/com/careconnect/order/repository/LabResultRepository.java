package com.careconnect.order.repository;

import com.careconnect.order.model.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LabResultRepository extends JpaRepository<LabResult, Long> {

    Optional<LabResult> findByOrderId(Long orderId);

    @Query("select r from LabResult r where r.order.patient.id = :patientId order by r.resultedAt desc")
    List<LabResult> findForPatient(@Param("patientId") Long patientId);
}
