package com.careconnect.order.model;

import com.careconnect.order.model.OrderEnums.AlertSeverity;
import com.careconnect.order.model.OrderEnums.AlertType;
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

import java.time.LocalDateTime;

/** A decision-support alert shown when the order was placed, kept for review by pharmacists and auditors. */
@Getter
@Setter
@Entity
@Table(name = "order_alerts")
public class OrderAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_alerts_seq")
    @SequenceGenerator(name = "order_alerts_seq", sequenceName = "order_alerts_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ClinicalOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertSeverity severity;

    @Column(nullable = false, length = 500)
    private String message;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean overridden;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
