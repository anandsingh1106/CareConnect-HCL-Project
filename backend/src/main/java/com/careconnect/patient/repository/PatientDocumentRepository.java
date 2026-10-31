package com.careconnect.patient.repository;

import com.careconnect.patient.model.PatientDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientDocumentRepository extends JpaRepository<PatientDocument, Long> {

    List<PatientDocument> findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId);

    Optional<PatientDocument> findByIdAndDeletedFalse(Long id);
}
