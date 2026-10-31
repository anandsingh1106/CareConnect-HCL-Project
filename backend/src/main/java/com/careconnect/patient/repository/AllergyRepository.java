package com.careconnect.patient.repository;

import com.careconnect.patient.model.Allergy;
import com.careconnect.patient.model.PatientEnums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllergyRepository extends JpaRepository<Allergy, Long> {

    List<Allergy> findByPatientIdOrderByStatusAscNotedOnDesc(Long patientId);

    List<Allergy> findByPatientIdAndStatus(Long patientId, RecordStatus status);
}
