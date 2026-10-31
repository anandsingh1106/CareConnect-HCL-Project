package com.careconnect.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.NumericBooleanConverter;

import java.math.BigDecimal;

/** Lab test catalogue entry with normal and critical ranges used to flag results. */
@Getter
@Setter
@Entity
@Table(name = "lab_tests")
public class LabTest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lab_tests_seq")
    @SequenceGenerator(name = "lab_tests_seq", sequenceName = "lab_tests_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 60)
    private String category;

    @Column(length = 60)
    private String specimen;

    @Column(length = 30)
    private String unit;

    @Column(name = "normal_low", precision = 12, scale = 3)
    private BigDecimal normalLow;

    @Column(name = "normal_high", precision = 12, scale = 3)
    private BigDecimal normalHigh;

    @Column(name = "critical_low", precision = 12, scale = 3)
    private BigDecimal criticalLow;

    @Column(name = "critical_high", precision = 12, scale = 3)
    private BigDecimal criticalHigh;

    @Column(name = "turnaround_hours")
    private Integer turnaroundHours;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean active = true;
}
