package com.careconnect.patient.repository;

import com.careconnect.patient.model.Immunization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImmunizationRepository extends JpaRepository<Immunization, Long> {

    List<Immunization> findByPatientIdOrderByAdministeredOnDesc(Long patientId);
}
