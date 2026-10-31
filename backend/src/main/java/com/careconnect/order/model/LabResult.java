package com.careconnect.order.model;

import com.careconnect.common.model.Auditable;
import com.careconnect.order.model.OrderEnums.ResultFlag;
import com.careconnect.order.model.OrderEnums.ResultStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Result of a LAB order (numeric value flagged against the test's ranges) or report of an IMAGING order. */
@Getter
@Setter
@Entity
@Table(name = "lab_results")
public class LabResult extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lab_results_seq")
    @SequenceGenerator(name = "lab_results_seq", sequenceName = "lab_results_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private ClinicalOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_test_id")
    private LabTest labTest;

    @Column(name = "value_numeric", precision = 12, scale = 3)
    private BigDecimal valueNumeric;

    @Column(name = "value_text", length = 4000)
    private String valueText;

    @Column(length = 30)
    private String unit;

    @Column(name = "reference_range", length = 60)
    private String referenceRange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ResultFlag flag = ResultFlag.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 12)
    private ResultStatus resultStatus = ResultStatus.FINAL;

    @Column(length = 1000)
    private String comments;

    @Column(name = "resulted_at", nullable = false)
    private LocalDateTime resultedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resulted_by", nullable = false)
    private AppUser resultedBy;
}
