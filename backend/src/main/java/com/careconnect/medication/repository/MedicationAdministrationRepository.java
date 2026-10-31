package com.careconnect.medication.repository;

import com.careconnect.medication.model.MedicationAdministration;
import com.careconnect.medication.model.MedicationEnums.AdministrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, Long> {

    List<MedicationAdministration> findByPatientIdOrderByAdministeredAtDesc(Long patientId);

    List<MedicationAdministration> findByPrescriptionIdOrderByAdministeredAtDesc(Long prescriptionId);

    long countByAdministeredAtBetween(LocalDateTime from, LocalDateTime to);

    long countByStatusAndAdministeredAtBetween(AdministrationStatus status, LocalDateTime from, LocalDateTime to);
}
