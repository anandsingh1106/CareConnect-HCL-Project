package com.careconnect.patient.repository;

import com.careconnect.patient.model.PatientEnums.ProblemStatus;
import com.careconnect.patient.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    List<Problem> findByPatientIdOrderByStatusAscOnsetDateDesc(Long patientId);

    List<Problem> findByPatientIdAndStatus(Long patientId, ProblemStatus status);
}
