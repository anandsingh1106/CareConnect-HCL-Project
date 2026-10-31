package com.careconnect.order.model;

import com.careconnect.clinical.model.Encounter;
import com.careconnect.common.model.Auditable;
import com.careconnect.order.model.OrderEnums.OrderPriority;
import com.careconnect.order.model.OrderEnums.OrderStatus;
import com.careconnect.order.model.OrderEnums.OrderType;
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

/** A CPOE order. Type-specific fields: lab test (LAB), study/body site (IMAGING), specialty (REFERRAL); MEDICATION details live in Prescription. */
@Getter
@Setter
@Entity
@Table(name = "clinical_orders")
public class ClinicalOrder extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "clinical_orders_seq")
    @SequenceGenerator(name = "clinical_orders_seq", sequenceName = "clinical_orders_seq", allocationSize = 1)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 20, updatable = false)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordered_by", nullable = false)
    private AppUser orderedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 12)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private OrderStatus status = OrderStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderPriority priority = OrderPriority.ROUTINE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_test_id")
    private LabTest labTest;

    @Column(name = "imaging_study", length = 200)
    private String imagingStudy;

    @Column(name = "body_site", length = 100)
    private String bodySite;

    @Column(name = "referral_specialty", length = 100)
    private String referralSpecialty;

    @Column(name = "referral_to", length = 120)
    private String referralTo;

    @Column(name = "clinical_indication", length = 500)
    private String clinicalIndication;

    @Column(length = 1000)
    private String instructions;

    /** Why the prescriber proceeded despite decision-support alerts. */
    @Column(name = "alert_override_reason", length = 500)
    private String alertOverrideReason;

    @Column(name = "placed_at")
    private LocalDateTime placedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;
}
