package com.careconnect.patient.model;

import com.careconnect.common.model.Auditable;
import com.careconnect.patient.model.PatientEnums.Gender;
import com.careconnect.user.model.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import org.hibernate.type.NumericBooleanConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Patient demographics. Soft-deleted (deleted = 1) records are hidden but never physically removed. */
@Getter
@Setter
@Entity
@Table(name = "patients")
public class Patient extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "patients_seq")
    @SequenceGenerator(name = "patients_seq", sequenceName = "patients_seq", allocationSize = 1)
    private Long id;

    /** Medical Record Number, unique per patient, e.g. CC-2026-000123. */
    @Column(nullable = false, length = 20, updatable = false)
    private String mrn;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(name = "blood_group", length = 5)
    private String bloodGroup;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(name = "address_line", length = 200)
    private String addressLine;

    @Column(length = 60)
    private String city;

    @Column(name = "state_name", length = 60)
    private String stateName;

    @Column(name = "postal_code", length = 12)
    private String postalCode;

    @Column(name = "emergency_name", length = 120)
    private String emergencyName;

    @Column(name = "emergency_phone", length = 20)
    private String emergencyPhone;

    @Column(name = "emergency_relation", length = 40)
    private String emergencyRelation;

    @Column(name = "insurance_provider", length = 100)
    private String insuranceProvider;

    @Column(name = "insurance_policy_no", length = 50)
    private String insurancePolicyNo;

    @Column(name = "insurance_valid_till")
    private LocalDate insuranceValidTill;

    @Column(name = "medical_history", length = 4000)
    private String medicalHistory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_provider_id")
    private AppUser primaryProvider;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 50)
    private String deletedBy;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
