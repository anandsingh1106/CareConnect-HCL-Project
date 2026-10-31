package com.careconnect.patient.model;

import com.careconnect.common.model.Auditable;
import com.careconnect.patient.model.PatientEnums.AllergySeverity;
import com.careconnect.patient.model.PatientEnums.AllergyType;
import com.careconnect.patient.model.PatientEnums.RecordStatus;
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

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "patient_allergies")
public class Allergy extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "patient_allergies_seq")
    @SequenceGenerator(name = "patient_allergies_seq", sequenceName = "patient_allergies_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** Free text matched against drug name, generic name and drug class by the allergy check. */
    @Column(nullable = false, length = 100)
    private String allergen;

    @Enumerated(EnumType.STRING)
    @Column(name = "allergy_type", nullable = false, length = 20)
    private AllergyType allergyType;

    @Column(length = 200)
    private String reaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AllergySeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecordStatus status = RecordStatus.ACTIVE;

    @Column(name = "noted_on")
    private LocalDate notedOn;
}
