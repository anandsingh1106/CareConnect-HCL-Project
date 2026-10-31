package com.careconnect.patient.model;

import com.careconnect.common.model.Auditable;
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

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "immunizations")
public class Immunization extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "immunizations_seq")
    @SequenceGenerator(name = "immunizations_seq", sequenceName = "immunizations_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "vaccine_name", nullable = false, length = 120)
    private String vaccineName;

    @Column(name = "dose_number")
    private Integer doseNumber;

    @Column(name = "administered_on", nullable = false)
    private LocalDate administeredOn;

    @Column(name = "administered_by", length = 120)
    private String administeredBy;

    @Column(name = "lot_number", length = 40)
    private String lotNumber;

    @Column(length = 500)
    private String notes;
}
