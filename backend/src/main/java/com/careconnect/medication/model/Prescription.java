package com.careconnect.medication.model;

import com.careconnect.clinical.model.Encounter;
import com.careconnect.common.model.Auditable;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.MedicationEnums.Route;
import com.careconnect.order.model.ClinicalOrder;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Electronic prescription created by a MEDICATION order, then verified and dispensed by pharmacy. */
@Getter
@Setter
@Entity
@Table(name = "prescriptions")
public class Prescription extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prescriptions_seq")
    @SequenceGenerator(name = "prescriptions_seq", sequenceName = "prescriptions_seq", allocationSize = 1)
    private Long id;

    @Column(name = "rx_no", nullable = false, length = 20, updatable = false)
    private String rxNo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private ClinicalOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescriber_id", nullable = false)
    private AppUser prescriber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @Column(nullable = false, length = 50)
    private String dose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Route route;

    @Column(nullable = false, length = 30)
    private String frequency;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 500)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 22)
    private PrescriptionStatus status = PrescriptionStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private AppUser verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "pharmacist_note", length = 500)
    private String pharmacistNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispensed_by")
    private AppUser dispensedBy;

    @Column(name = "dispensed_at")
    private LocalDateTime dispensedAt;

    @Column(name = "dispensed_qty")
    private Integer dispensedQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discontinued_by")
    private AppUser discontinuedBy;

    @Column(name = "discontinued_at")
    private LocalDateTime discontinuedAt;

    @Column(name = "discontinue_reason", length = 500)
    private String discontinueReason;

    /** Set when this prescription replaced (modified) an earlier one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaces_id")
    private Prescription replaces;

    /**
     * Whether the patient is currently on this medicine: pending/verified prescriptions always,
     * dispensed ones until the prescribed duration has elapsed.
     */
    public boolean isCurrentlyActive() {
        return switch (status) {
            case PENDING_VERIFICATION, VERIFIED -> true;
            case DISPENSED -> dispensedAt == null
                    || !dispensedAt.toLocalDate().plusDays(durationDays).isBefore(LocalDate.now());
            default -> false;
        };
    }
}
