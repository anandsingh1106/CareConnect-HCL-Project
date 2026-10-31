package com.careconnect.clinical.model;

import com.careconnect.common.model.Auditable;
import com.careconnect.patient.model.Patient;
import com.careconnect.user.model.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One set of vital signs. BMI is calculated by the server from weight and height. */
@Getter
@Setter
@Entity
@Table(name = "vitals")
public class Vital extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vitals_seq")
    @SequenceGenerator(name = "vitals_seq", sequenceName = "vitals_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id")
    private Encounter encounter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private AppUser recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    private Integer systolic;

    private Integer diastolic;

    private Integer pulse;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "resp_rate")
    private Integer respRate;

    private Integer spo2;

    @Column(name = "weight_kg", precision = 5, scale = 1)
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 1)
    private BigDecimal heightCm;

    @Column(precision = 4, scale = 1)
    private BigDecimal bmi;

    @Column(length = 500)
    private String notes;
}
