package com.careconnect.patient.repository;

import com.careconnect.patient.model.FamilyHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyHistoryRepository extends JpaRepository<FamilyHistory, Long> {

    List<FamilyHistory> findByPatientIdOrderByRelationAsc(Long patientId);
}
