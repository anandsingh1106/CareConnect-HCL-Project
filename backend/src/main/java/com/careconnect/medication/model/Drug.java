package com.careconnect.medication.model;

import com.careconnect.common.model.Auditable;
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

/** Drug master record with stock level. The inherited @Version prevents lost updates when two pharmacists dispense at once. */
@Getter
@Setter
@Entity
@Table(name = "drugs")
public class Drug extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "drugs_seq")
    @SequenceGenerator(name = "drugs_seq", sequenceName = "drugs_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "generic_name", nullable = false, length = 120)
    private String genericName;

    /** Pharmacological class; the allergy check matches allergens against it (e.g. "Penicillin" -> "Penicillins"). */
    @Column(name = "drug_class", length = 100)
    private String drugClass;

    @Column(name = "dosage_form", nullable = false, length = 30)
    private String dosageForm;

    @Column(length = 40)
    private String strength;

    @Column(name = "default_route", length = 20)
    private String defaultRoute;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "stock_qty", nullable = false)
    private Integer stockQty = 0;

    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel = 0;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean active = true;

    public boolean isLowStock() {
        return stockQty <= reorderLevel;
    }
}
