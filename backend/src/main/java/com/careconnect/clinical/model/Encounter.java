package com.careconnect.clinical.model;

import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.model.ClinicalEnums.EncounterType;
import com.careconnect.common.model.Auditable;
import com.careconnect.patient.model.Patient;
import com.careconnect.scheduling.model.Appointment;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Department;
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

/** A visit: links one patient with one provider. Notes, diagnoses, vitals and orders hang off it. */
@Getter
@Setter
@Entity
@Table(name = "encounters")
public class Encounter extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "encounters_seq")
    @SequenceGenerator(name = "encounters_seq", sequenceName = "encounters_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private AppUser provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(name = "encounter_type", nullable = false, length = 15)
    private EncounterType encounterType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EncounterStatus status = EncounterStatus.OPEN;

    @Column(name = "encounter_date", nullable = false)
    private LocalDateTime encounterDate;

    @Column(name = "chief_complaint", length = 500)
    private String chiefComplaint;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
