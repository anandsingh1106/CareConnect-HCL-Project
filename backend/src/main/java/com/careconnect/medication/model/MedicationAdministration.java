package com.careconnect.medication.model;

import com.careconnect.common.model.Auditable;
import com.careconnect.medication.model.MedicationEnums.AdministrationStatus;
import com.careconnect.medication.model.MedicationEnums.Route;
import com.careconnect.patient.model.Patient;
import com.careconnect.user.model.AppUser;
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

import java.time.LocalDateTime;

/** Medication Administration Record (MAR) entry made by a nurse. */
@Getter
@Setter
@Entity
@Table(name = "medication_administrations")
public class MedicationAdministration extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "med_admins_seq")
    @SequenceGenerator(name = "med_admins_seq", sequenceName = "med_admins_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administered_by", nullable = false)
    private AppUser administeredBy;

    @Column(name = "administered_at", nullable = false)
    private LocalDateTime administeredAt;

    @Column(name = "dose_given", length = 50)
    private String doseGiven;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Route route;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_status", nullable = false, length = 10)
    private AdministrationStatus status;

    @Column(length = 500)
    private String notes;
}
