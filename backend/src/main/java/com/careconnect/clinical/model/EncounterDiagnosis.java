package com.careconnect.clinical.model;

import com.careconnect.clinical.model.ClinicalEnums.DiagnosisType;
import com.careconnect.common.model.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "encounter_diagnoses")
public class EncounterDiagnosis extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "encounter_diagnoses_seq")
    @SequenceGenerator(name = "encounter_diagnoses_seq", sequenceName = "encounter_diagnoses_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "icd10_code", nullable = false)
    private Icd10Code icd10;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagnosis_type", nullable = false, length = 10)
    private DiagnosisType diagnosisType = DiagnosisType.SECONDARY;

    @Column(length = 500)
    private String notes;
}
