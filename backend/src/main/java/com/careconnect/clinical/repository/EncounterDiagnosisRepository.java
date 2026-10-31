package com.careconnect.clinical.repository;

import com.careconnect.clinical.model.ClinicalEnums.DiagnosisType;
import com.careconnect.clinical.model.EncounterDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EncounterDiagnosisRepository extends JpaRepository<EncounterDiagnosis, Long> {

    List<EncounterDiagnosis> findByEncounterIdOrderByDiagnosisTypeAscIdAsc(Long encounterId);

    List<EncounterDiagnosis> findByEncounterIdIn(Collection<Long> encounterIds);

    List<EncounterDiagnosis> findByEncounterIdAndDiagnosisType(Long encounterId, DiagnosisType type);

    boolean existsByEncounterIdAndIcd10Code(Long encounterId, String icd10Code);
}
