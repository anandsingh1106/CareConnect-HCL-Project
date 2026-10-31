package com.careconnect.medication.model;

import com.careconnect.medication.model.MedicationEnums.InventoryTxnType;
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

/** Stock movement ledger: every receipt, dispense and adjustment with the resulting balance. */
@Getter
@Setter
@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_txns_seq")
    @SequenceGenerator(name = "inventory_txns_seq", sequenceName = "inventory_txns_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @Column(name = "change_qty", nullable = false)
    private Integer changeQty;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 12)
    private InventoryTxnType txnType;

    @Column(length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private AppUser performedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
